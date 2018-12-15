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
public class Test_ISSIHeaderWordTest extends TestCase {
   private static Logger logger = Logger.getLogger("gov.nist.p25.issi.p25payload");
   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Test creating block headers.
    */
   public void testISSIHeaderWord() {
      // Create block header 1 with empty constructor
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Creating ISSI Header Word 1");

      ISSIHeaderWord issiHeaderWord1 = new ISSIHeaderWord();
      byte[] messageIndicatorBytes1 = new byte[9];
      issiHeaderWord1.setMessageIndicator(messageIndicatorBytes1);
      issiHeaderWord1.setAlgId(ByteUtil.getMaxIntValueForNumBits(8));
      issiHeaderWord1.setKeyId(ByteUtil.getMaxIntValueForNumBits(16));
      issiHeaderWord1.setMFID(ByteUtil.getMaxIntValueForNumBits(8));
      issiHeaderWord1.setGroupId(ByteUtil.getMaxIntValueForNumBits(16));
      issiHeaderWord1.setNID(ByteUtil.getMaxIntValueForNumBits(16));
      issiHeaderWord1.setSF(ByteUtil.getMaxIntValueForNumBits(2));
      issiHeaderWord1.setVBB(ByteUtil.getMaxIntValueForNumBits(2));
      issiHeaderWord1.setReserved(ByteUtil.getMaxIntValueForNumBits(4));
      
      // Generate byte representation from block header 1
      byte[] issiHeaderWordBytes = issiHeaderWord1.getBytes();

      // Create block header 2 with block header 1 byte representation
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Creating ISSI Header Word 2");
         
      ISSIHeaderWord issiHeaderWord2 = new ISSIHeaderWord(issiHeaderWordBytes);

      // Test ISSI header word equivalence
      assertTrue(issiHeaderWord1.getMessageIndicator().length == 
         issiHeaderWord2.getMessageIndicator().length);
      assertTrue(issiHeaderWord1.getAlgId() == issiHeaderWord2.getAlgId());
      assertTrue(issiHeaderWord1.getKeyId() == issiHeaderWord2.getKeyId());
      assertTrue(issiHeaderWord1.getMFID() == issiHeaderWord2.getMFID());
      assertTrue(issiHeaderWord1.getGroupId() == issiHeaderWord2.getGroupId());
      assertTrue(issiHeaderWord1.getNID() == issiHeaderWord2.getNID());
      assertTrue(issiHeaderWord1.getSF() == issiHeaderWord2.getSF());
      assertTrue(issiHeaderWord1.getVBB() == issiHeaderWord2.getVBB());
      assertTrue(issiHeaderWord1.getReserved() == issiHeaderWord2.getReserved());
      
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("ISSI Header Word 1 = ISSI Header Word 2");
      
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.debug("DONE: All assertions passed.");
   }
}
