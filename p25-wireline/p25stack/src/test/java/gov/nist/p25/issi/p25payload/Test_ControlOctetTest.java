//
package gov.nist.p25.issi.p25payload;

import gov.nist.p25.issi.utils.ByteUtil;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

/**
 * This class tests P25 control octets.
 */
public class Test_ControlOctetTest extends TestCase {
   protected static org.apache.log4j.Level LOGGER_LEVEL = org.apache.log4j.Level.DEBUG;

   /** The logger for this class. */
   private static Logger logger = Logger.getLogger("gov.nist.p25.issi.p25payload");
   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Contruct a control octet JUnit test.
    */
   public Test_ControlOctetTest() {
   }

   /**
    * Test creating block headers.
    */
   public void testBlockHeader() {
      try {
         // Create block header 1 with empty constructor
         if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
            logger.info("Creating block header 1");

         BlockHeader blockHeader1 = new BlockHeader();
         blockHeader1.setE( ByteUtil.getMaxIntValueForNumBits(1));
         blockHeader1.setBT( 1);
         blockHeader1.setBlockLength( ByteUtil.getMaxIntValueForNumBits(10));
         blockHeader1.setTimeStampOffset( ByteUtil.getMaxIntValueForNumBits(14));
         
         // Generate byte representation from block header 1
         byte[] blockHeaderBytes = blockHeader1.getBytes();
         
         // Create block header 2 with block header 1 byte representation
         if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
            logger.info("Creating block header 2");
         
         BlockHeader blockHeader2 = new BlockHeader(blockHeaderBytes);

         // Test block header equivalence
         assertTrue(blockHeader1.getE() == blockHeader2.getE());
         assertTrue(blockHeader1.getBT() == blockHeader2.getBT());
         assertTrue(blockHeader1.getTimeStampOffset() == blockHeader2.getTimeStampOffset());
         assertTrue(blockHeader1.getBlockLength() == blockHeader2.getBlockLength());
         if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
            logger.info("Block header 1 = block header 2");
         
      } catch (P25BlockException pbe) {
         pbe.printStackTrace();
         fail("unexpected exception ");
      }
      
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.debug("DONE: All assertions passed.");
   }
}
