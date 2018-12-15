//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.CallSetupPreferenceType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Call Setup Preference
 */
public class CallSetupPreference extends ServiceProfileLine {

   public static final String NAME = "u-prefsetup";
   public static final String SHORTNAME = "u-prf";
   public static final CallSetupPreferenceType DEFAULTVALUE = 
         CallSetupPreferenceType.CALLER_PREFERS_AVAILABILITY_CHECK;

   private CallSetupPreferenceType callSetupPreferenceType;

   CallSetupPreference(String val) throws ParseException {
      super(NAME, val);
      int intval = super.getIntFromString(val);
      try {
         this.callSetupPreferenceType = CallSetupPreferenceType.getInstance(intval);
      } catch (IllegalArgumentException ex) {
         throw new ParseException("Illegal argument [" + val + "]", 0);
      }
   }

   CallSetupPreference() {
      super(NAME, DEFAULTVALUE.getIntValue());
      this.callSetupPreferenceType = DEFAULTVALUE;
   }

   /**
    * @param callSetupPreferenceType
    *            The callSetupPreferenceType to set.
    */
   public void setCallSetupPreferenceType(CallSetupPreferenceType callSetupPreferenceType) {
      this.callSetupPreferenceType = callSetupPreferenceType;
   }

   /**
    * @return Returns the callSetupPreferenceType.
    */
   public CallSetupPreferenceType getCallSetupPreferenceType() {
      return callSetupPreferenceType;
   }
}
