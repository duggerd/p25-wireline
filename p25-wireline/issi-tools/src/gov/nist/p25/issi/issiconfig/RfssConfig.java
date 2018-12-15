//
package gov.nist.p25.issi.issiconfig;

import gov.nist.p25.issi.rfss.RFSS;
import gov.nist.p25.issi.utils.EndPointHelper;
import gov.nist.p25.issi.utils.ReadOnlyIterator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 * This class represents the static configuration of an RFSS. This is
 * constructed by reading the configuraiton information of the emulator
 * from the XML topology configuration file.
 */
public class RfssConfig implements Comparable<RfssConfig>
{
   private static Logger logger = Logger.getLogger(RfssConfig.class);
   
   private String rfssName;
   private int rfssId;
   private String address;
   private String ipAddress;
   private int sipPort;
   private boolean isEmulated;

   private boolean advancedRtpResourceManagementSupported;   
   private SystemConfig systemConfig;
   
   private boolean pttRequestGranted;
   private transient RFSS rfss;

   // List of SU for which this RFSS is home.
   private Hashtable<String, SuConfig> assignedSu= new Hashtable<String, SuConfig>() ;
   private ConcurrentHashMap<String, SuConfig> initialServedSubscriberUnits = 
      new ConcurrentHashMap<String, SuConfig>();
   private Hashtable<String, GroupConfig> groups= new Hashtable<String, GroupConfig>();
   private HashSet<SuConfig> unknownSuTable = new HashSet<SuConfig>();

   // The time for which a RFSS is registered with us
   // Assuming we are the home rfss of a group.
   // after this time we will drop the registration.
   private int groupRegistrationExpiresTime;
   private int servedGroupLeaseTime;   
   private int groupCallInviteProcessingTime;
   private boolean rfResourcesAvailable;   
   private int maxRtpPorts;

   private boolean suCredentialsQueriedBeforeRegister;
   private boolean groupCredentialsQueriedBeforeRegister;
   private int registerConfirmTime;   
   private boolean notInterestedInLosingAudio = true;
   
   private boolean isCallingSuInitialTransmitter;
   private String tag;
   private TopologyConfig topologyConfig;
   private String colorString = "BLACK";
   private int selfTestPort;
         
   /**
    * Construct a RfssConfig object.
    * 
    * @param topologyConfig -- topology configuration.
    * @param systemConfig  -- system configuration
    * @param rfssId   -- RFSS ID
    * @param symbolicName -- the symbolic id for this RFSS.
    * @param isEmulated -- the emulated flag ( false means not emulated ).
    * 
    */
   public RfssConfig(TopologyConfig topologyConfig, SystemConfig systemConfig,
      int rfssId, String ipAddress, int port, String symbolicName, boolean isEmulated) {
      
      if (rfssId > 0xff) {
         throw new IllegalArgumentException("Bad RFSS ID  " + rfssId);
      }
      this.ipAddress = ipAddress;
      this.sipPort = port;
      this.rfssName = symbolicName;
      this.systemConfig = systemConfig;
      this.topologyConfig = topologyConfig;
      this.isEmulated = isEmulated;
      this.rfssId = rfssId;
      systemConfig.setAssigned(true);
      
      // Default behaviors.
      this.groupRegistrationExpiresTime = -1;
      this.servedGroupLeaseTime = -1;
      this.groupCallInviteProcessingTime = 0;   
      this.rfResourcesAvailable = true;
      this.maxRtpPorts = -1;
      this.advancedRtpResourceManagementSupported = false;
      this.suCredentialsQueriedBeforeRegister = false;
      this.groupCredentialsQueriedBeforeRegister = false;
      this.pttRequestGranted = true;
      this.registerConfirmTime = 0;
      this.notInterestedInLosingAudio = true;
      this.isCallingSuInitialTransmitter = true;
   }
   
   /**
    * Get an iterator to the Su's for which this Rfss is the home subscriber
    * unit.
    * 
    * @return
    */
   public Iterator<SuConfig> getHomeSubsciberUnits() {
      return new ReadOnlyIterator<SuConfig>(this.assignedSu.values().iterator());
   }

   /**
    * Get an iterator to the groups assigned to the Rfss.
    * 
    * @return
    */
   public Iterator<GroupConfig> getAssignedGroups() {
      return new ReadOnlyIterator<GroupConfig>(this.groups.values().iterator());
   }

   /**
    * Add a subscriber unit to the server set.
    * 
    * @param suConfig
    */
   public void addServedSubscriberUnit(SuConfig suConfig) {
      initialServedSubscriberUnits.put(suConfig.getRadicalName(), suConfig);
   }

