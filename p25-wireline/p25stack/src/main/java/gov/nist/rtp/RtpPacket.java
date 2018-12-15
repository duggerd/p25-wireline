//
package gov.nist.rtp;

import java.io.Serializable;
import gov.nist.p25.issi.utils.ByteUtil;
import org.apache.log4j.Logger;

/**
 * This class implements a RTP packet as defined in <a
 * href="http://www.ietf.org/rfc/rfc3550.txt">IETF RFC 3550</a> with the
 * following exceptions:
 * <P>
 * 1. No CSRC support.<BR>
 * 2. No header extension<BR>
 * <p>
 * Future versions of this class may support CSRC and RTP header extensions.
 * 
 * The RTP header has the following format:
 * <p>
 * 
 * <pre>
 *         0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *         |V=2|P|X|  CC   |M|     PT      |     sequence number (SN)      |
 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *         |                         timestamp (TS)                        |
 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *         |           synchronization source (SSRC) identifier            |
 *         +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 *         |            contributing source (CSRC) identifiers             |
 *         |                             ....                              |
 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 */
public class RtpPacket implements Serializable {
   
   private static final long serialVersionUID = -1L;
   
   /** The logger for this class. */
   private static Logger logger = Logger.getLogger(RtpPacket.class);

   //private static final String HEADER_START = "{\\insrsid15927085 ";
   //private static final String IGNORED_FIELD = "}{\\i\\fs16\\insrsid15927085 ";
   //private static final String HEADER_END = "\\par }\n";
   //private static final String HEADER_RESTART = "} " + HEADER_START;
   //private static final String END_MESSAGE = "{\\par}}\n";
   //public static final String LINE_FEED = "{\\par}\n";

   /** Constant that identifies the total byte length of fixed fields. */
   public final static int FIXED_HEADER_LENGTH = 12; // V..SSRC only

   /** The maximum buffer (byte array) size for payload data. */
   public static final int MAX_PAYLOAD_BUFFER_SIZE = 512;

   /*
    * Note that each of the following use data types that are larger than what
    * is required. The motivation here is to the keep two's complement negative
    * bit separate from the bits that represent the value.
    */

   /** Version number (2 bits). */
   private int V = 2;

   /** Padding (1 bit). */
   private int P = 0;

   /** Header extension (1 bit). */
   private int X = 0;

   /** CSRC count (4 bits). */
   private int CC = 0;

   /** Marker (1 bit). */
   private int M = 0;

   /** Payload type (7 bits). */
   private int PT = 0;

   /** Sequence number (16 bits). */
   private int SN = 0;

   /** Time stamp (32 bits). */
   private long TS = 0;

   /** Synchronization source (32 bits). */
   private long SSRC = 0;

   /** Contributing sources (32 bits) -- not supported yet. */
   // private CSRC;
   /** Header extension Defined By Profile (16 bits) -- not supported yet. */
   // private short DP = 0;
   /** Header extension length (16 bits) -- not supported yet. */
   // private short EL = 0;
   /** The payload. Transient because we are only interested in the headers for comparing.*/
   private transient byte[] payload = null;

   /** The length of the payload. */
   private int payloadLength = 0;

   /**
    * Construct an RTP packet.
    */
   public RtpPacket() {
   }

   /**
    * Set this RTP packet with the given byte array.
    * 
    * @param bytes
    *            The byte array for populating this RTP packet.
    * @param length
    *            The number of bytes to read from the byte array.
    */
   public RtpPacket(byte[] bytes, int length) {
      setData(bytes, length);
   }

