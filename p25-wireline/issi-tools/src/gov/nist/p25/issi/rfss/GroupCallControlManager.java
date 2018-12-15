//
package gov.nist.p25.issi.rfss;

import gov.nist.javax.sdp.fields.OriginField;
import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.CProtectedDisposition;
import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.CallParamContent;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25body.SdpContent;
import gov.nist.p25.issi.p25body.params.CallParam;
import gov.nist.p25.issi.p25body.serviceprofile.group.ConfirmedCallSetupTime;
import gov.nist.p25.issi.rfss.SipUtils;
import gov.nist.p25.issi.rfss.tester.GroupCallSetupScenario;
import gov.nist.p25.issi.rfss.tester.TestSU;

import gov.nist.p25.issi.transctlmgr.PttPointToMultipointSession;
import gov.nist.p25.issi.transctlmgr.ptt.LinkType;
import gov.nist.p25.issi.transctlmgr.ptt.MmfSession;
import gov.nist.p25.issi.transctlmgr.ptt.PttManager;
import gov.nist.p25.issi.transctlmgr.ptt.PttSession;
import gov.nist.p25.issi.transctlmgr.ptt.PttSessionInterface;
import gov.nist.p25.issi.transctlmgr.ptt.PttSessionMultiplexer;
import gov.nist.p25.issi.transctlmgr.ptt.SmfSession;
import gov.nist.p25.issi.utils.ProtocolObjects;
import gov.nist.p25.issi.utils.WarningCodes;
import gov.nist.rtp.RtpException;
import gov.nist.rtp.RtpSession;

import java.text.ParseException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
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
import javax.sip.header.MimeVersionHeader;
import javax.sip.header.PriorityHeader;
import javax.sip.header.RetryAfterHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

/**
 * The group call control manager. This performs the Group Call Control function
 * for the emulator. The GCCF of the serving RFSS is responsible for the
 * following functions:
 * <ul>
 * <li> - Requesting a group call set up when an SU triggers a PTT request in
 * its area for a group for which no call currently exists
 * <li> - Participating in the establishment of new call segments in response to
 * requests from the home RFSS
 * <li> - Releasing a group call segment with the Home RFSS on loss of RTP
 * connectivity or other local policy condition
 * <li> - Once a group call segment has been set up, tearing down or setting up
 * RTP sessions upon preemption or Home RFSS request.
 * <li> - Communicating to the home RFSS the RTP session parameters
 * <li> - Notifying the home RFSS of the availability or lack of availability of
 * RF resources required for a confirmed call
 * <li> - Requesting a change of group call priority based on local policy
 * </ul>
 * 
 * The GCCF of the Home RFSS is responsible for the following functions:
 * 
 * <ul>
 * <li> - Establishing group call segments in response to group call setup
 * requests from serving RFSSs or SU PTT triggers within its own area,
 * including:
 * <li> - to get the list of registered serving RFSSs from the GMMF
 * <li> - to initiate group call set ups with the registered serving RFSSs
 * <li> - to bridge RTP sessions which are set up with the serving RFSSs
 * <li> - On-going management of group call segments during a call, including
 * <li> - To initiate group call segment set ups with any registered serving
 * RFSSs not already joined to the call (late call entry)
 * <li> - To handle a group call segment set up when it receives a group call
 * set up request from any serving RFSS
 * <li> - Once group call segment has been set up, tearing down or setting up
 * RTP sessions upon preemption or serving RFSS request.
 * <li> - Releasing the group call on ISSI hangtime expiry or other local policy
 * condition
 * <li> - Releasing a group call segment with a serving RFSS on loss of RTP
 * connectivity or other local policy condition
 * <li> - Communicating to each serving RFSS the RTP session parameters
 * associated with its call segment
 * <li> - Managing the establishment and maintenance of confirmed group calls
 * <li> - Changing the group call priority upon serving RFSS request or upon its
 * own local policy.
 * </ul>
 * 
 */
@SuppressWarnings("unused")
public class GroupCallControlManager implements SipListener {
   
   private static Logger logger = Logger.getLogger(GroupCallControlManager.class);
   public static void showln(String s) { System.out.println(s); }

   private Hashtable<String, GroupInviteServingDelayTask> pendingTimers = 
         new Hashtable<String, GroupInviteServingDelayTask>();

   /*
    * Provider from where we will allocate transactions and get requests (this
    * is our event source).
    */
   private SipProvider provider;

   /*
    * Configuration for our RFSS
    */
   private RfssConfig rfssConfig;

   /*
    * System topology configuration.
    */
   private TopologyConfig topologyConfig;

   /*
    * Factories for the SIP stack.
    */
   private static AddressFactory addressFactory = ProtocolObjects.addressFactory;
   private static HeaderFactory headerFactory = ProtocolObjects.headerFactory;
   private static MessageFactory messageFactory = ProtocolObjects.messageFactory;

   /*
    * The Currently active group calls we are fielding.
    */
   private ConcurrentHashMap<String, GroupServing> activeGroupCalls;

   /*
    * Sessions that are pending a response (this is a temporary holding place)
    */
   private RFSS rfss;

   enum Operation { REINVITE, CALL_SETUP };
   
   class PendingResponse {
      Operation procedure;
      GroupServing callSegment;
      GroupCallSetupScenario scenario;
   }

   /**
    * The task that simulates a delay in invite processing so we can generate
    * call collisions.
    */
   class GroupInviteServingDelayTask extends TimerTask {
      private ServerTransaction serverTransaction;
      private RequestEvent requestEvent;

      public GroupInviteServingDelayTask(RequestEvent requestEvent,
            ServerTransaction serverTransaction) {
         this.serverTransaction = serverTransaction;
         this.requestEvent = requestEvent;
      }

      @Override
      public void run() {
         logger.debug("GroupInviteServingDelayTask: Done delaying INVITE -- actually processing it!");
         try {
            pendingTimers.remove(serverTransaction.getBranchId());
            if (serverTransaction.getState() != TransactionState.TERMINATED) {
               logger.debug("GroupInviteServingDelayTask: actuallyProcessRequest...");
               GroupCallControlManager.this.actuallyProcessRequest(
                     requestEvent, serverTransaction);
            }
         } catch (Exception ex) {
            rfss.getTestHarness().fail("Unexpected exception", ex);
         }
      }
      public ServerTransaction getServerTransaction() {
         return serverTransaction;
      }
   }

   class GroupInviteHomeDelayTask extends TimerTask {
      private RequestEvent requestEvent;
      private GroupHome groupHome;
      private ServerTransaction st;
      
      public GroupHome getGroupHome() { return groupHome; }

      public GroupInviteHomeDelayTask(RequestEvent requestEvent,
            GroupHome groupHome, ServerTransaction st) {
         this.requestEvent = requestEvent;
         this.groupHome = groupHome;
         this.st = st;
      }

      @Override
      public void run() {
         logger.debug("GroupInviteHomeDelayTask: Done delaying INVITE -- actually processing it!");
         Request request = requestEvent.getRequest();
         ToHeader toHeader = ((ToHeader) request.getHeader(ToHeader.NAME));
         SipURI toUri = ((SipURI) toHeader.getAddress().getURI());
         String groupId = SipUtils.getRadicalName(toUri);

         PriorityHeader priorityHeader = (PriorityHeader) request.getHeader(PriorityHeader.NAME);
         int priority = SipUtils.getPriorityValue(priorityHeader);
         boolean emergency = SipUtils.isEmergency(priorityHeader);

         GroupConfig groupConfig = topologyConfig.getGroupConfig(groupId);
         GroupMobilityManager gmm = getGroupMobilityManager();
         GroupHome groupHome = gmm.getGroup(groupConfig.getRadicalName());

         if (Request.INVITE.equals(request.getMethod())
               && (!groupHome.getCallId().equals( ((CallIdHeader) request
                           .getHeader(CallIdHeader.NAME)).getCallId()))
               && isCurrentCallHigherPriority(emergency, priority,
                     groupHome.isEmergency(), groupHome.getPriority())) {
            logger.debug("GroupInviteHomeDelayTask: handleCollidingRequest...");
            // There is a higher priority call queued.
            handleCollidingRequest(requestEvent, st);
            return;
         } else {
            logger.debug("GroupInviteHomeDelayTask: actuallyProcessRequest...");
            actuallyProcessRequest(requestEvent, st);
         }
         logger.debug("GroupInviteHomeDelayTask: decrementPendingRequestCount...");
         groupHome.decrementPendingRequestCount();
      }
   }

   private GroupMobilityManager getGroupMobilityManager() {
      return rfss.getGroupMobilityManager();
   }

   // constructor
   public GroupCallControlManager(SipProvider provider, RfssConfig rfssConfig) {
      this.provider = provider;
      this.rfssConfig = rfssConfig;
      topologyConfig = rfssConfig.getSysConfig().getTopologyConfig();
      rfss = rfssConfig.getRFSS();
      activeGroupCalls = new ConcurrentHashMap<String, GroupServing>();
   }

   /**
    * Return true if the current call is at a higher priority than the new
    * call.
    * 
    * @param emergency
    * @param priority
    * @param currentCallIsEmergency
    * @param currentCallPriority
    * @return
    */
   private boolean isCurrentCallHigherPriority(boolean emergency,
         int priority, boolean currentCallIsEmergency, int currentCallPriority) {
      logger.debug("isCurrentCallHigherPriority: emergency=" + emergency +
            " priority=" + priority + 
	    " currentCallIsEmergency=" + currentCallIsEmergency + 
	    " currentCallPriority=" + currentCallPriority);

      if (emergency == currentCallIsEmergency) {
         return currentCallPriority >= priority;
      } else if (currentCallIsEmergency) {
         return true;
      } else {
         return false;
      }
   }

   /**
    * Handle reINVITE at the group home. Essentially the same as processing an
    * invite except re-use the previously allocated MmfSession.
    * 
    * @param requestEvent -- SIP Request Event.
    * @param groupHome -- group home record for the group.
    * @throws Exception if an exception occured during processing.
    */
   private void handleReInviteAtGroupHome(RequestEvent requestEvent, GroupHome groupHome)
         throws Exception {
      logger.debug("Processing a re-invite for the group " + groupHome);
      Request request = requestEvent.getRequest();

      ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
      String senderDomain = viaHeader.getHost();
      ServerTransaction st = requestEvent.getServerTransaction();
      groupHome.setServerTransaction(st);

      ClientTransaction ct = groupHome.getClientTransaction(senderDomain);
      if (logger.isDebugEnabled()) {
         logger.debug("retrieved client transaction " + ct);
         if (ct != null) {
            logger.debug("ct state = " + ct.getState());
         }
      }

      if (ct != null &&
         (ct.getState() == TransactionState.PROCEEDING || 
          ct.getState() == TransactionState.CALLING)) {
         Response response = messageFactory.createResponse(
               Response.REQUEST_PENDING, st.getRequest());
         logger.debug("handleReInvite: createResponse=\n" + response);
         st.sendResponse(response);
         return;
      }

      PttSession pttSession = groupHome.getMmfSession(senderDomain);
      int mediaPort = SipUtils.getMediaPortFromMessage(request);
      int originalMediaPort = pttSession.getRtpSession().getRemoteRtpRecvPort();
      pttSession.setRemoteRtpRecvPort(mediaPort);

      // If he sends me a reinvite with a 0 port, I should also shutdown my port.
      logger.debug("handleReInvite: mediaPort=" +mediaPort +" originalMediaPort=" +originalMediaPort);
      if (mediaPort == 0) {
         pttSession.stopReceiver();
      }

      //--------------------------------------------------------------------------------
      Response response = messageFactory.createResponse(Response.OK, st.getRequest());

      SessionDescription sessionDescription = SipUtils.getSessionDescriptionFromMessage(request);
      SessionDescription newSessionDescription = SdpFactory.getInstance()
            .createSessionDescription(sessionDescription.toString());
      CallControlManager.fixupSdpAnnounce(rfssConfig, pttSession, newSessionDescription);

      ContactHeader contactHeader = 
         SipUtils.createContactHeaderForGroup(groupHome.getGroupConfig(), rfssConfig);
      response.setHeader(contactHeader);

      //REFACTOR response(200 OK) - may need isConfirmedGroupCall() check ?
      buildGroupCallContentList(st.getRequest(), response, rfss, newSessionDescription);

/*** replaced
      // need to pass along the contentlist
      ContentList contentList = ContentList.getContentListFromMessage(st.getRequest());
      CallParamContent callParamContent = contentList.getCallParamContent();
      if(callParamContent == null) {
         response.setContent(newSessionDescription.toString(), SipUtils.createAppSdpContentTypeHeader());
      }
      else {
         ContentList clist = new ContentList();
         SdpContent sdpContent = SdpContent.createSdpContent();
         sdpContent.setSessionDescription(newSessionDescription);
         clist.add(sdpContent);

         CallParam callParam = callParamContent.getCallParam();
         if(callParam.isConfirmedGroupCall()) {
            callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
         }
         // support explicit c-groupcalltype: 0
         callParam.setConfirmedGroupCallIsSet(true);

         //logger.debug("handleReInviteAtGroupHome(1): callParam="+callParam);
         clist.add(callParamContent);
         response.setContent(clist.toString(), clist.getContentTypeHeader());
      }
 *****/
      SipUtils.addAllowHeaders(response);            
      SipUtils.checkContentLength(response);            

      st.sendResponse(response);
      logger.debug("handleReInviteAtGroupHome(1): response=\n"+response);
      
      // If this is a media port reassignment - just return.
      if (mediaPort != originalMediaPort) {
         logger.debug("media port reassignment -- returning ");
         return;
      }
      ContactHeader senderContactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
      SipURI senderContactUri = (SipURI) senderContactHeader.getAddress().getURI();

      // extract the sender domain name.
      String senderDomainName = SipUtils.getDomainName(senderContactUri);
      GroupConfig groupConfig = groupHome.getGroupConfig();

      for (Iterator<RfssConfig> it = groupHome.getSubscribers().iterator(); it.hasNext();) {
         RfssConfig xrfssConfig = it.next();
         String domainName = xrfssConfig.getDomainName();

         // Forward the INVITE to all domains names excepting the sender.
         if (!domainName.equals(senderDomainName) &&
             !domainName.equals(rfssConfig.getDomainName())) {

            logger.debug("handleReInviteAtGroupHome: forwarding invite to " +domainName);
            MmfSession mmfSession = groupHome.getMmfSession(domainName);
            sessionDescription = mmfSession.getSessionDescription();

            Dialog dialog = groupHome.getDialog(mmfSession);
            Request newRequest = dialog.createRequest(Request.INVITE);
            
            ViaHeader myViaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
            newRequest.setHeader(SipUtils.createContactHeaderForGroup(groupConfig, rfssConfig));
            newRequest.setHeader(myViaHeader);
            newRequest.setHeader(SipUtils.createTimeStampHeader());

            SipURI sipUri = addressFactory.createSipURI(groupConfig.getRadicalName(), domainName);
            sipUri.setUserParam(ISSIConstants.TIA_P25_SG);
            newRequest.removeHeader(RouteHeader.NAME);

            // RouteHeader routeHeader = SipUtils.createRouteToRfss(xrfssConfig);
            // SipURI sipUri = (SipURI)routeHeader.getAddress().getURI();
            // sipUri.setUser(ISSIConstants.P25_GROUP_CALL);
            // newRequest.setHeader(routeHeader);
            newRequest.setRequestURI(sipUri);
            newRequest.setHeader(request.getHeader(PriorityHeader.NAME));

            newSessionDescription = SdpFactory.getInstance()
                  .createSessionDescription(sessionDescription.toString());

            // Get the old mmf and use the
            CallControlManager.fixupSdpAnnounce(rfssConfig, mmfSession, newSessionDescription);

            //REFACTOR using new interface
            CallParamContent xcallParamContent = groupHome.getCallParamContent();
            buildGroupCallContentList(xcallParamContent, newRequest, rfss, newSessionDescription);

/***** replaced
            if (xcallParamContent == null) {
               newRequest.setContent(newSessionDescription.toString(), 
                  SipUtils.createAppSdpContentTypeHeader());
            } 
	    else {
               ContentList clist = new ContentList();
               SdpContent sdpContent = SdpContent.createSdpContent();
               sdpContent.setSessionDescription(newSessionDescription);
               clist.add(sdpContent);

               CallParam callParam = xcallParamContent.getCallParam();
               //#610 20.5.x explicit c-groupcalltype: 0
               callParam.setConfirmedGroupCallIsSet(true);

               //BUG: use xcallParamContent insteadof callParamContent
               clist.add(xcallParamContent);
               newRequest.setContent(clist.toString(), clist.getContentTypeHeader());
            }
 ***/
	    SipUtils.checkContentLength( newRequest);

            ClientTransaction clientTransaction = provider.getNewClientTransaction(newRequest);
            groupHome.setClientTransaction(domainName, clientTransaction);
            groupHome.addPttSession(domainName, mmfSession);

            // Store away the MMF session in the application data location.
            clientTransaction.setApplicationData(mmfSession);
            dialog.sendRequest(clientTransaction);
         }
      }  // for-loop
   }

