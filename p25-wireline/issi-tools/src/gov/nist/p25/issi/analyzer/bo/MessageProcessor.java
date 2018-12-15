//
package gov.nist.p25.issi.analyzer.bo;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;

import jpcap.packet.UDPPacket;
import org.apache.log4j.Logger;

//import gov.nist.p25.issi.analyzer.vo.EndPoint;
//import gov.nist.p25.issi.analyzer.vo.CapturedPacket;
import gov.nist.p25.issi.packetmonitor.CapturedPacket;
import gov.nist.p25.issi.packetmonitor.EndPoint;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25payload.ISSIPacketType;
import gov.nist.p25.issi.p25payload.P25BlockException;
import gov.nist.p25.issi.rfss.SipUtils;

/**
 * Message Processor.
 */
public class MessageProcessor {

   public static Logger logger = Logger.getLogger(MessageProcessor.class);
   public static void showln(String s) { System.out.println(s); }

   private boolean verbose = true;
   private boolean errorFlag;
   private String errorString = "";

   private int[] ports;
   private LinkedList<CapturedPacket> capturedPackets;
   private List<EndPoint> endPointList;

   private Hashtable<String, MediaSession> pendingSessions;
   private Hashtable<String, MediaSession> pttSessionsByDestination;
   private LinkedList<CapturedSipMessage> sipMessages;
   private Hashtable<String, String> transactionTable;
   private LinkedHashMap<String, LinkedList<CapturedSipMessage>> inviteTransactions;
   private LinkedHashMap<String, LinkedList<CapturedSipMessage>> ackDialog;
   private LinkedHashMap<Integer, CapturedPttMessage> pttSessions;
   private MessageFactory messageFactory;

   enum XPacketType {
      SIPREQUEST, SIPRESPONSE, SIPMESSAGE, PTT, ALLMESSAGE
   }

   // accessor
   public boolean getErrorFlag() { return errorFlag; }
   public String getErrorString() { return errorString; }
   
   public void setPortsRange(int[] ports) {
      this.ports = ports;
   }

   public void putTransactionTable(String key, String value) {
      // tid=z9hG4bK55aaf9e4.16 source=02.ad5.bee07.p25dr
      transactionTable.put( key, value);
   }
   public String getTransactionTable(String key) {
      return transactionTable.get( key);
   }

   public LinkedList<CapturedPacket> getCapturedPackets() {
      return capturedPackets;
   }
   public void setCapturedPackets(LinkedList<CapturedPacket>capturedPackets) {
      this.capturedPackets = capturedPackets;
   }

   // mapping between domainName vs host:port 
   // use a list of EndPoint(host, port, domainName, name)
   //---------------------------------------------------------------------------
   public void setEndPointList( List<EndPoint> endPointList)
   {
      this.endPointList = endPointList;
   }
   public List<EndPoint> getEndPointList()
   {
      return endPointList;
   }

   public String getRfssDomainName( String host, int port)
   {
      // source=02.ad5.bee07.p25dr hostPort=10.0.0.24:6061 
      EndPoint target = new EndPoint( host, port);
      for( EndPoint ep: endPointList) {
         if( ep.equals( target)) {
            return ep.getDomainName();
         }
      }
      //return host+":"+port;
      throw new RuntimeException("Could not find RFSS Domain Name for " +
            host +":" +port);
   }

   public String getRfssHostPort( String domainName)
   {
      for( EndPoint ep: endPointList) {
         String dn = ep.getDomainName();
         //showln("Comparing...dn="+dn+" domainName="+domainName);
         if( dn != null  && dn.equals(domainName)) {
            return ep.getHost() +":" + ep.getPort();
         }
      }
      return "";
   }

   //---------------------------------------------------------------------------
   public void displayTableStatus() {

      showln("\n+++++++++++++++++++++++++");
      showln("sipMessages="+sipMessages.size());
      showln("pendingSession="+pendingSessions.size());
      showln("pttSessionsByDestination="+pttSessionsByDestination.size());
   
      showln("transactionTable="+transactionTable.size());
      showln("inviteTransaction="+inviteTransactions.size());
      showln("ackDialog="+ackDialog.size());
      showln("pttSessions="+pttSessions.size());
      showln("+++++++++++++++++++++++++\n");
   }

