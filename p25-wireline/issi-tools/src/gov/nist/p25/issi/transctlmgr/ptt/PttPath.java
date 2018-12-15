//
package gov.nist.p25.issi.transctlmgr.ptt;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import gov.nist.p25.issi.p25payload.PacketType;

/**
 * This class defines a path for PTT state machine.
 */
@SuppressWarnings("unchecked")
abstract class PttPath {

   PttState currentState = null;
   PttStateTransition transition = null;
   PttState nextState = null;
   PacketType inPttMsgType = null;
   PacketType outPttMsgType = null;

   private static Hashtable<PttState, List<PttPath>> transitionTable = new Hashtable<PttState, List<PttPath>>();

   // constructor
   PttPath(PttState currentState, PttStateTransition transition,
         PttState nextState) {
      this.currentState = currentState;
      this.transition = transition;
      this.nextState = nextState;
   }

   static void add(PttPath pttPath) {
      List<PttPath> transitions = transitionTable.get(pttPath.currentState);
      if (transitions == null) {
         transitions = new LinkedList<PttPath>();
         transitions.add(pttPath);
         transitionTable.put(pttPath.currentState, transitions);
      }
      transitions.add(pttPath);
   }

   public static PttState getNextState(PttState currentState,
         PttStateTransition transition) throws IllegalStateException {
      if (transitionTable.get(currentState) == null)
         throw new IllegalStateException("Could not find transition for "
               + currentState);
      for (PttPath pathElement : transitionTable.get(currentState)) {
         if (pathElement.transition.equals(transition))
            return pathElement.nextState;
      }
      throw new IllegalStateException(String.format(
            "Cannot find transition for %s %s ", currentState, transition));
   }

   abstract Enum getCurrentState();

   abstract Enum getTransition();

   abstract Enum getNextState();

   public abstract String toString();
}
