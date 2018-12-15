//
package gov.nist.p25.issi.rfss;

import javax.sip.ResponseEvent;

/**
 * Call Teardown Response Event
 */
public class CallTeardownResponseEvent extends UnitToUnitCallControlResponseEvent {
   private static final long serialVersionUID = -1L;

   public CallTeardownResponseEvent(UnitToUnitCall call, ResponseEvent responseEvent) {
      super(call, responseEvent);
   }
}
