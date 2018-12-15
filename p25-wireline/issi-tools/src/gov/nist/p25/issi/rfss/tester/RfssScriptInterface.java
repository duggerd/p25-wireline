//
package gov.nist.p25.issi.rfss.tester;

/**
 * Rfss Script Interface
 */
public interface RfssScriptInterface {

   /**
    * Stop the heartbeat transmission to a given RFSS from the group home RFSS.
    * 
    * @param groupName --
    *            the group name for which we want to stop the transmission.
    * @param rfssId --
    *            the RFSS we want to stop transmission to.
    * @throws Exception --
    *             if we cannot find the group or if this method is called from
    *             something other than the home RFSS.
    */
   public abstract void stopHeartbeatTransmissionFromGroupHome(
         String groupName, String rfssId) throws Exception;

   /**
    * Stop an RSS heartbeat from a Serving RFSS of a group.
    * 
    * @param groupName --
    *            the group name.
    * 
    * @throws Exception --
    *             if we are not at a served Rfss of this group.
    * 
    */
   public abstract void stopHeartbeatTransmissionFromGroupServing(
         String groupName) throws Exception;

   /**
    * issue a heartbeat query from this rfss to a specific group.
    * 
    * @param groupName -- the symbolic name of the group.
    */
   public abstract void pttHeartbeatQuery(String groupName) throws Exception;

   /**
    * Issue a registration query for the given named SU.
    * 
    * @param suName --
    *            Name of the Su to query.
    * @param force --
    *            register force parameter should be "force" or "noforce"
    * @param confirm --
    *            register confirm parameter should be "confirm" or "noconfirm"
    * 
    */
   public abstract void suHomeQuery(String suName, String forceString,
         String confirmString) throws Exception;

   /**
    * Issue a home query from the group home to the group served.
    * 
    * @param groupName -- name of the group to query.
    * @param targetRfssName -- name of the served RFSS.
    * @param forceString -- force parameter.
    * @param confirmString -- confirm parameter.
    * 
    */
   public abstract void groupHomeQuery(String groupName,
         String targetRfssName, String forceString, String confirmString)
         throws Exception;

   /**
    * Do an su query from a serving to home.
    * 
    * @param suName --
    *            name of the Su to query.
    */
   public abstract void querySu(String suName) throws Exception;

   /**
    * Issue a registration query to the home of the group.
    * 
    * @param groupName -
    *            name of group to query
    * @throws Exception*
    */
   public abstract void queryGroup(String groupName) throws Exception;

   /**
    * Reinvite an RFSS to a group call.
    * 
    * @param groupName --
    *            name of group housed at this rfss.
    * @param rfssName --
    *            rfss to reinvite to group call
    */
   public abstract void reInviteRfssToGroup(String groupName, String rfssName)
         throws Exception;

   /**
    * Issue a RegisterRegister  for a given su
    * 
    * @param suName -- the su name.
    * @param assertPresence -- the r-presence flag
    */
   public abstract void triggerRegistration(String suName, String presenceFlag) throws Exception;
   /**
    * Change the group call priority.
    * 
    * @param groupName --
    *            name of the group.
    * @param newLevel --
    *            new level.
    * @param emergency --
    *            whether emergency or not.
    */
   public abstract void changeGroupCallPriority(String groupName,
         int newPriority, String emergency) throws Exception;

   /**
    * Enable RF resources at this RFSS.
    * 
    */
   public abstract void enableRfResources() throws Exception;

   /**
    * Disable RF resources at this rfss.
    * 
    */
   public abstract void disableRfResources() throws Exception;

   /**
    * Add an RTP port ( resource )
    * 
    * @param nports
    */
   public abstract void addRtpPort(int nports);

   /**
    * Remove RTP ports.
    * 
    * @param ports -- number of ports to remove
    */
   public abstract void removeRtpPort(int ports);

   //--------------------------------------------
   public void muteFromGroupHome(String groupName, String rfssId) 
      throws Exception;

   public void unmuteFromGroupHome(String groupName, String rfssId, long msec)
      throws Exception;
}
