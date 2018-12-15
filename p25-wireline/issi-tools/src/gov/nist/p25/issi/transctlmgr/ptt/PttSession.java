//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.common.util.ByteArrayUtil;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.p25payload.*;
import gov.nist.p25.issi.rfss.RFSS;
import gov.nist.p25.issi.rfss.tester.TesterUtility;
import gov.nist.p25.issi.transctlmgr.PttPointToMultipointSession;
import gov.nist.p25.issi.utils.ByteUtil;
import gov.nist.rtp.*;

import java.io.*;
import java.util.*;
import javax.sdp.SessionDescription;

import org.apache.log4j.Logger;

/**
 * This class defines the base functionality for SMF or MMF sessions.
 */
public abstract class PttSession implements RtpListener, PttSessionInterface,
      Comparable<PttSession> {

   private boolean PTT_DELAY_2MS = true;

   /** The group Id in case we are transmitting to a group. */
   private int groupId = 0; // If 0 this is an SU-to-SU, not group, call.

   /** Denotes if this PTT session is being used only in test mode. */
   protected static boolean PTT_TEST_MODE = false;

   /* Denotes that the receiver for this session is in the MUTED state. */
   protected boolean muted = false;

   private boolean sendMute = true;

   /** Set the owning rfss for this session. */
   private RfssConfig owningRfss = null;

   /** The RFSS to which I am sending and getting data */
   private String remoteRfssDomainName;
   
   /** The encapsulated RTP session. */
   private RtpSession rtpSession = null;

   private Hashtable<Integer, Integer> unitIdToTsnMap = new Hashtable<Integer, Integer>();
   
   /** The losing audio flag for this transmission */
   private HashSet<Integer> losingAudioMap = new HashSet<Integer>();

   /** The wide area communication network ID. */
   private int wacnId = 0;

   /** The system ID. */
   private int systemId = 0;

   /** The service options for this RFSS. */
   private static ServiceOptions serviceOptions = new ServiceOptions();

   /** The link type for this RFSS. */
   private LinkType linkType;

   /** The synchronization source. */
   private int ssrc;

   /** The session type (SMF or MMF). */
   public String sessionType = "";

   /** The mute receiver. */
   public MuteReceiver muteReceiver = null;

   /** The mute heartbeat transmitter. */
   MuteTransmitter muteTransmitter = null;

   /** The communication maintenance heartbeat transmitter. */
   private HeartbeatTransmitter heartbeatTransmitter = null;

   /** The communication maintenance heartbeat receiver. */
   private HeartbeatReceiver heartbeatReceiver = null;

   /** The SDP information associated with the PttSession */
   private SessionDescription sessionDescription;

   /** The point to multipoint session associated with this ptt session */
   private PttPointToMultipointSession pttPointToMultipointSession;

   /** The logger for this class. */
   private static Logger logger = Logger.getLogger(PttSession.class);

   /** A set of generated tsns */
   private HashSet<Integer> myTsnSet = new HashSet<Integer>();

   /** A set of remote RFSS TSN information objects. */
   private HashSet<Integer> remoteTSNs = new HashSet<Integer>();

   /**
    * Timestamp to be used for RTP packets. Note that an RTP timestamp is not
    * set as wallclock time. Instead, it is randomly set initially to an
    * arbitrary value (or 0) in 125 microsecond resolution, then incremented
    * for each subsequent RTP packet that contains a PTT PROGRESS (i.e.,
    * contains voice). Each increment, as defined below by
    * rtpTimeStampIncrement, represents the duration of voice samples contained
    * in each RTP packet.
    */
   private long rtpTimeStamp = 0;

   /**
    * The total duration of voice samples in 125 microsecond resolution
    * contained in an RTP (PTT IMBE) packet. This value is used to increment
    * rtpTimeStamp for each RTP packet containing voice. Each IMBE block
    * contains 20ms of voice, or (20 * 1000) / 8 = 2500 (in 125 microsecond
    * resolution).
    */
   private int rtpTimeStampIncrement = 2500; // 125 microsecond resolution

   /**
    * This is incremented every 18 IMBE packets. It wraps when > 3 (2 bits)
    */
   private int superFrameIndex = 0;

   /**
    * Number of IMBE packets sent. Used to increment superFrameIndex. Wraps
    * every 18 packets.
    */
   private int imbeIndex = 0;

   /**
    * Flags if an ISSI header word has already been sent for this talk spurt.
    * (Kameron) Only one ISSI header word per talk spurt.
    */
   private boolean issiHeaderWordSent = false;

   /** The priority type for the transmission. */
   protected TransmitPriorityType transmitPriorityType = null;

   /** The priority level of the transmission. */
   protected int transmitPriorityLevel = 0;

   /** The ptt manager that created me */
   private PttManager manager;

   //NOTE: use for MUTE/UNMUTE
   private int currentTSN;
   public int getCurrentTSN() { return currentTSN; }
   public void setCurrentTSN(int tsn) { currentTSN=tsn; }

   /**
    * Construct a PTT session.
    * 
    * @param rtpSession
    *            The encapsulated RTP session
    * @param myLinkType
    *            The link type to assign for the session.
    * @throws RtpException
    * @throws IOException
    */
   protected PttSession(RtpSession rtpSession, LinkType myLinkType,
         PttManager manager) throws IOException, RtpException {
      if (rtpSession == null)
         throw new NullPointerException("RtpSession is null!");

      this.linkType = myLinkType;
      this.ssrc = myLinkType.getValue();
      this.manager = manager;
      this.rtpSession = rtpSession;
      if (rtpSession.getMyRtpRecvPort() > 0) {
         rtpSession.receiveRTPPackets();
      } else {
         logger.debug("Deferring on starting the rtp listener because listen port is 0 ");
      }
      sessionType = this instanceof MmfSession ? "MMF" : "SMF";
   }

   /**
    * Create a PTT packet with the settings appropriate for this session.
    * 
    * @param packetType
    *            The type of packet to be created.
    * @param transmitPriorityType --
    *            transmit priority type.
    * @param transmitPriorityLevel --
    *            the transmit priority level.
    * 
    * @see Figure 48, TIA-102.BACA
    */
   private P25Payload createPttPacket(PacketType packetType, int systemId, int unitId,
         TransmitPriorityType transmitPriorityType, int transmitPriorityLevel) {
      int tsn = unitId != 0 ? this.getTsnForUnitId(systemId,unitId) : 0;

      P25Payload p25Payload = null;
      ISSIPacketType issiPacketType = null;
      PTTControlWord pttControlWord = null;
      ISSIHeaderWord issiHeaderWord = null;

      // Set ISSI Packet Type
      issiPacketType = new ISSIPacketType();
      issiPacketType.setMuteStatus(muteTransmitter.peerMuteState.getBooleanValue());

      issiPacketType.setServiceOptions(serviceOptions);
      issiPacketType.setPacketType(packetType);
      issiPacketType.setLosingAudio(this.isLosingAudio(tsn));
      // Note that THEARTBEAT is in milliseconds
      issiPacketType.setInterval(TimerValues.THEARTBEAT / 1000);

      try {
         if (packetType == PacketType.PTT_TRANSMIT_START) {

            issiPacketType.setTranssmissionSequenceNumber(tsn);
            // Set PTT Control Word for PTT START
            pttControlWord = new PTTControlWord();
            pttControlWord.setWacnId(wacnId);
            pttControlWord.setSystemId(systemId);
            pttControlWord.setUnitId(unitId);
            pttControlWord.setPriorityTypeAndPriorityLevel(
                  transmitPriorityType, transmitPriorityLevel);

         } else if (packetType == PacketType.PTT_TRANSMIT_GRANT) {

            // issiPacketType.setTranssmissionSequenceNumber(remoteTsn);

         } else if (packetType == PacketType.PTT_TRANSMIT_WAIT) {

            // issiPacketType.setTranssmissionSequenceNumber(remoteTsn);

         } else if (packetType == PacketType.PTT_TRANSMIT_DENY) {

            // issiPacketType.setTranssmissionSequenceNumber(remoteTsn);

         } else if (packetType == PacketType.PTT_TRANSMIT_END) {

            issiPacketType.setTranssmissionSequenceNumber(tsn);

         } else if (packetType == PacketType.PTT_TRANSMIT_REQUEST) {

            issiPacketType.setTranssmissionSequenceNumber(tsn);

            // Set PTT Control Word for PTT REQUEST
            pttControlWord = new PTTControlWord();
            pttControlWord.setWacnId(wacnId);
            pttControlWord.setSystemId(systemId);
            pttControlWord.setUnitId(unitId);
            pttControlWord.setPriorityTypeAndPriorityLevel(
                  transmitPriorityType, transmitPriorityLevel);

         } else if (packetType == PacketType.PTT_TRANSMIT_PROGRESS) {

            issiPacketType.setTranssmissionSequenceNumber(tsn);

            // Set PTT Control Word for PTT PROGRESS
            pttControlWord = new PTTControlWord();
            pttControlWord.setWacnId(wacnId);
            pttControlWord.setSystemId(systemId);
            pttControlWord.setUnitId(unitId);
            pttControlWord.setPriorityTypeAndPriorityLevel(
                  transmitPriorityType, transmitPriorityLevel);

         } else if (packetType == PacketType.PTT_TRANSMIT_MUTE) {
            // issiPacketType.setTranssmissionSequenceNumber(remoteTsn);
         } else if (packetType == PacketType.PTT_TRANSMIT_UNMUTE) {
            // issiPacketType.setTranssmissionSequenceNumber(remoteTsn);
         } else if (packetType == PacketType.HEARTBEAT_MUTE_TRANSMISSION) {
            if (logger.isDebugEnabled()) {
               logger.debug("MUTE HB: M = " + issiPacketType.getM());
            }

            issiPacketType.setTranssmissionSequenceNumber(tsn);
            issiPacketType.setPacketType(PacketType.HEARTBEAT);

         } else if (packetType == PacketType.HEARTBEAT_UNMUTE_TRANSMISSION) {
            if (logger.isDebugEnabled()) {
               logger.debug("UNMUTE HB: M = " + issiPacketType.getM());
            }
            issiPacketType.setTranssmissionSequenceNumber(tsn);
            issiPacketType.setPacketType(PacketType.HEARTBEAT);

         } else {
            throw new RuntimeException("Unexpected packet type " + packetType);
         }


         p25Payload = new P25Payload(issiPacketType, pttControlWord,
               issiHeaderWord, null, null);

         if (logger.isDebugEnabled()) {
            logger.debug(owningRfss.getRfssName() + " createPttPacket: "
               + "packetType=" + packetType 
               + ", TSN=" + issiPacketType.getTransmissionSequenceNumber());
         }

      } catch (P25BlockException pbe) {
         logger.error("unexpected exception ", pbe);
         throw new RuntimeException("Unexpected exception ", pbe);
      }
      return p25Payload;
   }

   /**
    * Create a PTT packet with IMBE voice.
    * 
    * @param packetType
    * @param imbeVoiceBlocks
    * @param unitId -
    *            the Unit id for the block.
    * @param transmitPriorityType
    *            the priority type
    * @param transmitPrioityLevel
    *            the priority level.
    * @return
    */
   private P25Payload createPttPacket(PacketType packetType,
         IMBEVoiceBlock[] imbeVoiceBlocks, int systemId, int unitId,
         TransmitPriorityType transmitPriorityType, int transmitPriorityLevel) {

      if (imbeIndex >= 18) { // 18 blocks in super frame
         imbeIndex = 0;
         if (superFrameIndex >= 3) { // Wrap after 3 (2 bits)
            superFrameIndex = 0;
         } else {
            superFrameIndex++;
         }
      }

      int tsn = unitId != 0 ? this.getTsnForUnitId(systemId, unitId) : 0;
      int numVoiceBlocks = imbeVoiceBlocks.length;
      imbeIndex += numVoiceBlocks;

      P25Payload p25Payload = null;
      ISSIPacketType issiPacketType = null;
      PTTControlWord pttControlWord = null;
      ISSIHeaderWord issiHeaderWord = null;
      ManufacturerSpecific[] mfrSpecificBlocks = null;

      // Set ISSI Packet Type
      issiPacketType = new ISSIPacketType();
      issiPacketType.setMuteStatus(muteTransmitter.peerMuteState.getBooleanValue());
      issiPacketType.setServiceOptions(serviceOptions);
      issiPacketType.setPacketType(packetType);
      issiPacketType.setLosingAudio(this.isLosingAudio(tsn));

      // Note that THEARTBEAT is in milliseconds
      issiPacketType.setInterval(TimerValues.THEARTBEAT / 1000);
      try {
         if (packetType == PacketType.PTT_TRANSMIT_REQUEST) {

            issiPacketType.setTranssmissionSequenceNumber(tsn);
            // Set PTT Control Word for PTT REQUEST
            pttControlWord = new PTTControlWord();
            pttControlWord.setWacnId(wacnId);
            pttControlWord.setSystemId(systemId);
            pttControlWord.setUnitId(unitId);
            pttControlWord.setPriorityTypeAndPriorityLevel(
                  transmitPriorityType, transmitPriorityLevel);
            // (Kameron) Only one ISSI header word to be sent for a talk
            // spurt.
            if (!issiHeaderWordSent) {

               // Set ISSI Header Word for PTT REQUEST
               issiHeaderWord = new ISSIHeaderWord();
               int messageIndicator = 0; // No encryption, so use 0
               issiHeaderWord.setMessageIndicator(ByteUtil
                     .intToBytes(messageIndicator));
               issiHeaderWord.setAlgId(128); // 0x80 means no encryption
               issiHeaderWord.setKeyId(0); // No encryption, so use 0
               issiHeaderWord.setMFID(144); // 0x90 is Motorola
               issiHeaderWord.setGroupId(groupId);
               // Set NID to 12-bit NAC (0xF7F) and 4-bit DUID (0x0).
               issiHeaderWord.setNID(63472);
               issiHeaderWord.setSF(superFrameIndex);
               issiHeaderWord.setReserved(0);
               issiHeaderWordSent = true;

            }

            // Increment RTP timestamp here. This should be set to the
            // total sampling time of all contained IMBE voice blocks.
            rtpTimeStamp += (rtpTimeStampIncrement * imbeVoiceBlocks.length);
            p25Payload = new P25Payload(issiPacketType, pttControlWord,
                  issiHeaderWord, imbeVoiceBlocks, mfrSpecificBlocks);

         } else if (packetType == PacketType.PTT_TRANSMIT_PROGRESS) {

            issiPacketType.setTranssmissionSequenceNumber(tsn);

            // Set PTT Control Word for PTT PROGRESS
            pttControlWord = new PTTControlWord();
            pttControlWord.setWacnId(wacnId);
            pttControlWord.setSystemId(systemId);
            pttControlWord.setUnitId(unitId);
            pttControlWord.setPriorityTypeAndPriorityLevel(
                  transmitPriorityType, transmitPriorityLevel);

            // (Kameron) Only one ISSI header word to be sent for a talk
            // spurt.
            if (!issiHeaderWordSent) {

               // Set ISSI Header Word for PTT REQUEST
               issiHeaderWord = new ISSIHeaderWord();
               int messageIndicator = 0; // No encryption, so use 0
               issiHeaderWord.setMessageIndicator(ByteUtil
                     .intToBytes(messageIndicator));
               issiHeaderWord.setAlgId(128); // 0x80 means no encryption
               issiHeaderWord.setKeyId(0); // No encryption, so use 0
               issiHeaderWord.setMFID(144); // 0x90 is Motorola
               issiHeaderWord.setGroupId(groupId);
               // Set NID to 12-bit NAC (0xF7F) and 4-bit DUID (0x0).
               issiHeaderWord.setNID(63472);
               issiHeaderWord.setSF(superFrameIndex);
               //EHC: issiHeaderWord.setVBB(1);
               issiHeaderWord.setReserved(0);
               issiHeaderWordSent = true;
            }

            // Increment RTP timestamp here. This should be set to the
            // total sampling time of all contained IMBE voice blocks.
            rtpTimeStamp += (rtpTimeStampIncrement * imbeVoiceBlocks.length);
            p25Payload = new P25Payload(issiPacketType, pttControlWord,
                  issiHeaderWord, imbeVoiceBlocks, mfrSpecificBlocks);
         }

         if (logger.isDebugEnabled())
            logger.debug("Sending " + packetType + ", TSN="
                  + issiPacketType.getTransmissionSequenceNumber());

      } catch (P25BlockException pbe) {
         pbe.printStackTrace();
         logger.fatal("Unexpected exception", pbe);

      }
      return p25Payload;

   }

   private P25Payload createPttPacketWithSpecifiedTsn(PacketType packetType,
         int tsn) {
      assert tsn > 0;
      P25Payload retval = this.createPttPacket(packetType, 0, 0, null, 0);
      retval.getISSIPacketType().setTranssmissionSequenceNumber(tsn);
      return retval;
   }

   /**
    * Handle incoming PTT packets from RTP stack.
    * 
    * @param p25Payload
    *            The incoming PTT packet.
    */
   void handleIncomingPttPacket(P25Payload p25Payload) {
      ISSIPacketType issiPacketType = p25Payload.getISSIPacketType();
      PacketType packetType = issiPacketType.getPacketType();
      int incomingTSN = issiPacketType.getTransmissionSequenceNumber();

      if (logger.isDebugEnabled()) {
         logger.debug("handleIncomingPttPacket: packetType="+packetType+" incomingTSN="+incomingTSN);
         logger.debug(sessionType + ": " 
               + rtpSession.getMyIpAddress().getHostAddress() + ":"
               + rtpSession.getMyRtpRecvPort()
               + " receiving " + packetType
               + " from remote RTP port "
               + rtpSession.getRemoteIpAddress() + ":"
               + rtpSession.getRemoteRtpRecvPort());
      }

      boolean correctTsn = verifyPttPacket(incomingTSN, packetType);
      if (!correctTsn) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
            logger.debug(sessionType + ": " + " received incorrect TSN="
                  + incomingTSN);
         }
         return;
      }
      // save currentTSN
      setCurrentTSN( incomingTSN);

      /*
       * Handle packets in a layered fashion: First, handle connection
       * heartbeats, followed by mute-related packets, and lastly by normal
       * SMF/MMF packets, if needed.
       */
      if ((incomingTSN == 0)
            && heartbeatReceiver.isPacketTypeHandled(packetType)) {
         logger.debug("handleIncomingPttPacket: heartbeatReceiver...");
         // Handle connection maintenance HEARTBEATs or HEARTBEAT QUERYs
         heartbeatReceiver.handlePttPacket(p25Payload);

      } else if (muteReceiver.isPacketTypeHandled(packetType)) {
//EHC
         logger.debug("handleIncomingPttPacket: muteReceiver...");
         muteReceiver.handlePttPacket(p25Payload);

      } else if (muteTransmitter.isPacketTypeHandled(packetType)) {
//EHC
         logger.debug("handleIncomingPttPacket: muteTransmitter...");
         muteTransmitter.handlePttPacket(p25Payload);
      }

