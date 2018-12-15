//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.p25body.CallParamContent;
import gov.nist.p25.issi.transctlmgr.PttPointToMultipointSession;
import gov.nist.p25.issi.transctlmgr.ptt.HeartbeatListener;
import gov.nist.p25.issi.transctlmgr.ptt.PttSession;
import gov.nist.p25.issi.transctlmgr.ptt.PttSessionInterface;

import java.util.Hashtable;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.ServerTransaction;
import javax.sip.SipProvider;
import javax.sip.address.SipURI;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;

import org.apache.log4j.Logger;

/**
 * A wrapper for SIP Dialogs that is used in a point to point call. This is an
 * abstraction that the SU sees.
 * 
 */
public class UnitToUnitCall implements HeartbeatListener {

   private static Logger logger = Logger.getLogger(UnitToUnitCall.class);

   private SipProvider provider;
   private ClientTransaction clientTransaction;   
   private Hashtable<String,Dialog> dialogs;
   private SuConfig callingSuConfig;
   private SuConfig calledSuConfig;
   private PttSession pttSession;
   private PttPointToMultipointSession pointToMultipointSession;
   private ServerTransaction serverTransaction;
   private CallParamContent callParam;
   private boolean isCancelled;   
   private boolean initrans;
   private boolean emergency;
   private int priorityValue;

   // constructor
   public UnitToUnitCall( SuConfig callingSuConfig, SuConfig calledSuConfig,
         SipProvider provider) {
      logger.debug("Creating call segment " + this);
      this.callingSuConfig = callingSuConfig;
      this.calledSuConfig = calledSuConfig;
      this.provider = provider;
      this.priorityValue = 1;
      this.emergency = false;
      this.dialogs = new Hashtable<String,Dialog>();
      this.isCancelled = false;
   }

   public SipProvider getProvider() {
      return provider;
   }

   public Dialog getDialog(String radicalName) {
      Dialog retval = dialogs.get(radicalName);
      if ( retval == null) {
         if ( logger.isDebugEnabled() ) {
            logger.debug(dialogs);
         }
      }
      return retval;
   }

   public Dialog getPeerDialog(Dialog dialog) {
      Dialog retval = null;
      for ( Dialog d : dialogs.values()) {
         if ( d != dialog) retval = d;
      }
      return retval;
   }

   public SuConfig getCallingSuConfig() {
      return callingSuConfig;
   }

   public SuConfig getCalledSuConfig() {
      return calledSuConfig;
   }

   public boolean includesSU(SuConfig suConfig) {
      return callingSuConfig.equals(suConfig)
            || calledSuConfig.equals(suConfig);
   }

   public String getCallID() {
      Dialog dialog = dialogs.values().iterator().next();
      return dialog.getCallId().getCallId();
   }

   public void setPttSession(PttSession rtpSession) {
      this.pttSession = rtpSession;
      rtpSession.getHeartbeatReceiver().setHeartbeatListener(this);
   }

   public PttSession getPttSession() {
      return pttSession;
   }

   public void setPriority(int priorityValue) {
      this.priorityValue = priorityValue;
   }

   public int getPriority() {
      return priorityValue;
   }

   public void setEmergency(boolean emergency) {
      this.emergency = emergency;
   }

   public boolean isEmergency() {
      return emergency;
   }
   
   public void setPointToMultipointSession( PttPointToMultipointSession pointToMultipointSession) {
      logger.debug("setPointToMultipointSession : " + this + " session " + 
         pointToMultipointSession);
      this.pointToMultipointSession = pointToMultipointSession;
   }

   public PttPointToMultipointSession getPointToMultipointSession() {
      if ( pointToMultipointSession == null)
         logger.debug("getPointToMultipointSession : " + this + " returning null " );
      return pointToMultipointSession;
   }

   public void setClientTransaction(ClientTransaction ct) {
      this.clientTransaction = ct;
      Request request = ct.getRequest();
      ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
      String radicalName = ((SipURI)toHeader.getAddress().getURI()).getUser();
      dialogs.put(radicalName, ct.getDialog());
   }
   
   public ClientTransaction getClientTransaction () {
      return clientTransaction;
   }
   
   public void setServerTransaction( ServerTransaction st) {
      this.serverTransaction = st;
      Request request = st.getRequest();
      FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
      String radicalName = ((SipURI)fromHeader.getAddress().getURI()).getUser();
      dialogs.put(radicalName, st.getDialog());
   }
   
   public ServerTransaction getServerTransaction() {
      return serverTransaction;
   }   

   public void setCallParam(CallParamContent callParam) {
      this.callParam = callParam;      
   }
   
   public CallParamContent getCallParamContent () {
      callParam.getCallParam().setIncallRoaming(false);
      return callParam;
   }

   public boolean isCancelled() {
      return isCancelled;
   }
   
   public void setCanceled() {
      this.isCancelled = true;
   }

   public void heartbeatTimeout(PttSessionInterface pttSession) {
      logger.debug("[UnitToUnitCall] Received heartbeat timeout.");      
   }
   
   public void receivedHeartbeat(int TSN) {
      logger.debug("[UnitToUnitCall] Received heartbeat. TSN="+TSN);
   }
   
   public void receivedHeartbeatQuery(PttSessionInterface pttSession, int TSN) {
      logger.debug("[UnitToUnitCall] Received heartbeat query. TSN="+TSN);
   }

   public void setCallingSuInitrans(boolean initrans) {
      this.initrans = initrans;
   }

   public boolean isCallingSuInitrans() {
      return initrans;
   }
}
