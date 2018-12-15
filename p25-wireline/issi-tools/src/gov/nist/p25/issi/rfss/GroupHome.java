//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.p25body.CallParamContent;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25body.SdpContent;
import gov.nist.p25.issi.rfss.SipUtils;
import gov.nist.p25.issi.transctlmgr.PttPointToMultipointSession;
import gov.nist.p25.issi.transctlmgr.ptt.HeartbeatListener;
import gov.nist.p25.issi.transctlmgr.ptt.MmfSession;
import gov.nist.p25.issi.transctlmgr.ptt.PttSession;
import gov.nist.p25.issi.transctlmgr.ptt.PttSessionInterface;
import gov.nist.p25.issi.transctlmgr.ptt.TimerValues;
//import gov.nist.p25.issi.utils.ProtocolObjects;

import java.text.ParseException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.ServerTransaction;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.message.Request;

import org.apache.log4j.Logger;

/**
 * The runtime rerpesentation of a group. This is the call record at the home
 * RFSS of the group. It is kept indexed by the radical name of the group. One
 * of these structures resides at the group home for each group.
 * 
 */
public class GroupHome implements HeartbeatListener {

   /** Logger for this class */
   private static Logger logger = Logger.getLogger(GroupHome.class);

   /** Call ID for this group call setup * */
   private String callId;

   /** A statically configured list of subscribers to this group * */
   private HashSet<RfssConfig> subscribers;

   /** Priority of this group * */
   private int priority;

   /** Indicate if this is an emergency group * */
   private boolean isEmergency;

   /** A table of dialogs that belong to this group that is indexed by RFSS ID * */
   private ConcurrentHashMap<String, Dialog> dialogs;

   /** A table of timer tasks to be canceled when the dialog is removed from the table */
   private ConcurrentHashMap<Dialog,TimerTask> timerTaskTable;
   
   /** The media multiplexer for this Group home */
   private PttPointToMultipointSession pointToMultipointSession;

   /** A table of PTT sessions indexed by the RFSS ID of the remote party * */
   private  ConcurrentHashMap <String, MmfSession> pttSessions;

   /** Number of requests pending for this group * */
   private int pendingRequestCount;

   /** Server Tx that started a group call * */
   private ServerTransaction serverTransaction;

   /** A set of client transactions ( one per RFSS ) */
   private ConcurrentHashMap<String,ClientTransaction> clientTransactions;

   /** A delay task that is stored here to delay processing at the home RFSS * */
   private TimerTask delayTask;

   /** The provider used to create this group * */
   private SipProvider provider;
   private GroupConfig groupConfig;
   private RFSS rfss;
   private CallParamContent callParamContent;

   /**
    * The heartbeat task that scans the dialog for RTP resources from the
    * remote side and exits when RTP resources are available. SIP heartbeats,
    * in the form of SIP re-INVITE requests, are sent by the home RFSS to
    * synchronize the SIP states between the home RFSS and serving RFSS when
    * there is no RTP session. More precisely, a SIP heartbeat is a SIP re-
    * INVITE where the SDP offer in the re-INVITE is identical to the previous
    * SDP offer, in particular, the version parameter of the origin line has
    * not been changed, per Section 8 of [RFC3264]. The following procedure
    * applies:
    * 
    * 1. Upon successful setup of a SIP session with a serving RFSS (i.e., upon
    * sending or receiving a SIP ACK request for the SIP INVITE request), the
    * home RFSS SHALL start a tgchconfirmsip timer.
    * 
    * 2. The home RFSS SHALL stop the timer under the following situations:
    * 
    * a. Upon successful termination of the SIP session, or
    * 
    * b. Upon successful setup of the associated RTP session, or
    * 
    * c. In the event that, while awaiting availability of RTP resources at the
    * serving RFSS for setting up an impending RTP session, RTP resources at
    * the home RFSS has been preempted.
    * 
    * 3. The home RFSS SHALL restart the timer under the following situations:
    * 
    * a. Upon successful termination of the associated RTP session, or
    * 
    * b. Upon sending a SIP ACK request corresponding to its re-INVITE request
    * to the serving RFSS.
    * 
    * 4. Upon expiry of the timer, the home RFSS SHALL send a re-INVITE request
    * to the serving RFSS over the ISSI, in an attempt to initiate an RTP
    * session, following the procedures in Section 6.4.3 of this document.
    * 
    * 5. Selectively repeat the above steps, as appropriate.
    * 
    */
   class SipHeartbeatTimerTask extends TimerTask {
      private String peerDomainName;
      private GroupHome groupHome;
      private GroupCallControlManager groupCallControlManager;
   
      SipHeartbeatTimerTask(String peerDomainName, GroupCallControlManager groupCallControlManager) {
         this.peerDomainName = peerDomainName;
         this.groupHome = GroupHome.this;
         this.groupCallControlManager = groupCallControlManager;
      }

