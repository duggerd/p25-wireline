//
package gov.nist.p25.issi.setup;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import gov.nist.p25.common.swing.table.TableColumnHeaderToolTips;
import gov.nist.p25.common.swing.table.TextFieldEditor;
import gov.nist.p25.common.swing.widget.FilteredDocument;
import gov.nist.p25.common.swing.widget.GridBagConstraints2;

import gov.nist.p25.issi.constants.IvsDelaysConstants;
import gov.nist.p25.issi.message.XmlIvsDelays;
import gov.nist.p25.issi.setup.IvsDelaysSetup;
//import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays;

/**
 * IvsDelaysSetup panel.
 */
public class IvsDelaysSetupPanel extends JPanel 
   implements ActionListener
{
   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private static final int TABLE_ROW_HEIGHT = 18;

   private String title;
   private String xmlFile;
   private XmlIvsDelays xmldoc;
   private List<IvsDelaysSetup> gcsdList;
   private List<IvsDelaysSetup> gmtdList;
   private List<IvsDelaysSetup> ucsdList;
   private List<IvsDelaysSetup> umtdList;
   private JTable gcsdTable;
   private JTable gmtdTable;
   private JTable ucsdTable;
   private JTable umtdTable;
   private JButton okButton;
   private JButton cancelButton;

   // accessor
   public String getXmlFile() {
      return xmlFile;
   }

   // constructor
   public IvsDelaysSetupPanel(String title, String xmlFile)
      throws Exception
   {
      this.title = title;
      this.xmlFile = xmlFile;
      xmldoc = new XmlIvsDelays(xmlFile);
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
      // setup table
      IvsDelaysSetupTableModel model = new IvsDelaysSetupTableModel(delaysList);
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

      TableColumn col;
      TableColumnModel cm = table.getColumnModel();

      // setup cell editor and renderer
      col = cm.getColumn(IvsDelaysSetup.COL_PARAMETER_NAME);
      col.setPreferredWidth(80);
      col.setCellEditor(new TextFieldEditor(14,FilteredDocument.ANY));

      col = cm.getColumn(IvsDelaysSetup.COL_AVERAGE_TIME);
      col.setPreferredWidth(80);

      col = cm.getColumn(IvsDelaysSetup.COL_MAXIMUM_TIME);
      col.setPreferredWidth(80);
      return table;
   }

   public void buildGUI()
   {
      setLayout( new GridBagLayout());

      // setup title panel
      JLabel titleLabel = new JLabel( title);
      JPanel titlePanel = new JPanel();
      titlePanel.add( titleLabel);

      GridBagConstraints2 b = new GridBagConstraints2();
      GridBagConstraints2 c = new GridBagConstraints2();
      // x, y, w, h, anchor, fill, l, t, r, b

      // row-0
      c.set( 0, 0, 6, 1, "c", "horz", 4, 4, 4, 4);
      add( titlePanel, c);

      String gcsdTitle = IvsDelaysHelper.getIvsDelaysData(xmldoc,IvsDelaysConstants.TAG_IVS_GCSD,IvsDelaysConstants.KEY_TITLE);
      gcsdList = IvsDelaysHelper.getIvsDelaysSetupList(xmldoc,IvsDelaysConstants.TAG_IVS_GCSD);
      gcsdTable = buildTable( gcsdList);
      JScrollPane gcsdScroll = new JScrollPane( gcsdTable);
      gcsdScroll.setPreferredSize( new Dimension(300, 240));
      gcsdScroll.setBorder(BorderFactory.createTitledBorder(gcsdTitle));

      // row-1,0
      c.set( 0, 1, 3, 4, "c", "horz", 4, 4, 4, 4);
      add( gcsdScroll, c);

      String gmtdTitle = IvsDelaysHelper.getIvsDelaysData(xmldoc,IvsDelaysConstants.TAG_IVS_GMTD,IvsDelaysConstants.KEY_TITLE);
      gmtdList = IvsDelaysHelper.getIvsDelaysSetupList(xmldoc,IvsDelaysConstants.TAG_IVS_GMTD);
      gmtdTable = buildTable( gmtdList);
      JScrollPane gmtdScroll = new JScrollPane( gmtdTable);
      gmtdScroll.setPreferredSize( new Dimension(300, 180));
      gmtdScroll.setBorder(BorderFactory.createTitledBorder(gmtdTitle));

      // row-2,0
      c.set( 0, 5, 3, 3, "c", "horz", 4, 4, 4, 4);
      add( gmtdScroll, c);


      String ucsdTitle = IvsDelaysHelper.getIvsDelaysData(xmldoc,IvsDelaysConstants.TAG_IVS_UCSD,IvsDelaysConstants.KEY_TITLE);
      ucsdList = IvsDelaysHelper.getIvsDelaysSetupList(xmldoc,IvsDelaysConstants.TAG_IVS_UCSD);
      ucsdTable = buildTable( ucsdList);
      JScrollPane ucsdScroll = new JScrollPane( ucsdTable);
      ucsdScroll.setPreferredSize( new Dimension(300, 240));
      ucsdScroll.setBorder(BorderFactory.createTitledBorder(ucsdTitle));

      // row-1,1
      c.set( 3, 1, 3, 4, "c", "horz", 4, 4, 4, 4);
      add( ucsdScroll, c);

      String umtdTitle = IvsDelaysHelper.getIvsDelaysData(xmldoc,IvsDelaysConstants.TAG_IVS_UMTD,IvsDelaysConstants.KEY_TITLE);
      umtdList = IvsDelaysHelper.getIvsDelaysSetupList(xmldoc,IvsDelaysConstants.TAG_IVS_UMTD);
      umtdTable = buildTable( umtdList);
      JScrollPane umtdScroll = new JScrollPane( umtdTable);
      umtdScroll.setPreferredSize( new Dimension(300, 180));
      umtdScroll.setBorder(BorderFactory.createTitledBorder(umtdTitle));

      // row-2,1
      c.set( 3, 5, 3, 3, "c", "horz", 4, 4, 4, 4);
      add( umtdScroll, c);

      // button panel
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new GridBagLayout());
      okButton = new JButton( "OK");
      okButton.setPreferredSize( new Dimension(90, 24));
      okButton.addActionListener(this);
      b.set( 0, 0, 1, 1, "c", "horz", 2, 2, 2, 2);
      buttonPanel.add( okButton, b);

      cancelButton = new JButton( "Cancel");
      cancelButton.setPreferredSize( new Dimension(90, 24));
      cancelButton.addActionListener(this);
      b.set( 1, 0, 1, 1, "c", "horz", 2, 2, 2, 2);
      buttonPanel.add( cancelButton, b);

      // row-3,1
      c.set( 3, 8, 3, 1, "e", "none", 4, 4, 4, 4);
      add( buttonPanel, c);
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

   // implementation of ActionListener
   //-----------------------------------------------------------
   public void actionPerformed(ActionEvent evt)
   {
      Object src = evt.getSource();
      if( src == okButton) {

	 stopCellEditing();

         IvsDelaysHelper.reconcile(xmldoc,gcsdList,IvsDelaysConstants.TAG_IVS_GCSD);
         IvsDelaysHelper.reconcile(xmldoc,gmtdList,IvsDelaysConstants.TAG_IVS_GMTD);
         IvsDelaysHelper.reconcile(xmldoc,ucsdList,IvsDelaysConstants.TAG_IVS_UCSD);
         IvsDelaysHelper.reconcile(xmldoc,umtdList,IvsDelaysConstants.TAG_IVS_UMTD);
         showln(" reconcile(): xmlFile="+xmlFile);
	 try {
	    //xmldoc.saveIvsDelays("logs/ivsdelays-test.xml");
	    xmldoc.saveIvsDelays( xmlFile);
         } catch(IOException ex) { }
      }
      else if( src == cancelButton) {
         //showln(" Cancel Button.....No Op");
      }
   }

   //===============================================================
   public static void main(String args[]) throws Exception
   {
      String title = IvsDelaysConstants.IVS_PERFORMANCE_PARAMETERS_SETUP;
      String xmlFile = IvsDelaysConstants.USER_MEASUREMENTS_FILE;

      JFrame frame = new JFrame("IvsDelaysSetupPanel");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      IvsDelaysSetupPanel panel = new IvsDelaysSetupPanel(title, xmlFile);

      panel.setPreferredSize( new Dimension(640, 640));
      frame.getContentPane().add(panel);
      frame.setSize(680, 680);
      frame.setVisible(true);
   }
}
