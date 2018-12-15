//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SuState;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25body.RegisterParamContent;
import gov.nist.p25.issi.p25body.params.RegisterParam;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfile;
import gov.nist.p25.issi.p25body.serviceprofile.user.UserServiceProfile;
import gov.nist.p25.issi.rfss.SipUtils;
import gov.nist.p25.issi.rfss.tester.TestSU;
import gov.nist.p25.issi.utils.ProtocolObjects;
import gov.nist.p25.issi.utils.WarningCodes;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.Vector;

import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
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
import javax.sip.address.URI;
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
 * Registrar that all registration requests and responses are directed to. This
 * will become a child SBB. The parent SBB will field the request create a child
 * SBB and fire SipEvent on the child sbb. This implements SipListener but is
 * not directly registered with the SIP stack as a listener. For testing on a
 * single machine the test program is the listener and de-multiplexes requests
 * and responses to one or more MobilityManagers using the provider as the event
 * source to de-multiplex the events.
 * 
 */
@SuppressWarnings("unchecked")
public class UnitToUnitMobilityManager implements SipListener {

   private static Logger logger = Logger.getLogger(UnitToUnitMobilityManager.class);
   public static void showln(String s) { System.out.println(s); }

   public static final int REGISTRATION = 1;
   public static final int DEREGISTRATION = 2;
   public static final int QUERY = 3;

   // time in seconds
   public static final int EXPIRES_TIME_MIN = 1;
   public static final int EXPIRES_TIME_MAX = 8*3600;

   private static final AddressFactory addressFactory = ProtocolObjects.addressFactory;
   private static final MessageFactory messageFactory = ProtocolObjects.messageFactory;
   private static final HeaderFactory headerFactory = ProtocolObjects.headerFactory;

   enum Procedure {
      REGISTRATION, DEREGISTRATION, QUERY, ROAMED;
   }

   /**
    * Registration table for the RFSS - this is initially populated by the
    * mappings for the RFSS ID from the static configuration.
    */
   private RegistrationsTable registrationsTable;
   private Hashtable<String, RfssConfig> previousRegistration;

   // System ( topology configuration ) and my own RFSS configuration
   private final RfssConfig rfssConfig;
   private final SipProvider provider;
   private final TopologyConfig topologyConfig;
   private RFSS rfss;

   class PendingResponse {
      SuInterface su;
      Procedure procedure;
      public Response response;
   }

   class QueryTimerTask extends TimerTask {
      private Request request;
      private SuConfig suConfig;
      private ServerTransaction serverTransaction;

      QueryTimerTask(ServerTransaction st, Request request, SuConfig suConfig) {
         this.serverTransaction = st;
         this.request = request;
         this.suConfig = suConfig;
      }
      public void run() {
         try {
            processRegisterQuery(serverTransaction, request, suConfig);
         } catch (Exception ex) { }
      }
   }

   // constructor
   public UnitToUnitMobilityManager(RFSS rfss) {
      this.rfss = rfss;
      rfssConfig = rfss.getRfssConfig();
      topologyConfig = rfssConfig.getSysConfig().getTopologyConfig();
      registrationsTable = new RegistrationsTable(this, topologyConfig);
      previousRegistration = new Hashtable<String, RfssConfig>();
      preLoadRegistrations();
      provider = rfss.getProvider();
   }

   /**
    * Get the RFSS for this U2U mobility manager.
    * 
    * @return the RFSS corresponding to this unit.
    */
   public RFSS getRFSS() {
      return rfss;
   }

   public Registration getRegistration(String key) {
      return (Registration) registrationsTable.getRegistrations().get(key);
   }

   /**
    * This is called upon initialization to preload all the RFSS configuration
    * information that belongs to this RFSS. The home RFSS keeps the profile
    * information of the SUs and Groups assigned to the RFSS.
    * 
    */
   private void preLoadRegistrations() {
      try {
         for (Iterator<SuConfig> mySu = rfssConfig.getHomeSubsciberUnits(); mySu.hasNext();) {
            SuConfig su = mySu.next();
            if (su.getHomeRfssRegistersMeFor() != 0) {
               ServiceProfile serviceProfile = su.getUserServiceProfile();
               String radicalName = su.getRadicalName();
               URI registrationURI = getCleanUri(SipUtils.createSUSipURI(radicalName));
               ContactHeader contactHeader = SipUtils.createContactHeaderForRfss(
                     su.getInitialServingRfss(), ISSIConstants.TIA_P25_SU);
               registrationsTable.createRegistration(registrationURI, contactHeader, serviceProfile);
               previousRegistration.put(su.getRadicalName(), su.getInitialServingRfss());
            }
         }
      } catch (Exception ex) {
         rfss.logError("Unexpected exception : ", ex);
         rfss.getTestHarness().fail("Unexpected exception", ex);
      }
   }

