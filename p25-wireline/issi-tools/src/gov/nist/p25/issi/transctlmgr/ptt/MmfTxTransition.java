//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines Tx transitions of an MMF state machine.
 */
enum MmfTxTransition implements PttStateTransition {
   SEND_AUDIO, 
   SEND_CHANGE_IN_LOSING_STATE, 
   END_TRIGGER, 
   TX_TRIGGER
}
