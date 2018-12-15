//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.rfss.RFSS;
import gov.nist.rtp.RtpException;
import gov.nist.rtp.RtpManager;
import gov.nist.rtp.RtpSession;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * This class implements the PTT manager. The PTT manager forms the interface
 * between PTT applications and the RTP stack. The PTT manager manages sending
 * and receiving of PTT packets via RTP. The PTT manager is in charge of doling
 * out MMF and SMF machines and associating them with RTP sessions.
 * 
 */
@SuppressWarnings("unused")
public class PttManager {
   
   private static Logger logger = Logger.getLogger(PttManager.class);

   /** The RTP manager. */
   private RtpManager rtpManager = null;

   /** The IP address of this PTT manager. */
   private String myIpAddress;

   /** The owning RFSS. */
   private RFSS rfss = null;

   /** The wide area communication network ID. */
   private int wacnId = 0;

   /** The system ID. */
   private int systemId = 0;

   /** TSNs for Serving to Home RFSS. */
   private int lastServingToHomeTSN = 0;

   /** TSNs for Home to Serving RFSS. */
   private int lastHomeToServingTSN = -1;

   /** TSNs for Calling Home to Called Home RFSS. */
   private int lastCallingHomeToCalledHomeTSN = 0;

   /** TSNs for Called Home to Calling Home RFSS. */
   private int lastCalledHomeToCallingHomeTSN = -1;

   /** TSNs for group serving. */
   private int lastGroupServingTSN = 0;

   /** TSNs for group home. */
   private int lastGroupHomeTSN = -1;

   /** MAX number of ports that can be allocated on this Rfss */
   private int maxPorts;

   /** Current number allocated */
   private int portCount = 0;

   /** A collection of SMFs ( for later retrieval and printing ) */
   private HashSet<SmfSession> smfSessions = new HashSet<SmfSession>();

   /** A collection of MMMFs */
   private HashSet<MmfSession> mmfSessions = new HashSet<MmfSession>();

   /** A collection of RTP session Ids that we own */
   private HashSet<RtpSession> myRtpSessions;

   /** A collection of session multiplexers indexed by session id */
   private Hashtable<String, PttSessionMultiplexer> multiplexers = new Hashtable<String, PttSessionMultiplexer>();


   /**
    * Construct a PTT manager. This constructor is used primarily for testing
    * PTT behavior and does not use typical SMF characteristics such as
    * integration with an RFSS. Using this method results in the turning on of
    * the PTT_TEST_MODE flag in newly created PttSession objects.
    * 
    * @param myIpAddress
    *            The IP address of this PTT manager.
    */
   public PttManager(String myIpAddress) {
      logger.debug("PttManager(ip): myIpAddress="+myIpAddress);
      try {
         this.myIpAddress = myIpAddress;
         rtpManager = new RtpManager(myIpAddress);
      } catch (UnknownHostException uhe) {
         logger.error(uhe);
      }
   }

   /**
    * Construct a PTT manager.
    * 
    * @param rfss
    *            The owning RFSS.
    */
   public PttManager(RFSS rfss) {
      String myIpAddress = rfss.getIpAddress();
      logger.debug("PttManager(rfss): myIpAddress="+myIpAddress);
      try {
         this.myIpAddress = myIpAddress;
         this.rfss = rfss;
         this.wacnId = rfss.getRfssConfig().getWacnId();
         this.systemId = rfss.getRfssConfig().getSystemId();
         rtpManager = new RtpManager(myIpAddress);
         this.maxPorts = rfss.getRfssConfig().getMaxRtpPorts();
         this.myRtpSessions = new HashSet<RtpSession>();
      } catch (UnknownHostException uhe) {
         logger.error(uhe);
      }
   }

