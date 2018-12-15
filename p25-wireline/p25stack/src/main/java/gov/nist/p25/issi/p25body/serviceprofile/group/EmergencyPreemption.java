//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Emergency Preemption
 * 
 */
public class EmergencyPreemption extends ServiceProfileLine {

   public static final String NAME = "g-eprempt";
   public static final String SHORTNAME = "g-ep";
   public static final boolean DEFAULTVALUE = false;

   private boolean ruthlessOnEmergency = DEFAULTVALUE;

   EmergencyPreemption() {
      super(NAME, DEFAULTVALUE);
      this.ruthlessOnEmergency = DEFAULTVALUE;
   }

   EmergencyPreemption(String ruthlessFlag) throws ParseException {
      super(NAME, ruthlessFlag);
      if ("1".equals(ruthlessFlag))
         ruthlessOnEmergency = true;
      else if ("0".equals(ruthlessFlag))
         ruthlessOnEmergency = false;
      else
         throw new ParseException("Invalid flag " + ruthlessFlag, 0);
   }

   /**
    * @param ruthlessOnEmergency
    *            The ruthlessPreemptionAppliedOnEmergencyCalls to set.
    */
   public void setRuthlessOnEmergency(boolean ruthlessOnEmergency) {
      this.ruthlessOnEmergency = ruthlessOnEmergency;
      super.setValue(ruthlessOnEmergency);
   }

   /**
    * @return Returns the ruthlessPreemptionAppliedOnEmergencyCalls flag.
    */
   public boolean isRuthlessOnEmergency() {
      return ruthlessOnEmergency;
   }

   public static EmergencyPreemption createFromXmlAttribute(String attr) {
      EmergencyPreemption retval = new EmergencyPreemption();
      if ( "NonRuthless".equals(attr)) {
         retval.setRuthlessOnEmergency(false);
      } else {
         retval.setRuthlessOnEmergency(true);
      }
      return retval;
   }
}
