//
package gov.nist.rtp;

import gov.nist.p25.issi.utils.ByteUtil;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 * This class implements an RTP session. An RTP session is the application API
 * to both an RTP transmitter and an RTP receiver. This version of RTP is based
 * on <a href="http://www.ietf.org/rfc/rfc3550.txt">IETF RFC 3550</a>, but does
 * not include RTCP. Thus, this version of RTP is geared toward those
 * applications that need to transmit RTP packets, but use mechanisms other than
 * RTCP for managing those packets.
 * 
 */
public class RtpSession {

   private static final long serialVersionUID = -1L;

   /** The logger for this class. */
   private static Logger logger = Logger.getLogger(RtpSession.class);

   /**
    * The relative directory path for storing RTP log files.
    */
   private final String logDirectory = "../nistrtp_logs";

   /** My IP address. */
   private InetAddress myIpAddress = null;

   /** The remote IP address. */
   private String remoteIpAddress = "";

   /** The remote RTP receive port. */
   private int remoteRtpRecvPort = -1;

   /** My RTP receive port. */
   protected int myRtpRecvPort = -1;

   /** The RTP packet receiver. */
   private RtpPacketReceiver rtpPacketReceiver = null;

   /** A thread for executing the RTP packet receiver. */
   // private Thread rtpPacketReceiverThread = null;
   /** The RTP receive socket. */
   private DatagramSocket myRtpSendSocket = null;

   /** The RTP send socket. */
   private DatagramSocket myRtpRecvSocket = null;

   /** The remote Inet address. */
   private InetAddress remoteInetAddress = null;

   /** List of RTP listeners. */
   protected ArrayList<RtpListener> listeners = null;

   /**
    * Received RTP packets. This is particularly useful for examining RTP
    * packets at a later time. Contents of these RTP packets will be written to
    * an RTP log file.
    */
   protected ArrayList<byte[]> loggedRtpPackets = new ArrayList<byte[]>();

   /** The rtp sequence number for this session */
   private int rtpSequenceNumber = 1;

   /**
    * Construct an RTP session.
    * 
    * @param myIpAddress
    *            The IP address of this host.
    * @param myRtpRecvPort
    *            The RTP receive port.
    * @param remoteIpAddress
    *            The remote IP address.
    * @param remoteRtpRecvPort
    *            The remote RTP receive port.
    * @throws SocketException
    * @throws IOException
    */
   public RtpSession(InetAddress myIpAddress, int myRtpRecvPort,
         String remoteIpAddress, int remoteRtpRecvPort)
         throws SocketException, IOException {

      this.myIpAddress = myIpAddress;
      this.myRtpRecvPort = myRtpRecvPort;
      this.remoteIpAddress = remoteIpAddress;
      this.remoteRtpRecvPort = remoteRtpRecvPort;

      myRtpRecvSocket = new DatagramSocket(myRtpRecvPort);
      myRtpSendSocket = myRtpRecvSocket; // Auto binds to an open port
      remoteInetAddress = InetAddress.getByName(remoteIpAddress);
      listeners = new ArrayList<RtpListener>();
   }

   /**
    * Construct an RTP session. This constructor is typically used if the
    * remoteRtpRecvPort is not known at the time of instantiation. Here, only
    * myRtpRecvPort is bound and remoteRtpRecvPort is bound later when that
    * port becomes known. <i>Care should be taken when using this constructor
    * since it is left to the application to set the remote RTP IP address and
    * remote RTP receive port. If an application attempts to send an RTP packet
    * without the remote IP address and RTP receive port defined, the
    * sendRtpPacket() method will throw an exception.</i>
    * 
    * @param myIpAddress
    *            The IP address of this host.
    * @param myRtpRecvPort
    *            The RTP receive port.
    * @throws SocketException
    */
   public RtpSession(InetAddress myIpAddress, int myRtpRecvPort)
         throws SocketException {

      this.myIpAddress = myIpAddress;
      this.myRtpRecvPort = myRtpRecvPort;

      if (myRtpRecvPort != 0) {
         myRtpRecvSocket = new DatagramSocket(myRtpRecvPort);
         myRtpSendSocket = myRtpRecvSocket; // Auto binds to an open port
      } else {
         // A 0 port argument can occur when there is no RTP resources
         // available.
         myRtpSendSocket = new DatagramSocket(0);
         myRtpRecvSocket = null;
      }
      listeners = new ArrayList<RtpListener>();
   }

   /**
    * Add an RTP listener.
    * 
    * @param listener
    *            The RTP listener to be added.
    */
   public void addRtpListener(RtpListener listener) {
      listeners.add(listener);
   }

   /**
    * Remove an RTP listener.
    * 
    * @param listener
    *            The RTP listener to be removed.
    */
   public void removeRtpListener(RtpListener listener) {
      listeners.remove(listener);
   }

