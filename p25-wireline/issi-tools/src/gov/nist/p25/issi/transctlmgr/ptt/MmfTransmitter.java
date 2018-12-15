//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.p25payload.IMBEVoiceBlock;
import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.p25.issi.p25payload.TransmitPriorityType;
import gov.nist.rtp.*;

import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import org.apache.log4j.Logger;

/**
 * This class defines a state machine driver for an MMF transmitter. Note there
 * is a race condition when implementing the TIA spec for SmfTransmitter and
 * MmfReceiver. To stay consistent with the SMF transmitter, we update the state
 * after sending a packet.
 */
public class MmfTransmitter {

   private static Logger logger = Logger.getLogger(MmfTransmitter.class);

   /** Indicates that this transmitter has already started. */
   private boolean transmitterStarted = false;

   /** My MMF session. */
   private MmfSession mmfSession = null;

   private ConcurrentHashMap<Integer, MmfTxState> tsnTable = 
         new ConcurrentHashMap<Integer, MmfTxState>();

   /** Used to test SMF Rx audio timeout by blocking MMF Tx transmission. */
   private boolean blockOutgoingAudioTransmission = false;

   static {
      // Set Tx transitions from START state
      PttPath.add(new MmfTxPath(MmfTxState.BEGIN, MmfTxTransition.TX_TRIGGER,
            MmfTxState.TRANSMITTING));

      PttPath.add(new MmfTxPath(MmfTxState.BEGIN, MmfTxTransition.SEND_AUDIO,
            MmfTxState.TRANSMITTING));

      // Set Tx transitions from TRANSMITTING state
      PttPath.add(new MmfTxPath(MmfTxState.TRANSMITTING,
            MmfTxTransition.SEND_AUDIO, MmfTxState.TRANSMITTING));

      PttPath.add(new MmfTxPath(MmfTxState.TRANSMITTING,
            MmfTxTransition.SEND_CHANGE_IN_LOSING_STATE,
            MmfTxState.TRANSMITTING));

      PttPath.add(new MmfTxPath(MmfTxState.TRANSMITTING,
            MmfTxTransition.END_TRIGGER, MmfTxState.TERMINATED));
   }

   // constructor
   MmfTransmitter(MmfSession mmfSession) {
      this.mmfSession = mmfSession;
   }

