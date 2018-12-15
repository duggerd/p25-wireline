//
package gov.nist.p25.issi.rfss;

import gov.nist.javax.sdp.fields.OriginField;
import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.CProtectedDisposition;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SuState;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.CallParamContent;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25body.SdpContent;
import gov.nist.p25.issi.p25body.params.CallParam;
import gov.nist.p25.issi.p25body.serviceprofile.AvailabilityCheckType;
import gov.nist.p25.issi.p25body.serviceprofile.CallSetupPreferenceType;
import gov.nist.p25.issi.p25body.serviceprofile.DuplexityType;
import gov.nist.p25.issi.p25body.serviceprofile.user.UserServiceProfile;
import gov.nist.p25.issi.rfss.SipUtils;
//import gov.nist.p25.issi.rfss.TransmissionControlSAP;
import gov.nist.p25.issi.rfss.UnitToUnitCall;
import gov.nist.p25.issi.rfss.tester.GroupCallSetupScenario;
import gov.nist.p25.issi.rfss.tester.SuToSuCallSetupScenario;
import gov.nist.p25.issi.transctlmgr.PttPointToMultipointSession;
import gov.nist.p25.issi.transctlmgr.TransmissionControlManager;
import gov.nist.p25.issi.transctlmgr.ptt.LinkType;
import gov.nist.p25.issi.transctlmgr.ptt.PttSession;
import gov.nist.p25.issi.transctlmgr.ptt.SessionType;
import gov.nist.p25.issi.transctlmgr.ptt.SmfSession;
import gov.nist.p25.issi.utils.WarningCodes;
import gov.nist.rtp.RtpException;

import java.util.Hashtable;
import java.util.ListIterator;

import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.TransactionState;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WarningHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import org.apache.log4j.Logger;

/**
 * Implementation of the Call Control SAP abstraction for the emulated RFSS.
 */
public class CallControlSAP {

   private static Logger logger = Logger.getLogger(CallControlSAP.class);
   //public static void showln(String s) { System.out.println(s); }
   
   private final SuConfig suConfig;
   private final UnitToUnitCallControlManager unitToUnitCallControlManager;
   private final GroupCallControlManager groupCallControlManager;
   private final TransmissionControlManager transmissionControlManager;
   
   private ServiceAccessPoints saps;
   private RfssConfig rfssConfig;
   private TopologyConfig topologyConfig;
   private Hashtable<String, PttSession> pendingRtpSessions;

   // accessors
   public RfssConfig getRfssConfig() {
      return rfssConfig;
   }
   public SuConfig getSuConfig() {
      return suConfig;
   }

   // constructor
   //-----------------------------------------------------------------------
   public CallControlSAP(RFSS rfss, SuConfig suConfig) {
      this.suConfig = suConfig;
      this.rfssConfig = rfss.getRfssConfig();
      this.topologyConfig = this.rfssConfig.getSysConfig().getTopologyConfig();
      this.unitToUnitCallControlManager = rfss.getCallControlManager().getUnitToUnitCallControlManager();
      this.groupCallControlManager = rfss.getCallControlManager().getGroupCallControlManager();
      this.transmissionControlManager = rfss.getTransmissionControlManager();
      this.pendingRtpSessions = new Hashtable<String, PttSession>();
   }
      
   public void setSaps(ServiceAccessPoints saps) {
      this.saps = saps;
   }

