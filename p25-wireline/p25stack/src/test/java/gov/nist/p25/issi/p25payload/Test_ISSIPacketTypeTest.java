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
public class Test_ISSIPacketTypeTest extends TestCase {
   private static Logger logger = Logger.getLogger("gov.nist.p25.issi.p25payload");
   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Test creating ISSI Packet Type.
    */
   public void testISSIPacketType() {
      // Create block header 1 with empty constructor
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Creating ISSI Packet Type 1");

      ISSIPacketType issiPacketType1 = new ISSIPacketType();
      issiPacketType1.setM(ByteUtil.getMaxIntValueForNumBits(1));
      issiPacketType1.setPT(ByteUtil.getMaxIntValueForNumBits(7));
      issiPacketType1.setSO(128);      // Emergency call
      issiPacketType1.setTranssmissionSequenceNumber(ByteUtil.getMaxIntValueForNumBits(7));
      issiPacketType1.setL(ByteUtil.getMaxIntValueForNumBits(1));
      issiPacketType1.setInterval(ByteUtil.getMaxIntValueForNumBits(8));
      
      // Generate byte representation from block header 1
      byte[] issiPacketTypeBytes = issiPacketType1.getBytes();

      // Create block header 2 with block header 1 byte representation
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Creating ISSI Packet Type 2");
         
      ISSIPacketType issiPacketType2 = new ISSIPacketType(issiPacketTypeBytes);

      // Test ISSI packet type equivalence
      assertTrue(issiPacketType1.getM() == issiPacketType2.getM());
      assertTrue(issiPacketType1.getPT() == issiPacketType2.getPT());
      assertTrue(issiPacketType1.getServiceOptions() == issiPacketType2.getServiceOptions());
      assertTrue(issiPacketType1.getTransmissionSequenceNumber() == issiPacketType2.getTransmissionSequenceNumber());
      assertTrue(issiPacketType1.getL() == issiPacketType2.getL());
      assertTrue(issiPacketType1.getInterval() == issiPacketType2.getInterval());
      
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("ISSI Packet Type 1 = ISSI Packet Type 2");
      
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.debug("DONE: All assertions passed.");
   }
}
