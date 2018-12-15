//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Interconnect Security parameter indicates whether PSTN interconnect calls
 * are to start in the clear or in a secure mode. 0 indicates that interconnect
 * calls are to start in the clear, and 1 indicates that they are to start
 * secure. If the interconnect security attribute is not provided, calls are to
 * be started in the clear.
 * 
 */
public class InterconnectSecurity extends ServiceProfileLine {

   public static final String NAME = "g-icsecstart";
   public static final String SHORTNAME = "g-ics";
   public static final boolean DEFAULTVALUE = false;

   private boolean interconnectCallsSecure = DEFAULTVALUE;

   /**
    * Default constructor.
    */
   InterconnectSecurity() {
      super(NAME, "0");
      this.interconnectCallsSecure = DEFAULTVALUE;
   }

   /**
    * Constructor called by the parser.
    * 
    * @param flag --
    *            "0" or "1" are the valid args.
    * @throws ParseException --
    *             if the supplied arg is not good.
    */
   InterconnectSecurity(String flag) throws ParseException {
      super(NAME, flag);
      this.interconnectCallsSecure = super.getBoolFromString(flag);
   }

   /**
    * @param interconnectCallsSecure
    *            The interconnectCallsSecure to set.
    */
   public void setInterconnectCallsSecure(boolean interconnectCallsSecure) {
      this.interconnectCallsSecure = interconnectCallsSecure;
   }

   /**
    * @return Returns the interconnectCallsSecure.
    */
   public boolean isInterconnectCallsSecure() {
      return interconnectCallsSecure;
   }
}