      @Override
      public void run() {
         try {
            //logger.debug("SipHeartbeatTimerTask: peerDoaminName=" + peerDomainName);
            MmfSession mmfSession = groupHome.getMmfSession(peerDomainName);
            Dialog dialog = groupHome.getDialog(mmfSession);
            
            if (dialog != null && dialog.getState() == DialogState.CONFIRMED &&
                  mmfSession.getRtpSession().getRemoteRtpRecvPort() == 0) {
               //logger.debug("SipHeartbeatTimerTask: remoteRtpRecvPort=" + mmfSession.getRtpSession().getRemoteRtpRecvPort());
               
               //20.2.x create ReINVITE for group call
               Request request = groupCallControlManager.createReInviteForGroupCall(groupHome, dialog);
               SessionDescription sdpAnnounce = mmfSession.getSessionDescription();
               RfssConfig rfssConfig = GroupHome.this.rfss.getRfssConfig();

               CallControlManager.fixupSdpAnnounce(rfssConfig, mmfSession, sdpAnnounce);
               CallParamContent callParamContent = groupHome.getCallParamContent();
               if (callParamContent == null) {
                  request.setContent(sdpAnnounce, SipUtils.createAppSdpContentTypeHeader());
               } else {
                  ContentList contentList = new ContentList();
                  contentList.add(callParamContent);
                  SdpContent sdpContent = SdpContent.createSdpContent();
                  sdpContent.setSessionDescription(sdpAnnounce);
                  contentList.add(sdpContent);
                  request.setContent(contentList.toString(), contentList.getContentTypeHeader());
               }
               SipUtils.filterSipHeaders( request);
	       SipUtils.checkContentLength( request);
               logger.debug("SipHeartbeatTimerTask: request=\n" + request);

               ClientTransaction clientTransaction = provider.getNewClientTransaction(request);
               clientTransaction.setApplicationData(mmfSession);
               dialog.sendRequest(clientTransaction);
            }
         } catch (Exception ex) {
            rfss.getTestHarness().fail("unexpected exception", ex);
         }
      }
   }

   class RegistrationTimer extends TimerTask {
      private RfssConfig rfssConfig;

      public RegistrationTimer(RfssConfig rfssConfig) {
         this.rfssConfig = rfssConfig;
      }
      @Override
      public void run() {
         GroupHome.this.subscribers.remove(rfssConfig);
      }
   }

   // constructor
   public GroupHome(RFSS rfss, PttPointToMultipointSession session, SipProvider provider,
         GroupConfig groupConfig) {
      this.subscribers = new HashSet<RfssConfig>();
      this.pointToMultipointSession = session;
      this.dialogs = new ConcurrentHashMap<String, Dialog>();
      this.pttSessions = new ConcurrentHashMap<String, MmfSession>();
      this.priority = 1;
      this.isEmergency = false;
      this.provider = provider;
      this.groupConfig = groupConfig;
      this.clientTransactions = new ConcurrentHashMap<String,ClientTransaction>();
      this.rfss = rfss;
      this.timerTaskTable = new ConcurrentHashMap<Dialog,TimerTask>();
   }
   
   public GroupConfig getGroupConfig() {
      return groupConfig;
   }
   
   public boolean hasPttSession(RfssConfig rfssConfig) {
      return pttSessions.containsKey(rfssConfig.getDomainName());
   }

   public void addDialog(String rfssId, Dialog dialog) {
      logger.debug("Group: addDialog " + rfssId);
      dialogs.put(rfssId, dialog);
      TimerTask timerTask = 
         new SipHeartbeatTimerTask(rfssId,rfss.getCallControlManager().getGroupCallControlManager());
      timerTaskTable.put(dialog,timerTask);
      ISSITimer.getTimer().schedule(timerTask, TimerValues.TGCHCONFIRMSIP, TimerValues.TGCHCONFIRMSIP);
   }

   public Collection<Dialog> getDialogs() {
      return dialogs.values();
   }

   public PttPointToMultipointSession getPttPointToMultipointSession() {
      return pointToMultipointSession;
   }

   /**
    * This method is called when a Dialog is terminated.
    * 
    * @param dialog -- the dialog that has terminated.
    */
   public void removeDialog(Dialog dialog) {
      Address address = dialog.getRemoteTarget();
      if (address != null) {
         SipURI sipUri = (SipURI) address.getURI();
         String id = SipUtils.getDomainName(sipUri);
         removePttSession(id);
         dialogs.remove(id);
      }
      TimerTask ttask = timerTaskTable.get(dialog);
      if( ttask != null) {
         ttask.cancel();
      }
   }

