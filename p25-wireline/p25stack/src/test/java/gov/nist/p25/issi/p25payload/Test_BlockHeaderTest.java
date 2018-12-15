//
package gov.nist.p25.issi.p25payload;

import gov.nist.p25.issi.utils.ByteUtil;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

/**
 * This class tests P25 block headers.
 */
public class Test_BlockHeaderTest extends TestCase {
   private static Logger logger = Logger.getLogger("gov.nist.p25.issi.p25payload");
   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Test creating block headers.
    */
   public void testControlOctet() {
      // Create block header 1 with empty constructor
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Creating control octet 1");

      ControlOctet controlOctet1 = new ControlOctet();
      controlOctet1.setS(ByteUtil.getMaxIntValueForNumBits(1));
      controlOctet1.setC(ByteUtil.getMaxIntValueForNumBits(1));
      controlOctet1.setBHC(ByteUtil.getMaxIntValueForNumBits(6));
         
      // Generate byte representation from block header 1
      byte[] controlOctetBytes = controlOctet1.getBytes();
         
      // Create block header 2 with block header 1 byte representation
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Creating control octet 2");
         
      ControlOctet controlOctet2 = new ControlOctet(controlOctetBytes[0]);

      // Test block header equivalence
      assertTrue(controlOctet1.getS() == controlOctet2.getS());
      assertTrue(controlOctet1.getC() == controlOctet2.getC());
      assertTrue(controlOctet1.getBHC() == controlOctet2.getBHC());

      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Control octet 1 = control octet 2");
      
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.debug("DONE: All assertions passed.");
   }
}
