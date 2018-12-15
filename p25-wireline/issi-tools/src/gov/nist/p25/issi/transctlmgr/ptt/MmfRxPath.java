//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.p25payload.PacketType;

/**
 * This class defines Rx paths of an MMF state machine.
 * 
 * @author steveq@nist.gov
 * @version $Revision: 1.6 $, $Date: 2007/07/24 03:20:36 $
 * @since 1.5
 */
class MmfRxPath extends PttPath {
   
   MmfRxPath(MmfRxState currentState, MmfRxTransition transition,
          MmfRxState nextState) {
      super(currentState, transition, nextState);      
   }
   
   MmfRxState getCurrentState() {
      return (MmfRxState) currentState;
   }
   
   MmfRxTransition getTransition() {
      return (MmfRxTransition) transition;
   }
   
   MmfRxState getNextState() {
      return (MmfRxState) nextState;
   }
   
   PacketType getIncomingPttMsgType() {
      return inPttMsgType;
   }

   PacketType getOutgoingPttMsgType() {
      return outPttMsgType;
   }
   
   public String toString() {
      return "\tRx Current State: " + currentState + "\n"
            + "\tRx Transition: " + transition + "\n"
            + "\tRx Next State: " + nextState + "\n";
   }
}