   /**
    * Add a PTT session to this group.
    * 
    * @param rfssId
    * @param pttSession
    */
   public void addPttSession(String rfssId, MmfSession pttSession) {
      //+++pttSessions.put(rfssId.toLowerCase(), pttSession);
      pttSessions.put(rfssId, pttSession);
      pttSession.getHeartbeatReceiver().setHeartbeatListener(this);
   }

   /**
    * Remove a ptt session from this group.
    * 
    * @param senderDomainName
    */
   public void removePttSession(String senderDomainName) {
      //+++PttSession pttSession = pttSessions.get(senderDomainName.toLowerCase());
      PttSession pttSession = pttSessions.get(senderDomainName);
      if (pttSession != null) {
         pttSession.shutDown();
         pttSessions.remove(senderDomainName);
      }
   }
   
   /**
    * Get the Mmf Session for the given Rfss.
    * 
    * @param rfssId -- rfssDomain name of the rfss for which to get the ptt session.
    */
   public MmfSession getMmfSession(String rfssDomainName) {
      return pttSessions.get(rfssDomainName);
      //+++return pttSessions.get(rfssDomainName.toLowerCase());
   }

   /**
    * Add a RFSS to the list of Rfss's that are subscribers to this group.
    * 
    * @param rfssConfig -- the rfss to add.
    */
   public void addSubscriber(RfssConfig rfssConfig) {
      subscribers.add(rfssConfig);

      // Remove the registration after the given time.
      if (rfssConfig.getGroupRegistrationExpiresTime() > 0) {
         ISSITimer.getTimer().schedule(new RegistrationTimer(rfssConfig),
               rfssConfig.getGroupRegistrationExpiresTime() * 1000);
      }
   }

   public Collection<RfssConfig> getSubscribers() {
      return subscribers;
   }

   /**
    * Remove an RFSS from the subscribers of this group.
    * 
    * @param departingRfss -- rfss to remove.
    */
   public void removeSubscriber(RfssConfig registeringRfss) {
      subscribers.remove(registeringRfss);
   }

   public boolean isSubscribed(RfssConfig rfssConfig) {
      return subscribers.contains(rfssConfig);
   }

   /**
    * @param priority -- The priority to set.
    */
   public void setPriority(int priority) {
      this.priority = priority;
   }

   /**
    * @return Returns the priority.
    */
   public int getPriority() {
      return priority;
   }

   /**
    * @param isEmergency -- The isEmergency to set.
    */
   public void setEmergency(boolean isEmergency) {
      this.isEmergency = isEmergency;
   }

   /**
    * @return Returns the isEmergency.
    */
   public boolean isEmergency() {
      return isEmergency;
   }

   /**
    * @param pendingRequestCount
    *            The pendingRequestCount to set.
    */
   public void setPendingRequestCount(int pendingRequestCount) {
      this.pendingRequestCount = pendingRequestCount;
   }

   /**
    * @return Returns the pendingRequestCount.
    */
   public int getPendingRequestCount() {
      return pendingRequestCount;
   }

   public void incrementPendingRequestCount() {
      pendingRequestCount++;
   }

   public void decrementPendingRequestCount() {
      pendingRequestCount--;
   }

   /**
    * Set the call ID.
    *
    * @param callId -- The callId to set.
    */
   public void setCallId(String callId) {
      this.callId = callId;
   }

   /**
    * Get the call ID.
    *
    * @return Returns the callId.
    */
   public String getCallId() {
      return callId;
   }

   /**
    * Set the server transaction.
    *
    * @param serverTransaction -- the serverTransaction to set
    */
   public void setServerTransaction(ServerTransaction serverTransaction) {
      this.serverTransaction = serverTransaction;
      // Extract the call parameters and store them.
      Request request = serverTransaction.getRequest();
      try {
         ContentList clist = ContentList.getContentListFromMessage(request);
         this.callParamContent = clist.getCallParamContent();
      } catch (ParseException ex) {
         rfss.getTestHarness().fail("Badly formatted message ", ex);
      }
   }
   
   /**
    * Get the call param content.
    * 
    * @return the callParam Content.
    */
   public CallParamContent getCallParamContent() {
      return callParamContent;
   }

   /**
    * @return the serverTransaction
    */
   public ServerTransaction getServerTransaction() {
      return serverTransaction;
   }

   public void setDelayTask(TimerTask delayTask) {
      this.delayTask = delayTask;
   }

   public TimerTask getDelayTask() {
      return delayTask;
   }

   public void setClientTransaction(String rfssDomainName, ClientTransaction clientTransaction) {
      if (logger.isDebugEnabled()) {
         logger.debug("setClientTransaction: " +rfssDomainName +" : " +clientTransaction);
      }
      clientTransactions.put(rfssDomainName,clientTransaction);
   }

