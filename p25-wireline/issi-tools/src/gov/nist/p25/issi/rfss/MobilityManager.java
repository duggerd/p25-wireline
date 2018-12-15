//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.rfss.SipUtils;

import java.util.HashSet;
import java.util.TimerTask;

import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

/**
 * Top level Mobility Manager. This multiplexes requests to either a SU to SU
 * mobility manager or a Group Mobilty Manager depending upon whether the
 * request belongs to a Unit to Unit call or to a Group call.
 * 
 */
public class MobilityManager implements SipListener {
   private static Logger logger = Logger.getLogger(MobilityManager.class);

   // milisec between membership change
   private static final int SCAN_INTERVAL = 500;

   private UnitToUnitMobilityManager unitToUnitMobilityManager;
   private GroupMobilityManager groupMobilityManager;
   private RFSS rfss;
   private MembershipScanner membershipScanner;

   enum MessageType {
      U2U, GROUP
   };

   /**
    * This timer task scans the membership of the Rfss and looks for changes in
    * membership. If it detects a change in membership, it notifies the unit to
    * unit and group mobility managers. this.servedUnits contains the
    * Subscriber units served by the RFSS in this scan.
    * 
    * @undocumented
    */
   class MembershipScanner extends TimerTask {
      HashSet<SuConfig> servedUnits;
      RfssConfig rfssConfig;

      MembershipScanner() {
         this.rfssConfig = rfss.getRfssConfig();
         this.servedUnits = new HashSet<SuConfig>(rfss.getServedSubscriberUnits());
      }

      public synchronized void removeServedSubscriberUnit(SuConfig suConfig) {
         this.servedUnits.remove(suConfig);
         this.rfssConfig.getRFSS().removeServedSubscriberUnit(suConfig);
      }

      public synchronized void run() {
         HashSet<SuConfig> currentServed;
         currentServed = new HashSet<SuConfig>(rfss.getServedSubscriberUnits());
         // We dont need to sycnrhonize the entire scan because we will catch
         // it on the next scan.
         // this.servedUnits is what we saw in the last scan.
         for (SuConfig suConfig : this.servedUnits) {
            // check for departures.
            if (!currentServed.contains(suConfig)) {
               unitToUnitMobilityManager.suDeparted(suConfig);
               groupMobilityManager.suDeparted(suConfig);
            }
         }
         for (SuConfig suConfig : currentServed) {
            if (!this.servedUnits.contains(suConfig)) {
               unitToUnitMobilityManager.suArrived(suConfig);
               groupMobilityManager.suArrived(suConfig);
            }
         }
         this.servedUnits = currentServed;
      }
   }

   public MobilityManager(RFSS rfss) {
      assert rfss != null;
      this.rfss = rfss;
      unitToUnitMobilityManager = new UnitToUnitMobilityManager(rfss);
      groupMobilityManager = new GroupMobilityManager(rfss);

      this.membershipScanner = new MembershipScanner();
      try {
         ISSITimer.getTimer().schedule(membershipScanner, SCAN_INTERVAL, SCAN_INTERVAL);
      } catch( IllegalStateException ex) {
         ex.printStackTrace();
         String msg = "ResetTimer: Timer cancelled - "+ex;
         logger.debug( msg);
         ISSITimer.resetTimer();
         ISSITimer.getTimer().schedule(membershipScanner, SCAN_INTERVAL, SCAN_INTERVAL);
      }
   }

   private MessageType getMessageType(Message message) {

      FromHeader fromHeader = (FromHeader) message.getHeader(FromHeader.NAME);
      SipURI sipUri = (SipURI) fromHeader.getAddress().getURI();
      if (sipUri.getUser() == null) {
         rfss.getTestHarness().fail("Null UserParameter in the From Header of Request");
      }
      if (fromHeader.getTag() == null) {
         rfss.getTestHarness().fail("Missing tag Parameter in the From Header");
      }
      //ToHeader toHeader = (ToHeader) message.getHeader(ToHeader.NAME);
      //if (toHeader.getTag() != null) {
      //   rfss.getTestHarness().fail( "To Header contains a Tag Parameter");
      //}

      if (SipUtils.isSubscriberUnit(sipUri)) {
         return MessageType.U2U;
      } else if (SipUtils.isGroup(sipUri)) {
         return MessageType.GROUP;
      } else {
         rfss.logError("Unknown type of request : " + message);
         rfss.getTestHarness().fail("Unknown User Parameter in the FromHeader of request ");
         return null;
      }
   }

   /**
    * SIP Register-Register is used as part of the Registration procedure to
    * provide a means for a serving RFSS to express interest in a mobility
    * object. The Register message is distinguished by the presence of a
    * contact parameter and a non-zero expires parameter. The SIP header fields
    * SHALL feature an "expires" parameter indicating its registration
    * expiration interval chosen by the home RFSS, and the body SHALL include
    * service profile information for the requested object.
    *
    * @param request -- the SIP request.
    * @return -- true if the request is Register-Register message.
    */
   public static boolean isRegisterRegister(Request request) {
      ExpiresHeader expires = request.getExpires();
      return request.getMethod().equals(Request.REGISTER)
            && request.getHeader(ContactHeader.NAME) != null
            && expires != null && expires.getExpires() != 0;
   }

