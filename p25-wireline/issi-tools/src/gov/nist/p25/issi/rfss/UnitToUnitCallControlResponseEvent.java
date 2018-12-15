//
package gov.nist.p25.issi.rfss;

import javax.sip.ResponseEvent;

/**
 * Wrapper class for SIP response events.
 */
public class UnitToUnitCallControlResponseEvent extends CallControlResponseEvent {
   private static final long serialVersionUID = -1L;

   public UnitToUnitCallControlResponseEvent(UnitToUnitCall unitToUnitCall,
         ResponseEvent responseEvent) {
      super(unitToUnitCall, responseEvent);
   }

   public UnitToUnitCall getCallSegment() {
      return (UnitToUnitCall) getSource();
   }
}
