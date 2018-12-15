//
package gov.nist.p25.issi.testlauncher;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.common.util.IpAddressUtility;
import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.GlobalTopologyParser;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfigurationParser;
import gov.nist.p25.issi.startup.ConfigurationServlet;
import gov.nist.p25.issi.startup.StartupHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

import org.mortbay.http.HttpContext;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;


@SuppressWarnings("unused")
public class DietsService {

   private static Logger logger = Logger.getLogger(DietsService.class);
   public static void showln(String s) { System.out.println(s); }

   private static boolean serviceStarted;
   private static boolean daemonRunning;
   private static int httpPort = ISSITesterConstants.DEFAULT_DIETS_HTTP_PORT;
   private static Server httpServer;
   private static HttpContext httpContext;

   private SocketListener socketListener;
   private DietsConfigProperties configProperties;
   private Properties localhostProperties;
   private ISSITesterConfiguration testerConfig;
   private TopologyConfig topologyConfig;
   private TestExecutionPanel testExecutionPanel;

   // accessors
   public TestExecutionPanel getTestExecutionPanel() {
      return testExecutionPanel;
   }

   public String getStartupFile() {
      return configProperties.getFileName();
   }

   //------------------------------------------------------------------------
   public String getTesterTopologyFileName() {
      return configProperties.getProperty(DietsConfigProperties.DAEMON_CONFIG_PROPERTY);
   }
   public void setTesterTopologyFileName(String testerTopologyFileName) {
      if (testerTopologyFileName != null) {
         configProperties.setProperty(DietsConfigProperties.DAEMON_CONFIG_PROPERTY,
             testerTopologyFileName);
      } else {
         configProperties.remove(DietsConfigProperties.DAEMON_CONFIG_PROPERTY);
      }
   }

   public String getSystemTopologyFileName() {
      return configProperties.getProperty(DietsConfigProperties.SYSTEM_TOPOLOGY_PROPERTY);
   }
   public void setSystemTopologyFileName(String systemTopologyFileName) {
      if (systemTopologyFileName != null) {
         configProperties.setProperty(DietsConfigProperties.SYSTEM_TOPOLOGY_PROPERTY,
            systemTopologyFileName);
      } else {
         configProperties.remove(DietsConfigProperties.SYSTEM_TOPOLOGY_PROPERTY);
      } 
   }
   
   public String getGlobalTopologyFileName() {
      String testSuiteName = getTestSuiteName();
      if (testSuiteName != null)
         return ISSITesterConstants.getGlobalTopologyFileName(testSuiteName);
      else
         return null;
   }

   public String getTestSuiteName() {
      return configProperties.getProperty(DietsConfigProperties.TESTSUITE_PROPERTY);
   }
   public void setTestSuiteName(String testSuiteName) {
      if (testSuiteName != null) {
         configProperties.setProperty(DietsConfigProperties.TESTSUITE_PROPERTY,
            testSuiteName);
      } else {
         configProperties.remove(DietsConfigProperties.TESTSUITE_PROPERTY);
      }
   }

   public static void saveTestFiles(boolean verbose) throws Exception {
      TesterHelper.saveFiles(verbose);
   }

   //------------------------------------------------------------------------
   public StartupHttpClient getStartupHttpClient()
   {
      StartupHttpClient sc = new StartupHttpClient(
            getTesterTopologyFileName(),
            testerConfig,
            getSystemTopologyFileName(),
            getGlobalTopologyFileName(),
            configProperties,
            topologyConfig);
      return sc;
   }

   public void shipConfigFilesList(List<String> filesList) throws Exception
   {
      logger.debug("DietsService: shipConfigFilesList(): size="+filesList.size());
      try {
         StartupHttpClient sc = getStartupHttpClient();
         sc.shipConfigFilesList( filesList);
      }
      catch (Exception ex) {
         logger.error("shipConfigFilesList: fail to ship config files", ex);
         throw ex;
      }
   } 

