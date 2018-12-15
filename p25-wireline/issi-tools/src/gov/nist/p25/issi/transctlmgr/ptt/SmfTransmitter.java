//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.p25payload.IMBEVoiceBlock;
import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.p25.issi.p25payload.PacketType;
import gov.nist.p25.issi.p25payload.TransmitPriorityType;
import gov.nist.rtp.RtpException;
import java.io.IOException;
import java.util.List;
import java.util.HashSet;
import java.util.TimerTask;
import org.apache.log4j.Logger;

/**
 * This class defines a state machine driver for an SMF transmitter. Note: There
 * is a race condition when implementing the TIA spec that occurs if we attempt
 * to update the state after sending a packet. The problem is that a packet may
 * be sent and a response received before the state is updated. To resolve this
 * problem, we update the state before sending the packet. Note that the SMF
 * deals with just one TSN whereas the MMf deals with multiple TSNs.
 */
public class SmfTransmitter {

   private static Logger logger = Logger.getLogger(SmfTransmitter.class);

   /**
    * Number of IMBE blocks per PROGRESS packet. This must be within the range
    * as specified in P25Payload.
    */
   public int NUM_IMBE_BLOCKS_PER_PACKET = 3;

   /**
    * The delay between IMBE voice blocks in miliseconds. Note that a packet
    * may have up to NUM_IMBE_BLOCKS_PER_PACKET.
    */
   private static final int AUDIO_TRANSMISSION_RATE = 20;


   /** Application listener for GRANT, DENY, and WAIT responses. */
   private SmfTxListener smfTxListener = null;

   /**
    * Indicates that this transmitter has already started. This is used
    * primarily for the PTT test suite in package ptt_test.
    */
   private boolean transmitterStarted = false;

   /** My session. */
   private SmfSession smfSession = null;

   /** Starting state. */
   private SmfTxState currentState = SmfTxState.BEGIN;

   /**
    * Used to test MMF Rx audio timeout by blocking outgoing SMF Tx
    * transmission.
    */
   private boolean blockOutgoingTransmission = false;

   /** Request task. */
   private RequestTask requestTask = null;

   /** Wait task. */
   private WaitTask waitTask = null;

   /** Array of IMBE voice blocks for transmission. */
   protected List<IMBEVoiceBlock> imbeVoiceBlocks = null;

   /** Collection of packet types this class handles. */
   private static HashSet<PacketType> handledPacketTypes = null;

   /** Transmits IMBE audio. */
   private Thread audioSourceTransmitterThread = null;


   /** Application-dependent and default transition behavior. */
   private SmfTxTransition requestTransition = null;

   /** Defines local policy transitions. */
   private SmfTxTransition localPolicyTransition = SmfTxTransition.FAIL_ON_TIMEOUT;

   /** Defines policy for handling wait. */
   private SmfTxTransition waitTransition = null;

   /** Defines policy for handling denials. */
   private SmfTxTransition denyTransition = null;

   /** Local policy flag -- added mranga */
   private LocalPolicy localPolicy = LocalPolicy.SELF_GRANT;

   private int unitId;

   private int numberOfBlocksToSend;

   private int systemId;

