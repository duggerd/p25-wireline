//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;
//import org.apache.log4j.Logger;

import gov.nist.p25.common.util.DataCoder;
import gov.nist.p25.issi.utils.ByteUtil;

/**
 * This class implements a P25 IMBE Voice block. The P25 IMBE Voice block has
 * the following format:
 * <p>
 * 
 * <pre>
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       FT      |       U0 (b11-0)      |       U1 (b11-0)      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |        U2 (b10-0)     |       U3 (b11-0)      |   U4 (b10-3)  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  U4 |     U5 (b10-0)      |      U6 (b10-0)     |  U7 (b6-0)  |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |  Et | Er  |M|L|E|  E1 |SF |rs | Additional Frame Type Specific|
 * |     |     | | |4|     |   |vd | Data (variable length)        |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |             Additional Frame Type Specific Data               |
 * |                        (variable length)                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 */
public class IMBEVoiceBlock implements Serializable {
   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(IMBEVoiceBlock.class);

   /** Constant that identifies the total byte length of fixed fields. */
   protected final static int FIXED_LENGTH = 14;

   /** Constant that identifies an IMBE Voice 1 (8 bits). */
   public static final int IMBE1 = 0xC2;

   /** Constant that identifies an IMBE Voice 2 (8 bits). */
   public static final int IMBE2 = 0xC3;

   /** Constant that identifies an IMBE Voice 3 (8 bits). */
   public static final int IMBE3 = 0xC4;

   /** Constant that identifies an IMBE Voice 4 (8 bits). */
   public static final int IMBE4 = 0xC5;

   /** Constant that identifies an IMBE Voice 5 (8 bits). */
   public static final int IMBE5 = 0xC6;

   /** Constant that identifies an IMBE Voice 6 (8 bits). */
   public static final int IMBE6 = 0xC7;

   /** Constant that identifies an IMBE Voice 7 (8 bits). */
   public static final int IMBE7 = 0xC8;

   /** Constant that identifies an IMBE Voice 8 (8 bits). */
   public static final int IMBE8 = 0xC9;

   /** Constant that identifies an IMBE Voice 9 + Low Speed Data (8 bits). */
   public static final int IMBEFRAME9 = 0x6A;

   /** Constant that identifies an IMBE Voice 10 (8 bits). */
   public static final int IMBE10 = 0xCB;

   /** Constant that identifies an IMBE Voice 11 (8 bits). */
   public static final int IMBE11 = 0xCC;

   /** Constant that identifies an IMBE Voice 12 + Encryption Sync (8 bits). */
   public static final int IMBEFRAME12 = 0x6D;

   /** Constant that identifies an IMBE Voice 13 + Encryption Sync (8 bits). */
   public static final int IMBEFRAME13 = 0x6E;

   /** Constant that identifies an IMBE Voice 14 + Encryption Sync (8 bits). */
   public static final int IMBEFRAME14 = 0x6F;

   /** Constant that identifies an IMBE Voice 15 + Encryption Sync (8 bits). */
   public static final int IMBEFRAME15 = 0x70;

   /** Constant that identifies an IMBE Voice 16 + Encryption Sync (8 bits). */
   public static final int IMBEFRAME16 = 0x71;

   /** Constant that identifies an IMBE Voice 17 + Encryption Sync (8 bits). */
   public static final int IMBEFRAME17 = 0x72;

   /** Constant that identifies an IMBE Voice 18 + Low Speed Data (8 bits). */
   public static final int IMBEFRAME18 = 0x73;

   public static int[] IMBE_FRAME_TYPES = {
      IMBE1,
      IMBE2,
      IMBE3,
      IMBE4,
      IMBE5,
      IMBE6,
      IMBE7,
      IMBE8,
      IMBEFRAME9,
      IMBE10,
      IMBE11,
      IMBEFRAME12,
      IMBEFRAME13,
      IMBEFRAME14,
      IMBEFRAME15,
      IMBEFRAME16,
      IMBEFRAME17,
      IMBEFRAME18
   };
   public static int getFrameType(int index) {
      // assume index: [1 - 18]
      int k = (index - 1) % 18;
      return IMBEVoiceBlock.IMBE_FRAME_TYPES[ k ]; 
   }
   public static int getIMBEVoiceIndex(int frameType) {
      for(int i=0; i < IMBE_FRAME_TYPES.length; i++) {
         if( frameType == IMBE_FRAME_TYPES[i])
            return i+1;
      }
      return 0;
   }


