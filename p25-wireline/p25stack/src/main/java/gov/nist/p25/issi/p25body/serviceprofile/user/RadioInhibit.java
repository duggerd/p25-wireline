//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;

import gov.nist.p25.issi.p25body.serviceprofile.RadioInhibitType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Radio Inhibit 
 */
public class RadioInhibit extends ServiceProfileLine {

   public static final String NAME = "u-radioinhib";
   public static final String SHORTNAME = "u-rdinh";

   public static final RadioInhibitType DEFAULTVALUE = 
         RadioInhibitType.NOT_RADIO_INHIBITED;

   private RadioInhibitType radioInhibitType = DEFAULTVALUE;

   // constructor
   public RadioInhibit() {
      super(NAME, DEFAULTVALUE.getIntValue());
      this.radioInhibitType = DEFAULTVALUE;
   }
   public RadioInhibit(String val) throws ParseException {
      super(NAME, val);
      int intval = super.getIntFromString(val);
      try {
         this.radioInhibitType = RadioInhibitType.getInstance(intval);
      } catch (IllegalArgumentException ex) {
         throw new ParseException("Illegal argument [" + val + "]", 0);
      }
   }

   // accessor
   public RadioInhibitType getRadioInhibitType() {
      return radioInhibitType;
   }
   public void setRadioInhibitType(RadioInhibitType radioInhibitType) {
      this.radioInhibitType = radioInhibitType;
   }
}
