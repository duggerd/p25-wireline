//
package gov.nist.p25.issi.p25payload;

import java.util.*;
import gov.nist.rtp.*;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

/**
 * This class compares two RTP or PTT objects for equivalence.
 * This class does not compare values for optional fields as
 * defined in the EADS Conformance Document.
 * 
 */
public class PttMatcher {

   private static Logger logger = Logger.getLogger(PttMatcher.class);

   static {
      PropertyConfigurator.configure("log4j.properties");
      // You can add more appenders here if you wish.
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   /**
    * Compare all non-optional fields between two RTP packets.
    */
   public static boolean matchRtp(RtpPacket rtp1, RtpPacket rtp2) {
      
      // Compare RTP Header fields
      if (rtp1.getV() != rtp2.getV()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("[PttMatcher.matchRtp()] Version fields do not " +
            "match: " + rtp1.getV() + " != " + rtp2.getV() + ".");
         return false;
         
      } else if (rtp1.getP() != rtp2.getP()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("[PttMatcher.matchRtp()] Padding fields do not " +
            "match: " + rtp1.getP() + " != " + rtp2.getP() + ".");
         return false;
         
      } else if (rtp1.getX() != rtp2.getX()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("[PttMatcher.matchRtp()] Extension fields do not " +
            "match: " + rtp1.getX() + " != " + rtp2.getX() + ".");
         return false;
         
      } else if (rtp1.getCC() != rtp2.getCC()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("[PttMatcher.matchRtp()] Contr. source count " +
            "fields do not match: " + rtp1.getCC() + " != " + rtp2.getCC() + ".");
         return false;
         
      } else if (rtp1.getM() != rtp2.getM()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("[PttMatcher.matchRtp()] Marker fields do not " +
            "match: " + rtp1.getM() + " != " + rtp2.getM() + ".");
         return false;
         
      } else if (rtp1.getPT() != rtp2.getPT()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("[PttMatcher.matchRtp()] Payload type fields do not " +
            "match: " + rtp1.getPT() + " != " + rtp2.getPT() + ".");
         return false;
         
      } 
      // SN is an optional field, so don't include
      // TS is an optional field, so don't include
      else if (rtp1.getSSRC() != rtp2.getSSRC()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("[PttMatcher.matchRtp()] Synch source fields do not " +
            "match: " + rtp1.getSSRC() + " != " + rtp2.getSSRC() + ".");
         return false;
         
      } else {
         // Now match payloads
         byte[] rtpPayload1 = rtp1.getPayload();
         byte[] rtpPayload2 = rtp2.getPayload();
         
         if (rtpPayload1.length != rtpPayload2.length) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
               logger.debug("[PttMatcher.matchRtp()] RTP payload sizes do not " +
               "match: " + rtpPayload1.length + " != " + rtpPayload2.length + ".");
            return false;
         }

         try {
            P25Payload p25Payload1 = new P25Payload(rtpPayload1);
            P25Payload p25Payload2 = new P25Payload(rtpPayload2);
               
            boolean match = matchP25Payload(p25Payload1, p25Payload2);
            if (! match) {
               return false;
            } 
         } catch (P25BlockException pbe) {
            pbe.printStackTrace();
         }
         return true;
      }
   }
   
