//
package gov.nist.p25.common.swing.table;

import java.awt.Component;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * ComboBox Renderer.
 */
public class ComboBoxRenderer extends JComboBox
   implements TableCellRenderer {  

   private static final long serialVersionUID = -1L;
   //public static void showln(String s) { System.out.println(s); }

   // constructor
   public ComboBoxRenderer(Vector<?> itemsVec) {
      super(itemsVec);
   }
   public ComboBoxRenderer(String[] items) {
      super(items);
   }

   // implementation of TableCellRenderer
   //------------------------------------------------------------------------
   public Component getTableCellRendererComponent(JTable table, Object value,
         boolean isSelected, boolean hasFocus, int row, int column) {
      //showln("ComboBoxRenderer(1): Setting index: "+value);
      if (isSelected) {
         setForeground(table.getSelectionForeground());
         super.setBackground(table.getSelectionBackground());
      } else {
         setForeground(table.getForeground());
         setBackground(table.getBackground());
      }

      if( value instanceof Integer) {
         setSelectedIndex(((Integer)value).intValue());
      }
      else if( value instanceof String) {
         //showln("ComboBoxRenderer(1): String index: "+value);
	 //if( ((String)value).length() > 0)
            setSelectedItem( (String)value);
      }
      return this;
   }
}
