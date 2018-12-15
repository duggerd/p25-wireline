//
package gov.nist.p25.common.swing.jctree;

/*
=====================================================================
   AbstractLayout.java
   Created by Claude Duguay
   Copyright (c) 1998
   JComponent Tree, Java Developers Journal, V3 Issue 10.
=====================================================================
*/

import java.awt.Dimension;
import java.awt.Container;
import java.awt.Component;
import java.awt.LayoutManager2;
import java.io.Serializable;

public abstract class AbstractLayout
   implements LayoutManager2, Serializable
{
   private static final long serialVersionUID = -1L;
   protected int hgap;
   protected int vgap;

   public AbstractLayout()
   {
      this(0, 0);
   }

   public AbstractLayout(int hgap, int vgap)
   {
      super();
      setHgap(hgap);
      setVgap(vgap);
   }

   public int getHgap()
   {
      return hgap;
   }

   public int getVgap()
   {
      return vgap;
   }

   public void setHgap(int gap)
   {
      hgap = gap;
   }

   public void setVgap(int gap)
   {
      vgap = gap;
   }

   /**
    * Returns the maximum dimensions for this layout given
    * the component in the specified target container.
    * @param target The component which needs to be laid out
   **/
      public Dimension maximumLayoutSize(Container target)
   {
      return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
   }

   /**
    * Returns the alignment along the x axis. This specifies how
    * the component would like to be aligned relative to other 
    * components. The value should be a number between 0 and 1
    * where 0 represents alignment along the origin, 1 is aligned
    * the furthest away from the origin, 0.5 is centered, etc.
   **/
   public float getLayoutAlignmentX(Container parent)
   {
      return 0.5f;
   }

   /**
    * Returns the alignment along the y axis. This specifies how
    * the component would like to be aligned relative to other 
    * components. The value should be a number between 0 and 1
    * where 0 represents alignment along the origin, 1 is aligned
    * the furthest away from the origin, 0.5 is centered, etc.
   **/
   public float getLayoutAlignmentY(Container parent)
   {
      return 0.5f;
   }

   /**
    * Invalidates the layout, indicating that if the layout
    * manager has cached information it should be discarded.
   **/
   public void invalidateLayout(Container target) {}
                  
   /**
    * Adds the specified component with the specified name
    * to the layout. By default, we call the more recent
    * addLayoutComponent method with an object constraint
    * argument. The name is passed through directly.
    * @param name The name of the component
    * @param comp The component to be added
   **/
   public void addLayoutComponent(String name, Component comp)
   {
      addLayoutComponent(comp, name);
   }

   /**
    * Add the specified component from the layout.
    * By default, we let the Container handle this directly.
    * @param comp The component to be added
    * @param constraints The constraints to apply when laying out.
   **/
   public void addLayoutComponent(Component comp, Object constraints) {}

   /**
    * Removes the specified component from the layout.
    * By default, we let the Container handle this directly.
    * @param comp the component to be removed
   **/
   public void removeLayoutComponent(Component comp) {}

   public String toString()
   {
      return getClass().getName() +
         "[hgap=" + hgap + ",vgap=" + vgap + "]";
   }
}
