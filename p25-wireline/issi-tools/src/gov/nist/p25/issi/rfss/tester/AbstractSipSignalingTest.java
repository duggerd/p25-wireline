//
package gov.nist.p25.issi.rfss.tester;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.TimerTask;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SuState;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.rfss.SipStackFactory;
import gov.nist.p25.issi.rfss.RFSS;
import gov.nist.p25.issi.rfss.SuInterface;
import gov.nist.p25.issi.rfss.ServiceAccessPoints;
import gov.nist.p25.issi.testlauncher.TestHarness;
import gov.nist.p25.issi.transctlmgr.ptt.SmfPacketEvent;
import gov.nist.p25.issi.transctlmgr.ptt.SmfRxListener;
import gov.nist.p25.issi.utils.ProtocolObjects;

import org.apache.log4j.Logger;
import junit.framework.TestCase;

/**
 * This is the general pattern for all test cases.
 */
public abstract class AbstractSipSignalingTest extends TestCase
   implements SipListener, SmfRxListener {
   
   private static Logger logger = Logger.getLogger(AbstractSipSignalingTest.class);
   private static boolean verbose = false;
   public static void showln(String s) { System.out.println(s); }

   private TopologyConfig topologyConfig;
   private Hashtable<SipProvider, SipListener> providerTable;
   private Hashtable<String, AbstractScenario> scenarios;
   private Hashtable<String, RfssConfig> currentSuLocation;
   private String currentScenarioName;
   private TestScript testScript;
   private HashSet<AbstractScenario> messageTriggeredScenarios;
   private boolean isFailed;
   
   //private HashMap<SipProvider,AbstractSipSignalingTest> listenerMap = 
   //   new HashMap<SipProvider,AbstractSipSignalingTest>();

   /**
    * To signal the remote test harness that the max time for execution of test
    * cases is done.
    */
   class ScenarioTimer extends TimerTask {
      public ScenarioTimer() {

      }

      @Override
      public void run() {
         doCompletion();
         logger.info("Test Completed : you can run the next test");
         providerTable.clear();
         ProtocolObjects.stop();
         
         logger.info("RFSS: shutDownAll...resetTimer");
         RFSS.shutDownAll();
         ISSITimer.resetTimer();
      }
   }

   class ScenarioExecutor extends TimerTask {
      AbstractScenario scenario;

      ScenarioExecutor(AbstractScenario scenario) {
         this.scenario = scenario;
         scenario.setTimerTask(this);
      }

      @Override
      public void run() {
         try {
            if (testScript.isInteractive() && scenario.isTimeTriggered()) {
               scenario.doWait();
            }
            AbstractSipSignalingTest.this.runScenario(scenario);

         } catch (Exception ex) {
            logger.error("unnexpected exception ", ex);
            fail("Unexpected exception");
         }
      }
   }

   protected AbstractSipSignalingTest() {
      this.scenarios = new Hashtable<String, AbstractScenario>();
      this.providerTable = new Hashtable<SipProvider, SipListener>();
      this.currentSuLocation = new Hashtable<String, RfssConfig>();
      this.messageTriggeredScenarios = new HashSet<AbstractScenario>();
   }

   private void addSu(SuConfig suConfig, SuInterface su) {
      suConfig.setSU(su);
      RfssConfig rfssConfig = currentSuLocation.get(suConfig.getRadicalName());
      if (rfssConfig != null && rfssConfig.getRFSS() != null) {
         ServiceAccessPoints saps = rfssConfig.getRFSS().getServiceAccessPoints(suConfig);
         su.setServiceAccessPoints(saps);
      }
      else {
         logger.debug("addSu(): rfssConfig or RFSS is null, saps not created");
      }
   }

   private void setUp(TopologyConfig topologyConfig) throws Exception {
      this.topologyConfig = topologyConfig;
      for (SuConfig suConfig : topologyConfig.getSuConfigurations()) {
         currentSuLocation.put(suConfig.getRadicalName(), suConfig.getInitialServingRfss());
      }
   }

   // ////////////////////////////////////////////////////////////////
   // Methods that get invoked when the tester runs in self test mode.
   // The provider is used to multiplex requests to different emulated
   // RFSSs
   // ////////////////////////////////////////////////////////////////
   /**
    * Redirect a request event
    * 
    * @param sipEvent
    */
   private void redirectEvent(RequestEvent sipEvent) {
      SipProvider sipProvider = (SipProvider) sipEvent.getSource();
      SipListener sipListener = providerTable.get(sipProvider);
      sipListener.processRequest(sipEvent);
   }

   private void redirectEvent(ResponseEvent sipEvent) {
      SipProvider sipProvider = (SipProvider) sipEvent.getSource();
      SipListener sipListener = providerTable.get(sipProvider);
      sipListener.processResponse(sipEvent);
   }

   private void redirectEvent(TimeoutEvent sipEvent) {
      /*
       * SipProvider sipProvider = (SipProvider) sipEvent.getSource();
       * SipListener sipListener = providerTable.get(sipProvider);
       * sipListener.processTimeout(sipEvent);
       */
      logger.error("A timeout occured!");
   }

   private void redirectEvent(TransactionTerminatedEvent sipEvent) {
      SipProvider sipProvider = (SipProvider) sipEvent.getSource();
      SipListener sipListener = providerTable.get(sipProvider);
      if (sipListener != null) {
         sipListener.processTransactionTerminated(sipEvent);
      } else {
         logger.debug("Could not redirect event -- null listener!");
      }
   }

   /**
    * Do the actions necessary after the completion of the test. This is
    * triggered by either a timer action or by the user signaling that 
    * the test is complete and he wants to evaluate a predicate.
    */
   private void doCompletion() {
      try {
         TestHarness th = AbstractSipSignalingTest.this.getRfssConfig().getRFSS().getTestHarness();
         for (RfssConfig rfssConfig : this.topologyConfig.getRfssConfigurations()) {
            RFSS rfss = rfssConfig.getRFSS();
            if (rfss != null) {
               // Are we asked to capture the trace?
               if (testScript.isTraceCaptureEnabled()) {
                  rfss.saveTrace();
               } else {
                  //logger.debug(rfssConfig.getRfssName()+": Not saving -- trace capture disabled");
               }

               //NOTE: test pass/fail is controlled by isEvaluatePassFail
               //logger.debug("doCompletion: RFSS-id="+rfss.getRfssConfig().getRfssIdString()+
               //   " >>> isEvaluatePassFail="+ testScript.isEvaluatePassFail());

               if (testScript.isEvaluatePassFail()) {
                  logger.debug("Evaluating post condition for  " + getRfssConfig());
                  if (!rfss.testCapturedMessages()) {
                     rfss.getTestHarness().fail("Post condition assertion test failed on RFSS "
                                 + rfss.getRfssConfig().getRfssId());
                     isFailed = true;
                  } else {
                     logger.info("Post condition assertion test passed. ");
                  }
               }

            } else {
               logger.debug("rfss is null, not evaluating post condition: "+rfssConfig.getRfssName());
            }
         }
         for (AbstractScenario scenario : scenarios.values()) {
            TimerTask ttask = scenario.getTimerTask();
            if (ttask != null) {
               ttask.cancel();
            }
         }
         scenarios.clear();
         // This may cause problem when cancel outside the ISSITimer
         //ISSITimer.getTimer().cancel();
         //ISSITimer.getTimer().purge();
         ISSITimer.resetTimer();         
         th.logTestCompleted(currentScenarioName);
         
      } catch (Exception ex) {
         logger.error("Unexpected exception completing test ", ex);
      }
   }

   /**
    * Run the given scenario.
    * 
    * @param scenario
    */
   private void runScenario(AbstractScenario scenario) {
      for (TimeTrigger trigger : scenario.getPostTrigger()) {
         int time = trigger.getTime();
         if( trigger instanceof MsecTimeTrigger) {
           logger.debug("Scheduling MsecTimeTrigger: time="+time);
           ISSITimer.getTimer().schedule( new ScenarioExecutor(trigger.getScenario()), time);
         } else {
           logger.debug("Scheduling depending scenarios: trigger-time="+time);
           ISSITimer.getTimer().schedule( new ScenarioExecutor(trigger.getScenario()), time*1000);
         }
      }

      if (scenario instanceof SuToSuCallSetupScenario) {
         doCallSetup(scenario);
      } else if (scenario instanceof RoamingScenario) {
         doRoam(scenario);
      } else if (scenario instanceof GroupCallSetupScenario) {
         doGroupCallSetup(scenario);
      } else if (scenario instanceof SuScript) {
         AbstractSipSignalingTest.this.doSuAction(scenario);
      } else if (scenario instanceof RfssScript) {
         AbstractSipSignalingTest.this.doRfssAction(scenario);
      } else if (scenario instanceof CallTerminator) {
         AbstractSipSignalingTest.this.doCallTeardown(scenario);
      }
   }

   /**
    * Register an event driven scenario with this test.
    * 
    * @param scenario
    * @param method
    * @throws Exception
    */
   private void registerScenario(AbstractScenario scenario, String method)
         throws Exception {
      scenario.setMethodName(method);
      this.scenarios.put(scenario.getId(), scenario);
      if (!(scenario.getTrigger() instanceof TimeTrigger)) {
         logger.debug("registering message driven scenario");
         this.messageTriggeredScenarios.add(scenario);
      }
   }

   /**
    * This method will return the RFSS for a given remote SU.
    * 
    * @param suConfig
    * @return -- the RFSS
    */
   private RFSS getCurrentRfssForSU(SuConfig suConfig) {
      RfssConfig config = currentSuLocation.get(suConfig.getRadicalName());
      return (config==null) ? null : config.getRFSS();
   }

   private void redirectEvent(DialogTerminatedEvent sipEvent) {
      SipProvider sipProvider = (SipProvider) sipEvent.getSource();
      SipListener sipListener = providerTable.get(sipProvider);
      if (sipListener != null) {
         sipListener.processDialogTerminated(sipEvent);
      }
   }

   public void processRequest(RequestEvent requestEvent) {
      logger.debug("processRequest: " + requestEvent.getRequest().getMethod());
      for (AbstractScenario scenario : messageTriggeredScenarios) {
         if (scenario.getTrigger().isReady( getRfssConfig().getDomainName(), requestEvent.getRequest())) {
            this.runScenario(scenario);
         }
      }
      redirectEvent(requestEvent);
   }

   public void processResponse(ResponseEvent responseEvent) {
      redirectEvent(responseEvent);
   }

   public void processTimeout(TimeoutEvent timeoutEvent) {
      redirectEvent(timeoutEvent);
   }

   public void processIOException(IOExceptionEvent arg0) {
      fail("Unexpected exception");
   }

   public void processTransactionTerminated( TransactionTerminatedEvent transactionTerminatedEvent) {
      redirectEvent(transactionTerminatedEvent);
   }

   public void processDialogTerminated( DialogTerminatedEvent dialogTerminatedEvent) {
      redirectEvent(dialogTerminatedEvent);
   }

   // SmfRxListener Methods
   public void receivedPttStart(SmfPacketEvent event) {
      logger.debug("[SmfRxListener] GOT SPURT START");
   }

   public void receivedPttEnd(SmfPacketEvent event) {
      logger.debug("[SmfRxListener] GOT SPURT END");
   }

   public void receivedPttProgress(SmfPacketEvent event) {
      logger.debug("AbstractSipSignalingTest: [SmfRxListener] GOT AUDIO");
      for (AbstractScenario scenario : this.messageTriggeredScenarios) {
         if (scenario.getTrigger().isReady( getRfssConfig().getDomainName(),event.getPttPacket())) {
            this.runScenario(scenario);
         }
      }
   }

   // JUNIT Test Case override method
   @Override
   public void tearDown() {
      logger.info("Done running the test!");
      ProtocolObjects.stop();
      providerTable.clear();
      RFSS.shutDownAll();
   }

   /**
    * Abort the current test.
    */
   public void abort() {
      for (AbstractScenario scenario : scenarios.values()) {
         TimerTask ttask = scenario.getTimerTask();
         if ( ttask != null) ttask.cancel();
      }
      scenarios.clear();
      ISSITimer.resetTimer();
   }

   /**
    * This method gets called for stand alone emulation.
    */
   public void testCallSetup() {
      try {
         this.registerSubscriberUnits();
         for (AbstractScenario scenario : this.scenarios.values()) {
            if (scenario.isTimeTriggered()) {
               int time = scenario.getTriggerAsTimeTrigger().getTime();
               Trigger trigger = scenario.getTrigger();
               if( trigger instanceof MsecTimeTrigger) {
                  logger.debug("Scheduling MsecTimeTrigger for "+time);
                  ISSITimer.getTimer().schedule( new ScenarioExecutor(scenario), time);
               } else {
                  logger.debug("Scheuling time triggered scenario for "+time);
                  ISSITimer.getTimer().schedule( new ScenarioExecutor(scenario), time*1000);
               }
            } else {
               logger.debug("scenario is not time triggered");
            }
         }
         //showln("**** Why sleep TEST_RUNS_FOR seconds...");
         Thread.sleep(ISSITesterConstants.TEST_RUNS_FOR);
         //showln("**** Wakeup from sleep TEST_RUNS_FOR seconds...");

         for (RFSS rfss : RFSS.getAllRfss()) {
            // RFSS rfss = rfssConfig.getRFSS();
            if (rfss != null) {
               if (testScript.isTraceCaptureEnabled()) {
                  rfss.saveTrace();
               }
               for (PostCondition pc: testScript.getPostConditions()) {
                  if (!pc.testPostCondition(rfss, testScript, topologyConfig)) {
                     rfss.getTestHarness().fail("Post condition assertion test failed.");
                  }
               }
            }
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         fail("unexpected exception ");
      }
   }

   /**
    * This method gets called from the tester.
    */
   public void doCallSetup() {
      try {
         if (this.scenarios.size() == 0) {
            logger.info("No scenario to execute !");
         }
         //logger.debug("topologyConfig = " + this.topologyConfig);
         this.registerSubscriberUnits();
         for (AbstractScenario scenario : this.scenarios.values()) {
            if (scenario.isTimeTriggered()) {
               int time = scenario.getTriggerAsTimeTrigger().getTime();
               Trigger trigger = scenario.getTrigger();
               if( trigger instanceof MsecTimeTrigger) {
                  if(verbose)
                  logger.debug("Scheduling MsecTimeTrigger for "+time);
                  ISSITimer.getTimer().schedule( new ScenarioExecutor(scenario), time);
               } else {
                  if(verbose)
                  logger.debug("Scheduling time triggered scheario for "+time);
                  ISSITimer.getTimer().schedule( new ScenarioExecutor(scenario), time*1000);
               }
            } else {
               if(verbose)
                  logger.debug("Scenario is not time triggered");
            }
         }

         if (!testScript.isInteractive()) {
            // Generate Ref trace path ?
            if(verbose)
               logger.debug("new ScenarioTimer: isInteractive="+testScript.isInteractive());
            ISSITimer.getTimer().schedule(new ScenarioTimer(),
                  ISSITesterConstants.TEST_RUNS_FOR);
         }

      } catch (Exception ex) {
         logger.fatal("Unexpected exception ", ex);
         fail("unexpected exception ");
      }
   }

   /**
    * Register an event driven scenario with the test framework.
    * 
    * @param scenario -- scenario to register.
    * @throws Exception
    */
   public void registerScenario(AbstractScenario scenario) throws Exception {
      String method = null;
      if (scenario instanceof SuToSuCallSetupScenario) {
         method = "doCallSetup";
      } else if (scenario instanceof RoamingScenario) {
         method = "doRoam";
      } else if (scenario instanceof GroupCallSetupScenario) {
         method = "doGroupCall";
      } else if (scenario instanceof SuScript) {
         method = "runScript";
      } else if (scenario instanceof RfssScript) {
         method = "runScript";
      } else if (scenario instanceof CallTerminator) {
         method = "doCallTeardown";
      } else {
         throw new Exception("Unsupported scenario");
      }
      this.registerScenario(scenario, method);
   }

   /**
    * Return the failure flag.
    * 
    * @return
    */
   public boolean isFailed() {
      return isFailed;
   }

   /**
    * Return true if this is interactive mode.
    * 
    * @return
    */
   public boolean isInteractive() {
      return testScript.isInteractive();
   }

   /**
    * This method is called to execute a call setup action.
    * 
    * @param abstractScenario -- the call setup scenario descriptor.
    */
   public void doCallSetup(AbstractScenario abstractScenario) {

      this.currentScenarioName = abstractScenario.getId();
      
      SuToSuCallSetupScenario scenario = (SuToSuCallSetupScenario) abstractScenario;
      SuConfig calledSuConfig = scenario.getCalledSuConfig();
      SuConfig callingSuConfig = scenario.getCallingSuConfig();
      
      logger.info("doCallSetup: "+ currentScenarioName +
                  " callingSu: " + scenario.getCallingSuConfig().getSuName() +
                  " calledSu: " + scenario.getCalledSuConfig().getSuName());

      TestSU callingSu = (TestSU) callingSuConfig.getSU();
      if (callingSu == null) {
         if (getRfssConfig() != null) {
            callingSu = new TestSU(callingSuConfig, getRfssConfig().getRFSS().getTestHarness());
	 } else {
            callingSu = new TestSU(callingSuConfig, new TestHarness());
         }
         addSu(callingSuConfig, callingSu);
      }

      TestSU calledSu = (TestSU) calledSuConfig.getSU();
      if (calledSu == null) {
         if (getRfssConfig() != null) {
            calledSu = new TestSU(calledSuConfig, getRfssConfig().getRFSS().getTestHarness());
	 } else {
            calledSu = new TestSU(calledSuConfig, new TestHarness());
         }
         addSu(calledSuConfig, calledSu);
      }
      callingSu.setScenario(scenario);
      calledSu.setScenario(scenario);

      TestSU callingSU = (TestSU) scenario.getCallingSuConfig().getSU();

      // This method is invoked only at the initial serving RFSS of the
      // calling SU. Note that the test for rfssConfig being null indicates
      // that this is a self test.

      if (logger.isDebugEnabled() && verbose) {
         logger.debug("rfssConfig = " + getRfssConfig());
         logger.debug("currentSuLocation = " + currentSuLocation.get(callingSuConfig.getRadicalName()));
      }
      if (getRfssConfig() == null ||
          getRfssConfig() == currentSuLocation.get(callingSuConfig.getRadicalName())) {
         callingSU.doCallSetup(scenario);
      }
   }

   /**
    * This method is called to execute a group call setup action.
    * 
    * @param abstractScenario -- the call setup scenario descriptor
    */
   public void doGroupCallSetup(AbstractScenario abstractScenario) {
      try {
         logger.debug("doGroupCallSetup " + "topologyConfig = "
               + topologyConfig + " configuratios = "
               + topologyConfig.getSuConfigurations());
         GroupCallSetupScenario scenario = (GroupCallSetupScenario) abstractScenario;
         for (SuConfig suConfig: topologyConfig.getSuConfigurations()) {
            TestSU su = (TestSU) suConfig.getSU();
            if (su == null) {
               if (getRfssConfig() != null) {
                  su = new TestSU(suConfig, getRfssConfig().getRFSS().getTestHarness());
	       } else {
                  su = new TestSU(suConfig, new TestHarness());
               }
               addSu(suConfig, su);
            }
            su.setScenario(scenario);
         }

         SuConfig callingSu = scenario.getCallingSuConfig();
         TestSU callingtester = (TestSU) callingSu.getSU();

         // Invoke this method only from the inital serving rfss of the calling SU.
         if (getRfssConfig() == null ||
             getRfssConfig() == currentSuLocation.get(callingSu.getRadicalName())) {
            callingtester.doGroupCallSetup(scenario);
         }
      } catch (Exception ex) {
         String errorString = "unexpected exception";
         getRfssConfig().getRFSS().getTestHarness().fail(errorString, ex);
         logger.error(errorString);
      }
   }

   /**
    * This method is called to execute a roaming action.
    * 
    * @param scenario -- the call setup scenario descriptor
    */
   public void doRoam(AbstractScenario scenario) {
      try {
         // At the target location, get the SAP from the
         // target RFSS and issue a registration from there.
         RoamingScenario roamingScenario = (RoamingScenario) scenario;
         //logger.debug("doRoam(): scenario=" + scenario);
         logger.debug("doRoam(): scenario-id=" + scenario.getId());
         RfssConfig targetRfss = roamingScenario.getDestinationRfssConfig();
         SuConfig suConfig = roamingScenario.getSuConfig();

         TestSU su = (TestSU) suConfig.getSU();
         logger.debug("doRoam(): suConfig.getSU()="+su +" getRfssConfig()="+getRfssConfig());
         if (su == null) {
            //logger.debug("doRoam(): new TestSU, addSu()...");
            if (getRfssConfig() != null) {
               su = new TestSU(suConfig, getRfssConfig().getRFSS().getTestHarness());
	    } else {
               su = new TestSU(suConfig, new TestHarness());
            }
            addSu(suConfig, su);
         }

         logger.debug("doRoma(): isInitiatedByHome="+roamingScenario.isIntiatedByHome());

         // Just set up the serving Rfss.
         // the timer there will do the rest.
         if (!roamingScenario.isIntiatedByHome()) {
            if (getCurrentRfssForSU(suConfig) != null) {
               // This is the case BUGBUG
               //getCurrentRfssForSU(suConfig).removeServedSubscriberUnit(suConfig);
               logger.debug("doRoam(): getCurrentRfssForSU NOT null! NO removeServedSubscriberUnit");
            } else {
               logger.debug("doRoam(): getCurrentRfssForSU is null!");
            }

         } else {
            if (suConfig.getHomeRfss() == getRfssConfig()
                  || getRfssConfig() == null) {
               RFSS rfss = suConfig.getHomeRfss().getRFSS();
               rfss.getMobilityManager().getUnitToUnitMobilityManager().sendRegisterDeRegister(suConfig.getSU());
            }
         }

logger.debug("doRoam(): targetRfss.getRFSS="+targetRfss.getRFSS());

         if (targetRfss.getRFSS() != null && getRfssConfig() == targetRfss) {
            logger.debug("doRoam(): add subscriber: "+suConfig.getSuName() +
                         " to targetRfss: " + targetRfss.getRfssName());

//TODO: change the serving RFSS from rfss_2 to rfss_5 
logger.debug("doRoam(): FIX FIX suConfig.servingRfss="+targetRfss.getRfssName());
suConfig.setServingRfss( targetRfss);
if(suConfig.getSU() != null) {
   logger.debug("doRoam(): FIX FIX suConfig.getState="+suConfig.getSU().getState());
   suConfig.getSU().setState(SuState.ON);
   logger.debug("doRoam(): FIX FIX suConfig.setState=SuState.ON");
}
boolean bflag = targetRfss.getRFSS().isSubscriberUnitServed(suConfig);
logger.debug("doRoam(): isSubscriberUnitServed-bflag="+bflag +
             " ====> RefMeFor="+ suConfig.getServingRfssReferencesMeFor());

            assert (!targetRfss.getRFSS().isSubscriberUnitServed(suConfig));
            targetRfss.getRFSS().addServedSubscriberUnit(suConfig);
            if (suConfig.getServingRfssReferencesMeFor() == 0
                  && targetRfss == getRfssConfig()) {
               logger.debug("doRoam(): RFSS.suArrived()...");
               targetRfss.getRFSS().suArrived(suConfig);
            }
            logger.debug("doRoam(): RFSS.getServiceAccessPoints()...");
            ServiceAccessPoints saps = targetRfss.getRFSS().getServiceAccessPoints(suConfig);
            su.setServiceAccessPoints(saps);
            logger.debug("doRoam(): saps="+saps);
         }

         //logger.debug("doRoam(): currentSuLocation="+targetRfss.getRfssName());
         //logger.debug("doRoam(): put(targetRfss): "+suConfig.getRadicalName());
         currentSuLocation.put(suConfig.getRadicalName(), targetRfss);

      } catch (Exception ex) {
         String errorString = "unexpected exception ";
         logger.error(errorString, ex);
         getRfssConfig().getRFSS().getTestHarness().fail(errorString);
      }
   }

   private void registerSubscriberUnits() {
      //logger.debug("registerSubscriberUnits(): START...");
      for (SuConfig suConfig :  this.topologyConfig.getSuConfigurations()) {
         try {
            if (suConfig.getSU() == null) {
               TestSU su;
               if (getRfssConfig() != null) {
                  su = new TestSU(suConfig, getRfssConfig().getRFSS().getTestHarness());
               } else {
                  su = new TestSU(suConfig, new TestHarness());
               }
               addSu(suConfig, su);
            }
            if (logger.isDebugEnabled() && verbose) {
               logger.debug("rfssConfig = " + getRfssConfig());
               logger.debug("currentSuLocation = " + currentSuLocation.get(suConfig.getRadicalName()));
            }

            if (getRfssConfig() == null ||
                getRfssConfig() == currentSuLocation.get(suConfig.getRadicalName())) {
               suConfig.getSU().setState(SuState.ON);
            }
         } catch (Exception ex) {
            logger.error("registerSubscriberUnits- exception: ", ex);
         }
      }  // for-loop
      //logger.debug("registerSubscriberUnits(): DONE...");
   }

   public void doSuAction(AbstractScenario scenario) {
      SuScript testSuEventScenario = (SuScript) scenario;
      try {
         SuConfig suConfig = testSuEventScenario.getSuConfig();
         if (suConfig.getSU() == null) {
            TestSU su;
            if (getRfssConfig() != null) {
               su = new TestSU(suConfig, getRfssConfig().getRFSS().getTestHarness());
            } else {
               su = new TestSU(suConfig, new TestHarness());
            }
            addSu(suConfig, su);
         }
         if (logger.isDebugEnabled()) {
            logger.debug("rfssConfig = " + getRfssConfig());
            logger.debug("currentSuLocation = " + currentSuLocation.get(suConfig.getRadicalName()));
            logger.debug("scenario = " + scenario);
         }
         if (getRfssConfig() == null ||
             getRfssConfig() == currentSuLocation.get(suConfig.getRadicalName())) {
            testSuEventScenario.runScript(this.topologyConfig);
         }
      } catch (Exception ex) {
         String errorString = "unexpected exception ";
         logger.error(errorString, ex);
      }
   }
   
   public void doCallTeardown(AbstractScenario scenario) {
      CallTerminator callTerminator = (CallTerminator) scenario;
      try {
         SuConfig suConfig = callTerminator.getSuConfig();
         logger.debug("doCallTeardown: " + suConfig.getSuName() + 
                      " rfss=" + getRfssConfig().getRfssName());
         
         if (getRfssConfig() == null ||
             getRfssConfig() == currentSuLocation.get(suConfig.getRadicalName())) {
            callTerminator.terminateCallSegment();
         }
      } catch (Exception ex) {
         String errorString = "unexpected exception ";
         logger.error(errorString, ex);
      }
   }

   public void doRfssAction(AbstractScenario scenario) {
      logger.info("doRfssAction");
      RfssScript testRfssEventScenario = (RfssScript) scenario;
      try {
         if (getRfssConfig() == null
            || testRfssEventScenario.getRfssConfig() == getRfssConfig()) {
            testRfssEventScenario.runScript(this.topologyConfig);
         }
      } catch (Exception ex) {
         String errorString = "unexpected exception ";
         logger.error(errorString, ex);
      }
   }

   /**
    * Signal the wait loop that the user is giving the go ahead for
    * the next action.
    *
    * @param scenarioName -- the test scenario name.
    * @throws Exception
    */
   public void executeNextScenario(String scenarioName) throws Exception {
      AbstractScenario scenario = this.scenarios.get(scenarioName);
      if (scenario == null) {
         throw new Exception("Cannot find named scenario: " + scenarioName);
      }
      scenario.doNotify();
   }

   /**
    * Setup method for Test case.
    * 
    * @param testScript -- the test script to execute.
    * @throws Exception
    */
   public void setUp(TestScript testScript) throws Exception {
      if (ProtocolObjects.isRunning()) {
         throw new Exception ("Previous Test is Still Running");
      }
      TopologyConfig topologyConfig = testScript.getTopologyConfig();
      this.testScript = testScript;
      this.setUp(topologyConfig);
      this.topologyConfig = topologyConfig;
      for (RfssConfig rfssConfig : topologyConfig.getRfssConfigurations()) {
         SipStack sipStack = SipStackFactory.getSipStack(rfssConfig.getRfssName(), topologyConfig);
         ListeningPoint listeningPoint = sipStack.createListeningPoint(
               rfssConfig.getIpAddress(), rfssConfig.getSipPort(), "udp");
         SipProvider sipProvider = sipStack.createSipProvider(listeningPoint);
         RFSS rfss = RFSS.create(sipProvider, rfssConfig, testScript);
         rfss.setCurrentTestCase(this);
         providerTable.put(sipProvider, rfss);
      }

      for (SipProvider sipProvider : providerTable.keySet()) {
         sipProvider.addSipListener(this);
         //listenerMap.put(sipProvider, this);
      }
      ProtocolObjects.start();
      for (AbstractScenario as : testScript.getScenariosValues()) {
         this.registerScenario(as);
      }
   }

   /**
    * This method is invoked when we want to run as a stand alone tester.
    * (i.e. not in self test mode).
    * 
    * @param testConfig -- the test configuration for the test case to run.
    * @param provider -- the provider assigned to this test case.
    * @throws Exception
    */
   public void setUp(TestScript testConfig, SipProvider provider) throws Exception {
      TopologyConfig topologyConfig = testConfig.getTopologyConfig();
      this.testScript = testConfig;
      this.setUp(topologyConfig);
      RfssConfig rfssConfig = getRfssConfig();
      RFSS rfss = rfssConfig.getRFSS();

      // Populate the redirect table.
      providerTable.put(provider, rfss);
      for (AbstractScenario as : testConfig.getScenariosValues()) {
         this.registerScenario(as);
      }
         
//      AbstractSipSignalingTest obj = listenerMap.get(provider);
//      showln("??? TooManyListener: obj= "+ obj);
//      if( obj != null) {
//         showln("+++ in listenerMap... removeSipListener provider="+provider);
//         provider.removeSipListener( obj);
//      }
//      provider.addSipListener(this);
//      listenerMap.put(provider, this);

      provider.addSipListener(this);
   }

   public void startCompletionTimer() {
      logger.debug("startingCompletionTimer: rfssId=" + getRfssConfig().getRfssId());
      ISSITimer.getTimer().schedule(new ScenarioTimer(), ISSITesterConstants.TEST_RUNS_FOR);
   }
   
   /**
    * Get the RFSS configuration to which we are assigned.
    * 
    * @return -- the rfss configuration. Null if in self test mode.
    */
   public abstract RfssConfig getRfssConfig();
}
