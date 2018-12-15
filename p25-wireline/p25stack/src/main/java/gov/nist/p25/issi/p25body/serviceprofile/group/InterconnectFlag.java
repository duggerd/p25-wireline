//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Interconnect Flag
 * 
 */
public class InterconnectFlag extends ServiceProfileLine {

   public static final String NAME = "g-ic";
   public static final String SHORTNAME = "g-ic"; // same as NAME
   public static final boolean DEFAULTVALUE = false;

   private boolean interconnectAllowed = DEFAULTVALUE;

   public InterconnectFlag() {
      super(NAME, DEFAULTVALUE);
   }

   InterconnectFlag(String flag) throws ParseException {
      super(NAME, flag);
      this.interconnectAllowed = getBoolFromString(flag);
   }

   /**
    * @param flag
    *            The flag to set.
    */
   public void setInterconnectAllowed(boolean flag) {
      this.interconnectAllowed = flag;
   }

   /**
    * @return Returns the flag.
    */
   public boolean isInterconnectAllowed() {
      return interconnectAllowed;
   }
}