   // constructor
   //--------------------------------------------------------------
   public MessageProcessor( )
         throws PeerUnavailableException
   {
      this( new LinkedList<CapturedPacket>());
   }

   public MessageProcessor( LinkedList<CapturedPacket> capturedPackets)
         throws PeerUnavailableException
   {
      setCapturedPackets( capturedPackets);

      transactionTable = new Hashtable<String, String>();
      inviteTransactions = new LinkedHashMap<String, LinkedList<CapturedSipMessage>>();
      ackDialog = new LinkedHashMap<String, LinkedList<CapturedSipMessage>>();
      pttSessions = new LinkedHashMap<Integer, CapturedPttMessage>();
      pendingSessions = new Hashtable<String, MediaSession>();
      sipMessages = new LinkedList<CapturedSipMessage>();
      pttSessionsByDestination = new Hashtable<String, MediaSession>();

      SipFactory sipFactory = SipFactory.getInstance();
      sipFactory.setPathName("gov.nist");
      messageFactory = sipFactory.createMessageFactory();
   }

   public void clearTables()
   {
      errorFlag = false;
      errorString = "";
      sipMessages.clear();
      transactionTable.clear();
      inviteTransactions.clear();
      ackDialog.clear();
      pttSessionsByDestination.clear();
   }

   public void logError(String errorString) {
      logger.error( errorString);
      errorFlag = true;
      this.errorString = errorString;
   }
   public void logError(String errorString, Exception ex) {
      logger.error( errorString, ex);
      errorFlag = true;
   }

   
//   public boolean isControlMessage( String messageString) {
//      boolean bflag = false;
//      // check the first 10 is not ASCII ? 
//      for(int i=0; i < 10; i++) {
//         char c = messageString.charAt(i);
//         if( !Character.isLetterOrDigit( c)) {
//            bflag = true;
//            break;
//         }
//      }
//      return bflag;
//   }
   
