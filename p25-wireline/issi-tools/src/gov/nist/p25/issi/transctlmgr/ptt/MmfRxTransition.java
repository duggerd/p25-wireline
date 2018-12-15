//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines Rx transitions of an MMF state machine.
 */
enum MmfRxTransition implements PttStateTransition {

   RECEIVE_AUDIO, 
   RECEIVE_CHANGE_IN_LOSING_AUDIO, 
   DENY_DECISION, 
   DENY_TRIGGER, 
   END_LOSS_TIMEOUT, 
   GRANT_DECISION, 
   GRANT_TRIGGER, 
   START_RECEIVING,  // STATE
   WAIT_DECISION, 
   WAIT_THEN_GRANT,  // Used for specifying a grant after waiting
   WAIT_THEN_DENY,   // Used for specifying a deny after waiting
   SET_LOSING, 
   SPURT_REQUEST, 
   SPURT_END
}