   /**
    * Create an SMF session to test SMF behavior. This method is used primarily
    * for testing SMF behavior from a test SMF application. This method does
    * not require typical SMF characteristics such as integration with an RFSS.
    * Using this method results in the turning on of the PTT_TEST_MODE flag in
    * PttSession.
    * 
    * @param myRtpRecvPort
    *            The port for receiving RTP packets.
    * @param remoteIpAddress
    *            The IP address of the remote RFSS.
    * @param remoteRtpRecvPort
    *            The RTP port of the remote RFSS.
    * @param unitID
    *            The SUID.
    * @param linkType
    *            The link type for this RFSS.
    * @throws RtpException
    * @throws IOException
    */
   public SmfSession createTestSmfSession(int myRtpRecvPort,
         String remoteIpAddress, int remoteRtpRecvPort, LinkType linkType)
         throws RtpException, IOException {

      RtpSession rtpSession = rtpManager.createRtpSession(myRtpRecvPort,
            remoteIpAddress, remoteRtpRecvPort);
      this.myRtpSessions.add(rtpSession);
      int transmissionSequenceNumber = getNewTsn(linkType);

      SmfSession smfSession = new SmfSession(rtpSession, linkType,this);
      SmfSession.PTT_TEST_MODE = true;
      this.smfSessions.add(smfSession);
      return smfSession;
   }

   /**
    * Create an SMF session that binds only the RTP receive port and waits
    * indefinitely to receive an RTP packet. <I>When using this method, care
    * must be taken to set the remote IP address and remote RTP receive port on
    * the RtpSession object before calling RtpSession.sendRtpPacket().</I>
    * 
    * @param myRtpRecvPort
    *            The port for receiving RTP packets.
    * @param unitId
    *            The unit ID.
    * @param linkType
    *            The link type for this RFSS.
    * @throws IOException
    * @throws RtpException
    */
   public SmfSession createSmfSession(int myRtpRecvPort, LinkType linkType)
         throws RtpException, IOException {

      RtpSession rtpSession = rtpManager.createRtpSession(myRtpRecvPort);
      this.myRtpSessions.add(rtpSession);
      int transmissionSequenceNumber = getNewTsn(linkType);

      SmfSession smfSession = new SmfSession(rtpSession, linkType, this);
      smfSession.setSystemId(systemId);
      smfSession.setWacnId(wacnId);

      //logger.debug("createSmfSession(1): start HBTransmitter...");
      //smfSession.getHeartbeatTransmitter().start();

      smfSession.setOwningRfss(rfss.getRfssConfig());
      this.smfSessions.add(smfSession);
      return smfSession;
   }

   /**
    * Create a Smfmultiplexer to be used in the group serving. A multiplexer
    * wraps a single RTP session and multiplexes requests to several Smf State
    * machines. A table of multiplexers is maintained here for advanced Rtp
    * resource management (i.e. when rtp resources are released we need to use
    * the same session Id and re-issue the invite ).
    * 
    * @param sdpSessionId
    * @param groupId
    * @return
    * @throws RtpException
    */
   public PttSessionMultiplexer createPttSessionMultiplexer(
         String sdpSessionId, int groupId) throws RtpException {
      RtpSession rtpSession = null;
      logger.debug("createPttSessionMultiplexer : " + sdpSessionId);

      if (this.multiplexers.get(sdpSessionId) != null) {
         return this.multiplexers.get(sdpSessionId);
      }
      if (!this.grabPort()) {
         if (this.rfss.getRfssConfig()
               .isAdvancedRtpResourceManagementSupported()) {
            try {
               rtpSession = rtpManager.createRtpSession(0);
               this.myRtpSessions.add(rtpSession);
            } catch (SocketException ex) {
               throw new RtpException("Unexpected exception ", ex);
            }
         } else {
            throw new RtpException("No rtp resources");
         }
      } else {
         for (int port = 25000; port < 25200; port += 2) {
            try {
               rtpSession = rtpManager.createRtpSession(port);
               this.myRtpSessions.add(rtpSession);
            } catch (Exception ex) {
               continue;
            }
            break;
         }
      }
      if (rtpSession == null)
         throw new RtpException("No port");

      PttSessionMultiplexer mx = new PttSessionMultiplexer(rtpSession, groupId);
      this.multiplexers.put(sdpSessionId, mx);
      return mx;
   }

   /**
    * Create an SMF session that binds only the RTP receive port and waits
    * indefinitely to receive an RTP packet. <I>When using this method, care
    * must be taken to set the remote IP address and remote RTP receive port on
    * the RtpSession object before calling RtpSession.sendRtpPacket().</I>
    * This method is used by the group serving RFSS to create an smf session to
    * be attached to a multiplexer.
    * 
    * @param rtpSession -
    *            a previously created RTP session.
    * @param unitId
    *            The unit ID.
    * @param linkType
    *            The link type for this RFSS.
    * @throws IOException
    * @throws RtpException
    */
   public SmfSession createSmfSession(RtpSession rtpSession, LinkType linkType)
         throws RtpException, IOException {

      int transmissionSequenceNumber = getNewTsn(linkType);

      SmfSession smfSession = new SmfSession(rtpSession, linkType, this);
      smfSession.setSystemId(systemId);
      smfSession.setWacnId(wacnId);

      //logger.debug("createSmfSession(2): start HBTransmitter...");
      //smfSession.getHeartbeatTransmitter().start();

      smfSession.setOwningRfss(rfss.getRfssConfig());
      this.smfSessions.add(smfSession);
      return smfSession;
   }

