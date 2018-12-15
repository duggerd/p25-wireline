//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SuState;
import gov.nist.p25.issi.p25body.CallParamContent;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25payload.TransmitPriorityType;
import gov.nist.p25.issi.rfss.CallControlSAP;
import gov.nist.p25.issi.rfss.CallSetupConfirmEvent;
import gov.nist.p25.issi.rfss.CallSetupRequestEvent;
import gov.nist.p25.issi.rfss.CallTeardownIndicateEvent;
import gov.nist.p25.issi.rfss.CallTeardownResponseEvent;
import gov.nist.p25.issi.rfss.DeregistrationResponseEvent;
import gov.nist.p25.issi.rfss.GroupServing;
import gov.nist.p25.issi.rfss.MobilityManagementSAP;
import gov.nist.p25.issi.rfss.RFSS;
import gov.nist.p25.issi.rfss.RegisterEvent;
import gov.nist.p25.issi.rfss.RegistrationResponseEvent;
import gov.nist.p25.issi.rfss.ServiceAccessPoints;
import gov.nist.p25.issi.rfss.SuInterface;
import gov.nist.p25.issi.rfss.TransmissionControlSAP;
import gov.nist.p25.issi.rfss.UnitToUnitCall;
import gov.nist.p25.issi.rfss.UnitToUnitCallControlResponseEvent;
import gov.nist.p25.issi.testlauncher.TestHarness;
//import gov.nist.p25.issi.transctlmgr.TransmissionControlSAP;
import gov.nist.p25.issi.utils.ThreadedExecutor;
import gov.nist.p25.issi.utils.WarningCodes;

import java.util.TimerTask;
import javax.sip.DialogState;
import javax.sip.ServerTransaction;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

/**
 * This is an abstraction for a Test Subscriber Unit ( SU ). The Test SU is
 * called by the test driver to send requests and expect defined responses for
 * test cases.
 */
@SuppressWarnings("unused")
public class TestSU implements SuInterface {

   private static Logger logger = Logger.getLogger(TestSU.class);

   private CallControlSAP ccSap;
   private SuConfig peerSuConfig;
   private TransmissionControlSAP tcSap;
   private MobilityManagementSAP mmSap;
   private boolean gotCCSetupResponse;
   private UnitToUnitCall callSegment;
   private GroupServing groupServing;
   private boolean byeOKReceived;
   private boolean byeSent;
   private boolean isCaller;
   private boolean okSent;
   private boolean ackReceived;
   private ServiceAccessPoints saps;
   private SuConfig suConfig;

   private boolean gotRegisterDeregister;
   private boolean gotGroupCallSetupResponseEvent;

   private TransmitPriorityType transmitPriorityType = TransmitPriorityType.NORMAL;
   private int transmissionPriorityLevel = 0;
   private GroupConfig groupToCall;
   private TestHarness testHarness;
   private SuState suState;
   private AbstractScenario scenario;
   private boolean cancelSent;

   class SuToSuCallCancelTimerTask extends TimerTask {
      private UnitToUnitCall callSegment;

      SuToSuCallCancelTimerTask(UnitToUnitCall callSegment) {
         this.callSegment = callSegment;
      }

      @Override
      public void run() {
         try {
            TestSU.this.ccSap.ccCancelCallSegment(this.callSegment);
         } catch (Exception ex) {
            testHarness.fail("Unexpected exception", ex);
         }
      }
   }

   class DelayTask implements Runnable {
      private CallSetupRequestEvent ccRequestEvent;

      public DelayTask(CallSetupRequestEvent ccRequestEvent) {
         this.ccRequestEvent = ccRequestEvent;

      }
      private void sleep(long msec) {
         try {
            Thread.sleep( msec);
         } catch(Exception ex) { }
      }

