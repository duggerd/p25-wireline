//
package gov.nist.p25.issi.issiconfig;

import gov.nist.p25.issi.p25body.serviceprofile.user.UserServiceProfile;
import gov.nist.p25.issi.rfss.SuInterface;
import gov.nist.p25.issi.utils.EndPointHelper;
import java.text.ParseException;
import java.util.HashSet;

import org.apache.log4j.Logger;

/**
 * This is the main test driver emulates the SU. Eventually
 * this class will implement both SipListener and RTPListener.
 */
public class SuConfig {

   private static Logger logger = Logger.getLogger(SuConfig.class);
   
   private TopologyConfig config;
   private SystemConfig sysConfig;
   private RfssConfig homeRfss;
   private RfssConfig initialServingRfss;   
   private boolean isEmulated;
      
   // The user service Profile for the SU.
   private UserServiceProfile userServiceProfile;
   
   // The set of RFSSs that this SU is forbidden from 
   // registering with.
   private HashSet<RfssConfig> registrationForbiddenSet;   
   private HashSet<RfssConfig> registerQueryForbiddenSet;

   // The set of groups for which the SU is a member
   private HashSet<GroupConfig> groups;

   // whether the SU is initially on or off.
   private SuState initialState;   
   private int inviteProcessingDelay;
   private int suId;
   
   // Temporary storage for radical name 
   private String radicalName;

   // The SU for this guy.
   private SuInterface su;
   
   // Time for which the Home keeps a record of this SU.
   private int homeRfssKnowsAboutMeFor;
   
   // Time for which the Home Mobility manager keeps a registration record
   // of this SU
   private int homeRfssRegistersMeFor;

   private HashSet<GroupConfig> subscribedGroups;
   private boolean registerOnForce;   
   private String symbolicName;
   private int servingRfssReferencesMeFor;   
   private boolean available;   
   private boolean checkSuPresenceOnRegister;   
   private boolean checkCallPermission = true;   
   private boolean skipRegisterRegister = false;   
   private CProtectedDisposition cProtectedDisposition;
   
   // constructor
   public SuConfig(int suId, String suName,
         TopologyConfig config, RfssConfig homeRfss,
         RfssConfig servingRfss, SuState initialSuState, 
         int homeRfssKnowsMeFor, 
         int servingRfssReferencesMeFor,
         int inviteProcessingDelay,
         int homeRfssRegistersMeFor, 
         boolean registerOnForce,
         boolean isEmulated,
         boolean availability)
   {
      this.groups = new HashSet<GroupConfig>();
      setTopologyConfig(config);
      this.homeRfss = homeRfss;
      this.initialServingRfss = servingRfss;
      this.sysConfig = homeRfss.getSysConfig();
      this.subscribedGroups = new HashSet<GroupConfig>();
      this.registrationForbiddenSet = new HashSet<RfssConfig>();
      this.registerQueryForbiddenSet = new HashSet<RfssConfig>();
      setSuId(suId);
      this.initialState = initialSuState;
      this.homeRfssKnowsAboutMeFor = homeRfssKnowsMeFor;
      this.inviteProcessingDelay = inviteProcessingDelay;
      this.homeRfssRegistersMeFor = homeRfssRegistersMeFor;
      this.registerOnForce = registerOnForce;
      this.userServiceProfile = new UserServiceProfile();
      this.symbolicName = suName;
      this.servingRfssReferencesMeFor = servingRfssReferencesMeFor;
      this.isEmulated = isEmulated;
      this.available = availability;      
   }

   // accessor
   public TopologyConfig getTopologyConfig() {
      return config;
   }
   public void setTopologyConfig(TopologyConfig config) {
      this.config = config;
   }
     
   public void setSU(SuInterface su) {
      if (su == null)
         throw new NullPointerException("Null arg!");
      if (this.su != null)
         throw new IllegalStateException("Illegal state -- already set");
      this.su = su;
   }

   public SuInterface getSU() {
      return su;
   }

   public RfssConfig getHomeRfss() {
      return homeRfss;
   }

   public RfssConfig getInitialServingRfss() {
      return initialServingRfss;
   }

   /**
    * Get radical name for SU given its id and system id according to 3.4.1.2
    * 
    * @param suID -- su id.
    * @param systemConfig -- system config record.
    * @return -- the radical name
    */
   public static String getRadicalName(SystemConfig sysConfig, int suId) {
      String wacnIdName = Integer.toHexString(sysConfig.getWacnId());

      // prepend 0's to the right length.
      int k = 5 - wacnIdName.length();
      for (int i = 0; i < k; i++) {
         wacnIdName = "0" + wacnIdName;
      }

      String systemIdName = Integer.toHexString(sysConfig.getSystemId());
      k = 3 - systemIdName.length();
      for (int i = 0; i < k; i++) {
         systemIdName = "0" + systemIdName;
      }
      String uidName = Integer.toHexString(suId);
      k = 6 - uidName.length();
      for (int i = 0; i < k; i++) {
         uidName = "0" + uidName;
      }
      String radicalName = wacnIdName + systemIdName + uidName;
      return radicalName.toLowerCase();
   }

