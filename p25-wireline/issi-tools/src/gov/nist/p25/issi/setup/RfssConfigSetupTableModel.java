//
package gov.nist.p25.issi.setup;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
//import javax.swing.JFrame;
//import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

import gov.nist.p25.common.swing.widget.MessageBox;

/**
 * RfssConfigSetup Table Model
 */
@SuppressWarnings("unchecked")
public class RfssConfigSetupTableModel extends AbstractTableModel {

   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private int mode;
   private List<RfssConfigSetup> rfssList;

   // constructor
   public RfssConfigSetupTableModel() {
   }
   public RfssConfigSetupTableModel(int mode, List<RfssConfigSetup> rfssList) {
      this.mode = mode;
      this.rfssList = rfssList;
   }

   public RfssConfigSetupTableModel setParameter(int mode, List<RfssConfigSetup> rfssList) {
      this.mode = mode;
      this.rfssList = rfssList;
      return this;
   }

   public int getColumnCount() {
      // remove Enabled column
      return RfssConfigSetup.getColumnNames().length - 1;
   }

   public String getColumnName(int column) {
      return RfssConfigSetup.getColumnNames()[column];
   }

   public int getRowCount() {
      return rfssList.size();
   }

   public Class getColumnClass(int column) {
      return RfssConfigSetup.getColumnClasses()[column];
   }

   public Object getValueAt(int row, int column) {
      RfssConfigSetup endPoint = rfssList.get(row);
      if( endPoint == null)
         return null;
      return endPoint.getValue(column);
   }

   public void setValueAt(Object value, int row, int column) {
//showln("RfssConfigSetupTableModel: setValueAt: "+row+","+column+" --> "+value);
      RfssConfigSetup endPoint = rfssList.get(row);
      if( endPoint == null)
         return;

      Object oldObj = null;
      if(column == RfssConfigSetup.COL_NAME) {
         //NOTE: this is rfssName
         oldObj = (String)endPoint.getValue( column);
      }
      if(column == RfssConfigSetup.COL_EMULATED) {
         //NOTE: this is RfssConfigSetup
         oldObj = endPoint;
      }

      try {
         endPoint.setValue( column, value);
         if(column == RfssConfigSetup.COL_NAME) {
             pcs.firePropertyChange("RfssName", oldObj, value);
//showln("RfssConfigSetupTableModel: fireProperty: RfssName....");
         }
         // so we can set the SU emulated 
         if(column == RfssConfigSetup.COL_EMULATED) {
             pcs.firePropertyChange("RfssEmulated", oldObj, value);
//showln("RfssConfigSetupTableModel: fireProperty: RfssEmulated....");
         }
         fireTableDataChanged();

      } catch(Exception ex) {
         String msg = "Error in setValueAt: "+row+","+column +
            "\nVaue=" + value +"\n\n" + ex.getMessage() +"\n";
         MessageBox.doOk( 200, 200, msg);
         /***
         showln(msg);
         JOptionPane.showMessageDialog( null, msg,
            "Data Input Error",
            JOptionPane.ERROR_MESSAGE);
         throw new RuntimeException(ex.getMessage());
          ***/
      }
   }

   public boolean isCellEditable(int row, int column) {
      if( mode == RfssConfigSetup.MODE_SETUP) {
         // all columns editable
         return true;
      }
      // assume playback mode
      return RfssConfigSetup.getColumnEditable()[column];
   }

   // PropertyChangeSupport 
   //-------------------------------------------------------------
   private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
   public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
      pcs.addPropertyChangeListener(listener);
   }
   public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
      pcs.removePropertyChangeListener(listener);
   }
}
