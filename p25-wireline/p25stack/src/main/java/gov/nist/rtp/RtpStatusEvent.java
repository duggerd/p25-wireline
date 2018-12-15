//
package gov.nist.rtp;

/**
 * RtpStatus events are encapsulated using this class.
 * 
 */
public class RtpStatusEvent extends RtpEvent {

   private static final long serialVersionUID = -1L;

   /** The status of the RTP stack. */
   private RtpStatus status = null;

   /**
    * Construct an RTP status event.
    * 
    * @param session
    *            The RTP session
    * @param status
    *            The RTP status
    * @param statusMessage
    *            The status message
    */
   public RtpStatusEvent(RtpSession session, RtpStatus status,
         String statusMessage) {
      super(session, statusMessage);
      this.status = status;
   }

   /**
    * Get the status message
    * 
    * @return the status message
    */
   public RtpStatus getStatus() {
      return status;
   }
}
