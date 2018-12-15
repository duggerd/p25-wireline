//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;
//import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

import gov.nist.p25.common.util.DataCoder;
import gov.nist.p25.issi.utils.ByteUtil;

/**
 * This class implements a P25 block header.
 * <p>
 * 
 * <pre>
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |E|      BT     |             TSO           |         BL        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 */
public class BlockHeader implements Serializable {

   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(BlockHeader.class);

   /** Constant that identifies the total byte length of fixed fields. */
   protected static final int FIXED_LENGTH = 4;

   /** Payload type (1 bit). */
   private int E = 0;

   /** Block type (7 bits). */
   private int BT = 0;

   /**
    * Timestamp offset (14 bits). Here, we use 0 as the default for all packets
    * that are not IMBEVoiceBlock packets. Only in the case of IMBEVoiceBlock
    * packets is this field set to the voice sample duration of an IMBE voice
    * packet (20ms in 125 microsecond resolution).
    */
   private int timeStampOffset = 0;

   /** Length of corresponding block (10 bits). */
   private int blockLength = 0;

   /**
    * Construct a P25 block header.
    */
   public BlockHeader() {
   }

   /**
    * Construct a P25 block header given its byte array representation.
    * 
    * @param bytes The byte array representation of this block header.
    * @throws P25BlockException
    */
   public BlockHeader(byte[] bytes) throws P25BlockException {
      
      int i = ByteUtil.bytesToInt(bytes);

      this.E = (i >>> 0x1F) & 0x01;      // Extract E
      this.BT = (i >>> 0x18) & 0x07F;    // Extract BT
      this.timeStampOffset = (i >>> 0x0A) & 0x3FFF;    // Extract TSO
      this.blockLength = i & 0x3FF;      // Extract BL

      if ((BT != BlockType.IMBE_VOICE_INDEX) &&
          (BT != BlockType.PACKET_TYPE_INDEX) &&
          (BT != BlockType.ISSI_HEADER_INFO_INDEX) &&
          (BT != BlockType.MFGSPEC_MIN_INDEX) &&
          (BT != BlockType.PTT_CONTROL_WORD_INDEX)
	 ) {
         throw new P25BlockException("Unsupported block type: " + BT);
      }
   }

   /**
    * Get the size of this block header in bytes.
    * 
    * @return The size of this block header in bytes.
    */
   public int size() {
      return FIXED_LENGTH;
   }

   /**
    * Get the block header as a byte array.
    * 
    * @return The block header byte array.
    */
   public byte[] getBytes() {
      int i = 0;
      i |= E;     // Add E
      i <<= 0x07; // Make room for BT
      i |= BT;    // Add BT
      i <<= 0x0E; // Make room for TSO
      i |= timeStampOffset;    // Add TSO
      i <<= 0x0A; // Make room for BL
      i |= blockLength;    // Add BL
      return ByteUtil.intToBytes(i);
   }

   /**
    * Set the payload type.
    * 
    * @param i
    *            The payload type (1 bit)
    * @throws IllegalArgumentException
    */
   public void setE(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         E = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }


   /**
    * Get the payload type int value.
    * 
    * @return The payload type int value.
    */
   public int getE() {
      return E;
   }
   
   /**
    * Get the payload type.
    * 
    * @return The payload type.
    */
   private PayloadType getPayloadType() {
      return PayloadType.getInstance(E);
   }

   /**
    * Set the block type.
    * 
    * @param i
    *            The block type (7 bits)
    * @throws IllegalArgumentException
    */
   public void setBT(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(7)))
         BT = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get the block type.
    * 
    * @return The block type.
    */
   public int getBT() {
      return BT;
   }

   /**
    * Get the block type.
    * 
    * @return The block type.
    */
   public BlockType getBlockType() {
      return BlockType.getInstance(BT);
   }

   /**
    * Set the timestamp offset. Each block header SHALL contain a 14-bit
    * timestamp offset. This SHALL be in the same units as the RTP header
    * timestamp (125 microseconds). The sum of the RTP timestamp and the offset
    * is the timestamp of each block.
    * 
    * @param i
    *            The timestamp offset (14 bits)
    * @throws IllegalArgumentException
    */
   public void setTimeStampOffset(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(14)))
         timeStampOffset = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get the timestamp offset.
    * 
    * @return the timestamp offset.
    */
   public int getTimeStampOffset() {
      return timeStampOffset;
   }

   /**
    * Set the block length.
    * 
    * @param i
    *            The block length (10 bits)
    * @throws IllegalArgumentException
    */
   public void setBlockLength(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(10)))
         blockLength = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get the block length. Each block header SHALL contain a 10-bit length
    * indicator. This SHALL be the number of octets contained in the
    * corresponding block.
    * 
    * @return The block length.
    */
   public int getBlockLength() {
      return blockLength;
   }
   
   /**
    * Compare this object with another object.
    * 
    * @param other The other object to be compared to.
    * @return True if this object is equivalent to the other object.
    */
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof BlockHeader))
         return false;
      BlockHeader bh = (BlockHeader) other;
      return this.E == bh.E && this.BT == bh.BT &&
             this.timeStampOffset == bh.timeStampOffset &&
             this.blockLength == bh.blockLength;
   }

   /**
    * Get the XML formatted string representation.
    * 
    * @return The XML formatted string representation.
    */
   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("<block-header");
      sbuf.append("\n payloadType=\"" + getPayloadType() + "\"");
      sbuf.append("\n blockType=\"" + getBlockType() + "\"");
      sbuf.append("\n timeStampOffset=\"" + getTimeStampOffset() + "\"");
      sbuf.append("\n blockLength=\"" + getBlockLength() + "\"");
      sbuf.append("\n/>");
      return sbuf.toString();
   }
   
   public String toISSIString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append(getPayloadType().toString() + ": ");
      sbuf.append(DataCoder.toBinary(getPayloadType().intValue()));
      sbuf.append("\nBlock Type: ");
      sbuf.append(DataCoder.toTextIntegerBinary(getBlockType().toString(),getBlockType().intValue(),7));
      sbuf.append("\nTimestamp offset: ");
      sbuf.append(DataCoder.toIntegerHex(getTimeStampOffset(),14));
      sbuf.append("\nLength: " + DataCoder.toIntegerBinary(getBlockLength(),10));
      return sbuf.toString();
   }
   
   /**
    * Create an instance from XML attributes.
    *
    * @param attrs The XML attributes
    * @return the BlockHeader object
    */
   public BlockHeader createFromAttributes(Attributes attrs) {
      BlockHeader retval = new BlockHeader();
      retval.setE(PayloadType.getValueFromDescription(attrs.getValue("payloadType")));
      retval.setBT(BlockType.getValueFromDescription(attrs.getValue("blockType")));
      retval.setTimeStampOffset(Integer.parseInt(attrs.getValue("timeStampOffset")));
      retval.setBlockLength(Integer.parseInt(attrs.getValue("blockLength")));
      return retval;
   }
}
