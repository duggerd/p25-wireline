//
package gov.nist.p25.issi.analyzer.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

//===import gov.nist.p25.issi.analyzer.vo.EndPoint;
import gov.nist.p25.issi.packetmonitor.EndPoint;

/**
 * Trace Analyzer EndPoint panel.
 */
@SuppressWarnings("unchecked")
public class EndPointPanel extends JPanel 
{
   private static final long serialVersionUID = -1L;
   
   private String fileName;
   private List<EndPoint> endPointList;

   //public List<EndPoint> getEndPointList() {
   //   return endPointList;
   //}

   // constructor
   public EndPointPanel() 
   {
      this( "", new ArrayList<EndPoint>());
   } 
   public EndPointPanel(String fileName, List<EndPoint> endPointList)
   {
      this.fileName = fileName;
      this.endPointList = endPointList;
      buildGUI();
   }

   public void buildGUI()
   {
      TableModel model = new AbstractTableModel() {
         private static final long serialVersionUID = -1L;
         private List<EndPoint> endPointList;

         public AbstractTableModel setParameter( List<EndPoint> endPointList) {
            this.endPointList = endPointList;
            return this;
         }

         public int getColumnCount() {
            // skip the Emulated column
            return EndPoint.columnNames.length - 1;
         }

         public String getColumnName(int column) {
            return EndPoint.columnNames[column];
         }

         public int getRowCount() {
            return endPointList.size();
         }

         public Class getColumnClass(int column) {
            return EndPoint.columnClasses[column];
         }

         public Object getValueAt(int row, int column) {
            EndPoint endPoint = endPointList.get(row);
            if( endPoint == null)
               return null;

            switch( column) {
            case EndPoint.COL_ENABLED:
               return new Boolean(endPoint.getEnabled());
            case EndPoint.COL_HOST:
               return endPoint.getHost();
            case EndPoint.COL_PORT:
               return new Integer(endPoint.getPort());
            case EndPoint.COL_DOMAIN_NAME:
               return endPoint.getDomainName();
            case EndPoint.COL_NAME:
               return endPoint.getName();
            case EndPoint.COL_EMULATED:
               return new Boolean(endPoint.getEmulated());
            default:
               return null;
            }
         }

         public void setValueAt(Object value, int row, int column) {
            EndPoint endPoint = endPointList.get(row);
            if( endPoint == null)
               return;
            switch( column) {
            case EndPoint.COL_ENABLED:
               Boolean bValue = (Boolean)value;
               endPoint.setEnabled( bValue.booleanValue());
               break;
            case EndPoint.COL_HOST:
               endPoint.setHost( (String)value);
               break;
            case EndPoint.COL_PORT:
               Integer iValue = (Integer)value;
               endPoint.setPort( iValue.intValue());
               break;
            case EndPoint.COL_DOMAIN_NAME:
               endPoint.setDomainName( (String)value);
               break;
            case EndPoint.COL_NAME:
               endPoint.setName( (String)value);
               break;
            case EndPoint.COL_EMULATED:
               Boolean eValue = (Boolean)value;
               endPoint.setEmulated( eValue.booleanValue());
               break;
            }
         }

         public boolean isCellEditable(int row, int column) {
            switch( column) {
            case EndPoint.COL_ENABLED:
               return true;
            case EndPoint.COL_HOST:
            case EndPoint.COL_PORT:
               return false;
            case EndPoint.COL_DOMAIN_NAME:
            case EndPoint.COL_NAME:
               return true;
            case EndPoint.COL_EMULATED:
               return false;    // disabled
            default:
               return false;
            }
         }
      }.setParameter( endPointList);

      //--------------------------------------------------------
      setLayout( new BorderLayout());
      JTable table = new JTable(model) {
         private static final long serialVersionUID = -1L;

         public Component prepareRenderer(TableCellRenderer renderer,
                                         int rowIndex, int colIndex) {
            Component c = super.prepareRenderer(renderer, rowIndex, colIndex);
            if (c instanceof JComponent) {

               String tip = "";
               switch( colIndex) {
               case EndPoint.COL_ENABLED:
                  tip = "Set Enabled to include the EndPoint in the analysis.";
                  break;
               case EndPoint.COL_HOST:
                  tip = "IP Address of the EndPoint.";
                  break;
               case EndPoint.COL_PORT:
                  tip = "SIP Signalling Port of the EndPoint.";
                  break;
               case EndPoint.COL_DOMAIN_NAME:
                  tip = "DomainName <RfssId>.<SystemId>.<WacnId>.p25dr of the EndPoint.";
                  break;
               case EndPoint.COL_NAME:
                  tip = "RFSS Name of the EndPoint.";
                  break;
               case EndPoint.COL_EMULATED:
                  tip = "Set Emulated to designate the EndPoint is emulated.";
                  break;
               }
               JComponent jc = (JComponent)c;
               jc.setToolTipText( tip);
            }
            return c;
         }
      };
      
      // setup column header bold font
      JTableHeader header = table.getTableHeader();
      final Font boldFont = header.getFont().deriveFont(Font.BOLD);
      final TableCellRenderer headerRenderer = header.getDefaultRenderer();
      header.setDefaultRenderer( new TableCellRenderer() {
         public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
            Component comp = headerRenderer.getTableCellRendererComponent(
               table, value, isSelected, hasFocus, row, column );
            comp.setFont( boldFont);
            return comp;
         }
      });
   
      //TableColumnModel cm = table.getColumnModel();
      //cm.getColumn(EndPoint.COL_HOST).setPreferredWidth(50);
      //cm.getColumn(EndPoint.COL_DOMAIN_NAME).setPreferredWidth(100);

      JScrollPane scrollPane = new JScrollPane(table);
      scrollPane.setPreferredSize( new Dimension(600, 180));

      JLabel fileLabel = new JLabel("File:");
      JLabel fileNameLabel = new JLabel(fileName);

      JPanel filePanel = new JPanel();
      filePanel.add( fileLabel);
      filePanel.add( fileNameLabel);

      JLabel sizeLabel = new JLabel("     Size:");
      JLabel fileSizeLabel = new JLabel("");
      filePanel.add( sizeLabel);
      filePanel.add( fileSizeLabel);
      if( fileName != null && fileName.length() > 0) {
         try {
            long fileSize = new File(fileName).length();
            fileSizeLabel.setText(""+fileSize);
         } catch(Exception ex) { }
      }
      add( filePanel, BorderLayout.NORTH);
      add( scrollPane, BorderLayout.CENTER);
   }

   public static EndPointPanel createTestPanel()
   {
      String fileName = "C:/project/issi-emulator/subnet-test.pcap";
      List<EndPoint> epList = new ArrayList<EndPoint>();
      epList.add( new EndPoint("10.0.0.24",5060,"02.ad5.bee07.p25dr","UHF"));
      epList.add( new EndPoint("10.0.2.25",5060,"03.ad5.bee07.p25dr","VHF"));
      return  new EndPointPanel( fileName, epList);
   }

   //===============================================================
   public static void main(String args[]) 
   {
      JFrame frame = new JFrame("EndPointPanel");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      JPanel panel = EndPointPanel.createTestPanel();
      panel.setPreferredSize( new Dimension(500, 240));
      frame.getContentPane().add(panel);
      frame.setSize(540, 320);
      frame.setVisible(true);
   }
}