   private void sortPacket(CapturedPacket capturedPacket, XPacketType packetType)
      throws P25BlockException
   {      
      UDPPacket udpPacket = (UDPPacket) capturedPacket.getPacket();
      InetAddress destInetAddress = udpPacket.dst_ip;
      String host = destInetAddress.getHostAddress();
      int port = udpPacket.dst_port;
      EndPoint packetAddress = new EndPoint(host, port);

      int packetNumber = capturedPacket.getPacketNumber();
      long timeStamp = capturedPacket.getTimeStamp();

      if( verbose) {
         logger.info("-------------------------------------- id="+packetNumber);
         logger.info("sortPacket: " + packetAddress + " PacketType=" + packetType);
         logger.info("sortPacket: srcAddr=" + udpPacket.src_ip.getHostAddress() + 
                  " srcPort=" + udpPacket.src_port);
      }

      byte[] data = udpPacket.data;
      boolean sflag = getEndPointList().contains( packetAddress);

      if( verbose)
         logger.info("isSipEndPoint: "+sflag+" packetAddress:"+packetAddress);

      if ( sflag) {

         String messageString = new String(data);
         if( verbose)
            logger.info("messageString=\n"+messageString);
         try {
            // SIP Responses start with the string SIP
            if ((!messageString.startsWith("SIP"))
                  && (packetType == XPacketType.SIPREQUEST || 
                      packetType == XPacketType.SIPMESSAGE)) {

               Request request = messageFactory.createRequest(messageString);
               ViaHeader viaHeader = (ViaHeader)request.getHeader(ViaHeader.NAME);
               String transactionId = viaHeader.getBranch();
               String radicalName = viaHeader.getHost();

               // radicalName=02.ad5.bee07.p25dr
               // <rfssId>.<sysId>.<wacn>.<holder>
               String srcRfssConfig = radicalName;
               String dstRfssConfig = getRfssDomainName(host, port);

               logger.info("*** NOT SIP: radicalName=" +radicalName + " dst-host="+host+":"+port);
               logger.info(" src="+srcRfssConfig +" dst="+dstRfssConfig);

               CapturedSipMessage captured = new CapturedSipMessage( packetAddress, timeStamp, request, this);
               captured.setPacketNumber(packetNumber);
               sipMessages.add(captured);

               // compute Dialog ID
               Message msg =  captured.getMessage();
               String fromTag = ((FromHeader) msg.getHeader("From")).getTag();
               String toTag = ((ToHeader) msg.getHeader("To")).getTag();
               String callId = ((CallIdHeader) msg.getHeader("Call-ID")).getCallId();


               if (Request.INVITE.equals(request.getMethod())) {
//TODO: Capture Data
ContactHeader senderContactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
SipURI senderContactUri = (SipURI) senderContactHeader.getAddress().getURI();
String senderDomainName = senderContactUri.getHost();
logger.info("+++packetEndpoint="+packetAddress+" senderDomainName="+senderDomainName);

                  // record INVITE transactions for measurements
                  if (!inviteTransactions.containsKey(transactionId))
                     inviteTransactions.put(transactionId, new LinkedList<CapturedSipMessage>());
                  inviteTransactions.get(transactionId).add(captured);

                  byte[] contents = request.getRawContent();
                  if (contents == null) {
                     logError("Missing content in the INVITE");
                     return;
                  }
                  SdpFactory sdpFactory = SdpFactory.getInstance();
                  try {
                     ContentTypeHeader ctHeader = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
                     SessionDescription sdpAnnounce = null;
                     if ( SipUtils.isAppSdpContentTypeHeader(ctHeader)) {
                        sdpAnnounce = sdpFactory.createSessionDescription( new String(contents));

                     } else {
                        ContentList clist = ContentList.getContentListFromMessage(request);
                        sdpAnnounce = clist.getSdpContent().getSessionDescription();
                     }
                     if( sdpAnnounce==null) {
                        logger.debug("sdpAnnounce NULL, return...");
                        return;
                     }
                     MediaDescription m = (MediaDescription) sdpAnnounce.getMediaDescriptions(true).get(0);
                     String ipAddress = sdpAnnounce.getConnection().getAddress();

                     if (m.getConnection() != null) {
                        ipAddress = m.getConnection().getAddress();
                     }
                     EndPoint endPoint = new EndPoint(ipAddress, m.getMedia().getMediaPort());

                     // media session: 10.0.24:25000
                     MediaSession pendingSession = new MediaSession(endPoint);
                     logger.info(" ---> pendingSession: endPoint="+endPoint);

                     // 10.0.0.24:5060 -> 02.ad5.bee07.p25dr 
                     pendingSession.setOwningRfssDomainName( dstRfssConfig);
                     pendingSession.setRemoteRfssDomainName( srcRfssConfig);
                     pttSessionsByDestination.put(endPoint.toString(), pendingSession);

                     logger.info("add PttSession : " + endPoint);
                     pendingSessions.put(transactionId, pendingSession);

                  } 
                  catch (Exception ex) {
                     logger.error("Error in parsing sdp ", ex);
                     String xmsg = "Error in parsing sdp \n" + "Here is the request [" + messageString + "]";
                     logError(xmsg);
                     return;
                  }
               } 
               else if (Request.ACK.equals(request.getMethod())) {
                  //logger.debug("*** record ACK in the dialog ***");
                  // record ACK in the dialog
                  String dialogId = callId + fromTag + toTag;
                  if (ackDialog.containsKey(dialogId))
                     ackDialog.get(dialogId).add(captured);
               }

            } else if (messageString.startsWith("SIP")
                  && (packetType == XPacketType.SIPRESPONSE || 
                      packetType == XPacketType.SIPMESSAGE)) {

               Response response = messageFactory.createResponse(messageString);
               CapturedSipMessage captured = new CapturedSipMessage(
                     packetAddress, timeStamp, response, this);
               captured.setPacketNumber(packetNumber);
               sipMessages.add(captured);

               // compute Dialog ID
               Message msg =  captured.getMessage();
               String fromTag = ((FromHeader) msg.getHeader("From")).getTag();
               String toTag = ((ToHeader) msg.getHeader("To")).getTag();
               String callId = ((CallIdHeader) msg.getHeader("Call-ID")).getCallId();

               String method = ((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod();

logger.info("*** YES SIP: method="+method+" SC="+response.getStatusCode());
//TODO: Capture Data
ContactHeader senderContactHeader = (ContactHeader) response.getHeader(ContactHeader.NAME);
SipURI senderContactUri = (SipURI) senderContactHeader.getAddress().getURI();
String senderDomainName = senderContactUri.getHost();
logger.info("+++SIP packetEndpoint="+packetAddress+" senderDomainName="+senderDomainName);

               if ((response.getStatusCode()==Response.TRYING ||
                    response.getStatusCode()==Response.RINGING) &&
                    Request.INVITE.equals(method))
               {
                  String transactionId = ((ViaHeader) response
                        .getHeader(ViaHeader.NAME)).getBranch();

                  // record INVITE transactions for measurements
                  if (inviteTransactions.containsKey(transactionId))
                     inviteTransactions.get(transactionId).add( captured);

                  // record 100 TRYING in the dialog
                  String dialogId = callId + fromTag + toTag;
//logger.debug("*** record 100 TRYING in dialogId..."+dialogId);
                  if (!ackDialog.containsKey(dialogId))
                     ackDialog.put(dialogId, new LinkedList<CapturedSipMessage>());
                  ackDialog.get(dialogId).add(captured);
               }
               else if (response.getStatusCode()==Response.OK
                     && Request.INVITE.equals(method))
               {
                  String transactionId = ((ViaHeader) response.getHeader(ViaHeader.NAME)).getBranch();
//logger.debug("*** record INVITE - tid="+transactionId);

                  // record INVITE transactions for measurements
                  if (inviteTransactions.containsKey(transactionId))
                     inviteTransactions.get(transactionId).add( captured);

                  // record 200 OK in the dialog
                  String dialogId = callId + fromTag + toTag;
//logger.debug("*** record 200 OK in dialogId..."+dialogId);
                  if (!ackDialog.containsKey(dialogId))
                     ackDialog.put(dialogId, new LinkedList<CapturedSipMessage>());
                  ackDialog.get(dialogId).add(captured);

                  byte[] contents = response.getRawContent();
                  if (contents == null) {
                     logger.error("Missing content in the 200 OK ");
                     String xmsg = "Error in parsing sdp \n" + "Here is the response [" + messageString + "]";
                     logError(xmsg);
                     return;
                  }
                  SdpFactory sdpFactory = SdpFactory.getInstance();
                  try {
                     SessionDescription sdpAnnounce = null;
                     ContentTypeHeader ctHeader = (ContentTypeHeader) response.getHeader(ContentTypeHeader.NAME);
                     if ( SipUtils.isAppSdpContentTypeHeader(ctHeader)) {
                        sdpAnnounce = sdpFactory.createSessionDescription( new String(contents));
                     } else {
                        ContentList contentList = ContentList.getContentListFromMessage(response);
                        sdpAnnounce = contentList.getSdpContent().getSessionDescription();
                     }
                     String ipAddress = sdpAnnounce.getConnection().getAddress();

                     MediaDescription m = (MediaDescription) sdpAnnounce.getMediaDescriptions(true).get(0);
                     if (m.getConnection() != null) {
                        ipAddress = m.getConnection().getAddress();
                     }
                     EndPoint endPoint = new EndPoint(ipAddress, m.getMedia().getMediaPort());
                     MediaSession session = pendingSessions.get(transactionId);
                     if (session != null) {
                        session.setSource(endPoint);
                        MediaSession reversed = session.reverse();
                        reversed.setDestination( endPoint);
                        pttSessionsByDestination.put(endPoint.toString(), reversed);
                        logger.info("process response : pttSession : " + endPoint);
                     } else {
                        String xmsg = "Could not find transaction ID  " + transactionId;
                        logger.error( xmsg);
                        logger.error(pendingSessions.keySet());
                        logError(xmsg);
                        return;
                     }

                  } catch (Exception ex) {
                     logger.error("Error in parsing sdp ", ex);
                     String xmsg = "Here is the response [" + messageString + "]";
                     logError(xmsg);
                  }
               }
               else {
                  logger.debug("*** Unhandled SIP 100 XXX ...");
               }
            }
         }
         catch (ParseException ex) {
            //logger.error("Error parsing message", ex);
            //logger.error("Message : [" + messageString + "]");
            //throw new RuntimeException("Parser error: "+ex);
         }
         catch (Exception ex) {
            //logger.error("NPE: ex=", ex);
            showln("NPE: ex="+ ex);
         }
      } 
      else if (pttSessionsByDestination.containsKey(packetAddress.toString())
            && packetType == XPacketType.PTT)
      {
         //logger.info("Adding PTT Packet: " +packetNumber +" --> " +packetAddress);
         MediaSession session = pttSessionsByDestination.get(packetAddress.toString());
         try {
            CapturedPttMessage pttMsg = session.addPttMessage(packetNumber,data,timeStamp);

            // store measurement required PTT messages
            int type = pttMsg.getP25Payload().getISSIPacketType().getPT();
            switch (type) {
            case ISSIPacketType.PTT_TRANSMIT_REQUEST:
            case ISSIPacketType.PTT_TRANSMIT_GRANT:
            case ISSIPacketType.PTT_TRANSMIT_START:
            case ISSIPacketType.HEARTBEAT_QUERY:
               if (!pttSessions.containsKey(type)) {
                  pttSessions.put(new Integer(type), pttMsg);
               }
               break;
            case ISSIPacketType.PTT_TRANSMIT_PROGRESS:
               logger.debug("PTT_TRANSMIT_PROGRESS SEEN");
               break;
            default:
               logger.debug("#### Unknown PTT packet type: " + type);
               break;
            }
         }
         catch( P25BlockException ex) {
            logger.debug("CaputredPttMessage: could not extract PTT message...");
            throw ex;
         }
      } 
      else
      {
         logger.debug("Dropping PTT packet " + packetAddress);
      }
   }

   public String getSipMessages() 
   {
      logger.debug("getSipMessages(): size=" + capturedPackets.size());
      clearTables();
      for (CapturedPacket packet: capturedPackets) {
         try {
            sortPacket(packet, XPacketType.SIPMESSAGE);
         } catch(P25BlockException ex) { }
      }

      logger.debug(" *** ENCODING: SIP size="+sipMessages.size());
      StringBuffer sbuf = new StringBuffer();
      for (CapturedSipMessage sipMsg: sipMessages) {
         //showln("  --- sipMsg=\n"+sipMsg.toString());
         sbuf.append( sipMsg.toString());
      }
      return sbuf.toString();
   }

   public String getPttMessages()
   {
      logger.debug("getPttMessages(): size=" + capturedPackets.size());
      clearTables();
      for (CapturedPacket packet: capturedPackets) {
         try {
            sortPacket(packet, XPacketType.SIPMESSAGE);
         } catch(P25BlockException ex) {
            logger.debug("Error in sortPacket: "+packet.getPacketNumber(), ex);
         }
      }

      logger.debug("pttSessionsByDestionation " + pttSessionsByDestination);
      for (CapturedPacket packet: capturedPackets) {
         try {
            sortPacket(packet, XPacketType.PTT);
         } catch(P25BlockException ex) {
            logger.debug("Error in sortPacket: "+packet.getPacketNumber(), ex);
         }
      }

      logger.debug(" *** ENCODING: PTT size="+pttSessionsByDestination.values().size());
      StringBuffer sbuf = new StringBuffer();
      for (MediaSession mediaSession: pttSessionsByDestination.values()) {
         for (CapturedPttMessage pttMsg: mediaSession.getCapturedPttMessages()) {
            sbuf.append( pttMsg.toString());
         }
      }
      //logger.debug("getPttMessages(): sbuf=\n"+sbuf.toString());
      return sbuf.toString();
   }

   public List<EndPoint> getTargetList()
   {
      logger.debug("getEndPointList(): size=" + capturedPackets.size());
      int ka = 5060;
      int kb = 5070;
      //int ka = 5000;
      //int kb = 60000;
      if( ports != null) {
         ka = ports[0];
         kb = ports[1];
      }
      logger.debug("getEndPointList(): ka=" + ka + " kb=" + kb);

      StringBuffer sb = new StringBuffer();
      List<EndPoint> list = new ArrayList<EndPoint>();
      for (CapturedPacket capturedPacket: capturedPackets)
      {
         UDPPacket udpPacket = (UDPPacket) capturedPacket.getPacket();
         InetAddress destInetAddress = udpPacket.dst_ip;
         String host = destInetAddress.getHostAddress();
         int port = udpPacket.dst_port;

         EndPoint ep = new EndPoint(host,port);
         //if( !list.contains( ep) && port < 25000 && 
         //    (port >= 5000 && port <= 5100))
         if( !list.contains( ep) && 
               (port >= ka && port <= kb))
         {
            list.add( ep);

            // try to decode domainName
            String msgString = new String( udpPacket.data);
            logger.info("+++msg(0): ep="+ep+"\n"+msgString);
            if( msgString != null)
            try {
              if ( !msgString.startsWith("SIP")) {

                 Request request = messageFactory.createRequest(msgString);
                 //String radicalName = ((SipURI) request.getRequestURI()).getUser();
                 String sipHost = ((SipURI) request.getRequestURI()).getHost();
                 String method = request.getMethod().trim();
logger.info("+++endpoint(1): method="+method+"  sipHost="+sipHost);
                 if( Request.BYE.equals( method))
                    continue;
 
                 if (Request.INVITE.equals( method)) {
                    RouteHeader routeHeader = (RouteHeader) request.
                       getHeader(RouteHeader.NAME);
logger.info("+++endpoint(1): routeHeader="+routeHeader);
                    if( routeHeader != null) {
                       SipURI routeUri = (SipURI) routeHeader.getAddress().getURI();
logger.info("+++endpoint(1): routeUri="+routeUri);
                       if( routeUri != null) {
                          String domainName = routeUri.getHost();
                          logger.info("+++endpoint(1)="+ep+" domainName="+domainName);
                          ep.setDomainName( domainName);
                       }
                    }
                    else {
logger.info("+++endpoint(11): setSipHost="+sipHost);
                       ep.setDomainName( sipHost);
                    }
                 } 
                 else {
logger.info("+++endpoint(12): setSipHost="+sipHost);
                    ep.setDomainName( sipHost);
                 }
              } 
              else if (msgString.startsWith("SIP")) {
  
                 Response response = messageFactory.createResponse(msgString);
                 /*** use Via header                
                 CallIdHeader cidHdr = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
                 String cid = cidHdr.getCallId();
                 String[] parts = cid.split("\\@");
                 logger.info("+++endpoint(2): parts.length="+parts.length);
                 // bee07ad50333@02.ad5.bee07.p25dr
                 if( parts.length==2) {
                    logger.info("+++endpoint(2)="+ep+" p1="+parts[1]);
                    ep.setDomainName( parts[1]);
                 }
                  ***/
                 ViaHeader viaHeader = (ViaHeader) response.getHeader(ViaHeader.NAME);
                 String viaHost = viaHeader.getHost();
                 logger.info("+++endpoint(2): viaHost="+viaHost);
                 ep.setDomainName( viaHost);

              }
            } catch (Exception ex) {
              // ignore and move on
              //logger.debug("Skip the non-SIP message...");
              logger.debug("getTargetList: "+ex.toString());
              showln("getTargetList: "+ex.toString());
              sb.append(ex.getMessage());
              sb.append("\n");
            }
         }
      }  // for-loop

      if( sb.toString().length() > 0) {
         throw new IllegalArgumentException( sb.toString());
      }
      return list;
   }
}
