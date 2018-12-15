//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;
//import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

import gov.nist.p25.common.util.DataCoder;
import gov.nist.p25.issi.utils.ByteUtil;

/**
 * This class implements a P25 ISSI Header Word. The P25 ISSI Header Word has
 * the following format:
 * <p>
 * 
 * <pre>
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                   Message Indicator (72 bits)                 |
 * |                                                               |
 * +               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |               |     AlgID     |           Key ID              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     MFID      |            Group ID           |   NID (15-8)  |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |   NID (7-0)   |SF |VBB| Rsvd  |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               |
 * </pre>
 * 
 */
public class ISSIHeaderWord implements Serializable {

   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(ISSIHeaderWord.class);

   /** Constant that identifies the total byte length of fixed fields. */
   protected final static int FIXED_LENGTH = 18;

   /** Fixed length for message indicator. */
   public final static int MAX_MSG_INDICATOR_LENGTH = 9; // in bytes


   /** The message indicator (72 bits). */
   private byte[] messageIndicator = new byte[MAX_MSG_INDICATOR_LENGTH];

   /** The AlgID (8 bits). */
   private int algId = 0;

   /** The key ID (16 bits). */
   private int keyId = 0;

   /** The MFID (8 bits). */
   private int MFID = 0;

   /** The group ID (16 bits). */
   private int groupId = 0;   // Group ID=0 means SU-to-SU (i.e., no group)

   /** The NID (16 bits). */
   private int NID = 0;

   /** The SF (2 bits). */
   private int SF = 0;
   
   /** EC: The VBB (2 bits). */
   private int VBB = 0;

   /** Reserved (6 bits). */
   /** EC: Reserved (4 bits). */
   private int reserved = 0;

   /**
    * Constructs a P25 ISSI header word.
    */
   public ISSIHeaderWord() {
   }
   
   /**
    * Construct an ISSI Header Word given its byte array representation.
    * 
    * @param bytes The byte array representation of this ISSI Header Word.
    * @throws P25BlockException
    */
   public ISSIHeaderWord(byte[] bytes) {
      
      // Extract message indicator
      this.messageIndicator = new byte[MAX_MSG_INDICATOR_LENGTH];
      System.arraycopy(bytes, 0, this.messageIndicator, 0,
            MAX_MSG_INDICATOR_LENGTH);
      int offset = MAX_MSG_INDICATOR_LENGTH;

      // Extract Alg_ID
      int Alg_ID_length = 1;
      this.algId = (bytes[offset] & 0xFF);
      offset += Alg_ID_length;

      // Extract Key_ID
      int Key_ID_length = 2;
      byte[] Key_ID_bytes = new byte[Key_ID_length];
      System.arraycopy(bytes, offset, Key_ID_bytes, 0, Key_ID_length);
      this.keyId = ByteUtil.bytesToShort(Key_ID_bytes) & 0xFFFF;
      offset += Key_ID_length;

      // Extract MFID
      int MFID_length = 1;
      this.MFID = bytes[offset] & 0xFF;
      offset += MFID_length;

      // Extract Group_ID
      int Group_ID_length = 2;
      byte[] Group_ID_bytes = new byte[Group_ID_length];
      System.arraycopy(bytes, offset, Group_ID_bytes, 0, Group_ID_length);
      this.groupId = ByteUtil.bytesToShort(Group_ID_bytes) & 0xFFFF;
      offset += Group_ID_length;

      // Extract NID
      int NID_length = 2;
      byte[] NID_bytes = new byte[NID_length];
      System.arraycopy(bytes, offset, NID_bytes, 0, NID_length);
      this.NID = ByteUtil.bytesToShort(NID_bytes) & 0xFFFF;
      offset += NID_length;

      // Extract SF and Reserved
      byte SF_Reserved_byte = bytes[offset];
      //==this.SF = ((SF_Reserved_byte >>> 0x06) & 0x03);
      this.SF = (SF_Reserved_byte & 0xC0) >> 0x06;
      
      // EHC: need to extract VBB and fix rsvd
      //==this.rsvd = (SF_Reserved_byte & 0x3F);
      this.VBB = (SF_Reserved_byte & 0x3F) >> 0x04;       
      this.reserved = (SF_Reserved_byte & 0x0F);
   }

   /**
    * Get the size of this ISSI header word in bytes.
    * 
    * @return The size of this ISSI header word in bytes.
    */
   public int size() {
      return FIXED_LENGTH;
   }

