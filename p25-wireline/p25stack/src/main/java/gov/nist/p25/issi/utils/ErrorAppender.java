package gov.nist.p25.issi.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

//import gov.nist.p25.issi.constants.ISSITesterConstants;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

public class ErrorAppender extends org.apache.log4j.RollingFileAppender {

   //private static String fileName = ISSITesterConstants.ERROR_LOG;
   public static final String ERROR_LOG = "logs/errorlog.txt";
   private static String fileName = ERROR_LOG;


   private static void writeErrorLog(String logToWrite) {
      try {
         File file = new File(fileName);
         if (!file.exists()) {
            file.createNewFile();
         }
         PrintWriter pw = new PrintWriter(new FileWriter(file));
         pw.write(logToWrite);
         pw.close();
         
      } catch (Exception ex) {
         // nothing to do ?
      }
   }

   @Override
   public void doAppend(LoggingEvent loggingEvent) {

      Level level = loggingEvent.getLevel();
      if (level.equals(Level.ERROR) || level.equals(Level.FATAL)) {
         super.doAppend(loggingEvent);
         String messageToWrite = loggingEvent.getRenderedMessage();
         writeErrorLog(messageToWrite);

      }
   }

   @Override
   protected void subAppend(LoggingEvent loggingEvent) {
      Level level = loggingEvent.getLevel();
      if (level.equals(Level.ERROR) || level.equals(Level.FATAL)) {
         super.subAppend(loggingEvent);
         String messageToWrite = loggingEvent.getRenderedMessage();
         writeErrorLog(messageToWrite);
      }

   }
}
