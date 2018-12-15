//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.InterruptModeType;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Interrupt Mode (g-intmode) The Interrupt Mode attribute specifies the audio
 * interrupt mode of the voice group. imode = 0 indicates that audio
 * interruption by SUs within a talk group call is never allowed, 1 indicates
 * that it is allowed based on SU priority, and 2 indicates that it is always
 * allowed. If the Interrupt Mode is not specified, the Interrupt Mode is 0.
 * 
 */
public class InterruptMode extends ServiceProfileLine {

   public static final String NAME = "g-intmode";
   public static final String SHORTNAME = "g-in";
   public static final InterruptModeType DEFAULTVALUE = 
         InterruptModeType.AUDIO_INTERRRUPT_NEVER_ALLOWED;

   private InterruptModeType mode = DEFAULTVALUE;

   /**
    * Constructor called by parser.
    * 
    * @param value --
    *            "0", "1" or "2". Anything else results in parse exception.
    * @throws ParseException
    */
   InterruptMode(String value) throws ParseException {
      super(NAME, value);
      try {
         int interruptMode = Integer.parseInt(value);
         if (interruptMode < 0 || interruptMode > 2)
            throw new ParseException(value
                  + ": Value out of range 0,1,2 only allowed", 0);
         this.setMode(InterruptModeType.getModeValues(interruptMode));

      } catch (NumberFormatException ex) {
         throw new ParseException("Bad number format [" + value + "]", 0);
      }
   }

   /**
    * Default constructor.
    * 
    * @return
    */
   InterruptMode() {
      super(NAME, DEFAULTVALUE.getIntValue());
   }

   /**
    * @param modeValue
    *            The modeValue to set.
    */
   public void setMode(InterruptModeType modeValue) {
      this.mode = modeValue;
   }

   /**
    * @return Returns the modeValue.
    */
   public InterruptModeType getMode() {
      return mode;
   }

   public static InterruptMode createFromXmlAttributes(String interruptMode) {
      InterruptMode im = new InterruptMode();
      im.setMode(InterruptModeType.getValue(interruptMode));
      return im;
   }
}
