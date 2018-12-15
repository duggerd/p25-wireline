//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Emergency Capable (g-ecap). The Emergency Capable attribute defines whether
 * or not emergency calls can be made on the group. If the emergency capable
 * attribute is not provided, emergency calls are allowed from the serving RFSS.
 * 
 */
public class EmergencyCapable extends ServiceProfileLine {

   public static final String NAME = "g-ecap";
   public static final String SHORTNAME = "g-ec";
   public static final boolean DEFAULTVALUE = true;

   private boolean emergencyCapable = DEFAULTVALUE;

   EmergencyCapable() {
      super(NAME, "1");
      emergencyCapable = DEFAULTVALUE;
   }

   EmergencyCapable(String ecapString) throws ParseException {
      super(NAME, ecapString);
      //if (ecapString == null)
      //   throw new NullPointerException("Null input arg.");
      this.emergencyCapable = super.getBoolFromString(ecapString);
   }
   
   public static EmergencyCapable createFromXmlAttribute(String value) {
      EmergencyCapable retval = new EmergencyCapable();
      retval.setEmergencyCapable( "EmergencyCallsAllowed".equals(value));
      return retval;
   }

   /**
    * @param emergencyCapable
    *            The emergencyCapable to set.
    */
   public void setEmergencyCapable(boolean emergencyCapable) {
      this.emergencyCapable = emergencyCapable;
      super.setValue(emergencyCapable);
   }

   /**
    * @return Returns the emergencyCapable.
    */
   public boolean isEmergencyCapable() {
      return emergencyCapable;
   }
}