   /**
    * Add a subscriber unit to the home set.
    * 
    * @param suConfig
    */
   public void addAssignedSubscriberUnit(SuConfig suConfig) {
      String key = suConfig.getRadicalName();
      if (suConfig.getHomeRfssKnowsAboutMeFor() !=  0) {
         if (logger.isDebugEnabled()) {
            logger.debug("addAssignedSubsriberUnit : " + key);
         }
         assignedSu.put(key, suConfig);

      } else {
         if (logger.isDebugEnabled()) {
            logger.debug("SU is not a known SU -- not adding to the assigned Subscriber unit list");
         }
      }
   }

   /**
    * Return a read-only iterator over the set of Subscriber units that are
    * initially assigned to us at emulation startup.
    * 
    * @return -- iterator over subscriber units that are assigned to us.
    */
   public Collection<SuConfig> getInitialServedSubscriberUnits() {
      return this.initialServedSubscriberUnits.values();
   }

   public static String getDomainName(SystemConfig systemConfig, int rfssId) {
      return getDomainName(systemConfig.getWacnId(), systemConfig.getSystemId(), rfssId);
   }
   
   public static String getDomainName(int wacnId, int systemId, int rfssId)  {
      String systemName = SystemConfig.getSystemName(wacnId, systemId);
      String rfssIdString = Integer.toHexString(rfssId);
      int k = 2 - rfssIdString.length();
      for (int i = 0; i < k; i++) {
         rfssIdString = "0" + rfssIdString;
      }
      String rfssDomainName = rfssIdString + "." + systemName + "." + "p25dr";
      return rfssDomainName.toLowerCase();
   }

   public String getDomainName() {      
      return  getDomainName(this.systemConfig, this.rfssId);      
   }

   public SuConfig getSubscriberUnit(int suId) {
      String key = SuConfig.getRadicalName(this.systemConfig, suId);
      logger.debug("RfssConfig: getSubscriberUnit(): " + key);
      return (SuConfig) this.assignedSu.get(key);
   }

   public SuConfig getSubscriberUnit(String radicalName) {
      return (SuConfig) this.assignedSu.get(radicalName);
   }

   /**
    * Set a pointer to the dynamic emulation structure for RFSS.
    * 
    * @param rfss -- the dynamic emulation structure for an RFSS.
    */
   public void setRFSS(RFSS rfss) {
      if (rfss == null) {
         throw new NullPointerException("Null arg !");
      }
      // if (this.rfss != null) throw new IllegalStateException("Already Initialized");
      this.rfss = rfss;
   }

   /**
    * Return the Runtime RFSS structure corresponding to this Configuration
    * Structure.
    * 
    * @return
    */
   public RFSS getRFSS() {
      return rfss;
   }

   public SystemConfig getSysConfig() {
      return systemConfig;
   }
   
   public String getSystemName() {
      return systemConfig.getSystemName();
   }
      
   public void setSystemName(String systemName)  {
      try {
         this.systemConfig = topologyConfig.getSystemConfigByName(systemName);
      } catch (Exception ex) {
         throw new IllegalArgumentException("System not found " + systemName,ex);
      }
   }

   public String getHostPort() {
      return ipAddress + ":" + sipPort;
   }

   public int getWacnId() {
      return systemConfig.getWacnId();
   }
   public void setWacnId(int wacnId) {
      systemConfig.setWacnId( wacnId);
   }
   public String getWacnIdString() {
      return EndPointHelper.encodeWacnId(systemConfig.getWacnId());
   }

   public int getSystemId() {
      return systemConfig.getSystemId();
   }
   public void setSystemId(int systemId) {
      systemConfig.setSystemId(systemId);
   }
   public String getSystemIdString() {
      return EndPointHelper.encodeSystemId(systemConfig.getSystemId());
   }

   /**
    * Add a group for which I am the home RFSS.
    * 
    * @param groupConfig
    */
   public void addAssignedGroup(GroupConfig groupConfig) {
      if (groupConfig.getHomeRfssKnowsAboutMeFor() != 0) {
         groups.put(groupConfig.getRadicalName(), groupConfig);
      }
   }

   /**
    * Return true if a group is assigned to me.
    * 
    * @param groupId -- the group id.
    * @return true if a given group (identified by the group id ) is assigned
    *         to me.
    */
   public boolean isAssignedGroup(String groupId) {
      return groups.containsKey(groupId);
   }

   /**
    * Get a group from our assigned groups.
    * 
    * @param groupId
    * @return -- the group configuration.
    */
   public GroupConfig getAssignedGroup(String groupId) {
      return groups.get(groupId);
   }

   public String getConsoleLog() {
      return "rfss" + getRfssId() + "consolelog.txt";
   }

   public boolean isSubscriberUnitMine(SuConfig suConfig) {
      return assignedSu.contains(suConfig);
   }

