//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines Tx states of an MMF state machine.
 */
public enum MmfTxState implements PttState {
   BEGIN, 
   TRANSMITTING, 
   TERMINATED
}
