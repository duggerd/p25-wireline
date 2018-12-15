//
package gov.nist.p25.common.tool.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** 
 * FileSearcher walks thru a given directory and subdirectories to
 * search for the a given string pattern.
 */
public class FileSearcher
{
   public static void showln(String s) { System.out.println(s); }

   public static List<String> searchPattern(File file, String pattern)
      throws IOException
   {
      BufferedReader brdr= new BufferedReader( new FileReader(file));
      ArrayList<String> list = new ArrayList<String>();
      int count = 0;
      while( brdr.ready()) {
         count++;
         String line = brdr.readLine();
         if(line == null)
            break;
         //showln("Line: <"+line+">");
         if( line.indexOf(pattern) != -1) {
            list.add( "Line-" + count +": " + line);
         }
      }
      brdr.close();
      return list;
   }

   public static void searchFile(String dirname, String pattern, OutputStream os)
      throws IOException
   {
      boolean onlyDir = false;
      int mfiles = 0;
      int nfiles = 0;
      String path = null;


      os.write("FileSearcher:\n".getBytes());
      os.write("-------------------\n".getBytes());
      String str;
      FileWalker fw = new FileWalker( dirname);
      while ((path = fw.getNext( onlyDir)) != null)
      {
         nfiles++;
         File file = new File(path);
         if( file.isFile()) 
         {
            List<String> list= searchPattern( file, pattern);
            if(list.size() > 0)
            {
               mfiles++;
               str = "File: " + path;
               os.write( str.getBytes());
               os.write( "\n".getBytes());
               for( String line: list) {
                  os.write( "   ".getBytes());
                  os.write( line.getBytes());
                  os.write( "\n".getBytes());
               }
               os.write("-------------------\n".getBytes());
            }
         }
      }
      os.write("\nSummary:".getBytes());
      os.write("\nTarget directory: ".getBytes());
      os.write(dirname.getBytes());
      os.write("\nTarget pattern: ".getBytes());
      os.write(pattern.getBytes());
      str = "\nTotal number of files processed: "+nfiles;
      os.write( str.getBytes());
      str = "\nTotal number of files containing pattern: "+mfiles;
      os.write( str.getBytes());
   }

   //================================================================
   public static void main(String args[]) throws Exception
   {
      if( args.length != 2)
      {
         showln("Usage:");
         showln("   java FileSearcher directory pattern");
         showln("e.g.");
         showln("   java FileSearcher c:/testsuites  rfss_1");
         System.exit(0);
      }
      String dirname = args[0];
      String pattern = args[1];
      OutputStream os = new FileOutputStream("output.txt");
      FileSearcher.searchFile(dirname, pattern, os);
      os.close();
   }
}
