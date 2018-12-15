//
package gov.nist.p25.issi.testlauncher;

import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.PacketMonitorWebServerAddress;
import gov.nist.p25.issi.issiconfig.WebServerAddress;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;

import java.net.ConnectException;
import javax.swing.JOptionPane;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

/**
 * This is the remote test controller. This controller can launch a test via 
 * HTTP. It can gather the results via RMI or HTTP from each of the RFSSs
 * involved in the test case.
 */
public class RemoteTestController implements TestControllerInterface {

   private static Logger logger = Logger.getLogger(RemoteTestController.class);
   public static void showln(String s) { System.out.println(s); }
   
   private boolean verbose = false;
   private TestStatusInterface gui;
   private HttpClient httpClient;
   private ISSITesterConfiguration issiTesterConfig;

   public TestStatusInterface getTestStatusInterface() {
      return gui;
   }

   /*
    * private void registerForRemoteNotification() throws Exception { for
    * (ISSITesterWebServerConfig rfssConfig : issiTesterConfig
    * .getTestLauncherConfigurations()) {
    * 
    * RemoteMonitorParameters remoteMonitor = new RemoteMonitorParametersImpl();
    * remoteMonitor.setIpAddress(ipAddress);
    * remoteMonitor.setPort(httpPort); NameValuePair[] nvPairs = new
    * NameValuePair[] { new NameValuePair( ISSITesterConstants.COMMAND,
    * ISSITesterConstants.REGISTER_REMOTE_MONITOR) };
    * sendHttpRequest(rfssConfig, remoteMonitor, false, nvPairs); } }
    */

   /**
    * The constructor. Register myself with each of the emulated RFSSs. Make
    * sure that each emulated RFSS has the same set of test cases. This talks
    * to each of the RFSSs using HTTP to get the registered test cases.
    * 
    * @param gui - the GUI.
    * @throws Exception
    */
   public RemoteTestController(TestStatusInterface gui) throws Exception {
      this.gui = gui;      
      httpClient = new HttpClient();
      issiTesterConfig = gui.getIssiTesterConfiguration();
   }

   /**
    * Client side web server for posting results. Enable this code if you want
    * to have the testers post results to the Client. Starting the web server
    * will give you logging on the browser window but it will not work across
    * the network. Hence this is only for the local case.
    * 
    * private void startWebServer() throws Exception { httpServer = new
    * Server();
    * 
    * SocketListener socketListener = new SocketListener();
    * socketListener.setMaxIdleTimeMs(100); socketListener.setMaxThreads(16);
    * socketListener.setMinThreads(3); socketListener.setHost(ipAddress);
    * httpPort = 9090; // Need to randomize
    * socketListener.setPort(httpPort);
    * httpServer.addListener(socketListener); // Set up a file handler to
    * export class files to the rmiregistry. HttpContext context = new
    * HttpContext(); context.setContextPath("/client/*"); ServletHandler
    * srevletHandler = new ClientServletHandler(this); srevletHandler
    * .addServlet("logger", "/*", ClientServlet.class.getName());
    * context.addHandler(srevletHandler); // Finally start the HTTP server.
    * httpServer.addContext(context);
    * 
    * httpServer.start(); }
    */

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

      String url = webServerAddress.getHttpControlUrl();
      if( verbose) {
         logger.debug("RemoteTestController- sendHttpRequest: " + url);
         for( NameValuePair nv: nvPairs) {
           logger.debug("   NameValuePair: " + nv);
         }
      }
      method.setQueryString(nvPairs);

