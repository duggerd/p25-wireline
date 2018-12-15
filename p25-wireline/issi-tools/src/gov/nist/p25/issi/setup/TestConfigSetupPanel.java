//
package gov.nist.p25.issi.setup;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import gov.nist.p25.common.swing.table.ComboBoxEditor;
import gov.nist.p25.common.swing.table.ComboBoxRenderer;
import gov.nist.p25.common.swing.table.HexEditor;
import gov.nist.p25.common.swing.table.HexRenderer;
import gov.nist.p25.common.swing.table.TableColumnHeaderToolTips;
import gov.nist.p25.common.swing.table.TextFieldEditor;
import gov.nist.p25.common.swing.widget.FilteredDocument;
import gov.nist.p25.common.swing.widget.GridBagConstraints2;

/**
 * TestConfigSetup panel.
 */
public class TestConfigSetupPanel extends JPanel 
{
   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private static final int TABLE_ROW_HEIGHT = 20;  // 18

   private int mode;
   private String title;
   private List<RfssConfigSetup> epList;
   private List<GroupConfigSetup> gcsList;
   private List<SuConfigSetup> scsList;
   private Vector<String> rfssVec;

   private JTable rfssTable;
   private JTable gcsTable;
   private JTable scsTable;

   // accessor
   public List<RfssConfigSetup> getRfssConfigSetupList() {
      return epList;
   }
   public List<GroupConfigSetup> getGroupConfigSetupList() {
      return gcsList;
   }
   public List<SuConfigSetup> getSuConfigSetupList() {
      return scsList;
   }

   // constructor
   public TestConfigSetupPanel() 
   {
      this( 0, "", new ArrayList<RfssConfigSetup>(), 
                   new ArrayList<SuConfigSetup>(),
                   new ArrayList<GroupConfigSetup>());
   } 
   public TestConfigSetupPanel(int mode, String title, List<RfssConfigSetup> epList,
         List<SuConfigSetup> scsList, List<GroupConfigSetup> gcsList)
   {
      this.mode = mode;
      this.title = title;
      this.epList = epList;
      this.scsList = scsList;
      this.gcsList = gcsList;
      initRfssVector();
      buildGUI();
   }

   public void initRfssVector() {
      rfssVec = new Vector<String>();
      for( RfssConfigSetup rcs: epList) {
         String name = rcs.getName();
	 if( name != null && name.length() > 0) {
            rfssVec.add( new String(name));
         }
      }
   }

   protected void setupTableHeaderFont(JTable table) 
   {
      // setup column header bold font
      JTableHeader header = table.getTableHeader();
      final Font boldFont = header.getFont().deriveFont(Font.BOLD);
      final TableCellRenderer headerRenderer = header.getDefaultRenderer();
      header.setDefaultRenderer( new TableCellRenderer() {
         public Component getTableCellRendererComponent(JTable table,
               Object value, boolean isSelected, boolean hasFocus,
               int row, int column) {
            Component c = headerRenderer.getTableCellRendererComponent(
               table, value, isSelected, hasFocus, row, column );
            c.setFont( boldFont);
            return c;
         }
      });
   }

