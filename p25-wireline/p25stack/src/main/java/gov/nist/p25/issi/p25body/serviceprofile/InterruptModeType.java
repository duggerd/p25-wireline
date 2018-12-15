//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * Interrupt Mode Type
 */
public enum InterruptModeType {

   // (NeverAllowed|PriorityBasedAllowed|AlwaysAllowd)
   AUDIO_INTERRRUPT_NEVER_ALLOWED(0, 
      "0: Audio Interrupt Never Allowed","NeverAllowed"),
   AUDIO_INTERRRUPT_BASED_ON_PRIORITY(1,
      "1: Audio Interrupt Based On Priority","PriorityInterruptAllowed"),
   AUDIO_INTERRRUPT_ALWAYS_ALLOWED(2,
      "2: Audio Interrupt Always Allowed","AlwaysAllowed");

   private int intValue;
   private String description;
   private String shortName;

   private InterruptModeType(int modeValue, String description, String shortName) {
      this.intValue = modeValue;
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

   public static InterruptModeType getModeValues(int intValue) {
      for (InterruptModeType mv : InterruptModeType.values()) {
         if (intValue == mv.intValue)
            return mv;
      }
      throw new IllegalArgumentException("Illegal input arg: " + intValue);
   }

   /**
    * Find mode corresponding to xml attribute.
    * 
    * @param name
    * @return the InterruptModeType object
    */
   public static InterruptModeType getValue(String name) {
      for (InterruptModeType mv : InterruptModeType.values()) {
         if (mv.shortName.equals( name))
            return mv;
      }
      throw new IllegalArgumentException("Illegal input arg: " + name);
   }
}
