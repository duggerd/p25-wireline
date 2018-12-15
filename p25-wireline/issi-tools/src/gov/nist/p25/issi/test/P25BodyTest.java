//
package gov.nist.p25.issi.test;

import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.GlobalTopologyParser;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfigParser;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25body.GroupServiceProfileContent;
import gov.nist.p25.issi.p25body.SdpContent;
import gov.nist.p25.issi.rfss.SipUtils;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfigurationParser;

import java.util.LinkedList;
import javax.sdp.SessionDescription;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
//import javax.sip.header.AcceptHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import junit.framework.TestCase;


public class P25BodyTest extends TestCase {
   private static Logger logger = Logger.getLogger(P25BodyTest.class);
   public static void showln(String s) { System.out.println(s); }
   
   private SipFactory sipFactory;
   private MessageFactory messageFactory;
   private HeaderFactory headerFactory;
   private TopologyConfig topologyConfig;
   private AddressFactory addressFactory;
   private RfssConfig rfssConfig;
   private SuConfig callingSu;
   private SuConfig calledSu;

   private static String sdpAnnounce = "v=0\r\n"
         + "o=- 0003578 0 IN IP4 192.53.18.122\r\n"
         + "s=TIA-P25-Groupcall\r\n" 
         + "c=IN IP4 192.53.18.122\r\n" 
         + "t=0 0\r\n"
         + "m=audio 49298 RTP/AVP 100\r\n"
         + "a=rtpmap:100 X-TIA-P25-IMBE/8000\r\n";

   private static String sdpAnnounce1 = "v=0\r\n"
         + "o=- 0003578 0 IN IP4 127.0.0.1\r\n" 
         + "s=TIA-P25-Groupcall\r\n"
         + "c=IN IP4 127.0.0.1\r\n"
         + "t=0 0\r\n" 
         + "m=audio 25000 RTP/AVP 100\r\n"
         + "a=rtpmap:100 X-TIA-P25-IMBE/8000\r\n";

   private static String groupServiceProfile = "g-access:1\r\n"
         + "g-agroup:0B4561A27271\r\n" + "g-pri:2\r\n" + "g-ecap:1\r\n"
         + "g-eprempt:0\r\n" + "g-rfhangt:0\r\n" + "g-ccsetupT:0\r\n"
         + "g-intmode:0\r\n" + "g-sec:1\r\n" + "g-ic:0\r\n"
         + "g-icsecstart:0\r\n";

   //private static String separator = "--P25 ISSI body--";

   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Set up an outgoing invite
    * 
    * @param callingSu
    * @param calledSu
    * @return
    * @throws Exception
    */
   private Request createInvite(SuConfig callingSu, SuConfig calledSu)
         throws Exception {
      String transport = "udp";
      // create my SIP URI. This goes into the From Header.
      String radicalName = callingSu.getRadicalName();
      String domainName = ISSIConstants.P25DR;
      SipURI callingSipURI = SipUtils.createSipURI(radicalName, domainName);
      String tag = SipUtils.createTag();
      Address fromAddress = addressFactory.createAddress(callingSipURI);
      FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, tag);

      // Create the SIP URI for the Called RFSS.
      radicalName = calledSu.getRadicalName();
      domainName = ISSIConstants.P25DR;
      SipURI calledSipURI = SipUtils.createSipURI(radicalName, domainName);
      Address toAddress = addressFactory.createAddress(calledSipURI);
      ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

      String myAddress = this.rfssConfig.getIpAddress();
      int myPort = this.rfssConfig.getSipPort();

      SipURI contactSipURI = addressFactory.createSipURI(null, myAddress);
      contactSipURI.setPort(myPort);
      contactSipURI.setTransportParam(transport);

