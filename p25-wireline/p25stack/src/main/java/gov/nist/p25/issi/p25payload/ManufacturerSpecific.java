//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;
//import org.apache.log4j.Logger;

import gov.nist.p25.issi.utils.ByteUtil;

/**
 * This class implements a skeleton for manufacturer-specific data. Such data
 * may have the following format:
 * <p>
 * 
 * <pre>
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    MFID       |     Length    |  Manufacturer specified data  |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 */
public class ManufacturerSpecific implements Serializable {
   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(ManufacturerSpecific.class);

   /** Constant that identifies the total byte Length of fixed fields. */
   protected final static int FIXED_LENGTH = 2;

   /***************************************************************************
    * Variables:  Note that each of the following use data types that are 
    * larger than what is required to keep two's complement sign bit from 
    * changing the sign.
    **************************************************************************/

   /** The manufacturer's ID (8 bits). */
   private int MFID = 0;

   /**
    * The number of octets in this block following the Length field (8 bits).
    */
   private int length = 0;

   /** The manufacturer-specific data for this block (variable). */
   private byte[] manufacturerData = null;

   /**
    * Constructs a manufacturer-specific block. Package local because we want
    * to avoid allocaton outside the package.
    * 
    */
   public ManufacturerSpecific() {
   }
   
   /**
    * Construct a Mfr-specific block given its byte array representation.
    * 
    * @param bytes The byte array representation of this Mfr-specific block.
    */
   public ManufacturerSpecific(byte[] bytes) {
      this.MFID = bytes[0] & 0xFF;
      this.length = bytes[1] & 0xFF;
      this.manufacturerData = new byte[this.length];
      System.arraycopy(bytes, 2, this.manufacturerData, 0, this.length);      
   }

   /**
    * Get the size of this ISSI packet type in bytes.
    * 
    * @return The size of this ISSI packet type in bytes.
    */
   public int size() {
      return FIXED_LENGTH + length;
   }

   /**
    * Get the mfr-specific block as a byte array.
    * 
    * @return The mfr-specific block byte array.
    */
   public byte[] getBytes() {

      byte[] bytes = new byte[this.size()];
      bytes[0] = (byte) MFID;
      bytes[1] = (byte) length;
      if (length > 0) {
         System.arraycopy(manufacturerData, 0, bytes, 2, length);
      }
      return bytes;
   }

   /**
    * Set manfacturer ID.
    * 
    * @param i
    *            The manfacturer ID (8 bits)
    * @throws IllegalArgumentException
    */
   public void setMFID(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(8)))
         MFID = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get the manfacturer ID.
    * 
    * @return The manfacturer ID.
    */
   public int getMFID() {
      return MFID;
   }

   /**
    * Set the length.
    * 
    * @param i
    *            The length.
    * @throws IllegalArgumentException
    */
   public void setLength(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(8)))
         length = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the length.
    * 
    * @return The length.
    */
   public int getLength() {
      return length;
   }

   /**
    * Set the manufacturer-specific data.
    * 
    * @param bytes
    *            The manufacturer-specific data (variable bytes)
    */
   public void setManufacturerData(byte[] bytes) {
      if (length == bytes.length) {
         manufacturerData = new byte[length];
         System.arraycopy(bytes, 0, manufacturerData, 0, length);
      } else {
         throw new IllegalArgumentException(
               "Variable length does not equal length of manufacturerData!");
      }
   }
   
   /**
    * Get the manufacturer-specific data.
    * 
    * @return the manufacturer-specific data.
    */
   public byte[] getManufacturerData() {
      return manufacturerData;
   }
   
   /**
    * Get the XML formatted string representation.
    * 
    * @return The XML formatted string representation.
    */
   @Override
   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("<manufacturer-specific\n");
      sbuf.append(" length=\"" + length + "\"\n");
      sbuf.append(" MFID=\"" + MFID + "\"\n");
      sbuf.append("\n>");
      sbuf.append("<![CDATA[\n");
      sbuf.append(ByteUtil.writeBytes(manufacturerData));
      sbuf.append("\n]]>");
      sbuf.append("\n</manufacturer-specific>");
      return sbuf.toString();
   }
}
