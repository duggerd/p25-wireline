//
package gov.nist.p25.issi.rfss;

import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.p25.issi.ISSITimer;

import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SuState;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25payload.IMBEVoiceBlock;
import gov.nist.p25.issi.p25payload.IMBEVoiceDataGenerator;
import gov.nist.p25.issi.p25payload.ISSIPacketType;
import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.p25.issi.p25payload.PacketType;
import gov.nist.p25.issi.rfss.tester.AbstractSipSignalingTest;
//import gov.nist.p25.issi.rfss.tester.TestMessages;
//import gov.nist.p25.issi.rfss.tester.TestMessagesParser;
import gov.nist.p25.issi.rfss.tester.TestScript;
import gov.nist.p25.issi.rfss.TransmissionControlSAP;
import gov.nist.p25.issi.testlauncher.TestHarness;
import gov.nist.p25.issi.transctlmgr.TransmissionControlManager;
//import gov.nist.p25.issi.transctlmgr.TransmissionControlSAP;
import gov.nist.p25.issi.transctlmgr.ptt.CapturedPttPacket;
import gov.nist.p25.issi.transctlmgr.ptt.PttSessionInterface;

import gov.nist.p25.issi.utils.ProtocolObjects;
import gov.nist.p25.issi.verifier.TestMessages;
import gov.nist.p25.issi.verifier.TestMessagesParser;
import gov.nist.rtp.RtpPacket;
import gov.nist.rtp.RtpSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.TimerTask;

import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.TimeStampHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WarningHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

/**
 * This is the RFSS emulation class. Its function is mainly a management
 * interface. It registers a SIP listener and recieves SIP Events via the SIP
 * listener. It looks at the request/response event type of the incoming SIP
 * request and directs these to appropriate handlers. It creates one instance of
 * each of the following upon startup:
 * 
 * <ul>
 * <li> MobilityManager
 * <li> CallControlManager
 * <li> TransmissionControlManager
 * </ul>
 * 
 * <p>
 * All incoming REGISTER requests and responses are directed to the Mobility
 * Manager and all INVITE, ACK, BYE are directed to the Call Control Manager.
 * 
 * <p>
 * Each RFSS has an IP Address and port which is specificed in the
 * configuration.xml file for the system. It is provided as an argument to the
 * RFSS on startup. An RFSS configuration is contained in the class RfssConfig.
 * 
 * <p>
 * This class may be invoked in listener thread if we are emulating the system
 * or in its own JVM ( as a test tool ).
 * 
 */
@SuppressWarnings("unchecked")
public class RFSS implements SipListener {

   private static Logger logger = Logger.getLogger(RFSS.class);
   private static Logger errorLogger = Logger.getLogger("gov.nist.p25.ERRORLOG");
   
   public static void showln(String s) { System.out.println(s); }

   private static List<IMBEVoiceBlock> imbeVoiceBlocks = 
         IMBEVoiceDataGenerator.getIMBEVoiceData("voice/imbe-hex-test.txt");

   public static List<IMBEVoiceBlock> getImbeVoiceBlocks() {
      return imbeVoiceBlocks;
   } 

   private AbstractSipSignalingTest currentTestCase;
   private boolean isRfResourcesAvailable;
   private static Hashtable<String, RFSS> rfssTable = new Hashtable<String, RFSS>();

   private ArrayList<Request> requests;
   private ArrayList<Response> responses;
   private RfssConfig rfssConfig;
   private int port;
   private String ipAddress;
   private SipProvider provider;
   private TestHarness testHarness;
   private TransmissionControlManager transmissionControlManager;

   private TestScript testScript;
   public TestScript getTestScript() { return testScript; }
   
   private boolean isTransactionTerminated;
   public boolean isTransactionTerminated() { return isTransactionTerminated; }
   
   private boolean isDialogTerminated;
   public boolean isDialogTerminated() { return isDialogTerminated; }

   private boolean isSaveTrace;
   public boolean isSaveTrace() { return isSaveTrace; }

   /*
    * Manages user and group service profiles. This should really be managed by
    * a SLEE Profile in a reference implementation of this standards.
    */
   private MobilityManager mobilityManager;
   private Hashtable<String, SipListener> methodHash;
   private HashSet<SuConfig> servedSubscriberUnits;
   private Hashtable<String, HomeAgent> homeAgents;
   private Hashtable<PttSessionInterface, CapturedPttPacket> lastCapturedPttPacketTable = 
              new Hashtable<PttSessionInterface, CapturedPttPacket>();
   private CallControlManager callControlManager;
   private TopologyConfig topologyConfig;
   private ArrayList<CapturedPttPacket> capturedPackets = new ArrayList<CapturedPttPacket>();
   private String failureReason;
   private TestMessages testMessages;
   private TestMessages refMessages;

   private static boolean matchSipRequest(SIPRequest request, SIPRequest template) {
      SIPRequest testSipRequest = (SIPRequest) request.clone();
      testSipRequest.removeHeader(CallIdHeader.NAME);
      template.removeHeader(CallIdHeader.NAME);
      testSipRequest.removeHeader(ExpiresHeader.NAME);
      template.removeHeader(ExpiresHeader.NAME);
      return testSipRequest.match(template);
   }

   private static boolean matchSipResponse(SIPResponse response, SIPResponse template) {
      SIPResponse testSipResponse = (SIPResponse) response.clone();
      /*
       * We dont care about the call ID header in making the comparison.
       * However call id header is mandatory and if the request did not have a
       * call id header it would fail in transaction matching anyways.
       */
      testSipResponse.removeHeader(CallIdHeader.NAME);
      template.removeHeader(CallIdHeader.NAME);
      testSipResponse.removeHeader(ExpiresHeader.NAME);
      template.removeHeader(ExpiresHeader.NAME);
      return testSipResponse.match(template);
   }

