//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.p25payload.*;
import gov.nist.rtp.*;

import java.io.IOException;
import java.util.Hashtable;
//import org.apache.log4j.Logger;

/**
 * This class implements an MMF session.
 */
public class MmfSession extends PttSession {

   //private static Logger logger = Logger.getLogger(MmfSession.class);

   /** Map of incoming tsn to outgoing tsn */
   private Hashtable<Integer,Integer> incomingTsnToOutgoingTsnMap = 
      new Hashtable<Integer,Integer> ();

   /** The MMF receiver for this session. */
   private MmfReceiver mmfReceiver = null;

   /** The MMF transmitter for this session. */
   private MmfTransmitter mmfTransmitter = null;

   /**
    * Construct an MMF session.
    */
   MmfSession(RtpSession rtpSession, LinkType linkType, PttManager manager)
      throws RtpException, IOException {
      super(rtpSession, linkType,manager);
      rtpSession.addRtpListener(this);

      mmfTransmitter = new MmfTransmitter(this);
      mmfReceiver = new MmfReceiver(this);
      // rtpSession.setApplicationData(this);

      // Start mute and heartbeat components
      startMuteComponents();
      startHeartbeatComponents();
   }

   /**
    * Handle incoming PTT packets.
    */
   void handlePttPacket(P25Payload p25Payload) {

      PacketType packetType = p25Payload.getISSIPacketType().getPacketType();
      if (mmfReceiver.isPacketTypeHandled(packetType)) {
         mmfReceiver.handlePttPacket(p25Payload);
      } else {
         // Silently discard packet
      }
   }

   /**
    * @return the mmfReceiver
    */
   public MmfReceiver getMmfReceiver() {
      return mmfReceiver;
   }

   /**
    * @return the mmfTransmitter
    */
   public MmfTransmitter getMmfTransmitter() {
      return mmfTransmitter;
   }

   public void addNewTsnToMap(int incomingTsn, int outgoingTsn) {
      this.incomingTsnToOutgoingTsnMap.put(incomingTsn, outgoingTsn);
   }
   
   public int getOutgoingTsn(int incomingTsn) {
      return this.incomingTsnToOutgoingTsnMap.get(incomingTsn);
   }
}
