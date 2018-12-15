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
public class Test_ManufacturerSpecificTest extends TestCase {
   private static Logger logger = Logger.getLogger("gov.nist.p25.issi.p25payload");
   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Test creating ISSI Packet Type.
    */
   public void test() {
      // Create block header 1 with empty constructor
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Creating Mfr Specific Block 1");

      ManufacturerSpecific mfrSpecific1 = new ManufacturerSpecific();
      mfrSpecific1.setMFID(ByteUtil.getMaxIntValueForNumBits(8));
      mfrSpecific1.setLength(ByteUtil.getMaxIntValueForNumBits(8));
      byte[] variableLengthData = new byte[ByteUtil.getMaxIntValueForNumBits(8)];
      mfrSpecific1.setManufacturerData(variableLengthData);
      
      // Generate byte representation from block header 1
      byte[] mfrSpecificBytes = mfrSpecific1.getBytes();
         
      // Create block header 2 with block header 1 byte representation
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Creating Mfr Specific Block 2");
         
      ManufacturerSpecific mfrSpecific2 = new ManufacturerSpecific(mfrSpecificBytes);

      // Test ISSI packet type equivalence
      assertTrue(mfrSpecific1.getMFID() == mfrSpecific2.getMFID());
      assertTrue(mfrSpecific1.getLength() == mfrSpecific2.getLength());
      assertTrue(mfrSpecific1.getManufacturerData().length == 
         mfrSpecific2.getManufacturerData().length);
      
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Mfr Specific Block 1 = Mfr Specific Block 2");
      
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.debug("DONE: All assertions passed.");
   }
}