   public UnitToUnitCall ccSendCallSetupRequest(SuToSuCallSetupScenario callSetupScenario)
         throws Exception {

      SuConfig calledSuConfig = callSetupScenario.getCalledSuConfig();
      boolean isEmergency = callSetupScenario.isEmergency();
      logger.debug("ccSendCallSetupRequest: protection=" + callSetupScenario.isProtectedCall());

      SipProvider provider = unitToUnitCallControlManager.getProvider();
      CallIdHeader callId = provider.getNewCallId();
      PttPointToMultipointSession ptpSession = transmissionControlManager
            .getPttPointToMultipointSession(callId.getCallId(), SessionType.CALLING_SERVING);

      // the following should be random
      long sessId = (long) (Math.random() * 100000);
      long sessVersion = (long) (Math.random() * 100000);
      String sessIdKey = Long.toString(sessId);

      // we are setting up the side of the RTP session
      // where we are going to be listening for incoming packets.
      // this is where we setup the initial SDP OFFER. We attach
      // it to the SIP INIVTE. Note in that we do not know
      // where the next hop is listening at this point. We will
      // know that only when the answer comes back.
      SmfSession smfSession = ptpSession.createSmfSession(
            LinkType.UNIT_TO_UNIT_CALLING_SERVING_TO_CALLING_HOME, sessIdKey);
      smfSession.setRemoteRfssDomainName(suConfig.getHomeRfss().getDomainName());
      
      // In this case the current test case becomes the listener
      // becuase we want to be able to trigger on message match.
      smfSession.getSmfReceiver().addListener(rfssConfig.getRFSS().getCurrentTestCase());

      int port = smfSession.getMyRtpRecvPort();
      pendingRtpSessions.put(sessIdKey, smfSession);
      //logger.debug("ccSendCallSetupRequest(): pendingRtpSessions.put - sessId="+sessId);
   
      String ipAddress = suConfig.getHomeRfss().getIpAddress();
      //String callIdSuffix = callSetupScenario.getId();
      
      //PTT-BUG
      //-------------------------
      ipAddress = rfssConfig.getIpAddress();
      logger.debug("ccSendCallSetupRequest(): PTT-BUG after ipAddress="+ipAddress);

      Request inviteRequest = unitToUnitCallControlManager.createInviteForU2UCall( callSetupScenario) ;
           
      String sdpBody = SipUtils.createSdpBody(sessId, sessVersion, ISSIConstants.P25_U2U_CALL, ipAddress, port);
      inviteRequest.setHeader(callId);

      ContentList contentList = new ContentList();
      CallParamContent callParamContent = CallParamContent.createCallParamContent();
      CallParam callParam = callParamContent.getCallParam();
      SuConfig callingSuConfig = callSetupScenario.getCallingSuConfig();
      UserServiceProfile userProfile = callingSuConfig.getUserServiceProfile();

      logger.debug("ccSendCallSetupRequest(): isFullDuplexCall="+ callSetupScenario.isFullDuplexCall());
      // 12.3.1.x c-duplex:0 
      callParam.setFullDuplexRequested(callSetupScenario.isFullDuplexCall());
      callParam.setProtectedMode(callSetupScenario.isProtectedCall());

      logger.debug("ccSendCallSetupRequest(): calledSu.CProtectedDisp="+ calledSuConfig.getCProtectedDisposition());
      if( calledSuConfig.getCProtectedDisposition() == CProtectedDisposition.CLEAR_PROTECTED) {
         //12.5.5 c-protected:0
         callParam.setProtectedModeIsSet(true);
      }
      else if( calledSuConfig.getCProtectedDisposition() == CProtectedDisposition.REJECT_UNPROTECTED) {
         //12.13.5 c-protected:0
         callParam.setProtectedModeIsSet(true);
      }

      logger.debug("ccSendCallSetupRequest(): usp.isFullDuplexIsSet="+ userProfile.isFullDuplexIsSet());
      callParam.setFullDuplexRequestedIsSet( userProfile.isFullDuplexIsSet());

      CallSetupPreferenceType csetupPref = userProfile.getCallSetupPreference().getCallSetupPreferenceType();

      //#577 12.8.x.x check U2UCCMgr ringing code
      boolean acRequested = CallSetupPreferenceType.CALLER_PREFERS_AVAILABILITY_CHECK.equals(csetupPref);
      //logger.debug("ccSendCallSetupRequest(): csetuPref.acRequest="+ acRequested);
      callParam.setAvailabilityCheckRequested( acRequested);

      //#577 12.8.x.x adjust based on preference and AC/DC type
      //DONOT apply adjustment: donot want to inherit c-pref setting
      //unitToUnitCallControlManager.adjustCallParamACDC(callParam, callingSuConfig);

      callParam.setRfResourceAvailable(rfssConfig.getRFSS().isRfResourcesAvailable());

      //#577 12.8.x
      logger.debug("ccSendCallSetupRequest(99): csetupPref="+ csetupPref);
      logger.debug("ccSendCallSetupRequest(99): callParam="+ callParam);

      //------------------------------------------------------
      SdpContent sdpContent = SdpContent.createSdpContent(sdpBody);
      smfSession.setSessionDescription(sdpContent.getSessionDescription());
      contentList.add(sdpContent);

      contentList.add(callParamContent);

      ContentTypeHeader contentTypeHeader = contentList.getContentTypeHeader();
      inviteRequest.setHeader(contentTypeHeader);
      inviteRequest.setContent(contentList.toString(), contentTypeHeader);

      UnitToUnitCall unitToUnitCall = unitToUnitCallControlManager
            .setupRequest(inviteRequest, suConfig.getSU(), calledSuConfig);

      unitToUnitCall.setCallParam(callParamContent);
      unitToUnitCall.setEmergency(isEmergency);
      unitToUnitCall.setPttSession(smfSession);
      unitToUnitCall.setPointToMultipointSession(ptpSession);
      
      //logger.debug("ccSendCallSetupRequest: inviteRequets=\n"+inviteRequest);
      return unitToUnitCall;
   }

