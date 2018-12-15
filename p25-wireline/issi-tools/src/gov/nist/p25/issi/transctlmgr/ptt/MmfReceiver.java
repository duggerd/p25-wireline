//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.p25payload.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import org.apache.log4j.*;

/**
 * This class defines a state machine driver for an MMF. Note: There is a race
 * condition when implementing the TIA spec that occurs if we attempt to update
 * the state after sending a packet. The problem is that a packet may be sent
 * and a response received before the state is updated. To fix this problem, we
 * update the state immediately before sending the packet.
 */
public class MmfReceiver {

   /** Indicates that this transmitter has already started. */
   private boolean transmitterStarted = false;

   /** The owning MMF session. */
   private MmfSession mmfSession = null;

   /** The application audio listener. */
   //private AudioListener audioListener = null;

   /** The application control listener */
   private MmfRxListener mmfRxListener = null;

   /** The current state indexed by TSN*/
   private Hashtable<Integer,MmfRxState> currentState;
   //private MmfRxState currentState = MmfRxState.BEGIN;

   /** The collection of packet types this class can handle. */
   private static HashSet<PacketType> handledPacketTypes = null;

   /** The wait task. */
   private WaitTask waitTask = null;

   /** The time to wait during a WAIT state. */
   private long waitDuration = 0;

   /** User-defined flag to ignore incoming requests without a response. */
   private boolean ignoreRequests = false;

   /**
    * The timestamp of the first (of Rend) received PTT TRANSMIT END packet.
    * This is used by the ISSI Control Function to compare against its
    * MinKeybackTime timeout value.
    */
   private long lastPttEndPacketTimeStamp;

   /** Sets timeout between GRANT and first PTT PROGRESS. */
   private FirstPacketTimeoutTask firstPacketTimeoutTask = null;

   /** The audio timeout task */
   protected AudioTimeoutTask audioTimeoutTask = null;

   private static Logger logger = Logger.getLogger(MmfReceiver.class);

   /* TODO (steveq): Remove the following state machine variables in v2. */

   /** The application-dependent and default transition behavior. */
   private MmfRxTransition arbitrateTransition = MmfRxTransition.GRANT_DECISION;

