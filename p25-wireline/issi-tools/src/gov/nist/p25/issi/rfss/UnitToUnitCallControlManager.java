//
package gov.nist.p25.issi.rfss;

import gov.nist.javax.sdp.fields.OriginField;
import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.CProtectedDisposition;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SystemConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.CallParamContent;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25body.SdpContent;
import gov.nist.p25.issi.p25body.params.CallParam;
import gov.nist.p25.issi.p25body.serviceprofile.AvailabilityCheckType;
import gov.nist.p25.issi.p25body.serviceprofile.CallPermissionType;
import gov.nist.p25.issi.p25body.serviceprofile.CallSetupPreferenceType;
import gov.nist.p25.issi.p25body.serviceprofile.DuplexityType;
import gov.nist.p25.issi.p25body.serviceprofile.RadioInhibitType;
import gov.nist.p25.issi.p25body.serviceprofile.user.UserServiceProfile;
import gov.nist.p25.issi.rfss.SipUtils;
import gov.nist.p25.issi.rfss.tester.SuToSuCallSetupScenario;
import gov.nist.p25.issi.rfss.tester.TestSU;
import gov.nist.p25.issi.transctlmgr.PttPointToMultipointSession;
import gov.nist.p25.issi.transctlmgr.TransmissionControlManager;
import gov.nist.p25.issi.transctlmgr.ptt.LinkType;
import gov.nist.p25.issi.transctlmgr.ptt.MmfSession;
import gov.nist.p25.issi.transctlmgr.ptt.PttSession;
import gov.nist.p25.issi.transctlmgr.ptt.SessionType;
import gov.nist.p25.issi.transctlmgr.ptt.SmfSession;
import gov.nist.p25.issi.utils.ProtocolObjects;
import gov.nist.p25.issi.utils.WarningCodes;
import gov.nist.rtp.RtpException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.AcceptHeader;
import javax.sip.header.AllowHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.PriorityHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.TimeStampHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WarningHeader;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

/**
 * The Call control manager - this is implemented as a Back to Back SIP User
 * Agent. This component handles all SIP Invites, SIP responses to Invites,
 * ACKs, BYEs etc. in other words, all the signaling having to do with SIP Call
 * setup. Note that as the requests go through this component, it interacts with
 * the Transmission control manager to set up RTP ports on which to listen and
 * it modifies the SDP announce accordingly. This is to force the media stream
 * to traverse the RFSSs as required by the ISSI specification.
 * 
 */
@SuppressWarnings("unused")
public class UnitToUnitCallControlManager implements SipListener
{
   private static Logger logger = Logger.getLogger(UnitToUnitCallControlManager.class);
   public static void showln(String s) { System.out.println(s); }

   private static MessageFactory messageFactory = ProtocolObjects.messageFactory;
   private static HeaderFactory headerFactory = ProtocolObjects.headerFactory;
   private static AddressFactory addressFactory = ProtocolObjects.addressFactory;

   private boolean verbose = false;

   // 12.18.1 & 12.20.1
   private boolean cancelReceived;
   public boolean getCancelReceived() { return cancelReceived; }
   public void setCancelReceived(boolean bflag) { 
      cancelReceived=bflag;
   }
   private boolean cancelSent;
   public boolean getCancelSent() { return cancelSent; }
   public void setCancelSent(boolean bflag) { 
      cancelSent=bflag;
   }
   private Object okLock = new Object();
   private boolean okSent;
   private boolean getOkSent() { return okSent; }
   private void setOkSent(boolean bflag) { 
      okSent=bflag;
   }
   // 12.7.3.x
   //private boolean reqTermSent;
   //public boolean getRequestTerminatedSent() { return reqTermSent; }
   //public void setRequestTerminatedSent(boolean bflag) { 
   //   reqTermSent=bflag;
   //}

   // 12.22.x
   private boolean busyHereSent;
   public boolean getBusyHereSent() { return busyHereSent; }
   public void setBusyHereSent(boolean bflag) { 
      busyHereSent=bflag;
   }

   private SystemConfig sysConfig;
   private RfssConfig rfssConfig;
   private TopologyConfig topologyConfig;
   private SipProvider provider;
   private RFSS rfss;
   private HashMap<String,UnitToUnitCall> sidMap = new HashMap<String,UnitToUnitCall>();

   //--------------------------------------------------------------
   class PendingResponse {
      HomeAgentInterface su;
      int procedure;
      UnitToUnitCall unitToUnitCall;
   }

   class PendingSDPAnswer {
      ServerTransaction serverTransaction;
      PttPointToMultipointSession pointToMultipointSession;
      PttSession pendingOutgoingSession;
      SessionDescription incomingSdpOffer;
      SessionDescription outgoingSdpOffer;
   }

   /**
    * Creates new Call Control Manager.
    *
    * @param rfss
    */
   public UnitToUnitCallControlManager(RFSS rfss) {
      try {
         this.rfss = rfss;
         rfssConfig = rfss.getRfssConfig();
         provider = rfss.getProvider();
         sysConfig = rfssConfig.getSysConfig();
         topologyConfig = sysConfig.getTopologyConfig();
      }
      catch (Exception ex) {
         rfss.getTestHarness().fail( "UnitToUnitCallControlManagager: ", ex);
      }
   }

   public TransmissionControlManager getTransmissionControlManager() {
      return rfssConfig.getRFSS().getTransmissionControlManager();
   }

   //-----------------------------------------------------------------------------------
   public void adjustCallParamACDC( CallParam callParam, SuConfig calledSuConfig) {

      //TODO: make it common code from UnitToUnitCallControlManager.java
         /*
          * From 102 BACA page 128 The Called Home RFSS compares the received
          * preference with the Availability Check and the Direct Call
          * attributes of the called SU service profile to set the preference
          * parameter in the INVITE message sent to the Called Serving RFSS:
          * 
          * if the preference is set to Availability Check (1) and the SU
          * supports Availability Check in its USP, then the preference is
          * set to Availability Check (1).
          */
         logger.debug("isAvailCheckRequested=" + callParam.isAvailabilityCheckRequested());
         logger.debug("calledSu.AvailCheckType="
               + calledSuConfig.getUserServiceProfile().getAvailabilityCheck().getAvailabilityCheckType());

         AvailabilityCheckType calledSuAvailCheckType = calledSuConfig
               .getUserServiceProfile().getAvailabilityCheck().getAvailabilityCheckType();

         if (callParam.isAvailabilityCheckRequested()
             && calledSuAvailCheckType != AvailabilityCheckType.DIRECT_CALL_ONLY) {
            logger.debug("RESET-1: setACRequested(true)");
            // ringing !!!
            callParam.setAvailabilityCheckRequested(true);

         } else if (callParam.isAvailabilityCheckRequested() 
               && calledSuAvailCheckType == AvailabilityCheckType.DIRECT_CALL_ONLY) {
            /*
             * if the preference is set to Availability Check (1) and the SU
             * supports only Direct Call in its USP, then the preference is
             * set to Direct Call (0).
             */
            logger.debug("RESET-2: setACRequested(false)");
            callParam.setAvailabilityCheckRequested(false);

         } else if ((!callParam.isAvailabilityCheckRequested())
               && calledSuAvailCheckType == AvailabilityCheckType.AVAIL_CHECK_ONLY) {
            /*
             * if the preference is set to Direct Call (0) and the SU
             * supports only Availability Check, in its USP, then the
             * preference is set to Availability Check (1).
             */
            logger.debug("RESET-3: setACRequested(true)");
            callParam.setAvailabilityCheckRequested(true);

         } else if ((!callParam.isAvailabilityCheckRequested())
               && calledSuAvailCheckType != AvailabilityCheckType.AVAIL_CHECK_ONLY) {
            /*
             * if the preference is set to Direct Call (0) and the SU
             * supports Direct Call then the preference is set to Direct
             * Call (0).
             */
            logger.debug("RESET-4: setACRequested(false)");
            callParam.setAvailabilityCheckRequested(false);
         }
   }

   /**
    * Create and setup an outgoing client tx. This munges the SDP announce part
    * of the outgoing request
    * 
    * @param serverTransaction
    * @param newRequest
    * @param suId
    * @param linkType
    * @param unitToUnitCall
    * @return ClientTransaction
    * @throws Exception
    */
   private ClientTransaction setupClientTransaction( ServerTransaction serverTransaction,
      Request newRequest, int suId, LinkType linkType, UnitToUnitCall unitToUnitCall)
      throws Exception
   {
      Request request = serverTransaction.getRequest();

      RouteHeader routeHeader = (RouteHeader) newRequest.getHeader(RouteHeader.NAME);
      if( routeHeader == null) {
         String msg = "setupClientTransaction: null routeHeader in request.\n" +newRequest;
         logger.debug( msg);
	      throw new RuntimeException( msg);
      }
      String targetDomainName = ((SipURI) routeHeader.getAddress().getURI()).getHost();

      WarningHeader warningHeader = SipUtils.checkIncomingRequest(request);
      SuConfig calledSuConfig = unitToUnitCall.getCalledSuConfig();
      SuConfig callingSuConfig = unitToUnitCall.getCallingSuConfig();
      if (warningHeader != null) {
         Response response = messageFactory.createResponse( Response.BAD_REQUEST, request);
         warningHeader.setAgent("RFSS: " + rfssConfig.getRfssId());
         response.addHeader(warningHeader);
         serverTransaction.sendResponse(response);
         return null;
      }

      ContentList contentList = ContentList.getContentListFromMessage(request);
      SdpContent sdpContent = (SdpContent) ContentList.getContentByType(contentList,
            ISSIConstants.APPLICATION, ISSIConstants.SDP);

      if (sdpContent == null) {
         Response response = messageFactory.createResponse( Response.BAD_REQUEST, request);
         warningHeader = headerFactory.createWarningHeader(rfssConfig.getDomainName(),
               WarningHeader.MISCELLANEOUS_WARNING,
               ISSIConstants.WARN_TEXT_MISSING_REQUIRED_SDP_ANNOUNCE);
         response.addHeader(warningHeader);
         serverTransaction.sendResponse(response);
         return null;
      }

      if (linkType != LinkType.UNIT_TO_UNIT_CALLED_HOME_TO_CALLED_SERVING &&
          linkType != LinkType.UNIT_TO_UNIT_CALLING_HOME_TO_CALLED_HOME) {
         throw new IllegalArgumentException("Unexpected link type : " + linkType);
      }

      SessionDescription sessionDescription = sdpContent.getSessionDescription();
      PendingSDPAnswer pendingAnswer = new PendingSDPAnswer();

      // deep clone is not suport for javax.sdp
      pendingAnswer.incomingSdpOffer = SdpFactory.getInstance()
            .createSessionDescription(sessionDescription.toString());
      String sessionId = SipUtils.getCallIdFromMessage(newRequest);

      /*
       * Here we are setting up an RTP Session where we listen for incoming
       * packets. From this we get the port and place it in the SDP announce
       * (OFFER) to send to the other end. The port is a randomly generated
       * even number where WE will be listening for incoming packets.
       */
      PttPointToMultipointSession ptToMpSession = null;

      if (linkType == LinkType.UNIT_TO_UNIT_CALLED_HOME_TO_CALLED_SERVING) {
         ptToMpSession = getTransmissionControlManager()
               .getPttPointToMultipointSession(sessionId, SessionType.CALLED_HOME);

         CallParamContent callParamContent = contentList.getCallParamContent();
         CallParam callParam = callParamContent.getCallParam();
         callParam.setCallingSuInitialTransmitter(
               rfssConfig.isCallingSuInitialTransmitter());

	      // fixup ACDC request
         adjustCallParamACDC( callParam, calledSuConfig);

         if(calledSuConfig.getCProtectedDisposition() == CProtectedDisposition.CLEAR_PROTECTED) {
            callParam.setProtectedModeIsSet(true);
         }

         // See if the protection bit needs to be cleared or forwarded.
         if (callParam.isProtectedMode() &&
             calledSuConfig.getCProtectedDisposition() == CProtectedDisposition.CLEAR_PROTECTED) {
            callParam.setProtectedMode(false);
         }

         // Adjust the duplexity of the call.
         UserServiceProfile usp = calledSuConfig.getUserServiceProfile();

	      // based on testscript isFullDuplex setting
         callParam.setFullDuplexRequestedIsSet( usp.isFullDuplexIsSet());

	      // 12.3.2.x
         if (callParam.isFullDuplexRequested()) {
            DuplexityType duplexity = usp.getDuplexity().getDuplex();
            if(duplexity == DuplexityType.FULL) {
               callParam.setFullDuplexRequested(true);
               callParam.setFullDuplexRequestedIsSet(true);
            }
	         else if(duplexity == DuplexityType.HALF) {
               callParam.setFullDuplexRequested(false);
               callParam.setFullDuplexRequestedIsSet(true);
            }
         }

      } else {
         ptToMpSession = getTransmissionControlManager()
               .getPttPointToMultipointSession(sessionId, SessionType.CALLING_HOME);
      }

      PttSession rtpSession = null;
      long newSessionId = new Long((long) (Math.random() * 100000));
      sessionDescription.getOrigin().setSessionId(newSessionId);

      try {
         if (linkType == LinkType.UNIT_TO_UNIT_CALLED_HOME_TO_CALLED_SERVING) {
            rtpSession = ptToMpSession.createMmfSession(linkType,
                  ((OriginField) sessionDescription.getOrigin()).getSessIdAsString());
            ((MmfSession) rtpSession)
                  .setSetIsNotInterestedInLosingAudio(rfssConfig.isNotInterestedInLosingAudio());
            rtpSession.setRemoteRfssDomainName(targetDomainName);
            // rtpSession.setRemoteRfssRadicalName(callingSuConfig.getHomeRfss().getDomainName());

         } else {
            // CALLING HOME TO CALLED HOME
            // This is a combined mmf/smf back to back session.
            SmfSession smfSession = ptToMpSession.createSmfSession(linkType,
                  ((OriginField) sessionDescription.getOrigin()).getSessIdAsString());
	         rtpSession = smfSession;

            //logger.debug("CALLING_HOME to CALLED_HOME SmfReceiver: addListener()...");
            //smfSession.getSmfReceiver().addListener(rfss.getCurrentTestCase());

            rtpSession.setRemoteRfssDomainName(calledSuConfig.getHomeRfss().getDomainName());
         }

      } catch (RtpException ex) {
         Response response = SipUtils.createErrorResponse(rfssConfig,
               request, WarningCodes.NO_RTP_RESOURCE);
         serverTransaction.sendResponse(response);
         logger.debug("setupClientTransaction(1): NO_RTP_RESOURCE...response="+response);
         return null;
      }
      rtpSession.getHeartbeatReceiver().setHeartbeatListener(unitToUnitCall);

      // fixupSdpAnnounce takes the incoming SessionDescription
      // (that was part of the ServerTransaction) and edits the information
      // to send to the next hop.
      CallControlManager.fixupSdpAnnounce(rfssConfig, rtpSession, sessionDescription);

      sdpContent.setSessionDescription(sessionDescription);
      ContentTypeHeader ctHeader = contentList.getContentTypeHeader();

      // The SPD has been modified and the port number
      // where we are listening has been placed in the origin field add it to
      // the outgoing SIP INVITE
      unitToUnitCall.setCallParam(contentList.getCallParamContent());
      newRequest.setContent(contentList.toString(), ctHeader);
      //logger.debug("setupClientTransaction(2): contentList="+contentList.toString());
      SipUtils.checkContentLength( newRequest);

      ClientTransaction ct = getProvider().getNewClientTransaction(newRequest);
      // NOTE: if ct is null, it will cause RFSS to drop the stray request.
      logger.debug( "setupClientTransaction(2): ct="+ct);
      if( ct == null) {
         String msg = "setupClientTransaction(3): setup error -- not creating call segment";
         logger.error( msg);
         throw new IllegalArgumentException( msg);
         //return ct;
      }

      pendingAnswer.pointToMultipointSession = ptToMpSession;
      pendingAnswer.pendingOutgoingSession = rtpSession;
      pendingAnswer.serverTransaction = serverTransaction;
      pendingAnswer.outgoingSdpOffer = sessionDescription;
      ct.setApplicationData(pendingAnswer);
      return ct;
   }

