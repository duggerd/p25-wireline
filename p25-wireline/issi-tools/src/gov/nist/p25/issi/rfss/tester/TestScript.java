//
package gov.nist.p25.issi.rfss.tester;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import org.apache.log4j.Logger;

import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.TopologyConfig;

/**
 * This is a the Java class that represents a test script. A test script
 * consists of several Scenarios that are fired at particular times during the
 * execution.
 * 
 */
public class TestScript {
   
   private static Logger logger = Logger.getLogger(TestScript.class);
   
   private String description;   
   private int testCompletionDelay;   
   private int traceGenerationTime;
   private HashSet<PostCondition> postConditions;
   private Hashtable<String, AbstractScenario> scenarios;
   
   private TopologyConfig topologyConfig;
   private String testName;
   private String testDirectory;
   private boolean isInteractive;

   // Set this flag to capture trace.
   private boolean isTraceCaptureEnabled;   
   private boolean evaluatePassFail;

   /**
    * Where XML formatted rtp messages go.
    */
   public static String rtpMessagelog = ISSITesterConstants.DEFAULT_RTP_MESSAGE_LOG;

   /**
    * Where RTP debug messages go
    */   
   public static String rtpDebuglog = ISSITesterConstants.DEFAULT_RTP_DEBUG_LOG;

   /**
    * Where SIP Debug messages go.
    */
   public static String sipDebuglog = ISSITesterConstants.DEBUG_LOG;
      
   /*
    * Whether or not to generate the SIP trace.
    */
   public static boolean generateSipTrace;

   /*
    * Wheter or not a meaningful ptt trace is captured.
    */
   public static boolean generatePttTrace;


   // constructor
   public TestScript() {
      this.scenarios = new Hashtable<String, AbstractScenario>();
      this.postConditions = new HashSet<PostCondition>();      
      this.testCompletionDelay = 0;
      this.traceGenerationTime = 25000;
   }
   
   public void setTopologyConfig(TopologyConfig topologyConfig) {
      this.topologyConfig = topologyConfig;
   }
   
   public AbstractScenario getScenario(String scenarioName) {
      return scenarios.get(scenarioName);
   }

   public Hashtable<String, AbstractScenario> getScenarios() {
      return scenarios;
   }
   public Collection<AbstractScenario> getScenariosValues() {
      return scenarios.values();
   }

   public void printScenarios() {
      for (AbstractScenario sc: scenarios.values()) {
         logger.info("id = " + sc.getId() + " method = "
            + sc.getMethodName() + " time = " + sc.getTrigger());
      }
   }

   public TopologyConfig getTopologyConfig() {
      return topologyConfig;
   }

   public String getReferenceMessagesDirName() {
      String retval = getTopologyConfig().getTopologyFile().substring(0, 
         topologyConfig.getTopologyFile().indexOf(".xml")) + ".testmessages";

      String dirName = retval.replaceFirst(ISSITesterConstants.getScenarioDir(), 
         ISSITesterConstants.getTraceDirectory());
      logger.debug("referenceMessagesDirName = " + dirName);
      return dirName;
   }
      
   public void addPostCondition(PostCondition postCondition) {
      logger.info("AddPostCondition");
      postConditions.add(postCondition);
   }

   public HashSet<PostCondition> getPostConditions() {
      return postConditions;
   }

   public String getDescription() {
      return description;
   }
   public void setDescription(String description) {
      this.description = description;
   }

   public String getTestName() {
      return testName;
   }
   public void setTestName(String testName) {
      this.testName = testName;
   }

   public String getTestDirectory() {
      return testDirectory;
   }
   public void setTestDirectory(String dirName) {
      this.testDirectory = dirName;
   }

   /**
    * The interactive flag determines whether the test is run with prompting.
    * 
    * @return -- the interactive flag.
    */
   public boolean isInteractive() {
      return isInteractive;
   }
   public void setInteractive(boolean interactive) {
      this.isInteractive = interactive;
   }
   
   /**
    * @param isTraceCaptureEnabled The isTraceCaptureEnabled to set.
    */
   public boolean isTraceCaptureEnabled() {
      return isTraceCaptureEnabled;
   }
   public void setTraceCaptureEnabled(boolean isTraceCaptureEnabled) {
      this.isTraceCaptureEnabled = isTraceCaptureEnabled;
   }

   /**
    * Evaluate predicate at the end of the test?
    * 
    * @param evaluatePassFail
    */
   public boolean isEvaluatePassFail() {
      return evaluatePassFail;
   }
   public void setEvaluatePassFail(boolean evaluatePassFail) {
      this.evaluatePassFail = evaluatePassFail;
   }
   
   public int getTestCompletionDelay() {
      return testCompletionDelay;
   }
   public void setTestCompletionDelay(int testCompletionDelay) {
      this.testCompletionDelay = testCompletionDelay;
   }
   
   public int getTraceGenerationTime() {
      return traceGenerationTime;
   }
   public void setTraceGenerationTime(int traceGenerationTime) {
      this.traceGenerationTime = traceGenerationTime;
   }
   
   // Static methods 
   //---------------------------------------------------------------------
   public static String getSipDebugLog() {
      return sipDebuglog;
   }

   public static String getRtpMessageLog() {
      return rtpMessagelog;
   }

   public static String getRtpDebugLog() {
      return rtpDebuglog;
   }
}