      Address contactAddress = addressFactory.createAddress(contactSipURI);
      ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);

      ViaHeader viaHeader = headerFactory.createViaHeader(myAddress, myPort, "UDP", null);
      LinkedList<ViaHeader> viaHeaders = new LinkedList<ViaHeader>();
      viaHeaders.add(viaHeader);

      // My home Serving RFSS.
      RfssConfig callingHomeRfssConfig = callingSu.getHomeRfss();

      RouteHeader routeHeader = SipUtils.createRouteToRfss(callingHomeRfssConfig);
      // Get a new call ID header for the outgoing invite.
      CallIdHeader callIdHeader = headerFactory.createCallIdHeader("123456");
      CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1L,Request.INVITE);

      MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

      Request sipRequest = messageFactory.createRequest(calledSipURI,
            Request.INVITE, callIdHeader, cseqHeader, fromHeader, toHeader,
            viaHeaders, maxForwardsHeader);
      sipRequest.addHeader(routeHeader);
      sipRequest.addHeader(contactHeader);
      SipUtils.addAllowHeaders( sipRequest);
      
      //======================
      showln("P25BodyTest: createINVITE()- ContentDispositionHeader");
      ContentTypeHeader ctHeader = headerFactory.createContentTypeHeader(
            ISSIConstants.APPLICATION, ISSIConstants.SDP);

      sipRequest.addHeader(ctHeader); 
      ContentDispositionHeader cdHeader = 
         headerFactory.createContentDispositionHeader(ContentDispositionHeader.SESSION); 
      cdHeader.setHandling("required"); 
      sipRequest.addHeader(cdHeader); 
      //======================

      SipUtils.addAcceptHeader( sipRequest);
      /***
      AcceptHeader acceptHeader = headerFactory.createAcceptHeader(
         ISSIConstants.APPLICATION, ISSIConstants.SDP);
      acceptHeader.setParameter("level", "1");
      sipRequest.addHeader(acceptHeader);
      sipRequest.addHeader(headerFactory.createAcceptHeader(
            ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI));
       ***/
      return sipRequest;
   }

   public void setUp() throws Exception {

      sipFactory = SipFactory.getInstance();
      sipFactory.setPathName("gov.nist");

      headerFactory = sipFactory.createHeaderFactory();
      addressFactory = sipFactory.createAddressFactory();
      messageFactory = sipFactory.createMessageFactory();
      ISSITesterConfiguration config = new ISSITesterConfigurationParser("unit-test/issi-tester-configuration.xml").parse();
      
      TopologyConfig systemTopology = new SystemTopologyParser(config).parse("unit-test/systemtopology.xml");
      TopologyConfig globalTopology = new GlobalTopologyParser(false)
            .parse(systemTopology,"testscripts/globaltopology.xml");
      topologyConfig = new TopologyConfigParser().parse(globalTopology,
            "src/gov/nist/p25/issi/issiconfig/topologytest.xml");
      this.rfssConfig = topologyConfig.getRfssConfig(1, 2, 1);
      this.callingSu = topologyConfig.getSuConfig(1, 2, 0x12);
      this.calledSu = topologyConfig.getSuConfig(1, 2, 0x34);
      assertTrue(rfssConfig != null);
      assertTrue(callingSu != null);
      assertTrue(calledSu != null);
   }

   public void testContentUnpack() throws Exception {
      try {
         Request inviteRequest = this.createInvite(callingSu, calledSu);

         ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader(
            ISSIConstants.APPLICATION, ISSIConstants.SDP);
         inviteRequest.setContent(sdpAnnounce, contentTypeHeader);

         ContentList clist = ContentList.getContentListFromMessage(inviteRequest);
         SdpContent content = (SdpContent) clist.getContent(
            ISSIConstants.APPLICATION, ISSIConstants.SDP);
         SessionDescription sessionDescription = content.getSessionDescription();
         assertNotNull(sessionDescription);
         assertNotNull(content.getContentTypeHeader());
         assertNull(content.getBoundary());

         logger.info("session description = " + sessionDescription);
         logger.info("content type header  = " + content.getContentTypeHeader());
         logger.info("content encoded to string = " + content.toString());

      } catch (Exception ex) {

         logger.error("unexpected error unpacking content", ex);
         fail("unexpected error ");
      } finally {
         logger.info("testContentUnpack(): completed");
      }

   }

   public void testContentUnpack1() {
      try {
         Request inviteRequest = this.createInvite(callingSu, calledSu);
         ContentTypeHeader multipartMimeCt = headerFactory.createContentTypeHeader("multipart", "mixed");
         multipartMimeCt.setParameter("boundary", ISSIConstants.BODY_BOUNDARY);
         inviteRequest.setHeader(multipartMimeCt);

         ContentList contentList = new ContentList();
         ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader(
            ISSIConstants.APPLICATION, ISSIConstants.SDP);
         SdpContent sdpContent = new SdpContent(contentTypeHeader, sdpAnnounce);
         assertTrue(sdpContent != null);
         contentList.add(sdpContent);

         GroupServiceProfileContent gspContent = GroupServiceProfileContent
               .createGroupServiceProfileContent(groupServiceProfile);

         contentList.add(gspContent);
         // Try packing it.
         String body1 = contentList.toString();
         logger.debug("body = " + body1);

         inviteRequest.removeContent();
         inviteRequest.setContent(body1, multipartMimeCt);

         logger.debug("Invite request after resetting content = " + inviteRequest.toString());

         // get the content list from message again.

         ContentList clist = ContentList
               .getContentListFromMessage(inviteRequest);
         assertTrue(clist != null);
         String body = clist.toString();

         assertFalse(body == null || body.equals(""));

         logger.debug("content to set = " + body);

         inviteRequest.removeContent();
         inviteRequest.setContent(body, multipartMimeCt);

         logger.debug("Invite request after resetting content again = "
               + inviteRequest.toString());

      } catch (Exception ex) {
         logger.error("Unexpected error unpacking the content ", ex);
         fail("Unexpected error ");
      }
   }

   public void testMatch() {
      try {
         ContentList contentList = new ContentList();
         ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader(
            ISSIConstants.APPLICATION, ISSIConstants.SDP);

         SdpContent sdpContent = new SdpContent(contentTypeHeader, sdpAnnounce);
         contentList.add(sdpContent);

         GroupServiceProfileContent gspContent = GroupServiceProfileContent
               .createGroupServiceProfileContent(groupServiceProfile);
         contentList.add(gspContent);
         logger.debug("contentList = " + contentList.toString());

         ContentList contentList1 = new ContentList();
         ContentTypeHeader contentTypeHeader1 = headerFactory.createContentTypeHeader(
            ISSIConstants.APPLICATION, ISSIConstants.SDP);
         SdpContent sdpContent1 = new SdpContent(contentTypeHeader1, sdpAnnounce1);
         contentList.add(sdpContent1);

         GroupServiceProfileContent gspContent1 = GroupServiceProfileContent
               .createGroupServiceProfileContent(groupServiceProfile);
         contentList1.add(gspContent1);
         contentList1.add(sdpContent1);
         logger.debug("contentList1 = " + contentList1);
         
         assertTrue("Content list mismatch", contentList.match(contentList1));
         assertTrue("Content list mismatch 1 ", contentList1.match(contentList));

      } catch (Exception ex) {
         logger.error("Unexpected exception ",ex);
         fail("unexpected exception ");

      }
   }
}
