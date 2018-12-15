//
package gov.nist.p25.issi.issiconfig;

import gov.nist.p25.issi.p25body.serviceprofile.group.GroupServiceProfile;
import gov.nist.p25.issi.utils.EndPointHelper;
import gov.nist.p25.issi.utils.ReadOnlyIterator;

import java.io.Serializable;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A configuration class for ISSI groups. A group has a number of subscriber
 * RFSS's. A group has a home RFSS that keeps track of its state and who is
 * subscribed to it. This class is constructed initially by reading the xml
 * configuration file for the group. Its function is to keep track of the
 * current group membership as well as the static information that is read from
 * the system configuration file. The current membership information is stored
 * at the home RFSS for the group.
 * 
 */
public class GroupConfig implements Serializable {
   
   private static final long serialVersionUID = -1L;

   private String groupName;
   private int groupId;
   private RfssConfig homeRfss;
   private int homeRfssKnowsAboutMeFor;
   //private SystemConfig sysConfig;

   private CProtectedDisposition cProtectedDisposition = CProtectedDisposition.FORWARD_PROTECTED;
   private GroupServiceProfile groupServiceProfile = new GroupServiceProfile();

   private HashSet<SuConfig> subscribers = new HashSet<SuConfig>();
   private HashSet<RfssConfig> registerForbiddenFromRfss = new HashSet<RfssConfig>();
   private HashSet<RfssConfig> queryForbiddenFromRfss = new HashSet<RfssConfig>();

   // accessors
   public String getGroupName() {
      return groupName;
   }
   public void setGroupName(String groupName) {
      this.groupName = groupName;
   }

   public int getGroupId() {
      return groupId;
   }
   public void setGroupId(int sgId) {
      if (sgId > 0xffff) {
         throw new IllegalArgumentException("SgId outside allowable range: "+sgId);
      }
      this.groupId = sgId;
   }
   public String getGroupIdString() {
      return EndPointHelper.encodeGroupId(groupId);
   }

   public RfssConfig getHomeRfss() {
      return homeRfss;
   }
   public void setHomeRfss(RfssConfig homeRfss) {
      this.homeRfss = homeRfss;
   }

   public SystemConfig getSysConfig() {
      return getHomeRfss().getSysConfig();
      //return sysConfig;
   }
   //public void setSysConfig(SystemConfig sysConfig) {
   //   this.sysConfig = sysConfig;
   //}

   // constructor
   public GroupConfig(String groupName, int sgId, RfssConfig homeRfss,
         int homeRfssKnowsAboutMeFor) {

      setGroupId( sgId);
      setGroupName( groupName);
      setHomeRfss( homeRfss);
      setHomeRfssKnowsAboutMeFor( homeRfssKnowsAboutMeFor);
      //this.sysConfig = homeRfss.getSysConfig();
   }

   /**
    * Get radical name for SU given its id and system id according to 3.4.1.2
    * 
    * @param sgId -- group id.
    * @param systemConfig -- system config record.
    */
   public static String getRadicalName(SystemConfig sysConfig, int sgId) {
      
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
      String uidName = Integer.toHexString(sgId);
      k = 4 - uidName.length();
      for (int i = 0; i < k; i++) {
         uidName = "0" + uidName;
      }
      String radicalName = wacnIdName + systemIdName + uidName;
      return radicalName.toLowerCase();
   }

   public String getRadicalName() {
      return getRadicalName(getSysConfig(), getGroupId());
   }

   /**
    * Add a SU that subscribes to the given group.
    * 
    * @param suConfig -- the Su to add.
    */
   public void addSubscriber(SuConfig suConfig) {
      subscribers.add(suConfig);
   }

   /**
    * Get the subscribers to the group.
    * 
    * @return
    */
   public Iterator<SuConfig> getSubscribers() {
      return new ReadOnlyIterator<SuConfig>(subscribers.iterator());
   }

