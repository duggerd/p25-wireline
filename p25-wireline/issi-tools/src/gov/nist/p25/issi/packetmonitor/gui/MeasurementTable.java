//
package gov.nist.p25.issi.packetmonitor.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.constants.ISSILogoConstants;
import gov.nist.p25.issi.rfss.SipUtils;

/**
 * MeasurementTable - popup table dialog
 * called in ControlPanel by "Evaluate Trace" button
 */
public class MeasurementTable extends JDialog 
     implements ActionListener {
   
   private static final long serialVersionUID = -1L;   
   private static String[] title = {"Parameter Name", "Measured Time(ms)"};

   private String tableString = "";    
   private String callType = "Measurement Table";
   private JButton save, exit, diagram;   
   private JFrame parent;
   private JFrame pic;   
   private JPanel buttonPanel;   
   private JTable parameterTable;   
   private DefaultTableModel parameterTableModel;   
   private LinkedHashMap<String, Long> parameterList = new LinkedHashMap<String, Long> ();

   public MeasurementTable(JFrame parent) {
      super();
      this.parent = parent;
      initGUI();
   }
   
   public void initGUI() {
      JScrollPane scrollpane = new JScrollPane(getParameterTable());
      addRowsToParameterTable();
      
      if(buttonPanel == null)
         constructButtonPanel();

      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      setIconImage(new ImageIcon(ISSILogoConstants.ISSI_TESTER_LOGO).getImage());
      setTitle(callType);
      
      Rectangle rectangle = getBounds();
      int width = parent.getWidth() / 2;
      int height = parent.getHeight() / 2;
      rectangle.width = 300;
      if(parameterTable.getRowCount() == 0)
         rectangle.height = 300;
      else
         rectangle.height = parameterTable.getRowCount()*parameterTable.getRowHeight()+100;

      setBounds(rectangle);
      setLocation(parent.getX() + width, parent.getY() + height);
      setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
      add(scrollpane);
      add(buttonPanel);
      setVisible(false);      
   }
   
   public void showTable() throws Exception {
      if(this != null) {
         // set table title
         if( SipUtils.isGroup(callType)) {
            setTitle("IVS Group Call Setup Delay");
         } 
	 else if( SipUtils.isSubscriberUnit(callType)) {
            setTitle("IVS SU-to-SU Call Setup Delay");
         }
         addRowsToParameterTable();

         // resize the table height
         Rectangle rectangle = getBounds();
         rectangle.width = 300;
         if(parameterTable.getRowCount() == 0) {
            rectangle.height = 300;
	 } else {
            rectangle.height = parameterTable.getRowCount()*parameterTable.getRowHeight()+100;
         }
         setBounds(rectangle);
         
         setVisible(true);
         if(pic != null) {
            pic.setVisible(true);
         }
         return;
      }
      initGUI();
      return;
   }
   
   public void hideTable() {
      if(this != null) {
         setVisible(false);
         if(pic != null) {
            pic.setVisible(false);
         }
      }
   }
   
   /**
    * This method initializes jTable   
    *    
    * @return javax.swing.JTable   
    */
   private JTable getParameterTable() {
      if (parameterTable == null) {
         parameterTableModel = new DefaultTableModel(title, 0);
         parameterTable = new JTable(parameterTableModel) {
            private static final long serialVersionUID = -1L;
            public boolean isCellEditable(int row, int column) {
               return false;
            }
         };

         Border border = BorderFactory.createLineBorder(Color.black);
         parameterTable.setBorder(border);
         parameterTable.setShowVerticalLines(true);
         parameterTable.setName("");
         parameterTable.setShowHorizontalLines(true);
         parameterTable.setDefaultRenderer(Object.class, new ColorTableCellRenderer());
      }      
      return parameterTable;
   }
   
   private void constructButtonPanel() {
      buttonPanel = new JPanel();
      save = new JButton("Save");
      exit = new JButton("Exit");
      diagram = new JButton("Diagram");
      
      save.addActionListener(this);
      exit.addActionListener(this);
      diagram.addActionListener(this);
      
      buttonPanel.add(save);
      buttonPanel.add(diagram);
      buttonPanel.add(exit);
   }

   public void addRowsToParameterTable() {

      // clear the table first
      while(parameterTableModel.getRowCount() > 0) {
         parameterTableModel.removeRow(0);
      }
      // refill the table
      if(parameterList != null && !parameterList.isEmpty()) {
         String[] newRow = new String[2];
         // add to table
         for(Map.Entry<String, Long> parameter : parameterList.entrySet()) {
            newRow[0] = parameter.getKey();
            newRow[1] = parameter.getValue().toString();
            parameterTableModel.addRow(newRow);
         }      
      }      
      return;
   }
   
   private void reset() {
      // clear all existing components
      pic = null;
      tableString = "";
      parameterList.clear();
   }
   
   // called by ControlPanel.java, TraceAnalyzerGUI.java
   public void setData(String result) {
      reset();

      //extract message content
      String [] lines = result.split("\n");
      String temp = "";
      String [] words;
      for(int i=0; i<lines.length; i++) {
         temp = lines[i].trim();
         // record result for saving
         if(i>0 && i<lines.length-1)
            tableString += temp+"\n";
         
         words = temp.split("\"");
         if(temp.startsWith("<call ")) {
            // get the call type, group call or su-to-su
            callType = words[1];
         } else if(temp.startsWith("<param ")) {
            // add to parameterList
            parameterList.put(words[1], new Long(words[3]));
         }
      }
   }
   /***/
   
   class ColorTableCellRenderer extends DefaultTableCellRenderer 
         implements TableCellRenderer {
      private static final long serialVersionUID = -1L;
      public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, 
            int row, int column) {
         super.setHorizontalAlignment(JLabel.CENTER);
         String s = table.getModel().getValueAt(row,column).toString();
         if(s.equalsIgnoreCase("Not applicable")) {
            setForeground(Color.GRAY);
         } else {
            setForeground(null);
         }
         return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }
   }

   // implementation of ActionListener
   //-----------------------------------------------------------------
   public void actionPerformed(ActionEvent ae) {

      Object src = ae.getSource();
      if( src == save) {
         boolean status = saveFile();
         if (!status) {
            JOptionPane.showMessageDialog (this,
               "IO error in saving file!!", 
               "File Save Error",
               JOptionPane.ERROR_MESSAGE);
         }
      }
      else if( src == diagram) {
         showDiagram();
      }
      else if( src == exit) {
         dispose();
      }
   }
   
   private boolean saveFile () {
      File file = new File("result.xml");
      JFileChooser fc = new JFileChooser();
      // Start in current directory
      fc.setCurrentDirectory (new File("."));

      // Set to a default name for save.
      fc.setSelectedFile(file);

      // Open chooser dialog
      int result = fc.showSaveDialog(this);
      if (result == JFileChooser.CANCEL_OPTION) {
         return true;
      } else if (result == JFileChooser.APPROVE_OPTION) {
         file = fc.getSelectedFile();
         if (file.exists()) {
            int response = JOptionPane.showConfirmDialog (this,
               "Overwrite existing file ?",
               "Confirm Overwrite",
               JOptionPane.OK_CANCEL_OPTION,
               JOptionPane.QUESTION_MESSAGE);
            if (response == JOptionPane.CANCEL_OPTION) 
               return false;
         }
         // write parameter table into a file
         //return writeFile(file, tableString);
	 try {
            FileUtility.saveToFile(file.getAbsolutePath(), tableString);
            return true;
         } catch(Exception ex) { }
      }
      return false;
   }
   
   private void showDiagram() {
      if(pic != null) {
        pic.setVisible(true);
        return;
      }
        
      pic = new JFrame();        
      JLabel label = new JLabel();
      if( SipUtils.isGroup(callType)) {
         label.setIcon(new ImageIcon("gfx/diagrams/IVS-GCSD.JPG"));
         pic.setTitle("IVS Group Call Setup Diagram");
      }
      else if( SipUtils.isSubscriberUnit(callType)) {
         label.setIcon(new ImageIcon("gfx/diagrams/IVS-UCSD.JPG"));
         pic.setTitle("IVS SU-to-SU Call Setup Diagram");
      }
      else {
         pic = null;
         return;
      }
            
      JScrollPane scrollpane = new JScrollPane(label);        
      pic.getContentPane().add(scrollpane);
      pic.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      pic.setIconImage(new ImageIcon("gfx/nist.gif").getImage());
      pic.pack();
      pic.setVisible(true);
   }
     
   // dispose the diagram 
   public void dispose() {
      if(pic != null) {
         pic.dispose();
         pic = null;
      }
      super.dispose();
   }
     
}
