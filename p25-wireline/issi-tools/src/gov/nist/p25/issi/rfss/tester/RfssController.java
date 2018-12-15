//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.rfss.RFSS;
import gov.nist.p25.issi.testlauncher.TestControllerInterface;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;

/**
 * The server side of the remote test controller. The methods here may be either
 * directly invoked from the gui or via http.
 */
public class RfssController implements TestControllerInterface {

   private static Logger logger = Logger.getLogger(RfssController.class);
   public static void showln(String s) { System.out.println(s); }

   private String testSuiteName;
   private HashSet<TestRFSS> testRfssCollection = new HashSet<TestRFSS>();
   private Collection<String> ipAddress;

   // accessor
   public void setIpAddress(Collection<String> ipAddress) {
      this.ipAddress = ipAddress;
   }
   /****
   public String getTestSuiteName( ) {
      return testSuiteName;
   }
   public void setTestSuiteName(String testSuiteName) {
      this.testSuiteName = testSuiteName;
   }
    ***/
   
   // constructor
   public RfssController(Collection<String> ipAddress) {
      this.ipAddress = ipAddress;
   }
   
   /**
    * Load the test case and initialize all RFSSs.
    * 
    * @param testCase -- the test case to load.
    * @param interactive -- whether interactive or not.
    * 
    * @throws Exception
    */
   public void loadTest(String scenarioDir, String testCase, String testNumber,
         String testTopology, String globalTopologyName,
         String systemTopologyName, boolean interactive,
         boolean evaluatePassFail) throws Exception {

      testRfssCollection.clear();
      RFSS.reset();

      File logDir = new File(ISSITesterConstants.DEFAULT_LOGS_DIR);
      if (!logDir.exists()) {
         logDir.mkdirs();
      }

      File[] logFiles = logDir.listFiles(new FilenameFilter() {
         public boolean accept(File dir, String name) {
            return (name.endsWith(".sip") || name.endsWith(".ptt"));
         }
      });

      // Delete all the SIP log files
      for (File file: logFiles) {
         file.delete();
         //logger.info("sipTraceFileLength= " + file.length());
      }

      String pttTrace = TestScript.getRtpMessageLog();
      File pttTraceFile = new File(pttTrace);
      if (pttTraceFile.delete()) {
         logger.debug("Successfully deleted ptt trace file");
      }
      /*
       * File debugLogFile = new File(ISSITesterConstants.DEBUG_LOG); if
       * (debugLogFile.delete()) { System.out.println("Successfully deleted
       * debug log file"); }
       */

      File errorLogFile = new File(ISSITesterConstants.ERROR_LOG);
      if (errorLogFile.delete()) {
         logger.debug("Successfully deleted errorlog.txt");
      }

      try {
         //***ISSITesterConstants.setScenarioDir(scenarioDir);
         testSuiteName = new File(scenarioDir).getParentFile().getName();

         //logger.info("RfssController: setTestSuite(): "+testSuiteName);
         ISSITesterConstants.setTestSuite(testSuiteName);

         logger.info("loadScenario: " + testCase);
         String testDir = new File(testCase).getParent();
         testCase = scenarioDir + "/" + testCase;
         String localTopology = scenarioDir + "/" + testTopology;

         // Record the test case in the logs directory.
         String testCaseLogFileName = ISSITesterConstants.getCurrentTestCaseLogFileName();
         logger.debug("Creating current-test-case: "+testCaseLogFileName);
         String testCaseString = "<testcase\n" +
            " date=\"" + Calendar.getInstance().getTime() + "\"\n" +
            " testNumber=\""+testNumber + "\"\n" +
            " interactive=\"" + interactive + "\"\n" +
            " scenarioDir=\"" + scenarioDir + "\"\n" +
            " testDirectory=\"" + testDir + "\"\n" +
            " topology=\"" + testTopology + "\"\n" +
            "/>";
         logger.debug("testCaseString:\n"+testCaseString);
         FileUtility.saveToFile( testCaseLogFileName, testCaseString);

         logger.debug("Creating File - globalTopologyLog");
         File globalTopologyLog = new File(
               ISSITesterConstants.DEFAULT_LOGS_DIR + "/"
                     + ISSITesterConstants.GLOBAL_TOPOLOGY_XML);
         if (globalTopologyLog.exists()) {
            globalTopologyLog.delete();
         }

         globalTopologyLog.createNewFile();

         // TODO read this from the manifest.
         File testGlobalTopology = new File(new File(scenarioDir) + "/"
               + ISSITesterConstants.GLOBAL_TOPOLOGY_XML);

         FileReader reader = new FileReader(testGlobalTopology);
         char[] buffer = new char[(int) testGlobalTopology.length()];
         reader.read(buffer);
         FileWriter writer = new FileWriter(globalTopologyLog);
         writer.write(buffer);
         writer.close();
         reader.close();
         
         logger.debug("Creating File - systemTopologyLog");
         File systemTopologyLog = new File(
               ISSITesterConstants.DEFAULT_LOGS_DIR + "/"
                     + ISSITesterConstants.SYSTEM_TOPOLOGY_XML);

         if (systemTopologyLog.exists()) {
            systemTopologyLog.delete();
         }
         systemTopologyLog.createNewFile();


         // NOTE: 
         // this assume systemtopology.xml under the testsuites/emulator
         // cannot be changed to emulated-systemtopology.xml !!!!
         File testSystemTopology = new File(testGlobalTopology
               .getParentFile().getParentFile().getAbsolutePath()
               + "/" + ISSITesterConstants.SYSTEM_TOPOLOGY_XML);

         reader = new FileReader(testSystemTopology);
         buffer = new char[(int) testSystemTopology.length()];
         reader.read(buffer);

         writer = new FileWriter(systemTopologyLog);
         writer.write(buffer);
         writer.close();
         reader.close();

         logger.debug("globalTopologyName = " + globalTopologyName);
         logger.debug("systemTopologyName = " + systemTopologyName);

         logger.debug("Creating TestScriptParser...");
         TestScript tscript = new TestScriptParser(testCase, localTopology,
               globalTopologyName, systemTopologyName, interactive)
               .parse();
         tscript.setInteractive(interactive);
         if (!interactive) {
            tscript.setTraceCaptureEnabled(true);
         } else {
            tscript.setTraceCaptureEnabled(false);
         }
         tscript.setEvaluatePassFail(evaluatePassFail);
         
         //logger.debug("Creating TopologyConfig...");
         TopologyConfig topology = tscript.getTopologyConfig();

         logger.debug("Processing ipAddress="+ipAddress);
	 //----------------------------------------------------------------
         for (String ipStr: ipAddress) {
            logger.debug(" interactive="+interactive+" ipStr="+ipStr);
            for (RfssConfig rfssConfig: topology.getRfssConfigurations()) {
               // Distributed interactive=false
               if ((rfssConfig.getIpAddress().equals(ipStr) && rfssConfig.isEmulated())
                    || !interactive) {
//TODO: what if more than one ???
logger.debug("RfssController: loadScenario....rfssName="+rfssConfig.getRfssName());
                  TestRFSS testRfss = null;
                  try {
                     testRfss = new TestRFSS(rfssConfig, topology);
                     testRfss.loadScenario(tscript);
                     testRfssCollection.add(testRfss);
                     
                  } catch( javax.sip.ObjectInUseException ex) {
                     ex.printStackTrace();
                     String msg = "Error in loading testcase for ipStr="+ipStr;
                     // provider already attached
                     logger.error( msg, ex);
                     //showln("ipStr="+ipStr+"  ex="+ex);

                     // this is due to same IP used in issi-tester-config
                     throw  new IllegalStateException( msg+" "+ex.getMessage());
                     //throw ex;
                  }
               }
            }
         }
	 //----------------------------------------------------------------
      } catch (Exception ex) {
         ex.printStackTrace();
         logger.error("Error in loading testcase: ", ex);
         throw ex;
      }
   }

