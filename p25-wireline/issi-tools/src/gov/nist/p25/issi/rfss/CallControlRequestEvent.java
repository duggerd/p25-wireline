//
package gov.nist.p25.issi.rfss;

import java.util.EventObject;
import javax.sip.RequestEvent;

/**
 * Call control request event base class. This is the class that the Su sees.
 * 
 */
public abstract class CallControlRequestEvent extends EventObject {
   private static final long serialVersionUID = -1L;

   private RequestEvent requestEvent;

   // constructor
   public CallControlRequestEvent(UnitToUnitCall call) {
      super(call);
   }
   public CallControlRequestEvent(UnitToUnitCall call, RequestEvent requestEvent) {
      super(call);
      setRequestEvent( requestEvent);
   }

   public RequestEvent getRequestEvent() {
      return requestEvent;
   }
   public void setRequestEvent(RequestEvent requestEvent) {
      this.requestEvent = requestEvent;
   }

   public UnitToUnitCall getCallSegment() {
      return (UnitToUnitCall) getSource();
   }
}
