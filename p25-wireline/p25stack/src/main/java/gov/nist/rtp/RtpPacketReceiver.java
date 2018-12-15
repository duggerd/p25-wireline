//
package gov.nist.rtp;

import gov.nist.rtp.RtpPacket;
import java.io.*;
import java.net.*;
import org.apache.log4j.Logger;

/**
 * This class implements an RTP packet receiver. An RTP packet receiver listens
 * on the designated port for incoming RTP packets. This class is implemented as
 * a Thread rather than a Runnable object so that we may invoke an interrupt to
 * halt execution.
 * 
 */
public class RtpPacketReceiver extends Thread {

   private static Logger logger = Logger.getLogger(RtpPacketReceiver.class);

   /**
    * The time to live for waiting for an RTP packet in milliseconds.
    * 0 means wait indefinitely.
    */
   private static int TTL = 0;

   /** The socket for receiving an RTP packet. */
   private DatagramSocket receiveSocket = null;

   /** The calling RTP session. */
   private RtpSession rtpSession = null;

   /** Logs sequence number of last packet received. */
   private int lastRtpPacketSequenceNumber = 0;

   /**
    * Construct an RTP packet receiver.
    * 
    * @param rtpSession
    *            the calling RTP session.
    * @throws SocketException
    */
   public RtpPacketReceiver(RtpSession rtpSession) throws SocketException {
      this.rtpSession = rtpSession;
      this.receiveSocket = rtpSession.getRtpRecvSocket();
   }

   /**
    * Run this object.
    */
   public void run() {
      try {
         // Set timeout on packet-receive wait
         receiveSocket.setSoTimeout(TTL);

         // Since RTP packets are variable size, we have to be smart about
         // how large to set the incoming datagram packet buffer. If we
         // set to the maximum UDP packet size, we will ensure getting all
         // packet data, but performance will be extremely slow. If we
         // set to a smaller size, speed will increase, but we risk losing
         // data at the end of the packet.
         int bufferSize = RtpPacket.FIXED_HEADER_LENGTH
               + RtpPacket.MAX_PAYLOAD_BUFFER_SIZE;
         byte[] buffer = new byte[bufferSize];
         DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

         for (;;) {

            // Receive the UDP packet
            receiveSocket.receive(packet);
            
            byte[] packetData = packet.getData();
            // Get packet size. Note that this is NOT the same as
            // packetData.length!
            int packetSize = packet.getLength();

            // If we are logging in debug mode, store the raw RTP data
            // so that we can write it to a file later.
            // if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
            if (logger.isTraceEnabled()) {
               byte[] payload = new byte[packetSize];
               System.arraycopy(packetData, 0, payload, 0, packetSize);
               rtpSession.loggedRtpPackets.add(payload);
            }

            RtpPacket rtpPacket = new RtpPacket(packetData, packetSize);
            // rtpPacket.set();

            // Only process RTP packets in sequence. Otherwise, discard
            int rtpPacketSN = rtpPacket.getSN();
            if (rtpPacketSN > lastRtpPacketSequenceNumber) {

               lastRtpPacketSequenceNumber = rtpPacketSN;

               // Send event to listeners
               RtpPacketEvent rtpEvent = new RtpPacketEvent(rtpSession,
                     rtpPacket, "Received RTP packet");

               for (RtpListener listener : rtpSession.listeners)
                  listener.handleRtpPacketEvent((RtpPacketEvent) rtpEvent);
            } else {
               // Silently discard
               // if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
               // logger.info("[RtpPacketReceiver] Received out of " +
               // "sequence RTP packet. Discarding.");
            }
         }
      } catch (SocketException se) {

         RtpTimeoutEvent rtpEvent = new RtpTimeoutEvent(rtpSession, se);
         for (RtpListener listener : rtpSession.listeners)
            listener.handleRtpTimeoutEvent((RtpTimeoutEvent) rtpEvent);

      } catch (IOException se) {

         RtpErrorEvent rtpEvent = new RtpErrorEvent(rtpSession, se);
         for (RtpListener listener : rtpSession.listeners)
            listener.handleRtpErrorEvent((RtpErrorEvent) rtpEvent);

      } finally {

         // This is invoked when an interrupt is called on this thread.
         if (logger.isDebugEnabled()) {
            logger.debug("RtpPacketReceiver shutting down.");
         }

         if (receiveSocket != null) {
            receiveSocket.close();
            receiveSocket = null;
         }
      }
   }
}
