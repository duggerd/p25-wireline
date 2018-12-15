//
package gov.nist.rtp;

import gov.nist.p25.issi.utils.ByteUtil;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import junit.framework.TestCase;

/**
 * This class tests P25 control octets.
 */
public class RtpPacketTest extends TestCase {

   protected static org.apache.log4j.Level LOGGER_LEVEL = org.apache.log4j.Level.DEBUG;

   /** The logger for this class. */
   private static Logger logger = Logger.getLogger("gov.nist.rtp");

   static {
      PropertyConfigurator.configure("log4j.properties");
      // You can add more appenders here if you wish.
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Contruct an RTP packet JUnit test.
    */
   public RtpPacketTest() {
   }

   /**
    * Set up this JUnit test.
    */
   public void setUp() {

   }

   /**
    * Test packing and unpacking an RTP packet with maximum values.
    */
   public void testPackUnpackMaxValues() {

      // Test pack
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("---------------------------------------------\n"
               + "Pack RtpPacket Maximum Values");

      RtpPacket rtpPacket = new RtpPacket();
      rtpPacket.setV(ByteUtil.getMaxIntValueForNumBits(2));
      rtpPacket.setP(ByteUtil.getMaxIntValueForNumBits(1));
      rtpPacket.setX(ByteUtil.getMaxIntValueForNumBits(1));
      rtpPacket.setCC(ByteUtil.getMaxIntValueForNumBits(4));
      rtpPacket.setM(ByteUtil.getMaxIntValueForNumBits(1));
      rtpPacket.setPT(ByteUtil.getMaxIntValueForNumBits(7));
      rtpPacket.setSN(ByteUtil.getMaxIntValueForNumBits(16));
      rtpPacket.setTS(ByteUtil.getMaxLongValueForNumBits(32));
      rtpPacket.setSSRC(ByteUtil.getMaxLongValueForNumBits(32));
      // byte[] testPayload = "This is some payload".getBytes();
      byte[] testPayload = new byte[11];
      rtpPacket.setPayload(testPayload, testPayload.length);

      byte[] encodedPacket = rtpPacket.getData();

      // Test unpack
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("---------------------------------------------\n"
               + "Unpack RtpPacket Maximum Values");

      RtpPacket newRtpPacket = new RtpPacket(encodedPacket,
            encodedPacket.length);
      // newRtpPacket.unpack(encodedPacket, encodedPacket.length);

      assertTrue(newRtpPacket.getV() == ByteUtil.getMaxIntValueForNumBits(2));
      assertTrue(newRtpPacket.getP() == ByteUtil.getMaxIntValueForNumBits(1));
      assertTrue(newRtpPacket.getX() == ByteUtil.getMaxIntValueForNumBits(1));
      assertTrue(newRtpPacket.getCC() == ByteUtil.getMaxIntValueForNumBits(4));
      assertTrue(newRtpPacket.getM() == ByteUtil.getMaxIntValueForNumBits(1));
      assertTrue(newRtpPacket.getPT() == ByteUtil.getMaxIntValueForNumBits(7));
      assertTrue(newRtpPacket.getSN() == ByteUtil.getMaxIntValueForNumBits(16));
      assertTrue(newRtpPacket.getTS() == ByteUtil.getMaxLongValueForNumBits(32));
      assertTrue(newRtpPacket.getSSRC() == ByteUtil.getMaxLongValueForNumBits(32));
   }

   /**
    * Test packing and unpacking an RTP packet with out of range values.
    */
   public void testPackUnpackOutOfRangeValues() {

      // Test pack
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("---------------------------------------------\n"
               + "Pack RtpPacket Out of Range");

      RtpPacket rtpPacket = new RtpPacket();
      try {
         rtpPacket.setV(ByteUtil.getMaxIntValueForNumBits(2) + 1);
      } catch (IllegalArgumentException iae) {
         if (logger.isEnabledFor(org.apache.log4j.Level.FATAL))
            logger.fatal("Set V = " + (ByteUtil.getMaxIntValueForNumBits(2) + 1)
                  + ": " + iae);
      }

      try {
         rtpPacket.setP(ByteUtil.getMaxIntValueForNumBits(1) + 1);
      } catch (IllegalArgumentException iae) {
         if (logger.isEnabledFor(org.apache.log4j.Level.FATAL))
            logger.fatal("Set P = " + (ByteUtil.getMaxIntValueForNumBits(1) + 1) + ": "
                  + iae);
      }

      try {
         rtpPacket.setX(ByteUtil.getMaxIntValueForNumBits(1)+ 1);
      } catch (IllegalArgumentException iae) {
         if (logger.isEnabledFor(org.apache.log4j.Level.FATAL))
            logger.fatal("Set X = " + (ByteUtil.getMaxIntValueForNumBits(1) + 1) + ": "
                  + iae);
      }

      try {
         rtpPacket.setCC(ByteUtil.getMaxIntValueForNumBits(4) + 1);
      } catch (IllegalArgumentException iae) {
         if (logger.isEnabledFor(org.apache.log4j.Level.FATAL))
            logger.fatal("Set CC = " + (ByteUtil.getMaxIntValueForNumBits(4) + 1)
                  + ": " + iae);
      }

      try {
         rtpPacket.setM(ByteUtil.getMaxIntValueForNumBits(1) + 1);
      } catch (IllegalArgumentException iae) {
         if (logger.isEnabledFor(org.apache.log4j.Level.FATAL))
            logger.fatal("Set M = " + (ByteUtil.getMaxIntValueForNumBits(1) + 1) + ": "
                  + iae);
      }

      try {
         rtpPacket.setPT(ByteUtil.getMaxIntValueForNumBits(7) + 1);
      } catch (IllegalArgumentException iae) {
         if (logger.isEnabledFor(org.apache.log4j.Level.FATAL))
            logger.fatal("Set PT = " + (ByteUtil.getMaxIntValueForNumBits(7) + 1)
                  + ": " + iae);
      }

      try {
         rtpPacket.setSN(ByteUtil.getMaxIntValueForNumBits(16) + 1);
      } catch (IllegalArgumentException iae) {
         if (logger.isEnabledFor(org.apache.log4j.Level.FATAL))
            logger.fatal("Set SN = " + (ByteUtil.getMaxIntValueForNumBits(16) + 1)
                  + ": " + iae);
      }

      try {
         rtpPacket.setTS(ByteUtil.getMaxLongValueForNumBits(32) + 1);
      } catch (IllegalArgumentException iae) {
         if (logger.isEnabledFor(org.apache.log4j.Level.FATAL))
            logger.fatal("Set TS = " + (ByteUtil.getMaxLongValueForNumBits(32) + 1)
                  + ": " + iae);
      }

      try {
         rtpPacket.setSSRC(ByteUtil.getMaxLongValueForNumBits(32) + 1);
      } catch (IllegalArgumentException iae) {
         if (logger.isEnabledFor(org.apache.log4j.Level.FATAL))
            logger.fatal("Set SSRC = " + (ByteUtil.getMaxLongValueForNumBits(32) + 1)
                  + ": " + iae);
      }
      if (logger.isEnabledFor(org.apache.log4j.Level.INFO))
         logger.info("\nEnd of test.");
   }
}
