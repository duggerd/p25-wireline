//
package gov.nist.p25.issi.packetmonitor.gui;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.PacketMonitorWebServerAddress;
import gov.nist.p25.issi.issiconfig.WebServerAddress;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfigurationParser;

import javax.swing.JOptionPane;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

/**
 * Remote controller for the packet monitor.
 */
public class RemotePacketMonitorController implements PacketMonitorController {
   
   private static Logger logger = Logger.getLogger(RemotePacketMonitorController.class);
   
   private ISSITesterConfiguration issiHttpServerConfig;
   private HttpClient httpClient;
   private String configFileName;
   private String systemConfigFile;
   
   // constructor
   public RemotePacketMonitorController(String configFileName, 
         String systemConfigFile) throws Exception {

      // this.startWebServer();
      this.issiHttpServerConfig = new ISSITesterConfigurationParser(
            configFileName).parse();
      this.httpClient = new HttpClient();
      //this.gui = gui;
      this.configFileName = configFileName;
      this.systemConfigFile = systemConfigFile;
   }
   
   /**
    * This method sends a command to a tester by encoding the body in XML and
    * returns a result.
    * 
    * @param webServerAddress --
    *            the configuration to which the request is headed.
    * @param resultFlag --
    *            true if a result is returned.
    * @param command
    * @return -- the return object if one exists or null.
    * 
    * @throws Exception
    */
   private String sendHttpRequest(WebServerAddress webServerAddress,
         boolean resultFlag, NameValuePair[] nvPairs) throws Exception {

      PostMethod method = new PostMethod(webServerAddress.getHttpControlUrl());
      logger.debug("sending http request to URL " + webServerAddress.getHttpControlUrl());
      method.setQueryString(nvPairs);
      try {
         int rc = this.httpClient.executeMethod(method);
         if (rc != 200) {
            String retval = method.getResponseBodyAsString();

            System.out.println("RemotePacketMonitorController: rc= " + rc);
            logger.error("Failure reason = " + retval);
            throw new Exception("Unexpected error in sending request " + rc);
         }

         if (resultFlag) {
            String retval = method.getResponseBodyAsString();
            return retval;
         } else
            return null;
      } catch (Exception ex) {
         errorPopup("A failure occured communicating with the tester\n"
            + "Please check if the tester daemons are running at \n"
            + " the addresses and ports specified in "
            + configFileName);
         throw ex;
      } finally {
         method.releaseConnection();
      }
   }
   
   public void errorPopup(String errorMessage) {
      JOptionPane.showMessageDialog(null,
         errorMessage,
         "Communication Error",
         JOptionPane.WARNING_MESSAGE);
   }

   public String fetchSipTraces() throws Exception {
      StringBuilder sbuf = new StringBuilder();
      sbuf.append("<messages>\n");
      for (PacketMonitorWebServerAddress pmc: issiHttpServerConfig.getPacketMonitors()) {
         String host = pmc.getIpAddress();
         int port = pmc.getHttpPort();
         String url = "http://" + host + ":" + port + "/sniffer/siptrace";

         logger.info("getting from URL : " + url);
         GetMethod method = new GetMethod(url);
         int rc = httpClient.executeMethod(method);
         String trace = method.getResponseBodyAsString();
         if (rc != 200) {
            throw new Exception("Unexpected return code " + rc);
         }
         sbuf.append(trace);
      }
      sbuf.append("\n</messages>\n");
      String sipTraces = sbuf.toString();

      FileUtility.saveToFile("logs/sipTraces.txt", sipTraces);
      return sipTraces;
   }
   
   public String fetchPttTraces() throws Exception {
      StringBuilder sbuf = new StringBuilder();
      sbuf.append("<pttmessages>\n");
      for (PacketMonitorWebServerAddress pmc: issiHttpServerConfig.getPacketMonitors()) {
         String host = pmc.getIpAddress();
         int port = pmc.getHttpPort();
         String url = "http://" + host + ":" + port + "/sniffer/ptttrace";

         logger.info("getting from URL : " + url);
         GetMethod method = new GetMethod(url);
         int rc = httpClient.executeMethod(method);
         String trace = method.getResponseBodyAsString();

         if (rc != 200) {
            logger.error("Unexpected return retrieving  PTT trace." + rc);
            throw new Exception("Unexpected return code " + rc);
         }
         sbuf.append(trace);

      }
      sbuf.append("\n</pttmessages>\n");
      String pttTraces = sbuf.toString();

      FileUtility.saveToFile("logs/pttTraces.txt", pttTraces);
      return pttTraces;
   }
   
   public String fetchAllTraces() throws Exception {
      // NOT supportted yet
      return "<allmessages></allmessages>";
   }

   /**
    * get measurement result string from PacketMonitor
    */
   public String fetchResult() throws Exception {

      StringBuilder sbuf = new StringBuilder();
      sbuf.append("<messages>\n");
      for (PacketMonitorWebServerAddress pmc: issiHttpServerConfig.getPacketMonitors()) {
         String host = pmc.getIpAddress();
         int port = pmc.getHttpPort();
         String url = "http://" + host + ":" + port + "/sniffer/result";

         logger.info("getting from URL : " + url);
         GetMethod method = new GetMethod(url);
         int rc = this.httpClient.executeMethod(method);
         String trace = method.getResponseBodyAsString();
         if (rc != 200) {
            throw new Exception("Unexpected return code " + rc);
         }
         sbuf.append(trace);
      }
      sbuf.append("\n</messages>\n");
      String result = sbuf.toString();

      FileUtility.saveToFile("logs/result.txt", result);
      return result;
   }

   public void startCapture() throws Exception {
      for (PacketMonitorWebServerAddress config : issiHttpServerConfig.getPacketMonitors()) {
         sendHttpRequest(config, false,
            new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND,
               ISSITesterConstants.START_PACKET_MONITOR),
               new NameValuePair (ISSITesterConstants.SYSTEM_TOPOLOGY,systemConfigFile)});
      }
   }

   @Override
   public boolean fetchErrorFlag() throws Exception {
      NameValuePair[] nvPairs = new NameValuePair[] {
         new NameValuePair ( 
            ISSITesterConstants.COMMAND,
            ISSITesterConstants.GET_ERROR_FLAG) 
      };
      for (PacketMonitorWebServerAddress pmc : issiHttpServerConfig.getPacketMonitors()) {
         String retval = sendHttpRequest(pmc, true, nvPairs);
         if ( "true".equals(retval))
            return true;
      }
      return false;
   }

   @Override
   public String fetchErrorString() throws Exception {
      NameValuePair[] nvPairs = new NameValuePair[] {
            new NameValuePair ( 
            ISSITesterConstants.COMMAND,
            ISSITesterConstants.GET_ERROR_LOG) 
      };

      StringBuffer sbuf = new StringBuffer();
      sbuf.append("----------------------\n");
      for (PacketMonitorWebServerAddress pmc: issiHttpServerConfig.getPacketMonitors()) {
         String retval = sendHttpRequest(pmc, true, nvPairs);
         sbuf.append(pmc.getIpAddress() + ":" + pmc.getHttpPort() + "\n");
         sbuf.append(retval);
      }
      return sbuf.toString();
   }
}
