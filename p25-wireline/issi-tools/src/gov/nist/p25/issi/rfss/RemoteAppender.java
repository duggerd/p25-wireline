//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This is an appender which can append messages to the TesterGUI
 * provided the TesterGUI registers itself as a remote monitor
 * with the front end.
 */
@SuppressWarnings("unused")
public class RemoteAppender implements Appender {
   private RfssConfig rfssConfig;
   private Filter filter;
   private ErrorHandler errorHandler;
   private Layout layout;
   private HttpClient httpClient;
   private String ipAddress;
   private int port;
   private PostMethod method;

   // constructor
   public RemoteAppender(RfssConfig rfssConfig, String ipAddress, int httpPort, Layout layout) {
      this.rfssConfig = rfssConfig;
      this.layout = layout;
      this.httpClient = new HttpClient();
      this.ipAddress = ipAddress;
      this.port = httpPort;
      method = new PostMethod("http://" + this.ipAddress + ":" + this.port + "/client/");
      method.setFollowRedirects(true);
      method.setStrictMode(false);
   }

   public void addFilter(Filter filter) {
      this.filter = filter;
   }

   public Filter getFilter() {
      return filter;
   }

   public void clearFilters() {
      this.filter = null;
   }

   public void close() {
   }

   public void doAppend(LoggingEvent loggingEvent) {
      if (filter != null && filter.decide(loggingEvent) == Filter.DENY)
         return;
      Object message = loggingEvent.getMessage();
      if (message == null)
         return;
      String messageStr = message.toString();
      String[] throwable = loggingEvent.getThrowableStrRep();

      StringBuffer sbuf = new StringBuffer();
      if (throwable != null) {
         for (String st : throwable) {
            sbuf.append(st + "\n");
         }
      }

      Level level = loggingEvent.getLevel();
      if (level.equals(Level.INFO)) {
         postMessage(rfssConfig.getDomainName() + " ["
               + rfssConfig.getHostPort() + "] " + messageStr,
               ISSITesterConstants.LOG_INFO);
      } else if (level.equals(Level.ERROR)) {
         postMessage(rfssConfig.getDomainName() + " ["
               + rfssConfig.getHostPort() + "] " + messageStr, 
               sbuf.toString(), ISSITesterConstants.LOG_ERROR);
      } else if (level.equals(Level.FATAL)) {
         postMessage(rfssConfig.getDomainName() + " ["
               + rfssConfig.getHostPort() + "] " + messageStr, 
               sbuf.toString(), ISSITesterConstants.LOG_FATAL);
      }
   }

   private void fillInMessage(String message, PostMethod method) throws Exception {
      method.setRequestBody(message);
      method.setRequestContentLength(message.length());
   }

   private void postMessage(String message, String exception, String command) {
      try {
         NameValuePair[] nvPairs = new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND, command) };
         method.setQueryString(nvPairs);

         // Encode the request into XML
         fillInMessage(message + "\n" +  exception,  method);

         int sc = httpClient.executeMethod(method);
         if (sc != 200) {
            System.out.println("HTTP Error returned from remote logger " + sc);
         }

      } catch (Exception ex) {
         // Ignore the IO Exception -- the monitor may be off line.
         System.out.println("Exception occured -- monitor may be offline");
         ex.printStackTrace();
         Logger logger = Logger.getLogger("gov.nist.p25");
         logger.removeAppender(this);
         method.releaseConnection();
      }
   }

   /**
    * Post a message to all the registered monitors.
    */
   public void postMessage(String message, String command) {
      try {
         method = new PostMethod("http://" + ipAddress + ":" + port + "/client/");
         NameValuePair[] nvPairs = new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND, command) ,
               };
         method.setQueryString(nvPairs);
         fillInMessage(message,  method);
         //method.setRequestContentLength(message.length());
         //method.setRequestBody(message);
         method.setFollowRedirects(true);
         method.setStrictMode(false);
         int sc = httpClient.executeMethod(method);
         //System.out.println("Status code = " + sc);

      } catch (Exception ex) {
         // Ignore the IO Exception -- the monitor may be off line.
         System.out.println("IOException -- monitor may be offline" + ex);
         Logger logger = Logger.getLogger("gov.nist.p25");
         logger.removeAppender(this);
         method.releaseConnection();
      }
   }

   public void setName(String name) {
   }
   public String getName() {
      return null;
   }

   public void setErrorHandler(ErrorHandler errorHandler) {
      this.errorHandler = errorHandler;
   }
   public ErrorHandler getErrorHandler() {
      return errorHandler;
   }

   public void setLayout(Layout layout) {
      this.layout = layout;
   }
   public Layout getLayout() {
      return layout;
   }

   public boolean requiresLayout() {
      return false;
   }
}
