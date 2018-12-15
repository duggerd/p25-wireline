//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.p25payload.P25Payload;
import java.util.*;

/**
 * This abstract class defines a PTT event.
 */
public abstract class PttEvent extends EventObject {
   private static final long serialVersionUID = -1L;

   /** The PTT payload associated with this event. */
   protected P25Payload p25Payload = null;

   /** The timestamp for this event. */
   private long receptionTime;

   /**
    * Construct a PTT event.
    * @param pttSession The PTT session associated with this event.
    * @param p25Payload The PTT payload associated with this event.
    */
   public PttEvent(PttSession pttSession, P25Payload p25Payload) {
      super(pttSession);
      this.p25Payload = p25Payload;
      receptionTime = System.currentTimeMillis();
   }
   
   /**
    * Get the PTT packet associated with this event.
    */
   public P25Payload getPttPacket() {
      return p25Payload;
   }
   
   /**
    * Get a string representation of this event.
    * @return The string representation of this event.
    */
   public String toString() {
      PttSession session = (PttSession) this.getSource();
      StringBuffer sbuf = new StringBuffer();
      //sbuf.append("\n<!-- PTT PACKET BEGIN -->");
      sbuf.append("\n<ptt-packet ");
      sbuf.append("\n receptionTime=\"" + receptionTime + "\"\n");
      sbuf.append(">\n");
      sbuf.append(session.toString());
      sbuf.append("\n");
      sbuf.append(p25Payload.toString());
      sbuf.append("\n</ptt-packet>");
      return sbuf.toString();
   }
}
