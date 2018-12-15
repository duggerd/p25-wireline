//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.p25payload.*;
import gov.nist.rtp.*;
import java.io.*;
//import org.apache.log4j.Logger;

/**
 * This class implements a SMF session.
 */
public class SmfSession extends PttSession {

   //private static Logger logger = Logger.getLogger(SmfSession.class);

   /** The SMF transmitter */
   SmfTransmitter smfTransmitter = null;

   /** The SMF Reciever */
   SmfReceiver smfReceiver = null;
   
   /** Session type for SMF session */
   public static final String SESSION_TYPE = "SMF";

   
   /** The session multiplexer ( for group calls ) **/
   PttSessionMultiplexer multiplexer = null;

   /**
    * Construct an SMF session.
    */
   SmfSession(RtpSession rtpSession, LinkType linkType, PttManager manager ) throws RtpException, IOException {

      super(rtpSession, linkType,manager);
      if (linkType != LinkType.GROUP_SERVING) {
         
         rtpSession.addRtpListener(this);
      }

      transmitPriorityType = TransmitPriorityType.NORMAL;
      transmitPriorityLevel = 0;
      smfTransmitter = new SmfTransmitter(this);
      smfReceiver = new SmfReceiver(this);

      // Start mute and heartbeat components
      startMuteComponents();
      if ( linkType != LinkType.GROUP_SERVING)
         this.startHeartbeatComponents();
   }

   /**
    * Handle incoming PTT packets from PTT Session.
    */
   void handlePttPacket(P25Payload p25Payload) {

      PacketType packetType = p25Payload.getISSIPacketType().getPacketType();
      if (smfReceiver.isPacketTypeHandled(packetType)) {
         smfReceiver.handlePttPacket(p25Payload);

      } else if (smfTransmitter.isPacketTypeHandled(packetType)) {
         smfTransmitter.handlePttPacket(p25Payload);

      } else {
         // Silently discard packet
      }
   }

   public SmfTransmitter getSmfTransmitter() {
      return this.smfTransmitter;
   }

   public SmfReceiver getSmfReceiver() {
      return this.smfReceiver;
   }

   public void setTransmitPriorityType(TransmitPriorityType priorityType) {
      transmitPriorityType = priorityType;

   }
   
   public TransmitPriorityType getTransmitPriorityType() {
      return this.transmitPriorityType;
   }

   public void setTransmitPriorityLevel(int priorityLevel) {
      transmitPriorityLevel = priorityLevel;

   }

   public int getTransmitPriorityLevel() {
      return this.transmitPriorityLevel;
   }
   
   public PttSessionMultiplexer getMultiplexer() {
      return this.multiplexer;
   }
   
   @Override 
   public void setOwningRfss(RfssConfig owningRfss) {
      super.setOwningRfss(owningRfss);
      if ( this.multiplexer != null ) {
         this.multiplexer.setOwningRfss(owningRfss);
      }
   }
   
   /**
    * Create a PTT progress packet for this session.
    * 
    * @param unitId
    * @param vbarray
    * @return
    */
   public P25Payload createPttTransmitProgress(int systemId, int unitId,
         IMBEVoiceBlock[] vbarray) {
      return super.createPttTransmitProgress(systemId,unitId, transmitPriorityType, transmitPriorityLevel, 
            vbarray);
   }
}