   /**
    * Create an in call roaming request
    * 
    * @param roamingSu
    * @param unitToUnitCall
    * @throws Exception
    */
   Request createInviteForInCallRoaming(SuConfig roamingSu, UnitToUnitCall unitToUnitCall)
         throws Exception {

      int priority = unitToUnitCall.getPriority();
      boolean isEmergency = unitToUnitCall.isEmergency();

      // create my SIP URI. This goes into the From Header.
      // String radicalName = roamingSu.getRadicalName();
      String radicalName = unitToUnitCall.getCallingSuConfig().getRadicalName();
      String domainName = ISSIConstants.P25DR;
      SipURI callingSipURI = SipUtils.createSipURI(radicalName, domainName);
      callingSipURI.setParameter(ISSIConstants.USER, ISSIConstants.TIA_P25_SU);
      Address fromAddress = addressFactory.createAddress(callingSipURI);
      
      String tag = SipUtils.createTag();
      FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, tag);

      // Create the SIP URI for the Called RFSS.
      // SuConfig otherSu = roamingSu == unitToUnitCall.getCallingSuConfig() ?
      // unitToUnitCall.getCalledSuConfig() : unitToUnitCall.getCallingSuConfig();

      SuConfig otherSu = unitToUnitCall.getCalledSuConfig();
      SipURI calledSipURI = SipUtils.createSipURI(otherSu.getRadicalName(), domainName);
      calledSipURI.setParameter(ISSIConstants.USER, ISSIConstants.TIA_P25_SU);
      Address toAddress = addressFactory.createAddress(calledSipURI);
      ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
      ContactHeader contactHeader = SipUtils.createContactHeaderForSU( rfssConfig, roamingSu);

      ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
      LinkedList<ViaHeader> viaHeaders = new LinkedList<ViaHeader>();
      viaHeaders.add(viaHeader);

      // PTT-BUG
      //logger.debug("createInviteForInCallRoaming(): PTT-BUG new CallId...");
      CallIdHeader callIdHeader = getProvider().getNewCallId();
      //CallIdHeader callIdHeader = headerFactory.createCallIdHeader(unitToUnitCall.getCallID());
      
      CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);
      MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);
      SipURI requestURI = SipUtils.createSipURI(roamingSu.getRadicalName(), ISSIConstants.P25DR);

      // create SIP request
      Request sipRequest = messageFactory.createRequest(requestURI,
            Request.INVITE, callIdHeader, cseqHeader, fromHeader, toHeader,
            viaHeaders, maxForwardsHeader);

      SipUtils.addPriorityHeader( sipRequest, priority, isEmergency);
      //logger.debug("createInviteForInCallRoaming(): sipRequest=" + sipRequest);

      //SipURI sipUri = (SipURI) rfssConfig.getRFSS()
      //   .getUnitToUnitMobilityManager().getRegisteredContactURI( requestURI).clone();
      //
      URI nextUri = rfssConfig.getRFSS()
            .getUnitToUnitMobilityManager().getRegisteredContactURI( requestURI);
      if( nextUri == null) {
         // no contact found
         String msg = "createInviteForInCallRoaming: null SipUri for "+requestURI;
         logger.debug( msg);
	      throw new InvalidConfigException( msg);
      }
      SipURI sipUri = (SipURI) nextUri.clone();
      sipUri.setLrParam();
      sipUri.setParameter("remove", "true");

      RouteHeader routeHeader = headerFactory.createRouteHeader(addressFactory.createAddress(sipUri));
      sipRequest.addHeader(routeHeader);
      sipRequest.addHeader(contactHeader);
      SipUtils.addAcceptHeader(sipRequest);
      SipUtils.addAllowHeaders(sipRequest);
      SipUtils.checkContentLength(sipRequest);

      if (logger.isDebugEnabled()) {
         logger.debug("createInviteForInCallRoaming: sipRequest=\n"+ sipRequest);
      }
      return sipRequest;
   }

   /**
    * Set up an outgoing invite
    * 
    * @param scenario
    * @return Request
    * @throws Exception
    */
   public Request createInviteForU2UCall(SuToSuCallSetupScenario scenario)
      throws Exception {

      SuConfig callingSu = scenario.getCallingSuConfig();
      SuConfig calledSu = scenario.getCalledSuConfig();
      int priority = scenario.getPriority();
      boolean isEmergency = scenario.isEmergency();

      String radicalName = callingSu.getRadicalName();
      SipURI callingSipURI = SipUtils.createSipURI(radicalName, ISSIConstants.P25DR);
      callingSipURI.setParameter(ISSIConstants.USER, ISSIConstants.TIA_P25_SU);
      
      Address fromAddress = addressFactory.createAddress(callingSipURI);
      String tag = SipUtils.createTag();
      FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, tag);

      // Create the SIP URI for the Called RFSS.
      radicalName = calledSu.getRadicalName();
      SipURI calledSipURI = SipUtils.createSipURI(radicalName, ISSIConstants.P25DR);
      calledSipURI.setParameter(ISSIConstants.USER, ISSIConstants.TIA_P25_SU);
      Address toAddress = addressFactory.createAddress(calledSipURI);
      ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
      ContactHeader contactHeader = SipUtils.createContactHeaderForSU( rfssConfig, callingSu);

      ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
      LinkedList<ViaHeader> viaHeaders = new LinkedList<ViaHeader>();
      viaHeaders.add(viaHeader);

      // Get a new call ID header for the outgoing invite.
      CallIdHeader callIdHeader = getProvider().getNewCallId();
      CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);
      MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

      Request sipRequest = messageFactory.createRequest(calledSipURI,
            Request.INVITE, callIdHeader, cseqHeader, fromHeader, toHeader,
            viaHeaders, maxForwardsHeader);
      SipUtils.addPriorityHeader( sipRequest, priority, isEmergency);

      //MARKER_ROUTE_HEADER
      //refactor out
      buildRouteHeader(sipRequest, rfssConfig, callingSu, calledSu);

      sipRequest.addHeader(contactHeader);
      SipUtils.addAcceptHeader(sipRequest);
      SipUtils.addAllowHeaders(sipRequest);
      SipUtils.checkContentLength(sipRequest);

      if (logger.isDebugEnabled()) {
         logger.debug("createInviteForU2UCall: sipRequest=\n"+ sipRequest);
      }
      return sipRequest;
   }

   /**
    * Setup and send a requestOut.
    * 
    * @param inviteRequest
    * @param callingSu
    * @param calledSuConfig
    * @return UnitToUnitCall
    * @throws Exception
    */
   public UnitToUnitCall setupRequest(Request inviteRequest, SuInterface callingSu,
         SuConfig calledSuConfig) throws Exception {

      SuConfig callingSuConfig = callingSu.getSuConfig();

      // 12.26.1
      ServiceAccessPoints saps = callingSu.getServiceAccessPoints();
      //CallControlSAP ccSap = saps.getCallControlSAP();
      
      //logger.debug("setupRequest(1): callingSuConfig="+callingSuConfig.toString());
      //logger.debug("setupRequest(1): rfss="+rfss.getRfssConfig().getRfssName());
      //logger.debug("setupRequest(1): saps="+saps);
      if( saps == null) {
         saps = rfss.getServiceAccessPoints( callingSuConfig);
         callingSu.setServiceAccessPoints(saps);
         //logger.debug("setupRequest(2): saps="+saps);
      }

      PendingResponse pendingResponse = new PendingResponse();
      pendingResponse.su = callingSu;
      pendingResponse.procedure = CallControlManager.CC_SETUP_SEND_REQUEST;
      SipUtils.checkContentLength( inviteRequest);

      ClientTransaction ct = getProvider().getNewClientTransaction(inviteRequest);
      ct.setApplicationData(pendingResponse);
      Dialog dialog = ct.getDialog();
      UnitToUnitCall unitToUnitCall = new UnitToUnitCall(callingSuConfig, calledSuConfig, provider);

      // Record the priority of the call if a priority exists.
      PriorityHeader priorityHeader = (PriorityHeader) inviteRequest.getHeader(PriorityHeader.NAME);
      if (priorityHeader != null) {
         unitToUnitCall.setPriority( SipUtils.getPriorityValue(priorityHeader));
         unitToUnitCall.setEmergency( SipUtils.isEmergency(priorityHeader));
      }
      unitToUnitCall.setClientTransaction(ct);
      dialog.setApplicationData(unitToUnitCall);
      ct.sendRequest();
      logger.debug("setupRequest: inviteRequest=\n"+ inviteRequest);
      
      return unitToUnitCall;
   }

   /**
    * Send an in-call roaming request.
    * 
    * @param inviteRequest
    * @param unitToUnitCall
    * @param su
    * @throws Exception
    */
   public void setupInCallRoamingRequest(Request inviteRequest,
         UnitToUnitCall unitToUnitCall, SuInterface su) throws Exception {

      ServiceAccessPoints saps = su.getServiceAccessPoints();
      //CallControlSAP ccSap = saps.getCallControlSAP();

      //logger.debug("setupInCallRoamingRequest(1): SuConfig="+su.getSuConfig().toString());
      //logger.debug("setupInCallRoamingRequest(1): rfss="+rfss.getRfssConfig().getRfssName());
      //logger.debug("setupInCallRoamingRequest(1): saps="+saps);
      if( saps == null) {
         saps = rfss.getServiceAccessPoints( su.getSuConfig());
         su.setServiceAccessPoints(saps);
         //logger.debug("setupInCallRoamingRequest(2): saps="+saps);
      }
      SipUtils.checkContentLength(inviteRequest);
      PendingResponse pendingResponse = new PendingResponse();
      pendingResponse.su = su;
      pendingResponse.procedure = CallControlManager.CC_SETUP_SEND_REQUEST;

      ClientTransaction ct = getProvider().getNewClientTransaction(inviteRequest);
      ct.getDialog().setApplicationData(unitToUnitCall);
      ct.setApplicationData(pendingResponse);

      unitToUnitCall.setClientTransaction(ct);
      ct.sendRequest();
      logger.debug("setupInCallRoamingRequest: *** moved(ct) inviteRequest=\n"+ inviteRequest);

      //TODO: moved up, any impact ?
      //unitToUnitCall.setClientTransaction(ct);
   }

   /**
    * Send a response to the Transaction. Remove the record route header
    * because we are structuring this whole thing as a B2BUA
    * 
    * @param callSetupRequestEvent
    * @param response
    * @throws Exception
    */
   public void sendResponse(CallSetupRequestEvent callSetupRequestEvent,
         Response response) throws Exception {

      // this ftn is called by CallControlSAP
      String id = rfssConfig.getRfssName() + ":";

      // only called by CallControlSAP at 4 locations
      // 12.18.1 and 12.20.x
      logger.debug(id+"sendResponse(21): cancelReceived="+getCancelReceived());
      if( getCancelReceived()) {
         logger.debug(id+"sendResponse(21): cancelReceived - SKIP response="+response);
         return;
      }

      ServerTransaction serverTx = callSetupRequestEvent.getServerTransaction();
      int responseCode = response.getStatusCode();
      //logger.debug(id+"sendResponse(21): responseCode="+responseCode);

      if(responseCode == Response.OK) {
         CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
         String method = cseq.getMethod();
         logger.debug(id+"sendResponse(21): responseCode="+responseCode+" method="+method);
         logger.debug(id+"sendResponse(21): okSent="+getOkSent());

         if( "CANCEL".equals( method)) {
            logger.debug(id+"sendResponse(21): 200 OK-CANCEL");
            if( !getOkSent() && getCancelReceived()) {
               synchronized(okLock) {
                  setOkSent(true);
                  serverTx.sendResponse(response);
               }
            } else {
               logger.debug(id+"sendResponse(21): REQ TERM-CANCEL");
               Response xresponse = messageFactory.createResponse(
                  Response.REQUEST_TERMINATED, serverTx.getRequest());
               synchronized(okLock) {
                  serverTx.sendResponse(xresponse);
                  //setOkSent(false);
               }
            } 
         }
         else if( "INVITE".equals( method)) {
            logger.debug(id+"sendResponse(21): 200 OK-INVITE");
            //if( getCancelReceived()) {
            //   logger.debug(id+"sendResponse(21): got CANCEL, delayed 200 OK-INVITE");
            //   return;
            //}

	    if( !getOkSent() && !getCancelReceived()) {
               // 14.3.x
               synchronized(okLock) {
                  setOkSent(true);
                  serverTx.sendResponse(response);
               }
	    } else if( !getOkSent() && getCancelReceived()) {
               // 12.20.x
               synchronized(okLock) {
                  setOkSent(true);
                  serverTx.sendResponse(response);
               }
            } else {
               logger.debug(id+"sendResponse(21): REQ TERM-INVITE");
               Response xresponse = messageFactory.createResponse(
                  Response.REQUEST_TERMINATED, serverTx.getRequest());
               synchronized(okLock) {
                  serverTx.sendResponse(xresponse);
                  //setOkSent(false);
               }
            } 
         }
         else if( "BYE".equals( method)) {
            fixResponseHeaderForBye(response, callSetupRequestEvent.getCallSegment());
            serverTx.sendResponse(response);
         }
         else {
            serverTx.sendResponse(response);
         }
      }
      else {
         serverTx.sendResponse(response);
      }
      logger.debug(id+"sendResponse(21): st.state="+serverTx.getState()+ " response=\n"+response);
   }

   private void fixResponseHeaderForBye(Message response, UnitToUnitCall unitToUnitCall)
   {
      FromHeader fromHeader = (FromHeader) response.getHeader(FromHeader.NAME);
      SipURI fromUri = (SipURI) fromHeader.getAddress().getURI();
      SuConfig callingSu = topologyConfig.getSuConfig(fromUri.getUser());

      CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
      if( "BYE".equals(cseqHeader.getMethod())) {
         // 12.26.x
         //if(callingSu == unitToUnitCall.getCallingSuConfig())
            SipUtils.addAllowHeaders( response);
         if(callingSu == unitToUnitCall.getCalledSuConfig())
            response.removeHeader(ContactHeader.NAME);
         logger.debug("fixResponseHeaderForBye: fix Allow in response=\n"+response);
      }
   }

   /**
    * Teardown a call segment.
    * 
    * @param su -- Home agent interface
    * @param unitToUnitCall -- the call segment to tear down.
    * @param radicalName -- the radical name
    * @throws Exception
    */
   public void teardownRequest(HomeAgentInterface su, UnitToUnitCall unitToUnitCall,
         String radicalName) throws Exception {

      logger.debug("teardownRequest(): createRequest.BYE radicalName = " + radicalName);
      Dialog dialog = unitToUnitCall.getDialog(radicalName);

      Request byeRequest = dialog.createRequest(Request.BYE);
      SipURI sipUri = (SipURI) byeRequest.getRequestURI();
      sipUri.setUserParam(ISSIConstants.TIA_P25_SU);

//#187
//BYE sip:00002002000012@f2.001.00001.p25dr;user=TIA-P25-SU
//BYE sip:00002002000012@p25dr;user=TIA-P25-SU
//This will trim BYE sip F15 in 12.23.1
//sipUri.setHost(ISSIConstants.P25DR);
//logger.debug("teardownRequest : sipUri= " + sipUri);

      // remove route header
      RouteHeader routeHeader = (RouteHeader)byeRequest.getHeader(RouteHeader.NAME);
      if( routeHeader != null) {
         SipURI routeUri = (SipURI) routeHeader.getAddress().getURI();
         routeUri.setLrParam();
         routeUri.setParameter("remove", "true");
      }

      // Apply rule
      if(su.getSuConfig() == unitToUnitCall.getCallingSuConfig()) {
         SipUtils.addAllowHeaders(byeRequest);
      }
      else if(su.getSuConfig() == unitToUnitCall.getCalledSuConfig()) {
         byeRequest.removeHeader(AllowHeader.NAME);
      }

      SipUtils.filterSipHeaders(byeRequest);
      SipUtils.checkContentLength(byeRequest);

      // #157 12.13.1 skip the BYE in testscript.xml terminateAfter=-1 
      ClientTransaction ct = unitToUnitCall.getProvider().getNewClientTransaction(byeRequest);

      //logger.debug("teardownRequest: SKIP BYE - CC_SETUP_TEARDOWN_REQUEST by SuConfig...");
      PendingResponse pendingResponse = new PendingResponse();
      pendingResponse.procedure = CallControlManager.CC_SETUP_TEARDOWN_REQUEST;
      pendingResponse.su = su;
      pendingResponse.unitToUnitCall = unitToUnitCall;

      ct.setApplicationData(pendingResponse);
      dialog.sendRequest(ct);
      logger.debug("teardownRequest(): byeRequest=\n" + byeRequest);
   }

   /**
    * Send a response to the BYE.
    * 
    * @param requestRequest -- incoming request.
    * @throws Exception -- if something bad happened.
    */
   public void sendCallTeardownResponse(RequestEvent requestEvent)
         throws Exception {

      Request request = requestEvent.getRequest();
      ServerTransaction st = requestEvent.getServerTransaction();
      UnitToUnitCall callSegment = (UnitToUnitCall) st.getDialog().getApplicationData();

      Response response = messageFactory.createResponse(Response.OK, request);
      // 12.26.x
      //SipUtils.addAllowHeaders( response);
      fixResponseHeaderForBye(response, callSegment);

      SipUtils.checkContentLength( response);

      st.sendResponse(response);
      logger.debug("sendCallTeardownResponse: response=\n"+response);

      // Testing
      /***
      Iterator<URI> sipUris = rfssConfig.getRFSS()
            .getUnitToUnitMobilityManager().getRegisteredContactURIs(request);
      logger.debug("sendCallTeardownResponse: sipUris LIST...");
      if( sipUris != null) {
         while( sipUris.hasNext() ) {
            logger.debug("sendCallTeardownResponse: sipUri="+sipUris.next());
         }
      }
       ***/
      logger.debug("sendCallTeardownResponse: sleep 500 msec ...");
      sleep(500L);
   }

   /**
    * The callback from the sip listener. This gets called when an incoming
    * request is received by the sip stack.
    *
    * @param requestEvent -- request event.
    */
   public void processRequest(RequestEvent requestEvent) {

      String method = requestEvent.getRequest().getMethod();      
      logger.info("processRequest: " + method
            + " hostPort=" + rfssConfig.getHostPort()
            + " rfssName=" + rfssConfig.getRfssName()
            + " rfssId=" + rfssConfig.getRfssIdString()
            + " listening point="
            + ((SipProvider) requestEvent.getSource()).getListeningPoint("udp").getPort());
      
      if (Request.INVITE.equals(method)) {
         processInvite(requestEvent);
      } 
      else if (Request.ACK.equals(method)) {
         processAck(requestEvent);
      }
      else if (Request.BYE.equals(method)) {
         processBye(requestEvent);
      }
      else if (Request.CANCEL.equals(method)) {
         processCancel(requestEvent);
      }
      else {
         logger.debug("processRequest: no-op for method="+method);
      }
   }

   //--------------------------------------------------------------------------
   private void sendRequestTerminated(String fromTag, ServerTransaction st,
      Response cancelResponse) throws Exception
   {
         logger.debug(fromTag+" sendReqTerm(): st.state=" + st.getState());
	      //============ always Proceeding Transaction
         //if( st.getState() != TransactionState.TERMINATED) 
	      {
            // send 487 Request Terminated
            Response response = messageFactory.createResponse(
                  Response.REQUEST_TERMINATED, st.getRequest());

            FromHeader cancelFrom = (FromHeader)cancelResponse.getHeader(FromHeader.NAME);
            FromHeader responseFrom = (FromHeader)response.getHeader(FromHeader.NAME);
            if( responseFrom != null) {
	            if( cancelFrom != null && cancelFrom.getTag() != null)
                  responseFrom.setTag( cancelFrom.getTag()); 
            }

            ToHeader cancelTo = (ToHeader)cancelResponse.getHeader(ToHeader.NAME);
            ToHeader responseTo = (ToHeader)response.getHeader(ToHeader.NAME);
            if( responseTo != null) {
               if( cancelTo != null && cancelTo.getTag() != null)
                  responseTo.setTag( cancelTo.getTag());
               //else responseTo.setTag( SipUtils.createTag());
            }

            SipUtils.checkContentLength(response);

            //if(st.getState() != TransactionState.TERMINATED)
	         // >>> doesnot help dup Request Terminated
	         //if( !getRequestTerminatedSent()) 
	         {
               logger.debug(fromTag+" senReqTerm(): st.state="+st.getState()+"  response=\n"+response);
               st.sendResponse(response);
               //setRequestTerminatedSent( true);
            }
            //else {
            //   logger.debug(fromTag+"sendReqTerm(): SKIP 487 Request Terminated...");
            //}
         }
   }
   //--------------------------------------------------------------------------
  
   /**
    * Process an incoming CANCEL request.
    * 
    * @param requestEvent
    */
   private void processCancel(RequestEvent requestEvent) {

      setCancelReceived(true);
      logger.debug("processCancel(): START...setCancelReceived(T)");

      ServerTransaction serverTx = requestEvent.getServerTransaction();
      Request request = serverTx.getRequest();
      logger.debug("processCancel(): serverTx.state="+serverTx.getState()+" request=\n"+request);

      FromHeader from = (FromHeader) request.getHeader(FromHeader.NAME);
      ToHeader to = (ToHeader)request.getHeader(ToHeader.NAME);
      String fromTag = rfssConfig.getRfssName() +":" +"cancelFromTag=" +from.getTag() +":" +to.getTag() +" ";

      // Get the Dialog from the incoming request event.
      // this will be the same as the Dialog assigned to the INVITE
      ServerTransaction st = null;
      UnitToUnitCall unitToUnitCall = null;
      Dialog dialog = serverTx.getDialog();
      if( dialog == null) {
         // 12.20.1 repeated CANCEL sip if exception...
         //throw new RuntimeException("processCancel(): Null serverTx.dialog: request="+request);
	 
	      // send 200 OK and 487 Request Terminated
         logger.debug(fromTag+"processCancel(): null dialog path...");
         st = serverTx;
      }
      else {
         unitToUnitCall = (UnitToUnitCall) dialog.getApplicationData();
         //logger.debug(fromTag+"processCancel(): dialog.state=" + dialog.getState());
         st = unitToUnitCall.getServerTransaction();
      }

      Response cancelResponse = null;
      try {
         // send 200 OK to Cancel
         cancelResponse = messageFactory.createResponse(Response.OK, request);
	      cancelResponse.removeHeader( RecordRouteHeader.NAME);

	      /*** cause repeated CANCEL 
         ToHeader cancelToHdr = (ToHeader) cancelResponse.getHeader(ToHeader.NAME);
         cancelToHdr.setTag( SipUtils.createTag());
	       ***/
         SipUtils.checkContentLength(cancelResponse);

         logger.debug(fromTag+"processCancel(1): XXX send 200 OK st.state="+
            serverTx.getState()+" cancelResponse=\n" + cancelResponse);

         logger.debug(fromTag+"processCancel(1): okSent="+getOkSent());
         //===serverTx.sendResponse(cancelResponse);

	      /***/
         // implicitly cancelReceived is true
	      if( !getOkSent()) {
            synchronized(okLock) {
               setOkSent(true);
               serverTx.sendResponse(cancelResponse);
            }

            // 12.20.x send 487 Request Terminated
            sendRequestTerminated(fromTag, st, cancelResponse);

         } else {

	         // too late to cancel ??? 
	         // Skip 200 OK to CANCEL, send Request Terminated to CANCEL ?
            logger.debug(fromTag+"processCancel(1): okSent(T), too late, send ReqTerm");

	         // 12.20.3.x Req Term - CANCEL, missing ACK
            Response response = messageFactory.createResponse(
                  Response.REQUEST_TERMINATED,
                  serverTx.getRequest());
            synchronized(okLock) {
               serverTx.sendResponse(response);
               //setOkSent(false);
            }
	         return;
         }
	      /***/

         // 12.20.x send 487 Request Terminated
         //+++sendRequestTerminated(fromTag, st, cancelResponse);
/******
	 // 12.20.x - refactor
         //if( st.getState() != TransactionState.TERMINATED) 
	      {
            // send 487 Request Terminated
            Response response = messageFactory.createResponse(
                  Response.REQUEST_TERMINATED, st.getRequest());

            FromHeader cancelFrom = (FromHeader)cancelResponse.getHeader(FromHeader.NAME);
            FromHeader responseFrom = (FromHeader)response.getHeader(FromHeader.NAME);
            if( responseFrom != null) {
	            if( cancelFrom != null && cancelFrom.getTag() != null)
                  responseFrom.setTag( cancelFrom.getTag()); 
            }

            ToHeader cancelTo = (ToHeader)cancelResponse.getHeader(ToHeader.NAME);
            ToHeader responseTo = (ToHeader)response.getHeader(ToHeader.NAME);
            if( responseTo != null) {
               if( cancelTo != null && cancelTo.getTag() != null)
                  responseTo.setTag( cancelTo.getTag());
               //else responseTo.setTag( SipUtils.createTag());
            }

            SipUtils.checkContentLength(response);

            //if(st.getState() != TransactionState.TERMINATED)
	         {
               logger.debug(fromTag+"processCancel(2): st.state="+st.getState()+" response=\n"+response);
               st.sendResponse(response);
            }
            //else {
            //   logger.debug(fromTag+"processCancel(2): SKIP 487 Request Terminated...");
            //}
         }
 *****/

         logger.debug(fromTag+"processCancel(111): u2uCall="+unitToUnitCall);
	      //----------------------------------------------------------
         if(unitToUnitCall == null) return;

         Dialog peerDialog = unitToUnitCall.getPeerDialog(dialog);
         if (peerDialog == null) {
            logger.debug(fromTag+"processCancel(3): null peerDialog...end of line");
            // this is the end of the line.
            ToHeader toHeader = (ToHeader) st.getRequest().getHeader(ToHeader.NAME);
            SipURI sipUri = (SipURI) toHeader.getAddress().getURI();
            String radicalName = SipUtils.getRadicalName(sipUri);
            SuConfig suConfig = topologyConfig.getSuConfig(radicalName);

            try {
               TestSU testSu = (TestSU) suConfig.getSU();
               //logger.debug(fromTag+"processCancel(3b): handleCancelIndicate()");
               testSu.handleCancelIndicate(unitToUnitCall);

            } catch(Exception ex) {
                // too late to cancel, st.state not proceeding
               logger.debug(fromTag+"processCancel(3b): "+ex);
            }
         } 
         else if (peerDialog.getState() != DialogState.CONFIRMED &&
                  peerDialog.getState() != DialogState.TERMINATED) {

            ClientTransaction ct = unitToUnitCall.getClientTransaction();
            logger.debug(fromTag+"processCancel(4): forward cancel ? ct.state="+ct.getState());
            if (ct.getState() == TransactionState.PROCEEDING) {
               Request cancelRequest = ct.createCancel();

               ToHeader cancelTo = (ToHeader) cancelRequest.getHeader(ToHeader.NAME);
               //+++cancelTo.setTag( SipUtils.createTag());

               SipUtils.filterSipHeaders(cancelRequest);
               SipUtils.checkContentLength(cancelRequest);

               ClientTransaction cancelTx = getProvider().getNewClientTransaction(cancelRequest);
               cancelTx.sendRequest();
               logger.debug(fromTag+"processCancel(4): cancelTx.state="+cancelTx.getState()+
                            " cancelRequest=\n"+cancelRequest);
            } else {
               logger.debug(fromTag+"processCancel(5): Cannot forward cancel "+
                  "-- already saw a final response. ct.state="+ct.getState());
            }
         }
      } catch (Exception ex) {
         logger.debug(fromTag+"processCancel(7): fail to send 200 OK/Request Terminated: "+ex);
         //rfss.getTestHarness().fail( "Unexpected exception processing cancel", ex);
      }
   }

   private void processBye(RequestEvent requestEvent) {
      try {
         Request request = requestEvent.getRequest();
         //logger.debug("processBye(0): request=\n" + request);

         FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
         SipURI fromUri = (SipURI) fromHeader.getAddress().getURI();
         SuConfig sourceSu = topologyConfig.getSuConfig(fromUri.getUser());

         ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
         SipURI toUri = (SipURI) toHeader.getAddress().getURI();
         SuConfig targetSu = topologyConfig.getSuConfig(toUri.getUser());

         ServerTransaction serverTransaction = requestEvent.getServerTransaction();
         UnitToUnitCall callSegment = (UnitToUnitCall) serverTransaction.getDialog().getApplicationData();

         assert (callSegment.includesSU(targetSu) || callSegment.includesSU(sourceSu));
         if (callSegment.getPeerDialog(serverTransaction.getDialog()) == null) {
            if (callSegment.includesSU(targetSu)) {
               String sessionId = SipUtils.getCallIdFromMessage(request);
               rfss.getTransmissionControlManager().teardownRTPPointToMultipointSession(sessionId);
               sendCallTeardownResponse(requestEvent);
               logger.debug("processBye(1): targetSu CallTeardownIndicateEvent()...");
               targetSu.getSU().handleTeardownIndicate( new CallTeardownIndicateEvent(callSegment,requestEvent));
            } else {
               logger.debug("processBye(1): sourceSu CallTeardownIndicateEvent()...");
               sourceSu.getSU().handleTeardownIndicate( new CallTeardownIndicateEvent(callSegment,requestEvent));
            }
         }

         if (rfssConfig == sourceSu.getHomeRfss()) {
            logger.debug("processBye(2): sourceSu CallTeardownIndicateEvent()...");
            HomeAgent homeAgent = rfss.getHomeAgent(sourceSu .getRadicalName());
            homeAgent.handleTeardownIndicate(new CallTeardownIndicateEvent(callSegment,requestEvent));
         } 
	 else if (rfssConfig == targetSu.getHomeRfss()) {
            logger.debug("processBye(2): targetSu CallTeardownIndicateEvent()...");
            HomeAgent homeAgent = rfss.getHomeAgent(targetSu.getRadicalName());
            homeAgent.handleTeardownIndicate(new CallTeardownIndicateEvent(callSegment,requestEvent));
         }

         // BYE is handled hop by hop.
         Response response = messageFactory.createResponse(Response.OK,request);
         fixResponseHeaderForBye(response, callSegment);
         SipUtils.checkContentLength( response);

         serverTransaction.sendResponse(response);
         logger.debug("processBye(3): response=\n" + response);

         //----------------------------------------------
         Dialog dialog = serverTransaction.getDialog();
         Dialog peerDialog = ((UnitToUnitCall) dialog.getApplicationData()).getPeerDialog(dialog);
         if (peerDialog != null &&
             peerDialog.getState() == DialogState.TERMINATED) {
            logger.debug("peerDialog=" + peerDialog);
            logger.debug("peerDialog.state=" + peerDialog.getState());
            return;
         }
         if (peerDialog != null) {

            Request newRequest = peerDialog.createRequest(Request.BYE);
            SipURI sipUri = (SipURI) newRequest.getRequestURI();
            sipUri.setUserParam(ISSIConstants.TIA_P25_SU);

            ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
            newRequest.setHeader(viaHeader);

	         // #424 13.2.1 BYE sip
            MaxForwardsHeader reqMF = (MaxForwardsHeader)request.getHeader(MaxForwardsHeader.NAME);
            //logger.debug("processBye: reqMF=" + reqMF);
            MaxForwardsHeader byeMF = (MaxForwardsHeader)newRequest.getHeader(MaxForwardsHeader.NAME);
            byeMF.setMaxForwards(reqMF.getMaxForwards()-1);

            RouteHeader routeHeader = (RouteHeader)newRequest.getHeader(RouteHeader.NAME);
	         if( routeHeader != null) {
               SipURI routeUri = (SipURI) routeHeader.getAddress().getURI();
               routeUri.setLrParam();
               routeUri.setParameter("remove", "true");
	         }
	         // fix Allow for BYE
            if(sourceSu == callSegment.getCallingSuConfig()) {
               SipUtils.addAllowHeaders(newRequest);
            }
	         else if(sourceSu == callSegment.getCalledSuConfig()) {
               newRequest.removeHeader( AllowHeader.NAME);
            }

            SipUtils.filterSipHeaders(newRequest);
            SipUtils.checkContentLength(newRequest);

            ClientTransaction ct = getProvider().getNewClientTransaction(newRequest);
            if (logger.isDebugEnabled()) {
               logger.debug("processBye(4): sending to peerDialog: newRequest=\n"+newRequest);
            }
            peerDialog.sendRequest(ct);
         } 
         else {
            logger.debug("processBye(5): Peer dialog is null -- not forwarding BYE");
         }

      } catch (Exception ex) {
         String s = "processBye(6): internal error: ";
         rfss.logError(s, ex);
         throw new RuntimeException(s, ex);
      }
   }

   private void processAck(RequestEvent requestEvent) {
      try {
         Request request = requestEvent.getRequest();
         logger.debug("processAck(0): request=\n "+request);

         ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
         SipURI toUri = (SipURI) toHeader.getAddress().getURI();
         String targetRadicalName = toUri.getUser();
         SuConfig targetSu = topologyConfig.getSuConfig(targetRadicalName);

         ServerTransaction serverTransaction = requestEvent.getServerTransaction();
         if( serverTransaction == null) {
            throw new RuntimeException("processAck(1): Null serverTx in request event...");
         }

         // I got the ACK - I can now start media flowing the other way.
         Dialog dialog = serverTransaction.getDialog();
         if( dialog == null) {
            throw new RuntimeException("processAck(): Null dialog in server transaction...");
         }

         if (logger.isDebugEnabled()) {
            logger.debug("processAck(1): dialog=" + dialog);
         }

         UnitToUnitCall unitToUnitCall = (UnitToUnitCall) dialog.getApplicationData();
         ServerTransaction inviteTx = unitToUnitCall.getServerTransaction();

         String sessId = SipUtils.getSessionIdFromMessage(inviteTx.getRequest());
         PttSession pttSession = unitToUnitCall.getPointToMultipointSession().getMmfSession(sessId);

         SessionType sessionType = unitToUnitCall.getPointToMultipointSession().getSessionType();
         //logger.debug("processAck(): HEARTBEAT MMF-sessionType(1):"+sessionType+"  server-sessId="+sessId);

         if ( SessionType.CALLED_HOME.equals(sessionType) ||
              SessionType.CALLED_SERVING.equals(sessionType) || 
              SessionType.CALLING_HOME.equals(sessionType) ||
              SessionType.CALLING_SERVING.equals(sessionType)
            ) {

            if (pttSession != null) {
               //logger.debug("processAck(): HEARTBEAT MMF-sessionType(1): start HBTransmitter...");
               pttSession.getHeartbeatTransmitter().start();
            } else {
	       sidMap.put(sessId, unitToUnitCall);
             }
         } else {
            logger.error("processAck(): Unknown session type " + sessionType);
         }

         //--------------------------------------------------------
         if (rfss.isSubscriberUnitServed(targetSu)) {

            targetSu.getSU().handleCallSetupConfirmEvent( new CallSetupConfirmEvent(unitToUnitCall, requestEvent));
            //logger.debug("processAck(): HEARTBEAT MMF-sessionType(0): isSUServed()...");
            startHeartbeatTransmitter();
         }
	 else {
            Dialog peerDialog = unitToUnitCall.getPeerDialog(dialog);
            CSeqHeader cseq = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
            if (peerDialog != null) {

               Request newRequest = peerDialog.createAck(cseq.getSeqNumber());
               ClientTransaction ct = unitToUnitCall.getClientTransaction();
               Request ctInvite = ct.getRequest();
               String sessionId = SipUtils.getSessionIdFromMessage(ctInvite);

	       // #406 12.6.x and 13.1.x  ACK sip
	       // #424 13.2.x ACK sip
               MaxForwardsHeader mf = (MaxForwardsHeader)request.getHeader(MaxForwardsHeader.NAME);
               MaxForwardsHeader ackMF = (MaxForwardsHeader)newRequest.getHeader(MaxForwardsHeader.NAME);
               ackMF.setMaxForwards(mf.getMaxForwards()-1);
	       
//#238 Need to transfer ContentList c-resavail:1 in processAck
ContentList xcontentList = ContentList.getContentListFromMessage(ctInvite);
CallParamContent xcallParamContent = xcontentList.getCallParamContent();
if( xcallParamContent != null) {
   CallParam xcallParam = xcallParamContent.getCallParam();
   if( xcallParam.isRfResourceAvailable())
   {
      ContentList contentList = new ContentList();
      CallParamContent callParamContent = CallParamContent.createCallParamContent();
      CallParam callParam = callParamContent.getCallParam();
      //logger.debug("processAck(): transfer and setRfResourceAvailable(true)");
      callParam.setRfResourceAvailable( true);
      contentList.add(callParamContent);
   
      ContentTypeHeader contentTypeHeader = contentList.getContentTypeHeader();
      newRequest.setHeader(contentTypeHeader);
      newRequest.setContent(contentList.toString(), contentTypeHeader);
  }
}   
               startHeartbeatTransmitter(sessionId, unitToUnitCall);
               /***
               PttSession xpttSession = unitToUnitCall.getPointToMultipointSession().getMmfSession(sessionId);
               logger.debug("processAck(): HEARTBEAT MMF-sessionType(0):"+sessionType+"  client-sessionId="+sessionId);
               if (xpttSession != null) {
                  logger.debug("processAck(): HEARTBEAT MMF-sessionType(0): start client HEARTBEAT transmitter...");
                  xpttSession.getHeartbeatTransmitter().start();
               }
               else {
                  //sidMap.put( sessionId, unitToUnitCall);
                  xpttSession = unitToUnitCall.getPointToMultipointSession().getSmfSession(sessionId);
                  if (xpttSession != null) {
                     xpttSession.getHeartbeatTransmitter().start();
                  }
               }
                ***/
               startHeartbeatTransmitter();
           
               SipUtils.filterSipHeaders( newRequest);
               SipUtils.checkContentLength( newRequest);

               peerDialog.sendAck(newRequest);
               logger.debug("processAck(9): sendAck:\n"+newRequest);
            }
         }

      } catch (Exception ex) {
         String s = "processAck(): Internal error ";
         logger.error(s, ex);
         throw new RuntimeException(s, ex);
      }
   }

   private void startHeartbeatTransmitter(String sid, UnitToUnitCall unitToUnitCall) {
      //logger.debug("processAck(): HEARTBEAT MMF(10): sid="+sid);
      PttSession pttSession = unitToUnitCall.getPointToMultipointSession().getMmfSession(sid);
      if (pttSession != null) {
         //logger.debug("processAck(): HEARTBEAT MMF(10): start HEARTBEAT transmitter...");
         pttSession.getHeartbeatTransmitter().start();
      }
      else {
         pttSession = unitToUnitCall.getPointToMultipointSession().getSmfSession(sid);
         if (pttSession != null) {
            //logger.debug("processAck(): HEARTBEAT SMF(10): start HEARTBEAT transmitter...");
            pttSession.getHeartbeatTransmitter().start();
         }
      }
   }

   private void startHeartbeatTransmitter() {
      //logger.debug("processAck(): HEARTBEAT startHeartbeatTransmitter()...");
      Iterator it = sidMap.keySet().iterator();
      while( it.hasNext()) {
         String sid = (String)it.next();
         UnitToUnitCall unitToUnitCall = (UnitToUnitCall)sidMap.get(sid);
         startHeartbeatTransmitter(sid, unitToUnitCall);
      }
      sidMap.clear();
   }

   //MARKER_ROUTE_HEADER
   //-------------------------------------------------------------------------------
   private void buildRouteHeader(Request sipRequest, RfssConfig rfssConfig, 
         SuConfig callingSu, SuConfig calledSu)
      throws Exception
   {
      boolean bflag = false;
      if (callingSu.getHomeRfss() != rfssConfig)
      {
         RouteHeader routeHeader = SipUtils.createRouteToRfss(callingSu.getHomeRfss());
         SipURI sipUri = (SipURI) routeHeader.getAddress().getURI();
         sipUri.setUser(ISSIConstants.U2UORIG);

         sipRequest.addHeader(routeHeader);
         //logger.debug("buildRouteHeader(1)-U2UORIG routeHeader="+ routeHeader);
	 bflag = true;
      }
      else {
         //logger.debug("buildRouteHeader(1)-U2UORIG no RouteHeader...");
      }

      //------------------------------------------------------------------------
      if (calledSu.getHomeRfss() == rfssConfig) {
         URI nextUri = rfssConfig.getRFSS()
               .getUnitToUnitMobilityManager().getRegisteredContactURI( sipRequest);
         if( nextUri == null) {
            // check 12.5.5 and 12.9.1, 12.10.1
            // no contacts found for key : 00001001000034
            logger.debug("buildRouteHeader(2)- null sipUri from getRegisteredContactURI(): "+sipRequest);
	         throw new InvalidConfigException( "Invalid Configuration for request: "+sipRequest);
         }
	      SipURI sipUri = (SipURI) nextUri.clone();
         sipUri.setLrParam();
         sipUri.setParameter("remove", "true");

         RouteHeader routeHeader = headerFactory.createRouteHeader(addressFactory.createAddress(sipUri));
         sipRequest.addHeader(routeHeader);
         logger.debug("buildRouteHeader(2)-add RouteHeader="+ routeHeader);
      }
      else if (calledSu.getHomeRfss() != callingSu.getHomeRfss()) {

         RouteHeader routeHeader = SipUtils.createRouteToRfss( calledSu.getHomeRfss());
         SipURI sipUri = (SipURI) routeHeader.getAddress().getURI();
         sipUri.setUser(ISSIConstants.U2UDEST);

	      if( !bflag)
	      { 
            sipRequest.addHeader(routeHeader);
            //logger.debug("buildRouteHeader(3)-U2UDest add RouteHeader="+ routeHeader);
	      }
      }
   } 

   //-------------------------------------------------------------------------------
   /**
    * This function is called when processing an incoming invite. Right now it
    * only handles point to point calls.
    * 
    * @param requestEvent --
    *            incoming SIP request event.
    */
   private void processInvite(RequestEvent requestEvent)
   {
      setCancelSent(false);
      setCancelReceived(false);

      try {
         Request request = requestEvent.getRequest();
         //logger.debug("processInvite(0): request=\n"+request);

         FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
         SipURI fromUri = (SipURI) fromHeader.getAddress().getURI();
         String radicalName = fromUri.getUser();
         SuConfig fromSu = topologyConfig.getSuConfig(radicalName);
         int fromUid = fromSu.getSuId();

         ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
         SipURI toUri = (SipURI) toHeader.getAddress().getURI();
         //String targetRadicalName = toUri.getUser();
         //logger.debug("processInvite(): targetRadicalName="+targetRadicalName);
	 
         String targetRadicalName = ((SipURI) request.getRequestURI()).getUser();
         logger.debug("processInvite(0x): targetRadicalName="+targetRadicalName);
         SuConfig targetSu = topologyConfig.getSuConfig(targetRadicalName);

         // Just to be consistent 
         SuConfig callingSu = fromSu;
         SuConfig calledSu = targetSu;

         SipUtils.checkContentLength( request);
         ServerTransaction serverTransaction = requestEvent.getServerTransaction();
         if (serverTransaction == null) {
            logger.debug("processInvite(): null severTx - [getNewServerTransaction]: request=\n"+request);
            serverTransaction = getProvider().getNewServerTransaction(request);
         }

         //logger.debug("processInvite(): inviteDialog " + serverTransaction.getDialog());
         ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
         
         Request newRequest = (Request) request.clone();
         logger.debug("processInvite(9): cloned request=\n"+ newRequest);

//PTT-BUG Cannot getNewCallId --> repeated 200 OK
CallIdHeader callIdHeader = (CallIdHeader)newRequest.getHeader(CallIdHeader.NAME);
logger.debug("processInvite(9): PTT-BUG CallIdHeader="+callIdHeader);

//-----------------------For debug
//#187 12.13.5 will not work, it will affect other testcases
//logger.debug("U2UCall: processInvite(9): targetSu.getRadicalName="+ targetSu.getRadicalName());
//SipURI requestURI = SipUtils.createSipURI(targetSu.getRadicalName(), ISSIConstants.P25DR);
//logger.debug("U2UCall: processInvite(9): requestURI="+ requestURI);
//logger.debug("U2UCall: processInvite(9): rfssConfig="+ SipUtils.createDomainSipURI(rfssConfig));

SipURI requestURI = (SipURI) newRequest.getRequestURI();
//if(rfssConfig == targetSu.getHomeRfss()) {
if(rfssConfig == fromSu.getHomeRfss()) {
   requestURI = SipUtils.createDomainSipURI( targetSu.getHomeRfss());
   requestURI.setUser( targetSu.getRadicalName());
   //logger.debug("processInvite(9): targetHomeRfssURI="+ requestURI);

   //NOT YET
   //newRequest.setRequestURI( requestURI);
}
//-----------------------For debug
//logger.debug("processInvite(9): patch request=\n"+ newRequest);

         // pop the RouteHeader list
         RouteHeader firstRouteHeader = (RouteHeader)newRequest.getHeader(RouteHeader.NAME);
         newRequest.removeFirst(RouteHeader.NAME);
         //logger.debug("U2UCall: processInvite(7): removeFirst="+ firstRouteHeader);
	 
         try {
            //MARKER_ROUTE_HEADER
            buildRouteHeader(newRequest, rfssConfig, callingSu, calledSu);
            //logger.debug("U2UCall: processInvite(9): buildRouteHeader-newRequest=\n"+newRequest);
         }
         catch(InvalidConfigException ex) {
            logger.debug("processInvite(9): 12.10.x no contact: ex="+ex);
/*****
            //logger.debug("processInvite(9): >>>> no contact....callingSu");
            Response response = SipUtils.createErrorResponse( rfssConfig, request,
                  WarningCodes.NOT_FOUND_UNKNOWN_CALLED_SU);
                  // #415 12.15.1
                  //WarningCodes.FORBIDDEN_CALLING_SU_NOT_REGISTERED);
            SipUtils.checkContentLength(response);
            serverTransaction.sendResponse(response);
            logger.debug("processInvite(9): response="+response);
            return;
 ****/
         }

         //----------------------------------------------------------------------------------
         if (rfss.isSubscriberUnitServed(targetSu)) {
            // This must be the Serving RFSS of this SU. If so then call the SU.
            logger.debug("Reached the Serving RFSS of the Called SU -- invoking method");
            Dialog dialog = serverTransaction.getDialog();

            if(verbose)
            logger.debug("processInvite(9a): fromSu="+fromSu+"  targetSu="+targetSu);
            UnitToUnitCall unitToUnitCall = new UnitToUnitCall(fromSu, targetSu, provider);
            unitToUnitCall.setServerTransaction(serverTransaction);

            // Record the priority of the call if a priority exists.
            PriorityHeader priorityHeader = (PriorityHeader) request.getHeader(PriorityHeader.NAME);
            if (priorityHeader != null) {
               unitToUnitCall.setPriority( SipUtils.getPriorityValue(priorityHeader));
               unitToUnitCall.setEmergency( SipUtils.isEmergency(priorityHeader));
            }
            dialog.setApplicationData(unitToUnitCall);
            ContentList contentList = ContentList.getContentListFromMessage(request);
            CallParamContent callParamContent = contentList.getCallParamContent();
            unitToUnitCall.setCallParam(callParamContent);

//#564
logger.debug("processInvite(9a): handleSetupIndicate(): st.state="+serverTransaction.getState());
            targetSu.getSU().handleSetupIndicate( new CallSetupRequestEvent(unitToUnitCall, 
                  requestEvent, serverTransaction));
            logger.debug("processInvite(9a): handleSetupIndicate(): Done...");

         //----------------------------------------------------------------------------------
         } else if (fromSu.getHomeRfss() == rfssConfig) {

            // calling serving --> calling home ??
            logger.debug("processInvite(9b): fromSu.getHomeRfss()==rfssConfig...");

            if (!rfssConfig.isSubscriberUnitMine(fromSu)) {
               logger.debug("processInvite(9b): isSubscriberUnitMine ????");
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.NOT_FOUND_UNKNOWN_CALLING_SU);
               serverTransaction.sendResponse(response);
               return;
            }

            // Calling Home RFSS checks if the unit is allowed to make a U2U
            CallPermissionType callPermissionType = fromSu.getUserServiceProfile()
                  .getUnitToUnitCallPermission().getPermission();
            logger.debug("processInvite(9b): fromSu-isCheckCallPermission="+fromSu.isCheckCallPermission());

            // check calling su radioInhibit 
            RadioInhibitType radioInhibitType = callingSu.getUserServiceProfile()
                  .getRadioInhibit().getRadioInhibitType();

            logger.debug("processInvite(9b): callPermissionType="+callPermissionType);
            // the request is not propagated case:
            if (radioInhibitType == RadioInhibitType.RADIO_INHIBITED &&
                callPermissionType == CallPermissionType.RECEIVE) {
               logger.debug("processInvite(9b): callingSu-radioInhibitType="+radioInhibitType);
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_CALLING_SU_RADIO_INHIBITED);
               logger.debug("serverTx: send CALLING_SU_RADIO_INHIBITED");
               serverTransaction.sendResponse(response);
               return;
            }

           if( fromSu.isCheckCallPermission())
            if (callPermissionType == CallPermissionType.NONE ||
                callPermissionType == CallPermissionType.RECEIVE) {

               //#725 12.9.x
               UnitToUnitMobilityManager mobilityManager = rfssConfig.getRFSS().getUnitToUnitMobilityManager();
// For debug
logger.debug("processInvite(9b): check registered: "+ rfssConfig.isSubscriberUnitMine(fromSu));

               WarningCodes code = WarningCodes.FORBIDDEN_CALLING_SU_NOT_AUTHORIZED;
               //logger.debug("processInvite(9b): su.radicalName: "+fromSu.getRadicalName());
	       /* if( mobilityManager.getRegistration(fromSu.getRadicalName()) == null) {
                  logger.debug("processInvite(9b): null Registration: ");
		  code = WarningCodes.FORBIDDEN_CALLING_SU_NOT_REGISTERED;
               } else */
	            if( fromSu.isSkipRegisterRegister()) {
                  logger.debug("processInvite(9b): skip register..");
		            code = WarningCodes.FORBIDDEN_CALLING_SU_NOT_REGISTERED;
               }
               Response response = SipUtils.createErrorResponse(rfssConfig, request, code);
                     //WarningCodes.FORBIDDEN_CALLING_SU_NOT_AUTHORIZED);
               logger.debug("processInvite(9b): permission NONE|RECEIVE- fromSu not authorized|registered");
               serverTransaction.sendResponse(response);
               return;
            }
            logger.debug(request.getMethod() + " homeRfss of the Caller -- forwarding to homeRfss of called");

            MaxForwardsHeader mf = (MaxForwardsHeader) newRequest.getHeader(MaxForwardsHeader.NAME);
            mf.decrementMaxForwards();
            FromHeader newFromHeader = (FromHeader) newRequest.getHeader(FromHeader.NAME);

            newFromHeader.removeParameter("tag");
            newFromHeader.setTag( SipUtils.createTag());

            //logger.debug("processInvite(): 12.13.1 viaHeader="+viaHeader);
            newRequest.addFirst(viaHeader);
            //newRequest.setHeader(viaHeader);
            //logger.debug("???? 12.1.1 setHeader of viaHeader instead of addFirst...");

            RecordRouteHeader rrHeader = SipUtils.createRecordRouteHeaderForRfss(rfssConfig);
            if( firstRouteHeader != null) {
              SipURI routeHeaderUri = (SipURI) firstRouteHeader.getAddress().getURI();
              SipURI sipUri = (SipURI) rrHeader.getAddress().getURI();
              sipUri.setUser( routeHeaderUri.getUser());
            }
            newRequest.addFirst(rrHeader);
            //logger.debug("processInvite(21): rrHeader=\n"+ rrHeader);

            firstRouteHeader = (RouteHeader) newRequest.getHeader(RouteHeader.NAME);
            if( firstRouteHeader != null) {
               String targetDomainName = ((SipURI) firstRouteHeader.getAddress().getURI()).getHost();
               //logger.debug("processInvite(20): 12.13.5 targetDomainName="+targetDomainName);
               requestURI.setHost( targetDomainName);
               newRequest.setRequestURI( requestURI);
            }
            else {
               requestURI = SipUtils.createDomainSipURI( targetSu.getHomeRfss());
               requestURI.setUser( targetSu.getRadicalName());
               //logger.debug("processInvite(21): 12.23.1 targetHomeRfssURI="+ requestURI);
               newRequest.setRequestURI( requestURI);
            }
            
            // Setup the call segment for the hop
            UnitToUnitCall unitToUnitCall = new UnitToUnitCall(fromSu, targetSu, provider);

            // Record the priority of the call if a priority exists.
            PriorityHeader priorityHeader = (PriorityHeader) request.getHeader(PriorityHeader.NAME);
            if (priorityHeader != null) {
               unitToUnitCall.setPriority( SipUtils.getPriorityValue(priorityHeader));
               unitToUnitCall.setEmergency( SipUtils.isEmergency(priorityHeader));
            }
            SipUtils.checkContentLength( newRequest);
            //logger.debug("processInvite(21): newRequest=\n"+ newRequest);

            ClientTransaction ct = setupClientTransaction( serverTransaction, newRequest, fromUid,
                  LinkType.UNIT_TO_UNIT_CALLING_HOME_TO_CALLED_HOME, unitToUnitCall);
            if (ct == null) {
               logger.error("Client transaction setup error -- not creating call segment");
               return;
            }

            unitToUnitCall.setServerTransaction(serverTransaction);
            unitToUnitCall.setClientTransaction(ct);

            // 12.3.2.x forward c-duplex:1 
            ContentList contentList = ContentList.getContentListFromMessage(request);
            CallParamContent callParamContent = contentList.getCallParamContent();
            CallParam callParam = callParamContent.getCallParam();

// c-protected: 0
//TODO:
//adjustCallParamDuplexity( callParam, fromSu);

            logger.debug("processInvite(21a): request=\n"+request);
            logger.debug("processInvite(21a): callParam-request=\n"+ callParam);
            unitToUnitCall.setCallParam(callParamContent);

            serverTransaction.getDialog().setApplicationData(unitToUnitCall);
            ct.getDialog().setApplicationData(unitToUnitCall);

            PendingSDPAnswer pendingAnswer = (PendingSDPAnswer) ct.getApplicationData();
            pendingAnswer.pendingOutgoingSession.getHeartbeatReceiver().setHeartbeatListener(unitToUnitCall);
            ct.sendRequest();
            logger.debug("processInvite(21a): sendRequest=\n"+ newRequest);

            // check if callingSu radioInhibit
            if (radioInhibitType == RadioInhibitType.RADIO_INHIBITED) {
               //logger.debug("processInvite(21c): radioInhibitType="+radioInhibitType);
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_CALLING_SU_RADIO_INHIBITED);
               serverTransaction.sendResponse(response);

               logger.debug("processInvite(21c): cancelSent="+getCancelSent());
	            //>>>789setCancelSent(true);
               sendCancel(ct, response);

               logger.debug("processInvite(21c): clientTx: sendCancel for CALLING_SU_RADIO_INHIBITED: ct.state="+ct.getState());
            }
            
         //----------------------------------------------------------------------------------
         } else if (targetSu.getHomeRfss().equals(rfssConfig)) {

            logger.debug("processInvite(31): targetSu.getHomeRfss()==rfssConfig...");
            logger.debug(request.getMethod() + ":  homeRfss of the called -- forwarding to servingRfss of called");

            // There should only be one serving RFSS for this URI
            if (!rfssConfig.isSubscriberUnitMine(targetSu)) {

               // #538 12.15.x unknown called SU
               boolean isSuUnknown = rfssConfig.isSuUnknown(targetSu);
               logger.debug("processInvite(31): #538 isSuUnknown(targetSu)="+isSuUnknown);

               WarningCodes code = WarningCodes.FORBIDDEN_CALLED_SU_NOT_REGISTERED;
               if( isSuUnknown) {
                  code = WarningCodes.NOT_FOUND_UNKNOWN_CALLED_SU;
               }
               // 12.15.x requires UNKNOWN_CALLED_SU
               // 12.10.x requires CALLED_SU_NOT_REGISTERED
               Response response = SipUtils.createErrorResponse( rfssConfig, request, code);
               serverTransaction.sendResponse(response);
               return;
            }

            CallPermissionType callPermissionType = targetSu.getUserServiceProfile()
                  .getUnitToUnitCallPermission().getPermission();

            // check called su radioInhibit
            RadioInhibitType radioInhibitType = targetSu.getUserServiceProfile()
                  .getRadioInhibit().getRadioInhibitType();

            //logger.debug("processInvite(33): callPermissionType="+callPermissionType);
            if (radioInhibitType == RadioInhibitType.RADIO_INHIBITED && 
                callPermissionType == CallPermissionType.INITIATE) {
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_CALLED_SU_RADIO_INHIBITED);
               //logger.debug("processInvite(33): serverTx: send CALLED_SU_RADIO_INHIBITED");
               serverTransaction.sendResponse(response);
               return;
            }

           //logger.debug("processInvite(33): targetSu-isCheckCallPermission="+targetSu.isCheckCallPermission());
           //12.17.x
           if( targetSu.isCheckCallPermission())
            if (callPermissionType == CallPermissionType.NONE ||
                callPermissionType == CallPermissionType.INITIATE) {
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_CALLED_SU_NOT_AUTHORIZED);
               //logger.debug("processInvite(33): permission NONE|INITIATE- targetSu not authorized");
               serverTransaction.sendResponse(response);
               return;
            }

            // I am the targetSU home RFSS.
            // Get the registration record of which is the Serving RFSS.
            UnitToUnitMobilityManager mobilityManager = rfssConfig.getRFSS().getUnitToUnitMobilityManager();
            SipURI nextUri = (SipURI) mobilityManager.getRegisteredContactURI(request);
            if (nextUri == null) {
               //logger.debug("processInvite(41): >>>>>> no contact....calledSu or callingSU ???");
               // I dont know about this SU so send back a forbidden response
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_CALLED_SU_NOT_REGISTERED);
               SipUtils.checkContentLength(response);
               serverTransaction.sendResponse(response);
               logger.debug("processInvite(42): "+request.getMethod() + ": response="+response);
               return;
            }

            //logger.debug("12.23.1 nextUri.getHost="+nextUri.getHost());
            requestURI.setHost( nextUri.getHost());
            newRequest.setRequestURI( requestURI);

            ContentList contentList = ContentList.getContentListFromMessage(request);
