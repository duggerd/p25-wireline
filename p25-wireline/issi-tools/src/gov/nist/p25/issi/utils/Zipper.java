package gov.nist.p25.issi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

public class Zipper {
   
   private static Logger logger = Logger.getLogger(Zipper.class);
   
   private static void zip(File x,  ZipOutputStream z) {
      try {
         if (!x.exists())
            logger.error("file not found" + x.getAbsolutePath());
         if (!x.isDirectory()) {
            String dir = new File ( x.getPath()).getParent();
            z.putNextEntry(new ZipEntry(( x.getPath()).replace('\\',
                  '/')));
            FileInputStream y = new FileInputStream(x);
            byte[] a = new byte[(int) x.length()];
            int did = y.read(a);
            if (did != x.length())
               logger.error("DID NOT GET WHOLE FILE " + dir
                     + x.getName() + " ; only " + did + " of "
                     + x.length());
            z.write(a, 0, a.length);
            z.closeEntry();
            y.close();
            x = null;
         } else // recurse
         {
            String nnn =  x.getPath() + File.separator;
            x = null;
            z.putNextEntry(new ZipEntry(nnn.replace('\\', '/')));
            z.closeEntry();
            String[] dirlist = (new File(nnn)).list();
            for (int i = 0; i < dirlist.length; i++) {
               zip(new File(nnn + dirlist[i]), z);
            }
         }
      } catch (Exception e) {
         logger.error("Error in zip-Method!!", e);
      }
   }

   // only creates the zip and initiates the recursive zipping
   // here all folders beginning with dirsStartingWith are included
   public static void zipAll(String[] dirsStartingWith, String name) {
      try {
         ZipOutputStream z = new ZipOutputStream(new FileOutputStream(name));
         for (int i = 0; i < dirsStartingWith.length; i++)
               zip(new File( "./"+dirsStartingWith[i]), z);
         z.close();
      } catch (Exception e) {
         System.err.println("Error in zipAll-Method!!" + e);
      }
   }

} // Class Zipper