   public String getRadicalName() {
      // Compute the radical name according to 3.4.1.2
      if (radicalName == null)
         radicalName = getRadicalName(sysConfig, suId);
      return radicalName;
   }

   public int hashCode() {
      return getRadicalName().hashCode();
   }

   public boolean equals(Object that) {
      return (that != null && that instanceof SuConfig && this
            .getRadicalName().equals(((SuConfig) that).getRadicalName()));
   }

   public int getSuId() {
      return suId;
   }
   public String getSuIdString() {
      return EndPointHelper.encodeSuId(suId);
   }

   public int getWacnId() {
      return sysConfig.getWacnId();
   }

   public int getSystemId() {
      return sysConfig.getSystemId();
   }

   public void setUserServiceProfile(String buffer) throws ParseException {
      this.userServiceProfile = 
           UserServiceProfile.createUserServiceProfile(buffer);
   }

   public UserServiceProfile getUserServiceProfile() {      
      return userServiceProfile;
   }

   public void setServingRfss(RfssConfig rfssConfig) {
      /*** 12.23.1 roaming from rfss_2 to rfss_5
      if (initialServingRfss != null)
         throw new IllegalStateException("setServingRfss: Already set.");
	 ***/
      this.initialServingRfss = rfssConfig;
   }

   public void addSubscribedTo(GroupConfig groupConfig) {
      subscribedGroups.add(groupConfig);
   }

   public HashSet<GroupConfig> getSubscribedGroups() {
      return subscribedGroups;
   }

   /**
    * Add a forbidden RFSS. The SU is not allowed to register from a
    * "forbidden" rfss.
    * 
    * @param rfssConfig -- the forbidden RFSS to add.
    */
   public void addForbiddenRfss(RfssConfig rfssConfig) {
      registrationForbiddenSet.add(rfssConfig);
      registerQueryForbiddenSet.add(rfssConfig);
   }

   /**
    * add a forbidden for registration only RFSS.
    */
   public void addRegisterForbiddenRfss(RfssConfig rfssConfig) {
      registrationForbiddenSet.add(rfssConfig);
   }
   
   /**
    * Add a forbidden for query RFSS.
    */
   public void addRegisterQueryForbiddenRfss(RfssConfig rfssConfig) {
      registerQueryForbiddenSet.add(rfssConfig);
   }

   /**
    * Return true if a given Rfss is forbidden.
    * 
    * @param rfssConfig -- the rfss to check.
    */
   public boolean isRegistrationForbiddenFrom(RfssConfig rfssConfig) {
      return registrationForbiddenSet.contains(rfssConfig);
   }
   
   public boolean isQueryForbiddenFrom(RfssConfig rfssConfig) {
      return registerQueryForbiddenSet.contains(rfssConfig);      
   }

   /**
    * Add a group to the set of groups to which this su is subscrbed.
    * 
    * @param groupConfig
    */
   public void addGroup(GroupConfig groupConfig) {
      groups.add(groupConfig);
   }

   public HashSet<GroupConfig> getGroups() {      
      return groups;
   }

   public SuState getInitialSuState() {
      return initialState;
   }
   public void setInitialSuState(SuState initialState) {
      this.initialState = initialState;
   }
   
   /**
    * The time for which the Home RFSS has knowledge of me
    * (allow you to generate anamolies for testing).
    * 
    * @return
    */
   public int getHomeRfssKnowsAboutMeFor() {
      return homeRfssKnowsAboutMeFor;
   }
   public void setHomeRfssKnowsAboutMeFor(int homeRfssKnowsAboutMeFor) {
      this.homeRfssKnowsAboutMeFor = homeRfssKnowsAboutMeFor;
   }

   public int getInviteProcessingDelay() {
      return inviteProcessingDelay;
   }
   public void setInviteProcessingDelay(int inviteProcessingDelay) {
      this.inviteProcessingDelay = inviteProcessingDelay;
   }

   /**
    * The time for which registration records are held. 0 means the 
    * unit is not registered.
    * 
    * @return
    */
   public int getHomeRfssRegistersMeFor() {      
      return homeRfssRegistersMeFor;
   }
   public void setHomeRfssRegistersMeFor(int homeRfssRegistersMeFor) {      
      this.homeRfssRegistersMeFor = homeRfssRegistersMeFor;
   }

   public boolean isRegisterOnForce() {
      return registerOnForce;
   }
   public void setRegisterOnForce(boolean registerOnForce) {
      this.registerOnForce = registerOnForce;
   }

   public String getSuName() {
      return symbolicName;
   }