   public void shipConfigFiles() throws Exception
   {
      logger.debug("DietsService: shipConfigFiles()");
      try {
         StartupHttpClient sc = getStartupHttpClient();
         sc.shipConfigFiles();
         sc.sendRedrawCommand( testExecutionPanel.getStartupPropertiesFileName());
      }
      catch (Exception ex) {
         logger.error("shipConfigFiles: fail to ship config files", ex);
         throw ex;
      }
   } 

   public void sendStaticReinitialization() throws Exception
   {
      logger.debug("DietsService: sendStaticReinitialization()");
      try {
         StartupHttpClient sc = getStartupHttpClient();
         sc.sendStaticReinitialization( testExecutionPanel.getStartupPropertiesFileName());
      }
      catch (Exception ex) {
         logger.error("sendStaticReinitialization: ", ex);
         throw ex;
      }
   } 

   /**
   public void sendSetConformanceTest(int index) throws Exception
   {
      // default to CONFORMANCE vs CAP
      sendSetConformanceTest(index, ISSITesterConstants.TEST_CLASS_CONFORMANCE);
   } 
    **/
   public void sendSetConformanceTest(int index, String zclass) throws Exception
   {
      logger.debug("DietsService: sendSetConformanceTest(): index="+index+" class="+zclass);
      try {
         StartupHttpClient sc = getStartupHttpClient();
         // handle long timeout 
         sc.pingHost();
         sc.sendSetConformanceTest(index, zclass);
      }
      catch (Exception ex) {
         logger.error("sendSetConformanceTest: fail to set test: ", ex);
         throw ex;
      }
   } 

   public void sendPingHost() throws Exception
   {
      logger.debug("DietsService: sendPingHost(): ...");
      try {
         StartupHttpClient sc = getStartupHttpClient();
         sc.pingHost();
      }
      catch (Exception ex) {
         logger.error("sendPingHost: fail to reach host: ", ex);
         throw ex;
      }
   } 

   //------------------------------------------------------------------------
   public void sendSaveTestFiles() throws Exception
   {
      logger.debug("DietsService: sendSaveTestFiles()");
      try {
         StartupHttpClient sc = getStartupHttpClient();
         sc.sendSaveTestFiles();
      }
      catch (Exception ex) {
         logger.error("sendSaveTestFiles: ", ex);
         throw ex;
      }
   } 

   //------------------------------------------------------------------------
   public void startTestServices() throws Exception
   {
      logger.debug("DietsService: startTestServices()");
      if( serviceStarted) {
         logger.debug("DietsService: startTestServices() already started");
         return;
      }
      try {
         StartupHttpClient sc = getStartupHttpClient();

         // ping host before shipping files
         sc.pingHost();

         //logger.debug("DietsService: shipConfigFiles....");
         //sc.shipConfigFiles();
         sc.startServices();
         serviceStarted = true;
      }
      catch (Exception ex) {
         logger.error("startTestServices: fail to ship config files", ex);
         throw ex;
      }
   } 

   public void stopTestServices()
   {
      logger.debug("DietsService: stopTestServices()");
      if( !serviceStarted)
         return;
      try {
         StartupHttpClient sc = getStartupHttpClient();
         sc.stopServices();
         serviceStarted = false;
      }
      catch (Exception ex) {
         logger.error("stopTestServices: fail to stop services", ex);
      }
   }

   // 14.4.x
   //------------------------------------------------------------------------
   public void sendHeartbeatQuery() {
      logger.debug("DietsService: sendHeartbeatQuery()");
      try {
         StartupHttpClient sc = getStartupHttpClient();
         sc.sendHeartbeatQuery();
      }
      catch (Exception ex) {
         logger.error("sendHeartbeatQuery: ", ex);
         //throw ex;
      }
   }  

