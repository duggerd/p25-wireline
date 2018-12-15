//
package gov.nist.p25.issi.p25body.serviceprofile.user;

//import gov.nist.p25.issi.constants.XMLTagsAndAttributes;
import gov.nist.p25.issi.p25body.serviceprofile.AvailabilityCheckType;
import gov.nist.p25.issi.p25body.serviceprofile.DuplexityType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfile;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;
import gov.nist.p25.issi.p25body.serviceprofile.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

/**
 * UserServiceProfile
 */
@SuppressWarnings({"unused", "unchecked"})
public class UserServiceProfile extends ServiceProfile {

   private static Logger logger = Logger.getLogger(UserServiceProfile.class);

   static {
      profileLineClasses.put(AuthenticationCapability.NAME,
            AuthenticationCapability.class);
      profileLineClasses.put(AuthenticationCapability.SHORTNAME,
            AuthenticationCapability.class);

      profileLineClasses.put(AuthenticationParameters.NAME,
            AuthenticationParameters.class);
      profileLineClasses.put(AuthenticationParameters.SHORTNAME,
            AuthenticationParameters.class);

      profileLineClasses.put(AuthenticationPolicy.NAME,
            AuthenticationPolicy.class);
      profileLineClasses.put(AuthenticationPolicy.SHORTNAME,
            AuthenticationPolicy.class);

      profileLineClasses.put(AvailabilityCheck.NAME, AvailabilityCheck.class);
      profileLineClasses.put(AvailabilityCheck.SHORTNAME,
            AvailabilityCheck.class);

      profileLineClasses.put(CallSetupPreference.NAME,
            CallSetupPreference.class);
      profileLineClasses.put(CallSetupPreference.SHORTNAME,
            CallSetupPreference.class);

      profileLineClasses.put(Duplexity.NAME, Duplexity.class);
      profileLineClasses.put(Duplexity.SHORTNAME, Duplexity.class);

      profileLineClasses.put(GroupCallPermissions.NAME,
            GroupCallPermissions.class);
      profileLineClasses.put(GroupCallPermissions.SHORTNAME,
            GroupCallPermissions.class);

      profileLineClasses.put(InterconnectPermission.NAME,
            InterconnectPermission.class);
      profileLineClasses.put(InterconnectPermission.SHORTNAME,
            InterconnectPermission.class);

      //BUG-474
      profileLineClasses.put(RadioInhibit.NAME, RadioInhibit.class);
      profileLineClasses.put(RadioInhibit.SHORTNAME, RadioInhibit.class);

      profileLineClasses.put(SecureCapable.NAME, SecureCapable.class);
      profileLineClasses.put(SecureCapable.SHORTNAME, SecureCapable.class);

      profileLineClasses.put(SystemAccessPermission.NAME,
            SystemAccessPermission.class);
      profileLineClasses.put(SystemAccessPermission.SHORTNAME,
            SystemAccessPermission.class);

      profileLineClasses.put(UnitToUnitCallPermission.NAME,
            UnitToUnitCallPermission.class);
      profileLineClasses.put(UnitToUnitCallPermission.SHORTNAME,
            UnitToUnitCallPermission.class);

      profileLineClasses.put(UnitToUnitCallPriority.NAME,
            UnitToUnitCallPriority.class);
      profileLineClasses.put(UnitToUnitCallPriority.SHORTNAME,
            UnitToUnitCallPriority.class);

      profileLineClasses.put(InterconnectSecurity.NAME,
            InterconnectSecurity.class);
      profileLineClasses.put(InterconnectSecurity.SHORTNAME,
            InterconnectSecurity.class);

      profileLineClasses.put(InterconnectCallPriority.NAME,
            InterconnectCallPriority.class);
      profileLineClasses.put(InterconnectCallPriority.SHORTNAME,
            InterconnectCallPriority.class);
   }

   private boolean isFullDuplexIsSet = false;
   public boolean isFullDuplexIsSet() { return isFullDuplexIsSet; }
   public void setFullDuplexIsSet(boolean bflag) { isFullDuplexIsSet=bflag; }