   public void removeClientTransaction(ClientTransaction clientTransaction) {
      if (logger.isDebugEnabled()) {
         logger.debug("removeClientTransaction: " +clientTransaction);
      }
      for (String key : clientTransactions.keySet()){
         if (clientTransactions.get(key) == clientTransaction ) {
            clientTransactions.remove(key);
            break;
         }
      }
   }

   public Collection<ClientTransaction> getClientTransactions() {
      return clientTransactions.values();
   }

   public void heartbeatTimeout(PttSessionInterface pttSession) {
      try {
         logger.debug("Got a heartbeat timeout notification " );
         Enumeration<String> en = pttSessions.keys();
         String rfssId = null;
         while (en.hasMoreElements()) {
            String key = en.nextElement();
            if (pttSessions.get(key) == pttSession) {
               rfssId = key;
            }
         }
         pttSessions.remove(rfssId);

         Dialog dialog = dialogs.get(rfssId);
         if (dialog != null && dialog.getState() != null 
               && dialog.getState() != DialogState.TERMINATED) {
            Request bye = dialog.createRequest(Request.BYE);

	    /*** doesnot work : reject logging BYE message 
	    //#705 10.2.x URI changes:
	    //BYE sip:000020020001@f2.001.00001.p25dr;user=TIA-P25-SG
	    //BYE sip:000020020001@p25dr;user=TIA-P25-SG
            SipURI sipUri = (SipURI) bye.getRequestURI();
            sipUri.setUserParam(ISSIConstants.TIA_P25_SG);
            sipUri.setHost(ISSIConstants.P25DR);
            logger.debug("heartbeatTimeout(705): sipUri=" +sipUri);
	     ***/

            SipUtils.filterSipHeaders( bye);
            SipUtils.checkContentLength( bye);

            ClientTransaction ct = provider.getNewClientTransaction(bye);
            dialog.sendRequest(ct);
            logger.debug("heartbeatTimeout(): byeRequest=" + bye);

         } else {
            logger.debug("Could not find the dialog for " + rfssId);
         }
         dialogs.remove(rfssId);
         pttSession.shutDown();

      } catch (Exception ex) {
         rfss.logError("Unexpected error", ex);
      }
   }
   
   public void receivedHeartbeat(int TSN) {
      logger.debug("[Group] Received heartbeat.");
   }
   
   public void receivedHeartbeatQuery(PttSessionInterface pttSession, int TSN) {
      logger.debug("[Group] Received heartbeat query.");
   }

   /**
    * Stop the outgoing Heartbeats to a given RFSS ID.
    * 
    * @param rfssId -- rfss id to quench outgoing heartbeats to.
    */
   public void stopHeartbeatTransmission(String rfssId) {
      try {
         PttSession pttSession = pttSessions.get(rfssId);
         pttSession.getHeartbeatTransmitter().blockOutgoingHearbeats();
      } catch (Exception ex) {
         logger.error("stopHeartbeatTransmission: exception=", ex);
      }
   }
   
   public Collection<MmfSession> getMmfSessions() {
      return pttSessions.values();
   }
   
   /**
    * Get the dialog corresponding to an mmf session - to reinvite the group.
    * 
    * @param mmfSession
    * @return Dialog
    */
   public Dialog getDialog(PttSessionInterface mmfSession) {
      for ( String key : pttSessions.keySet() ) {
         if ( pttSessions.get(key) == mmfSession) {
            return dialogs.get(key);
         }
      }
      return null;
   }

   /**
    * Get the SIP provider.
    *
    * @return the provider
    */
   public SipProvider getProvider() {
      return provider;
   }

   /**
    * Get the client transacton for the given remote target.
    * 
    * @param senderDomain
    * @return
    */
   public ClientTransaction getClientTransaction(String senderDomain) {
      if(logger.isDebugEnabled()) {
         logger.debug("GroupHome: getClientTransaction: " + senderDomain);
      }
      return clientTransactions.get(senderDomain);
   }

   //-----------------------------------------------------------------
   public void sendMute(String rfssId) {
      try {
         PttSession pttSession = pttSessions.get(rfssId);
         //logger.debug("sendMute: rfssId="+rfssId+" pttSession=" +pttSession);
         int tsn = pttSession.getCurrentTSN();
         pttSession.sendMute(tsn);
      } catch (Exception ex) {
         logger.error("sendMute: exception=", ex);
      }
   }
   public void sendUnmute(String rfssId) {
      try {
         PttSession pttSession = pttSessions.get(rfssId);
         //logger.debug("sendUnmute: rfssId="+rfssId+" pttSession=" +pttSession);
         int tsn = pttSession.getCurrentTSN();
         pttSession.sendUnmute(tsn);
      } catch (Exception ex) {
         logger.error("sendUnmute: exception=", ex);
      }
   }
}
