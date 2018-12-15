//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;
//import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

import gov.nist.p25.common.util.DataCoder;
import gov.nist.p25.issi.utils.ByteUtil;

/**
 * This class implements a P25 ISSI Packet Type. The P25 Packet Type has the
 * following format:
 * <p>
 * 
 * <pre>
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |M|     PT      |       SO      |    TSN      |L|   Interval    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 */
public class ISSIPacketType implements Serializable {
   
   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(ISSIPacketType.class);


   /** Constant that identifies the total byte length of fixed fields. */
   protected final static int FIXED_LENGTH = 4;

   /** PTT Transmit Request (7 bits). */
   public static final int PTT_TRANSMIT_REQUEST = 0;

   /** PTT Transmit Grant (7 bits). */
   public static final int PTT_TRANSMIT_GRANT = 1;

   /** PTT Transmit Progress (7 bits). */
   public static final int PTT_TRANSMIT_PROGRESS = 2;

   /** PTT Transmit End (7 bits). */
   public static final int PTT_TRANSMIT_END = 3;

   /** PTT Start (7 bits). */
   public static final int PTT_TRANSMIT_START = 4;

   /** PTT Transmit Mute (7 bits). */
   public static final int PTT_TRANSMIT_MUTE = 5;

   /** PTT Transmit Unmute (7 bits). */
   public static final int PTT_TRANSMIT_UNMUTE = 6;

   /** PTT Transmit Wait (7 bits). */
   public static final int PTT_TRANSMIT_WAIT = 7;

   /** PTT Deny (7 bits). */
   public static final int PTT_TRANSMIT_DENY = 8;

   /** Heartbeat (7 bits). */
   public static final int HEARTBEAT = 9;

   /** Heartbeat query (7 bits). */
   public static final int HEARTBEAT_QUERY = 10;

   /** Heartbeat used for local indication of Connection Maintenance. */
   public static final int HEARTBEAT_CONNECTION = 11;

   /** Heartbeat used for local indication of Mute Transmission. */
   public static final int HEARTBEAT_MUTE_TRANSMISSION = 12;

   /** Heartbeat used for local indication of Unmute Transmission. */
   public static final int HEARTBEAT_UNMUTE_TRANSMISSION = 13;

   /** The mute status (1 bit). */
   private int M = 0;

   /** The RTP packet type (7 bits). */
   private int PT = 0;

   /** The transmission sequence number (7 bits). */
   private int TSN = 0;

   /** The losing audio bit (1 bit). */
   private int L = 0;

   /**
    * Specifies periodicity, in seconds, at which the sending RFSS wishes to
    * receive heartbeats from the receiving RFSS (8 bits).
    */
   private int interval = 0;

   /** Service options */
   private transient ServiceOptions serviceOptions;


   /**
    * Constructs a P25 ISSI packet type.
    */
   public ISSIPacketType() {
   }
   
   /**
    * Construct an ISSI Packet Type given its byte array representation.
    * 
    * @param bytes The byte array representation of this ISSI Packet Type.
    */
   public ISSIPacketType(byte[] bytes) {
      int i = ByteUtil.bytesToInt(bytes);
      this.M = (i >>> 0x1F) & 0x01;
      this.PT = (i >>> 0x18) & 0x7F;
      this.serviceOptions = new ServiceOptions((i >>> 0x10) & 0x0FF);
      this.TSN = (i >>> 0x09) & 0x7F;
      this.L = (i >>> 0x08) & 0x01;
      this.interval = i & 0xFF;
   }

   /**
    * Get the size of this ISSI packet type in bytes.
    * 
    * @return The size of this ISSI packet type in bytes.
    */
   public int size() {
      return FIXED_LENGTH;
   }

   /**
    * Get the ISSI Packet Type as a byte array.
    * 
    * @return The ISSI Packet Type byte array.
    */
   public byte[] getBytes() {

      int i = 0;
      i |= M; // Add M
      i <<= 0x07; // Make room for PT
      i |= PT; // Add PT
      i <<= 0x08; // Make room for SO
      i |= getServiceOptions(); // Add SO
      i <<= 0x07; // Make room for TSN
      i |= TSN; // Add TSN
      i <<= 0x01; // Make room for L
      i |= L; // Add L
      i <<= 0x08; // Make room for interval
      i |= interval; // Add interval
      byte[] bytes = ByteUtil.intToBytes(i);
      return bytes;
   }

   /**
    * Set the mute status (1=muted, 0=unmuted).
    * 
    * @param i
    *            The mute status (1 bit)
    * @throws IllegalArgumentException
    */
   public void setM(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         M = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   /**
    * Get the mute status.
    * 
    * @return The mute status.
    */
   public int getM() {
      return M;
   }
   
   /**
    * Set the mute status flag.
    * 
    * @param muteStatus
    *           The mute status flag.
    */
   public void setMuteStatus(boolean muteStatus) {
      M = (muteStatus ? 1 : 0);
   }
   
   /**
    * Get the mute status flag.
    * 
    * @return The mute status flag.
    */
   private boolean getMuteStatus() {
      return getM() == 1;
   }

   /**
    * Set the packet type int value.
    * 
    * @param i
    *            The packet type int value (7 bits)
    * @throws IllegalArgumentException
    */
   public void setPT(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(7)))
         PT = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get the packet type int value.
    * 
    * @return The packet type int value.
    */
   public int getPT() {
      return PT;
   }

