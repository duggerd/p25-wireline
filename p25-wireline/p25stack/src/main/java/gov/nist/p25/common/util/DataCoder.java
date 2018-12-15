//
package gov.nist.p25.common.util;

import java.io.Serializable;

/**
 * Data Encoder/Decoder Utility
 *
 */
public class DataCoder implements Serializable
{
   private static final long serialVersionUID = -1L;

   public static void showln(String s) { System.out.println(s); }

   /**
    *  Pad a string S with a size of N with char C 
    *  on the left (true) or on the right (false)
    */
   public static synchronized String padString( String s, int n, char c,
      boolean paddingLeft)
   {
      StringBuffer str = new StringBuffer(s);
      int strLength  = str.length();
      if ( n > 0 && n > strLength ) {
         for ( int i = 0; i <= n ; i ++ ) {
            if ( paddingLeft ) {
               if ( i < n - strLength)
                  str.insert( 0, c);
            }
            else {
               if ( i > strLength)
                 str.append( c );
            }
         }
      }
      return str.toString();
   }
   
   public static String toBinary( int bvalue)
   {
      // ivalue=1 ==> %b1
      if( bvalue==0) 
         return  "%b0";
      else
         return  "%b1";
   }
   public static String toBooleanBinary( int bvalue)
   {
      // bvalue=1  True (%b1)
      return toBooleanBinary( bvalue==0 ? false : true);
   }
   public static String toBooleanBinary( boolean bvalue)
   {
      // bvalue=true  True (%b1)
      if( !bvalue) 
         return  "False (%b0)";
      else
         return  "True (%b1)";
   }
   
   public static String toBinary( int value, int ndigits)
   {
      // %b001010
      String valueStr = Integer.toBinaryString(value);
      String padStr = padString( valueStr, ndigits, '0', true);
      return "%b" + padStr; 
   }
   public static String toTextBinary(String text, int value, int ndigits)
   {
      //  text: %b10010
      return text + ": " + toBinary( value, ndigits);
   }
   
   public static String toTextIntegerBinary(String text, int value, int ndigits)
   {
      //  text (%b10010)
      return text + " (" + toBinary( value, ndigits) +")";
   }

   public static String toIntegerBinary( int value, int ndigits)
   {
      // value=2 ==> 2 (%b10)
      String valueStr = Integer.toBinaryString(value);
      String padStr = padString( valueStr, ndigits, '0', true);
      return Integer.toString(value) + " (%b" + padStr +")"; 
   }

   public static String toLongBinary( long value, int ndigits)
   {
      // value=2 ==> 2 (%b10)
      String valueStr = Long.toBinaryString(value);
      String padStr = padString( valueStr, ndigits, '0', true);
      return Long.toString(value) + " (%b" + padStr +")";
   }

   public static String toHex( byte[] data, int nibbles)
   {
      // data[] ==> %x00118f 
      StringBuffer sb = new StringBuffer();
      int nbytes = nibbles/2;

      nbytes = (nbytes > data.length ? data.length : nbytes);

      for( int i=0; i < nbytes; i++) {
         String valueStr = Integer.toHexString( data[i] & 0xFF);
         String padStr = padString( valueStr, 2, '0', true);
         sb.append( padStr);
      }
      if( nibbles % 2 != 0) {
         String valueStr = Integer.toHexString( data[nbytes-1] & 0x0F);
         sb.append( valueStr);        
      }
      return "%x" + sb.toString(); 
   }

   public static String toHex( int value, int ndigits)
   {
      // value=17  ==> %x0011 
      String valueStr = Integer.toHexString(value);
      String padStr = padString( valueStr, ndigits, '0', true);
      return "%x" + padStr; 
   }
   public static String toHex( long value, int ndigits)
   {
      // value=17  ==> %x0011 
      String valueStr = Long.toHexString(value);
      String padStr = padString( valueStr, ndigits, '0', true);
      return "%x" + padStr; 
   }

   public static String toIntegerHex( int value, int ndigits)
   {
      // value=17  ==> 100 (%x0011)
      return Integer.toString(value) +" (" +toHex(value,ndigits) +")"; 
   }

   public static String toLongHex( long value, int ndigits)
   {
      // ivalue=100 ==> 100 (%x1100100)
      String valueStr = Long.toHexString(value);
      String padStr = padString( valueStr, ndigits, '0', true);
      return Long.toString(value) + " (%x" + padStr +")"; 
   }

   //===========================================================================
   public static void main(String[] argv) 
   {
      int bvalue = 1;
      showln("booleanBinary: "+ DataCoder.toBooleanBinary( bvalue));

      int ivalue = 7;
      showln("integerBinary: "+ DataCoder.toIntegerBinary( ivalue, 8));

      ivalue = 100;
      showln("integerHex: "+ DataCoder.toIntegerHex( ivalue, 4));

      long lvalue = 3200;
      showln("longHex: "+ DataCoder.toLongHex( lvalue, 8));
   }
}
