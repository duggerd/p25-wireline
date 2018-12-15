//
package gov.nist.p25.common.swing.widget;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * CheckBoxList
 * http://www.devx.com/tips/Tip/5342
 */
public class CheckBoxList extends JList
{
   protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

   // constructor
   public CheckBoxList() {
      super();
      initialize();
   }
   public CheckBoxList(ListModel model) {
      super(model);
      initialize();
   }
   public CheckBoxList(JCheckBox[] listData) {
      super(listData);
      initialize();
   }
   public CheckBoxList(Vector<JCheckBox> listData) {
      super(listData);
      initialize();
   }

   private void initialize()
   {
      setCellRenderer(new CellRenderer());
      addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent e)
         {
            int index = locationToIndex(e.getPoint());
            if (index != -1) {
               JCheckBox checkbox = (JCheckBox)getModel().getElementAt(index);
               checkbox.setSelected(!checkbox.isSelected());
               repaint();
            }
         }
      });
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
   }

   protected class CellRenderer implements ListCellRenderer
   {
      public Component getListCellRendererComponent(JList list,
         Object value, int index, boolean isSelected, boolean cellHasFocus)
      {
         JCheckBox checkbox = (JCheckBox) value;
         checkbox.setBackground(isSelected ?
            getSelectionBackground() : getBackground());
         checkbox.setForeground(isSelected ?
            getSelectionForeground() : getForeground());
         checkbox.setEnabled(isEnabled());
         checkbox.setFont(getFont());
         checkbox.setFocusPainted(false);
         checkbox.setBorderPainted(true);
         checkbox.setBorder(isSelected ?
            UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
         return checkbox;
      }
   }

   // for unit test only
   public static CheckBoxList getCheckBoxList() {
      String[] data = {
         "4.1.1 : su_registration_successful_presence",
         "9.1.1 : group_call_setup_successful",
         "10.1.1 : group_call_setup_successful_released_by_serving",
         "11.1.1 : group_call_setup_unconfirmed_serving_talk_spurt"
      };
      Vector<JCheckBox> cbVec = new Vector<JCheckBox>();
      for( String tag: data) {
         cbVec.add( new JCheckBox(tag));
      }
      CheckBoxList cbList = new CheckBoxList( cbVec);
      return cbList;
   }

   //====================================================================
   public static void main(String args[]) throws Exception
   {
      CheckBoxList cbList = getCheckBoxList();
      JFrame frame = new JFrame("CheckBoxList");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add( cbList);
      frame.pack();
      frame.setVisible(true);
   }
}

