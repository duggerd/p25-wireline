//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * Authentication Type
 */
public enum AuthenticationType {

   NONE(0, "No authentication"), 
   LINK_LAYER_AUTHENTICATION(1, "Link Layer Authentication");

   private int intValue;
   private String description;

   AuthenticationType(int intValue, String description) {
      this.intValue = intValue;
      this.description = description;
   }

   public int intValue() {
      return intValue;
   }

   @Override
   public String toString() {
      return description;
   }

   public static AuthenticationType getInstance(int intValue) {
      if (intValue == 0)
         return NONE;
      else if (intValue == 1)
         return LINK_LAYER_AUTHENTICATION;
      else
         throw new IllegalArgumentException("Illegal input arg: "+intValue);
   }
}
