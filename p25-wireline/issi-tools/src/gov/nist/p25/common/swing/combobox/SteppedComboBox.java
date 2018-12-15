//
package gov.nist.p25.common.swing.combobox;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;


public class SteppedComboBox extends JComboBox {

   private static final long serialVersionUID = -1L;
   protected int popupWidth;
                      
   public SteppedComboBox(ComboBoxModel aModel) {
      super(aModel);
      setUI(new SteppedComboBoxUI());
      popupWidth = 0;
   }

   public SteppedComboBox(final Object[] items) {
      super(items);
      setUI(new SteppedComboBoxUI());
      popupWidth = 0;
   }
                      
   public SteppedComboBox(Vector<String> items) {
      super(items);
      setUI(new SteppedComboBoxUI());
      popupWidth = 0;
   }
                      
   public void setPopupWidth(int width) {
      popupWidth = width;
   }
                      
   public Dimension getPopupSize() {
      Dimension size = getSize();
      if (popupWidth < 1) popupWidth = size.width;
      return new Dimension(popupWidth, size.height);
   }
}

class SteppedComboBoxUI extends MetalComboBoxUI {
   protected ComboPopup createPopup() {
      BasicComboPopup popup = new BasicComboPopup( comboBox ) {
         private static final long serialVersionUID = -1L;
                           
         public void show() {
            Dimension popupSize = ((SteppedComboBox)comboBox).getPopupSize();
            popupSize.setSize( popupSize.width,
               getPopupHeightForRowCount( comboBox.getMaximumRowCount() ) );
            int x=popupSize.width-comboBox.getBounds().width;
            Rectangle popupBounds = computePopupBounds( -x,
               comboBox.getBounds().height, popupSize.width, popupSize.height);
            //Rectangle popupBounds = computePopupBounds( -(popupSize.width-23),
            //   comboBox.getBounds().height, popupSize.width, popupSize.height);
            scroller.setMaximumSize( popupBounds.getSize() );
            scroller.setPreferredSize( popupBounds.getSize() );
            scroller.setMinimumSize( popupBounds.getSize() );
            list.invalidate();
            int selectedIndex = comboBox.getSelectedIndex();
            if ( selectedIndex == -1 ) {
               list.clearSelection();
            } else {
               list.setSelectedIndex( selectedIndex );
            }
            list.ensureIndexIsVisible( list.getSelectedIndex() );
            setLightWeightPopupEnabled( comboBox.isLightWeightPopupEnabled() );

            show( comboBox, popupBounds.x, popupBounds.y );
         }
      };
      popup.getAccessibleContext().setAccessibleParent(comboBox);
      return popup;
   }
}

