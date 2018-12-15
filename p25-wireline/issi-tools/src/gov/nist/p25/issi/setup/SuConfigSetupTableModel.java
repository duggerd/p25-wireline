//
package gov.nist.p25.issi.setup;

import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

/**
 * SuConfigSetup Table Model
 */
@SuppressWarnings("unchecked")
public class SuConfigSetupTableModel extends AbstractTableModel {

   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private int mode;
   private List<SuConfigSetup> configList;
   
   // accessor
   public int getMode() {
      return mode;
   }
   public List<SuConfigSetup> getSuConfigSetupList() {
      return configList;
   }

   // constructor
   public SuConfigSetupTableModel() {
   }
   public SuConfigSetupTableModel(int mode, List<SuConfigSetup> configList) {
      this.mode = mode;
      this.configList = configList;
   }

   public SuConfigSetupTableModel setParameter(int mode, List<SuConfigSetup> configList) {
      this.mode = mode;
      this.configList = configList;
      return this;
   }

   public int getColumnCount() {
      return SuConfigSetup.getColumnNames().length;
   }

   public String getColumnName(int column) {
      return SuConfigSetup.getColumnNames()[column];
   }

   public int getRowCount() {
      return configList.size();
   }

   public Class getColumnClass(int column) {
      return SuConfigSetup.getColumnClasses()[column];
   }

   public Object getValueAt(int row, int column) {
      SuConfigSetup config = configList.get(row);
      if( config == null)
         return null;
      return config.getValue(column);
   }

   public void setValueAt(Object value, int row, int column) {
      //showln("SuConfigSetupTableModel: setValueAt: "+row+","+column+" --> "+value);
      SuConfigSetup config = configList.get(row);
      if( config == null)
         return;
      try {
         config.setValue( column, value);
         fireTableDataChanged();
      } catch(Exception ex) {
         String msg = "Error in setValueAt: "+row+","+column +
            "\nVaue=" + value +"\n\n" + ex.getMessage() +"\n";
         JOptionPane.showMessageDialog( null, msg,
            "Data Input Error",
            JOptionPane.ERROR_MESSAGE);
      }
   }

   public boolean isCellEditable(int row, int column) {
      return SuConfigSetup.getColumnEditable()[column];
   }
}
