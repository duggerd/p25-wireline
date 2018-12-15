//
package gov.nist.p25.issi.rfss;

import javax.sip.header.ContactHeader;

import gov.nist.p25.issi.issiconfig.SuConfig;

/**
 * This is the service access point for mobility management. 
 * This is essentially a provider for the MobiliytManager.
 */
public class MobilityManagementSAP {

   private RFSS rfss;
   private UnitToUnitMobilityManager mobilityManager;
   private SuConfig suConfig;
   
   // constructor
   public MobilityManagementSAP(RFSS rfss, SuConfig suConfig) {
      this.rfss = rfss;
      this.mobilityManager = rfss.getUnitToUnitMobilityManager();
      this.suConfig = suConfig;
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.MobilityManagementSAP#mmRegistrationRequest()
    */
   public void mmRegistrationRequest() throws Exception {
      mobilityManager.sendRegisterRegister(suConfig);
   }
   
   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.MobilityManagementSAP#mmRoamedRequest(gov.nist.p25.issi.rfss.CallSegment)
    */

   public void mmRoamedRequest(UnitToUnitCall unitToUnitCall,
         ContactHeader newContact, ContactHeader oldContact)
         throws Exception {
      mobilityManager.sendMMRoamed(this.suConfig, newContact, oldContact,
            (unitToUnitCall != null ? unitToUnitCall.getCallID(): null));
   }

   /**
    * Get the current registration.
    *
    * @return -- the contact header of the subscriber.
    */
   public ContactHeader getCurrentRegistration() {
      Registration registration = 
         mobilityManager.getRegistration(suConfig.getRadicalName());
      return registration.getContact();
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.MobilityManagementSAP#mmSuDeparted()
    */
   public void mmSuDeparted() {
      rfss.removeServedSubscriberUnit(suConfig);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.rfss.MobilityManagementSAP#mmSuArrived()
    */
   public void mmSuArrived() {
      rfss.addServedSubscriberUnit(suConfig);
   }
}
