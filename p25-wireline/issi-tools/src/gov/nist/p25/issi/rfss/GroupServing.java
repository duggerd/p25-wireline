//
package gov.nist.p25.issi.rfss;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.ServerTransaction;
import javax.sip.SipProvider;
import javax.sip.Transaction;
import javax.sip.address.SipURI;
import javax.sip.message.Request;

import org.apache.log4j.Logger;

import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.transctlmgr.ptt.HeartbeatListener;
import gov.nist.p25.issi.transctlmgr.ptt.PttSessionInterface;
import gov.nist.p25.issi.transctlmgr.ptt.PttSessionMultiplexer;
import gov.nist.p25.issi.transctlmgr.ptt.SmfSession;

/**
 * The structure that each RFSS participating in a group call keeps around.
 */
@SuppressWarnings("unused")
public class GroupServing implements HeartbeatListener {
   private static Logger logger = Logger.getLogger(GroupServing.class);
   
   private GroupConfig groupConfig;
   private Dialog dialog; /* My Dialog to signal the group */   
   private Hashtable<String,Integer> tsnTable;
   private HashSet<SuConfig> subscribers;
   private ClientTransaction clientTransaction;
   private ServerTransaction serverTransaction;
   private SipProvider provider;
   private Transaction lastTransaction;
   private int priority;
   private boolean emergency;
   private PttSessionMultiplexer multiplexer;
   private RFSS rfss;

   // constructor
   private GroupServing() {
      this.subscribers = new HashSet<SuConfig>();
      this.tsnTable = new Hashtable<String,Integer>();
   }

   public GroupServing(RFSS rfss, Dialog myDialog, ClientTransaction ct,
         GroupConfig groupConfig, SipProvider provider) {
      this();
      this.groupConfig = groupConfig;
      this.dialog = myDialog;
      this.clientTransaction = ct;
      this.provider = provider;
      this.lastTransaction = ct;
      this.rfss = rfss;
   }

   public GroupServing(RFSS rfss, Dialog myDialog, ServerTransaction st,
         GroupConfig groupConfig, SipProvider provider) {
      this();
      this.groupConfig = groupConfig;
      this.dialog = myDialog;
      this.serverTransaction = st;
      this.provider = provider;
      this.lastTransaction = st;
      this.rfss = rfss;
   }

   /**
    * @return true if the given su is already in the group call.
    */
   public boolean containsSu(SuConfig suConfig) {
      return this.subscribers.contains(suConfig);
   }

   /**
    * @return Returns the groupConfig.
    */
   public GroupConfig getGroupConfig() {
      return groupConfig;
   }

   /**
    * Get Dialog
    *
    * @return Returns the dialog.
    */
   public Dialog getDialog() {
      return dialog;
   }

   /**
    * Add an SU to the list of subscribers.
    * 
    * @param suConfig
    */
   public void addSu(SuConfig suConfig) {
      subscribers.add(suConfig);
   }
   
   /**
    * Remove an SU from the list of subscribers.
    * 
    * @param suConfig
    */
   public void removeSu(SuConfig suConfig) {
      subscribers.remove(suConfig);
   }

   /**
    * Get the list of subscribers.
    * 
    * @return -- the subscribers.
    */
   public Collection<SuConfig> getSubscribers() {
      return subscribers;
   }

   /**
    * @return the clientTransaction
    */
   public ClientTransaction getClientTransaction() {
      return clientTransaction;
   }

   /**
    * Method that gets called when your heartbeat times out.
    * 
    * @param pttSession --
    *            session which timed out.
    */
   public void heartbeatTimeout(PttSessionInterface pttSession) {
      logger.debug("processHeartbeatTimeout");
      try {
         for (SmfSession smfSession : multiplexer.getSmfSessions()) {
            smfSession.shutDown();
         }
         // Kill the associated SIP dialog if it is not already dead.
         if (dialog != null && dialog.getState() != null
               && dialog.getState() != DialogState.TERMINATED) {
            Request request = dialog.createRequest(Request.BYE);

            //#592 10.1.x URI changes:
            //BYE sip:000020020001@f1.002.00002.p25dr;user=TIA-P25-SG
            //BYE sip:00002002000012@p25dr;user=TIA-P25-SG
            SipURI sipUri = (SipURI) request.getRequestURI();
            sipUri.setUserParam(ISSIConstants.TIA_P25_SG);
            sipUri.setHost(ISSIConstants.P25DR);
            //logger.debug("heartbeatTimeout: sipUri=" +sipUri);

            SipUtils.filterSipHeaders( request);
            SipUtils.checkContentLength( request);

            ClientTransaction ct = provider.getNewClientTransaction(request);
            dialog.sendRequest(ct);
         }
      } catch (Exception ex) {
         rfss.logError("Unexpected error", ex);
      }
   }
   
   public void receivedHeartbeat(int TSN) {
      logger.debug("[GroupCall] Received heartbeat.");
   }
   
   public void receivedHeartbeatQuery(PttSessionInterface pttSession, int TSN) {
      logger.debug("[GroupCall] Received heartbeat query.");
   }

   /**
    * Block outgoing transmission of heartbeats.
    */
   public void stopHeartbeatTransmission() {
      multiplexer.getHeartbeatTransmitter().blockOutgoingHearbeats();
   }
   
   /**
    * Get the provider for this ServedGroup.
    * 
    * @return -- the provider
    */
   public SipProvider getProvider() {
      return provider;
   }

   /**
    * Set the client transaction for this served group.
    * 
    * @param clientTransaction -- the client transaction to set.
    */
   public void setClientTransaction(ClientTransaction clientTransaction) {
      this.clientTransaction = clientTransaction;
      this.lastTransaction = clientTransaction;
   }

   /**
    * Get the server transaction that caused the creation of this subscribed group.
    * 
    * @return -- the client transaction that caused the subscription of the group.
    */
   public ServerTransaction getServerTransaction() {
      return serverTransaction;
   }

   /**
    * Get the transaction that caused the creation of this record 
    * (client or server tx ).
    * 
    * @return -- the transaction that created this record.
    * 
    */
   public Transaction getLastTransaction() {
      return lastTransaction;
   }

   /**
    * Set the priority of the group.
    * 
    * @param priority -- the priority to set.
    * 
    */
   public void setPriority(int priority) {
      this.priority = priority;
   }
   
   /**
    * Get the priority.
    * 
    * @return -- the priority of the group call.
    */
   public int getPriority() {
      return priority;
   }
   
   /**
    * set the emergency flag.
    * 
    * @param emergency -- emergency flag to set.
    */
   public void setEmergency( boolean emergency) {
      this.emergency = emergency;
   }
   
   /**
    * Get the emergency flag.
    * 
    * @return -- the emergency flag.
    */
   public boolean isEmergency() {
      return emergency;
   }

   /**
    * Set my multiplexer.
    * 
    * @param multiplexer
    */
   public void setMultiplexer(PttSessionMultiplexer multiplexer) {
      this.multiplexer = multiplexer;
      multiplexer.getHeartbeatReceiver().setHeartbeatListener(this);
   }
   
   public PttSessionMultiplexer getMultiplexer() {
      return multiplexer;
   }
   
   /**
    * Get the smf session corresponding to a sequence number.
    * 
    * @param tsn
    * @return
    */
   public SmfSession getSmfSession(int tsn) {
      return (SmfSession) multiplexer.getPttSession(tsn);
   }
   
   /**
    * Send a heartbeat query to the group home.
    *
    */
   public void sendHeartbeatQuery() throws Exception {
      this.multiplexer.sendHeartbeatQuery();
   }
}
