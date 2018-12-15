//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class implements an PTT exception.
 */
public class PttException extends Exception {

   private static final long serialVersionUID = -1L;
   
   /**
    * Construct an PTT exception.
    * @param The exception description.
    */
   public PttException(String exceptionType) {
      super(exceptionType);
   }
}
