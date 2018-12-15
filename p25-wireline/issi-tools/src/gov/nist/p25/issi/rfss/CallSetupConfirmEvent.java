package gov.nist.p25.issi.rfss;

import javax.sip.RequestEvent;

/**
 * Call Setup Confirm Event
 */
public class CallSetupConfirmEvent extends CallControlRequestEvent {
   
   private static final long serialVersionUID = -1L;

   public CallSetupConfirmEvent(UnitToUnitCall unitToUnitCall, RequestEvent requestEvent) {
      super(unitToUnitCall, requestEvent);
   }
}