   /**
    * Start receiving thread for RTP packets. Note that only one RTP packet
    * receiver can be running at a time.
    * 
    * @throws IOException
    * @throws RtpException
    */
   public void receiveRTPPackets() throws SocketException, RtpException {
      if (this.myRtpRecvSocket == null)
         throw new RtpException("No socket -- cannot recieve packets! ");
      if (this.myRtpRecvSocket.isClosed())
         throw new SocketException("Socket is closed.");
      if ((rtpPacketReceiver == null)
            || (rtpPacketReceiver.getState() == Thread.State.TERMINATED)) {
         rtpPacketReceiver = new RtpPacketReceiver(this);
         rtpPacketReceiver.start();
      }
   }

   /**
    * Release the port and stop the reciever.
    * 
    */
   public void stopRtpPacketReceiver() {
      this.rtpPacketReceiver.interrupt();
      this.myRtpRecvPort = 0;
      // Note that the interrupt call will close the socket
      // if the remote rtp recv port is still open, we
      // allocate a socket for it. This is because the sending
      // and receiving socket are the same.
      try {
         if (this.remoteRtpRecvPort > 0)
            this.myRtpSendSocket = new DatagramSocket();
      } catch (SocketException ex) {

      }
   }

   /**
    * Send an RTP packet.
    * 
    * @param rtpPacket
    *            The RTP packet to send.
    * @throws IOException
    * @throws UnknownHostException
    * @throws RtpException
    */
   public synchronized void sendRtpPacket(RtpPacket rtpPacket)
         throws RtpException, UnknownHostException, IOException {

      // Ensure that outgoingDatagramPacket has been initialized
      // with a remote IP address and remote RTP receive port
      rtpPacket.setSN(rtpSequenceNumber++);
      if (remoteInetAddress == null) {
         if (remoteIpAddress == "") {
            throw new RtpException("Failed sending RTP packet. "
                  + "Remote IP address is undefined.");
         } else {
            remoteInetAddress = InetAddress.getByName(remoteIpAddress);
         }
      }

      if (remoteRtpRecvPort < 0) {
         throw new RtpException("Failed sending RTP packet. "
               + "Remote RTP receive port is undefined.");
      }

      DatagramPacket outgoingDatagramPacket = new DatagramPacket(new byte[1],
            1, remoteInetAddress, remoteRtpRecvPort);

      // Convert RTP packet to byte array
      byte[] rtpPacketBytes = rtpPacket.getData();

      // Set RTP packet as UDP payload and send
      outgoingDatagramPacket.setData(rtpPacketBytes);

      if (myRtpSendSocket != null)
         myRtpSendSocket.send(outgoingDatagramPacket);
   }

   /**
    * Similar to normal shutdown except that we log all received RTP raw data
    * to a timestamped file for future analyses and debugging.
    * 
    * @param sessionID1
    *            An application-generated ID for distinguishing this RTP
    *            sesion.
    * @param sessionID2
    *            A second application-generated ID for furthering
    *            distinguishing this RTP session.
    */
   public void shutDown(String sessionID1, String sessionID2) {

      //System.out.println("shutDown: ID1="+sessionID1 +"  ID2="+sessionID2);
      // Do normal shutdown routine first
      shutDown();

      // If logging in TRACE mode, write raw data from logged RTP packets
      // to time-stamped file.
      if (logger.isTraceEnabled()) {

         // Check if NIST RTP Log directory exists. If not, create it
         // at the same directory level as this application.
         File rtpLogDirectory = new File(logDirectory);

         if (!rtpLogDirectory.exists()) {

            // Make the directory
            boolean success = rtpLogDirectory.mkdirs();
            logger.debug("shutDown: mkdir-success="+success);
         }

         SimpleDateFormat fSDateFormat = new SimpleDateFormat(
               "yyyy-MM-dd_HH-mm-ss");
         String date = fSDateFormat.format(new Date().getTime());
         String file_name = date + "_" + sessionID1 + "_" + sessionID2;

         try {
            BufferedWriter out = new BufferedWriter(new FileWriter(
                  logDirectory + "/" + file_name + ".rtp"));

            for (int i = 0; i < loggedRtpPackets.size(); i++) {
               out.write("RTP Packet " + i);
               byte[] rtpBytes = loggedRtpPackets.get(i);
               out.write(ByteUtil.writeBytes(rtpBytes) + "\n");
            }
            out.close();

         } catch (IOException ioe) {
            ioe.printStackTrace();
         }
      }
   }

   /**
    * Shut this RTP session down.
    */
   public void shutDown() {

      if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
         logger.debug("[RtpSession " + getMyIpAddress() + ":"
               + getMyRtpRecvPort() + "] shutting down");
      }