   public JTable buildRfssTable(List<RfssConfigSetup> epList)
   {
      if( epList.size() == 0)
         return null;

      // setup RFSS Config table
      RfssConfigSetupTableModel model = new RfssConfigSetupTableModel(mode, epList);
      model.addPropertyChangeListener( new PropertyChangeListener() {

         public void propertyChange(PropertyChangeEvent evt) {
//showln("propertyChange: propertyName="+evt.getPropertyName());
            if( "RfssName".equals(evt.getPropertyName())) {
               String oldValue = (String)evt.getOldValue();
               String newValue = (String)evt.getNewValue();
//showln("propertyChange: oldValue="+oldValue+"  new="+newValue);
               if( oldValue != null && rfssVec.contains(oldValue)) {
                  rfssVec.setElementAt( newValue, rfssVec.indexOf(oldValue));
               } else {
                  rfssVec.add( newValue);
               }
               for( GroupConfigSetup gcs: gcsList) {
                  if( gcs.getHomeRfssName().equals(oldValue)) {
                     gcs.setHomeRfssName(newValue);
                  }
               }
               for( SuConfigSetup scs: scsList) {
                  if( scs.getHomeRfssName().equals(oldValue)) {
                     scs.setHomeRfssName(newValue);
                  }
                  if( scs.getServingRfssName().equals(oldValue)) {
                     scs.setServingRfssName(newValue);
                  }
               }
               updateUI();
            }
            if( "RfssEmulated".equals(evt.getPropertyName())) {
               RfssConfigSetup configSetup = (RfssConfigSetup)evt.getOldValue();
               String rfssName = configSetup.getName();
               Boolean newValue = (Boolean)evt.getNewValue();
//showln("propertyChange: rfssName="+rfssName);
//showln("propertyChange: newValue="+newValue);
               for( SuConfigSetup scs: scsList) {
                  if( scs.getHomeRfssName().equals(rfssName)) {
                     scs.setEmulated(newValue);
                  }
               }
               updateUI();
            }
         }
      });

      JTable table = new JTable(model);
      table.setRowHeight( TABLE_ROW_HEIGHT);
      setupTableHeaderFont( table);

      // setup table header column tooltip
      TableColumnHeaderToolTips tips = new TableColumnHeaderToolTips();
      for (int i=0; i < table.getColumnCount(); i++) {
         TableColumn col = table.getColumnModel().getColumn(i);
         tips.setToolTip(col, RfssConfigSetup.getColumnTips()[i]);
      }
      table.getTableHeader().addMouseMotionListener(tips);

      //ComboBoxEditor cbEditor = new ComboBoxEditor(rfssVec);
      //ComboBoxRenderer cbRenderer = new ComboBoxRenderer(rfssVec);
   
      TableColumn col;
      TableColumnModel cm = table.getColumnModel();
      cm.getColumn(RfssConfigSetup.COL_IP_ADDRESS).setPreferredWidth(140);  // 110
      cm.getColumn(RfssConfigSetup.COL_NAME).setPreferredWidth(120);        // 130
      cm.getColumn(RfssConfigSetup.COL_DOMAIN_NAME).setPreferredWidth(160); // 130

      // setup cell editor and renderer
      TextFieldEditor ipEditor = new TextFieldEditor(15, 
         FilteredDocument.IP_ADDRESS, "IpAddress in form of ddd.ddd.ddd.ddd");
      col = cm.getColumn(RfssConfigSetup.COL_IP_ADDRESS);
      col.setCellEditor( ipEditor);
      //===col.setCellRenderer( ipEditor);

      col = cm.getColumn(RfssConfigSetup.COL_PORT);
      col.setCellEditor(new TextFieldEditor(5,FilteredDocument.DIGIT));

      col = cm.getColumn(RfssConfigSetup.COL_NAME);
      TextFieldEditor nameEditor = new TextFieldEditor(24,FilteredDocument.ANY);
      nameEditor.addCellEditorListener( new RfssNameListener(nameEditor, rfssVec)); 
      col.setCellEditor( nameEditor);

      //col.setCellEditor( cbEditor);
      //col.setCellRenderer( cbRenderer);

      //TextFieldEditor tf2 = new TextFieldEditor(2,FilteredDocument.HEX_DIGIT);
      HexRenderer hexRenderer = new HexRenderer();
      HexEditor editor2 = new HexEditor(2);
      col = cm.getColumn(RfssConfigSetup.COL_ID);
      col.setCellEditor( editor2);
      col.setCellRenderer( hexRenderer);
      //col.setCellEditor(tf2);
      //col.setCellRenderer(tf2);

      //
      //TextFieldEditor tf3 = new TextFieldEditor(3,FilteredDocument.HEX_DIGIT);
      HexEditor editor3 = new HexEditor(3);
      col = cm.getColumn(RfssConfigSetup.COL_SYSTEM_ID);
      col.setCellEditor( editor3);
      col.setCellRenderer( hexRenderer);
      //col.setCellEditor(tf3);
      //col.setCellRenderer(tf3);
      
      //
      //TextFieldEditor tf5 = new TextFieldEditor(5,FilteredDocument.HEX_DIGIT);
      HexEditor editor5 = new HexEditor(5);
      col = cm.getColumn(RfssConfigSetup.COL_WACN_ID);
      col.setCellEditor( editor5);
      col.setCellRenderer( hexRenderer);
      //col.setCellEditor(tf5);
      //col.setCellRenderer(tf5);

      col = cm.getColumn(RfssConfigSetup.COL_DOMAIN_NAME);
      col.setCellEditor(new TextFieldEditor(18,FilteredDocument.ANY));

      return table;
   }

