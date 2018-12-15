//
package gov.nist.p25.issi.rfss.tester;

import java.util.HashSet;

import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

public class SipMessageTrigger extends Trigger {

   private static Logger logger = Logger.getLogger(SipMessageTrigger.class);

   private boolean isRequest;
   private int responseCode;
   private HashSet<String > firedTable = new HashSet<String>();

   // constructor
   public SipMessageTrigger(String messageType) {
      super(messageType);
      if ( Request.INVITE.equals(messageType) || 
           Request.ACK.equals(messageType) || 
           Request.BYE.equals(messageType) || 
           Request.CANCEL.equals(messageType)) {
         isRequest = true;
      } else {
         responseCode = Integer.parseInt(messageType);
      }
   }

   @Override
   public boolean isReady(String rfssDomainName, Object matchObject) {
      if ( isRequest && matchObject instanceof Request) {
         Request request = (Request) matchObject;
         String type  = request.getMethod();
         boolean fired = firedTable.contains(rfssDomainName.toLowerCase());
         logger.debug("SipMessageTrigger: type : " + type + " fired " + fired + " oneShot " + isOneShot());
         
         if (fired && isOneShot())
            return false;
         fired = type.equals(super.getMatchValue());
         
         logger.debug("Returning " + fired);
         if (fired)
            firedTable.add(rfssDomainName.toLowerCase());
         return fired;
         
      } else if ( (!isRequest) && matchObject instanceof Response)  {
         Response response = (Response) matchObject;
         int statusCode = response.getStatusCode();
         boolean fired = firedTable.contains(rfssDomainName.toLowerCase());
         
         if (fired && isOneShot())
            return false;
         fired =  this.responseCode == statusCode;
         if (fired)
            firedTable.add(rfssDomainName.toLowerCase());
         return fired;
      } else
         return false;
   }
}