   /**
    * Create an MMF session to test MMF behavior. This method is used primarily
    * for testing MMF behavior from a test MMF application. This method does
    * not require typical MMF characteristics such as integration with an RFSS.
    * Using this method results in the turning on of the PTT_TEST_MODE flag in
    * PttSession.
    * 
    * @param myRtpRecvPort
    *            The port for receiving RTP packets.
    * @param myRtpRecvTtl
    *            The TTL for receiving RTP packets.
    * @param remoteIpAddress
    *            The IP address of the remote RFSS.
    * @param remoteRtpRecvPort
    *            The RTP port of the remote RFSS.
    * @param unitID
    *            The SUID.
    * @param linkType
    *            The link type for this RFSS.
    * @throws RtpException
    * @throws IOException
    */
   public MmfSession createTestMmfSession(int myRtpRecvPort,
         String remoteIpAddress, int remoteRtpRecvPort, LinkType linkType,
         HeartbeatListener listener) throws RtpException, IOException {

      RtpSession rtpSession = rtpManager.createRtpSession(myRtpRecvPort,
            remoteIpAddress, remoteRtpRecvPort);

      this.myRtpSessions.add(rtpSession);
      int transmissionSequenceNumber = getNewTsn(linkType);

      MmfSession mmfSession = new MmfSession(rtpSession, linkType, this);
      mmfSession.getHeartbeatReceiver().setHeartbeatListener(listener);
      mmfSession.getHeartbeatTransmitter().start();
      PttSession.PTT_TEST_MODE = true;

      this.mmfSessions.add(mmfSession);
      return mmfSession;
   }

   /**
    * Create an MMF session that binds only the RTP receive port and waits
    * indefinitely to receive an RTP packet. <I>When using this method, care
    * must be taken to manually set the remote IP address and remote RTP
    * receive port on the RtpSession object before calling
    * RtpSession.sendRtpPacket().</I>
    * 
    * @param myRtpRecvPort
    *            The port for receiving RTP packets.
    * @param unitID
    *            The SUID.
    * @param linkType
    *            The link type for this RFSS.
    * @throws IOException
    * @throws RtpException
    * 
    */
   public MmfSession createMmfSession(int myRtpRecvPort, LinkType linkType)
         throws RtpException, IOException {

      RtpSession rtpSession = rtpManager.createRtpSession(myRtpRecvPort);
      this.myRtpSessions.add(rtpSession);

      int transmissionSequenceNumber = getNewTsn(linkType);

      MmfSession mmfSession = new MmfSession(rtpSession, linkType,this);
      mmfSession.setSystemId(systemId);
      mmfSession.setWacnId(wacnId);
      mmfSession.setOwningRfss(rfss.getRfssConfig());
      this.mmfSessions.add(mmfSession);
      return mmfSession;
   }

   /**
    * Get my assigned IP Address.
    */
   public String getIpAddress() {
      return myIpAddress;
   }

