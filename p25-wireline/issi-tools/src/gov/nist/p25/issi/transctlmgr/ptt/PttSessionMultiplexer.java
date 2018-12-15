//
package gov.nist.p25.issi.transctlmgr.ptt;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Hashtable;
import java.util.PriorityQueue;
import javax.sdp.SessionDescription;
import org.apache.log4j.Logger;

import gov.nist.p25.common.util.ByteArrayUtil;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.p25payload.ISSIPacketType;
import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.p25.issi.p25payload.PacketType;
import gov.nist.p25.issi.rfss.tester.TesterUtility;
import gov.nist.rtp.RtpErrorEvent;
import gov.nist.rtp.RtpException;
import gov.nist.rtp.RtpListener;
import gov.nist.rtp.RtpPacket;
import gov.nist.rtp.RtpPacketEvent;
import gov.nist.rtp.RtpSession;
import gov.nist.rtp.RtpStatusEvent;
import gov.nist.rtp.RtpTimeoutEvent;

/**
 * A class that listens for RTP packets and multiplexes to the appropriate
 * PttSession based on the tsn.
 */
public class PttSessionMultiplexer implements RtpListener, PttSessionInterface {
   
   private static Logger logger = Logger.getLogger(PttSessionMultiplexer.class);

   /** A set of smf sessions that this mux multiplexes */
   private Hashtable<Integer, SmfSession> pttSessions;   
   private Hashtable<SmfSession,Integer> pttSessionToTsnMap;
   
   /** Each SU In the group served owns a TSN * */
   private Hashtable<String, Integer> tsnTable;

   /** The session description used to create this mux */
   private SessionDescription sessionDescription;

   private RtpSession rtpSession;
   private LinkType linkType;
   private HeartbeatTransmitter heartbeatTransmitter;
   private HeartbeatReceiver heartbeatReceiver;
   private RfssConfig owningRfss;
   private int gid;
   private SmfSession currentTransmitter;
   private String remoteRfssRadicalName;
   private PriorityQueue<SmfSession> pendingTransmitters;
   private byte[] rtpPayload;

   //NOTE: keep the incomingTSN for MUTE/UNMUTE
   private int currentTSN;
   public void setCurrentTSN(int tsn) { currentTSN=tsn; }
   public int getCurrentTSN() { return currentTSN; }


   // constructor
   public PttSessionMultiplexer(RtpSession rtpSession, int gid) {
      pttSessions = new Hashtable<Integer, SmfSession>();
      tsnTable = new Hashtable<String, Integer>();
      pttSessionToTsnMap = new Hashtable<SmfSession,Integer> ();
      this.rtpSession = rtpSession;
      this.rtpSession.addRtpListener(this);
      this.linkType = LinkType.GROUP_SERVING;
      this.startHeartbeatComponents();
      this.gid = gid;
      this.pendingTransmitters = new PriorityQueue<SmfSession>();
   }

   public void setOwningRfss(RfssConfig owningRfss) {
      this.owningRfss = owningRfss;
   }

   public RfssConfig getOwningRfss() {
      return this.owningRfss;
   }

   /**
    * Get the tsn for the given su.
    */
   public int getTsn(String suRadicalName) {
      return this.tsnTable.get(suRadicalName);
   }

   public void addSu(String suRadicalName, int tsn) {
      logger.debug("PttSessionMux: addSu " + suRadicalName);
      this.tsnTable.put(suRadicalName, tsn);
   }

   public boolean isSuSubscribed(String suRadicalName) {
      return this.tsnTable.containsKey(suRadicalName);
   }

   public int getMyRtpRecvPort() {
      return this.rtpSession.getMyRtpRecvPort();
   }

   public void addPttSession(int tsn, SmfSession smfSession) {
      this.pttSessions.put(tsn, smfSession);
      this.pttSessionToTsnMap.put(smfSession, tsn);
      smfSession.multiplexer = this;
      smfSession.setGroupId(gid);
   }

   public PttSession getPttSession(int tsn) {
      return this.pttSessions.get(tsn);
   }