   //-----------------------------------------------------------------------------------
   public void ccSetupIndicateResponse( CallSetupRequestEvent callSetupRequestEvent, int responseCode)
         throws Exception {
      ServerTransaction st = callSetupRequestEvent.getServerTransaction();
      logger.debug("ccSetupIndicateResponse: responseCode="+responseCode +
                   "  state="+st.getState());

      RequestEvent requestEvent = callSetupRequestEvent.getRequestEvent();
      Request request = requestEvent.getRequest();
      //logger.debug("ccSetupIndicateResponse: event-request=\n"+request);

      if (st.getState() != TransactionState.PROCEEDING) {
         logger.debug("ccSetupIndicateResponse: Too late to send the response... st.state="+st.getState());
         return;
      }

      Response response = unitToUnitCallControlManager.createResponse(responseCode, request);

      ContactHeader contactHeader = SipUtils.createContactHeaderForSU(rfssConfig, suConfig);
      response.setHeader(contactHeader);

      ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
      SipURI toUri = (SipURI) toHeader.getAddress().getURI();
      String radicalname = SipUtils.getRadicalName(toUri);
      SuConfig calledSu = topologyConfig.getSuConfig(radicalname);
      if (calledSu == null) {
         throw new Exception("ccSetupIndicateResponse: Could not find calledSu: "+calledSu);
      }

      UserServiceProfile usp = calledSu.getUserServiceProfile();
      // need to add additional information here.
      // TransmissionControlSAP tcSAP = this.mySubscriberUnit.getTransmissionControlSAP();

      // If responding with a Success responseCode. Then create
      // a new RTP Session for this SU if needed.
      // Modify the response SDP Body to reflect the IP address
      // and port of the new session we just created and
      // send the response back.
      if (responseCode == Response.OK) {

         // #202 12.23.x 200 OK
         SipUtils.addAllowHeaders( response);

         //showln("ccSetupIndicateResponse: responseCode=Response.OK");
         ContentList contentList = ContentList.getContentListFromMessage(request);
         SdpContent sdpContent = (SdpContent) contentList.getContent(ISSIConstants.APPLICATION, ISSIConstants.SDP);
         SessionDescription sdpAnnounce = (SessionDescription) sdpContent.getSessionDescription();

         // The SU is answering with a SIP OK. At this point,
         // we need to allocate a new RTP Session (i.e. a PTT session),
         // from which we extract the IP address and port and send it
         // in the OK.

         String sessionId = SipUtils.getCallIdFromMessage(response);
         PttPointToMultipointSession session = transmissionControlManager
               .getPttPointToMultipointSession(sessionId,SessionType.CALLED_SERVING);
         SmfSession smfSession = null;
         try {
            smfSession = session.createSmfSession(
                  LinkType.UNIT_TO_UNIT_CALLED_SERVING_TO_CALLED_HOME,
                  ((OriginField)sdpAnnounce.getOrigin()).getSessIdAsString());
            smfSession.setRemoteRfssDomainName(calledSu.getHomeRfss().getDomainName());
            smfSession.setSessionDescription(sdpAnnounce);
            smfSession.getSmfReceiver().addListener(rfssConfig.getRFSS().getCurrentTestCase());
         } 
	 catch (RtpException ex) {

            Response errorResponse = SipUtils.createErrorResponse(rfssConfig, 
                  request, WarningCodes.NO_RTP_RESOURCE);
            logger.debug("ccSetupIndicateResponse: NO_RTP_RESOURCE errorResponse=\n"+errorResponse);
            // JAIN-SIP needs a list of ViaHeaders; else message truncated
            // #178 fixup Via Header
            //ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig); 
            //errorResponse.setHeader(viaHeader);
	    //
            st.sendResponse(errorResponse);
            return;
         }
         
         String toHost = SipUtils.getContactHost(callSetupRequestEvent.getServerTransaction().getRequest());
         transmissionControlManager.addMediaLeg(0, smfSession, sdpAnnounce,toHost);
         saps.getTransmissionControlSAP().setPttSession(smfSession);

         CallControlManager.fixupSdpAnnounce(rfssConfig, smfSession, sdpAnnounce);

         CallParamContent callParamContent = contentList.getCallParamContent();
         if (callParamContent != null) {
           CallParam callparam = callParamContent.getCallParam();
           if (callparam != null) {
             callparam.setCallingSuInitialTransmitter(null);

             if (callparam.isAvailabilityCheckRequested()) {
               AvailabilityCheckType checkType = usp.getAvailabilityCheck().getAvailabilityCheckType();
               logger.debug("availcheckrequested: availchektype=" + checkType);
               if (!checkType.equals(AvailabilityCheckType.AVAIL_CHECK_ONLY) &&
                   !checkType.equals(AvailabilityCheckType.AVAIL_CHECK_AND_DIRECT_CALL)) {
                  //logger.debug("setAvailabilityCheckRequested: false");
                  callparam.setAvailabilityCheckRequested(false);
               } else {
                  //logger.debug("setAvailabilityCheckRequested: true");
                  callparam.setAvailabilityCheckRequested(true);
               }
   	     }

             // based on testscript isFullDuplex setting
             callparam.setFullDuplexRequestedIsSet( usp.isFullDuplexIsSet());
             if (callparam.isFullDuplexRequested()) {
               DuplexityType duplexity = usp.getDuplexity().getDuplex();
               if (duplexity == DuplexityType.FULL) {
                  callparam.setFullDuplexRequested(true);
               }
               else if (duplexity == DuplexityType.HALF) {
                  callparam.setFullDuplexRequested(false);
               }
             }
           }
         }

         String origin = ((OriginField) sdpAnnounce.getOrigin()).getSessIdAsString();
         pendingRtpSessions.put(origin, smfSession);
         logger.debug("ccSetupIndicateResponse: pendingRtpSessions.put - origin="+origin);

         //transmissionControlManager.addRtpListener(tcSap);
         response.setContent(contentList.toString(), contentList.getContentTypeHeader());

         Dialog dialog = st.getDialog();
         UnitToUnitCall unitToUnitCall = (UnitToUnitCall) dialog.getApplicationData();
         unitToUnitCall.setPointToMultipointSession(session);
         smfSession.getHeartbeatReceiver().setHeartbeatListener(unitToUnitCall);

         // Fix Allow in 200 OK to BYE
         FromHeader fromHeader = (FromHeader) response.getHeader(FromHeader.NAME);
         SipURI fromUri = (SipURI) fromHeader.getAddress().getURI();
         SuConfig callingSu = topologyConfig.getSuConfig(fromUri.getUser());

         CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
         if( "BYE".equals(cseqHeader.getMethod())) {
            if(callingSu == unitToUnitCall.getCallingSuConfig())
               SipUtils.addAllowHeaders( response);
            if(callingSu == unitToUnitCall.getCalledSuConfig())
               response.removeHeader(ContactHeader.NAME);
         }
      }
      else if (responseCode == Response.RINGING) {

         // #246 need to transfer RecordRoute here
         logger.debug("ccSetupIndicateResponse: RINGING: transfer RecordRoute from request");
         // for 180 Ringing
         ListIterator iter = request.getHeaders(RecordRouteHeader.NAME);
	 if( iter != null) {
            int nroutes = 0;
            while( iter.hasNext()) {
               RecordRouteHeader rrHeader = (RecordRouteHeader) iter.next();
               response.addHeader( rrHeader);
               nroutes++;
            }
	    // 12.22.x but screwup 12.6.1.x
            //if(nroutes > 0) {
            //   toHeader.setTag( SipUtils.createTag());
            //}
         }
         SipUtils.addAllowHeaders(response);
         response.removeHeader(ContactHeader.NAME);
      }
      else if (responseCode == Response.SESSION_PROGRESS) {

         ListIterator iter = request.getHeaders(RecordRouteHeader.NAME);
	 if( iter != null) {
            while( iter.hasNext()) {
               RecordRouteHeader rrHeader = (RecordRouteHeader) iter.next();
               response.addHeader( rrHeader);
            }
         }

         // #398 12.6.x 183 Session Progress
         logger.debug("ccSetupIndicateResponse(8): responseCode="+responseCode);
         logger.debug("ccSetupIndicateResponse(8): isRfResources="+rfssConfig.isRfResourcesAvailable());
         if( !rfssConfig.isRfResourcesAvailable()) {
            WarningHeader warnHdr = SipUtils.createWarningHeader(rfssConfig,
               WarningCodes.SESSION_PROGRESS_NO_RF_RESOURCES);
            response.setHeader(warnHdr);
         }
         SipUtils.addAllowHeaders(response);
         response.removeHeader(ContactHeader.NAME);
      }
      else {
         // can this be 487 Request Terminated ?
         logger.debug("ccSetupIndicateResponse(9): Unsupported responseCode="+responseCode);
      }

      //----------------------------------------------------------
      // 13.1.1 
      SipUtils.checkContentLength( response);

      logger.debug("ccSetupIndicateResponse: "+rfssConfig.getRfssName()+
           " >> CancelSent="+ unitToUnitCallControlManager.getCancelSent());
      logger.debug("ccSetupIndicateResponse: "+rfssConfig.getRfssName()+
           " >> CancelReceived="+ unitToUnitCallControlManager.getCancelReceived());

      // 12.20.1
      if(!unitToUnitCallControlManager.getCancelSent()) {

         // #427 12.22.x Skip Session progress, just send 486 Busy here
         // #375 12.6.1 responseCode=183 Session Progress
         if (responseCode == Response.SESSION_PROGRESS) {
             checkForCallBusy(callSetupRequestEvent, response);
         } 
         else {
            // send 200 OK to CANCEL
            if(!unitToUnitCallControlManager.getCancelReceived()) {
               logger.debug("ccSetupIndicateResponse(7): sendResponse=\n"+response);
               unitToUnitCallControlManager.sendResponse(callSetupRequestEvent, response);
            }
         }
      }
      else {
         // just reset flag
         logger.debug("ccSetupIndicateResponse(7): reset CancelSent...");
         unitToUnitCallControlManager.setCancelSent(false);
      }

      if (logger.isDebugEnabled()) {
         logger.debug("Here are the sessions after response from called rfss: "
               + transmissionControlManager.toString());
      }
   }

