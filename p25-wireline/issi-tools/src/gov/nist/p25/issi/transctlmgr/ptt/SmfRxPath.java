//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines Rx paths of an SMF state machine.
 */
class SmfRxPath extends PttPath {

   SmfRxPath(SmfRxState currentState, SmfRxTransition transition,
         SmfRxState nextState) {
      super(currentState, transition,  nextState);
   }
   
   SmfRxState getCurrentState() {
      return (SmfRxState) currentState;
   }
   
   SmfRxTransition getTransition() {
      return (SmfRxTransition) transition;
   }
   
   SmfRxState getNextState() {
      return (SmfRxState) nextState;
   }
   
   public String toString() {
      return "\tRx Current State: " + currentState + "\n"
            + "\tRx Transition: " + transition + "\n"
            + "\tRx Next State: " + nextState + "\n"
            + "\tRx Incoming Packet Type: " + inPttMsgType + "\n"
            + "\tRx Outgoing Packet Type: " + outPttMsgType;
   }
}
