//
package gov.nist.rtp;

/**
 * This class implements RTP timeout events.
 * 
 */
public class RtpTimeoutEvent extends RtpEvent {

   private static final long serialVersionUID = -1L;

   /**
    * Construct an RTP timeout event.
    * 
    * @param session
    *            the RTP session.
    * @param cause
    *            the timeout error message.
    */
   public RtpTimeoutEvent(RtpSession session, Throwable cause) {
      super(session, cause.getMessage());
   }
}