logger.debug("processInvite(42): targetSu.CProtectedDisposition="+targetSu.getCProtectedDisposition());

            if (contentList.getCallParamContent() != null) {

               CallParam callParam = contentList.getCallParamContent().getCallParam();
               if (targetSu.getCProtectedDisposition() == CProtectedDisposition.CLEAR_PROTECTED) {
                  callParam.setProtectedModeIsSet(true);
               }

               if (callParam.isProtectedMode()) {
                  if (targetSu.getCProtectedDisposition() == CProtectedDisposition.CLEAR_PROTECTED) {
                     callParam.setProtectedMode(false);

                  } else if (targetSu.getCProtectedDisposition() == CProtectedDisposition.REJECT_PROTECTED) {
                     Response response = SipUtils.createErrorResponse(rfssConfig, request,
                        WarningCodes.FORBIDDEN_PROTECTED_MODE_AUDIO_CANNOT_BE_GRANTED);
                     serverTransaction.sendResponse(response);
                     return;
                  }
               } else {
                  if (targetSu.getCProtectedDisposition() == CProtectedDisposition.REJECT_UNPROTECTED) {
                     Response response = SipUtils.createErrorResponse(rfssConfig, request,
                        WarningCodes.FORBIDDEN_UNPROTECTED_MODE_AUDIO_CANNOT_BE_GRANTED);
                     serverTransaction.sendResponse(response);
                     return;
                  }
               }
               callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
            }
            if (logger.isDebugEnabled()) {
               logger.debug("processInvite(27): routing to RFSS " + nextUri);
            }

            nextUri.setLrParam();
            nextUri.setParameter("remove", "true");

            RouteHeader routeHeader = (RouteHeader) headerFactory.createRouteHeader(
                                          addressFactory.createAddress(nextUri));
            MaxForwardsHeader mf = (MaxForwardsHeader) newRequest.getHeader(MaxForwardsHeader.NAME);
            mf.decrementMaxForwards();

            FromHeader newFromHeader = (FromHeader) newRequest.getHeader(FromHeader.NAME);
            newFromHeader.removeParameter("tag");
            newFromHeader.setTag( SipUtils.createTag());

