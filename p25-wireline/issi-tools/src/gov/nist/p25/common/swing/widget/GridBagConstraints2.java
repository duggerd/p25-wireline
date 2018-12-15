//
package gov.nist.p25.common.swing.widget;

import java.awt.GridBagConstraints;
//import java.awt.Insets;

/**
 *  GridBagConstraints2
 *  Specializes GridBagConstraints by adding convenience methods
 *  for setting fields. No fields are added to the class.
 *  Does not include means to set ipadx and ipady since
 *  these are rarely useful. (Although they can still be
 *  assigned directly if necessary.)
 */

public class GridBagConstraints2 extends GridBagConstraints
{
   private static final long serialVersionUID = -1L;

   // set the 12 specified fields in this object
   public void set(int gridx, int gridy, int gridwidth, int gridheight,
     String anchorStr, String fillStr,
     int left, int top, int right, int bottom,
     double weightx, double weighty) {

      this.gridx = gridx;
      this.gridy = gridy;

      this.gridwidth = gridwidth;
      this.gridheight = gridheight;

      if (anchorStr.equalsIgnoreCase("n"))
         anchor = NORTH;
      else if (anchorStr.equalsIgnoreCase("s"))
         anchor = SOUTH;
      else if (anchorStr.equalsIgnoreCase("e"))
         anchor = EAST;
      else if (anchorStr.equalsIgnoreCase("w"))
         anchor = WEST;
      else if (anchorStr.equalsIgnoreCase("ne"))
         anchor = NORTHEAST;
      else if (anchorStr.equalsIgnoreCase("nw"))
         anchor = NORTHWEST;
      else if (anchorStr.equalsIgnoreCase("se"))
         anchor = SOUTHEAST;
      else if (anchorStr.equalsIgnoreCase("sw"))
         anchor = SOUTHWEST;
      else if (anchorStr.equalsIgnoreCase("c"))
         anchor = CENTER;
      else
         throw new IllegalArgumentException(
           "Illegal anchor '" + anchorStr + "'");

      if (fillStr.equalsIgnoreCase("horz"))
         fill = HORIZONTAL;
      else if (fillStr.equalsIgnoreCase("vert"))
         fill = VERTICAL;
      else if (fillStr.equalsIgnoreCase("both"))
         fill = BOTH;
      else if (fillStr.equalsIgnoreCase("none"))
         fill = NONE;
      else
         throw new IllegalArgumentException(
           "Illegal fill '" + fillStr + "'");

      insets.left = left;
      insets.top = top;
      insets.right = right;
      insets.bottom = bottom;

      this.weightx = weightx;
      this.weighty = weighty;
   }

   //--------------------------------------------------------------------
   // set 10 fields, and default weigthx and weighty
   public void set(int gridx, int gridy, int gridwidth, int gridheight,
     String anchorStr, String fillStr,
     int left, int top, int right, int bottom) {
      set(gridx, gridy, gridwidth, gridheight, anchorStr, fillStr,
          left, top, right, bottom, 0.0, 0.0);
   }
   public void setWeight(int gridx, int gridy, int gridwidth, int gridheight,
     String anchorStr, String fillStr,
     int left, int top, int right, int bottom) {
      set(gridx, gridy, gridwidth, gridheight, anchorStr, fillStr,
          left, top, right, bottom, 1.0, 1.0);
   }

   //--------------------------------------------------------------------
   // set 6 fields, and default insets, weigthx and weighty
   public void set(int gridx, int gridy, int gridwidth, int gridheight,
     String anchorStr, String fillStr) {
      set(gridx, gridy, gridwidth, gridheight, anchorStr, fillStr,
          0, 0, 0, 0, 0.0, 0.0);
   }
   public void setWeight(int gridx, int gridy, int gridwidth, int gridheight,
     String anchorStr, String fillStr) {
      set(gridx, gridy, gridwidth, gridheight, anchorStr, fillStr,
          0, 0, 0, 0, 1.0, 1.0);
   }

   //--------------------------------------------------------------------
   // set 4 fields, and default anchor, fill, insets, weigthx and weighty
   public void set(int gridx, int gridy, int gridwidth, int gridheight) {
      set(gridx, gridy, gridwidth, gridheight, "c", "none",
          0, 0, 0, 0, 0.0, 0.0);
   }
   public void setWeight(int gridx, int gridy, int gridwidth, int gridheight) {
      set(gridx, gridy, gridwidth, gridheight, "c", "none",
          0, 0, 0, 0, 1.0, 1.0);
   }
}
