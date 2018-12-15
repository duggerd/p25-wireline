//
package gov.nist.p25.issi.p25payload;

/**
 * Block header payload type. Each block header SHALL contain a bit (E) to
 * identify whether the payload type is a standard IANA type (E set to 0) or a
 * profile specific type (E set to 1). For voice services over the ISSI, the E
 * bit SHALL always be set to 1 (profile specific type).
 */
public enum PayloadType {

   IANA_TYPE(0, "IANA type"), 
   PROFILE_SPECIFIC_TYPE(1, "P25 profile specific type");

   /** The payload type as an int. */
   private int type;

   /** A description of the payload type. */
   private String description;
   
   /**
    * Construct a payload type.
    * 
    * @param type
    *            the payload type in a byte representation
    * @param description
    *            a description of the payload type
    */
   PayloadType(int type, String description) {
      this.type = type;
      this.description = description;
   }

   /**
    * Get this payload type as an int.
    * 
    * @return this payload type as an int.
    */
   public int intValue() {
      return type;
   }

   /**
    * Get the description of this payload type.
    * 
    * @return the description of this payload type.
    */
   public String toString() {
      return description;
   }

   /**
    * Get payload type as a PayloadType object.
    * 
    * @param ptype
    *            the payload type as a byte.
    * @return the payload type as a PayloadTypes object.
    */
   public static PayloadType getInstance(int ptype) {
      if (ptype != 0 && ptype != 1) {
         throw new IllegalArgumentException("Invalid PayloadType[0|1]: ptype="+ptype);
      }
      return (ptype == IANA_TYPE.type ? IANA_TYPE : PROFILE_SPECIFIC_TYPE);
   }

   /**
    * Get the appropriate type from a description.
    */
   public static int getValueFromDescription(String description) {
      if ( IANA_TYPE.description.equals(description)) {
         return IANA_TYPE.type;
      } else if (PROFILE_SPECIFIC_TYPE.description.equals(description)) {
         return PROFILE_SPECIFIC_TYPE.type;
      } else  {
         throw new IllegalArgumentException("Invalid PayloadType description:" + description);
      }
   }
}
