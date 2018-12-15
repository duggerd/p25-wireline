//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines a listener for connection maintenance heartbeats.
 *
 * @see ISSI spec v8, Section 7.2, Figure 65 Summary of Transitions
 */
public interface MuteHeartbeatListener {
   
   /**
    * Indicates that a mute-related heartbeat was received from a peer 
    * SMF or MMF.
    */
   public void receivedMuteHeartbeat(int TSN, int M);
}