   /**
    * Handle re-invitation at the group serving.
    * 
    * @param requestEvent
    * @param groupServing
    * @throws Exception
    */
   private void handleReInviteAtGroupServing(RequestEvent requestEvent,
         GroupServing groupServing) throws Exception {
      logger.debug("handleReInviteAtGroupServing for group "
            + groupServing.getGroupConfig().getGroupId());
      Request request = requestEvent.getRequest();

      GroupConfig groupConfig = groupServing.getGroupConfig();
      ServerTransaction st = requestEvent.getServerTransaction();

      ClientTransaction ct = groupServing.getClientTransaction();
      logger.debug("KKK handleReInviteAtGroupServing: ct="+ct);
      if (ct != null &&
         (ct.getState() == TransactionState.CALLING ||
          ct.getState() == TransactionState.PROCEEDING)) {

         Response response = messageFactory.createResponse( Response.REQUEST_PENDING, st.getRequest());
         RetryAfterHeader retryAfter = headerFactory.createRetryAfterHeader((int)(Math.random()*100));
         response.addHeader(retryAfter);
         st.sendResponse(response);
         return;
      } else {
         if (ct != null)
            logger.debug("ct state at group serving is " + ct.getState());
      }

      ContentList clist = ContentList.getContentListFromMessage(request);
      if (clist == null) {
         throw new Exception("Expecting SDP Content in request="+request);
      }

      SdpContent sdpContent = clist.getSdpContent();
      SessionDescription sdes = sdpContent.getSessionDescription();
      MediaDescription mdes = (MediaDescription) sdes.getMediaDescriptions(false).get(0);
      int remotePort = mdes.getMedia().getMediaPort();
      if (remotePort == 0) {
         groupServing.getMultiplexer().getRtpSession().stopRtpPacketReceiver();
      }
      groupServing.getMultiplexer().setRemoteRtpRecvPort(remotePort);
      sendResponse(requestEvent, groupConfig, st, Response.OK);
   }

   /**
    * Handle a colliding request at the Home RFSS.
    * 
    * @param requestEvent -- sip request event
    * @param st -- server transaction
    */
   private void handleCollidingRequest(RequestEvent requestEvent, ServerTransaction st) 
   {
      Request request = requestEvent.getRequest();
      //logger.debug("handleCollidingRequest(0): request=\n"+request);
      handleCollidingRequest(request, st);
   }

   private void handleCollidingRequest(Request request, ServerTransaction st)
   {
      logger.debug("handleCollidingRequest(1): request=\n"+request);
      Response response = SipUtils.createErrorResponse(rfssConfig,
            request, WarningCodes.DECLINE_COLLIDING_CALL);
      // add new tag
      ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
      try {
         toHeader.setTag( SipUtils.createTag());
      } catch(Exception ex) {
         logger.debug("handleCollidingRequest(2): createTag: "+ex);
      }

      if (st == null) {
         try {
            st = provider.getNewServerTransaction(request);
         } catch (TransactionAlreadyExistsException ex) {
            // Already seen this request so ignore the retransmission
            logger.debug("Ignoring retransmission of request "+ex);
            return;
         } catch (TransactionUnavailableException e) {
            rfss.logError("unexpected exception", e);
            return;
         }
      }
      try {
         st.sendResponse(response);
      } catch (Exception ex) {
         rfss.getTestHarness().fail("Unexpected exception", ex);
      }
   }

   private void actuallyProcessRequest(RequestEvent requestEvent,
         ServerTransaction st) {

      String method = requestEvent.getRequest().getMethod();
      //logger.debug("GroupCallControlManager: actuallyProcessXXXX(): "+method);
      if (Request.INVITE.equals(method)) {
         processInvite(requestEvent, st);
      } 
      else if (Request.ACK.equals(method)) {
         processAck(requestEvent);
      } 
      else if (Request.BYE.equals(method)) {
         processBye(requestEvent);
      } 
      else if (Request.CANCEL.equals(method)) {
         processCancel(requestEvent, st);
      }
      else {
         logger.debug("GroupCallControlManager: unhandled "+method);         
      }
   }

   /**
    * Process incoming BYE event.
    * 
    * @param requestEvent -- request event to process.
    */
   private void processBye(RequestEvent requestEvent) {
      try {
         ServerTransaction st = requestEvent.getServerTransaction();
         Request request = requestEvent.getRequest();
         ToHeader to = (ToHeader) request.getHeader( ToHeader.NAME);
         String radicalName = SipUtils.getGroupID(to.getAddress().getURI());

         GroupConfig groupConfig = topologyConfig.getGroupConfig(radicalName);
         if (rfssConfig == groupConfig.getHomeRfss()) {
            GroupHome groupHome = getGroupMobilityManager().getGroup( radicalName);
            groupHome.removeDialog(st.getDialog());
         } else {
            logger.debug("Got a bye at group serving - tearing down call");
            GroupServing groupServing = getGroupCall(radicalName);
            groupServing.getMultiplexer().getRtpSession().shutDown();
         }

         Response response = messageFactory.createResponse(Response.OK, request);
         SipUtils.addAllowHeaders(response);            
         SipUtils.checkContentLength(response);
         st.sendResponse(response);

      } catch (Exception ex) {
         logger.fatal("Unexpected exception -- failing test. ", ex);
         rfss.getTestHarness().fail("Unexpected exception", ex);
      }
   }

   /**
    * Process an incoming ACK event.
    * 
    * @param requestEvent -- the event to process.
    */
   private void processAck(RequestEvent requestEvent) {

      logger.debug(">>> processAck(): START...");
      try {
         Request request = requestEvent.getRequest();
         
         ToHeader to = (ToHeader) request.getHeader(ToHeader.NAME);
         SipURI sipUri = (SipURI) to.getAddress().getURI();
         String groupId = SipUtils.getGroupID(sipUri);

         GroupConfig groupConfig = topologyConfig.getGroupConfig(groupId);
         rfss.getTestHarness().assertTrue(groupConfig != null);
         
         if (rfssConfig == groupConfig.getHomeRfss()) {
            GroupHome groupHome = getGroupMobilityManager().getGroup( groupId);
            ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
            String rfssDomainName = viaHeader.getHost();
            PttSession pttSession = groupHome.getPttPointToMultipointSession().getPttSession(rfssDomainName);
            if (pttSession.getRtpSession().getRemoteRtpRecvPort() > 0) {
               pttSession.getHeartbeatTransmitter().start();
            }
         } else {
            GroupServing groupServing = getGroupCall(groupId);
            PttSessionMultiplexer pttSessionMux = groupServing.getMultiplexer();
            if (pttSessionMux.getRtpSession().getRemoteRtpRecvPort() > 0) {
               pttSessionMux.getHeartbeatTransmitter().start();
            } else {
               logger.debug("no remote rtp port available -- not starting heartbeat transmitter");
            }
         }
      } catch (Exception ex) {
         rfss.getTestHarness().fail("Unexpected exception", ex);
      }
   }

   /**
    * Process Cancel for the group call.
    * 
    * @param requestEvent -- the event to process.
    * @param st -- server transaction
    */
   private void processCancel(RequestEvent requestEvent, ServerTransaction st) {
      try {
         Request request = requestEvent.getRequest();

         ToHeader toHeader = ((ToHeader) request.getHeader(ToHeader.NAME));
         SipURI toUri = ((SipURI) toHeader.getAddress().getURI());

         String groupId = SipUtils.getGroupID(toUri);
         GroupConfig groupConfig = topologyConfig.getGroupConfig(groupId);
         GroupMobilityManager gmm = getGroupMobilityManager();
         
         // I am at the home RFSS.
         if (groupConfig.getHomeRfss() == rfssConfig) {

            GroupHome groupHome = gmm.getGroup(groupConfig.getRadicalName());
            ServerTransaction inviteServerTransaction = groupHome.getServerTransaction();

            Response cancelResponse = messageFactory.createResponse( Response.OK, request);
            //cancelResponse.setHeader(SipUtils.createTimeStampHeader());
            st.sendResponse(cancelResponse);
            
            if (inviteServerTransaction.getState() == TransactionState.PROCEEDING) {
               if (groupHome.getDelayTask() != null)
                  groupHome.getDelayTask().cancel();
               Response response = messageFactory.createResponse( Response.REQUEST_TERMINATED,
                     inviteServerTransaction.getRequest());
               inviteServerTransaction.sendResponse(response);
            }
            for (ClientTransaction ct : groupHome.getClientTransactions()) {
               if (ct.getState() == TransactionState.PROCEEDING) {
                  Request cancelRequest = ct.createCancel();

                  SipUtils.filterSipHeaders( cancelRequest);
                  ClientTransaction cancelTx = provider.getNewClientTransaction(cancelRequest);
                  cancelTx.sendRequest();
               }
            }
         } else {
            GroupInviteServingDelayTask delayTask = pendingTimers.get(st.getBranchId());
            if (delayTask == null) {
               st.sendResponse(messageFactory.createResponse(Response.OK, st.getRequest()));
               return;
            } else {
               // #697 9.7.x send Cancel OK before Request Terminated
               st.sendResponse(messageFactory.createResponse(Response.OK, st.getRequest()));

               ServerTransaction inviteTx = delayTask.getServerTransaction();
               if (inviteTx.getState() != TransactionState.TERMINATED) {
                  delayTask.cancel();
                  pendingTimers.remove(st.getBranchId());
                  inviteTx.sendResponse(messageFactory.createResponse(
                        Response.REQUEST_TERMINATED, inviteTx.getRequest()));
               }
            }
         }
      } catch (Exception ex) {
         rfss.getTestHarness().fail("Unexpected exception", ex);
      }
   }