   //------------------------------------------------------------------------
   public void startDaemon() throws Exception
   {
      logger.debug("DietsService: startDaemon()");
      if( daemonRunning) {
         // already running
         logger.debug("DietsService: Daemon already running...");
         return;
      }
      String ipAddress = null;
      int port = httpPort;
      try {
         httpServer = new Server();
         ipAddress = localhostProperties.getProperty(
                DietsConfigProperties.DAEMON_IP_ADDRESS);

         //NOTE: this where the 127.0.0.1 is comming from !!!
         showln("  startDaemon(): socketListener-ip="+ipAddress);

         // check ipAddress is loopback
         if( ISSITesterConstants.LOCALHOST_IP_ADDRESS.equals(ipAddress)) {
            String newIp = null;
            try {
               newIp = IpAddressUtility.getLocalHostAddress().getHostAddress();
               if( newIp!=null) {
                  ipAddress = newIp;
                  showln("  startDaemon(): replace 127.0.0.1 with "+newIp);
                  localhostProperties.setProperty(
                     DietsConfigProperties.DAEMON_IP_ADDRESS, newIp);
               }
            } catch(Exception ex) { }
         }
         showln("  startDaemon(): actual ipAddress="+ipAddress+":"+port);

         socketListener = new SocketListener();
         socketListener.setMaxThreads(10);
         socketListener.setMinThreads(3);
         socketListener.setHost(ipAddress);
         socketListener.setPort(port);
         socketListener.start();

         httpServer.addListener(socketListener);
         httpContext = new HttpContext();
         httpContext.setContextPath("/diets/*");

         // Register a welcome servlet with the Servlet Handler.
         ServletHandler servletHandler = new ServletHandler();
         servletHandler.addServlet("controller", "/controller/*",
            ConfigurationServlet.class.getName());
         ConfigurationServlet.setServer(httpServer);

         // How to handle this ??? externally by caller
         // wireup the callback functions to display status
         //===ConfigurationServlet.setClusterManagerStartup(DietsService.this);
         ConfigurationServlet.setDietsService(DietsService.this);

         httpContext.addHandler(servletHandler);
         httpServer.addContext(httpContext);

         httpContext.start();
         httpServer.start();
         daemonRunning = true;
      }
      catch (BindException ex) {
         String msg = "Could not bind to " + ipAddress + ":" + port
            + "\nPlease configure localhost IP address and port";
         logger.error( msg, ex);
         throw new BindException( msg);
      }
      catch (Exception ex) {
         logger.error( ex.getMessage(), ex);
         throw ex;
      }
   }

   public void stopDaemon()
   {
      logger.debug("DietsService: stopDaemon()");
      try {
         if( socketListener != null) {
            socketListener.stop();
            socketListener = null;
         }

         if( daemonRunning) {
            httpContext.stop();
            httpServer.removeContext(httpContext);
            httpServer.stop();
            httpServer = null;
            daemonRunning = false;
         }
      }
      catch (Exception ex) {
         logger.error("Could not stop server", ex);
      }
   }

   public void close() 
   {
      logger.debug("DietsService: close()");
      if(daemonRunning) {
         stopDaemon();
      }
   }

   // constructor
   //------------------------------------------------------------------------
   public DietsService(DietsConfigProperties props, String testSuiteName,
         String testerTopologyFileName, String systemTopologyFileName,
         TestExecutionPanel testExecutionPanel)
         throws Exception
   {
      logger.debug("testerTopologyFileName = " + testerTopologyFileName);
      configProperties = (props==null ? new DietsConfigProperties() : props);
      this.testExecutionPanel = testExecutionPanel;

      localhostProperties = new Properties();
      try {
         localhostProperties.load(new FileInputStream(new File(
            ISSITesterConstants.LOCALHOST_PROPERTIES_FILE)));
         if (localhostProperties.getProperty(DietsConfigProperties.DAEMON_HTTP_PORT) == null)
            localhostProperties.setProperty(DietsConfigProperties.DAEMON_HTTP_PORT,
               new Integer(ISSITesterConstants.DEFAULT_DIETS_HTTP_PORT).toString());
      }
      catch (Exception ex) {
         logger.debug("Cannot read " + ISSITesterConstants.LOCALHOST_PROPERTIES_FILE);
         logger.debug("Will start daemon on 127.0.0.1 address and port "
            + ISSITesterConstants.DEFAULT_DIETS_HTTP_PORT);
         localhostProperties.setProperty( DietsConfigProperties.DAEMON_IP_ADDRESS,
            ISSITesterConstants.LOCALHOST_IP_ADDRESS);
         localhostProperties.setProperty( DietsConfigProperties.DAEMON_HTTP_PORT,
            new Integer( ISSITesterConstants.DEFAULT_DIETS_HTTP_PORT).toString());
      }

      setTestSuiteName(testSuiteName);
      setSystemTopologyFileName(systemTopologyFileName);
      setTesterTopologyFileName(testerTopologyFileName);

      //showln("DietsService: testSuiteName="+testSuiteName);
      //showln("DietsService: systemTopologyFileName="+systemTopologyFileName);
      //showln("DietsService: testerTopologyFileName="+testerTopologyFileName);

      reconfigure();
   }

