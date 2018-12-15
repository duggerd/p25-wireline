//
package gov.nist.p25.common.util;

import java.io.*;

//
// http://www.rgagnon.com/javadetails/java-0029.html
//
public class StackTraceUtility {


   public static String stack2string(Exception e) {
      try {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         return sw.toString();
      }
      catch(Exception e2) {
         return "stack2string: "+e2.toString();
      }
   }
     
   //========================================================================
   public static void main(String s[]){
      try {
         // force an exception for demonstration purpose
         Class.forName("unknown").newInstance();
         // or this could be changed to:
         //    throw new Exception();
     
      }
      catch (Exception e) {
         System.out.println(stack2string(e));
      }
   }
}
     
