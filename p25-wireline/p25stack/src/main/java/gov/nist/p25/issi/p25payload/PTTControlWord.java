//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;
//import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

import gov.nist.p25.common.util.DataCoder;
import gov.nist.p25.issi.utils.ByteUtil;


/**
 * This class implements a P25 PTT Control Word block. The P25 Control Word
 * block has the following format:
 * <p>
 * 
 * <pre>
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                  WACN ID              |       System ID       |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |            Unit ID                            |      TP       |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 *
 */
public class PTTControlWord implements Serializable {
   
   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(PTTControlWord.class);


   /** Constant that identifies the total byte length of fixed fields. */
   protected final static int FIXED_LENGTH = 8;

   // NOTE: The SU ID is a fully qualified identifier that includes
   // the WACN, System ID, and Unit ID (56 bits).
   /** The Wide Area Communication Network (WACN) ID (20 bits). */
   private int wacnId = 0;

   /** The system ID (12 bits). */
   private int systemId = 0;

   /** The unit ID (24 bits). */
   private int unitId = 0;

   /**
    * The transmit priority (8 bits). Note that the first four bits in TP is
    * the priority type (PT) and the last 4 bits are the priority level (PL).
    */
   private int TP = 0;


   /**
    * Construct a PTT control word.
    */
   public PTTControlWord() {
   }

   /**
    * Construct a PTT Control Word given its byte array representation.
    * 
    * @param bytes
    *            The byte array representation of this PTT Control Word.
    * @throws P25BlockException
    */
   public PTTControlWord(byte[] bytes) {
      long l = ByteUtil.bytesToLong(bytes);
      this.wacnId = (int) ((l >>> 0x2C) & 0x0FFFFF);
      this.systemId = (int) ((l >>> 0x20) & 0xFFF);
      this.unitId = (int) ((l >>> 0x08) & 0xFFFFFF);
      this.TP = (int) (l & 0xFF);
   }

   /**
    * Get the size of this block header in bytes.
    * 
    * @return the size of this block header in bytes.
    */
   public int size() {
      return FIXED_LENGTH;
   }

   /**
    * Get all PTT control word data.
    * 
    * @return all PTT control word data.
    */
   public byte[] getBytes() {

      long l = 0;
      l |= (wacnId & 0xFFFFF);
      l <<= 0x0C;
      l |= (systemId & 0xFFF);
      l <<= 0x18;
      l |= (unitId & 0xFFFFFF);
      l <<= 0x08;
      l |= (TP & 0xFF);
      byte[] bytes = ByteUtil.longToBytes(l);
      return bytes;
   }

