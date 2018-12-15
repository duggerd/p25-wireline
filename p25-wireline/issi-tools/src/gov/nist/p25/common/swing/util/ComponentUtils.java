//
package gov.nist.p25.common.swing.util;

import java.awt.Component;
import java.awt.Container;
import java.util.Collection;
//import javax.swing.JMenu;
import javax.swing.JMenuItem;


public class ComponentUtils
{
   public static void getAllComponents(Component c, Collection collection)
   {
      // To get a reference to top container:
      //    c = comp.getTopLevelAncestor();
      // or
      //    c = SwingUtilities.getWindowAncestor(comp);
      //
      collection.add(c);
   
      if (c instanceof Container)
      {
         Component[] kids = ((Container)c).getComponents();
         for(int i=0; i < kids.length; i++)
         {
            getAllComponents(kids[i], collection);
         }
      }
   }

   public static void getFilteredComponents(Component c, Object obj, Collection collection)
   {
      if( c instanceof JMenuItem)
         collection.add(c);
   
      if (c instanceof Container)
      {
         Component[] kids = ((Container)c).getComponents();
         for(int i=0; i < kids.length; i++)
         {
            getAllComponents(kids[i], collection);
         }
      }
   }
}
