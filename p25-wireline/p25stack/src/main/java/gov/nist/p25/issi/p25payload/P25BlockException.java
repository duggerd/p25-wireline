//
package gov.nist.p25.issi.p25payload;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

/**
 * This class implements an P25 block exception.
 * 
 */
public class P25BlockException extends Exception {
   
   public static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(P25BlockException.class);


   /** Constant that identifies an incomplete P25 payload. */
   protected final static String INCOMPLETE = "P25 packet is not complete.";

   /** Constant that identifies an incomplete P25 payload. */
   protected final static String INCONSISTENT_BLOCK_COUNT = "Number of block headers in control octect do not match number of block headers in P25 payload.";

   /** Constant that identifies an out-of-range numeric parameter. */
   public final static String OUT_OF_RANGE = "Parameter value out of range.";

   /** Constant that identifies an incorrect length for a byte array. */
   public final static String INCORRECT_BYTE_LENGTH = "Length of byte array is incorrect.";

   static {
      PropertyConfigurator.configure("log4j.properties");
      // You can add more appenders here if you wish.
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Construct a P25 block exception.
    * 
    * @param exceptionType
    *            the type of P25 block exception.
    */
   public P25BlockException(String exceptionType) {
      super(exceptionType);
   }

   /**
    * Construct a P25 block exception.
    * 
    * @param error
    *            the error message for this exception
    * @param wrappedException
    *            the exception object
    */
   public P25BlockException(String error, Exception wrappedException) {
      super(error, wrappedException);
   }
}
