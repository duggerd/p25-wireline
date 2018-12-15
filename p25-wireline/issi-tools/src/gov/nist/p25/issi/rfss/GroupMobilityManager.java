//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SuState;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25body.params.RegisterParam;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfile;
import gov.nist.p25.issi.rfss.SipUtils;
import gov.nist.p25.issi.transctlmgr.PttPointToMultipointSession;
import gov.nist.p25.issi.transctlmgr.ptt.SessionType;
import gov.nist.p25.issi.utils.ProtocolObjects;
import gov.nist.p25.issi.utils.WarningCodes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimerTask;

import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.AcceptHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WarningHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

/**
 * Implements the functionality of the Group Mobility Manager gor groups. From
 * the ISSI spec, the function is described as follows: Within each RFSS, the
 * Group Mobility Management Function (GMMF) is responsible for group mobility
 * management. The GMMF supports different functions depending on the role of
 * the RFSS with respect to the group in question.
 * 
 * When the RFSS is the serving RFSS relative to the group, the GMMF is
 * responsible for the following functions:
 * 
 * <ul>
 * <li> - Registration of the group with the Home RFSS when the RFSS detects it
 * has SUs currently affiliated with the group in its serving area,
 * <li> - De-registration of the group with the home RFSS when the RFSS
 * determines that it no longer has affiliated members for the group,
 * <li> - Responding to the home RFSS when the Home RFSS requests confirmation
 * that the RFSS still has an interest in being registered for the group,
 * <li> - Requesting service profile information for the group on the ISSI only
 * when the RFSS is currently registered to the SG, or is in the process of
 * registering with the group.
 * </ul>
 * 
 * When the RFSS is the home RFSS relative to the group, the GMMF is responsible
 * for the following function:
 * <ul>
 * <li> - Providing the group service profile information to suitably authorized
 * serving RFSSs that request the information.
 * <li> - Prohibiting registration of the group in serving domains for which it
 * is not authorized,
 * <li> - Maintaining a list of serving RFSSs of the SG
 * <li> - Checking whether a serving RFSS still has an interest in being
 * registered for the SG, in accordance with local policy.
 * </ul>
 * 
 * <b>
 * 
 * NOTE The list of affiliated P25 subscriber units to a group in a serving RFSS
 * is not known at the SGs home RFSS. As a consequence, the SGs home RFSS might
 * only control the right of a serving RFSS to be in a group call coverage or
 * not.
 * 
 * </b>
 * 
 * Implementation Notes: This class is structured as a SipListener although it
 * is not directly registered with the SIP Stack. Instead it is registered with
 * the Mobility manager which directs SIP Events to an instance of this class by
 * looking at the From and To headers of the incoming SIP request/response to
 * determine whether the Request/Response belongs to a Group or to an SU to SU
 * call. Although SU's belong to Groups, during runtime, group memebership is
 * tracked on the basis of RFSS's. This is because a given Serving RFSS keeps at
 * most one active session with a group and multiplexes all signialing and media
 * to all the SU's in that group which it is currently serving. Thus, at the
 * RFSS, we maintain a dynamic structure called "Group" which tracks RFSS
 * membership of the Group.
 * 
 */
public class GroupMobilityManager implements SipListener {

   private static Logger logger = Logger.getLogger(GroupMobilityManager.class);
   public static void showln(String s) { System.out.println(s); }

   private static HeaderFactory headerFactory = ProtocolObjects.headerFactory;
   private static MessageFactory messageFactory = ProtocolObjects.messageFactory;
   private static AddressFactory addressFactory = ProtocolObjects.addressFactory;

   private RFSS rfss;
   private RfssConfig rfssConfig;
   private SipProvider provider;
   private TopologyConfig topologyConfig;

   /*
    * The Groups for which we are the home RFSS
    */
   private Hashtable<String, GroupHome> myGroups;

   /*
    * The Groups for which there are currently served SUs.
    */
   private Hashtable<String, Integer> groupRefcountTable;
   
   enum Operation {
      REGISTER, QUERY, DEREGISTER, HOME_QUERY
   }

   class PendingResponse {
      Operation op;
      ClientTransaction ct;
      Response response;
   }
   