   /**
    * Get the ISSI header word as a byte array.
    * 
    * @return The ISSI header word byte array.
    */
   public byte[] getBytes() {

      // Since Alg_ID..Key_ID are 24 bits, create a 3 byte array for V..SN.
      byte[] A_K_bytes = new byte[3];
      A_K_bytes[0] = (byte) algId;
      A_K_bytes[1] = (byte) ((keyId >>> 0x08) & 0xFF);
      A_K_bytes[2] = (byte) (keyId & 0xFF);

      // Since MFID..Group_ID are 24 bits, create a 3 byte array for
      // MFID..Group_ID.
      byte[] M_G_bytes = new byte[3];
      M_G_bytes[0] = (byte) MFID;
      M_G_bytes[1] = (byte) ((groupId >>> 0x08) & 0xFF);
      M_G_bytes[2] = (byte) (groupId & 0xFF);

      // Since NID is 16 bits, create a 2 byte array for NID
      byte[] N_bytes = new byte[2];
      N_bytes[0] = (byte) ((NID >>> 0x08) & 0xFF);
      N_bytes[1] = (byte) (NID & 0xFF);

      // Since SF_Reserved is 1 byte, create a 1 byte array for SF_Reserved
      byte[] S_r_bytes = new byte[1];
      S_r_bytes[0] |= SF;
      S_r_bytes[0] <<= 0x06;
      
      //EHC: add VBB      
      S_r_bytes[0] |= (VBB << 0x04);      
      S_r_bytes[0] |= (reserved & 0x0F);

      // Now add all data together in one contiguous byte array
      int totalLength = messageIndicator.length + A_K_bytes.length
            + M_G_bytes.length + N_bytes.length + S_r_bytes.length;

      byte[] bytes = new byte[totalLength];
      int offset = 0;
      System.arraycopy(messageIndicator, 0, bytes, offset,
            messageIndicator.length);
      offset += messageIndicator.length;

      System.arraycopy(A_K_bytes, 0, bytes, offset, A_K_bytes.length);
      offset += A_K_bytes.length;

      System.arraycopy(M_G_bytes, 0, bytes, offset, M_G_bytes.length);
      offset += M_G_bytes.length;

      System.arraycopy(N_bytes, 0, bytes, offset, N_bytes.length);
      offset += N_bytes.length;

      System.arraycopy(S_r_bytes, 0, bytes, offset, S_r_bytes.length);

      return bytes;
   }

   /**
    * Set the message indicator.
    * 
    * @param bytes
    *            The message indicator.
    * @throws IllegalArgumentException
    */
   public void setMessageIndicator(byte[] bytes)
         throws IllegalArgumentException {
      if (bytes.length <= MAX_MSG_INDICATOR_LENGTH)
         System.arraycopy(bytes, 0, messageIndicator, 0, bytes.length);
      else
         throw new IllegalArgumentException(
               P25BlockException.INCORRECT_BYTE_LENGTH);
   }

   /**
    * Get the message indicator.
    * 
    * @return The message indicator.
    */
   public byte[] getMessageIndicator() {
      return messageIndicator;
   }

