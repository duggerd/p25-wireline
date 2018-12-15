//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines the state machine for an SMF.
 */
public enum SmfTxTransition implements PttStateTransition {
   SEND_AUDIO, 
   SEND_CHANGE_IN_LOSING_STATE, 
   DENIED, END_TRIGGER, 
   FAIL_ON_TIMEOUT, 
   RECEIVED_GRANT, 
   REQUEST_TIMEOUT, 
   RESPONSE_FOR_INACTIVE_TSN, 
   SELF_GRANT, 
   START_TRANSMIT, 
   TX_TRIGGER, 
   WAIT, 
   WAIT_TIMEOUT
}