   /**
    * Create a home query request.
    * 
    * SIP Register-Query is sent by a serving RFSS to query a home RFSS for
    * service profile information, and by a home RFSS to confirm the interest
    * of a serving RFSS in a mobility object. It is distinguished by the
    * absence of both the expires parameter and the contact parameter. The SIP
    * Register-Query message is used in two procedures described in ?5.5, the
    * Home Query procedure and the Serving Query procedure. The content of the
    * message is consistent in each case, and the particular procedure is
    * determined by the roles of the participating RFSSs pursuant to the
    * mobility object involved.
    */
   private Request createHomeQueryRequest(GroupConfig groupConfig,
         RfssConfig targetRfss, boolean force, boolean confirm) {

      try {
         String groupName = groupConfig.getRadicalName();
         SipURI fromURI = (SipURI) SipUtils.createGroupSipURI(groupName);

         Address fromAddress = addressFactory.createAddress(fromURI);
         FromHeader fromHeader = headerFactory.createFromHeader(fromAddress,
			 SipUtils.createTag());

         SipURI toURI = fromURI;
         Address toAddress = addressFactory.createAddress(toURI);
         ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
         String domainName = targetRfss.getDomainName();
   
         // The request URI for Register requests has the form
         // sip:domain-name
         SipURI registrationURI = SipUtils.createDomainSipURI(domainName);
   
         // Create the via header for the outgoing request.
         ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
         LinkedList<ViaHeader> viaHeaders = new LinkedList<ViaHeader>();
         viaHeaders.add(viaHeader);
   
         // Create a maxforwards header for the outgoing request.
         MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
         CallIdHeader callIdHeader = provider.getNewCallId();
         CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1L,Request.REGISTER);
   
         Request sipRequest = messageFactory.createRequest(registrationURI,
               Request.REGISTER, callIdHeader, cseqHeader, fromHeader,
               toHeader, viaHeaders, maxForwards);

   
         SipUtils.addAllowHeaders(sipRequest);
         SipUtils.addAcceptHeader(sipRequest);
         sipRequest.setHeader(SipUtils.createTimeStampHeader());
         
         RegisterParam registerParams = RegisterParam.createRegisterParam();
         ContentTypeHeader ctHdr = headerFactory.createContentTypeHeader(
               ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
	 //BUG-317
         //if (force) 
            registerParams.setForceRegister(force);
   
         //if (confirm) 
            registerParams.setConfirmRegister(confirm);

         sipRequest.setContent(registerParams.toString(), ctHdr);
         //logger.debug("createHomeQueryRequest(1): force="+force+"  confirm="+confirm);
         //logger.debug("createHomeQueryRequest(2): registerParams="+registerParams);

	 // 18.4.1 CDisp Header
         SipUtils.checkContentLength( sipRequest);
         //logger.debug("createHomeQueryRequest(3): sipRequest=\n"+sipRequest);
         return sipRequest;

      } catch (Exception ex) {
         rfss.getTestHarness().fail("unexpected exception", ex);
         logger.fatal("unexpected exception ", ex);
         return null;
      }
   }