   // constructor
   public UserServiceProfile() {
      /**
       * Create a Default UserService profile.
       */
      //this.profileLines = new Hashtable<String, ServiceProfileLine>();
      this.profileLines = new LinkedHashMap<String, ServiceProfileLine>();

      // arranged into output order
      this.addProfileLine(new SystemAccessPermission());
      this.addProfileLine(new Duplexity());
      this.addProfileLine(new SecureCapable());
      this.addProfileLine(new GroupCallPermissions());
      this.addProfileLine(new UnitToUnitCallPermission());
      this.addProfileLine(new UnitToUnitCallPriority());
      this.addProfileLine(new InterconnectPermission());
      this.addProfileLine(new InterconnectSecurity());
      this.addProfileLine(new InterconnectCallPriority());

      this.addProfileLine(new AuthenticationCapability());
      this.addProfileLine(new AuthenticationPolicy());
      this.addProfileLine(new AvailabilityCheck());
      this.addProfileLine(new CallSetupPreference());
//BUG-474:
      this.addProfileLine(new RadioInhibit());
   }

   /**
    * Create a group service profile class given the unparsed line.
    * 
    * @param groupServiceProfileLine
    * @return the ServiceProfileLine object
    * @throws ParseException --
    *             if an error was reached parsing the line.
    */
   protected ServiceProfileLine createServiceProfileLine(String groupServiceProfileLine) 
      throws ParseException {
      if (groupServiceProfileLine == null) {
         throw new NullPointerException("null argument");
      }

      String[] tokens = groupServiceProfileLine.split(":");
      if (tokens == null || tokens.length != 2) {
         logger.error("Error in parsing service profile line : "
               + groupServiceProfileLine);
         throw new ParseException(groupServiceProfileLine, 0);
      }
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
         Constructor cons = gspLineClass.getDeclaredConstructor(new Class[] { String.class });
         logger.debug("service profile line class " + gspLineClass);
         ServiceProfileLine retval = (ServiceProfileLine) cons
               .newInstance((Object[]) (new String[] {value }));
         return retval;

      } catch (InvocationTargetException ex) {
         // The cause has the actual reason failure
         logger.error("Error parsing profile line " + name + " value=" + value);
         throw new RuntimeException(ex.getCause());
      } catch (NoSuchMethodException ex) {
         throw new RuntimeException(ex);
      } catch (IllegalArgumentException e) {
         throw new RuntimeException(e);
      } catch (InstantiationException e) {
         throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
         throw new ParseException("Invalid field Name : " + name, 0);
      }
   }

   public Duplexity getDuplexity() {
      return (Duplexity) this.getByName(Duplexity.NAME);
   }

   public AuthenticationCapability getAuthenticationCapability() {
      return (AuthenticationCapability) this
            .getByName(AuthenticationCapability.NAME);
   }

   public AuthenticationPolicy getAuthenticationPolicy() {
      return (AuthenticationPolicy) this.getByName(AuthenticationPolicy.NAME);
   }

   public AuthenticationParameters getAuthenticationParameters() {
      return (AuthenticationParameters) getByName(AuthenticationParameters.NAME);
   }

   public AvailabilityCheck getAvailabilityCheck() {
      return (AvailabilityCheck) getByName(AvailabilityCheck.NAME);
   }

   public CallSetupPreference getCallSetupPreference() {
      return (CallSetupPreference) getByName(CallSetupPreference.NAME);
   }

   public GroupCallPermissions getGroupCallPermissions() {
      return (GroupCallPermissions) getByName(GroupCallPermissions.NAME);
   }

   public InterconnectPermission getInterconnectPermission() {
      return (InterconnectPermission) getByName(InterconnectPermission.NAME);
   }

