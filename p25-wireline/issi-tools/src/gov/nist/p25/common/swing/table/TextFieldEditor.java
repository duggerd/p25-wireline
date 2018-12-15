//
package gov.nist.p25.common.swing.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import gov.nist.p25.common.swing.widget.FilteredDocument;

/**
 * General purpose TextField Editor
 * Usage:
 *  String tip = "IpAddress in form of ddd.ddd.ddd.ddd";
 *  String allow = FilteredDocument.DIGIT + "." + ...;
 *  TextFieldEditor editor = new TextFieldEditor( len, allow, tip);
 *
 */
public class TextFieldEditor extends AbstractCellEditor 
   implements TableCellEditor, TableCellRenderer, ActionListener {

   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private JTextField component = new JTextField();
   private Vector<CellEditorListener> editorListeners = new Vector<CellEditorListener>();

   // constructor
   public TextFieldEditor(int maxLength, String allow) {
      this( maxLength, allow, null);
   }
   public TextFieldEditor(int maxLength, String allow, String tip) {
      super();
      FilteredDocument doc = new FilteredDocument( maxLength, allow);
      component.setDocument( doc);
      if( tip != null) {
         component.setToolTipText( tip);
      }
      component.addActionListener(this);
   }

   // implementation of ActionListener
   //----------------------------------------------------------------------
   public void actionPerformed(ActionEvent e) {
      //showln("TextFieldEditor: actionPerformed(): "+component.getText());
      xfireEditingStopped();
   }

   protected void xfireEditingStopped() {
      //showln("TextFieldEditor: fireEditingStopped()...");
      for(int i=0; i < editorListeners.size(); ++i) {
         //showln("TextFieldEditor: fireEditingStoped: i="+i);
         try {
         ((CellEditorListener)editorListeners.elementAt(i)).editingStopped(
	    new ChangeEvent(this));
         } catch(Throwable ex) { }
      }
   }

   // implementation of TableCellRenderer
   //----------------------------------------------------------------------
   public Component getTableCellRendererComponent(JTable table, Object value,
         boolean isSelected, boolean hasFocus, int row, int column) {
      //showln("TextFieldEditor(1): Setting value: "+value);
      if( value != null) {
         component.setText( value.toString());
      }
      else {
         component.setText("");
      }
      return (Component)component;
   }

   // implementation of TabeCellEditor
   //----------------------------------------------------------------------
   public Component getTableCellEditorComponent(JTable table, Object value,
      boolean isSelected, int row, int column) {
      //showln("TextFieldEditor(2): Setting value: "+value);
      if( value != null) {
         component.setText( value.toString());
      }
      else {
         component.setText("");
      }
      return component;
   }

   public boolean isCellEditable(EventObject anEvent) {
      return true;
   }
   public boolean shouldSelectCell(EventObject anEvent) {
      return true;
   }
   public Object getCellEditorValue() {
      //showln("TextFieldEditor: getCellEditorValue(): "+component.getText());
      return component.getText();
   }
   public boolean stopCellEditing() {
      //showln("TextFieldEditor: stopCellEditing(): "+component.getText());
      actionPerformed(new ActionEvent(this, 1, "stopCellEditing"));
      return true;
   }
   public void cancelCellEditing() {
      //showln("TextFieldEditor: cancelCellEditing(): ...");
   }

   //--------------------------------------------------------------
   public void addCellEditorListener(CellEditorListener l) {
      editorListeners.add(l);
   }
   public void removeCellEditorListener(CellEditorListener l) {
      editorListeners.remove(l);
   }
}
