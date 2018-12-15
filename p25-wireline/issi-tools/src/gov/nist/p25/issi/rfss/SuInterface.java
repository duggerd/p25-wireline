//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SuState;

/**
 * Thie main Subscriber Unit interface. The TestSU implements this interface and
 * interacts with the system, expecting to get responses for requests according
 * to the test case at hand.
 * 
 */
public interface SuInterface extends HomeAgentInterface {

   /**
    * This method is called at the home agent only when a RegisterDeregister
    * event comes in during mobility
    * 
    * @param registerDeregisterEvent --
    *            the register-deregister event for mobility.
    */
   public void handleRegistrationEvent(RegisterEvent registerRegister);

   /**
    * Response to a registration request or register-deregister request.
    * 
    * @param registrationResponseEvent
    */

   public void handleRegistrationResponseEvent( RegistrationResponseEvent registrationResponseEvent);

   /**
    * Handle a dergister request.
    */
   public void handleDeRegistrationEvent(RegisterEvent event);

   /**
    * handle the MM Roamed Indicte.
    * 
    * @param
    */
   public void handleMMRoamedEvent(RegisterEvent deregistrationRequestEvent);

   /**
    * Handle the OK when an SU dergisters
    * 
    * @param deregistrationResponseEvent
    */
   public void handleMMRoamedResponseEvent( DeregistrationResponseEvent deregistrationResponseEvent);

   /**
    * Called for the intermediate and final responses for the incoming invite.
    * 
    * @param ccResponseEvent
    */
   public void handleCallSetupResponseEvent( UnitToUnitCallControlResponseEvent ccResponseEvent);

   /**
    * This method is called on the server side of a dialog when an incoming
    * INVITE is detected
    * 
    * @param ccRequestEvent
    */
   public void handleSetupIndicate(CallSetupRequestEvent ccRequestEvent);

   /**
    * This method is called by the call Control Manager when an ok is receieved
    * for the BYE
    * 
    * @param ccTeardownEvent
    */
   public void handleTeardownResponse(CallTeardownResponseEvent ccTeardownEvent);

   /**
    * This method is called by the CallControlManager when an ACK is received
    * at the SU
    * 
    * @param callSetupConfirmEvent
    */
   public void handleCallSetupConfirmEvent(CallSetupConfirmEvent callSetupConfirmEvent);

   /**
    * Called by the test framework to setup the service access points for the
    * su.
    * 
    * @param saps
    */
   public void setServiceAccessPoints(ServiceAccessPoints saps);

   /**
    * Get the service accecss points for this SU.
    * 
    * @return the service access points.
    */
   public ServiceAccessPoints getServiceAccessPoints();

   /**
    * Get the SU configuration for a given SU
    * 
    * @return the su configuration.
    */
   public SuConfig getSuConfig();

   /**
    * Called when a response to the deregistration request arrives
    * 
    * @param registrationResponseEvent
    */
   public void handleDeRegistrationResponseEvent( DeregistrationResponseEvent registrationResponseEvent);

   /**
    * Return the current state of the SU.
    * 
    * @return
    */
   public SuState getState();
   
   /**
    * Set the current state of the SU.
    * 
    */
   public void setState(SuState newState);
   
   /**
    * Get the current unit to unit call
    */
   public UnitToUnitCall getCurrentUnitToUnitCall();
}