   /**
    * Create a register request and point it to the home rfss of the SU who
    * sent us a register.
    * 
    * SIP Register-Register is used as part of the Registration procedure to
    * provide a means for a serving RFSS to express interest in a mobility
    * object. The Register message is distinguished by the presence of a
    * contact parameter and a non-zero expires parameter.
    * 
    * SIP Register-Deregister is used by a serving RFSS to deregister a
    * mobility object or by a home RFSS to request deregistration of an SU. The
    * SIP Register-Deregister command is distinguished by the presence of a
    * contact parameter and a zero expires parameter. As with the SIP
    * Register-Query message, the SIP Register-Deregister message is used in
    * two separate procedures, the De-registration procedure and the Roamed
    * procedure. Again, the choice of procedure is dependant on the roles of
    * the RFSSs involved.
    * 
    * SIP Register-Query is sent by a serving RFSS to query a home RFSS for
    * service profile information, and by a home RFSS to confirm the interest
    * of a serving RFSS in a mobility object. It is distinguished by the
    * absence of both the expires parameter and the contact parameter. The SIP
    * Register-Query message is used in two procedures described in ?5.5, the
    * Home Query procedure and the Serving Query procedure. The content of the
    * message is consistent in each case, and the particular procedure is
    * determined by the roles of the participating RFSSs pursuant to the
    * mobility object involved.
    * 
    * @param groupConfig -- the group to register.
    * @param expiresTime -- the expire time.
    * @return -- the formatted Register request
    * 
    */
   private Request createRegisterRequest(GroupConfig groupConfig, int expiresTime) {
      try {
         String groupName = groupConfig.getRadicalName();
         SipURI fromURI = (SipURI) SipUtils.createGroupSipURI(groupName);

         Address fromAddress = addressFactory.createAddress(fromURI);
         FromHeader fromHeader = headerFactory.createFromHeader(fromAddress,
			 SipUtils.createTag());
   
         SipURI toURI = fromURI;
         Address toAddress = addressFactory.createAddress(toURI);
         ToHeader toHeader = headerFactory.createToHeader(toAddress, null);
   
         RfssConfig homeRfssConfig = groupConfig.getHomeRfss();
         String domainName = homeRfssConfig.getDomainName();
   
         // The request URI for Register requests has the form
         // sip:domain-name
         SipURI registrationURI = SipUtils.createDomainSipURI(domainName);
   
         // Create the via header for the outgoing request.
         ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
         LinkedList<ViaHeader> viaHeaders = new LinkedList<ViaHeader>();
         viaHeaders.add(viaHeader);
   
         // Create a maxforwards header for the outgoing request.
         MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
         CallIdHeader callIdHeader = provider.getNewCallId();
         CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1L,Request.REGISTER);
   
         Request sipRequest = messageFactory.createRequest(registrationURI,
               Request.REGISTER, callIdHeader, cseqHeader, fromHeader,
               toHeader, viaHeaders, maxForwards);
         
         SipUtils.addAllowHeaders(sipRequest);
         SipUtils.addAcceptHeader(sipRequest);
   
         ContactHeader contactHeader = SipUtils.createContactHeaderForGroup( groupConfig, rfssConfig);
         contactHeader.removeParameter("expires");
         sipRequest.addHeader(contactHeader);
   
         //RouteHeader routeHeader = SipUtils.createRouteToRfss(homeRfssConfig);
         //sipRequest.addHeader(routeHeader);
   
         ExpiresHeader expiresHeader = headerFactory.createExpiresHeader(expiresTime);
         sipRequest.addHeader(expiresHeader);
         sipRequest.setHeader( SipUtils.createTimeStampHeader());
         return sipRequest;

      } catch (Exception ex) {
         rfss.getTestHarness().fail("unexpected exception", ex);
         logger.fatal("unexpected exception ", ex);
         return null;
      }
   }

   
   private void processRegister(RequestEvent requestEvent, Request request, 
         GroupConfig groupConfig, GroupHome groupHome) throws Exception {
      // All the error handling is done at this point we can go ahead and
      // process the request.
      if (MobilityManager.isRegisterRegister(request) ||
          MobilityManager.isRegisterDeregister(request)) {

         ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
         Address address = contactHeader.getAddress();
         if (contactHeader.isWildCard()) {
            Response response = messageFactory.createResponse(Response.BAD_REQUEST, request);
            SipUtils.addAllowHeaders(response);
            provider.sendResponse(response);
            rfss.logError("Expected a valid address -- got wild card");
            return;
         }

         SipURI sipUri = (SipURI) address.getURI();
         String rfssId = SipUtils.getDomainName(sipUri);
         RfssConfig registeringRfss = topologyConfig.getRfssConfig(rfssId);
         if (registeringRfss == null) {
            Response response = messageFactory.createResponse( Response.NOT_FOUND, request);
            SipUtils.addAllowHeaders(response);
            provider.sendResponse(response);
            logger.debug("RFSS not found");
            return;
         }

         if (!groupHome.getSubscribers().contains(registeringRfss) &&
              MobilityManager.isRegisterDeregister(request)) {
            Response response = messageFactory.createResponse( Response.METHOD_NOT_ALLOWED, request);
            SipUtils.addAllowHeaders(response);
            provider.sendResponse(response);
            return;
         }
         if (MobilityManager.isRegisterRegister(request)) {
            // Add a subscriber to the group iff the registration lease time is non zero.
            if (rfssConfig.getGroupRegistrationExpiresTime() != 0) {
               groupHome.addSubscriber(registeringRfss);
            }

         } else if (MobilityManager.isRegisterDeregister(request)) {
            groupHome.removeSubscriber(registeringRfss);
         }

         // send OK response
	 // ---------------------------------------------------------------------------
         Response response = messageFactory.createResponse(Response.OK, request);
         //ContactHeader myContactHeader = SipUtils.createContactHeaderForRfss(rfssConfig,
         //      ISSIConstants.TIA_P25_SG);
	 //
//logger.debug("ZZZ processRegister: Change method: createContactHedareForGroup");
         ContactHeader myContactHeader = SipUtils.createContactHeaderForGroup(groupConfig,rfssConfig);
         response.setHeader(myContactHeader);
         response.addHeader(SipUtils.createContentDispositionHeader());
         
         ServiceProfile sp = groupConfig.getGroupServiceProfile();
         if (MobilityManager.isRegisterRegister(request)) {
            ContentTypeHeader ctHdr = headerFactory.createContentTypeHeader(
                  ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
            response.setContent(sp.toString(), ctHdr);
            if ( groupHome.isSubscribed(registeringRfss)) {
	       // some number.
               SipUtils.addExpiresHeader(response, ISSIConstants.EXPIRES_TIME_81400);
            } else {
               int expTime = 0;   // it was 0 - no registration
               logger.debug("+++ processRegister: expires=" + expTime);
	       // no registration record.
               SipUtils.addExpiresHeader(response, expTime);
            }
         } else {
            SipUtils.addExpiresHeader(response, 0);
            response.removeHeader(ContactHeader.NAME);
	 }
         SipUtils.addAllowHeaders(response);
	 SipUtils.checkContentLength(response);
         
         //showln("+++ processRegister: sendResponse=\n"+response);
         provider.sendResponse(response);

      } else {

         ContentList contentList = ContentList.getContentListFromMessage(request);
         
         Response response = messageFactory.createResponse(Response.OK, request);
         //ContactHeader myContactHeader = SipUtils.createContactHeaderForRfss(rfssConfig,
         //   ISSIConstants.TIA_P25_SG);
	 //
//logger.debug("ZZZ-2 processRegister: Change method: createContactHedareForGroup");
         ContactHeader myContactHeader = SipUtils.createContactHeaderForGroup(groupConfig,rfssConfig);
         response.setHeader(myContactHeader);
         response.addHeader(SipUtils.createContentDispositionHeader());
         SipUtils.addAllowHeaders(response);

	 // 18.4.1
         SipUtils.addExpiresHeader(response, ISSIConstants.EXPIRES_TIME_86400);

         ServiceProfile sp = groupConfig.getGroupServiceProfile();
         if (sp != null) {
            if (MobilityManager.isRegisterQuery(request) && 
                rfssConfig == groupConfig.getHomeRfss()) {
               ContentTypeHeader ctHdr = headerFactory.createContentTypeHeader(
                     ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
               response.setContent(sp.toString(), ctHdr);
            }
         }
         //18.5.1
         SipUtils.checkContentLength(response);
         provider.sendResponse(response);

         RegisterParam registerParam = null;
         if (contentList != null) {
            registerParam = contentList.getRegisterParamContent().getRegisterParam();
         }
//logger.debug("ZZZ-2 processRegister: registerParam="+registerParam);
         if ( registerParam != null && 
              rfssConfig != groupConfig.getHomeRfss() &&  
              registerParam.isForceRegister()) {

            // See if we have any SU's that are registered with the group.
            if ( groupRefcountTable.get(groupConfig.getRadicalName()) != null &&
                 groupRefcountTable.get(groupConfig.getRadicalName()).intValue() > 0 ) {
               Request registerRequest = createRegisterRequest(groupConfig, 
                    ISSIConstants.EXPIRES_TIME_86400);

               /*** #686 donot propagate
               //#595 17.2.1 - propergate force, confirm
               ContentTypeHeader ctHdr = headerFactory.createContentTypeHeader(
                  ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
               registerRequest.setContent(registerParam.toString(), ctHdr);
	        ***/

               SipUtils.checkContentLength( registerRequest);

               ClientTransaction ct = provider.getNewClientTransaction(registerRequest);
               PendingResponse pendingResponse = new PendingResponse ();
               pendingResponse.op = Operation.REGISTER;
               pendingResponse.ct = ct;
               ct.setApplicationData(pendingResponse);
               ct.sendRequest();
//logger.debug("ZZZ-2 processRegister: registerRequest=\n"+registerRequest);
            }  // forceRegister, refcount > 0
         }
      }
   }
   
   /**
    * Construct a group mobility manager.
    * 
    * @param rfss -- the RFSS to which I am assigned.
    */
   public GroupMobilityManager(RFSS rfss) {
      this.rfss = rfss;
      this.provider = rfss.getProvider();
      this.rfssConfig = rfss.getRfssConfig();
      this.topologyConfig = rfssConfig.getSysConfig().getTopologyConfig();
      this.myGroups = new Hashtable<String, GroupHome>();

      // Populate the tables for which we are responsible.
      for (Iterator<GroupConfig> groupIterator = rfssConfig.getAssignedGroups();
            groupIterator.hasNext(); ) {
         GroupConfig gc = groupIterator.next();
         String sessionId = gc.getRadicalName();
         PttPointToMultipointSession pttPointToMultipointSession = this.rfss
               .getTransmissionControlManager()
               .getPttPointToMultipointSession(sessionId, SessionType.GROUP_HOME);
         GroupHome groupHome = new GroupHome(rfss,pttPointToMultipointSession,provider,gc);
         myGroups.put(gc.getRadicalName(), groupHome);
         // Load the initial configuration into the group
         // This is the starting scenario for the test system.
         for (Iterator<SuConfig> it = gc.getSubscribers(); it.hasNext();) {
            SuConfig su = it.next();
            if (su.getInitialSuState() == SuState.ON) {
               RfssConfig servingRfss = su.getInitialServingRfss();
               if (servingRfss.getGroupRegistrationExpiresTime() != 0) {
                  groupHome.addSubscriber(servingRfss);
               }
            }
         }
      }

      // Populate the group refcount table.
      // The group refcount table tracks how many subscribers belong to a
      // given group in a serving RFSS.
      groupRefcountTable = new Hashtable<String, Integer>();

      for (SuConfig suConfig : topologyConfig.getSuConfigurations()) {
         if (suConfig.getInitialSuState() == SuState.ON) {
            // For all the RFSSs that we are minding, increment our
            // group reference count table for the groups for which this
            // rfss is a member.
            if (suConfig.getInitialServingRfss() == rfssConfig) {
               HashSet<GroupConfig> gcset = suConfig.getGroups();
               if (gcset != null) {
                  for (GroupConfig gc : gcset) {
                     String radicalName = gc.getRadicalName();
                     if (rfssConfig.getServedGroupLeaseTime() != 0) {
                        if (!groupRefcountTable.containsKey(radicalName)) {
                           groupRefcountTable.put(radicalName, new Integer(1));
                        } else {
                           Integer rc = groupRefcountTable.get(radicalName);
                           Integer newrc = new Integer( rc.intValue()+1);
                           groupRefcountTable.put(radicalName, newrc);
                        }
                     } else {
                        logger.debug(rfssConfig.getRfssName()
                           + " : Group with 0 lease time - not registering group");
                     }
                  }
               }
            }

         /*** too verbose !
         } else {
            logger.debug(rfssConfig.getRfssName()
               + " : Not inserting into the group configuration table -- su is off: "
	       +suConfig.getSuName());
          ***/
         }
      }
   }

   /**
    * Return true if this is a group which is known to the Serving Rfss.
    *
    * @param groupRadicalName -- the radical name of group
    */
   public boolean isKnownGroup(String groupRadicalName) {
      logger.debug(rfssConfig.getRfssName() + " isKnownGroup : " + groupRadicalName);
      return groupRefcountTable.containsKey(groupRadicalName);
   }
   
   /**
    * Handles group registration requests. This method updates the registered
    * RFSSs for a given group.
    *
    * @param requestEvent -- the request event.
    */
   public void processRequest(RequestEvent requestEvent) {
      Request request = requestEvent.getRequest();
      logger.debug("processRequest: request=\n"+request);

      assert request.getMethod().equals(Request.REGISTER);
      try {       
         ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
         toHeader.setTag( SipUtils.createTag());
         String groupName = ((SipURI) (toHeader.getAddress().getURI())).getUser();

         GroupConfig groupConfig = topologyConfig.getGroupConfig(groupName);
         if (groupConfig == null) {
            Response response = SipUtils.createErrorResponse( rfssConfig, request,
                  WarningCodes.NOT_FOUND_UNKNOWN_TARGET_GROUP);
            SipUtils.addAllowHeaders(response);
            provider.sendResponse(response);
            logger.error("processRequest: cannot find target group: "+groupName);
            return;
         }
         GroupHome group = myGroups.get(groupName);
         // Request is aimed at a group that I know nothing about.
         // Cannot deregister a group that I know nothing about.
         // This condition could happen when the RFSS is deliberately
         // misconfigured.

         if (group == null && groupConfig.getHomeRfss() == rfssConfig) {
            if (MobilityManager.isRegisterDeregister(request)) {
               Response response = messageFactory.createResponse(
                     Response.METHOD_NOT_ALLOWED, request);
               SipUtils.addAllowHeaders(response);
               provider.sendResponse(response);
            } else {
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.NOT_FOUND_UNKNOWN_TARGET_GROUP);
	       SipUtils.addAllowHeaders(response);
               provider.sendResponse(response);
               logger.error("processRequest: cannot find target group: "+groupName);
            }
            return;
         }

         String sender = ((ViaHeader) request.getHeader(ViaHeader.NAME)).getHost();
         RfssConfig sendingRfss = topologyConfig.getRfssConfig(sender);

         // Register query can only be issued by home to a group member.
         if (MobilityManager.isRegisterQuery(request)) {
            if (groupConfig.getHomeRfss() != rfssConfig
                  && sendingRfss != groupConfig.getHomeRfss()) {
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_SG_NOT_AUTHORIZED);
	       SipUtils.addAllowHeaders(response);
               provider.sendResponse(response);
               return;

            } else if (groupConfig.isQueryForbiddenFromRfss(sendingRfss)
                  && MobilityManager.isRegisterQuery(request)) {
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_SG_NOT_AUTHORIZED);
	       SipUtils.addAllowHeaders(response);
               provider.sendResponse(response);
               return;
            } else if (groupConfig.getHomeRfss() != rfssConfig &&
                  !groupRefcountTable.containsKey(groupConfig.getRadicalName())) {
               // I am a serving Rfss for this group but I have
               // no knowledge of the group.
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.NOT_FOUND_UNKNOWN_TARGET_GROUP);
	       SipUtils.addAllowHeaders(response);
               provider.sendResponse(response);
               return;
            }
         } else if (MobilityManager.isRegisterRegister(request)
               || MobilityManager.isRegisterDeregister(request)) {
            if (groupConfig.isRegisterForbiddenFromRfss(sendingRfss)
                  && MobilityManager.isRegisterRegister(request)) {
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.FORBIDDEN_SG_NOT_AUTHORIZED);
	       SipUtils.addAllowHeaders(response);
               provider.sendResponse(response);
               return;
            }
         } else {
            Response response = messageFactory.createResponse( Response.BAD_REQUEST, request);
	    SipUtils.addAllowHeaders(response);
            provider.sendResponse(response);
            logger.error("Could not classify request");
            return;
         }
         
         //If we are configured to do so
	 //send a session progress and then send the response.
         if ( rfssConfig.getRegisterConfirmTime() != 0 ) {
            Response response = messageFactory.createResponse(Response.SESSION_PROGRESS,request);
            WarningHeader warnHdr = SipUtils.createWarningHeader(rfssConfig,
               WarningCodes.SESSION_PROGRESS_QUERY_PROCESSING);
            response.setHeader(warnHdr);
	    SipUtils.addAllowHeaders(response);
            provider.sendResponse(response);

            ISSITimer.getTimer().schedule( new  TimerTask () {
               private RequestEvent requestEvent;
               private Request request;
               private GroupHome groupHome;
               private GroupConfig groupConfig;

               public TimerTask setParameters( RequestEvent requestEvent, Request request, 
                     GroupConfig groupConfig,GroupHome groupHome) {
                  this.requestEvent = requestEvent;
                  this.request = request;
                  this.groupHome = groupHome;
                  this.groupConfig = groupConfig;
                  return this;
               }
               @Override
               public void run() {
                  try {
                     GroupMobilityManager.this.processRegister(requestEvent,request,groupConfig,groupHome);
                  } catch (Exception e) {
                     logger.error("Unexpected exception", e);
                     GroupMobilityManager.this.rfss.getTestHarness().fail("Unexpected exception",e);
                  }
               }
               
            }.setParameters(requestEvent, request, groupConfig, group), this.rfssConfig.getRegisterConfirmTime()*1000);
            
         } else {
            this.processRegister(requestEvent, request, groupConfig, group);
         }
      } catch (Exception ex) {
         this.rfss.getTestHarness().fail("Unexpected exception ", ex);
         logger.fatal("Unexpected exception ", ex);
      }
   }

   /*
    * @see javax.sip.SipListener#processResponse(javax.sip.ResponseEvent)
    */
   public void processResponse(ResponseEvent responseEvent) {
      logger.debug("processResponse: START...");
      assert responseEvent.getClientTransaction() != null;
      PendingResponse pendingResponse = 
         (PendingResponse) responseEvent.getClientTransaction().getApplicationData();
      pendingResponse.response = responseEvent.getResponse();
   }

   /*
    * @see javax.sip.SipListener#processDialogTerminated(javax.sip.DialogTerminatedEvent)
    */
   public void processTimeout(TimeoutEvent timeoutEvent) {
      Request request = timeoutEvent.getClientTransaction() != null ? 
         timeoutEvent.getClientTransaction().getRequest() :
            timeoutEvent.getServerTransaction().getRequest(); 
      rfss.logError("Unexpected timeout event for request :\n" + request );
   }

   /*
    * @see javax.sip.SipListener#processIOException(javax.sip.IOExceptionEvent)
    */
   public void processIOException(IOExceptionEvent ioe) {
      rfss.logError("Unexpected IOException event");
   }

   public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
      logger.debug("processTransactionTerminated: ...");
   }

   /**
    * @see javax.sip.SipListener#processDialogTerminated(javax.sip.DialogTerminatedEvent)
    */
   public void processDialogTerminated(DialogTerminatedEvent arg0) {
      rfss.logError("Unexpected Dialog terminated event");
   }

   /**
    * The mobility manager invokes this method when an SU departs from a given
    * RFSS or when the Mobility manager detects that a SU has departed. Check
    * if the departure of the SU has resulted in any groups becoming empty. If
    * so then, send a deregistration request to the home RFSS of the given
    * group to deregister this RFSS from the home RFSS.
    * 
    * @param suConfig -- the SU configuration that is departing this RFSS
    */
   public synchronized void suDeparted(SuConfig suConfig) {
      logger.info("GroupMobilityManager: suDeparted=" + suConfig.getSuId());
      try {
         for (GroupConfig gc : suConfig.getGroups()) {
            Integer rc = groupRefcountTable.get(gc.getRadicalName());
            Integer newRc = new Integer(rc.intValue() - 1);
            if (newRc == 0) {
               groupRefcountTable.remove(gc.getRadicalName());

               // Generate a Register-Deregister message from the old location.
               Request request = this.createRegisterRequest(gc, 0);

               ClientTransaction ct = provider.getNewClientTransaction(request);
               PendingResponse pendingResponse = new PendingResponse();
               pendingResponse.op = Operation.DEREGISTER;
               pendingResponse.ct = ct;
               ct.setApplicationData(pendingResponse);
               ct.sendRequest();
            }
         }
      } catch (Exception ex) {
         logger.error("unexpected exception ", ex);
         this.rfss.getTestHarness().fail("Unexpected exception ", ex);
      }
   }

   /**
    * This methiod is invoked when an SU arrives at an RFSS or when a Mobility
    * manager detects that the SU has come alive. Check if there are any active
    * calls in progress for the given RFSS. If so do nothing. If not, send a
    * registration request to the home RFSS of the group to register this RFSS
    * with the group. Note that in this case the group home sends out the Re-INVITE
    * when we register for the group call.
    * 
    * @param suConfig -- the SU configuration that has arrived.
    */
   public synchronized void suArrived(SuConfig suConfig) {

      logger.debug("suArrived(): state="+suConfig.getSU().getState());
      if (suConfig.getSU().getState() == SuState.ON) {
         try {
            for (GroupConfig gc : suConfig.getSubscribedGroups()) {

               logger.debug("suArrived(): radicalName="+gc.getRadicalName());
               if (!groupRefcountTable.containsKey( gc.getRadicalName())) {

                  if (rfssConfig.isGroupCredentialsQueriedBeforeRegister()) {
                     ClientTransaction ct = sendRegisterQuery(gc);
                     while ( ct.getState() != TransactionState.TERMINATED) {
                        Thread.sleep(1000); // Let the OK come back for the query.
                     }
                     PendingResponse pendingResponse = (PendingResponse) ct.getApplicationData();
                     assert pendingResponse != null;
                     if ( pendingResponse.response == null) {
                        logger.error("suArrived(): Tx timed out maybe the peer died -- returning silently");
                        return;
                     }
                     if ( pendingResponse.response.getStatusCode()/100 > 2 ) {
                        logger.debug("Error in query -- returning without registering!");
                        return;
                     }
                  }
                  logger.debug("suArrived(): createRegisterRequest...");
                  Request request = createRegisterRequest(gc, ISSIConstants.EXPIRES_TIME_86400);
//--------------------------
                  //BUG-441:
                  boolean rpresence = suConfig.isCheckSuPresenceOnRegister();
                  logger.debug("suArrived(): rpresence="+rpresence);
                  /***
                  if( rpresence) {
                     RegisterParam registerParams = RegisterParam.createRegisterParam();
                     request.addHeader( SipUtils.createContentDispositionHeader());
                     ContentTypeHeader ctHdr = headerFactory.createContentTypeHeader(
                        ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
                     logger.debug("suArrived(): setConfirmPresence(true) *****");
                     registerParams.setConfirmPresence(true);
                     request.setContent(registerParams.toString(), ctHdr);
                  }
		   ***/
//--------------------------
                  SipUtils.checkContentLength( request);

                  // #688 9.2.x Reuse skipRegisterRegister
                  if( suConfig.isSkipRegisterRegister()) {
                     logger.debug("suArrived(9): SKIP REGISTER-REGISTER=\n"+request);
                     // 9.4.x
                     //Integer rc = new Integer(1);
                     //groupRefcountTable.put(gc.getRadicalName(), rc);

                  } else {
                     ClientTransaction ct = provider.getNewClientTransaction(request);
                     PendingResponse pendingResponse = new PendingResponse();
                     pendingResponse.ct = ct;
                     pendingResponse.op = Operation.REGISTER;
                     ct.sendRequest();
                     ct.setApplicationData(pendingResponse);
                     logger.debug("suArrived(9): send REGISTER=\n"+request);
                  }
                  //-------------------------------------------------
                  if (rfssConfig.getServedGroupLeaseTime() != 0) {
                     Integer rc = new Integer(1);
                     groupRefcountTable.put(gc.getRadicalName(), rc);
                  }

               } else {
                  logger.info("suArrived(9): skip register refcount=" + groupRefcountTable.get(gc.getRadicalName()));
               }
            }
         } catch (Exception ex) {
            logger.error("Unexpected exception ", ex);
            rfss.getTestHarness().fail("Unexpected exception", ex);
         }
      }
      logger.info("suArrived(): DONE...");
   }

   /**
    * Get a Group given the group radical name.
    * 
    * @param radicalName -- group radical name.
    * @return -- the Group Home.
    */
   public GroupHome getGroup(String radicalName) {
      return myGroups.get(radicalName);
   }
   public Collection<GroupHome> getGroups() {
      return myGroups.values();
   }

   /**
    * Send off a register query from a group.
    * 
    * @param sgConfig
    * @return -- the client transaction
    * @throws Exception
    */
   public ClientTransaction sendRegisterQuery(GroupConfig sgConfig) throws Exception {
      Request request = createRegisterRequest(sgConfig, 0);
      request.removeHeader(ExpiresHeader.NAME);
      request.removeHeader(ContactHeader.NAME);

      ClientTransaction ct = provider.getNewClientTransaction(request);
      PendingResponse pendingResponse = new PendingResponse();
      pendingResponse.op = Operation.QUERY;
      pendingResponse.ct = ct;
      ct.setApplicationData(pendingResponse);
      ct.sendRequest();
      return ct;
   }

   /**
    * Send off a query from a group to a member. This allows you to send
    * queries to any RFSS at all ( it should do some parameter checking but it
    * does not ).
    * 
    * @param sgConfig
    * @param targetRfss
    * @param force
    * @param confirm
    * @throws Exception
    */
   public void homeQueryGroup(GroupConfig sgConfig, RfssConfig targetRfss,
         boolean force, boolean confirm) throws Exception {

      Request request = createHomeQueryRequest(sgConfig, targetRfss, force, confirm);
      //logger.debug("homeQueryGroup(1): request=\n"+request);

      // 18.4.1 CDisp Header
      ClientTransaction ct = provider.getNewClientTransaction(request);
      PendingResponse pendingResponse = new PendingResponse();
      pendingResponse.op = Operation.HOME_QUERY;
      pendingResponse.ct = ct;
      ct.setApplicationData(pendingResponse);
      ct.sendRequest();
   }
}
