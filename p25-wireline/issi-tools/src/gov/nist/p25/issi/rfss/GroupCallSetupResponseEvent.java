//
package gov.nist.p25.issi.rfss;

import javax.sip.ResponseEvent;

/**
 * Group Call Setup Response Event
 */
public class GroupCallSetupResponseEvent extends CallControlResponseEvent {
   private static final long serialVersionUID = -1L;

   // constructor
   public GroupCallSetupResponseEvent(GroupServing callSegment, ResponseEvent responseEvent) {
      super(callSegment, responseEvent);
   }

   public GroupServing getCallSegment() {
      return (GroupServing) getSource();
   }
}
