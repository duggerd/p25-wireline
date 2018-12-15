//
package gov.nist.p25.issi.rfss;

/**
 * The return type for the query function that tells what role a given rfss
 * serves given its SIP Signaling message.
 * 
 */
public class RfssRole {

   private boolean callingHome = false;
   private boolean calledHome = false;
   private boolean callingServing = false;
   private boolean calledServing = false;

   /**
    * @param callingHome
    *            The callingHome to set.
    */
   public void setCallingHome(boolean callingHome) {
      this.callingHome = callingHome;
   }

   /**
    * @return Returns the callingHome.
    */
   public boolean isCallingHome() {
      return callingHome;
   }

   /**
    * @param calledHome
    *            The calledHome to set.
    */
   public void setCalledHome(boolean calledHome) {
      this.calledHome = calledHome;
   }

   /**
    * @return Returns the calledHome.
    */
   public boolean isCalledHome() {
      return calledHome;
   }

   /**
    * @param callingServing
    *            The callingServing to set.
    */
   public void setCallingServing(boolean callingServing) {
      this.callingServing = callingServing;
   }

   /**
    * @return Returns the callingServing.
    */
   public boolean isCallingServing() {
      return callingServing;
   }

   /**
    * @param calledServing
    *            The calledServing to set.
    */
   public void setCalledServing(boolean calledServing) {
      this.calledServing = calledServing;
   }

   /**
    * @return Returns the calledServing.
    */
   public boolean isCalledServing() {
      return calledServing;
   }
}
