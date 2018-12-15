//
package gov.nist.p25.issi.p25payload;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

import gov.nist.p25.issi.utils.ByteUtil;

/**
 * This class tests IMBE Voice block data generator.
 */
public class Test_IMBEVoiceDataGeneratorTest extends TestCase {
   private static Logger logger = Logger.getLogger("gov.nist.p25.issi.p25payload");
   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Test IMBE voice block fields.  Note that we do not test IMBE Voice
    * Block packing and unpacking; that should exist as a separate test.
    * This tests only getIMBEVoiceData(String filePath).
    */
   public void testIMBEDataGenerator() {
      System.out.println("testIMBEDataGenerator: ...");
      List<IMBEVoiceBlock> imbeVoiceBlocks = 
         IMBEVoiceDataGenerator.getIMBEVoiceData("voice/imbe-hex-test.txt");

      int superFrameIndex = 0;
      int imbePacketIndex = 0;
      for (IMBEVoiceBlock imbeVoice : imbeVoiceBlocks) {
         imbePacketIndex++;
         System.out.println("testIMBEDataGenerator: packetIndex="+imbePacketIndex);
         
         // Check if FT is in possible range (we don't check specific val)
         int frameType = imbeVoice.getFT();
         assertTrue((frameType >= 0x6A) && (frameType <= 0xCC));
         
         assertTrue(imbeVoice.getU0() <= ByteUtil.getMaxIntValueForNumBits(12));
         assertTrue(imbeVoice.getU1() <= ByteUtil.getMaxIntValueForNumBits(12));
         assertTrue(imbeVoice.getU2() <= ByteUtil.getMaxIntValueForNumBits(12));
         assertTrue(imbeVoice.getU3() <= ByteUtil.getMaxIntValueForNumBits(12));
         assertTrue(imbeVoice.getU4() <= ByteUtil.getMaxIntValueForNumBits(11));
         assertTrue(imbeVoice.getU5() <= ByteUtil.getMaxIntValueForNumBits(11));
         assertTrue(imbeVoice.getU6() <= ByteUtil.getMaxIntValueForNumBits(11));
         assertTrue(imbeVoice.getU7() <= ByteUtil.getMaxIntValueForNumBits(7));
         
         // Verify that these block types have 3 bytes of additional frame data.
         if (frameType == IMBEVoiceBlock.IMBEFRAME9
               || frameType == IMBEVoiceBlock.IMBEFRAME18) {
            assertTrue(imbeVoice.getAdditionalFrameData().length == 3);
         }
         
         if (frameType == IMBEVoiceBlock.IMBEFRAME12
            || frameType == IMBEVoiceBlock.IMBEFRAME13
            || frameType == IMBEVoiceBlock.IMBEFRAME14
            || frameType == IMBEVoiceBlock.IMBEFRAME15
            || frameType == IMBEVoiceBlock.IMBEFRAME16
            || frameType == IMBEVoiceBlock.IMBEFRAME17) {
            assertTrue(imbeVoice.getAdditionalFrameData().length == 4);
         }
         
         // Check values for these (these values might change later)
         assertTrue(imbeVoice.getEt() == 0);
         assertTrue(imbeVoice.getEr() == 0);
         assertTrue(imbeVoice.getM() == 0);
         assertTrue(imbeVoice.getL() == 0);
         assertTrue(imbeVoice.getE4() == 0);
         assertTrue(imbeVoice.getE1() == 0);
         
         // Check superframe
         assertTrue(imbeVoice.getSF() == superFrameIndex);
         
         // Update superFrame
         if (imbePacketIndex == 18) {
            imbePacketIndex = 0;
            if (superFrameIndex < 3)
               superFrameIndex++;
            else
               superFrameIndex = 0;
         }
      }
   }
   
