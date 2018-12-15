//
package gov.nist.p25.issi.rfss;

import gov.nist.javax.sdp.fields.OriginField;
import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25body.SdpContent;
import gov.nist.p25.issi.utils.ProtocolObjects;
import gov.nist.p25.issi.utils.WarningCodes;

import java.util.LinkedList;
import java.util.ListIterator;
import javax.sdp.MediaDescription;
import javax.sdp.SessionDescription;
import javax.sip.InvalidArgumentException;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import java.text.ParseException;
import org.apache.log4j.Logger;

/**
 * ISSI and SIP utilities class.
 */
public class SipUtils {

   private static Logger logger = Logger.getLogger(SipUtils.class);
   public static void showln(String s) { System.out.println(s); }

   private static AddressFactory addressFactory = ProtocolObjects.getAddressFactory();
   private static HeaderFactory headerFactory = ProtocolObjects.getHeaderFactory();
   private static MessageFactory messageFactory = ProtocolObjects.getMessageFactory();
   
   //-------------------------------------------------------------------------
   public static SipURI createSipURI(String radicalName, String domainName)
         throws ParseException {
      SipURI sipURI = addressFactory.createSipURI(radicalName, domainName);
      return sipURI;
   }

   /**
    * Creaete and return a SIPUri corresponding to a SU.
    * 
    * @param radicalName --
    *            Radical name for the SU.
    * @return -- the SIP URI corresponding to the SU.
    * @throws ParseException
    */
   public static URI createSUSipURI(String radicalName) throws ParseException {

      SipURI sipUri = createSipURI(radicalName, ISSIConstants.P25DR);
      sipUri.setParameter("user", ISSIConstants.TIA_P25_SU);
      return sipUri;
   }

   /**
    * Create and return a SIP URI corresponding to a group.
    * 
    * @param radicalName --
    *            group radical name.
    * @return -- the SIP URI for the group.
    * @throws ParseException
    */
   public static URI createGroupSipURI(String radicalName) throws ParseException {
      SipURI sipUri = createSipURI(radicalName, ISSIConstants.P25DR);
      sipUri.setUserParam(ISSIConstants.TIA_P25_SG);
      return sipUri;
   }

   // accessors
   //--------------------------------------------------------------------------
   public static boolean isSubscriberUnit(String user) {
      return ISSIConstants.TIA_P25_SU.equals(user); 
   }
   public static boolean isSubscriberUnit(SipURI sipUri) {
      //String user = sipUri.getParameter(ISSIConstants.USER);
      String user = sipUri.getUserParam();
      return ISSIConstants.TIA_P25_SU.equals(user); 
   }

   public static boolean isGroup(String user) {
      // TIA_P25_SG
      return ISSIConstants.TIA_P25_SG.equals(user);
   }
   public static boolean isGroup(SipURI sipUri) {
      String user = sipUri.getUserParam();
      return ISSIConstants.TIA_P25_SG.equals(user);
   }

   public static boolean isHostP25Dr(String host) {
      return ISSIConstants.P25DR.equals(host);
   }
   public static boolean isHostP25Dr(SipURI sipUri) {
      String host = sipUri.getHost();
      return ISSIConstants.P25DR.equals(host);
   }

   //-----------------
   /*** Not used ?
   public static boolean isP25GroupCall(String user) {
      //TIA-P25-Groupcall
      return ISSIConstants.P25_GROUP_CALL.equals(user);
   }
   public static boolean isP25GroupCall(SipURI sipUri) {
      String user = sipUri.getUser();
      return ISSIConstants.P25_GROUP_CALL.equals(user);
   }
    ***/

   //-----------------
   public static boolean isCallingHomeRFSS(URI sipURI) {
      // This allows a RFSS to know it should be behave as a calling home RFSS.
      String user = ((SipURI) sipURI).getUser();
      return ISSIConstants.CALLING_HOME_NAME.equals(user);
   }

   public static boolean isCalledHomeRFSS(URI sipURI) {
      // This allows an RFSS to know it should behave as a called home RFSS.
      String user = ((SipURI) sipURI).getUser();
      return ISSIConstants.CALLED_HOME_NAME.equals(user);
   }

