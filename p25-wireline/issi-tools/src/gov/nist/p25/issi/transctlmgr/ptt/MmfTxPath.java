//
package gov.nist.p25.issi.transctlmgr.ptt;


/**
 * This class defines Tx paths of an MMF state machine.
 */
class MmfTxPath extends PttPath {

   MmfTxPath(MmfTxState currentState, 
         MmfTxTransition transition, MmfTxState nextState) {
      super(currentState,  transition, nextState);
   }
   
   MmfTxState getCurrentState() {
      return (MmfTxState) currentState;
   }

   MmfTxTransition getTransition() {
      return (MmfTxTransition) transition;
   }
   
   MmfTxState getNextState() {
      return (MmfTxState) nextState;
   }

   
   public String toString() {
      return "\tTx Current State: " + currentState + "\n"
            + "\tTx Transition: " + transition + "\n"
            + "\tTx Next State: " + nextState + "\n";
   }
}
