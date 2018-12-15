//
package gov.nist.p25.issi.p25payload;

import gov.nist.p25.issi.utils.ByteUtil;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

/**
 * This class tests setting and getting a PTT control word object.
 * 
 */
public class Test_PTTControlWordTest extends TestCase {

   protected static org.apache.log4j.Level LOGGER_LEVEL = org.apache.log4j.Level.DEBUG;

   /** The logger for this class. */
   private static Logger logger = Logger.getLogger("gov.nist.p25.issi.p25payload");
   static {
      PropertyConfigurator.configure("log4j.properties");
      // You can add more appenders here if you wish.
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Construct a PTT control word test.
    */
   public Test_PTTControlWordTest() {
   }

   /**
    * Set up this JUnit test.
    */
   public void setUp() {
   }

   /**
    * Test packing and unpacking a PTT control word block.
    */
   public void testPackUnpack() {
      /*******************************************************************
       * Create PTTControlWord #1
       ******************************************************************/
      PTTControlWord pttControlWord = new PTTControlWord();
      pttControlWord.setWacnId(ByteUtil.getMaxIntValueForNumBits(20));
      pttControlWord.setSystemId(ByteUtil.getMaxIntValueForNumBits(12));
      pttControlWord.setUnitId(ByteUtil.getMaxIntValueForNumBits(24));

      int pl = 1;
      int myTP = (TransmitPriorityType.NORMAL.intValue() + (pl << 4));
      pttControlWord.setTP(myTP);
      byte[] encodedPacket = pttControlWord.getBytes();

      // Test unpack
      PTTControlWord newPTTControlWord = new PTTControlWord(encodedPacket);

      /*******************************************************************
       * Create PTTControlWord #2
       ******************************************************************/
      assertTrue(newPTTControlWord.getWacnId() == ByteUtil.getMaxIntValueForNumBits(20));
      assertTrue(newPTTControlWord.getSystemId() == ByteUtil.getMaxIntValueForNumBits(12));
      assertTrue(newPTTControlWord.getUnitId() == ByteUtil.getMaxIntValueForNumBits(24));

      int tpInt = newPTTControlWord.getTP();
      logger.debug("getTP()="+tpInt);
      assertTrue(newPTTControlWord.getTP() == myTP);

      // Try getPT
      int ptInt = newPTTControlWord.getPT();
      if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
         logger.debug("PT = " + ptInt + ": "
               + ByteUtil.writeBytes(ByteUtil.intToBytes(ptInt)));

      // Try getPL
      int plInt = newPTTControlWord.getPL();

      if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
         logger.debug("PL = " + plInt + ": "
               + ByteUtil.writeBytes(ByteUtil.intToBytes(plInt)));

      // Now change values of PT and PL
      ptInt = TransmitPriorityType.PREEMPTIVE_PRIORITY.intValue();
      plInt = 9; // some random decimal value
      newPTTControlWord.setPTAndPL(ptInt, plInt);

      assertTrue(newPTTControlWord.getPL() == plInt);
      assertTrue(newPTTControlWord.getPT() == ptInt);
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.debug("DONE: All assertions passed.");
   }
}
