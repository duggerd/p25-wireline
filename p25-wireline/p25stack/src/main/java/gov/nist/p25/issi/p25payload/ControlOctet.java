//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;
//import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

import gov.nist.p25.common.util.DataCoder;
import gov.nist.p25.issi.utils.ByteUtil;


/**
 * This class implements a P25 control octet. The P25 control octet has the
 * following format:
 * <p>
 * 
 * <pre>
 * 0 1 2 3 4 5 6 7 8
 * +-+-+-+-+-+-+-+-+
 * |S|C|   BHC     |
 * +-+-+-+-+-+-+-+-+
 * </pre>
 * 
 */
public class ControlOctet  implements Serializable {
   
   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(ControlOctet.class);

   /** Constant that identifies the total byte length of fixed fields. */
   protected final static int FIXED_LENGTH = 1;

   /** Signal bit (1 bit). */
   private int S = 0;

   /** Indicates a compact (1) or verbose (0) block header (1 bit). */
   private int C = 0;

   /** The number of block headers following this control octet (6 bits). */
   private int BHC = 0;

   /**
    * Construct a P25 control octet.
    */
   public ControlOctet() {
   }
   public ControlOctet(byte[] bytes) {
      this( bytes[0]);
   } 
   /**
    * Construct a P25 block control octet given its byte array representation.
    * 
    * @param b The byte represents this control octet.
    * @throws P25BlockException
    */
   public ControlOctet(byte b) {
      this.S = (int) ((b >>> 0x07) & 0x01);   // Extract S
      this.C = (int) ((b >>> 0x06) & 0x01);   // Extract C
      this.BHC = (int) (b & 0x3F);            // Extract BHC
   }

   /**
    * Get the size of this control octet in bytes.
    * 
    * @return The size of this control octet in bytes.
    */
   public int size() {
      return FIXED_LENGTH;
   }

   /**
    * Get the control octet as a byte array.
    * 
    * @return The control octet byte array.
    */
   public byte[] getBytes() {

      byte b = 0;
      b |= S;     // Set S
      b <<= 0x01; // Make room for C
      b |= C;     // Set C
      b <<= 0x06; // Make room for BHC
      b |= BHC;   // Set BHC
      byte[] bytes = new byte[size()];
      bytes[0] = b;
      return bytes;
   }

   /**
    * Set the signal bit (1=on, 0=off).
    * 
    * @param i
    *            The signal bit (1 bit)
    * @throws IllegalArgumentException
    */
   public void setS(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         S = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }
   
   /**
    * Get the signal bit.
    * 
    * @return The signal bit (1=on, 0=off).
    */
   public int getS() {
      return S;
   }
   
   /**
    * Set the compact bit (1=compact, 0=verbose).
    * 
    * @param i
    *            The compact bit (1 bit)
    * @throws IllegalArgumentException
    */
   public void setC(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(1)))
         C = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get the compact bit.
    * 
    * @return The compact bit (1=compact, 0=verbose).
    */
   public int getC() {
      return C;
   }

   /**
    * Set the number of block headers following this control octet.
    * 
    * @param i
    *         The number of block headers following this control octet (6 bits).
    * @throws IllegalArgumentException
    */
   public void setBHC(int i) throws IllegalArgumentException {
      if ((0 <= i) && (i <= ByteUtil.getMaxIntValueForNumBits(6)))
         BHC = i;
      else
         throw new IllegalArgumentException(P25BlockException.OUT_OF_RANGE);
   }

   /**
    * Get the number of block headers following this control octet.
    * 
    * @return The number of block headers following this control octet.
    */
   public int getBHC() {
      return BHC;
   }
   
   /**
    * Compare this control octet with another object.
    * 
    * @param other The other object.
    * @return True if this control octet is equivalent to the other object.
    */
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof ControlOctet))
         return false;
      ControlOctet that = (ControlOctet) other;
      return this.C == that.C && 
             this.S == that.S &&
             this.BHC == that.BHC;
   }
   
   /**
    * Match the significant fileds of the control octet with another control octet.
    * This is for conformance testing of PTT.
    */
   public boolean match(ControlOctet other) {
      ControlOctet that = (ControlOctet) other;
      return this.C == that.C && 
             this.S == that.S;
   }

   /**
    * Get the XML formatted string representation.
    * 
    * @return The XML formatted string representation.
    */
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("<control-octet");
      sb.append("\n signalBit=\"" + getS() + "\"");
      sb.append("\n compactBit=\"" + getC() + "\"");
      sb.append("\n blockHeaderCount=\"" + getBHC() + "\"");
      sb.append("\n/>");
      return sb.toString();
   }

   public String toISSIString() {
      StringBuffer sb = new StringBuffer();
      sb.append("\tSignal bit S-bit: " + DataCoder.toBinary(getS()));
      sb.append("\n\tCompact: " + DataCoder.toBinary(getC()));
      sb.append("\n\tBlock header count: "+ DataCoder.toIntegerBinary(getBHC(),6));
      sb.append("\n");
      return sb.toString();
   }
   public String toFSIString() {
      StringBuffer sb = new StringBuffer();
      sb.append("\nControl Octet: ");
      sb.append("\nSignal bit: " + DataCoder.toBinary(getS()));
      sb.append("\nCompact bit: " + DataCoder.toBinary(getC()));
      sb.append("\nBlock header count: "+ DataCoder.toIntegerBinary(getBHC(),6));
      sb.append("\n");
      return sb.toString();
   }
   
   /**
    * Construct this field from a set of attributes.
    *
    * @param attrs The XML attributes
    */
   public ControlOctet createFromAttributes(Attributes attrs) {   
      ControlOctet retval = new ControlOctet();
      retval.setS(Integer.parseInt(attrs.getValue("signalBit")));
      retval.setC(Integer.parseInt(attrs.getValue("compactBit")));
      retval.setBHC(Integer.parseInt(attrs.getValue("blockHeaderCount")));
      return retval;      
   }
   /**
    * Set the signal bit.
    */
   //public void setSignal(boolean b) {
   //   this.setS(b ? 1 : 0);   
   //}
}
