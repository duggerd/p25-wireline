//
package gov.nist.p25.issi.rfss;

import java.util.EventObject;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;

/**
 * Definitions for the RegisterEvent
 * 
 */
public class RegisterEvent extends EventObject {
   private static final long serialVersionUID = -1L;
   
   enum EventTypes {
      REGISTER_DEREGISTER, REGISTER_QUERY, REGISTER_REGISTER
   };

   public static final EventTypes REGISTER_DEREGISTER = EventTypes.REGISTER_DEREGISTER;
   public static final EventTypes REGISTER_QUERY = EventTypes.REGISTER_QUERY;
   public static final EventTypes REGISTER_REGISTER = EventTypes.REGISTER_REGISTER;

   private RequestEvent requestEvent;
   private EventTypes eventType;
   private ServerTransaction serverTransaction;

   // accessor
   public RequestEvent getRequestEvent() { return requestEvent; }
   public EventTypes getEventType() { return eventType; }
   public ServerTransaction getServerTransaction() { return serverTransaction; }

   // constructor
   public RegisterEvent(RFSS source, RequestEvent requestEvent,
         ServerTransaction serverTx, EventTypes type) {
      super(source);
      this.eventType = type;
      this.requestEvent = requestEvent;
      this.serverTransaction = serverTx;
   }
}