   /**
    * Set the algorithm ID.
    * 
    * @param i
    *            The algorithm ID ( 8 bits)
    * @throws IllegalArgumentException
    */
   public void setAlgId(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(8)))
         algId = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the algorithm ID.
    * 
    * @return The algorithm ID.
    */
   public int getAlgId() {
      return algId;
   }

   /**
    * Set the key ID.
    * 
    * @param i
    *            The key ID (16 bits)
    * @throws IllegalArgumentException
    */
   public void setKeyId(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(16)))
         keyId = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the key ID.
    * 
    * @return The key ID.
    */
   public int getKeyId() {
      return keyId;
   }

   /**
    * Set the manufacturer ID.
    * 
    * @param i
    *            The manufacturer ID (8 bits)
    * @throws IllegalArgumentException
    */
   public void setMFID(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(8)))
         MFID = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the manufacturer ID.
    * 
    * @return The manufacturer ID.
    */
   public int getMFID() {
      return MFID;
   }

   /**
    * Set the group ID.
    * 
    * @param i
    *            The group ID (16 bits)
    * @throws IllegalArgumentException
    */
   public void setGroupId(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(16)))
         groupId = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE
               + " : " + i + " max value = " + ByteUtil.getMaxIntValueForNumBits(16));
   }
   
   /**
    * Get the group ID.
    * 
    * @return The group ID.
    */
   public int getGroupId() {
      return groupId;
   }

   /**
    * Set the network ID.
    * 
    * @param i The network ID (16 bits)
    * @throws IllegalArgumentException
    */
   public void setNID(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(16)))
         NID = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the network ID.
    * 
    * @return The network ID.
    */
   public int getNID() {
      return NID;
   }

   /**
    * Set the superframe counter.  SF is a 2 bit free running value that is
    * initilized to 0 and incremented by one on the first frame of each
    * superframe.
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
    * Get the superframe counter..
    * 
    * @return The superframe counter.
    */
   public int getSF() {
      return SF;
   }

   // 
   public void setVBB(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(2)))
         VBB = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   public int getVBB() {
      return VBB;
   }
   
   /**
    * Set the reserved field.
    * 
    * @param i
    *            The reserved field (4 bits)
    * @throws IllegalArgumentException
    */
   public void setReserved(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(4)))
         reserved = i;
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
   
   /**
    * Compare this object with another object.
    * 
    * @param other The other object to be compared to.
    * @return True if this object is equivalent to the other object.
    */
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof ISSIHeaderWord))
         return false;
      ISSIHeaderWord that = (ISSIHeaderWord) other;
      for (int i = 0; i < MAX_MSG_INDICATOR_LENGTH; i++) {
         if (that.messageIndicator[i] != this.messageIndicator[i])
            return false;
      }
      return this.algId == that.algId && this.groupId == that.groupId
            && this.keyId == that.keyId && this.MFID == that.MFID
            && this.NID == that.NID && this.SF == that.SF
            && this.VBB == that.VBB
            && this.reserved == that.reserved;
   }

   /**
    * Get the XML formatted string representation.
    * 
    * @return The XML formatted string representation.
    */
   public String toISSIString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("\tMessage Indicator: ");
      sbuf.append(DataCoder.toHex(messageIndicator,18));
      sbuf.append("\n\tAlgID: ");
      sbuf.append(DataCoder.toHex(algId,2));
      sbuf.append("\n\tKeyID: ");
      sbuf.append(DataCoder.toHex(keyId,4));
      sbuf.append("\n\tMFID: ");
      sbuf.append(DataCoder.toHex(MFID,2));
      sbuf.append("\n\tGroupID: ");
      sbuf.append(DataCoder.toHex(groupId,4));
      sbuf.append("\n\tNetworkID: ");
      sbuf.append(DataCoder.toHex(NID,6));
      sbuf.append("\n\tFree Running Super Frame Counter: ");
      sbuf.append(DataCoder.toBinary(SF,2));
      sbuf.append("\n\tVBB: ");
      sbuf.append(DataCoder.toBinary(VBB,2));
      sbuf.append("\n\tReserved: ");
      sbuf.append(DataCoder.toBinary(reserved,4));
      return sbuf.toString();
   }
   
   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("<issi-header-word");
      sbuf.append("\n messageIndicator=\"");
      sbuf.append(ByteUtil.writeBytes(messageIndicator));
      sbuf.append("\"");
      sbuf.append("\n algId=\"" + algId + "\"");
      sbuf.append("\n keyId=\"" + keyId + "\"");
      sbuf.append("\n manufacturerID= \"" + MFID + "\"");
      sbuf.append("\n groupID=\"" + groupId + "\"");
      sbuf.append("\n networkID=\"" + NID + "\"");
      sbuf.append("\n superFrameCounter=\"" + SF + "\"");      
      sbuf.append("\n vbb=\"" + VBB + "\"");
      sbuf.append("\n reserved=\"" + reserved + "\"");
      sbuf.append("\n/>");
      return sbuf.toString();
   }
   
   public ISSIHeaderWord createFromAttributes(Attributes attrs) {
      ISSIHeaderWord retval = new ISSIHeaderWord();
      retval.messageIndicator = ByteUtil.readBytes(attrs.getValue("messageIndicator"));
      retval.algId = Integer.parseInt(attrs.getValue("algId"));
      retval.keyId = Integer.parseInt(attrs.getValue("keyId"));
      retval.MFID = Integer.parseInt(attrs.getValue("manufacturerID"));
      retval.groupId = Integer.parseInt(attrs.getValue("groupID"));
      retval.NID = Integer.parseInt(attrs.getValue("networkID"));
      retval.SF = Integer.parseInt(attrs.getValue("superFrameCounter"));    
      retval.VBB = Integer.parseInt(attrs.getValue("vbb"));
      retval.reserved = Integer.parseInt(attrs.getValue("reserved"));
      return retval;         
   }
}
