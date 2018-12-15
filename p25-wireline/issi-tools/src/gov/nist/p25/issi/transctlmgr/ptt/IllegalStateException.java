//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class handles illegal MMF or SMF state transitions.
 */
public class IllegalStateException extends Exception {
   private static final long serialVersionUID = -1L;

   /**
    * Construct an illegal state exception.
    * @param exception The exception description.
    */
   public IllegalStateException(String exception) {
      super(exception);
   }
}
