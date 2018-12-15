//
package gov.nist.p25.issi.rfss;

import javax.sip.RequestEvent;

/**
 * Call Teardown Indicate Event
 */
public class CallTeardownIndicateEvent extends CallControlRequestEvent {
   private static final long serialVersionUID = -1L;
   
   public CallTeardownIndicateEvent(UnitToUnitCall unitToUnitCall, RequestEvent requestEvent) {
      super(unitToUnitCall, requestEvent);
   }
}
