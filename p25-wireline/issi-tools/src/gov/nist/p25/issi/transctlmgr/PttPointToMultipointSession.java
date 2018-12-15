//
package gov.nist.p25.issi.transctlmgr;

import gov.nist.p25.issi.p25payload.*;
import gov.nist.p25.issi.transctlmgr.ptt.*;
import gov.nist.rtp.RtpException;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * This class encapsulates a set of PTT sessions Data comes in on one of the
 * sessions and is forwarded out on all the other sessions.
 * 
 */
public class PttPointToMultipointSession implements SmfRxListener,
      MmfRxListener, SmfTxListener, MuteListener {
   
   private static Logger logger = Logger.getLogger(PttPointToMultipointSession.class);

   /**
    * A set of PTT (SMF or MMF) sessions - for point to point - theres only one
    * element in this set.
    */
   private Hashtable<String, PttSession> peerSessions;
   private TransmitPriorityType transmitPriority;
   private int priorityLevel;
   private PttManager pttManager;

   // The sesion id is the rfss id.
   private String sessionId;

   private boolean isRunning;
   private SessionType sessionType;
   private Hashtable<String, SmfSession> smfSessions;
   private Hashtable<String, MmfSession> mmfSessions;

   // current tsn for all the Mmf sessions we are managing.
   private Hashtable<MmfSession, Integer> activePeersTsnTable;
   private Hashtable<MmfSession, HashSet<PttMuteRecord>> mutedMmfsTsnTable;
   private Hashtable<SmfSession, PttMuteRecord> mutedSmfsTsnTable;
   private TreeSet<PttMuteRecord> mutedSet;


   class PttMuteRecord implements Comparable<PttMuteRecord> {

      int tsn;
      int priorityLevel;
      PttSession mutedSession;
      TransmitPriorityType priorityType;

      public PttMuteRecord(PttSession mutedSession, int tsn,
            TransmitPriorityType priorityType, int priorityLevel) {
         this.mutedSession = mutedSession;
         this.tsn = tsn;
         this.priorityType = priorityType;
         this.priorityLevel = priorityLevel;
      }

      @Override
      public boolean equals(Object other) {
         PttMuteRecord that = (PttMuteRecord) other;
         return this.mutedSession == that.mutedSession
               && this.tsn == that.tsn;
      }

      public int compareTo(PttMuteRecord that) {
         TransmitPriorityType ptype = that.priorityType;
         int pl = that.priorityLevel;
         int tp = (int) (ptype.intValue() & 0x0F);
         tp <<= 0x04;
         tp |= (pl & 0x0F);

         int cp = (int) this.priorityType.intValue() & 0x0f;
         cp <<= 0x04;
         cp |= (this.priorityLevel & 0x0f);
         int retval = cp == tp ? 0 : cp > tp ? -1 : 1;
         logger.debug("isCurrentPriorityHigher " + retval + " ptype = "
                     + ptype + " pl = " + pl + " this.priority = "
                     + this.priorityType + " this.level = "
                     + this.priorityLevel);
         return retval;
      }
   }

   // constructor
   PttPointToMultipointSession(PttManager pttManager, SessionType sessionType) {
      this.sessionType = sessionType;
      this.peerSessions = new Hashtable<String, PttSession>();
      this.smfSessions = new Hashtable<String, SmfSession>();
      this.mmfSessions = new Hashtable<String, MmfSession>();
      this.activePeersTsnTable = new Hashtable<MmfSession, Integer>();
      this.mutedMmfsTsnTable = new Hashtable<MmfSession, HashSet<PttMuteRecord>>();
      this.mutedSmfsTsnTable = new Hashtable<SmfSession, PttMuteRecord>();
      this.mutedSet = new TreeSet<PttMuteRecord>();
      this.pttManager = pttManager;
      this.isRunning = true;
   }

   /**
    * Compare the given priority to the current highest stream priority. If the
    * given priority of the incoming packet is higher then return +1. If equals
    * then return 0. If the current priority is higher then return -1.
    */
   private int compareToCurrentPriority(TransmitPriorityType ptype, int pl) {
      if (this.transmitPriority == null)
         return 1;
      int tp = (int) (ptype.intValue() & 0x0F);
      tp <<= 0x04;
      tp |= (pl & 0x0F);

      int cp = (int) this.transmitPriority.intValue() & 0x0f;
      cp <<= 0x04;
      cp |= (this.priorityLevel & 0x0f);
      int retval = cp > tp ? -1 : 0;
      logger.debug("isCurrentPriorityHigher " + retval + " ptype = " + ptype
            + " pl = " + pl + " this.priority = " + this.transmitPriority
            + " this.level = " + this.priorityLevel);
      return retval;
   }

   /**
    * Return true if the current MMF is muted.
    */
   private boolean isSessionMuted(PttSessionInterface mmfSession, int tsn) {
      if (mutedMmfsTsnTable.containsKey(mmfSession)) {
         return mutedMmfsTsnTable.get(mmfSession).contains(tsn);
      } else
         return false;
   }

   /**
    * Return true if smf session is muted.
    */
   private boolean isSessionMuted(SmfSession smfSession, int tsn) {
      if (mutedSmfsTsnTable.containsKey(smfSession)) {
         return mutedSmfsTsnTable.get(smfSession).tsn == tsn;
      } else
         return false;
   }

   private void addTsnToMutedSet(SmfSession smfSession, int tsn) {
      PttMuteRecord muteRecord = new PttMuteRecord(smfSession, tsn,
            this.transmitPriority, this.priorityLevel);
      mutedSmfsTsnTable.put(smfSession, muteRecord);
      mutedSet.add(muteRecord);
   }

   private void addTsnToMutedSet(MmfSession mmfSession, int tsn) {
      HashSet<PttMuteRecord> hset = mutedMmfsTsnTable.get(mmfSession);
      PttMuteRecord pttMuteRecord = new PttMuteRecord(mmfSession, tsn,
            this.transmitPriority, this.priorityLevel);
      if (hset == null) {
         hset = new HashSet<PttMuteRecord>();
         mutedMmfsTsnTable.put(mmfSession, hset);
      }
      hset.add(pttMuteRecord);
      mutedSet.add(pttMuteRecord);
   }

   private void removeTsnFromMutedSet(PttSession pttSession, int tsn) {
      if (pttSession instanceof MmfSession) {
         PttSessionInterface mmfSession = (PttSessionInterface) pttSession;
         if (mutedMmfsTsnTable.containsKey(mmfSession)) {
            HashSet<PttMuteRecord> hset = mutedMmfsTsnTable.get(mmfSession);
            PttMuteRecord found = null;
            for (PttMuteRecord record : hset) {
               if (record.tsn == tsn) {
                  found = record;
                  break;
               }
            }
            if (found != null) {
               hset.remove(found);
               mutedSet.remove(found);
            }
            if (hset.isEmpty()) {
               mutedMmfsTsnTable.remove(mmfSession);
            }
         }
      } else {
         SmfSession smfSession = (SmfSession) pttSession;
         if (mutedSmfsTsnTable.containsKey(smfSession)) {
            PttMuteRecord muteRecord = mutedSmfsTsnTable.get(smfSession);
            mutedSet.remove(muteRecord);
            mutedMmfsTsnTable.remove(smfSession);
         }
      }
   }

   /**
    * Create an SMF session and start listening for incoming packets.
    * 
    * @throws IOException
    */
   public SmfSession createSmfSession(LinkType linkType, String sdpSessId)
         throws RtpException, IOException {

      logger.debug("createSmfSession : sdpSessId = " + sdpSessId);
      if (smfSessions.get(sdpSessId) != null) {
         return smfSessions.get(sdpSessId);

      } else if (!pttManager.grabPort()) {
         // If we cannot find a port, then assign a send only session ( no
         // receiver ) until port is available.
         if (pttManager.getRfss().getRfssConfig()
               .isAdvancedRtpResourceManagementSupported()) {

            // create a transmit only Smf session.
            SmfSession smfSession = pttManager.createSmfSession(0, linkType);
            smfSession.setPointToMultipointSession(this);
            smfSession.getSmfReceiver().addListener(this);
            smfSession.getSmfTransmitter().addListener(this);

            smfSession.setOwningRfss(pttManager.getRfss().getRfssConfig());
            smfSessions.put(sdpSessId, smfSession);
            return smfSession;
         } else
            throw new RtpException("No Rtp Resources!");
      }

      SmfSession smfSession = null;

      // Generate an even RTP receive port above 4000
      // int rand = getEvenRandom();
      int rand = 25000;

      // Try incrementing port by two until SMF session is created
      boolean sessionCreated = false;

      for (int i = 0; i < 100 && !sessionCreated; i++) {
         try {
            smfSession = pttManager.createSmfSession(rand, linkType);
            sessionCreated = true;
         } catch (SocketException se) {
            // rtpSession could not bind to a RTP receive socket,
            // so generate new port number and try again.
            rand += 2;
         }
      }

      if (!sessionCreated)
         throw new RtpException("Cannot create Session - tried for 100 ports and failed!");

      if (logger.isDebugEnabled())
         logger.debug("[PttPointToMultipoint] PTT/RTP receive port is: "
               + smfSession.getMyRtpRecvPort());

      smfSession.getSmfReceiver().addListener(this);
      smfSession.getSmfTransmitter().addListener(this);

      // set the back pointer from ptt session to myself.
      smfSession.setPointToMultipointSession(this);
      smfSessions.put(sdpSessId, smfSession);
      return smfSession;
   }

   /**
    * Create a MMF session and start listening for incoming packets.
    */
   public MmfSession createMmfSession(LinkType linkType, String sessionId)
         throws RtpException, IOException {

      if (mmfSessions.get(sessionId) != null) {
         logger.debug("Returning an existing mmf Session for " + sessionId);
         return mmfSessions.get(sessionId);
      } else if (!pttManager.grabPort()) {
         if (pttManager.getRfss().getRfssConfig()
               .isAdvancedRtpResourceManagementSupported()) {
            // create a transmit only session.
            MmfSession mmfSession = pttManager.createMmfSession(0, linkType);
            mmfSession.setPointToMultipointSession(this);
            mmfSession.setOwningRfss(pttManager.getRfss().getRfssConfig());
            mmfSession.getMmfReceiver().addListener(this);
            mmfSessions.put(sessionId, mmfSession);
            return mmfSession;
         } else
            throw new RtpException("No Rtp resources!");
      }

      MmfSession mmfSession = null;

      // Generate an even RTP receive port above 4000
      // int rand = getEvenRandom();
      // Hard code starting point just for ease.
      int rand = 25000;
      boolean sessionCreated = false;

      // Try incrementing port by two until SMF session is created
      for (int i = 0; i < 100 && !sessionCreated; i++) {
         try {
            mmfSession = pttManager.createMmfSession(rand, linkType);
            mmfSession.setPointToMultipointSession(this);
            sessionCreated = true;
         } catch (SocketException se) {
            // rtpSession could not bind to a RTP receive socket,
            // so generate new port number and try again.
            rand += 2;
         }
      }
      if (!sessionCreated)
         throw new RtpException("No ports available ");

      if (logger.isDebugEnabled())
         logger.debug("[PttPointToMultipoint] PTT/RTP receive port is: "
               + mmfSession.getMyRtpRecvPort());
      mmfSession.getMmfReceiver().addListener(this);
      mmfSession.getMmfReceiver().addListener(this);
      mmfSessions.put(sessionId, mmfSession);
      return mmfSession;
   }

   /**
    * Get the session type.
    * 
    * @return the session type associated with this ptmp session.
    */
   public SessionType getSessionType() {
      return this.sessionType;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {

      StringBuffer sb = new StringBuffer();
      sb.append("<ptt-point-to-multipoint-session\n");
      sb.append("sessionId = \"" + sessionId + "\"\n");
      sb.append("sessionType = \"" + this.sessionType + "\"\n>");

      for (PttSession pttSession : peerSessions.values()) {
         sb.append(pttSession.toString());
         sb.append("\n");
      }
      sb.append("\n<ptt-point-to-multipoint-session>");
      return sb.toString();
   }

   /**
    * Shutdown all the sessions of this PttSession.
    * 
    */
   public void shutDown() {
      if (!isRunning) {
         logger.debug("Session is already shut down ");
         return;
      }
      logger.debug("Shutting down " + this.toString());
      for (PttSession pttSession : peerSessions.values()) {
         pttSession.shutDown();
      }
      this.isRunning = false;
   }

   public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
   }
   
   public void printPeerTable() {
      if ( logger.isDebugEnabled()) {
         logger.debug("PEER SESSIONS \n" + peerSessions);
      }
   }

   public void addPeerSession(PttSession pttSession, String rfssDomainName) {
      if (logger.isDebugEnabled())
         logger.debug("pttPointToMultipointSession : addPttSession : " + rfssDomainName);
   
      // The Peer ID is the RFSS ID to which this leg of the media belongs.
      peerSessions.put(rfssDomainName, pttSession);
   }

   public PttSession getPttSession(String rfssDomainName) {
      logger.debug("PttPointToMultipointSession : getPttSession : " + rfssDomainName);
      return peerSessions.get(rfssDomainName);
   }

   public MmfSession getMmfSession(String sessId) {
      return mmfSessions.get(sessId);
   }

   public SmfSession getSmfSession(String sessId) {
      return smfSessions.get(sessId);
   }

   /***************************************************************************
    * SmfTxListener Methods
    **************************************************************************/
   public void receivedPttGrant(SmfPacketEvent event) {

      SmfSession mySession = event.getSmfSession();
      for (PttSession pttSession : peerSessions.values()) {
         if (pttSession != mySession) {
            try {
               if (pttSession instanceof MmfSession) {
                  MmfSession mmfSession = (MmfSession) pttSession;
                  mmfSession.getMmfReceiver().setArbitrateGrant();
                  mmfSession.getMmfReceiver().arbitrate(event);
               }
            } catch (Exception ex) {
               ex.printStackTrace();
            }
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.transctlmgr.ptt.SmfControlListener#denyInd(gov.nist.p25.issi.transctlmgr.ptt.SmfPacketEvent)
    */
   public void receivedPttDeny(SmfPacketEvent event) {
      SmfSession mySession = event.getSmfSession();
      for (PttSession pttSession : peerSessions.values()) {
         if (pttSession != mySession) {
            try {
               if (pttSession instanceof MmfSession) {
                  MmfSession mmfSession = (MmfSession) pttSession;
                  mmfSession.getMmfReceiver().setArbitrateDeny();
                  mmfSession.getMmfReceiver().arbitrate(event);
               }
            } catch (Exception ex) {
               ex.printStackTrace();
            }
         }
      }
   }

   public void receivedPttWait(SmfPacketEvent event) {
      // What to do here?
   }

   public void requestTimeout() {
      logger.debug("[PttPointToMultipoint] Got SMF Request Timeout.");
   }

   public void waitTimeout() {
      logger.debug("[PttPointToMultipoint] Got SMF Wait Timeout.");
   }

   /***************************************************************************
    * SmfRxListener Methods
    **************************************************************************/
   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.transctlmgr.ptt.AudioListener#spurtStartInd(gov.nist.p25.issi.transctlmgr.ptt.PttEvent)
    */
   public void receivedPttStart(SmfPacketEvent event) {
      try {
         P25Payload pttPacket = event.getPttPacket();
         SmfSession mySession = (SmfSession) event.getSource();
         int tsn = event.getPttPacket().getISSIPacketType().getTransmissionSequenceNumber();

         if (isSessionMuted(mySession, tsn)) {
            logger.debug("received a start while in the muted state. TSN "+tsn);
            mySession.sendMute(tsn);
            return;

         }
         // Saw a packet with losing audio. Mute the sender and throw into
         // the mute set.
         if (pttPacket.getISSIPacketType().getLosingAudio()
               && mySession.isNotInterestedInLosingAudio()) {
            logger.debug("Got a packet with a losing audio. TSN "+tsn);
            addTsnToMutedSet(mySession, tsn);
            mySession.sendMute(tsn);
         }

         logger.debug("Got a spurt start packet -- forwarding");
         int unitId = event.getPttPacket().getPTTControlWord().getUnitId();
         int systemId  = event.getPttPacket().getPTTControlWord().getSystemId();
         TransmitPriorityType ptype = pttPacket.getPTTControlWord().getTransmitPriority();
         int plevel = pttPacket.getPTTControlWord().getPriorityLevel();

         for (PttSession pttSession : peerSessions.values()) {
            if (pttSession != mySession) {
               if (pttSession instanceof MmfSession) {
                  MmfSession mmfSession = (MmfSession) pttSession;
                  if (mmfSession.getMmfTransmitter().getCurrentState(tsn) == MmfTxState.BEGIN)
                     mmfSession.getMmfTransmitter().sendSpurtStart( systemId, unitId, tsn, ptype, plevel);
               }
            }
         }
      } catch (Exception ex) {
         logger.fatal("*Unexpected exception processing mesage", ex);
         throw new RuntimeException("Unexpected exception", ex);
      }
   }

   /**
    * Callback for processing an incoming ptt progress packet.
    * 
    * @event -- the smf packet event.
    */
   public void receivedPttProgress(SmfPacketEvent event) {
      try {
         SmfSession mySession = (SmfSession) event.getSource();
         P25Payload pttPacket = event.getPttPacket();
         int tsn = pttPacket.getISSIPacketType().getTransmissionSequenceNumber();
         //boolean losingAudio = pttPacket.getISSIPacketType().getLosingAudio();

         if (this.isSessionMuted(mySession, tsn)) {
            logger.debug("received a start while in the muted state. TSN "+tsn);
            mySession.sendMute(tsn);
            return;
         }

         // Saw a packet with losing audio. If I do not want to
         // process packets with losing audio flag set, then mute the sender
         // and throw into the mute set.
         if (pttPacket.getISSIPacketType().getLosingAudio()
               && mySession.isNotInterestedInLosingAudio()) {
            logger.debug("Got a packet with a losing audio. TSN "+tsn);
            addTsnToMutedSet(mySession, tsn);
            // mutedSet.add(mySession);
            mySession.sendMute(tsn);
            return;
         }

         IMBEVoiceBlock[] imbeVoices = pttPacket.getIMBEVoiceBlockArray();
         int unitId = pttPacket.getPTTControlWord().getUnitId();
         int systemId = pttPacket.getPTTControlWord().getSystemId();
         TransmitPriorityType ptype = pttPacket.getPTTControlWord().getTransmitPriority();
         int plevel = pttPacket.getPTTControlWord().getPriorityLevel();
         logger.debug("Got an audio packet -- forwarding");

         for (PttSession pttSession : peerSessions.values()) {
            if (pttSession != mySession) {
               if (pttSession instanceof MmfSession) {
                  MmfSession mmfSession = (MmfSession) pttSession;
                  if (mmfSession.getMmfTransmitter().getCurrentState(tsn) == MmfTxState.BEGIN)
                     mmfSession.getMmfTransmitter().sendSpurtStart(
                           systemId,unitId, tsn, ptype, plevel);
                  mmfSession.getMmfTransmitter().sendVoice(imbeVoices,
                        systemId,unitId, tsn, ptype, plevel);

               } else if (pttSession instanceof SmfSession) {

                  SmfSession smfSession = (SmfSession) pttSession;
                  smfSession.getSmfTransmitter().forwardPttProgress(
                        imbeVoices,systemId, unitId, tsn, ptype, plevel);
               }
            }
         }
      } catch (Exception ex) {

         logger.fatal("Unexpected exception", ex);
         throw new RuntimeException("Unexpected exception", ex);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.transctlmgr.ptt.AudioListener#spurtEndInd(gov.nist.p25.issi.transctlmgr.ptt.PttEvent)
    */
   public void receivedPttEnd(SmfPacketEvent event) {
      PttSession mySession = (PttSession) event.getSource();
      logger.debug("Got a spurt end notification -- forwarding ");
      int tsn = event.getPttPacket().getISSIPacketType().getTransmissionSequenceNumber();

      for (PttSession pttSession : peerSessions.values()) {
         if (pttSession != mySession) {
            try {
               if (pttSession instanceof MmfSession) {
                  MmfSession mmfSession = (MmfSession) pttSession;
                  mmfSession.getMmfTransmitter().sendSpurtEndNotification(tsn);
               } else if (pttSession instanceof SmfSession) {
                  SmfSession smfSession = (SmfSession) pttSession;
                  smfSession.getSmfTransmitter().forwardPttEnd();
               }
            } catch (Exception ex) {
               ex.printStackTrace();
            }
         }
      }
   }

   /***************************************************************************
    * MmfRxListener Methods
    **************************************************************************/
   public void receivedPttRequest(MmfPacketEvent event) {
      // If this is a point to point call, the spurt request is forwarded
      // at calling home.
      logger.debug("Got a spurtRqstInd " + this.sessionType);
      MmfSession mySession = event.getMmfSession();
      P25Payload pttPacket = event.getPttPacket();
      int unitId = event.getPttPacket().getPTTControlWord().getUnitId();
      int systemId  = event.getPttPacket().getPTTControlWord().getSystemId();
      int tsn = pttPacket.getISSIPacketType().getTransmissionSequenceNumber();
      try {
         if (!mySession.getOwningRfss().isUnitKnown(unitId)) {
            mySession.getMmfReceiver().setArbitrateDeny();
            mySession.getMmfReceiver().arbitrate(event);
            return;
         }
         if (this.isSessionMuted(mySession, tsn)) {
            logger.debug("received a start while in the muted state. TSN "+tsn);
            mySession.sendMute(tsn);
            return;
         }
         // Saw a packet with losing audio. Mute the sender and throw into
         // the mute set.
         if (pttPacket.getISSIPacketType().getLosingAudio()) {
            if (mySession.isNotInterestedInLosingAudio()) {
               logger.debug("Got a spurt request packet with a losing audio. TSN "+tsn);
               addTsnToMutedSet(mySession, tsn);
               mySession.sendMute(tsn);
               return;
            }
         }

         if (this.sessionType == SessionType.CALLING_HOME) {
            for (PttSession pttSession : peerSessions.values()) {
               if (pttSession != mySession) {
                  if (pttSession instanceof SmfSession) {
                     SmfSession smfSession = (SmfSession) pttSession;
                     smfSession.getSmfTransmitter().forwardPttRequest(systemId, unitId);
                  }
               }
            }
         } else if (sessionType == SessionType.CALLED_HOME) {
            TransmitPriorityType ptype = pttPacket.getPTTControlWord().getTransmitPriority();
            int plevel = pttPacket.getPTTControlWord().getPriorityLevel();
            if (this.compareToCurrentPriority(ptype, plevel) == -1) {
               try {
                  mySession.getMmfReceiver().setArbitrateDeny();
               } catch (Exception ex) {
                  logger.error("Unexpected exception occured", ex);
                  return;
               }
            } else {
               // The given priority won.
               this.priorityLevel = plevel;
               this.transmitPriority = ptype;
            }

            mySession.getMmfReceiver().arbitrate(event);
            if (mySession.getMmfReceiver().getCurrentState(tsn) == MmfRxState.DENY) {
               return;
            }
            for (PttSession pttSession : peerSessions.values()) {
               if (pttSession != mySession && pttSession instanceof MmfSession) {
                  ((MmfSession) pttSession).getMmfTransmitter()
                        .sendSpurtStart(systemId,unitId, tsn, ptype, plevel);
               }
            }

         } else if (sessionType == SessionType.GROUP_HOME) {
            TransmitPriorityType ptype = pttPacket.getPTTControlWord().getTransmitPriority();
            int plevel = pttPacket.getPTTControlWord().getPriorityLevel();

            // The current priority is higher so deny the request.
            MmfSession mmfSession = (MmfSession) mySession;

            if (this.compareToCurrentPriority(ptype, plevel) == -1
                  || isSessionMuted(mmfSession, tsn)) {
               // Current priority is strictly higher.
               mySession.getMmfReceiver().setArbitrateDeny();
               mySession.getMmfReceiver().arbitrate(event);
            } else {

               if (this.transmitPriority == null) {
                  this.priorityLevel = plevel;
                  this.transmitPriority = ptype;
                  activePeersTsnTable.put(mmfSession, tsn);

               } else {
                  // Here if we are preempted by a higher priority Stream.
                  // If we have an ongoing mmf session then send him a
                  // mute. This will mute any transmission from another SU
                  // under  the same serving RFSS transmitting at a lower
                  // priority.
                  this.priorityLevel = plevel;
                  this.transmitPriority = ptype;

                  if (activePeersTsnTable.containsKey(mmfSession)) {
                     int oldTsn = activePeersTsnTable.get(mmfSession);

                     if (oldTsn != tsn) {
                        if (mmfSession.isNotInterestedInLosingAudio()) {
                           mmfSession.sendMute(oldTsn);
                           activePeersTsnTable.put(mmfSession, tsn);
                           addTsnToMutedSet(mmfSession, oldTsn);
                        }
                     }

                  } else {
                     activePeersTsnTable.put(mmfSession, tsn);
                  }

                  for (PttSession peer : peerSessions.values()) {
                     if (peer != mmfSession
                           && activePeersTsnTable.containsKey(peer)) {
                        int oldTsn = activePeersTsnTable.get(peer);
                        if (mmfSession.isNotInterestedInLosingAudio()) {
                           peer.sendMute(oldTsn);
                           addTsnToMutedSet(((MmfSession) peer), oldTsn);
                        }
                     }
                  }
               }
               mySession.getMmfReceiver().setArbitrateGrant();
               mySession.getMmfReceiver().arbitrate(event);

            }
            if (mySession.getMmfReceiver().getCurrentState(tsn) == MmfRxState.DENY) {
               logger.debug("current state is deny");
               return;
            }
            for (PttSession pttSession : peerSessions.values()) {
               if (pttSession != mySession && pttSession instanceof MmfSession) {
                  ((MmfSession) pttSession).getMmfTransmitter()
                        .sendSpurtStart(systemId,unitId, tsn, ptype, plevel);
               }
            }

         }
      } catch (Exception ex) {
         logger.fatal("Unexpected exception forwarding message", ex);
         throw new RuntimeException("Unexpected exception ", ex);
      }
   }

   /* FINISH THIS */
   public void receivedPttRequestWithVoice(MmfPacketEvent event) {
      logger.debug("[PttPointToMultipointSession] GOT REQUEST WITH VOICE");
   }

   public void receivedPttProgress(MmfPacketEvent event) {
      try {
         MmfSession mySession = (MmfSession) event.getSource();
         P25Payload pttPacket = event.getPttPacket();
         IMBEVoiceBlock[] imbeVoices = pttPacket.getIMBEVoiceBlockArray();
         int unitId = pttPacket.getPTTControlWord().getUnitId();
         int systemId = pttPacket.getPTTControlWord().getSystemId();
         TransmitPriorityType ptype = pttPacket.getPTTControlWord()
               .getTransmitPriority();
         int plevel = pttPacket.getPTTControlWord().getPriorityLevel();
         int tsn = pttPacket.getISSIPacketType().getTransmissionSequenceNumber();
         //boolean losingAudio = pttPacket.getISSIPacketType().getLosingAudio();

         if (this.isSessionMuted(mySession, tsn)) {
            logger.debug("received a PROGRESS in muted state for TSN "+tsn);
            mySession.sendMute(tsn);
            return;
         }
         // Saw a packet with losing audio. Mute the sender and throw into
         // the mute set.
         if (pttPacket.getISSIPacketType().getLosingAudio()) {
            logger.debug("Got a packet with a losing audio for TSN " + tsn);
            addTsnToMutedSet(mySession, tsn);
            if (mySession.isNotInterestedInLosingAudio()) {
               mySession.sendMute(tsn);
            }
         }
         logger.debug("Got an audio packet -- forwarding");

         for (PttSession pttSession : peerSessions.values()) {

            if (pttSession != mySession) {
               if (pttSession instanceof MmfSession) {
                  MmfSession mmfSession = (MmfSession) pttSession;
                  if (mmfSession.getMmfTransmitter().getCurrentState(tsn) == MmfTxState.BEGIN)
                     mmfSession.getMmfTransmitter().sendSpurtStart(
                           systemId, unitId, tsn, ptype, plevel);
                  mmfSession.getMmfTransmitter().sendVoice(imbeVoices,
                        systemId, unitId, tsn, ptype, plevel);

               } else if (pttSession instanceof SmfSession) {

                  SmfSession smfSession = (SmfSession) pttSession;
                  smfSession.getSmfTransmitter().forwardPttProgress(
                        imbeVoices, systemId, unitId, tsn, ptype, plevel);
               }
            }

         }
      } catch (Exception ex) {

         logger.error("Unexpected error forwarding packet", ex);
         throw new RuntimeException("Unexpected error forwarding packet", ex);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.transctlmgr.ptt.AudioListener#spurtEndInd(gov.nist.p25.issi.transctlmgr.ptt.PttEvent)
    */
   public void receivedPttEnd(MmfPacketEvent event) {

      PttSession mySession = (PttSession) event.getSource();
      transmitPriority = null;
      int tsn = event.getPttPacket().getISSIPacketType().getTransmissionSequenceNumber();
      try {
         if (mySession.isNotInterestedInLosingAudio()) {
            for (PttMuteRecord mutedSession : mutedSet) {
               // Get the highest priority guy to unmute.
               if (!mutedSession.mutedSession.isReceiverTerminated(mutedSession.tsn)) {
                  mutedSession.mutedSession.sendUnmute(mutedSession.tsn);
                  removeTsnFromMutedSet(mutedSession.mutedSession, mutedSession.tsn);
               }
            }
         }
         logger.debug("Got a spurt end notification -- forwarding ");

         for (PttSession pttSession : peerSessions.values()) {

            if (pttSession != mySession) {
               if (pttSession instanceof MmfSession) {
                  MmfSession mmfSession = (MmfSession) pttSession;
                  if (mmfSession.getMmfTransmitter().getCurrentState(tsn) == 
                      MmfTxState.TRANSMITTING) {
                     mmfSession.getMmfTransmitter().sendSpurtEndNotification(tsn);
                  }

               } else if (pttSession instanceof SmfSession) {

                  SmfSession smfSession = (SmfSession) pttSession;
                  if (smfSession.getSmfTransmitter().getCurrentState() != SmfTxState.BEGIN
                        && smfSession.getSmfTransmitter()
                              .getCurrentState() != SmfTxState.TERMINATED)
                     smfSession.getSmfTransmitter().forwardPttEnd();
               }
            }
         }

      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }

   public void audioTimeout() {
      logger.debug("[PttPointToMultipointSession] AUDIO TIMEOUT");
   }

   public Collection<PttSession> getPeerSessions() {
      return peerSessions.values();
   }

   public void receivedMute(PttMuteEvent muteEvent) {
      int tsn = muteEvent.getTSN();
      PttSession mutedSession = (PttSession) muteEvent.getSource();
      if (mutedSession instanceof MmfSession) {
         addTsnToMutedSet((MmfSession) mutedSession, tsn);
      } else {
         addTsnToMutedSet((SmfSession) mutedSession, tsn);
      }
   }

   public void receivedUnmute(PttUnmuteEvent unmuteEvent) {
      int tsn = unmuteEvent.getTSN();
      PttSession mutedSession = (PttSession) unmuteEvent.getSource();
      this.removeTsnFromMutedSet(mutedSession, tsn);
   }
}