   //--------------------------------------------------------------------------
   // CAI Frame Types
   /** Constant that identifies an IMBE Voice 1 (8 bits). */
   public static final int CAI_IMBEFRAME1 = 0x62;

   /** Constant that identifies an IMBE Voice 2 (8 bits). */
   public static final int CAI_IMBEFRAME2 = 0x63;

   /** Constant that identifies an IMBE Voice 3 + Link Control (8 bits). */
   public static final int CAI_IMBEFRAME3 = 0x64;

   /** Constant that identifies an IMBE Voice 4 + Link Control (8 bits). */
   public static final int CAI_IMBEFRAME4 = 0x65;

   /** Constant that identifies an IMBE Voice 5 + Link Control (8 bits). */
   public static final int CAI_IMBEFRAME5 = 0x66;

   /** Constant that identifies an IMBE Voice 6 + Link Control (8 bits). */
   public static final int CAI_IMBEFRAME6 = 0x67;

   /** Constant that identifies an IMBE Voice 7 + Link Control (8 bits). */
   public static final int CAI_IMBEFRAME7 = 0x68;

   /** Constant that identifies an IMBE Voice 8 + Link Control (8 bits). */
   public static final int CAI_IMBEFRAME8 = 0x69;

   /** Constant that identifies an IMBE Voice 9 + Low Speed Data (8 bits). */
   public static final int CAI_IMBEFRAME9 = 0x6A;

   /** Constant that identifies an IMBE Voice 10 (8 bits). */
   public static final int CAI_IMBEFRAME10 = 0x6B;

   /** Constant that identifies an IMBE Voice 11 (8 bits). */
   public static final int CAI_IMBEFRAME11 = 0x6C;

   /** Constant that identifies an IMBE Voice 12 + Encryption Sync (8 bits). */
   public static final int CAI_IMBEFRAME12 = 0x6D;

   /** Constant that identifies an IMBE Voice 13 + Encryption Sync (8 bits). */
   public static final int CAI_IMBEFRAME13 = 0x6E;

   /** Constant that identifies an IMBE Voice 14 + Encryption Sync (8 bits). */
   public static final int CAI_IMBEFRAME14 = 0x6F;

   /** Constant that identifies an IMBE Voice 15 + Encryption Sync (8 bits). */
   public static final int CAI_IMBEFRAME15 = 0x70;

   /** Constant that identifies an IMBE Voice 16 + Encryption Sync (8 bits). */
   public static final int CAI_IMBEFRAME16 = 0x71;

   /** Constant that identifies an IMBE Voice 17 + Encryption Sync (8 bits). */
   public static final int CAI_IMBEFRAME17 = 0x72;

   /** Constant that identifies an IMBE Voice 18 + Low Speed Data (8 bits). */
   public static final int CAI_IMBEFRAME18 = 0x73;

   public static int[] CAI_FRAME_TYPES = {
      CAI_IMBEFRAME1,
      CAI_IMBEFRAME2,
      CAI_IMBEFRAME3,
      CAI_IMBEFRAME4,
      CAI_IMBEFRAME5,
      CAI_IMBEFRAME6,
      CAI_IMBEFRAME7,
      CAI_IMBEFRAME8,
      CAI_IMBEFRAME9,
      CAI_IMBEFRAME10,
      CAI_IMBEFRAME11,
      CAI_IMBEFRAME12,
      CAI_IMBEFRAME13,
      CAI_IMBEFRAME14,
      CAI_IMBEFRAME15,
      CAI_IMBEFRAME16,
      CAI_IMBEFRAME17,
      CAI_IMBEFRAME18
   };

   public static int getCAIFrameType(int index) {
      // assume index: [1 - 18]
      int k = (index - 1) % 18;
      return IMBEVoiceBlock.CAI_FRAME_TYPES[ k ]; 
   }
   public static int getCAIVoiceIndex(int frameType) {
      for(int i=0; i < CAI_FRAME_TYPES.length; i++) {
         if( frameType == CAI_FRAME_TYPES[i])
            return i+1;
      }
      return 0;
   }

   //--------------------------------------------------------------------------
   /** Frame type (8 bits). */
   private int FT = 0;

   /** Message vector 1 of 8 (12 bits). */
   private int U0 = 0;

   /** Message vector 2 of 8 (12 bits). */
   private int U1 = 0;

