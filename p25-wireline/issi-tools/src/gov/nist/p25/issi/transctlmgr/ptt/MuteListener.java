//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines a listener for receiving mute/unmute.
 * 
 * @see ISSI spec v8, Section 7.2, Figure 65 Summary of Transitions
 */
public interface MuteListener {
   
   /**
    * Indicates that a mute was received from a peer SMF or MMF.
    */
   public void receivedMute(PttMuteEvent muteEvent);
   
   /**
    * Indicates that a unmute was received from a peer SMF or MMF.
    */
   public void receivedUnmute(PttUnmuteEvent pttUnmuteEvent);
}
