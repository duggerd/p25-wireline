//
package gov.nist.p25.common.swing.widget;

import java.awt.Component;
import java.awt.Dimension;

/**
 * Filler - a transparent, lightweight component used for 
 * creating spacing in layouts.
 */
public class Filler extends Component
{
   private static final long serialVersionUID = -1L;

   private int prefWidth;
   private int prefHeight;

   public int getPrefWidth() { return prefWidth; }
   public int getPrefHeight() { return prefHeight; }
   
   public void setPrefWidth(int w) { prefWidth=w; }
   public void setPrefHeight(int h) { prefHeight=h; }

   public Filler()
   {
      super();
   }

   public Filler(int pfWidth, int pfHeight)
   {
      super();
      setPrefWidth( pfWidth );
      setPrefHeight( pfHeight );
   }

   public Dimension getPreferredSize()
   {
      return new Dimension(getPrefWidth(), getPrefHeight());
   }
}
