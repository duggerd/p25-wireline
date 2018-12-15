//
package gov.nist.p25.issi.p25payload;

/**
 * An enumeration of Console Transmit Request Priority.
 * 
 */
public enum ConsoleTransmissionRequestPriority {

   PRIORITY_1(0, "10", "Priority 1"),
   PRIORITY_2(1, "30", "Priority 2"), 
   PRIORITY_3(2, "50", "Priority 3"), 
   PRIORITY_4(3, "70", "Priority 4"), 
   PRIORITY_5(4, "90", "Priority 5"), 
   PRIORITY_6(5, "B0", "Priority 6"), 
   PRIORITY_7(6, "D0", "Priority 7"), 
   PRIORITY_8(7, "F0", "Priority 8"); 

   private static ConsoleTransmissionRequestPriority[] tpTypes = { 
         PRIORITY_1, 
         PRIORITY_2,
         PRIORITY_3,
         PRIORITY_4,
         PRIORITY_5,
         PRIORITY_6,
         PRIORITY_7,
         PRIORITY_8
   };
   
   private int type;
   private String hexString;
   private String description;

   
   /**
    * Construct a transmit priority type.
    * 
    * @param type
    *            the transmit priority type as a byte.
    * @param description
    *            the description of this transmit priority type.
    */
   ConsoleTransmissionRequestPriority(int type, String hexString, String description) {
      this.type = type;
      this.hexString = hexString;
      this.description = description;
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
    * @return the type
    * @throws IllegalArgumentException
    */
   public static int valueFromString(String description)
      throws IllegalArgumentException
   {
      for ( ConsoleTransmissionRequestPriority tp: tpTypes) {
         if (tp.description.equals(description)) {
            return tp.type;
         }
      }
      throw new IllegalArgumentException("Illegal ConsoleTransmissionRequestPriority description: " + description);
   }
   public static int valueFromHexString(String hexString)
      throws IllegalArgumentException
   {
      for ( ConsoleTransmissionRequestPriority tp: tpTypes) {
         if (tp.hexString.equals(hexString)) {
            return tp.type;
         }
      }
      throw new IllegalArgumentException("Illegal ConsoleTransmissionRequestPriority hexString: " + hexString);
   }


   /**
    * Get this transmit priority type as an integer.
    * 
    * @return this transmit priority as an integer.
    */
   public int intValue() {
      return type;
   }
   public String hexStringValue() {
      return hexString;
   }

   /**
    * Get this transmit priority as a ConsoleTransmissionRequestPrioritys object.
    * 
    * @param tpType
    *            the transmit priority type as an integer.
    * @return transmit priority as a ConsoleTransmissionRequestPrioritys object.
    */
   public static ConsoleTransmissionRequestPriority getInstance(int tpType) {
      return tpTypes[tpType];
   }
   public static ConsoleTransmissionRequestPriority getInstance(String hexString)
      throws IllegalArgumentException
   {
      return tpTypes[valueFromHexString(hexString)];
   }
   
   /**
    * Determine the number of elements in this enumeration.
    *
    * @return size of transmit priority.
    */
   public static int size() {
      return tpTypes.length;
   }
}
