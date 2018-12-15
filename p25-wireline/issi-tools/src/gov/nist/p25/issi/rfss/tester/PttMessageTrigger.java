//
package gov.nist.p25.issi.rfss.tester;

import java.util.HashSet;
import org.apache.log4j.Logger;

import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.p25.issi.p25payload.PacketType;

/**
 * Ptt message trigger gets consulted on each ptt message arrival.
 * 
 */
public class PttMessageTrigger extends Trigger {
   
   private static Logger logger = Logger.getLogger(PttMessageTrigger.class);
   
   private HashSet<String> firedTable = new HashSet<String>();
   
   public PttMessageTrigger( PacketType packetType) {
      super( packetType);
   }
   
   @Override
   public boolean isReady(String rfssDomainName, Object message) {
      if ( !(message instanceof P25Payload)) {
         return false;
      }
      P25Payload pttMessage = (P25Payload) message;
      boolean fired = firedTable.contains(rfssDomainName.toLowerCase()) ;
      if ( fired && isOneShot()) {
         return false;
      }
      fired = super.getMatchValue().equals(pttMessage.getISSIPacketType().getPacketType());
      if ( fired) {
         firedTable.add(rfssDomainName.toLowerCase());
      }
      logger.debug("pttMessageTrigger isReady = " + fired);
      return fired;
   }
}