      public void run() {

         // send redundant provisional responses (one every minute -- 
         // see RFC 3216 section 13.2.1.1)
         int count = suConfig.getInviteProcessingDelay() / 60;
         if (count == 0 && suConfig.getInviteProcessingDelay() > 0)
            count = 1;
         SuConfig calledSu = ccRequestEvent.getCallSegment().getCalledSuConfig();
         CallParamContent callParamContent = ccRequestEvent.getCallSegment().getCallParamContent();

         logger.debug("DelayTask: run(): count="+count);
	 boolean isACRequested = false;
	 boolean isACSupported = false;
         for (int niterations = 0; niterations < count; niterations++) {
            try {
               // #557 Remove Ringing for 12.7.1.x AC/DC
               isACRequested=callParamContent.getCallParam().isAvailabilityCheckRequested();
               isACSupported=calledSu.getUserServiceProfile().isAvailabilityCheckSupported();
               logger.debug("run(#557): isACRequested="+isACRequested+" isACSupported="+isACSupported);

               if (getCurrentRfss().getRFSS().isRfResourcesAvailable()) {

                  logger.debug("run(): isRfResourcesAvailable=true RINGING-"+niterations);

                  // #557
                  if (calledSu.getUserServiceProfile().isAvailabilityCheckSupported()) {
                    logger.debug("run(): calledSu.isAvailabilityCheckSupported=true");
                    ccSap.ccSetupIndicateResponse(ccRequestEvent, Response.RINGING);
                    //sleep( 2L);
		  }

               } else {

                  //isACRequested=callParamContent.getCallParam().isAvailabilityCheckRequested();
                  //isACSupported=calledSu.getUserServiceProfile().isAvailabilityCheckSupported();
                  //logger.debug("run(): isACRequested="+isACRequested+" isACSupported="+isACSupported);

                  if (calledSu.getUserServiceProfile().isAvailabilityCheckSupported()) {
                     if (callParamContent.getCallParam().isAvailabilityCheckRequested()) {

                        // #375 12.6.1 <ringing>
			// 12.1.1.3 and 12.1.1.4
                        logger.debug("run(): RINGING instead of SESSION_PROGRESS...");
                        ccSap.ccSetupIndicateResponse(ccRequestEvent, Response.RINGING);
			
			//=== I dont know if the RFC-3612 is applied here !! 
                        ccSap.ccSetupIndicateResponse(ccRequestEvent, Response.SESSION_PROGRESS);

                     } else {
                        break;
                     }
                  } else {
                     break;
                  }
               }

            } catch (Exception e1) {
               logger.error("unexpected exception", e1);
            }

            int timeToSleep = suConfig.getInviteProcessingDelay() - niterations * 60;
            int msec = (timeToSleep < 60) ? timeToSleep : 60;
            try {
               Thread.sleep(msec * 1000);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }  // for-loop
         actuallyHandleSetupIndicate(ccRequestEvent);
      }
   }

   // constructor
   //--------------------------------------------------------------------------
   public TestSU(SuConfig suConfig, TestHarness testHarness) {
      this.suConfig = suConfig;
      this.testHarness = testHarness;
      suState = suConfig.getInitialSuState();
      logger.debug("TestSU: "+suConfig.getSuName()+" initialSuState:"+suState);
   }

   // get the current RFSS where this TestSU is residing.
   private RfssConfig getCurrentRfss() {
      return ccSap.getRfssConfig();
   }

   private void doSendTalkSpurt(SuToSuCallSetupScenario callSetupScenario) {
      if (callSetupScenario.isForce()) {
         ISSITimer.getTimer().schedule(new TimerTask() {
            @Override
            public void run() {
               TestSU.this.forceTalkSpurt();
            }
         }, 1000);

      } else {
         ISSITimer.getTimer().schedule(new TimerTask() {
            @Override
            public void run() {
               TestSU.this.sendTalkSpurt();
            }
         }, 1000);

      }
   }

   // class methods
   public String getCurrentUnitToUnitCallCallId() {
      return callSegment == null ? null : callSegment.getCallID();
   }
   
   /**
    * Send a talk spurt after you get an OK.
    * 
    * @param callSetupScenario
    */
   public void doSendTalkSpurt(GroupCallSetupScenario callSetupScenario) {
      if (callSetupScenario.isTalkSpurtForced()) {
         ISSITimer.getTimer().schedule(new TimerTask() {
            private GroupCallSetupScenario groupCallSetupScenario;

            TimerTask setScenario(GroupCallSetupScenario scenario) {
               this.groupCallSetupScenario = scenario;
               return this;
            }

            @Override
            public void run() {
               TestSU.this.forceTalkSpurt(groupCallSetupScenario.getCalledGroupConfig().getRadicalName());
            }
         }.setScenario(callSetupScenario), 1000);
      }
      else {
         ISSITimer.getTimer().schedule(new TimerTask() {
            private GroupCallSetupScenario groupCallSetupScenario;

            TimerTask setScenario(GroupCallSetupScenario scenario) {
               this.groupCallSetupScenario = scenario;
               return this;
            }

            @Override
            public void run() {
               TestSU.this.sendTalkSpurt(groupCallSetupScenario.getCalledGroupConfig().getRadicalName());
            }
         }.setScenario(callSetupScenario), 1000);
      }
   }

