//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import java.text.ParseException;

import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Group Access Permission attribute defines whether or not the group is
 * allowed to make non-emergency calls at the serving RFSS. It has the following
 * format in ABNF notation: <br/> access-permission = g-access: / g-as: boolean
 * CRLF <br/> where 0 is used to indicate that non-emergency calls on the group
 * MAY NOT be placed from the serving RFSS, and 1 is used to indicate that
 * non-emergency calls MAY be placed. If the Access Permission attribute is not
 * present, non-emergency calls MAY be placed from the serving RFSS.
 * 
 */
public class AccessPermission extends ServiceProfileLine {

   public static final boolean DEFAULTVALUE = true;
   public static final String NAME = "g-access";
   public static final String SHORTNAME = "g-as";

   private boolean isNonEmergencyCallingAllowed;

   /**
    * Default constructor
    */
   AccessPermission() {
      super(NAME, DEFAULTVALUE);
      isNonEmergencyCallingAllowed = DEFAULTVALUE;
   }

   AccessPermission(String permission) throws ParseException {
      super(NAME, permission);
      if ("1".equals(permission))
         isNonEmergencyCallingAllowed = true;
      else if ("0".equals(permission))
         isNonEmergencyCallingAllowed = false;
      else
         throw new ParseException("Permission is invalid ", 0);
   }

   AccessPermission(boolean accessPermission) {
      super(NAME, accessPermission);
      this.isNonEmergencyCallingAllowed = accessPermission;
   }

   /**
    * @param isNonEmergencyCallingAllowed
    *            The isNonEmergencyCallingAllowed to set.
    */
   public void setNonEmergencyCallingAllowed( boolean isNonEmergencyCallingAllowed) {
      this.isNonEmergencyCallingAllowed = isNonEmergencyCallingAllowed;
      super.setValue(isNonEmergencyCallingAllowed);
   }

   /**
    * @return Returns the isNonEmergencyCallingAllowed.
    */
   public boolean isNonEmergencyCallingAllowed() {
      return isNonEmergencyCallingAllowed;
   }
   
   public static AccessPermission createFromXmlAttribute(String accessPermission) {
      //accessPermission (EmergencyOnly|NonEmergencyAllowed) 
      AccessPermission retval = new AccessPermission();
      if ( "EmergencyOnly".equals(accessPermission)) {
         retval.setNonEmergencyCallingAllowed(false);
      } else {
         retval.setNonEmergencyCallingAllowed(true);
      }
      return retval;
   }
}
