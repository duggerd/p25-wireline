//
package gov.nist.rtp;

import java.util.EventObject;

/**
 * An abstract base class for all RTP events.
 * 
 */
public abstract class RtpEvent extends EventObject {
   
   private static final long serialVersionUID = -1L;

   /** A description of this event. */
   private String description = "";

   /**
    * Construct an RTP event.
    * 
    * @param rtpSession
    *            The RTP session.
    * @param description
    *            A description of this event.
    */
   public RtpEvent(RtpSession rtpSession, String description) {
      super(rtpSession);
      this.description = description;
   }

   /**
    * Get a String representation of this event.
    * 
    * @return A String representation of this event.
    */
   public String toString() {
      return description;
   }
}