   public void handleRegistrationResponseEvent( RegistrationResponseEvent registrationResponseEvent) {
      Response response = registrationResponseEvent.getResponse();
      if (response.getExpires() == null &&
          response.getStatusCode() == Response.OK) {
         testHarness.fail("No expires header in response");
      }
      FromHeader from = (FromHeader) response.getHeader(FromHeader.NAME);
      ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
      testHarness.assertEquals( "From and To Header Must have Identical Addresses!",
         from.getAddress(), toHeader.getAddress());
      logger.info("handleRegistrationResponseEvent");
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.tester.TestSUInterface#setState(gov.nist.p25.issi.issiconfig.SuState)
    */
   public void setState(SuState suState) {
      logger.debug(this.getSuConfig().getSuName() +
                   " currentSuState:" + this.suState +
                   " setting to newSustate: " + suState);
      //logger.debug("saps is " + saps);
      try {
         SuState oldSuState = this.suState;
         this.suState = suState;
         if (oldSuState == SuState.OFF && suState == SuState.ON) {
            if( mmSap != null)
               mmSap.mmSuArrived();
	    else
               logger.debug("NOTE: mmSap is null, skip mmSuArrived()");
         } else if (oldSuState == SuState.ON && suState == SuState.OFF) {
            if( mmSap != null)
               mmSap.mmSuDeparted();
	    else
               logger.debug("NOTE: mmSap is null, skip mmSuDeparted()");
         }
      } catch (Exception ex) {
         this.testHarness.fail("Unexpected exception ", ex);
      }
   }

   public SuState getState() {
      return this.suState;
   }

   public void handleMMRoamedResponseEvent( DeregistrationResponseEvent deregistrationResponseEvent) {
      logger.info("TestSU: gotDeregisterationResponseEvent ");
   }

   public void handleCallSetupResponseEvent( UnitToUnitCallControlResponseEvent ccResponseEvent) {
      int rc = ccResponseEvent.getStatusCode();
      logger.debug("handeCallSetupResponseEvent: rc="+rc+" suState="+getState());
      try {
         if (rc / 100 == 2 && this.suState == SuState.ON) {
            this.gotCCSetupResponse = true;
            ccSap.ccSetupConfirm(ccResponseEvent);
            logger.debug("got a response event for " + ccResponseEvent.getCallSegment());
         }
         if (logger.isDebugEnabled()) {
            logger.debug("handleCallSetupResponseEvent: "+getCurrentRfss().getRfssName()
               + " Here is the state of the rfss at the caller\n"
               + getCurrentRfss().getRFSS().getTransmissionControlManager().toString());
         }
         //-------------------------------------------------------
         SuToSuCallSetupScenario callSetupScenario = (SuToSuCallSetupScenario) this.scenario;
         if (callSetupScenario.getTalkSpurtSender() != null
               && callSetupScenario.getTalkSpurtSender() == this.suConfig) {
            this.doSendTalkSpurt(callSetupScenario);
         }
         //logger.debug("handeCallSetupResponseEvent: *** rc="+rc+" DONE...");

      } catch (Exception ex) {
         logger.error("Unexpected exception: ", ex);
         testHarness.fail("Unexpected exception occured.");
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.su.SU#handleSetupIndicate(gov.nist.p25.issi.rfss.CallSetupRequestEvent)
    */
   public void handleSetupIndicate(CallSetupRequestEvent ccRequestEvent) {

      // 12.23.1
      //-------------
      logger.debug("handleSetupIndicate(1): START suState="+suState+" callSegment="+callSegment);
      if (callSegment == null) {
         callSegment = ccRequestEvent.getCallSegment();
         if(suState != SuState.ON) suState = SuState.ON;
      }
      logger.debug("handleSetupIndicate(2): AFTER suState="+suState+" callSegment="+callSegment);
      //-------------
      
      try {
         if (suState == SuState.ON && callSegment == null) {
            callSegment = ccRequestEvent.getCallSegment();
         }
         // Check if another call is in progress.
         Request request = ccRequestEvent.getServerTransaction().getRequest();
         String requestCallId = ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();

         // If we are in a confirmed state and another call (with different
         // call Id comes in, signal a busy here.
         FromHeader from = (FromHeader) request.getHeader(FromHeader.NAME);
         String radicalName = ((SipURI) from.getAddress().getURI()).getUser();

         // 12.26.1
         logger.debug("handleSetupIndicate(3): radicalName="+radicalName+" callSegment="+callSegment);
         if(callSegment != null)
         if (callSegment.getDialog(radicalName).getState() == DialogState.CONFIRMED
               && !requestCallId.equals(this.callSegment.getCallID())) {
            ccSap.ccSetupIndicateErrorResponse(ccRequestEvent, WarningCodes.BUSY_HERE_CALLED_SU_BUSY);
            return;
         }

         ContentList clist = ContentList.getContentListFromMessage(request);
         boolean ac = clist.getCallParamContent().getCallParam().isAvailabilityCheckRequested();

         int delay = ac && suConfig.getInviteProcessingDelay() == 0 ? 10
               : suConfig.getInviteProcessingDelay();

         int count = delay / 60;
         if (count == 0 && delay > 0)
            count = 1;
         if (count >= 1) {
            new ThreadedExecutor().execute(new DelayTask(ccRequestEvent));
         } else {
            this.actuallyHandleSetupIndicate(ccRequestEvent);
         }
      } catch (Exception ex) {
         logger.fatal("Unexpected exception", ex);
         System.exit(0);
      }
   }

   private void actuallyHandleSetupIndicate( CallSetupRequestEvent ccRequestEvent) {
      logger.debug("actuallyHandleSetupIndicate: evt.callSegment=" + ccRequestEvent.getCallSegment());
      logger.debug("actuallyHandleSetupIndicate: callSegment=" + callSegment);
      logger.debug("actuallyHandleSetupIndicate: suState=" + suState);
      try {
         if (suState == SuState.ON) {
            String callSegmentId = callSegment.getCallID();
            ServerTransaction st = ccRequestEvent.getServerTransaction();
            String callerCallId = ((CallIdHeader) st.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
            logger.debug("TestSU: callerCallId = " + callerCallId 
                  + " callSegmentId = "+callSegmentId);

            int incomingPriority = ccRequestEvent.getCallSegment().getPriority();
            int currentCallPriority = callSegment.getPriority();
            logger.debug("TestSU: incomingPriority = " + incomingPriority
                  + " currentCallPriority = " + currentCallPriority);

            // Check if another call is in progress.
            FromHeader from = (FromHeader) st.getRequest().getHeader( FromHeader.NAME);
            String radicalName = ((SipURI) from.getAddress().getURI()).getUser();
            logger.debug("TestSU: radicalName = " + radicalName); 
            //logger.debug("TestSU: getState = " + callSegment.getDialog(radicalName).getState());

            if (callSegment.getDialog(radicalName).getState() == DialogState.CONFIRMED) {
               //logger.debug("TestSU: DialogState check...");
               ccSap.ccSetupIndicateErrorResponse(ccRequestEvent, WarningCodes.BUSY_HERE_CALLED_SU_BUSY);
               return;
            }
            if ((!callSegmentId.equals(callerCallId))
                  && incomingPriority <= currentCallPriority) {
               //logger.debug("TestSU: callerCallId check...");
               ccSap.ccSetupIndicateErrorResponse(ccRequestEvent, WarningCodes.DECLINE_COLLIDING_CALL);
               return;
            }
            if (!suConfig.isAvailable()) {
               //logger.debug("TestSU: suConfig.isAvailable()...");
               ccSap.ccSetupIndicateErrorResponse(ccRequestEvent, WarningCodes.BUSY_HERE_CALLED_SU_BUSY);
               return;
            }
            if (!getCurrentRfss().getRFSS().isRfResourcesAvailable()) {
               logger.debug("TestSU: isRfResourcesAvailable()...");
               if( isInCallRoaming( st.getRequest())) {
	          // 12.26.x 606 Not Acceptable : No RF Resource
                  ccSap.ccSetupIndicateErrorResponse(ccRequestEvent, WarningCodes.NO_RF_RESOURCE);
                  return;

               } else {
                  // 12.6.x 183 Session progress followed by 200 OK
                  ccSap.ccSetupIndicateErrorResponse(ccRequestEvent, WarningCodes.SESSION_PROGRESS_NO_RF_RESOURCES);
                  // continue to send 200 OK
                  //+++return;
               }
            }
            callSegment = ccRequestEvent.getCallSegment();

            //logger.debug("TestSU: actuallyHandleSetupIndicate(): before 200 okSent=" + okSent);
            ccSap.ccSetupIndicateResponse(ccRequestEvent, Response.OK);
            okSent = true;
         }
	 else {
            // this path leads to Trying...
            logger.debug("actuallyHandleSetupIndicate: NO-OP suState=" +suState);
         }
      } catch (Exception ex) {
         logger.error("unexpected error ", ex);
         testHarness.fail("unexpected excetption ");
         throw new RuntimeException("unexpected error", ex);
      }
   }
   private boolean isInCallRoaming( Request request) throws Exception {
       ContentList clist = ContentList.getContentListFromMessage(request);
       boolean incall = clist.getCallParamContent().getCallParam().isIncallRoaming();
       return incall;
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.su.SU#handleTeardownResponse(gov.nist.p25.issi.rfss.CallTeardownResponseEvent)
    */
   public void handleTeardownResponse(CallTeardownResponseEvent ccTeardownEvent) {
      byeOKReceived = true;
      logger.info("handle teardown response");
   }

   /**
    * method is invoked when I get an ACK.
    * 
    * @param callSetupConfirmEvent --
    *            encapsulation of the ACK event.
    * 
    */
   public void handleCallSetupConfirmEvent( CallSetupConfirmEvent callSetupConfirmEvent) {
      try {
         if (!okSent)
            testHarness.fail("This is not the callee! I should not see an ACK !");
         ackReceived = true;
         callSegment = callSetupConfirmEvent.getCallSegment();
         SuToSuCallSetupScenario callSetupScenario = (SuToSuCallSetupScenario) scenario;
         if (callSetupScenario.getTalkSpurtSender() != null
               && callSetupScenario.getTalkSpurtSender() == suConfig) {
            doSendTalkSpurt(callSetupScenario);
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         testHarness.fail("unexpected exception !");
      }
   }

   public void handleTeardownIndicate( CallTeardownIndicateEvent callTeardownIndicateEvent) {
      logger.info("handleTeardownIndicate()");
      if (callSegment != null) {
         if(callSegment.getPointToMultipointSession() != null) {
            callSegment.getPointToMultipointSession().shutDown();
         }
         callSegment = null;
      }
   }

   /**
    * Send out the invite for an su to su call.
    */
   public void doCallSetup(SuToSuCallSetupScenario callsetupScenario) {
      logger.info("doCallSetup()");
      try {
         UnitToUnitCall callSegment = ccSap.ccSendCallSetupRequest(callsetupScenario);
         isCaller = true;
         if (callsetupScenario.getCancelAfter() != -1) {
            ISSITimer.getTimer().schedule( new SuToSuCallCancelTimerTask(callSegment),
                  callsetupScenario.getCancelAfter() * 1000);
         }
         logger.debug("doCallSetup: callSegment=" + callSegment
               + " dialog=" + callSegment.getDialog(suConfig.getRadicalName()));
         this.callSegment = callSegment;

      } catch (Exception ex) {
         logger.error("call setup this.rfss.getTestHarness().failed", ex);
         testHarness.fail("Call setup this.rfss.getTestHarness().failed");
      }
   }

   public void setServiceAccessPoints(ServiceAccessPoints saps) {
      this.saps = saps;
      this.ccSap = saps.getCallControlSAP();
      this.tcSap = saps.getTransmissionControlSAP();
      this.mmSap = saps.getMobilityManagementSAP();
      logger.debug("TestSU: " + suConfig.getSuName() +
		   "  SET ServiceAccessPoints()-saps=" +saps);
   }

   public ServiceAccessPoints getServiceAccessPoints() {
      logger.debug("TestSU: " + suConfig.getSuName() +
		   "  GET ServiceAccessPoints()-saps=" +saps);
      return saps;
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.tests.TestSUInterface#getSuConfig()
    */
   public SuConfig getSuConfig() {
      return suConfig;
   }

   public void setScenario(SuToSuCallSetupScenario scenario) {
      this.peerSuConfig = scenario.getCalledSuConfig();
      this.scenario = scenario;
   }

   public void setScenario(GroupCallSetupScenario scenario) {
      this.groupToCall = scenario.getCalledGroupConfig();
      this.scenario = scenario;
   }

   public void handleRegistrationEvent(RegisterEvent registerDeregister) {
      testHarness.fail("unexpected event");
   }

   public void handleMMRoamedEvent(RegisterEvent registerEvent) {
      logger.info("handleMMRoamedEvent: START...");
      try {
         assert registerEvent.getEventType() == RegisterEvent.REGISTER_DEREGISTER;
         gotRegisterDeregister = true;

	 String radicalName = suConfig.getRadicalName();
         logger.info("handleMMRoamedEvent: suConfig.radicalName="+radicalName);

         /*** where is the agent ? use null agent
         if (callSegment != null) { 
            logger.info("handleMMRoamedEvent: ccTeardownCallSegment()...");
            ccSap.ccTeardownCallSegment(null, callSegment, radicalName);
            callSegment = null;
	 }
          ***/

      } catch (Exception ex) {
         logger.error("Unexpected exception", ex);
         testHarness.fail("unexpected exception");
      }
   }

   public void handleDeRegistrationEvent(RegisterEvent event) {
      logger.info("Got a DeRegistration Event!");
   }

   public void handleDeRegistrationResponseEvent( DeregistrationResponseEvent registrationResponseEvent) {
      logger.info("Got a RegistrationResponse Event!");
   }

   public void doGroupCallSetup(GroupCallSetupScenario groupCallSetupScenario) throws Exception {
      logger.debug("TestSu: doGroupCallSetup()");
      groupServing = ccSap.triggerGroupCallSetup(groupCallSetupScenario);
      if (groupCallSetupScenario.getCancelAfter() != -1) {
         ISSITimer.getTimer().schedule(new TimerTask() {

            @Override
            public void run() {
               try {
                  TestSU.this.ccSap.ccCancelGroupCall(TestSU.this.groupServing);
               } catch (Exception ex) {
                  testHarness.fail("Unexpected exception", ex);
               }
            }

         }, groupCallSetupScenario.getCancelAfter() * 1000);
      }
      if (groupServing.getDialog().getState() == DialogState.CONFIRMED) {
         if (groupCallSetupScenario.isTalkSpurtSentAfterCallSetup()) {
            doSendTalkSpurt(groupCallSetupScenario);
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.tests.TestSUInterface#sendTalkSpurt()
    */
   public void sendTalkSpurt(int nblocks) {
      try {
         if (suState == SuState.OFF) {
            testHarness.fail("Cannot send talk spurt -- unit is off ");
	 } else if (callSegment == null) {
            testHarness.fail("No Call segment -- cannot send talk spurt");
         } else {
            logger.debug("sending talk spurt");
            tcSap.sendTalkSpurt(RFSS.getImbeVoiceBlocks(), suConfig.getSystemId(), 
                  suConfig.getSuId(), transmitPriorityType,
                  transmissionPriorityLevel, nblocks);
         }
      } catch (Exception ex) {
         logger.error("unexpected exception", ex);
         testHarness.fail("Unexpected exception", ex);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.tests.TestSUInterface#sendTalkSpurt()
    */
   public void sendTalkSpurt() {
      try {
         if (suState == SuState.OFF) {
            testHarness.fail("Cannot send talk spurt -- unit is off ");
         } else if (callSegment == null) {
            testHarness.fail("No Call segment -- cannot send talk spurt");
         } else {
            logger.debug("sending talk spurt");
            tcSap.sendTalkSpurt(RFSS.getImbeVoiceBlocks(), 
                  suConfig.getSystemId(), suConfig.getSuId(),
                  transmitPriorityType, transmissionPriorityLevel,
                  RFSS.getImbeVoiceBlocks().size());
         }
      } catch (Exception ex) {
         logger.error("unexpected exception", ex);
         testHarness.fail("Unexpected exception", ex);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.tester.TestSUInterface#forceTalkSpurt(int)
    */
   public void forceTalkSpurt(int nblocks) {

      try {
         if (suState == SuState.OFF) {
            testHarness.fail("Cannot send talk spurt -- unit is off ");
	 } else if (callSegment == null) {
            testHarness.fail("No Call segment -- cannot send talk spurt");
         } else {
            logger.debug("sending talk spurt");
            tcSap.forceTalkSpurt(RFSS.getImbeVoiceBlocks(), 
                  suConfig.getSystemId(), suConfig.getSuId(),
                  transmitPriorityType, transmissionPriorityLevel, nblocks);
         }
      } catch (Exception ex) {
         logger.error("unexpected exception", ex);
         testHarness.fail("Unexpected exception", ex);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.tester.TestSUInterface#forceTalkSpurt()
    */
   public void forceTalkSpurt() {
      try {
         if (suState == SuState.OFF) {
            testHarness.fail("Cannot send talk spurt -- unit is off ");
	 } else if (callSegment == null) {
            testHarness.fail("No Call segment -- cannot send talk spurt");
         } else {
            logger.debug("sending talk spurt");
            tcSap.forceTalkSpurt(RFSS.getImbeVoiceBlocks(), 
                  suConfig.getSystemId(), suConfig.getSuId(),
                  transmitPriorityType, transmissionPriorityLevel,
                  RFSS.getImbeVoiceBlocks().size());
         }
      } catch (Exception ex) {
         logger.error("unexpected exception", ex);
         testHarness.fail("Unexpected exception", ex);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.tester.TestSUInterface#sendTalkSpurt(java.lang.String,
    *      int)
    */
   public void sendTalkSpurt(String groupRadicalName, int nblocks) {
      try {
         if (this.suState == SuState.OFF) {
            testHarness.fail("Cannot send talk spurt -- unit is off ");
         } else {
            logger.debug("sending talk spurt");
            tcSap.sendTalkSpurt(RFSS.getImbeVoiceBlocks(),
                  groupRadicalName, suConfig,
                  transmitPriorityType,
                  transmissionPriorityLevel, nblocks);
         }
      } catch (Exception ex) {
         logger.error("Unexpected exception", ex);
         testHarness.fail("Unexpected exception", ex);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.tester.TestSUInterface#sendTalkSpurt(java.lang.String)
    */
   public void sendTalkSpurt(String groupRadicalName) {
      try {
         if (suState == SuState.OFF) {
            testHarness.fail("Cannot send talk spurt -- unit is off ");
         } else {
            logger.debug("sending talk spurt");
            tcSap.sendTalkSpurt(RFSS.getImbeVoiceBlocks(),
                  groupRadicalName, suConfig,
                  transmitPriorityType, transmissionPriorityLevel, 
                  RFSS.getImbeVoiceBlocks().size());
         }
      } catch (Exception ex) {
         logger.error("Unexpected exception", ex);
         testHarness.fail("Unexpected exception", ex);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.tests.TestSUInterface#terminateUnitToUnitCall()
    */
   public void terminateUnitToUnitCall() {
      try {
         // Bad coding - using same variable name !!!
         if (callSegment != null) {
            SuToSuCallSetupScenario scenario = (SuToSuCallSetupScenario) this.scenario;
            SuConfig suConfig = (this.suConfig == scenario.getCallingSuConfig() ? scenario.getCalledSuConfig()
                  : scenario.getCallingSuConfig());
            ccSap.ccTeardownCallSegment(this, this.callSegment, suConfig.getRadicalName());
            byeSent = true;
            this.callSegment = null;
         }
      } catch (Exception ex) {
         testHarness.fail("Unexpected exception tearing down call", ex);
      }
   }

   public void cancelUnitToUnitCall() {
      try {
         if (callSegment != null && isCaller) {
            ccSap.ccCancelCallSegment(callSegment);
            cancelSent = true;
            callSegment.setCanceled();
         } else if (callSegment != null) {
            callSegment.setCanceled();
         }
      } catch (Exception ex) {
         testHarness.fail("Unexpected exception in canceling call", ex);
      }
   }

   public void handleCancelIndicate(UnitToUnitCall callSegment) {
      try {
         ccSap.ccCancelResponse(callSegment.getServerTransaction());
      } catch (Exception ex) {
         // SipException: Too late to cancel the request
         testHarness.fail( "Unexpected exception in sending response to cancel", ex);
      }
   }

   @Override
   public UnitToUnitCall getCurrentUnitToUnitCall() {
      return callSegment;
   }

   public void clearCallSegment() {
      callSegment = null;
   }

   /**
    * Set the transmit priority type
    * 
    * @param transmitPriorityType
    */
   public void setTransmitPriorityType( TransmitPriorityType transmitPriorityType) {
      this.transmitPriorityType = transmitPriorityType;
   }

   /**
    * @param transmissionPriorityLevel
    *            the transmissionPriorityLevel to set
    */
   public void setTransmissionPriorityLevel(int transmissionPriorityLevel) {
      this.transmissionPriorityLevel = transmissionPriorityLevel;
   }

   /**
    * Force a talk spur to the group.
    * 
    * @param radicalName
    */
   public void forceTalkSpurt(String groupRadicalName, int nblocks) {
      try {
         if (suState == SuState.OFF) {
            testHarness.fail("Cannot send talk spurt -- unit is off ");
         } else {
            logger.debug("sending talk spurt");
            tcSap.forceTalkSpurt(RFSS.getImbeVoiceBlocks(),
                  groupRadicalName, suConfig,
                  transmitPriorityType,
                  transmissionPriorityLevel, nblocks);
         }
      } catch (Exception ex) {
         logger.error("Unexpected exception", ex);
         testHarness.fail("Unexpected exception", ex);
      }
   }

   /**
    * Force the entire talk spurt ( the whole file ) to the group.
    * 
    * @param groupRadicalNamem --
    *            radical name.
    */
   public void forceTalkSpurt(String groupRadicalName) {
      try {
         if (suState == SuState.OFF) {
            testHarness.fail("Cannot send talk spurt -- unit is off ");
         } else {
            logger.debug("sending talk spurt");
            tcSap.forceTalkSpurt(RFSS.getImbeVoiceBlocks(),
                  groupRadicalName, suConfig,
                  transmitPriorityType, transmissionPriorityLevel,
                  RFSS.getImbeVoiceBlocks().size());
         }
      } catch (Exception ex) {
         logger.error("Unexpected exception", ex);
         testHarness.fail("Unexpected exception", ex);
      }
   }

   //=====================================================
   class ServingMuteTask implements Runnable {
      private String groupRadicalName;
      private SuConfig suConfig;

      public ServingMuteTask(String groupRadicalName, SuConfig suConfig) {
         this.groupRadicalName = groupRadicalName;
         this.suConfig = suConfig;
      }
      @Override
      public void run() {
         try {
            TestSU.this.tcSap.sendMute(groupRadicalName, suConfig);
         } catch (Exception ex) {
            testHarness.fail("ServingMuteTask: ", ex);
         }
      }
   }
   class ServingUnmuteTask implements Runnable {
      private String groupRadicalName;
      private SuConfig suConfig;
      private long msec;

      public ServingUnmuteTask(String groupRadicalName, SuConfig suConfig, long msec) {
         this.groupRadicalName = groupRadicalName;
         this.suConfig = suConfig;
         this.msec = msec;
      }
      @Override
      public void run() {
         try {
            // impose delay 
            Thread.sleep(msec);
            TestSU.this.tcSap.sendUnmute(groupRadicalName, suConfig);
         } catch (Exception ex) {
            testHarness.fail("ServingUnmuteTask: ", ex);
         }
      }
   }

   //#682 11.4.x serving sendMute
   public void sendMute(String groupRadicalName) throws Exception {
      //tcSap.sendMute(groupRadicalName, suConfig);
      new ThreadedExecutor().execute(
          new ServingMuteTask(groupRadicalName,suConfig));
   }
   public void sendUnmute(String groupRadicalName, long msec) throws Exception {
      //tcSap.sendUnmute(groupRadicalName, suConfig);
      new ThreadedExecutor().execute(
          new ServingUnmuteTask(groupRadicalName,suConfig,msec));
   }
   public void sleep(long msec) {
      logger.debug("TestSU: sleep(): "+msec);
      try {
         Thread.sleep(msec);
      } catch(InterruptedException ex) { }
   }
}
