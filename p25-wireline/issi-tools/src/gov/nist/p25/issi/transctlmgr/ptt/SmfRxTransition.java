//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines Rx transitions of an MMF state machine.
 */
enum SmfRxTransition implements PttStateTransition {
   SPURT_START, 
   RECEIVE_AUDIO, 
   RECEIVE_CHANGE_IN_LOSING_AUDIO, 
   SPURT_END
}
