//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.LinkedHashMap;
//import org.apache.log4j.Logger;

//import gov.nist.p25.issi.constants.XMLTagsAndAttributes;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfile;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Group Service profile structure. Implements the group service profile
 * attributes defined in section 3.8 of the ISSI Specification. Except for the
 * mandatory fields, each field has a default profile line assigned to it when
 * this structure is created.
 */
@SuppressWarnings("unchecked")
public class GroupServiceProfile extends ServiceProfile {
   //private static Logger logger = Logger.getLogger(GroupServiceProfile.class);


   static {
      profileLineClasses.put(AccessPermission.NAME, AccessPermission.class);
      profileLineClasses.put(AccessPermission.SHORTNAME, AccessPermission.class);

      profileLineClasses.put(AnnouncementGroup.NAME, AnnouncementGroup.class);
      profileLineClasses.put(AnnouncementGroup.SHORTNAME, AnnouncementGroup.class);

      profileLineClasses.put(ConfirmedCallSetupTime.NAME.toLowerCase(), ConfirmedCallSetupTime.class);
      profileLineClasses.put(ConfirmedCallSetupTime.SHORTNAME, ConfirmedCallSetupTime.class);

      profileLineClasses.put(EmergencyCapable.NAME, EmergencyCapable.class);
      profileLineClasses.put(EmergencyCapable.SHORTNAME, EmergencyCapable.class);

      profileLineClasses.put(EmergencyPreemption.NAME, EmergencyPreemption.class);
      profileLineClasses.put(EmergencyPreemption.SHORTNAME, EmergencyPreemption.class);

      profileLineClasses.put(GroupPriority.NAME, GroupPriority.class);
      profileLineClasses.put(GroupPriority.SHORTNAME, GroupPriority.class);

      profileLineClasses.put(GroupSecurityLevel.NAME, GroupSecurityLevel.class);
      profileLineClasses.put(GroupSecurityLevel.SHORTNAME, GroupSecurityLevel.class);

      profileLineClasses.put(InterconnectSecurity.NAME, InterconnectSecurity.class);
      profileLineClasses.put(InterconnectSecurity.SHORTNAME, InterconnectSecurity.class);

      profileLineClasses.put(InterruptMode.NAME, InterruptMode.class);
      profileLineClasses.put(InterruptMode.SHORTNAME, InterruptMode.class);

      profileLineClasses.put(InterconnectFlag.NAME, InterconnectFlag.class);
      // No short form for this.

      profileLineClasses.put(RfHangTime.NAME, RfHangTime.class);
      profileLineClasses.put(RfHangTime.SHORTNAME, RfHangTime.class);
   }

   /**
    * Create a new group service profile with default values.
    * 
    */
   public GroupServiceProfile() {
      //this.profileLines = new Hashtable<String, ServiceProfileLine>();
      this.profileLines = new LinkedHashMap<String, ServiceProfileLine>();

      /** Add default profile lines where ever applicable */
      this.addProfileLine(new AccessPermission());
//BUG-436: place holder for g-agroup: agid
      this.addProfileLine(new AnnouncementGroup());
      this.addProfileLine(new GroupPriority());
      this.addProfileLine(new EmergencyCapable());
      this.addProfileLine(new EmergencyPreemption());
      this.addProfileLine(new RfHangTime());
      this.addProfileLine(new ConfirmedCallSetupTime());
      this.addProfileLine(new InterruptMode());
      this.addProfileLine(new GroupSecurityLevel());
      this.addProfileLine(new InterconnectFlag());
      this.addProfileLine(new InterconnectSecurity());
   }

   /**
    * Create a group service profile class given the unparsed line.
    * 
    * @param groupServiceProfileLine
    * @return the ServiceProfileLine object
    * @throws ParseException --
    *             if an error was reched parsing the line.
    */
   protected ServiceProfileLine createServiceProfileLine(
         String groupServiceProfileLine) throws ParseException {
      if (groupServiceProfileLine == null)
         throw new NullPointerException("null argument");

      String[] tokens = groupServiceProfileLine.split(":");
      if (tokens == null || tokens.length != 2)
         throw new ParseException(groupServiceProfileLine, 0);
      return createServiceProfileLine(tokens[0], tokens[1]);
   }