   /**
    * Send a spurt start to the given unit ID.
    * 
    * @param unitId --
    *            the unit id.
    * @param priorityType --
    *            the priority type.
    * @param priorityLevel --
    *            the priority level.
    * @throws PttException
    */
   public void sendSpurtStart(int systemId, int unitId, int tsn,
         TransmitPriorityType priorityType, int priorityLevel)
         throws PttException {

      try {

         if (MmfTransmitter.this.getCurrentState(tsn) == MmfTxState.BEGIN) {

            updateTxState(tsn, MmfTxTransition.TX_TRIGGER);
            if (logger.isDebugEnabled())
               logger.debug(this + ":\n\tMMF TX sending PTT " + "TRANSMIT START");

            // Note that PTT STARTS can be sent RSTART times, which
            // range from 1 to 5 (default 4). Here, we only send a single
            // PTT START.
            int newTsn = mmfSession.getTsnForUnitId(systemId, unitId);
            mmfSession.clearLosingAudio(newTsn);
            
            P25Payload p25Payload = mmfSession.createPttTransmitStart(systemId, unitId, priorityType,
                  priorityLevel);
            mmfSession.addNewTsnToMap(tsn, newTsn);
            mmfSession.sendPttPacket(p25Payload);

         } else {
            throw new PttException(
                  "Cannot send PTT START if not in MmfTxState.BEGIN state. Current state is    "
                        + MmfTransmitter.this.getCurrentState(tsn));
         }

      } catch (IllegalStateException ise) {
         ise.printStackTrace();
      } catch (RtpException re) {
         re.printStackTrace();
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
   }

   /**
    * Start a spurt request.
    * 
    * @param imbeVoiceBlocks
    *            An array of IMBE voice blocks.
    */
   public void sendVoice(IMBEVoiceBlock[] imbeVoiceBlocks, int systemId, int fromUnitId,
         int tsn, 
         TransmitPriorityType transmitPriorityType, int transmitPriorityLevel)
         throws IllegalStateException {

      if (mmfSession.muteTransmitter.peerMuteState == Mute.MUTED) {
         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tMMF TX peer (SMF) is muted.  "
                  + "Not sending PTT PROGRESS. Discarding packet silently");
      } else {
         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tMMF TX STATE: "
                  + getCurrentState(tsn));

         if (MmfTransmitter.this.getCurrentState(tsn) == MmfTxState.TRANSMITTING) {
            updateTxState(tsn, MmfTxTransition.SEND_AUDIO);
            sendProgressPacket(imbeVoiceBlocks, systemId,fromUnitId, tsn,
                  transmitPriorityType, transmitPriorityLevel);
         } else {
            logger.debug("Not sending audio current state = "
                  + getCurrentState(tsn));
         }
      }
   }

   /**
    * Self-grant (force) a transmission without a spurt request.
    * 
    * @param imbeVoiceBlocks
    *            An array of IMBE voice blocks.
    */
   public void forceAudio(IMBEVoiceBlock[] imbeVoiceBlocks, int systemId, int fromUnitId,
         int tsn, TransmitPriorityType priorityType, int level)
         throws IllegalStateException {
      if (this.getCurrentState(tsn) != MmfTxState.TRANSMITTING)
         this.updateTxState(tsn, MmfTxTransition.TX_TRIGGER);

      sendProgressPacket(imbeVoiceBlocks, systemId, fromUnitId, tsn, 
            priorityType, level);
   }

   /**
    * Block outgoing audio transmission. Can be used to test SMF audio timeout.
    */
   public void blockOutgoingAudio() throws PttException {

      if (transmitterStarted) {
         throw new PttException( "Cannot set AUDIO BLOCK after transmitter "
               + "has started. Make sure that MmfTransmitter."
               + "setOutgoingAudioBlock() is set before starting "
               + "mfTransmitter.spurtRqst() or SmfTransmitter.forceAudio().");
      } else {
         blockOutgoingAudioTransmission = true;
      }
   }

   /**
    * Send a progress packet after if the state is not muted.
    * 
    * @param imbeVoiceBlocks --
    *            voice blocks to send.
    */
   private void sendProgressPacket(IMBEVoiceBlock[] imbeVoiceBlocks,
         int fromSystemId,
         int fromUnitId, int tsn, 
         TransmitPriorityType transmitPriorityType, int transmitPriorityLevel) {
      if (mmfSession.muteTransmitter.peerMuteState == Mute.MUTED) {

         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tMMF TX peer (SMF) is muted.  "
                  + "Not sending PTT PROGRESS.");
      } else {
         try {
            if (logger.isDebugEnabled()) {
               logger.debug("fromUnitId : " + fromUnitId);
            }
            P25Payload p25Payload = mmfSession.createPttTransmitProgress(
                  fromSystemId, fromUnitId,  transmitPriorityType,
                  transmitPriorityLevel,imbeVoiceBlocks);
            mmfSession.addNewTsnToMap(tsn,p25Payload.getISSIPacketType().getTransmissionSequenceNumber());
            if (!blockOutgoingAudioTransmission) {
               mmfSession.sendPttPacket(p25Payload);

            } else {
               /* Uncomment this to test SMF audio timeout */
               // if (i == 0)
               // mmfSession.sendPttPacket(p25Payload);
               if (logger.isDebugEnabled())
                  logger.debug(this + ":\n\tMMF blocking outgoing " + "audio packet");
            }
         } catch (Exception ex) {
            logger.error("Unexpected exception while sending packet", ex);
         }
      }
   }

   /**
    * @return the currentState
    */
   public MmfTxState getCurrentState(int tsn) {
      if (this.tsnTable.get(tsn) == null) {
         return MmfTxState.BEGIN;
      } else
         return this.tsnTable.get(tsn);
   }

   /**
    * Send spurt end notification.
    */
   public void sendSpurtEndNotification(int tsn) {

      // First stop MUTE-related heartbeats
      mmfSession.muteTransmitter.shutDown();

      if (logger.isDebugEnabled())
         logger.debug(this + ":\n\tMMF MUTE Tx stopping " + "MUTE heartbeats.");

      try {
         updateTxState(tsn, MmfTxTransition.END_TRIGGER);
         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tMMF TX sending " + "PTT_TRANSMIT_END");

         // Note that PTT ENDs can be sent REND times, which
         // range from 1 to 5 (default 4). Here, we only send a single
         // PTT END.
         int remoteTsn = this.mmfSession.getOutgoingTsn(tsn);
         if ( this.mmfSession.isLosingAudio(remoteTsn)) {
            this.mmfSession.clearLosingAudio(remoteTsn);
         } else {
            this.mmfSession.clearNextLosingAudio();
         }
         P25Payload p25Payload = mmfSession.createPttEndForTsn(remoteTsn);
            
         // End packet has to be sent REND times.
         mmfSession.sendPttPacket(p25Payload);

      } catch (IllegalStateException ise) {
         ise.printStackTrace();

      } catch (RtpException re) {
         re.printStackTrace();

      } catch (IOException ioe) {
         ioe.printStackTrace();

      }
   }

   /**
    * Update the current state.
    * 
    * @param rxTransition
    *            The current transition.
    * @throws IllegalStateException
    */
   private void updateTxState(int tsn, MmfTxTransition txTransition)
         throws IllegalStateException {
      this.setCurrentState(tsn, (MmfTxState) PttPath.getNextState(this
            .getCurrentState(tsn), txTransition));
   }

   private void setCurrentState(int tsn, MmfTxState currentState) {
      this.tsnTable.put(tsn, currentState);
   }
}
