//
package gov.nist.p25.common.swing.util;

import java.awt.image.BufferedImage;
import java.awt.Component;
import java.awt.Graphics2D;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 *  Image Utility for SWING Components.
 */
public class ImageUtility
{

   public static void saveComponentAsImage(Component c, String filename, String type)
      throws IOException
   {
      int w = c.getWidth();
      int h = c.getHeight();
      BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2 = image.createGraphics();
      c.paint(g2);
      g2.dispose();

      // type: png, gif, jpg ...
      ImageIO.write(image, type, new File(filename));
   }

   public static void doSaveAs(Component myPane, String target, String type)
   {
      //System.out.println("target="+target);

      JFileChooser fc = new JFileChooser();
      File file = new File( target);
      fc.setSelectedFile( file);

      int retval = fc.showSaveDialog( myPane);
      if( retval == JFileChooser.APPROVE_OPTION)
      {
         File saveFile = fc.getSelectedFile();
         String filename = saveFile.getAbsolutePath();
         try {
            saveComponentAsImage( myPane, filename, type);

            JOptionPane.showMessageDialog(null,
               "Successfully saved image file:\n\n"+filename,
               "Saving Image File", 
               JOptionPane.INFORMATION_MESSAGE);
         }
         catch(IOException ex) {
            JOptionPane.showMessageDialog(null,
               "Error in saving image file: "+filename,
               "Save As Error", 
               JOptionPane.ERROR_MESSAGE);
         }
      }
   } 
}