   public JTable buildSuTable( List<SuConfigSetup> scsList)
   {
      if( scsList.size() == 0)
         return null;

      // setup Su Config table
      SuConfigSetupTableModel model = new SuConfigSetupTableModel(mode, scsList);
      //+++JTable table = new JTable(model);
      // Disable cell editing
      JTable table = new JTable(model) {
         public boolean isCellEditable(int row, int column) {
            //showln("SU table: no cell editing...");
            if( column == SuConfigSetup.COL_ID)
               return true;
            return false;
         }
      };

      table.setRowHeight( TABLE_ROW_HEIGHT);
      setupTableHeaderFont( table);

      // setup table header column tooltip
      TableColumnHeaderToolTips tips = new TableColumnHeaderToolTips();
      for (int i=0; i < table.getColumnCount(); i++) {
        TableColumn col = table.getColumnModel().getColumn(i);
        tips.setToolTip(col, SuConfigSetup.getColumnTips()[i]);
      }
      table.getTableHeader().addMouseMotionListener(tips);

      TableColumn col;
      TableColumnModel cm = table.getColumnModel();
      //cm.getColumn(SuConfigSetup.COL_RADICAL_NAME).setPreferredWidth(130);

      // setup cell editor and renderer
      col = cm.getColumn(SuConfigSetup.COL_NAME);
      col.setCellEditor(new TextFieldEditor(24,FilteredDocument.ANY));

      HexRenderer hexRenderer = new HexRenderer();
      HexEditor editor6 = new HexEditor(6);
      col = cm.getColumn(SuConfigSetup.COL_ID);
      col.setCellEditor( editor6);
      col.setCellRenderer( hexRenderer);

      ComboBoxRenderer cbRenderer = new ComboBoxRenderer(rfssVec);
      ComboBoxEditor cbEditor = new ComboBoxEditor(rfssVec);
      col = cm.getColumn(SuConfigSetup.COL_HOME_RFSS_NAME);
      col.setCellEditor( cbEditor);
      col.setCellRenderer( cbRenderer);

      cbEditor = new ComboBoxEditor(rfssVec);
      cbRenderer = new ComboBoxRenderer(rfssVec);
      col = cm.getColumn(SuConfigSetup.COL_SERVING_RFSS_NAME);
      col.setCellEditor( cbEditor);
      col.setCellRenderer( cbRenderer);

      /*****
      // Home and Serving Rfss
      HexEditor editor2 = new HexEditor(2);
      col = cm.getColumn(SuConfigSetup.COL_HOME_RFSS_ID);
      col.setCellEditor( editor2);
      col.setCellRenderer( hexRenderer);

      HexEditor editor3 = new HexEditor(3);
      col = cm.getColumn(SuConfigSetup.COL_SYSTEM_ID);
      col.setCellEditor( editor3);
      col.setCellRenderer( hexRenderer);
      
      HexEditor editor5 = new HexEditor(5);
      col = cm.getColumn(SuConfigSetup.COL_WACN_ID);
      col.setCellEditor( editor5);
      col.setCellRenderer( hexRenderer);
       *****/

      return table;
   }

   public JTable buildGroupTable(List<GroupConfigSetup> gcsList)
   {
      if( gcsList.size() == 0)
         return null;

      // setup table
      GroupConfigSetupTableModel model = new GroupConfigSetupTableModel(gcsList);
      //+++JTable table = new JTable(model);
      // Disable cell editing
      JTable table = new JTable(model) {
         public boolean isCellEditable(int row, int column) {
            //showln("Group table: no cell editing...");
            return false;
         }
      };

      table.setRowHeight( TABLE_ROW_HEIGHT);
      setupTableHeaderFont( table);

      // setup table header column tooltip
      TableColumnHeaderToolTips tips = new TableColumnHeaderToolTips();
      for (int i=0; i < table.getColumnCount(); i++) {
        TableColumn col = table.getColumnModel().getColumn(i);
        tips.setToolTip(col, GroupConfigSetup.getColumnTips()[i]);
      }
      table.getTableHeader().addMouseMotionListener(tips);

      TableColumn col;
      TableColumnModel cm = table.getColumnModel();
      cm.getColumn(GroupConfigSetup.COL_SU_NAME_LIST).setPreferredWidth(260);

      ComboBoxEditor cbEditor = new ComboBoxEditor(rfssVec);
      ComboBoxRenderer cbRenderer = new ComboBoxRenderer(rfssVec);

      // setup cell editor and renderer
      col = cm.getColumn(GroupConfigSetup.COL_NAME);
      col.setCellEditor(new TextFieldEditor(24,FilteredDocument.ANY));

      HexRenderer hexRenderer = new HexRenderer();
      HexEditor editor6 = new HexEditor(4);
      col = cm.getColumn(GroupConfigSetup.COL_ID);
      col.setCellEditor( editor6);
      col.setCellRenderer( hexRenderer);

      // Home Rfss Name
      col = cm.getColumn(GroupConfigSetup.COL_HOME_RFSS_NAME);
      col.setCellEditor( cbEditor);
      col.setCellRenderer( cbRenderer);

      // Su Name List
      return table;
   }

