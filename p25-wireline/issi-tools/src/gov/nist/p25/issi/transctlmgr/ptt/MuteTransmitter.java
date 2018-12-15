//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.p25payload.*;

import java.util.*;
import org.apache.log4j.Logger;

/**
 * This class handles incoming MUTE/UNMUTE packets. This class also sends PTT
 * HEARTBEAT packets as well as other packets related to mute/unmute functions.
 * This class is associated with the source of the transmission (i.e., the SMF
 * or MMF transmitter).
 */
public class MuteTransmitter {

   protected static Logger logger = Logger.getLogger(MuteTransmitter.class);

   /** My PTT session. */
   private PttSession pttSession = null;

   /** Collection of packet types this class handles. */
   private static HashSet<PacketType> handledPacketTypes = null;
   
   /** The mute state of the peer RFSS. */
   Mute peerMuteState = Mute.UNMUTED;

   private MuteListener muteListener = null;
   
   private Hashtable<Integer,MuteTransmissionHeartbeatTask> heartbeatTasks = 
      new Hashtable<Integer,MuteTransmissionHeartbeatTask>();

   static {
      handledPacketTypes = new HashSet<PacketType>();
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_MUTE);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_UNMUTE);
   }

   /**
    * Construct a PTT mute transmitter.
    * 
    * @param pttSession
    *            The owning PTT session.
    */
   MuteTransmitter(PttSession pttSession) {
      this.pttSession = pttSession;
   }

   /**
    * Handle incoming PTT packets especially MUTE/UNMUTE packets.
    * 
    * @param p25Payload
    *            The incoming PTT packet.
    */
   void handlePttPacket(P25Payload p25Payload) {
      ISSIPacketType issiPacketType = p25Payload.getISSIPacketType();
      PacketType packetType = issiPacketType.getPacketType();
      if (packetType.equals(PacketType.PTT_TRANSMIT_MUTE)) {
         if (peerMuteState != Mute.MUTED)
            muteInd(p25Payload);

      } else if (packetType.equals(PacketType.PTT_TRANSMIT_UNMUTE)) {
         if (peerMuteState != Mute.UNMUTED)
            unmuteInd(p25Payload);
      }
   }

   /**
    * This method handles incoming PTT MUTE messages.
    * 
    * @see TIA Specification Section 7.6.8
    */
   private void muteInd(P25Payload p25Payload) {

      peerMuteState = Mute.MUTED;
      int tsn = p25Payload.getISSIPacketType().getTransmissionSequenceNumber();
      PttMuteEvent pttMuteEvent = new PttMuteEvent( this.pttSession, p25Payload);

      // Alert application
      if (muteListener != null)
         muteListener.receivedMute(pttMuteEvent);

      // Send mute HEARTBEATs
      MuteTransmissionHeartbeatTask muteTransmissionHeartbeatTask = new MuteTransmissionHeartbeatTask(tsn);
      
      this.heartbeatTasks.put(tsn, muteTransmissionHeartbeatTask);
      try {
         ISSITimer.getTimer().schedule(muteTransmissionHeartbeatTask, 0,
               TimerValues.TMUTEPROGRESS);
      } catch (Exception e) {
         // Only caught if timer has already been cancelled
         // (which means that the program is terminating).
      }
   }

   /**
    * This method handles incoming PTT UNMUTE messages.
    * See the TIA Specification Section 7.6.9
    * 
    * @param p25UnmutePayload -- the unmute payload.
    */
   private void unmuteInd(P25Payload p25UnmutePayload) {
      peerMuteState = Mute.UNMUTED;
      PttUnmuteEvent pttMuteEvent = new PttUnmuteEvent( this.pttSession, p25UnmutePayload);
      int tsn = p25UnmutePayload.getISSIPacketType().getTransmissionSequenceNumber();

      // Alert application
      if (muteListener != null)
         muteListener.receivedUnmute(pttMuteEvent);

      // Cancel mute HEARTBEATs
      if ( this.heartbeatTasks.get(tsn) != null) {
         this.heartbeatTasks.get(tsn).cancel();
      }

      // Send unmute HEARTBEATs
      if (logger.isDebugEnabled())
         logger.debug(pttSession.sessionType + " sending "
               + PacketType.HEARTBEAT_UNMUTE_TRANSMISSION + "(M=0)");

      P25Payload p25Payload = pttSession.createHeartbeatUnmuteTransmission(tsn);
      try {
         pttSession.sendPttPacket(p25Payload);
      } catch (Exception e) {
         e.printStackTrace();
         logger.debug(pttSession.sessionType + " ex: " + e.toString());
      }
   }

   /**
    * Checks if this class handles this packet type.
    * 
    * @param packetType
    *            The packet type to be handled.
    * @return True if handled, false otherwise.
    */
   boolean isPacketTypeHandled(PacketType packetType) {
      return handledPacketTypes.contains(packetType);
   }

   public void addListener(MuteListener muteListener) {
      if (muteListener != null)
         this.muteListener = muteListener;
   }
   
   public void shutDown() {
      for (MuteTransmissionHeartbeatTask ht : this.heartbeatTasks.values()) {
         ht.cancel();
      }
   }

   /**
    * This class sends mute-related HEARTBEATs.
    * 
    * @see TIA-109.BACA Section 7.2.3
    */
   class MuteTransmissionHeartbeatTask extends TimerTask {
      int tsn;
      private P25Payload p25Payload;
      
      MuteTransmissionHeartbeatTask( int tsn ) {
         this.tsn = tsn;
         this.p25Payload = pttSession.createPttMuteTransmission(tsn);
      }

      public void run() {
         if (logger.isDebugEnabled())
            logger.debug(pttSession.sessionType + " sending "
                  + PacketType.HEARTBEAT_MUTE_TRANSMISSION + " TSN="
                  + p25Payload.getISSIPacketType().getTransmissionSequenceNumber()+ ", (M=1)");

         try {
            pttSession.sendPttPacket(p25Payload);
         } catch (Exception e) {
            e.printStackTrace();
            logger.debug(pttSession.sessionType + " ex: " + e.toString());
         }
      }
   }
}
