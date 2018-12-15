//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * Duplexity Type
 *
 */
public enum DuplexityType {

   HALF(0, "full duplex"),
   FULL(1, "half duplex");

   private int intValue;
   private String description;

   DuplexityType(int intValue, String description) {
      this.intValue = intValue;
      this.description = description;
   }

   public int getIntValue() {
      return intValue;
   }

   @Override
   public String toString() {
      return description;
   }

   public static DuplexityType getInstance(int intValue) {
      for (DuplexityType d : DuplexityType.values()) {
         if (d.intValue == intValue)
            return d;
      }
      throw new IllegalArgumentException("Illegal input arg: "+intValue);
   }
}
