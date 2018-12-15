//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import gov.nist.p25.issi.p25body.serviceprofile.SecurityLevelType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

import java.text.ParseException;

/**
 * Group Security Level 
 * 
 */
public class GroupSecurityLevel extends ServiceProfileLine {

   public static final String NAME = "g-sec";
   public static final String SHORTNAME = "g-s";
   public static final SecurityLevelType DEFAULTVALUE = SecurityLevelType.SECURE_AND_CLEAR_CALLS;
   private SecurityLevelType level = DEFAULTVALUE;

   /**
    * Default constructor -- assigns the default group security level.
    */
   GroupSecurityLevel() {
      super(NAME, DEFAULTVALUE.getIntValue());
      this.level = DEFAULTVALUE;
   }

   /**
    * Constructor taking the level String as an argument
    * 
    * @param level --
    *            level 1,2,3
    * @throws ParseException
    */
   GroupSecurityLevel(String level) throws ParseException {
      super(NAME, level);
      try {
         int lval = Integer.parseInt(level);
         if (lval < 1 || lval > 3)
            throw new ParseException("Value out of range", 0);
         this.level = SecurityLevelType.getInstance(lval);
      } catch (NumberFormatException ex) {
         throw new ParseException("Illegal value for level ", 0);
      }
   }

   GroupSecurityLevel(SecurityLevelType securityLevel) {
      super(NAME, new Integer(securityLevel.getIntValue()).toString());
      this.level = securityLevel;
   }

   public SecurityLevelType getLevel() {
      return level;
   }

   public void setLevel(SecurityLevelType level) {
      this.level = level;
      super.setValue(level.getIntValue());
   }

   /**
    * Create (parse) from xml attributes.
    * 
    * @param level
    * @return the ServiceProfileLine object
    */
   public static ServiceProfileLine createFromXmlAttributes(String level) {
      return new GroupSecurityLevel(SecurityLevelType.getInstance(level));
   }
}
