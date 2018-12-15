//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * Availability Check Type
 *
 */
public enum AvailabilityCheckType {

   AVAIL_CHECK_ONLY(1, 
      "SU supports only Availability Check",
      "AvailabilityCheck"), 
   DIRECT_CALL_ONLY(2, 
      "SU supports only Direct Call",
      "DirectCall"), 
   AVAIL_CHECK_AND_DIRECT_CALL(3,
      "SU supports both Availability Check and Direct Call",
      "AvailabilityCheckAndDirectCall");

   private int intValue;
   private String shortDesc;
   private String description;
   
   AvailabilityCheckType(int intValue, String description, String shortDesc) {
      this.intValue = intValue;
      this.description = description;
      this.shortDesc = shortDesc;
   }

   public int getIntValue() {
      return intValue;
   }

   @Override
   public String toString() {
      return description;
   }

   public static AvailabilityCheckType getInstance(int intValue) {
      for (AvailabilityCheckType ac : AvailabilityCheckType.values()) {
         if (ac.intValue == intValue)
            return ac;
      }
      throw new IllegalArgumentException("Illegal input arg: " + intValue);
   }
   
   public static AvailabilityCheckType getInstance(String name) {
      for (AvailabilityCheckType ac: AvailabilityCheckType.values()) {
         if ( ac.shortDesc.equals(name)) {
            return ac;
         }
      }
      throw new IllegalArgumentException("Illegal input arg: " + name);
   }
}