   /** Message vector 3 of 8 (12 bits). */
   private int U2 = 0;

   /** Message vector 4 of 8 (12 bits). */
   private int U3 = 0;

   /** Message vector 5 of 8 (11 bits). */
   private int U4 = 0;

   /** Message vector 6 of 8 (11 bits). */
   private int U5 = 0;

   /** Message vector 7 of 8 (11 bits). */
   private int U6 = 0;

   /** Message vector 8 of 8 (7 bits). */
   private int U7 = 0;

   /** Total errors detected in the frame (3 bits) */
   private int Et = 0;

   /** NOT SURE WHAT THIS IS (3 bits)! */
   private int Er = 0;

   /** Mute frame flag (1 bit) */
   private int M = 0;

   /** Lost frame flag (1 bit) */
   private int L = 0;

   /** Number of errors detected in vector U4 (1 bit) */
   private int E4 = 0;

   /** Number of errors detected in vector U1 (3 bits) */
   private int E1 = 0;

   /** Superframe counter (2 bits). */
   private int SF = 0;

   /** Reserved (2 bits). */
   private int reserved;

   /** Additional frame data (variable bits). */
   private byte[] additionalFrameData = null;

   /** The voice data */
   private byte[] U2_U7_bytes;
   
   /**
    * Constructs a P25 IMBE Voice block.
    */
   public IMBEVoiceBlock() {
   }
   
   /**
    * Construct an IMBE voice block given its byte array representation.
    * 
    * @param bytes The byte array representation of this IMBE voice block.
    * @throws P25BlockException
    */
   public IMBEVoiceBlock(byte[] bytes) {
      
      int offset = 0;

      // Extract FT..U1.
      int FT_U1_length = 4;
      byte[] FT_U1_bytes = new byte[FT_U1_length];
      System.arraycopy(bytes, offset, FT_U1_bytes, 0, FT_U1_length);
      int FT_U1 = ByteUtil.bytesToInt(FT_U1_bytes);
      offset += FT_U1_length;

      this.FT = (short) ((FT_U1 >>> 0x18) & 0xFF);
      this.U0 = (short) ((FT_U1 >>> 0x0C) & 0xFFF);
      this.U1 = (short) (FT_U1 & 0xFFF);

      // Extract U2..U7.
      int U2_U7_length = 8;
      this.U2_U7_bytes = new byte[U2_U7_length];
      System.arraycopy(bytes, offset, U2_U7_bytes, 0, U2_U7_length);
      long U2_U7 = ByteUtil.bytesToLong(U2_U7_bytes);
      offset += U2_U7_length;

      this.U2 = (short) ((U2_U7 >>> 0x34) & 0xFFF);
      this.U3 = (short) ((U2_U7 >>> 0x28) & 0xFFF);
      this.U4 = (short) ((U2_U7 >>> 0x1D) & 0x7FF);
      this.U5 = (short) ((U2_U7 >>> 0x12) & 0x7FF);
      this.U6 = (short) ((U2_U7 >>> 0x07) & 0x7FF);
      this.U7 = (byte) (U2_U7 & 0x7F);

      // Extract Et..Rsvd.
      int Et_Rsvd_length = 2;
      byte[] Et_Rsvd_bytes = new byte[Et_Rsvd_length];
      System.arraycopy(bytes, offset, Et_Rsvd_bytes, 0, Et_Rsvd_length);
      short Et_Rsvd = ByteUtil.bytesToShort(Et_Rsvd_bytes);
      offset += Et_Rsvd_length;

      this.Et = (byte) ((Et_Rsvd >>> 0x0D) & 0x07);
      this.Er = (byte) ((Et_Rsvd >>> 0x0A) & 0x07);
      this.M = (byte) ((Et_Rsvd >>> 0x09) & 0x01);
      this.L = (byte) ((Et_Rsvd >>> 0x08) & 0x01);
      this.E4 = (byte) ((Et_Rsvd >>> 0x07) & 0x01);
      this.E1 = (byte) ((Et_Rsvd >>> 0x04) & 0x07);
      this.SF = (byte) ((Et_Rsvd >>> 0x02) & 0x03);
      this.reserved = (byte) (Et_Rsvd & 0x03);

      // Finally extract the variable length data at the end of the block.
      if (bytes.length > offset) {
         
         // There must be additional frame data
         this.additionalFrameData = 
            new byte[bytes.length - offset];
         
         System.arraycopy(bytes, offset, this.additionalFrameData, 
            0, this.additionalFrameData.length);
      }
   }

