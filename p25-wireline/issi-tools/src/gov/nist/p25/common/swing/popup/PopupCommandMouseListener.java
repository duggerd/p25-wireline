//
package gov.nist.p25.common.swing.popup;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JMenuItem;


public class PopupCommandMouseListener implements MouseListener
{
   //public static void showln(String s) { System.out.println(s); }

   private Component invoker;
   private PopupCommandMenu popupMenu;

   // constructor
   public PopupCommandMouseListener( Component invoker,
      PopupCommandMenu popupMenu)
   {
      this.invoker = invoker;
      this.popupMenu = popupMenu;
   }

   //------------------------------------------------------------------
   // implementation of MouseListener
   //------------------------------------------------------------------
   public void mousePressed( MouseEvent e) { handlePopup(e); }
   public void mouseReleased( MouseEvent e) { handlePopup(e); }
   public void mouseEntered( MouseEvent e) { }
   public void mouseExited( MouseEvent e) { }
   public void mouseDragged( MouseEvent e) { }
   public void mouseMoved( MouseEvent e) { }
   public void mouseClicked( MouseEvent e)
   {
      handlePopup(e);
   }

   private void handlePopup(MouseEvent e)
   {
      //JComponent jc = (JComponent)e.getSource();
      //showln("handlePopup: src="+jc.getClass().getName());
      //showln("handlePopup: x,y="+e.getX()+","+e.getY());

      if (e.isPopupTrigger() && popupMenu != null)
      {
         //NPE
         //showln("handlePopup:X,Y="+jc.getLocation(null).x+","+jc.getLocation(null).y);
         //showln("handlePopup:evt-X,Y="+e.getX()+","+e.getY());
         popupMenu.show( invoker, e.getX(), e.getY());
      }
   }

   public void disablePopupMenu()
   {
      if( popupMenu != null)
      for(int i=0; i < popupMenu.getSubElements().length; i++)
      {
         ((JMenuItem)popupMenu.getComponent(i)).setEnabled(false);
      }
   }
}
