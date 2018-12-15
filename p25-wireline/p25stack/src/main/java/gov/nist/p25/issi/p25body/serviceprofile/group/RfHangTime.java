//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The RF Hang Time attribute specifies the RF hang-timer for the group. RF
 * hang-timer is specified in tenths of seconds and is limited to the range 0
 * 32767. If the RF hang timer attribute is not specified, the RF hang timer is
 * 0 (Transmission Trunked). The RF hang time has no infinity value.
 * 
 */
public class RfHangTime extends ServiceProfileLine {

   public static final String NAME = "g-rfhangt";
   public static final String SHORTNAME = "g-h";
   public static final int DEFAULTVALUE = 0;

   private int hangtime = DEFAULTVALUE;

   /**
    * Default constructor.
    */
   RfHangTime() {
      super(NAME, new Integer(DEFAULTVALUE).toString());
      this.hangtime = DEFAULTVALUE;
   }

   /**
    * Constructor called by the parser.
    * 
    * @param hangtime --
    *            parser supplied string.
    * @throws ParseException
    */
   RfHangTime(String hangtime) throws ParseException {
      super(NAME, hangtime);
      try {
         this.hangtime = new Integer(hangtime).intValue();
         if (this.hangtime > 32766 || this.hangtime < 0)
            throw new ParseException("Value out of range [" + hangtime
                  + "]", 0);
      } catch (NumberFormatException ex) {
         throw new ParseException("Invalid integer specified [" + hangtime
               + "]", 0);
      }
   }

   /**
    * @param rfHangtime
    *            The rfHangtime to set.
    * @throws IllegalArgumentException
    *             if value is outside range 0--32767
    */
   public void setHangtime(int rfHangtime) {
      if (rfHangtime > 32766 || rfHangtime < 0) {
         throw new IllegalArgumentException("Value out of range ["
               + rfHangtime + "]");
      }
      super.setValue(rfHangtime);
      this.hangtime = rfHangtime;
   }
   
   /**
    * Constructor given an int 
    * 
    * @param rfHangtime
    */
   public  RfHangTime(int rfHangtime) {
      super(NAME, new Integer(rfHangtime).toString());
      if (rfHangtime > 32766 || rfHangtime < 0) {
         throw new IllegalArgumentException("Value out of range ["
               + rfHangtime + "]");
      }
      this.hangtime = rfHangtime;
   }

   /**
    * @return Returns the rfHangtime.
    */
   public int getHangime() {
      return hangtime;
   }
}
