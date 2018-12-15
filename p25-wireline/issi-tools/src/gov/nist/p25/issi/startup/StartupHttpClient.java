//
package gov.nist.p25.issi.startup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.List;
import javax.swing.JOptionPane;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.DaemonWebServerAddress;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.issiconfig.WebServerAddress;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

@SuppressWarnings("unused")
public class StartupHttpClient {
   
   private static Logger logger = Logger.getLogger(StartupHttpClient.class);
   public static void showln(String s) { System.out.println(s); }

   public static int PING_TIMEOUT = 2000;

   private boolean verbose = true;
   private String testerConfigFileName;
   private String systemConfigFileName;
   private String globalConfigFileName;
   private DietsConfigProperties startupProps;
   private ISSITesterConfiguration testerConfig;
   private TopologyConfig globalConfig;
   private HttpClient httpClient;

   private String sendHttpRequest(WebServerAddress webServerAddress,
         boolean resultFlag, NameValuePair[] nvPairs, String body)
         throws Exception
   {
      PostMethod method = new PostMethod(webServerAddress.getHttpControlUrl());

      String url = webServerAddress.getHttpControlUrl();
      logger.debug("StartupHttpClient- sendHttpRequest: url=" + url);
      if( verbose) {
         for( NameValuePair nv: nvPairs) {
           logger.debug("   NameValuePair: " + nv);
         }
      }
      logger.debug("body=" + body);

      method.setQueryString(nvPairs);
      if (body != null)
         method.setRequestBody(new ByteArrayInputStream(body.getBytes()));

      try {
         int rc = this.httpClient.executeMethod(method);
         if (rc != 200) {
            String retval = method.getResponseBodyAsString();

            logger.debug("StartupHttpClient: rc= " + rc);
            logger.debug("Failure reason = " + retval);
            throw new Exception("Unexpected error in sending request " + rc);
         }

         if (resultFlag) {
            String retval = method.getResponseBodyAsString();
            return retval;
         } else
            return null;
      } catch (Exception ex) {
         String msg = "A failure occured while completing the operation"
            + "\nurl: " + url
            + "\nPlease check if daemons are running at addresses/ports in\n"
            + testerConfigFileName
            + "\nStartupHttpClient reported error: "+ex.getMessage();
         logger.error(msg);
         JOptionPane.showMessageDialog(null,
            msg,
            "Error in communicating with Daemons",
            JOptionPane.ERROR_MESSAGE);
         throw ex;
      } finally {
         method.releaseConnection();
      }
   }

   // constructor
   public StartupHttpClient(String configFileName,
         ISSITesterConfiguration testerConfig, String systemConfigFileName,
         String globalConfigFileName, DietsConfigProperties startupProps,
         TopologyConfig globalConfig)
   {
      this.httpClient = new HttpClient();
      this.testerConfigFileName = configFileName;
      this.systemConfigFileName = systemConfigFileName;
      this.globalConfigFileName = globalConfigFileName;
      this.testerConfig = testerConfig;
      this.startupProps = startupProps;
      this.globalConfig = globalConfig;
   }

   public void shipConfigFile(DaemonWebServerAddress config, String filename)
      throws Exception
   {
      logger.debug("shipConfigFile(): filename="+filename);
      String text = FileUtility.loadFromFileAsString( filename);
      sendHttpRequest(config, false, new NameValuePair[] {
         new NameValuePair(ISSITesterConstants.COMMAND,
            ISSITesterConstants.UPDATE_CONFIGURATION_FILE),
         new NameValuePair( ISSITesterConstants.FILENAME_TO_UPDATE,
            filename) }, text);
   }

   public void shipConfigFilesList(List<String> filesList) throws Exception
   {
      logger.debug("shipConfigFilesList(): START...nfiles="+filesList.size());
      Exception nex = null;
      for( String filename: filesList) {

         for (DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses()) {
            if( !dwsa.isConformanceTester()) {
               logger.debug("shipConfigFilesList(): skipped=\n"+dwsa);
               continue;
            }
            //logger.debug("shipConfigFile(): daemon-config=\n"+dwsa);
            try {
               shipConfigFile( dwsa, filename);

            } catch(ConnectException ex2) {
               // the Emulated or Real RFSS may be offline 
               // relax the rule: just ignore the execption
               logger.debug("shipConfigFilesList: "+ex2.getMessage());
               logger.debug("shipConfigFilesList: host offline ? ignored: "+dwsa);
            } catch(Exception ex) {
               logger.debug("shipConfigFilesList: Failed to ship: "+filename+
                     " due to\n"+ex.getMessage());
               nex = ex;
               break;
            }
         }
         if( nex != null)
            break;
      } 
      logger.debug("shipConfigFilesList():  nfiles="+filesList.size());
      if( nex != null) {
         nex.printStackTrace();
         logger.debug("shipConfigFilesList(): nex="+nex);
         //XXX: on error just assume the host is down
         //throw nex;
//07/04/2009 enable to catch illegal RFSS Emulator
         throw nex;
      }
      logger.debug("shipConfigFilesList():  DONE...");
   }