      try {
         int rc = httpClient.executeMethod(method);
         if (rc != 200) {
            String retval = method.getResponseBodyAsString();

            logger.debug("RemoteTestController: rc= " + rc);
            logger.debug("Failure reason = " + retval);
            throw new Exception("Error in sending request " + rc +
                  "\nFaileure reason: "+retval);
         }

         if (resultFlag) {
            String retval = method.getResponseBodyAsString();
            return retval;
         } else
            return null;
         
      } catch (ConnectException ex) {
         String msg = "Error in connecting to "+ url
            + "\nPlease check if tester daemons are running at the addresses\n"
            + "and ports specified in " + gui.getWebConfigurationFileName()
            + "\nError message: "+ex.getMessage();
         errorPopup(msg);
         logError(msg);
         throw ex;

      } catch (Exception ex) {
         String msg = "A failure occured while completing the operation\n"
            + "Please check if tester daemons are running at the addresses\n"
            + "and ports specified in "
            + gui.getWebConfigurationFileName()
            + "\nMake sure you generate traces before running test !"
            + "\nRemoteTestController reported error: "+ex.getMessage();
         errorPopup(msg);
         logError(msg);
         throw ex;
      } finally {
         method.releaseConnection();
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestController#getSipTraces()
    */
   public String getSipTraces() throws Exception {
      
      if ( issiTesterConfig.getPacketMonitors().size() == 0) 
         throw new Exception("Packet monitors are not configured!");

      StringBuffer sbuf = new StringBuffer();
      for (PacketMonitorWebServerAddress pmc: issiTesterConfig.getPacketMonitors()) {
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
      return sbuf.toString();
   }

   /**
    * This method is called in interactive mode to tell the tester to run to
    * the given scenario.
    * 
    * @param scenario
    * @throws Exception
    */
   public void signalNextScenario(String scenario) throws Exception {

      for (WebServerAddress config: issiTesterConfig.getEmulatorConfigurations()) {
         sendHttpRequest(config, false, new NameValuePair[] {
            new NameValuePair(ISSITesterConstants.COMMAND,
               ISSITesterConstants.EXECUTE_NEXT_SCENARIO),
            new NameValuePair(ISSITesterConstants.SCENARIO, scenario)
         });
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestController#tearDownCurrentTest()
    */

   public void tearDownCurrentTest() throws Exception {
      // http://host:8763/rfss/control?command=tear-down-test
      for (WebServerAddress config: issiTesterConfig.getEmulatorConfigurations()) {
         sendHttpRequest(config, false,
            new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND,
               ISSITesterConstants.TEAR_DOWN_TEST)
            });
      }
   }

   public boolean isTestCompleted() throws Exception {

      boolean b = true;
      for (WebServerAddress config: issiTesterConfig.getEmulatorConfigurations()) {
         String retval = sendHttpRequest(config, true,
            new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND,
               ISSITesterConstants.IS_TEST_COMPLETED)
            });
         b &= "true".equals(retval);
      }
      return b;
   }

