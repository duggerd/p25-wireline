package gov.nist.p25.issi.rfss;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;

/**
 * Call Setup Request Event
 */
public class CallSetupRequestEvent extends CallControlRequestEvent {
   private static final long serialVersionUID = -1L;

   private ServerTransaction serverTransaction;

   public CallSetupRequestEvent(UnitToUnitCall unitToUnitCall, RequestEvent requestEvent,
         ServerTransaction serverTransaction) {
      super(unitToUnitCall, requestEvent);
      this.serverTransaction = serverTransaction;
   }

   public ServerTransaction getServerTransaction() {
      return serverTransaction;
   }
}