   public void handleRtpPacketEvent(RtpPacketEvent rtpPacketEvent) {
      try {
         RtpPacket rtpPacket = rtpPacketEvent.getRtpPacket();
//logger.debug("ZMARKER(21): rtpPacket="+rtpPacket.toString());
// #368
rtpPacket.setPT(100);
//logger.error("#386 setting PT from 72 to 100...");

         //byte[] rtpPayload = rtpPacket.getPayload();
         rtpPayload = rtpPacket.getPayload();
         P25Payload p25Payload = new P25Payload(rtpPayload);

         ISSIPacketType issiPacketType = p25Payload.getISSIPacketType();
         PacketType packetType = issiPacketType.getPacketType();
         int incomingTSN = issiPacketType.getTransmissionSequenceNumber();
	 setCurrentTSN( incomingTSN);

         if (!PttSession.PTT_TEST_MODE) {
            this.owningRfss.getRFSS().capturePttPacket(rtpPacket,
                  p25Payload, false, this);
         }

         if (this.heartbeatReceiver.isPacketTypeHandled(issiPacketType
               .getPacketType())) {
            this.heartbeatReceiver.handlePttPacket(p25Payload);
         } else {
            //===PacketType packetType = issiPacketType.getPacketType();
            if (packetType == PacketType.PTT_TRANSMIT_MUTE
                  || packetType == PacketType.PTT_TRANSMIT_UNMUTE
                  || packetType == PacketType.PTT_TRANSMIT_WAIT
                  || packetType == PacketType.PTT_TRANSMIT_GRANT
                  || packetType == PacketType.PTT_TRANSMIT_DENY) {
               // Mute and unmute and wait are directed towards specific
               // remote
               // TSNs. Note that the remote tsn is stamped on the packet
               // when it is constructed ( see PttSession.java ).
               //===int incomingTSN = issiPacketType.getTransmissionSequenceNumber();
               PttSession pttSession = this.pttSessions.get(incomingTSN);
               if (pttSession == null) {
                  logger.error("Could not find PTT session for"
                        + incomingTSN + " discarding packet ");
               } else {
                  pttSession.handleIncomingPttPacket(p25Payload);
               }
            } else {
               for (PttSession pttSession : this.pttSessions.values()) {
                  pttSession.handleIncomingPttPacket(p25Payload);
               }
            }
         }
      } catch (Exception ex) {
         // java.lang.IllegalStateException: Timer already cancelled.
         ex.printStackTrace();
         logger.error("Invalid PTT packet", ex);
      }
   }

   public RtpSession getRtpSession() {
      return this.rtpSession;
   }

   public void handleRtpErrorEvent(RtpErrorEvent arg0) {
      // TODO Auto-generated method stub
   }

   public void handleRtpStatusEvent(RtpStatusEvent arg0) {
      // TODO Auto-generated method stub
   }

   public void handleRtpTimeoutEvent(RtpTimeoutEvent arg0) {
      // TODO Auto-generated method stub
   }

   public void setSessionDescription(SessionDescription sessionDescription) {
      this.sessionDescription = sessionDescription;
   }

   public SessionDescription getSessionDescription() {
      return this.sessionDescription;
   }

   public void shutDown() {
      this.rtpSession.shutDown();
      for (PttSession pttSession : this.pttSessions.values()) {
         pttSession.shutDown();
      }
   }

   public HeartbeatReceiver getHeartbeatReceiver() {
      return this.heartbeatReceiver;
   }

   public HeartbeatTransmitter getHeartbeatTransmitter() {
      return this.heartbeatTransmitter;
   }

   public LinkType getLinkType() {
      return this.linkType;
   }

   public String getSessionType() {
      return SmfSession.SESSION_TYPE;
   }

   public void startHeartbeatComponents() {
      // Create connection maintenance heartbeat components
      // check for 0 port before starting the reciever.
      heartbeatReceiver = new HeartbeatReceiver(this);
      if (this.rtpSession.getRemoteRtpRecvPort() > 0) {
         this.heartbeatTransmitter = new HeartbeatTransmitter(this);
      }
   }