      if (rtpPacketReceiver != null) // may be null because recieve port has
         // not yet been associagted
         rtpPacketReceiver.interrupt(); // Shut down RTP packet receiver

      if (myRtpRecvSocket != null) {
         if( !myRtpRecvSocket.isClosed()) {
            myRtpRecvSocket.close();
         }
         myRtpRecvSocket = null;
      }

      if (myRtpSendSocket != null) {
         if( !myRtpSendSocket.isClosed()) {
            myRtpSendSocket.close();
         }
         myRtpSendSocket = null;
      }
   }

   /**
    * Get my IP address.
    * 
    * @return My IP address
    */
   public InetAddress getMyIpAddress() {
      return myIpAddress;
   }

   /**
    * Get the remote IP address.
    * 
    * @return The remote IP address.
    */
   public String getRemoteIpAddress() {
      return remoteIpAddress;
   }

   /**
    * Set the remote IP address.
    * 
    * @param remoteIpAddress
    *            The remote IP address.
    */
   public void setRemoteIpAddress(String remoteIpAddress) {
      this.remoteIpAddress = remoteIpAddress;
   }

   private void logStackTrace() {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      StackTraceElement[] ste = new Exception().getStackTrace();
      // Skip the log writer frame and log all the other stack frames.
      for (int i = 1; i < ste.length; i++) {
         String callFrame = "[" + ste[i].getFileName() + ":"
               + ste[i].getLineNumber() + "]";
         pw.print(callFrame);
      }
      pw.close();
      String stackTrace = sw.getBuffer().toString();
      logger.debug(stackTrace);
   }

   /**
    * Set the remote RTP receive port.
    * 
    * @param remoteRtpRecvPort
    *            The remote RTP receive port.
    */
   public void setRemoteRtpRecvPort(int remoteRtpRecvPort) {

      if (remoteRtpRecvPort % 2 != 0) {
         throw new IllegalArgumentException("RtpRecvPort must be even.");
      }
      if (logger.isDebugEnabled()) {
         logStackTrace();
         logger.debug("rtpSession: setRemoteRtpRecvPort : " + remoteRtpRecvPort);
      }

      this.remoteRtpRecvPort = remoteRtpRecvPort;
      if (remoteRtpRecvPort == 0
            && this.myRtpSendSocket != this.myRtpRecvSocket ) {
         if ( myRtpSendSocket != null ) this.myRtpSendSocket.close();
         this.myRtpSendSocket = null;
      }

   }

   /**
    * Set my RTP recv port. This method is called when setting up half duplex
    * sessions when RTP resources later become available.
    */
   public void resetMyRtpRecvPort(int myRtpRecvPort) throws RtpException {
      if (myRtpRecvPort == 0 || myRtpRecvPort % 2 != 0) {
         throw new IllegalArgumentException("RtpRecvPort must be even.");
      }

      try {
         if (this.rtpPacketReceiver != null)
            this.rtpPacketReceiver.interrupt();
         this.rtpPacketReceiver = null;
         this.myRtpRecvPort = myRtpRecvPort;
         this.myRtpRecvSocket = new DatagramSocket(myRtpRecvPort);
         this.myRtpSendSocket = myRtpRecvSocket;
      } catch (SocketException ex) {
         throw new RtpException("failed to assign recv port", ex);
      }
   }

   /**
    * Get the remote RTP receive port.
    * 
    * @return The remote RTP receive port.
    */
   public int getRemoteRtpRecvPort() {
      return remoteRtpRecvPort;
   }

   /**
    * Get my RTP receive port.
    * 
    * @return My RTP receive port.
    */
   public int getMyRtpRecvPort() {
      return myRtpRecvPort;
   }

   /**
    * Get my RTP receive socket.
    * 
    * @return My RTP receive port.
    */
   public DatagramSocket getRtpRecvSocket() {
      return myRtpRecvSocket;
   }

   /**
    * Get the XML formatted string representation.
    * 
    * @return the XML formatted string representation.
    */
   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("<rtp-session\n");
      sbuf.append(" senderIpAddress=\"" + remoteIpAddress + "\"\n");
      sbuf.append(" remoteRtpRecvPort=\"" + remoteRtpRecvPort + "\"\n");
      //sbuf.append(" remoteRtcpRecvPort=\"" + remoteRtcpRecvPort +"\"\n");
      sbuf.append(" myAddress=\"" + myIpAddress.getHostAddress() + "\"\n");
      sbuf.append(" myRtpRecvPort=\"" + myRtpRecvPort + "\"\n");
      //sbuf.append(" myRtcpRecvPort=\"" + myRtcpRecvPort + "\"\n");
      sbuf.append("\n/>");
      return sbuf.toString();
   }
}