   /**
    * Create an RFSS. Return one if its already created.
    * 
    * @param provider
    * @param rfssConfig
    * @param testScript
    * @return
    */
   public static RFSS create(SipProvider provider, RfssConfig rfssConfig,
         TestScript testScript) throws Exception {

      //logger.debug("RFSS(0): create(): testScript.testName="+testScript.getTestName());
      //logger.debug("RFSS(0): create(): testScript.description="+testScript.getDescription());
      if (rfssTable.get(rfssConfig.getDomainName()) != null) {
         return rfssTable.get(rfssConfig.getDomainName());

      } else {
         if (testScript.isInteractive()) {
            // load the traces for this test.
            String fname = rfssConfig.getRfssName() + ".xml";
            String traceFileName = testScript.getReferenceMessagesDirName() + "/" + fname;
            logger.debug("RFSS: traceFileName="+traceFileName);

            if (!new File(traceFileName).exists()) {
               if (logger.isDebugEnabled()) {
                  logger.error("Reference Trace file not found for the emulated RFSS "
                              + rfssConfig.getRfssName());
                  logger.error("Configured to receive SIP requests at the following address "
                              + rfssConfig.getHostPort());
               }
               ProtocolObjects.stop();
               RFSS.reset();
               logger.debug("RFSS: Trace file not found: "+traceFileName);
               throw new Exception( "Reference trace is missing, generate trace first !");
            }
         }

         RFSS rfss = new RFSS(provider, rfssConfig);
         rfssTable.put(rfssConfig.getDomainName(), rfss);
         rfss.testScript = testScript;
         ((RfssRouter) provider.getSipStack().getRouter()).setRfss(rfss);
         if (testScript.isInteractive()) {
            // load the traces for this test.
            String fname = rfssConfig.getRfssName() + ".xml";
            String traceFileName = testScript.getReferenceMessagesDirName() + "/" + fname;
            rfss.testMessages = new TestMessagesParser(traceFileName).parse();
            rfss.refMessages = new TestMessagesParser(traceFileName).parse();
         }
         return rfss;
      }
   }

   /**
    * Reset the table after the test is over.
    */
   public static void reset() {
      rfssTable.clear();
   }

   /**
    * Capture a ptt packet for later analysis. Note that this just creates an
    * object and stores it in memory for later retrieval. This is because
    * otherwise we cannot handle a large volume of packets
    * 
    * @param rtpPacket
    * @param payload
    * @param isSender -
    *            true if I am sender.
    * @param session
    */
   public void capturePttPacket(RtpPacket rtpPacket, P25Payload payload,
         boolean isSender, PttSessionInterface session) {

      PacketType packetType = payload.getISSIPacketType().getPacketType();
      boolean matchPacket = true;
      if (packetType == PacketType.HEARTBEAT ||
          packetType == PacketType.HEARTBEAT_QUERY) {
         matchPacket = false;
      }
      capturePttPacket(rtpPacket, payload, isSender, session, matchPacket);
   }

   public void capturePttPacket(RtpPacket rtpPacket, P25Payload payload,
      boolean isSender, PttSessionInterface session, boolean matchPacket)
   {
      RtpSession rtpSession = session.getRtpSession();

// Why 14.1.1 rfss_1 has 17 PTT packets ?
String myTag = rtpSession.getMyIpAddress().getHostAddress() + ":" + rtpSession.getMyRtpRecvPort();
String remoteTag = rtpSession.getRemoteIpAddress() + ":" + rtpSession.getRemoteRtpRecvPort();
logger.debug(rfssConfig.getRfssName()+" capturePttPacket():" +
      " matchPacket=" +matchPacket +
      " rfssIp=" +getIpAddress() +
      " isSender="+isSender +
      " packetType="+payload.getISSIPacketType().getPacketType() +
      " remoteTag="+remoteTag +
      " myTag="+myTag);

      // Dont record packets that we route within an RFSS (internal to RFSS).
      // Need to verify: record packets if the remote RFSS is a REAL RFSS
      boolean bflag = getTransmissionControlManager().getPttManager().ownsRemoteRtpSession(rtpSession);
      RfssConfig remoteRfssConfig = topologyConfig.getRfssConfigByIpAddress( rtpSession.getRemoteIpAddress());
      
      //boolean emulated = remoteRfssConfig.getEmulated();
      boolean emulated = (remoteRfssConfig==null ? false : remoteRfssConfig.getEmulated());
      logger.debug(rfssConfig.getRfssName()+" ownsRemoteRtpSession: "+bflag+" remoteEmulated="+emulated);

      if (bflag) {
         logger.debug(rfssConfig.getRfssName()+" Not logging a packet that we send to ourselves");
         return;
      }
      if (isSender && emulated) {
         logger.debug(rfssConfig.getRfssName()+" Not logging a packet: we are sender or remote is emulated RFSS");
         return;
      }

      CapturedPttPacket pttPacket = new CapturedPttPacket(rtpPacket, payload, isSender, session);

      // I would like to store the packets in memory at runtime and delay flushing.
      CapturedPttPacket lastPacket = lastCapturedPttPacketTable.get(session);
if( matchPacket) {
      if (lastPacket != null
            && lastPacket.getRemotePort() == pttPacket.getRemotePort()
            && lastPacket.getRemoteIpAddress().equals( pttPacket.getRemoteIpAddress())
            && lastPacket.match(pttPacket)) {
         logger.debug(rfssConfig.getRfssName()+" Not logging a packet: matched previous one ");
         return;
      } 
}
      lastCapturedPttPacketTable.put(session, pttPacket);
     synchronized( capturedPackets) {
      capturedPackets.add(pttPacket);
     }

      logger.debug(rfssConfig.getRfssName() +" capturePttPacket():" +
            " size=" +capturedPackets.size() +
            " isSender=" +isSender +
            " packetType="+payload.getISSIPacketType().getPacketType() +
            " remoteTag=" +remoteTag +
            " myTag=" +myTag);
   }