   public void setRemoteRtpRecvPort(int remoteRecvPort) {
      int originalPort = rtpSession.getRemoteRtpRecvPort();
      if (this.heartbeatTransmitter != null && originalPort != remoteRecvPort) {
         // If the port got reset, then kill the original transmitter.
         this.heartbeatTransmitter.stop();
      }
      if (remoteRecvPort > 0 && originalPort != remoteRecvPort) {
         this.heartbeatTransmitter = new HeartbeatTransmitter(this);
      }
      this.rtpSession.setRemoteRtpRecvPort(remoteRecvPort);
      logger.debug("pttSessionMultiplexer : remoteRtpRecvPort = "
            + this.rtpSession.getRemoteRtpRecvPort() + " originalPort = "
            + originalPort);
   }

   /**
    * Set the remote IP address of this rtp session belonging to this mux.
    * 
    * @param ipAddress --
    *            IP Address of the remote host to which we are sending media.
    * 
    */
   public void setRemoteIpAddress(String ipAddress) {
      this.rtpSession.setRemoteIpAddress(ipAddress);
   }

   /**
    * This is for advanced RTP resource management where the RTP port is not
    * available initially. The session is setup as a half session. As soon as
    * the Recv port is set up, the Session may be completed with a re-invite
    * using the same Call ID as initially assigned.
    * 
    * 
    */
   public void assignRtpRecvPort() {
      int rand = 25000;
      boolean assigned = false;
      for (int i = 0; i < 100 && !assigned; i++) {
         try {
            rand = rand + 2;
            this.rtpSession.resetMyRtpRecvPort(rand);
            assigned = true;
         } catch (RtpException ex) {
            logger.debug("failed to assign port - retry");
         }
      }
      try {
         this.rtpSession.receiveRTPPackets();
      } catch (Exception ex) {
         logger.fatal("Unexpected exception! ");
      }

   }

   public void stopReceiver() {
      this.rtpSession.stopRtpPacketReceiver();
   }

   @Override
   public String toString() {

      StringBuffer sbuf = new StringBuffer();
      sbuf.append("<ptt-session\n");
      sbuf.append(" sessionType=");
      sbuf.append("\"smfSession\"\n");
      sbuf.append(" rfssId=\""
                     + (owningRfss != null ? owningRfss.getRfssId() : 0)
                     + "\"\n");
      sbuf.append(" myIpAddress=\""
                     + rtpSession.getMyIpAddress().getHostAddress()
                     + "\"\n");
      sbuf.append(" myRtpRecvPort=\"" 
                     + rtpSession.getMyRtpRecvPort()
                     + "\"\n");
      sbuf.append(" remoteIpAddress=\""
                     + rtpSession.getRemoteIpAddress() + "\"\n");
      sbuf.append(" remoteRtpRecvPort=\""
                     + rtpSession.getRemoteRtpRecvPort() + "\"\n");
      sbuf.append(" linkType=\"" + this.linkType + "\"\n");
      sbuf.append("/>\n");
      return sbuf.toString();

   }