   /**
    * Compare all non-optional fields between two P25Payload objects.
    */
   public static boolean matchP25Payload(P25Payload p25Payload1, 
         P25Payload p25Payload2) {
      
      // Compare control octets
      ControlOctet controlOctet1 = p25Payload1.getControlOctet();
      ControlOctet controlOctet2 = p25Payload2.getControlOctet();
      
      boolean match = matchControlOctet(controlOctet1, controlOctet2);
      if (! match) {
         return false;
      } 
      
      // Compare block header vectors
      ArrayList<BlockHeader> blockHeaders1 = p25Payload1.getBlockHeaders();
      ArrayList<BlockHeader> blockHeaders2 = p25Payload2.getBlockHeaders();
      if (blockHeaders1.size() != blockHeaders2.size()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("[PttMatcher.matchP25Payload()] Number of block headers do not " +
            "match: " + blockHeaders1.size() + " != " + blockHeaders2.size() + ".");
         return false;
         
      }
      
      for (int i = 0; i < blockHeaders1.size(); i++) {
         // Compare block header
         BlockHeader bh1 = blockHeaders1.get(i);
         BlockHeader bh2 = blockHeaders2.get(i);
         
         match = matchBlockHeaders(bh1, bh2);
         if (! match) {
            return false;
         }
         
         // Compare associated block
         BlockType bt1 = bh1.getBlockType();
         BlockType bt2 = bh2.getBlockType();
         if (bt1.intValue() != bt2.intValue()) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
               logger.debug("[PttMatcher.matchP25Payload()] Block types do not " +
               "match: " + bt1.intValue() + " != " + bt2.intValue() + ".");
            return false;
         }
         
	 //EHC
         if (bt1 == BlockType.getInstance(BlockType.PACKET_TYPE_INDEX)) {
            ISSIPacketType ipt1 = p25Payload1.getISSIPacketType();
            ISSIPacketType ipt2 = p25Payload2.getISSIPacketType();
            match = matchISSIPacketType(ipt1, ipt2);
            if (! match) {
               return false;
            } 
            
	 //EHC
         } else if (bt1 == BlockType.getInstance(BlockType.PTT_CONTROL_WORD_INDEX)) {
            PTTControlWord pcw1 = p25Payload1.getPTTControlWord();
            PTTControlWord pcw2 = p25Payload2.getPTTControlWord();
            match = matchPTTControlWord(pcw1, pcw2);
            if (! match) {
               return false;
            }
         } 
         
         // Ignore all ISSI Header Info
         
         // Ignore IMBE Voice
         
      }
      return true;
   }
   
   /**
    * Compare all non-optional fields between two Control Octet bjects.
    */
   public static boolean matchControlOctet(ControlOctet co1, 
      ControlOctet co2) {
      
      if (co1.getS() != co2.getS()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchControlOctet()] Signal fields do not " +
            "match: " + co1.getS()  + " != " + co2.getS() + ".");
         return false;
         
      } else if (co1.getC() != co2.getC()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchControlOctet()] Compact fields do not " +
            "match: " + co1.getC() + " != " + co1.getC() + ".");
         return false;
         
      } else if (co1.getBHC() != co2.getBHC()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchControlOctet()] Block header count " +
                  " fields do not " +
            "match: " + co1.getBHC() + " != " + co1.getBHC() + ".");
         return false;
         
      } else {
         return true;
      }
   }
   
   public static boolean matchBlockHeaders(BlockHeader bh1, BlockHeader bh2) {
         
      if (bh1.getE() != bh2.getE()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchBlockHeaders()] Payload types do not " +
            "match: " + bh1.getE()  + " != " + bh2.getE() + ".");
         return false;
         
      } else if (bh1.getBT() != bh2.getBT()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchBlockHeaders()] Block types do not " +
            "match: " + bh1.getBT()  + " != " + bh2.getBT() + ".");
         return false;   
            
      } else if (bh1.getTimeStampOffset() != bh2.getTimeStampOffset()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchBlockHeaders()] Timestamp offsets do not " +
            "match: " + bh1.getTimeStampOffset()  + " != " + bh2.getTimeStampOffset() + ".");
         return false;
            
      } else if (bh1.getBlockLength() != bh2.getBlockLength()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchBlockHeaders()] Block lengths do not " +
            "match: " + bh1.getBlockLength()  + " != " + bh2.getBlockLength() + ".");
         return false;
            
      } else  {
         return true;
      }
   }
   
   public static boolean matchISSIPacketType(ISSIPacketType ipt1, ISSIPacketType ipt2) {
      
      if (ipt1.getM() != ipt2.getM()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchISSIPacketType()] Mute flags do not " +
            "match: " + ipt1.getM()  + " != " + ipt2.getM() + ".");
         return false;
         
      } else if (ipt1.getPT() != ipt2.getPT()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchISSIPacketType()] Packet types do not " +
            "match: " + ipt1.getPacketType()  + " != " + ipt2.getPacketType() + ".");
         return false;   
      } 
      
      // Ignore comparison of optional SO field
      
      // Ignore comparison of optional TSN field
      
      else if (ipt1.getL() != ipt2.getL()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchISSIPacketType()] Losing audio flags do not " +
            "match: " + ipt1.getL()  + " != " + ipt2.getL() + ".");
         return false;   
      } 
      
      // Ignore comparison of optional Interval field
      else {
         return true;
      }
   }
   
   public static boolean matchPTTControlWord(PTTControlWord pcw1, PTTControlWord pcw2) {
      
      if (pcw1.getWacnId() != pcw2.getWacnId()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchPTTControlWord()] WACN IDs do not " +
            "match: " + pcw1.getWacnId()  + " != " + pcw2.getWacnId() + ".");
         return false;
         
      } else if (pcw1.getSystemId() != pcw2.getSystemId()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchPTTControlWord()] System IDs do not " +
            "match: " + pcw1.getSystemId()  + " != " + pcw2.getSystemId() + ".");
         return false;   
            
      } else if (pcw1.getUnitId() != pcw2.getUnitId()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchPTTControlWord()] Unit IDs do not " +
            "match: " + pcw1.getUnitId()  + " != " + pcw2.getUnitId() + ".");
         return false;   
            
      } else if (pcw1.getTP() != pcw2.getTP()) {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.warn("[PttMatcher.matchPTTControlWord()] Transmit priorities do not " +
            "match: " + pcw1.getTP()  + " != " + pcw2.getTP() + ".");
         return false;   
            
      } else {
         return true;   
      }
   }
   
   public static boolean matchISSIHeaderWord(ISSIHeaderWord ihw1, 
         ISSIHeaderWord ihw2) {
      
      // Ignore comparison of optional Message Indicator
      // Ignore comparison of optional AlgID
      // Ignore comparison of optional KeyID
      // Ignore comparison of optional MFID
      // Ignore comparison of optional GroupID
      // Ignore comparison of optional NID
      // Ignore comparison of optional Free running superframe counter
      // Ignore comparison of optional Reserved
      return true;
   }
   
   public static boolean matchIMBEVoiceBlock(IMBEVoiceBlock ivb1, 
         IMBEVoiceBlock ivb2) {
      
      // Ignore Frame Type
      // Ignore Voice Frame
      // Ignore Et
      // Ignore Er
      // Ignore Mute Frame
      // Ignore Lost Frame
      // Ignore E4
      // Ignore E1
      // Ignore Free Running Super Frame Counter
      // Ignore Reserved
      return true;
   }
}