   public void reconfigure() throws Exception {

      logger.debug("DietsService: reconfigure()");
      logger.debug("DietsService: testSuiteName="+getTestSuiteName());
      logger.debug("DietsService: systemTopologyFileName="+getSystemTopologyFileName());
      logger.debug("DietsService: testerTopologyFileName="+getTesterTopologyFileName());

      if (getTesterTopologyFileName() == null) {
         testerConfig = new ISSITesterConfiguration();
      } else {
         testerConfig = new ISSITesterConfigurationParser( 
            getTesterTopologyFileName()).parse();
      }

      if (getGlobalTopologyFileName() == null &&
          getSystemTopologyFileName() != null)
      {
         topologyConfig = new SystemTopologyParser(testerConfig).parse(
            getSystemTopologyFileName());
      }
      else if (getSystemTopologyFileName() != null)
      {
         TopologyConfig systemTopology = new SystemTopologyParser(testerConfig)
            .parse(getSystemTopologyFileName());

         topologyConfig = new GlobalTopologyParser(true).parse( systemTopology, 
            getGlobalTopologyFileName());
      }
      else 
      {
         topologyConfig = new TopologyConfig();
      }
   }

   //public void clearTesterTopology() {
   //   setTesterTopologyFileName(null);
   //   testerConfig = new ISSITesterConfiguration();
   //}
   
   public void addTesterTopology(String testerTopologyFileName)
      throws Exception {
      if (testerTopologyFileName != null) {
         String fullpath = ISSITesterConstants.getTesterConfigFileName( testerTopologyFileName);
         setTesterTopologyFileName( fullpath);
         testerConfig = new ISSITesterConfigurationParser( getTesterTopologyFileName()).parse();
         topologyConfig.rebindIpAddresses(testerConfig);
      }
   }

   //public void clearSystemTopology() {
   //   setSystemTopologyFileName(null);
   //   topologyConfig = new TopologyConfig();
   //}

   //public void addSystemTopology(String systemTopologyFileName)
   //   throws Exception {
   // NOT YET !!!
   //}

   public void clearTestSuite() {
      setTestSuiteName(null);
      setSystemTopologyFileName(null);
      topologyConfig = new TopologyConfig();
   }
   public void addTestSuite(String testSuiteName) throws Exception {

      if (testSuiteName != null) {
         setTestSuiteName( testSuiteName);
         ISSITesterConstants.setTestSuite( getTestSuiteName());
         setSystemTopologyFileName(ISSITesterConstants.getSystemTopologyFileName());
      } else {
         setTestSuiteName(null);
      }

      if (getTesterTopologyFileName() != null) {
         testerConfig = new ISSITesterConfigurationParser(
               getTesterTopologyFileName()).parse();
      } else {
         testerConfig = new ISSITesterConfiguration();
      }

      if (getGlobalTopologyFileName() == null) {
          topologyConfig = new SystemTopologyParser(testerConfig).parse(
                getSystemTopologyFileName());
      } else {
          TopologyConfig systemTopology = new SystemTopologyParser( testerConfig)
                .parse(getSystemTopologyFileName());
          topologyConfig = new GlobalTopologyParser(true).parse(systemTopology, 
                getGlobalTopologyFileName());
      }
   }

