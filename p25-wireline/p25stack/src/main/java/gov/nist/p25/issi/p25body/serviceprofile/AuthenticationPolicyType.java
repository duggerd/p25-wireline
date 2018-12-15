//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * The Authentication Policy attribute specifies the authentication policy 
 * to be applied to the unit.
 * 
 */
public enum AuthenticationPolicyType {

   NONE(0, "No policy is required"), 
   REQUIRED(1, "authentication is required when the home is reachable"),
   REJECT_IF_NOT_CURRENT( 2,
      "authentication is always required and authentication SHOULD be rejected "
         + "if the serving RFSS does not have the current parameters of the SU.");

   private int intValue;
   private String description;

   // constructor
   AuthenticationPolicyType(int intValue, String description) {
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

   public static AuthenticationPolicyType getInstance(int intValue) {
      for (AuthenticationPolicyType d : AuthenticationPolicyType.values()) {
         if (d.intValue == intValue)
            return d;
      }
      throw new IllegalArgumentException("Illegal input arg: "+intValue);
   }
}