   //-----------------------------------------------------------------------------
   private void processRegisterQuery(ServerTransaction serverTransaction,
         Request request, SuConfig suConfig) throws Exception {
      String radicalName = suConfig.getRadicalName();
      Vector contactHeaders = getContactHeaders(radicalName);

      Response response = messageFactory.createResponse(Response.OK, request);
      if (logger.isDebugEnabled()) {
         logger.debug("processRegisterQuery(): request=\n"+request);
         logger.debug("processRegisterQuery(): radicalName="+radicalName);
         logger.debug("processRegisterQuery(): contactHeaders="+contactHeaders);
      }

      if (contactHeaders != null) {
        // #306 15.2.1 check if request has ContactHeader
        ContactHeader reqContactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
	if(reqContactHeader != null) {
         logger.debug("processRegisterQuery(1): uri.setUser="+radicalName);
         for (int i = 0; i < contactHeaders.size(); i++) {
            ContactHeader contact = (ContactHeader) contactHeaders.elementAt(i);
            logger.debug("processRegisterQuery(1): contact="+contact);
            //#470: suid@domainName.p25dr
            SipURI uri = (SipURI) contact.getAddress().getURI();
            uri.setUser(radicalName);
            response.addHeader(contact);
         }
        } else {
         logger.debug("processRegisterQuery(1): request doesnot contain Contact");
// Check if registered, add ContactHeader
logger.debug("processRegisterQuery(1): register="+ registrationsTable.hasRegistration(radicalName));
         // 15.2.x 200 OK remove ContactHeader
         if( !suConfig.isSkipRegisterRegister()) {
	    // #715 15.1.1 add contact header
            ContactHeader contactHeader = SipUtils.createContactHeaderForRfss(
               suConfig.getHomeRfss(), ISSIConstants.TIA_P25_SU);
            SipURI uri = (SipURI)contactHeader.getAddress().getURI();
            uri.setUser(radicalName);
            logger.debug("processRegisterQuery(1): contactHeader="+contactHeader);
            response.setHeader(contactHeader);
	 }
        }
      } else {
         // This is the Serving Rfss response.
         ContactHeader contactHeader = SipUtils.createContactHeaderForRfss(
               rfssConfig, ISSIConstants.TIA_P25_SU);
         SipURI uri = (SipURI) contactHeader.getAddress().getURI();
         uri.setUser(radicalName);
         logger.debug("processRegisterQuery(2): contactHeader="+contactHeader);
         response.setHeader(contactHeader);
      }
      ExpiresHeader expiresHeader = request.getExpires();
      if (expiresHeader != null) {
         response.addHeader(expiresHeader);
      } else {
         SipUtils.addExpiresHeader(response, ISSIConstants.EXPIRES_TIME_86400);
      }

      if (suConfig.getHomeRfss() == rfssConfig) {
         Registration registration = registrationsTable.getRegistration(radicalName);
         ServiceProfile serviceProfile = registration.getServiceProfile();
         if (serviceProfile != null) {
            response.addHeader( SipUtils.createContentDispositionHeader());
            ContentTypeHeader ctype = headerFactory.createContentTypeHeader(
                  ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
            response.setContent(serviceProfile.toString(), ctype);
         }
      } else {
         // nothing todo
      }

      SipUtils.addAllowHeaders(response);
      SipUtils.checkContentLength(response);

      if (serverTransaction != null) {
         serverTransaction.sendResponse(response);
      }

      if (logger.isDebugEnabled()) {
         logger.debug("processRegisterQuery(): response=\n" + response.toString());
      }

      // Dealing with the Register Force parameter.
      if (response.getStatusCode() == Response.OK &&
          request.getContentLength().getContentLength() != 0 &&
          suConfig.getHomeRfss() != rfssConfig)
      {
         ContentList contentList = ContentList.getContentListFromMessage(request);
         RegisterParamContent registerParamContent = contentList.getRegisterParamContent();
         if (registerParamContent != null) {
            RegisterParam registerParam = registerParamContent.getRegisterParam();
            if (registerParam.isForceRegister() && suConfig.isRegisterOnForce()) {
               sendRegisterRegister(suConfig);
            }
         }
      }
   }

   /**
    * Process the register message: add, remove, update the bindings and manage
    * also the expiration time.
    * 
    * @param requestEvent -- the request event
    * @param serverTransaction -- the server transaction
    */
   public synchronized void processRegister(RequestEvent requestEvent,
         ServerTransaction serverTransaction) {
      try {
         Request request = requestEvent.getRequest();
         String radicalName = getRadicalName(request);

         // Add the key if it is a new user:
         if (logger.isDebugEnabled()) {
            logger.debug("processRegister(): radicalName=" + radicalName);
         }
         ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
         SipURI toURI = (SipURI) toHeader.getAddress().getURI();
         SuConfig suConfig = topologyConfig.getSuConfig(radicalName);

         // Handle all the error cases.
         if (suConfig == null) {
            Response response = SipUtils.createErrorResponse(rfssConfig,
                  request, WarningCodes.NOT_FOUND_UNKNOWN_SU);
            serverTransaction.sendResponse(response);
            return;
         }
         ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
         String rfssDomainName = viaHeader.getHost();
         RfssConfig sendingRfss = topologyConfig.getRfssConfig(rfssDomainName);

         // Deregistration does not require a forbidden check. 
         // Only registration and query.
         if (suConfig.isRegistrationForbiddenFrom(sendingRfss)
               && MobilityManager.isRegisterRegister(request)) {
            Response response = SipUtils.createErrorResponse(rfssConfig, request,
                  WarningCodes.FORBIDDEN_SU_NOT_AUTHORIZED);
                  //#310 WarningCodes.FORBIDDEN_CALLING_SU_NOT_AUTHORIZED);
            logger.debug("processRegister: SU not authorized - RegisterRegister.");
            serverTransaction.sendResponse(response);
            return;
         }
         if (suConfig.isQueryForbiddenFrom(sendingRfss)
               && MobilityManager.isRegisterQuery(request)) {
            Response response = SipUtils.createErrorResponse(rfssConfig, request,
                  WarningCodes.FORBIDDEN_SU_NOT_AUTHORIZED);
            logger.debug("processRegister: SU not authorized - RegisterQuery.");
            serverTransaction.sendResponse(response);
            return;
         }
         //----------------------------------------------------------------------
         if (!rfss.getRfssConfig().isSubscriberUnitMine(suConfig)) {
            //SipProvider provider = (SipProvider) requestEvent.getSource();
            Response response = null;
            logger.debug("processRegister: MMM-000");

            if (MobilityManager.isRegisterDeregister(request)) {
               // Processing deregistration at the serving RFSS
               if (!rfss.isSubscriberUnitServed(suConfig)) {
                  logger.debug("processRegister: Deregistration Request SubscriberUnit is not served here "
                     + rfssConfig.getRfssId() + " sending OK response");
                  try {
                     response = messageFactory.createResponse(Response.OK, request);
                     SipUtils.addAllowHeaders( response);
                  } catch (Exception ex) {
                     logger.fatal("processRegister: Unexpected error creating response", ex);
                     return;
                  }
               }

            } else if (MobilityManager.isRegisterRegister(request)
                  || MobilityManager.isRegisterQuery(request)) {
               logger.debug("processRegister: MMM-222");
               if (rfss.isSubscriberUnitServed(suConfig)) {
                  logger.debug("processRegister: MMM-222-1");
                  // Check if the sender is the Home RFSS
                  if (!sendingRfss.isSubscriberUnitMine(suConfig)) {
                     /*
                      * This request can only be issued by the home Rfss
                      * to the serving Rfss. If the serving RFSS receives
                      * a query from a location that is not the home
                      * rfss, it responds with a FORBIDDEN
                      */
                     response = SipUtils.createErrorResponse( rfssConfig, request,
                           WarningCodes.FORBIDDEN_CALLING_SU_NOT_AUTHORIZED);
                  }
               } else if (!rfss.getRfssConfig().isSubscriberUnitMine(suConfig)) {
                  /*
                   * If not the serving Rfss, are we at the home Rfss of
                   * this SU? Cannot issue register register at an RFSS
                   * that is not the home rfss.
                   */
                  logger.debug("processRegister: MMM-222-2");
                  try {
                     response = SipUtils.createErrorResponse(rfssConfig, request,
                           WarningCodes.NOT_FOUND_UNKNOWN_SU);
                  } catch (Exception ex) {
                     logger.fatal("Unexpected error creating response", ex);
                     return;
                  }
               }
            }
            try {
               if (response != null) {
                  serverTransaction.sendResponse(response);
                  return;
               }
            } catch (SipException e) {
               rfss.getTestHarness().fail("Unexpected exception ", e);
               return;
            }
         } else {
            // I am the Home RFSS.
            if (MobilityManager.isRegisterDeregister(request)) {
               // I am the home RFSS for this SU but I dont have a
               // registration for this SU.
               Registration registration = registrationsTable.getRegistration(radicalName);
               logger.debug("processRegister: RFSS: " + rfssConfig.getRfssId() + " sending NOT_ALLOWED response");
               try {
                  // check if the contact is the presumed contact.
                  if (registration == null) {
                     Response response = messageFactory.createResponse(
                           Response.METHOD_NOT_ALLOWED, request);
                     SipUtils.addAllowHeaders(response);
                     SipUtils.checkContentLength(response);
                     serverTransaction.sendResponse(response);
                     return;
                  }
               } catch (Exception ex) {
                  logger.fatal("Unexpected error creating response", ex);
                  return;
               }
            } else if (MobilityManager.isRegisterQuery(request)) {
               Registration registration = registrationsTable.getRegistration(radicalName);
               if (registration == null) {
                  // See if I am the ServingRfss for this mobility object.
                  // Need to respond that we cannot handle query
                  Response newResponse = SipUtils.createErrorResponse( rfssConfig, request,
                        WarningCodes.NOT_FOUND_UNKNOWN_SU);
                  serverTransaction.sendResponse(newResponse);
                  return;
               }
            }
         }

         /*
          * SIP Register-Query is sent by a serving RFSS to query a home RFSS
          * for service profile information, and by a home RFSS to confirm
          * the interest of a serving RFSS in a mobility object. It is
          * distinguished by the absence of both the expires parameter and
          * the contact parameter. The SIP Register-Query message is used in
          * two procedures described in ?5.5, the Home Query procedure and
          * the Serving Query procedure. The content of the message is
          * consistent in each case, and the particular procedure is
          * determined by the roles of the participating RFSSs pursuant to
          * the mobility object involved.
          */
         if (MobilityManager.isRegisterQuery(request)) {
            logger.debug("processing registerQuery");
            ContentList clist = ContentList.getContentListFromMessage(request);
            RegisterParamContent registerParamContent = clist.getRegisterParamContent();
            if (registerParamContent != null) {
               RegisterParam registerParam = registerParamContent.getRegisterParam();
               /**
                * Register confirm means we have to ping the SU and make
                * sure the is still there. This could result in a delay.
                */
               if (registerParam.isConfirmRegister() &&
                   rfssConfig.getRegisterConfirmTime() != 0) {
                  Response response = messageFactory.createResponse(Response.SESSION_PROGRESS, request);
                  SipUtils.addAllowHeaders(response);
                  WarningHeader warnHdr = SipUtils.createWarningHeader(rfssConfig,
                     WarningCodes.SESSION_PROGRESS_QUERY_PROCESSING);
                  response.setHeader(warnHdr);

                  serverTransaction.sendResponse( response);
                  ISSITimer.getTimer().schedule(
                     new QueryTimerTask(serverTransaction, request, suConfig),
                     rfssConfig.getRegisterConfirmTime() * 1000);
               } else {
                  processRegisterQuery(serverTransaction, request, suConfig);
               }
            } else {
               processRegisterQuery(serverTransaction, request, suConfig);
            }

         } 
         //-------------------------------------------------------------------------------------
         else if (MobilityManager.isRegisterDeregister(request)) {
            logger.debug("process registerDeregister");
            /*
             * SIP Register-Deregister is used by a serving RFSS to
             * deregister a mobility object or by a home RFSS to request
             * deregistration of an SU. The SIP Register-Deregister command
             * is distinguished by the presence of a contact parameter and a
             * zero expires parameter. As with the SIP Register-Query
             * message, the SIP Register-Deregister message is used in two
             * separate procedures, the De-registration procedure and the
             * Roamed procedure. Again, the choice of procedure is dependant
             * on the roles of the RFSSs involved.
             */
            if (suConfig.getHomeRfss() == rfssConfig) {
               // Processing Register-deregister at the home RFSS
               if (logger.isDebugEnabled())
                  logger.debug("Processing a registerDeregister request at the home RFSS of SU "
                              + suConfig.getSuId());
               ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);

               // This is the calling home of the RFSS
               Registration current = registrationsTable.getRegistration(radicalName);
               if (current != null) {
                  registrationsTable.replaceContact(radicalName, contactHeader);
               } else {
                  registrationsTable.createRegistration(toURI, contactHeader, null);
               }
               Response response = messageFactory.createResponse( Response.OK, request);
               SipUtils.addAllowHeaders(response);
               SipUtils.addExpiresHeader( response, 0);
               SipUtils.checkContentLength(response);

               serverTransaction.sendResponse(response);

               // I am at the home so pass the request to the home agent of
               // the SU. he will do the right processing.
               HomeAgent homeAgent = rfss.getHomeAgent(suConfig.getRadicalName());
               assert homeAgent != null;

               RegisterEvent registerEvent = new RegisterEvent( rfssConfig.getRFSS(), requestEvent,
                     serverTransaction, RegisterEvent.REGISTER_DEREGISTER);
               if (rfssConfig == suConfig.getHomeRfss()) {
                  homeAgent.handleDeRegistrationEvent(registerEvent);
               }
               if (rfss.isSubscriberUnitServed(suConfig)) {
                  rfss.getTestHarness().assertTrue( request.getExpires().getExpires() == 0);
                  sendRegisterResponse(registerEvent, Response.OK);
                  //suConfig.getSU().handleMMRoamedEvent(registerEvent);
               }

            } else if (rfss.isSubscriberUnitServed(suConfig)) {
               if (logger.isDebugEnabled()) {
                  logger.debug("Processing RegisterDeRegister of SU at current Serving RFSS: "+
                               rfssConfig.getRfssName());
               }

               RegisterEvent registerEvent = new RegisterEvent( rfssConfig.getRFSS(), requestEvent,
                     serverTransaction, RegisterEvent.REGISTER_DEREGISTER);
               suConfig.getSU().handleDeRegistrationEvent(registerEvent);
               rfss.removeServedSubscriberUnit(suConfig);
               logger.debug("removeServedSubscriberUnit: suConfig="+suConfig);

               suConfig.getSU().handleMMRoamedEvent(registerEvent);
               sendRegisterResponse(registerEvent, Response.OK);
            } else {
               if (logger.isDebugEnabled())
                  logger.debug("Could not find  SU for the RegisterDeregister");
               Response response = SipUtils.createErrorResponse( rfssConfig, request,
                     WarningCodes.NOT_FOUND_UNKNOWN_SU);
               serverTransaction.sendResponse(response);
            }

         }
         //-------------------------------------------------------------------------------------
         else if (MobilityManager.isRegisterRegister(request)) {
            //logger.debug("process RegisterRegister");
            /*
             * SIP Register-Register is used as part of the Registration
             * procedure to provide a means for a serving RFSS to express
             * interest in a mobility object. The Register message is
             * distinguished by the presence of a contact parameter and a
             * non-zero expires parameter.
             */
            assert rfssConfig == suConfig.getHomeRfss();

            // Remove the existing registration record.
            Registration registration = registrationsTable.getRegistration(radicalName);
            ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
            ContactHeader oldContact = (registration != null ? registration.getContact() : null);

            if(logger.isDebugEnabled()) {
               logger.debug("processRegister(0): registration="+registration);
               logger.debug("processRegister(0): oldContact="+oldContact);
            }
            if (oldContact == null) {
               if (suConfig.getHomeRfssRegistersMeFor() != 0) {
                  Response response = SipUtils.createErrorResponse( rfssConfig, request,
                        WarningCodes.FORBIDDEN_SU_NOT_AUTHORIZED);
                  serverTransaction.sendResponse(response);
                  return;
               } else {
                  // We are configured to respond OK with 0 expires time
                  Response response = messageFactory.createResponse( Response.OK, request);
                  response.setHeader(contactHeader);
                  SipUtils.addExpiresHeader( response, 0);
                  SipUtils.addAllowHeaders(response);

                  UserServiceProfile sp = suConfig.getUserServiceProfile();
                  if (sp != null && !suConfig.isQueryForbiddenFrom(sendingRfss)) {
                     response.addHeader( SipUtils.createContentDispositionHeader());
                     ContentTypeHeader ctHdr = headerFactory.createContentTypeHeader(
                        ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
                     response.setContent(sp.toString(), ctHdr);
                  } else {
                     logger.debug("No Profile Information associated with the SU");
                  }
                  if (serverTransaction != null) {
                     SipUtils.checkContentLength(response);
                     serverTransaction.sendResponse(response);
                  }
                  return;
               }
            }

	    /***
            if(logger.isDebugEnabled()) {
               logger.debug("YYY processRegister(1): contactHeader="+contactHeader.getAddress().getURI());
               logger.debug("YYY processRegister(1): oldContact="+oldContact.getAddress().getURI());
            }
	     ***/

            // oldContact=Contact: <sip:f2.001.00001.p25dr;user=TIA-P25-SU>
            // contactHeader=sip:00002002000012@f2.001.00001.p25dr;user=TIA-P25-SU

            boolean contactChanged = false;
            //===if (!compareContactHeader(oldContact, contactHeader)) {
            if (!oldContact.getAddress().getURI().equals( contactHeader.getAddress().getURI())) {
               contactChanged = true;
               logger.debug("inserting a new contact for SU: " + Integer.toString(suConfig.getSuId(),16));
            }

            //logger.debug("processRegister: *********************************");
            //logger.debug("processRegister: >>> contactChanged="+contactChanged);  
            
            // Get the radical name
            if (contactChanged) {
               // Make sure that the r-presence flag is asserted on the new request
               RegisterParamContent registerParams = ContentList.getContentListFromMessage(request).
                     getRegisterParamContent();
               boolean rpresence = registerParams.getRegisterParam().isConfirmPresence();
               
               //logger.debug("processRegister: contactChanged-rpresence="+rpresence);
               if (!rpresence) {
                  Response response = SipUtils.createErrorResponse( rfssConfig, request,
                        WarningCodes.FORBIDDEN_PRESENCE_REQUIRED);
                  serverTransaction.sendResponse(response);
                  return;
               }
               HomeAgent homeAgent = rfss.getHomeAgent(suConfig.getRadicalName());
               RegisterEvent registerEvent = new RegisterEvent( rfssConfig.getRFSS(), requestEvent,
                     serverTransaction, RegisterEvent.REGISTER_REGISTER);
               //logger.debug("processRegister: homeAgent.handleRegistrationEvent="+suConfig.getRadicalName());
               homeAgent.handleRegistrationEvent(registerEvent);
            }
            registrationsTable.replaceContact(radicalName, contactHeader);
            Vector contactHeaders = getContactHeaders(radicalName);

            //TODO: need to re-factor out
            // create OK response
            //-----------------------------------------------------------------------
            Response response = messageFactory.createResponse(Response.OK, request);

	    // add ToHeader tag
	    ToHeader toHdr = (ToHeader) response.getHeader(ToHeader.NAME);
            toHdr.setTag( SipUtils.createTag());

            response.addHeader(request.getHeader(ExpiresHeader.NAME));

            //logger.debug("YYY processRegister: ContactHeader for radicalName="+radicalName);
            /*** 
            if (contactHeaders != null) {
               for (int i = 0; i < contactHeaders.size(); i++) {
                  ContactHeader contact = (ContactHeader) contactHeaders.elementAt(i);
                  //#463: suid@domainName.p25dr
                  SipURI uri = (SipURI) contact.getAddress().getURI();
                  uri.setUser(radicalName);
                  logger.debug("YYY processRegister:    contact="+contact);
                  response.addHeader(contact);
               }
            }
            ***/

            // create a contact as HomeRFSS          
            ContactHeader contactHdr = SipUtils.createContactHeaderForRfss(suConfig.getHomeRfss(),
                  ISSIConstants.TIA_P25_SU);
            SipURI uri = (SipURI)contactHdr.getAddress().getURI();
            uri.setUser(radicalName);
            response.addHeader(contactHdr);

            Registration reg = registrationsTable.getRegistration(radicalName);
            ServiceProfile sp = reg.getServiceProfile();
            if (sp != null && !suConfig.isQueryForbiddenFrom(sendingRfss)) {
               response.addHeader( SipUtils.createContentDispositionHeader());
               ContentTypeHeader ctHdr = headerFactory.createContentTypeHeader(
                  ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
               response.setContent(sp.toString(), ctHdr);
            } else {
               logger.debug("No Profile Information associated with the SU");
            }
            SipUtils.addAllowHeaders(response);
            SipUtils.checkContentLength( response);

            serverTransaction.sendResponse(response);
            if (logger.isDebugEnabled()) {
               logger.debug("Registrar, processRegister(), response sent:\n"+response);
            }

         } else {
            logger.error("Could not classify the request:\n" + request);
            logger.error("Dropping the request!");
            rfss.getTestHarness().fail("Could not classify register request " + request);
         }
      } catch (IOException ex) {
         if (logger.isDebugEnabled()) {
            logger.debug("Registrar exception raised:");
            logger.error(ex);
         }
      } catch (SipException ex) {
         if (logger.isDebugEnabled()) {
            logger.debug("Registrar exception raised:");
            logger.error(ex);
         }
      } catch (Exception ex) {
         if (logger.isDebugEnabled()) {
            logger.debug("Registrar, processRegister(), internal error, "
                  + "exception raised:");
            logger.error("Unexpected exception", ex);
         }
      }
   }

   public void sendRegisterRegister(SuConfig registeringSu) throws Exception {
      sendRegisterRegister(registeringSu, registeringSu.isCheckSuPresenceOnRegister());
   }

   /**
    * SIP Register-Register is used as part of the Registration procedure to
    * provide a means for a serving RFSS to express interest in a mobility
    * object. The Register message is distinguished by the presence of a
    * contact parameter and a non-zero expires parameter.
    * 
    * @param registeringSu -- the su that is registering
    * @param rpresence -- the flag to indicate presence.
    * @throws Exception
    */
   public void sendRegisterRegister(SuConfig registeringSu, boolean rpresence)
         throws Exception {

      //logger.debug("sendRegisterRegister(2): rpresence="+rpresence);
      Request newRequest = createRegisterRequest(registeringSu, ISSIConstants.EXPIRES_TIME_86400);

      //#311 make explicit
      //if (rpresence) 
      {
         RegisterParam registerParams = RegisterParam.createRegisterParam();
         // 4.1.1 doesnot required ContentDisp
         //>>>newRequest.addHeader( SipUtils.createContentDispositionHeader());
         ContentTypeHeader ctHdr = headerFactory.createContentTypeHeader(
            ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);

         //#471: Test 15.2.1 disabled r-presence
         //NOTE: Test 4.1.1 needs r-presence !!!
         logger.debug("sendRegisterRegister(): setConfirmPresence: "+rpresence);
         registerParams.setConfirmPresence(rpresence);
         newRequest.setContent(registerParams.toString(), ctHdr);
      }
      SipUtils.checkContentLength( newRequest);

      // #407
      if( registeringSu.isSkipRegisterRegister()) {
         logger.debug("sendRegisterRegister(9): SKIP REGISTER-REGISTER=\n"+newRequest);
	 return;
      }

      ClientTransaction ct = provider.getNewClientTransaction(newRequest);
      PendingResponse pendingResponse = new PendingResponse();
      pendingResponse.procedure = Procedure.REGISTRATION;
      pendingResponse.su = registeringSu.getSU();
      ct.setApplicationData(pendingResponse);
      ct.sendRequest();
      logger.debug("UnitToUnitMobilityManager: send REGISTER request=\n"+newRequest);
   }

   /**
    * These are invoked by the MM SAP. Send a deregistration request out. SIP
    * Register-Deregister is used by a serving RFSS to deregister a mobility
    * object or by a home RFSS to request deregistration of an SU. The SIP
    * Register-Deregister command is distinguished by the presence of a contact
    * parameter and a zero expires parameter. As with the SIP Register-Query
    * message, the SIP Register-Deregister message is used in two separate
    * procedures, the De-registration procedure and the Roamed procedure.
    * Again, the choice of procedure is dependant on the roles of the RFSSs
    * involved.
    * 
    * @param su -- Subscriber interface
    * @throws Exception
    */
   public void sendRegisterDeRegister(SuInterface su) throws Exception {
      SuConfig deRegisteringSu = su.getSuConfig();

      //logger.debug("sendRegisterDeRegister(): START...");
      Request newRequest = createRegisterRequest(deRegisteringSu, 0);
      SipUtils.checkContentLength( newRequest);
      logger.debug("sendRegisterDeRegister(): newRequest=\n"+newRequest);

      ClientTransaction ct = provider.getNewClientTransaction(newRequest);
      PendingResponse pendingResponse = new PendingResponse();
      pendingResponse.procedure = Procedure.DEREGISTRATION;
      pendingResponse.su = su;
      ct.setApplicationData(pendingResponse);
      ct.sendRequest();
   }

   /**
    * These are invoked by the MM SAP. Send a deregistration request out. SIP
    * Register-Deregister is used by a serving RFSS to deregister a mobility
    * object or by a home RFSS to request deregistration of an SU. The SIP
    * Register-Deregister command is distinguished by the presence of a contact
    * parameter and a zero expires parameter. As with the SIP Register-Query
    * message, the SIP Register-Deregister message is used in two separate
    * procedures, the De-registration procedure and the Roamed procedure.
    * Again, the choice of procedure is dependant on the roles of the RFSSs
    * involved.
    * 
    * @param deRegisteringSu -- the deregistring SU
    * @param newContact -- new location for the SU.
    * @throws Exception
    */
   public void sendMMRoamed(SuConfig deRegisteringSu, ContactHeader newContact,
      ContactHeader oldContact, String callId) throws Exception
   {
      Request newRequest = createRegisterRequest(deRegisteringSu, 0);
      previousRegistration.put(deRegisteringSu.getRadicalName(),
            registrationsTable.getRegistration( deRegisteringSu.getRadicalName()).getCurrentRfss());

      //RouteHeader route = headerFactory.createRouteHeader(oldContact.getAddress());
      //newRequest.setHeader(route);

      CallIdHeader callIdHeader = (CallIdHeader) newRequest.getHeader(CallIdHeader.NAME);
      if (callId != null) {
         callIdHeader.setCallId(callId);
      }

      //#224 12.23.1 Reg-DeReg ContactHeader
      logger.debug("sendMMRoamed(): Reg-DeReg-oldContact="+oldContact);
      logger.debug("sendMMRoamed(): Reg-DeReg-newContact="+newContact);
      SipURI newUri = (SipURI) newContact.getAddress().getURI();
      String newHost = newUri.getHost();
      logger.debug("sendMMRoamed(): Reg-DeReg-newHost="+newHost);

      //#303 8.2.1
      String suId = deRegisteringSu.getRadicalName();
      logger.debug("sendMMRoamed(): OLDCONTACT-setUser(): suId="+suId);
      SipURI oldUri = (SipURI) oldContact.getAddress().getURI();
      oldUri.setUser(suId);

      newRequest.setHeader(oldContact);
      //+++newRequest.setHeader(newContact);

      // fixup headers
      SipUtils.checkContentLength( newRequest);
      logger.debug("sendMMRoamed(): newRequest=\n"+newRequest);

      // send De-Register with Expires 0
      ClientTransaction ct = provider.getNewClientTransaction(newRequest);
      PendingResponse pendingResponse = new PendingResponse();
      pendingResponse.procedure = Procedure.ROAMED;
      pendingResponse.su = deRegisteringSu.getSU();
      ct.setApplicationData(pendingResponse);
      ct.sendRequest();
   }

   /**
    * SIP Register-Query is sent by a serving RFSS to query a home RFSS for
    * service profile information, and by a home RFSS to confirm the interest
    * of a serving RFSS in a mobility object. It is distinguished by the
    * absence of both the expires parameter and the contact parameter. The SIP
    * Register-Query message is used in two procedures described in ?5.5, the
    * Home Query procedure and the Serving Query procedure. The content of the
    * message is consistent in each case, and the particular procedure is
    * determined by the roles of the participating RFSSs pursuant to the
    * mobility object involved. Register Query may also be sent by the Home
    * RFSS for register Force or Register Query operations.
    * 
    * @param suConfig -- su to query.
    * @param force -- force flag
    * @param confirm -- confirm flag.
    * @throws Exception
    */
   public void sendRegisterQuery(SuConfig suConfig, boolean force,
         boolean confirm) throws Exception {

      //logger.debug("sendRegisterQuery(): START...");
      Request newRequest = createRegisterRequest(suConfig, 0);
      newRequest.removeHeader(ContactHeader.NAME);
      newRequest.removeHeader(ExpiresHeader.NAME);

      RegisterParam registerParams = RegisterParam.createRegisterParam();
      // #721, #722 explicit support
      //if (force)
      {
         registerParams.setForceRegister(force);
      }
      //if (confirm)
      {
         registerParams.setConfirmRegister(confirm);
      }
      ContentTypeHeader ctHdr = headerFactory.createContentTypeHeader(
         ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
      newRequest.setContent(registerParams.toString(), ctHdr);

      SipUtils.checkContentLength( newRequest);
      logger.debug("sendRegisterQuery(3): request=\n"+newRequest);

      PendingResponse pendingResponse = new PendingResponse();
      pendingResponse.procedure = Procedure.QUERY;
      ClientTransaction ct = provider.getNewClientTransaction(newRequest);
      ct.setApplicationData(pendingResponse);
      ct.sendRequest();
   }

   /**
    * Send a register query to the Home RFSS of the su from the serving Rfss to
    * determine its profile information.
    * 
    * @param suConfig -- Su to send query to.
    * @return -- the client transaction.
    * @throws Exception
    */
   public ClientTransaction sendRegisterQuery(SuConfig suConfig)
         throws Exception {
      Request newRequest = createRegisterRequest(suConfig, 0);
      newRequest.removeHeader(ContactHeader.NAME);
      newRequest.removeHeader(ExpiresHeader.NAME);
      SipUtils.checkContentLength( newRequest);
      logger.debug("sendRegisterQuery(1): request=\n"+newRequest);

      PendingResponse pendingResponse = new PendingResponse();
      pendingResponse.procedure = Procedure.QUERY;
      ClientTransaction ct = provider.getNewClientTransaction(newRequest);
      ct.setApplicationData(pendingResponse);
      ct.sendRequest();
      return ct;
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
    * @param suConfig
    * @param expiresTime
    * @return -- the SIP request
    * @throws Exception
    */
   private Request createRegisterRequest(SuConfig suConfig, int expiresTime)
         throws Exception {
      String suId = suConfig.getRadicalName();
      SipURI fromURI = (SipURI) SipUtils.createSUSipURI(suId);
      String tag = SipUtils.createTag();
      Address fromAddress = addressFactory.createAddress(fromURI);
      FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, tag);

      SipURI toURI = fromURI;
      Address toAddress = addressFactory.createAddress(toURI);
      ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

      RfssConfig homeRfssConfig = suConfig.getHomeRfss();
      RfssConfig sendTo = null;
      // Roaming procedure initiated by HOME RFSS of SU.
      // Home RFSS wants to de-register.
      if (suConfig.getHomeRfss() == rfssConfig && expiresTime == 0) {
         String key = suConfig.getRadicalName();
         sendTo = previousRegistration.get(key);
         if (sendTo == null) {
            sendTo = suConfig.getInitialServingRfss();
            logger.debug("sendTo -> inititalServingRfss...");
         }
      } else {
         // Roamed procedure initiated from serving RFSS.
         sendTo = homeRfssConfig;
         //logger.debug("sendTo -> homeRfssConfig...");
      }
      String domainName = sendTo.getDomainName();

      // The request URI for Register requests has the form sip:domain-name
      SipURI registrationURI = SipUtils.createDomainSipURI(domainName);

      // Create the via header for the outgoing request.
      LinkedList<ViaHeader> viaHeaders = SipUtils.createViaHeaderListForRfss(rfssConfig);
      //ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
      //LinkedList<ViaHeader> viaHeaders = new LinkedList<ViaHeader>();
      //viaHeaders.add(viaHeader);

      // Create a maxforwards header for the outgoing request.
      MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
      CallIdHeader callIdHeader = SipUtils.createCallIdHeader(getRFSS().getRfssConfig());
      CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1L, Request.REGISTER);

      Request sipRequest = messageFactory.createRequest(registrationURI,
            Request.REGISTER, callIdHeader, cseqHeader, fromHeader,
            toHeader, viaHeaders, maxForwards);
      SipUtils.addAllowHeaders( sipRequest);
      SipUtils.addAcceptHeader( sipRequest);

      //ContactHeader contactHeader = SipUtils.createContactHeaderForRfss(
      //      rfssConfig, ISSIConstants.TIA_P25_SU);

      //This will causes 4.1.1 extra SIP messages
      //SipURI contactURI = (SipURI) contactHeader.getAddress().getURI();
      //contactURI.setUser( suId);
      
      ContactHeader contactHeader = SipUtils.createContactHeaderForSU( rfssConfig, suConfig);

      contactHeader.removeParameter("expires");
      sipRequest.addHeader(contactHeader);

      //RouteHeader routeHeader = SipUtils.createRouteToRfss(sendTo);
      //sipRequest.addHeader(routeHeader);

      SipUtils.addExpiresHeader( sipRequest, expiresTime);
      sipRequest.setHeader( SipUtils.createTimeStampHeader());
      return sipRequest;
   }

   public static URI getCleanUri(URI uri) {
      if (uri instanceof SipURI) {
         SipURI sipURI = (SipURI) uri.clone();

         Iterator iterator = sipURI.getParameterNames();
         while (iterator != null && iterator.hasNext()) {
            String name = (String) iterator.next();
            sipURI.removeParameter(name);
         }
         return sipURI;
      } else {
         return uri;
      }
   }

   /**
    * The key is built following this rule: The mobilityManager extracts the
    * address-of-record from the To header field of the request. The URI MUST
    * then be converted to a canonical form. To do that, all URI parameters
    * MUST be removed (including the user-param), and any escaped characters
    * MUST be converted to their unescaped form. The result serves as an index
    * into the list of bindings
    *
    * @param request -- the Request
    * @return -- the radical name
    */
   private String getRadicalName(Request request) {
      ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
      return ((SipURI) toHeader.getAddress().getURI()).getUser();
   }

   /**
    * Extract the domain for the incoming invite To header and return an
    * iterator containing the URIs that are registered to this domain.
    * 
    * @param request -- the Request
    * @return -- an iterator of SipURI.
    */
   public Iterator<SipURI> getDomainContactURIs(Request request) {

      URI uri = request.getRequestURI();
      URI cleanedURI = getCleanUri(uri);

      if (!(cleanedURI instanceof SipURI))
         return null;

      // We have to check the host part:
      String host = ((SipURI) cleanedURI).getHost();
      Vector contacts = getContactHeaders("sip:" + host);
      if (contacts == null)
         return null;
      Vector<SipURI> results = new Vector<SipURI>();
      for (int i = 0; i < contacts.size(); i++) {
         ContactHeader contact = (ContactHeader) contacts.elementAt(i);
         Address address = contact.getAddress();
         uri = address.getURI();
         cleanedURI = getCleanUri(uri);
         results.addElement((SipURI) cleanedURI);
      }
      return results.iterator();
   }

   /**
    * Return true if the incoming request (usually invite) has a contact
    * registered.
    * 
    * @param request -- the request
    * @return -- true if we know of this request.
    * 
    */
   public boolean hasContactsRegistered(Request request) {
      String key = getRadicalName(request);
      return registrationsTable.hasRegistration(key);
   }

   /**
    * The result is a list of URI that we kept from a registration related to
    * the ToHeader URI from this request. These are used to route the request
    * to the next hop.
    * 
    * @param request -- incoming request (Usually an INVITE).
    * @return -- an iterator with the registered contact headers that are not
    *         expired.
    */
   public Iterator<URI> getRegisteredContactURIs(Request request) {
      String key = getRadicalName(request);
      Vector contacts = getContactHeaders(key);
      if (contacts == null) {
         logger.debug("no contacts found for key : " + key);
         return new Vector<URI>().iterator();
      }
      Vector<URI> results = new Vector<URI>();
      for (int i = 0; i < contacts.size(); i++) {
         ContactHeader contact = (ContactHeader) contacts.elementAt(i);
         Address address = contact.getAddress();
         URI uri = address.getURI();
         URI cleanedURI = getCleanUri(uri);
         results.addElement(cleanedURI);
      }
      return results.iterator();
   }

   public URI getRegisteredContactURI(SipURI requestUri) {
      String key = requestUri.getUser();
      Vector<ContactHeader> contacts = getContactHeaders(key);
      if (contacts == null) {
         return null;
      } else {
         return contacts.get(0).getAddress().getURI();
      }
   }

   public URI getRegisteredContactURI(Request request) {
      Iterator<URI> contacts = getRegisteredContactURIs(request);
      if (contacts.hasNext())
         return contacts.next();
      else
         return null;
   }

   private Vector<ContactHeader> getContactHeaders(String key) {
      if (registrationsTable.getRegistration(key) != null)
         return registrationsTable.getRegistration(key).getContactsList();
      else
         return null;
   }

   protected void printRegistrations() {
      registrationsTable.printRegistrations();
   }

   // implementation of SipListener
   //-------------------------------------------------------------------------------
   public void processRequest(RequestEvent requestEvent) {

      Request request = requestEvent.getRequest();
      assert request.getMethod().equals(Request.REGISTER);
      ServerTransaction serverTransaction = requestEvent.getServerTransaction();
      SipProvider sipProvider = (SipProvider) requestEvent.getSource();
      //FromHeader from = (FromHeader) request.getHeader(FromHeader.NAME);
      //SipURI fromUri = (SipURI) from.getAddress().getURI();
      //
      //logger.debug("processRequest(): \n"+request);
      if (serverTransaction == null) {
         try {
            serverTransaction = sipProvider.getNewServerTransaction(request);
            processRegister(requestEvent, serverTransaction);
         } catch (TransactionAlreadyExistsException e) {
            logger.error("could not create server transaction ", e);
         } catch (TransactionUnavailableException e) {
            logger.error("could not create server transaction", e);
         }
      }
   }

   public void processResponse(ResponseEvent responseEvent) {
      try {
         logger.debug("processResponse: got response from peer rfss -- responding to ServerTx");
         ClientTransaction ct = responseEvent.getClientTransaction();
         if (ct == null) {
            logger.error("Unexpected response - no client transaction -- discarding!");
            return;
         }
         PendingResponse pendingResponse = (PendingResponse) ct.getApplicationData();
         Response response = responseEvent.getResponse();
         pendingResponse.response = response;
         logger.debug(">>> U2UMobilityMgr: procedure="+pendingResponse.procedure);
         logger.debug(">>> U2UMobilityMgr: processResponse(): \n"+response);

         if (pendingResponse.procedure == Procedure.REGISTRATION) {
            //logger.debug(">>> U2UMobilityMgr: REGISTRATION...");
            RegistrationResponseEvent registrationResponseEvent = new RegistrationResponseEvent(
                  this, responseEvent.getResponse());
            // This will later become a SLEE fireEvent
            if (pendingResponse.su != null) {
               pendingResponse.su.handleRegistrationResponseEvent(registrationResponseEvent);
            }

         } else if (pendingResponse.procedure == Procedure.DEREGISTRATION) {
            //logger.debug(">>> U2UMobilityMgr: DEREGISTRATION...");
            DeregistrationResponseEvent registrationResponseEvent = new DeregistrationResponseEvent(
                  this, responseEvent.getResponse());

            // This will later become a SLEE fireEvent
            if (response.getStatusCode() == Response.OK) {
               String key = ((SipURI) ((ToHeader) response
                     .getHeader(ToHeader.NAME)).getAddress().getURI())
                     .getUser();
               logger.debug(">>> DEREGISTRATION: remove from table key="+key);
               registrationsTable.removeRegistration(key);
            }
            pendingResponse.su.handleDeRegistrationResponseEvent(registrationResponseEvent);

         } else if (pendingResponse.procedure == Procedure.ROAMED) {
            //logger.debug(">>> U2UMobilityMgr: ROAMED...");
            DeregistrationResponseEvent registrationResponseEvent = new DeregistrationResponseEvent(
                  this, responseEvent.getResponse());

            if(pendingResponse.su != null) {
               //logger.debug(">>> U2UMobilityMgr: su.handleMMRoamedResponseEvent()...");
               // This will later become a SLEE fireEvent
               pendingResponse.su.handleMMRoamedResponseEvent(registrationResponseEvent);
               pendingResponse.su = null;
               //logger.debug(">>> U2UMobilityMgr: su.handleMMRoamedResponseEvent()...null su...");
	    }
         }
      } catch (Exception ex) {
         String s = "Unexpected exception:";
         logger.error(s, ex);
         throw new RuntimeException(s, ex);
      }
   }

   public void sendRegisterResponse(RegisterEvent registerEvent, int statusCode)
         throws Exception {
      ServerTransaction st = registerEvent.getServerTransaction();
      Request request = st.getRequest();

      Response response = messageFactory.createResponse( statusCode, request);

      // Tag for ToHeader is mandatory in 2xx response.
      if (statusCode/100 == 2) {
         ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
         toHeader.setTag( SipUtils.createTag());
      }
      SipUtils.addAllowHeaders(response);
      SipUtils.addExpiresHeader(response, 0);
      SipUtils.checkContentLength( response);
      st.sendResponse(response);
   }

   public void processTimeout(TimeoutEvent timeoutEvent) {
      Request request = timeoutEvent.getClientTransaction() != null ? timeoutEvent
            .getClientTransaction().getRequest()
            : timeoutEvent.getServerTransaction().getRequest();
      rfss.logError("Unexpected timeout event for request :\n" + request);
   }

   public void processIOException(IOExceptionEvent ioExceptionEvent) {
      rfss.logError("io exception event occured!");
   }

   public void processTransactionTerminated(TransactionTerminatedEvent txe) {
      logger.debug("tx terminated event");
   }

   public void processDialogTerminated(DialogTerminatedEvent arg0) {
      logger.debug("dialog terminated event");
   }

   /**
    * Invoked when an SU departs from an RFSS or when the SU is turned OFF from
    * the ON state.
    * 
    * @param suConfig -- the SU configuration
    */
   public void suDeparted(SuConfig suConfig) {
      logger.debug("suDeparted from " + rfss.getRfssConfig().getHostPort());
      TestSU su = (TestSU) suConfig.getSU();
      try {
         if (su.getState() == SuState.OFF)
            sendRegisterDeRegister(su);
      } catch (Exception ex) {
         rfss.getTestHarness().fail("Unexpected exception ", ex);
      }
   }

   /**
    * Method is invoked when a SU arrives at a new RFSS or when the SU is
    * turned ON from the OFF state.
    * 
    * @param suConfig -- the SU configuration
    */
   public void suArrived(SuConfig suConfig) {
      try {
         if (logger.isDebugEnabled()) {
            logger.debug("suArrived at " + rfssConfig.getRfssName());
            logger.debug("queryCredentials ? " + rfssConfig.isSuCredentialsQueriedBeforeRegister());
            logger.debug("isSubscriberUnitServed ? " + rfss.isSubscriberUnitServed(suConfig));
         }

         TestSU su = (TestSU) suConfig.getSU();
         if (rfssConfig.isSuCredentialsQueriedBeforeRegister()) {
            ClientTransaction ct = sendRegisterQuery(suConfig);
            while (ct.getState() != TransactionState.TERMINATED) {
               Thread.sleep(1000); // Wait for a response
            }
            PendingResponse pendingResponse = (PendingResponse) ct.getApplicationData();
            if (pendingResponse == null) {
               logger.debug("Transaction terminated - before response received! Possible timeout!");
               return;
            }
            if (pendingResponse.response.getStatusCode() / 100 != 2) {
               logger.debug("Did not get a positive response from the home -- returning! ");
               return;
            }
         }
         sendRegisterRegister(suConfig);
         su.clearCallSegment();

      } catch (Exception ex) {
         rfss.getTestHarness().fail("Unexpected exception ", ex);
         logger.error("unexpected exception", ex);
      }
   }

   // same as HomeAgent
   private boolean compareContactHeader(ContactHeader oldContact, ContactHeader contactHeader)
   {
      logger.debug("compareContactHeader: oldContact="+oldContact.toString());
      logger.debug("compareContactHeader: contactHdr="+contactHeader.toString());

      // old contact == Contact: <sip:02.001.00001.p25dr;user=TIA-P25-SU>
      // new contact == Contact: <sip:00002002000012@02.001.00001.p25dr;user=TIA-P25-SU>
      boolean match = false;
      String prefix = "Contact: <sip:";
      int oldIdx = oldContact.toString().indexOf("@");
      int newIdx = contactHeader.toString().indexOf("@");
      //logger.debug(" >>>>>> oldIdx=" + oldIdx + " newIdx=" + newIdx);
      if( oldIdx == -1) {
         if( newIdx == -1) {
            match = oldContact.toString().equals(contactHeader.toString());
         }
         else {
            // 02.001.00001.p25dr;user=TIA-P25-SU>
            String newStr = prefix + contactHeader.toString().substring(newIdx+1);
            match = oldContact.toString().trim().equals(newStr.trim());
            logger.debug("newStr=[" + newStr +"]");
         }
      }
      else {
         if( newIdx == -1) {
            String oldStr = prefix + oldContact.toString().substring(oldIdx+1);
            match = contactHeader.toString().trim().equals(oldStr.trim());
            logger.debug("oldStr=[" + oldStr +"]");
         }
         else {
            match = oldContact.toString().equals(contactHeader.toString());
         }
      }
      logger.debug(" >>>>>> match=" + match);
      return match;
   }
}
