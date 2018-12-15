//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.SuState;

/**
 * Test SU Interface
 */
public interface TestSUInterface {

   /**
    * Send a talk spurt to the Peer of an su to su call.
    * 
    */
   public abstract void sendTalkSpurt(int nblocks);
   
   /**
    * Send the entire file to the peer of an SU to SU call.
    */
   public abstract void sendTalkSpurt();
   
   
   /**
    * Send talk spurt to a group.
    * 
    * @param groupRadicalName -- radical name of group to send the talk spurt to.
    * 
    */
   public abstract void sendTalkSpurt(String groupRadicalName,int nblocks);

   /**
    * Send entire file to a group.
    * 
    * @param groupRadicalName -- radical name of group to send talk spurt to.
    */
   public abstract void sendTalkSpurt(String groupRadicalName);

   /**
    * Terminate the current call segment.
    * 
    */
   public abstract void terminateUnitToUnitCall();

   /**
    * Set the current state of the SU ( on or off ).
    * 
    * @param suState -- the state of the SU.
    */
   public void setState(SuState suState);
   
   /**
    * Cancel the current SU to SU call segment.
    */
   public void cancelUnitToUnitCall() ;
   
   /**
    * Force a talk spurt.
    */
   public void forceTalkSpurt();
   
   /**
    * Force a talk spurt n blocks long
    */
   public void forceTalkSpurt(int nblocks);
}