   public void shipConfigFiles() throws Exception
   {
      logger.debug("shipConfigFiles(): START...");
      for (DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses()) {
         if( !dwsa.isConformanceTester()) {
            logger.debug("shipConfigFiles(): skipped=\n"+dwsa);
            continue;
         }
         logger.debug("shipConfigFiles(): daemon-config=\n"+dwsa);

         logger.debug("shipConfigFiles(): update testerConfigFileName="+testerConfigFileName);
         sendHttpRequest(dwsa, false, new NameValuePair[] {
            new NameValuePair(ISSITesterConstants.COMMAND,
               ISSITesterConstants.UPDATE_CONFIGURATION_FILE),
            new NameValuePair(ISSITesterConstants.FILENAME_TO_UPDATE,
               testerConfigFileName) }, testerConfig.toString());
//logger.debug("shipConfigFiles(): testerConfigFileName="+testerConfigFileName);

	 /***
         if (systemConfigFileName != null) {
            sendHttpRequest(dwsa, false, new NameValuePair[] {
               new NameValuePair(ISSITesterConstants.COMMAND,
                  ISSITesterConstants.UPDATE_CONFIGURATION_FILE),
               new NameValuePair(ISSITesterConstants.FILENAME_TO_UPDATE,
                  systemConfigFileName) }, globalConfig.exportSystemTopology());
	 }
         logger.debug("shipConfigFiles(): systemConfigFileName="+systemConfigFileName);
	  ***/

	 /***
         if (globalConfigFileName != null) {
            sendHttpRequest(dwsa, false, new NameValuePair[] {
               new NameValuePair(ISSITesterConstants.COMMAND,
                  ISSITesterConstants.UPDATE_CONFIGURATION_FILE),
               new NameValuePair( ISSITesterConstants.FILENAME_TO_UPDATE,
                  globalConfigFileName) }, globalConfig.exportGlobalTopology());
         }
         logger.debug("shipConfigFiles(): globalConfigFileName="+globalConfigFileName);
	 ***/

         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         startupProps.store(bos, "Shipped");
         byte bytes[] = bos.toByteArray();
         String propsString = new String(bytes);

         String startupFileName = startupProps.getFileName();
         logger.debug("shipConfigFiles(): update startupFile="+ startupFileName);
         sendHttpRequest(dwsa, false, new NameValuePair[] {
            new NameValuePair(ISSITesterConstants.COMMAND,
               ISSITesterConstants.UPDATE_CONFIGURATION_FILE),
            new NameValuePair(ISSITesterConstants.FILENAME_TO_UPDATE,
               startupFileName) }, propsString);
//logger.debug("shipConfigFiles(): startupFile="+ startupFileName);

      }   // for all-daemon
      logger.debug("shipConfigFiles(): DONE...");
   }

   public void sendRedrawCommand() throws Exception
   {
      logger.debug("sendRedrawCommand(): START...");
      sendRedrawCommand( ISSITesterConstants.STARTUP_PROPERTIES_FILENAME);
      logger.debug("sendRedrawCommand(): DONE...");
   }

   public void sendRedrawCommand(String startupFile) throws Exception
   {
      logger.debug("sendRedrawCommand(): START..."+startupFile);
      for (DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses()) {
         if( !dwsa.isConformanceTester()) {
            logger.debug("sendRedrawCommand(): skipped=\n"+dwsa);
            continue;
         }
         sendHttpRequest(dwsa, false, new NameValuePair[] {
            new NameValuePair(ISSITesterConstants.COMMAND,
               ISSITesterConstants.REDRAW),
            new NameValuePair( ISSITesterConstants.KEY_STARTUP_FILE,
               startupFile) }, null);
      }   
      logger.debug("sendRedrawCommand(): DONE...");
   }

   public void sendStaticReinitialization(String startupFile) throws Exception
   {
      logger.debug("sendStaticReinitialization(): START..."+startupFile);
      for (DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses()) {
         if( !dwsa.isConformanceTester()) {
            logger.debug("sendStaticReinitialization(): skipped=\n"+dwsa);
            continue;
         }
         sendHttpRequest(dwsa, false, new NameValuePair[] {
            new NameValuePair(ISSITesterConstants.COMMAND,
               ISSITesterConstants.STATIC_REINITIALIZATION),
            new NameValuePair( ISSITesterConstants.KEY_STARTUP_FILE,
               startupFile) }, null);
      }   
      logger.debug("sendStaticReinitialization(): DONE...");
   }