   /**
    * Set the Wide Area Communication Network ID.
    * 
    * @param wacnId
    *            The Wide Area Communication Network ID (20 bits)
    * @throws IllegalArgumentException
    */
   public void setWacnId(int wacnId) throws IllegalArgumentException {
      if ((0 <= wacnId) && (wacnId <= ByteUtil.getMaxIntValueForNumBits(20)))
         this.wacnId = wacnId;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get the Wide Area Communication Network ID.
    * 
    * @return The Wide Area Communication Network ID.
    */
   public int getWacnId() {
      return wacnId;
   }

   /**
    * Set the system ID.
    * 
    * @param systemId
    *            The system ID (12 bits)
    * @throws IllegalArgumentException
    */
   public void setSystemId(int systemId) throws IllegalArgumentException {
      if ((0 <= systemId)
            && (systemId <= ByteUtil.getMaxIntValueForNumBits(12)))
         this.systemId = systemId;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get the system ID.
    * 
    * @return The system ID.
    */
   public int getSystemId() {
      return systemId;
   }

   /**
    * Set the unit ID.
    * 
    * @param i
    *            The unit ID (24 bits)
    * @throws IllegalArgumentException
    */
   public void setUnitId(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(24)))
         this.unitId = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get the unit ID.
    * 
    * @return The unit ID.
    */
   public int getUnitId() {
      return unitId;
   }

   /**
    * Set the transmit priority.
    * 
    * @param i
    *            The transmit priority (8 bits)
    * @throws IllegalArgumentException
    */
   public void setTP(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i < ByteUtil.getMaxIntValueForNumBits(8)))
         TP = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE
               + "Transmit Priority can only have the values 0..3");
   }

   /**
    * Get the transmit priority.
    * 
    * @return The transmit priority.
    */
   public int getTP() {
      return TP;
   }

   /**
    * Set the priority type and priority level.
    * 
    * @param pt
    *            The priority type
    * @param pl
    *            The priority level
    */
   public void setPTAndPL(int pt, int pl) {

      if (((0 <= pt) && (pt < TransmitPriorityType.size()))
            && ((0 <= pt) && (pt <= ByteUtil.getMaxIntValueForNumBits(4)))) {
         TP = (int) (pt & 0x0F);
         TP <<= 0x04;
         TP |= (pl & 0x0F);
      } else {
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
      }
   }

   /**
    * Get the priority type.
    * 
    * @return The priority type.
    */
   public int getPT() {
      return (int) ((TP >>> 0x04) & 0x0F);
   }

   /**
    * Get the priority type.
    * 
    * @return The priority type.
    */
   public int getPriorityType() {
      return getPT();
   }

   /**
    * Get the priority level.
    * 
    * @return The priority level.
    */
   public int getPL() {
      return (int) (TP & 0x0F);
   }

   /**
    * Get the priority level.
    * 
    * @return The priority level.
    */
   public int getPriorityLevel() {
      return getPL();
   }

   /**
    * Set the transmit priority and priority level.
    * 
    * @param tpType
    *            The transmit priority.
    * @param priorityLevel
    *            The priority level.
    */
   public void setPriorityTypeAndPriorityLevel(TransmitPriorityType tpType,
         int priorityLevel) {
      int pt = tpType.intValue();
      setPTAndPL(pt, priorityLevel);
   }

   /**
    * Get transmit priority.
    * 
    * @return The transmit priority.
    */
   public TransmitPriorityType getTransmitPriority() {
      return TransmitPriorityType.getInstance(getPT());
   }

   /**
    * Compare this object with another object.
    * 
    * @param other
    *            The other object to be compared to.
    * @return True if this object is equivalent to the other object.
    */
   @Override
   public boolean equals(Object other) {
      PTTControlWord that = (PTTControlWord) other;
      return this.systemId == that.systemId && this.TP == that.TP
            && this.unitId == that.unitId && this.wacnId == that.wacnId;
   }

   /**
    * Get the XML formatted string representation.
    * 
    * @return The XML formatted string representation.
    */
   public String toISSIString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("\tWide Area Communication Network: ");
      sbuf.append(DataCoder.toHex(getWacnId(),5));
      sbuf.append("\n\tSystem ID: ");
      sbuf.append(DataCoder.toHex(getSystemId(),3));
      sbuf.append("\n\tUnit ID: ");
      sbuf.append(DataCoder.toHex(getUnitId(),6));
      sbuf.append("\n\tTransmit Priority: ");
      sbuf.append(DataCoder.toHex(getTransmitPriority().intValue(),2));
      sbuf.append("\n\tTransmit Level: ");
      sbuf.append(DataCoder.toHex(getPriorityLevel(),2));
      return sbuf.toString();
   }
   
   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("<ptt-control-word");
      sbuf.append("\n wacnId=\"" + getWacnId() + "\"");
      sbuf.append("\n systemId=\"" + getSystemId() + "\"");
      sbuf.append("\n unitId=\"" + getUnitId() + "\"");
      sbuf.append("\n transmitPriority=\"" + getTransmitPriority() + "\"");
      sbuf.append("\n transmitPriorityLevel=\"" + getPriorityLevel() + "\"");
      sbuf.append("\n/>");
      return sbuf.toString();
   }
   
   /**
    * Populate this field from a set of attributes.
    * 
    * @return the populated structure ( constructed from attributes)
    */
   public static PTTControlWord createFromAttributes(Attributes attrs) {
      PTTControlWord retval  = new PTTControlWord();
      retval.wacnId = Integer.parseInt(attrs.getValue("wacnId"));
      retval.systemId = Integer.parseInt(attrs.getValue("systemId"));
      int pl = Integer.parseInt(attrs.getValue("transmitPriorityLevel"));
      int pt = TransmitPriorityType.valueFromString(attrs.getValue("transmitPriority"));
      retval.setPTAndPL(pt, pl);
      retval.setUnitId(Integer.parseInt(attrs.getValue("unitId")));
      return retval;         
   }
}
