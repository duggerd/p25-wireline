//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;

/**
 * The sccenario description for group call setup test case.
 * 
 */
public class GroupCallSetupScenario extends AbstractScenario {

   private boolean isEmergency;
   private boolean isConfirmed;
   private boolean talkSpurtSentAfterCallSetup;
   private boolean talkSpurtForced;
   private boolean isProtected;

   private int priority;
   private int cancelAfter;
   private int terminateAfter;
   private SuConfig callingSuConfig;
   private GroupConfig calledGroupConfig;
   
   // constructor
   public GroupCallSetupScenario( SuConfig callingSu, GroupConfig calledGroup,
         int priority, int cancelAfter, int terminateAfter, 
         boolean isEmergency, boolean isConfirmed, boolean sendTalkSpurtAfterCallSetup,
         boolean isTalkSpurtForced, boolean isProtected)
      throws Exception {
      if ( cancelAfter != -1 && terminateAfter != -1 ) {
         throw new Exception("Cannot have both cancelAfter and terminateAfter specified");
      }
      this.isProtected = isProtected;
      this.callingSuConfig = callingSu;
      this.calledGroupConfig = calledGroup;
      this.isEmergency = isEmergency;
      this.isConfirmed = isConfirmed;
      this.cancelAfter = cancelAfter;
      this.terminateAfter = terminateAfter;
      this.talkSpurtSentAfterCallSetup = sendTalkSpurtAfterCallSetup;
      this.talkSpurtForced = isTalkSpurtForced;
      this.priority = priority;

      StringBuffer descr = new StringBuffer();
      descr.append("Setup a group call with the following parameters : \n" +
         "calling SU Id = " + Integer.toHexString(callingSuConfig.getSuId()) + "(hex)\n" +
         "called group Id = " + Integer.toHexString(calledGroupConfig.getGroupId()) + "(hex)\n" +
         "priority = " + priority  + "\n" +
         "isEmergency = " + isEmergency  + "\n");
      if ( cancelAfter != -1 ) {
        descr.append ("Cancel Call after " + cancelAfter + "\n" );
      }
      SuConfig talkSpurtSender = callingSu;

      if ( talkSpurtSender != null) {
	 descr.append((!talkSpurtForced ? "Send" : "FORCE"));
	 descr.append(" a talk spurt from the following SU " + Integer.toHexString(talkSpurtSender.getSuId()) + "(hex)\n");
      }
      if ( terminateAfter != -1 ) {
	 descr.append("Terminate call after " + terminateAfter + "\n");
      }
      super.setDescription(descr.toString());
   }
   
   /**
    * Get the radical name of the called group.
    * 
    * @return -- the called group radical name.
    */
   public String getCalledGroupName() {
      return calledGroupConfig.getGroupName();
   }

   /**
    * @return Returns the callingSu.
    */
   public SuConfig getCallingSuConfig() {
      return callingSuConfig;
   }

   /**
    * @return Returns the calledGroup.
    */
   public GroupConfig getCalledGroupConfig() {
      return calledGroupConfig;
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
   
   /**
    * @return Returns the isConfirmed.
    */
   public boolean isConfirmed() {
      return isConfirmed;
   }

   /**
    * @return the cancelAfter
    */
   public int getCancelAfter() {
      return cancelAfter;
   }

   public int getTerminateAfter() {
      return terminateAfter;
   }
   
   public boolean isTalkSpurtForced() {
      return talkSpurtForced;
   }

   public boolean isTalkSpurtSentAfterCallSetup() {
      return talkSpurtSentAfterCallSetup;
   }

   public boolean isProtected() {
      return isProtected;
   }
}
