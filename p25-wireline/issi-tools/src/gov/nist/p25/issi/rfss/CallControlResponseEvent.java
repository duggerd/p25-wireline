//
package gov.nist.p25.issi.rfss;

import java.util.EventObject;
import javax.sip.ResponseEvent;

/**
 * Call Control Resonse Event
 */
public abstract class CallControlResponseEvent extends EventObject {
   private static final long serialVersionUID = -1L;

   private ResponseEvent responseEvent;

   // constructor
   public CallControlResponseEvent(Object eventObject) {
      super(eventObject);
   }
   public CallControlResponseEvent(Object eventObject, ResponseEvent responseEvent) {
      super(eventObject);
      setResponseEvent( responseEvent);
   }

   public ResponseEvent getResponseEvent() {
      return responseEvent;
   }
   public void setResponseEvent(ResponseEvent responseEvent) {
      this.responseEvent = responseEvent;
   }

   public int getStatusCode() {
      return responseEvent.getResponse().getStatusCode();
   }
}
