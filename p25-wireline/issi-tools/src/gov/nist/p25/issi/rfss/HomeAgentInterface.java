//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.issiconfig.SuConfig;

/**
 * Each SU has a HomeAgent that is registered at the home RFSS.
 * 
 */
public interface HomeAgentInterface {

   public SuConfig getSuConfig();

   /**
    * This method is called when a BYE request comes in.
    * 
    * @param callTeardownIndicateEvent
    */
   void handleTeardownIndicate(CallTeardownIndicateEvent event);

   public void handleCallSetupResponseEvent(UnitToUnitCallControlResponseEvent event);

   public void handleTeardownResponse(CallTeardownResponseEvent event);

   public void setServiceAccessPoints(ServiceAccessPoints saps);

}
