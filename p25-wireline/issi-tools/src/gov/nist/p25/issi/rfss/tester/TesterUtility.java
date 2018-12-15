//
package gov.nist.p25.issi.rfss.tester;

//import gov.nist.p25.issi.constants.ISSITesterConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.log4j.Logger;


/**
 * Tester Utility
 */
public class TesterUtility {
   
   private static Logger logger = Logger.getLogger(TesterUtility.class);

   /**
    * Print writer used to write out rtp records
    */
   private static PrintWriter rtpPrintWriter;
   private static File msgLogFile = null;
   private static FileWriter rtpMessageLogFileWriter;

   public synchronized static PrintWriter getRtpMessageLogStream() {
      try {
         if (rtpPrintWriter != null) {
            return rtpPrintWriter;
         } else if (TestScript.rtpMessagelog != null) {
            msgLogFile = new File(TestScript.rtpMessagelog);
            rtpMessageLogFileWriter = new FileWriter(msgLogFile);
            rtpPrintWriter = new PrintWriter(rtpMessageLogFileWriter);
            return rtpPrintWriter;
         } else
            return null;
      } catch (Exception ex) {
         return null;
      }
   }

   public static void closeRtpMessageLogStream() {
      try {
         if (rtpPrintWriter != null) {
            rtpMessageLogFileWriter.close();
            rtpPrintWriter.close();
            // msgLogFile.delete();
            rtpPrintWriter = null;
            rtpMessageLogFileWriter = null;
         }
      } catch (Exception ex) {
         logger.error("Unexpected exception", ex);
      }
   }
} 
