//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Security Capable
 */
public class SecureCapable extends ServiceProfileLine {

   public static final String NAME = "u-sec";
   public static final String SHORTNAME = "u-s";
   public static final boolean DEFAULTVALUE = false;

   private boolean isSecure = DEFAULTVALUE;

   // constructor
   SecureCapable(String str) throws ParseException {
      super(NAME, str);
      isSecure = super.getBoolFromString(str);
   }

   SecureCapable() {
      super(NAME, DEFAULTVALUE);
      isSecure = DEFAULTVALUE;
   }

   public boolean isSecure() {
      return this.isSecure;
   }

   public void setSecureCapable(boolean bool) {
      isSecure = bool;
      super.setValue(bool);
   }
}