   /**
    * SIP Register-Deregister is used by a serving RFSS to deregister a
    * mobility object or by a home RFSS to request deregistration of an SU. The
    * SIP Register-Deregister command is distinguished by the presence of a
    * contact parameter and a zero expires parameter. As with the SIP
    * Register-Query message, the SIP Register-Deregister message is used in
    * two separate procedures, the De-registration procedure and the Roamed
    * procedure. Again, the choice of procedure is dependant on the roles of
    * the RFSSs involved.
    *
    * @param request -- the SIP request.
    * @return -- true if the request is Register-DeRegister message.
    */
   public static boolean isRegisterDeregister(Request request) {
      ExpiresHeader expires = request.getExpires();
      return request.getMethod().equals(Request.REGISTER)
            && request.getHeader(ContactHeader.NAME) != null
            && expires != null && expires.getExpires() == 0;
   }

   /**
    * SIP Register-Query is sent by a serving RFSS to query a home RFSS for
    * service profile information, and by a home RFSS to confirm the interest
    * of a serving RFSS in a mobility object. It is distinguished by the
    * absence of both the expires parameter and the contact parameter. The SIP
    * Register-Query message is used in two procedures described in ?5.5, the
    * Home Query procedure and the Serving Query procedure. The content of the
    * message is consistent in each case, and the particular procedure is
    * determined by the roles of the participating RFSSs pursuant to the
    * mobility object involved.
    *
    * @param request -- the SIP request.
    * @return -- true if the request is Register-Query message.
    */
   public static boolean isRegisterQuery(Request request) {
      return request.getMethod().equals(Request.REGISTER)
            && request.getHeader(ContactHeader.NAME) == null
            && request.getHeader(ExpiresHeader.NAME) == null;
   }

   // implementation of SIPListener
   //-------------------------------------------------------------------
   public void processRequest(RequestEvent requestEvent) {
      Request request = requestEvent.getRequest();
      MessageType requestType = getMessageType(request);
      if (requestType == MessageType.GROUP) {
         groupMobilityManager.processRequest(requestEvent);
      } else if (requestType == MessageType.U2U) {
         unitToUnitMobilityManager.processRequest(requestEvent);
      }
   }

   public void processResponse(ResponseEvent responseEvent) {
      Response response = responseEvent.getResponse();
      MessageType requestType = getMessageType(response);
      if (requestType == MessageType.GROUP) {
         groupMobilityManager.processResponse(responseEvent);
      } else if (requestType == MessageType.U2U) {
         unitToUnitMobilityManager.processResponse(responseEvent);
      }
   }

   public void processTimeout(TimeoutEvent timeoutEvent) {
      if (timeoutEvent.isServerTransaction()) {
         ServerTransaction serverTransaction = timeoutEvent.getServerTransaction();
         Request request = serverTransaction.getRequest();
         MessageType type = getMessageType(request);
         if (type == MessageType.GROUP) {
            groupMobilityManager.processTimeout(timeoutEvent);
	 } else {
            unitToUnitMobilityManager.processTimeout(timeoutEvent);
         }
      } else {
         ClientTransaction clientTransaction = timeoutEvent.getClientTransaction();
         Request request = clientTransaction.getRequest();
         MessageType type = getMessageType(request);
         if (type == MessageType.GROUP) {
            groupMobilityManager.processTimeout(timeoutEvent);
	 } else {
            unitToUnitMobilityManager.processTimeout(timeoutEvent);
         }
      }
   }

   public void processIOException(IOExceptionEvent ioex) {
      rfss.logError("Unexpected event -- IO Exception : " + ioex.getHost() + 
         ":" + ioex.getPort() + "/" + ioex.getTransport());
   }

   public void processTransactionTerminated( TransactionTerminatedEvent terminatedEvent) {
      if (terminatedEvent.isServerTransaction()) {
         ServerTransaction serverTransaction = terminatedEvent.getServerTransaction();
         Request request = serverTransaction.getRequest();
         MessageType type = getMessageType(request);
         if (type == MessageType.GROUP) {
            groupMobilityManager.processTransactionTerminated(terminatedEvent);
	 } else {
            unitToUnitMobilityManager.processTransactionTerminated(terminatedEvent);
         }
      } else {
         ClientTransaction clientTransaction = terminatedEvent.getClientTransaction();
         Request request = clientTransaction.getRequest();
         MessageType type = getMessageType(request);
         if (type == MessageType.GROUP) {
            groupMobilityManager.processTransactionTerminated(terminatedEvent);
         } else {
            unitToUnitMobilityManager.processTransactionTerminated(terminatedEvent);
         }
      }
   }

   public void processDialogTerminated(DialogTerminatedEvent arg0) {
      rfss.getTestHarness().fail("unexpected event");
   }

   //-------------------------------------------------------------------
   public UnitToUnitMobilityManager getUnitToUnitMobilityManager() {
      return unitToUnitMobilityManager;
   }

   public GroupMobilityManager getGroupMobilityManager() {
      return groupMobilityManager;
   }

   /**
    * Stop the membership scanner timer task.
    */
   public void stop() {
      membershipScanner.cancel();
   }
}
