//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;

import gov.nist.p25.issi.p25body.serviceprofile.AuthenticationPolicyType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Authentication Policy attribute specifies the authentication policy to be
 * applied to the unit.
 * 
 */
public class AuthenticationPolicy extends ServiceProfileLine {

   public static final String NAME = "u-authpol";
   public static final String SHORTNAME = "u-apo";
   public static final AuthenticationPolicyType DEFAULTVALUE = AuthenticationPolicyType.NONE;

   private AuthenticationPolicyType authenticationType = DEFAULTVALUE;

   AuthenticationPolicy(String capability) throws ParseException {
      super(NAME, capability);
      int ctype = super.getIntFromString(capability);
      if (ctype < 0 && ctype > 2)
         throw new ParseException("Bad value [" + capability + "]", 0);
      this.authenticationType = AuthenticationPolicyType.getInstance(ctype);
   }

   AuthenticationPolicy() {
      super(NAME, DEFAULTVALUE.getIntValue());
   }

   /**
    * @param authenticationType
    *            The authenticationType to set.
    */
   void setAuthenticationPolicyType(AuthenticationPolicyType authenticationType) {
      this.authenticationType = authenticationType;
   }

   /**
    * @return Returns the authenticationType.
    */
   public AuthenticationPolicyType getAuthenticationPolicyType() {
      return authenticationType;
   }

   @Override
   public String toString() {
      //System.out.println("+++++++AuthenticationType: "+authenticationType);
      if( authenticationType == DEFAULTVALUE)
         return "";
      return super.toString();
   }
}
