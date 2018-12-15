//
package gov.nist.rtp;

/**
 * This class implements an RTP error event.
 */
public class RtpErrorEvent extends RtpEvent {

   private static final long serialVersionUID = -1L;
   private Throwable cause = null;

   /**
    * Construct an RTP error event.
    * 
    * @param session
    *            The RTP session.
    * @param cause
    *            The Throwable event.
    */
   public RtpErrorEvent(RtpSession session, Throwable cause) {
      super(session, cause.getMessage());
   }

   /**
    * Get the Throwable object for this event.
    * 
    * @return The cause of this event.
    */
   public Throwable getCause() {
      return cause;
   }
}
