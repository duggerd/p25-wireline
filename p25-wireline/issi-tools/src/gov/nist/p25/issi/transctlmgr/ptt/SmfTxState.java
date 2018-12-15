//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines the state machine for an SMF.
 */
public enum SmfTxState implements PttState {
   BEGIN,
   REQUESTING, 
   LOCAL_POLICY,  // We treat this as a Decision State
   TRANSMITTING, 
   WAITING,
   TERMINATED
}
