//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;

import gov.nist.p25.issi.p25body.serviceprofile.DuplexityType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Duplexity
 */
public class Duplexity extends ServiceProfileLine {

   public static final String NAME = "u-dup";
   public static final String SHORTNAME = "u-d";
   public static final DuplexityType DEFAULTVALUE = DuplexityType.HALF;

   private DuplexityType duplex;

   Duplexity(String duplexity) throws ParseException {
      super(NAME, duplexity);
      int d = super.getIntFromString(duplexity);
      this.duplex = DuplexityType.getInstance(d);
   }

   Duplexity() {
      super(NAME, DEFAULTVALUE.getIntValue());
      this.duplex = DEFAULTVALUE;
   }

   /**
    * @param duplex
    *            The duplex to set.
    */
   public void setDuplex(DuplexityType duplex) {
      this.duplex = duplex;
      super.setValue(duplex.getIntValue());
   }

   /**
    * @return Returns the duplex.
    */
   public DuplexityType getDuplex() {
      return duplex;
   }
}