   static {
      // Set Tx transitions from BEGIN state
      PttPath.add(new SmfTxPath(SmfTxState.BEGIN, SmfTxTransition.TX_TRIGGER,
            SmfTxState.REQUESTING));

      PttPath.add(new SmfTxPath(SmfTxState.BEGIN,
            SmfTxTransition.RESPONSE_FOR_INACTIVE_TSN,
            SmfTxState.TERMINATED));

      PttPath.add(new SmfTxPath(SmfTxState.BEGIN, SmfTxTransition.SELF_GRANT,
            SmfTxState.TRANSMITTING));

      // Set Tx transitions from REQUESTING state
      PttPath.add(new SmfTxPath(SmfTxState.REQUESTING,
            SmfTxTransition.TX_TRIGGER, SmfTxState.REQUESTING));

      PttPath.add(new SmfTxPath(SmfTxState.REQUESTING,
            SmfTxTransition.SELF_GRANT, SmfTxState.TRANSMITTING));

      PttPath.add(new SmfTxPath(SmfTxState.REQUESTING, SmfTxTransition.WAIT,
            SmfTxState.WAITING));

      PttPath.add(new SmfTxPath(SmfTxState.REQUESTING,
            SmfTxTransition.END_TRIGGER, SmfTxState.TERMINATED));

      PttPath.add(new SmfTxPath(SmfTxState.REQUESTING,
            SmfTxTransition.DENIED, SmfTxState.TERMINATED));

      PttPath.add(new SmfTxPath(SmfTxState.REQUESTING,
            SmfTxTransition.RECEIVED_GRANT, SmfTxState.TRANSMITTING));

      PttPath.add(new SmfTxPath(SmfTxState.REQUESTING,
            SmfTxTransition.REQUEST_TIMEOUT, SmfTxState.LOCAL_POLICY));

      // Set Tx transitions from LOCAL_POLICY state
      PttPath.add(new SmfTxPath(SmfTxState.LOCAL_POLICY,
            SmfTxTransition.SELF_GRANT, SmfTxState.TRANSMITTING));

      PttPath.add(new SmfTxPath(SmfTxState.LOCAL_POLICY,
            SmfTxTransition.FAIL_ON_TIMEOUT, SmfTxState.TERMINATED));

      // Set Tx transitions from WAITING state
      PttPath.add(new SmfTxPath(SmfTxState.WAITING,
            SmfTxTransition.WAIT_TIMEOUT, SmfTxState.REQUESTING));

      PttPath.add(new SmfTxPath(SmfTxState.WAITING,
            SmfTxTransition.START_TRANSMIT, SmfTxState.TRANSMITTING));

      PttPath.add(new SmfTxPath(SmfTxState.WAITING,
            SmfTxTransition.SEND_CHANGE_IN_LOSING_STATE,
            SmfTxState.WAITING));

      PttPath.add(new SmfTxPath(SmfTxState.WAITING, SmfTxTransition.DENIED,
            SmfTxState.TERMINATED));

      PttPath.add(new SmfTxPath(SmfTxState.WAITING,
            SmfTxTransition.END_TRIGGER, SmfTxState.TERMINATED));

      PttPath.add(new SmfTxPath(SmfTxState.WAITING,
            SmfTxTransition.SELF_GRANT, SmfTxState.TRANSMITTING));

      PttPath.add(new SmfTxPath(SmfTxState.WAITING,
            SmfTxTransition.RECEIVED_GRANT, SmfTxState.TRANSMITTING));

      // Set Tx transitions from TRANSMITTING state
      PttPath.add(new SmfTxPath(SmfTxState.TRANSMITTING,
            SmfTxTransition.SEND_AUDIO, SmfTxState.TRANSMITTING));

      PttPath.add(new SmfTxPath(SmfTxState.TRANSMITTING,
            SmfTxTransition.SEND_CHANGE_IN_LOSING_STATE,
            SmfTxState.TRANSMITTING)); // Correct?

      PttPath.add(new SmfTxPath(SmfTxState.TRANSMITTING,
            SmfTxTransition.END_TRIGGER, SmfTxState.TERMINATED));

      // Handled packet types
      handledPacketTypes = new HashSet<PacketType>();
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_GRANT);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_DENY);
      handledPacketTypes.add(PacketType.PTT_TRANSMIT_WAIT);
   }

   // Constructors
   SmfTransmitter(SmfSession smfSession) {
      this.smfSession = smfSession;
   }

   /***************************************************************************
    * Public API methods. Note that some of these methods are used solely for
    * test purposes.
    **************************************************************************/
   /**
    * Set the control listener.
    * 
    * @param smfControlListener
    *            The control listener to set.
    */
   public void addListener(SmfTxListener smfTxListener) {
      this.smfTxListener = smfTxListener;
   }

   /**
    * Start a spurt request from the calling serving RFSS. Here, the
    * application passes the source of the voice transmission and stores it
    * locally. This method is not intended for use by SMFs residing on
    * intermediate RFSSs.
    * 
    * @param imbeVoiceBlocks
    *            An array list of IMBE voice blocks.
    */

   public void sendSpurtRequest(List<IMBEVoiceBlock> imbeVoiceBlocks,
         int systemId, int unitId, int nblocks) throws Exception {
      if (transmitterStarted) {
         if (logger.isDebugEnabled())
            logger.warn(this + ":\n\tSMF TX already started.  Ignoring "
                  + "command for spurtRqst");
         return;
      }

      if (this.smfSession.getMultiplexer() != null) {
         PttSessionMultiplexer multiplexer = this.smfSession.getMultiplexer();
         SmfSession currentTransmitter = multiplexer.getCurrentTransmitter();

         if (currentTransmitter != null) {
            if (this.smfSession.compareTo(currentTransmitter) == -1
                  || this.smfSession.compareTo(currentTransmitter) == 0) {
               throw new PttException( "Current session in this Rfss has a higher "
                           + "priority -- cannot send ");
            } else {
               // We are higher than the current transmitter. Pre-empt him
               // ruthlessly.
               // Mute the current transmitter. The MMF will transmit mute
               // heartbeat
               // which will mute the transmission from the SMF.
               multiplexer.pushTransmitter(this.smfSession);
            }
         } else {
            multiplexer.pushTransmitter(this.smfSession);
         }
      }

      transmitterStarted = true;
      this.imbeVoiceBlocks = imbeVoiceBlocks;
      this.numberOfBlocksToSend = nblocks;

      if (logger.isDebugEnabled())
         logger.debug(this + ":\n\tSMF TX STATE: " + currentState);
      this.systemId = systemId;
      this.unitId = unitId;
      requestTask = new RequestTask(systemId,unitId);
      ISSITimer.getTimer().schedule(requestTask, 0, TimerValues.TREQUEST);
   }

   public void sendSpurtRequestWithVoice(
         List<IMBEVoiceBlock> imbeVoiceBlocks, int systemId, int unitId)
         throws Exception {

      if (transmitterStarted) {
         if (logger.isDebugEnabled())
            logger.warn(this + ":\n\tSMF TX already started.  Ignoring "
                  + "command for spurtRqst");
         return;
      }
      transmitterStarted = true;
      this.imbeVoiceBlocks = imbeVoiceBlocks;

      if (logger.isDebugEnabled())
         logger.debug(this + ":\n\tSMF TX STATE: " + currentState);

      this.unitId = unitId;
      this.systemId = systemId;
      requestTask = new RequestTask(unitId, imbeVoiceBlocks);
      ISSITimer.getTimer().schedule(requestTask, 0, TimerValues.TREQUEST);
   }

   /**
    * "Forward" a spurt request (without IMBE voice data). This is used by an
    * intermediate node to forward a spurt request. Note that the original
    * packet is not forwarded, only the packet type (Request).
    * 
    * @throws IllegalStateException
    */
   public void forwardPttRequest(int systemId, int unitId) throws IllegalStateException {
      if (transmitterStarted) {
         if (logger.isDebugEnabled())
            logger.warn(this + ":\n\tSMF TX already started.  Ignoring "
                  + "command for spurtRqst");
         return;
      }
      transmitterStarted = true;
      this.systemId = systemId;
      this.unitId = unitId;
      requestTask = new RequestTask(systemId, unitId);
      ISSITimer.getTimer().schedule(requestTask, 0, TimerValues.TREQUEST);
   }

   /**
    * "Forward" a PTT progress packet. This is used by an intermediate node to
    * forward a progress packet. Note that only the payload (IMBE voice blocks)
    * are forwarded in new PTT progress packets.
    * 
    * @param imbeVoiceBlocks
    *            The IMBE voice blocks to send.
    * @throws IllegalStateException
    */
   public void forwardPttProgress(IMBEVoiceBlock[] imbeVoiceBlocks,
         int fromSystemId, int fromUnitId, int tsn, 
         TransmitPriorityType transmitPriorityType, int transmitPriorityLevel)
         throws IllegalStateException {

      try {
          this.unitId = fromUnitId;
          this.systemId = fromSystemId;
         if (this.currentState == SmfTxState.TRANSMITTING) {

            P25Payload p25Payload = smfSession.createPttTransmitProgress(
                  fromSystemId,fromUnitId, transmitPriorityType,
                  transmitPriorityLevel, imbeVoiceBlocks);

            // p25Payload.setIMBEVoiceBlockArray(imbeVoiceBlocks);
            smfSession.sendPttPacket(p25Payload);

         } else if (this.currentState == SmfTxState.BEGIN
               || this.currentState == SmfTxState.REQUESTING
               && localPolicy == LocalPolicy.SELF_GRANT) {
            // I added this -- mranga
            this.updateTxState(SmfTxTransition.SELF_GRANT);
            P25Payload p25Payload = smfSession.createPttTransmitProgress(
                  fromSystemId,fromUnitId,  transmitPriorityType,
                  transmitPriorityLevel,imbeVoiceBlocks);
            smfSession.sendPttPacket(p25Payload);
         } else {
            // I think one should silently drop packet here rather than
            // throw exception.
            throw new IllegalStateException( "Cannot send unless in transmitting state");
         }
      } catch (IOException ex) {
         logger.error("unexpected IO Exception ", ex);
      } catch (RtpException ex) {
         logger.error("Unexpected RTP exception ", ex);
      }
   }

   /**
    * "Forward" a PTT END packet. This is used by an intermediate node to
    * forward a PTT END. Note that the original packet is not forwarded, only
    * the packet type (End).
    * 
    * @throws Exception
    */
   public void forwardPttEnd() throws Exception {
      if (logger.isDebugEnabled())
         logger.debug("forwardPttEnd(): sendSpurtEndNotification(567): SmfTxTransition.END_TRIGGER");
      sendSpurtEndNotification(SmfTxTransition.END_TRIGGER, this.systemId, this.unitId);
   }

   /**
    * Self-grant (force) a transmission without a spurt request due to an
    * unstoppable trigger.
    * 
    * @param imbeVoiceBlocks
    *            The voice block array to transmit.
    */
   public void forceAudio(List<IMBEVoiceBlock> imbeVoiceBlocks,
         int systemId, int unitId, int nblocks) throws Exception {

      if (transmitterStarted) {
         if (logger.isDebugEnabled())
            logger.warn(this + ":\n\tSMF TX already started.  Ignoring "
                  + "command to force audio");
         return;
      }
      if (this.smfSession.getMultiplexer() != null) {
         PttSessionMultiplexer multiplexer = this.smfSession.getMultiplexer();
         SmfSession currentTransmitter = multiplexer.getCurrentTransmitter();

         if (currentTransmitter != null) {
            if (this.smfSession.compareTo(currentTransmitter) == -1
                  || this.smfSession.compareTo(currentTransmitter) == 0) {
               throw new PttException( "Current session in this Rfss has a higher "
                           + "priority -- cannot send ");
            } else {
               // We are higher than the current transmitter. Pre-empt him
               // ruthlessly.
               // Mute the current transmitter. The MMF will transmit mute
               // heartbeat
               // which will mute the transmission from the SMF.
               multiplexer.pushTransmitter(this.smfSession);
            }
         } else {
            multiplexer.pushTransmitter(this.smfSession);
         }
      }

      this.unitId = unitId;
      this.systemId = systemId;
      this.imbeVoiceBlocks = imbeVoiceBlocks;
      this.numberOfBlocksToSend = nblocks;

      if (logger.isDebugEnabled())
         logger.debug(this + ":\n\tSMF TX STATE: " + currentState);

      try {
         this.updateTxState(SmfTxTransition.SELF_GRANT);
         if (imbeVoiceBlocks != null) { // We must be a source SMF
            if (this.audioSourceTransmitterThread == null) {
               this.audioSourceTransmitterThread = new Thread(
                     new AudioSourceTransmitter(systemId,unitId));
               this.audioSourceTransmitterThread.start();
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /***************************************************************************
    * ISSI Control Function Emulation Settings.
    **************************************************************************/
   public SmfTxState getCurrentState() {
      return this.currentState;
   }

   /**
    * Instruct the transmitter to invoke END TRIGGER after sending a single PTT
    * TRANSMIT REQUEST.
    * 
    * @throws PttException
    */
   public void setRqstEndTrigger() throws PttException {

      if (transmitterStarted) {
         throw new PttException("Cannot set REQUEST END TRIGGER after "
               + "transmitter has started. Make sure that "
               + "SmfTransmitter.setRqstEndTrigger() is "
               + "set before starting SmfTransmitter.spurtRqst() or"
               + "SmfTransmitter.forceAudio().");
      } else {
         requestTransition = SmfTxTransition.END_TRIGGER;
      }
   }

   /**
    * Instruct the transmitter to invoke UNSTOPPABLE TX TRIGGER after sending a
    * single PTT TRANSMIT REQUEST.
    * 
    * @throws PttException
    */
   public void setRqstUnstoppableTrigger() throws PttException {
      if (transmitterStarted) {
         throw new PttException("Cannot set REQUEST END TRIGGER after "
               + "transmitter has started. Make sure that "
               + "SmfTransmitter.setRqstEndTrigger() is "
               + "set before starting SmfTransmitter.spurtRqst() or"
               + "SmfTransmitter.forceAudio().");
      } else {
         requestTransition = SmfTxTransition.SELF_GRANT;
      }
   }

   /**
    * Instruct the transmitter to invoke FAIL ON TIMEOUT after timing out from
    * the REQUESTING state.
    * 
    * @throws PttException
    */
   public void setFailOnRqstTimeout() throws PttException {
      if (transmitterStarted) {
         throw new PttException("Cannot set FAIL ON TIMEOUT after "
               + "transmitter has started. Make sure that "
               + "SmfTransmitter.setRqstFailOnTimeout() is "
               + "set before starting SmfTransmitter.spurtRqst() or"
               + "SmfTransmitter.forceAudio().");
      } else {
         localPolicyTransition = SmfTxTransition.FAIL_ON_TIMEOUT;
      }
   }

   /**
    * Instruct the transmitter to invoke TRANSMIT ON TIMEOUT after timing out
    * from the REQUESTING state.
    * 
    * @throws PttException
    */
   public void setTransmitOnRqstTimeout() throws PttException {
      if (transmitterStarted) {
         throw new PttException("Cannot set TRANSMIT ON TIMEOUT after "
               + "transmitter has started. Make sure that "
               + "SmfTransmitter.setRqstTransmitOnTimeout() is "
               + "set before starting SmfTransmitter.spurtRqst() or"
               + "SmfTransmitter.forceAudio().");

      } else {
         localPolicyTransition = SmfTxTransition.SELF_GRANT;
      }
   }

   /**
    * Instruct the transmitter to invoke TRANSMIT ON TIMEOUT after timing out
    * from the REQUESTING state.
    * 
    * @throws PttException
    */
   public void setWaitUnstoppableTrigger() throws PttException {
      if (transmitterStarted) {
         throw new PttException("Cannot set WAIT UNSTOPPABLE TRIGGER "
               + "after transmitter has started.  Make sure that "
               + "SmfTransmitter.setWaitUnstoppableTrigger() is "
               + "set before starting SmfTransmitter.spurtRqst() or"
               + "SmfTransmitter.forceAudio().");
      } else {
         waitTransition = SmfTxTransition.SELF_GRANT;
      }
   }

   /**
    * Instruct the transmitter to invoke TRANSMIT ON TIMEOUT after timing out
    * from the REQUESTING state.
    * 
    * @throws PttException
    */
   public void setDenyUnstoppableTrigger() throws PttException {
      if (transmitterStarted) {
         throw new PttException("Cannot set WAIT UNSTOPPABLE TRIGGER "
               + "after transmitter has started.  Make sure that "
               + "SmfTransmitter.setWaitUnstoppableTrigger() is "
               + "set before starting SmfTransmitter.spurtRqst() or"
               + "SmfTransmitter.forceAudio().");
      } else {
         denyTransition = SmfTxTransition.SELF_GRANT;
      }
   }

   /**
    * This method is used to test MMF Rx audio timeout by blocking transmission
    * of audio and PTT END from this SMF Tx.
    */
   public void blockOutgoingAudio() throws PttException {
      if (transmitterStarted) {
         throw new PttException("Cannot set AUDIO BLOCK after "
               + "transmitter has started.  Make sure that "
               + "SmfTransmitter.setOutgoingAudioBlock() is "
               + "set before starting SmfTransmitter.spurtRqst() or"
               + "SmfTransmitter.forceAudio().");
      } else {
         blockOutgoingTransmission = true;
      }
   }

   /**
    * Handle incoming PTT packet. Note that the behavior of each case below
    * depends on if this SMF is the source of a transmission or resides on an
    * intermediate RFSS. If the latter, we perform an *ind() method that
    * invokes ISSI Control code to return a GRANT, WAIT, or DENY to the initial
    * requesting RFSS.
    * 
    * @param p25Payload
    *            The incoming PTT packet.
    */
   void handlePttPacket(P25Payload p25Payload) {

      PacketType packetType = p25Payload.getISSIPacketType().getPacketType();
      SmfPacketEvent smfPacketEvent = new SmfPacketEvent(smfSession, p25Payload);

      if (logger.isDebugEnabled())
         logger.debug(this + ":\n\tSMF TX received " + packetType.toString());

      if (currentState == SmfTxState.TERMINATED) {
         logger.debug(this + ":\n\tSMF TX received " + packetType
               + " in the TERMINATED state.  Ignoring.");

      } else if (packetType.equals(PacketType.PTT_TRANSMIT_GRANT)) {
         if (currentState == SmfTxState.REQUESTING) {
            if (requestTask != null)
               requestTask.cancel();
         } else if (currentState == SmfTxState.WAITING) {
            if (waitTask != null)
               waitTask.cancel();
         }

         if (this.currentState != SmfTxState.TRANSMITTING) {
            try {
               this.updateTxState(SmfTxTransition.RECEIVED_GRANT);
            } catch (Exception ex) {
               logger.error("Unexpected exception", ex);
               return;
            }
         }

         // We only transmit audio if we are the source SMF, not
         // if we are an intermediate SMF. If we are a source SMF,
         // imbeVoiceBlocks should NOT be null. If are an intermediate
         // SMF, imbeVoiceBlocks will be null.
         if (imbeVoiceBlocks != null) { // We must be a source SMF
            if (this.audioSourceTransmitterThread == null) {
               this.audioSourceTransmitterThread = new Thread(
                     new AudioSourceTransmitter(systemId,unitId));
               this.audioSourceTransmitterThread.start();
            }
         }

         // Currently, the following is only relevant if we are an SMF on an
         // intermediate RFSS
         if (this.smfTxListener != null)
            this.smfTxListener.receivedPttGrant(smfPacketEvent);

      } else if (packetType.equals(PacketType.PTT_TRANSMIT_DENY)) {

         if (requestTask != null)
            requestTask.cancel();
         if (waitTask != null)
            waitTask.cancel();

         try {
            // Check if the SMF wants to force sending data.
            // If so, ignore the deny and send
            if (denyTransition == SmfTxTransition.SELF_GRANT) {
               try {
                  updateTxState(SmfTxTransition.SELF_GRANT);
                  if (imbeVoiceBlocks != null) { // We must be a source
                     // SMF
                     if (this.audioSourceTransmitterThread == null) {
                        this.audioSourceTransmitterThread = new Thread(
                              new AudioSourceTransmitter(systemId,unitId));
                        this.audioSourceTransmitterThread.start();
                     }
                  }
               } catch (Exception e) {
                  logger.error("Unexpected error updating state");
               }
            } else {
               // In most cases, the SMF will simply send a
               // spurt end notification
               sendSpurtEndNotification(SmfTxTransition.DENIED, this.systemId, this.unitId);
            }

            // Currently, the following is only relevant if we are an SMF on
            // an intermediate RFSS
            if (this.smfTxListener != null)
               this.smfTxListener.receivedPttDeny(smfPacketEvent);

         } catch (Exception e) {
            e.printStackTrace();
         }

      } else if (packetType.equals(PacketType.PTT_TRANSMIT_WAIT)) {

         if (requestTask != null)
            requestTask.cancel();

         waitTask = new WaitTask(systemId,unitId);
         ISSITimer.getTimer().schedule(waitTask, TimerValues.WAITTIMEOUT);

         // Currently, the following is only relevant if we are an SMF on an
         // intermediate RFSS
         if (this.smfTxListener != null)
            this.smfTxListener.receivedPttWait(smfPacketEvent);
      } else {
         if (logger.isDebugEnabled())
            logger.error(this + ":\n\tSMF TX cannot handle packet type: "
                  + packetType);
      }
   }

   /**
    * Send spurt end notification.
    * 
    * @param transition
    *            The current transition.
    */
   private void sendSpurtEndNotification(SmfTxTransition transition, 
         int systemId, int unitId)
      throws IllegalStateException, IOException, RtpException {

      //EHC
      if (logger.isDebugEnabled())
         logger.debug("sendSpurtEndNotification: transition="+transition);

      // First stop MUTE-related heartbeats
      smfSession.muteTransmitter.shutDown();

      if (logger.isDebugEnabled())
         logger.debug(this + ":\n\tSMF MUTE Tx stopping "
               + "MUTE heartbeats.");
      updateTxState(transition);

      if (logger.isDebugEnabled())
         logger.debug(this + ":\n\tSMF TX sending PTT_TRANSMIT_END");

      // Note that PTT ENDs can be sent REND times, which
      // range from 1 to 5 (default 4). Here, we only send a single
      // PTT END.
      P25Payload p25Payload = smfSession.createPttEndForUnitId(systemId, unitId);
      smfSession.sendPttPacket(p25Payload);

      if (requestTask != null)
         requestTask.cancel();
      if (waitTask != null)
         waitTask.cancel();

      // If this session is multiplexed, pop the waiting transmitter
      // if any.
      if (this.smfSession.getMultiplexer() != null) {
         this.smfSession.getMultiplexer().popTransmitter();
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

   /**
    * Update the current state.
    * 
    * @param rxTransition
    *            The current transition.
    * @throws IllegalStateException
    */
   private void updateTxState(SmfTxTransition txTransition)
         throws IllegalStateException {
      this.currentState = (SmfTxState) PttPath.getNextState(currentState,
            txTransition);

      // If the state transitions to terminated we are done.
      if (currentState == SmfTxState.TERMINATED) {
         if (audioSourceTransmitterThread != null) {
            audioSourceTransmitterThread.interrupt();
         }
      }
   }

   /**
    * This class sends periodic requests. Note that PTT requests are sent
    * immediately upon a spurt request and every Trequest=500ms until we reach
    * REQUEST_TIMEOUT=1000ms. Thus, only two PTT requests will be sent before
    * timing out.
    * 
    * @see TIA-109 Section 7
    */
   class RequestTask extends TimerTask {

      int nretries = 2;
      int unitId;
      int systemId;
      private List<IMBEVoiceBlock> localVoiceBlocks = null;

      public RequestTask(int systemId, int unitId) {
         this.unitId = unitId;
         this.systemId = systemId;
      }

      public RequestTask(int unitId, List<IMBEVoiceBlock> imbeVoiceBlocks) {

         this.unitId = unitId;
         localVoiceBlocks = imbeVoiceBlocks;

      }

      public void run() {

         if (nretries > 0) {
            try {
               if (logger.isDebugEnabled())
                  logger.debug(this + ":\n\tSMF TX sending PTT Transmit Request");

               if (currentState == SmfTxState.BEGIN) {
                  updateTxState(SmfTxTransition.TX_TRIGGER);

               } else if (currentState == SmfTxState.REQUESTING) {
                  updateTxState(SmfTxTransition.TX_TRIGGER);

               } else if (currentState == SmfTxState.WAITING) {
                  updateTxState(SmfTxTransition.WAIT_TIMEOUT);
               }

               // Now send PTT REQUEST
               P25Payload p25Payload;
               if (localVoiceBlocks == null) {
                  p25Payload = smfSession.createPttTransmitRequest(systemId, unitId);

               } else {
                  /*
                   * Here, we simply send the same test voice sample in
                   * the payload as this is only used to facilitate
                   * testing of Request packets with voice.
                   */
                  IMBEVoiceBlock[] vbarray = new IMBEVoiceBlock[NUM_IMBE_BLOCKS_PER_PACKET];
                  for (int i = 0; i < NUM_IMBE_BLOCKS_PER_PACKET; i++) {
                     vbarray[i] = imbeVoiceBlocks.get(i);
                  }
                  p25Payload = smfSession.createPttTransmitRequest(
                        systemId, unitId , vbarray);

               }
               smfSession.sendPttPacket(p25Payload);
               if (nretries == 2) {
                  // Send a MUTE heartbeat M=0 after spurt request
                  // smfSession.muteTransmitter.startMuteHeartbeat();
               }

               // Check for configured behavior
               if (requestTransition == SmfTxTransition.END_TRIGGER) {
                  sendSpurtEndNotification(SmfTxTransition.END_TRIGGER, systemId, unitId);
                  // Terminate this task
                  this.cancel();

               } else if (requestTransition == SmfTxTransition.SELF_GRANT) {

                  if (audioSourceTransmitterThread == null) {
                     transmitterStarted = false;
                     forceAudio(imbeVoiceBlocks, this.systemId, this.unitId,
                           numberOfBlocksToSend);
                  }
                  // Terminate this task
                  this.cancel();
               }
            } catch (Exception e) {
               logger.error("An exception occured in processing ", e);
            }
            nretries--;

         } else {

            // If we reach this point, then our request has timed out,
            // so check local policy and proceed

            // Alert listeners that we have timed-out on request
            if (smfTxListener != null)
               smfTxListener.requestTimeout();

            if (currentState == SmfTxState.TERMINATED) {
               this.cancel();
               return;
            }

            try {
               updateTxState(SmfTxTransition.REQUEST_TIMEOUT);
               if (localPolicyTransition == SmfTxTransition.FAIL_ON_TIMEOUT) {

                  try {
                     if (logger.isDebugEnabled())
                        logger.debug(this + ":\n\tSMF TX LOCAL POLICY: FAIL_ON_TIMEOUT");
                     sendSpurtEndNotification(SmfTxTransition.FAIL_ON_TIMEOUT, systemId,unitId);

                  } catch (Exception e) {
                     logger.error("unexepcted error", e);
                  }

               } else if (localPolicyTransition == SmfTxTransition.SELF_GRANT) {

                  if (logger.isDebugEnabled())
                     logger.debug(this + ":\n\tSMF TX LOCAL POLICY: SELF_GRANT");
                  if (SmfTransmitter.this.audioSourceTransmitterThread == null) {
                     transmitterStarted = false;
                     forceAudio(imbeVoiceBlocks, this.systemId, this.unitId,
                           numberOfBlocksToSend);
                  }
               }
               // Terminate this task
               this.cancel();

            } catch (Exception e) {
               logger.error("unexpected exception", e);
            }
         }
      }
   }

   /** This class waits in the WAITING state. */
   class WaitTask extends TimerTask {

      private int unitId;
      private int systemId;

      public WaitTask(int systemId , int unitId) {
         this.unitId = unitId;
         this.systemId = systemId;
         try {
            if (currentState == SmfTxState.REQUESTING) {
               updateTxState(SmfTxTransition.WAIT);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      public void run() {

         // Alert listeners that we have timed-out on wait
         if (smfTxListener != null)
            smfTxListener.waitTimeout();

         // Check if we should execute an unstoppable trigger
         if (waitTransition != null
               && waitTransition == SmfTxTransition.SELF_GRANT) {

            if (logger.isDebugEnabled())
               logger.debug(this + ":\n\tSMF TX LOCAL POLICY: SELF_GRANT");
            try {
               if (audioSourceTransmitterThread == null) {
                  transmitterStarted = false;
                  forceAudio(imbeVoiceBlocks, systemId,unitId,
                        numberOfBlocksToSend);
               }
               // Terminate this task
               this.cancel();
            } catch (Exception ex) {
               logger.debug("unexpected exception -- bailing");
               return;
            }
         } else {
            if (logger.isDebugEnabled())
               logger.debug(this + ":\n\tSMF TX moving from"
                     + " WAIT to REQUEST STATE");

            // We have timed out, so transition back to the request state
            requestTask = new RequestTask(systemId,unitId);

            // Here, we send a PTT REQUEST immediately and thereafter every
            // Trequest=500 miliseconds until we reach REQUEST_TIMEOUT=1000.
            // Thus, we will send only two PTT REQUESTs until we reach the
            // timeout.
            ISSITimer.getTimer().schedule(requestTask, 0,
                  TimerValues.TREQUEST);

         }
         // Stop waiting
         waitTask.cancel();
      }
   }

   /**
    * This class implements an audio sender.
    */
   class AudioSourceTransmitter implements Runnable {

      private int unitId;
      private int systemId;
      private long deadline;
      private long[] timeDiffs = new long[imbeVoiceBlocks.size()
            / NUM_IMBE_BLOCKS_PER_PACKET + 1]; // 3 per packet

      public AudioSourceTransmitter(int systemId, int unitId) {
         transmitterStarted = true;
         this.unitId = unitId;
         this.systemId = systemId;
      }

      public void run() {
         try {
            // set first packet deadline to now, will get incremented after
            // sending packet below
            deadline = System.currentTimeMillis();
            int m = 0;
            for (int i = 0; i < numberOfBlocksToSend; i += NUM_IMBE_BLOCKS_PER_PACKET) {
               int nblocks;
               if (i + NUM_IMBE_BLOCKS_PER_PACKET <= numberOfBlocksToSend)
                  nblocks = NUM_IMBE_BLOCKS_PER_PACKET;
               else
                  nblocks = numberOfBlocksToSend - i;

               IMBEVoiceBlock[] vbarray = new IMBEVoiceBlock[nblocks];
               for (int k = i, j = 0; k < i + nblocks; k++, j++) {
                  vbarray[j] = imbeVoiceBlocks.get(k % imbeVoiceBlocks.size());
               }

               if (smfSession.muteTransmitter.peerMuteState == Mute.MUTED) {
                  if (logger.isDebugEnabled())
                     logger.debug(this
                                 + ":\n\tSMF TX peer (MMF) is muted.  "
                                 + "Not sending PTT PROGRESS. Discarding packet silently");
                  // Note that we need to sleep here because we may be
                  // unmuted and then
                  // need to transmit the rest.
                  try {
                     // set the next packet deadline
                     deadline += nblocks * AUDIO_TRANSMISSION_RATE;

                     // by always sleeping for the same amount of time, the
                     // packets being sent will gradually become later and later
                     // due to the amount of error in the sleep() function.
                     // instead, calculate the exact time between now and the next
                     // packet deadline and sleep for that amount of time.
                     long currentTime = System.currentTimeMillis();
                     long timeDiff = deadline - currentTime;

                     // timeDiffs[m++] = timeDiff;
                     if (timeDiff > 0)
                        Thread.sleep(timeDiff);
                  } catch (Exception ex) {
                     ex.printStackTrace();
                  }

               } else {
                  if (logger.isDebugEnabled())
                     logger.debug(this
                                 + ":\n\tSMF TX sending PTT TRANSMIT PROGRESS");

                  P25Payload p25Payload = smfSession.createPttTransmitProgress(
                        systemId,unitId, vbarray);

                  if (!blockOutgoingTransmission) {
                     smfSession.sendPttPacket(p25Payload);
                     // set the next packet deadline
                     deadline += nblocks * AUDIO_TRANSMISSION_RATE;

                  } else {
                     /*
                      * Uncomment the following if statement to test \
                      * MMF audio timeout (rather than MMF firstpacket
                      * timeout).
                      */
                     // if (i == 0)
                     // smfSession.sendPttPacket(p25Payload);
                     //                     
                     if (logger.isDebugEnabled())
                        logger.debug(this
                              + ":\n\tSMF blocking outgoing "
                              + "audio packet");
                  }
               }

               try {
                  // by always sleeping for the same amount of time, the
                  // packets being sent will gradually become later and later 
                  // due to the amount of error in the sleep() function.
                  // instead, calculate the exact time between now and the next
                  // packet deadline and sleep for that amount of time.
                  long currentTime = System.currentTimeMillis();
                  long timeDiff = deadline - currentTime;

                  if (logger.isTraceEnabled()) {
                     timeDiffs[m++] = timeDiff;
                  }

                  if (timeDiff > 0)
                     Thread.sleep(timeDiff);

               } catch (Exception ex) {
                  ex.printStackTrace();
               }
            }
            if (!blockOutgoingTransmission) {
               sendSpurtEndNotification(SmfTxTransition.END_TRIGGER,systemId,unitId);
            }
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
            transmitterStarted = false;
            audioSourceTransmitterThread = null;
         }

         if (logger.isTraceEnabled()) {
            // Print how bad our jitter is.
            for (int n = 0; n < timeDiffs.length; n++) {
               logger.debug("Audio delay: " + timeDiffs[n]);
            }
         }
      }
   }
}