   /**
    * Flush all the captured messages to disk.
    * 
    */
   public synchronized void flushCapturedPttMessages() {
     synchronized( capturedPackets) {
      logger.debug(rfssConfig.getRfssName()+" flushCapturedPttMessages(): flushing " + capturedPackets.size() + " packets !");
      for (CapturedPttPacket pttMessage: capturedPackets) {
         pttMessage.flush();
      }
      //logger.debug(rfssConfig.getRfssName()+" flushCapturedPttMessages(): SKIP capturedPackets.clear()...");
      //capturedPackets.clear();
     }
   }

   /**
    * Save the expected messages as a file that can be later used to test
    * against the messages that the RFSS actually sees.
    * 
    * @throws IOException
    */
   public synchronized void saveTrace() throws IOException {
      try {
         logger.debug("saving trace for " + rfssConfig.getRfssName());
         String fname = rfssConfig.getRfssName() + ".xml";
         File dir = new File(testScript.getReferenceMessagesDirName());
         dir.mkdirs();

         String fullpath = testScript.getReferenceMessagesDirName() + "/" + fname;
         //showln("saveTrace(): fullpath="+fullpath);

         File file = new File(fullpath);
         file.delete();
         file.createNewFile();
         FileOutputStream fos = new FileOutputStream(file);

         String urlStr = "<!DOCTYPE expected-messages SYSTEM \"" +
            ISSIDtdConstants.URL_ISSI_DTD_EXPECTED_MESSAGES + "\">\n";
         StringBuffer ps = new StringBuffer();
         ps.append("<?xml version=\"1.0\" ?>\n");
         ps.append( urlStr);
         ps.append("<expected-messages>\n");
         ps.append("<siprequests>\n");
         for (Request sipRequest : requests) {
            ViaHeader viaHeader = (ViaHeader) sipRequest.getHeader(ViaHeader.NAME);
            String host = viaHeader.getHost();
            if (host.equals(rfssConfig.getDomainName())) {
               // We do not care about self routed requests.
               continue;
            }
            Request newRequest = (Request) sipRequest.clone();

            ps.append("<siprequest>\n");
            ps.append("<![CDATA[\n");
            // Timestamp header is not mandatory.
            //+++newRequest.removeHeader(TimeStampHeader.NAME);         
            newRequest.removeHeader(MaxForwardsHeader.NAME);

            // Remove the warning header (not compared)
            newRequest.removeHeader(WarningHeader.NAME);

            ListIterator li = newRequest.getHeaders(RouteHeader.NAME);
            // Only check the first route header.
            if (li != null && li.hasNext()) {
               RouteHeader route = (RouteHeader) li.next();
               newRequest.setHeader(route);
            }
            ListIterator it = newRequest.getHeaders(ViaHeader.NAME);
            newRequest.removeHeader(ViaHeader.NAME);
            while (it.hasNext()) {
               // Get rid of the branch in the request header
               ViaHeader via = (ViaHeader) it.next();
               via.removeParameter("branch");
               newRequest.addHeader(via);
            }
            ToHeader to = (ToHeader) newRequest.getHeader(ToHeader.NAME);
            // The tag is a random quanity -- get rid of it.
            to.removeParameter("tag");
            FromHeader from = (FromHeader) newRequest.getHeader(FromHeader.NAME);
            from.removeParameter("tag");
            ps.append(newRequest.toString());
            ps.append("]]>\n");
            ps.append("</siprequest>\n");
         }
         ps.append("</siprequests>\n");

         /**
          * For each of the captured messages - save only the parts that we
          * want
          */
         ps.append("<sipresponses>\n");
         for (Response response : responses) {
            Response newResponse = (Response) response.clone();
            FromHeader from = (FromHeader) newResponse.getHeader(FromHeader.NAME);
            from.removeParameter("tag");
            ToHeader to = (ToHeader) newResponse.getHeader(ToHeader.NAME);
            to.removeParameter("tag");
            //M1014
            //+++newResponse.removeHeader(TimeStampHeader.NAME);
            newResponse.removeHeader(MaxForwardsHeader.NAME);

            for (Iterator it = newResponse.getHeaders(ViaHeader.NAME); it.hasNext();) {
               ViaHeader viaHeader = (ViaHeader) it.next();
               viaHeader.removeParameter("branch");
            }

            if (response.getStatusCode() != 100) {
               ps.append("<sipresponse>\n");
               ps.append("<![CDATA[\n");
               ps.append(newResponse.toString());
               ps.append("]]>\n");
               ps.append("</sipresponse>\n");
            }
         }
         ps.append("</sipresponses>\n");
         // TODO -- store the PTT messages here.
         ps.append("</expected-messages>\n");
         
         fos.write(ps.toString().getBytes());
         fos.close();
         file=null;
         dir=null;
            
         if (capturedPackets.size() > 0) {
            fname = rfssConfig.getRfssName() + "_ptt.bin";
            String fullname = testScript.getReferenceMessagesDirName() + "/" + fname;
            logger.debug("RFSS: capturedPackets: fullname="+fullname);
            
            file = new File( fullname);
            file.delete();
            file.createNewFile();
            fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
	   synchronized( capturedPackets) {
            for (CapturedPttPacket pttMessage : capturedPackets) {
               oos.writeObject(pttMessage);
            }
	   }
            oos.close();
            fos.close();
            
            // M1009 PTT PACKET ptt.bin files
            isSaveTrace = true;
         }
         else {
            // M1009 no PTT packet involved in the test
            //showln("RFSS: No PTT packet, set isSaveTrace to true");         
            isSaveTrace = true;
         }
      } catch (IOException ex) {
         logger.error("Error capturing trace", ex);
         throw ex;
      }
   }