   public ISSITesterConfiguration getTesterConfiguration() {
      return testerConfig;
   }
   public void setConfiguration( ISSITesterConfiguration testerConfig,
      TopologyConfig topologyConfig) {
      this.testerConfig = testerConfig;
      this.topologyConfig = topologyConfig;
      logger.debug("DietsService: setConfiguration(): testerConfig=\n"+testerConfig.toString());
      logger.debug("DietsService: setConfiguration(): topologyConfig=\n"+topologyConfig.toString());
   }

   public void saveConfiguration( ISSITesterConfiguration testerConfig,
      TopologyConfig topologyConfig) throws Exception {

      showln("saveConfiguration: START ***");
      setConfiguration( testerConfig, topologyConfig);

      StringBuffer sbuf = new StringBuffer();
      try {
         configProperties.store();

         // startup/diets.daemon.localhost.properties
         String propertiesFileName = ISSITesterConstants.LOCALHOST_PROPERTIES_FILE;
         File startupFile = new File(propertiesFileName);
         PrintWriter printWriter = new PrintWriter(new FileWriter( startupFile));
         localhostProperties.store(printWriter, "M1: File generated automatically by ISSI Tester.");
         printWriter.close();
         sbuf.append( propertiesFileName + "\n");

         // testerconfig/standalone-configuration.xml
         String fileName = getTesterTopologyFileName();
         if (fileName != null) {
            String testerTopology = testerConfig.toString();
            FileUtility.saveToFile( fileName, testerTopology);
            sbuf.append( fileName + "\n");
         }

         String systemTopologyFileName = getSystemTopologyFileName();
         if (systemTopologyFileName != null) {
            String systemTopology = topologyConfig.exportSystemTopology();
            //FileUtility.saveToFile( systemTopologyFileName, systemTopology);
            //sbuf.append( systemTopologyFileName + "\n");
         }

         String globalTopologyFileName = getGlobalTopologyFileName();
         if (globalTopologyFileName != null) {
            String globalTopology = topologyConfig.exportGlobalTopology();
            //FileUtility.saveToFile( globalTopologyFileName, globalTopology);
            //sbuf.append( globalTopologyFileName + "\n");
         }
         logger.debug("Configuration saved successfully.\nList of files:\n\n");
         logger.debug(sbuf.toString());
         showln("saveConfiguration:\n"+sbuf.toString());

      } catch (Exception ex) {
         logger.error("Error in saving configs: ", ex);
         throw ex;
      }
   }

   //========================================================================
   public static final void main(String[] args) throws Exception
   {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));

      String propertiesFileName = ISSITesterConstants.STARTUP_PROPERTIES_FILENAME;
      DietsConfigProperties props = null;
      String testSuiteName = null;
      String testerTopologyFileName = null;
      String systemTopologyFileName = null;

      // Test-1: null
      //DietsService service = new DietsService(props, testSuiteName,
      //   testerTopologyFileName, systemTopologyFileName);

      // Test-2: non-null
      props = new DietsConfigProperties(propertiesFileName);
      testSuiteName = props.getProperty(DietsConfigProperties.TESTSUITE_PROPERTY);
      testerTopologyFileName = props.getProperty(DietsConfigProperties.DAEMON_CONFIG_PROPERTY);
      systemTopologyFileName = ISSITesterConstants.getSystemTopologyFileName(testSuiteName);

      DietsService service = new DietsService(props, testSuiteName,
         testerTopologyFileName, systemTopologyFileName, null);

      showln("service="+service);
      service.startDaemon();
      service.startTestServices();

      showln("Sleep 10 secs...");
      try {
         Thread.sleep(10000L);
      } catch(Exception ex) { }
      showln("Wakeup...stopXXX()");

      service.stopTestServices();
      service.stopDaemon();
      showln("Done...");
   }
}
