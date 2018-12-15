//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;
import java.nio.ByteBuffer;

//import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

import gov.nist.p25.common.util.DataCoder;
import gov.nist.p25.issi.utils.ByteUtil;


/**
 * This class implements a P25 Console PTT Control Word block.
 * The P25 Control Word block has the following format:
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
 *     |            Console Identifier(56 bit SUID)                    |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |            CID (Contd)                        |      TD       |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |            Voice Source Identifier(56 bit SUID)               |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |            VSID (Contd)                       |      CTXP     |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * </pre>
 *
 */
public class ConsolePTTControlWord implements Serializable {
   
   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(ConsolePTTControlWord.class);


   /** Constant that identifies the total byte length of fixed fields. */
   protected final static int FIXED_LENGTH = 24;

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

   //CSSI
   /**
    *  Console Identifier(56 bit SUID)
    */
   private long consoleId = 0;

   /**
    * Transmission Descriptor: (8 bits)
    * $00: indicates theat the transmission is a trunking transmission
    * $01: indicates theat the transmission is a convention transmission
    */
   private int TD = 0;

   /**
    *  Voice Source Identifier(56 bit SUID)
    */
   private long voiceSourceId = 0;

   /**
    * Console TX Priority (8 bits)
    */
   private int CTXP = 1;

   //-----------------------------------------------------------------------
   /**
    * Construct a Console PTT control word.
    */
   public ConsolePTTControlWord() {
   }