   /**
    * Run the loaded test case.
    */
   public void runTest() {
      for (TestRFSS testRfss: testRfssCollection) {
         testRfss.runScenario();
      }
   }

   public boolean isTestCompleted() throws Exception {
      try {
         boolean retval = true;
         for (TestRFSS testRfss: testRfssCollection) {
            retval &= testRfss.getRfss().isSignalingCompleted();
         }
         logger.debug("RfssController: isTestCompleted " + retval);
         return retval;
      } catch (Exception ex) {
         logger.error("Error in isSignalingCompleted check: ", ex);
         return false;
      }
   }
   
   // Check if save trace done ?
   public boolean isSaveTraceCompleted() throws Exception {
      try {
         boolean retval = true;
         for (TestRFSS testRfss: testRfssCollection) {
            retval &= testRfss.getRfss().isSaveTrace();
         }
         logger.debug("RfssController: isSaveTraceCompleted " + retval);
         return retval;
      } catch (Exception ex) {
         logger.error("Error in isSaveTrace check: ", ex);
         return false;
      }
   }

   public String getRfssPttSessionInfo(boolean header) {
      StringBuffer retval = new StringBuffer();
      if(header)
         retval.append("<issi-runtime-data>\n");
      for (TestRFSS testRfss: testRfssCollection) {
         retval.append( testRfss.getPttSessionInfo());
      }
      if(header)
         retval.append("\n</issi-runtime-data>");
      return retval.toString();
   }