   /**
    * Test IMBE voice block fields.  Note that we do not test IMBE Voice
    * Block packing and unpacking; that should exist as a separate test.
    * This tests only getIMBEVoiceData(String filePath, int numBlocks).
    */
   public void testIMBEDataGenerator2() {
      System.out.println("testIMBEDataGenerator2: ...");
      int numBlocksToRead = 3;
      List<IMBEVoiceBlock> imbeVoiceBlocks = IMBEVoiceDataGenerator
            .getIMBEVoiceData("voice/imbe-hex-test.txt", numBlocksToRead);

      int superFrameIndex = 0;
      int imbePacketIndex = 0;
      assertTrue(imbeVoiceBlocks.size() == numBlocksToRead);
      
      for (IMBEVoiceBlock imbeVoice : imbeVoiceBlocks) {
         imbePacketIndex++;
         System.out.println("testIMBEDataGenerator2: packetIndex="+imbePacketIndex);
         
         // Check if FT is in possible range (we don't check specific val)
         int frameType = imbeVoice.getFT();
         assertTrue((frameType >= 0x6A) && (frameType <= 0xCC));
         
         assertTrue(imbeVoice.getU0() <= ByteUtil.getMaxIntValueForNumBits(12));
         assertTrue(imbeVoice.getU1() <= ByteUtil.getMaxIntValueForNumBits(12));
         assertTrue(imbeVoice.getU2() <= ByteUtil.getMaxIntValueForNumBits(12));
         assertTrue(imbeVoice.getU3() <= ByteUtil.getMaxIntValueForNumBits(12));
         assertTrue(imbeVoice.getU4() <= ByteUtil.getMaxIntValueForNumBits(11));
         assertTrue(imbeVoice.getU5() <= ByteUtil.getMaxIntValueForNumBits(11));
         assertTrue(imbeVoice.getU6() <= ByteUtil.getMaxIntValueForNumBits(11));
         assertTrue(imbeVoice.getU7() <= ByteUtil.getMaxIntValueForNumBits(7));
         
         // Verify that these block types have 3 bytes of additional frame data.
         if (frameType == IMBEVoiceBlock.IMBEFRAME9
               || frameType == IMBEVoiceBlock.IMBEFRAME18) {
            assertTrue(imbeVoice.getAdditionalFrameData().length == 3);
         }
         
         if (frameType == IMBEVoiceBlock.IMBEFRAME12
            || frameType == IMBEVoiceBlock.IMBEFRAME13
            || frameType == IMBEVoiceBlock.IMBEFRAME14
            || frameType == IMBEVoiceBlock.IMBEFRAME15
            || frameType == IMBEVoiceBlock.IMBEFRAME16
            || frameType == IMBEVoiceBlock.IMBEFRAME17) {
            assertTrue(imbeVoice.getAdditionalFrameData().length == 4);
         }
         
         // Check values for these (these values might change later)
         assertTrue(imbeVoice.getEt() == 0);
         assertTrue(imbeVoice.getEr() == 0);
         assertTrue(imbeVoice.getM() == 0);
         assertTrue(imbeVoice.getL() == 0);
         assertTrue(imbeVoice.getE4() == 0);
         assertTrue(imbeVoice.getE1() == 0);
         
         // Check superframe
         assertTrue(imbeVoice.getSF() == superFrameIndex);
         
         // Update superFrame
         if (imbePacketIndex == 18) {
            imbePacketIndex = 0;
            if (superFrameIndex < 3)
               superFrameIndex++;
            else
               superFrameIndex = 0;
         }
      }
   }

   //==============================================================================
   public static void main(String args[]) {
      TestSuite suite= new TestSuite( Test_IMBEVoiceDataGeneratorTest.class);
      //TestSuite suite= new TestSuite();
      //suite.addTest(new Test_IMBEVoiceDataGeneratorTest("testIMBEDataGenerator"));
      //suite.addTest(new Test_IMBEVoiceDataGeneratorTest("testIMBEDataGenerator2"));
      junit.textui.TestRunner.run( suite);
   }
}
