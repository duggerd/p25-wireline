//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.p25payload.*;
import gov.nist.rtp.RtpPacket;
import gov.nist.rtp.RtpSession;

import java.util.*;
import org.apache.log4j.Logger;

/**
 * This class handles sending connection maintenance heartbeats.
 * This class does NOT handle MUTE-related heartbeats.
 */
public class HeartbeatTransmitter {

   protected static Logger logger = Logger.getLogger(HeartbeatTransmitter.class);

   /** The mute transmission heartbeat task. */
   private SendHeartbeatsTask sendHeartbeatsTask = null;

   /**
    * Used to test heartbeat receive timeout by blocking outgoing heartbeat
    * transmission.
    */
   private boolean blockOutgoingHeartbeatTransmission = false;
   private String sessionType;
   private LinkType linkType;
   private RtpSession rtpSession;
   private RfssConfig owningRfssConfig;
   private PttSessionInterface pttSession;
   private String target;

   /**
    * Construct a connection hearbeat transmitter (this is not the same as a
    * MUTE transmission heartbeat transmitter).
    * 
    * @param pttSession
    *            The owning PTT session.
    */
   HeartbeatTransmitter(PttSessionInterface pttSession) {
      this.pttSession = pttSession;
      this.sessionType = pttSession.getSessionType();
      this.linkType = pttSession.getLinkType();
      this.rtpSession = pttSession.getRtpSession();
      this.owningRfssConfig = pttSession.getOwningRfss();
      this.target = buildTarget();
      logger.debug("Creating new heartbeat transmitter "+ getTarget());
   }

   private String buildTarget() {
      String remoteHost = rtpSession.getRemoteIpAddress() + ":" +
                          rtpSession.getRemoteRtpRecvPort();
      String myHost = rtpSession.getMyIpAddress().getHostAddress() + ":" +
                      rtpSession.getMyRtpRecvPort();
      /**
      String target = sessionType + ":" + linkType +":" +
                      owningRfssConfig.getRfssName() +":" +
                      myHost +":" + remoteHost;
       **/
      String target = sessionType + ":" +
                      owningRfssConfig.getRfssName() +":" +
                      remoteHost;
      return target;
   }
   private String getTarget() {
      //return target;
      return buildTarget();
   }
   private void sendPttPacket(P25Payload p25Payload) throws Exception {
      
      RtpPacket rtpPacket = new RtpPacket();

      // The NIST RTP stack already define default values for some of these,
      // but we set each here for testing purposes.
      rtpPacket.setV(2);
      rtpPacket.setP(0);
      rtpPacket.setX(0);
      rtpPacket.setCC(0);
      rtpPacket.setM(0);
      rtpPacket.setPT(100);
      rtpPacket.setTS(0);
      rtpPacket.setSSRC(linkType.getValue());

      byte[] payload = p25Payload.getBytes();
      rtpPacket.setPayload(payload,payload.length);

      if (rtpSession.getRemoteRtpRecvPort() != -1) {
         if ( logger.isDebugEnabled()) {
            logger.debug("HeartbeatTransmitter: sendPttPacket: " +
                  getTarget() + "\n" +
                  p25Payload.getISSIPacketType());
//logger.debug("ZMARKER(41a): HeartbeatTransmitter: rtpPacket=" + rtpPacket);
         }
         rtpSession.sendRtpPacket(rtpPacket);

         // Log the outgoing packet if we are sending to another host
         // (other than ourselves).
         //if (!rtpSession.getRemoteIpAddress().equals(
         //      rtpSession.getMyIpAddress().getHostAddress())) {
//logger.debug("ZMARKER(11): rtpPacket="+rtpPacket.toString());

            //=== donot match heartbeat packet
            owningRfssConfig.getRFSS().capturePttPacket(rtpPacket, 
                  p25Payload, true, pttSession, false);
         //}
      } else {
         if ( logger.isDebugEnabled()) {
            logger.debug("HeartbeatTransmitter: cannot sendPttPacket: " +
                  getTarget() + "\n" +
                  p25Payload.getISSIPacketType());
         }
      }
   }

