//
package gov.nist.p25.issi.constants;

/**
 * A set of constants that are used in various places in the tester
 */
public class ISSITesterConstants {

   public static final String LOCALHOST_IP_ADDRESS = "127.0.0.1";
   
   // testClass 
   public static final String TEST_CLASS_DEPRECATED = "Deprecated";
   public static final String TEST_CLASS_CONFORMANCE = "Conformance";
   public static final String TEST_CLASS_UDP = "User Defined Profile";

   // Please place constants in order !!!
   //------------------------------------------------------------------------
   // Servlet parameter for a test case.
   public static final String TEST_CASE = "testcase";
   public static final String FILE = "file";
   public static final String SIPDEBUG = "sipdebug";
   public static final String PTTDEBUG = "pttdebug";
   public static final String SIPMESSAGES = "sipmessages";
   public static final String PTTMESSAGES = "pttmessages";
   public static final String CONSOLE = "console";
   public static final String RFSSCONFIG_PARAM = "rfssconfig";

   public static final String COMMAND = "command";
   public static final String REDRAW = "redraw";
   public static final String STATIC_REINITIALIZATION = "staticReinitialization";
   public static final String SET_CONFORMANCE_TEST = "setConformanceTest";
   public static final String KEY_TEST_INDEX = "testIndex";
   public static final String KEY_TEST_CLASS = "testClass";
   public static final String SAVE_TEST_FILES = "saveTestFiles";

   public static final String LOAD = "load";
   public static final String RUN = "run";
   public static final String EXIT_EMULATOR = "Exit";
   public static final String GET_SIP_TRACE = "getSipTrace";

   public static final String GET_RFSS_CONFIG = "getRfssConfig";
   //public static final String GET_GROUP_CONFIG = "groupConfig";
   //public static final String GET_SU_CONFIG = "suConfig";
   public static final String GET_GLOBAL_TOPOLOGY = "getGlobalTopology";
   public static final String GET_SYSTEM_TOPOLOGY = "getSystemTopology";
   public static final String GET_TESTER_CONFIG = "getTesterConfig";
   public static final String GET_RUNTIME_EXCEPTION = "getRuntimeException";
   public static final String GET_DATE_TIME = "getDateTime";


   // Where are the scenario's are kept
   public static final String SCENARIO_DIR_KEY = "scenarioDir";

   // Commands to send to the remote controller
   public static final String ASSERTION_SUCCEEDED = "Test Succeeded";
   public static final String ASSERTION_FAILURE = "AssertionFailure";
   public static final String CONTROLLER = "controller";
   public static final String LOG_ERROR = "LogError";
   public static final String LOG_EXCEPTION = "LogException";
   public static final String LOG_FAILURE = "LogFailure";
   public static final String LOG_FATAL = "LogFatal";
   public static final String LOG_INFO = "LogStatus";

   // Commands to send to the ISSI Emulators.
   public static final String GET_TEST_CASES = "getTestCases";
   public static final String REGISTER_REMOTE_MONITOR = "registerRemoteMonitor";
   public static final String GET_RFSS_STATUS_INFO = "getRfssStatusInfo";
   public static final String GET_TEST_RESULTS = "getTestResults";
   public static final String HEARTBEAT_QUERY = "heartbeatQuery";

   //TODO: removed 'ed'
   public static final String FAILED = "Fail";
   public static final String PASSED = "Pass";
   public static final String GET_ERROR_LOG = "getErrorLog";

   // Length of time (default) for which the test is expected to run
   public static int TEST_RUNS_FOR = 25000;
   
   public static final String DEFAULT_TRACES_DIR = "refmessages";
   public static final String EXECUTE_NEXT_SCENARIO = "next-scenario";
   public static final String SCENARIO = "scenario";
   public static final String INTERACTIVE = "interactive";
   public static final String TEAR_DOWN_TEST = "tear-down-test";
   public static final String TEST_COMPLETED = "test-completed";
   public static final String TOPOLOGY_XML = "topology.xml";
   public static final String DEFAULT_LOGS_DIR = "logs";
   public static final String GLOBAL_TOPOLOGY_XML = "globaltopology.xml";
   public static final String CURRENT_TEST_CASE_XML = "current-test-case.xml";
   public static final String TEST_TOPOLOGY = "testTopology";
   public static final String TESTER_DAEMON_TOPOLOGY = "testerDaemonTopology";
   public static final String GET_PTT_TRACE = "getPttTrace";
   public static final String EVALUATE_POST_CONDITION = "evaluatePostCondition";
   public static final String START_PACKET_MONITOR = "startPacketMonitor";
   public static final String GLOBAL_TOPOLOGY = "globalTopology";
   public static final int VOICE_FILE_MAX_LENGTH = 10; // Seconds

   public static final String DEBUG_LOG = "logs/debuglog.txt";
   public static final String ERROR_LOG = "logs/errorlog.txt";
   public static final String IS_TEST_COMPLETED = "isTestCompleted";
   public static final String SYSTEM_TOPOLOGY = "systemTopology";
   public static final String TESTSUITES_DIR = "testsuites";
   public static final String TEST_REGISTRY_XML = "testregistry.xml";
   public static final String SYSTEM_TOPOLOGY_XML = "systemtopology.xml";
   public static final String DEFAULT_TEST_RUN_DIRECTORY = "testrun";
   public static final String GET_ERROR_FLAG = "getErrorFlag";
   public static final String TESTSCRIPTS_DIR = "testscripts";

