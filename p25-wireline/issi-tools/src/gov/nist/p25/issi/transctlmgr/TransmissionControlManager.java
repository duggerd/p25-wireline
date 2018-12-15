//
package gov.nist.p25.issi.transctlmgr;

import gov.nist.p25.issi.rfss.RFSS;
import gov.nist.p25.issi.transctlmgr.ptt.*;

import java.util.Hashtable;
import javax.sdp.MediaDescription;
import javax.sdp.SessionDescription;

import org.apache.log4j.Logger;

/**
 * The Transmission control manager is the main RTP listener as well as the
 * manager of rtp Sessions. When an RTP packet comes in, it finds the
 * appropriate point to multipoint session to forward the packet on.
 * 
 */
public class TransmissionControlManager {

   private static Logger logger = Logger.getLogger(TransmissionControlManager.class);

   private PttManager pttManager;
   private Hashtable<String, PttPointToMultipointSession> pttSessions;
   private boolean isRunning;

   // accessor
   public PttManager getPttManager() {
      return pttManager;
   }

   // constructor
   public TransmissionControlManager(RFSS rfss) throws Exception {
      this.pttSessions = new Hashtable<String, PttPointToMultipointSession>();
      this.pttManager = new PttManager(rfss);
      this.isRunning = true;
   }

   /**
    * get pt to mpt session for a given (uniques) session id. The session id is
    * obtained by concatenating from and to address for pt-to-pt calls
    * 
    * @param sessionId
    * @return
    */
   public PttPointToMultipointSession getPttPointToMultipointSession(
         String sessionId, SessionType sessionType) {

      if (pttSessions.get(sessionId) != null) {
         return pttSessions.get(sessionId);
      }
      else {
         PttPointToMultipointSession retval = new PttPointToMultipointSession(
               this.pttManager, sessionType);
         retval.setSessionId(sessionId);
         pttSessions.put(sessionId, retval);
         return retval;
      }
   }

   public void shutDown() {
      if (!this.isRunning) {
         logger.debug("Session is already shut down");
      }
      for (PttPointToMultipointSession session : this.pttSessions.values()) {
         session.shutDown();
      }

      // Ask PttManager to shutDown the PttSession(25000, 25006)
      pttManager.shutDown();

      isRunning = false;
   }

   @Override
   public String toString() {
      return pttSessions.toString();
   }

   /**
    * Extract parameters from a session description and add a media leg
    * corresponding to that session description.
    * 
    * @param sessionDescription
    * @return
    */
   public PttPointToMultipointSession addMediaLeg(long groupId,
      PttSession pttSession,
      SessionDescription sessionDescription, String mediaLegId) {

      try {
         String ipAddress = sessionDescription.getConnection().getAddress();
         logger.debug("sessionDescription: ipAddress=" + ipAddress);

         // TODO -- we need to sort through different media types here.
         MediaDescription mediaDescription = (MediaDescription) sessionDescription
               .getMediaDescriptions(true).get(0);
         logger.debug("mediaDescription = " + mediaDescription);

         if (mediaDescription.getConnection() != null) {
            ipAddress = mediaDescription.getConnection().getAddress();
            logger.debug("mediaDescription: ipAddress=" + ipAddress);
         }
         int port = mediaDescription.getMedia().getMediaPort();
         if (logger.isDebugEnabled()) {
            //logger.debug("Setting ipAddress : " + ipAddress);
            //logger.debug("Setting port " + port);
            logger.debug("Setting remote ipAddress:port = " + ipAddress +":" +port);
         }

         // The last four hex digits of the radical name are the
         // group ID.
         pttSession.setGroupId((int) (groupId & 0xFFFF));
         pttSession.setRemoteIpAddress(ipAddress);
         pttSession.setRemoteRtpRecvPort(port);
         
         PttPointToMultipointSession pointToMultipointSession =
             pttSession.getPointToMultipointSession();
         pointToMultipointSession.addPeerSession(pttSession,mediaLegId);
         return pointToMultipointSession;

      } catch (Exception ex) {
         logger.error("Could not add media leg for given session description", ex);
         throw new RuntimeException("Unexpected exception !", ex);
      }
   }

   public void teardownRTPPointToMultipointSession(String sessionId) {
      logger.debug("teardownRTPPointToMultipointSession: sessionId="+sessionId);

      // TODO -- close the session.
      PttPointToMultipointSession pttSession = pttSessions.get(sessionId);
      if(pttSession != null) {
         pttSession.shutDown();
      }
      pttSessions.remove(sessionId);
   }
}
