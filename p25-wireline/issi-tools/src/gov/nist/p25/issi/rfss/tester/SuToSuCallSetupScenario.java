//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.p25body.serviceprofile.AvailabilityCheckType;
import gov.nist.p25.issi.p25body.serviceprofile.CallSetupPreferenceType;
import gov.nist.p25.issi.p25body.serviceprofile.DuplexityType;
//import gov.nist.p25.issi.p25body.serviceprofile.user.AvailabilityCheck;
//import gov.nist.p25.issi.p25body.serviceprofile.user.Duplexity;
import gov.nist.p25.issi.p25body.serviceprofile.user.UnitToUnitCallPriority;
import gov.nist.p25.issi.p25payload.TransmitPriorityType;

/**
 * SU to SU Setup Scenario
 */
public class SuToSuCallSetupScenario extends AbstractScenario {

   private SuConfig callingSuConfig;
   private SuConfig calledSuConfig;   
   private int priority;   
   private boolean isEmergency;   
   private boolean isFullDuplexCall;      
   private int transmitPriority;
   private TransmitPriorityType transmitPriorityType;
   private int cancelAfter;   
   
   private boolean terminatedByCalledServing;
   private SuConfig talkSpurtSender;
   private boolean forceFlag;   
   private boolean isProtectedCall;

   public SuToSuCallSetupScenario(SuConfig callingSu , SuConfig calledSu,
          boolean isEmergency, 
          int cancelAfter, int teardownAfter, boolean calledSuTearsDownCall,
          SuConfig talkSpurtSender, boolean forceFlag, boolean protectionFlag )
      throws Exception {

      if ( teardownAfter != -1 && cancelAfter != -1 ) 
         throw new Exception ("Only terminateAfter or cancelAfter may be specified!");
      this.callingSuConfig = callingSu;
      this.calledSuConfig = calledSu;
      this.priority = callingSu.getUserServiceProfile() == null ? UnitToUnitCallPriority.getDefaultValue() :
         callingSu.getUserServiceProfile().getUnitToUnitCallPriority().getPriority();
      this.isEmergency = isEmergency;

      // The caller prefers full duplex.
      this.isFullDuplexCall =  (callingSu.getUserServiceProfile().getDuplexity().getDuplex() == DuplexityType.FULL);
      this.talkSpurtSender = talkSpurtSender;
      this.forceFlag = forceFlag;
      this.isProtectedCall = protectionFlag;
      this.cancelAfter = cancelAfter;

      StringBuffer sbuf = new StringBuffer();
      sbuf.append(
            "Set up a unit to unit call with the following parameters : \n " +
            "==========================================================\n" +
            " calling Su = %x" + Integer.toHexString(callingSu.getSuId()) + 
	    " ( isEmulated = " + this.callingSuConfig.isEmulated() + " ) \n" +
            " calledSu = %x" + Integer.toHexString(calledSu.getSuId()) + 
	    "( isEmulated = " + this.callingSuConfig.isEmulated() + " ) \n" +
            " priority = " + priority + "\n" +
            " isEmergencyCall = " + isEmergency + "\n" +
            " isFullDuplexCall = " + isFullDuplexCall + "\n" +
            " isProtectedCall = " +  isProtectedCall + "\n");

      if ( cancelAfter != -1 ) {
         sbuf.append("\n and cancel it immediately. \n");
      } else if ( teardownAfter != -1 ) {
         sbuf.append("\n and hang up after succcessful call setup ");
         if ( calledSuTearsDownCall) {
            sbuf.append("from the SU %x" + Integer.toHexString( calledSuConfig.getSuId()) + "\n");
         } else {
            sbuf.append("from the SU %x" + Integer.toHexString( callingSuConfig.getSuId()) + "\n");
         }
      }
      super.setDescription(sbuf.toString());
   }

   /**
    * @return Returns the callingSuConfig.
    */
   public SuConfig getCallingSuConfig() {
      return callingSuConfig;
   }

   /**
    * @return Returns the calledSuConfig.
    */
   public SuConfig getCalledSuConfig() {
      return calledSuConfig;
   }

   /**
    * @return Returns the priority.
    */
   public int getPriority() {
      return priority;
   }   

   /**
    * @return Returns the isEmergency.
    */
   public boolean isEmergency() {
      return isEmergency;
   }
   
   public boolean isAvailCheckNeeded() {
      
      return callingSuConfig.getUserServiceProfile().getCallSetupPreference()
         .getCallSetupPreferenceType()
         .equals(CallSetupPreferenceType.CALLER_PREFERS_AVAILABILITY_CHECK);
   }
   
   public boolean isFullDuplexCall() {
      return isFullDuplexCall;      
   }
   
   public boolean isAvailabilityCheckSupported() {
      return calledSuConfig.getUserServiceProfile().getAvailabilityCheck()
         .equals(AvailabilityCheckType.AVAIL_CHECK_ONLY) ||
         calledSuConfig.getUserServiceProfile().getAvailabilityCheck()
         .equals(AvailabilityCheckType.AVAIL_CHECK_AND_DIRECT_CALL);
   }
   
   /**
    * @returns the transmit priority can vary over the length of the call.
    */
   public int getTransmitPriority() {
      return transmitPriority;
   }

   /**
    * Get the priority type of the transmission.
    * 
    * @return the transmit priority type.
    */
   public TransmitPriorityType getTransmitPriorityType() {
      return transmitPriorityType;
   }

   /**
    * @return the cancelAfter
    */
   public int getCancelAfter() {
      return cancelAfter;
   }   

   /**
    * Return the flag which indicates that called serving terminates call.
    * 
    * @return
    */
   public boolean isTerminatedByCalledServing() {
      return terminatedByCalledServing;
   }

   public SuConfig getTalkSpurtSender() {
      return talkSpurtSender;
   }
   
   public boolean isForce() {
      return forceFlag;
   }

   public boolean isProtectedCall() {
      return isProtectedCall;
   }
}
