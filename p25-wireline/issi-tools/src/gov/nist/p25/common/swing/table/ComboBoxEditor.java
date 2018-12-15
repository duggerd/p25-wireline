//
package gov.nist.p25.common.swing.table;

import java.util.Vector;
import javax.swing.DefaultCellEditor;

import gov.nist.p25.common.swing.combobox.SteppedComboBox;

/**
 * ComboBox Editor
 */
public class ComboBoxEditor extends DefaultCellEditor {

   private static final long serialVersionUID = -1L;

   // constructor
   public ComboBoxEditor(Vector<String> itemsVec) {
      //super( new JComboBox(itemsVec));
      super( new SteppedComboBox(itemsVec));
   }
   public ComboBoxEditor(String[] items) {
      //super( new JComboBox(items));
      super( new SteppedComboBox(items));
   }
}
