//
package gov.nist.p25.issi.packetmonitor;

//import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.issiconfig.PacketMonitorWebServerAddress;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25payload.ISSIPacketType;
import gov.nist.p25.issi.p25payload.P25BlockException;
import gov.nist.p25.issi.rfss.SipUtils;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfigurationParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Properties;

import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sip.SipFactory;
import javax.sip.header.FromHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.PacketReceiver;
import jpcap.packet.Packet;
import jpcap.packet.UDPPacket;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import org.mortbay.http.HttpContext;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;

/**
 * ISSI packet monitor.
 */
public class PacketMonitor implements PacketReceiver {

   public static Logger logger = Logger.getLogger(PacketMonitor.class);
   static {
      try {
         logger.addAppender(new FileAppender(new SimpleLayout(),
               "logs/packetdebug.txt"));
         logger.setLevel(Level.DEBUG);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   public static void showln(String s) { System.out.println(s); }
   
   private static ISSITesterConfiguration configurations;   
   private static PacketMonitor currentInstance;
   private static NetworkInterface networkInterface;
   private static JpcapCaptor jpcap;
   //private static boolean isEnabled = true;

   // use by performance analysis
   private LinkedList<Object> allMessages;

   private LinkedList<CapturedSipMessage> sipMessages;
   private Hashtable<String, MediaSession> pendingSessions;
   private HashSet<EndPoint> sipAddresses;
   private volatile LinkedList<CapturedPacket> capturedPackets;
   private Hashtable<String, MediaSession> pttSessionsByDestination;

   private Hashtable<String, String> transactionTable;
   private LinkedHashMap<String, LinkedList<CapturedSipMessage>> inviteTransactions;
   private LinkedHashMap<String, LinkedList<CapturedSipMessage>> ackDialog;
   private LinkedHashMap<Integer, CapturedPttMessage> pttSessions;
   private MessageFactory messageFactory;
   private TopologyConfig topologyConfig;

   private static MonitorWrapper monitorWrapper;
   private static PacketAnalyzer packetAnalyzer;
   private static HttpContext httpContext;
   private static Server httpServer;
   private static String testerConfigFile;
   private static Thread monitorThread;
   private static boolean configured;

   int packetNumber;
   private boolean errorFlag;
   private String errorString;
   
   // Keep duplicate IMBE messages
   private boolean keepDuplicateMessage = false;

   // accessor
   public int getPacketNumber() {
      return packetNumber;
   }
   public void setPacketNumber(int packetNumber) {
      this.packetNumber = packetNumber;
   }

   public boolean getKeepDuplicateMessage() {
      return keepDuplicateMessage;
   }
   public void setKeepDuplicateMessage(boolean bflag) {
      keepDuplicateMessage = bflag;
   }

   public Hashtable<String, String> getTransactionTable() {
      return transactionTable;
   }

   public LinkedHashMap<Integer, CapturedPttMessage> getPttSessions() {
      return pttSessions;
   }

   public LinkedHashMap<String, LinkedList<CapturedSipMessage>> getInviteTransactions() {
      return inviteTransactions;
   }
   
   public LinkedHashMap<String, LinkedList<CapturedSipMessage>> getAckDialog() {
      return ackDialog;
   }

   public TopologyConfig getTopologyConfig() {
      return topologyConfig; 
   }
   public void setTopologyConfig(TopologyConfig topologyConfig) {
      //PacketMonitor.topologyConfig = topologyConfig;
      this.topologyConfig = topologyConfig;
   }

   public void logError(String errorString) {
      logger.error( errorString);
      errorFlag = true;
      this.errorString = errorString;
   }
   public void logError(String errorString, Exception ex) {
      logger.error( errorString, ex);
      errorFlag = true;
   }

   public void clearTables()
   {
      errorFlag = false;
      errorString = "";
      allMessages.clear();
      sipMessages.clear();
      transactionTable.clear();
      inviteTransactions.clear();
      ackDialog.clear();
      pttSessionsByDestination.clear();
   }

   /**
   public static void clear() {
      PacketMonitor.monitorWrapper.setActualMonitor( null);
   }
    */

   /**
    * Check the local IP interfaces on the current machine and open the
    * applicable ones for packet capture. This method consults the tester
    * configuration file to see whether a packet monitor should be started on
    * the current machine and opens the applicable interfaces for using pacap.
    * 
    * @param testerConfig
    * @throws Exception
    */
   private static void configureInterfaces(ISSITesterConfiguration testerConfig)
         throws Exception {

      //if (!isEnabled) return;

      jpcap.NetworkInterface[] devices = JpcapCaptor.getDeviceList();

      showln("*************************************************");
      showln("************ AVAILABLE INTERFACES ***************");
      showln("*************************************************");

      for (int i = 0; i < devices.length; i++) {
         showln(i + ": " + devices[i].name + "("
               + devices[i].description + ")");
         showln("\tData link:" + devices[i].datalink_name + "("
               + devices[i].datalink_description + ")");
         showln("\tMAC address:");

         for (byte b : devices[i].mac_address) {
            System.out.print(Integer.toHexString(b & 0xff) + ":");
         }
         System.out.println();

         for (NetworkInterfaceAddress a : devices[i].addresses) {
            showln("\tAddress:" + a.address + " " + a.subnet
                  + " " + a.broadcast);
         }
      }

      PacketMonitorWebServerAddress packetMonitor = null;
      for (jpcap.NetworkInterface device : devices) {
         if (packetMonitor != null)
            break;
         PacketMonitor.networkInterface = device;
         for (int k = 0; k < device.addresses.length; k++) {
            showln("Checking the address " + device.addresses[k].address.getHostAddress());
            if ((packetMonitor = testerConfig.getPacketMonitor(device.addresses[k].address
                        .getHostAddress())) != null)
               break;
         }
      }

      if (packetMonitor == null) {
         showln("Here are the configured addresses ");
         for (PacketMonitorWebServerAddress config : testerConfig.getPacketMonitors()) {
            showln("Host = " + config.getIpAddress());
            showln("Port = " + config.getHttpPort());
         }
         testerConfig.printPacketMonitorTable();
         showln("No Packet Montors found for configuration file "
                     + PacketMonitor.testerConfigFile);
         return;
      }
      if (PacketMonitor.monitorThread == null) {

         jpcap = JpcapCaptor.openDevice(networkInterface, 2000, true, 20);
         monitorWrapper = new MonitorWrapper();
         PacketMonitor.monitorThread = new Thread(new JPCapRunner(
               monitorWrapper, jpcap));
         PacketMonitor.configured = true;
         monitorThread.start();
      }
      Thread.sleep(1000);
   }

   /**
    * Return true if the PacketMonitor is configured.
    * 
    * @return true if the packet monitor is configured.
    */
   public static boolean isConfigured() {
      return PacketMonitor.configured;
   }

   /**
    * Set the unconfigured flag to false and stop the http context to talk to
    * the packet monitor.
    * 
    * @throws Exception
    */
   public static void unconfigure() throws Exception {
      // monitorThread.interrupt();
      // jpcap.close();
      if (PacketMonitor.isConfigured()) {
         PacketMonitor.configured = false;
         httpContext.stop();
         httpServer.removeContext(httpContext);
      }
   }

   // ////////////////////////////////////////////////////////////////////
   // Inner classes
   // ///////////////////////////////////////////////////////////////////
   enum  XPacketType {
      SIPREQUEST, SIPRESPONSE, SIPMESSAGE, PTT, ALLMESSAGE
   }

   // constructor
   public PacketMonitor(TopologyConfig topologyConfig) {

      setTopologyConfig( topologyConfig);
      try {
         this.transactionTable = new Hashtable<String, String>();
         this.inviteTransactions = new LinkedHashMap<String, LinkedList<CapturedSipMessage>>();
         this.ackDialog = new LinkedHashMap<String, LinkedList<CapturedSipMessage>>();
         this.pttSessions = new LinkedHashMap<Integer, CapturedPttMessage>();
         this.pendingSessions = new Hashtable<String, MediaSession>();

         this.allMessages = new LinkedList<Object>();
         this.sipMessages = new LinkedList<CapturedSipMessage>();
         this.sipAddresses = new HashSet<EndPoint>();
         this.capturedPackets = new LinkedList<CapturedPacket>();
         this.pttSessionsByDestination = new Hashtable<String, MediaSession>();
         SipFactory sipFactory = SipFactory.getInstance();
         sipFactory.setPathName("gov.nist");
         messageFactory = sipFactory.createMessageFactory();
         packetAnalyzer = new PacketAnalyzer(this);

         Collection<RfssConfig> rfssCollection = topologyConfig.getRfssConfigurations();
         for (RfssConfig rfssConfig : rfssCollection) {
            String address = rfssConfig.getIpAddress();
            int port = rfssConfig.getSipPort();
            EndPoint hostPort = new EndPoint(address, port);
            sipAddresses.add(hostPort);
         }
         PacketMonitor.currentInstance = this;

      } catch (Exception ex) {
         logger.fatal("Unexpected exception parsing configuraiton file", ex);
         // System.exit(0);
      }
   }

   private static void initializeContexts(Server httpServer) throws Exception {

      //if (!isEnabled) throw new Exception("Packet Monitor is not enabled!");
      //
      PacketMonitor.httpServer = httpServer;
      // PacketMonitor.packetAnalyzer = new PacketAnalyzer(PacketMonitor.currentInstance);
      httpContext = new HttpContext();
      httpContext.setContextPath("/sniffer/*");
      ServletHandler servletHandler = new ServletHandler();
      httpContext.addHandler(servletHandler);
      servletHandler.addServlet("siptrace", "/siptrace", SipTraceGetter.class.getName());
      servletHandler.addServlet("ptttrace", "/ptttrace", PttTraceGetter.class.getName());
      servletHandler.addServlet("result", "/result", ResultGetter.class.getName());
      servletHandler.addServlet("control", "/controller", MonitorController.class.getName());
      httpServer.addContext(httpContext);
      httpContext.start();
   }

   /**
    * Starts the packet monitor. Call this after setting up the web server.
    * 
    */
   public static PacketMonitor startPacketMonitor(TopologyConfig testerConfig)
         throws Exception {

      logger.debug("Starting Packet Monitor ");
      PacketMonitor packetMonitor = new PacketMonitor(testerConfig);
      PacketMonitor.currentInstance = packetMonitor;
      PacketMonitor.monitorWrapper.setActualMonitor( packetMonitor);
      httpContext.setAttribute("packetmonitor", packetMonitor);
      return packetMonitor;
   }
      
   /**
    * This method receives the raw Packet and stores them. At run time, there
    * is no time for packet sorting.
    * 
    */
   public synchronized void receivePacket(Packet packet) {
      if (!PacketMonitor.isConfigured())
         return;
      // Raw capture of packet.
      if (packet instanceof UDPPacket) {
         CapturedPacket capturedPacket = new CapturedPacket( 0, packet);
         capturedPackets.add(capturedPacket);
      }
   }

   /**
    * This method reads from a file and calls Receive packet iteratively.
    * 
    * @param fileName --
    *            the file name from which to read packets.
    */
   public void readTraceFromFile(String fileName) throws Exception {

      logger.debug("PacketMonitor: readTraceFromFile: " + fileName);
      JpcapCaptor captor = JpcapCaptor.openFile(fileName);
      int count = 0;
      Packet packet = captor.getPacket();
      while (packet != Packet.EOF) {
         if (packet instanceof UDPPacket) {
            //logger.debug("got a packet ");
            CapturedPacket capturedPacket = new CapturedPacket(count, packet);
            capturedPackets.add(capturedPacket);
            
            // current TS + captured sec and micro-sec
            //==long ts = capturedPacket.getTimeStamp() + packet.sec*1000 + packet.usec/1000;
            //==capturedPacket.setTimeStamp( ts);
         }
         packet = captor.getPacket();
	 count++;
      }
      logger.debug("PacketMonitor: DONE count="+count);
   }

   /**
    * This method sorts packets. It goes through the list of captured packets
    * pulls out the SIP Packets, identifies the SIP packets and from the SIP
    * packets, it pulls out the media sessions. It then associates the captured
    * PTT packets with the media sessions. This sorting step is done after the
    * packet capture is complete.
    * 
    * @param capturedPaket --
    *            the captured packet to sort.
    * 
    * @param packetType --
    *            the type of packet we are looking for.
    */
   private void sortPacket(CapturedPacket capturedPacket, XPacketType packetType) 
      throws P25BlockException
   {
      long ts = 0;
      if( capturedPackets.get(0) != null) {
         ts = capturedPackets.get(0).getTimeStamp();
      }
      sortPacket( capturedPacket, packetType, ts);
   }

   private synchronized void sortPacket(CapturedPacket capturedPacket,
         XPacketType packetType, long reftime)
      throws P25BlockException
   {
      UDPPacket udpPacket = (UDPPacket) capturedPacket.getPacket();
      InetAddress destInetAddress = udpPacket.dst_ip;
      String host = destInetAddress.getHostAddress();
      int port = udpPacket.dst_port;
      EndPoint packetAddress = new EndPoint(host, port);

      int packetNumber = capturedPacket.getPacketNumber();
      long timeStamp = capturedPacket.getTimeStamp();

      byte[] data = udpPacket.data;
      long delta = timeStamp - reftime;
      logger.debug("sortPacket: " + packetAddress +" " + packetType +" " +
         udpPacket.src_ip.getHostAddress() +":" + udpPacket.src_port +" " +
         timeStamp + " " + delta);

      if (sipAddresses.contains(packetAddress)) {

         String messageString = new String(data);
         try {
            // SIP Responses start with the string SIP
            if ((!messageString.startsWith("SIP")) &&
                  (packetType == XPacketType.SIPREQUEST || 
                   packetType == XPacketType.SIPMESSAGE ||
                   packetType == XPacketType.ALLMESSAGE)) {

               Request request = messageFactory.createRequest(messageString);
               String transactionId = ((ViaHeader) request.getHeader(ViaHeader.NAME)).getBranch();
               String radicalName = ((ViaHeader) (request.getHeader(ViaHeader.NAME))).getHost();
               RfssConfig srcRfssConfig = topologyConfig.getRfssConfig(radicalName);
               if (srcRfssConfig == null) {
                  String msg = "Could not find an RFSS configuration for " + radicalName;
                  logger.debug("getRfssConfig(1): " + msg);
                  //logError(msg);
                  //return;
		  throw new P25BlockException(msg);
               }
               RfssConfig destinationRfss = topologyConfig.getRfssConfig( host, port);
               if (destinationRfss == null) {
                  String msg = "Could not find an RFSS configuration for " +host +":" +port;
                  logger.debug("getRfssConfig(2): " + msg);
                  //logError(msg);
                  //return;
		  throw new P25BlockException(msg);
               }

               CapturedSipMessage captured = new CapturedSipMessage(this,
                     packetAddress, timeStamp, request);
               captured.setPacketNumber( packetNumber);
               if( packetType == XPacketType.ALLMESSAGE)
                  allMessages.add(captured);
               else
                  sipMessages.add(captured);
               logger.debug("         -->" + captured.getShortDescription());

               // compute Dialog ID
               Message message = captured.getMessage();
               String fromTag = ((FromHeader) message.getHeader("From")).getTag();
               String toTag = ((ToHeader) message.getHeader("To")).getTag();
               String callid = ((CallIdHeader) message.getHeader("Call-ID")).getCallId();

               if (Request.INVITE.equals(request.getMethod())) {
                  // record INVITE transactions for measurements
                  if (!inviteTransactions.containsKey(transactionId)) {
                     inviteTransactions.put(transactionId, new LinkedList<CapturedSipMessage>());
                  }
                  inviteTransactions.get(transactionId).add(captured);

                  byte[] contents = request.getRawContent();
                  if (contents == null) {
                     String msg = "Missing content in the INVITE.";
                     logger.debug("getRawContent(1): " + msg);
                     //logError("Missing content in the INVITE");
                     //return;
		     throw new P25BlockException(msg);
                  }
                  SdpFactory sdpFactory = SdpFactory.getInstance();
                  try {
                     ContentTypeHeader ctHeader = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
                     SessionDescription sdpAnnounce = null;
                     if (SipUtils.isAppSdpContentTypeHeader( ctHeader)) {
                        sdpAnnounce = sdpFactory.createSessionDescription(new String(contents));
                     } else {
                        ContentList clist = ContentList.getContentListFromMessage(request);
                        sdpAnnounce = clist.getSdpContent().getSessionDescription();
                     }

                     String ipAddress = sdpAnnounce.getConnection().getAddress();
                     MediaDescription m = (MediaDescription) sdpAnnounce.getMediaDescriptions(true).get(0);
                     if (m.getConnection() != null) {
                        ipAddress = m.getConnection().getAddress();
                     }
                     EndPoint endPoint = new EndPoint(ipAddress, m.getMedia().getMediaPort());

                     MediaSession pendingSession = new MediaSession(this, endPoint);
                     pendingSession.setOwningRfss( destinationRfss);
                     pendingSession.setRemoteRfss( srcRfssConfig);
                     pttSessionsByDestination.put(endPoint.toString(), pendingSession);

                     logger.debug("add PttSession: " + endPoint);
                     pendingSessions.put(transactionId, pendingSession);

                  } catch (Exception ex) {
                     logger.error("Error in parsing sdp ", ex);
                     String msg = "Error in parsing sdp \n" +
                                  "Here is the request [" + messageString + "]";
                     //logError(msg);
                     //return;
		     throw new P25BlockException(msg);
                  }
               } else if (Request.ACK.equals(request.getMethod())) {
                  // record ACK in the dialog
                  String dialogId = callid + fromTag + toTag;
                  if (ackDialog.containsKey(dialogId)) {
                     ackDialog.get(dialogId).add(captured);
                  }
               }
            } else if (messageString.startsWith("SIP") &&
                  (packetType == XPacketType.SIPRESPONSE || 
                   packetType == XPacketType.SIPMESSAGE ||
                   packetType == XPacketType.ALLMESSAGE) ) {

               Response response = messageFactory.createResponse(messageString);
               CapturedSipMessage captured = new CapturedSipMessage(this,
                     packetAddress, timeStamp, response);
               //RfssConfig destinationRfss = topologyConfig.getRfssConfig(host, port);

               logger.debug("         -->" + captured.getShortDescription());
               captured.setPacketNumber( packetNumber);
               if( packetType == XPacketType.ALLMESSAGE)
                  allMessages.add(captured);
               else
                  sipMessages.add(captured);

               // compute Dialog ID
               Message message = captured.getMessage();
               String fromTag = ((FromHeader) message.getHeader("From")).getTag();
               String toTag = ((ToHeader) message.getHeader("To")).getTag();
               String callid = ((CallIdHeader) message.getHeader("Call-ID")).getCallId();

               String method = ((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod();
               if (response.getStatusCode() == Response.OK &&
                   Request.INVITE.equals(method)) {
                  String transactionId = ((ViaHeader) response
                        .getHeader(ViaHeader.NAME)).getBranch();

                  // record INVITE transactions for measurements
                  if (inviteTransactions.containsKey(transactionId)) {
                     inviteTransactions.get(transactionId).add(captured);
                  }

                  // record 200 OK in the dialog
                  String dialogId = callid + fromTag + toTag;
                  if (!ackDialog.containsKey(dialogId)) {
                     ackDialog.put(dialogId, new LinkedList<CapturedSipMessage>());
                  }
                  ackDialog.get(dialogId).add(captured);

                  byte[] contents = response.getRawContent();
                  if (contents == null) {
                     logger.error("Missing content in the 200 OK ");
                     String msg = "Error in parsing sdp \n" +
                           "Here is the response [" + messageString + "]";
                     //logError(msg);
                     //return;
		     throw new P25BlockException(msg);
                  }
                  SdpFactory sdpFactory = SdpFactory.getInstance();
                  try {
                     SessionDescription sdpAnnounce = null;
                     ContentTypeHeader ctHeader = (ContentTypeHeader) response.getHeader(ContentTypeHeader.NAME);
                     if ( SipUtils.isAppSdpContentTypeHeader( ctHeader)) {
                        sdpAnnounce = sdpFactory.createSessionDescription(new String(contents));

                     } else {
                        ContentList contentList = ContentList.getContentListFromMessage(response);
                        sdpAnnounce = contentList.getSdpContent().getSessionDescription();
                     }

                     String ipAddress = sdpAnnounce.getConnection().getAddress();
                     MediaDescription m = (MediaDescription) sdpAnnounce.getMediaDescriptions(true).get(0);
                     if (m.getConnection() != null) {
                        ipAddress = m.getConnection().getAddress();
                     }

                     EndPoint endPoint = new EndPoint(ipAddress, m.getMedia().getMediaPort());
                     MediaSession session = pendingSessions.get(transactionId);
                     if (session != null) {
                        session.setSource(endPoint);
                        MediaSession reversed = session.reverse();
                        reversed.setDestination( endPoint);
                        pttSessionsByDestination.put(endPoint.toString(), reversed);
                        logger.debug("process response: pttSession: " + endPoint);
                     } else {
                        String msg = "Could not find transaction ID " + transactionId;
                        logger.error(msg);
                        logger.error(pendingSessions.keySet());
		        throw new P25BlockException(msg);
                     }

                  } catch (Exception ex) {
                     logger.error("Error in parsing sdp ", ex);
                     String msg = "Here is the response [" + messageString + "]";
                     //logError(msg);
		     throw new P25BlockException(msg);
                  }
               }
            }
         } catch (ParseException ex) {
            logger.error("Error parsing message", ex);
            logger.error("Message : [" + messageString + "]");
         }
      } 
      else if (pttSessionsByDestination.containsKey(packetAddress.toString()) &&
               (packetType == XPacketType.PTT || packetType == XPacketType.ALLMESSAGE)) {

         MediaSession session = pttSessionsByDestination.get(packetAddress.toString());
         try {
            CapturedPttMessage pttMsg = session.addPttMessage(packetNumber, data, timeStamp);

            logger.debug("sortPacket: Adding PTT Packet headed for " + packetAddress + " " +
               pttMsg.getTimeStamp());
            logger.debug("         -->" + pttMsg.getShortDescription());

            if( pttMsg != null && packetType == XPacketType.ALLMESSAGE)
               allMessages.add( pttMsg);

            // store measurement required PTT messages
            int type = pttMsg.getP25Payload().getISSIPacketType().getPT();
            switch (type) {
            case ISSIPacketType.PTT_TRANSMIT_REQUEST:
            case ISSIPacketType.PTT_TRANSMIT_GRANT:
            case ISSIPacketType.PTT_TRANSMIT_START:
            case ISSIPacketType.HEARTBEAT_QUERY :
               if (!pttSessions.containsKey(type)) {
                  pttSessions.put(new Integer(type), pttMsg);
               }
               break;
            case ISSIPacketType.PTT_TRANSMIT_PROGRESS:
               logger.debug("PTT_TRANSMIT_PROGRESS seen");
               break;
            case ISSIPacketType.HEARTBEAT:
               logger.debug("See PTT packet: Heartbeat " + type);
               break;
            default:
               logger.debug("#### Unknown PTT packet type: " + type);
               break;
            }
         }
         catch( P25BlockException ex) {
            logger.debug("CaputredPttMessage: could not extract PTT message...");
            throw ex;
         }
      }  
      else {
         logger.debug("sortPacket: *** Ignoring packet " + packetAddress);
      }
   }

   /**
    * Get the accumlated SIP Messages and clear the SIP message list.
    * 
    * @return
    */
   public synchronized String getSipMessages() {

      logger.debug("========================");
      logger.debug("getSipMessages(): capturedPackets.size=" + capturedPackets.size());
      if (errorFlag) {
         String msg = "Cannot get SIP messages: " + errorString;
         logger.info( msg);
         throw new RuntimeException( msg);
         //return "";
      }
      clearTables();

      for (CapturedPacket packet: capturedPackets) {
         try {
            sortPacket(packet, XPacketType.SIPMESSAGE);
         } catch(P25BlockException ex) {
            logger.debug("getSipMessages(): "+ex);
	 }
      }

      StringBuffer sbuf = new StringBuffer();
      for (CapturedSipMessage message: sipMessages) {
         sbuf.append(message.toString());
      }
      
      // Save to file for xmlbeans
//      try {
//         String xmlStr = "<sipmessages>\n"+sbuf.toString()+"\n</sipmessages>";
//         FileUtility.saveToFile("sipmessages.xml", xmlStr);
//      } catch(IOException ex) { }
      
      logger.debug("getSipMessages(): sipMessages.size=" + sipMessages.size());
      return sbuf.toString();
   }

   public synchronized String getPttMessages() {

      logger.debug("========================");
      logger.debug("getPttMessages(): capturedPackets.size="+capturedPackets.size());
      if (errorFlag) {
         String msg = "Cannot get PTT messages: " + errorString;
         logger.debug( msg);
         throw new RuntimeException(msg);
         //return "";
      }
      clearTables();

      boolean incHex = true;
      StringBuffer sbuf = new StringBuffer();
      for (CapturedPacket packet: capturedPackets) {
         try {
            sortPacket(packet, XPacketType.SIPMESSAGE);
         } catch(P25BlockException ex) {
            logger.debug("getPttMessages(): "+ex);
	 }
      }
      logger.debug("getPttMessages(): pttSessionsByDestionation=\n" + pttSessionsByDestination);
      //logger.debug("getPttMessages(): values=\n" + pttSessionsByDestination.values());

      for (CapturedPacket packet: capturedPackets) {
         try {
            sortPacket(packet, XPacketType.PTT);
         } catch(P25BlockException ex) { }
      }

      for (MediaSession mediaSession: pttSessionsByDestination.values()) {
         for (CapturedPttMessage capturedPacket: mediaSession.getCapturedPttMessages()) {
            sbuf.append(capturedPacket.toString(incHex));
         }
      }
      if (logger.isDebugEnabled())
         logger.debug("Returning " + sbuf);
      
      logger.debug("getPttMessages(): pttSessions.size="+pttSessions.size());
      //showln("getPttMessages(): sbuf=\n"+sbuf.toString());
      return sbuf.toString();
   }

   public synchronized String getAllMessages() {

      logger.debug("========================");
      logger.debug("getAllMessages(): capturedPackets.size=" + capturedPackets.size());

      if (errorFlag) {
         String msg = "Cannot get SIP messages: " + errorString;
         logger.info( msg);
         throw new RuntimeException( msg);
      }
      clearTables();

      for (CapturedPacket packet: capturedPackets) {
         try {
            sortPacket(packet, XPacketType.ALLMESSAGE);
         } catch(P25BlockException ex) { }
      }

      StringBuffer sbuf = new StringBuffer();
      for (Object message: allMessages) {
         sbuf.append(message.toString());
      }
      
      // Save to file for xmlbeans
      /***
      try {
         String xmlStr = "<allmessages>\n"+sbuf.toString()+"\n</allmessages>";
         FileUtility.saveToFile("logs/allmessages.xml", xmlStr);
      } catch(IOException ex) { }
       **/
      
      logger.debug("getAllMessages(): allMessages.size=" + allMessages.size());
      return sbuf.toString();
   }

   //------------------------------------------------------------------------------------------
   public synchronized String getResultTable() {
      logger.debug("getResultTable(): START...");
      if (errorFlag) {
         String msg = "Cannot get result table: " + errorString;
         logger.debug( msg);
         throw new RuntimeException(msg);
         //return "";
      }
      String result = packetAnalyzer.getResultString();
      return result;
   }

   public static int configure(Server httpServer, String configFileName)
         throws Exception {
      logger.info("Configuring Packet Montior");
      PacketMonitor.testerConfigFile = configFileName;
      PacketMonitor.configurations = new ISSITesterConfigurationParser(configFileName).parse();

      int count = 0;
      for (String ipAddress : configurations.getLocalAddresses()) {
         PacketMonitorWebServerAddress wsAddr= getConfigurations().getPacketMonitor(ipAddress);
         if (wsAddr != null) {
            count++;
         }
      }
      if (count != 0) {
         PacketMonitor.configureInterfaces(configurations);
         PacketMonitor.initializeContexts(httpServer);
         PacketMonitor.configured = true;
      }
      return count;
   }

   public static PacketMonitor getCurrentInstance() {
      return currentInstance;
   }

   public static void setConfigurations(ISSITesterConfiguration configurations) {
      PacketMonitor.configurations = configurations;
   }

   public static ISSITesterConfiguration getConfigurations() {
      return configurations;
   }

   public boolean getErrorStatus() {
      return errorFlag;
   }

   public String getErrorMessage() {
      return errorString;
   }

   public void readTraceFromString(String fileName) throws Exception {
      jpcap = JpcapCaptor.openFile(fileName);
      jpcap.processPacket(-1, new MonitorWrapper(this));
   }

   /**
    * Use this to start the packet monitor as a standalone monitoring tool.
    */
   //========================================================================
   public static void main(String[] args) throws Exception {

      PropertyConfigurator.configure("log4j.properties");
      Properties props = new Properties();
      for (int i = 0; i < args.length; i++) {
         if (args[i].equals("-configuration")) {
            testerConfigFile = args[++i];
         } 
         else if (args[i].equals("-startup")) {
            String fileName = args[++i];
            InputStream inStream = new FileInputStream(new File(fileName));
            props.load(inStream);
         }
      }

      testerConfigFile = props.getProperty(
            DietsConfigProperties.DAEMON_CONFIG_PROPERTY, testerConfigFile);

      setConfigurations(new ISSITesterConfigurationParser(testerConfigFile)
            .parse());

      if (configurations == null)
         throw new Exception("missing required parameter -configuration   ");

      Server httpServer = new Server();
      configure(httpServer, testerConfigFile);
      httpServer.start();
   }
}
