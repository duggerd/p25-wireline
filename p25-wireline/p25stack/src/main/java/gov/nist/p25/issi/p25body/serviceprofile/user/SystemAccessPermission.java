//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;

import gov.nist.p25.issi.p25body.serviceprofile.AccessPermissionType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * System Access Permission
 * 
 */
public class SystemAccessPermission extends ServiceProfileLine {

   public static final String NAME = "u-access";
   public static final String SHORTNAME = "u-as";
   public static final AccessPermissionType DEFAULTVALUE = 
      AccessPermissionType.FULL_ACCESS;

   private AccessPermissionType accessPermissionType = DEFAULTVALUE;

   SystemAccessPermission(int value) {
      super(NAME, value);
      this.accessPermissionType = AccessPermissionType.getInstance(value);
   }

   SystemAccessPermission() {
      super(NAME, DEFAULTVALUE.getIntValue());
   }

   SystemAccessPermission(String value) throws ParseException {
      super(NAME, value);
      int accessVal = super.getIntFromString(value);
      try {
         this.accessPermissionType = AccessPermissionType.getInstance(accessVal);
      } catch (IllegalArgumentException ex) {
         throw new ParseException("Illegal accessPermissionType value", 0);
      }
   }

   public void setAccessPermissionType(AccessPermissionType access) {
      this.accessPermissionType = access;
   }

   public AccessPermissionType getAccessPermissionType() {
      return this.accessPermissionType;
   }
}