   // used by CallControlManager
   public static boolean isU2UCall(Message message) {
      ToHeader toHeader = (ToHeader) message.getHeader(ToHeader.NAME);
      SipURI sipUri = (SipURI) toHeader.getAddress().getURI();
      return SipUtils.isSubscriberUnit(sipUri);
   }

   public static boolean isGroupCall(Message message) {
      ToHeader toHeader = (ToHeader) message.getHeader(ToHeader.NAME);
      SipURI sipUri = (SipURI) toHeader.getAddress().getURI();
      return SipUtils.isGroup(sipUri);
   }

   public static String getGroupID(URI sipURI) {
      String gidString = ((SipURI) sipURI).getUser();
      return gidString;
   }

   //--------------------------------------------------------------------------
   public static String createTag() {
      int rc = (int) (Math.random() * 10000) + 13;
      return Integer.toString( rc);
   }
   
   public static TimeStampHeader createTimeStampHeader() 
      throws InvalidArgumentException
   {
      TimeStampHeader tsHeader = headerFactory.createTimeStampHeader(0);
      tsHeader.setTime(System.currentTimeMillis());
      return tsHeader;
   }

   public static ContentTypeHeader createAppSdpContentTypeHeader()
      throws ParseException {
      return headerFactory.createContentTypeHeader(
               ISSIConstants.APPLICATION, ISSIConstants.SDP);
   }

   public static boolean isAppSdpContentTypeHeader(ContentTypeHeader ctHeader)
   {
      if (ISSIConstants.APPLICATION.equals(ctHeader.getContentType()) &&
          ISSIConstants.SDP.equals(ctHeader.getContentSubType()))
          return true;
      return false;
   }

   /** Not used yet ?
   public static boolean isMultipartMixedContentTypeHeader(ContentTypeHeader ctHeader)
   {
      if (ISSIConstants.TAG_MULTIPART.equals(ctHeader.getContentType()) &&
          ISSIConstants.TAG_MIXED.equals(ctHeader.getContentSubType())) 
          return true;
      return false;
   }
    **/

   /**
    * Create and return an LR parameter route header for outgoing invites to
    * route requests to a given RFSS.
    * 
    * @param rfssConfig --
    *            rfss configuration where we want to route requests.
    * 
    * @return
    * @throws Exception
    */
   public static RouteHeader createRouteToRfss(RfssConfig rfssConfig) {
      try {
         String ipAddress = rfssConfig.getDomainName();
         SipURI homeRfssURI = addressFactory.createSipURI(null, ipAddress);

         // homeRfssURI.setPort(rfssConfig.getPort());
         homeRfssURI.setLrParam();
         Address homeRfssAddress = addressFactory.createAddress(homeRfssURI);
         RouteHeader route = headerFactory.createRouteHeader(homeRfssAddress);
         return route;
      } catch (Exception ex) {
         logger.error("Internal error ", ex);
         throw new RuntimeException("Internal error", ex);
      }
   }

   /**
    * Create a contact address for the SU at the Serving RFSS
    */
   public static ContactHeader createContactHeaderForSU(RfssConfig rfssConfig,
         SuConfig suConfig) {
      try {
         ContactHeader retval = createContactHeaderForRfss(rfssConfig, null);
         SipURI contactURI = (SipURI) retval.getAddress().getURI();
         String radicalName = suConfig.getRadicalName();
         contactURI.setUser(radicalName);
         contactURI.setUserParam(ISSIConstants.TIA_P25_SU);
         Address contactAddress = addressFactory.createAddress(contactURI);
//logger.debug("createContactHeadreForSU: uri="+contactURI+" radicalName="+radicalName);
         ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
         return contactHeader;
      } catch (Exception ex) {
         logger.error("Internal error ", ex);
         throw new RuntimeException("Internal error", ex);
      }
   }

   public static String getRadicalName(SipURI sipUri) {
      return sipUri.getUser();
   }

   public static SipURI createDomainSipURI(String domainName) throws ParseException {
      //logger.debug("ZZZ createDomainSipURI: domainName="+domainName);
      return addressFactory.createSipURI(null, domainName);
   }

