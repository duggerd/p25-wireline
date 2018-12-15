//
package gov.nist.p25.common.tool.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** 
 * LOCCalculator walks thru a given directory and subdirectories to
 * calculate the line of codes in each java files.
 */
public class LOCCalculator
{
   public static void showln(String s) { System.out.println(s); }

   public static void calculate(String dirname, String extension, OutputStream os)
      throws IOException
   {
      boolean onlyDir = false;
      long nfiles = 0;
      long nlines = 0;
      String path = null;

      os.write("LOCCalculator:\n".getBytes());
      os.write("Files:\n".getBytes());

      String str;
      FileWalker fw = new FileWalker( dirname);
      while ((path = fw.getNext( onlyDir)) != null)
      {
         if( !path.endsWith(extension))
            continue;
         File file = new File(path);
         if( file.isFile()) 
         {
            nfiles++;
            long[] data = LineCount.count( path);
            nlines += data[0];
            str = "\t" +data[0] +"\t" +data[1] +"\t" +path;
            os.write( str.getBytes());
            os.write( "\n".getBytes());
         }
      }
      os.write("\nSummary:".getBytes());
      str = "\nTarget directory: "+dirname;
      os.write( str.getBytes());
      str = "\nTarget extension: "+extension;
      os.write( str.getBytes());
      str = "\nTotal number of files: "+nfiles;
      os.write( str.getBytes());
      str = "\nTotal number of lines: "+nlines;
      os.write( str.getBytes());
      str = "\nAvg no lines per file: "+ (long)((float)nlines/(float)nfiles);
      os.write( str.getBytes());
   }

   //=============================================================================
   public static void main(String args[]) throws Exception
   {
      if( args.length != 2)
      {
         showln("Usage:");
         showln("   java LOCCalculator directory fileExt");
         showln("e.g.");
         showln("   java LOCCalculator c:/research/p25-wireline/issi-tools/src java");
         System.exit(0);
      }
      String dirname = args[0];
      String extension = args[1];
      OutputStream os = new FileOutputStream("locoutput.txt");
      LOCCalculator.calculate(dirname, extension, os);
      os.close();
   }
}
