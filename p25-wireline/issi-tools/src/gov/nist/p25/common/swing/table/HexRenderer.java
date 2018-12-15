//
package gov.nist.p25.common.swing.table;

import javax.swing.table.DefaultTableCellRenderer;

/**
 * Hex Renderer
 */
public class HexRenderer extends DefaultTableCellRenderer {
   private static final long serialVersionUID = -1L;

   public HexRenderer() {
      super();
   }

   public void setValue(Object value) {
      if( value instanceof Integer) {
         setText(Integer.toHexString((Integer)value));
      }
      else if( value instanceof Long) {
        setText("0x"+Long.toHexString((Long)value));
      }
   }

   public void validate() {
      //System.out.println("HexRenderer: validate()...");
   }
}
