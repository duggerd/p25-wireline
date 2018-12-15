//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * Security Level Type
 *
 */
public enum SecurityLevelType {

   // (ClearOnly|SecureOnly|ClearAndSecure)
   CLEAR_MODE_ONLY_CALLS(1, "1: Clear Mode Only","ClearOnly"),
   SECURE_MODE_ONLY_CALLS(2,"2: Secure Mode Only Calls","SecureOnly"), 
   SECURE_AND_CLEAR_CALLS(3,"3: Clear Mode and Secure mode calls","ClearAndSecure");

   private int intValue;
   private String description;
   private String shortName;

   private SecurityLevelType(int intValue, String description, String shortName) {
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

   public static SecurityLevelType getInstance(int intValue) {
      for (SecurityLevelType sl : SecurityLevelType.values()) {
         if (intValue == sl.intValue)
            return sl;
      }
      throw new IllegalArgumentException("Illegal input arg: " + intValue);
   }

   public static SecurityLevelType getInstance(String name) {
      for (SecurityLevelType sl : SecurityLevelType.values()) {
         if ( sl.shortName.equals(name)) 
            return sl;
      }
      throw new IllegalArgumentException("Illegal input arg: " + name);
   }
}
