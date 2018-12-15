//
package gov.nist.p25.issi.p25payload;

/**
 * An enumeration of priority types. The priority type is set to decimal 2 for
 * preemptive priority transmissions, 1 for priority transmissions, and 0 for
 * all other transmissions.
 */
public enum TransmitPriorityType {

   PREEMPTIVE_PRIORITY(2, "Preemptive Priority"), 
   PRIORITY(1, "Priority"), 
   NORMAL(0, "Normal");

   private static TransmitPriorityType[] tpTypes = { 
      NORMAL, 
      PRIORITY,
      PREEMPTIVE_PRIORITY
   };
   
   /** The payload type as an int. */
   private int type;

   /** A description of the payload type. */
   private String description;

   
   /**
    * Construct a transmit priority type.
    * 
    * @param type
    *            the transmit priority type as a byte.
    * @param description
    *            the description of this transmit priority type.
    */
   TransmitPriorityType(int type, String description) {
      this.description = description;
      this.type = type;
   }

   /**
    * Get the description of this transmit priority type.
    * 
    * @return the description of this transmit priority type.
    */
   public String toString() {   
      return description;
   }
   
   /**
    * Get the type from the given descriptive string.
    * 
    * @param description
    * @return the transmission priority type
    * @throws IllegalArgumentException
    */
   public static int valueFromString(String description) throws IllegalArgumentException {
      for ( TransmitPriorityType tp : tpTypes) {
         if (tp.description.equalsIgnoreCase(description))
            return tp.type;
      }
      throw new IllegalArgumentException(
                  "Illegal TransmitPriorityType description: " + description);
   }

   /**
    * Get this transmit priority type as an integer.
    * 
    * @return this transmit priority type as an integer.
    */
   public int intValue() {
      return type;
   }

   /**
    * Get this transmit priority type as a TransmitPriorityTypes object.
    * 
    * @param tpType
    *            the transmit priority type as an integer.
    * @return the transmit priority type as a TransmitPriorityTypes object.
    */
   public static TransmitPriorityType getInstance(int tpType) {
      return tpTypes[tpType];
   }
   public static TransmitPriorityType getInstance(String description) {
      return tpTypes[ valueFromString(description)];
   }
   
   /**
    * Determine the number of elements in this enumeration.
    *
    * @return this size of transmit priority type.
    */
   public static int size() {
      return tpTypes.length;
   }
}
