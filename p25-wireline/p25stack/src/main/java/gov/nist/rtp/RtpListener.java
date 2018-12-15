//
package gov.nist.rtp;

/**
 * This class implements an RTP listener.
 *
 */
public interface RtpListener {

   /**
    * Handle the received RTP packet.
    * 
    * @param rtpEvent
    *            The received RTP packet event.
    */
   public void handleRtpPacketEvent(RtpPacketEvent rtpEvent);

   /**
    * Handle an RTP Status event.
    * 
    * @param rtpEvent
    *            The received RTP status event.
    */
   public void handleRtpStatusEvent(RtpStatusEvent rtpEvent);

   /**
    * Handle an RTP timeout event.
    * 
    * @param rtpEvent
    *            The received RTP timeout event.
    */
   public void handleRtpTimeoutEvent(RtpTimeoutEvent rtpEvent);

   /**
    * Handle an RTP error event.
    * 
    * @param rtpEvent
    *            The received RTP error event.
    */
   public void handleRtpErrorEvent(RtpErrorEvent rtpEvent);

}
