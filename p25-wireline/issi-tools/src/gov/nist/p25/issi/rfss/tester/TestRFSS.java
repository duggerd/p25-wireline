//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.issiconfig.WebServerAddress;
import gov.nist.p25.issi.rfss.SipStackFactory;
import gov.nist.p25.issi.rfss.RFSS;
import gov.nist.p25.issi.rfss.servlets.ControlServlet;
import gov.nist.p25.issi.rfss.servlets.WelcomeServlet;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import javax.sip.ListeningPoint;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import org.apache.log4j.Logger;
import org.mortbay.http.HttpContext;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.util.FileResource;

/**
 * This is the test proxy wrapper. A test case is first configured by starting
 * as many of these test proxy servers as are necessary on multiple test
 * machines. A test is launched by invoking the methods loadScenario and
 * runScenario with the <b>same</b> arguments on each emulator.
 */
@SuppressWarnings("unchecked")
public class TestRFSS {
   
   private static Logger logger = Logger.getLogger(TestRFSS.class);
   public static void showln(String s) { System.out.println(s); }
   
   //private static boolean remoteAppenderInitialized;
   private static ISSITesterConfiguration testerConfiguration;
   private static String testerConfigFile;   
   private static boolean configured;
   private static HttpContext httpContext;
   private static HttpContext logContext;
   
   // The HTTP Server to query information and manage the RFSS 
   private static Server httpServer;
   private transient RFSS rfss;
   private transient SipProvider provider;
   private String ipAddress;
   private RfssConfig rfssConfig;
   private CallSetupTest currentCallSetupTest;

   // accessors
   public RfssConfig getRfssConfig() { return rfssConfig; }
   public String getIpAddress() { return ipAddress; }


   static class TestDirectoryNameFilter implements FileFilter {
      public boolean accept(File pathname) {
         return pathname.isDirectory()
               && !pathname.getPath().endsWith("dtd");
      }
   }

//   private void registerRemoteMonitor(String ipAddress, int httpPort)
//         throws Exception {
//      //String key = ipAddress + ":" + httpPort;
//      if (!TestRFSS.remoteAppenderInitialized) {
//         Logger logger = Logger.getLogger("gov.nist.p25");
//         logger.addAppender(new RemoteAppender(rfssConfig, ipAddress,
//               httpPort, new SimpleLayout()));
//         TestRFSS.remoteAppenderInitialized = true;
//      }
//   }

   /**
    * Configure the web server to talk to the outside world.
    * 
    * @param configPort --
    *            port through which to talk to the outside world
    */
   private static void startWebServer() throws Exception {

      TestRFSS.httpContext = new HttpContext();
      httpContext.setContextPath("/rfss/*");
      ServletHandler servletHandler = new ServletHandler();
      // Register a welcome servlet with the Servlet Handler.
      servletHandler.addServlet("welcome", "/*", WelcomeServlet.class.getName());
      servletHandler.addServlet("control", "/control/*", ControlServlet.class.getName());
      // "gov.nist.p25.issi.servlets.ControlServlet");
      httpContext.addHandler(servletHandler);
      httpServer.addContext(httpContext);

      // Set up a handler to export the log files.
      TestRFSS.logContext = new HttpContext();
      logContext.setContextPath("/logs/*");
      FileResource logFilesDir = (FileResource) FileResource.newResource("./logs/");
      ResourceHandler rhandler = new ResourceHandler();
      rhandler.setName("logs");
      rhandler.setAllowedMethods(new String[] { "GET", "CONNECT", "HEAD" });
      logContext.setBaseResource(logFilesDir);
      rhandler.setDirAllowed(true);
      logContext.addHandler(rhandler);
      httpServer.addContext(logContext);
      rhandler.start();
      httpContext.start();
   }

   /**
    * Flush the PTT messsage log to the disk.
    * 
    * @throws Exception
    */
   public void flushPttMessageLog() throws Exception {
      getRfss().flushCapturedPttMessages();
   }

   /**
    * Get the SIP Message log.
    */
   public String getSipMessageLog() {
      String filename = ISSITesterConstants.DEFAULT_LOGS_DIR
            + "/" + rfssConfig.getRfssName() + ".messagelog.sip";
      try {
         return FileUtility.loadFromFileAsString(filename);
      } catch (Exception ex) {
         logger.error("Error reading log file: " +filename, ex);
         return "";
      }
   }

   /**
    * Get the error log.
    */
   public static String getErrorLog() {
      String filename = ISSITesterConstants.ERROR_LOG;
      try {
         return FileUtility.loadFromFileAsString(filename);
      } catch (Exception ex) {
         logger.error("Error reading log file: "+filename, ex);
         return "";
      }
   }

   /**
    * Run the previously loaded scenario.
    */
   public void runScenario() {
      logger.debug("TestRFSS: runScenario(): " + this);
      currentCallSetupTest.doCallSetup();
   }

   /**
    * Get the current call setup test.
    */
   public CallSetupTest getCurrentCallSetupTest() {
      return currentCallSetupTest;
   }

   /**
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.RFSSTestProxyIF#loadScenario(java.lang.String)
    */
   public void loadScenario(TestScript testScript) throws Exception {
      logger.info("loadScenario(): testScript=" + testScript);

      if( testScript==null) {
         throw new Exception("loadScenario: input testscript is null.");
      }
      long currentTime = System.currentTimeMillis();
      if (currentCallSetupTest != null &&
          (currentTime - currentCallSetupTest.getStartTime() < ISSITesterConstants.TEST_RUNS_FOR)) {
         throw new Exception("Test currently in progress");
      }
      try {
         if (currentCallSetupTest != null) {
            provider.removeSipListener(currentCallSetupTest);
            currentCallSetupTest = null;
         }
//logger.info("loadScenario(): RFSS.create()...testScript=" + testScript);
         rfss = RFSS.create(provider, rfssConfig, testScript);
         currentCallSetupTest = new CallSetupTest(rfssConfig, testScript, rfss);
         rfss.setCurrentTestCase(currentCallSetupTest);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new Exception( "Exception while launching the test case", ex);
      }
   }