   /**
    * Create a group service profile line class given name and value.
    * 
    * @param name
    * @param value
    * @return the ServiceProfileLine object
    * @throws ParseException
    */
   public static ServiceProfileLine createServiceProfileLine(String name,
         String value) throws ParseException {
      if (name == null || value == null)
         throw new NullPointerException("null argument");

      try {
         Class<? extends ServiceProfileLine> gspLineClass = getProfileLineClass(name);
         if (gspLineClass == null)
            throw new ParseException("Invalid field Name : " + name, 0);
         Constructor cons = gspLineClass
               .getDeclaredConstructor(new Class[] { String.class });
         ServiceProfileLine retval = (ServiceProfileLine) cons
               .newInstance((Object[]) (new String[] { value }));
         return retval;
      } catch (InvocationTargetException ex) {
         // The cause has the actual reason failure
         throw new RuntimeException(ex.getCause());
      } catch (NoSuchMethodException ex) {
         throw new RuntimeException(ex);
      } catch (IllegalArgumentException e) {
         throw new RuntimeException(e);
      } catch (InstantiationException e) {
         throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Create an announcement group field. Note there is no default value for
    * this. This field is mandatory.
    */
   //public void setAnnouncementGroupId(long groupId) {
   //   AnnouncementGroup retval = new AnnouncementGroup(groupId);
   //   this.profileLines.put(AnnouncementGroup.NAME, retval);
   //}
   public void setAnnouncementGroupId(long groupId) {
      this.getAnnouncementGroup().setGroupId(groupId);
   }

   /**
    * Get the announcement group from this structure.
    */
   public AnnouncementGroup getAnnouncementGroup() {
      return (AnnouncementGroup) getByName(AnnouncementGroup.NAME);
   }

   /**
    * Get the access permission line.
    */
   public AccessPermission getAccessPermission() {
      return (AccessPermission) profileLines.get(AccessPermission.NAME);
   }
   
   /**
    * Set the access permission profile line.
    * 
    * @param accessPermission
    */
   public void setAccessPermission(String accessPermission) {
      this.addProfileLine(AccessPermission.createFromXmlAttribute(accessPermission));      
   }

   /**
    * get the confirmed call setup time.
    */
   public ConfirmedCallSetupTime getConfirmedCallSetupTime() {
      return (ConfirmedCallSetupTime) getByName(ConfirmedCallSetupTime.NAME);
   }
   
   /**
    * Set the confirmed group call setup time
    */
   public void setConfirmedGroupCallSetupTime ( int time ) {
      this.addProfileLine(new ConfirmedCallSetupTime(time));
   }   

   /**
    * get the emergency capable line
    */
   public EmergencyCapable getEmergencyCapable() {
      return (EmergencyCapable) getByName(EmergencyCapable.NAME);
   }
      
   /**
    * Set the emergency capable line
    */
   public void setEmergencyCapable(String str) {
      this.addProfileLine(EmergencyCapable.createFromXmlAttribute(str));
   }
   
   /**
    * Get the group priority line
    */
   public GroupPriority getGroupPriority() {
      return (GroupPriority) this.getByName(GroupPriority.NAME);
   }
   /**
    * Set the group priority
    */
   public void setGroupPriority(int priority) {
       this.addProfileLine(new GroupPriority(priority));
   }

   /**
    * Get the interconnect security line.
    */
   public InterconnectSecurity getInterconnectSecurity() {
      return (InterconnectSecurity) this.getByName(InterconnectSecurity.NAME);
   }
   
   public void setInterconnectSecurity(String security) {
      this.getInterconnectSecurity().setInterconnectCallsSecure(security.equals("Secure"));
   }

   /**
    * Get the RF Hangtime line.
    */
   public RfHangTime getRfHangTime() {
      return (RfHangTime) getByName(RfHangTime.NAME);
   }
   
   /**
    * Set the rf hang timer
    */
   public void setRfHangTime(int time) throws ParseException {
      RfHangTime  rfhangTime = new RfHangTime(time);
      this.addProfileLine(rfhangTime);
   }

   /**
    * Get the GroupSecurityLevel
    */
   public GroupSecurityLevel getGroupSecurityLevel() {
      return (GroupSecurityLevel) getByName(GroupSecurityLevel.NAME);
   }
   
   /**
    * Set the group security level
    */
   public void setGroupSecurityLevel(String level) {
      this.addProfileLine(GroupSecurityLevel.createFromXmlAttributes(level));
   }

   /**
    * Get the emergency preemption line.
    */
   public EmergencyPreemption getEmergencyPreemption() {
      return (EmergencyPreemption) getByName(EmergencyPreemption.NAME);
   }

   /**
    * Set the emergency preemption string
    */
   public void setEmergencyPreemption( String attr) {
      this.addProfileLine(EmergencyPreemption.createFromXmlAttribute(attr));
   }
   /**
    * Get the interrupt mode.
    * 
    * @return the InterruptMode object
    */
   public InterruptMode getInterruptMode() {
      return (InterruptMode) this.getByName(InterruptMode.NAME);
   }
   
   public void setInterruptMode(String interruptMode) {      
      this.addProfileLine(InterruptMode.createFromXmlAttributes(interruptMode));
   }

   /**
    * Get the interconnect flag line
    */
   public InterconnectFlag getInterconnectFlag() {
      return (InterconnectFlag) getByName(InterconnectFlag.NAME);
   }
   
   /**
    * Set the interconnect flag
    */
   public void setInterconnectFlag(boolean flag) {
      this.getInterconnectFlag().setInterconnectAllowed(flag);
   }


   @Override
   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      for (ServiceProfileLine gsLine : this.profileLines.values()) {
//BUG-436: g-agroup: skip if agid=0 
         String msg = gsLine.toString();
         if( msg != null  && msg.length() > 0) {
            sbuf.append(msg);
	    sbuf.append("\r\n");
         }
      }
      return sbuf.toString();
   }

   public String getXMLTag() {
      //return XMLTagsAndAttributes.GROUP_SERVICE_PROFILE;
      return "group-service-profile";
   }

   /**
    * Parse and return a Group Service profile structure from an input buffer
    * by parsing it.
    * 
    * @param buffer
    * @return the GroupServiceProfile object
    */
   public static GroupServiceProfile createGroupServiceProfile(String buffer)
         throws ParseException {
      GroupServiceProfile retval = new GroupServiceProfile();
      parseProfileLines(retval, buffer);
      return retval;
   }

   @Override
   public boolean match(Object template) {
      if (!(template instanceof GroupServiceProfile))
         return false;
      return true;
   }
}
