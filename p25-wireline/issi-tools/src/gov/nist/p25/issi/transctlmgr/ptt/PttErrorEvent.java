//
package gov.nist.p25.issi.transctlmgr.ptt;

import java.util.EventObject;
import gov.nist.p25.issi.p25payload.PacketType;

/**
 * This class implements PTT error events.
 */
public class PttErrorEvent extends EventObject {
   private static final long serialVersionUID = 1L;

   private PacketType payloadType;
   private Exception exception;

   // Constructor
   PttErrorEvent(PttSession pttSession, PacketType payloadType, Exception e) {
      super(pttSession);
      this.payloadType = payloadType;
      this.exception = e;
   }
   
   /**
    * @return Returns the payloadType.
    */
   public PacketType getPayloadType() {
      return payloadType;
   }

   /**
    * @return Returns the exception.
    */
   public Exception getException() {
      return exception;
   }
}
