//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * Group Call Permission Type
 *
 */
public enum GroupCallPermissionType {

   // ("None"|"NonEmergency"|"Emergency"|"EmergencyAndNonEmergency")
   NONE(0, "No group permission","None"), 
   NON_EMERGENCY(1,
      "Non emergency group call permission","NonEmergency"),
   EMERGENCY(2,
      "Emergency group call permission","Emergency"),
   EMERGENCY_AND_NON_EMERGENCY(3,
      "Emergency and non-emergency permission","EmergencyAndNonEmergency");

   private int intValue;
   private String description;
   private String shortName;

   GroupCallPermissionType(int intValue, String description, String shortName) {
      this.intValue = intValue;
      this.description = description;
      this.shortName = shortName;
   }

   public int getIntValue() {
      return intValue;
   }

   @Override
   public String toString() {
      return description;
   }

   public static GroupCallPermissionType getInstance(int intValue) {
      for (GroupCallPermissionType p : values()) {
         if (p.intValue == intValue)
            return p;
      }
      throw new IllegalArgumentException("Illegal input arg: " + intValue);
   }

   public static GroupCallPermissionType getInstance(String name) {
      for ( GroupCallPermissionType gc: GroupCallPermissionType.values()) {
         if ( gc.shortName.equals(name))
            return gc;
      }
      throw new IllegalArgumentException("Illegal input arg: " + name);
   }
}
