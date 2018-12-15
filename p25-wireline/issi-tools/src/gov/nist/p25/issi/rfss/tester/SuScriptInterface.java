package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.SuState;

/**
 * Su Script Interface
 */
public interface SuScriptInterface {

   /**
    * Terminate current SU to Su call segment.
    */
   public abstract void terminateCallSegment();

   /**
    * Read nblocks from the file and send it.
    * 
    * @param nblocks -- number of blocks to read.
    */
   public abstract void sendTalkSpurt(int nblocks);

   /**
    * Send talk spurt to an su in an su to su call.
    */
   public abstract void sendTalkSpurt();

   /**
    * Force a talk spurt to the other end. This sends a progress packet without
    * a START
    */
   public abstract void forceTalkSpurt(int nblocks);

   /**
    * Change the ON / OFF state of an SU.
    * 
    * @param suState -- SuState.ON or SuState.OFF 
    */
   public abstract void setState(SuState suState);

   /**
    * Send a talk spurt of n blocks length to the group.
    * 
    * @param groupName - group name to send the talk spurt to.
    * @param nblocks -- number of blocks to spend.
    * @throws Exception -- if an error occured while sending the talk spurt.
    */
   public abstract void sendTalkSpurt(String groupName, int nblocks)
         throws Exception;

   /**
    * Read the entire set of blocks from the file and send the whole thing to the group.
    * 
    * @param groupName -- group name to send the talk spurt to.
    * 
    * @throws Exception
    */
   public abstract void sendTalkSpurt(String groupName) throws Exception;

   /**
    * force a talk spurt to the group.
    * 
    * @param groupName -- group name to force talk spurt to.
    * @param nblocks -- number of blocks to force.
    * 
    * @throws Exception -- if any problem occured while sending the talk spurt.
    */
   public abstract void forceTalkSpurt(String groupName, int nblocks)
         throws Exception;

   /**
    * force a talk spurt to the group.
    * 
    * @param groupName -- group name to force talk spurt to.
    * @param nblocks -- number of blocks to force.
    * 
    * @throws Exception -- if any problem occured while sending the talk spurt.
    */
   public abstract void forceTalkSpurt(String groupName) throws Exception;

   /**
    * Set the type and level of the transmit priority for subsequent
    * transmissions
    * 
    * @param transmitPriority -- the transmit priority.
    * @param level -- the level.
    * 
    * @throws Exception - if the type or level are incorrect.
    */
   public abstract void setTransmitPriority(String type, int level)
         throws Exception;
   
   /**
    * send a heartbeat query from the serving rfss to the home rfss.
    * Note that usually the home sends a heartbeat query to the
    * serving rfss. This support is provided for the test case that
    * requires the opposite mechanism to be exercised.
    */
   public void sendHeartbeatQuery();

   //-------------------------------------
   public void sendMute(String groupName) throws Exception;

   public void sendUnmute(String groupName,long msec) throws Exception;

   public void sleep(long msec);
   //-------------------------------------
}