   /**
    * Run the next scenario in prompting mode.
    * 
    * @param scenario
    * @throws Exception
    */
   public void signalNextScenario(String scenario) throws Exception {
      for (TestRFSS testRfss: testRfssCollection) {
         testRfss.getCurrentCallSetupTest().executeNextScenario(scenario);
      }
   }

   /**
    * Tear down the test.
    */
   public void tearDownCurrentTest() {
      logger.debug("RfssController: tearDownCurrentTest() ...");
      for (TestRFSS testRfss: testRfssCollection) {
         testRfss.getCurrentCallSetupTest().abort();
         testRfss.getCurrentCallSetupTest().tearDown();
      }
   }

   /**
    * Do the completion tasks
    */
   public void signalTestCompletion() {
      for (TestRFSS testRfss: testRfssCollection) {
         testRfss.getCurrentCallSetupTest().startCompletionTimer();
      }
   }

   /**
    * Get the test results from our RFSSs.
    * 
    * @return the test pass or fail.
    */
   public boolean getTestResults() {
      boolean passed = true;
      boolean checked = false;
      for (TestRFSS testRfss: testRfssCollection) {
         if (testRfss.getCurrentCallSetupTest().isFailed()) {
            passed = false;
         }
	 checked = true;
      }
      if( !checked) passed = false;
      return passed;
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestControllerInterface#getStackSipLog()
    */
   public String getStackSipLog() throws Exception {
      logger.debug("RfssController: getStackSipLog(): START ...");
      try {
         StringBuffer retval = new StringBuffer();
         for (TestRFSS testRfss: testRfssCollection) {
            String msglog = testRfss.getSipMessageLog();
	    if(msglog == null) continue;
            retval.append(msglog);
         }
         return retval.toString();
      } catch (Exception ex) {
         return null;
      }
   }

   public String getStackSipLogs() throws Exception {
      return this.getStackSipLog();
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestControllerInterface#getStackPttLog()
    */
   public String getStackPttLog() throws Exception {
      logger.debug("RfssController: getStackPttLog(): START ...");
      try {
         for (TestRFSS testRfss: testRfssCollection) {
            testRfss.flushPttMessageLog();
         }
         //TopologyConfig.closeRtpMessageLogStream();
         TesterUtility.closeRtpMessageLogStream();

         String messageLogFile = TestScript.getRtpMessageLog();
         logger.debug("RfssController: messageLogFile="+messageLogFile);
         String text = FileUtility.loadFromFileAsString( messageLogFile);
//logger.debug("RfssController: text.size="+text.length()+"\n"+text);
         return text;
         
      } catch (Exception ex) {
         //ex.printStackTrace();
         logger.debug("RfssController: getStackPttLog() ex="+ex);
         return "";   // M1009 null -> ""
      }
   }

   public String getStackPttLogs() throws Exception {
      logger.debug("RfssController: getStackPttLogs() ===> getStackPttLog()");
      return this.getStackPttLog();
   }

   public String getPttTraces() throws Exception {
      throw new Exception("Method getPttTraces() not supported");
   }

   public String getSipTraces() throws Exception {
      throw new Exception("Method getSipTraces() not supported");
   }

   public void reset() {
      testRfssCollection = new HashSet<TestRFSS>();
      RFSS.reset();
   }

   public String getErrorLog() throws Exception {
      return TestRFSS.getErrorLog();
   }

   public String getRfssConfigs() throws Exception {
      StringBuffer retval = new StringBuffer();
      for (TestRFSS testRfss: testRfssCollection) {
         String config = testRfss.getRfssConfig().toString();
         retval.append(config);
      }
      return retval.toString();
   }

   //------------------------------------------------
   // 14.4.x
   public void doHeartbeatQuery() {
      logger.debug("RfssController: doHeartbeatQuery()");
      for (TestRFSS testRfss: testRfssCollection) {
         RFSS rfss = testRfss.getRfss();
         if( rfss != null) {
            logger.debug("RfssController: rfss.doHeartbeatQuery(): "+
                         rfss.getRfssConfig().getRfssName());
            rfss.doHeartbeatQuery();
         }
      }
   }
}
