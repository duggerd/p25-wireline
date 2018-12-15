//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.p25payload.*;

/**
 * This class implements an PTT packet event.
 */
public class SmfPacketEvent extends PttEvent {

   private static final long serialVersionUID = -1L;

   /** The owning SMF session. */
   private SmfSession smfSession = null;
   
   /**
    * Construct an SMF packet event.
    * @param smfSession The owning SMF session.
    * @param p25Payload The associated PTT packet.
    */
   public SmfPacketEvent(SmfSession smfSession, P25Payload p25Payload) {
      super(smfSession, p25Payload);
      this.smfSession = smfSession;
   }

   /** 
    * Get the associated SMF session.
    * @return The associated SMF session.
    */
   public SmfSession getSmfSession() {
      return smfSession;
   }
}