   /**
    * Setter for the group service profile.
    * 
    * @param groupServiceProfile
    */
   public void setGroupServiceProfie(GroupServiceProfile groupServiceProfile) {
      this.groupServiceProfile = groupServiceProfile;
   }

   /**
    * Set the group service profile for this group
    * 
    * @param sgProfile;
    */
   public void setGroupServiceProfile(String sgProfile) throws ParseException {
      this.groupServiceProfile = GroupServiceProfile.createGroupServiceProfile(sgProfile);
   }

   /**
    * Get the group service profile.
    */
   public GroupServiceProfile getGroupServiceProfile() {
      return groupServiceProfile;
   }

   /**
    * Add a forbidden Rfss ( registrations from here are not accepted when a Su
    * roams. )
    * 
    * @param rfssConfig
    */
   public void addForbiddenRfss(RfssConfig rfssConfig) {
      registerForbiddenFromRfss.add(rfssConfig);
   }

   /**
    * Return true if an SU is forbidden from registering from a given RFSS.
    * 
    * @param rfssConfig
    * @return
    */
   public boolean isRegisterForbiddenFromRfss(RfssConfig rfssConfig) {
      return registerForbiddenFromRfss.contains(rfssConfig);
   }

   /**
    * Return true if a register query is forbidden from the given rfss.
    * 
    * @param rfssConfig
    * @return
    */
   public boolean isQueryForbiddenFromRfss(RfssConfig rfssConfig) {
      return queryForbiddenFromRfss.contains(rfssConfig);
   }

   /**
    * The amount of time for which the Home RFSS keeps record of this group.
    * 
    * @return the amount of time that the home rfss keeps account of this
    *         group.
    */
   public int getHomeRfssKnowsAboutMeFor() {
      return homeRfssKnowsAboutMeFor;
   }
   public void setHomeRfssKnowsAboutMeFor(int homeRfssKnowsAboutMeFor) {
      this.homeRfssKnowsAboutMeFor = homeRfssKnowsAboutMeFor;
   }

   /**
    * Add a rfss config to the "query forbidden from" RFSS set.
    * 
    * @param rfssConfig -- rfss from which query is forbidden.
    */
   public void addRegisterQueryForbiddenFromRfss(RfssConfig rfssConfig) {
      queryForbiddenFromRfss.add(rfssConfig);
   }

   public CProtectedDisposition getCProtectedDisposition() {
      return cProtectedDisposition;
   }
   public void setCProtectedDisposition( CProtectedDisposition forwardingDirective) {
      this.cProtectedDisposition = forwardingDirective;
   }

   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("<!-- Subscriber Group Definition -->\n");
      sbuf.append("<sgconfig\n");
      sbuf.append("\tgroupName=\"" + getGroupName() + "\"\n");
      sbuf.append("\tsgId=\"" + Integer.toHexString(groupId) + "\"\n");
      sbuf.append("\thomeRfssName=\"" + getHomeRfss().getRfssName() + "\"\n");
      sbuf.append("/>\n");
      return sbuf.toString();
   }

   public String getDescription() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("\n---------- GROUP Configuration ----------");
      sbuf.append("\n Symbolic Name = " + getGroupName());
      sbuf.append("\n Group ID = " + Integer.toHexString(getGroupId()) + "(hex)");
      sbuf.append("\n Home Rfss Name = " + getHomeRfss().getRfssName());
      sbuf.append("\n System Id = " + Integer.toHexString(getSysConfig().getSystemId()));
      sbuf.append("\n Wacn Id = " + Integer.toHexString(getSysConfig().getWacnId()));
      sbuf.append("\n GroupServiceProfile = [\n");
      sbuf.append(getGroupServiceProfile().toString() + " ]");
      sbuf.append("\n Subsciber Units in this Group = [");
      for (Iterator<SuConfig> it = getSubscribers(); it.hasNext();) {
         sbuf.append(it.next().getSuName());
         if (it.hasNext())
            sbuf.append(", ");
      }
      sbuf.append(" ]");
      sbuf.append("\n-----------------------------------------\n");
      return sbuf.toString();   
   }
}
