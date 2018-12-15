//
package gov.nist.p25.common.util;


public class ByteArrayUtil {

   /**
    * Convert a byte array to hex string format. 
    */
   public static String toHexString(byte[] in, int bytesPerLine) {
   
      if (in == null || in.length <= 0)
         return null;
           
      String pseudo[] = {
         "0", "1", "2", "3", "4", "5", "6", "7",
         "8", "9", "A", "B", "C", "D", "E", "F"
      };
   
      byte ch = 0x00;
      int i = 0; 
      StringBuffer out = new StringBuffer(in.length * 2);
      while (i < in.length) {

         if( i > 0 && i < in.length) {
            //if(i % bytesPerLine == 0)
            //   out.append("\n");
            //else
               out.append(" ");
         }
   
         // Strip off high nibble
         ch = (byte) (in[i] & 0xF0);
         ch = (byte) (ch >>> 4);
         ch = (byte) (ch & 0x0F);    
         out.append(pseudo[ (int) ch]);
   
         // Strip off low nibble 
         ch = (byte) (in[i] & 0x0F);
         out.append(pseudo[ (int) ch]);
         i++;
      }
      return out.toString();
   }

   /**
    * Convert the byte hex string to byte array. 
    * <01 81 00 00 04 0A 04 00 0A>
    */
   public static byte[] fromHexString(String hexString) {

      String[] parts = hexString.trim().split(" ");
      int nbytes = parts.length;
      //System.out.println("fromHexString: nbytes="+nbytes);

      byte[] byteArray = null;
      if( nbytes > 0) {
         byteArray = new byte[ nbytes];
	 for(int i=0; i < nbytes; i++) {
            try {
               int kbyte = Integer.parseInt(parts[i], 16);
               byteArray[i] = (byte)(kbyte & 0xFF);
            } catch(NumberFormatException ex) { }
	 }
      }
      return byteArray;
   }

   //=============================================================
   public static void main(String[] args) {

      int MAX_LEN = 120;
      byte[] data = new byte[MAX_LEN];
      for(int i=0; i < MAX_LEN; i++) {
         data[i] = (byte)i;
      }
      System.out.println("data=\n<" + toHexString( data, 16) +">");
   }
}