   /**
    * Get the size of this IMBE Voice block in bytes.
    * 
    * @return the size of this IMBE Voice block in bytes.
    */
   public int size() {
      if (additionalFrameData != null)
         return FIXED_LENGTH + additionalFrameData.length;
      else
         return FIXED_LENGTH;
   }

   /**
    * Get the IMBE voice block as a byte array.
    * 
    * @return The IMBE voice block byte array.
    */
   public byte[] getBytes() {

      // Since FT..U1 are 32 bits, populate an int with FT..U1.
      int FT_U1 = 0;
      FT_U1 |= FT;
      FT_U1 <<= 0x0C;
      FT_U1 |= (U0 & 0xFFF);
      FT_U1 <<= 0x0C;
      FT_U1 |= (U1 & 0xFFF);
      byte[] FT_U1_bytes = ByteUtil.intToBytes(FT_U1);

      // Since U2..U7 are 64 bits, populate a long with U2..U7.
      long U2_U7 = 0;
      U2_U7 |= (U2 & 0xFFF);
      U2_U7 <<= 0x0C;
      U2_U7 |= (U3 & 0xFFF);
      U2_U7 <<= 0x0B;
      U2_U7 |= (U4 & 0x7FF);
      U2_U7 <<= 0x0B;
      U2_U7 |= (U5 & 0x7FF);
      U2_U7 <<= 0x0B;
      U2_U7 |= (U6 & 0x7FF);
      U2_U7 <<= 0x07;
      U2_U7 |= (U7 & 0x7F);
      byte[] U2_U7_bytes = ByteUtil.longToBytes(U2_U7);

      // Since Et..Rsvd are 16 bits, populate a short with Et..Rsvd.
      short Et_Rsvd = 0;
      Et_Rsvd |= (Et & 0x07);
      Et_Rsvd <<= 0x03;
      Et_Rsvd |= (Er & 0x07);
      Et_Rsvd <<= 0x01;
      Et_Rsvd |= (M & 0x01);
      Et_Rsvd <<= 0x01;
      Et_Rsvd |= (L & 0x01);
      Et_Rsvd <<= 0x01;
      Et_Rsvd |= (E4 & 0x01);
      Et_Rsvd <<= 0x03;
      Et_Rsvd |= (E1 & 0x07);
      Et_Rsvd <<= 0x02;
      Et_Rsvd |= (SF & 0x03);
      Et_Rsvd <<= 0x02;
      Et_Rsvd |= (reserved & 0x03);
      byte[] Et_Rsvd_bytes = ByteUtil.shortToBytes(Et_Rsvd);

      // Now add all data together in one contiguous byte array
      int total_length = FT_U1_bytes.length + U2_U7_bytes.length
            + Et_Rsvd_bytes.length;

      if (additionalFrameData != null) {      
         total_length += additionalFrameData.length;         
      }
         
      byte[] bytes = new byte[total_length];
      int offset = 0;
      System.arraycopy(FT_U1_bytes, 0, bytes, offset, FT_U1_bytes.length);

      offset += FT_U1_bytes.length;
      System.arraycopy(U2_U7_bytes, 0, bytes, offset, U2_U7_bytes.length);

      offset += U2_U7_bytes.length;
      System.arraycopy(Et_Rsvd_bytes, 0, bytes, offset, Et_Rsvd_bytes.length);

      if (additionalFrameData != null) {
         offset += Et_Rsvd_bytes.length;
         System.arraycopy(additionalFrameData, 0, bytes, offset,
            additionalFrameData.length);         
      }
      return bytes;
   }

