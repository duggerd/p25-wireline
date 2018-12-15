//
package gov.nist.p25.issi.setup;

import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

/**
 * IvsDelaysSetup Table Model
 */
@SuppressWarnings("unchecked")
public class IvsDelaysSetupTableModel extends AbstractTableModel {

   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private List<IvsDelaysSetup> configList;
   private boolean isReadOnly;

   // constructor
   public IvsDelaysSetupTableModel() {
   }
   public IvsDelaysSetupTableModel(List<IvsDelaysSetup> configList) {
      this( configList, false);
   }
   public IvsDelaysSetupTableModel(List<IvsDelaysSetup> configList, boolean isReadOnly) {
      this.configList = configList;
      this.isReadOnly = isReadOnly;
   }
   /***
   public IvsDelaysSetupTableModel setParameter(List<IvsDelaysSetup> configList) {
      this.configList = configList;
      return this;
   }
    ***/

   public int getColumnCount() {
      return IvsDelaysSetup.getColumnNames().length;
   }
   public String getColumnName(int column) {
      return IvsDelaysSetup.getColumnNames()[column];
   }

   public int getRowCount() {
      return configList.size();
   }

   public Class getColumnClass(int column) {
      return IvsDelaysSetup.getColumnClasses()[column];
   }

   public boolean isCellEditable(int row, int column) {
      //showln("IvsDelaysSetupTableModel: isReadOnly="+isReadOnly);
      if( isReadOnly) return false;
      return IvsDelaysSetup.getColumnEditable()[column];
   }

   public Object getValueAt(int row, int column) {
      IvsDelaysSetup config = configList.get(row);
      if( config == null)
         return null;
      return config.getValue(column);
   }

   public void setValueAt(Object value, int row, int column) {
      //showln("IvsDelaysSetupTableModel: setValueAt: "+row+","+column+ " --> "+value);
      IvsDelaysSetup config = configList.get(row);
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
}