   /**
    * Get the RFSS object that this TestRFSS wraps.
    * 
    * @return the rfss object
    */
   public RFSS getRfss() {
      return rfss;
   }

   /**
    * Get the RFSS communication objects.
    */
   public String getPttSessionInfo() {
      return rfss.getRfssSessionInfo();
   }

   /**
    * The constructor.
    * 
    * @param rfssConfig -
    *            the static configuration of the RFSS for which we want to
    *            start this emulated instance.
    * 
    * @param topologyConfig --
    *            the topology configuration
    */
   public TestRFSS(RfssConfig rfssConfig, TopologyConfig topologyConfig)
         throws Exception {
      if (rfssConfig == null) {
         throw new NullPointerException("Null input rfssConfig arg!");
      }
      this.rfssConfig = rfssConfig;
      this.ipAddress = rfssConfig.getIpAddress();
      int port = rfssConfig.getSipPort();
      String tag = rfssConfig.getRfssName();

      // M1016 ProtocolObjects -> SipStackFactory
      SipStack sipStack = SipStackFactory.getSipStack(rfssConfig.getRfssName(),
         topologyConfig);

      logger.debug(tag+" createListeningPoint(udp): ipStr="+ipAddress+":"+port);
      ListeningPoint listeningPoint = sipStack.createListeningPoint(ipAddress,port,"udp");

      // TODO: May get javax.sip.ObjectInUseException
      // TODO: need to fetch provider for ListeningPoint on exception 
      try {
         provider = sipStack.createSipProvider(listeningPoint);
      }
      catch(javax.sip.ObjectInUseException ex) {
         provider = null;
         logger.debug("TestRFSS: ip="+ipAddress+ " createSipProvider: " +ex);
         for( Iterator it= sipStack.getSipProviders(); it.hasNext(); ) {
            SipProvider p = (SipProvider)it.next();
            showln("Checking Provider: p="+p);
            ListeningPoint[] lps = p.getListeningPoints();
            for(int i=0; i < lps.length; i++) {
               ListeningPoint lp = lps[i];
               showln("ListeningPoint: lp="+lp);
               if( lp.equals(listeningPoint)) {
                  showln("Found a match: "+listeningPoint);
                  provider = p;
		  break;
               }
            }
	    if( provider != null) break;
         }
         if( provider==null) {
            showln("Failed to find a match: "+listeningPoint);
            throw ex;
         }
      }
   }

   public static void setTesterConfiguration( ISSITesterConfiguration testerConfig) {
      testerConfiguration = testerConfig;
   }

   public static ISSITesterConfiguration getTesterConfiguration() {
      return testerConfiguration;
   }
   
   public static int configure(Server server, String configFileName) 
      throws Exception {

      // Start web severs for all addresses we own.
      int startCount = 0;
      int port = -1;
      testerConfigFile = configFileName;
      httpServer = server;
      TestRFSS.testerConfiguration = new ISSITesterConfigurationParser(testerConfigFile).parse();

      for (String ipAddress : testerConfiguration.getLocalAddresses()) {
         HashSet<String> testers = new HashSet<String>();
         
         for (WebServerAddress config : testerConfiguration.getEmulatorConfigurations()) {
            if (config.getIpAddress().equals(ipAddress)) {
               port = config.getHttpPort();
            }
            logger.info("configure(): ipAddress=" +ipAddress +":" +port);
            testers.add(ipAddress);
         }
         ControlServlet.setIpAddress(testers);
         WelcomeServlet.setISSITestLauncherConfig(testerConfiguration);
         startWebServer();
         startCount++;         
      }
      logger.info("Start count " + startCount);
      configured = true;
      logger.debug("TestRFSS: configure....setflag TRUE");
      return startCount;
   }
   
   public static boolean isConfigured() {
      return configured;
   }
   
   public static void unconfigure() throws Exception {
    if( isConfigured()) {
      httpContext.stop();
      httpServer.removeContext(httpContext);
      logContext.stop();
      httpServer.removeContext(logContext);      
      //ZZZ set flag
      logger.debug("TestRFSS: unconfigure....setflag FALSE");
      configured = false;
     }
   }

   // Main entry point for the RFSS when running in distributed mode.
   //===================================================================
   public static void main(String[] args) throws Exception {
      Properties props = new Properties();
      String fileName = null;      
      for (int i = 0; i < args.length; i++) {
          if (args[i].equals("-startup")) {
            fileName = args[++i];
            InputStream inStream = new FileInputStream(new File(fileName));
            props.load(inStream);
         } 
      }
      if (fileName == null) {
         fileName = System.getProperty("diets.startup");
      }
      if (fileName == null) {
         throw new Exception("Missing startup properties");
      }
      logger.info("Using startup property " + fileName);

      testerConfigFile = props.getProperty(DietsConfigProperties.DAEMON_CONFIG_PROPERTY,
            testerConfigFile);      
      if (testerConfigFile == null) {
         throw new Exception("Missing a required parameter -testerConfig");
      }
      Server httpServer = new Server();
      TestRFSS.configure(httpServer, testerConfigFile);      
      httpServer.start();   
   }
}
