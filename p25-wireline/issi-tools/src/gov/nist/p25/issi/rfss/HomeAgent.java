//
package gov.nist.p25.issi.rfss;

import java.util.EventObject;

import javax.sip.ResponseEvent;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;

/**
 * This is the controlling SU that runs at the HOME rfss of the SU. It keeps the
 * state associated with the SU and does whatever actions need to be performed
 * at the home RFSS. Each SU has a controlling SU registered at the Home RFSS
 * for this purpose.
 * 
 */
@SuppressWarnings("unused")
public class HomeAgent implements HomeAgentInterface {
   private static Logger logger = Logger.getLogger(HomeAgent.class);

   private ContactHeader oldContact;
   private EventObject pendingEvent;
   private RFSS homeRfss;
   private ServiceAccessPoints saps;
   private SuConfig suConfig;
   private TopologyConfig topologyConfig;
   private UnitToUnitCall unitToUnitCall;
   private boolean pendingCallSetupRequest;

   // constructor
   public HomeAgent(SuConfig suConfig, TopologyConfig topologyConfig, RFSS homeRfss) {
      this.suConfig = suConfig;
      this.topologyConfig = topologyConfig;
      this.homeRfss = homeRfss;
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.HomeAgentInterface#handleCallSetupResponseEvent(
    * gov.nist.p25.issi.rfss.UnitToUnitCallControlResponseEvent)
    */
   public void handleCallSetupResponseEvent(UnitToUnitCallControlResponseEvent ccResponseEvent) {
      try {
         ResponseEvent responseEvent = ccResponseEvent.getResponseEvent();
         if (pendingCallSetupRequest) {
            if (responseEvent.getResponse().getStatusCode() / 100 == 2) {
               unitToUnitCall = ccResponseEvent.getCallSegment();
               saps.getCallControlSAP().ccSetupConfirm(ccResponseEvent);
            }
         } else {
            // Store the call segment in our state.
            if (responseEvent.getResponse().getStatusCode() / 100 == 2) {
               unitToUnitCall = ccResponseEvent.getCallSegment();
            }
         }
      } catch (Exception ex) {
         throw new RuntimeException("Unexpected exception", ex);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.HomeAgentInterface#handleTeardownResponse(gov.nist.p25.issi.rfss.CallTeardownResponseEvent)
    */
   public void handleTeardownResponse(CallTeardownResponseEvent ccTeardownEvent) {
      try {
         logger.info("HomeAgent: got a call teardown response "
               + "... sending REinvite at the new called serving");

         // We initated a teardown because of the SU moving.
         if (pendingEvent  != null && pendingEvent instanceof RegisterEvent) {

            RegisterEvent registerEvent = (RegisterEvent) pendingEvent;
            Request request = registerEvent.getServerTransaction().getRequest();
            if (registerEvent.getEventType() == RegisterEvent.REGISTER_REGISTER) {

               this.homeRfss.getTestHarness().assertTrue(
                     this.suConfig == this.unitToUnitCall.getCalledSuConfig()
                  || this.suConfig == this.unitToUnitCall.getCallingSuConfig());
               this.homeRfss.getTestHarness().assertTrue(
                  this.suConfig.getHomeRfss() == this.saps.getCallControlSAP().getRfssConfig());

               // Get the contact header from the REGISTER request that we have stored away
               ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);

logger.debug("HomeAgent: handleTeardownResponse: suConfig="+getSuConfig());
logger.debug("HomeAgent: handleTeardownResponse: oldContact="+oldContact);
logger.debug("HomeAgent: handleTeardownResponse: contactHeader="+contactHeader);

               saps.getMobilityManagementSAP().mmRoamedRequest(
                     unitToUnitCall, contactHeader, oldContact);

               CallControlSAP ccSap = saps.getCallControlSAP();

               // Re-invite myself at my new location
               pendingCallSetupRequest = true;
               pendingEvent = null;

               UnitToUnitCall xunitToUnitCall = ccTeardownEvent.getCallSegment();
               //logger.debug("Unit to unit call teardown " + xunitToUnitCall);

/***
logger.debug("HomeAgent: handleTeardownResponse: ++++++++++++++++++++setSAPS()");
ServiceAccessPoints xsaps = getSuConfig().getSU().getServiceAccessPoints();
if(xsaps == null) {
   logger.debug("HomeAgent: handleTeardownResponse: wire saps="+saps);
   getSuConfig().getSU().setServiceAccessPoints(saps);
}
logger.debug("HomeAgent: handleTeardownResponse: ++++++++++++++++++++setSAPS()");
 ***/
logger.debug("HomeAgent: handleTeardownResponse: ccSendCallSetupInCallRomaingRequest...");
               ccSap.ccSendCallSetupInCallRoamingRequest(getSuConfig(),
                     xunitToUnitCall,
                     ((SipURI) oldContact.getAddress().getURI()).getHost(),
                     (Address) contactHeader.getAddress().clone());               
            }
         }
      } catch (Exception ex) {
         logger.error("HomeAgent: Unexpected exception ",ex);
         throw new RuntimeException("Unexpected exception", ex);
      }
   }
   
   public void setServiceAccessPoints(ServiceAccessPoints saps) {
      this.saps = saps;
      logger.debug("HomeAgent: "+suConfig.getSuName() +
                   "  SET ServiceAccessPoint-saps="+saps);
   }

   public void handleRegistrationEvent(RegisterEvent registerEvent) {
      try {
         logger.info("Got a register event - type="+registerEvent.getEventType());

         // This method gets invoked at the home RFSS when the registration state changes.
         if (registerEvent.getEventType() == RegisterEvent.REGISTER_REGISTER) {
            oldContact =  saps.getMobilityManagementSAP().getCurrentRegistration();
            Request request = registerEvent.getServerTransaction().getRequest();
            ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
            SipURI toURI = (SipURI) toHeader.getAddress().getURI();
            String radicalName = toURI.getUser();
            logger.debug("REGISTER_REGISTER: radicalName="+radicalName);

            if (unitToUnitCall != null) {
               logger.debug("REGISTER_REGISTER: ccTeardownCallSegment");

               //#146 12.13.1 didnot need to teardown !!!
               //#146 12.23.1 didnot need to teardown !!!
               //NOTE: if commented out, this will truncate 12.25.1 messages
               pendingEvent = registerEvent;
               saps.getCallControlSAP().ccTeardownCallSegment( this, unitToUnitCall, radicalName);

            } else {

               // 13.1.1 needs a TeardownCallSegment 
               ContactHeader contactHeader = (ContactHeader) request.getHeader(ContactHeader.NAME);
               if (logger.isDebugEnabled()) {
                  logger.debug("old contact=[" + oldContact +"]");
                  logger.debug("new contact=[" + contactHeader +"]");
               }
               logger.debug("HomeAgent: compare ContactHeader... use compareContactHeader()");
               //logger.debug("HomeAgent: compare ContactHeader... use equals()");
               // Test 4.1.1 
               // we need a flexible way to compare oldContact vs contactHeader !!
               //if (!oldContact.equals(contactHeader)) {
               if (!compareContactHeader( oldContact, contactHeader)) {
                  homeRfss.getMobilityManager().getUnitToUnitMobilityManager().sendMMRoamed(
                              suConfig, contactHeader, oldContact, null);
               }
            }
         }
      } catch (Exception ex) {
         logger.error("unexpected exception", ex);
         homeRfss.logError("unexpected exception ex", ex);
      }
   }

   private boolean compareContactHeader(ContactHeader oldContact, ContactHeader contactHeader)
   {
      // old contact == Contact: <sip:02.001.00001.p25dr;user=TIA-P25-SU>
      // new contact == Contact: <sip:00002002000012@02.001.00001.p25dr;user=TIA-P25-SU>
      boolean match = false;
      String prefix = "Contact: <sip:";
      int oldIdx = oldContact.toString().indexOf("@");
      int newIdx = contactHeader.toString().indexOf("@");
      //logger.debug(" >>>>>> oldIdx=" + oldIdx + " newIdx=" + newIdx);
      if( oldIdx == -1) {
         if( newIdx == -1) {
            match = oldContact.toString().equals(contactHeader.toString());
         }
         else {
            // 02.001.00001.p25dr;user=TIA-P25-SU>
            String newStr = prefix + contactHeader.toString().substring(newIdx+1);
            match = oldContact.toString().trim().equals(newStr.trim());
            logger.debug("newStr=[" + newStr +"]");
         }
      }
      else {
         if( newIdx == -1) {
            String oldStr = prefix + oldContact.toString().substring(oldIdx+1);
            match = contactHeader.toString().trim().equals(oldStr.trim());
            logger.debug("oldStr=[" + oldStr +"]");
         }
         else {
            match = oldContact.toString().equals(contactHeader.toString());
         }
      }
      logger.debug(" >>>>>> match=" + match);
      return match;
   }

   /***
   public ServiceAccessPoints getServiceAccessPoints() {
      logger.debug("HomeAgent: "+suConfig.getSuName() +
                   " GET ServiceAccessPoint-saps="+saps);
      return saps;
   }
    ***/

   public SuConfig getSuConfig() {
      return suConfig;
   }

   public void handleDeRegistrationEvent(RegisterEvent event) {
      logger.debug("HomeAgent: handleDeRegistrationEvent");
      try {
         homeRfss.getMobilityManager().getUnitToUnitMobilityManager()
               .sendRegisterResponse(event, Response.OK);
      } catch (Exception ex) {
         logger.error("unexpected exception", ex);
         homeRfss.getTestHarness().fail("unexpected exception", ex);
      }
   }

   public void handleTeardownIndicate( CallTeardownIndicateEvent callTeardownIndicateEvent) {
      logger.debug("HomeAgent: handleTeardownIndicate");
   }
}
