//
package gov.nist.p25.issi.testlauncher;

/**
 * The interface that is implemented by the local test controller and 
 * the remote test controller.
 */
public interface TestControllerInterface {
   /**
    * Get the SIP Traces from the log
    */
   public String getStackSipLog() throws Exception;
   
   /**
    * Get the SIP traces from all emulated nodes.
    * @return
    * @throws Exception
    */
   public String getStackSipLogs() throws Exception;
   
   /** 
    * Get the ptt traces.
    */
   public String getStackPttLog() throws Exception;
   
   /** 
    * Get the ptt traces.
    */
   public String getStackPttLogs() throws Exception;

   /**
    * Retrieve the captured traces from the packet monitor. Sends a http
    * request to the packet monitor to get the trace from there.
    * 
    */
   public abstract String getSipTraces() throws Exception;


   // CSSI support
   public abstract String getRfssConfigs() throws Exception;

   /**
    * This method is called during interactive testing to tell the tester to
    * tear down the current test case.
    */
   public abstract void tearDownCurrentTest() throws Exception;

   /**
    * Signal that the test has completed.
    * 
    * @throws Exception
    */
   public abstract void signalTestCompletion() throws Exception;

   /**
    * Get Pass/fail for the current test.
    * 
    * @return
    * @throws Exception
    */
   public abstract boolean getTestResults() throws Exception;
   
   /**
    * Get the error log.
    * 
    * @return the error log.
    * @throws Exception if the error log is bad.
    */
   public abstract String getErrorLog() throws Exception;

   /**
    * Get the Rfss information from each of the Rfss. Returns a hash table
    * containing the relevant information from the each of the emulated RFSS.
    * The hash table is indexed by the domain name of the Rfss.
    */
   public abstract String getRfssPttSessionInfo(boolean header) throws Exception;

   /**
    * Retrieve the captured PTT traces.
    */
   public abstract String getPttTraces() throws Exception;

   /**
    * Load the test.
    * 
    * @param scenario
    * @param testNumber
    * @param interactiveFlag
    * @param evaluatePassFail -- evaluate pass or fail at the end of the test.
    * @throws Exception
    */
   public abstract void loadTest(String scenarioDir, String scenario, 
      String testNumber, String topology, String globalTopology,
      String systemTopology, boolean interactiveFlag, boolean evaluatePassFail)
         throws Exception;
   
   /**
    * Run the previously loaded test.
    */
   public void runTest() throws Exception;
   
   /**
    * This method is called in interactive mode to tell the tester to run to
    * the given scenario.
    * 
    * @param scenario
    * @throws Exception
    */
   public void signalNextScenario(String scenario) throws Exception;

   /**
    * Reset the remote controller - reset all connections and let to 
    * of all local state - the test is over.
    */
   public void reset();
   
   /**
    * Checks if all the signaling has been seen for an emulated RFSS ( allows
    * for early termination of successful tests ).
    */
   public boolean isTestCompleted() throws Exception;

   public boolean isSaveTraceCompleted() throws Exception;
}
