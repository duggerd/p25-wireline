//
package gov.nist.p25.common.swing.table;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Hex TextField
 */
public class HexTextField extends JTextField 
{
   private static final long serialVersionUID = -1L;
   
   public HexTextField(int length)
   {
      //super( new HexTextDocument(length), "", length);
      super( "", length);
      setMaxLength( length);
   }
   public void setMaxLength(int length) {
      this.setDocument( new HexTextDocument(length));
   }
 
   class HexTextDocument extends PlainDocument
   {
      private static final long serialVersionUID = -1L;     
      private int maxlength;

      HexTextDocument(int maxlength) {
         setMaxLength(maxlength);
      }

      public void setMaxLength( int maxlength) {
         this.maxlength = maxlength;
      }

      public boolean isHexChar(char c) {
         return  (("0123456789abcdefABCDEF".indexOf(c)) >= 0);
      }

      public void insertString(int offs, String str, AttributeSet a) 
         throws BadLocationException
      {
         if (str == null) {
    	      return;
         }

         // check for input length
         if (!((getLength() + str.length()) > maxlength))
         {   
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < str.length(); i++)
            {
               char c = str.charAt(i);
               if( !isHexChar(c))
               {
                  // ring a bell 
                  //System.out.println("\007");
                  break;
               }
               sb.append( c);
            }
            super.insertString(offs, sb.toString(), a);
         }
      }
   }
}
