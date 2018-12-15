//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Interconnect Call Priority attribute specifies the priority to use for
 * the individual in interconnect calls. If no priority attribute is provided by
 * the Home RFSS, the serving RFSS MAY assign a priority to the unit in
 * accordance with local policy and programming.
 * 
 */
public class InterconnectCallPriority extends ServiceProfileLine {

   public static final String NAME = "u-icpri";
   public static final String SHORTNAME = "u-icp";
   public static int DEFAULTVALUE = 3; // Arbitrarily chosen

   private int priority;

   /**
    * Default constructor. Note that a priority value of 3 is arbitrarily
    * assigned
    * 
    */
   InterconnectCallPriority() {
      super(NAME, new Integer(DEFAULTVALUE).toString());
      this.priority = DEFAULTVALUE;
   }

   /**
    * Constructor -- supply the arg as a string
    * 
    * @param priorityVal --
    *            value of priority as a string.
    * @throws ParseException
    */
   InterconnectCallPriority(String priorityVal) throws ParseException {
      super(NAME, priorityVal);
      try {
         this.priority = Integer.parseInt(priorityVal);
         setPriority( priority);
      } catch (NumberFormatException ex) {
         throw new ParseException("Illegal Priority Value [" + priorityVal + "]", 0);
      }
   }

   /**
    * Constructor -- supply arg as an int.
    * 
    * @param priorityVal --
    *            priority value supplied as a valid int in the range 1 -- 10
    * @throws IllegalArgumentException
    */
   public InterconnectCallPriority(int priorityVal) throws IllegalArgumentException {
      super(NAME, new Integer(priorityVal).toString());
      setPriority( priorityVal);
   }

   /**
    * @param priority
    *            The priority to set.
    */
   public void setPriority(int priority) {
      if (priority > 10 || priority < 1) {
         throw new IllegalArgumentException("Illegal Priority Value [" + priority + "]");
      }
      this.priority = priority;
      super.setValue(priority);
   }

   /**
    * @return Returns the priority.
    */
   public int getPriority() {
      return priority;
   }
}
