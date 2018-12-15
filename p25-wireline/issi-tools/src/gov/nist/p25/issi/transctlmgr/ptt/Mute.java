//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class implements Mute enumeration.
 */
enum Mute {
   
   UNMUTED(0, false, "UNMUTED"), 
   MUTED(1, true, "MUTED");
   
   /** The int value of the muted state. */
   private int mValue;
   
   /** The boolean value of the muted state. */
   private boolean isMuted;
   
   /** The description of the muted state. */
   private String description;
   
   /**
    * Construct a Mute object.
    * @param mValue The value of the muted object.
    * @param isMuted True if muted, false otherwise.
    * @param description The description of the muted state.
    */
   Mute(int mValue, boolean isMuted, String description) {
      this.mValue = mValue;
      this.isMuted = isMuted;   
      this.description = description;
   }
   
   /**
    * Get the muted int value.
    * @return The muted value: 1=muted, 0=unmuted.
    */
   int getMValue() {
      return mValue;
   }
   
   /** 
    * Get the muted boolean value.
    * @return true if muted, false otherwise.
    */
   boolean getBooleanValue() {
      return isMuted;
   }

   /** 
    * Get the description of the muted state.
    * @return The description of the muted state.
    */
   public String toString() {
      return description;
   }
}