   /**
    * Return true if a group belongs to us.
    * 
    * @param groupConfig
    * @return
    */
   public boolean isGroupMine(GroupConfig groupConfig) {
      return groups.containsKey(groupConfig.getRadicalName());
   }

   /**
    * Return true if this RFSS is emulated.
    * 
    * @return
    */
   public boolean isEmulated() {
      return isEmulated;
   }
   public boolean getEmulated() {
      return isEmulated;
   }
   public void setEmulated(boolean emulatedFlag) {
      this.isEmulated = emulatedFlag;
   }

   public String getRfssName() {
      return rfssName;
   }
   public void setRfssName(String rfssName) {
      this.rfssName = rfssName;
   }

   /**
    * @return Returns the groupRegistrationExpiresTime.
    */
   public int getGroupRegistrationExpiresTime() {
      return groupRegistrationExpiresTime;
   }
   public void setGroupRegistrationExpiresTime(int expiresTime) {
      this.groupRegistrationExpiresTime = expiresTime;
   }

   /**
    * @return Returns the servedGroupLeaseTime.
    */
   public int getServedGroupLeaseTime() {
      return servedGroupLeaseTime;
   }
   public void setServedGroupLeaseTime(int leaseTime) {
      this.servedGroupLeaseTime = leaseTime;
   }
   
   public int getGroupCallInviteProcessingTime() {
      return groupCallInviteProcessingTime;
   }
   public void setGroupCallInviteProcessingTime(int processingTime ) {
      this.groupCallInviteProcessingTime = processingTime;
   }

   /**
    * @return Returns the rfResourcesAvailable.
    */
   public boolean isRfResourcesAvailable() {
      return rfResourcesAvailable;
   }
   public void setRfResourcesAvailable(boolean rfResourcesAvailable) {
      this.rfResourcesAvailable = rfResourcesAvailable;
   }

   /**
    * Limit on the RTP resources available on this machine.
    * 
    * @return
    */
   public int getMaxRtpPorts() {
      return maxRtpPorts;
   }
   public void setMaxRtpPorts( int maxPort) {
      this.maxRtpPorts = maxPort;
   }
   
   /**
    * @return the queryUnknownSu
    */
   public boolean isSuCredentialsQueriedBeforeRegister() {
      return suCredentialsQueriedBeforeRegister;
   }
   public void setSuCredentialsQueriedBeforeRegister(boolean suCredentials) {
      this.suCredentialsQueriedBeforeRegister = suCredentials;
   }
   
   /**
    * @return the queryGroupCredentials
    */
   public boolean isGroupCredentialsQueriedBeforeRegister() {
      return groupCredentialsQueriedBeforeRegister;
   }
   public void setGroupCredentialsQueriedBeforeRegister(boolean groupCredentials) {
      this.groupCredentialsQueriedBeforeRegister = groupCredentials;
   }

   /**
    * Default register confirm processing time (seconds).
    * 
    * @return
    */
   public int getRegisterConfirmTime() {
      return registerConfirmTime;
   }
   public void setRegisterConfirmTime(int registerConfirmTime) {
      this.registerConfirmTime = registerConfirmTime;
   }
   
   /**
    * @return the advancedRtpResourceManagementSupported
    */
   public boolean isAdvancedRtpResourceManagementSupported() {
      return advancedRtpResourceManagementSupported;
   }
      
   public void setAdvancedRtpResourceManagementSupported( 
         boolean advancedRtpResourceManagementSupported ) {
      this.advancedRtpResourceManagementSupported = advancedRtpResourceManagementSupported;
   }

   /**
    * @return the pttRequestGranted
    */
   public boolean isPttRequestGranted() {
      return pttRequestGranted;
   }
   public void setPttRequestGranted(boolean pttRequestGranted) {
      this.pttRequestGranted = pttRequestGranted;
   }

   public void addUnknownSu(SuConfig suConfig) {
      unknownSuTable.add(suConfig);      
   }
   
   public boolean isSuUnknown(SuConfig suConfig) {
      return unknownSuTable.contains(suConfig);
   }

   public boolean isUnitKnown(int unitId) {
      for (SuConfig suConfig: unknownSuTable) {
         if (suConfig.getSuId() == unitId) {
            return false;
         }
      }
      return true;
   }
   
   public boolean isNotInterestedInLosingAudio() {
      return notInterestedInLosingAudio;
   }
   public void setNotInterestedInLosingAudio(boolean notInterestedInLosingAudio) {
      this.notInterestedInLosingAudio = notInterestedInLosingAudio;
   }

   public boolean isCallingSuInitialTransmitter() {      
      return isCallingSuInitialTransmitter;
   }

   public void setCallingSuInitialTransmitter(boolean isCallingSuInitialTransmitter) {
      this.isCallingSuInitialTransmitter = isCallingSuInitialTransmitter;
   }
   
