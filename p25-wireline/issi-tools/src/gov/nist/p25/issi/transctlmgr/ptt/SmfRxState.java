//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines the Rx states for an SMF state machine.
 */
enum SmfRxState implements PttState {
   BEGIN, 
   RECEIVING,
   DONE
}