   public int getServingRfssReferencesMeFor() {
      return servingRfssReferencesMeFor;
   }
   public void setServingRfssReferencesMeFor(int servingRfssReferencesMeFor) {
      this.servingRfssReferencesMeFor = servingRfssReferencesMeFor;
   }

   public boolean isEmulated() {
      return isEmulated;
   }
   public boolean getEmulated() {
      return isEmulated;
   }
   public void setEmulated(boolean isEmulated) {
      this.isEmulated = isEmulated;
   }

   public void setUserServiceProfile(UserServiceProfile userServiceProfile) {
      this.userServiceProfile = userServiceProfile;      
   }

   /**
    * Get the availability state. The availability state
    * governs whether a "busy here" is returned or not.
    * 
    * @return -- true if available, otherwise false.
    */
   public boolean isAvailable() {
      return available;
   }
   public void setAvailable(boolean available) {
      this.available = available;
   }
   
   public void setSuId(int suId) {
      if (suId > 0xffffff) {
         throw new IllegalArgumentException(
               "SuId Outside allowable range : " + suId);   
      }
      this.suId = suId;      
   }

   public void setCProtectedDisposition(CProtectedDisposition cProtectedDisposition) {
      this.cProtectedDisposition = cProtectedDisposition;
   }

   public CProtectedDisposition getCProtectedDisposition() {
      return cProtectedDisposition;
   }
   
   public boolean isCheckSuPresenceOnRegister() {
      return checkSuPresenceOnRegister;
   }
   public boolean getCheckSuPresenceOnRegister() {
      return checkSuPresenceOnRegister;
   }
   public void setCheckSuPresenceOnRegister(boolean checkSuPresenceOnRegister) {
      this.checkSuPresenceOnRegister = checkSuPresenceOnRegister;
   }

   public boolean isCheckCallPermission() {
      return checkCallPermission;
   }
   public boolean getCheckCallPermission() {
      return checkCallPermission;
   }
   public void setCheckCallPermission(boolean checkCallPermission) {
      this.checkCallPermission = checkCallPermission;
   }

   public boolean isSkipRegisterRegister() {
      return skipRegisterRegister;
   }
   public boolean getSkipRegisterRegister() {
      return skipRegisterRegister;
   }
   public void setSkipRegisterRegister(boolean skipRegisterRegister) {
      this.skipRegisterRegister = skipRegisterRegister;
   }

   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("<!-- Subscriber Unit (SU) definition -->\n");
      sb.append("<suconfig\n");
      sb.append("\tsuId=\"" + Integer.toHexString(this.suId) + "\"\n");
      sb.append("\tsuName=\"" + getSuName() + "\"\n");
      sb.append("\thomeRfssName=\"" + homeRfss.getRfssName() + "\"\n");
      sb.append("\tservingRfssName=\"" + initialServingRfss.getRfssName() + "\"\n");
      sb.append("/>\n");      
      return sb.toString();
   }
   
   public String getDescription() {    
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("\n------------- SU Configuration ------------");
      sbuf.append("\n Symbolic Name = " + getSuName());
      sbuf.append("\n Unit Id = " + Integer.toHexString(getSuId()) + "(hex)");
      sbuf.append("\n Home Rfss = " + getHomeRfss().getRfssName());
      sbuf.append("\n Initial Serving RFSS = "+ getInitialServingRfss().getRfssName());
      sbuf.append("\n Wacn Id = " + Integer.toHexString(getWacnId()));
      sbuf.append("\n System Id = " + Integer.toHexString(getSystemId()));
      sbuf.append("\n isEmulated = " + isEmulated());
      sbuf.append("\n ServiceProfile = [\n");
      sbuf.append(getUserServiceProfile().toString() + "]");
      
      if (isEmulated()) {
         sbuf.append("\n-------- Emulator SU Settings ( -1 means UNBOUNDED): ---");
         sbuf.append("\n Force Register = " + isRegisterOnForce());
         sbuf.append("\n home RFSS registers me for "
               + getHomeRfssRegistersMeFor()
               + " seconds (0 means not registered at home)");
         sbuf.append("\n home Rfss knows about me for "
               + getHomeRfssKnowsAboutMeFor()
               + " seconds (0 means not known to home)");
         sbuf.append("\n serving Rfss references me for "
               + getServingRfssReferencesMeFor()
               + " seconds (0 means not registered at serving)");
         sbuf.append("\n Invite processing delay (user response time) "
               + getInviteProcessingDelay()
               + " seconds (>0 will generate provisional response)");
         sbuf.append("\n isPresenceAssertedOnRegister = "
               + isCheckSuPresenceOnRegister());              
         sbuf.append("\n isCheckCallPermission = "
               + isCheckCallPermission());              
      }
      sbuf.append("\n-------------------------------------------\n");
      return sbuf.toString();    
   }
}
