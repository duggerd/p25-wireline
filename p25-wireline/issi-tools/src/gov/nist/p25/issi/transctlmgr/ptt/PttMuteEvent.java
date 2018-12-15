//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.p25payload.P25Payload;

public class PttMuteEvent extends PttEvent {
   private static final long serialVersionUID = -1L;
   
   /**
    * Get the tsn being muted.
    */
   public int getTSN() {
      return super.p25Payload.getISSIPacketType().getTransmissionSequenceNumber();
   }

   public PttMuteEvent(PttSession pttSession, P25Payload p25Payload) {
      super(pttSession, p25Payload);
   }
}
