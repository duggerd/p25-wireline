//
package gov.nist.p25.issi.p25payload;

/**
 * An enumeration of Transmission Descriptor
 */
public enum TransmissionDescriptor {

   TRUNKING_TRANSMISSION(0, "00", "Trunking transmission"),
   CONVENTIONAL_TRANSMISSION(1, "01", "Conventional transmission"); 

   private static TransmissionDescriptor[] tpTypes = { 
      TRUNKING_TRANSMISSION, 
      CONVENTIONAL_TRANSMISSION,
   };
   
   private int type;
   private String hexString;
   private String description;
   
   /**
    * Construct a transmission descriptor.
    * 
    * @param type
    *            the transmission type.
    * @param description
    *            the description of this transmission type.
    */
   TransmissionDescriptor(int type, String hexString, String description) {
      this.type = type;
      this.hexString = hexString;
      this.description = description;
   }

   /**
    * Get the description of this transmission type.
    * 
    * @return the description of this transmission type.
    */
   public String toString() {   
      return description;
   }
   
   /**
    * Get the transmission type from the given descriptive string.
    * 
    * @param description
    * @return the transmission type as an integer
    * @throws IllegalArgumentException
    */
   public static int valueFromString(String description)
      throws IllegalArgumentException
   {
      for ( TransmissionDescriptor tp: tpTypes) {
         if (tp.description.equals(description)) {
            return tp.type;
         }
      }
      throw new IllegalArgumentException(
               "Illegal TransmissionDescriptor description: " + description);
   }

   /**
    * Get the transmission type from the given hex string.
    *
    * @param hexString
    * @return the transmission type as an integer
    * @throws IllegalArgumentException
    */
   public static int valueFromHexString(String hexString)
      throws IllegalArgumentException
   {
      for ( TransmissionDescriptor tp: tpTypes) {
         if (tp.hexString.equals(hexString)) {
            return tp.type;
         }
      }
      throw new IllegalArgumentException(
               "Illegal TransmissionDescriptor hexString: " + hexString);
   }

   /**
    * Get this transmission type as an integer.
    * 
    * @return this transmission type as an integer.
    */
   public int intValue() {
      return type;
   }
   public String hexStringValue() {
      return hexString;
   }

   /**
    * Get this transmission type as a TransmissionDescriptors object.
    * 
    * @param tpType
    *            the transmission type as an integer.
    * @return transmission type as a TransmissionDescriptors object.
    */
   public static TransmissionDescriptor getInstance(int tpType) {
      return tpTypes[tpType];
   }
   public static TransmissionDescriptor getInstance(String hexString)
      throws IllegalArgumentException
   {
      return tpTypes[valueFromHexString(hexString)];
   }
   
   /**
    * Determine the number of elements in this enumeration.
    *
    * @return size of transmission type.
    */
   public static int size() {
      return tpTypes.length;
   }
}