   public static SipURI createDomainSipURI(RfssConfig rfssConfig) throws ParseException {
      String domainName = rfssConfig.getDomainName();
      //logger.debug("ZZZ createDomainSipURI: rfss-domainName="+domainName);
      return createDomainSipURI(domainName);
   }

   public static ContactHeader createContactHeaderForRfss(RfssConfig rfssConfig, String userParam) {
      try {
         SipURI contactURI = SipUtils.createDomainSipURI(rfssConfig);
         Address contactAddress = addressFactory.createAddress(contactURI);
//logger.debug("ZZZ createContactHeaderForRfss: contactURI="+contactURI);
         ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
         if (userParam != null) {
            contactURI.setUserParam(userParam);
	 }
         return contactHeader;
      } catch (Exception ex) {
         logger.error("Internal error ", ex);
         throw new RuntimeException("Internal error", ex);
      }
   }

   /**
    * Create a record route header for this RFSS (this is used when forwarding
    * requests down via a set of RFSSs)
    * 
    * @param rfssConfig
    * @return
    */
   public static RecordRouteHeader createRecordRouteHeaderForRfss(RfssConfig rfssConfig) {
      try {
         SipURI contactURI = SipUtils.createDomainSipURI(rfssConfig);
         contactURI.setLrParam();
         Address contactAddress = addressFactory.createAddress(contactURI);
         RecordRouteHeader recordRouteHeader = headerFactory.createRecordRouteHeader(contactAddress);
         return recordRouteHeader;
      } catch (Exception ex) {
         logger.error("Internal error ", ex);
         throw new RuntimeException("Internal error", ex);
      }
   }

   public static WarningHeader checkIncomingRequest(Request request) {
      try {
         ContentTypeHeader ctHeader = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
	 if( ctHeader != null) 
         if (ISSIConstants.TAG_MULTIPART.equals(ctHeader.getContentType()) &&
             ISSIConstants.TAG_MIXED.equals(ctHeader.getContentSubType()))
         {
            String boundary = ctHeader.getParameter("boundary");
            if (boundary == null) {
               WarningHeader warningHeader = headerFactory.createWarningHeader(
                     "RFSS",
                     WarningHeader.MISCELLANEOUS_WARNING,
                     ISSIConstants.WARN_TEXT_MISSING_REQUIRED_PARAMETER);
               return warningHeader;
            }
         }
         return null;

      } catch (Exception ex) {
         logger.fatal("Exception encountered while checking request : " + request);
         logger.fatal("internal error !", ex);
         throw new RuntimeException( "checkIncomingRequest: Internal error occured", ex);
      }
   }

   public static WarningHeader createWarningHeader(RfssConfig rfssConfig, WarningCodes warnCode) {
      try {
         WarningHeader warningHeader = headerFactory.createWarningHeader(
               rfssConfig.getDomainName(), warnCode.getWarnCode(), warnCode.getWarnText());
         return warningHeader;
      } catch (Exception ex) {
         logger.error("unexpected exception ", ex);
         throw new RuntimeException("unexpected exception ", ex);
      }
   }

   /**
    * Get the RFSS Domain name from a registered URI.
    * 
    * @param registeredUri
    * @return
    */
   public static String getDomainName(SipURI registeredUri) {
      return registeredUri.getHost();
   }

   public static String getCallIdFromMessage(Message message) {
      return ((CallIdHeader) message.getHeader(CallIdHeader.NAME)).getCallId();
   }

   public static ViaHeader createViaHeaderForRfss(RfssConfig config) {
      try {
         ViaHeader viaHeader = headerFactory.createViaHeader(config.getDomainName(),
               -1, "UDP", null);
         return viaHeader;
      } catch (Exception ex) {
         ex.printStackTrace();
         return null;
      }
   }
   public static LinkedList<ViaHeader> createViaHeaderListForRfss(RfssConfig rfssConfig) {
      ViaHeader viaHeader = SipUtils.createViaHeaderForRfss(rfssConfig);
      LinkedList<ViaHeader> viaHeaders = new LinkedList<ViaHeader>();
      if( viaHeader != null) {
         viaHeaders.add(viaHeader);
      }
      return viaHeaders;
   }

