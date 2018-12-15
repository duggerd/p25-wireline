//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class implements ISSI link types.
 */
public enum LinkType {
   
   GROUP_SERVING( 2, "group serving"), 
   GROUP_HOME( 1, "group home"), 
   UNIT_TO_UNIT_CALLING_SERVING_TO_CALLING_HOME(
         16, "unit to unit calling serving to calling home"), 
   UNIT_TO_UNIT_CALLING_HOME_TO_CALLING_SERVING(
         14, "unit to unit calling home to calling serving"), 
   UNIT_TO_UNIT_CALLING_HOME_TO_CALLED_HOME(
         12, "unit to unit calling home to called home"), 
   UNIT_TO_UNIT_CALLED_HOME_TO_CALLING_HOME(
         11, "unit to unit called home to calling home"), 
   UNIT_TO_UNIT_CALLED_HOME_TO_CALLED_SERVING(
         13, "unit to unit called home to called serving"), 
   UNIT_TO_UNIT_CALLED_SERVING_TO_CALLED_HOME(
         15, "unit to unit called serving to called home");
   
   /** The type of link as defined in TIA-102.BACA Section 4.1. */
   private int type;
   
   /** Description of the link type. */
   private String description;
   
   /**
    * Construct a link type.
    *
    * @param type The link type as defined in TIA-102.BACA Section 4.1
    * @param description The description of the link type.
    */
   LinkType(int type, String description) {
      this.type = type;
      this.description = description;
   }
   
   /**
    * Get the type of this link.
    *
    * @return The type of this link.
    */
   public int getValue() {
      return type;
   }
   
   /**
    * Get a string representation of this link type.
    *
    * @return The string representation of this link type.
    */
   public String toString() {
      return description;
   }
}
