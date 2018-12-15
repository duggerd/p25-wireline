//
package gov.nist.p25.issi.setup;

import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

/**
 * GroupConfigSetup Table Model
 */
@SuppressWarnings("unchecked")
public class GroupConfigSetupTableModel extends AbstractTableModel {

   private static final long serialVersionUID = -1L;
   //public static void showln(String s) { System.out.println(s); }

   private List<GroupConfigSetup> configList;

   // constructor
   public GroupConfigSetupTableModel() {
   }
   public GroupConfigSetupTableModel(List<GroupConfigSetup> configList) {
      this.configList = configList;
   }
   public GroupConfigSetupTableModel setParameter(List<GroupConfigSetup> configList) {
      this.configList = configList;
      return this;
   }

   public int getColumnCount() {
      return GroupConfigSetup.getColumnNames().length;
   }

   public String getColumnName(int column) {
      return GroupConfigSetup.getColumnNames()[column];
   }

   public int getRowCount() {
      return configList.size();
   }

   public Class getColumnClass(int column) {
      return GroupConfigSetup.getColumnClasses()[column];
   }

   public Object getValueAt(int row, int column) {
      GroupConfigSetup config = configList.get(row);
      if( config == null)
         return null;
      return config.getValue(column);
   }

   public void setValueAt(Object value, int row, int column) {
      //showln("GroupConfigSetupTableModel: setValueAt: "+row+","+column+ " --> "+value);
      GroupConfigSetup config = configList.get(row);
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
      return GroupConfigSetup.getColumnEditable()[column];
   }
}