   /**
    * Get the data of this RTP packet as a byte array.
    * 
    * @return The data of this RTP packet as a byte array.
    */
   /*
    * (steveq) Note that we use the same convention for the method name as used
    * in DatagramPacket.getData(byte[]).
    */
   public byte[] getData() {

      /* Since V..SN are 32 bits, create a (int) byte array for V..SN. */
      int V_SN = 0;
      V_SN |= V; // Add V
      V_SN <<= 0x01; // Make room for P
      V_SN |= P; // Add P
      V_SN <<= 0x01; // Make room for X
      V_SN |= X; // Add X
      V_SN <<= 0x04; // Make room for CC
      V_SN |= CC; // Add CC
      V_SN <<= 0x01; // Make room for M
      V_SN |= M; // Add M
      V_SN <<= 0x07; // Make room for PT
      V_SN |= PT; // Add PT
      V_SN <<= 0x10; // Make room for SN
      V_SN |= SN; // Add SN
      byte[] V_SN_bytes = ByteUtil.intToBytes(V_SN);

      /*
       * Create a byte array for TS. Cast from long to int (we won't lose
       * precision because there are never more than 4 bytes of data).
       */
      byte[] TS_bytes = ByteUtil.intToBytes((int) TS);

      /*
       * Create a byte array for SSRC. Cast from long to int (we won't lose
       * precision because there are never more than 4 bytes of data).
       */
      byte[] SSRC_bytes = ByteUtil.intToBytes((int) SSRC);

      /* Create byte array for all data. */
      int length = V_SN_bytes.length + TS_bytes.length + SSRC_bytes.length
            + payloadLength;

      byte[] data = new byte[length];

      int offset = 0;
      System.arraycopy(V_SN_bytes, 0, data, offset, V_SN_bytes.length);

      offset += V_SN_bytes.length;
      System.arraycopy(TS_bytes, 0, data, offset, TS_bytes.length);

      offset += TS_bytes.length;
      System.arraycopy(SSRC_bytes, 0, data, offset, SSRC_bytes.length);

      offset += SSRC_bytes.length;
      System.arraycopy(payload, 0, data, offset, payloadLength);

      if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
         logger.debug("[RTPPacket] Packing V: " + V);
         logger.debug("[RTPPacket] Packing P: " + P);
         logger.debug("[RTPPacket] Packing X: " + X);
         logger.debug("[RTPPacket] Packing CC: " + CC);
         logger.debug("[RTPPacket] Packing M: " + M);
         logger.debug("[RTPPacket] Packing PT: " + PT);
         logger.debug("[RTPPacket] Packing SN: " + SN);
         logger.debug("[RTPPacket] Packing TS: " + TS);
         logger.debug("[RTPPacket] Packing SSRC: " + SSRC);
         logger.debug("[RTPPacket] Packing payload: " + ByteUtil.writeBytes(payload));
         logger.debug("[RTPPacket] Packed: " + ByteUtil.writeBytes(data));
      }
      return data;
   }

   /**
    * Set the data for this RTP packet. Note that CSRC, DP, and EL are not
    * included.
    * 
    * @param bytes
    *            The buffer containing the RTP data.
    * @param length
    *            The number of bytes in the buffer containing the RTP data.
    * @throws IllegalArgumentException
    */
   /*
    * (steveq) Note that we use the same convention for the method name as used
    * in DatagramPacket.setData(byte[], length).
    */
   public void setData(byte[] bytes, int length)
         throws IllegalArgumentException {
      /*
       * Since V..SN are 32 bits, build an int to hold V..SN before
       * extracting.
       */
      int V_SN_length = 4; // # bytes
      byte[] V_SN_bytes = new byte[V_SN_length];
      System.arraycopy(bytes, 0, V_SN_bytes, 0, V_SN_length);

      /* Extract V..SN */
      int V_SN = ByteUtil.bytesToInt(V_SN_bytes);
      V = (V_SN >>> 0x1E) & 0x03;
      P = (V_SN >>> 0x1D) & 0x01;
      X = (V_SN >>> 0x1C) & 0x01;
      CC = (V_SN >>> 0x18) & 0x0F;
      M = (V_SN >>> 0x17) & 0x01;
      PT = (V_SN >>> 0xF) & 0x7F;
      SN = (V_SN & 0xFFFF);
      int offset = V_SN_length;

      /* Extract TS */
      int TS_length = 4; // 4 bytes arriving, need to store as long
      byte[] TS_bytes = new byte[TS_length];
      System.arraycopy(bytes, offset, TS_bytes, 0, TS_length);
      byte[] longTS_bytes = new byte[8]; // Copy to long byte array
      System.arraycopy(TS_bytes, 0, longTS_bytes, 4, 4);
      TS = ByteUtil.bytesToLong(longTS_bytes);
      offset += TS_length;

      // Extract SSRC
      int SSRC_length = 4; // 4 bytes arriving, need to store as long
      byte[] SSRC_bytes = new byte[SSRC_length];
      System.arraycopy(bytes, offset, SSRC_bytes, 0, SSRC_length);
      byte[] longSSRC_bytes = new byte[8]; // Copy to long byte array
      System.arraycopy(SSRC_bytes, 0, longSSRC_bytes, 4, 4);
      SSRC = ByteUtil.bytesToLong(longSSRC_bytes);
      offset += SSRC_length;

      // Extract Payload
      int payload_length = (length - offset); // # bytes
      payloadLength = payload_length;
      payload = new byte[payload_length];

      System.arraycopy(bytes, offset, payload, 0, payload_length);
      if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
         logger.debug("[RTPPacket] Unpacking: " + ByteUtil.writeBytes(bytes));
         logger.debug("[RTPPacket] Unpacked V: " + V);
         logger.debug("[RTPPacket] Unpacked P: " + P);
         logger.debug("[RTPPacket] Unpacked X: " + X);
         logger.debug("[RTPPacket] Unpacked CC: " + CC);
         logger.debug("[RTPPacket] Unpacked M: " + M);
         logger.debug("[RTPPacket] Unpacked PT: " + PT);
         logger.debug("[RTPPacket] Unpacked SN: " + SN);
         logger.debug("[RTPPacket] Unpacked TS: " + TS);
         logger.debug("[RTPPacket] Unpacked SSRC: " + SSRC);
         logger.debug("[RTPPacket] Unpacked payload: " + ByteUtil.writeBytes(payload));
      }
   }

   /*
    * (steveq) The following uses method names based on solely on block
    * diagrams for setters and getters. The reasoning here is that those that
    * will be using this RTP stack will want to methods that are directly
    * reflective of the field parameter mnemonic (e.g., setV for field V).
    * Those that require further description are directed to refer to the
    * javadoc for this class.
    */
   /**
    * Set this RTP version.
    * 
    * @param i
    *            This RTP version (2 bits)
    * @throws IllegalArgumentException
    */
   public void setV(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(2)))
         V = i;
      else
         throw new IllegalArgumentException(RtpException.OUT_OF_RANGE);
   }

   /**
    * Get the RTP version.
    */
   public int getV() {
      return V;
   }

   /**
    * Set the padding bit.
    * 
    * @param i
    *            The padding (1 bit).
    * @throws IllegalArgumentException
    */
   public void setP(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         P = i;
      else
         throw new IllegalArgumentException(RtpException.OUT_OF_RANGE);
   }

   /**
    * Get the padding bit.
    * 
    * @return The padding.
    */
   public int getP() {
      return P;
   }

   /**
    * Set the extension.
    * 
    * @param i
    *            The extension (1 bit)
    * @throws IllegalArgumentException
    */
   public void setX(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         X = i;
      else
         throw new IllegalArgumentException(RtpException.OUT_OF_RANGE);
   }

   /**
    * Get the extension.
    * 
    * @return the extension.
    */
   public int getX() {
      return X;
   }

   /**
    * Set the CSRC count.
    * 
    * @param i
    *            The CSRC count (4 bits)
    * @throws IllegalArgumentException
    */
   public void setCC(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(4)))
         CC = i;
      else
         throw new IllegalArgumentException(RtpException.OUT_OF_RANGE);
   }

   /**
    * Get the CSRC count.
    * 
    * @return the CSRC count.
    */
   public int getCC() {
      return CC;
   }

   /**
    * Set the marker.
    * 
    * @param i
    *            The marker (1 bit)
    * @throws IllegalArgumentException
    */
   public void setM(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         M = i;
      else
         throw new IllegalArgumentException(RtpException.OUT_OF_RANGE);
   }

   /**
    * Get the marker.
    * 
    * @return the marker.
    */
   public int getM() {
      return M;
   }

   /**
    * Set the payload type.
    * 
    * @param i
    *            The payload type (7 bits)
    * @throws IllegalArgumentException
    */
   public void setPT(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(7)))
         PT = i;
      else
         throw new IllegalArgumentException(RtpException.OUT_OF_RANGE);
   }

   /**
    * Get the payload type.
    * 
    * @return The payload type.
    */
   public int getPT() {
      return PT;
   }

   /**
    * Set the sequence number.
    * 
    * @param i
    *            The sequence number (16 bits)
    * @throws IllegalArgumentException
    */
   public void setSN(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(16)))
         SN = i;
      else
         throw new IllegalArgumentException(RtpException.OUT_OF_RANGE);
   }

   /**
    * Get the sequence number.
    * 
    * @return the sequence number.
    */
   public int getSN() {
      return SN;
   }

   /**
    * Set the time stamp.
    * 
    * @param timeStamp
    *            The time stamp (32 bits).
    * @throws IllegalArgumentException
    */
   public void setTS(long timeStamp) throws IllegalArgumentException {
      if ((0 <= timeStamp) && (timeStamp <= ByteUtil.getMaxLongValueForNumBits(32)))
         TS = timeStamp;
      else
         throw new IllegalArgumentException(RtpException.OUT_OF_RANGE);
   }

   /**
    * Get the time stamp.
    * 
    * @return the time stamp.
    */
   public long getTS() {
      return TS;
   }

   /**
    * Set the synchronization source identifier.
    * 
    * @param ssrc
    *            the synchronization source identifier (32 bits)
    * @throws IllegalArgumentException
    */
   public void setSSRC(long ssrc) throws IllegalArgumentException {
      if ((0 <= ssrc) && (ssrc <= ByteUtil.getMaxLongValueForNumBits(32)))
         SSRC = ssrc;
      else
         throw new IllegalArgumentException(RtpException.OUT_OF_RANGE + ssrc);
   }

   /**
    * Get the synchronization source identifier.
    * 
    * @return the synchronization source identifier.
    */
   public long getSSRC() {
      return SSRC;
   }

   // public int getCSRC() {}
   // public void setCSRC() {}

   /***************************************************************************
    * RTP Header Extensions
    **************************************************************************/

   // public void getDP(){}
   // public void setDP(){}
   // public void getEL(){}
   // public void setEL(){}

   /**
    * Get the payload of this RTP packet.
    * 
    * @return the payload of this RTP packet.
    */
   public byte[] getPayload() {
      return payload;
   }

   /**
    * Set the payload of this RTP packet.
    * 
    * @param bytes
    *            the byte buffer containing the payload
    * @param length
    *            the number of buffer bytes containing the payload.
    */
   public void setPayload(byte[] bytes, int length)
         throws IllegalArgumentException {
      if (logger.isDebugEnabled())
         logger.debug("Payload length: " + length);

      if (length > MAX_PAYLOAD_BUFFER_SIZE)
         throw new IllegalArgumentException(
               "Payload is too large Max Size is limited to "
                     + MAX_PAYLOAD_BUFFER_SIZE);
      payloadLength = length;
      payload = bytes;
   }

   /**
    * Get the payload length.
    * 
    * @return they payload length.
    */
   public int getPayloadLength() {
      return payloadLength;
   }

   /**
    * Get the XML formatted string representation.
    * 
    * @return the XML formatted string representation.
    */
   public String toString() {
      // add padding, csrcCount
      StringBuffer sb = new StringBuffer();
      sb.append("<rtp-header");
      sb.append("\n version=\"" + V + "\"");
      sb.append("\n padding=\"" + P + "\"");
      sb.append("\n headerExtension=\"" + X + "\"");
      sb.append("\n csrcCount=\"" + CC + "\"");      
      sb.append("\n marker=\"" + M + "\"");
      sb.append("\n payloadType=\"" + PT + "\"");
      sb.append("\n sequenceNumber=\"" + SN + "\"");
      sb.append("\n timeStamp=\"" + TS + "\"");
      sb.append("\n SSRC=\"" + SSRC + "\"");
      sb.append("\n/>\n");
      return sb.toString();
   }
}
