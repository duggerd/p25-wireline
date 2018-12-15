//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines Rx states of an MMF state machine.
 */
public enum MmfRxState implements PttState {
	ARBITRATE, 
	DENY, 
	DONE, 
	BEGIN, 
	RECEIVING, 
	WAIT
}
