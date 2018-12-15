//
package gov.nist.p25.issi.p25payload;

import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import gov.nist.rtp.*;

/**
 * This class tests setting and getting a P25Payload object.
 */
public class Test_PttMatcherTest extends TestCase {

   private static Logger logger = Logger.getLogger(Test_PttMatcherTest.class);
   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Construct a test for setting and getting P25 blocks.
    */
   public Test_PttMatcherTest() {
   }
   
   public void testRtpMatcher() {
      // Construct an IBME Voice array with one IMBE Voice block
      // Construct an ISSI header word #1
      ISSIHeaderWord issiHeaderWord1 = new ISSIHeaderWord();

      byte[] messageIndicatorBytes = new byte[9];
      messageIndicatorBytes[8] |= 1;
      issiHeaderWord1.setMessageIndicator(messageIndicatorBytes);

      issiHeaderWord1.setAlgId(1);
      issiHeaderWord1.setKeyId(1);
      issiHeaderWord1.setMFID(1);
      issiHeaderWord1.setGroupId(1);
      issiHeaderWord1.setNID(1);
      issiHeaderWord1.setSF(1);
      issiHeaderWord1.setReserved(1);
      
      // Construct an ISSI header word #2
      ISSIHeaderWord issiHeaderWord2 = new ISSIHeaderWord();
      
      byte[] messageIndicatorBytes2 = new byte[9];
      messageIndicatorBytes2[8] |= 1;
      issiHeaderWord2.setMessageIndicator(messageIndicatorBytes2);

      issiHeaderWord2.setAlgId(1);
      issiHeaderWord2.setKeyId(1);
      issiHeaderWord2.setMFID(1);
      issiHeaderWord2.setGroupId(1);
      issiHeaderWord2.setNID(1);
      issiHeaderWord2.setSF(1);
      issiHeaderWord2.setReserved(1);

      // Construct an PTT control word #1
      PTTControlWord pttControlWord1 = new PTTControlWord();
      pttControlWord1.setWacnId(1);
      pttControlWord1.setSystemId(1);
      pttControlWord1.setUnitId(1);
      pttControlWord1.setTP(1);
      
      // Construct an PTT control word #2
      PTTControlWord pttControlWord2 = new PTTControlWord();
      pttControlWord2.setWacnId(1);
      pttControlWord2.setSystemId(1);
      pttControlWord2.setUnitId(1);
      pttControlWord2.setTP(1);

      // Construct an ISSI packet type #1
      ISSIPacketType issiPacketType1 = new ISSIPacketType();
      issiPacketType1.setM(1);
      issiPacketType1.setPacketType(PacketType.PTT_TRANSMIT_REQUEST);
      issiPacketType1.setSO(1);
      issiPacketType1.setTranssmissionSequenceNumber(1);
      issiPacketType1.setL(1);
      issiPacketType1.setInterval(1);

      // Construct an ISSI packet type #2
      ISSIPacketType issiPacketType2 = new ISSIPacketType();
      issiPacketType2.setM(1);
      issiPacketType2.setPacketType(PacketType.PTT_TRANSMIT_REQUEST);
      issiPacketType2.setSO(1);
      issiPacketType2.setTranssmissionSequenceNumber(1);
      issiPacketType2.setL(1);
      issiPacketType2.setInterval(1);

      // Create a P25 payload #1
//       * @param issiPacketType
//       * @param pttControlWord
//       * @param issiHeaderWord
//       * @param imbeVoiceBlocks
//       * @param manufacturerSpecifics

      P25Payload p25Payload1 = null;
      try {
         p25Payload1 = new P25Payload(issiPacketType1, pttControlWord1,
               issiHeaderWord1, null, null);
         p25Payload1.getControlOctet().setS(1);
      } catch (P25BlockException pbe) {
         pbe.printStackTrace();
         fail("unexpected failure");
      }
      
      // Create a P25 payload #2
      P25Payload p25Payload2 = null;
      try {
         p25Payload2 = new P25Payload(issiPacketType2, pttControlWord2,
               issiHeaderWord2, null, null);
         p25Payload2.getControlOctet().setS(1);
      } catch (P25BlockException pbe) {
         pbe.printStackTrace();
         fail("unexpected failure");
      }

      // Create RTP payload #1
      byte[] rtpPayload1 = null;
      try {
         rtpPayload1 = p25Payload1.getBytes();
      } catch (P25BlockException pbe) {
         pbe.printStackTrace();
      }
      
      // Create RTP payload #2
      byte[] rtpPayload2 = null;
      try {
         rtpPayload2 = p25Payload2.getBytes();
      } catch (P25BlockException pbe) {
         pbe.printStackTrace();
      }
      
      // Create RTP packet #1
      RtpPacket rtp1 = new RtpPacket();
      rtp1.setV(2);   
      rtp1.setP(0);
      rtp1.setX(0);
      rtp1.setCC(0);
      rtp1.setM(0);
      rtp1.setPT(0);
      rtp1.setSSRC(0);
      rtp1.setPayload(rtpPayload1, rtpPayload1.length);
      
      // Create RTP packet #2
      RtpPacket rtp2 = new RtpPacket();
      rtp2.setV(2);
      rtp2.setP(0);
      rtp2.setX(0);
      rtp2.setCC(0);   
      rtp2.setM(0);   
      rtp2.setPT(0);
      rtp2.setSSRC(0);
      rtp2.setPayload(rtpPayload2, rtpPayload2.length);
      assertTrue(PttMatcher.matchRtp(rtp1, rtp2));

      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.debug("DONE: All assertions passed.");
   }
}
