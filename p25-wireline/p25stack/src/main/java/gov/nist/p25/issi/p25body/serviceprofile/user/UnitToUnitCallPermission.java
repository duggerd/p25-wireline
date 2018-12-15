//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;

import gov.nist.p25.issi.p25body.serviceprofile.CallPermissionType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Unit-to-unit Call Permissions (u-ucall) indicates the types of
 * unit-to-unit calls in which the SU MAY participate. If no unit-to-unit call
 * permission is provided, the unit has no permission to participate in
 * unit-to-unit calls.
 * 
 */
public class UnitToUnitCallPermission extends ServiceProfileLine {

   public static final String NAME = "u-ucall";
   public static final String SHORTNAME = "u-uc";
   public static final CallPermissionType DEFAULTVALUE = CallPermissionType.NONE;
   private CallPermissionType permission = DEFAULTVALUE;

   UnitToUnitCallPermission(String perm) throws ParseException {
      super(NAME, perm);
      int p = super.getIntFromString(perm);
      try {
         this.permission = CallPermissionType.getInstance(p);
      } catch (IllegalArgumentException ex) {
         throw new ParseException("Value is out of range [" + perm + "]", 0);
      }
   }
   UnitToUnitCallPermission() {
      super(NAME, DEFAULTVALUE.getIntValue());
   }

   public CallPermissionType getPermission() {
      return permission;
   }
   public void setPermission(CallPermissionType permission) {
      this.permission = permission;
   }
}
