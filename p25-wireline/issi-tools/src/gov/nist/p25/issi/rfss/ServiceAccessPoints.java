//
package gov.nist.p25.issi.rfss;

//import gov.nist.p25.issi.transctlmgr.TransmissionControlSAP;

/**
 * A collection of Service Access Points - these are passed to the SU when the
 * SU is created. When we implement this in the SLEE, we will pass this in as
 * part of the environment. For now, we pass it in as part of the constructor of
 * the SU instance.
 * 
 */
public class ServiceAccessPoints {

   private String sapName = "SAPS";
   private CallControlSAP callControlSAP;
   private MobilityManagementSAP mobilityManagementSAP;
   private TransmissionControlSAP transmissionControlSAP;

   // constructor
   public ServiceAccessPoints(CallControlSAP callControlSAP,
         MobilityManagementSAP mobilityManagementSAP,
         TransmissionControlSAP transmissionControlSAP) {
      this.callControlSAP = callControlSAP;
      this.mobilityManagementSAP = mobilityManagementSAP;
      this.transmissionControlSAP = transmissionControlSAP;
   }

   public CallControlSAP getCallControlSAP() {
      return callControlSAP;
   }

   /***
   void setMobilityManagementSAP(MobilityManagementSAP mobilityManagementSAP) {
      this.mobilityManagementSAP = mobilityManagementSAP;
   }
    ***/
   public MobilityManagementSAP getMobilityManagementSAP() {
      return mobilityManagementSAP;
   }

   public TransmissionControlSAP getTransmissionControlSAP() {
      return transmissionControlSAP;
   }

   // Added for debug
   public void setSapName(String id) {
      sapName = id;
   }
   public String getSapName() {
      return sapName;
   }

   public String toString() {
      return getSapName()+": ["+ 
         getCallControlSAP().getRfssConfig().getRfssName()+":"+
         getCallControlSAP().getSuConfig().getSuName()+"-"+
         getCallControlSAP().getSuConfig().getInitialServingRfss().getRfssName()+
         "]";
   }
}
