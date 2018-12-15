//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;

import gov.nist.p25.issi.p25body.serviceprofile.AvailabilityCheckType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Availability Check attribute specifies the ability of the SU to support
 * an Availability Check, a direct call or both.
 * 
 */
public class AvailabilityCheck extends ServiceProfileLine {

   public static final String NAME = "u-availcheck";
   public static final String SHORTNAME = "u-ac";
   public static final AvailabilityCheckType DEFAULTVALUE = 
         AvailabilityCheckType.AVAIL_CHECK_AND_DIRECT_CALL;

   private AvailabilityCheckType availabilityCheckType;

   AvailabilityCheck(String val) throws ParseException {
      super(NAME, val);
      int intval = super.getIntFromString(val);
      if (intval < 1 || intval > 3)
         throw new ParseException("Illegal argument [" + val + "]", 0);
      this.availabilityCheckType = AvailabilityCheckType.getInstance(intval);
   }

   AvailabilityCheck() {
      super(NAME, DEFAULTVALUE.getIntValue());
      this.availabilityCheckType = DEFAULTVALUE;
   }

   /**
    * @param availabilityCheckType
    *            The availabilityCheckType to set.
    */
   public void setAvailabilityCheckType( AvailabilityCheckType availabilityCheckType) {
      this.availabilityCheckType = availabilityCheckType;
   }

   /**
    * @return Returns the availabilityCheckType.
    */
   public AvailabilityCheckType getAvailabilityCheckType() {
      return availabilityCheckType;
   }
}