   /**
    * Processes an Invite Request.
    * 
    * @param requestEvent -- the request event.
    * @param st -- the allocated SIP server transaction.
    */
   private void processInvite(RequestEvent requestEvent, ServerTransaction st) {
      try {
         Request request = requestEvent.getRequest();
         //logger.debug("processInvite(1): request=\n"+request);

         ToHeader toHeader = ((ToHeader) request.getHeader(ToHeader.NAME));
         SipURI toUri = ((SipURI) toHeader.getAddress().getURI());

         String groupId = SipUtils.getGroupID(toUri);
         long gid = Long.parseLong(groupId, 16);
         GroupConfig groupConfig = topologyConfig.getGroupConfig(groupId);

         // Get the sender.
         ContactHeader sender = (ContactHeader) request.getHeader(ContactHeader.NAME);
         if (sender == null) {
            Response response = messageFactory.createResponse( Response.BAD_REQUEST, request);
            st.sendResponse(response);
            return;
         }
         ViaHeader via = (ViaHeader) request.getHeader(ViaHeader.NAME);
         String callingRfssRadicalName = via.getHost();
         RfssConfig callingRfss = topologyConfig.getRfssConfig(callingRfssRadicalName);
         GroupMobilityManager gmm = getGroupMobilityManager();
         GroupHome groupHome = gmm.getGroup(groupConfig.getRadicalName());

         ContentList contentList = ContentList.getContentListFromMessage(request);
         CallParamContent callParamContent = contentList.getCallParamContent();
      
//logger.debug("processInvite(1): groupId="+groupId+" callingRfssRadicalName="+callingRfssRadicalName);
         if (rfssConfig.isAssignedGroup(groupId)) {

logger.debug("processInvite(21): isAssignedGroup(1)...st.getRequest()=\n"+st.getRequest());

            ClientTransaction ct = groupHome.getClientTransaction(callingRfssRadicalName);
            if( ct != null) {
            //if(groupHome.getClientTransaction(callingRfssRadicalName) != null) {
               //ClientTransaction ct = groupHome.getClientTransaction(callingRfssRadicalName);
//logger.debug("processInvite(21): ct.getState(): "+ct.getState());
//logger.debug("processInvite(21): isAssignedGroup(2)...ct.getRequest()=\n"+ct.getRequest());

               if (ct.getState() == TransactionState.CALLING ||
                   ct.getState() == TransactionState.PROCEEDING) {
                  /*****/ 
                  //#685 603 Decline - colliding calls
                  if(Request.INVITE.equals(st.getRequest().getMethod()) && 
                     Request.INVITE.equals(ct.getRequest().getMethod()) ) {
                  
                     PriorityHeader priorityHeader = 
                        (PriorityHeader) ct.getRequest().getHeader(PriorityHeader.NAME);
                     boolean emergency = SipUtils.isEmergency(priorityHeader);
                     int priority = SipUtils.getPriorityValue(priorityHeader);
                  
                     PriorityHeader currentPriorityHeader = 
                        (PriorityHeader) st.getRequest().getHeader(PriorityHeader.NAME);
                     boolean currentEmergency = SipUtils.isEmergency(currentPriorityHeader);
                     int currentPriority = SipUtils.getPriorityValue(currentPriorityHeader);
                  
                     if((emergency == currentEmergency) && (priority >= currentPriority)) {
                        logger.debug("#685 right direction- 603 Decline ");
                        handleCollidingRequest(requestEvent, st);
                     }
                     else if(isCurrentCallHigherPriority(emergency,priority,
                           currentEmergency,currentPriority)) {
                        //logger.debug("#685 right direction- 603 Decline ");
                        handleCollidingRequest(requestEvent, st);
                     }
                     else {
                        //logger.debug("#685 right direction- send 200 OK ???");
                        activeGroupCalls.put(groupId, new GroupServing( rfss,
                           requestEvent.getDialog(), st, groupConfig, provider));
                        sendResponse(requestEvent, groupConfig, st, Response.OK);
                     }
                     return;
                  }
                  /***/

                  Response response = messageFactory.createResponse(
                     Response.REQUEST_PENDING, st.getRequest());
                  logger.debug("processInvite(21): REQUEST_PENDING:\n"+response);
                  st.sendResponse(response);
                  return;
               }   // ct.getState == CALLING || PROCEEDING
            }

            // I got an INVITE from an RFSS which is not a group member.
            // Reject the INVITE.
            if(!groupHome.isSubscribed(callingRfss)) {
               Response errorResponse = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_SG_NOT_REGISTERED);
               logger.debug("processInvite(22): FORBIDDEN:\n"+errorResponse);
               st.sendResponse(errorResponse);
               return;
            }

            // I am configured to reject protected calls for the group.
            if(callParamContent != null &&
               CProtectedDisposition.REJECT_PROTECTED==groupConfig.getCProtectedDisposition())
	    {
               CallParam callParam = callParamContent.getCallParam();
               if ( callParam.isProtectedMode()) {
                  Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_PROTECTED_MODE_AUDIO_CANNOT_BE_GRANTED);
                  logger.debug("processInvite(23): FORBIDDEN_PROTECTED:\n"+response);
                  st.sendResponse(response);
                  return;   
               }               
            }
            
            // I am configured to reject unprotected calls for the group.
            if(callParamContent != null &&
               CProtectedDisposition.REJECT_UNPROTECTED==groupConfig.getCProtectedDisposition())
            {
               CallParam callParam = callParamContent.getCallParam();
               if (! callParam.isProtectedMode()) {
                  Response response = SipUtils.createErrorResponse( rfssConfig, request,
                        WarningCodes.FORBIDDEN_UNPROTECTED_MODE_AUDIO_CANNOT_BE_GRANTED);
                  logger.debug("processInvite(24): FORBIDDEN_UNPROTECTED:\n"+response);
                  st.sendResponse(response);
                  return;   
               }
            }

            // The Home group abstraction keeps a hash table of SIP Dialogs
            // indexed by radical name of each of the memebers. It also
            // keeps a hash table of all PTT MMF sessions hashed by the same
            // key. If we see the Dialog already in the set of dialogs that
            // are tracked by the group home, then this is a re-Invite.
            //
            if (groupHome.getDialogs().contains(st.getDialog())) {
               logger.debug("processInvite(25): reinvite");
               handleReInviteAtGroupHome(requestEvent, groupHome);
               return;
            }

            // this is a group for which we are the home rfss. broadcast the
            // request to all serving rfss's
            Response response = messageFactory.createResponse(Response.OK, request);

            ContactHeader contactHeader = SipUtils.createContactHeaderForGroup(groupConfig, rfssConfig);
            response.setHeader(contactHeader);
            SipUtils.addToHeaderTag(response);

            groupHome.addDialog(callingRfss.getDomainName(), st.getDialog());

            // rewrite
            PriorityHeader priorityHeader = (PriorityHeader) request.getHeader(PriorityHeader.NAME);
            if ( priorityHeader != null) {
               String callId = ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();
               groupHome.setCallId(callId);
               groupHome.setPriority( SipUtils.getPriorityValue( priorityHeader));
               groupHome.setEmergency( SipUtils.isEmergency( priorityHeader));
            }
            st.getDialog().setApplicationData(groupHome);
            PttPointToMultipointSession pttSession = groupHome.getPttPointToMultipointSession();

            ContactHeader senderContactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
            SipURI senderContactUri = (SipURI) senderContactHeader.getAddress().getURI();

            // extract the sender domain name.
            String senderDomainName = SipUtils.getDomainName(senderContactUri);
            int senderRfssId = SipUtils.getRfssId(senderDomainName);
            if (logger.isDebugEnabled()) {
               logger.debug("senderDomainName=" +senderDomainName +" senderRfssId=" +senderRfssId);
            }

            SdpContent sdpContent = (SdpContent)
               ContentList.getContentByType(contentList,ISSIConstants.APPLICATION,ISSIConstants.SDP);

            SessionDescription sessionDescription = sdpContent.getSessionDescription();
            String sessionId = ((OriginField) sessionDescription.getOrigin()).getSessIdAsString();

            MmfSession mmfSession = null;
            try {
               mmfSession = pttSession.createMmfSession( LinkType.GROUP_HOME, sessionId);
               boolean muteFlag = rfssConfig.isNotInterestedInLosingAudio();
               mmfSession.setSetIsNotInterestedInLosingAudio(muteFlag);
               mmfSession.setRemoteRfssDomainName(callingRfssRadicalName);

               logger.debug("RTP-DEBUG: myRtpRecvPort= " + mmfSession.getMyRtpRecvPort());
               logger.debug("RTP-DEBUG: sessionDescription= " + sessionDescription);
               logger.debug("RTP-DEBUG: sessionId= " + sessionId);
            }
	    catch (RtpException rtpException) {
               logger.debug("no rtp resources ", rtpException);
               Response errorResponse = SipUtils.createErrorResponse(
                     rfssConfig, request, WarningCodes.NO_RTP_RESOURCE);
               logger.debug("processInvite(25): NO_RTP_RES: "+errorResponse);
               st.sendResponse(errorResponse);
               return;
            }

            String sendingHost = SipUtils.getContactHost(request);
            rfss.getTransmissionControlManager().addMediaLeg(gid, mmfSession, sessionDescription, sendingHost);

            SessionDescription newSessionDescription = SdpFactory.getInstance().createSessionDescription(
                        sessionDescription.toString());

            CallControlManager.fixupSdpAnnounce(rfssConfig, mmfSession, newSessionDescription);

            logger.debug("processInvite(25): newSessDesc="+ newSessionDescription);
            logger.debug("processInvite(25): contentList="+ contentList);

            // 11.1.1 sdp body port 25000 instead of 25002
            //response.setContent(newSessionDescription.toString(), SipUtils.createAppSdpContentTypeHeader());

	    // from the commented codes below
            //+++groupHome.addPttSession(senderDomainName, mmfSession);
/***** 
            //=====================================
            ContentList clist = new ContentList();
            SdpContent sc = SdpContent.createSdpContent();
            sc.setSessionDescription(newSessionDescription);
            clist.add(sc);
          
            // pass along call param from request
            //++CallParamContent callParamContent = contentList.getCallParamContent();
            if( callParamContent != null) {
	       CallParam callParam = callParamContent.getCallParam();
	       //#665 set c-resavail: 0 ??
               if( callParam.isConfirmedGroupCall()) {
                  callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
               }
               //YYY: donot dispaly c-groupcalltype: 0/1
               callParam.setConfirmedGroupCallIsSet(false);

               logger.debug("sendResponse(888): callParam="+callParam);
               clist.add(callParamContent);
	    }

            logger.debug("sendResponse(888): clist="+clist);
            response.setContent( clist.toString(), clist.getContentTypeHeader());
            //=====================================

            response.addHeader( SipUtils.createContentDispositionHeader());
            //>>>groupHome.addPttSession(senderDomainName, mmfSession);

            SipUtils.addAllowHeaders(response);            
            SipUtils.checkContentLength(response);

            logger.debug("processInvite: send 200 OK-response=\n" +response);
            st.sendResponse(response);
  *****/
	    
            //----------------------------
            if (logger.isDebugEnabled()) {
               logger.debug("processInvite(29): senderDomainDame=" + senderDomainName);
            }
            boolean bcflag = false;
            //----------------------------------------------------------------------------------
            for (Iterator<RfssConfig> it=groupHome.getSubscribers().iterator(); it.hasNext();) {
               RfssConfig xrfssConfig = it.next();
               String domainName = xrfssConfig.getDomainName();

               //logger.debug("LOOP(555): domainName=" + domainName +":" +rfssConfig.getDomainName());
               //logger.debug("LOOP(555): hasPttSession=" + groupHome.hasPttSession(xrfssConfig));
               //logger.debug("LOOP(555): isRfResources=" + xrfssConfig.getRFSS().isRfResourcesAvailable());

               // Forward the INVITE to all domains names excepting the sender.
               if (!domainName.equals(senderDomainName)
                     && !domainName.equals(rfssConfig.getDomainName())
                     && !groupHome.hasPttSession(xrfssConfig)) {

                  if (logger.isDebugEnabled()) {
                     logger.debug("LOOP(555): forwarding invite to " + domainName);
                  }

                  Request newRequest = (Request) request.clone();
                  ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
                  newRequest.setHeader(viaHeader);
                  
                  newRequest.setHeader(SipUtils.createContactHeaderForGroup(groupConfig, rfssConfig));
                  newRequest.setHeader(SipUtils.createTimeStampHeader());
                  
                  SipURI sipUri = addressFactory.createSipURI(groupConfig.getRadicalName(), domainName);
                  sipUri.setUserParam(ISSIConstants.TIA_P25_SG);
                  newRequest.setRequestURI(sipUri);

                  newRequest.removeHeader(RouteHeader.NAME);
                  newRequest.setHeader(SipUtils.createContentDispositionHeader());

                  //#608 Add RouteHeader
                  if (groupConfig.getHomeRfss() != rfssConfig) 
                  {
                     RouteHeader routeHeader = SipUtils.createRouteToRfss(groupConfig.getHomeRfss());
                     SipURI xsipUri = (SipURI) routeHeader.getAddress().getURI();
                     xsipUri.setUser(ISSIConstants.P25_GROUP_CALL);
                     newRequest.addHeader(routeHeader);
                     logger.debug("processInvite(608): addRoute="+routeHeader);
                  }
                  else {
                     logger.debug("porcessInvite(608): NO RouteHeader...");
                  }

                  ToHeader newto = (ToHeader) newRequest.getHeader(ToHeader.NAME);
                  newto.removeParameter("tag");
                  SipUtils.addFromHeaderTag( newRequest);
                  
                  MmfSession xmmfSession = null;
                  String newSessionId = new Long((long) (Math.random() * 100000)).toString();
                  logger.debug("forwarding invite to using session Id : " + newSessionId);

                  try {
                     xmmfSession = pttSession.createMmfSession( LinkType.GROUP_HOME, newSessionId);
                     xmmfSession.setRemoteRfssDomainName(domainName);

                  } catch (RtpException rtpException) {
                     // The mmf session could not be created for some
                     // reason. Just discard it. ??
                     rfss.logError("Unexpected RtpException : ", rtpException);
                     continue;
                  }

                  SessionDescription newSd = 
                     SdpFactory.getInstance().createSessionDescription( sessionDescription.toString());

                  CallControlManager.fixupSdpAnnounce(rfssConfig, xmmfSession, newSd);
                  ((OriginField) newSd.getOrigin()).setSessionId(newSessionId);

//TODO: can be REFACTOR ???
                  CallParamContent content = groupHome.getCallParamContent();
                  if (content == null) {                  
                     newRequest.setContent(newSd.toString(), 
                                           SipUtils.createAppSdpContentTypeHeader()); 
                  }
		  else {
                     CallParam callParam = content.getCallParam(); 
                  
                     // Called home overrides the protected mode parameter setting if needed.
		     // default cProtectedDisposition is CProtectedDisposition.FORWARD_PROTECTED
                     if(callParam.isProtectedMode() &&  
                        CProtectedDisposition.CLEAR_PROTECTED == groupConfig.getCProtectedDisposition())
                     {
                        callParam.setProtectedMode(false);
                     } 

                     if(CProtectedDisposition.CLEAR_PROTECTED == groupConfig.getCProtectedDisposition())
                     {
                        // display 
                        callParam.setProtectedModeIsSet(true);
                     } 
                     //-----------------------------------------------
                     if( callParam.isConfirmedGroupCall()) {
                        callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
                     } else {
	                //#665 set c-resavail:0 always on INVITE
                        callParam.setRfResourceAvailable( false);
                     }

                     // explicit c-groupcalltype: 0
                     callParam.setConfirmedGroupCallIsSet( true);

                     //#589 18.1.x donot display c-resavail:0 in 2nd INVITE
                     //#589 20.4.x need to display c-resavail in 1st INVITE
                     contentList = new ContentList();
                     sdpContent = SdpContent.createSdpContent();
                     sdpContent.setSessionDescription(newSd);
                     contentList.add(sdpContent);

                     contentList.add(content);
                     newRequest.setContent(contentList.toString(), contentList.getContentTypeHeader());
                  }

                  SipUtils.checkContentLength(newRequest);
                  //logger.debug("processInvite(608): newRequest=\n"+newRequest);

                  ClientTransaction newct = provider.getNewClientTransaction(newRequest);
                  groupHome.setClientTransaction(domainName, newct);
                  groupHome.addPttSession(domainName, xmmfSession);

                  // Store away the MMF session in the application data.
                  newct.setApplicationData(xmmfSession);
                  groupHome.addDialog(domainName, newct.getDialog());
                  newct.getDialog().setApplicationData(groupHome);
                  newct.sendRequest();
                  logger.debug("processInvite(608): newRequest=\n"+newRequest);
                  bcflag = true;
               }
            }   // for-loop
            //----------------------------------------------------------------------------------
            logger.debug("processInvite(30): BROADCAST-bcflag=" + bcflag);
//EHC: if bcflag == false, then 404 Not found ???
if( !bcflag) {
   logger.debug("processInvite(30): 404 not found Target Group not found ???");
}	    
/*****/
            // 9.1.x or 18.1.x
            //cause problem for 20.2.x
            //>>>>> send 200 OK here ?
            //=====================================
            ContentList clist = new ContentList();
            SdpContent sc = SdpContent.createSdpContent();
            sc.setSessionDescription(newSessionDescription);
            clist.add(sc);
          
            // pass along call param from request
            //++CallParamContent callParamContent = contentList.getCallParamContent();
            if( callParamContent != null) {
	       CallParam callParam = callParamContent.getCallParam();
	       //#665 set c-resavail: 0 ??
               if( callParam.isConfirmedGroupCall()) {
                  callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
               }
               //YYY: donot dispaly c-groupcalltype: 0/1
               callParam.setConfirmedGroupCallIsSet(false);

               logger.debug("sendResponse(888): callParam="+callParam);
               clist.add(callParamContent);
	    }
            logger.debug("sendResponse(888): clist="+clist);
            response.setContent( clist.toString(), clist.getContentTypeHeader());
            //=====================================

            response.addHeader( SipUtils.createContentDispositionHeader());
            groupHome.addPttSession(senderDomainName, mmfSession);

            SipUtils.addAllowHeaders(response);            
            SipUtils.checkContentLength(response);

            logger.debug("processInvite: send 200 OK-response=\n\n"+response);
            st.sendResponse(response);
/***/

         } else {
            
logger.debug("processInvite(1): I am at serving RFSS...group.radicalName="+groupConfig.getRadicalName());
            // I am at the serving RFSS.
            GroupMobilityManager mm = getGroupMobilityManager();
            if (!mm.isKnownGroup(groupConfig.getRadicalName())) {
               logger.debug("processInvite(1): could not find " + groupId);
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.NOT_FOUND_UNKNOWN_TARGET_GROUP);
               st.sendResponse(response);
               return;
            }
            // Check if any of our SUs are subscribed to the group
            boolean found = false;
            for (SuConfig suconfig : rfss.getServedSubscriberUnits()) {
               if (suconfig.getSubscribedGroups().contains(groupConfig)) {
                  found = true;
               }
            }
            if (!found) {
               logger.debug("could not find " + groupId);
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.NOT_FOUND_UNKNOWN_TARGET_GROUP);
               st.sendResponse(response);

            } else if (activeGroupCalls.containsKey(groupId)) {

               // Look for the collinding case (we have sent out an invite at
               // the same time as the group has sent back an invite to us)
               if (requestEvent.getDialog() != activeGroupCalls.get(groupId).getDialog()) {

                  logger.debug("processInvite(2): Found an existing Active group call - declining invite");

                  ClientTransaction ct = activeGroupCalls.get(groupId).getClientTransaction();

                  PriorityHeader priorityHeader = 
                     (PriorityHeader) request.getHeader(PriorityHeader.NAME);
                  boolean emergency = SipUtils.isEmergency(priorityHeader);
                  int priority = SipUtils.getPriorityValue(priorityHeader);

                  PriorityHeader currentPriorityHeader = 
                     (PriorityHeader) ct.getRequest().getHeader(PriorityHeader.NAME);
                  boolean currentEmergency = SipUtils.isEmergency(currentPriorityHeader);
                  int currentPriority = SipUtils.getPriorityValue(currentPriorityHeader);

if (logger.isDebugEnabled()) {
logger.debug("#685 emergency="+emergency+"  priority="+priority);
logger.debug("#685 cur_emergency="+currentEmergency+"  cur_priority="+currentPriority);
logger.debug("#685 ct.getState()="+ct.getState()+" ct.getRequest()=\n"+ct.getRequest());
}

                  if (ct.getState() == TransactionState.CALLING
                  /* || ct.getState() == TransactionState.PROCEEDING */) {
                     Response response = messageFactory.createResponse( Response.REQUEST_PENDING,
                           st.getRequest());
                     logger.debug("#685 REQUEST_PENDING: response=\n"+response);
                     st.sendResponse(response);
                     return;

                  } else if (!(ct.getState() == TransactionState.COMPLETED) &&
                             !(ct.getState() == TransactionState.TERMINATED)) {

                     if ( (emergency == currentEmergency) &&
                          (priority >= currentPriority) ) {
                        // deferred to server-side
                        logger.debug("#685 deferred- 603 Decline ...");
                        //----------------------------
                        if (rfssConfig.getGroupCallInviteProcessingTime() != 0) {
                           logger.debug("Creating Group Invite serving delay task "
                              + rfssConfig.getGroupCallInviteProcessingTime());
                           GroupInviteServingDelayTask delayTask = new GroupInviteServingDelayTask(
                                 requestEvent, st);
                           pendingTimers.put(st.getBranchId(), delayTask);
                           ISSITimer.getTimer().schedule( delayTask,
                              rfssConfig.getGroupCallInviteProcessingTime() * 1000);
                        //} else {
                        //   logger.debug("GroupInvite serving delay is 0 -- processing request");
                        //   actuallyProcessRequest(requestEvent, st);
                        }
                        //----------------------------
                     } 
                     else if ( isCurrentCallHigherPriority(emergency, priority,
                           currentEmergency, currentPriority)) {
//#685 wrong direction of 603 Decline
//logger.debug("#685 wrong direction- 603 Decline ...");
                        handleCollidingRequest(requestEvent, st);
                     } else {
//logger.debug("#685 worng direction- send 200 OK ...");
                        activeGroupCalls.put(groupId, new GroupServing(rfss,
                           requestEvent.getDialog(), st, groupConfig, provider));
                        sendResponse(requestEvent, groupConfig, st, Response.OK);
                     }
                  } else {
                     logger.debug("Current tx state is " + ct.getState());
                     handleCollidingRequest(requestEvent, st);
                  }
               } else {
                  GroupServing groupServing = activeGroupCalls.get(groupId);
                  handleReInviteAtGroupServing(requestEvent, groupServing);
               }

            } else {
               // TODO check for resources etc.
               sendResponse(requestEvent, groupConfig, st, Response.OK);
            }
         }

      } catch (Exception ex) {
         rfss.logError("Unexpected exception", ex);
         rfss.getTestHarness().fail("unexpected exception", ex);
      }
   }

   /**
    * Create and send a response back in response to the GroupCallSetup RequestEvent.
    * 
    * @param responseCode -- response code to send back.
    * 
    */
   private void sendResponse(RequestEvent requestEvent, GroupConfig groupConfig,
         ServerTransaction serverTransaction, int responseCode)
         throws Exception
   {
      Request request = requestEvent.getRequest();
      Response response = messageFactory.createResponse(responseCode, request);
      
      ContactHeader contactHeader = SipUtils.createContactHeaderForGroup( groupConfig, rfssConfig);
      response.setHeader(contactHeader);
      logger.debug("\n***Building ContactHeader FROM request=\n"+request);
      
      SipUtils.addToHeaderTagByStatusCode( response);

      /***
      // Tag for to header is mandatory in 2xx response.
      ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
      if (response.getStatusCode()/100 == 2 && toHeader.getTag() == null) {
         toHeader.setTag( SipUtils.createTag());
      }
        ***/

      // If responding with a Success responseCode. Then create
      // a new RTP Session for this SU if needed.
      // Modify the response SDP Body to reflect the IP address
      // and port of the new session we just created and
      // send the response back.

      if (responseCode == Response.OK) {

         ContentList contentList = ContentList.getContentListFromMessage(request);
         SdpContent sdpContent = (SdpContent)
            contentList.getContent(ISSIConstants.APPLICATION, ISSIConstants.SDP);
         SessionDescription sessionDescription = 
            (SessionDescription) sdpContent.getSessionDescription();

         // The SU is answering with a SIP OK. At this point,
         // we need to allocate a new RTP Session (i.e. a PTT session),
         // from which we extract the IP address and port and send it
         // in the OK.
         // String sessionId = SipUtils.getRtpSessionIdFromMessage(response);
	 
         PttManager pttManager = rfss.getTransmissionControlManager().getPttManager();
         PttSessionMultiplexer mux = null;
         try {
            String sdpSessionId = ((OriginField) sessionDescription.getOrigin()).getSessIdAsString();
            mux = pttManager.createPttSessionMultiplexer(sdpSessionId, groupConfig.getGroupId());
            mux.setOwningRfss(rfssConfig);
            mux.setRemoteRfssDomainName(groupConfig.getHomeRfss().getDomainName());
            MediaDescription mediaDescription = 
               (MediaDescription) sessionDescription.getMediaDescriptions(true).get(0);
//TODO: check for null ??

            String ipAddress = sessionDescription.getConnection().getAddress();
            if (mediaDescription.getConnection() != null) {
               ipAddress = mediaDescription.getConnection().getAddress();
            }

            int port = mediaDescription.getMedia().getMediaPort();
            mux.setRemoteIpAddress(ipAddress);
            logger.debug("original remote rtp recvport = " + mux.getRtpSession().getRemoteRtpRecvPort());
            mux.setRemoteRtpRecvPort(port);

            Iterator<SuConfig> it = groupConfig.getSubscribers();
            while (it.hasNext()) {
               SuConfig groupMember = it.next();
               if (rfss.isSubscriberUnitServed(groupMember)) {
                  SmfSession smfSession = 
                     pttManager.createSmfSession( mux.getRtpSession(), LinkType.GROUP_SERVING);
                  logger.debug("smf session 2 " + smfSession);
                  smfSession.getSmfReceiver().addListener( rfss.getCurrentTestCase());
                  smfSession.setGroupId(groupConfig.getGroupId());
                  smfSession.setSessionDescription(sessionDescription);
                  int tsn = pttManager.getNewTsn( LinkType.GROUP_SERVING);
                  smfSession.setTsn(groupMember.getSuId(), tsn);
                  mux.addPttSession(tsn, smfSession);
                  mux.addSu(groupMember.getRadicalName(), tsn);
               }
            }

         } catch (RtpException ex) {
            Response errorResponse = SipUtils.createErrorResponse( rfssConfig, request,
                  WarningCodes.NO_RTP_RESOURCE);
            serverTransaction.sendResponse(errorResponse);
            return;
         }

	 //--------------------------------------------------------------------------
         //+++Request request = requestEvent.getRequest();
         //+++SipURI requestUri = (SipURI) request.getRequestURI();
         //+++String groupId = SipUtils.getGroupID(requestUri);
         //+++long gid = Long.parseLong(groupId, 16);
         CallControlManager.fixupSdpAnnounce(rfssConfig, mux, sessionDescription);

	 /*** 20.1.1 PTT
         ContentList xcontentList = ContentList.getContentListFromMessage(request);
         ContentTypeHeader ctHeader = xcontentList.getContentTypeHeader();
	  ***/

         ContentTypeHeader ctHeader = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
         logger.debug("sendResponse(): ctHeader="+ctHeader);

         if (ISSIConstants.TAG_MULTIPART.equals(ctHeader.getContentType()) &&
             ISSIConstants.TAG_MIXED.equals(ctHeader.getContentSubType())) {

            // multipart
            ContentList clist = new ContentList();
            SdpContent sc = SdpContent.createSdpContent();
            sc.setSessionDescription(sessionDescription);
            clist.add(sc);
          
            // 9.9.x protected mode
	    // 20.1.1 PTT
            //===CallParamContent callParamContent = CallParamContent.createCallParamContent();
            CallParamContent callParamContent = contentList.getCallParamContent();
            if( callParamContent != null) {
	       CallParam callParam = callParamContent.getCallParam();
               if( callParam.isConfirmedGroupCall()) {
                  callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
               }
               //YYY: donot dispaly c-groupcalltype: 0/1
               callParam.setConfirmedGroupCallIsSet(false);

               logger.debug("sendResponse(991): multi-part callParam="+callParam);
               clist.add(callParamContent);
	    }
            response.setContent(clist.toString(), clist.getContentTypeHeader());
         }
	 else
         {
            // application/sdp
            logger.debug("sendResponse(): NOT a multi-part isRfResource="+rfss.isRfResourcesAvailable());
            if (!rfss.isRfResourcesAvailable()) {
               // NO RF resources available 
               logger.debug("sendResponse(): NO RF Resource available...");

               ContentList clist = new ContentList();
               SdpContent sc = SdpContent.createSdpContent();
               sc.setSessionDescription(sessionDescription);
               clist.add(sc);

               //===CallParamContent callParamContent = CallParamContent.createCallParamContent();
               CallParamContent callParamContent = contentList.getCallParamContent();
               if( callParamContent != null) {
	          CallParam callParam = callParamContent.getCallParam();
                  callParam.setRfResourceAvailable( rfssConfig.isRfResourcesAvailable());
	       }
               clist.add(callParamContent);

               response.setContent(clist.toString(), clist.getContentTypeHeader());

            } else {
               logger.debug("sendResponse(): RF Resource available...");
               // 21.1.1
               //response.setContent(sessionDescription.toString(), ctHeader);
               response.setContent(sessionDescription.toString(), SipUtils.createAppSdpContentTypeHeader());
            }
         }
         response.setHeader( SipUtils.createContentDispositionHeader());
         //transmissionControlManager.addRtpListener(tcSap);
         
         SipUtils.addAllowHeaders(response);
	 SipUtils.checkContentLength(response);
         logger.debug("GroupCallControlManager: sendResponse=\n"+response);

	 // Possible tag mismatch SipException
         serverTransaction.sendResponse(response);

         //-----------------------------------------------------
         logger.debug("GroupCallControlManager: process GroupServing.addSu()...");
         Dialog dialog = serverTransaction.getDialog();
         GroupServing groupServing = 
            new GroupServing(rfss, dialog, serverTransaction, groupConfig, provider);
         groupServing.setMultiplexer(mux);
         Iterator<SuConfig> it = groupConfig.getSubscribers();
         while (it.hasNext()) {
            SuConfig groupMember = it.next();
            if (rfss.isSubscriberUnitServed(groupMember)) {
               groupServing.addSu(groupMember);
            }
         }

	 // rewrite
         PriorityHeader priorityHeader = (PriorityHeader) request.getHeader(PriorityHeader.NAME);
	 if( priorityHeader != null) {
            groupServing.setPriority(SipUtils.getPriorityValue( priorityHeader));
            groupServing.setEmergency(SipUtils.isEmergency( priorityHeader));
	 }
         activeGroupCalls.put(groupConfig.getRadicalName(), groupServing);

      } else {
	 SipUtils.checkContentLength( response);
         serverTransaction.sendResponse(response);

         logger.debug("GroupCallControlManager: NOT Request.OK responseCode="+responseCode);
         logger.debug("GroupCallControlManager: response=\n"+response);
      }
      //==================
      //showln("GroupCallControlManager: sendResponse(): response=\n\n"+response);
   }

   /**
    * Create a re-invite for a group call.
    * 
    * @param calledGroup -- group to call.
    * @param dialog -- original dialog for which the group call was
    *                  originally launched.
    * @return -- the formatted request.
    */
   public Request createReInviteForGroupCall(GroupHome calledGroup, Dialog dialog)
      throws Exception
   {
      try {
         Request sipRequest = dialog.createRequest(Request.INVITE);
         ServerTransaction st = calledGroup.getServerTransaction();
         Request originalRequest = st.getRequest();
         sipRequest.setHeader(originalRequest.getHeader(CallIdHeader.NAME));

	 /*** BUG ?
         ContactHeader contactHeader = SipUtils.createContactHeaderForRfss(
               rfssConfig, ISSIConstants.TIA_P25_SG);
         SipURI contactUri = (SipURI) contactHeader.getAddress().getURI();
         contactUri.setUser(calledGroup.getGroupConfig().getRadicalName());
	  ***/
         GroupConfig groupConfig = calledGroup.getGroupConfig();
         ContactHeader contactHeader = SipUtils.createContactHeaderForGroup( groupConfig, rfssConfig);
         sipRequest.setHeader(contactHeader);

         //sipRequest.setRequestURI(originalRequest.getRequestURI());
         sipRequest.setHeader(SipUtils.createTimeStampHeader());
         
         SipUtils.addAcceptHeader(sipRequest);
         sipRequest.addHeader(originalRequest.getHeader(PriorityHeader.NAME));
         sipRequest.addHeader(SipUtils.createContentDispositionHeader());

/***
logger.debug("createdReInviteForGroupCall : copy ContentList from originalRequest...");
ContentList clist = ContentList.getContentListFromMessage(originalRequest);
if (clist != null) {
   sipRequest.setContent(clist.toString(), clist.getContentTypeHeader());
}
 ***/
	 //===SipUtils.checkContentLength( sipRequest);
         if (logger.isDebugEnabled()) {
            logger.debug("createdReInviteForGroupCall : (GroupHome) request=\n"+sipRequest);
         }
         return sipRequest;
      }
      catch (Exception ex) {
         logger.fatal("unexpected exception ", ex);
	 throw ex;
      }
   }

   /**
    * Create a re-invite for a group call.
    * 
    * @param calledGroup -- group to call.
    * @param dialog -- original dialog for which the group call was
    *                  originally launched.
    * @return -- the formatted request.
    */
   private Request createReInviteForGroupCall(GroupServing calledGroup, Dialog dialog) {
      try {
         Request sipRequest = dialog.createRequest(Request.INVITE);
         //ClientTransaction ct = calledGroup.getClientTransaction();
         Request originalRequest = calledGroup.getLastTransaction().getRequest();
         logger.debug("createReInviteForGroupCall: (GroupServing) origRequest=" + originalRequest);

         sipRequest.setHeader(originalRequest.getHeader(CallIdHeader.NAME));

         //sipRequest.setHeader(SipUtils.createContactHeaderForRfss(
         //      rfssConfig, ISSIConstants.TIA_P25_SG));
         GroupConfig groupConfig = calledGroup.getGroupConfig();
         ContactHeader contactHeader = SipUtils.createContactHeaderForGroup( groupConfig, rfssConfig);
         sipRequest.setHeader(contactHeader);

	 //#602 RouteHeader
         if (groupConfig.getHomeRfss() != rfssConfig) 
         {
            RouteHeader routeHeader = SipUtils.createRouteToRfss(groupConfig.getHomeRfss());
            SipURI sipUri = (SipURI) routeHeader.getAddress().getURI();
            sipUri.setUser(ISSIConstants.P25_GROUP_CALL);
            sipRequest.addHeader(routeHeader);
            logger.debug("createReInviteForGroupCall: addRoute="+routeHeader);
         }
         else {
            logger.debug("createReInviteForGroupCall: NO RouteHeader...");
         }

         //#706 20.7.x p25dr
         //sipRequest.setRequestURI(originalRequest.getRequestURI());
         //logger.debug("createReInviteForGroupCall: #706 orig.RequestURI ...");
	 //
         // 20.2.x p25dr
         SipURI sipUri = (SipURI) sipRequest.getRequestURI();
         sipUri.setHost(ISSIConstants.P25DR);
         //logger.debug("createReInviteForGroupCall(777): sipUri=" +sipUri);

         sipRequest.setHeader(SipUtils.createTimeStampHeader());
         SipUtils.addAcceptHeader(sipRequest);
         sipRequest.addHeader(originalRequest.getHeader(PriorityHeader.NAME));
         sipRequest.addHeader(SipUtils.createContentDispositionHeader());
         // may need to copy ContentList ? Leave this to caller ?

	 SipUtils.checkContentLength(sipRequest);
         if (logger.isDebugEnabled()) {
            logger.debug("createReInviteForGroupCall: (GroupServing) request=\n" + sipRequest);
	 }
         return sipRequest;

      } catch (Exception ex) {
         logger.fatal("unexpected exception ", ex);
         return null;
      }
   }

   /**
    * Set up an outgoing invite
    * 
    * @param callingSu
    * @param calledGroup
    * @return Invite request
    * @throws Exception
    */
   private Request createInviteForGroupCall(GroupConfig calledGroup, int priority, boolean isEmergency)
         throws Exception {

      // create my SIP URI. This goes into the From Header.
      String calledRadicalName = calledGroup.getRadicalName();
      SipURI requestUri = addressFactory.createSipURI(calledRadicalName, ISSIConstants.P25DR);
      requestUri.setUserParam(ISSIConstants.TIA_P25_SG);
      SipURI callingSipURI = SipUtils.createSipURI(calledRadicalName, ISSIConstants.P25DR);
      callingSipURI.setParameter(ISSIConstants.USER, ISSIConstants.TIA_P25_SG);

      Address fromAddress = addressFactory.createAddress(callingSipURI);
      String tag = SipUtils.createTag();      
      FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, tag);

      // Create the SIP URI for the Called RFSS.
      SipURI calledSipURI = SipUtils.createSipURI(calledRadicalName, ISSIConstants.P25DR);
      calledSipURI.setParameter(ISSIConstants.USER, ISSIConstants.TIA_P25_SG);
      Address toAddress = addressFactory.createAddress(calledSipURI);
      ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
      ContactHeader contactHeader = SipUtils.createContactHeaderForGroup( calledGroup, rfssConfig);

      ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
      LinkedList<ViaHeader> viaHeaders = new LinkedList<ViaHeader>();
      viaHeaders.add(viaHeader);

      // Get a new call ID header for the outgoing invite.
      CallIdHeader callIdHeader = SipUtils.createCallIdHeader(rfssConfig);
      CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1L,Request.INVITE);
      MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

      Request sipRequest = messageFactory.createRequest(requestUri,
            Request.INVITE, callIdHeader, cseqHeader, fromHeader, toHeader,
            viaHeaders, maxForwardsHeader);

      // Test 21.5.1
      logger.debug("createInviteForGroupCall: calledGroup.rfssId="+calledGroup.getHomeRfss().getRfssIdString());
      logger.debug("createInviteForGroupCall: calledGroup.emulated="+calledGroup.getHomeRfss().isEmulated());
      logger.debug("createInviteForGroupCall: rfss.rfssId="+rfssConfig.getRfssIdString());
      logger.debug("createInviteForGroupCall: rfss.emulated="+rfssConfig.isEmulated());
      //if (calledGroup.getHomeRfss() != rfssConfig ||
      //   (calledGroup.getHomeRfss().isEmulated() && rfssConfig.isEmulated())) 
      if (calledGroup.getHomeRfss() != rfssConfig) 
      {
         RouteHeader routeHeader = SipUtils.createRouteToRfss(calledGroup.getHomeRfss());
         SipURI sipUri = (SipURI) routeHeader.getAddress().getURI();
         sipUri.setUser(ISSIConstants.P25_GROUP_CALL);
         sipRequest.addHeader(routeHeader);
         logger.debug("createInviteForGroupCall: addRoute="+routeHeader);
      }
      else {
         logger.debug("createInviteForGroupCall: NO RouteHeader...");
      }
      sipRequest.addHeader(contactHeader);
      SipUtils.addAllowHeaders(sipRequest);
      SipUtils.addAcceptHeader(sipRequest);
      SipUtils.addPriorityHeader( sipRequest, priority, isEmergency);

      //sipRequest.addHeader( SipUtils.createContentDispositionHeader());
      SipUtils.checkContentLength(sipRequest);

      if (logger.isDebugEnabled()) {
         logger.debug("createInviteForGroupCall: request=\n" + sipRequest);
      }
      return sipRequest;
   }

   private void groupCallConfirm(ResponseEvent responseEvent) throws Exception {

      ClientTransaction ct = responseEvent.getClientTransaction();
      Dialog dialog = ct.getDialog();
      Response response = responseEvent.getResponse();
      SessionDescription sdes = ContentList.getContentListFromMessage(
            response).getSdpContent().getSessionDescription();

      if (sdes == null) {
         rfss.logError("No session description - dropping response\n" + response);
         return;
      }

      // The sender preserves the origin field.
      // got an OK from the invitee.
      // now extract the session that we originally stored away
      // when we sent out the invite. One half of the session
      // has already been set up. The OK Contains the SDP answer.
      // we extract the port from the SDP answer.
      // long origin = sdes.getOrigin().getSessionId();
      // PttSession pendingSession = pendingSessions.get(origin);
      //
      String groupId = SipUtils.getGroupIdFromMessage(response);
      GroupServing groupServing = getGroupCall(groupId);
      if (groupServing == null) {
         rfss.logError("Could not find the serving group record for group " + groupId);
         return;
      }

      // GroupServing groupServing = (GroupServing)
      // dialog.getApplicationData();
      PttSessionMultiplexer pendingSession = groupServing.getMultiplexer();
      pendingSession.setRemoteRfssDomainName(groupServing.getGroupConfig()
            .getHomeRfss().getDomainName());

      if (pendingSession == null) {
         rfss.logError("Could not find pending session while processing  " + response);
         return;
      }
      String ipAddress = sdes.getConnection().getAddress();
      MediaDescription mediaDescription = (MediaDescription) sdes.getMediaDescriptions(true).get(0);
      if (mediaDescription.getConnection() != null) {
         ipAddress = mediaDescription.getConnection().getAddress();
      }

      int port = mediaDescription.getMedia().getMediaPort();
      if (logger.isDebugEnabled()) {
         logger.debug("Setting ipAddress : " + ipAddress);
         logger.debug("Setting port " + port);
      }

      // The last four hex digits of the radical name are the group ID.
      pendingSession.setRemoteIpAddress(ipAddress);
      pendingSession.setRemoteRtpRecvPort(port);
      if (pendingSession.getRtpSession().getRemoteRtpRecvPort() > 0) {
         pendingSession.getHeartbeatTransmitter().start();
      }

      CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

      Request ackRequest = dialog.createAck(cseqHeader.getSeqNumber());
      SipUtils.filterSipHeaders( ackRequest);
      SipUtils.checkContentLength( ackRequest);

      dialog.sendAck(ackRequest);
      logger.debug("groupCallConfirm: sendAck(1): ackRequest=\n"+ackRequest);

   }

   // //////////////////////////////////////////////////////////////////////////
   // Public methods
   // //////////////////////////////////////////////////////////////////////////

   /**
    * Processes incoming requests for GROUP calls ( both at group home and at
    * serving rfss).
    * 
    * @param requestEvent -
    *            the JAIN-SIP request event.
    */
   public void processRequest(RequestEvent requestEvent) {

      Request request = requestEvent.getRequest();
logger.debug("processRequest(1): request=\n"+request);

      ToHeader toHeader = ((ToHeader) request.getHeader(ToHeader.NAME));
      SipURI toUri = ((SipURI) toHeader.getAddress().getURI());
      String groupId = SipUtils.getRadicalName(toUri);
      String callId = ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();

      PriorityHeader priorityHeader = (PriorityHeader) request.getHeader(PriorityHeader.NAME);
      int priority = SipUtils.getPriorityValue(priorityHeader);
      boolean emergency = SipUtils.isEmergency(priorityHeader);

      GroupConfig groupConfig = topologyConfig.getGroupConfig(groupId);
      GroupMobilityManager gmm = getGroupMobilityManager();
      GroupHome groupHome = gmm.getGroup(groupConfig.getRadicalName());
logger.debug("processRequest(1): groupHome="+groupHome);

      ServerTransaction st = null;
      try {
         if ( Request.INVITE.equals(request.getMethod()) ) {

            // We are processing the INVITE for the group call.
            // Look for whether there are colliding invite calls.
            st = requestEvent.getServerTransaction();
            if (st == null) {
               try {
                  st = provider.getNewServerTransaction(request);
               } catch (Exception ex) {
                  logger.error("Request already processed!!", ex);
                  return;
               }
            }

            if (rfssConfig == groupConfig.getHomeRfss()) {
               // Check to see if we know about this group. If not, return
               // an error.
               if (groupHome == null) {
                  Response response = SipUtils.createErrorResponse( rfssConfig, request,
                        WarningCodes.NOT_FOUND_UNKNOWN_TARGET_GROUP);
                  try {
                     st.sendResponse(response);
                  } catch (Exception ex) {
                     logger.error("Unexpected error sending response", ex);
                  }
                  return;
               }

               // If we are at the home RFSS check if there are any ongoing
               // delayed requests.
               // if so, then block the call and return error.
               if (groupHome.getPendingRequestCount() > 0) {

                  logger.debug("processRequest(1): pendingRequest="+groupHome.getPendingRequestCount());
                  // This is not a reinvitation.
                  if (isCurrentCallHigherPriority(emergency, priority,
                        groupHome.isEmergency(), groupHome.getPriority())) {
                     // There is a higher priority call queued.
                     logger.debug("collision detected -- higher priority call in progress");
                     handleCollidingRequest(requestEvent, st);

                  } else {
                     // If we are a higher priority call, we are at the
                     // top of the queue.
                     groupHome.setPriority(priority);
                     groupHome.setEmergency(emergency);
                     // groupHome.incrementPendingRequestCount();
                     groupHome.setCallId(callId);
                     actuallyProcessRequest(requestEvent, st);
                  }

               } else {
                  groupHome.setPriority(priority);
                  groupHome.setEmergency(emergency);
                  groupHome.setCallId(callId);
                  groupHome.setServerTransaction(st);
                  ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
                  logger.debug("myRfssId = " + rfssConfig.getDomainName());
                  logger.debug("request is sent from = " + viaHeader.getHost());
                  if (rfssConfig.getGroupCallInviteProcessingTime() != 0 &&
                      !viaHeader.getHost().equals( rfssConfig.getDomainName())) {
                     logger.debug("Delaying processing of the INVITE at Group Home");
                     groupHome.incrementPendingRequestCount();

                     long period = rfssConfig.getGroupCallInviteProcessingTime() * 1000;
                     TimerTask delayTask = new GroupInviteHomeDelayTask(requestEvent,groupHome,st);
                     groupHome.setDelayTask(delayTask);
                     ISSITimer.getTimer().schedule( delayTask, period);

                  } else {
                     actuallyProcessRequest(requestEvent, st);
                  }
               }

            } else {
               if (rfssConfig.getGroupCallInviteProcessingTime() != 0) {
                  logger.debug("Creating Group Invite serving delay task "
                     + rfssConfig.getGroupCallInviteProcessingTime());
                  GroupInviteServingDelayTask delayTask = new GroupInviteServingDelayTask(
                        requestEvent, st);
                  pendingTimers.put(st.getBranchId(), delayTask);
                  ISSITimer.getTimer().schedule( delayTask,
                     rfssConfig.getGroupCallInviteProcessingTime() * 1000);
               } else {
                  logger.debug("GroupInvite serving delay is 0 -- processing request");
                  actuallyProcessRequest(requestEvent, st);
               }
            }
         } else {
            st = requestEvent.getServerTransaction();
            actuallyProcessRequest(requestEvent, st);
         }

      } catch (Exception ex) {
         logger.error("Unexpected exception", ex);
         rfss.getTestHarness().fail("Unexpected exception ");
      } finally {
         if (groupHome != null)
            logger.debug("processRequest: Pending count = " + groupHome.getPendingRequestCount());
      }
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.sip.SipListener#processResponse(javax.sip.ResponseEvent)
    */
   public void processResponse(ResponseEvent responseEvent) {
      logger.debug("Got a response");
      ClientTransaction ct = responseEvent.getClientTransaction();
      if (ct == null) {
         logger.debug("response retransmission -- dropping");
         return;
      }

      Response response = responseEvent.getResponse();
      ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
      CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
      if (!cseq.getMethod().equals(Request.INVITE))
         return;
      SipURI toUri = (SipURI) toHeader.getAddress().getURI();
      String groupId = SipUtils.getRadicalName(toUri);
      GroupConfig groupConfig = topologyConfig.getGroupConfig(groupId);

      if (groupConfig == null) {
         logger.error("Unepxected error, group not found ");
         return;
      }

      if (response.getStatusCode() == Response.OK) {
         try {
            // I am the home RFSS of this Group.
            if (ct.getApplicationData() != null &&
                ct.getApplicationData() instanceof MmfSession) {

               // This is the controller that got the response.
               MmfSession mmfSession = (MmfSession) ct.getApplicationData();
               ContentList clist = ContentList.getContentListFromMessage(response);
               SessionDescription sessionDescription = clist.getSdpContent().getSessionDescription();
               // Got a response.
               long gid = Long.parseLong(groupId, 16);
               String senderDomainName = SipUtils.getContactHost(response);
               rfss.getTransmissionControlManager().addMediaLeg(gid,
                     mmfSession, sessionDescription, senderDomainName);
               Dialog dialog = ct.getDialog();
               long seqno = cseq.getSeqNumber();
               // Ack the 200 OK.
               GroupHome groupHome = (GroupHome) ct.getDialog().getApplicationData();
               groupHome.addPttSession(senderDomainName.toLowerCase(), mmfSession);

               Request ackRequest = dialog.createAck(seqno);
               SipUtils.filterSipHeaders(ackRequest);
               SipUtils.checkContentLength(ackRequest);

               logger.debug(">>>>>>>>>>> sendAck(2): ackRequest=\n"+ackRequest);
               dialog.sendAck(ackRequest);
               
               if (mmfSession.getRtpSession().getRemoteRtpRecvPort() > 0) {
                  mmfSession.getHeartbeatTransmitter().start();
               }
            } else {

               // 21.4.1 NPE
               // NOTE: to be here, ct.getAppData is null
               PendingResponse pendingResponse = (PendingResponse) ct.getApplicationData();
               //logger.debug(">>>>>processResponse(1): pendingResponse="+pendingResponse);
               logger.debug(">>>>>processResponse(1): response=\n"+response);

               GroupServing calledGroup = getGroupCall(groupId);
               if (calledGroup == null) {
                  rfss.getTestHarness().fail( "Could not find group from the Response Check To heade");
                  return;
               }

               int rc = responseEvent.getResponse().getStatusCode();
               if (rc / 100 == 2) {
                  groupCallConfirm(responseEvent);
               } else {
                  logger.info("Ignoring response code " + rc);
               }
               assert calledGroup != null;
               activeGroupCalls.put(calledGroup.getGroupConfig().getRadicalName(), calledGroup);

	     if( pendingResponse != null) {
               GroupCallSetupScenario scenario = pendingResponse.scenario;
               if (scenario != null && scenario.isTalkSpurtSentAfterCallSetup()) {
                  SuConfig suConfig = scenario.getCallingSuConfig();
                  if (rfss.isSubscriberUnitServed(suConfig)) {
                     ((TestSU) suConfig.getSU()).doSendTalkSpurt(scenario);
                  }
               }
	     }  // null pendingResponse
            }
         } catch (Exception ex) {
            rfss.getTestHarness().fail("Unexpected exception ", ex);
         }
      } else if (response.getStatusCode() / 100 > 2) {

         // handle the error cases here.
         if (ct.getApplicationData() != null
               && ct.getApplicationData() instanceof MmfSession) {
            PttSessionInterface mmfSession = (PttSessionInterface) ct.getApplicationData();
            mmfSession.shutDown();
         } else {
            logger.debug("application data = " + ct.getDialog().getApplicationData());
            GroupServing calledGroup = (GroupServing) ct.getDialog().getApplicationData();
            calledGroup.getMultiplexer().shutDown();
         }
         activeGroupCalls.remove(groupConfig.getRadicalName());
      }
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.sip.SipListener#processTimeout(javax.sip.TimeoutEvent)
    */
   public void processTimeout(TimeoutEvent timeoutEvent) {
      logger.error("Client tx = " + timeoutEvent.getClientTransaction());
      Request request = null;
      if (timeoutEvent.getClientTransaction() != null) {
         logger.error("Ct request = " + timeoutEvent.getClientTransaction().getRequest());
         request = timeoutEvent.getClientTransaction().getRequest();
      }

      if (timeoutEvent.getServerTransaction() != null) {
         logger.error("St request = " + timeoutEvent.getServerTransaction().getRequest());
         request = timeoutEvent.getServerTransaction().getRequest();
      }
      rfss.logError("Timeout occured for the following request :\n" + request);

      if (!timeoutEvent.isServerTransaction()) {
         // Client transaction timed out. S
         ClientTransaction ct = timeoutEvent.getClientTransaction();

         ToHeader toHeader = ((ToHeader) request.getHeader(ToHeader.NAME));
         SipURI toUri = ((SipURI) toHeader.getAddress().getURI());
         String groupId = SipUtils.getRadicalName(toUri);
         GroupConfig groupConfig = topologyConfig.getGroupConfig(groupId);
         if (groupConfig.getHomeRfss() == rfssConfig &&
             request.getMethod().equals(Request.INVITE)) {
            GroupHome groupHome = getGroupMobilityManager().getGroup( groupId);
            Dialog dialog = ct.getDialog();
            groupHome.removeDialog(dialog);
         }
      }
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.sip.SipListener#processIOException(javax.sip.IOExceptionEvent)
    */
   public void processIOException(IOExceptionEvent ioex) {
      rfss.getTestHarness().fail("Unexpected event -- IOException ");
   }

   /**
    * (non-Javadoc)
    * 
    * @see javax.sip.SipListener#processTransactionTerminated(javax.sip.TransactionTerminatedEvent)
    */
   public void processTransactionTerminated( TransactionTerminatedEvent transactionTerminatedEvent) {
      ClientTransaction ct = transactionTerminatedEvent .getClientTransaction();
      if (ct != null) {
         if (ct.getDialog().getApplicationData() instanceof GroupHome) {
            GroupHome groupHome = (GroupHome) ct.getDialog().getApplicationData();
            groupHome.removeClientTransaction(ct);
         }
      }
   }

   public void processDialogTerminated( DialogTerminatedEvent dialogTerminatedEvent) {
      Dialog dialog = dialogTerminatedEvent.getDialog();

      if (dialog.getApplicationData() instanceof GroupHome) {
         GroupHome groupHome = (GroupHome) dialog.getApplicationData();
         groupHome.removeDialog(dialog);
      } else {
         GroupServing gc = (GroupServing) dialog.getApplicationData();
         if (gc != null) {
            String gid = gc.getGroupConfig().getRadicalName();
            activeGroupCalls.remove(gid);
            gc.getMultiplexer().shutDown();
         }
      }
   }

   /**
    * Join an arriving SU to an existing group.
    * 
    * @param suConfig
    * @param groupRadicalName
    * @return GroupServing object
    * @throws Exception
    */
   public GroupServing joinExistingGroup(SuConfig suConfig, String groupRadicalName)
         throws Exception {
      GroupConfig groupConfig = topologyConfig.getGroupConfig(groupRadicalName);
      logger.debug(String.format("joinExistingGroup: %s %s ", suConfig
            .getSuName(), groupConfig.getGroupName()));
      if (activeGroupCalls.containsKey(groupRadicalName)) {
         GroupServing gc = activeGroupCalls.get(groupRadicalName);
         logger.debug("Found an active group call at rfss " + rfssConfig.getRfssName());

         assert !gc.containsSu(suConfig);

         PttManager pttManager = rfss.getTransmissionControlManager().getPttManager();

         RtpSession rtpSession = gc.getMultiplexer().getRtpSession();
         SmfSession smfSession = pttManager.createSmfSession(rtpSession, LinkType.GROUP_SERVING);
         int tsn = pttManager.getNewTsn(LinkType.GROUP_SERVING);
         smfSession.setTsn(suConfig.getSuId(), tsn);
         gc.getMultiplexer().addPttSession(tsn, smfSession);
         gc.getMultiplexer().addSu(suConfig.getRadicalName(), tsn);
         gc.addSu(suConfig);
         // Need to add an smf session to the smf mux for the group.

         return gc;
      } else
         return null;

   }

   /**
    * Send out a request to establish a group call.
    * 
    * @param groupConfig
    * @return GroupServing object
    * @throws Exception
    */
   public GroupServing subscribeToGroup( GroupCallSetupScenario groupCallSetupScenario)
         throws Exception {
      logger.debug("subscribeToGroup " + groupCallSetupScenario.getCallingSuConfig().getSuName());
      GroupConfig calledGroup = groupCallSetupScenario.getCalledGroupConfig();
      ConfirmedCallSetupTime ccSetp = calledGroup.getGroupServiceProfile()
            .getConfirmedCallSetupTime();

      SuConfig suConfig = groupCallSetupScenario.getCallingSuConfig();
      int priority = groupCallSetupScenario.getPriority();
      boolean isEmergency = groupCallSetupScenario.isEmergency();
      boolean isConfirmed = ccSetp.getSetupTime() > 0;
      if (groupCallSetupScenario.isConfirmed()) {
         isConfirmed = groupCallSetupScenario.isConfirmed();
      }
      
      boolean isProtected = groupCallSetupScenario.isProtected();
      GroupServing groupServing = joinExistingGroup(suConfig, calledGroup.getRadicalName());
      if (groupServing != null)
         return groupServing;

      try {
         CallIdHeader callId = SipUtils.createCallIdHeader( rfssConfig);
		
         PttManager pttManager = rfss.getTransmissionControlManager().getPttManager();

         // the following should be random
         long sessId = (long) (Math.random() * 100000);

         // we are setting up the side of the RTP session
         // where we are going to be listening for incoming packets.
         // this is where we setup the initial SDP OFFER. We attach
         // it to the SIP INIVTE. Note in that we do not know
         // where the next hop is listening at this point. We will
         // know that only when the answer comes back.

         String ipAddress = rfss.getIpAddress();
         long sessVersion = (long) (Math.random() * 100000);
         int gid = calledGroup.getGroupId();
         PttSessionMultiplexer pttSessionMultiplexer = 
            pttManager.createPttSessionMultiplexer(Long.toString(sessId), gid);

         pttSessionMultiplexer.setRemoteRfssDomainName(calledGroup .getHomeRfss().getDomainName());
         SmfSession smfSession = pttManager.createSmfSession( pttSessionMultiplexer.getRtpSession(),
               LinkType.GROUP_SERVING);
         smfSession.setRemoteRfssDomainName(calledGroup.getHomeRfss().getDomainName());
         smfSession.getSmfReceiver().addListener( rfss.getCurrentTestCase());

         pttSessionMultiplexer.setOwningRfss(rfssConfig);
         int tsn = pttManager.getNewTsn(LinkType.GROUP_SERVING);
         smfSession.setTsn(suConfig.getSuId(), tsn);
         pttSessionMultiplexer.addPttSession(tsn, smfSession);

         if (logger.isDebugEnabled()) {
            logger.debug("smf session : " + smfSession);
         }
         int port = pttSessionMultiplexer.getRtpSession().getMyRtpRecvPort();

         String sdpBody = SipUtils.createSdpBody( sessId, sessVersion,
            ISSIConstants.P25_GROUP_CALL, ipAddress, port); 

         Request inviteRequest = createInviteForGroupCall(calledGroup, priority, isEmergency);
         inviteRequest.setHeader(callId);
         inviteRequest.addHeader( SipUtils.createContentDispositionHeader());

         logger.debug("subscribeToGroup(777): isConfirmed="+isConfirmed+" isProtected="+isProtected);
         logger.debug("subscribeToGroup(777): sdpBody="+sdpBody);

         // support explicit c-groupcalltype: 0
         //if (isConfirmed || isProtected)
         {
            ContentList contentList = new ContentList();
            SdpContent sdpContent = SdpContent.createSdpContent(sdpBody);
            contentList.add(sdpContent);

            //CallParamContent callParamContent = CallParamContent.createCallParamContent();
            CallParamContent callParamContent = contentList.getCallParamContent();
            CallParam callParam = callParamContent.getCallParam();
            callParam.setConfirmedGroupCall(isConfirmed);

	    //#665 set c-resavail:0 always on INVITE
            callParam.setRfResourceAvailable( false);

	    if( isConfirmed) {
               callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
	    } else {
	       //#665 set c-resavail:0 always on INVITE
               callParam.setRfResourceAvailable( false);
	    }

            // explicit: c-protected:0
            //#713 9.10.3
	    if(calledGroup.getCProtectedDisposition() == CProtectedDisposition.CLEAR_PROTECTED ||
               calledGroup.getCProtectedDisposition() == CProtectedDisposition.REJECT_PROTECTED ||
               calledGroup.getCProtectedDisposition() == CProtectedDisposition.REJECT_UNPROTECTED) {
               callParam.setProtectedModeIsSet(true);
            }
            if( isProtected) {
               callParam.setProtectedMode(isProtected);
               //callParam.setProtectedModeIsSet(true);
	    }
            logger.debug("subscribeToGroup(777): callParam="+callParam);

            contentList.add(callParamContent);
            logger.debug("subscribeToGroup(777): contentList="+contentList);

            inviteRequest.setContent(contentList.toString(), contentList.getContentTypeHeader());
         }
	 /**
	 else 
	 {
            //TODO: explicit c-groupcalltype: 0
            inviteRequest.setContent(sdpBody, SipUtils.createAppSdpContentTypeHeader()); 
         }
	  **/

         SessionDescription sd = SdpFactory.getInstance().createSessionDescription(sdpBody);
         smfSession.setSessionDescription(sd);
         PendingResponse pendingResponse = new PendingResponse();
         pendingResponse.procedure = Operation.CALL_SETUP;

         SipUtils.checkContentLength( inviteRequest);

         ClientTransaction ct = provider.getNewClientTransaction(inviteRequest);
         pendingResponse.scenario = groupCallSetupScenario;
         ct.setApplicationData(pendingResponse);

         Dialog dialog = ct.getDialog();
         groupServing = new GroupServing(rfss, dialog, ct, calledGroup, provider);
         pendingResponse.callSegment = groupServing;
         groupServing.setMultiplexer(pttSessionMultiplexer);
         groupServing.setPriority(priority);
         groupServing.setEmergency(isEmergency);
         activeGroupCalls.put(calledGroup.getRadicalName(), groupServing);
         dialog.setApplicationData(groupServing);
         ct.sendRequest();
         logger.debug("subscribeToGroup: inviteRequest=\n"+inviteRequest);

         groupServing.addSu(suConfig);
         pttSessionMultiplexer.addSu(suConfig.getRadicalName(), tsn);
         //activeGroupCalls.put(calledGroup.getRadicalName(),callSegment);
         return groupServing;

      } catch (Exception ex) {
         rfss.getTestHarness().fail("unexpected exception", ex);
         logger.error("Unexpected exception", ex);
         throw ex;
      }
   }

   /**
    * RTP resources have become available at the home RFSS. Re-invite group
    * members who have established half sessions with me. Reinvite myself to
    * groups that I am serving.
    * 
    * @param nports - number of ports to add.
    * @throws Exception
    */
   public void rtpResourcesAvailable(int nports) throws Exception {
      int portsUsed = 0;
      logger.debug("activeGroupCalls : " + activeGroupCalls);
      logger.debug("rtpResourcesAvailable() : Adding an rtp port = "+nports);
      Collection<GroupHome> groupHomes = getGroupMobilityManager().getGroups();

      for (GroupHome groupHome : groupHomes) {
         for (MmfSession mmfSession : groupHome.getMmfSessions()) {
            if (mmfSession.getMyRtpRecvPort() == 0) {

               mmfSession.assignRtpRecvPort();
               Dialog dialog = groupHome.getDialog(mmfSession);
               String domainName = ((SipURI) dialog.getRemoteTarget().getURI()).getHost();
               if (logger.isDebugEnabled()) {
                  logger.debug("domain name for remote target = " + domainName);
               }

               ClientTransaction clientTransaction = groupHome.getClientTransaction(domainName);
               Request originalRequest = clientTransaction != null ? clientTransaction.getRequest()
                     : groupHome.getServerTransaction().getRequest();

               Request request = createReInviteForGroupCall(groupHome, dialog);

               SessionDescription sd = mmfSession.getSessionDescription();
               CallControlManager.fixupSdpAnnounce(rfssConfig, mmfSession, sd);

               //REFACTOR
               buildGroupCallContentList(originalRequest, request, rfss, sd);
	       SipUtils.checkContentLength(request);
               logger.debug("rtpResourcesAvailable(): request=\n"+request);

               ClientTransaction ct = groupHome.getProvider().getNewClientTransaction(request);
               ct.setApplicationData(mmfSession);
               groupHome.setClientTransaction(domainName, ct);
               dialog.sendRequest(ct);

               portsUsed++;
               if (portsUsed >= nports)
                  return;
            }
         }
      }

      // look for groups for which we are serving and for which 
      // we dont have rtp resources
      for (GroupServing groupServing : activeGroupCalls.values()) {
         if (groupServing.getMultiplexer().getMyRtpRecvPort() == 0) {
            groupServing.getMultiplexer().assignRtpRecvPort();
            Dialog dialog = groupServing.getDialog();

            // The SIP dialog which we are using to talk to our home
            Request request = createReInviteForGroupCall(groupServing, dialog);

            Request originalRequest = groupServing.getLastTransaction().getRequest();

            ContentList contentList = ContentList.getContentListFromMessage(originalRequest);
            CallParamContent callParamContent = contentList.getCallParamContent();
            if (callParamContent != null) {
               CallParam callParam = callParamContent.getCallParam();
               // explicit c-groupcalltype: 0
               callParam.setConfirmedGroupCallIsSet(true);
               callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
            }

            SessionDescription sd = contentList.getSdpContent().getSessionDescription();
            CallControlManager.fixupSdpAnnounce(rfssConfig, groupServing.getMultiplexer(), sd);

            request.setContent(contentList.toString(), contentList.getContentTypeHeader());
                  //(ContentTypeHeader)originalRequest.getHeader(ContentTypeHeader.NAME));

	    SipUtils.checkContentLength(request);

            ClientTransaction ct = groupServing.getProvider().getNewClientTransaction(request);
            groupServing.setClientTransaction(ct);
            PendingResponse pendingResponse = new PendingResponse();
            pendingResponse.callSegment = groupServing;
            pendingResponse.procedure = Operation.REINVITE;
            dialog.sendRequest(ct);

            portsUsed++;
            if (portsUsed >= nports)
               return;
         }
      }
      rfss.getTransmissionControlManager().getPttManager().incrementPortLimit(nports - portsUsed);
   }

   /**
    * Remove rtp resources.
    *
    * @param nports - number of ports to remove.
    *
    */
   public void removeRtpPorts(int nports) throws Exception {

      int portsReleased = 0;
      logger.debug("activeGroupCalls: " + activeGroupCalls);
      logger.debug("GroupCallControlManager: removing rtp ports ");
      Collection<GroupHome> groupHomes = getGroupMobilityManager().getGroups();

      for (GroupHome groupHome : groupHomes) {

         for (MmfSession mmfSession : groupHome.getMmfSessions()) {

            if (mmfSession.getMyRtpRecvPort() != 0) {
               logger.debug("GroupCallControlManager : rtp-port="+mmfSession.getMyRtpRecvPort());
               mmfSession.stopReceiver();
               Dialog dialog = groupHome.getDialog(mmfSession);
               String domainName = ((SipURI) dialog.getRemoteTarget().getURI()).getHost();
               if (logger.isDebugEnabled()) {
                  logger.debug("domain name for remote target=" + domainName);
               }

               ClientTransaction clientTransaction = groupHome.getClientTransaction(domainName);
               logger.debug("client tx " + groupHome.getClientTransaction(domainName)
                           + " server tx  " + groupHome.getServerTransaction());

               Request originalRequest = clientTransaction != null ? clientTransaction.getRequest()
                     : groupHome.getServerTransaction().getRequest();

               Request request = createReInviteForGroupCall( groupHome, dialog);
               SessionDescription sd = mmfSession.getSessionDescription();
               CallControlManager.fixupSdpAnnounce(rfssConfig, mmfSession, sd);

               //REFACTOR
               buildGroupCallContentList(originalRequest, request, rfss, sd);
	       SipUtils.checkContentLength(request);

               ClientTransaction ct = groupHome.getProvider().getNewClientTransaction(request);
               ct.setApplicationData(mmfSession);
               groupHome.setClientTransaction(domainName, ct);
               dialog.sendRequest(ct);
               portsReleased++;
               if (portsReleased > nports)
                  return;
            }
         }
      }
      // look for groups for which we are serving and for which we dont
      // have rtp resources;
      //
      for (GroupServing groupServing : activeGroupCalls.values()) {

         if (groupServing.getMultiplexer().getMyRtpRecvPort() != 0) {
            groupServing.getMultiplexer().stopReceiver();

            Dialog dialog = groupServing.getDialog();
            // The SIP dialog which we are using to talk to our home
            Request request = createReInviteForGroupCall(groupServing, dialog);

	    PttSessionMultiplexer pttSession = groupServing.getMultiplexer();
            Request originalRequest = groupServing.getLastTransaction().getRequest();

            ContentList contentList = ContentList.getContentListFromMessage(originalRequest);
            CallParamContent callParamContent = contentList.getCallParamContent();

            SessionDescription sd = contentList.getSdpContent().getSessionDescription();
            CallControlManager.fixupSdpAnnounce(rfssConfig, pttSession, sd);

            //REFACTOR
            buildGroupCallContentList(originalRequest, request, rfss, sd);

/**** replaced by
//TODO: can we just use the callParamContent ??? see alertRfResourceChange()
            ContentList newContentList = new ContentList();
            if (callParamContent != null) {
               CallParamContent newCallParamContent = CallParamContent.createCallParamContent();
               CallParam newCallParam = newCallParamContent.getCallParam();
	       // support explicit c-groupcalltype: 0
               newCallParam.setConfirmedGroupCallIsSet(true);
               if( callParamContent.getCallParam().isConfirmedGroupCall()) {
                  newCallParam.setConfirmedGroupCall(true);
               }
	       newCallParam.setRfResourceAvailable(rfss.isRfResourcesAvailable());
               newContentList.add(newCallParamContent);
            }
            SessionDescription sd = contentList.getSdpContent().getSessionDescription();
            CallControlManager.fixupSdpAnnounce(rfssConfig, pttSession, sd);

            SdpContent newSdpContent = SdpContent.createSdpContent();
            newSdpContent.setSessionDescription(sd);
            newContentList.add(newSdpContent);

            request.setContent(newContentList.toString(), newContentList.getContentTypeHeader());
  *****/
	    SipUtils.checkContentLength( request);
                  
            ClientTransaction ct = groupServing.getProvider().getNewClientTransaction(request);
            groupServing.setClientTransaction(ct);
            dialog.sendRequest(ct);
            portsReleased++;
            if (portsReleased > nports)
               return;
         }
      }
   }

   /**
    * Get the current group call corresponding to a group radical name.
    * 
    * @return -- the group call if it exists or null if not.
    */
   public GroupServing getGroupCall(String groupRadicalName) {
      return activeGroupCalls.get(groupRadicalName);
   }

   /**
    * For the all groups for which I am HOME RFSS, send bye on all call legs.
    * For all groups for which I am a member RFSS send bye.
    */
   public void dropAllCalls() {
      try {
         logger.debug("Active group calls " + activeGroupCalls.values());
         for (GroupConfig groupConfig : topologyConfig.getGroupConfigurations()) {
            RfssConfig home = groupConfig.getHomeRfss();
            if (home == rfssConfig) {
               GroupMobilityManager gmm = getGroupMobilityManager();
               GroupHome groupHome = gmm.getGroup(groupConfig.getRadicalName());

               for (Dialog dialog : groupHome.getDialogs()) {
                  Request bye = dialog.createRequest(Request.BYE);

                  SipUtils.filterSipHeaders( bye);
                  ClientTransaction ct = provider.getNewClientTransaction(bye);
                  dialog.sendRequest(ct);
               }
            }
         }

         // For all groups for which I am a member, drop the call.
         for (GroupServing groupServing : activeGroupCalls.values()) {
            Dialog dialog = groupServing.getDialog();
            Request request = dialog.createRequest(Request.BYE);

            SipUtils.filterSipHeaders( request);
            ClientTransaction ctx = provider.getNewClientTransaction(request);
            dialog.sendRequest(ctx);
         }
      } catch (Exception ex) {
         logger.fatal("Error sending or creating bye ", ex);
      }
   }

   /**
    * Re-invite a group member to a call.
    * 
    * @param groupId -- Group Id to reinvite
    * @param rfssConfig -- Rfss to reinvite.
    *
    */
   public void reInviteRfssToGroup(String radicalName, RfssConfig rfssConfig)
         throws Exception {
      GroupHome groupHome = getGroupMobilityManager().getGroup( radicalName);
      if (groupHome == null)
         throw new Exception("Group not found " + radicalName);
      String rfssDomainName = rfssConfig.getDomainName();
      if (logger.isDebugEnabled()) {
         logger.debug("domain name for remote target = " + rfssDomainName);
      }

      MmfSession mmfSession = groupHome.getMmfSession(rfssDomainName);
      if (mmfSession == null)
         throw new Exception("MmfSession not found for this Rfss " + rfssConfig.getRfssName());

      Dialog dialog = groupHome.getDialog(mmfSession);
      if (dialog == null)
         throw new Exception("Could not find dialog for re-inviting");

      Request request = createReInviteForGroupCall(groupHome, dialog);
      //if (request == null)
      //   throw new Exception("Could not create request for ReInviteForGroupCall");

      SessionDescription sd = mmfSession.getSessionDescription();
      CallControlManager.fixupSdpAnnounce(rfssConfig, mmfSession, sd);

      //===request.setContent(sd.toString(), SipUtils.createAppSdpContentTypeHeader());
      //========================
      {
         ContentList newContentList = new ContentList();
         SdpContent sdpContent = SdpContent.createSdpContent();
         sdpContent.setSessionDescription( sd);
         newContentList.add( sdpContent);

	 CallParamContent callParamContent = newContentList.getCallParamContent();
         CallParam callParam = callParamContent.getCallParam();
	 // support explicit c-groupcalltype: 0
         callParam.setConfirmedGroupCallIsSet( true);
         //callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());

         logger.debug("reInviteRfssToGroup(1): callParam="+callParam);
         newContentList.add(callParamContent);
         request.setContent(newContentList.toString(), newContentList.getContentTypeHeader());
      }
      //========================
      SipUtils.checkContentLength(request);

      ClientTransaction ct = groupHome.getProvider().getNewClientTransaction(request);
      ct.setApplicationData(mmfSession);
      groupHome.setClientTransaction(rfssDomainName, ct);
      dialog.sendRequest(ct);
   }

   /**
    * Send a cancel for an outstanding invite of a group call.
    * 
    * @param groupServing -- group serving to send cancel to.
    * @throws Exception
    */
   public void sendCancel(GroupServing groupServing) throws Exception {

      ClientTransaction ct = groupServing.getClientTransaction();
      if (ct.getState() == TransactionState.PROCEEDING) {
         Request cancelRequest = ct.createCancel();
         
         SipUtils.filterSipHeaders( cancelRequest);
         ClientTransaction cancelTx = provider.getNewClientTransaction(cancelRequest);
         cancelTx.setApplicationData(groupServing);
         cancelTx.sendRequest();
      } 
      else if (rfssConfig.isAssignedGroup(groupServing.getGroupConfig().getRadicalName())) {
         logger.debug("Sending CANCEL from group home!");
         // We are home for this group.
         // We send CANCEL to the other legs.
         GroupHome groupHome = getGroupMobilityManager().getGroup(
               groupServing.getGroupConfig().getRadicalName());

         for (ClientTransaction inviteTx : groupHome.getClientTransactions()) {
            if (inviteTx.getState() == TransactionState.PROCEEDING) {
               Request cancelRequest = inviteTx.createCancel();
               SipUtils.filterSipHeaders(cancelRequest);
               ClientTransaction cancelTx = provider.getNewClientTransaction(cancelRequest);
               cancelTx.sendRequest();
            }
         }
      }
   }

   /**
    * Re-invite to the group home or group serving when RF resources become
    * available.
    * 
    * @param resourceVal --
    *            true / false indicates whether rf resources became available
    *            or not
    * 
    * @throws Exception
    */
   public void alertRfResourceChange(boolean resourceVal) throws Exception {

      for (GroupServing groupServing : activeGroupCalls.values()) {

         Dialog dialog = groupServing.getDialog();
         // The SIP dialog which we are using to talk to our home
         Request request = createReInviteForGroupCall(groupServing, dialog);

         PttSessionMultiplexer pttSession = groupServing.getMultiplexer();
         Request originalRequest = groupServing.getLastTransaction().getRequest();

         ContentList contentList = ContentList.getContentListFromMessage(originalRequest);
         CallParamContent callParamContent = contentList.getCallParamContent();
         if (callParamContent != null) {
            CallParam callParam = callParamContent.getCallParam();
	    // explicit c-groupcalltype: 0
            callParam.setConfirmedGroupCallIsSet(true);
            callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
         }
         SessionDescription sd = contentList.getSdpContent().getSessionDescription();
         CallControlManager.fixupSdpAnnounce(rfssConfig, pttSession, sd);

//TODO: do we need the fixed up sd in contentList ?

         request.setContent(contentList.toString(), contentList.getContentTypeHeader());
         SipUtils.checkContentLength( request);

         ClientTransaction ct = groupServing.getProvider().getNewClientTransaction(request);
         groupServing.setClientTransaction(ct);
         dialog.sendRequest(ct);
      }

      for (GroupHome groupHome : getGroupMobilityManager().getGroups()) {

         for (MmfSession mmfSession : groupHome.getMmfSessions()) {
            Dialog dialog = groupHome.getDialog(mmfSession);
            String domainName = ((SipURI) dialog.getRemoteTarget().getURI()).getHost();
            if (logger.isDebugEnabled()) {
               logger.debug("domain name for remote target=" + domainName);
            }

            Request originalRequest = groupHome.getClientTransaction( domainName).getRequest();

            Request request = createReInviteForGroupCall(groupHome, dialog);
            SessionDescription sd = mmfSession.getSessionDescription();
            CallControlManager.fixupSdpAnnounce(rfssConfig, mmfSession, sd);

            //REFACTOR
            buildGroupCallContentList(originalRequest, request, rfss, sd);
            SipUtils.checkContentLength(request);

            ClientTransaction ct = groupHome.getProvider().getNewClientTransaction(request);
            ct.setApplicationData(mmfSession);
            groupHome.setClientTransaction(domainName, ct);
            dialog.sendRequest(ct);
	    //logger.debug("alertRfResourceChange: request=\n"+request);
         }
      }
   }

   /**
    * Alert for group priority change.
    * 
    * @param radicalName -- group radical name.
    */
   public void groupPriorityChange(String groupRadicalName, int newPriority,
         boolean isEmergency) throws Exception {

      logger.debug("groupPriorityChange : " + groupRadicalName
            + " : newPriority " + newPriority + " isEmergency " + isEmergency);

      GroupServing groupServing = getGroupCall(groupRadicalName);
      logger.debug("groupServing=" + groupServing);
      logger.debug("   priority="+groupServing.getPriority() +" emergency=" +groupServing.isEmergency());

      if (groupServing != null &&
         (groupServing.getPriority() != newPriority || 
          groupServing.isEmergency() != isEmergency)) {

         //logger.debug("BBBB1: addPriorityHeader...");
         Dialog dialog = groupServing.getDialog();

	 // Test 21.4.1 Use new settings
         logger.debug("BBBB2: newPri="+newPriority+" Em="+isEmergency);
         Request originalRequest = groupServing.getLastTransaction().getRequest();

         // The SIP dialog which we are using to talk to our home
         Request request = createReInviteForGroupCall(groupServing, dialog);
	 SipUtils.addPriorityHeader(request, newPriority, isEmergency);

         ContentList contentList = ContentList.getContentListFromMessage(originalRequest);
         CallParamContent callParamContent = contentList.getCallParamContent();
         if (callParamContent != null) {
            CallParam callParam = callParamContent.getCallParam();
	    // support explicit c-groupcalltype: 0
            callParam.setConfirmedGroupCallIsSet( true);
            callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
         }
         SessionDescription sd = contentList.getSdpContent().getSessionDescription();
         PttSessionMultiplexer pttSessionMux = groupServing.getMultiplexer();
         CallControlManager.fixupSdpAnnounce(rfssConfig, pttSessionMux, sd);

         request.setContent(contentList.toString(), contentList.getContentTypeHeader());
         request.addHeader( SipUtils.createContentDispositionHeader());
	 SipUtils.checkContentLength( request);

         logger.debug("groupPriorityChange: request=\n" + request);
         ClientTransaction ct = groupServing.getProvider().getNewClientTransaction(request);
         groupServing.setClientTransaction(ct);
         dialog.sendRequest(ct);
      } 
      else {
         //logger.debug("BBBB3: checking groupHome...");
         GroupHome groupHome = getGroupMobilityManager().getGroup(groupRadicalName);
         logger.debug("groupHome = " + groupHome);

         if (groupHome == null ||
             (groupHome.getPriority() == newPriority && 
              groupHome.isEmergency() == isEmergency)) {
            logger.debug("could not find group home record ... returning ");
            return;
         }

         for (MmfSession mmfSession : groupHome.getMmfSessions()) {
            Dialog dialog = groupHome.getDialog(mmfSession);
            String domainName = ((SipURI) dialog.getRemoteTarget().getURI()).getHost();
            if (logger.isDebugEnabled()) {
               logger.debug("domain name for remote target=" + domainName);
            }

            Request originalRequest = groupHome.getClientTransaction( domainName).getRequest();

            Request request = createReInviteForGroupCall(groupHome, dialog);
            PriorityHeader priorityHeader = (PriorityHeader) request.getHeader(PriorityHeader.NAME);
            if (isEmergency) {
               String priStr = Integer.toString(newPriority) + ";" + (isEmergency ? "e" : "a");
               priorityHeader.setPriority( priStr);
            }
            request.setHeader(priorityHeader);

            SessionDescription sd = mmfSession.getSessionDescription();
            CallControlManager.fixupSdpAnnounce(rfssConfig, mmfSession, sd);

            //REFACTOR
            buildGroupCallContentList(originalRequest, request, rfss, sd);
            SipUtils.checkContentLength(request);

            ClientTransaction ct = groupHome.getProvider().getNewClientTransaction(request);
            ct.setApplicationData(mmfSession);
            groupHome.setClientTransaction(domainName, ct);
            dialog.sendRequest(ct);

         }  // for-loop
      }
   }

   //---------------------------------------------------------------------------------
   private void buildGroupCallContentList(Request origRequest, Message newRequest,
      RFSS rfss, SessionDescription sd) throws ParseException
   {
      ContentList contentList = ContentList.getContentListFromMessage(origRequest);
      CallParamContent callParamContent = contentList.getCallParamContent();

      buildGroupCallContentList(callParamContent, newRequest, rfss, sd);
   }

   private void buildGroupCallContentList(CallParamContent callParamContent,
      Message newRequest, RFSS rfss, SessionDescription sd)
      throws ParseException
   {
      if(callParamContent == null) {
         newRequest.setContent(sd.toString(), SipUtils.createAppSdpContentTypeHeader());
      }
      else {
         ContentList newContentList = new ContentList();
         SdpContent sdpContent = SdpContent.createSdpContent();
         sdpContent.setSessionDescription(sd);
         newContentList.add(sdpContent);

         CallParam callParam = callParamContent.getCallParam();
         if( newRequest instanceof Response) {
            // 20.2.x donot display c-groupcalltype: 0
	    // 200 OK to INVITE
            callParam.setConfirmedGroupCallIsSet(false);
         }
	 else {
            // support explicit c-groupcalltype: 0
            callParam.setConfirmedGroupCallIsSet(true);
         }
         //callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());

logger.debug("buildGroupCallContentList(1a): isConfirmedGC="+callParam.isConfirmedGroupCall());
         /****#687 20.5.x */
         if( callParam.isConfirmedGroupCall()) {
            callParam.setRfResourceAvailable( rfss.isRfResourcesAvailable());
         } else {
	    // set c-resavail:0 always on INVITE
            callParam.setRfResourceAvailable( false);
         }
	 /***/
         logger.debug("buildGroupCallContentList(1): callParam="+callParam);

         newContentList.add(callParamContent);
         newRequest.setContent(newContentList.toString(), newContentList.getContentTypeHeader());
      }
   }
}