   /**
    * Get the packet type.
    * 
    * @return The packet type.
    */
   public PacketType getPacketType() {
      return PacketType.getInstance(PT);
   }
   
   /**
    * Set the packet type.
    * 
    * @param packetType
    *            The packet type to set.
    */
   public void setPacketType(PacketType packetType) {
      PT = packetType.intValue();
   }

   /**
    * Set the service options int value.
    * 
    * @param i
    *            The service options (8 bits)
    * @throws IllegalArgumentException
    */
   public void setSO(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(8))) {
         this.serviceOptions = new ServiceOptions(i);

      } else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Set the service options.
    * 
    * @param serviceOptions The service options.
    * 
    */
   public void setServiceOptions(ServiceOptions serviceOptions) {
      if (serviceOptions == null)
         throw new NullPointerException("null serviceOptions ");
      this.serviceOptions = serviceOptions;
   }

   /**
    * Get the service options.
    * 
    * @return The service options.
    */
   public int getServiceOptions() {
      return serviceOptions.encode();
   }

   /**
    * Set the transmission sequence number.
    * 
    * @param i
    *            The transmission sequence number (7 bits)
    * @throws IllegalArgumentException
    */
   public void setTranssmissionSequenceNumber(int i)
      throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(7)))
         TSN = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the transmission sequence number.
    * 
    * @return The transmission sequence number.
    */
   public int getTransmissionSequenceNumber() {
      return TSN;
   }
   
   /**
    * Set the losing audio bit.
    * 
    * @param i
    *            The losing audio bit (1 bit)
    * @throws IllegalArgumentException
    */
   public void setL(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         L = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the losing audio bit.
    * 
    * @return the losing audio bit.
    */
   public int getL() {
      return L;
   }
   
   /**
    * Set the losing audio flag.
    * 
    * @param losingAudio The losing audio flag.
    */
   public void setLosingAudio(boolean losingAudio) {
      L = (losingAudio ? 1 : 0);
   }

   /**
    * Get the losing audio flag.
    * 
    * @return the losing audio flag.
    */
   public boolean getLosingAudio() {
      return L == 1;
   }

   /**
    * Set the periodicity of received heartbeats.
    * 
    * @param i
    *            The periodicity of received heartbeats (8 bits)
    * @throws IllegalArgumentException
    */
   public void setInterval(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(8)))
         interval = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the periodicity of received heartbeats.
    * 
    * @return The periodicity of received heartbeats.
    */
   public int getInterval() {
      return interval;
   }
   
   /**
    * Compare this object with another object.
    * 
    * @param other The other object to be compared to.
    * @return True if this object is equivalent to the other object.
    */
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof ISSIPacketType))
         return false;
      ISSIPacketType pt = (ISSIPacketType) other;
      return M == pt.M && PT == pt.PT && TSN == pt.TSN && L == pt.L
            && interval == pt.interval ;
   }

   /**
    * Get the XML formatted string representation.
    * 
    * @return The XML formatted string representation.
    */
   public String toISSIString() {
      StringBuffer sb = new StringBuffer();
      sb.append("\tMute status M-bit: ");
      sb.append(DataCoder.toBinary(getM()));
      sb.append("\n\tPacket type: ");
      sb.append(getPacketType().toString());
      sb.append(" ");
      sb.append(DataCoder.toIntegerBinary(getPacketType().intValue(),7));
      sb.append(DataCoder.toTextBinary("\n\tService options: ",getServiceOptions(),8));
      sb.append("\n\tTransmission sequence number: ");
      sb.append(DataCoder.toIntegerHex(getTransmissionSequenceNumber(),4));
      sb.append("\n\tLosing audio L-bit: ");
      sb.append(DataCoder.toBinary(getL()));
      sb.append("\n\tInterval: ");
      sb.append(DataCoder.toHex(getInterval(),2));
      return sb.toString();
   }
   
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("<issi-packet-type");
      sb.append("\n muteStatus=\"" + getMuteStatus() + "\"");
      sb.append("\n packetType=\"" + getPacketType() + "\"");
      sb.append("\n serviceOptions=\"" + getServiceOptions() + "\"");
      sb.append("\n transmissionSequenceNumber=\"" + getTransmissionSequenceNumber() + "\"");
      sb.append("\n losingAudio=\"" + getLosingAudio() + "\"");
      sb.append("\n interval=\"" + getInterval() + "\"");
      sb.append("\n/>");
      return sb.toString();
   }
   
   /**
    * create from xml attributes.
    * 
    * @return the packet type (created from attributes)
    */
   public ISSIPacketType createFromAttributes( Attributes attrs) {
      ISSIPacketType retval = new ISSIPacketType() ;
      retval.M = Boolean.parseBoolean(attrs.getValue("muteStatus")) ? 1: 0;
      retval.PT = PacketType.getValueFromString(attrs.getValue("packetType"));
      retval.serviceOptions = new ServiceOptions ( Integer.parseInt(attrs.getValue("serviceOptions")));
      retval.TSN = Integer.parseInt(attrs.getValue("transmissionSequenceNumber"));
      retval.L = Boolean.parseBoolean(attrs.getValue("losingAudio")) ? 1 : 0;
      retval.interval = Integer.parseInt(attrs.getValue("interval"));
      return retval;
   }   
}