   /**
    * Create an error response with the appropriate warning header.
    * 
    * @param request
    * @param warningCode
    * @return
    */
   public static Response createErrorResponse(RfssConfig rfssConfig, Request request, WarningCodes warningCode) {
      try {
         Response response = messageFactory.createResponse(warningCode.getRc(), request);
         WarningHeader warnHdr = SipUtils.createWarningHeader(rfssConfig, warningCode);
         response.setHeader(warnHdr);
         addAllowHeaders(response);

         if( warningCode.getRc() == Response.FORBIDDEN) {
            //logger.debug("SipUtils(): createErrorResponse: addTag()...");
            SipUtils.addToHeaderTag(response);
         }
         response.setHeader(SipUtils.createTimeStampHeader());
         return response;

      } catch (Exception ex) {
         logger.error("Unexpected exception", ex);
         return null;
      }
   }

   public static ContactHeader createContactHeaderForGroup( GroupConfig calledGroup, RfssConfig rfssConfig)
         throws Exception {
      ContactHeader contact = createContactHeaderForRfss(rfssConfig, ISSIConstants.TIA_P25_SG);
      SipURI uri = (SipURI) contact.getAddress().getURI();
      uri.setUser(calledGroup.getRadicalName());
logger.debug("createContactHeadreForGroup: uri="+uri+" user="+calledGroup.getRadicalName());
      return contact;
   }

   public static int getPriorityValue(PriorityHeader priorityHeader) {
      if (priorityHeader != null) {
         String priorityString = priorityHeader.getPriority();
         String[] args = priorityString.split(";");
         if( args != null && args.length > 0)
            return Integer.parseInt(args[0]);
      }
      return 1;
   }

   public static boolean isEmergency(PriorityHeader priorityHeader) {
      if (priorityHeader != null) {
         String priorityString = priorityHeader.getPriority();
         String[] args = priorityString.split(";");
	 if( args != null && args.length > 1)
            return "e".equals(args[1]);
      }
      return false;
   }

   /**
    * 
    * @param domainName
    * @return
    */
   public static int getRfssId(String domainName) {
      String[] args = domainName.split("\\.");
      if( args != null && args.length > 2)
         return Integer.parseInt( args[2], 16);
      return 0;
   }

   /**
    * Add necessary Allow headers to a Message.
    * 
    * @param message --
    *            message to enhance.
    */
   public static void addAllowHeaders(Message message) {
      //logger.debug("addAllowHeaders: message="+message);

      // per spec - arrange in alphabetical order
      for (String allow : new String[] { Request.ACK, Request.BYE,
            Request.CANCEL, Request.INVITE, Request.REGISTER })
      {
         boolean addFlag = true;
	 ListIterator list = message.getHeaders(AllowHeader.NAME);
         if (list != null) {
            while( list.hasNext()) {
               String header = ((AllowHeader)list.next()).getMethod();
               if( allow.equals( header)) {
                  addFlag = false;
		  break;
               }
	    }
         }
	 //logger.debug("addAllow: "+allow+"  addFlag="+addFlag);
	 if( addFlag) {
            try {
               AllowHeader allowHeader = headerFactory.createAllowHeader(allow);
               message.addHeader(allowHeader);
            } catch (ParseException e) {
               logger.fatal("Unexpected exception", e);
            }
         }
      }
   }
   
   // Add tag to ToHeader
   public static void addToHeaderTag(Message message) throws ParseException {
      ToHeader header = (ToHeader) message.getHeader(ToHeader.NAME);
      //logger.debug("SipUtils(): addToHeader: header="+header);
      if( header != null) {
         header.setTag( createTag());
         //logger.debug("SipUtils(): setTag: header="+header);
      }
   }
   
   public static boolean addToHeaderTagByStatusCode(Response response) 
         throws ParseException {
      boolean bflag = false;
      ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
      // Tag for to header is mandatory in 2xx response.
      if( toHeader != null) {
         if (response.getStatusCode()/100==2 && toHeader.getTag()==null) {
            toHeader.setTag( createTag());
            bflag = true;
         }
      }
      return bflag;
   }
   
   public static void addFromHeaderTag(Message message) throws ParseException {
      FromHeader header = (FromHeader) message.getHeader(FromHeader.NAME);
      if( header != null) {
         header.setTag( createTag());
      }
   }   