   public static final String DEFAULT_TESTERCONFIG_DIR = "testerconfig";
   public static final String START_TESTER = "startTester";

   public static final int DEFAULT_DIETS_HTTP_PORT = 8763;
   public static final String LOCALHOST_PROPERTIES_FILE = "startup/diets.daemon.localhost.properties";
   public static final String START_SERVICES = "startServices";
   public static final String UPDATE_CONFIGURATION_FILE = "updateConfigurationFile";
   public static final String FILENAME_TO_UPDATE = "fileNameToUpdate";
   public static final String STOP_SERVICES = "stopServices";
   public static final String STARTUP_PROPERTIES_FILENAME = "startup/diets.properties";
   public static final String KEY_STARTUP_FILE = "startupFile";

   public static final String TEST_NUMBER = "testNumber";
   public static String DEFAULT_RTP_MESSAGE_LOG = "logs/messagelog.ptt";
   public static String DEFAULT_RTP_DEBUG_LOG = "logs/rtpdebuglog.txt";
   
   //NOTE: private static
   private static String testSuite = "conformance";

   //---------------------------------------------------------------
   public static String getRootDir() {
      //return "./";
      return ".";
   }

   //---------------------------------------------------------------
   public static String getTestSuite() {
      return testSuite;
   }
//TODO: bad practice !!!
   public static void setTestSuite(String testSuite) {
      ISSITesterConstants.testSuite = testSuite;
   }

   //---------------------------------------------------------------
   public static String getScenarioDir(String testSuiteName) {
      return ISSITesterConstants.TESTSUITES_DIR 
             + "/" + testSuiteName 
	     + "/" + ISSITesterConstants.TESTSCRIPTS_DIR;
   }
   public static String getScenarioDir() {
      return ISSITesterConstants.TESTSUITES_DIR 
             + "/" + testSuite 
	     + "/" + ISSITesterConstants.TESTSCRIPTS_DIR;
   }

   //---------------------------------------------------------------
   public static String getTestRegistry() {
      return ISSITesterConstants.getScenarioDir() 
         + "/" + ISSITesterConstants.TEST_REGISTRY_XML;
   }

   //---------------------------------------------------------------
   public static String getGlobalTopologyFileName() {
      return ISSITesterConstants.getScenarioDir()
         + "/" + ISSITesterConstants.GLOBAL_TOPOLOGY_XML;
   }
   public static String getGlobalTopologyFileName(String testSuiteName) {
      if( testSuiteName==null)
         return null;
      return ISSITesterConstants.TESTSUITES_DIR 
         + "/" + testSuiteName
         + "/" + ISSITesterConstants.TESTSCRIPTS_DIR
	 + "/" + ISSITesterConstants.GLOBAL_TOPOLOGY_XML;
   }

   //---------------------------------------------------------------
   public static String getSystemTopologyFileName() {
      return ISSITesterConstants.TESTSUITES_DIR
         + "/" + ISSITesterConstants.getTestSuite()
         + "/" + ISSITesterConstants.SYSTEM_TOPOLOGY_XML;
   }
   public static String getSystemTopologyFileName(String testSuiteName) {
      if( testSuiteName==null)
         return null;
      return ISSITesterConstants.TESTSUITES_DIR
         + "/" + testSuiteName
         + "/" + ISSITesterConstants.SYSTEM_TOPOLOGY_XML;
   }

   public static String getCurrentTopologyFileName(String curTopologyName) {
      if( curTopologyName==null)
         return null;
      return getScenarioDir() + "/" + curTopologyName;
   }

   //---------------------------------------------------------------
   public static String getTraceDirectory() {
      //=== refmessages/conformance
      return ISSITesterConstants.DEFAULT_TRACES_DIR 
         + "/" + ISSITesterConstants.getTestSuite();
   }
   public static String getTraceDirectory(String testSuiteName) {
      //=== refmessages/conformance
      return ISSITesterConstants.DEFAULT_TRACES_DIR 
         + "/" + testSuiteName;
   }

   public static String getTraceDirectory(String testSuiteName, String topology) {
      // DietsGUI.renderRefTraceLogs()
      //=== refmessages/conformance/topology1.logs
      return ISSITesterConstants.DEFAULT_TRACES_DIR 
         + "/" + testSuiteName + "/" + topology;
   }

   //---------------------------------------------------------------
   public static String getCurrentTestCaseFileName(String dirpath) {
      return  dirpath + "/" + ISSITesterConstants.CURRENT_TEST_CASE_XML; 
   }

   public static String getCurrentTestCaseLogFileName() {
      return ISSITesterConstants.DEFAULT_LOGS_DIR
         + "/" + ISSITesterConstants.CURRENT_TEST_CASE_XML;
   }

   public static String getTesterConfigFileName(String testerTopologyName) {
      //return getRootDir() + "/" +
      return ISSITesterConstants.DEFAULT_TESTERCONFIG_DIR
	     + "/" + testerTopologyName;
   }
}
