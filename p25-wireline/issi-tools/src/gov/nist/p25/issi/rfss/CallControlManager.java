//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.rfss.SipUtils;
import gov.nist.p25.issi.transctlmgr.ptt.PttSession;
import gov.nist.p25.issi.transctlmgr.ptt.PttSessionMultiplexer;

import javax.sdp.Connection;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.SipURI;
import javax.sip.header.ToHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

/**
 * The top level call control manager. This is the listener that fields the
 * INVITE. It looks at the user parameter of the To: address and sends the
 * INVITE off to the right place (i.e. UnitToUnitCallControlManager or
 * GroupCallControlManager)
 */
public class CallControlManager implements SipListener {

   private static Logger logger = Logger.getLogger(CallControlManager.class);
   
   public static final int CC_SETUP_SEND_REQUEST = 1;
   public static final int CC_SETUP_TEARDOWN_REQUEST = 2;

   private UnitToUnitCallControlManager unitToUnitCallControlManager;
   private GroupCallControlManager groupCallControlManager;
   private SipProvider provider;
   private RfssConfig rfssConfig;

   /**
    * A utility function that fixes up the SDP announce to match the port of PttSession.
    * 
    * @param session --
    *            RTPPoint to multipoint session from which to extract params to
    *            stuff into SdpAnnounce
    * @param sdpContent --
    *            sdpContent to fixup.
    * 
    * @throws Exception
    */
   public static void fixupSdpAnnounce(RfssConfig rfss, PttSession session,
         SessionDescription sdpAnnounce) {

      // Address where the current RFSS is expected to be receiving IP packets.
      try {
         String myIpAddress = rfss.getIpAddress();
         //TransmissionControlManager.addMediaLeg(session, sdpAnnounce);
         Connection connection = sdpAnnounce.getConnection();
         connection.setAddress(myIpAddress);

         MediaDescription outgoingMediaDescription = 
            (MediaDescription) (sdpAnnounce.getMediaDescriptions(true).get(0));
         Media outgoingMedia = outgoingMediaDescription.getMedia();

         // This is where this RFSS is going to be recieving IP PACKETS.
         outgoingMedia.setMediaPort(session.getMyRtpRecvPort());
         sdpAnnounce.getOrigin().setAddress(myIpAddress);
         session.setSessionDescription(sdpAnnounce);

      } catch (Exception ex) {
         logger.fatal("Unexpected exception ", ex);
      }
   }

   /**
    * A utility function that fixes up the SDP announce to match the port of the PttSession.
    * 
    * @param session --
    *              ptt session mux from which to extract parameters.
    *           
    * @param sdpContent --
    *            sdpContent to fixup.
    * 
    * @throws Exception
    */
   public static void fixupSdpAnnounce(RfssConfig rfssConfig, PttSessionMultiplexer session,
         SessionDescription sdpAnnounce) {

      // Addres where the current RFSS is expected to be receiving IP packets.
      try {
         String myIpAddress = rfssConfig.getIpAddress();
         //TransmissionControlManager.addMediaLeg(session, sdpAnnounce);
         Connection connection = sdpAnnounce.getConnection();
         connection.setAddress(myIpAddress);

         MediaDescription outgoingMediaDescription = 
            (MediaDescription) (sdpAnnounce.getMediaDescriptions(true).get(0));
         Media outgoingMedia = outgoingMediaDescription.getMedia();

         // This is where this RFSS is going to be recieving IP PACKETS.
         outgoingMedia.setMediaPort(session.getRtpSession().getMyRtpRecvPort());
         sdpAnnounce.getOrigin().setAddress(myIpAddress);
         session.setSessionDescription(sdpAnnounce);

      } catch (Exception ex) {
         logger.fatal("Unexpected exception ", ex);
      }
   }

   // constructor
   //--------------------------------------------------------------------------
   public CallControlManager(RFSS rfss) {
      try {
         provider = rfss.getProvider();
         rfssConfig = rfss.getRfssConfig();
         ListeningPoint listeningPoint = provider.getListeningPoint("udp");
         logger.info("setSentBy " + rfssConfig.getDomainName());
         listeningPoint.setSentBy(rfssConfig.getDomainName());
         unitToUnitCallControlManager = new UnitToUnitCallControlManager( rfss);
         groupCallControlManager = new GroupCallControlManager(provider, rfssConfig);

      } catch (Exception ex) {
         logger.fatal("Unexpected exception", ex);
         rfssConfig.getRFSS().getTestHarness().fail("unexpected exception ", ex);
      }
   }