//EHC
      logger.debug("handleIncomingPttPacket: forward to SMF/MMF Tx/Rx for handling...");
      // Now forward to SMF/MMF Tx/Rx for handling
      handlePttPacket(p25Payload);
   }

   /**
    * This method handles incoming PTT packets AFTER the packet has been
    * processed by handleIncomingPttPacket(). This method is implemented by an
    * SMF or MMF session.
    * 
    * @param payload
    *            The PTT packet.
    */
   abstract void handlePttPacket(P25Payload payload);

   /**
    * Create a heartbeat packet. Note that this method is static because it
    * does not have any information that is pertinent to the given session
    * (i.e. tsn ).
    * 
    * @return -- the heartbeat connection packet.
    */
   static P25Payload createHeartbeatConnection() {
      P25Payload p25Payload = null;
      try {
         ISSIPacketType issiPacketType = new ISSIPacketType();
         issiPacketType.setMuteStatus(false);
         issiPacketType.setServiceOptions(serviceOptions);
         issiPacketType.setPacketType(PacketType.HEARTBEAT_CONNECTION);
         issiPacketType.setPacketType(PacketType.HEARTBEAT);
         issiPacketType.setLosingAudio(false);
         // Note that THEARTBEAT is in milliseconds
         issiPacketType.setInterval(TimerValues.THEARTBEAT / 1000);
         issiPacketType.setTranssmissionSequenceNumber(0);
         p25Payload = new P25Payload(issiPacketType, null, null, null, null);
         return p25Payload;

      } catch (Exception ex) {
         logger.fatal("Unexpected error creating heartbeat connection ", ex);
         throw new RuntimeException("Unexpected excepton ");
      }
   }

   /**
    * Create a heartbeat query. Note that this is a static method because it
    * does not refer to any instance variables. Note that it gets called from
    * the ptt session multiplexer.
    * 
    * @return
    */
   static P25Payload createHeartbeatQuery() {
      try {
         ISSIPacketType issiPacketType = new ISSIPacketType();
         issiPacketType.setMuteStatus(false);
         issiPacketType.setServiceOptions(serviceOptions);
         issiPacketType.setLosingAudio(false);
         // Note that THEARTBEAT is in milliseconds
         issiPacketType.setInterval(TimerValues.THEARTBEAT / 1000);
         issiPacketType.setTranssmissionSequenceNumber(0);
         issiPacketType.setPacketType(PacketType.HEARTBEAT_QUERY);
         issiPacketType.setTranssmissionSequenceNumber(0);
         return new P25Payload(issiPacketType, null, null, null, null);
      } catch (Exception ex) {
         logger.fatal("Unexpected error creating heartbeat query ", ex);
         throw new RuntimeException("Unexpected exception ", ex);
      }
   }

   /**
    * Create a ptt packet of the given type given its unit id. TODO -- check
    * the type.
    * 
    * @param packettype
    */
   P25Payload createPttPacket(PacketType packetType) {
      assert packetType != PacketType.PTT_TRANSMIT_PROGRESS
            && packetType != PacketType.PTT_TRANSMIT_START;
      return this.createPttPacket(packetType, 0,0, null, 0);
   }

   /**
    * Create a tx end for specified tsn.
    * 
    * @param tsn
    * @return
    */
   P25Payload createPttEndForTsn(int tsn) {
      return createPttPacketWithSpecifiedTsn(PacketType.PTT_TRANSMIT_END, tsn);
   }

   /**
    * Create a PTT transmit grant.
    */
   P25Payload createPttTransmitGrant(int tsn) {
      return createPttPacketWithSpecifiedTsn(PacketType.PTT_TRANSMIT_GRANT, tsn);
   }

   /**
    * Create a PTT wait packet.
    * 
    * @param tsn
    * @return
    */
   P25Payload createPttTransmitDeny(int tsn) {
      return createPttPacketWithSpecifiedTsn(PacketType.PTT_TRANSMIT_DENY, tsn);
   }

   /**
    * Create a PTT wait packet.
    * 
    * @param tsn
    * @return
    */
   P25Payload createPttWait(int tsn) {
      return createPttPacketWithSpecifiedTsn(PacketType.PTT_TRANSMIT_WAIT, tsn);
   }

   /**
    * create an un mute packet.
    */
   P25Payload createPttUnmute(int tsn) {
      return createPttPacketWithSpecifiedTsn(PacketType.PTT_TRANSMIT_UNMUTE, tsn);
   }

   /**
    * create a mute packet.
    */
   P25Payload createPttMute(int tsn) {
      assert tsn > 0;
      return createPttPacketWithSpecifiedTsn(PacketType.PTT_TRANSMIT_MUTE, tsn);
   }

   /**
    * createMuteTransmission
    * 
    * @param tsn --
    *            tsn for which to create the mute transmission.
    */
   P25Payload createPttMuteTransmission(int tsn) {
      assert tsn > 0;
      return this.createPttPacketWithSpecifiedTsn( PacketType.HEARTBEAT_MUTE_TRANSMISSION, tsn);
   }

   /**
    * Create a mute heartbeat transmission packet.
    * 
    * @param tsn
    * @return
    */
   public P25Payload createHeartbeatUnmuteTransmission(int tsn) {
      assert tsn > 0;
      return this.createPttPacketWithSpecifiedTsn( PacketType.HEARTBEAT_UNMUTE_TRANSMISSION, tsn);
   }

   /**
    * Create a PTT end packet to send out.
    * 
    * @param unitId
    * @return
    */
   P25Payload createPttEndForUnitId(int systemId, int unitId) {
      assert unitId > 0;
      assert systemId > 0;
      return this.createPttPacket(PacketType.PTT_TRANSMIT_END, systemId,unitId, null, 0);
   }

   /**
    * Create a transmit start request.
    * 
    * @param unitId
    * @param transmitPriorityType
    * @param transmitPriorityLevel
    * @return
    */
   P25Payload createPttTransmitStart(int systemId, int unitId,
         TransmitPriorityType transmitPriorityType, int transmitPriorityLevel) {
      assert unitId > 0;
      assert systemId > 0;
      return createPttPacket(PacketType.PTT_TRANSMIT_START, systemId, unitId,
            transmitPriorityType, transmitPriorityLevel);
   }

   /**
    * Create a PTT transmit request for this Ptt session.
    * 
    * @param unitId
    * @return
    */
   P25Payload createPttTransmitRequest(int systemId, int unitId) {

      return createPttPacket(PacketType.PTT_TRANSMIT_REQUEST,systemId, unitId,
            this.transmitPriorityType, this.transmitPriorityLevel);
   }

   /**
    * Create a PTT transmit request for this Ptt session.
    * 
    * @param unitId
    * @param voiceBlocks
    * @return
    */
   P25Payload createPttTransmitRequest(int systemId, int unitId,
         IMBEVoiceBlock[] imbeVoiceBlocks) {
      return this.createPttPacket(PacketType.PTT_TRANSMIT_REQUEST,
            imbeVoiceBlocks, systemId, unitId, this.transmitPriorityType,
            this.transmitPriorityLevel);
   }
   
   /**
    * Create a ptt tx progress for this SU.
    * 
    * @param fromUnitId
    * @param transmitPriorityType
    * @param transmitPriorityLevel
    * @param imbeVoiceBlocks
    * @return
    */
   P25Payload createPttTransmitProgress( int fromSystemId, int fromUnitId, 
         TransmitPriorityType transmitPriorityType,
         int transmitPriorityLevel, IMBEVoiceBlock[] imbeVoiceBlocks) {
      return createPttPacket(PacketType.PTT_TRANSMIT_PROGRESS, imbeVoiceBlocks,
            fromSystemId,
            fromUnitId, transmitPriorityType, transmitPriorityLevel);   
   }

   /**
    * Create a ptt tx progress for this SU with specified losing audio.
    * 
    * @param fromUnitId
    * @param losingAudio
    * @param transmitPriorityType
    * @param transmitPriorityLevel
    * @param imbeVoiceBlocks
    * @return
    */
   /* P25Payload createPttTransmitProgress(int fromUnitId, boolean losingAudio,
         TransmitPriorityType transmitPriorityType,
         int transmitPriorityLevel, IMBEVoiceBlock[] imbeVoiceBlocks) {
      P25Payload retval = createPttPacket(PacketType.PTT_TRANSMIT_PROGRESS,
            imbeVoiceBlocks, fromUnitId, transmitPriorityType,
            transmitPriorityLevel);
      retval.getISSIPacketType().setLosingAudio(losingAudio);
      return retval;
   }
   */

   /**
    * Encapsulates a PTT payload into an RTP packet and sends it out.
    */
   void sendPttPacket(P25Payload p25Payload) throws IOException, RtpException {

      // intentional delay to resolve the systime different between RFSSes
      if( PTT_DELAY_2MS) {
         try {
            Thread.sleep(2L);
         } catch(Exception ex) { }
      }

      RtpPacket rtpPacket = new RtpPacket();
      // The NIST RTP stack already define default values for some of these,
      // but we set each here for testing purposes.
      rtpPacket.setV(2);
      rtpPacket.setP(0);
      rtpPacket.setX(0);
      rtpPacket.setCC(0);
      rtpPacket.setM(0);
      rtpPacket.setPT(100);

      // See Section 5.1 RFC3550 (RTP) and Section 4.1 TIA-102.BACA
      // for timestamp computation. We make several assumptions regarding
      // timestamp computation:
      // 1. RTP timestamps are not wallclock time. Instead, the intial
      // RTP timestamp of the initial RTP packet (in a set of packets)
      // must be randomly set (but we set it to 0).
      // 2. RTP timestamps are in 125 microsecond resolution (as defined
      // in Section 4, TIA-102.BACA.
      // 3. The value of an RTP timestamp will be the same as that found
      // in a preceding RTP packet if it is a non-PROGRESS packet.

      // If RTP time stamp has been incremented over its (int) limit,
      // wrap around to zero.
      if (rtpTimeStamp >= ByteUtil.getMaxLongValueForNumBits(32))
         rtpTimeStamp = 0;

      rtpPacket.setTS(rtpTimeStamp);
      rtpPacket.setSSRC(ssrc);

      if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
//logger.debug("ZMARKER(1): rtpPacket="+rtpPacket.toString());
         logger.debug(owningRfss.getRfssName() + " sendPttPacket(): "
               + sessionType + ": "
               + rtpSession.getMyIpAddress().getHostAddress() + ":"
               + rtpSession.getMyRtpRecvPort() 
               + " sending "
               + p25Payload.getISSIPacketType().getPacketType()
               + " to remote RTP port " 
               + rtpSession.getRemoteIpAddress() + ":"
               + rtpSession.getRemoteRtpRecvPort());
      }
      try {
         byte[] p25PayloadBytes = p25Payload.getBytes();
         rtpPacket.setPayload(p25PayloadBytes, p25PayloadBytes.length);

         if (rtpSession.getRemoteRtpRecvPort() != -1) {
            rtpSession.sendRtpPacket(rtpPacket);
            // Log the outgoing packet if we are sending to another host
            // (other than ourselves).
            if (!rtpSession.getRemoteIpAddress().equals(
                  rtpSession.getMyIpAddress().getHostAddress())) {
//logger.debug("ZMARKER(2): rtpPacket="+rtpPacket.toString());
               if (!PTT_TEST_MODE) {
                  this.owningRfss.getRFSS().capturePttPacket(rtpPacket,
                        p25Payload, true, this);
               }
            }
         }

      } catch (P25BlockException pbe) {
         // This should never occur.
         pbe.printStackTrace();
         logger.fatal("Unexpected exception", pbe);

      } catch (RtpException re) {
         throw re;
      }
   }

   /**
    * Compare a transmit priority level and type to the current transmit
    * priority level and type. Has the same semantics as Comparable.compareTo.
    * 
    * @param sessionToCompare --
    *            the session to compare with.
    * @return -1 if the current priority is greater than the priority to
    *         compare with. 0 if the priorities are equal. +1 if the priority
    *         of the session we are comparing with is greater.
    */
   public int compareTo(PttSession sessionToCompare) {
      TransmitPriorityType priorityType = sessionToCompare.transmitPriorityType;
      int priorityLevel = sessionToCompare.transmitPriorityLevel;
      if (this.transmitPriorityType == null)
         return -1;
      int tp = (int) (priorityType.intValue() & 0x0F);
      tp <<= 0x04;
      tp |= (priorityLevel & 0x0F);

      int cp = (int) this.transmitPriorityType.intValue() & 0x0f;
      cp <<= 0x04;
      cp |= (this.transmitPriorityLevel & 0x0f);
      int retval = cp > tp ? 1 : 0;
      logger.debug("isCurrentPriorityHigher " + retval + " ptype = "
            + priorityType + " pl = " + priorityLevel + " this.priority = "
            + this.transmitPriorityType + " this.level = "
            + this.transmitPriorityLevel);
      return retval;
   }

   /**
    * 
    * Send a mute to the remote peer. Note that the tsn is passed in as an
    * argument because the new tesn is already recorded here when the listener
    * sees the packet.
    * 
    * @param tsn -- the tsn to mute.
    */
   public void sendMute(int tsn) throws Exception {
      P25Payload p25Payload = this.createPttMute(tsn);
      this.sendPttPacket(p25Payload);
   }

   /**
    * Send an umute to the remote peer.
    * 
    * @param tsn -- the tsn to unmute.
    */
   public void sendUnmute(int tsn) throws Exception {
      P25Payload p25Payload = this.createPttUnmute(tsn);
      this.sendPttPacket(p25Payload);
   }

   /**
    * Returns the point to multipoint session associated with this ptt session.
    * 
    * @return
    */
   public PttPointToMultipointSession getPointToMultipointSession() {
      return this.pttPointToMultipointSession;
   }

   /**
    * set the pointToMultipoint session pointer.
    * 
    * @param pttPointToMultipointSession
    */
   public void setPointToMultipointSession(
         PttPointToMultipointSession pttPointToMultipointSession) {
      this.pttPointToMultipointSession = pttPointToMultipointSession;
   }

   /**
    * This method starts mute-related components. This method is required since
    * the SMF or MMF must be able to start the mute-related receiver and
    * transmitter AFTER it starts its own receiver and transmitter.
    */
   void startMuteComponents() {
      // Create mute-related components
      muteReceiver = new MuteReceiver(this);
      muteTransmitter = new MuteTransmitter(this);
   }

   /**
    * This method starts heartbeat-related components. This method is required
    * since the SMF or MMF must be able to start the heartbeat-related receiver
    * and transmitter AFTER it starts its own receiver and transmitter.
    */
   public void startHeartbeatComponents() {
      // Create connection maintenance heartbeat components
      // check for 0 port before starting the reciever.
      heartbeatReceiver = new HeartbeatReceiver(this);
      if (rtpSession.getRemoteRtpRecvPort() > 0) {
         heartbeatTransmitter = new HeartbeatTransmitter(this);
      }
   }

   /**
    * Get the ID for this PTT session.
    * 
    * @return The ID for this PTT session.
    */
   public String getSessionId() {
      String sid = systemId + ":" + wacnId + ":";
      if (this instanceof MmfSession) {
         return "[ MmfSession: " + sid + "]";
      } else {
         return "[ SmfSession: " + sid + "]";
      }
   }

   /**
    * release the port
    */
   public void stopReceiver() {
      logger.debug("PttSession:stopping receiver and releasing rtp resources ");
      rtpSession.removeRtpListener(this);
      rtpSession.stopRtpPacketReceiver();
   }

   /**
    * Log the PTT packet.
    */
   
   public void logPttPacket(RtpPacket rtpPacket, P25Payload p25Payload,
         boolean isSender, long timeStamp, int packetNumber,
         String remoteRfssDomainName, String remoteIpAddress, int remotePort) {
      
      //PrintWriter rtpLog = TopologyConfig.getRtpMessageLogStream();
      PrintWriter rtpLog = TesterUtility.getRtpMessageLogStream();
      if (rtpLog != null) {
         StringBuffer sbuf = new StringBuffer();
         //sbuf.append("\n<!-- PTT PACKET BEGIN 1 -->");
         sbuf.append("\n<ptt-packet\n");
         sbuf.append(" receptionTime=\"" + timeStamp + "\"\n");
         sbuf.append(" packetNumber=\"" + packetNumber + "\"\n");
         sbuf.append(" isSender=\"" + isSender + "\"\n");
         sbuf.append(" rawdata=\"");
         sbuf.append( ByteArrayUtil.toHexString( rtpPacket.getPayload(), 16));
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
	 // disable RTF support
         sbuf.append(" RTF Endcoding not supportted. ");
         sbuf.append("]]>\n");
         sbuf.append("</rtf-format>");
	  ***/
         sbuf.append("\n</ptt-packet>");
         rtpLog.println(sbuf.toString());
         rtpLog.flush();
      }
   }

   @Override
   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("<ptt-session\n");
      sbuf.append(" sessionType=");
      sbuf.append(this instanceof MmfSession ? "\"mmfSession\"\n"
                  : "\"smfSession\"\n");
      sbuf.append(" rfssId=\""
                  + (owningRfss != null ? owningRfss.getRfssId() : 0));
      sbuf.append("\"\n");
      sbuf.append(" myIpAddress=\""
                        + rtpSession.getMyIpAddress().getHostAddress()
                        + "\"\n");
      sbuf.append(" myRtpRecvPort=\"" + rtpSession.getMyRtpRecvPort()
                        + "\"\n");
      sbuf.append(" remoteIpAddress=\""
                        + rtpSession.getRemoteIpAddress() + "\"\n");
      sbuf.append(" remoteRtpRecvPort=\""
                        + rtpSession.getRemoteRtpRecvPort() + "\"\n");
      sbuf.append(" linkType=\"" + this.linkType + "\"\n");
      sbuf.append("/>\n");
      return sbuf.toString();
   }

   /**
    * Set the wide area communication network ID.
    * 
    * @param wacnId
    *            The wide area communication network ID.
    */
   void setWacnId(int wacnId) {
      this.wacnId = wacnId;
   }

   /**
    * Set the system ID.
    * 
    * @param systemId
    *            The system ID.
    */
   void setSystemId(int systemId) {
      this.systemId = systemId;
   }

   /**
    * Set the owning RFSS.
    * 
    * @param owningRfss
    *            The owning RFSS.
    */
   public void setOwningRfss(RfssConfig owningRfss) {
      if (owningRfss == null)
         throw new NullPointerException("null arg!");
      this.owningRfss = owningRfss;
   }

   /**
    * Get the identity of the Rfss that created this ptt session.
    * 
    * @return - the id of the owning rfss.
    */

   public RfssConfig getOwningRfss() {
      return this.owningRfss;
   }

   /**
    * Get the recv port for embedded Rtp session
    */
   public int getMyRtpRecvPort() {
      return rtpSession.getMyRtpRecvPort();
   }

   /**
    * Get the link type.
    */
   public LinkType getLinkType() {
      return this.linkType;
   }

   /**
    * Shut down this PTT session.
    */
   public void shutDown() {

      if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
         logger.debug(getSessionId() + " Shutting down...");
      }

      // Shutdown RTP
      if (PTT_TEST_MODE) {
         rtpSession.shutDown("TEST", sessionType);

      } else if (rtpSession != null) {
         // rtpSession.shutDown();
         /* Use the following for dumping raw RTP data for later analyses */
         rtpSession.shutDown(owningRfss.getRfssName(), sessionType);
      }

      // Shutdown Heartbeat transmitter and receiver
      if (heartbeatTransmitter != null) {
         //   && heartbeatTransmitter.sendHeartbeatsTask != null) {
         //===heartbeatTransmitter.sendHeartbeatsTask.cancel();
         heartbeatTransmitter.stop();
         logger.debug(getSessionId() + " Stop HeartbeatTransmitter...");
      }

      if (heartbeatReceiver != null) {
         //   && heartbeatReceiver.receiveHeartbeatsTimeoutTask != null) {
         //===heartbeatReceiver.receiveHeartbeatsTimeoutTask.cancel();
         heartbeatReceiver.stop();
         logger.debug(getSessionId() + " Stop HeartbeatReceiver...");
      }

      // Shutdown Mute transmitter and receiver
      if (muteTransmitter != null) {
         muteTransmitter.shutDown();
         logger.debug(getSessionId() + " Stop MuteTransmitter...");
      }

      if (muteReceiver != null) {
         if (muteReceiver.unmuteTask != null) {
            muteReceiver.unmuteTask.cancel();
            logger.debug(getSessionId() + " Stop MuteReceiver (need work)...");
         }
      }
      // TODO (steveq) Release TSN for this session
      // Shut down
   }

   /**
    * Set the group ID.
    * 
    * @param groupId
    *            The group ID.
    */
   public void setGroupId(int groupId) {
      this.groupId = groupId;
   }

   public void setRemoteIpAddress(String ipAddress) {
      logger.debug("PttSession: setRemoteIpAddress() - ipAddress="+ipAddress);
      this.rtpSession.setRemoteIpAddress(ipAddress);
   }

   public void setRemoteRtpRecvPort(int remoteRecvPort) {
      int originalPort = rtpSession.getRemoteRtpRecvPort();
      if (heartbeatTransmitter != null && originalPort != remoteRecvPort) {
         // If the port got reset, then kill the original transmtter.
         heartbeatTransmitter.stop();
      }
      if (remoteRecvPort > 0 && originalPort != remoteRecvPort) {
         heartbeatTransmitter = new HeartbeatTransmitter(this);
      }
      this.rtpSession.setRemoteRtpRecvPort(remoteRecvPort);
      logger.debug("remoteRtpRecvPort = "
            + this.rtpSession.getRemoteIpAddress() + ":"
            + this.rtpSession.getRemoteRtpRecvPort() + " originalPort = "
            + originalPort);
   }

   /***************************************************************************
    * RtpListener Methods
    **************************************************************************/
   /**
    * Handle incoming RTP packets.
    * 
    * @param rtpPacketEvent
    *            The RTP packet event.
    */
   public void handleRtpPacketEvent(RtpPacketEvent rtpPacketEvent) {
      try {
         RtpPacket rtpPacket = rtpPacketEvent.getRtpPacket();

//logger.debug("ZMARKER(41): handleRtpPacketEvent(): rtpPacket="+rtpPacket);
//TODO: somehow the payloadType is changed from 100 to 72, reset here
//#368
rtpPacket.setPT(100);

         byte[] rtpPayload = rtpPacket.getPayload();

         P25Payload p25Payload = new P25Payload(rtpPayload);
         RtpSession rtpSession = (RtpSession) rtpPacketEvent.getSource();

         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
            logger.debug(owningRfss.getRfssName() + " handleRtpPacketEvent(): "
                  + sessionType + ": " 
                  + rtpSession.getMyIpAddress().getHostAddress() +":"
                  + rtpSession.getMyRtpRecvPort()
                  + " receiving "
                  + p25Payload.getISSIPacketType().getPacketType()
                  + " from remote RTP port "
                  + rtpSession.getRemoteIpAddress() + ":"
                  + rtpSession.getRemoteRtpRecvPort());
         }

         if (!PTT_TEST_MODE) {
            this.owningRfss.getRFSS().capturePttPacket(rtpPacket,
                  p25Payload, false, this);
         }
         handleIncomingPttPacket(p25Payload);

      } catch (P25BlockException ex) {
         logger.error("Invalid PTT packet", ex);
         this.getRfss().getTestHarness().fail(
               "Unexpected exception unpacking block");
      }
   }

   public void handleRtpStatusEvent(RtpStatusEvent arg0) {
   }

   public void handleRtpTimeoutEvent(RtpTimeoutEvent arg0) {
   }

   public void handleRtpErrorEvent(RtpErrorEvent arg0) {
   }

   /**
    * This method verifies an incoming packet's TSN. This method verifies TSNs
    * in the following ways: (1) checks if TSN is correct with respect to
    * packet type, (2) checks if TSN is known, and (3) checks if TSN is active.
    * This method also adds TSNs to the set of known TSNs when receiving a PTT
    * START or PTT REQUEST and deactives TSNs from the set of known TSNs when
    * receiving a PTT END. see TIA Specification Section 7.6
    * 
    * @param p25Payload
    *            The incoming PTT packet.
    * 
    */
   private boolean verifyPttPacket(int incomingTSN, PacketType packetType) {

      boolean correctTsn = true;
      boolean correctPacketType = true;
      boolean incomingTsnIsEven = (incomingTSN % 2 == 0) ? true : false;

      /* Check if TSN is incorrect with respect to values. */
      if (incomingTSN == 0
            && (packetType == PacketType.HEARTBEAT || packetType == PacketType.HEARTBEAT_QUERY)) {
         // This is correct, so return
         return true;

      } else if ("SMF".equals(sessionType)) {

         if ((incomingTsnIsEven && (packetType == PacketType.PTT_TRANSMIT_START
               || packetType == PacketType.PTT_TRANSMIT_PROGRESS || packetType == PacketType.PTT_TRANSMIT_END))
               || (!incomingTsnIsEven && (packetType == PacketType.PTT_TRANSMIT_GRANT
                  || packetType == PacketType.PTT_TRANSMIT_DENY
                  || packetType == PacketType.PTT_TRANSMIT_WAIT
                  || packetType == PacketType.PTT_TRANSMIT_MUTE 
                  || packetType == PacketType.PTT_TRANSMIT_UNMUTE))) {
            if (logger.isDebugEnabled())
               logger.debug(sessionType + " received incorrect " + "TSN="
                     + incomingTSN + " for " + packetType);
            correctTsn = false;

         } else if (incomingTsnIsEven
               && (!this.myTsnSet.contains(incomingTSN))
               && (packetType == PacketType.PTT_TRANSMIT_GRANT
                     || packetType == PacketType.PTT_TRANSMIT_DENY
                     || packetType == PacketType.PTT_TRANSMIT_WAIT
                     || packetType == PacketType.PTT_TRANSMIT_MUTE 
                     || packetType == PacketType.PTT_TRANSMIT_UNMUTE)) {
            if (logger.isDebugEnabled())
               logger.debug(sessionType + " received incorrect " + "TSN="
                     + incomingTSN + " for " + packetType);
            correctTsn = false;

         } else {

            // TSN is correct
            // if (logger.isDebugEnabled())
            // logger.debug(sessionType + " received correct " +
            // "TSN=" + incomingTSN + " for " + packetType);
         }

      } else if ("MMF".equals(sessionType)) {

         if ((!incomingTsnIsEven && (packetType == PacketType.PTT_TRANSMIT_REQUEST
               || packetType == PacketType.PTT_TRANSMIT_PROGRESS 
               || packetType == PacketType.PTT_TRANSMIT_END))
               || (incomingTsnIsEven && (packetType == PacketType.PTT_TRANSMIT_MUTE 
                  || packetType == PacketType.PTT_TRANSMIT_UNMUTE))) {

            if (logger.isDebugEnabled())
               logger.debug(sessionType + " received incorrect " + "TSN="
                     + incomingTSN + " for " + packetType);
            correctTsn = false;

         } else if (!incomingTsnIsEven
               && (!myTsnSet.contains(incomingTSN))
               && (packetType == PacketType.PTT_TRANSMIT_MUTE 
                  || packetType == PacketType.PTT_TRANSMIT_UNMUTE)) {
            if (logger.isDebugEnabled())
               logger.debug(sessionType + " received incorrect " + "TSN="
                     + incomingTSN + " for " + packetType);
            correctTsn = false;

         } else {
            // TSN is correct
            // if (logger.isDebugEnabled())
            // logger.debug(sessionType + " received correct " +
            // "TSN=" + incomingTSN + " for " + packetType);
         }
      }

      /* Check if packet type is correct. */
      if (((linkType == LinkType.GROUP_HOME) || ("MMF".equals(sessionType)))
            && (packetType == PacketType.PTT_TRANSMIT_START
                  || packetType == PacketType.PTT_TRANSMIT_GRANT
                  || packetType == PacketType.PTT_TRANSMIT_DENY 
		  || packetType == PacketType.PTT_TRANSMIT_WAIT)) {
         if (logger.isDebugEnabled())
            logger.debug(sessionType + " received incorrect " + "PacketType=" + packetType);
         correctPacketType = false;

      } else if ("SMF".equals(sessionType)
            && packetType == PacketType.PTT_TRANSMIT_REQUEST) {

         if (logger.isDebugEnabled())
            logger.debug(sessionType + " received incorrect "
                  + "PacketType=" + packetType);
         correctPacketType = false;

      } else {
         // TSN is correct
         // if (logger.isDebugEnabled())
         // logger.debug(sessionType + " received correct " +
         // "PacketType=" + packetType);
      }

      /* Now check if TSN is already known or active. */
      if (!remoteTSNs.contains(incomingTSN)) { // Remote TSN is unknown

         if (packetType == PacketType.PTT_TRANSMIT_START
               || packetType == PacketType.PTT_TRANSMIT_REQUEST
               || packetType == PacketType.PTT_TRANSMIT_PROGRESS) {

            if (logger.isDebugEnabled())
               logger.debug(sessionType + " received unknown " + "TSN="
                     + incomingTSN + " for " + packetType
                     + ".  Adding to set of known TSNs");

            // This is the beginning of a talk spurt so add to set
            // of known remote TSNs
            // this.remoteTsn = incomingTSN;

            remoteTSNs.add(incomingTSN);

         } else if ((!myTsnSet.contains(incomingTSN))
               && ((packetType == PacketType.PTT_TRANSMIT_GRANT)
                     || (packetType == PacketType.PTT_TRANSMIT_DENY)
                     || (packetType == PacketType.PTT_TRANSMIT_WAIT)
                     || (packetType == PacketType.PTT_TRANSMIT_MUTE)
                     || (packetType == PacketType.PTT_TRANSMIT_UNMUTE))) {

            // We received an unknown TSN
            if (logger.isDebugEnabled())
               logger.debug(sessionType + " received unknown " + "TSN="
                     + incomingTSN + " for " + packetType
                     + ". Sending PTT END.");
            try {

               if (logger.isDebugEnabled())
                  logger.debug(this + ":\n\tMMF TX sending "
                        + "PTT_TRANSMIT_END");

               // Note that PTT ENDs can be sent REND times, which
               // range from 1 to 5 (default 4). Here, we only send a
               // single PTT END.
               P25Payload p25Payload = this.createPttEndForTsn(incomingTSN);
               // End packet has to be sent REND times.
               sendPttPacket(p25Payload);
               correctTsn = false;

            } catch (RtpException re) {
               re.printStackTrace();
               logger.debug("Unexpected exception", re);

            } catch (IOException ioe) {
               ioe.printStackTrace();
               logger.debug("Unexpected exception", ioe);

            }

         } else if (packetType == PacketType.PTT_TRANSMIT_END
               || packetType == PacketType.HEARTBEAT && (incomingTSN > 0)) {
            // Received unknown TSN for this type of packet.
            correctTsn = false;
         }

      } else { // Remote TSN is already known

         if (packetType == PacketType.PTT_TRANSMIT_END) {

            // Deactivate this TSN
            remoteTSNs.remove(incomingTSN);

            if (logger.isDebugEnabled())
               logger.debug(sessionType + " deactivating TSN="
                     + incomingTSN);

         } else if (packetType == PacketType.PTT_TRANSMIT_GRANT
               || packetType == PacketType.PTT_TRANSMIT_DENY
               || packetType == PacketType.PTT_TRANSMIT_WAIT
               || packetType == PacketType.PTT_TRANSMIT_MUTE
               || packetType == PacketType.PTT_TRANSMIT_UNMUTE) {

            if (logger.isDebugEnabled())
               logger.debug(sessionType + " received inactive " + "TSN="
                     + incomingTSN + " for " + packetType);
            try {
               if (logger.isDebugEnabled())
                  logger.debug(this + ":\n\tMMF TX sending "
                        + "PTT_TRANSMIT_END");

               // Note that PTT ENDs can be sent REND times, which
               // range from 1 to 5 (default 4). Here, we only send a
               // single
               // PTT END.
               P25Payload p25Payload = createPttPacket(PacketType.PTT_TRANSMIT_END);
               // End packet has to be sent REND times.
               sendPttPacket(p25Payload);

            } catch (Exception re) {
               logger.error("Unexpected exception ", re);

            } 
         }
      }
      return (correctTsn && correctPacketType);
   }

   /**
    * Get the remote ip address and port
    */
   public String getRemoteIpAddressAndPort() {
      return this.rtpSession.getRemoteIpAddress() + ":"
            + this.rtpSession.getRemoteRtpRecvPort();
   }

   /**
    * Get the session type.
    */
   public String getSessionType() {
      return this.sessionType;
   }

   /**
    * This class defines TSN information for remote RFSSs.
    * 
    * @see TIA-109.BACA Section 7.6
    */
   class RemoteTsnInfo {

      /** The TSN of the remote RFSS. */
      int tsn;

      /** Defines whether the TSN is active (true) or inactive (false). */
      boolean active = true;

      public RemoteTsnInfo(int tsn) {
         this.tsn = tsn;
      }
   }

   public RFSS getRfss() {
      return this.owningRfss.getRFSS();
   }
   
   /**
    * 
    * @return the heartbeatTransmitter
    */
   public HeartbeatTransmitter getHeartbeatTransmitter() {
      return heartbeatTransmitter;
   }

   /**
    * 
    * @return the heartbeatReceiver
    */
   public HeartbeatReceiver getHeartbeatReceiver() {
      return heartbeatReceiver;
   }

   public void setTsn(int suId, int tsn) {
      this.unitIdToTsnMap.put(suId, tsn);
      this.myTsnSet.add(tsn);
   }

   public int getTsnForUnitId(int systemId, int unitId) {
      //===long key = ((long) systemId )<< 24 + unitId;
      if (unitId > 0) {
         if (this.unitIdToTsnMap.containsKey(unitId)) {
            return this.unitIdToTsnMap.get(unitId);
         } else {
            int tsn = this.manager.getNewTsn(this.linkType);
            this.unitIdToTsnMap.put(unitId, tsn);
            this.myTsnSet.add(tsn);
            return tsn;
         }
      } else
         throw new IllegalArgumentException("unitId is " + unitId);
   }
   
   public void clearNextLosingAudio() {
      if  ( this.losingAudioMap.iterator().hasNext()) {
         int n = this.losingAudioMap.iterator().next();
         this.losingAudioMap.remove(n);      
      }      
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

   /**
    * Set the SessionDescription for this ptt session. This is stored here
    * because we need to retrieve it later for re-invitation.
    * 
    * @param sessionDescription
    *            the sessionDescription to set
    */
   public void setSessionDescription(SessionDescription sessionDescription) {
      this.sessionDescription = sessionDescription;
   }

   /**
    * @return the sessionDescription
    */
   public SessionDescription getSessionDescription() {
      return sessionDescription;
   }

   /**
    * @return the rtpSession
    */
   public RtpSession getRtpSession() {
      return rtpSession;
   }

   /**
    * @param losingAudio
    *            the losingAudio to set
    */
   public void setLosingAudio(int tsn) {
      logger.debug(String.format("setLosingAudio %d ", tsn));
      this.losingAudioMap.add(tsn);
   }
   
   public void clearLosingAudio(int tsn) {
      assert myTsnSet.contains(tsn);
      logger.debug(String.format("clearLosingAudio %d ", tsn));
      this.losingAudioMap.clear();
      for ( int t : this.myTsnSet) {
         if ( t != tsn) losingAudioMap.add(t);
      }
   }

   public void setSetIsNotInterestedInLosingAudio(boolean muteFlag) {
      this.sendMute = muteFlag;      
   }
   
   public boolean isNotInterestedInLosingAudio() {      
      return this.sendMute ;
   }
   
   /**
    * @return the losingAudio
    */
   public boolean isLosingAudio(int tsn) {
      return this.losingAudioMap.contains(tsn) ;
   }

   public boolean isReceiverTerminated(int tsn) {
      // TODO Auto-generated method stub
      if (this instanceof MmfSession) {
         return ((MmfSession) this).getMmfReceiver().getCurrentState(tsn)
               .equals(MmfRxState.DONE);
      } else {
         return ((SmfSession) this).getSmfReceiver().getCurrentState()
               .equals(SmfRxState.DONE);
      }
   }

   /**
    * @param remoteRfssRadicalName
    *            the remoteRfssRadicalName to set
    */
   public void setRemoteRfssDomainName(String remoteRfssRadicalName) {
      if (this.remoteRfssDomainName != null)
         logger.warn("RESET!!!!  " + remoteRfssRadicalName + " this remote "
               + this.remoteRfssDomainName);
      this.remoteRfssDomainName = remoteRfssRadicalName;
   }
   
   /**
    * Get the remote rfss radical name
    */   
   public String getRemoteRfssDomainName() {
      return this.remoteRfssDomainName;
   }
}
