//
package gov.nist.p25.common.swing.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Hex Editor
 */
public class HexEditor extends HexTextField 
   implements TableCellEditor, TableCellRenderer, ActionListener {

   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private Vector<CellEditorListener> editorListeners;
   private int thevalue;
   private int oldvalue;

   public HexEditor(int maxLength) {
      super( maxLength);
      editorListeners = new Vector<CellEditorListener>();
      addActionListener(this);
   }

   // implementation of ActionListener
   public void actionPerformed(ActionEvent e) {

      //showln("HexEditor: actionPerformed() text="+getText());
      try {
         if(getText().indexOf("x") >= 0) {
            // 0x0fad format
            thevalue = Integer.parseInt( getText().substring(getText().indexOf("x")+1),16);
	 } else {
            // 0fad
            thevalue = Integer.parseInt( getText(),16);
         }
      } catch(Exception ex) {
         thevalue = oldvalue;
         //showln("HexEditor: ex="+ex);
      }
      xfireEditingStopped();
   }

   private void xfireEditingStopped() {
      //showln("HexEditor: fireEditingStopped()...");
      for(int i=0; i < editorListeners.size(); ++i) {
         //showln("HexEditor: actionPerformed() stopEditing: i="+i);
         ((CellEditorListener)editorListeners.elementAt(i)).editingStopped(
	    new ChangeEvent(this));
      }
   }

   // implementation of TableCellRenderer
   public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {

      //showln("HexEditor: Setting value: "+value);
      if( value instanceof Integer) {
         setText(Integer.toHexString((Integer)value));
      }
      else if( value instanceof Long) {
        setText("0x"+Long.toHexString((Long)value));
      }
      else if( value instanceof String) {
         try {
            int ival = Integer.parseInt((String)value);
            setText(Integer.toHexString(ival));
         } catch(Exception ex) { }
      }
      return this;
   }

   // implementation of TabeCellEditor
   //----------------------------------------------------------------------
   public Component getTableCellEditorComponent(JTable table, Object value,
      boolean isSelected, int row, int column) {
      if( value instanceof Integer) {
         //showln("HexEditor: getTableCell: Integer="+value);
         oldvalue = thevalue;
         thevalue = ((Integer)value).intValue();
         setText(Integer.toHexString(thevalue));
      }
      else if( value instanceof Long) {
         oldvalue = thevalue;
         thevalue = ((Long)value).intValue();
         setText("0x"+Long.toHexString((Long)value));
      }
      else if( value instanceof String) {
         //showln("HexEditor: getTableCell: String="+value);
         try {
            int ival = Integer.parseInt((String)value);
            setText(Integer.toHexString(ival));
         } catch(Exception ex) { }
      }
      return this;
   }

   public boolean isCellEditable(EventObject anEvent) {
      return true;
   }
   public boolean shouldSelectCell(EventObject anEvent) {
      return true;
   }
   public Object getCellEditorValue() {
      //showln("HexEditor: getCellEditorValue(): "+getText());
      return thevalue;
   }
   public boolean stopCellEditing() {
      //showln("HexEditor: stopCellEditing(): text="+getText());
      actionPerformed(new ActionEvent(this, 1, "stopCellEditing"));
      return true;
   }
   public void cancelCellEditing() {
      //showln("HexEditor: cancelCellEditing(): ...");
   }

   //--------------------------------------------------------------
   public void addCellEditorListener(CellEditorListener l) {
      editorListeners.add(l);
   }
   public void removeCellEditorListener(CellEditorListener l) {
      editorListeners.remove(l);
   }
}
