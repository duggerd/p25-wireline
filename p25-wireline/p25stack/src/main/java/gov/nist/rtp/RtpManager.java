//
package gov.nist.rtp;

import java.io.*;
import java.net.*;
import org.apache.log4j.Logger;

/**
 * This class implements an RTP manager. An RTP manager is a single point of
 * access for an RTP sender/receiver and RTCP sender/receiver. An RTP manager
 * logically implements RTP sessions between RTP senders and receivers. An RTP
 * manager also traps RTP events of interest to listening applications.
 * 
 */
public class RtpManager {

   /** The logger for this class. */
   private static Logger logger = Logger.getLogger(RtpManager.class);

   /** The IP address of this host. */
   private InetAddress myIpAddress = null;

   /**
    * Construct an RTP manager with the default localhost IP address.
    * 
    * @throws UnknownHostException
    */
   public RtpManager() throws UnknownHostException {
      this.myIpAddress = InetAddress.getLocalHost();
      //logger.debug("RtpManager(): myIpAddress="+myIpAddress);
   }

   /**
    * Construct a RTP manager with the given IP address. This constructor is
    * useful when a machine has multiple IP interfaces (i.e., NICs).
    * 
    * @param ipAddress
    *            the user-defined IP address for this host
    * @throws UnknownHostException
    */
   public RtpManager(String ipAddress) throws UnknownHostException {
      this.myIpAddress = InetAddress.getByName(ipAddress);
      //logger.debug("RtpManager(ip): myIpAddress="+myIpAddress);
   }

   /**
    * Create an RTP session.
    */
   public RtpSession createRtpSession(int myRtpRecvPort, 
         String remoteIpAddress, int remoteRtpRecvPort)
         throws SocketException, IOException {
      if (logger.isDebugEnabled())
         logger.debug("Creating RTP session: " + this.myIpAddress + ":"
               + myRtpRecvPort);
      return new RtpSession(this.myIpAddress, myRtpRecvPort, remoteIpAddress,
            remoteRtpRecvPort);
   }

   /**
    * Create an RTP session that binds only the RTP receive port and waits
    * indefinitely to receive an RTP packet. <I>When using this method, care
    * must be taken to manually set the remote IP address and remote RTP
    * receive port on the RtpSession object before calling
    * RtpSession.sendRtpPacket().</I>
    */
   public RtpSession createRtpSession(int myRtpRecvPort)
         throws SocketException {
      if (logger.isDebugEnabled())
         logger.debug("Creating RTP session: " + this.myIpAddress + ":"
               + myRtpRecvPort);
      return new RtpSession(this.myIpAddress, myRtpRecvPort);
   }

   /**
    * Get my IP address.
    * 
    * @return this host's IP address
    */
   public InetAddress getMyIpAddress() {
      return this.myIpAddress;
   }
}
