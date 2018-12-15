//
package gov.nist.p25.issi.rfss;

import java.util.EventObject;
import javax.sip.message.Response;

/**
 * Deregistration Response Event
 */
public class DeregistrationResponseEvent extends EventObject {
   private static final long serialVersionUID = -1L;

   private Response response;

   // constructor
   public DeregistrationResponseEvent(Object mobilityManager, Response response) {
      super(mobilityManager);
      this.response = response;
   }

   public Response getResponse() {
      return response;
   }
}