   public void logPttPacket(RtpPacket rtpPacket, P25Payload p25Payload,
         boolean isSender, long timeStamp, int sequenceNumber, 
         String remoteRfssDomainName, String ipAddress, int port) {

      // We don't need this if we are in test mode.
      if (PttSession.PTT_TEST_MODE)
         return;

      //PrintWriter rtpLog = TopologyConfig.getRtpMessageLogStream();
      PrintWriter rtpLog = TesterUtility.getRtpMessageLogStream();
      if (rtpLog != null) {

         if (this.owningRfss == null) {
            logger.warn("Warning -- owning rfss is null for " + this);
         }
         StringBuffer sbuf = new StringBuffer();
         sbuf.append("\n<!-- PTT PACKET BEGIN 2 -->");
         sbuf.append("\n<ptt-packet \n");
         sbuf.append(" receptionTime=\"" + timeStamp + "\"\n");
         sbuf.append(" packetNumber=\"" + sequenceNumber + "\"\n");
         sbuf.append(" isSender=\"" + isSender + "\"\n");
         sbuf.append(" rawdata=\"");
         sbuf.append( ByteArrayUtil.toHexString( rtpPayload, 16));
         sbuf.append("\"\n");

         sbuf.append(" receivingRfssId=\""
               + (this.owningRfss != null ? this.owningRfss
                     .getDomainName() : "UNKNOWN") + "\"\n");
         sbuf.append(" sendingRfssId=\"" + remoteRfssDomainName + "\"\n>\n");
         sbuf.append(this.toString());
         sbuf.append("\n");
         sbuf.append(rtpPacket.toString());
         sbuf.append(p25Payload.toString());
	 /***
         sbuf.append("\n<rtf-format>\n");
         sbuf.append("<![CDATA[\n");
	 // disbale RTF support
	 sbuf.append(" RTF Endcoding not supportted. ");
         sbuf.append("]]>\n");
         sbuf.append("</rtf-format>");
	  ***/
         sbuf.append("\n</ptt-packet>");

         rtpLog.println(sbuf.toString());
         rtpLog.flush();
      }
   }

   public Collection<SmfSession> getSmfSessions() {
      return this.pttSessions.values();
   }

   /**
    * @param currentTransmitter
    *            the currentTransmitter to set
    */
   public void pushTransmitter(SmfSession newTransmitter) throws Exception {
      logger.debug("pushTransmitter: " + newTransmitter);
      if (this.currentTransmitter != null) {
         this.pendingTransmitters.offer(currentTransmitter);
         assert this.pttSessionToTsnMap.containsKey(newTransmitter);
         int tsn = this.pttSessionToTsnMap.get(newTransmitter);
         this.currentTransmitter.setLosingAudio(tsn);
      }
      this.currentTransmitter = newTransmitter;
   }

   /**
    * Pop the previous transmitter from the stack and make him the current
    * transmitter. This removes the next highest muted transmitter and makes it
    * ready to send by sending the MMF a mute.
    */
   public void popTransmitter() {
      try {
         if (this.pendingTransmitters.isEmpty()) {
            this.currentTransmitter = null;
         } else {
            while (!this.pendingTransmitters.isEmpty()) {
               SmfSession candidate = this.pendingTransmitters.peek();
               if (candidate.getSmfTransmitter().getCurrentState() != SmfTxState.TERMINATED) {
                  // Completed the transmission so we set ourselves up in
                  // the non losing audio state to get ready for the unmute
                  // from the mmf.
                  this.currentTransmitter = candidate;
                  this.pendingTransmitters.remove(candidate);
                  assert this.pttSessionToTsnMap.containsKey(candidate);
                  int tsn = this.pttSessionToTsnMap.get(candidate);
                  this.currentTransmitter.clearLosingAudio(tsn);                     
                  break;
               }
            }
         }
      } catch (Exception ex) {
         logger.error("Got an unexpected exception ", ex);
         throw new RuntimeException("unexpected exception ", ex);
      }
   }

   /**
    * @return the currentTransmitter
    */
   public SmfSession getCurrentTransmitter() {
      return currentTransmitter;
   }

   /**
    * Send a heartbeat query to the Group Home.
    * 
    * @throws Exception --
    *             if something bad happened when sending the query.
    */
   public void sendHeartbeatQuery() throws PttException {

      if (this.rtpSession.getRemoteIpAddress() == null
            || rtpSession.getRemoteRtpRecvPort() <= 0
            || this.heartbeatTransmitter == null)
         throw new PttException(
               "Cannot send RTP query -- the session is not established");
      this.heartbeatTransmitter.sendHeartbeatQuery();
   }

   /**
    * @param remoteRfssRadicalName the remoteRfssRadicalName to set
    */
   public void setRemoteRfssDomainName(String remoteRfssRadicalName) {
      this.remoteRfssRadicalName = remoteRfssRadicalName;
   }

   /**
    * @return the remoteRfssRadicalName
    */
   public String getRemoteRfssDomainName() {
      return remoteRfssRadicalName;
   }
}
