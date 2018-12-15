//
package gov.nist.p25.issi.setup;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.xmlbeans.XmlException;

import gov.nist.p25.common.swing.popup.PopupCommandEvent;
import gov.nist.p25.common.swing.popup.PopupCommandListener;
import gov.nist.p25.common.swing.popup.PopupCommandMenu;
import gov.nist.p25.common.swing.popup.PopupCommandMouseListener;
import gov.nist.p25.common.swing.table.TableColumnHeaderToolTips;
import gov.nist.p25.common.swing.table.TextFieldEditor;
import gov.nist.p25.common.swing.table.XTableColumnModel;
import gov.nist.p25.common.swing.util.ImageUtility;
import gov.nist.p25.common.swing.widget.FilteredDocument;
import gov.nist.p25.common.swing.widget.GridBagConstraints2;
import gov.nist.p25.common.util.FileUtility;

import gov.nist.p25.issi.constants.IvsDelaysConstants;
import gov.nist.p25.issi.message.XmlIvsDelays;
import gov.nist.p25.issi.setup.IvsDelaysSetup;
//import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays;

/**
 * IvsDelaysSetup panel.
 */
public class IvsDelaysMeasurementPanel extends JPanel 
   implements PopupCommandListener
{
   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private static final int TABLE_WIDTH = 540;
   private static final int TABLE_ROW_HEIGHT = 18;

   private static final String TAG_SEPARATOR = "~Separator~";
   private static final String TAG_NEW_WINDOW = "New Window";
   private static final String TAG_SAVE_AS = "Save As";
   private static final String TAG_DIAGRAM = "Diagram";


   private static final String[] plist = {
      TAG_NEW_WINDOW,
      TAG_SAVE_AS,
      TAG_SEPARATOR,
      TAG_DIAGRAM,
   };

   private String xmlFile = "IvsDelays";
   private String title;
   private String type;
   private XmlIvsDelays xmldoc;
   private boolean isCalculated;
   private boolean isReadOnly;

   private List<IvsDelaysSetup> gcsdList;
   private List<IvsDelaysSetup> gmtdList;
   private List<IvsDelaysSetup> ucsdList;
   private List<IvsDelaysSetup> umtdList;
   private JTable gcsdTable;
   private JTable gmtdTable;
   private JTable ucsdTable;
   private JTable umtdTable;
   private JFrame diagramFrame;
   private PopupCommandMenu popupMenu;

   // accessor
   public String getXmlFile() {
      return xmlFile;
   }
   public String getTitle() {
      return title;
   }
   public String getType() {
      return type;
   }
   public XmlIvsDelays getXmlIvsDelays() {
      return xmldoc;
   }

   // constructor
   /*** not used yet
   public IvsDelaysMeasurementPanel(String xmlFile, String title, String type)
      throws XmlException, IOException
   {
      this.xmlFile = xmlFile;
      this.title = title;
      this.type = type;
      this.xmldoc = new XmlIvsDelays(xmlFile);
      buildGUI();
   }
    ***/
   public IvsDelaysMeasurementPanel(XmlIvsDelays xmldoc, String title, String type)
      throws XmlException, IOException
   {
      this( xmldoc, title, type, false, false);
   }
   public IvsDelaysMeasurementPanel(XmlIvsDelays xmldoc, String title, String type,
      boolean isCalculated, boolean isReadOnly) throws XmlException, IOException
   {
      this.title = title;
      this.type = type;
      this.xmldoc = xmldoc;
      this.isCalculated = isCalculated;
      this.isReadOnly = isReadOnly;
      buildGUI();
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
               table, value, isSelected, hasFocus, row, column);
            c.setFont( boldFont);
            return c;
         }
      });
   }

   public JTable buildTable(List<IvsDelaysSetup> delaysList)
   {
      // setup table model 
      IvsDelaysSetupTableModel model = new IvsDelaysSetupTableModel(delaysList,isReadOnly);
      JTable table = new JTable(model);
      table.setRowHeight( TABLE_ROW_HEIGHT);
      setupTableHeaderFont( table);

      // setup table header column tooltip
      TableColumnHeaderToolTips tips = new TableColumnHeaderToolTips();
      for (int i=0; i < table.getColumnCount(); i++) {
        TableColumn col = table.getColumnModel().getColumn(i);
        tips.setToolTip(col, IvsDelaysSetup.getColumnTips()[i]);
      }
      table.getTableHeader().addMouseMotionListener(tips);

      // hide table column
      XTableColumnModel xcm = new XTableColumnModel();
      table.setColumnModel( xcm);
      table.createDefaultColumnsFromModel();

      TableColumnModel cm = table.getColumnModel();
      TableColumn col;

      // setup cell editor and renderer
      col = cm.getColumn(IvsDelaysSetup.COL_PARAMETER_NAME);
      col.setPreferredWidth(80);
      col.setCellEditor(new TextFieldEditor(14,FilteredDocument.ANY));

      col = cm.getColumn(IvsDelaysSetup.COL_AVERAGE_TIME);
      col.setPreferredWidth(80);

      col = cm.getColumn(IvsDelaysSetup.COL_MAXIMUM_TIME);
      col.setPreferredWidth(80);

      // hide column
      col = xcm.getColumnByModelIndex(IvsDelaysSetup.COL_CALCULATED);
      xcm.setColumnVisible( col, isCalculated);

      return table;
   }

   public void buildGUI() throws XmlException, IOException
   {
      removeAll();
      setLayout( new GridBagLayout());

      // setup title panel
      JLabel titleLabel = new JLabel( title);
      JPanel titlePanel = new JPanel();
      titlePanel.add( titleLabel);

      //GridBagConstraints2 b = new GridBagConstraints2();
      GridBagConstraints2 c = new GridBagConstraints2();
      // x, y, w, h, anchor, fill, l, t, r, b

      // row-0
      c.set( 0, 0, 6, 1, "c", "horz", 4, 4, 4, 4);
      add( titlePanel, c);

      // row-1,0
      c.set( 0, 1, 6, 8, "c", "horz", 4, 4, 4, 4);

      // setup table based on type
      if( IvsDelaysConstants.TAG_IVS_GCSD.equals(type)) {
         String gcsdTitle = IvsDelaysHelper.getIvsDelaysData(xmldoc,type,IvsDelaysConstants.KEY_TITLE);
         gcsdList = IvsDelaysHelper.getIvsDelaysSetupList(xmldoc,IvsDelaysConstants.TAG_IVS_GCSD);
         gcsdTable = buildTable( gcsdList);
         JScrollPane gcsdScroll = new JScrollPane( gcsdTable);
         gcsdScroll.setPreferredSize( new Dimension(TABLE_WIDTH, 500));
         gcsdScroll.setBorder(BorderFactory.createTitledBorder(gcsdTitle));
         add( gcsdScroll, c);
      }
      else if( IvsDelaysConstants.TAG_IVS_GMTD.equals(type)) {
  
         String gmtdTitle = IvsDelaysHelper.getIvsDelaysData(xmldoc,IvsDelaysConstants.TAG_IVS_GMTD,IvsDelaysConstants.KEY_TITLE);
         gmtdList = IvsDelaysHelper.getIvsDelaysSetupList(xmldoc,IvsDelaysConstants.TAG_IVS_GMTD);
         gmtdTable = buildTable( gmtdList);
         JScrollPane gmtdScroll = new JScrollPane( gmtdTable);
         gmtdScroll.setPreferredSize( new Dimension(TABLE_WIDTH, 220));
         gmtdScroll.setBorder(BorderFactory.createTitledBorder(gmtdTitle));
         add( gmtdScroll, c);

      }
      else if( IvsDelaysConstants.TAG_IVS_UCSD.equals(type)) {

         String ucsdTitle = IvsDelaysHelper.getIvsDelaysData(xmldoc,IvsDelaysConstants.TAG_IVS_UCSD,IvsDelaysConstants.KEY_TITLE);
         ucsdList = IvsDelaysHelper.getIvsDelaysSetupList(xmldoc,IvsDelaysConstants.TAG_IVS_UCSD);
         ucsdTable = buildTable( ucsdList);
         JScrollPane ucsdScroll = new JScrollPane( ucsdTable);
         ucsdScroll.setPreferredSize( new Dimension(TABLE_WIDTH, 560));
         ucsdScroll.setBorder(BorderFactory.createTitledBorder(ucsdTitle));
         add( ucsdScroll, c);
      }
      else if( IvsDelaysConstants.TAG_IVS_UMTD.equals(type)) {

         String umtdTitle = IvsDelaysHelper.getIvsDelaysData(xmldoc,IvsDelaysConstants.TAG_IVS_UMTD,IvsDelaysConstants.KEY_TITLE);
         umtdList = IvsDelaysHelper.getIvsDelaysSetupList(xmldoc,IvsDelaysConstants.TAG_IVS_UMTD);
         umtdTable = buildTable( umtdList);
         JScrollPane umtdScroll = new JScrollPane( umtdTable);
         umtdScroll.setPreferredSize( new Dimension(TABLE_WIDTH, 440));
         umtdScroll.setBorder(BorderFactory.createTitledBorder(umtdTitle));
         add( umtdScroll, c);
      }

      // optional comment textarea
      // row-2,0
      //String comment = IvsDelaysHelper.getIvsStatusComment(xmldoc);
      String comment = xmldoc.getIvsStatusComment();
      if( !"".equals(comment)) {
         JTextArea status = new JTextArea(comment);
	 JScrollPane textScroll = new JScrollPane( status);
         textScroll.setPreferredSize( new Dimension(TABLE_WIDTH, 120));
         textScroll.setBorder(BorderFactory.createTitledBorder("Comment"));
         c.set( 0, 9, 6, 2, "c", "horz", 4, 4, 4, 4);
	 add( textScroll, c);
      }

      // build optional popupmenu
      PopupCommandListener listener = this;
      if( plist != null)
      {
         popupMenu = new PopupCommandMenu(this,plist);
         if( listener != null)
            popupMenu.addPopupCommandListener( listener);

         PopupCommandMouseListener ml =
            new PopupCommandMouseListener(this,popupMenu);

         if( gcsdTable != null)
            gcsdTable.addMouseListener( ml);
         if( gmtdTable != null)
            gmtdTable.addMouseListener( ml);
         if( ucsdTable != null)
            ucsdTable.addMouseListener( ml);
         if( umtdTable != null)
            umtdTable.addMouseListener( ml);
         addMouseListener( ml);
      }
   }

   public void stopCellEditing() {
      if(gcsdTable != null && gcsdTable.getCellEditor() != null)
         gcsdTable.getCellEditor().stopCellEditing();
      if(gmtdTable != null && gmtdTable.getCellEditor() != null)
         gmtdTable.getCellEditor().stopCellEditing();
      if(ucsdTable != null && ucsdTable.getCellEditor() != null)
         ucsdTable.getCellEditor().stopCellEditing();
      if(umtdTable != null && umtdTable.getCellEditor() != null)
         umtdTable.getCellEditor().stopCellEditing();
   }

   public void showDiagram(String diagram, String subtitle)
   {
      if( diagramFrame == null) {
         diagramFrame = new JFrame( subtitle);
         JLabel iconLabel = new JLabel();
         iconLabel.setIcon( new ImageIcon( diagram));
         JScrollPane diagramScroll = new JScrollPane( iconLabel);
         diagramFrame.getContentPane().add(diagramScroll);
         diagramFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
         diagramFrame.pack();
      }
      diagramFrame.setVisible(true);
   }

   // implementation of PopupCommandListener
   //-----------------------------------------------------------
   public void processPopupCommand( PopupCommandEvent evt)
   {
      Object obj = evt.getSource();
      if( obj instanceof JMenuItem )
      {
         JMenuItem item = (JMenuItem)obj;
         String cmd = item.getActionCommand();
         //showln( "CMD="+cmd);

         //-----------------------------------
	 JFrame frame;
         if( TAG_NEW_WINDOW.equals(cmd)) {
            try {
               frame = createNewWindow(getXmlIvsDelays(), getTitle(), getType()); 
	       frame.setVisible(true);
	    } catch(Exception ex) { }
         }
	 else if( TAG_SAVE_AS.equals(cmd)) {
            String imgFile = System.getProperty("user.dir") +
                  File.separator + xmlFile + ".jpg";
            ImageUtility.doSaveAs( this, imgFile, "jpg");
         }
         else if( TAG_DIAGRAM.equals(cmd)) {
            String subtitle = IvsDelaysHelper.getIvsDelaysData(xmldoc,type,
               IvsDelaysConstants.KEY_TITLE);
            String diagram = IvsDelaysHelper.getIvsDelaysData(xmldoc,type,
               IvsDelaysConstants.KEY_DIAGRAM);
            showDiagram( diagram, subtitle);
         }
      }
   }

   public static JFrame createNewWindow(XmlIvsDelays xmldoc, String title, 
      String type) throws Exception
   {
      JFrame frame = new JFrame("IvsDelays Measurement Panel");
      //===frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      IvsDelaysMeasurementPanel panel = 
         new IvsDelaysMeasurementPanel(xmldoc,title,type);

      panel.setPreferredSize( new Dimension(640, 780));
      frame.getContentPane().add(panel);
      frame.setSize(680, 800);
      frame.setVisible(true);
      return frame;
   }   

   public void autoSave() {
      String imgDir = System.getProperty("user.dir") + File.separator +
             "output" + File.separator + "pcap-test";
      FileUtility.makeDir( imgDir);
      String imgFile = imgDir + File.separator + xmlFile + ".jpg";
      showln("IvsDelaysMeasurementPanel: autoSave()-imgFile="+imgFile);
      try {
         ImageUtility.saveComponentAsImage( this, imgFile, "jpg");
      } catch(Exception ex) {
         showln("autoSave(): "+ex);
      }
   }

   //===============================================================
   public static void main(String args[]) throws Exception
   {
      String xmlFile = "xml/issi-spec-ivs-umtd.xml";
      String title = "ISSI IVS Performance Recommendations";
      String type = IvsDelaysConstants.TAG_IVS_UMTD;
      XmlIvsDelays xmldoc = new XmlIvsDelays(xmlFile);

      //JFrame frame = IvsDelaysMeasurementPanel.createNewWindow(xmlFile,
      //      title,type); 
      //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      //===frame.setVisible(true);
      //
      JFrame frame = new JFrame("IvsDelays Measurement Panel");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      IvsDelaysMeasurementPanel panel = 
         new IvsDelaysMeasurementPanel(xmldoc,title,type);

      panel.setPreferredSize( new Dimension(640, 780));
      frame.getContentPane().add(panel);
      frame.setSize(680, 800);
      frame.setVisible(true);
   }
}