   /**
    * Set the frame type.
    * 
    * @param i
    *            The frame type (8 bits)
    * @throws IllegalArgumentException
    */
   public void setFT(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(8))) {
         FT = i;
      }
      else {
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
      }
   }

   /**
    * Get the frame type.
    * 
    * @return The frame type.
    */
   public int getFT() {
      return FT;
   }

   /**
    * Set message vector 1.
    * 
    * @param i
    *            Message vector 1 (12 bits)
    * @throws IllegalArgumentException
    */
   public void setU0(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(12)))
         U0 = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get message vector 1.
    * 
    * @return Message vector 1.
    */
   public int getU0() {
      return U0;
   }

   /**
    * Set message vector 2.
    * 
    * @param i
    *            Message vector 2 (12 bits)
    * @throws IllegalArgumentException
    */
   public void setU1(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(12)))
         U1 = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get message vector 2.
    * 
    * @return Message vector 2.
    */
   public int getU1() {
      return U1;
   }

   /**
    * Set message vector 3.
    * 
    * @param i
    *            Message vector 3 (12 bits)
    * @throws IllegalArgumentException
    */
   public void setU2(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(12)))
         U2 = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get message vector 3.
    * 
    * @return Message vector 3.
    */
   public int getU2() {
      return U2;
   }

   /**
    * Set message vector 4.
    * 
    * @param i
    *            Message vector 4 (12 bits)
    * @throws IllegalArgumentException
    */
   public void setU3(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(12)))
         U3 = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get message vector 4.
    * 
    * @return Message vector 4.
    */
   public int getU3() {
      return U3;
   }

   /**
    * Set message vector 5.
    * 
    * @param i
    *            Message vector 5 (11 bits)
    * @throws IllegalArgumentException
    */
   public void setU4(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(11)))
         U4 = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get message vector 5.
    * 
    * @return Message vector 5.
    */
   public int getU4() {
      return U4;
   }

   /**
    * Set message vector 6.
    * 
    * @param i
    *            Message vector 6 (11 bits)
    * @throws IllegalArgumentException
    */
   public void setU5(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(11)))
         U5 = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get message vector 6.
    * 
    * @return Message vector 6.
    */
   public int getU5() {
      return U5;
   }

   /**
    * Set message vector 7.
    * 
    * @param i
    *            Message vector 7 (11 bits)
    * @throws IllegalArgumentException
    */
   public void setU6(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(11)))
         U6 = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get message vector 7.
    * 
    * @return Message vector 7.
    */
   public int getU6() {
      return U6;
   }

   /**
    * Set message vector 8.
    * 
    * @param i
    *            Message vector 8 (7 bits)
    * @throws IllegalArgumentException
    */
   public void setU7(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(7)))
         U7 = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get message vector 8.
    * 
    * @return Message vector 8.
    */
   public int getU7() {
      return U7;
   }

   /**
    * Set the total errors detected in the frame.
    * 
    * @param i
    *            The total errors detected in the frame (3 bits)
    * @throws IllegalArgumentException
    */
   public void setEt(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(3)))
         Et = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the total errors detected in the frame.
    * 
    * @return The total errors detected in the frame.
    */
   public int getEt() {
      return Et;
   }

   /**
    * Set Er.
    * 
    * @param i
    *            Er (3 bits)
    * @throws IllegalArgumentException
    */
   public void setEr(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(3)))
         Er = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get Er (not sure what this is).
    * 
    * @return Er.
    */
   public int getEr() {
      return Er;
   }

   /**
    * Set the mature frame flag.
    * 
    * @param i
    *            The mature frame flag (1 bit)
    * @throws IllegalArgumentException
    */
   public void setM(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         M = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the mature frame flag.
    * 
    * @return The mature frame flag.
    */
   public int getM() {
      return M;
   }

   /**
    * Set the lost frame flag.
    * 
    * @param i
    *            The lost frame flag (1 bit)
    * @throws IllegalArgumentException
    */
   public void setL(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         L = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the lost frame flag.
    * 
    * @return The lost frame flag.
    */
   public int getL() {
      return L;
   }

   /**
    * Set number of errors detected in vector U4.
    * 
    * @param i
    *            The number of errors detected in vector U4 (1 bit)
    * @throws IllegalArgumentException
    */
   public void setE4(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         E4 = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the number of errors detected in vector U4.
    * 
    * @return The number of errors detected in vector U4.
    */
   public int getE4() {
      return E4;
   }

   /**
    * Set the number of errors detected in vector U1.
    * 
    * @param i
    *            The number of errors detected in vector U1 (3 bits)
    * @throws IllegalArgumentException
    */
   public void setE1(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(3)))
         E1 = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the number of errors detected in vector U1.
    * 
    * @return The number of errors detected in vector U1.
    */
   public int getE1() {
      return E1;
   }

   /**
    * Set the superframe counter.
    * 
    * @param i
    *            The superframe counter (2 bits)
    * @throws IllegalArgumentException
    */
   public void setSF(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(2)))
         SF = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the superframe counter.
    * 
    * @return The superframe counter.
    */
   public int getSF() {
      return SF;
   }

   /**
    * Set the reserved field.
    * 
    * @param i
    *            The reserved field (2 bits)
    * @throws IllegalArgumentException
    */
   public void setReserved(int i) throws IllegalArgumentException {

      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(2)))
         reserved= i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the reserved field.
    * 
    * @return The reserved field.
    */
   public int getReserved() {
      return reserved;
   }

   // CAI Blocks
   public void setB(int i) throws IllegalArgumentException {
      setReserved( i);
   } 
   public int getB() {
      return getReserved();
   }

   /**
    * Set the additional frame data. The additional frame data will have
    * variable length.
    * 
    * @param bytes
    *            The value of SF.
    * @throws P25Exception
    */
   public void setAdditionalFrameData(byte[] bytes) {       
      additionalFrameData = bytes;      
   }

   /**
    * Get the additional frame data.
    * 
    * @return The additional frame data.
    */
   public byte[] getAdditionalFrameData() {      
      return additionalFrameData;     
   }
   
   /**
    * Get the XML formatted string representation.
    * 
    * @return The XML formatted string representation.
    */
   public String toISSIString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("\tFrame Type: ");
