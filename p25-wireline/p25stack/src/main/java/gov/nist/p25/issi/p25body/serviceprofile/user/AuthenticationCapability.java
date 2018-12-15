//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;

import gov.nist.p25.issi.p25body.serviceprofile.AuthenticationType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Authentication Type attribute specifies the type of authentication to be
 * applied to the unit. If no authentication type is specified, the unit does
 * not require authentication.
 * 
 */
public class AuthenticationCapability extends ServiceProfileLine {

   public static final String NAME = "u-authtype";
   public static final String SHORTNAME = "u-at";
   public static final AuthenticationType DEFAULTVALUE = AuthenticationType.NONE;
   private AuthenticationType authenticationType = DEFAULTVALUE;

   AuthenticationCapability(String capability) throws ParseException {
      super(NAME, capability);
      int ctype = super.getIntFromString(capability);
      if (ctype != 0 && ctype != 1)
         throw new ParseException("Bad value [" + capability + "]", 0);
      this.authenticationType = AuthenticationType.getInstance(ctype);
   }

   AuthenticationCapability() {
      super(NAME, DEFAULTVALUE.intValue());
   }

   /**
    * @param authenticationType
    *            The authenticationType to set.
    */
   public void setAuthenticationType(AuthenticationType authenticationType) {
      this.authenticationType = authenticationType;
   }

   /**
    * @return Returns the authenticationType.
    */
   public AuthenticationType getAuthenticationType() {
      return authenticationType;
   }
}
