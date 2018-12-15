//
package gov.nist.p25.common.swing.table;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 *
  JTable Column Header Tool tip support.
  Usage:
    JTable table = new JTable(rows, cols);
    JTableHeader header = table.getTableHeader();
    TableColumnHeaderToolTips tips = new TableColumnHeaderToolTips();
    
    // Assign a tooltip for each of the columns
    for (int i=0; i<table.getColumnCount(); i++) {
        TableColumn col = table.getColumnModel().getColumn(i);
        tips.setToolTip(col, "Col-"+i);
    }
    header.addMouseMotionListener(tips);
 *
 */
public class TableColumnHeaderToolTips extends MouseMotionAdapter {

   private static final long serialVersionUID = -1L;

   // Current column whose tooltip is being displayed.
   private TableColumn curCol;
 
   // Maps TableColumn objects to tooltips
   private Map<TableColumn, String> tips = new HashMap<TableColumn, String>();
 
   public void setToolTip(TableColumn col, String tooltip) {
      // If tooltip is null, removes any tooltip text.
      if (tooltip == null) {
         tips.remove(col);
      } else {
         tips.put(col, tooltip);
      }
   }
 
   public void mouseMoved(MouseEvent evt) {

      JTableHeader header = (JTableHeader)evt.getSource();
      JTable table = header.getTable();
      TableColumnModel colModel = table.getColumnModel();
      int column = colModel.getColumnIndexAtX(evt.getX());
 
      // Return if not clicked on any column header
      TableColumn col = null;
      if (column >= 0) {
         col = colModel.getColumn(column);
      }
 
      if (col != curCol) {
         header.setToolTipText((String)tips.get(col));
         curCol = col;
      }
   }
}