//Too many via header
//logger.debug("processInvite(27): 12.1.1 change to setHeader(Via)...instead of addFirst(Via)");
            newRequest.addFirst(viaHeader);

//logger.debug("processInvite(27): routeHeader(nextUri)=\n"+ routeHeader);
            newRequest.setHeader(routeHeader);

            RecordRouteHeader rrHeader = SipUtils.createRecordRouteHeaderForRfss(rfssConfig);
            //BUG: next 2 lines
            SipURI sipUri = (SipURI) rrHeader.getAddress().getURI();
            sipUri.setUser(ISSIConstants.U2UDEST);
            newRequest.addFirst(rrHeader);
//logger.debug("processInvite(27): rrHeader=\n"+ rrHeader);

            // Setup the call segment for the hop
            UnitToUnitCall unitToUnitCall = new UnitToUnitCall(fromSu, targetSu, provider);

            // Record the priority of the call if a priority exists.
            PriorityHeader priorityHeader = (PriorityHeader) request.getHeader(PriorityHeader.NAME);
            if (priorityHeader != null) {
               unitToUnitCall.setPriority( SipUtils.getPriorityValue(priorityHeader));
               unitToUnitCall.setEmergency( SipUtils.isEmergency(priorityHeader));
            }
            SipUtils.checkContentLength( newRequest);
