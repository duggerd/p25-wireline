//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.p25.issi.p25payload.PacketType;

import java.util.HashSet;
import java.util.TimerTask;
import org.apache.log4j.Logger;

/**
 * This class defines a state machine driver for an SMF receiver. Note: There is
 * a race condition when implementing the TIA spec for SmfTransmitter and
 * MmfReceiver. To stay consistent, we update the state after sending a packet.
 */
public class SmfReceiver {

   protected static Logger logger = Logger.getLogger(SmfReceiver.class);

   /** My SMF session. */
   private SmfSession smfSession = null;

   /** The application listener. */
   private SmfRxListener smfRxListener = null;

   /** Start state. */
   private SmfRxState currentState = SmfRxState.BEGIN;

   /** Sets timeout between SPURT START and first PTT PROGRESS. */
   private FirstPacketTimeoutTask firstPacketTimeoutTask = null;

   /** Audio timeout task */
   protected AudioTimeoutTask audioTimeoutTask = null;

   /** Collection of packet types this class handles. */
   private static HashSet<PacketType> handledPacketTypes = null;


   static {
      // Set Rx transitions from BEGIN state
      PttPath.add(new SmfRxPath(SmfRxState.BEGIN,
            SmfRxTransition.SPURT_START, SmfRxState.RECEIVING));

      PttPath.add(new SmfRxPath(SmfRxState.BEGIN,
            SmfRxTransition.RECEIVE_AUDIO, SmfRxState.RECEIVING));

      // Set Rx transitions from RECEIVING state
      PttPath.add(new SmfRxPath(SmfRxState.RECEIVING,
            SmfRxTransition.RECEIVE_AUDIO, SmfRxState.RECEIVING));

      PttPath.add(new SmfRxPath(SmfRxState.RECEIVING,
            SmfRxTransition.SPURT_END, SmfRxState.DONE));

      // Handled packet types
      handledPacketTypes = new HashSet<PacketType>();
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_START);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_PROGRESS);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_END);
   }

   /**
    * Construct an SMF receiver.
    * 
    * @param smfSession
    *            The associated SMF session.
    */
   SmfReceiver(SmfSession smfSession) {
      this.smfSession = smfSession;
   }

   /**
    * Handle incoming PTT packet.
    * 
    * @param p25Payload
    *            The incoming PTT packet.
    */
   void handlePttPacket(P25Payload p25Payload) {
      PacketType packetType = p25Payload.getISSIPacketType().getPacketType();
      SmfPacketEvent packetEvent = new SmfPacketEvent(smfSession, p25Payload);
      if (logger.isDebugEnabled())
         logger.debug(this + ":\n\tSMF RX received: " + packetType);

      if (currentState == SmfRxState.DONE) {
         logger.debug(this
               + ":\n\tSMF RX received packet in the DONE state.  Ignoring.");

      } else if (packetType == PacketType.PTT_TRANSMIT_START) {
         spurtRqstInd(packetEvent);

      } else if (packetType == PacketType.PTT_TRANSMIT_PROGRESS) {
         audioInd(packetEvent);

      } else if (packetType == PacketType.PTT_TRANSMIT_END) {
         spurtEndInd(packetEvent);

      } else {
         if (logger.isDebugEnabled()) {
            logger.error(this + ":\n\tSMF RX cannot handle packet type: " + packetType);
         }
      }
   }

   /**
    * Handle incoming spurt request.
    * 
    * @param event
    *            The SMF packet event.
    */
   private void spurtRqstInd(SmfPacketEvent event) {
      try {
         if (currentState == SmfRxState.BEGIN) {
            if (smfRxListener != null) {
               logger.debug("smfRxListener is " + this.smfRxListener);
               this.smfRxListener.receivedPttStart(event);
            } else {
               logger.debug("SmfRx : No listener registered");
            }
            updateRxState(SmfRxTransition.SPURT_START);

            // Start audio timeout task
            if (firstPacketTimeoutTask != null)
               firstPacketTimeoutTask.cancel();
            firstPacketTimeoutTask = new FirstPacketTimeoutTask();
            ISSITimer.getTimer().schedule(firstPacketTimeoutTask,
                  TimerValues.TFIRSTPACKETTIME);

            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
               logger.debug(this + ":\n\tSMF RX received spurt start");

         } else if (currentState == SmfRxState.RECEIVING) {

            // Reset the first packet timeout task
            if (firstPacketTimeoutTask != null) {
               // could be null if firstpacket is a PROGRESS packet.
               firstPacketTimeoutTask.cancel();
            }
            firstPacketTimeoutTask = new FirstPacketTimeoutTask();
            ISSITimer.getTimer().schedule(firstPacketTimeoutTask,
                  TimerValues.TFIRSTPACKETTIME);

            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
               logger.debug(this + ":\n\tSMF RX received "
                     + "PTT_TRANSMIT_START while in RECEIVING state."
                     + " Resetting TFirstpacketTime timeout");
         }

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Handle incoming audio request.
    * 
    * @param event
    *            The SMF packet event.
    */
   private void audioInd(SmfPacketEvent event) {
      try {
         updateRxState(SmfRxTransition.RECEIVE_AUDIO);

         // Cancel first packet timeout
         if (firstPacketTimeoutTask != null)
            firstPacketTimeoutTask.cancel();

         // Reset timeout timer
         if (audioTimeoutTask != null)
            audioTimeoutTask.cancel();

         audioTimeoutTask = new AudioTimeoutTask();
         ISSITimer.getTimer().schedule(audioTimeoutTask,
               TimerValues.TENDLOSS);

         if (smfSession.muteReceiver.myMuteState == Mute.MUTED) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
               logger.debug(this + ":\n\tSMF RX is muted.  "
                     + "Discarding audio packet");

         } else if (smfSession.muteReceiver.myMuteState == Mute.UNMUTED) {
            // Test to see if IMBE Block Header TimeStamp offset is set
            // properly:
            // P25Payload p = event.p25Payload;
            // ISSIPacketType pt = p.getISSIPacketType();
            // System.out.println("[TEST SmfReceiver: packet type: " +
            // PacketType.getInstance(pt.getPT()));
            //            
            // Vector<BlockHeader> bv = p.getBlockHeaderVector();
            // for (BlockHeader blockHeader : bv) {
            // System.out.println("[TEST SmfReceiver]: " +
            // BlockType.getInstance(blockHeader.getBT()) +
            // ", TSO: " +
            // blockHeader.getTimeStampOffset());
            // }
            if (smfRxListener != null)
               smfRxListener.receivedPttProgress(event);
         }

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Handle incoming spurt end.
    * 
    * @param event
    *            The SMF packet event.
    */
   private void spurtEndInd(SmfPacketEvent event) {
      try {
         updateRxState(SmfRxTransition.SPURT_END);
         if (firstPacketTimeoutTask != null)
            firstPacketTimeoutTask.cancel();

         if (audioTimeoutTask != null)
            audioTimeoutTask.cancel();

         if (smfRxListener != null)
            smfRxListener.receivedPttEnd(event);

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Check if this class handles this packet type.
    * 
    * @param packetType
    *            The packet type to be handled.
    * @return True if packet type handled, false otherwise.
    */
   boolean isPacketTypeHandled(PacketType packetType) {
      return handledPacketTypes.contains(packetType);
   }

   private void updateRxState(SmfRxTransition rxTransition)
         throws IllegalStateException {
      this.currentState = (SmfRxState) PttPath.getNextState(currentState, rxTransition);
   }

   /**
    * Get the current state of the smf receiver.
    * 
    * @return
    */
   public SmfRxState getCurrentState() {
      return this.currentState;
   }

   /**
    * Add an audio listener.
    */
   public void addListener(SmfRxListener smfRxListener) {
      this.smfRxListener = smfRxListener;
   }

   /**
    * This class implements the timeout for receiving audio. Note that there
    * are a number of specification issues with this class.
    * 
    * @see TIA-109.BACA Section 7.6.2
    */
   class FirstPacketTimeoutTask extends TimerTask {
      public void run() {
         // If we reach this point, then we have reached the timeout
         // for waiting for teh first audio packet, just to move to
         // DONE state.
         currentState = SmfRxState.DONE;
         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tSMF RX reached FIRST PACKET "
                  + "TIMEOUT.  Moving to DONE state.");

         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tSMF RX entering STATE: " + currentState);
      }
   }

   /**
    * This class implements the timeout for receiving audio.
    */
   class AudioTimeoutTask extends TimerTask {
      public void run() {
         // Check if we are in a MUTED state. If so, we can simply
         // cancel this task.
         if (smfSession.muted) {
            this.cancel();
            return;
         }

         // If we reach this point, then we have reached the timeout
         // for waiting for audio packets, just to move to DONE state.
         currentState = SmfRxState.DONE;
         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tSMF RX reached AUDIO TIMEOUT");

         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tSMF RX entering STATE: " + currentState);
      }
   }
}
