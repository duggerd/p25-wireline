//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.CallPermissionType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Interconnect Permissions attribute specifies the permissions of the unit
 * vis-a-vis interconnect (PSTN) calls. If no interconnect permission attribute
 * is specified, the unit does not have permission to participate in
 * interconnect calls.
 * 
 */
public class InterconnectPermission extends ServiceProfileLine {

   public static final String NAME = "u-iccall";
   public static final String SHORTNAME = "u-icc";
   public static final CallPermissionType DEFAULTVALUE = CallPermissionType.NONE;

   private CallPermissionType permission;

   // constructor
   InterconnectPermission(String perm) throws ParseException {
      super(NAME, perm);
      int p = super.getIntFromString(perm);
      if (p > CallPermissionType.values().length || p < 0)
         throw new ParseException("Bad arg : " + perm, 0);
      this.permission = CallPermissionType.getInstance(p);
   }

   InterconnectPermission() {
      super(NAME, DEFAULTVALUE.getIntValue());
      this.permission = DEFAULTVALUE;
   }

   public CallPermissionType getPermission() {
      return permission;
   }

   public void setPermission(CallPermissionType permission) {
      this.permission = permission;
   }
}