   private void checkForCallBusy(CallSetupRequestEvent callSetupRequestEvent, 
      Response response) throws Exception {

      // see TestSU.actuallyHandleSetupIndicate L398
         //Request request = callSetupRequestEvent.getServerTransaction().getRequest();
         //String requestCallId = ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();
         //FromHeader fromHdr = (FromHeader) request.getHeader(FromHeader.NAME);
         //String radicalName = ((SipURI) fromHdr.getAddress().getURI()).getUser();
	 //UnitToUnitCall callSegment = callSetupRequestEvent.getCallSegment();

         //logger.debug("checkForCallBusy(1): radicalName="+radicalName+" callSegment="+callSegment);
         //logger.debug("checkForCallBusy(1): requestCallId="+ requestCallId);
         //logger.debug("checkForCallBusy(1): callID="+ callSegment.getCallID());
	 
         // 12.22.x
         if( !suConfig.isAvailable()) {
            logger.debug("checkForCallBusy(2): BUSY_HERE_CALLED_SU_BUSY");
            ccSetupIndicateErrorResponse(callSetupRequestEvent,WarningCodes.BUSY_HERE_CALLED_SU_BUSY);
         }
	 else {
            logger.debug("checkForCallBusy(3): sendResponse=\n"+response);
            unitToUnitCallControlManager.sendResponse(callSetupRequestEvent, response);
         }
   }