   /**
    * Construct a PTT Control Word given its byte array representation.
    * 
    * @param bytes
    *            The byte array representation of this PTT Control Word.
    * @throws P25BlockException
    */
   public ConsolePTTControlWord(byte[] bytes) {

      ByteBuffer bb = ByteBuffer.wrap( bytes);
      long l = bb.getLong( 0);
      this.wacnId = (int) ((l >>> 0x2C) & 0x0FFFFF);
      this.systemId = (int) ((l >>> 0x20) & 0xFFF);
      this.unitId = (int) ((l >>> 0x08) & 0xFFFFFF);
      this.TP = (int) (l & 0xFF);

      l = bb.getLong( 8);
      this.consoleId = (long) ((l >>> 0x08) & 0xFFFFFFFFFFFFFFL);
      this.TD = (int) (l & 0xFF);

      l = bb.getLong( 16);
      this.voiceSourceId = (long) ((l >>> 0x08) & 0xFFFFFFFFFFFFFFL);
      this.CTXP = (int) (l & 0xFF);
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
    * Get all Console PTT control word data.
    * 
    * @return all Console PTT control word data.
    */
   public byte[] getBytes() {

      ByteBuffer bb = ByteBuffer.allocate(24);
      long l = 0;
      l |= (wacnId & 0xFFFFF);
      l <<= 0x0C;
      l |= (systemId & 0xFFF);
      l <<= 0x18;
      l |= (unitId & 0xFFFFFF);
      l <<= 0x08;
      l |= (TP & 0xFF);
      bb.putLong(l);

      l = 0;
      l |= (consoleId & 0xFFFFFFFFFFFFFFL);
      l <<= 0x08;
      l |= (TD & 0xFF);
      bb.putLong(l);

      l = 0;
      l |= (voiceSourceId & 0xFFFFFFFFFFFFFFL);
      l <<= 0x08;
      l |= (CTXP & 0xFF);
      bb.putLong(l);

      return bb.array();
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
      if ((0 <= systemId) && (systemId <= ByteUtil.getMaxIntValueForNumBits(12)))
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
               + " Transmit Priority can only have the values 0..3");
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
    *            The transmit priority type.
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
   public TransmitPriorityType getTransmitPriorityType() {
      return TransmitPriorityType.getInstance(getPT());
   }

   //-----------------------------------------------------------------------
   public long getConsoleId() {
      return consoleId;
   }
   public int getTD() {
      return TD;
   }
   public TransmissionDescriptor getTransmissionDescriptor() {
      return TransmissionDescriptor.getInstance(getTD());
   }

   public long getVoiceSourceId() {
      return voiceSourceId;
   }
   public int getCTXP() {
      return CTXP;
   }
   public ConsoleTransmissionRequestPriority getConsoleTransmissionRequestPriority() {
      return ConsoleTransmissionRequestPriority.getInstance(getCTXP());
   }

   //-----------------------------------------------------------------------
   /**
    * Compare this object with another object.
    * 
    * @param other
    *            The other object to be compared to.
    * @return True if this object is equivalent to the other object.
    */
   @Override
   public boolean equals(Object other) {
      ConsolePTTControlWord that = (ConsolePTTControlWord) other;
      return this.wacnId == that.wacnId
       && this.systemId == that.systemId
       && this.unitId == that.unitId
       && this.TP == that.TP
       && this.consoleId == that.consoleId
       && this.TD == that.TD
       && this.voiceSourceId == that.voiceSourceId
       && this.CTXP == that.CTXP;
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
      sbuf.append(DataCoder.toHex(getTransmitPriorityType().intValue(),2));
      sbuf.append("\n\tTransmit Level: ");
      sbuf.append(DataCoder.toHex(getPriorityLevel(),2));
//CSSI
      sbuf.append("\n\tConsole ID: ");
      sbuf.append(DataCoder.toHex(getConsoleId(),7));
      sbuf.append("\n\tTransmission Descriptor: ");
      sbuf.append(DataCoder.toHex(getTD(),2));
      sbuf.append("\n\tVoice Source ID: ");
      sbuf.append(DataCoder.toHex(getVoiceSourceId(),7));
      sbuf.append("\n\tConsole Transmission Priority: ");
      sbuf.append(DataCoder.toHex(getCTXP(),2));
      
      return sbuf.toString();
   }
   
   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("<console-ptt-control-word");
      sbuf.append("\n wacnId=\"" + getWacnId() + "\"");
      sbuf.append("\n systemId=\"" + getSystemId() + "\"");
      sbuf.append("\n unitId=\"" + getUnitId() + "\"");
      sbuf.append("\n transmitPriority=\"" + getTransmitPriorityType() + "\"");
      sbuf.append("\n transmitPriorityLevel=\"" + getPriorityLevel() + "\"");
//CSSI
      sbuf.append("\n consoleId=\"" + getConsoleId() + "\"");
      sbuf.append("\n transmissionDescriptor=\"" + DataCoder.toHex(getTD(),2) + "\"");
      sbuf.append("\n voiceSourceId=\"" + getVoiceSourceId() + "\"");
      sbuf.append("\n consoleTransmissionRequestPriority=\"" + DataCoder.toHex(getCTXP(),2) + "\"");
      sbuf.append("\n/>");
      return sbuf.toString();
   }
   
   /**
    * Populate this field from a set of attributes.
    * 
    * @return the populated structure ( constructed from attributes)
    */
   public static ConsolePTTControlWord createFromAttributes(Attributes attrs) {
      ConsolePTTControlWord retval  = new ConsolePTTControlWord();
      retval.wacnId = Integer.parseInt(attrs.getValue("wacnId"));
      retval.systemId = Integer.parseInt(attrs.getValue("systemId"));
      int pl = Integer.parseInt(attrs.getValue("transmitPriorityLevel"));
      int pt = TransmitPriorityType.valueFromString(attrs.getValue("transmitPriority"));
      retval.setPTAndPL(pt, pl);
      retval.setUnitId(Integer.parseInt(attrs.getValue("unitId")));
//CSSI
      retval.consoleId = Long.parseLong(attrs.getValue("consoleId"));
      retval.TD = Integer.parseInt(attrs.getValue("transmissionDescriptor"),16);
      retval.voiceSourceId = Long.parseLong(attrs.getValue("voiceSourceId"));
      retval.CTXP = Integer.parseInt(attrs.getValue("consoleTransmissionRequestPriority"),16);
      return retval;         
   }
} 
