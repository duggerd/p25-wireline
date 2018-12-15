//
package gov.nist.p25.common.tool.file;

import java.io.File;
import java.util.Stack;

/** 
 * FileWalker walks thru a given directory and subdirectories.
 */
public class FileWalker
{
   public static void showln(String s) { System.out.println(s); }

   private Stack<String> stack = new Stack<String>();

   public FileWalker(String fname)
   {
      stack.push(fname);
   }

   public String getNext( )
   {
      return getNext( false);
   }
   public synchronized String getNext( boolean isOnlyDir)
   {
      if (stack.empty())
         return null;

      String fn = (String)stack.pop();
      File f = new File(fn);

      if (f.isDirectory())
      {
         String list[] = f.list();
         int len = list.length - 1;
         for (int i=len; i >= 0; i--)
         {
            File f2 = new File(fn, list[i]);
            if( isOnlyDir)
            {
               if (f2.isDirectory())
                  stack.push( f2.getPath());
            }
            else
            {
               stack.push( f2.getPath());
            }
         }
      }
      else
      {
         if( isOnlyDir)
            throw new IllegalArgumentException( fn +" is not a directory.");
      }
      return f.getPath();
   }

   //================================================================
   public static void main(String args[])
   {
      if( args.length != 1)
      {
         showln("Usage:");
         showln("   java FileWalker directory");
         showln("e.g.");
         showln("   java FileWalker c:/tmp");
         System.exit(0);
      }

      FileWalker fw = new FileWalker(args[0]);

      boolean onlyDir = false;
      int nfiles = 0;
      String path = null;
      while ((path = fw.getNext( onlyDir)) != null)
      {
         nfiles++;
         showln(path);
      }
      showln("Total number of files: "+nfiles);
   }
}