   public boolean isSaveTraceCompleted() throws Exception {
      // for now just use the TestCompleted
      return isTestCompleted();
   }
   
   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestController#signalTestCompletion()
    */
   public void signalTestCompletion() throws Exception {

      for (WebServerAddress config: issiTesterConfig.getEmulatorConfigurations()) {
         String retval = sendHttpRequest(config, false,
            new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND,
               ISSITesterConstants.TEST_COMPLETED)
            });
         logger.info("signalTestCompletion: retval=" + retval);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestController#getTestResults()
    */
   public boolean getTestResults() throws Exception {
      boolean pass = true;
      boolean checked = false;
      for (WebServerAddress config: issiTesterConfig.getEmulatorConfigurations()) {
         String retval = sendHttpRequest(config, true,
            new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND,
               ISSITesterConstants.GET_TEST_RESULTS)
            });
         if (ISSITesterConstants.FAILED.equals(retval)) {
            pass = false;
         }
	 checked = true;
      }
      if( !checked) pass = false;
      return pass;
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestController#getRfssPttSessionInfo()
    */
   public String getRfssPttSessionInfo(boolean header) throws Exception {

      StringBuffer toParse = new StringBuffer();
      if(header)
         toParse.append("<issi-runtime-data>\n");
      for (WebServerAddress ws: issiTesterConfig.getEmulatorConfigurations()) {
         String info = sendHttpRequest(ws, true,
            new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND,
               ISSITesterConstants.GET_RFSS_STATUS_INFO)
            });
         toParse.append(info);
      }
      if(header)
         toParse.append("\n</issi-runtime-data>");
      return toParse.toString();
   }

   /**
    * Retrieve the captured trace from the stack. This contacts the server
    * using http and gets the log from there. Note that each emulator generates
    * the trace locally so you need only get a single trace.
    * 
    */
   public String getStackSipLog() {

      StringBuffer sbuf = new StringBuffer();
      WebServerAddress ws = issiTesterConfig.getEmulatorConfigurations().iterator().next();
      try {
         String msg = sendHttpRequest(ws, true,
            new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND,
               ISSITesterConstants.GET_SIP_TRACE)
            });
         sbuf.append(msg);
      } catch (Exception ex) {
         logger.error("Unexpected exception getting trace ", ex);
      }
      return sbuf.toString();
   }

   public String getStackSipLogs() {
      StringBuffer sbuf = new StringBuffer();
      for (WebServerAddress ws: issiTesterConfig.getEmulatorConfigurations()) {
         try {
            String msg = sendHttpRequest(ws, true,
               new NameValuePair[] { new NameValuePair(
                  ISSITesterConstants.COMMAND,
                  ISSITesterConstants.GET_SIP_TRACE)
            });
            sbuf.append(msg);
         } catch (Exception ex) {
            logger.error("Unexpected exception getting trace ", ex);
         }
      }
      return sbuf.toString();
   }

   /**
    * Get the logged PTT traces from a single RFSS ( for self test ).
    * 
    * @return -- a String containing the logged PTT traces.
    */
   public String getStackPttLog() {

      boolean gotTrace = false;

      StringBuffer sbuf = new StringBuffer();
      WebServerAddress ws = issiTesterConfig.getEmulatorConfigurations().iterator().next();
      try {
         String msg = sendHttpRequest(ws, true,
            new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND,
               ISSITesterConstants.GET_PTT_TRACE)
            });
         sbuf.append(msg);
         gotTrace = true;
         logger.debug("RemoteTestController: getStackPttLog() msg=\n"+msg);
      }
      catch (Exception ex) {
         logger.error("Unexpected exception getStackPttog: ", ex);
      }

      if (!gotTrace)
         return "";    // M1013 null -> ""
      else
         return sbuf.toString();
   }

   public String getStackPttLogs() {
      boolean gotTrace = false;
      StringBuffer sbuf = new StringBuffer();
      for (WebServerAddress ws: issiTesterConfig.getEmulatorConfigurations()) {
         try {
            String msg = sendHttpRequest(ws, true,
               new NameValuePair[] { new NameValuePair(
                  ISSITesterConstants.COMMAND,
                  ISSITesterConstants.GET_PTT_TRACE)
               });
            sbuf.append(msg);
            gotTrace = true;
         }
	 catch (Exception ex) {
            logger.error("Unexpected exception getStackPttLogs: ", ex);
         }
      }

      logger.debug("RemoteTestController: ws.getStackPttLogs(): sbuf=\n"+sbuf.toString());
      if (!gotTrace)
         return null;
      else
         return sbuf.toString();
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestController#getPttTraces()
    */
   public String getPttTraces() throws Exception {

      if ( issiTesterConfig.getPacketMonitors().size() == 0 ) 
         throw new Exception ("Packet monitors are not running!");
      
      StringBuffer sbuf = new StringBuffer();
      for (PacketMonitorWebServerAddress pmc : issiTesterConfig.getPacketMonitors()) {
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
      return sbuf.toString();
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestController#loadTest(java.lang.String,
    *      boolean)
    */
   public void loadTest(String scenarioDir, String scenario, String testNumber,
         String topology, String globalTopology, String systemTopology,
         boolean interactiveFlag, boolean evaluatePassFail)
      throws Exception {
      
      String interactive = interactiveFlag ? "true" : "false";
      //issiTesterConfig.printPacketMonitorTable();
      if (interactiveFlag) {
         for (PacketMonitorWebServerAddress config: issiTesterConfig.getPacketMonitors()) {
            sendHttpRequest(config, false,
               new NameValuePair[] { new NameValuePair(
                  ISSITesterConstants.COMMAND,
                  ISSITesterConstants.START_PACKET_MONITOR),
                  new NameValuePair (ISSITesterConstants.SYSTEM_TOPOLOGY,systemTopology)
               });
         }
      }
      for (WebServerAddress config: issiTesterConfig.getEmulatorConfigurations()) {
         sendHttpRequest(config, false, new NameValuePair[] {
            new NameValuePair(ISSITesterConstants.COMMAND, ISSITesterConstants.LOAD),
            new NameValuePair (ISSITesterConstants.SCENARIO_DIR_KEY, scenarioDir),
            new NameValuePair(ISSITesterConstants.TEST_CASE, scenario),
            new NameValuePair(ISSITesterConstants.TEST_NUMBER,testNumber),
            new NameValuePair(ISSITesterConstants.TEST_TOPOLOGY, topology),
            new NameValuePair(ISSITesterConstants.GLOBAL_TOPOLOGY, globalTopology),
            new NameValuePair(ISSITesterConstants.SYSTEM_TOPOLOGY, systemTopology),
            new NameValuePair(ISSITesterConstants.INTERACTIVE, interactive),
            new NameValuePair(ISSITesterConstants.EVALUATE_POST_CONDITION,
               Boolean.toString(evaluatePassFail))
         });
      }
   }

   /**
    * Run the previously loaded test. Note that this has to be a separate
    * action than loadTest because all RFSSs start execution at the same time.
    * 
    * @throws Exception
    */
   public void runTest() throws Exception {
      for (WebServerAddress config: issiTesterConfig.getEmulatorConfigurations()) {
         sendHttpRequest(config, false,
            new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND,
               ISSITesterConstants.RUN)
            });
      }
   }

   /**
    * Send command to the emulated RFSSs to exit.
    */
   public void sendExitCommand() {
      for (WebServerAddress rfssConfig: issiTesterConfig.getEmulatorConfigurations()) {
         try {
            sendHttpRequest(rfssConfig, false,
               new NameValuePair[] { new NameValuePair(
                  ISSITesterConstants.LOAD,
                  ISSITesterConstants.EXIT_EMULATOR)
               });
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
   }

   public void logError(String failureMessage, String errorTrace) {
      gui.logError(failureMessage, errorTrace);
   }

   public void errorPopup(String errorMessage) {
      JOptionPane.showMessageDialog(null, 
            errorMessage,
            "Communication Error", 
            JOptionPane.ERROR_MESSAGE);
   }

   public void logError(String failureMessage) {
      gui.logError(failureMessage);
   }

   public void reset() {
      httpClient = new HttpClient();
   }

   public String getErrorLog() throws Exception {
      StringBuffer sbuf = new StringBuffer();
      for (WebServerAddress ws: issiTesterConfig.getEmulatorConfigurations()) {
         try {
            String msg = sendHttpRequest(ws, true,
               new NameValuePair[] { new NameValuePair(
                  ISSITesterConstants.COMMAND,
                  ISSITesterConstants.GET_ERROR_LOG)
               });
            sbuf.append(msg);
         } catch (Exception ex) {
            logger.error("Unexpected exception getting trace ", ex);
         }
      }
      return sbuf.toString();
   }

//TODO: this is NOT tested !!!
   //-------------------------------------------------------------
   public String getRfssConfigs() throws Exception {
      logger.debug("RemoteTestController- getRfssConfigs:...");
      StringBuffer sbuf = new StringBuffer();
      for (WebServerAddress ws: issiTesterConfig.getEmulatorConfigurations()) {
         try {
            String info = sendHttpRequest(ws, true,
               new NameValuePair[] { new NameValuePair(
                  ISSITesterConstants.COMMAND,
                  ISSITesterConstants.GET_RFSS_CONFIG)
               });
            sbuf.append(info);
         } catch (Exception ex) {
            logger.error("Unexpected exception gett_rfss_config", ex);
         }
      }
      logger.debug("RemoteTestController- getRfssConfigs:\n"+sbuf.toString());
      return sbuf.toString();
   }
}