   public static String generateTag(String name) {
      return  name + ".daemon";
   }

   /**
    * The tag to assign to the emulated ISSI Tester daemons. This defines
    * the edge between emulated RFSSs and the ISSI Tester daemons where there
    * are assigned to run.
    * 
    * @return
    */
   public String getTag() {      
      return generateTag(this.rfssName);
   }
   public void setTag(String newTag) {
      this.tag = newTag;      
   }
   //public String getRawTag() {
   //   return tag;
   //}

   public String getAddress() {
      return address;
   }
   public void setAddress(String address) {
      this.address = address;      
   }

   public String getIpAddress() {
      return ipAddress;
   }
   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;      
   }

   public int getSipPort() {
      return this.sipPort;
   }
   public void setSipPort(int port) {
      if (port < 0) {
         throw new IllegalArgumentException("bad port value");
      }
      this.sipPort = port;
   }

   public int getSelfTestPort() {
      return selfTestPort;
   }
   public void setSelfTestPort(int selfTestPort) {
      this.selfTestPort = selfTestPort;
   }

   public int getRfssId() {
      return rfssId;
   }
   public void setRfssId(int rfssId) {
      if ( rfssId > 0xff) {
         throw new IllegalArgumentException(
            "RFSS ID out of range should be <= 0xff was 0x" +Integer.toHexString(rfssId));
      }
      this.rfssId = rfssId;      
   }
   public String getRfssIdString() {
      return EndPointHelper.encodeRfssId(rfssId);
   }
   
   public String toString() {
      StringBuffer sbuf =  new StringBuffer();
      sbuf.append("<rfssconfig\n");
      sbuf.append("\trfssName=\"" + rfssName + "\"\n");
      sbuf.append("\trfssId=\"" + Integer.toHexString(rfssId) + "\"\n");
      sbuf.append("\tsystemName=\"" + systemConfig.getSymbolicName() + "\"\n");
      if( !isEmulated) {
         sbuf.append("\tipAddress=\"" + ipAddress + "\"\n");
      }
      sbuf.append("\tport=\"" + sipPort + "\"\n");
      sbuf.append("\tcolor=\"" + colorString +"\"\n");
      sbuf.append("\temulated=\"" + isEmulated +"\"\n");
      if( address != null) {
         sbuf.append("\taddress=\"" + address +"\"\n");
      }
      sbuf.append("/>\n");
      return sbuf.toString();
   }

   public String getDescription() {
      
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("\n----------- RFSS Configuration ----------");
      sbuf.append("\n rfssName = " + getRfssName());
      sbuf.append("\n rfssId = " + Integer.toHexString(getRfssId()));
      sbuf.append("\n wacnId = " + Integer.toHexString(getWacnId()));
      sbuf.append("\n systemId = " + Integer.toHexString(getSystemId()));
      sbuf.append("\n rfssIpAddress = " + getIpAddress());
      sbuf.append("\n sipSignalingPort = " + getSipPort());
      sbuf.append("\n isEmulated = " + isEmulated());
      if( address != null) {
         sbuf.append("\n address = " + getAddress());
      }
      
      if (isEmulated()) {
         sbuf.append("\n---- Emulator RFSS SETTINGS SPECIFIC TO THIS TEST (-1 means UNBOUNDED)---");
         sbuf.append("\n isRfResourcesAvailable = " + isRfResourcesAvailable());
         sbuf.append("\n numberOfRtpPortsAvailable = " + getMaxRtpPorts());
         sbuf.append("\n servedGroupLeaseTime (sec) = " + getServedGroupLeaseTime());
         sbuf.append("\n groupRegistrationExpiresTime (sec) = " + getGroupRegistrationExpiresTime());
         sbuf.append("\n registerConfirmTime (sec) = " + getRegisterConfirmTime());

         sbuf.append("\n groupCallInviteProcessingTime (sec) = " + getGroupCallInviteProcessingTime());
         sbuf.append("\n isAdvancedRtpResourceManagementSupported = " + isAdvancedRtpResourceManagementSupported());
         sbuf.append("\n isSuCredentialsQueriedBeforeRegister = " + isSuCredentialsQueriedBeforeRegister());
         sbuf.append("\n isPttRequestGranted = " + isPttRequestGranted());
         sbuf.append("\n isCallingSuInitialTransmitter = " + isCallingSuInitialTransmitter());
         sbuf.append("\n isNotInterestedInLosingAudio = " + isNotInterestedInLosingAudio());
      }
      sbuf.append("\n-----------------------------------------\n");
      return sbuf.toString();
   }

   // implementation of Comparable
   // used by TestLayoutPanel sorting by rfssName
   //-------------------------------------------------------------------------------
   public int compareTo( RfssConfig other) {
      return getRfssName().compareTo( other.getRfssName());
   }
}