//BUG-474:
   public RadioInhibit getRadioInhibit() {
      return (RadioInhibit) getByName(RadioInhibit.NAME);
   }

   public SecureCapable getSecureCapable() {
      return (SecureCapable) getByName(SecureCapable.NAME);
   }

   public SystemAccessPermission getSystemAccessPermission() {
      return (SystemAccessPermission) getByName(SystemAccessPermission.NAME);
   }

   public UnitToUnitCallPermission getUnitToUnitCallPermission() {
      return (UnitToUnitCallPermission) getByName(UnitToUnitCallPermission.NAME);
   }

   public UnitToUnitCallPriority getUnitToUnitCallPriority() {
      return (UnitToUnitCallPriority) getByName(UnitToUnitCallPriority.NAME);
   }

   public InterconnectSecurity getInterconnectSecurity() {
      return (InterconnectSecurity) getByName(InterconnectSecurity.NAME);
   }

   public InterconnectCallPriority getInterconnectCallPriority() {
      return (InterconnectCallPriority) getByName(InterconnectCallPriority.NAME);
   }

   /**
    * Parse and return a Group Service profile structure from an input buffer
    * by parsing it.
    * 
    * @param buffer
    * @return the UserServiceProfile object
    */
   public static UserServiceProfile createUserServiceProfile(String buffer)
         throws ParseException {
      UserServiceProfile retval = new UserServiceProfile();
      parseProfileLines(retval, buffer);
      return retval;
   }

   protected String getXMLTag() {
      // Decouple to allow standalone p25stack.jar !!!
      //return XMLTagsAndAttributes.USER_SERVICE_PROFILE;
      return "user-service-profile";
   }

   @Override
   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      for (ServiceProfileLine gsLine : this.profileLines.values()) {
         //BUG-473: u-authpol: skip if empty string
         String msg = gsLine.toString();
         if( msg != null  && msg.length() > 0) {
            //sbuf.append(gsLine.toString());
            sbuf.append(msg);
	    sbuf.append("\r\n");
	 }
      }
      return sbuf.toString();
   }
   
   public static UserServiceProfile createServiceProfile(Attributes attributes)
         throws ParseException {
      UserServiceProfile retval = new UserServiceProfile();
      for (int i = 0; i < attributes.getLength(); i++) {
         String name = attributes.getLocalName(i);
         String value = attributes.getValue(i);
         ServiceProfileLine spLine = createServiceProfileLine(name, value);
         retval.addProfileLine(spLine);
      }
      return retval;
   }
   
   public boolean match (Object template) {
      UserServiceProfile templateProfile = (UserServiceProfile) template;
      for ( String key: this.profileLines.keySet() ) {
         ServiceProfileLine line  = this.profileLines.get(key);
         for (String key1 : templateProfile.profileLines.keySet()) {
            ServiceProfileLine line1 = templateProfile.profileLines.get(key1);            
         }
      }
      return true;
   }

   public boolean isAvailabilityCheckSupported() {
      return this.getAvailabilityCheck().getAvailabilityCheckType() ==
         AvailabilityCheckType.AVAIL_CHECK_ONLY ||
         this.getAvailabilityCheck().getAvailabilityCheckType() ==
            AvailabilityCheckType.AVAIL_CHECK_AND_DIRECT_CALL;      
   }

   public void setFullDuplex(boolean isDuplex) {
      if ( isDuplex) {
         this.getDuplexity().setDuplex(DuplexityType.FULL);
      } else { 
         this.getDuplexity().setDuplex(DuplexityType.HALF);      
      }
   }

   public void setAccessPermission(String accessPermission) {
      this.getSystemAccessPermission().setAccessPermissionType(
            AccessPermissionType.getInstance(accessPermission));      
   }

//BUG-474:
   public void setRadioInhibit(String value) {
      this.getRadioInhibit().setRadioInhibitType(
            RadioInhibitType.getInstance(value));      
   }

   public void setSecureCapable(boolean bool) {
      this.getSecureCapable().setSecureCapable(bool);      
   }

   public void setAvailabilityCheck(String value) {
      this.getAvailabilityCheck().setAvailabilityCheckType(
            AvailabilityCheckType.getInstance(value));      
   }

   public void setGroupCallPermission(String value) {
      this.getGroupCallPermissions().setGroupCallPermissionType(
            GroupCallPermissionType.getInstance(value));   
   }

   public void setUnitToUnitCallPermission(String value) {
      this.getUnitToUnitCallPermission().setPermission(
            CallPermissionType.getInstance(value));      
   }

   public void setCallPriority(int value) {
      this.getUnitToUnitCallPriority().setPriority(value);      
   }

   public void setUnitToUnitCallSetupPreference(String value) {
      this.getCallSetupPreference().setCallSetupPreferenceType(
            CallSetupPreferenceType.getInstance(value));      
   }

   public void setInterconnectCallPriority(int icp) {
      InterconnectCallPriority icpri = this.getInterconnectCallPriority();
      if (icpri != null) { 
         icpri.setPriority(icp);
      } else {
         icpri = new InterconnectCallPriority();
         icpri.setPriority(icp);
         this.addProfileLine(icpri);
      }      
   }
}
