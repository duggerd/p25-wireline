//
package gov.nist.p25.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.Serializable;

//import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.io.SyncFailedException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * General purpose file utility
 */
public class FileUtility implements Serializable
{
   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   //-------------------------------------------------------------------
   public static void copyFile(File inFile, File outFile)
      throws IOException
   {
      InputStream in = new FileInputStream(inFile);
      OutputStream out = new FileOutputStream(outFile);
    
      // Transfer bytes from in to out
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
         out.write(buf, 0, len);
      }
      in.close();
      out.close();
   }
   
   /***
   public static boolean renameFile(File inFile, File outFile)
   {
      return inFile.renameTo( outFile);
   }

   public static void syncFile(File outFile)
      throws FileNotFoundException, IOException, SyncFailedException
   {
      // force update to a file to the disk
      FileOutputStream os = new FileOutputStream(outFile);
      FileDescriptor fd = os.getFD();
      // ... update
      fd.sync();
   }
    ***/

   //-------------------------------------------------------------------
   public static List<String> loadFromFile( String fname)
      throws IOException
   { 
      File file = new File( fname );
      BufferedReader brdr= new BufferedReader(new FileReader(file));
      
      List<String> list = new ArrayList<String>();
      while( brdr.ready())
      {
         String xcmd = brdr.readLine();
         if( xcmd == null)
            break;
         //showln("CMD: <"+xcmd+">");
         //if( xcmd.startsWith("#") ) continue;
         list.add( xcmd );
      }
      brdr.close();
      return list;
   }

   public static String loadFromFileAsString( String fname)
      throws IOException
   { 
      File file = new File( fname );
      BufferedReader brdr= new BufferedReader(new FileReader(file));
      
      StringBuffer sbuf = new StringBuffer();
      while( brdr.ready())
      {
         String xcmd = brdr.readLine();
         if( xcmd == null)
            break;
         //showln("CMD: <"+xcmd+">");
         sbuf.append( xcmd );
         sbuf.append( "\n");
      }
      brdr.close();
      return sbuf.toString();
   }

   //-----------------------------------------------------------------------
   public static void saveToFile( String outfile, List<String> list)
      throws IOException
   { 
      FileWriter fw = new FileWriter( outfile );
      Iterator<String> iter = list.iterator();
      while( iter.hasNext() )
      {
         String text = (String)iter.next();
         if( text.length() > 0)
            fw.write( text, 0, text.length());
         fw.write("\n");
      }
      fw.flush();
      fw.close();
   }

   public static void saveToFile( String outfile, String outstr)
      throws IOException
   {
      FileWriter fw = new FileWriter( outfile );
      fw.write( outstr, 0, outstr.length());
      fw.flush();
      fw.close();
   }

   //-----------------------------------------------------------------------
   public static void uncompressFile( String inFilename, String outFilename)
      throws IOException
   {
      // Open the compressed file (Gzip)
      GZIPInputStream in = new GZIPInputStream(new FileInputStream(inFilename));
   
      // Open the output file
      OutputStream out = new FileOutputStream(outFilename);
   
      // Transfer bytes from the compressed file to the output file
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
         out.write(buf, 0, len);
      }
   
      // Close the file and stream
      in.close();
      out.close();
   } 

   //-----------------------------------------------------------------------
   public static void compressFile( String inFilename, String outFilename)
      throws IOException
   {
      // Create the GZIP output stream
      GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(outFilename));
    
      // Open the input file
      FileInputStream in = new FileInputStream(inFilename);
    
      // Transfer bytes from the input file to the GZIP output stream
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
         out.write(buf, 0, len);
      }
      in.close();
    
      // Complete the GZIP file
      out.finish();
      out.close();
   }

   //-----------------------------------------------------------------------
   public static Properties loadProperties(String name)
      throws FileNotFoundException, IOException
   {
      Properties prop = new Properties();
      FileInputStream fs = new FileInputStream( name);
      prop.load(fs);
      fs.close();
      return prop;
   }

   public static void writeProperties(File dir, String name, Properties prop)
      throws IOException
   {
      File file = null;
      FileOutputStream out = null;
      try
      {
          file = new File(dir, name);
          out = new FileOutputStream( file);
          prop.store(out, "auto store " + name + " - " + new Date());
          //prop.storeToXML(out, "auto store " + name + " - " + new Date());
      }
      finally
      {
         try {
            if(out != null)
               out.close();
         }
         catch (IOException e) { }
      }
   }

   //---------------------------------------------------------------------
   public final static String EXT_JPEG = "jpeg";
   public final static String EXT_JPG = "jpg";
   public final static String EXT_GIF = "gif";
   public final static String EXT_TIFF = "tiff";
   public final static String EXT_TIF = "tif";
   
   public static String getFilenameWOExtension(String filename)
   {
      String name = filename;
      //String ext = "";
      int index = filename.lastIndexOf(".");
      if( index >= 0) {
         name = filename.substring( 0, index);
         //ext = filename.substring( index+1);
      }  
      return name;
   }

   public static String getFileExtension(File f)
   {
      String ext = "";
      String s = f.getName();
      int i = s.lastIndexOf('.');
      if (i > 0 &&  i < s.length() - 1) {
         ext = s.substring(i+1).toLowerCase();
      }
      return ext;
   }
   
   public static boolean makeDir( String dir)
   {
      boolean bflag = false;
      File file = new File(dir);
      if( !file.exists()) {
         bflag = file.mkdir();
      }
      return bflag;
   }

   public static void cleanDir( String dir)
   {
      File file = new File(dir);
      if( file.exists()) {
         delete( file, true);
      }
      else {
         file.mkdir();
      }
   }

   public static void delete( File file, boolean includeDir)
   {
      if( file.isDirectory()) {
         deleteDir(file, includeDir);
      }
      else {
         file.delete();
      }
   }

   public static void deleteDir( File file, boolean includeDir)
   {
      File[] files = file.listFiles();
      for( int i=0; i < files.length; i++) {
         File xfile = files[i];
         if( xfile.isDirectory()) {
            deleteDir( xfile, true);
         }
         else {
            xfile.delete();
         }
      } 
      if( includeDir) {
         file.delete();
      }
   }

   //===========================================================
   /*** Unit test
   public static void testMakeDir(String[] args) {

      String dir1 = "c:/temp/WEB-INF";
      boolean b1 = FileUtility.makeDir( dir1);
      System.out.println("makeDir: "+dir1 +" bflag="+b1);

      String dir2 = "c:/temp/WEB-INF/client";
      boolean b2 = FileUtility.makeDir( dir2);
      System.out.println("makeDir: "+dir2 +" bflag="+b2);

      String dir3 = "c:/temp/WEB-INF/lib";
      FileUtility.cleanDir( dir3);
      System.out.println("makeDir: "+dir3);

      // create dir c:/temp/abc/def
      boolean bb = false;
      String dir = "c:/temp";

      bb = FileUtility.makeDir( dir);
      System.out.println("makeDir: "+dir +" bflag="+bb);

      dir = "c:/temp/abc";
      bb = FileUtility.makeDir( dir);
      System.out.println("makeDir: "+dir +" bflag="+bb);

      dir = "c:/temp/abc/def";
      bb = FileUtility.makeDir( dir);
      System.out.println("makeDir: "+dir +" bflag="+bb);
   }
    ***/
}
