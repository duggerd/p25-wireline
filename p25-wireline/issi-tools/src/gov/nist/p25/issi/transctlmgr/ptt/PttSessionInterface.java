//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.rtp.RtpPacket;
import gov.nist.rtp.RtpSession;

/**
 * A ptt session interface. 
 */
public interface PttSessionInterface {
   
   /**
    * get the type of link associated with this session.
    * 
    * @return -- the link type.
    */
   public LinkType getLinkType();
   
   /**
    * Get the session type (i.e. MMF or SMF)
    * @return -- a string indicating sesison type.
    */
   public String getSessionType();
   
   /**
    * Shutdown this sesison ( in the case of multiplexed sesison, shut down all sessions).
    *
    */
   public void shutDown();
   
   /**
    * Get the heartbeat transmitter for this session.
    * 
    * @return the heartbeat transmitter.
    */
   public HeartbeatTransmitter getHeartbeatTransmitter();
   
   /**
    * Get the heartbeat receiver.
    * 
    * @return -- the heartbeat receiver.
    */
   public HeartbeatReceiver getHeartbeatReceiver();
   
   /**
    * 
    * @return - the rtp port where I am getting incoming packets.
    */
   public int getMyRtpRecvPort();
   
   /**
    * Set the remote port where packets are supposed to be sent.
    * 
    * @param rport -- the location to send packets to.
    * 
    */
   public void setRemoteRtpRecvPort(int rport);
   
   /**
    * Set the remote IP address where packets are supposed to be sent.
    * 
    * @param ipAddress
    */
   public void setRemoteIpAddress(String ipAddress);
   
   /**
    * Get the associated RTP session.
    * 
    * @return
    */
   
   public RtpSession getRtpSession();
   
   /**
    * Start the heartbeat components ( heartbeat transmitter and receiver).
    *
    */
   public void startHeartbeatComponents();
   
   
   /**
    * 
    * Get the Rfss which created me.
    * 
    * @return -- the configuration of the RFSS which created me.
    */
   
   public RfssConfig getOwningRfss();
   
   /**
    * Get the RFSS from which I am getting data and to which I am sending data.
    * 
    */
   public String getRemoteRfssDomainName();
   
   
   /**
    * Set the RFSS from which I am getting data and to which I am sending data
    */
   public void setRemoteRfssDomainName(String radicalName);
   
      
   
   /**
    * Log a packet (store in memory or write to the log file ).
    * 
    * @param p25Payload -- packet to log.
    * @param isSender -- Am I sending or recieving this packet.
    * 
    */
   public void logPttPacket(RtpPacket rtpPacket, P25Payload p25Payload, boolean isSender, long timeStamp, int packetNumber,
         String remoteRfssDomainName, String remoteIpAddress, int remotePort);

}
