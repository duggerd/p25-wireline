//
package gov.nist.p25.common.tool.file;

import java.io.*;

/**
 * Command line program to count lines, words and characters
 * in files or from standard input, similar to the wc utility.
 *
 * Usage: 
 *   java LineCount FILE1 FILE2 ... 
 * or
 *   java LineCount < FILENAME.
 */
public class LineCount {

   /**
    * Count lines, words and characters in given input stream
    * and print stream name and those numbers to standard output.
    * @param name name of input source
    * @param input stream to be processed
    * @throws IOException if there were I/O errors
    */
   public static long[] count(String name, BufferedReader in)
      throws IOException 
   {
      long numLines = 0;
      long numWords = 0;
      long numChars = 0;
      String line;
      for( ; ; ) {
         line = in.readLine();
         if (line == null)
            break;
         // count all lines for now
         //line = line.trim();
         //if( line.length() > 0)
         {
            numLines++;
            numChars += line.length();
            numWords += countWords(line);
         }
      }

      long[] data = new long[3];
      data[0] =  numLines;
      data[1] =  numWords;
      data[2] =  numChars;
      return data;
   }

   public static void displayResults( String name, long[] data)
   {
      // name, numLines, numWords, numChars
      System.out.println(name + "\t" + data[0] + "\t" + 
         data[1] + "\t" + data[2]);
   }

   /**
    * Count words, lines and characters of given input stream
    * and print them to standard output.
    * @param streamName name of input stream (to print it to stdout)
    * @param input InputStream to read from
    */
   public static long[] count(String streamName, InputStream input)
   {
      long[] data = null;
      try {
         InputStreamReader inputStreamReader = new InputStreamReader(input);
         BufferedReader in = new BufferedReader(inputStreamReader);
         data = count(streamName, in);
         in.close();
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
      return data;
   }


   /**
    * Open file, count its words, lines and characters 
    * and print them to standard output.
    * @param fileName name of file to be processed
    */
   public static long[] count(String fileName)
   {
      long[] data = null;
      BufferedReader in = null;
      try {
         FileReader fileReader = new FileReader(fileName);
         in = new BufferedReader(fileReader);
         data = count(fileName, in);
      } 
      catch (IOException ioe) {
         ioe.printStackTrace();
      } 
      finally {
         if (in != null) {
            try {
               in.close();
            } catch (IOException ioe) {
               ioe.printStackTrace();
            }
         }
      }
      return data;
   }


   /**
    * Determine the number of words in the argument line.
    * @param line String to be examined, must be non-null
    * @return number of words, 0 or higher
    */
   private static long countWords(String line) {
      long numWords = 0;
      int index = 0;
      boolean prevWhitespace = true;
      while (index < line.length()) {
         char c = line.charAt(index++);
         boolean currWhitespace = Character.isWhitespace(c);
         if (prevWhitespace && !currWhitespace) {
            numWords++;
         }
         prevWhitespace = currWhitespace;
      }
      return numWords;
   }

   //==============================================================
   public static void main(String[] args)
   {
      if (args.length == 0) {
         displayResults( "stdin", count("stdin", System.in));
      } 
      else {
         for (int i = 0; i < args.length; i++) {
            displayResults( args[i], count(args[i]));
         }
      }
   }
}

