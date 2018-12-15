//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.p25.issi.p25payload.PacketType;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * This class handles incoming HEARTBEAT and other PTT packets related to
 * mute/unmute functions. This class also sends PTT MUTE/UNMUTE packets. This
 * class is associated with the receiver of the transmission (i.e., the SMF or
 * MMF receiver).
 * 
 */
public class MuteReceiver {

   /** The owning PTT session. */
   private PttSession pttSession = null;

   /** The collection of packet types this class handles. */
   private static HashSet<PacketType> handledPacketTypes = null;
   
   /** The unmute timer task. */
   protected UnmuteTask unmuteTask = null;

   /** The mute end loss timer task. */
   protected MuteEndLoss muteEndLossTask = null;

   private MuteHeartbeatListener muteHeartbeatListener = null;

   /** Indicates if unmute task is running. */
   private boolean unmuteTaskRunning = false;

   /** The current mute state. */
   Mute myMuteState = Mute.UNMUTED;

   /** The state of the M bit in the most recently received packet. */
   protected boolean badMBit = false;

   /** Indicates that the transmission is terminating. */
   private boolean transmissionTerminating = false;

   /** The logger for this class. */
   protected static Logger logger = Logger.getLogger(MuteReceiver.class);

   static {
      handledPacketTypes = new HashSet<PacketType>();
      handledPacketTypes.add(PacketType.HEARTBEAT);
      handledPacketTypes.add(PacketType.HEARTBEAT_QUERY);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_START);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_REQUEST);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_PROGRESS);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_END);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_GRANT);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_DENY);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_WAIT);
   }

   /**
    * Construct a PTT mute receiver.
    * 
    * @param pttSession
    *            The owning PTT session.
    */
   MuteReceiver(PttSession pttSession) {
      this.pttSession = pttSession;
   }

   /**
    * Handle incoming PTT packets especially HEARTBEAT packets. Check all
    * packets for correct M-bit.
    * 
    * @param p25Payload
    *            The incoming PTT packet.
    */
   void handlePttPacket(P25Payload p25Payload) {

      PacketType packetType = p25Payload.getISSIPacketType().getPacketType();
//      int unitId = p25Payload.getPTTControlWord() != null ?
//            p25Payload.getPTTControlWord().getUnitId() : 0;

      if (transmissionTerminating) {

         if (logger.isDebugEnabled())
            logger.debug(pttSession.sessionType + " MUTE Rx in DONE state");

         // Make sure we cancel all mute or unmute tasks
         if (unmuteTask != null)
            unmuteTask.cancel();

         // Cancel End Loss timout
         if (muteEndLossTask != null)
            muteEndLossTask.cancel();

      } else if (packetType == PacketType.PTT_TRANSMIT_END) {
         
         if (unmuteTask != null)
            unmuteTask.cancel();

         // Cancel End Loss timout
         if (muteEndLossTask != null)
            muteEndLossTask.cancel();
         transmissionTerminating = true;

      } else { // Any other packet type, except MUTE or UNMUTE

         badMBit = isMBitBad(p25Payload);
         if (badMBit) {
            if (myMuteState == Mute.UNMUTED) {
               // We are unmuted, but transmitter is sending muted so
               // inform the transmitter with an UNMUTE packet
               int tsn = p25Payload.getISSIPacketType().getTransmissionSequenceNumber();
               if (logger.isDebugEnabled())
                  logger.debug(pttSession.sessionType + " sending " + PacketType.PTT_TRANSMIT_UNMUTE);

               P25Payload outP25Payload = pttSession.createPttUnmute(tsn);
               try {
                  pttSession.sendPttPacket(outP25Payload);

               } catch (Exception e) {
                  e.printStackTrace();
               }

            } else if (myMuteState == Mute.MUTED) {
               // We are muted, but transmitter is sending unmuted so
               // inform the transmitter with a MUTE packet
               int tsn = p25Payload.getISSIPacketType().getTransmissionSequenceNumber();
               if (logger.isDebugEnabled())
                  logger.debug(pttSession.sessionType + " sending " + PacketType.PTT_TRANSMIT_MUTE);

               P25Payload outP25Payload = pttSession.createPttMute(tsn);
               try {
                  pttSession.sendPttPacket(outP25Payload);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }

         } else {

            // If not bad M bit, check if M bit is unmuted (M=0)
            // and cancel unmute transmitter.
            try {
               if (unmuteTask != null) {
                  if (unmuteTaskRunning) {
                     unmuteTaskRunning = false;
                     unmuteTask.cancel();
                  }
               }

            } catch (Exception e) {
               // If exception is caught here then unmute task is
               // already cancelled. Ignore.
            }
         }

         if (packetType == PacketType.HEARTBEAT) {
            heartBeatInd(p25Payload);
         }
      }
   }

   /**
    * Check if packet has correct M-bit.
    * 
    * @param p25Payload
    *            The incoming PTT packet.
    * @return True if M-bit is incorrect, false otherwise.
    */
   private boolean isMBitBad(P25Payload p25Payload) {

      boolean badM = false;
      int M = p25Payload.getISSIPacketType().getM();
      int myMuteStateValue = myMuteState.getMValue();
      if (myMuteStateValue != M) {
         badMBit = true;
         if (logger.isDebugEnabled()) {
            PacketType packetType = p25Payload.getISSIPacketType().getPacketType();
            logger.debug(pttSession.sessionType + " received " + packetType
                  + ", M=" + M + " (Incorrect M bit)");
         }
      }
      return badM;
   }

   /**
    * Handle incoming HEARTBEAT packet.
    * 
    * @param p25Payload
    *            The incoming PTT packet.
    */
   private void heartBeatInd(P25Payload p25Payload) {

      // There are two types of heartbeats in ISSI: (1) one for
      // connection maintenance and (2) for mute transmissions.
      // Here, we check which type of heartbeat is received before
      // processing; if TSN = 0, it is a connection maintenance heartbeat
      // or if TSN = 1, it is a mute transmission heartbeat.
      if (!badMBit) {

         int TSN = p25Payload.getISSIPacketType().getTransmissionSequenceNumber();
         int M = p25Payload.getISSIPacketType().getM();
         if (TSN != 0) {

            // Handle this mute transmission heartbeat
            if (logger.isDebugEnabled())
               logger.debug(pttSession.sessionType
                     + " received HEARTBEAT TSN=" + TSN + ", M=" + M
                     + " (Correct M bit)");

            // Alert application
            if (muteHeartbeatListener != null)
               muteHeartbeatListener.receivedMuteHeartbeat(TSN, M);

            // Reset End Loss timout
            if (muteEndLossTask != null)
               muteEndLossTask.cancel();

            muteEndLossTask = new MuteEndLoss();
            ISSITimer.getTimer().schedule(muteEndLossTask, TimerValues.TMUTEENDLOSS);

         } else {
            // Connection maintenance heartbeats should
            // already have been handled in HeartbeatReceiver.
         }
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

   public void addListener(MuteHeartbeatListener muteHeartbeatListener) {
      if (muteHeartbeatListener != null)
         this.muteHeartbeatListener = muteHeartbeatListener;
   }

   /**
    * Set this RFSS to MUTED state and send a mute packet to the transmitter.
    */
   /*public void setMute() {
      if (pttSession.remoteTsn > 0) {
         // Start End Loss timout
         if (muteEndLossTask != null)
            muteEndLossTask.cancel();

         muteEndLossTask = new MuteEndLoss();
         ISSITimer.getTimer().schedule(muteEndLossTask, TimerValues.TMUTEENDLOSS);

         // Kill PttSession.AudioTimeout since we won't be receiving
         // any progress packets during MUTE mode
         pttSession.muted = true;

         // Inform the transmitter with a MUTE packet
         if (logger.isDebugEnabled())
            logger.debug(pttSession.sessionType
                  + " STARTING MUTE. " + " Sending "
                  + PacketType.PTT_TRANSMIT_MUTE);
         myMuteState = Mute.MUTED;
         P25Payload outP25Payload = pttSession
               .createPttPacket(PacketType.PTT_TRANSMIT_MUTE);

         try {
            pttSession.sendPttPacket(outP25Payload);
         } catch (Exception e) {
            e.printStackTrace();
         }

      } else {
         if (logger.isDebugEnabled())
            logger.warn(pttSession.sessionType + " cannot MUTE "
                  + "because remote TSN is unknown.  Canceling "
                  + "mute task.");
      }
   }
   */
   
   /**
    * Set this RFSS to UNMUTED state and send an unmute packet to the 
    * transmitter.
    */
   public void setUnmute() {
      // inform the transmitter with an UNMUTE packet
      if (logger.isDebugEnabled())
         logger.debug(pttSession.sessionType + " STARTING UNMUTE. "
               + " Sending " + PacketType.PTT_TRANSMIT_UNMUTE);
      myMuteState = Mute.UNMUTED;

      // See Section 7.5.7
      unmuteTask = new UnmuteTask();
      ISSITimer.getTimer().schedule(unmuteTask, 0, TimerValues.TUNMUTE);
      unmuteTaskRunning = true;      
   }
   
   /**
    * This class sends PTT UNMUTEs to the transmitter.
    * 
    * @see TIA-109 Section 7.5.7
    */
   class UnmuteTask extends TimerTask {
      public void run() {
         // Cancel End Loss timout
         if (muteEndLossTask != null)
            muteEndLossTask.cancel();

         /* Tell the parent class that we are no longer in MUTED state. */
         pttSession.muted = false;
         P25Payload outP25Payload = pttSession
               .createPttPacket(PacketType.PTT_TRANSMIT_UNMUTE);
         try {
            pttSession.sendPttPacket(outP25Payload);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
   
   /**
    * This class implements EndLossTimeout when a MUTE HEARTBEAT is not
    * received within TMutedEndLoss time if this receiver is in a MUTE state.
    */
   class MuteEndLoss extends TimerTask {
      public void run() {
         if (logger.isDebugEnabled())
            logger.debug(pttSession.sessionType + " T MUTE END LOSS"
                  + " was reached.  No MUTE HEARTBEATS received while"
                  + " in MUTED state.  Shutting down.");

         // Make sure we cancel all mute or unmute tasks
         if (unmuteTask != null)
            unmuteTask.cancel();

         transmissionTerminating = true;
      }
   }
}
