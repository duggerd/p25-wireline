//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.SecurityLevelType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Interconnect Security attribute specifies the acceptable security modes
 * of interconnect calls. If no interconnect security attribute is specified,
 * both secure and clear calls are acceptable.
 * 
 */
public class InterconnectSecurity extends ServiceProfileLine {

   public static final String NAME = "u-icsec";
   public static final String SHORTNAME = "u-ics";
   public static final SecurityLevelType DEFAULTVALUE = 
      SecurityLevelType.SECURE_AND_CLEAR_CALLS;

   private SecurityLevelType securityLevel = DEFAULTVALUE;

   InterconnectSecurity(String levelStr) throws ParseException {
      super(NAME, levelStr);
      int lev = super.getIntFromString(levelStr);
      try {
         this.securityLevel = SecurityLevelType.getInstance(lev);
      } catch (IllegalArgumentException ex) {
         throw new ParseException("Value out of range " + levelStr, 0);
      }
   }

   InterconnectSecurity() {
      super(NAME, DEFAULTVALUE.getIntValue());
   }

   /**
    * @param securityLevel
    *            The securityLevel to set.
    */
   public void setSecurityLevel(SecurityLevelType securityLevel) {
      this.securityLevel = securityLevel;
   }

   /**
    * @return Returns the securityLevel.
    */
   public SecurityLevelType getSecurityLevel() {
      return securityLevel;
   }
}