//TODO: need text label for Frame Type
      sbuf.append(DataCoder.toHex(FT,2));
      sbuf.append("\n\tVoice Frame: ");
      sbuf.append(DataCoder.toHex(U2_U7_bytes,22));
      sbuf.append("\n\tET: ");
      sbuf.append(DataCoder.toIntegerBinary(Et,3));
      sbuf.append("\n\tER: ");
      sbuf.append(DataCoder.toIntegerBinary(Er,3));
      sbuf.append("\n\tMute Frame: ");
      sbuf.append(DataCoder.toBinary(M));
      sbuf.append("\n\tLost Frame: ");
      sbuf.append(DataCoder.toBinary(L));
      sbuf.append("\n\tE4: ");
      sbuf.append(DataCoder.toBinary(E4));
      sbuf.append("\n\tE1: ");
      sbuf.append(DataCoder.toBinary(E1,3));
      sbuf.append("\n\tFree Running Super Frame Counter: ");
      sbuf.append(DataCoder.toIntegerBinary(SF,2));
      sbuf.append("\n\tReserved: ");
      sbuf.append(DataCoder.toBinary(reserved,2));
      if(additionalFrameData != null) {
         sbuf.append("\n\tAdditionalFrameData:\"");
         sbuf.append(ByteUtil.writeBytes(additionalFrameData) + "\"");
      }
      return sbuf.toString();
   }
   
   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("\n<imbe-voice");
      sbuf.append("\n frameType=\"" + this.FT + "\"");
      sbuf.append("\n voiceFrame=\"" + ByteUtil.writeBytes(this.U2_U7_bytes) + "\"");
      sbuf.append("\n ET=\"" + this.Et + "\"");
      sbuf.append("\n ER=\"" + this.Er + "\"");
      sbuf.append("\n muteFrame=\"" + this.M + "\"");
      sbuf.append("\n lostFrame=\"" + this.L + "\"");
      sbuf.append("\n E4=\"" + this.E4 + "\"");
      sbuf.append("\n E1=\"" + this.E1 + "\""); 
      sbuf.append("\n SF=\"" + this.SF + "\"");
      sbuf.append("\n reserved=\"" + this.reserved + "\"");
      sbuf.append(additionalFrameData != null ?
         "\n additionalFrameData=\"" + ByteUtil.writeBytes(additionalFrameData) + "\"" : "");
      sbuf.append("\n/>"); 
      return sbuf.toString();
   }

   /**
    * Compare this object with another object.
    * 
    * @param other The other object to be compared to.
    * @return True if this object is equivalent to the other object.
    */
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof IMBEVoiceBlock))
         return false;
      IMBEVoiceBlock that = (IMBEVoiceBlock) other;
      return that.FT == FT && that.U0 == U0 
            && that.U1 == U1 && that.U2 == U2
            && that.U3 == U3 && that.U4 == U4 && that.U5 == U5
            && that.U6 == U6 && that.U7 == U7 && that.Et == Et
            && that.Er == Er && that.M == M
            && that.L == L && that.E4 == E4 && that.E1 == E1
            && that.SF == SF && that.reserved == reserved;
   }
}
