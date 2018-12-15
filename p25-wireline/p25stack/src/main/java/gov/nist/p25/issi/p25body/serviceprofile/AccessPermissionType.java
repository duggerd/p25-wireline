//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * Access Permission Type
 */
public enum AccessPermissionType {

   NO_ACCESS(0, "No access allowed","None"),
   FULL_ACCESS(1, "Full Access Allowed","Full"),
   EMERGENCY_ACCESS( 2, "Only Emergency Acccess Allowed","Emergency");

   private int intValue;
   private String description;
   private String shortDescr;

   AccessPermissionType(int intValue, String description, String shortDescr) {
      this.intValue = intValue;
      this.description = description;
      this.shortDescr = shortDescr;
   }

   public int getIntValue() {
      return this.intValue;
   }

   @Override
   public String toString() {
      return description;
   }

   public static AccessPermissionType getInstance(int intValue) {
      for (AccessPermissionType ac : AccessPermissionType.values()) {
         if (intValue == ac.intValue)
            return ac;
      }
      throw new IllegalArgumentException("Illegal input arg: " + intValue);
   }

   public static AccessPermissionType getInstance(String name) {
      for ( AccessPermissionType ac : AccessPermissionType.values()) {
         if (ac.shortDescr.equals(name))
            return ac;
      }
      throw new IllegalArgumentException("Illegal input arg: " + name) ;
   }
}
