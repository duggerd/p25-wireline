//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.p25payload.*;
import gov.nist.rtp.RtpSession;

import java.util.*;
import org.apache.log4j.Logger;

/**
 * This class handles incoming HEARTBEAT and other PTT packets related to
 * mute/unmute functions. This class also sends PTT MUTE/UNMUTE packets.
 */
public class HeartbeatReceiver {

   private static Logger logger = Logger.getLogger(HeartbeatReceiver.class);

   /** The owning PTT session. */
   private PttSessionInterface pttSession;
   
   /** The mute timer task. */
   private ReceiveHeartbeatsTimeoutTask receiveHeartbeatsTimeoutTask = null;
   private HeartbeatListener heartbeatListener;

   /** The collection of packet types this class handles. */
   private static HashSet<PacketType> handledPacketTypes = null;

   static {
      handledPacketTypes = new HashSet<PacketType>();
      handledPacketTypes.add(PacketType.HEARTBEAT);
      handledPacketTypes.add(PacketType.HEARTBEAT_QUERY);
   }

   /**
    * Construct a connection hearbeat receiver (this is not the same as a MUTE
    * transmission heartbeat receiver).
    * 
    * @param pttSession
    *            The owning PTT session.
    */
   public HeartbeatReceiver(PttSessionInterface pttSession) {

      this.pttSession = pttSession;
      //logger.debug(getTarget() + " creating new Heartbeat receiver.");

      // Set timeout for receiving connection maintenance heartbeats
      // See TIA-109.BACA Section 7.6.10
      receiveHeartbeatsTimeoutTask = new ReceiveHeartbeatsTimeoutTask();

      long delay = TimerValues.THEARTBEAT * 4;
      ISSITimer.getTimer().schedule(receiveHeartbeatsTimeoutTask, delay);
      logger.debug(getTarget() + " schedule HEARTBEAT TimeoutTask: delay="+delay);
   }

   private String getTarget() {
      RtpSession rtpSession = pttSession.getRtpSession();
      String remoteHost = rtpSession.getRemoteIpAddress() + ":" +
                          rtpSession.getRemoteRtpRecvPort();
      String myHost = rtpSession.getMyIpAddress().getHostAddress() + ":" +
                      rtpSession.getMyRtpRecvPort();

      String target = pttSession.getSessionType() + ":" + 
                      myHost + "|" + remoteHost;
      return target;
   }

   /**
    * Handle incoming PTT packets especially HEARTBEAT packets. Check all
    * packets for correct M-bit.
    * 
    * @param p25Payload
    *            The incoming PTT packet.
    */
   public void handlePttPacket(P25Payload p25Payload) {
      
      ISSIPacketType issiPacketType = p25Payload.getISSIPacketType();
      PacketType packetType = issiPacketType.getPacketType();

      if (packetType == PacketType.HEARTBEAT) {

         if (heartbeatListener != null) {
            heartbeatListener.receivedHeartbeat(issiPacketType.getTransmissionSequenceNumber());
	 } else { 
            logger.debug(getTarget() + " No heartbeat listener.");
         }

         if (receiveHeartbeatsTimeoutTask != null) {

            // Reset the timer. TODO: Note that in TIA-109.BACA Section
            // 7.6.10 requires us to reset using the value contained in
            // the Interval field of the message. Here, however, we
            // simply reset to the default.
            //
            receiveHeartbeatsTimeoutTask.cancel();
            receiveHeartbeatsTimeoutTask = new ReceiveHeartbeatsTimeoutTask();

	    //TODO: need to copy interval back to THEARTBEAT ?
	    //TimerValues.THEARTBEAT = issiPacketType.getInterval() * 1000L;
	    //long delay = TimerValues.THEARTBEAT * 4;
	    //
	    long delay = issiPacketType.getInterval() * 4 * 1000L;
            ISSITimer.getTimer().schedule(receiveHeartbeatsTimeoutTask, delay);

            if (logger.isDebugEnabled()) {
               logger.debug(getTarget() + " received HEARTBEAT. interval="+issiPacketType.getInterval());
               logger.debug(getTarget() + " received HEARTBEAT. Resetting heartbeat timeout: "+delay);
            }
         }

      } else if (packetType == PacketType.HEARTBEAT_QUERY) {

         if (heartbeatListener != null) {
            heartbeatListener.receivedHeartbeatQuery(pttSession,
                  issiPacketType.getTransmissionSequenceNumber());
	 } else { 
            logger.debug(getTarget() + " No heartbeat query listener.");
         }

         // Reset the timer. TODO: Note that in TIA-109.BACA Section
         // 7.6.10 requires us to reset using the value contained in
         // the Interval field of the message. Here, however, we
         // simply reset to the default.
         //
         receiveHeartbeatsTimeoutTask.cancel();
         receiveHeartbeatsTimeoutTask = new ReceiveHeartbeatsTimeoutTask();

	 //TODO: need to copy interval back to THEARTBEAT ?
	 //TimerValues.THEARTBEAT = issiPacketType.getInterval() * 1000L;
	 //long delay = TimerValues.THEARTBEAT * 4;
	 //
	 long delay = issiPacketType.getInterval() * 4 * 1000L;
         ISSITimer.getTimer().schedule(receiveHeartbeatsTimeoutTask, delay);

         if (logger.isDebugEnabled()) {
            logger.debug(getTarget() + " myRtpRecvPort=" + pttSession.getMyRtpRecvPort());
            logger.debug(getTarget() + " received HEARTBEAT QUERY. delay="+delay);
         }

         // Now respond with a HEARTBEAT
         if (pttSession.getHeartbeatTransmitter() != null) {
            logger.debug(getTarget() + " received HEARTBEAT QUERY. Sending heartbeat");
            pttSession.getHeartbeatTransmitter().sendHeartbeat();
         } else {
            logger.debug(getTarget() + " Cannot send heartbeat, heartbeat transmitter is null ");
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
   public boolean isPacketTypeHandled(PacketType packetType) {
      return handledPacketTypes.contains(packetType);
   }
   
   /**
    * @param heartbeatTimeoutListener
    *            the heartbeatTimeoutListener to set
    */
   public void setHeartbeatListener(HeartbeatListener heartbeatListener) {
      logger.debug(getTarget() + " set HEARTBEAT listener.");
      this.heartbeatListener = heartbeatListener;
   }

   public void stop() {
      if ( receiveHeartbeatsTimeoutTask != null) {
         this.receiveHeartbeatsTimeoutTask.cancel();
         this.receiveHeartbeatsTimeoutTask = null;
         logger.debug(getTarget() + " stop HEARTBEAT timeout task.");
      }
   }

   /**
    * This class sets the timeout for receiving CONNECTION MAINTENANCE
    * heartbeats. This class does NOT handle MUTE TRANSMISSION heartbeats.
    * 
    * @see TIA-109.BACA Section 7.6.10
    */
   class ReceiveHeartbeatsTimeoutTask extends TimerTask {

      // If we get here we have timed out waiting for a (connection
      // maintenance) heartbeat.  Note that in Section 7.6.10 of TIA-102.BACA,
      // an RFSS MAY tear down the call segment if it reaches this timeout.
      // Here, however, we simply print a statement that we have reached
      // the timeout and allow other timeouts to terminate the session.
      public void run() {
         if (pttSession.getMyRtpRecvPort() != 0) {
            if (logger.isDebugEnabled()) {
               logger.debug(getTarget() + " REACHED TIMEOUT waiting for HEARTBEAT " + this);
            }
            if (heartbeatListener != null) {
               heartbeatListener.heartbeatTimeout(pttSession);
            }
         }
      }
   }
}
