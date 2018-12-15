//
package gov.nist.rtp;

/**
 * This class implements an RTP event. Note that the event Source for the RTP
 * event is the RTP Packet Receiver that received the packet.
 * 
 */
public class RtpPacketEvent extends RtpEvent {

   private static final long serialVersionUID = -1L;

   /** The RTP packet. */
   private RtpPacket rtpPacket = null;

   /**
    * Construct an RTP packet event.
    * 
    * @param source
    *            The RTP session.
    * @param rtpPacket
    *            The RTP packet.
    * @param description
    *            A description of the RTP packet event.
    */
   public RtpPacketEvent(RtpSession source, RtpPacket rtpPacket,
         String description) {
      super(source, description);
      this.rtpPacket = rtpPacket;
   }

   /**
    * Get the RTP packet.
    * 
    * @return The RTP packet.
    */
   public RtpPacket getRtpPacket() {
      return rtpPacket;
   }
}