   /**
    * Extract the host part of the contact address and return it.
    * 
    * @param message
    * @return
    */
   public static String getContactHost(Message message) {
      return ((SipURI) ((ContactHeader) message.getHeader(ContactHeader.NAME))
            .getAddress().getURI()).getHost().toLowerCase();
   }

   public static String getSessionIdFromMessage(Message message) {
      try {
         ContentList contentList = ContentList.getContentListFromMessage(message);
         SdpContent sdpContent = (SdpContent) ContentList.getContentByType(
               contentList, ISSIConstants.APPLICATION, ISSIConstants.SDP);
         if (sdpContent == null)
            return null;
         SessionDescription sdes = sdpContent.getSessionDescription();
         return ((OriginField) sdes.getOrigin()).getSessIdAsString();
      } catch (ParseException ex) {
         logger.fatal("Unexpected exception ", ex);
         return null;
      }

   }

   public static int getMediaPortFromMessage(Message message) throws Exception {
      ContentList contentList = ContentList.getContentListFromMessage(message);
      SdpContent sdpContent = (SdpContent) ContentList.getContentByType(
            contentList, ISSIConstants.APPLICATION, ISSIConstants.SDP);
      if (sdpContent == null)
         throw new Exception("Could not find session description");
      SessionDescription sdes = sdpContent.getSessionDescription();
      MediaDescription mdes = (MediaDescription) sdes.getMediaDescriptions(false).get(0);
      return mdes.getMedia().getMediaPort();
   }

   public static SessionDescription getSessionDescriptionFromMessage( Message message)
         throws Exception {
      ContentList contentList = ContentList.getContentListFromMessage(message);
      SdpContent sdpContent = (SdpContent) ContentList.getContentByType(
            contentList, ISSIConstants.APPLICATION, ISSIConstants.SDP);
      return sdpContent.getSessionDescription();
   }

   public static String getRtpSessionIdFromMessage(Message message)
         throws Exception {
      ContentList contentList = ContentList.getContentListFromMessage(message);
      SdpContent sdpContent = (SdpContent) ContentList.getContentByType(
            contentList, ISSIConstants.APPLICATION, ISSIConstants.SDP);
      return ((OriginField) sdpContent.getSessionDescription().getOrigin())
            .getSessIdAsString();
   }

   public static String getGroupIdFromMessage(Message message) {
      FromHeader fromheader = (FromHeader) message.getHeader(FromHeader.NAME);
      SipURI sipUri = (SipURI) fromheader.getAddress().getURI();
      return getGroupID(sipUri);
   }

   public static int comparePriorityHeaders(PriorityHeader header1, PriorityHeader header2) {
         boolean emergency1 = SipUtils.isEmergency(header1);
         int priority1 = SipUtils.getPriorityValue(header1);
         
         boolean emergency2 = SipUtils.isEmergency(header2);
         int priority2 = SipUtils.getPriorityValue(header2);
         
         logger.debug("isHigherPriority : emergency = " + emergency1
               + " priority = " + priority1 + " currentCallIsEmergency =  "
               + emergency2 + " currentCallPriority = "
               + priority2);

         if (emergency1 == emergency2) {
            if (priority2 > priority1) return 1;
            else if (priority2 == priority1) return 0;
            else return -1;
            
         } else if (emergency2) {
            return 1;
         } else {
            return -1;
         }
   }

   public static String getViaHost(Request request) {
      ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
      return viaHeader.getHost();
   }

   //---------------------------------------------------------------------
   public static void addPriorityHeader(Request request, int priority, boolean isEmergency)
         throws ParseException
   {
      String pri = Integer.toString(priority) + ";" + (isEmergency ? "e" : "a");

      PriorityHeader priorityHeader = (PriorityHeader) request.getHeader(PriorityHeader.NAME);
      if( priorityHeader != null) {
         priorityHeader.setPriority( pri);
	 request.setHeader( priorityHeader);
         //showln("updatePriorityHeader: pri="+pri);
      }
      else {
         priorityHeader = headerFactory.createPriorityHeader( pri);
         request.addHeader(priorityHeader);
         //showln("newPriorityHeader: pri="+pri);
      }
   }

