//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Confirmmed Call Setup Time
 * 
 */
public class ConfirmedCallSetupTime extends ServiceProfileLine {

   public static final String NAME = "g-ccsetupT";
   public static final String SHORTNAME = "g-c";
   public static final int DEFAULTVALUE = 0;

   private int setupTime = DEFAULTVALUE;

   ConfirmedCallSetupTime(String hangtime) throws ParseException {
      super(NAME, hangtime);
      try {
         setupTime = new Integer(hangtime).intValue();
         if (setupTime > 32766 || setupTime < 0)
            throw new ParseException("Value out of range [" + setupTime
                  + "]", 0);
      } catch (NumberFormatException ex) {
         throw new ParseException("Invalid integer specified [" + hangtime
               + "]", 0);
      }
   }

   ConfirmedCallSetupTime(int csetupTime) {
      super(NAME, csetupTime);
      setSetupTime( csetupTime);
   }

   public ConfirmedCallSetupTime() {
      super(NAME, DEFAULTVALUE);
      this.setupTime = DEFAULTVALUE;
   }

   /**
    * @return Returns the confirmed call setup time.
    */
   public int getSetupTime() {
      return setupTime;
   }

   /**
    * Set the call setup time.
    */
   public void setSetupTime(int setupTime) {
      if (setupTime > 32766 || setupTime < 0) {
         throw new IllegalArgumentException("Value out of range ["
               + setupTime + "]");
      }
      super.setValue(setupTime);
      this.setupTime = setupTime;
   }

   /**
    * return true if setup time is infinite.
    */
   public boolean isInfiniteSetupTime() {
      return this.setupTime == 32766;
   }

   /**
    * return true if group calls do not need to be confirmed.
    */
   public boolean isUnconfirmed() {
      return this.setupTime == 0;
   }
}
