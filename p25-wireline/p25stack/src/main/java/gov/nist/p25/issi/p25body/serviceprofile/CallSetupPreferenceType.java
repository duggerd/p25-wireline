//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * Call Setup Preference Type
 * 
 */
public enum CallSetupPreferenceType {

   // ("AvailabilityCheck" | "DirectCall" )
   CALLER_PREFERS_AVAILABILITY_CHECK(0,
      "Calling SU prefers Availability Check for the Called SU",
      "AvailabilityCheck"), 
   CALLER_PREFERS_DIRECT_CALL(1,
      "Calling SU prefers Direct Call for the Called SU",
      "DirectCall");

   private int intValue;
   private String shortName;
   private String description;

   CallSetupPreferenceType(int intValue, String description, String shortName) {
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

   public static CallSetupPreferenceType getInstance(int intValue) {
      for (CallSetupPreferenceType d : values()) {
         if (d.intValue == intValue)
            return d;
      }
      throw new IllegalArgumentException("Illegal input arg: "+intValue);
   }

   public static CallSetupPreferenceType getInstance(String name) {
      for (CallSetupPreferenceType d : values()) {
         if (d.shortName.equals(name))
            return d;
      }
      throw new IllegalArgumentException("Illegal input arg: " + name);
   }
}
