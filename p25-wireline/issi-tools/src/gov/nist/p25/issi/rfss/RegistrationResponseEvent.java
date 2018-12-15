//
package gov.nist.p25.issi.rfss;

import java.util.EventObject;
import javax.sip.message.Response;

public class RegistrationResponseEvent extends EventObject {
   private static final long serialVersionUID = -1L;
   //public static int TIMEOUT = -1;

   private Response response;

   public RegistrationResponseEvent(Object mobilityManager, Response response) {
      super(mobilityManager);
      this.response = response;
   }

   public Response getResponse() {
      return response;
   }
}