   /**
    * This method generates a new TSN based on the RFSS's link type.
    * 
    * @param linkType --
    *            The link type for this RFSS.
    * @return The current TSN.
    */
   // TODO (steveq): Make sure that these do not conflict with
   // response TSNs (which have the same TSNs as the request).
   public int getNewTsn(LinkType linkType) {

      int tsn = -1;
      if (linkType == LinkType.UNIT_TO_UNIT_CALLED_SERVING_TO_CALLED_HOME
            || linkType == LinkType.UNIT_TO_UNIT_CALLING_SERVING_TO_CALLING_HOME) {
         tsn = generateTsn(lastServingToHomeTSN);
         lastServingToHomeTSN = tsn;

      } else if (linkType == LinkType.UNIT_TO_UNIT_CALLED_HOME_TO_CALLED_SERVING
            || linkType == LinkType.UNIT_TO_UNIT_CALLING_HOME_TO_CALLING_SERVING) {
         tsn = generateTsn(lastHomeToServingTSN);
         lastHomeToServingTSN = tsn;

      } else if (linkType == LinkType.UNIT_TO_UNIT_CALLING_HOME_TO_CALLED_HOME) {
         tsn = generateTsn(lastCallingHomeToCalledHomeTSN);
         lastCallingHomeToCalledHomeTSN = tsn;

      } else if (linkType == LinkType.UNIT_TO_UNIT_CALLED_HOME_TO_CALLING_HOME) {
         tsn = generateTsn(lastCalledHomeToCallingHomeTSN);
         lastCalledHomeToCallingHomeTSN = tsn;

      } else if (linkType == LinkType.GROUP_SERVING) {
         // Added this case @mranga
         tsn = generateTsn(lastGroupServingTSN);
         lastGroupServingTSN = tsn;

      } else if (linkType == LinkType.GROUP_HOME) {
         // Added @mranga
         tsn = generateTsn(lastGroupHomeTSN);
         lastGroupHomeTSN = tsn;

      } else {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.error(this + " Unknown link type: " + linkType);
      }
      return tsn;
   }

   /**
    * Generate a TSN. Note that we do NOT recycle TSNs yet. Thus, we have up to
    * 31 SMF and 32 MMF TSNs available.
    * 
    * @return int The generated TSN (-1 if no TSNs are available).
    */
   // TODO (steveq): Modify to handle recycling of TSNs
   public int generateTsn(int lastTsn) {

      int newTsn = (lastTsn + 2) % 64;
      if (newTsn == 0) // 0 is reserved for HEARTBEATS/HEARBEAT QUERIES
         newTsn = (lastTsn + 4) % 64;

      return newTsn;

      // // The following uses HashSet to store active TSNs. We need mods
      // // to recycle TSNs.
      // int oldTsn = lastTsn;
      // int newTsn = -1;
      //      
      // for (;;) {
      //         
      // if (hashSet.size() <= 64) { // 64 odd or 64 even TNSs available
      //            
      // newTsn = (oldTsn + 2) % 64;
      //            
      // if (newTsn == 0) // TSN=0 reserved for HEARTBEATs
      // newTsn += 2;
      //            
      // Integer i = new Integer(newTsn);
      //            
      // if (! hashSet.contains(i)) {
      //               
      // hashSet.add(i);
      // break;
      //               
      // } else {
      //
      // // TSN is already active so increment and try again
      // oldTsn += 2;
      //               
      // }
      //            
      // } else if (hashSet.size() > 64) {
      //               
      // // We have tested all values, so just FAIL
      // if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
      // logger.debug("[PttManager.generateTsn()]: " +
      // " FAIL: No more TSNs available.");
      //               
      // break;
      //               
      // }
      //
      // }
      //      
      // return newTsn;
   }

   /**
    * This method releases a TSN (i.e., removes from memory).
    * 
    * @param linkType --
    *            The link type for this RFSS.
    * @return The current TSN.
    */
   // TODO This method will be moved to PttSession
   // private int releaseTsn(int Tsn, LinkType linkType) {
   // }
   public HashSet<MmfSession> getMmfSessions() {
      return this.mmfSessions;
   }

   public HashSet<SmfSession> getSmfSessions() {
      return this.smfSessions;
   }

   public String getSessionDescriptions() {
      StringBuffer sbuf = new StringBuffer();
      for (PttSessionInterface mmfSession : this.mmfSessions) {
         sbuf.append(mmfSession.toString()).append("\n");
      }
      for (SmfSession smfSession : this.smfSessions) {
         sbuf.append(smfSession.toString()).append("\n");
      }
      for (PttSessionMultiplexer muxSession : this.multiplexers.values()) {
         sbuf.append(muxSession.toString()).append("\n");
      }
      return sbuf.toString();
   }

   public synchronized boolean grabPort() {
      if (this.maxPorts == -1)
         return true;
      if (this.portCount >= this.maxPorts)
         return false;
      else {
         this.portCount++;
         return true;
      }
   }

   /**
    * add to the max port limit. This method is needed for simulating 
    * advanced RTP mechanisms.
    * 
    * @param nports
    */
   public void incrementPortLimit(int nports) {
      if (this.maxPorts != -1) {
         this.maxPorts += nports;
      }
   }