   public void buildGUI()
   {
      setLayout( new GridBagLayout());

      // setup title
      JLabel fileLabel = new JLabel( title);
      JPanel filePanel = new JPanel();
      filePanel.add( fileLabel);

      // setup RFSS Config table
      rfssTable = buildRfssTable( epList);
      JScrollPane rfssScroll = new JScrollPane(rfssTable);
      rfssScroll.setPreferredSize( new Dimension(700, 160));
      rfssScroll.setBorder(BorderFactory.createTitledBorder("RFSS Configuration"));

      // setup GroupConfig table
      gcsTable = buildGroupTable( gcsList);
      JScrollPane gcsScroll = null;
      if( gcsTable != null) {
         gcsScroll = new JScrollPane( gcsTable);
         gcsScroll.setPreferredSize( new Dimension(700, 160));
         gcsScroll.setBorder(BorderFactory.createTitledBorder("Group Configuration"));
      }

      // setup SuConfig table
      scsTable = buildSuTable( scsList);
      JScrollPane scsScroll = null;
      if( scsTable != null) {
         scsScroll = new JScrollPane( scsTable);
         scsScroll.setPreferredSize( new Dimension(700, 200));
         scsScroll.setBorder(BorderFactory.createTitledBorder("Subscriber Configuration"));
      }

      GridBagConstraints2 c = new GridBagConstraints2();
      // x, y, w, h, anchor, fill, l, t, r, b

      // row-0
      c.set( 0, 0, 6, 1, "c", "horz", 2, 2, 2, 2);
      add( filePanel, c);

      // row-1
      if( rfssScroll != null) {
         c.set( 0, 1, 6, 4, "c", "horz", 2, 2, 2, 2);
         add( rfssScroll, c);
      }

      // row-2
      if( gcsScroll != null) {
         c.set( 0, 5, 6, 4, "c", "horz", 2, 2, 2, 2);
         add( gcsScroll, c);
      }

      // row-3
      if( scsScroll != null) {
         c.set( 0, 9, 6, 4, "c", "both", 2, 2, 2, 2);
         add( scsScroll, c);
      }
   }

   public void stopCellEditing() {
      if(rfssTable != null && rfssTable.getCellEditor() != null)
	 rfssTable.getCellEditor().stopCellEditing();
      if(gcsTable != null && gcsTable.getCellEditor() != null)
	 gcsTable.getCellEditor().stopCellEditing();
      if(scsTable != null && scsTable.getCellEditor() != null)
	 scsTable.getCellEditor().stopCellEditing();
   }

   public static TestConfigSetupPanel createTestPanel()
   {
      //String title = "C:/issi-data/Lab_Traces/subnet-test.pcap";
      String title = "ISSI Conformance Test Setup";
      int mode = RfssConfigSetup.MODE_SETUP;

      List<RfssConfigSetup> epList = new ArrayList<RfssConfigSetup>();
      List<SuConfigSetup> scsList = new ArrayList<SuConfigSetup>();
      List<GroupConfigSetup> gcsList = new ArrayList<GroupConfigSetup>();

      // Test-1
      epList.add( new RfssConfigSetup());
      epList.add( new RfssConfigSetup());

      scsList.add( new SuConfigSetup());
      scsList.add( new SuConfigSetup());
      scsList.add( new SuConfigSetup());

      // with or without group
      gcsList.add( new GroupConfigSetup());
      gcsList.add( new GroupConfigSetup());

      // Test-2
      //epList.add( new RfssConfigSetup("10.0.0.24",5060,"UHF","02.ad5.bee07.p25dr",false));
      //epList.add( new RfssConfigSetup("10.0.2.25",5060,"VHF","03.ad5.bee07.p25dr",false));
      //mode = RfssConfigSetupPanel.MODE_PLAYBACK;

      // Test-3
      //epList.add( new RfssConfigSetup("10.0.0.24",5060,"UHF","",false));
      //epList.add( new RfssConfigSetup("10.0.2.25",5060,"VHF","",false));
      //mode = RfssConfigSetup.MODE_PLAYBACK;

      TestConfigSetupPanel panel = new TestConfigSetupPanel( mode, title, epList,
            scsList, gcsList);
      return panel;
   }

   // RfssNameCellEditorListener
   //---------------------------------------------------------------
   class RfssNameListener implements CellEditorListener {
      TextFieldEditor editor;
      Vector<String> rfssVec;
      public RfssNameListener(TextFieldEditor editor, Vector<String> rfssVec) {
         this.editor = editor;
         this.rfssVec = rfssVec;
      }
      public void editingStopped( ChangeEvent evt) {
         Object value = editor.getCellEditorValue();
         showln("***** RfssConfigSetupPanel: editingStopped(): value="+value);
      }
      public void editingCanceled(ChangeEvent evt) {
         Object value = editor.getCellEditorValue();
         showln("RfssConfigSetupPanel: editingCanceled(): value="+value);
      }
   }
   //===============================================================
   public static void main(String args[]) 
   {
      JFrame frame = new JFrame("RfssConfigSetupPanel");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      TestConfigSetupPanel panel = TestConfigSetupPanel.createTestPanel();

      panel.setPreferredSize( new Dimension(700, 640));
      frame.getContentPane().add(panel);
      frame.setSize(740, 680);
      frame.setVisible(true);
   }
}