   private void sleep(long msec) {
      try {
         Thread.sleep( msec);
      } catch(Exception ex) { }
   }

   public void ccSetupConfirm(CallControlResponseEvent ccResponseEvent) throws Exception {

      ResponseEvent responseEvent = ccResponseEvent.getResponseEvent();
      Response response = responseEvent.getResponse();
      CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
//logger.debug("ccSetupConfirm(): START...response="+response);

      ClientTransaction ct = responseEvent.getClientTransaction();
      //logger.debug("ccSetupConfirm(): ct="+ct);
      Dialog dialog = ct.getDialog();
      Request ackRequest = dialog.createAck(cseqHeader.getSeqNumber());

//#238 transfer to ContentList c-resavail:1
ContentList xcontentList = ContentList.getContentListFromMessage(response);
if( xcontentList != null) {
   CallParamContent xcallParamContent = xcontentList.getCallParamContent();
   if( xcallParamContent != null) {
      CallParam xcallParam = xcallParamContent.getCallParam();
      if( xcallParam.isRfResourceAvailable()) {
         ContentList contentList = new ContentList();
         CallParamContent callParamContent = CallParamContent.createCallParamContent();
         CallParam callParam = callParamContent.getCallParam();

        // 12.23.x ACK - remove c-resavail:1
        // Bruce changed his mind again
        //if( !xcallParam.isIncallRoaming()) 
        {
         callParam.setRfResourceAvailable( true);
         contentList.add(callParamContent);
      
         ContentTypeHeader contentTypeHeader = contentList.getContentTypeHeader();
         ackRequest.setHeader(contentTypeHeader);
         ackRequest.setContent(contentList.toString(), contentTypeHeader);
        }
      }
   }
}   

      // daisy chain ACK 12.1.1.1 or 13.2.x
      //ackRequest.removeHeader(RouteHeader.NAME);
      SipUtils.filterSipHeaders(ackRequest);
      SipUtils.checkContentLength(ackRequest);

//#187
//12.1.1.1 ACK sip: 00002002000012@p25dr
//12.23.1 This will cause repeated 200 OK  
//SipURI sipUri = (SipURI) ackRequest.getRequestURI();
//logger.debug("ccSetupConfirm: BEFORE sipUri=" +sipUri);
//=== DISABLED sipUri.setHost(ISSIConstants.P25DR);
//logger.debug("ccSetupConfirm: AFTER sipUri=" +sipUri);

      dialog.sendAck(ackRequest);
      logger.debug(">>>>>>>> sendAck(0): ackRequest=\n"+ackRequest);

      //SessionDescription sdes = ContentList.getContentListFromMessage(
      //      response).getSdpContent().getSessionDescription();
      ContentList clist = ContentList.getContentListFromMessage(response);
      if (clist == null)
         throw new Exception("Expecting SDP Content in response: "+response);
      SdpContent sdpContent = clist.getSdpContent();
      SessionDescription sdes = sdpContent.getSessionDescription();
      if (sdes == null) {
         String msg = "No session description in the response - dropping response: "+response;
         logger.error(msg);
         throw new Exception(msg);
      }
      //logger.debug("ccSetupConfirm(): sdes="+sdes);
      // The sender preserves the origin field.

      // got an OK from the invitee.
      // now extract the session that we originally stored away
      // when we sent out the invite. One half of the session
      // has already been set up. The OK Contains the SDP answer.
      // we extract the port from the SDP answer.
      String origin = ((OriginField)sdes.getOrigin()).getSessIdAsString();
      logger.debug("ccSetupConfirm(): extract port from SDP answer - origin="+origin);

      PttSession pendingSession = pendingRtpSessions.get(origin);
      //PttSession pendingSession = (PttSession) dialog.getApplicationData();
      //logger.debug("ccSetupConfirm(): pendingSession="+pendingSession);

      if (pendingSession == null) {
         String msg = "SDP corruption: dropping the response. Could not find pending session: "+origin;
         logger.error(msg);
         throw new Exception(msg);
      }

      String host = SipUtils.getContactHost(response);
      ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
      if (((SipURI) (toHeader.getAddress().getURI())).getUserParam().equals( ISSIConstants.TIA_P25_SG)) {
         String groupId = SipUtils.getGroupID((SipURI) toHeader.getAddress().getURI());
         long groupIdLong = Long.parseLong(groupId, 16);
         transmissionControlManager.addMediaLeg(groupIdLong, pendingSession, sdes,host);
      } else {
         transmissionControlManager.addMediaLeg(0, pendingSession, sdes,host);
      }

      pendingSession.getHeartbeatTransmitter().start();      
      saps.getTransmissionControlSAP().setPttSession(pendingSession);
      //transmissionControlManager.addRtpListener(tcSap);
      
      logger.debug("ccSetupConfirm(): DONE...");
   }