   /**
    * This method sends a heartbeat.
    * 
    * @see TIA Specification Section 7.6.10
    */
   public void sendHeartbeat() {

      P25Payload p25Payload = PttSession.createHeartbeatConnection();
      try {
	 String dateString = new Date().toString();
         if (!blockOutgoingHeartbeatTransmission) {
            if (logger.isDebugEnabled()) {
               logger.debug(getTarget() + " sending HEARTBEAT TSN=0 "+dateString);
            }
	    // SMF ?
            this.sendPttPacket(p25Payload);
         } else {
            if (logger.isDebugEnabled()) {
               logger.debug(getTarget() + " blocked sending HEARTBEAT TSN=0 "+dateString);
            }
         }
      } catch (Exception e) {
         logger.debug(getTarget() + " ex1: "+e.toString());
         e.printStackTrace();
      }
   }

   /**
    * This method sends a heartbeat query.
    * 
    * @see TIA Specification Section 7.6.10
    */
   public void sendHeartbeatQuery() {

      P25Payload p25Payload = PttSession.createHeartbeatQuery();
      try {
	 String dateString = new Date().toString();
         if (!blockOutgoingHeartbeatTransmission) {
            if (logger.isDebugEnabled()) {
               logger.debug(getTarget() + " sending HEARTBEAT QUERY TSN=0 "+dateString);
            }
            sendPttPacket(p25Payload);
         } else {
            if (logger.isDebugEnabled()) {
               logger.debug(getTarget() + " blocked sending HEARTBEAT QUERY TSN=0 "+dateString);
            }
         }
      } catch (Exception e) {
         logger.debug(getTarget() + " ex2: "+e.toString());
         e.printStackTrace();
      }
   }

   /**
    * This method is used to test MMF Rx audio timeout by blocking transmission
    * of audio from this SMF Tx.
    */
   public void blockOutgoingHearbeats() {
      blockOutgoingHeartbeatTransmission = true;
   }

   /**
    * Start the transmitter. This has to be a separate method because
    * in call processing, you cannot send hearbeat until the ACK is seen.
    * The transmitter is created but not started until the ACK.
    */
   public void start() {
      logger.debug(getTarget() + " start()...sendHeartbeatsTask="+sendHeartbeatsTask);

      // Start sending heartbeats
      if (sendHeartbeatsTask != null) {
         logger.debug(getTarget() + " start()...sendHeartbeatsTask NOT NULL");
         return;
      }

      logger.debug(getTarget() + " start()...new sendHeartbeatsTask...");
      sendHeartbeatsTask = new SendHeartbeatsTask();

      // 14.3.x and 14.4.x
      if ("MMF".equalsIgnoreCase(sessionType)) {
         logger.debug(sessionType + " start()...MMF-sendHeartbeatQuery() ...");
         sendHeartbeatQuery();
      }

      long delay = TimerValues.THEARTBEAT;
      logger.debug(getTarget() + " start()...delay="+delay);
      logger.debug(getTarget() + " start()...THEARTBEAT="+TimerValues.THEARTBEAT);
      ISSITimer.getTimer().schedule(sendHeartbeatsTask, delay, TimerValues.THEARTBEAT);

   }

   /**
    * Stop this and exit all timers.
    */
   public void stop() {
      if (sendHeartbeatsTask != null) {
         sendHeartbeatsTask.cancel();
         sendHeartbeatsTask = null;
      }
   }

   /**
    * This class sends CONNECTION MAINTENANCE heartbeats with TSN=0. This class
    * does NOT handle MUTE TRANSMISSION heartbeats.
    * 
    * @see TIA-109.BACA Section 7.5.1
    */
   class SendHeartbeatsTask extends TimerTask {
      public void run() {
         try {
            String target = getTarget();
	    String dateString = new Date().toString();
            if (!blockOutgoingHeartbeatTransmission) {
               if (rtpSession.getRemoteRtpRecvPort() > 0) {
                  // MMF
                  P25Payload p25Payload = PttSession.createHeartbeatConnection();
                  HeartbeatTransmitter.this.sendPttPacket(p25Payload);

                  logger.debug(target + " sending HEARTBEAT Task TSN=0 "+dateString);
               } else {
                  logger.debug(target + " Not sending heartbeat, port is not set "+dateString);
               }
            } else {
               if (logger.isDebugEnabled()) {
                  logger.debug(target + " blocked sending HEARTBEAT TSN=0 "+dateString);
               }
            }
         } catch (Exception e) {
            logger.debug(target + " ex3: "+e.toString());
            e.printStackTrace();
         }
      }
   }
}