   private boolean isU2UCall(Message message) {
      ToHeader toHeader = (ToHeader) message.getHeader(ToHeader.NAME);
      SipURI sipUri = (SipURI) toHeader.getAddress().getURI();
      return SipUtils.isSubscriberUnit( sipUri);
   }

   private boolean isGroupCall(Message message) {
      ToHeader toHeader = (ToHeader) message.getHeader(ToHeader.NAME);
      SipURI sipUri = (SipURI) toHeader.getAddress().getURI();
      return SipUtils.isGroup(sipUri);
   }

   public void processRequest(RequestEvent requestEvent) {
      Request request = requestEvent.getRequest();
      if (isU2UCall(request)) {
         unitToUnitCallControlManager.processRequest(requestEvent);
      } else if (isGroupCall(request)) {
         groupCallControlManager.processRequest(requestEvent);
      } else {
         rfssConfig.getRFSS().getTestHarness().fail("Unknown call type");
      }
   }

   public void processResponse(ResponseEvent responseEvent) {
      Response response = responseEvent.getResponse();
      if (isU2UCall(response)) {
         unitToUnitCallControlManager.processResponse(responseEvent);
      } else if (isGroupCall(response)) {
         groupCallControlManager.processResponse(responseEvent);
      } else {
         rfssConfig.getRFSS().getTestHarness().fail("Unknown call type");
      }
   }

   public void processTimeout(TimeoutEvent timeoutEvent) {
      if (timeoutEvent.isServerTransaction()) {
         ServerTransaction st = timeoutEvent.getServerTransaction();
         Request request = st.getRequest();
         if (isU2UCall(request)) {
            unitToUnitCallControlManager.processTimeout(timeoutEvent);
         } else if (isGroupCall(request)) {
            groupCallControlManager.processTimeout(timeoutEvent);
         } else {
            rfssConfig.getRFSS().getTestHarness().fail("Unknown call type");
         }
      } else {
         ClientTransaction ct = timeoutEvent.getClientTransaction();
         Request request = ct.getRequest();
         if (isU2UCall(request)) {
            unitToUnitCallControlManager.processTimeout(timeoutEvent);
         } else if (isGroupCall(request)) {
            groupCallControlManager.processTimeout(timeoutEvent);
         } else {
            rfssConfig.getRFSS().getTestHarness().fail("Unknown call type");
         }
      }
   }

   public void processIOException(IOExceptionEvent arg0) {
      rfssConfig.getRFSS().getTestHarness().fail("unexpected event");
   }
   
   public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminated) {
      if (transactionTerminated.isServerTransaction()) {
         ServerTransaction st = transactionTerminated.getServerTransaction();
         Request request = st.getRequest();
         if (isU2UCall(request)) {
            unitToUnitCallControlManager.processTransactionTerminated(transactionTerminated);
         } else if (isGroupCall(request)) {
            groupCallControlManager.processTransactionTerminated(transactionTerminated);
         } else {
            rfssConfig.getRFSS().getTestHarness().fail("Unknown call type");
         }
      } else {
         ClientTransaction ct = transactionTerminated.getClientTransaction();
         Request request = ct.getRequest();
         if (isU2UCall(request)) {
            unitToUnitCallControlManager.processTransactionTerminated(transactionTerminated);
         } else if (isGroupCall(request)) {
            groupCallControlManager.processTransactionTerminated(transactionTerminated);
         } else {
            rfssConfig.getRFSS().getTestHarness().fail("Unknown call type");
         }
      }
   }

   public void processDialogTerminated(DialogTerminatedEvent dte) {
      if (dte.getDialog().getApplicationData() instanceof UnitToUnitCall) {
         unitToUnitCallControlManager.processDialogTerminated(dte);
      } else {
         groupCallControlManager.processDialogTerminated(dte);
      }
   }

   // accessors
   public UnitToUnitCallControlManager getUnitToUnitCallControlManager() {
      return unitToUnitCallControlManager;
   }

   public GroupCallControlManager getGroupCallControlManager() {
      return groupCallControlManager;
   }

   public void alertResourceChange(boolean resourceVal) throws Exception {
      unitToUnitCallControlManager.alertRfResourceChange(resourceVal);
      groupCallControlManager.alertRfResourceChange(resourceVal);
   }
   
   public void alertGroupPriorityChange(String groupRadicalName, int newPriority , boolean isEmergency)
      throws Exception {
      groupCallControlManager.groupPriorityChange(groupRadicalName, newPriority, isEmergency);
   }
}