   static {
      // Set Rx transitions from BEGIN state
      PttPath.add(new MmfRxPath(MmfRxState.BEGIN,
            MmfRxTransition.SPURT_REQUEST,MmfRxState.ARBITRATE));

      PttPath.add(new MmfRxPath(MmfRxState.BEGIN,
            MmfRxTransition.RECEIVE_AUDIO,      
            MmfRxState.RECEIVING));

      PttPath.add(new MmfRxPath(MmfRxState.BEGIN,
            MmfRxTransition.SPURT_END,
            MmfRxState.DONE));

      // Set Rx transitions from ARBITRATE state
      PttPath.add(new MmfRxPath(MmfRxState.ARBITRATE,
            MmfRxTransition.GRANT_DECISION,
             MmfRxState.RECEIVING));

      PttPath.add(new MmfRxPath(MmfRxState.ARBITRATE,
            MmfRxTransition.WAIT_DECISION,
            MmfRxState.WAIT));

      PttPath.add(new MmfRxPath(MmfRxState.ARBITRATE,
            MmfRxTransition.DENY_DECISION,
            MmfRxState.DENY));

      // Set Rx transitions from RECEIVING state
      PttPath.add(new MmfRxPath(MmfRxState.RECEIVING,
            MmfRxTransition.RECEIVE_AUDIO,
            MmfRxState.RECEIVING));

      PttPath.add(new MmfRxPath(MmfRxState.RECEIVING,
            MmfRxTransition.RECEIVE_CHANGE_IN_LOSING_AUDIO,
            MmfRxState.RECEIVING));

      PttPath.add(new MmfRxPath(MmfRxState.RECEIVING,
            MmfRxTransition.SPURT_END,
             MmfRxState.DONE));

      PttPath.add(new MmfRxPath(MmfRxState.RECEIVING,
            MmfRxTransition.END_LOSS_TIMEOUT,
            MmfRxState.DONE));

      // Set Rx transitions from WAIT state
      /*
       * Here, we simply stay in the wait state and send a WAIT message to the
       * sender. rxWaitPaths.add(new MmfRxPath(MmfRxState.WAIT,
       * MmfRxTransition.SPURT_REQUEST,
       * TransitionInvocation.INVOKED_BY_RECVD_PACKET, MmfRxState.ARBITRATE,
       * null, PacketType.PTT_TRANSMIT_WAIT)); // We always wait again
       */

      PttPath.add(new MmfRxPath(MmfRxState.WAIT,
            MmfRxTransition.GRANT_TRIGGER,
            MmfRxState.RECEIVING));

      PttPath.add(new MmfRxPath(MmfRxState.WAIT,
            MmfRxTransition.START_RECEIVING,
            MmfRxState.RECEIVING));

      PttPath.add(new MmfRxPath(MmfRxState.WAIT,
            MmfRxTransition.SPURT_END,
            MmfRxState.DONE));

      PttPath.add(new MmfRxPath(MmfRxState.WAIT,
            MmfRxTransition.GRANT_TRIGGER,
            MmfRxState.RECEIVING));

      PttPath.add(new MmfRxPath(MmfRxState.WAIT,
            MmfRxTransition.DENY_TRIGGER,
            MmfRxState.DENY));

      // Set Rx transitions from DENY state
      PttPath.add(new MmfRxPath(MmfRxState.DENY,
            MmfRxTransition.DENY_TRIGGER,
            MmfRxState.DENY));

      PttPath.add(new MmfRxPath(MmfRxState.DENY,
            MmfRxTransition.SPURT_END,
            MmfRxState.DONE));

      PttPath.add(new MmfRxPath(MmfRxState.DENY,
            MmfRxTransition.END_LOSS_TIMEOUT,
            MmfRxState.DONE));

      PttPath.add(new MmfRxPath(MmfRxState.DENY,
            MmfRxTransition.SPURT_REQUEST,
            MmfRxState.DENY));

      PttPath.add(new MmfRxPath(MmfRxState.DENY,
            MmfRxTransition.RECEIVE_AUDIO,
            MmfRxState.RECEIVING));

      handledPacketTypes = new HashSet<PacketType>();
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_REQUEST);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_PROGRESS);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_END);
   }

   // constructor
   MmfReceiver(MmfSession mmfSession) {
      this.mmfSession = mmfSession;
      this.currentState = new Hashtable<Integer,MmfRxState>();
   }

   public void setIgnoreRequest() {
      ignoreRequests = true;
   }

   /*
    * TODO (steveq): Remove all configuration methods for v2. This
    * functionality should be executed directly by the calling application or
    * ISSI Control Function.
    */

   /**
    * Tell the transmitter to invoke GRANT DECISION after receiving a PTT
    * TRANSMIT REQUEST.
    * 
    * @throws PttException
    */
   public void setArbitrateGrant() throws PttException {
      if (transmitterStarted) {
         throw new PttException( "Cannot set GRANT after transmitter has "
               + "started.   Make sure that MmfTransmitter.setArbitrateGrant()"
               + " is set before starting MmfTransmitter.spurtRqst() or "
               + "MmfTransmitter.forceAudio().");
      } else {
         arbitrateTransition = MmfRxTransition.GRANT_DECISION;
      }
   }

   /**
    * Tell the transmitter to invoke DENY DECISION after receiving a PTT
    * TRANSMIT REQUEST.
    * 
    * @throws PttException
    */
   public void setArbitrateDeny() throws PttException {
      if (transmitterStarted) {
         throw new PttException( "Cannot set DENY after transmitter has "
               + "started.  Make sure that MmfTransmitter.setArbitrateDeny() "
               + "is set before starting MmfTransmitter.spurtRqst() or "
               + "MmfTransmitter.forceAudio().");
      } else {
         arbitrateTransition = MmfRxTransition.DENY_DECISION;
      }
   }

   /**
    * This method tells the transmitter to invoke WAIT DECISION followed by a
    * GRANT DECISION after receiving a PTT TRANSMIT REQUEST.
    * 
    * @throws PttException
    */
   public void setArbitrateWaitThenGrant(long duration) throws PttException {
      if (transmitterStarted) {
         throw new PttException( "Cannot set WAIT/GRANT after transmitter "
               + "has started.  Make sure that MmfTransmitter."
               + "setArbitrateWaitThenGrant() is set before starting "
               + "MmfTransmitter.spurtRqst() or MmfTransmitter.forceAudio().");
      } else {
         waitDuration = duration;
         arbitrateTransition = MmfRxTransition.WAIT_THEN_GRANT;
      }
   }

   /**
    * This method tells the transmitter to invoke WAIT DECISION followed by a
    * DENY DECISION after receiving a PTT TRANSMIT REQUEST.
    * 
    * @throws PttException
    */
   public void setArbitrateWaitThenDeny(long duration) throws PttException {
      if (transmitterStarted) {
         throw new PttException( "Cannot set WAIT/GRANT after transmitter "
               + "has started.  Make sure that MmfTransmitter."
               + "setArbitrateWaitThenDeny() is  set before starting "
               + "MmfTransmitter.spurtRqst() or MmfTransmitter.forceAudio().");
      } else {

         waitDuration = duration;
         arbitrateTransition = MmfRxTransition.WAIT_THEN_DENY;
      }
   }
   
   private void setCurrentState(int tsn, MmfRxState newState) {
      logger.debug("MmfReceiver : setCurrentState [" + this.mmfSession.getMyRtpRecvPort() + "]" 
            + " tsn = " + tsn 
            + " newstate = " + newState   );
      if (logger.isDebugEnabled()) {
         logger.debug("setting mmf state [" + 
               this.mmfSession.getRtpSession().getMyRtpRecvPort() + "]"+  newState);
         logger.debug("current mmf Rx state " + this.currentState);
      }
      if (getCurrentState(tsn) == MmfRxState.DONE) {
         logStackTrace(); // see where we are getting called from
      }
      this.currentState.put(tsn, newState);
   }

   /**
    * Handle incoming PTT packets.
    * 
    * @param p25Payload
    *            The incoming PTT packet.
    */
   protected void handlePttPacket(P25Payload p25Payload) {

      PacketType packetType = p25Payload.getISSIPacketType().getPacketType();
      MmfPacketEvent event = new MmfPacketEvent(mmfSession, p25Payload);
      int tsn = p25Payload.getISSIPacketType().getTransmissionSequenceNumber();

      if (logger.isDebugEnabled())
         logger.debug(this + ":\n\tMMF RX received: " + packetType.toString());

      if (getCurrentState(tsn) == MmfRxState.DONE) {
         logger.debug(this + ":\n\tMMF RX received " + packetType
               + " in the DONE state.  Ignoring.");

      } else if (packetType == PacketType.PTT_TRANSMIT_REQUEST) {
         if (ignoreRequests) {
            logger.debug(this + ":\n\tMMF RX in IGNORE REQUEST MODE. "
                  + "Ignoring received " + packetType + ".");
            return;
         }
         receivedPttRequest(event);

      } else if (packetType == PacketType.PTT_TRANSMIT_PROGRESS) {

         // This goes into PttSession.verifyIncomingPacket
         // ISSIPacketType issiPacketType = p25Payload.getISSIPacketType();
         // mmfSession.remoteTsn = issiPacketType.getTSN();
         audioInd(event);

      } else if (packetType == PacketType.PTT_TRANSMIT_END) {
         spurtEndInd(event);

      } else {
         if (logger.isDebugEnabled())
            logger.error(this + ":\n\tMMF RX cannot handle packet type: " + packetType);
      }
   }
   
   /**
    * Get the curent state corresponding to a TSN. If a state
    * assignment for the tsn does not exist then we just return
    * BEGIN.
    * 
    * @param tsn - Transmission Seq No for the state we want to check.
    * @return  the current state.
    */
   public MmfRxState getCurrentState (int tsn) {
      if ( ! this.currentState.containsKey(tsn)) {
         return MmfRxState.BEGIN;
      } else {
         return this.currentState.get(tsn);
      }
   }
   /**
    * Handle incoming spurt request.
    * 
    * @param event
    *            The MMF packet event.
    */
   public void receivedPttRequest(MmfPacketEvent event) {
      try {
         // Check if we are already waiting on a previous SPURT_REQUEST on
         int tsn = event.getPttPacket().getISSIPacketType().getTransmissionSequenceNumber();
         
         if (getCurrentState(tsn) == MmfRxState.RECEIVING) {
            if (logger.isDebugEnabled())
               logger.debug(this + ":\n\tMMF RX State: " + this.getCurrentState(tsn)
                     + " - Already in receiving state. ");
            sendGrant(tsn);   
            return;

         } else if (getCurrentState(tsn) == MmfRxState.WAIT) {
            if (logger.isDebugEnabled())
               logger.debug(this + ":\n\tMMF RX State: " + this.getCurrentState(tsn)
                     + " - Already waiting on previous spurt request. ");

            // If we are already in the WAIT state, we simply stay in the
            // WAIT state and return a WAIT message.
            P25Payload p25Payload = mmfSession.createPttWait(tsn);
            mmfSession.sendPttPacket(p25Payload);
            return;

         }

         updateRxState(tsn,MmfRxTransition.SPURT_REQUEST);
         if (this.mmfRxListener != null) {

            // Check to see if this Request contains voice.
            IMBEVoiceBlock[] imbeVoiceBlocks = event.p25Payload.getIMBEVoiceBlockArray();

            if (imbeVoiceBlocks != null) {
               // The spurt request is forwarded if we have a control listener.
               this.mmfRxListener.receivedPttRequestWithVoice(event);
            } else {
               // The spurt request is forwarded if we have a control listener.
               this.mmfRxListener.receivedPttRequest(event);
            }

         } else if (arbitrateTransition == MmfRxTransition.GRANT_DECISION) {
            sendGrant(tsn);

         } else if (arbitrateTransition == MmfRxTransition.DENY_DECISION) {
            sendDeny(tsn);

         } else if (arbitrateTransition == MmfRxTransition.WAIT_THEN_GRANT) {
            // Wait a period of time then grant
            sendWait(MmfRxTransition.GRANT_DECISION,tsn);

         } else if (arbitrateTransition == MmfRxTransition.WAIT_THEN_DENY) {
            // Wait a period of time then deny
            sendWait(MmfRxTransition.DENY_DECISION,tsn);
         }
      } catch (Exception e) {
         logger.error("Error processing SPURT_REQUEST", e);
      }
   }

   /**
    * Called from the control listener to arbitrate and send the appropriate
    * signal.
    * 
    */
   public void arbitrate(PttEvent event) {

      int tsn = event.getPttPacket().getISSIPacketType().getTransmissionSequenceNumber();
      logger.debug("MmfReceiver: arbitrate TSN = " + tsn);
      
      if (this.getCurrentState(tsn) == MmfRxState.DENY || getCurrentState(tsn) == MmfRxState.DONE)
         return;

      if (arbitrateTransition == MmfRxTransition.GRANT_DECISION) {
         sendGrant(tsn);
      } else if (arbitrateTransition == MmfRxTransition.DENY_DECISION) {
         sendDeny(tsn);
      } else if (arbitrateTransition == MmfRxTransition.WAIT_THEN_GRANT) {
         // Wait a period of time then grant
         sendWait(MmfRxTransition.GRANT_DECISION, tsn);
      } else if (arbitrateTransition == MmfRxTransition.WAIT_THEN_DENY) {
         // Wait a period of time then deny
         sendWait(MmfRxTransition.DENY_DECISION,tsn);
      }
   }

   /**
    * Handle incoming audio.
    * 
    * @param event
    *            The MMF packet event.
    */
   private void audioInd(MmfPacketEvent event) {

      // Cancel first packet timeout
      if (firstPacketTimeoutTask != null)
         firstPacketTimeoutTask.cancel();

      // Reset audio timeout timer
      if (audioTimeoutTask != null)
         audioTimeoutTask.cancel();
      int tsn = event.getPttPacket().getISSIPacketType().getTransmissionSequenceNumber();

      audioTimeoutTask = new AudioTimeoutTask(tsn);
      ISSITimer.getTimer().schedule(audioTimeoutTask, TimerValues.TENDLOSS);
      try {

         if (getCurrentState(tsn) == MmfRxState.ARBITRATE) {
            updateRxState(tsn,MmfRxTransition.RECEIVE_AUDIO);

         } else if (getCurrentState(tsn) == MmfRxState.BEGIN) {
            updateRxState(tsn,MmfRxTransition.RECEIVE_AUDIO);

         } else if (getCurrentState(tsn) == MmfRxState.WAIT) {
            if (waitTask != null)
               waitTask.cancel();
            updateRxState(tsn,MmfRxTransition.START_RECEIVING);

         } else if (getCurrentState(tsn) == MmfRxState.DENY) {
            // Move to receiving state. Note that at this point,
            // this MMF will decide whether or not to MUTE or set
            // Losing Audio of the incoming transmission. For now,
            // we simply accept the incoming transmission.
            updateRxState(tsn,MmfRxTransition.RECEIVE_AUDIO);

         }

         if (mmfSession.muteReceiver.myMuteState == Mute.MUTED) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
               logger.debug(this + ":\n\tMMF RX is muted.  " + "Discarding audio packet");

         } else if (mmfSession.muteReceiver.myMuteState == Mute.UNMUTED) {

            // Test to see if IMBE Block Header TimeStamp offset is set
            // properly:
            // P25Payload p = event.p25Payload;
            // ISSIPacketType pt = p.getISSIPacketType();
            // System.out.println("[TEST MmfReceiver: packet type: " +
            // PacketType.getInstance(pt.getPT()));
            //            
            // Vector<BlockHeader> bv = p.getBlockHeaderVector();
            // for (BlockHeader blockHeader : bv) {
            // System.out.println("[TEST MmfReceiver]: " +
            // BlockType.getInstance(blockHeader.getBT()) +
            // ", TSO: " +
            // blockHeader.getTimeStampOffset());
            // }
            if (mmfRxListener != null)
               mmfRxListener.receivedPttProgress(event);
         }

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Handle incoming spurt end.
    * 
    * @param event
    *            The MMF packet event.
    */
   public void spurtEndInd(MmfPacketEvent event) {

      try {

         int tsn = event.getPttPacket().getISSIPacketType().getTransmissionSequenceNumber();
         updateRxState(tsn,MmfRxTransition.SPURT_END);

         // Timestamp the arrival of the first PTT TRANSMIT END packet.
         if (lastPttEndPacketTimeStamp == 0)
            lastPttEndPacketTimeStamp = System.currentTimeMillis();

         if (firstPacketTimeoutTask != null)
            firstPacketTimeoutTask.cancel();

         if (waitTask != null)
            waitTask.cancel();

         if (audioTimeoutTask != null)
            audioTimeoutTask.cancel();

         if (mmfRxListener != null)
            mmfRxListener.receivedPttEnd(event);

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Send GRANT response.
    */
   private void sendGrant(int tsn) {
      try {
         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tMMF RX sending PTT TRANSMIT GRANT");

         if (getCurrentState(tsn) == MmfRxState.ARBITRATE) {
            updateRxState(tsn,MmfRxTransition.GRANT_DECISION);

         } else if (getCurrentState(tsn) == MmfRxState.WAIT) {
            updateRxState(tsn,MmfRxTransition.GRANT_TRIGGER);
         }

         // Start audio timeout task
         if (firstPacketTimeoutTask != null)
            firstPacketTimeoutTask.cancel();

         firstPacketTimeoutTask = new FirstPacketTimeoutTask(tsn);
         ISSITimer.getTimer().schedule(firstPacketTimeoutTask,
               TimerValues.TFIRSTPACKETTIME);

         P25Payload p25Payload = mmfSession.createPttTransmitGrant(tsn);
         mmfSession.sendPttPacket(p25Payload);

      } catch (Exception e) {
         logger.error("error sending a grant ", e);
      }
   }

   /**
    * Send DENY response.
    */
   private void sendDeny(int tsn) {

      try {

         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug(this + ":\n\tMMF RX sending PTT TRANSMIT DENY");

         // Deny the spurt request
         if (getCurrentState(tsn) == MmfRxState.ARBITRATE)
            updateRxState(tsn,MmfRxTransition.DENY_DECISION);

         else if (getCurrentState(tsn) == MmfRxState.WAIT)
            updateRxState(tsn,MmfRxTransition.DENY_TRIGGER);

         P25Payload p25Payload = mmfSession.createPttTransmitDeny(tsn);
         mmfSession.sendPttPacket(p25Payload);

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Send WAIT response.
    * 
    * @param transition
    *            The current transition.
    */
   private void sendWait(MmfRxTransition transition, int tsn) {

      try {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug(this + ":\n\tMMF RX sending PTT TRANSMIT WAIT");

         updateRxState(tsn,MmfRxTransition.WAIT_DECISION);
         P25Payload p25Payload = mmfSession.createPttWait(tsn);
         mmfSession.sendPttPacket(p25Payload);

         if (waitTask == null) {
            // We don't want multiple wait tasks
            waitTask = new WaitTask(transition, tsn);
            ISSITimer.getTimer().schedule(waitTask, waitDuration);
         }

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
   protected boolean isPacketTypeHandled(PacketType packetType) {
      return handledPacketTypes.contains(packetType);
   }

   // /**
   // * Add an audio listener.
   // */
   // public void addAudioListener(AudioListener audioListener) {
   //
   // this.audioListener = audioListener;
   //
   // }

   /**
    * Add a control listener.
    */
   public void addListener(MmfRxListener mmfRxListener) {
      this.mmfRxListener = mmfRxListener;
   }

   /**
    * log a stack trace. This helps to look at the stack frame.
    */
   private void logStackTrace() {
      if (logger.isDebugEnabled()) {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         StackTraceElement[] ste = new Exception().getStackTrace();
         // Skip the log writer frame and log all the other stack frames.
         for (int i = 1; i < ste.length; i++) {
            String callFrame = "[" + ste[i].getFileName() + ":"
                  + ste[i].getLineNumber() + "]";
            pw.print(callFrame);
         }
         pw.close();
         String stackTrace = sw.getBuffer().toString();
         logger.debug(stackTrace);
      }
   }

   /**
    * Update the current state.
    * 
    * @param rxTransition
    *            The current transition.
    * @throws IllegalStateException
    */
   private void updateRxState( int tsn, MmfRxTransition rxTransition)
         throws IllegalStateException {

      this.setCurrentState(tsn, (MmfRxState)PttPath.getNextState(this.getCurrentState(tsn), rxTransition));

   }

   /**
    * This class implements a WAIT state. After the wait timeout, this class
    * will invoke either a grantInd() or denyInd().
    */
   class WaitTask extends TimerTask {

      // Default behavior after wait
      private MmfRxTransition transition = null;
      private int tsn;

      public WaitTask(MmfRxTransition mmfRxTransition, int tsn) {
         transition = mmfRxTransition;
         this.tsn = tsn;
      }

      public void run() {
         // We have reached the wait timeout and now invoke
         // either a GRANT or DENY
         if (transition != null) {

            if (transition == MmfRxTransition.GRANT_DECISION) {
               sendGrant(tsn);
               waitTask.cancel();
            } else if (transition == MmfRxTransition.DENY_DECISION) {
               sendDeny(tsn);
               waitTask.cancel();
            }
         } else {
            if (logger.isDebugEnabled())
               logger.debug(this + ":\n\tMMF RX STATE: " + currentState);
         }
      }
   }

   /**
    * This class implements the timeout for receiving audio. Note that there
    * are a number of specification issues with this class.
    * TIA-109.BACA Section 7.6.2
    */
   class FirstPacketTimeoutTask extends TimerTask {

      private int tsn;
      public FirstPacketTimeoutTask ( int tsn ) {
         this.tsn = tsn;
      }
      public void run() {
         // If we reach this point, then we have reached the timeout
         // for waiting for the first audio packet, just move to
         // DONE state.
         MmfReceiver.this.setCurrentState(tsn,MmfRxState.DONE);

         if (logger.isDebugEnabled())
            logger.debug(this
                  + ":\n\tMMF RX reached FIRST PACKET TIMEOUT. "
                  + "Moving to DONE state.");

         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tMMF RX entering STATE: "
                  + currentState);
         mmfRxListener.audioTimeout();
      }
   }

   /**
    * This class implements the timeout for receiving audio.
    */
   class AudioTimeoutTask extends TimerTask {
      private int tsn;

      public AudioTimeoutTask(int tsn) {
         this.tsn  = tsn;
      }

      public void run() {

         // Check if we are in a MUTED state. If so, we can simply
         // cancel this task.
         if (mmfSession.muted) {
            this.cancel();
            return;
         }

         // If we reach this point, then we have reached the timeout
         // for waiting for audio packets, so just move to DONE state.
         MmfReceiver.this.setCurrentState(tsn,MmfRxState.DONE);

         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tMMF RX reached AUDIO TIMEOUT");

         if (logger.isDebugEnabled())
            logger.debug(this + ":\n\tMMF RX entering STATE: "
                  + currentState);
         mmfRxListener.audioTimeout();
      }
   }
}
