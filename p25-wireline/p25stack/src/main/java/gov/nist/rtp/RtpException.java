//
package gov.nist.rtp;

/**
 * This class implements an RTP exception.
 * 
 */
public class RtpException extends Exception {

   private static final long serialVersionUID = -1L;

   /** Constant that identifies an out-of-range method parameter. */
   public final static String OUT_OF_RANGE = "Method parameter value is out of range.";

   /**
    * Construct an RTP exception.
    * 
    * @param exceptionType
    *            the type of RTP exception.
    */
   public RtpException(String exceptionType) {
      super(exceptionType);
   }

   /**
    * Construct an RTP exception.
    * 
    * @param error
    *            the error message for this exception
    * @param wrappedException
    *            the exception object
    */
   public RtpException(String error, Exception wrappedException) {
      super(error, wrappedException);
   }
}