   public void ccTeardownCallSegment(HomeAgentInterface agent, UnitToUnitCall unitToUnitCall,
         String radicalName) throws Exception {
      unitToUnitCallControlManager.teardownRequest(agent, unitToUnitCall, radicalName);
   }
   
   /*
    * (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.CallControlSAP#ccSendCallSetupInCallRoamingRequest(
    *    gov.nist.p25.issi.rfss.UnitToUnitCall, java.lang.String, javax.sip.address.Address)
    */
   public void ccSendCallSetupInCallRoamingRequest(SuConfig roamingSu,
         UnitToUnitCall unitToUnitCall, String oldContact, Address newContact)
         throws Exception {

      // 12.23.1
      //  oldContactString=f2.001.00001.p25dr
      // newContactAddress=<sip:00002002000012@f5.001.00001.p25dr;user=TIA-P25-SU>
      if ( logger.isDebugEnabled()) {
         logger.debug("ccSendCallSetupInCallRoamingRequest:  oldContactString=" + oldContact);
         logger.debug("ccSendCallSetupInCallRoamingRequest: newContactAddress=" + newContact);
         logger.debug("ccSendCallSetupInCallRoamingRequest: roamingSu=\n" + roamingSu);
         logger.debug("ccSendCallSetupInCallRoamingRequest: suConfig=\n" + suConfig);
      }

//12.23.1 ServigRfss must be changed from rfss_2 to rfss_5 !!!
String newHost = ((SipURI)newContact.getURI()).getHost();
RfssConfig newRfssConfig = roamingSu.getTopologyConfig().getRfssConfig(newHost);
logger.debug("ccSendCallSetupInCallRoamingRequest: newHost=" +newHost);
logger.debug("ccSendCallSetupInCallRoamingRequest: newRfssConfig=" +newRfssConfig);

      
      PttPointToMultipointSession ptpSession = unitToUnitCall.getPointToMultipointSession();
      PttSession rtpSession = ptpSession.getPttSession(oldContact);      
      if ( logger.isDebugEnabled()) {
         ptpSession.printPeerTable();
      }
      
      int port = rtpSession.getMyRtpRecvPort();
      long origin = (long) (Math.random() * 100000);
      pendingRtpSessions.put(new Long(origin).toString(), rtpSession);
//logger.debug("ccSendCallSetupInCallRoamingRequest: pendingRtpSessions.put - origin="+origin);

      String ipAddress = suConfig.getHomeRfss().getIpAddress();

      //PTT-BUG
      //-------------------------
      ipAddress = rfssConfig.getIpAddress();
      logger.debug("ccSendCallSetupInCallRoamingRequest(): PTT-BUG after ipAddress="+ipAddress);

      String sdpBody = SipUtils.createSdpBody(origin, 0L, ISSIConstants.P25_U2U_CALL, ipAddress, port);

      // 12.23.1
      //------------------
      logger.debug("ccSendCallSetupInCallRoamingRequest: FIX roamingSu--> newRfssConfig-RFSS="+newRfssConfig.getRFSS());
      roamingSu.setServingRfss( newRfssConfig);
      if(roamingSu.getSU() != null) {
         logger.debug("ccSendCallSetupInCallRoamingRequest: roamingSu-getState="+roamingSu.getSU().getState());
      
         // NPE: roamingSu.TestSU.SAPS-mmSaps is null
         roamingSu.getSU().setState(SuState.ON);
         logger.debug("ccSendCallSetupInCallRoamingRequest: roamingSu-setState=ON");
      }
      //-------------------
      
      Request inviteRequest = unitToUnitCallControlManager.createInviteForInCallRoaming(
         roamingSu,unitToUnitCall);
      SipURI sipUri = (SipURI) rfssConfig.getRFSS().getUnitToUnitMobilityManager().getRegisteredContactURI(
            (SipURI)inviteRequest.getRequestURI());
      //logger.debug("ccSendCallSetupInCallRoamingRequest: requestURI="+inviteRequest.getRequestURI());

      //-------------
      // #291 INVITE request header
      SipURI requestUri = (SipURI) inviteRequest.getRequestURI();
      requestUri.setHost( sipUri.getHost());
      //logger.debug("inviteRequest: AFTER requestUri=" +requestUri);
      //-------------

      logger.debug("ccSendCallSetupInCallRoamingRequest: setRemoteRfssDomainName="+sipUri.getHost());
      rtpSession.setRemoteRfssDomainName(sipUri.getHost());
      
      if (unitToUnitCall.getCallParamContent() == null) {
         SdpContent sdpContent = SdpContent.createSdpContent(sdpBody);
         CallParamContent callParamContent = CallParamContent.createCallParamContent();
         callParamContent.getCallParam().setIncallRoaming(true);

         boolean isCallingSuInitrans = unitToUnitCall.isCallingSuInitrans();
         callParamContent.getCallParam().setCallingSuInitialTransmitter(isCallingSuInitrans);
         ContentList contentList = new ContentList();
         contentList.add(sdpContent);
         contentList.add(callParamContent);
         inviteRequest.setContent(contentList.toString(), contentList.getContentTypeHeader());

      } else {
         ContentList contentList = new ContentList();
         contentList.add(SdpContent.createSdpContent(sdpBody));
         contentList.add(unitToUnitCall.getCallParamContent());
         contentList.getCallParamContent().getCallParam().setIncallRoaming(true);

         // 12.26.x and 12.24.x
         //logger.debug("ccSendCallSetupInCallRoamingRequest(789): set c-pref:0");
         contentList.getCallParamContent().getCallParam().setAvailabilityCheckRequested(false);

         boolean isCallingSuInitrans = unitToUnitCall.isCallingSuInitrans();
         contentList.getCallParamContent().getCallParam().setCallingSuInitialTransmitter(isCallingSuInitrans);
         inviteRequest.setContent(contentList.toString(), contentList.getContentTypeHeader());
      }
      
      // #292 12.23.1 - ContactHeader in INVITE
      ContactHeader chdr = SipUtils.createContactHeaderForRfss(rfssConfig, ISSIConstants.TIA_P25_SU);
      //===inviteRequest.setHeader(chdr);
      logger.debug("ccSendCallSetupInCallRoamingRequest: disabled ContactHeader: chdr="+chdr);  

      //logger.debug("ccSendCallSetupInCallRoamingRequest: setupInCallRomaingRequest()...");
      unitToUnitCallControlManager.setupInCallRoamingRequest(inviteRequest, unitToUnitCall, suConfig.getSU());
   }

