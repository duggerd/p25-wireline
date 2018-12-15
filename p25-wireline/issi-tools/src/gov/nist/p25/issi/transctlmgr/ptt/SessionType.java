
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * Defines the type of the PTT session. These are necessary
 * to define the behavior of the PTT state machines in forwarding
 * control packets.
 * This class defines the base functionality for SMF or MMF sessions.
 */
public enum SessionType {

   CALLING_HOME ("Calling Home"),
   CALLING_SERVING ("Calling Serving") ,
   CALLED_HOME ("Called Home"),
   CALLED_SERVING ("Called Serving"),
   GROUP_HOME("Group Home"),
   GROUP_SERVING("Group Serving");
   
   private String description;
   
   SessionType(String description) {
      this.description = description;
   }
   
   @Override
   public String toString() {
      return description;
   }
}
