//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines the listeners for an SMF.
 * 
 * @see ISSI spec v8, Section 7.2, Figure 65 Summary of Transitions
 */
public interface SmfTxListener {

   /* TODO (steveq):  All SMF and MMF control listeners should be refactored
    * by associating specific functionality with either an SMF or MMF.  There
    * are several alternatives.  One is to divide listeners into SmfTx, SmfRx,
    * and MmfRx listeners.  Another is to have a single PttListener that is
    * used for both SMFs and MMFs.  All Rx listeners should include a
    * receivedAudio() method.
    */
   public void receivedPttGrant(SmfPacketEvent event); 

   public void receivedPttDeny(SmfPacketEvent event); 

   public void receivedPttWait(SmfPacketEvent event); 
   
   public void requestTimeout();
   
   public void waitTimeout();

}