   public static void addAcceptHeader(Request request)
         throws ParseException 
   {
      AcceptHeader acceptHeader = headerFactory.createAcceptHeader(
         ISSIConstants.APPLICATION, ISSIConstants.SDP);
      acceptHeader.setParameter("level", "1");
      request.addHeader(acceptHeader);
      acceptHeader = headerFactory.createAcceptHeader(
         ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
      request.addHeader(acceptHeader);
   }

   public static void addExpiresHeader(Message message, int expires)
      throws InvalidArgumentException
   {
      ExpiresHeader expiresHeader = headerFactory.createExpiresHeader(expires);
      message.setHeader(expiresHeader);
   }

   public static ContentDispositionHeader createContentDispositionHeader()
         throws ParseException {
      ContentDispositionHeader cd = headerFactory.createContentDispositionHeader(
         ContentDispositionHeader.SESSION);
      cd.setHandling("required");
      return cd;
   }
   
   public static void checkContentLength(Message request)
         throws InvalidArgumentException, ParseException
   {
      //showln("checkContentLength(): before fixup request=\n"+request);
      request.setHeader(SipUtils.createTimeStampHeader());
      ContentLengthHeader clHeader = request.getContentLength();
      if( clHeader.getContentLength()==0) {
         request.removeHeader(ContentDispositionHeader.NAME);
         request.removeHeader(MimeVersionHeader.NAME);
      }
      else {
         // non-zero contentLength
         ContentTypeHeader ctHeader = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
	 if( ctHeader != null) { 
//logger.debug(" HHH content="+ctHeader.getContentType()+":"+ctHeader.getContentSubType());
            if (ISSIConstants.APPLICATION.equals(ctHeader.getContentType())) {
               if( ISSIConstants.SDP.equals(ctHeader.getContentSubType())) {
                  request.removeHeader(MimeVersionHeader.NAME);
                  //+++request.removeHeader(ContentDispositionHeader.NAME);
logger.debug(" --- DSP: remove MIME...add CDISP");
                  // 12.23.1 INVITE
                  request.setHeader( createContentDispositionHeader());
               }
	       else if( ISSIConstants.X_TIA_P25_ISSI.equals(ctHeader.getContentSubType())) {
                  request.removeHeader(MimeVersionHeader.NAME);
                  // 18.3.1
                  request.setHeader( createContentDispositionHeader());
//logger.debug(" --- x-tia-p25-issi: set ContentDisp...");
               }
            }
            else if (ISSIConstants.TAG_MULTIPART.equals(ctHeader.getContentType()) &&
                     ISSIConstants.TAG_MIXED.equals(ctHeader.getContentSubType()))
	    {
//logger.debug(" --- multipart: mixed ...");
               MimeVersionHeader mv = headerFactory.createMimeVersionHeader(1,0);
               request.addHeader(mv);
               request.setHeader( createContentDispositionHeader());
            }
            else {
//logger.debug(" --- else: remove MIME ...");
               request.removeHeader(MimeVersionHeader.NAME);           
            }
	 }
      }

      // filter response here
      //-------------------------------------------------------
      if( request instanceof Response) {
         Response response = (Response) request;
         int rc = response.getStatusCode();
         // 200 OK or 2xx
	 // if(rc/100 == 2) { 
         if(rc == Response.OK) {
            CSeqHeader cseq = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
            String seqmethod = cseq.getMethod();

            ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
            if("REGISTER".equals(seqmethod)) {
               // 12.26.x De-register: Allow Header
	       /***
               if(expiresHeader != null && expiresHeader.getExpires() == 0) {
                  request.removeHeader(AllowHeader.NAME);
               }
	        **/
            }
	    else if("BYE".equals(seqmethod)) {
               // Apply rule: 
               // for u2u:
               // 1. if BYE from Calling Serving, then has Allow
               //       in BYE and 200 OK message
               // 2. if BYE from Called Serving, then no Allow
               //       in BYE and 200 OK message
               //if( isU2UCall(request)) {
               //   request.removeHeader(AllowHeader.NAME);
               //}
            }
         }
      }
   }

   //-------------------------------------------------------------------------
   public static void filterSipHeaders(Request request)
         throws InvalidArgumentException
   {
      request.setHeader(SipUtils.createTimeStampHeader());
      //
      // cleanup request based on TIA-102.BACA-A: Table 10
      //
      String method = request.getMethod();
      if( Request.ACK.equals(method)) {
         // Not Applicable
         request.removeHeader(AcceptHeader.NAME);
         request.removeHeader(AllowHeader.NAME);
         request.removeHeader(ContactHeader.NAME);
         request.removeHeader(ExpiresHeader.NAME);
         request.removeHeader(PriorityHeader.NAME);
         request.removeHeader(RecordRouteHeader.NAME);
         request.removeHeader(WarningHeader.NAME);               

         // optional
	 // can not remove due to daisy chain acks
         //request.removeHeader(RouteHeader.NAME);

	 /*** List of Routes
	 ListIterator li = request.getHeaders(RouteHeader.NAME);
         if (li != null && li.hasNext()) {
            RouteHeader route = (RouteHeader) li.next();
            request.removeHeader(route);
         }
	  ***/
      }
      else if( Request.BYE.equals(method)) {
         // Not Applicable
         request.removeHeader(AcceptHeader.NAME);
         request.removeHeader(ContactHeader.NAME);
         request.removeHeader(ExpiresHeader.NAME);
         request.removeHeader(PriorityHeader.NAME);

	 // Apply rule: as above
         //if( isU2UCall(request)) {
         //   request.removeHeader(AllowHeader.NAME);
         //}

         // optional
         request.removeHeader(WarningHeader.NAME);               
         request.removeHeader(RecordRouteHeader.NAME);
	 // Cannot remove due to Daisy chain BYE
	 // 13.1.1
         //request.removeHeader(RouteHeader.NAME);               

	 // 12.23.x BYE
	 addAllowHeaders( request);
      }
      else if( Request.CANCEL.equals(method)) {
         // Not Applicable
         request.removeHeader(AcceptHeader.NAME);
         request.removeHeader(AllowHeader.NAME);
         request.removeHeader(ContactHeader.NAME);
         request.removeHeader(ContentDispositionHeader.NAME);
         request.removeHeader(ContentTypeHeader.NAME);
         request.removeHeader(ExpiresHeader.NAME);
         request.removeHeader(MimeVersionHeader.NAME);
         request.removeHeader(PriorityHeader.NAME);

         // optional
         request.removeHeader(RecordRouteHeader.NAME);
         request.removeHeader(RouteHeader.NAME);               
         request.removeHeader(WarningHeader.NAME);               
      }
      else if( Request.INVITE.equals(method)) {
         // Accept 2xx     
	 // 
      }
      else if( Request.REGISTER.equals(method)) {
         // 2xx
         //request.removeHeader(AcceptHeader.NAME);
         // 1xx
         //request.removeHeader(ContactHeader.NAME);

         request.removeHeader(PriorityHeader.NAME);
         request.removeHeader(RecordRouteHeader.NAME);
      }
   }
   //--------------------------------
   public static CallIdHeader createCallIdHeader(RfssConfig rfssConfig)
      throws ParseException
   {
      return headerFactory.createCallIdHeader( createCallId( rfssConfig));
   }
   public static String createCallId(RfssConfig rfssConfig)
   {
      return System.currentTimeMillis() +".1@" +rfssConfig.getDomainName();
   }

   //-------------------------------------------------------------------------
   public static String createSdpBody(long sessId, long sessVersion, String type,
      String ipAddress, int port)
   {
      // type: P25-GROUP-CALL or TIA-P25-U2UCALL
      // NOTE: s, c, t order
      // testcase 9.8.1
      String sdpBody = "v=0\r\n" 
         + "o=- " + Long.toString(sessId) + " "
         + Long.toString(sessVersion) + " IN IP4 " + ipAddress + "\r\n" 
         + "s=" + type + "\r\n"
         + "c=IN IP4 " + ipAddress + "\r\n" 
         + "t=0 0\r\n" 
         + "m=audio " + port + " RTP/AVP 100\r\n"
         + "a=rtpmap:100 X-TIA-P25-IMBE/8000\r\n";
      return sdpBody;
   }
   //-------------------------------------------------------------------------
}