   /**
    * This predicate tests to see if we find all the messages that are captured
    * by the RFSS.
    * 
    * @return true -- if each of the messages in the trace is seen in the
    *         captured messages. False otherwise.
    */
   public boolean testCapturedMessages() {
      try {

         // load the traces for this test.
         String fname = rfssConfig.getRfssName() + ".xml";
         String traceFileName = testScript.getReferenceMessagesDirName() + "/" + fname;
         //logger.debug("RFSS: testCapturedMessages(): "+traceFileName);

         if (!new File(traceFileName).exists()) {
            if (logger.isDebugEnabled()) {
               logger.error("Reference Trace file not found for the emulated RFSS " +traceFileName);
               logger.error("Configured to receive SIP requests at the following address "
                      + rfssConfig.getHostPort());
            }
            testMessages = new TestMessages();
         } else {
            testMessages = new TestMessagesParser(traceFileName).parse();
         }

         //logger.debug("RFSS: testMessages-requests.size(): "+testMessages.getRequests().size());
         //logger.debug("RFSS: captured-requests= "+requests.size());
         for (Request request : testMessages.getRequests()) {
            SIPRequest testSipRequest = (SIPRequest) request.clone();
            
            // Get the template sip request content list.
            ContentList contentList = ContentList.getContentListFromMessage(testSipRequest);
            /*
             * We dont care about the call ID header in making the
             * comparison. However call id header is mandatory and if the
             * request did not have a call id header it would fail in
             * transaction matching anyways.
             */
            testSipRequest.removeHeader(CallIdHeader.NAME);
            /*
             * The contents of the expires header are arbitrary so remove it
             * from the match template.
             */
            boolean expiresRemoved = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME) != null;
            testSipRequest.removeHeader(ExpiresHeader.NAME);
            
            // skip TimeStamp in matching
            testSipRequest.removeHeader(TimeStampHeader.NAME);
            //logger.debug("RFSS: testSipRequest:\n"+testSipRequest);
            
            boolean matchFound = false;
            SIPRequest capturedRequest = null;
            for (Request capRequest : requests) {
               capturedRequest = (SIPRequest) capRequest.clone();
               //capturedRequest.removeHeader(TimeStampHeader.NAME);
               ContentList contentList1 = ContentList.getContentListFromMessage(capturedRequest);
               
               //logger.debug("\nRFSS: capturedRequest: \n"+capturedRequest);
               boolean mflag = capturedRequest.match(testSipRequest);
               boolean cflag = contentList.match(contentList1);
               //logger.debug("   expiresRemoved="+expiresRemoved+ " mflag="+mflag +" cflag="+cflag);               
               if ( mflag
                     && ((!expiresRemoved) || capturedRequest.getExpires() != null)
                     && cflag) {
                  matchFound = true;
                  break;
               }
            }
            //logger.debug("\nRFSS: matchFound="+matchFound+"\n\n");

            if (!matchFound) {            
               String head = "Could not find a match for this REFERENCE request message:\n" +
               "----------------------------------------------------------\n";
               String content = "Unmatched content: " + contentList.getUnmatchedContent();
               failureReason = head + content + "\n\n" + request;
               logError(failureReason);
               logger.debug("testMessages.requests:\n" + testMessages.getRequests());
               return false;
            }
         }

         //logger.debug("RFSS: testMessages.responses.size(): "+testMessages.getResponses().size());
         for (Response response : testMessages.getResponses()) {
            SIPResponse testSipResponse = (SIPResponse) response.clone();
            
            // skip trying, ringing
            //-----------------------------------------------
            if( testSipResponse.getStatusCode() == Response.TRYING) {
               //logger.debug("testCapturedMessage: skip TRYING");
               continue;      
            }
            if( testSipResponse.getStatusCode() == Response.RINGING) {
               //logger.debug("testCapturedMessage: skip RINGING");
               continue;      
            }
            //-----------------------------------------------
            //logger.debug("testCapturedMessage: normal check...");
            testSipResponse.removeHeader(CallIdHeader.NAME);
            ContentList testContentList = ContentList.getContentListFromMessage(testSipResponse);

            boolean expiresRemoved = (testSipResponse.getHeader(ExpiresHeader.NAME) != null);
            testSipResponse.removeHeader(ExpiresHeader.NAME);
            response.removeHeader(ExpiresHeader.NAME);
            
            boolean matchFound = false;
            SIPResponse testResponse = null;
            for (Response capturedResponse : responses) {
               testResponse = (SIPResponse) capturedResponse;
               ContentList contentList = ContentList.getContentListFromMessage(capturedResponse);
               testResponse.removeHeader(CallIdHeader.NAME);

               if (((SIPResponse) capturedResponse).match(testSipResponse)
                     && ((!expiresRemoved) || capturedResponse.getExpires() != null)
                     && testContentList.match(contentList)) {
                  matchFound = true;
                  break;
               }
            }
            if (!matchFound) {
               String head = "Error: Could not find a match for this REFERENCE response message:\n" +
               "------------------------------------------------------------------\n";
               String content = "Unmatched content: " + testContentList.getUnmatchedContent();
               failureReason = head + content + "\n\n" + response;
               logError(failureReason);
               //logger.debug("testMessages.responses:\n" + testMessages.getResponses());
               return false;
            }
         }

         // check PTT
         //---------------------------------------------------------------------------
         fname = RFSS.this.rfssConfig.getRfssName() + "_ptt.bin";
         File file = new File(testScript.getReferenceMessagesDirName() + "/" + fname);
         if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream oos = new ObjectInputStream(fis);
            int counter = 0;
            while (oos.available() != 0) {
               CapturedPttPacket pttPacket = (CapturedPttPacket) oos.readObject();
               
               boolean matchFound = false;
              synchronized( capturedPackets) {
               while (capturedPackets.size() != 0) {
                  CapturedPttPacket packetToCheck = capturedPackets.get(counter++);
                  //TODO: this looks fishy for break, BUG ???
                  if (packetToCheck.match(pttPacket)) {
                     matchFound = true;   // M1001
                     break;
                  }
               }
              }
               if (!matchFound) {
                  failureReason = "Could not find a packet to match this REFERENCE packet: \n"
                     + pttPacket.toString();
                  logError(failureReason);
                  return false;
               }
            }
         }
         return true;
      } catch (Exception ex) {
         failureReason = "A parse error occured while trying to test messages";
         logError(failureReason, ex);
         return false;
      }
   }
   
   public static void shutDownAll() {
      for (RFSS rfss : rfssTable.values()) {
         rfss.shutDown();
      }
      rfssTable.clear();
   }

   public static Collection<RFSS> getAllRfss() {
      return rfssTable.values();
   }

   public String getRfssSessionInfo() {
      StringBuffer retval = new StringBuffer();
      retval.append("\n<rfss-session-info");
      retval.append("\n\trfssId = \"" + rfssConfig.getDomainName() + "\"");
      retval.append("\n>\n");
      retval.append(getTransmissionControlManager().getPttManager().getSessionDescriptions());
      retval.append("\n</rfss-session-info>");
      return retval.toString();
   }

   /**
    * Shut down an emulated RFSS>
    */
   public void shutDown() {
      logger.debug("Shutting down emulated rfss " + rfssConfig.getDomainName());
      transmissionControlManager.shutDown();
   }

   /**
    * The RFSS constructor.
    * 
    * @unpublished
    * @paream protocolObjects -- encapsuated SIP Stack.
    * @param provider -
    *            the provider - created by the caller.
    * @param rfssConfig -
    *            rfss configuration for this rfss
    */
   private RFSS(SipProvider provider, RfssConfig rfssConfig) {
      try {
         if (rfssConfig == null)
            throw new NullPointerException("null rfssconfig");
         this.rfssConfig = rfssConfig;
         this.isRfResourcesAvailable = rfssConfig.isRfResourcesAvailable();
         this.topologyConfig = rfssConfig.getSysConfig().getTopologyConfig();
         SipStack sipStack = provider.getSipStack();
         this.requests = new ArrayList<Request>();
         this.responses = new ArrayList<Response>();
         this.servedSubscriberUnits = new HashSet<SuConfig>();

         logger.debug("InitialServedSubscriberUnits : "
               + rfssConfig.getRfssName() + " = "
               + rfssConfig.getInitialServedSubscriberUnits());
         for (SuConfig suConfig : rfssConfig.getInitialServedSubscriberUnits()) {
            // Only add those SU's that are initially turned on.
            if (suConfig.getInitialSuState() == SuState.ON
                  && suConfig.getServingRfssReferencesMeFor() != 0) {
               this.servedSubscriberUnits.add(suConfig);
               /*
                * start a timer to remove the record if the time is not
                * unbounded.
                */
               if (suConfig.getServingRfssReferencesMeFor() != -1) {
                  ISSITimer.getTimer().schedule( new TimerTask() {
                        private SuConfig suConfig;

                        public TimerTask setSuConfig( SuConfig suConfig) {
                           this.suConfig = suConfig;
                           return this;
                        }
                        public void run() {
                           synchronized (RFSS.this) {
                              RFSS.this.servedSubscriberUnits.remove(suConfig);
                           }
                        }
                     }.setSuConfig(suConfig),
                     suConfig.getServingRfssReferencesMeFor() * 1000);
               }

            } else {
               logger.debug("not inserting into served set " + suConfig.getInitialSuState());
            }
         }

         /*
          * This is a NIST-SIP only feature - add an address resolver to the
          * SIP stack which knows how to convert from funky Via headers to
          * actual IP address and port.
          */
         ((SipStackImpl) sipStack).setAddressResolver(new RfssAddressResolver(topologyConfig));

         /*
          * Another NIST-SIP only feature. We register our own log record
          * factory to log the ISSI specific information into the log stream.
          */
         ((SipStackImpl) sipStack).setLogRecordFactory(new LogRecordFactoryImpl(topologyConfig));

         this.ipAddress = rfssConfig.getIpAddress();
         this.port = rfssConfig.getSipPort();
         this.rfssConfig.setRFSS(this);
         this.provider = provider;
         // This manages the RTP sessions.
         this.transmissionControlManager = new TransmissionControlManager( this);

         // Manages registrations etc.
         this.mobilityManager = new MobilityManager(this);
         this.callControlManager = new CallControlManager(this);
         this.methodHash = new Hashtable<String, SipListener>();
         methodHash.put(Request.ACK, callControlManager);
         methodHash.put(Request.BYE, callControlManager);
         methodHash.put(Request.CANCEL, callControlManager);
         methodHash.put(Request.INVITE, callControlManager);
         methodHash.put(Request.REGISTER, mobilityManager);
         
         int counter = 0;
         this.homeAgents = new Hashtable<String, HomeAgent>();
         for (Iterator<SuConfig> it = this.rfssConfig.getHomeSubsciberUnits(); it.hasNext();) {
            counter++;
            SuConfig su = it.next();
            HomeAgent homeAgent = new HomeAgent(su, this.topologyConfig, this);
            ServiceAccessPoints saps = this.getServiceAccessPoints(su);
            assert su.getHomeRfss() == this.rfssConfig;
            logger.debug("adding Home su " + su.getRadicalName());
            homeAgent.setServiceAccessPoints(saps);
            this.homeAgents.put(su.getRadicalName(), homeAgent);
         }
         if (counter == 0)
            logger.debug("I am not HOME to any SU ");

         logger.info("Emulated RFSS initialized!");
         //logger.info("RFSS ID = " + this.rfssConfig.getRfssId());
         logger.info("RFSS ID = " + this.rfssConfig.getRfssIdString());
         logger.info("Served Set = " + this.servedSubscriberUnits);

      } catch (Exception ex) {
         logger.error("exception in initializing RFS", ex);
         throw new RuntimeException("Error Intializing the RFSS", ex);
      }
   }

   // accessors
   UnitToUnitMobilityManager getUnitToUnitMobilityManager() {
      return mobilityManager.getUnitToUnitMobilityManager();
   }

   GroupMobilityManager getGroupMobilityManager() {
      return mobilityManager.getGroupMobilityManager();
   }

   CallControlManager getCallControlManager() {
      return callControlManager;
   }

   // //////////////////////////////////////////////////////////////////////
   // JAIN-SIP listener methods.
   // //////////////////////////////////////////////////////////////////////
   /**
    * @see javax.sip.SipListener#processRequest(javax.sip.RequestEvent)
    * @unpublished
    */
   public void processRequest(RequestEvent requestEvent) {

      Request request = requestEvent.getRequest();
      requests.add(request);
      String method = request.getMethod();
      logger.debug("RFSS: id=" + getRfssConfig().getRfssIdString() 
            + " processRequest: " + method
            + " isInteractive= " + testScript.isInteractive());
      SipListener listener = methodHash.get(method);

      if (testScript.isInteractive()) {
         if (this.refMessages.hasMoreRequests()) {
            SIPRequest template = (SIPRequest) this.refMessages.getFirstRequest();
            if (matchSipRequest((SIPRequest) request, (SIPRequest) template)) {
               refMessages.removeRequest(template);
            }
         }

         if (!this.refMessages.hasMoreRequests()) {
            logger.debug("Request COMPLETION seen on " + this.rfssConfig.getRfssName());
         } else {
            logger.debug("REQUEST COMPLETION not yet seen " + this.refMessages.getRequests().size());
         }
      }
      listener.processRequest(requestEvent);
   }

   /**
    * @see javax.sip.SipListener#processResponse(javax.sip.ResponseEvent)
    * 
    * @unpublished
    */
   public void processResponse(ResponseEvent responseEvent) {
      Response response = responseEvent.getResponse();
      ClientTransaction ct = responseEvent.getClientTransaction();
      if (ct == null) {
         logger.debug("RFSS: Dropping stray response !!!\n" + responseEvent.getResponse());
         return;
      }
      ViaHeader viaHeader = (ViaHeader) ct.getRequest().getHeader( ViaHeader.NAME);
      String host = viaHeader.getHost();

      if (!host.equals(this.rfssConfig.getDomainName())) {
         // We do not care about self routed requests.
         this.responses.add(response);
      }

      CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
      String method = cseqHeader.getMethod();
      SipListener listener = methodHash.get(method);

      if (this.testScript.isInteractive()) {
         if (this.refMessages.hasMoreResponses()) {
            SIPResponse template = (SIPResponse) this.refMessages.getFirstResponse();
            if (matchSipResponse((SIPResponse) response, (SIPResponse) template)) {
               refMessages.removeResponse(template);
            }
         }
         if (!this.refMessages.hasMoreResponses()) {
            logger.debug("Response COMPLETION seen on " + this.rfssConfig.getRfssName());
         } else {
            logger.debug("RESPONSE COMPLETION not yet seen " + this.refMessages.getResponses().size());
         }
      }
      listener.processResponse(responseEvent);
   }

   /**
    * @see javax.sip.SipListener#processTimeout(javax.sip.TimeoutEvent)
    * 
    * @unpublished
    */
   public void processTimeout(TimeoutEvent timeoutEvent) {
      logger.debug("timeoutEvent ");
      Request request;
      if (timeoutEvent.isServerTransaction()) {
         request = timeoutEvent.getServerTransaction().getRequest();
      } else {
         request = timeoutEvent.getClientTransaction().getRequest();
      }
      SipListener listener = methodHash.get(request.getMethod());
      listener.processTimeout(timeoutEvent);
   }

   /**
    * @unpubished
    */
   public void processIOException(IOExceptionEvent ioExceptionEvent) {
      logger.debug("ioExceptionEvent");
      this.testHarness.fail("Unexpected IO Exception");
   }

   /**
    * @see javax.sip.SipListener#processTransactionTerminated(javax.sip.TransactionTerminatedEvent)
    * @unpublished
    */
   public void processTransactionTerminated( TransactionTerminatedEvent terminatedEvent) {
      logger.debug("txTerminatedEvent: isServerTransaction="+terminatedEvent.isServerTransaction());
      Request request;
      if (terminatedEvent.isServerTransaction()) {
         request = terminatedEvent.getServerTransaction().getRequest();
      } else {
         request = terminatedEvent.getClientTransaction().getRequest();
      }
      SipListener listener = methodHash.get(request.getMethod());
      listener.processTransactionTerminated(terminatedEvent);
      
      //NOTE: may not be appropriate
      logger.debug("processTransactionTerminated(): DISABLE set to true...");
      //isTransactionTerminated = true;
   }

   /**
    * @see javax.sip.SipListener#processDialogTerminated(javax.sip.DialogTerminatedEvent)
    * @unpublished
    */
   public void processDialogTerminated(DialogTerminatedEvent dte) {
      logger.debug("dialgTerminatedEvent ");
      callControlManager.processDialogTerminated(dte);
      isDialogTerminated = true;
      //showln("RFSS: set isDialogTerminated to true...");
   }

   /**
    * @unpublished
    */
   public TransmissionControlManager getTransmissionControlManager() {
      return this.transmissionControlManager;
   }

   /**
    * @param currentTestCase
    *            the currentTestCase to set
    */
   public void setCurrentTestCase(AbstractSipSignalingTest currentTestCase) {
      this.currentTestCase = currentTestCase;
   }

   /**
    * @return the currentTestCase
    */
   public AbstractSipSignalingTest getCurrentTestCase() {
      return currentTestCase;
   }

   /**
    * Each Subscriber Unit abstraction ( i.e. implementation of SU) talks to
    * the individual services offered by the RFSS via Service Access points.
    * 
    * @param suConfig --
    *            the subscriber unit configuration file.
    * @param su --
    *            the Subscriber unit instance.
    * @return -- a set of service access points to talk to the RFSS.
    */
   public ServiceAccessPoints getServiceAccessPoints(SuConfig suConfig) {
      assert suConfig != null;
      MobilityManagementSAP mmSap = new MobilityManagementSAP(this, suConfig);
      CallControlSAP ccSap = new CallControlSAP(this, suConfig);
      TransmissionControlSAP tcSap = new TransmissionControlSAP(this, suConfig);
      ServiceAccessPoints saps = new ServiceAccessPoints(ccSap, mmSap, tcSap);
      ccSap.setSaps(saps);

      // attach custom id for debug
      //String id = suConfig.getSuName() +":" +suConfig.getInitialServingRfss().getRfssName();
      //saps.setSapName( id);
      return saps;
   }

   /**
    * Get the IP Address of this RFSS.
    * 
    * @return -- the IP address of the RFSS
    */
   public String getIpAddress() {
      return this.ipAddress;
   }

   /**
    * Get the port of this RFSS.
    * 
    * @return -- the port of the RFSS
    */
   public int getPort() {
      return this.port;
   }

   /**
    * Get the configuration of this RFSS. The configuration of the RFSS is read
    * from the topology configuration file when the system is started up. Each
    * RFSS has an RFSS ID. The topology configuration file contains the IP
    * address, port and other relevant information for the RFSS.
    * 
    * @return -- the RFSS configuration for this RFSS.
    */

   public RfssConfig getRfssConfig() {
      return this.rfssConfig;
   }

   /**
    * Get the Mobility Management comopnent for the RFSS.
    * 
    * @return -- the mobility manager.
    */
   public MobilityManager getMobilityManager() {
      return mobilityManager;
   }

   /**
    * Return a collection of all the subscriber units that we are currently
    * serving.
    * 
    * @return -- a collection of the currently served subscriber units.
    */
   public HashSet<SuConfig> getServedSubscriberUnits() {
      return this.servedSubscriberUnits;
   }

   /**
    * Remove a served subscriber unit ( for roaming emulation)
    * 
    * @param suConfig
    */
   public synchronized void removeServedSubscriberUnit(SuConfig suConfig) {
      this.servedSubscriberUnits.remove(suConfig);
   }

   /**
    * Add a served subscriber unit ( for roaming emulation ).
    * 
    * @param suConfig --
    *            suConfig to add
    * @param suConfig
    */
   public synchronized void addServedSubscriberUnit(final SuConfig suConfig) {
      this.servedSubscriberUnits.add(suConfig);
      if (suConfig.getServingRfssReferencesMeFor() != -1) {
         ISSITimer.getTimer().schedule(new TimerTask() {
            public void run() {
               synchronized (RFSS.this) {
                  RFSS.this.servedSubscriberUnits.remove(suConfig);
               }
            }
         }, suConfig.getServingRfssReferencesMeFor() * 1000);
      }
   }

   public HomeAgent getHomeAgent(String radicalName) {
      return this.homeAgents.get(radicalName);
   }

   /**
    * Boolean check for if rf resources are available.
    * 
    * @return
    */
   public boolean isRfResourcesAvailable() {
      return this.isRfResourcesAvailable;
   }

   /**
    * Set the resource flag.
    * 
    * @param resourceVal
    */
   public void doSetRfResourceAvailable(boolean resourceVal) throws Exception {
      boolean alert = this.isRfResourcesAvailable ^ resourceVal;
      this.isRfResourcesAvailable = resourceVal;
      if (alert) {
         callControlManager.alertResourceChange(resourceVal);
      }
   }

   /**
    * Return true if I am serving this SU.
    * 
    * @param suConfig
    * @return
    */
   public boolean isSubscriberUnitServed(SuConfig suConfig) {
      boolean retval = this.servedSubscriberUnits.contains(suConfig);
      if (retval) {
         logger.debug("RfssName " + rfssConfig.getRfssName()
               + " Served Subscriber Units " + this.servedSubscriberUnits);
      }
      return retval;
   }

   /**
    * @return Returns the provider.
    */
   public SipProvider getProvider() {
      return provider;
   }

   /**
    * @param testHarness
    *            The testHarness to set.
    */
   public void setTestHarness(TestHarness testHarness) {
      this.testHarness = testHarness;
   }

   /**
    * @return Returns the testHarness.
    */
   public TestHarness getTestHarness() {
      if (testHarness == null)
         testHarness = new TestHarness();
      return testHarness;
   }

   /**
    * @return Returns the requests.
    */
   public ArrayList<Request> getRequests() {
      return requests;
   }

   /**
    * @return Returns the responses.
    */
   public ArrayList<Response> getResponses() {
      return responses;
   }

   /**
    * Send a query to the home rfss.
    * 
    * @param suConfig --
    *            su to query.
    */
   public void doServingQuerySu(SuConfig suConfig) throws Exception {
      mobilityManager.getUnitToUnitMobilityManager().sendRegisterQuery( suConfig);
   }

   /**
    * Sends a query operation for the SU.
    * 
    * @param suConfig
    * @throws Exception
    */
   public void doHomeQuerySu(SuConfig suConfig, boolean force, boolean confirm) throws Exception {
      mobilityManager.getUnitToUnitMobilityManager().sendRegisterQuery( suConfig, force, confirm);
   }

   /**
    * Sends a query operation for the SG.
    * 
    * @param sgConfig
    * @throws Exception
    */
   public void doQueryGroup(GroupConfig sgConfig) throws Exception {
      mobilityManager.getGroupMobilityManager().sendRegisterQuery( sgConfig);
   }

   /**
    * Get the current GroupCall for the given radical name or null if nothing
    * exists.
    * 
    * @param groupRadicalName -- Group radical name
    */
   public GroupServing getServedGroupByRadicalName(String groupRadicalName) {
      return callControlManager.getGroupCallControlManager().getGroupCall(groupRadicalName);
   }

   /**
    * Return the group structure from the home RFSS.
    * 
    * @param radicalName -- group radical name.
    * @return the Group Home structure.
    * @throws Exception --
    *             if group not found.
    */
   public GroupHome getHomeGroupByRadicalName(String radicalName) throws Exception {
      return this.getGroupMobilityManager().getGroup(radicalName);
   }

   /**
    * Issue a query from the home Rfss of a group to the given target RFSS>
    * 
    * @param groupConfig --
    *            the group configuration. This should be homed at this rfss
    *            configuration but for misconfiguration emulation, this can be
    *            different.
    * @param targetRfss --
    *            the target RFSS to query from the group home
    * @param force -
    *            the force parameter
    * @param confirm --
    *            the confirm parameter.
    */
   public void doHomeQueryGroup(GroupConfig groupConfig,
         RfssConfig targetRfss, boolean force, boolean confirm)
         throws Exception {
      if (groupConfig.getHomeRfss() != this.rfssConfig) {
         logger.warn("Sending Home query from RFSS other than group home -- check your configuration! ");
      }
      this.getGroupMobilityManager().homeQueryGroup(groupConfig, targetRfss, force, confirm);
   }

   /**
    * Add an RTP port to the RTP resources for this RFSS.
    * 
    * @param nports - number of ports to add.
    */
   public void doAddRtpPort(int nports) {
      try {
         logger.debug("Adding rtp ports " + nports);
         callControlManager.getGroupCallControlManager().rtpResourcesAvailable(nports);
      } catch (Exception e) {
         logger.error("Unexpected exception ", e);
         getTestHarness().fail("Unexpected exception occured ");
      }
   }

   /**
    * Remove active RTP sessions for this RFSS.
    * 
    * @param nports - number of active sessions to stop
    */
   public void removeRtpPort(int nports) {
      try {
         callControlManager.getGroupCallControlManager().removeRtpPorts(nports);
         //showln("RFSS: removeRtpPort(): nports="+nports);
      } catch (Exception ex) {
         logger.error("Unexpected exception ", ex);
         this.getTestHarness().fail("Unexpected exception occured", ex);
      }
   }

   /**
    * Reinvite an Rfss to a group call from the group call home.
    * 
    * @param radicalName -- group radical name.
    * @param rfssConfig -- rfss config of the rfss to reinvite.
    * 
    * @throws Exception
    */
   public void doReInviteGroup(String radicalName, RfssConfig rfssConfig) throws Exception {
      callControlManager.getGroupCallControlManager().reInviteRfssToGroup(radicalName, rfssConfig);
   }

   /**
    * Change the group call priority.
    * 
    * @param groupRadicalName -- groupRadical name.
    * @param newPriority -- new priority.
    * @param isEmergency -- isEmergency.
    */
   public void doChangeGroupCallPriority(String groupRadicalName, int newPriority,
         boolean isEmergency) throws Exception {
      callControlManager.alertGroupPriorityChange(groupRadicalName, newPriority, isEmergency);
   }

   /**
    * Subscribe this SU to the given group.
    * 
    * @param groupRadicalName
    * @param suConfig
    */
   public void subscribeSuToServedGroup(String groupRadicalName, SuConfig suConfig) throws Exception {
      callControlManager.getGroupCallControlManager().joinExistingGroup( suConfig, groupRadicalName);
   }

   /**
    * The method that gets called when an SU arrives at an RFSS.
    * 
    * @param suConfig
    */
   public void suArrived(SuConfig suConfig) {
      getMobilityManager().getUnitToUnitMobilityManager().suArrived(suConfig);
      getMobilityManager().getGroupMobilityManager().suArrived(suConfig);
   }

   public void logError(String message) {
      logger.error(message);
      String newMessageStr = "[" + rfssConfig.getDomainName() + "] " + message;
      errorLogger.error(newMessageStr);
   }

   public void logError(String message, Exception ex) {
      this.getTestHarness().fail(message, ex);
      logger.error(message, ex);
      String newMessageStr = "[" + rfssConfig.getDomainName() + "] " + message;
      errorLogger.error(newMessageStr, ex);
   }

   public boolean isSignalingCompleted() {
      if( refMessages==null) {
         //showln(" +++ null refMessage: isTransTerminated="+
         //      isTransactionTerminated +"  isSaveTrace="+isSaveTrace);
         
         // logic: check if PTT is saved first
         if( isSaveTrace) {
            return isSaveTrace;
         }
         // Now check the isTransactionTerminated
         // generate ref trace, check if transaction is done !!!
         return (isTransactionTerminated & isSaveTrace);
      }         
      boolean retval = (!this.refMessages.hasMoreRequests())
            && (!this.refMessages.hasMoreResponses());
      showln(" +++ refMessage: hasMore retval="+ retval);
      return retval;
   }

   public void doRegisterRegister(SuConfig suConfig, boolean booleanValue) throws Exception {
      mobilityManager.getUnitToUnitMobilityManager().sendRegisterRegister(suConfig,booleanValue);      
   }

   // 14.4.x
   //-------------------------------------------------------
   public void doHeartbeatQuery() {
      logger.debug("RFSS(): doHeartbeatQuery: pttMgr.sendHeartbeatQuery()");
      getTransmissionControlManager().getPttManager().sendHeartbeatQuery();
   } 
}
