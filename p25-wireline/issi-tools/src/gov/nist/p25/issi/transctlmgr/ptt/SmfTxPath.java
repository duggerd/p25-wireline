//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines Tx paths of an SMF state machine.
 */
class SmfTxPath extends PttPath {

   SmfTxPath(SmfTxState currentState, SmfTxTransition transition,
          SmfTxState nextState) {
      super(currentState, transition, nextState);
   }
   
   SmfTxState getCurrentState() {
      return (SmfTxState) currentState;
   }
   
   SmfTxTransition getTransition() {
      return (SmfTxTransition) transition;
   }

   SmfTxState getNextState() {
      return (SmfTxState) nextState;
   }
   
   public String toString() {
      return "\tTx Current State: " + currentState + "\n"
            + "\tTx Transition: " + transition + "\n"
            + "\tTx Next State: " + nextState ;
   }
}