   public RFSS getRfss() {
      return this.rfss;
   }

   /**
    * This method is called when we are logging a packet. It prevents the
    * logging of self routed packets.
    * 
    * @param rtpSession -- session to check.
    * @return true if we own the remote rtp session.
    */
   public boolean ownsRemoteRtpSession(RtpSession rtpSession) {
      if (logger.isDebugEnabled()) {
         logger.debug("ownsRemoteRtpSession: RTP SESSIONS = " + myRtpSessions);
         logger.debug("ownsRemoteRtpSession: Checking rtpSession = "
               + rtpSession.getRemoteIpAddress() + ":"
               + rtpSession.getRemoteRtpRecvPort());
      }
      for (RtpSession session : this.myRtpSessions) {
         if (rtpSession.getRemoteRtpRecvPort() == session.getMyRtpRecvPort()
               && rtpSession.getRemoteIpAddress().equals(
                     session.getMyIpAddress().getHostAddress())) {
            logger.debug(" --- matched ipAddress="+rtpSession.getRemoteIpAddress());
            return true;
         }
      }
      return false;
   }

   public void shutDown() {
      // need to shutdown the RTP(25000, 25006)
      for (RtpSession rtpSession : this.myRtpSessions) {
         logger.debug("PttManager: shutDown(): " + rtpSession.toString());
         rtpSession.shutDown();
	 /**
         if(rtpSession.getRemoteIpAddress().equals(
            rtpSession.getMyIpAddress().getHostAddress())) {
            rtpSession.shutDown();
         }
	  **/
      }
   }

   // 14.4.x
   //---------------------------------------------------------------
   boolean mask1 = false;
   boolean mask2 = false;
   boolean mask3 = false;

   public void sendHeartbeatQuery() {

      LinkType linkType;
      logger.debug("PttManager: sendHeartbeatQuery(): START");
      /***
      for (PttSessionInterface mmfSession : mmfSessions) {
         linkType = mmfSession.getLinkType();
         if(linkType == LinkType.UNIT_TO_UNIT_CALLING_SERVING_TO_CALLING_HOME ||
            linkType == LinkType.UNIT_TO_UNIT_CALLING_HOME_TO_CALLED_HOME ||
            linkType == LinkType.UNIT_TO_UNIT_CALLED_SERVING_TO_CALLED_HOME)
         {
            logger.debug("---sendHeartbeatQuery(): mmfSession="+mmfSession);
            mmfSession.getHeartbeatTransmitter().sendHeartbeatQuery();
         }
      }
       ***/

      for (SmfSession smfSession : smfSessions) {
         logger.debug("---sendHeartbeatQuery(): "+myIpAddress+" mode="+smfSession.PTT_TEST_MODE);
         if (smfSession.PTT_TEST_MODE) {
            continue;
         }
         linkType = smfSession.getLinkType();
         if(!mask1 && linkType == LinkType.UNIT_TO_UNIT_CALLING_SERVING_TO_CALLING_HOME) {
            logger.debug("---sendHeartbeatQuery(1): smfSession="+smfSession);
            smfSession.getHeartbeatTransmitter().sendHeartbeatQuery();
            mask1 = true;
         }
         if(!mask2 && linkType == LinkType.UNIT_TO_UNIT_CALLING_HOME_TO_CALLED_HOME) {
            logger.debug("---sendHeartbeatQuery(2): smfSession="+smfSession);
            smfSession.getHeartbeatTransmitter().sendHeartbeatQuery();
            mask2 = true;
         }
         if(!mask3 && linkType == LinkType.UNIT_TO_UNIT_CALLED_SERVING_TO_CALLED_HOME) {
            logger.debug("---sendHeartbeatQuery(3): smfSession="+smfSession);
            smfSession.getHeartbeatTransmitter().sendHeartbeatQuery();
            mask3 = true;
         }
      }

      /***
      for (PttSessionMultiplexer muxSession : multiplexers.values()) {
         logger.debug("---sendHeartbeatQuery(): muxSession="+muxSession);
         try {
            muxSession.sendHeartbeatQuery();
         } catch (Exception ex) {
            logger.debug("sendHeartbeatQuery(): muxSession="+ex);
         }
      }
       ***/
      logger.debug("PttManager: sendHeartbeatQuery(): DONE");
   }
}
