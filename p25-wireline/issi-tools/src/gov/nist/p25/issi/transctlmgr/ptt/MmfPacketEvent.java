//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.p25payload.*;

/**
 * This class implements an MMF packet event.
 */
public class MmfPacketEvent extends PttEvent {
   private static final long serialVersionUID = -1L;
   
   /** The MMF session associated with this event. */
   private MmfSession mmfSession;

   /**
    * Construct an MMF packet event.
    * @param mmfSession The MMF session associated with this event.
    * @param p25Payload The PTT packet associated with this event.
    */
   public MmfPacketEvent(MmfSession mmfSession, P25Payload p25Payload) {
      super(mmfSession, p25Payload);
      this.mmfSession = mmfSession;
   }

   /**
    * Get the MMF session associated with this event.
    * @return The MMF session associated with this event.
    */
   public MmfSession getMmfSession() {
      return mmfSession;
   }
}
