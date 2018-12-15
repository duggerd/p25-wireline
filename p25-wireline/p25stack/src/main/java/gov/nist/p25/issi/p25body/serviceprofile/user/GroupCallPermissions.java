//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;

import gov.nist.p25.issi.p25body.serviceprofile.GroupCallPermissionType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Group Call Capability attribute specifies the types of group calls
 * allowed that the given SU MAY initiate. If the capability is not specified,
 * the unit is given both emergency and non-emergency permissions.
 * 
 */
public class GroupCallPermissions extends ServiceProfileLine {

   public static final String NAME = "u-gcall";
   public static final String SHORTNAME = "u-g";
   public static GroupCallPermissionType DEFAULTVALUE = 
      GroupCallPermissionType.EMERGENCY_AND_NON_EMERGENCY;

   private GroupCallPermissionType permission;

   // constructor
   GroupCallPermissions(String perm) throws ParseException {
      super(NAME, perm);
      int p = super.getIntFromString(perm);
      if (p > GroupCallPermissionType.values().length || p < 0)
         throw new ParseException("Bad arg : " + perm, 0);
      this.permission = GroupCallPermissionType.getInstance(p);
   }

   GroupCallPermissions() {
      super(NAME, DEFAULTVALUE.getIntValue());
      this.permission = DEFAULTVALUE;
   }

   public GroupCallPermissionType getPermission() {
      return permission;
   }

   public void setGroupCallPermissionType(GroupCallPermissionType permission) {
      this.permission = permission;
   }
}
