//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

import java.text.ParseException;

/**
 * The Priority attribute defines the suggested priority of the group when
 * queuing group calls on a busy system. The serving RFSS MAY independently
 * determine the relative priorities of competing groups homed to different
 * systems. However, it SHALL maintain the relative priorities provisioned by
 * the Home RFSS for competing calls homed to the same system. The priority
 * attribute has the following format, in ABNF notation:
 * 
 * <pre>
 * 
 *  group-priority =   g-pri: / g-p:  priority CRLF 
 *  priority = 1 /  2 / 3 / 4 / 5 / 6 / 7 / 8 / 9 / 10
 *  
 * </pre>
 * 
 * where the priority is restricted to the integer values 1 through 10. Group
 * calls with higher priority values are to be de-queued before group calls with
 * lower priority values. 1 is the lowest priority value. If no priority
 * attribute is provided by the Home RFSS, the serving RFSS MAY assign a
 * priority to the group in accordance with local policy and programming.
 * 
 */
public class GroupPriority extends ServiceProfileLine {

   public static final String NAME = "g-pri";
   public static final String SHORTNAME = "g-p";
   public static final int DEFAULTVALUE = 3; // Arbitrarily chosen

   private int priority = DEFAULTVALUE;

   /**
    * Default constructor. Note that a priority value of 3 is arbitrarily
    * assigned
    * 
    */
   GroupPriority() {
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
   GroupPriority(String priorityVal) throws ParseException {
      super(NAME, priorityVal);
      try {
         this.priority = Integer.parseInt(priorityVal);
         setPriority( priority);
      } catch (NumberFormatException ex) {
         throw new ParseException("Illegal Priority Value [" + priorityVal + "]", 0);
      }
      /**
      if (this.priority > 10 || this.priority < 1) {
         throw new ParseException("Illegal Priority Value [" + priorityVal + "]", 0);
      }
       */
   }

   /**
    * Constructor -- supply arg as an int.
    * 
    * @param priorityVal --
    *            priority value supplied as a valid int in the range 1 -- 10
    * @throws IllegalArgumentException
    */
   GroupPriority(int priority) throws IllegalArgumentException {
      super(NAME, priority);
      setPriority( priority);
      /**
      if (priority > 10 || priority < 1) {
         throw new IllegalArgumentException("Illegal priority value [" + priority + "]");
      }
      this.priority = priority;
       */
   }

   /**
    * @param priority
    *            The priority to set.
    */
   public void setPriority(int priority) throws IllegalArgumentException {
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