   public GroupServing triggerGroupCallSetup(GroupCallSetupScenario groupCallSetupScenario)
      throws Exception {      
      return groupCallControlManager.subscribeToGroup(groupCallSetupScenario);
   }

   public void ccCancelCallSegment(UnitToUnitCall unitToUnitCall) throws Exception {
      ClientTransaction ct = unitToUnitCall.getClientTransaction();
      if (ct.getState() == TransactionState.CONFIRMED ||
          ct.getState() == TransactionState.TERMINATED) {
         throw new Exception("Cannot cancel the request -- too late");
      }
      unitToUnitCallControlManager.sendCancel(ct);
   }
   
   public void ccCancelGroupCall( GroupServing groupServing ) throws Exception {
      ClientTransaction ct = groupServing.getClientTransaction();
      if ( groupServing.getGroupConfig().getHomeRfss() == getRfssConfig()) {
         groupCallControlManager.sendCancel(groupServing);
      } 
      else if (ct.getState() == TransactionState.CONFIRMED ||
               ct.getState() == TransactionState.TERMINATED) {
         throw new Exception ("Cannot cancel the request -- too late");
      } else {
         groupCallControlManager.sendCancel(groupServing);
      }      
   }

   public void ccCancelResponse(ServerTransaction st) throws Exception {

      logger.debug("ccCancelResponse: serverTx.state="+st.getState());
      unitToUnitCallControlManager.setCancelReceived(true);

      if (st.getState() == TransactionState.PROCEEDING) {
         logger.debug("ccCancelResponse: PROCEEDING, send 200 OK ...");
         unitToUnitCallControlManager.sendResponse(st, Response.OK);

      //#562 12.20.x Dup 200 OK
      } if (st.getState() == TransactionState.COMPLETED || 
            st.getState() == TransactionState.CONFIRMED ||
            st.getState() == TransactionState.TERMINATED) {
         logger.debug("ccCancelResponse: COMP||TERM, SKIP 200 OK ...");
         //+++unitToUnitCallControlManager.sendResponse(st, Response.OK);

      } else {
         throw new SipException("ccCancelResponse: Too late to cancel the request: st.state="+st.getState());
      }
   }