//logger.debug("processInvite(27): newRequest=\n"+ newRequest);

            //+++unitToUnitCall.setServerTransaction(serverTransaction);
            ClientTransaction ct = setupClientTransaction( serverTransaction, newRequest, fromUid,
                  LinkType.UNIT_TO_UNIT_CALLED_HOME_TO_CALLED_SERVING, unitToUnitCall);
            if (ct == null) {
               logger.debug("Client tx setup returned null -- error condition.");
               return;
            }

            //NOTE: moved up before ct==null ???
            unitToUnitCall.setServerTransaction(serverTransaction);
            unitToUnitCall.setClientTransaction(ct);

            serverTransaction.getDialog().setApplicationData(unitToUnitCall);
            ct.getDialog().setApplicationData(unitToUnitCall);

// 12.18.1
logger.debug("processInvite(27x): cancelReceived="+getCancelReceived());
logger.debug("processInvite(27x): cancelSent="+getCancelSent());

            if( !getCancelReceived()) {
               logger.debug("processInvite(27x): st.state="+serverTransaction.getState());
               logger.debug("processInvite(27x): ct.state="+ct.getState());
               logger.debug("processInvite(27x): newRequest=\n"+ newRequest);
               ct.sendRequest();
            }

            //+++++++++++ check radioInhibit
            if (radioInhibitType == RadioInhibitType.RADIO_INHIBITED) {

               //logger.debug("processInvite(27c): radioInhibitType="+radioInhibitType);
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_CALLED_SU_RADIO_INHIBITED);
               serverTransaction.sendResponse(response);

               //logger.debug("processInvite(27c): cancelSent="+getCancelSent());
	            //>>>789setCancelSent(true);
               sendCancel(ct,response);

               if(verbose)
               logger.debug("processInvite(27c): clientTx: sendCancel for CALLED_SU_RADIO_INHIBITED: ct.state="+ct.getState());
            }

         } else {

            /**
            if (!rfssConfig.isSubscriberUnitMine(targetSu)) {
               Response response = SipUtils.createErrorResponse(rfssConfig, request,
                  WarningCodes.NOT_FOUND_UNKNOWN_CALLED_SU);
               serverTransaction.sendResponse(response);
            } else **/ 
            {
               // #381 change from 1F to 23
               Response response = SipUtils.createErrorResponse(rfssConfig, request,
                  WarningCodes.NOT_FOUND_CALLED_SUID_NOT_REGISTERED);
               serverTransaction.sendResponse(response);
            }
         }
      } catch (Throwable ex) {
         String s = "processInvite(): internal error ";
         logger.fatal(s, ex);
         throw new RuntimeException(s, ex);
      }
   }

   /***
   private void sendErrorAck(String fromTag, ResponseEvent responseEvent)
      throws Exception {

      Response response = responseEvent.getResponse();
      int statusCode = response.getStatusCode();
      logger.debug(fromTag+"sendErroAck(): statusCode="+statusCode);

      // can only create ACK for 2xx response
      // The ACK sip is automatically generated for non-2xx
      CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
      Dialog dialog = responseEvent.getDialog();
      if (dialog != null) {
         Request ackRequest = dialog.createAck(cseq.getSeqNumber());
         ackRequest.setHeader(cseq);
         SipUtils.filterSipHeaders(ackRequest);
         SipUtils.checkContentLength(ackRequest);

         dialog.sendAck(ackRequest);
         logger.debug(fromTag+"sendErroAck(): ackRequest=\n"+ackRequest);
      }
   }
    ***/

   /**
    * This gets called by the SIP listener when a response comes in.
    * 
    * @param requestEvent
    */
   public void processResponse(ResponseEvent responseEvent) {

      Response response = responseEvent.getResponse();
      FromHeader from = (FromHeader) response.getHeader(FromHeader.NAME);
      ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
      int statusCode = response.getStatusCode();

      CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
      String method = cseq.getMethod();
      String fromTag = rfssConfig.getRfssName() + ":" + "FromTag="+from.getTag() +":"+to.getTag()+":"+statusCode+":"+method+" ";

      if (logger.isDebugEnabled()) {
         logger.debug(fromTag+"processResponse():" + 
                      " hostPort=" + rfssConfig.getHostPort() +
                      " statusCode=" + statusCode);
         logger.debug(fromTag+"processResponse(): response=\n"+response);
      }

      try {
         ClientTransaction ct = responseEvent.getClientTransaction();
         if (ct == null) {
            logger.debug(fromTag+"Cannot find client transaction -- silently dropping response");
            return;
         }
         logger.debug(fromTag+"processResponse(): ct != null");
         logger.debug(fromTag+"processResponse: cancelReceived="+getCancelReceived());
         logger.debug(fromTag+"processResponse: cancelSent="+getCancelSent());
         //logger.debug(fromTag+"processResponse: okSent="+getOkSent());

	 /***/ 
	      //+++++++++++++++++ will affect 12.2.x
	      if( getCancelReceived() || getCancelSent()) {
            //logger.debug(fromTag+"processResponse(): 200 OK to method="+method);
            if (statusCode == Response.OK && Request.INVITE.equals(method)) {
               logger.debug(fromTag+"processResponse(): sending ACK...");

	            // 12.18.x send ACK after CANCEL
               Dialog dialog = responseEvent.getDialog();
               if (dialog != null) {
                  Request ackRequest = dialog.createAck(cseq.getSeqNumber());
                  SipUtils.filterSipHeaders(ackRequest);
                  SipUtils.checkContentLength(ackRequest);
                  dialog.sendAck(ackRequest);
	               setCancelSent(false);
                  logger.debug(fromTag+"processResponse(): sendAck-ackRequest=\n"+ackRequest);
                 return;
               }
            } 
            else if(statusCode == Response.REQUEST_TERMINATED && Request.CANCEL.equals(method)) {
               logger.debug(fromTag+"processResponse(): cannot sendACK...487");
	            // fall thru
	            //return;
            }
         }
	 //+++++++++++++++++
	 /***/
         String callingSuRadicalName = SipUtils.getRadicalName((SipURI) from.getAddress().getURI());
         SuConfig callingSuConfig = topologyConfig.getSuConfig(callingSuRadicalName);
         String calledSuRadicalName = SipUtils.getRadicalName((SipURI) to.getAddress().getURI());
         SuConfig calledSuConfig = topologyConfig.getSuConfig(calledSuRadicalName);

         RfssConfig callingHomeRfss = callingSuConfig.getHomeRfss();
         RfssConfig calledHomeRfss = calledSuConfig.getHomeRfss();

         if (ct == null) {
            // ct == null THIS IS DEAD CODE !!!!
            //-----------------------------------------------------------------------
            if (statusCode == Response.OK && Request.INVITE.equals(method)) {
               logger.debug(fromTag+"processResponse(): Ct is null -- resending ACK");
               Dialog dialog = responseEvent.getDialog();
               if (dialog != null) {
                  Request ackRequest = dialog.createAck(cseq.getSeqNumber());
                  SipUtils.filterSipHeaders(ackRequest);
                  SipUtils.checkContentLength(ackRequest);
                  dialog.sendAck(ackRequest);
                  logger.debug(fromTag+"processResponse(): sendAck-ackRequest=\n"+ackRequest);
               }
            } else {
               logger.debug(fromTag+"processResponse(): Ct is null -- returning");
            }

         } else {
            // ct != null
            //-----------------------------------------------------------------------
            Dialog dialog = ct.getDialog();
            Object applicationData = ct.getApplicationData();

            if (logger.isDebugEnabled()) {
               logger.debug(fromTag+"processResponse(0): applicationData=" + applicationData);
            }

            if (applicationData != null &&
                applicationData instanceof PendingResponse) {

               // This is the sending end -- the end that originated the invite
               // if this is the home of the caller add the call segement to our table.
               UnitToUnitCall unitToUnitCall = (UnitToUnitCall) dialog.getApplicationData();

               PendingResponse pendingResponse = (PendingResponse) applicationData;
	       //----------------------------------------------------------------------------
               if (pendingResponse.procedure == CallControlManager.CC_SETUP_SEND_REQUEST) {

                  UnitToUnitCallControlResponseEvent ccResponseEvent = 
                     new UnitToUnitCallControlResponseEvent(unitToUnitCall, responseEvent);

                  logger.debug(fromTag+"processResponse(): CC_SETUP_SEND_REQUEST ...handleCallSetupResponseEvent");
                  HomeAgentInterface su = pendingResponse.su;
                  if (su != null) {
                     su.handleCallSetupResponseEvent(ccResponseEvent);
		  }

                  //---------------------------------------
                  if (statusCode / 100 > 2) {
                     logger.debug(fromTag+"processResponse(): START statusCode/100 > 2...statusCode="+statusCode);
                     Request request = ct.getRequest();
                     if (logger.isDebugEnabled()) {
                        logger.debug(fromTag+"processResponse(): request=\n" + request);
                     }

                     ContentList contentList = ContentList.getContentListFromMessage(request);
                     //logger.debug("processResponse(): QQQQ contentList=" + contentList);

                     CallParamContent callParamContent = contentList.getCallParamContent();
                     if( callParamContent != null) {
                        CallParam callParam = callParamContent.getCallParam();

                        //#200 U2U_SET_RF_RESOURCE
                        //logger.debug(fromTag+"processResponse(): setRfResourceAvailable()...");
                        callParam .setRfResourceAvailable( rfssConfig.isRfResourcesAvailable());

                        if (callParam.isIncallRoaming()) {
                           FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
                           String radicalName = ((SipURI) fromHeader.getAddress().getURI()).getUser();

                           // 12.25.1 teardownCallSegmentOnError=true or teardownCallSegment=true ? 
                           logger.debug(fromTag+"processResponse(): TEMP DISABLED teardownRequest(): radicalName="+radicalName);
                           // DISABLED: teardownRequest(null, unitToUnitCall, radicalName);
			   
                        } else {
                           logger.debug(fromTag+"processResponse(): callParam.isIncallRoaming:false");
                        }
                     }  // calParamContent
                     logger.debug(fromTag+"processResponse(): DONE statusCode/100 > 2...statusCode="+statusCode);
                  }

	       //----------------------------------------------------------------------------
               } else if (pendingResponse.procedure == CallControlManager.CC_SETUP_TEARDOWN_REQUEST) {

                  logger.debug(fromTag+"processResponse(): CC_SETUP_TEARDOWN_REQUEST su.handleTeardownResponse...");
                  CallTeardownResponseEvent ccTeardownEvent = new CallTeardownResponseEvent(
                        unitToUnitCall, responseEvent);
                  HomeAgentInterface su = pendingResponse.su;
                  if (su != null) {
                     su.handleTeardownResponse(ccTeardownEvent);
                  }
               }
	       //----------------------------------------------------------------------------

            } else if (applicationData != null) {

logger.debug(fromTag+"processResponse(): appData != null && pendingAnswer...");
               PendingSDPAnswer pendingAnswer = (PendingSDPAnswer) applicationData;
               ServerTransaction st = pendingAnswer.serverTransaction;
               Request originalRequest = st.getRequest();
logger.debug(fromTag+"processResponse(): originalRequest=\n"+originalRequest);

               if (statusCode == Response.TRYING) {
                  logger.debug(fromTag+"processResponse(): Got 100 response -- not forwarded");
                  return;
               }

// Request Terminated: Confirmed Transaction
logger.debug(fromTag+"processResponse(): st.state="+st.getState());

               Response newResponse = (Response) response.clone();
               newResponse.setHeader( SipUtils.createTimeStampHeader());
// 12.22.x
if( statusCode == Response.BUSY_HERE) {
   ToHeader newToHeader = (ToHeader) newResponse.getHeader(ToHeader.NAME);
   newToHeader.setTag( SipUtils.createTag());
   logger.debug(fromTag+"processResponse(): BUSY_HERE toHeader="+newToHeader);
}

	       // pop the via list
               newResponse.removeFirst(ViaHeader.NAME);
logger.debug(fromTag+"processResponse(): after POP viaHeader - newRequest=\n"+newResponse);

               //String xmethod = ((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod();
               //logger.debug(fromTag+"processResponse(): response-xmethod="+xmethod);
               if ( Request.INVITE.equals(method)) {
                  FromHeader fromHeader = (FromHeader) originalRequest.getHeader(FromHeader.NAME);
                  FromHeader newFromHeader = (FromHeader) newResponse.getHeader(FromHeader.NAME);
                  newFromHeader.setTag(fromHeader.getTag());

// 12.6.x Need a new ToHeader tag
//ToHeader newToHeader = (ToHeader) newResponse.getHeader(ToHeader.NAME);
//newToHeader.setTag( SipUtils.createTag());
//logger.debug(fromTag+"processResponse(): add ToHeader tag...");
               }

               /*
                * Got a 200 OK -- so get the SDP listener going. This part
                * of the code gets involved in the forwarding of a
                * response. When the response is received.
                */
               if (Request.INVITE.equals(method)) {

                  if (statusCode == Response.TRYING) {
                     logger.debug(fromTag+"processResponse(): Not forwarding the 100 response");

                  } else if (statusCode == Response.OK) {

logger.debug(fromTag+"processResponse(): statusCode 200...response=\n"+response);
                     /*
                      * Note that we stored the SDP offer in the pending
                      * answer during the time the request was forwarded.
                      * We add a media leg corresponding to the Sdp Offer
                      * now.
                      * 
                      * Add a media leg corresponding to the SdpAnswer now.
                      */
                     ContentList contentList = ContentList.getContentListFromMessage(response);
                     SdpContent sdpAnswerContent = (SdpContent) 
                           contentList.getContent( ISSIConstants.APPLICATION, ISSIConstants.SDP);
//#580
                   String senderDomainName = null;
                   if(sdpAnswerContent != null) {
                     SessionDescription sdpAnswer = sdpAnswerContent.getSessionDescription();

                     /*
                      * at this point, we have an answer from the other
                      * end to whom we originally forwarded the INVITE.
                      * we know what port the other end is listening at.
                      * So we put that information to our pending
                      * session. AddMeiaLeg simply fixes up the rtp
                      * session and inserts the information where the
                      * other end is listening. This media leg belongs to
                      * the host that sent us the request.
                      */
                     //String senderDomainName = null;
                     if (rfssConfig == calledHomeRfss) {
                        senderDomainName = SipUtils.getContactHost(response);
                     } else {
                        senderDomainName = calledHomeRfss.getDomainName();
                     }

                     rfss.getTransmissionControlManager().addMediaLeg( 0,
                         pendingAnswer.pendingOutgoingSession, sdpAnswer, senderDomainName);
                   }
                     /*
                      * Now we need to setup an RTP / PTT session to
                      * listen for the packets from the side that sent us
                      * the INVITE and for whom we are about to forward
                      * the response. For this We create an rtp session
                      * which will listen on a random even port. Here we
                      * are setting up a session for the end that sent us
                      * the invite.
                      */
                     LinkType linkType = null;
                     if (rfssConfig == calledHomeRfss) {
                        linkType = LinkType.UNIT_TO_UNIT_CALLED_HOME_TO_CALLING_HOME;
                     } else if (rfssConfig == callingHomeRfss) {
                        linkType = LinkType.UNIT_TO_UNIT_CALLING_HOME_TO_CALLING_SERVING;
                     }
                     MmfSession outgoingSession = null;
                     String sessId = ((OriginField) pendingAnswer.incomingSdpOffer
                           .getOrigin()).getSessIdAsString();

                     try {
                        outgoingSession = pendingAnswer.pointToMultipointSession.createMmfSession(linkType, sessId);
                        outgoingSession.setSetIsNotInterestedInLosingAudio(rfssConfig.isNotInterestedInLosingAudio());
                        if (rfssConfig == calledHomeRfss) {
                           String remoteRfssDomainName = callingSuConfig.getHomeRfss().getDomainName();
                           outgoingSession.setRemoteRfssDomainName(remoteRfssDomainName);
                        } else {
                           String remoteRfssDomainName = SipUtils.getViaHost(pendingAnswer.serverTransaction.getRequest());
                           outgoingSession.setRemoteRfssDomainName(remoteRfssDomainName);
                        }

                     } catch (RtpException ex) {

                        Response errorResponse = SipUtils.createErrorResponse(rfssConfig,
                                 pendingAnswer.serverTransaction.getRequest(),
                                 WarningCodes.NO_RTP_RESOURCE);
//errorResponse.removeHeader(RouteHeader.NAME);
//logger.debug(fromTag+"processResponse(2): NO_RTP_RESOURCE...errorResponse="+errorResponse);
                        pendingAnswer.serverTransaction.sendResponse(errorResponse);
                        pendingAnswer.pointToMultipointSession.shutDown();
                        return;
                     }

                     /*
                      * this media leg belongs to the host that sent us
                      * the response
                      */
                     senderDomainName = SipUtils.getViaHost(pendingAnswer.serverTransaction.getRequest());

                     getTransmissionControlManager().addMediaLeg(0, outgoingSession,
                           pendingAnswer.incomingSdpOffer, senderDomainName);
                     CallControlManager.fixupSdpAnnounce( rfssConfig, outgoingSession, pendingAnswer.incomingSdpOffer);

                     String sdpBody = pendingAnswer.incomingSdpOffer.toString();
                     SdpContent sdpContent = SdpContent.createSdpContent(sdpBody);
                     contentList.setContent( ISSIConstants.APPLICATION, ISSIConstants.SDP, sdpContent);

                     /*
                      * copy the content type header from the incoming
                      * response note that we may be dealing with a
                      * multipart mime header here.
                      */
                     UnitToUnitCall unitToUnitCall = (UnitToUnitCall) dialog.getApplicationData();
                     unitToUnitCall.setPointToMultipointSession(pendingAnswer.pointToMultipointSession);
                     outgoingSession.getHeartbeatReceiver().setHeartbeatListener(unitToUnitCall);

                     if (rfssConfig == callingHomeRfss) {
logger.debug(fromTag+"processResponse(1): callingHomeRfss...START");
                        /*
                         * Inform the calling home RFSS to add a new
                         * call segment to the HomeAgent there. if this
                         * is the home of the caller add the call
                         * segement to our table.
                         */
                        UnitToUnitCallControlResponseEvent ccResponseEvent = new UnitToUnitCallControlResponseEvent(
                              unitToUnitCall, responseEvent);
                        HomeAgentInterface homeAgent = rfss.getHomeAgent(callingSuConfig.getRadicalName());
                        homeAgent.handleCallSetupResponseEvent(ccResponseEvent);

                        CallParamContent callparamContent = contentList.getCallParamContent();
                        CallParam callParam = callparamContent.getCallParam();

                        unitToUnitCall.setCallingSuInitrans(callParam.isCallingSuInitialTransmitter());
                        //callParam.setRfResourceAvailable(rfssConfig.getRFSS().isRfResourcesAvailable());

logger.debug(fromTag+"processResponse(1): callingHomeRfss...DONE");

                     } else if (rfssConfig == calledHomeRfss) {

logger.debug(fromTag+"processResponse(2): calledHomeRfss...START");
                        /*
                         * Inform the called home RFSS to add a new call
                         * segment to the home agent there.
                         */

                        UnitToUnitCallControlResponseEvent ccResponseEvent = new UnitToUnitCallControlResponseEvent(
                              unitToUnitCall, responseEvent);
                        HomeAgentInterface homeAgent = rfss.getHomeAgent(calledSuConfig.getRadicalName());
                        homeAgent.handleCallSetupResponseEvent(ccResponseEvent);

                        /*
                         * If we are the called home RFSS we set up the
                         * c-initrans parameter for the 200 OK response.
                         */
                        CallParamContent callparamContent = contentList.getCallParamContent();
                        CallParam callParam = callparamContent.getCallParam();

                        callParam.setCallingSuInitialTransmitter( rfssConfig.isCallingSuInitialTransmitter());
                        unitToUnitCall.setCallingSuInitrans(rfssConfig.isCallingSuInitialTransmitter());
                        //callParam.setRfResourceAvailable(rfssConfig.getRFSS().isRfResourcesAvailable());

                        logger.debug(fromTag+"processResponse(2): calledHomeRfss...DONE");
                     }

                     //----------------------------------------
                     if (logger.isDebugEnabled()) {
                        logger.debug(fromTag+"Saw a 200 OK. Here is the Transmission Control manager sessions");
                        logger.debug(rfss.getTransmissionControlManager().toString());
                        logger.debug("-------------------------------------------------");
                     }
                     newResponse.setContent(contentList.toString(), contentList.getContentTypeHeader());

                     // #202 200 OK
                     SipUtils.addAllowHeaders(newResponse);

                     //998 Fix Allow in 200 OK to BYE
                     fixResponseHeaderForBye(newResponse, unitToUnitCall);

                     SipUtils.checkContentLength(newResponse);

logger.debug(fromTag+"processResponse(666): st.state="+st.getState());
logger.debug(fromTag+"processResponse(666): sendResponse: newResponse=\n"+newResponse);
                     st.sendResponse(newResponse);

                  } else if (statusCode / 100 > 2) {

logger.debug(fromTag+"processResponse(): statusCode/100 > 2, st.state="+st.getState());
                     // #163
                     // Error response --- tear down the session.
                     // or do something else like that...TODO
                     if (st.getState() != TransactionState.TERMINATED &&
                         st.getState() != TransactionState.CONFIRMED) {
logger.debug(fromTag+"processResponse(): busyHereSent="+getBusyHereSent());
logger.debug(fromTag+"processResponse(): cancelReceived="+getCancelReceived());

                     //EHC:999 200 OK to INVITE
                     synchronized(okLock) {
                        setOkSent(true);
                        st.sendResponse(newResponse);
                     }

logger.debug(fromTag+"processResponse(): sendResponse(2): statusCode="+statusCode+" newResponse=\n"+newResponse);

//logger.debug(fromTag+"processResponse(): rc="+statusCode+" >>> send ACK to client ???");
                        //+++sendErrorAck(fromTag, responseEvent);

                     } else {
                        // TODO: if sc=487 Request Terminated, need to forward ??
                        logger.debug(fromTag+"processResponse(): no-action due to state="+st.getState());
                     }

                  } else {

                     //  statusCode: 1XX
logger.debug(fromTag+"processResponse(): sendResponse(3): statusCode="+statusCode+" newResponse=\n"+newResponse);

                     // #347 could be 180 Ringing, 183 Session Progress
		     // 12.6.1.1 SIP order 180 Ringing and 183 Session Progress
                     st.sendResponse(newResponse);
                  }
               }  // INVITE
	       else if (Request.CANCEL.equals(method)) {

                  logger.debug(fromTag+"processResponse(88): Got 200 OK for CANCEL....state="+st.getState());
               }
            } 
	    else 
	    {
logger.debug(fromTag+"processResponse(): null-applicationData, method="+method);
               UnitToUnitCall unitToUnitCall = (UnitToUnitCall) dialog.getApplicationData();

               if (Request.BYE.equals(method)) {
                  if (rfssConfig == calledHomeRfss) {
                     CallTeardownResponseEvent ccTeardownEvent = new CallTeardownResponseEvent(
                           unitToUnitCall, responseEvent);
                     HomeAgentInterface homeAgent = rfss.getHomeAgent(calledSuConfig.getRadicalName());
                     homeAgent.handleTeardownResponse(ccTeardownEvent);

                  } else if (rfssConfig == callingHomeRfss) {
                     CallTeardownResponseEvent ccTeardownEvent = new CallTeardownResponseEvent(
                           unitToUnitCall, responseEvent);
                     HomeAgentInterface homeAgent = rfss.getHomeAgent(callingSuConfig.getRadicalName());
                     homeAgent.handleTeardownResponse(ccTeardownEvent);
                  }

               } else if (Request.CANCEL.equals(method)) {

                  logger.debug(fromTag+"processResponse(7): Got response from CANCEL -- not forwarding");

                  ServerTransaction st = unitToUnitCall.getServerTransaction();
                  logger.debug(fromTag+"processResponse(7): st.state="+st.getState());
                  logger.debug(fromTag+"processResponse(7): st.getRequest="+st.getRequest());

		  // 12.20.x
                  /****** 7788 send Request_Terminated  
                  if( st.getState() != TransactionState.TERMINATED) 
		  {
                     // send 487 Request Terminated
                     Response xresponse = messageFactory.createResponse(
                        Response.REQUEST_TERMINATED, st.getRequest());

                     FromHeader cancelFrom = (FromHeader) response.getHeader(FromHeader.NAME);
                     FromHeader responseFrom = (FromHeader) xresponse.getHeader(FromHeader.NAME);
                     responseFrom.setTag( cancelFrom.getTag());


                     ToHeader cancelTo = (ToHeader) response.getHeader(ToHeader.NAME);
                     ToHeader responseTo = (ToHeader) xresponse.getHeader(ToHeader.NAME);
                     if( responseTo != null) {
                        responseTo.setTag( cancelTo.getTag());
                     }
                     logger.debug(fromTag+"processCancel(7): xresponse=\n"+xresponse);
                     st.sendResponse(xresponse);
                  }  
                   ******/
               }
               logger.debug(fromTag+"processResponse(): not forwarding the response " +
                  "-- this must be a point to point request (i.e. CANCEL or BYE)");
            }
         }
      } catch (Exception ex) {
         String s = fromTag+"unexpected exception forwarding response";
         logger.error(s, ex);
         throw new RuntimeException(s, ex);
      }
   }

   //----------------------------------------------------------------------------------
   public void processTimeout(TimeoutEvent arg0) {
      logger.debug("processResponse(): processTimeout...");
   }

   public void processIOException(IOExceptionEvent arg0) {
      logger.debug("processResponse(): processIOException...");
   }

   public void processTransactionTerminated(TransactionTerminatedEvent event) {
      logger.debug("processResponse(): processTransactionTerminated...isServer="+event.isServerTransaction());
      Request request;
      if (event.isServerTransaction()) {
         request = event.getServerTransaction().getRequest();
         logger.debug("processTransactionTerminated(1): state="+event.getServerTransaction().getState());
         logger.debug("processTransactionTerminated(1): request="+request);

      } else {
         request = event.getClientTransaction().getRequest();
         logger.debug("processTransactionTerminated(2): state="+event.getClientTransaction().getState());
         logger.debug("processTransactionTerminated(2): request="+request);
      }
      logger.debug("processResponse(): processTransactionTerminated...DONE");
   }

   public void processDialogTerminated(DialogTerminatedEvent dte) {
      Dialog dialog = dte.getDialog();
      UnitToUnitCall unitToUnitCall = (UnitToUnitCall) dialog.getApplicationData();
      PttPointToMultipointSession pttSession = unitToUnitCall.getPointToMultipointSession();
      if (pttSession != null) {
         pttSession.shutDown();
      } else {
         logger.debug("PttSession is null for " + unitToUnitCall);
      }
   }

   public SipProvider getProvider() {
      return provider;
   }

   /**
    * Drop all the calls that are known to this call control manager. This
    * method terminates all call segments for all served subscriber units.
    */
   public void dropAllCalls() {
      for (SuConfig suConfig : rfss.getServedSubscriberUnits()) {
         if (suConfig.getSU() != null) {
            TestSU testSu = (TestSU) suConfig.getSU();
            testSu.terminateUnitToUnitCall();
         }
      }
   }

   /**
    * Send a cancel for this client transaction.
    * 
    * @param ct -- Client Transaction
    * @throws Exception
    */
   public void sendCancel(ClientTransaction ct) throws Exception {

      Request ctRequest = ct.getRequest();
      logger.debug("sendCancel(0): ctState="+ct.getState()+" ctRequest=\n"+ctRequest);

      Request cancelRequest = ct.createCancel();

      MaxForwardsHeader mfHeader = headerFactory.createMaxForwardsHeader(70);
      cancelRequest.setHeader(mfHeader);

//TODO: need ToHeader tag ???

      // remove RecordRoute
      SipUtils.filterSipHeaders(cancelRequest);

      // 12.7.1 transfer Via header
      ViaHeader viaHeader = (ViaHeader) ctRequest.getHeader(ViaHeader.NAME);
      if( viaHeader != null) {
         cancelRequest.setHeader(viaHeader);
      }
      RouteHeader routeHeader = (RouteHeader) ctRequest.getHeader(RouteHeader.NAME);
      if( routeHeader != null) {
         cancelRequest.setHeader(routeHeader);
      }
      SipUtils.checkContentLength(cancelRequest);
      
      ClientTransaction cancelTx = getProvider().getNewClientTransaction(cancelRequest);
      cancelTx.sendRequest();
      //>>>789
      setCancelSent(true);

      if(verbose)
      logger.debug("sendCancel(0): cancelRequest=\n"+cancelRequest);
   }

   public void sendCancel(ClientTransaction ct, Response response) 
      throws Exception {

logger.debug("sendCancel(888): ct.getState="+ct.getState());
      Request cancelRequest = ct.createCancel();
      ToHeader cancelTo = (ToHeader) cancelRequest.getHeader(ToHeader.NAME);
      //logger.debug("sendCancel(1): cancelTo="+cancelTo);

      // #344 MaxForwards
      MaxForwardsHeader mfHeader = headerFactory.createMaxForwardsHeader(70);
      cancelRequest.setHeader(mfHeader);

// check 12.7.x
//#564 12.20.x
      // create a new tag
      //+++cancelTo.setTag( SipUtils.createTag());
      //logger.debug("sendCancel(2): AFTER cancelTo="+cancelTo);

      /**
      //12.20.1 need a tag in SIP CANCEL ToHeader 
      ToHeader responseTo = (ToHeader) response.getHeader(ToHeader.NAME);
      //logger.debug("sendCancel(3): responseTo="+responseTo);
      if( responseTo != null && responseTo.getTag() != null) {
         cancelTo.setTag( responseTo.getTag());
         logger.debug("sendCancel(): AFTER cancelTo="+cancelTo);
      }
       **/

      // remove RecordRoute
      SipUtils.filterSipHeaders(cancelRequest);
      SipUtils.checkContentLength(cancelRequest);
      
      ClientTransaction cancelTx = getProvider().getNewClientTransaction(cancelRequest);
      cancelTx.sendRequest();
      //>>>789
      setCancelSent(true);

      if(verbose)
      logger.debug("sendCancel(4): cancelRequest=\n"+cancelRequest);
   }

   /**
    * Send the response via a server transaction .
    * 
    * @param serverTransaction -- the server transaction to respond to.
    * @param responseCode -- the response code.
    * @throws Exception
    */
   public void sendResponse(ServerTransaction st, int responseCode) 
      throws Exception {

      Request request = st.getRequest();
      //Response response = messageFactory.createResponse(responseCode, request);
      Response response = createResponse(responseCode, request);
      //logger.debug("sendResponse(XXX): request="+request);
      
// if 200 OK for INVITE, need ContactHeader
      String method = request.getMethod();
      if(responseCode == Response.OK && "INVITE".equals(method)) {
        ContactHeader respContact = (ContactHeader)response.getHeader(ContactHeader.NAME);
	if( respContact == null) {
         String sid = SipUtils.getCallIdFromMessage(request);
         UnitToUnitCall unitToUnitCall = (UnitToUnitCall)sidMap.get(sid);
	 if(unitToUnitCall == null) {
            logger.debug("sendResponse(XXX): null callsegment: "+sid +" for "+request);
            return;
         }
         //SuConfig calledSu = unitToUnitCall.getCalledSuConfig();
         SuConfig callingSu = unitToUnitCall.getCallingSuConfig();
	 if(callingSu == null) {
            // transaction terminated ?
            logger.debug("sendResponse(XXX): callingSu null, SKIP 200 OK for "+request);
            return;
         }
         ContactHeader contactHeader = SipUtils.createContactHeaderForSU(rfssConfig, callingSu);
         response.setHeader(contactHeader);
        }
      }
      else if(responseCode == Response.OK && "BYE".equals(method)) {
//995
         String sid = SipUtils.getCallIdFromMessage(request);
         UnitToUnitCall unitToUnitCall = (UnitToUnitCall)sidMap.get(sid);
         //ServerTransaction st = requestEvent.getServerTransaction();
         //UnitToUnitCall callSegment = (UnitToUnitCall) st.getDialog().getApplicationData();
         fixResponseHeaderForBye(response, unitToUnitCall);
      }

      logger.debug("sendResponse(XXX): response="+response);
      st.sendResponse(response);
   }

   public void alertRfResourceChange(boolean resourceVal) {
      logger.debug("alertRfResourceChange()...resourceVal="+resourceVal);
   }

   // utility functions (moved to SipUtils ???)
   //--------------------------------------------------------------------------
   public Response createResponse(int responseCode, Request request)
      throws Exception {
      Response response = messageFactory.createResponse(responseCode, request);

      // Tag for ToHeader is mandatory in 2xx response.
      ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
      if (response.getStatusCode() / 100 == 2) {
         toHeader.setTag( SipUtils.createTag());
         //===SipUtils.addAllowHeaders( response);
      }
      //===SipUtils.checkContentLength( response);
      return response;
   }
   private void sleep(long msec) {
      try {
         Thread.sleep( msec);
      } catch(Exception ex) { }
   }

   /***
   //--------------------------------------------------------------------------
   public void adjustCallParamDuplexity( CallParam callParam, SuConfig calledSuConfig)
   {
         UserServiceProfile userProfile = calledSuConfig.getUserServiceProfile();

         // Adjust the duplexity of the call.
         //-------------------------------------------------------------------
         DuplexityType duplexity = userProfile.getDuplexity().getDuplex();

         // based on testscript isFullDuplex setting
         callParam.setFullDuplexRequestedIsSet(userProfile.isFullDuplexIsSet());

         // 12.3.2.x
         if( callParam.isFullDuplexRequested() ) {
            if( duplexity == DuplexityType.FULL) {
               callParam.setFullDuplexRequested(true);
            } 
	    else if( duplexity == DuplexityType.HALF) {
               callParam.setFullDuplexRequested(false);
            }
         }
   }
    ***/
   //--------------------------------------------------------------------------
}
