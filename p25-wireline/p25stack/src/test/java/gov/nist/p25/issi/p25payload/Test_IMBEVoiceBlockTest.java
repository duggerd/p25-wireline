//
package gov.nist.p25.issi.p25payload;

import gov.nist.p25.issi.utils.ByteUtil;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

/**
 * This class tests P25 block headers.
 */
public class Test_IMBEVoiceBlockTest extends TestCase {
   private static Logger logger = Logger.getLogger("gov.nist.p25.issi.p25payload");
   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Test creating IMBE voice block.
    */
   public void testIMBEVoiceBlock() {

      // Create block header 1 with empty constructor
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Creating IMBE voice block 1");

      IMBEVoiceBlock imbeVoiceBlock1 = new IMBEVoiceBlock();
      imbeVoiceBlock1.setFT(ByteUtil.getMaxIntValueForNumBits(8));
      imbeVoiceBlock1.setU0(ByteUtil.getMaxIntValueForNumBits(12));
      imbeVoiceBlock1.setU1(ByteUtil.getMaxIntValueForNumBits(12));
      imbeVoiceBlock1.setU2(ByteUtil.getMaxIntValueForNumBits(12));
      imbeVoiceBlock1.setU3(ByteUtil.getMaxIntValueForNumBits(12));   
      imbeVoiceBlock1.setU4(ByteUtil.getMaxIntValueForNumBits(11));
      imbeVoiceBlock1.setU5(ByteUtil.getMaxIntValueForNumBits(11));
      imbeVoiceBlock1.setU6(ByteUtil.getMaxIntValueForNumBits(11));   
      imbeVoiceBlock1.setU7(ByteUtil.getMaxIntValueForNumBits(7));   
      imbeVoiceBlock1.setEt(ByteUtil.getMaxIntValueForNumBits(3));
      imbeVoiceBlock1.setEr(ByteUtil.getMaxIntValueForNumBits(3));
      imbeVoiceBlock1.setM(ByteUtil.getMaxIntValueForNumBits(1));
      imbeVoiceBlock1.setL(ByteUtil.getMaxIntValueForNumBits(1));
      imbeVoiceBlock1.setE4(ByteUtil.getMaxIntValueForNumBits(1));
      imbeVoiceBlock1.setE1(ByteUtil.getMaxIntValueForNumBits(3));
      imbeVoiceBlock1.setSF(ByteUtil.getMaxIntValueForNumBits(2));
      imbeVoiceBlock1.setReserved(ByteUtil.getMaxIntValueForNumBits(2));
      byte[] additionalFrameData1 = new byte[3];
      imbeVoiceBlock1.setAdditionalFrameData(additionalFrameData1);
      
      // Generate byte representation from block header 1
      byte[] imbeVoiceBlockBytes = imbeVoiceBlock1.getBytes();
         
      // Create block header 2 with block header 1 byte representation
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("Creating control octet 2");
         
      IMBEVoiceBlock imbeVoiceBlock2 = new IMBEVoiceBlock(imbeVoiceBlockBytes);

      // Test block header equivalence
      assertTrue(imbeVoiceBlock1.getFT() == imbeVoiceBlock2.getFT());
      assertTrue(imbeVoiceBlock1.getU0() == imbeVoiceBlock2.getU0());
      assertTrue(imbeVoiceBlock1.getU1() == imbeVoiceBlock2.getU1());
      assertTrue(imbeVoiceBlock1.getU2() == imbeVoiceBlock2.getU2());
      assertTrue(imbeVoiceBlock1.getU3() == imbeVoiceBlock2.getU3());
      assertTrue(imbeVoiceBlock1.getU4() == imbeVoiceBlock2.getU4());
      assertTrue(imbeVoiceBlock1.getU5() == imbeVoiceBlock2.getU5());
      assertTrue(imbeVoiceBlock1.getU6() == imbeVoiceBlock2.getU6());
      assertTrue(imbeVoiceBlock1.getU7() == imbeVoiceBlock2.getU7());
      assertTrue(imbeVoiceBlock1.getEt() == imbeVoiceBlock2.getEt());
      assertTrue(imbeVoiceBlock1.getEr() == imbeVoiceBlock2.getEr());
      assertTrue(imbeVoiceBlock1.getM() == imbeVoiceBlock2.getM());
      assertTrue(imbeVoiceBlock1.getL() == imbeVoiceBlock2.getL());
      assertTrue(imbeVoiceBlock1.getE4() == imbeVoiceBlock2.getE4());
      assertTrue(imbeVoiceBlock1.getE1() == imbeVoiceBlock2.getE1());
      assertTrue(imbeVoiceBlock1.getSF() == imbeVoiceBlock2.getSF());
      assertTrue(imbeVoiceBlock1.getReserved() == imbeVoiceBlock2.getReserved());
      assertTrue(imbeVoiceBlock1.getAdditionalFrameData().length == 
         imbeVoiceBlock2.getAdditionalFrameData().length);

      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("IMBE voice block 1 = IMBE voice block 2");
      
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.debug("DONE: All assertions passed.");
   }

   //==============================================================================
   public static void main(String args[]) {
      TestSuite suite= new TestSuite( Test_IMBEVoiceBlockTest.class);
      junit.textui.TestRunner.run( suite);
   }
}
