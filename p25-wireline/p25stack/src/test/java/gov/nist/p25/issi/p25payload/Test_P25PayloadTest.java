//
package gov.nist.p25.issi.p25payload;

import gov.nist.p25.issi.utils.ByteUtil;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

/**
 * This class tests setting and getting a P25Payload object.
 * 
 */
public class Test_P25PayloadTest extends TestCase {

   private static Logger logger = Logger.getLogger(Test_P25PayloadTest.class);
   static {
      PropertyConfigurator.configure("log4j.properties");
      // You can add more appenders here if you wish.
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Set a P25 payload object.
    * 
    * @return the P25 payload object as a byte array.
    */
   public void testP25Payload() {

      try {
         /*******************************************************************
          * Create P25 Payload #1
          ******************************************************************/

         // Construct an ISSI Packet Type block
         ISSIPacketType pt1 = new ISSIPacketType();
         pt1.setM(ByteUtil.getMaxIntValueForNumBits(1));
         pt1.setPacketType(PacketType.PTT_TRANSMIT_PROGRESS);
         pt1.setSO(128);  // Emergency calls
         pt1.setTranssmissionSequenceNumber(ByteUtil.getMaxIntValueForNumBits(7));
         pt1.setL(ByteUtil.getMaxIntValueForNumBits(1));
         pt1.setInterval(ByteUtil.getMaxIntValueForNumBits(8));

         // Construct an PTT Control Word block
         PTTControlWord cw1 = new PTTControlWord();
         cw1.setWacnId(ByteUtil.getMaxIntValueForNumBits(20));
         cw1.setSystemId(ByteUtil.getMaxIntValueForNumBits(12));
         cw1.setUnitId(ByteUtil.getMaxIntValueForNumBits(24));
         cw1.setTP(3);   // Max value for TP

         // Set ISSI Header Word Block
         ISSIHeaderWord hw1 = new ISSIHeaderWord();

         byte[] messageIndicatorBytes = new byte[9];
         messageIndicatorBytes[8] |= 1;
         hw1.setMessageIndicator(messageIndicatorBytes);
         hw1.setAlgId(ByteUtil.getMaxIntValueForNumBits(8));
         hw1.setKeyId(ByteUtil.getMaxIntValueForNumBits(16));
         hw1.setMFID(ByteUtil.getMaxIntValueForNumBits(8));
         hw1.setGroupId(ByteUtil.getMaxIntValueForNumBits(16));
         hw1.setNID(ByteUtil.getMaxIntValueForNumBits(16));
         hw1.setSF(ByteUtil.getMaxIntValueForNumBits(2));
         hw1.setVBB(ByteUtil.getMaxIntValueForNumBits(2));
         hw1.setReserved(ByteUtil.getMaxIntValueForNumBits(4));
         
         // Set one IMBE Voice Block
         IMBEVoiceBlock vb1 = new IMBEVoiceBlock();
         vb1.setFT(ByteUtil.getMaxIntValueForNumBits(8));
         vb1.setU0(ByteUtil.getMaxIntValueForNumBits(12));
         vb1.setU1(ByteUtil.getMaxIntValueForNumBits(12));
         vb1.setU2(ByteUtil.getMaxIntValueForNumBits(12));
         vb1.setU3(ByteUtil.getMaxIntValueForNumBits(12));   
         vb1.setU4(ByteUtil.getMaxIntValueForNumBits(11));
         vb1.setU5(ByteUtil.getMaxIntValueForNumBits(11));
         vb1.setU6(ByteUtil.getMaxIntValueForNumBits(11));   
         vb1.setU7(ByteUtil.getMaxIntValueForNumBits(7));   
         vb1.setEt(ByteUtil.getMaxIntValueForNumBits(3));
         vb1.setEr(ByteUtil.getMaxIntValueForNumBits(3));
         vb1.setM(ByteUtil.getMaxIntValueForNumBits(1));
         vb1.setL(ByteUtil.getMaxIntValueForNumBits(1));
         vb1.setE4(ByteUtil.getMaxIntValueForNumBits(1));
         vb1.setE1(ByteUtil.getMaxIntValueForNumBits(3));
         vb1.setSF(ByteUtil.getMaxIntValueForNumBits(2));
         vb1.setReserved(ByteUtil.getMaxIntValueForNumBits(2));
         byte[] additionalFrameData1 = new byte[3];
         vb1.setAdditionalFrameData(additionalFrameData1);
         IMBEVoiceBlock[] voiceBlocks1 = new IMBEVoiceBlock[1];
         voiceBlocks1[0] = vb1;
         
         // Set Mfr Specific Data (TBD)
//         ManufacturerSpecific ms1 = new ManufacturerSpecific();
//         ms1.setMFID(ByteUtil.getMaxIntValueForNumBits(8));
//         ms1.setLength(ByteUtil.getMaxIntValueForNumBits(8));
//         byte[] variableLengthData = 
//            new byte[ByteUtil.getMaxIntValueForNumBits(8)];
//         ms1.setManufacturerData(variableLengthData);
//         ManufacturerSpecific[] mfrBlocks1 = new ManufacturerSpecific[1];
//         mfrBlocks1[0] = ms1;
         
         // Create P25 Payload 1
         P25Payload p25Payload1 = new P25Payload(pt1, cw1, hw1, voiceBlocks1, null);
         ControlOctet co1 = p25Payload1.getControlOctet();
         co1.setS(1);
         
         // Create byte array representation
         byte[] p25Payload1Bytes = p25Payload1.getBytes();
         
         /*******************************************************************
          * Create P25 Payload #2
          ******************************************************************/
         // Create P25 Payload 2
         P25Payload p25Payload2 = new P25Payload(p25Payload1Bytes);
         
         // Compare Control Octets
         ControlOctet co2 = p25Payload2.getControlOctet();
         assertTrue(co1.getS() == co2.getS());
         assertTrue(co1.getC() == co2.getC());
         assertTrue(co1.getBHC() == co2.getBHC());
         
         // Compare Packet Types
         ISSIPacketType pt2 = p25Payload2.getISSIPacketType();
         assertTrue(pt1.getM() == pt2.getM());
         assertTrue(pt1.getPT() == pt2.getPT());
         assertTrue(pt1.getServiceOptions() == pt2.getServiceOptions());
         assertTrue(pt1.getTransmissionSequenceNumber() == pt2.getTransmissionSequenceNumber());
         assertTrue(pt1.getL() == pt2.getL());
         assertTrue(pt1.getInterval() == pt2.getInterval());
         
         // Compare PTT Control Words
         PTTControlWord cw2 = p25Payload2.getPTTControlWord();
         assertTrue(cw1.getWacnId() == cw2.getWacnId());
         assertTrue(cw1.getSystemId() == cw2.getSystemId());
         assertTrue(cw1.getUnitId() == cw2.getUnitId());
         assertTrue(cw1.getTP() == cw2.getTP());         
         
         // Compare ISSI Header Words
         ISSIHeaderWord hw2 = p25Payload2.getISSIHeaderWord();
         assertTrue(hw1.getMessageIndicator().length == hw2.getMessageIndicator().length);
         assertTrue(hw1.getAlgId() == hw2.getAlgId());
         assertTrue(hw1.getKeyId() == hw2.getKeyId());
         assertTrue(hw1.getMFID() == hw2.getMFID());   
         assertTrue(hw1.getGroupId() == hw2.getGroupId());   
         assertTrue(hw1.getNID() == hw2.getNID());
         assertTrue(hw1.getSF() == hw2.getSF());
         assertTrue(hw1.getVBB() == hw2.getVBB());
         assertTrue(hw1.getReserved() == hw2.getReserved());
         
         // Compare IMBE Voice Block length
         IMBEVoiceBlock[] voiceBlocks2 = p25Payload2.getIMBEVoiceBlockArray();
         IMBEVoiceBlock vb2 = voiceBlocks2[0];
         assertTrue(vb1.getFT() == vb2.getFT());
         assertTrue(vb1.getU0() == vb2.getU0());
         assertTrue(vb1.getU1() == vb2.getU1());
         assertTrue(vb1.getU2() == vb2.getU2());
         assertTrue(vb1.getU3() == vb2.getU3());
         assertTrue(vb1.getU4() == vb2.getU4());
         assertTrue(vb1.getU5() == vb2.getU5());
         assertTrue(vb1.getU6() == vb2.getU6());
         assertTrue(vb1.getU7() == vb2.getU7());
         assertTrue(vb1.getEt() == vb2.getEt());
         assertTrue(vb1.getEr() == vb2.getEr());
         assertTrue(vb1.getM() == vb2.getM());
         assertTrue(vb1.getL() == vb2.getL());
         assertTrue(vb1.getE4() == vb2.getE4());
         assertTrue(vb1.getE1() == vb2.getE1());
         assertTrue(vb1.getSF() == vb2.getSF());
         assertTrue(vb1.getReserved() == vb2.getReserved());
         assertTrue(vb1.getAdditionalFrameData().length == 
            vb2.getAdditionalFrameData().length);
         
         // Compare Mfr Specific blocks (TBD)
         
      } catch (P25BlockException pbe) {
         pbe.printStackTrace();
         fail();
      }
      
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.debug("DONE: All assertions passed.");
   }
}
