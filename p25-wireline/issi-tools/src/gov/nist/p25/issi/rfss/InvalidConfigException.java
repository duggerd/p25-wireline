//
package gov.nist.p25.issi.rfss;

/**
 * This class encapsulates the invalid configuration exception.
 */
public class InvalidConfigException extends Exception {

   private static final long serialVersionUID = -1L;

   public InvalidConfigException(String exception) {
      super(exception);
   }
}