   public void sendSetConformanceTest(int index, String zclass) throws Exception {

      logger.debug("sendSetConformanceTest(): START...index="+index+" class="+zclass);
      for (DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses()) {
         if( !dwsa.isConformanceTester()) {
            logger.debug("sendSetConfirmanceTest(): skipped=\n"+dwsa);
            continue;
         }
	 // http://host:8763/diets/controller?command=setConformanceTest&testIndex=36&testClass=Conformance
//07/04/2009
        try {
         sendHttpRequest(dwsa, false, new NameValuePair[] {
            new NameValuePair(ISSITesterConstants.COMMAND,
               ISSITesterConstants.SET_CONFORMANCE_TEST),
            new NameValuePair( ISSITesterConstants.KEY_TEST_INDEX,
               Integer.toString(index)),
            new NameValuePair( ISSITesterConstants.KEY_TEST_CLASS,
               zclass) }, null);
        } catch(Exception ex) {
            logger.debug("sendSetConfirmanceTest(): "+ex);
            throw ex;
       	}
      }   // for-loop
      logger.debug("sendSetConformanceTest(): DONE...");
   }

   public void sendSaveTestFiles() throws Exception {
      logger.debug("sendSaveTestFiles(): START...");

      // http://host:8763/diets/controller?command=saveTestFiles
      for (DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses()) {
         if( !dwsa.isConformanceTester()) {
            logger.debug("sendSaveTestFiles(): skipped=\n"+dwsa);
            continue;
         }
         sendHttpRequest(dwsa, false, new NameValuePair[] {
            new NameValuePair(ISSITesterConstants.COMMAND,
               ISSITesterConstants.SAVE_TEST_FILES) }, null);
      }
      logger.debug("sendSaveTestFiles(): DONE...");
   }

   //--------------------------------------------------------------------------
   public void pingHostByIpAddress(String ipAddress, int timeout) throws Exception {

      logger.debug("pingHostByIpAddress(): host="+ipAddress +"  timeout="+timeout);
      if( !InetAddress.getByName( ipAddress).isReachable(timeout)) {
         String msg = "Unreachable host: "+ipAddress;
         logger.debug( msg);
         throw new IllegalArgumentException(msg);
      }
   }

   public void pingHostByIpAddress(List<String> ipList) throws Exception {
      for (String ip: ipList) {
         pingHostByIpAddress( ip, PING_TIMEOUT);
      }
   }
   public void pingHost() throws Exception {
      for (DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses()) {
         if( !dwsa.isConformanceTester()) {
            logger.debug("pingHost(): skipped=\n"+dwsa);
            continue;
         }
         // check if host is reachable !!!
         pingHostByIpAddress( dwsa.getIpAddress(), PING_TIMEOUT);
      }
   }

   //--------------------------------------------------------------------------
   public void startServices() throws Exception {

      for (DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses()) {
         if( !dwsa.isConformanceTester()) {
            logger.debug("startServices(): skipped=\n"+dwsa);
            continue;
         }

         // check if host is reachable !!!
         logger.debug("startServices(): host="+dwsa.getIpAddress() +"  timeout="+PING_TIMEOUT);
         if( !InetAddress.getByName(dwsa.getIpAddress()).isReachable(PING_TIMEOUT)) {
            logger.debug("startServices(): unreachable="+dwsa.getIpAddress());
            continue;
         }
	 // http://host:8763/diets/controller?command=startServices&testerDaemonTopology=testerconfig/emulator-configuration.xml
         sendHttpRequest(dwsa, false, new NameValuePair[] {
            new NameValuePair(ISSITesterConstants.COMMAND,
               ISSITesterConstants.START_SERVICES),
            new NameValuePair( ISSITesterConstants.TESTER_DAEMON_TOPOLOGY,
               testerConfigFileName) }, null);
      }
   }

   public void stopServices() throws Exception {
      for (DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses()) {
         if( !dwsa.isConformanceTester()) {
            logger.debug("stopServices(): skipped=\n"+dwsa);
            continue;
         }
         // check if host is reachable !!!
         if( !InetAddress.getByName(dwsa.getIpAddress()).isReachable(PING_TIMEOUT)) {
            logger.debug("startServices(): unreachable="+dwsa.getIpAddress());
            continue;
         }

	 // http://host:8763/diets/controller?command=stopServices
         sendHttpRequest(dwsa, false,
            new NameValuePair[] { new NameValuePair(
               ISSITesterConstants.COMMAND,
               ISSITesterConstants.STOP_SERVICES) }, null);
      }
   }

   // 14.4.x
   //-----------------------------------------------------------------
   public void sendHeartbeatQuery() throws Exception {
      for (DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses()) {
         if( !dwsa.isConformanceTester()) {
            logger.debug("sendHeartbeatQuery(): skipped=\n"+dwsa);
            continue;
         }
         // check if host is reachable !!!
         if( !InetAddress.getByName(dwsa.getIpAddress()).isReachable(PING_TIMEOUT)) {
            logger.debug("sendHeartbeatQuery(): unreachable="+dwsa.getIpAddress());
            continue;
         }
         //logger.debug("sendHeartbeatQuery(): send to dwsa=\n"+dwsa);

	 // http://host:8763/rfss/control?command=heartbeatQuery
         sendHttpRequest(dwsa, false, new NameValuePair[] {
             new NameValuePair(ISSITesterConstants.COMMAND,
               ISSITesterConstants.HEARTBEAT_QUERY) }, null);
      }
   }
}
