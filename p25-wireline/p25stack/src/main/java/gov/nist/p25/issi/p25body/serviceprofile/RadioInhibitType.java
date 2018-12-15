//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * Radio Inhibit Type
 * 
 */
public enum RadioInhibitType {

   NOT_RADIO_INHIBITED(0,
      "SU is not radio inhibited",
      "NotRadioInhibited"), 
   RADIO_INHIBITED(1,
      "SU is radio inhibited",
      "RadioInhibited");

   private int intValue;
   private String shortName;
   private String description;

   // constructor
   RadioInhibitType(int intValue, String description, String shortName) {
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

   public static RadioInhibitType getInstance(int intValue) {
      for (RadioInhibitType d: values()) {
         if (d.intValue == intValue)
            return d;
      }
      throw new IllegalArgumentException("Illegal input arg: "+intValue);
   }

   public static RadioInhibitType getInstance(String name) {
      for (RadioInhibitType d: values()) {
         if (d.shortName.equals(name))
            return d;
      }
      throw new IllegalArgumentException("Illegal input arg: " + name);
   }
}
