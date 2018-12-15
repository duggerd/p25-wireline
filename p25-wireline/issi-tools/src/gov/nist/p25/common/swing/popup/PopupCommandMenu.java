//
package gov.nist.p25.common.swing.popup;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class PopupCommandMenu extends JPopupMenu
   implements ActionListener
{
   private static final long serialVersionUID = -1L;
   //public static void showln(String s) { System.out.println(s); }
   
   private JComponent owner;
   private EventListenerList popupListener = new EventListenerList();

   // accessors
   public JComponent getOwner() { return owner; }
   public EventListenerList getPopupCommandListener() { return popupListener; }

   // constructor
   public PopupCommandMenu( JComponent owner, String[] plist)
   {
      super();
      this.owner = owner;   // JTree, DynamicTree, FSTree
      buildPopupMenu( plist );
   }

   public void buildPopupMenu( String[] plist)
   {
      for(int i=0; i < plist.length; i++)
      {
         String cmd = plist[i];
         //showln("buildPopupMenu(): cmd="+cmd);
         if( "~Separator~".equals(cmd))
         {
            addSeparator();
         }
         else
         {
            JMenuItem item = new JMenuItem( plist[i] );
            item.setActionCommand( plist[i] );
            item.addActionListener( this );
            add( item );
         }
      }
   }

   //----------------------------------------------------------------------
   public void addPopupCommandListener( PopupCommandListener l)
   {
      getPopupCommandListener().add(PopupCommandListener.class,l);
   }

   public void removePopupCommandListener( PopupCommandListener l)
   {
      getPopupCommandListener().remove(PopupCommandListener.class,l);
   }

   protected void firePopupCommandEvent( PopupCommandEvent evt)
   {
      Object[] listeners = getPopupCommandListener().getListenerList();
      for (int i = listeners.length-2; i>=0; i-=2)
      {
         if (listeners[i]==PopupCommandListener.class)
         {
            ((PopupCommandListener)listeners[i+1]).processPopupCommand(evt);
         }
      }
   }

   // implementation of ActionListener
   //----------------------------------------------------------------------
   public void actionPerformed( ActionEvent e)
   {
      // src isA JMenuItem
      Object src = e.getSource();
      for( int i=0; i < getComponentCount(); i++)
      {
         Object obj = getComponent(i);
         if( !(obj instanceof JMenuItem)) continue;
         JMenuItem item = (JMenuItem)obj;
         if( src == item)
         {
            // passed along src and owner
            PopupCommandEvent pevt =
               new PopupCommandEvent(src,owner,null,null);
            firePopupCommandEvent( pevt );
            return;
         }
      }
   }
}
