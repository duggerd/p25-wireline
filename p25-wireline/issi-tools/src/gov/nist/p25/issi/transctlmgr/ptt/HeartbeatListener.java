//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines a listener for connection maintenance heartbeats.
 */
public interface HeartbeatListener {
   
   /**
    * Indicates that a heartbeat was received from a peer SMF or MMF.
    */
   public void receivedHeartbeat(int TSN);
   
   /**
    * Indicates that a heartbeat query was received from a peer SMF or MMF.
    */
   public void receivedHeartbeatQuery(PttSessionInterface pttSession, int TSN);
   
   /**
    * Indicates that a heartbeat timeout has occured.
    */
   public void heartbeatTimeout(PttSessionInterface pttSession);
}