   public void ccSetupIndicateErrorResponse( CallSetupRequestEvent callSetupRequestEvent,
         WarningCodes warningCode) throws Exception {

      int rc = warningCode.getRc();
      logger.debug("ccSetupIndicateErrorResponse: warningCode.Rc="+rc);

      ServerTransaction st = callSetupRequestEvent.getServerTransaction();
      if(st == null || st.getState()==TransactionState.TERMINATED) {
         logger.debug("ccSetupIndicateErrorResponse: severTx terminated...return");
	 return;
      }

      Response response = SipUtils.createErrorResponse(rfssConfig, st.getRequest(), warningCode);
      logger.debug("ccSetupIndicateErrorResponse: st.getRequest=\n"+st.getRequest());

      // 12.22.x
      //if( rc != Response.BUSY_HERE) {
      {
         //logger.debug("ccSetupIndicateErrorResponse: rc != BUSY_HERE");
         // 12.26.1 Need ToHeader tag
         ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
         toHeader.setTag( SipUtils.createTag());
      }

      //===================
      // send response
      //unitToUnitCallControlManager.sendResponse(callSetupRequestEvent,response);
      //logger.debug("ccSetupIndicateErrorResponse: st.state="+st.getState()+" response=\n"+response);
      //===================

      //logger.debug("ccSetupIndicateErrorResponse: busyHereSent="+unitToUnitCallControlManager.getBusyHereSent());

      if( rc == Response.BUSY_HERE) {
         // 12.22.x multiple [487 Busy here, ACK] messages 
         if( !unitToUnitCallControlManager.getBusyHereSent()) {
            unitToUnitCallControlManager.setBusyHereSent(true);
            logger.debug("ccSetupIndicateErrorResponse: rc == BUSY_HERE");
            unitToUnitCallControlManager.sendResponse(callSetupRequestEvent,response);
         } else {
            logger.debug("ccSetupIndicateErrorResponse: SKIP sending BUSY_HERE...");
         }
      }
      else if( rc == Response.SESSION_PROGRESS) {
         ccSetupIndicateResponse(callSetupRequestEvent, rc);
      }
      else {
         unitToUnitCallControlManager.sendResponse(callSetupRequestEvent,response);
         logger.debug("ccSetupIndicateErrorResponse: st.state="+st.getState()+" response=\n"+response);
      }
      /***/
   }
}
