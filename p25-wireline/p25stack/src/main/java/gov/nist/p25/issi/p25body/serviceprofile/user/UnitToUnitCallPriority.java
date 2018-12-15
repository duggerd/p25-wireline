//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * Unit to Unit Call Priority
 * 
 */
public class UnitToUnitCallPriority extends ServiceProfileLine {

   public static final String NAME = "u-upri";
   public static final String SHORTNAME = "u-p";

   // Arbitrarily chosen can be set by RFSS
   private static int defaultValue = 3;

   private int priority;

   /**
    * Set the default value.
    * 
    * @param xdefaultValue  The default value
    */
   public static void setDefaultValue(int xdefaultValue) {
      if (xdefaultValue < 1 || xdefaultValue > 10) {
         throw new IllegalArgumentException("Bad default value ["
               + xdefaultValue + "]");
      }
      defaultValue = xdefaultValue;
   }

   /**
    * Get the default value.
    */
   public static int getDefaultValue() {
      return defaultValue;
   }

   /**
    * Default constructor. Note that a priority value of 3 is arbitrarily
    * assigned.
    */
   UnitToUnitCallPriority() {
      super(NAME, new Integer(defaultValue).toString());
      this.priority = defaultValue;
   }

   /**
    * Constructor -- supply the arg as a string
    * 
    * @param priorityVal --
    *            The value of priority as a string.
    * @throws ParseException
    */
   UnitToUnitCallPriority(String priorityVal) throws ParseException {
      super(NAME, priorityVal);
      try {
         this.priority = Integer.parseInt(priorityVal);
         setPriority( priority);
      } catch (NumberFormatException ex) {
         throw new ParseException("Illegal Priority Value [" + priorityVal
               + "]", 0);
      }
   }

   /**
    * Constructor -- supply arg as an int.
    * 
    * @param priorityVal --
    *            priority value supplied as a valid int in the range 1 -- 10
    * @throws IllegalArgumentException
    */
   public UnitToUnitCallPriority(int priorityVal) {
      super(NAME, new Integer(priorityVal).toString());
      setPriority( priorityVal);
   }

   /**
    * Set the priority
    *
    * @param priorityVal
    *            The priority to set.
    */
   public void setPriority(int priorityVal) {
      if (priorityVal > 10 || priorityVal < 1) {
         throw new IllegalArgumentException("Illegal Priority Value ["
               + priorityVal + "]");
      }
      this.priority = priorityVal;
      super.setValue(priority);
   }

   /**
    * Get the priority.
    *
    * @return Returns the priority.
    */
   public int getPriority() {
      return priority;
   }
}
