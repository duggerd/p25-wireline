//
package gov.nist.p25.common.swing.widget;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Fixed length document to be used with JTextField
 *
 *  JTextField name = new JTextField();
 *  name.setDocument( new FixedLengthDocument(12));
 */
public class FixedLengthDocument extends PlainDocument
{
   private static final long serialVersionUID = -1L;

   public static int ACCEPT_ANY = 0;
   public static int ACCEPT_ALPHA   = 1;
   public static int ACCEPT_NUMERIC = 2;
   public static int ACCEPT_ALPHA_NUMERIC = 3;
   public static int ACCEPT_HEX_DIGIT = 4;

   public static int CASE_ANY = 0;
   public static int CASE_UPPER = 1;
   public static int CASE_LOWER = 2;

   private int maxLength;
   private int bitmap;
   private int casemap;

   // possible input checks:
   // 1. to upper case A/N
   // 2. to lower case A/N
   // 3. only digits [0..9]
   // 4. only hex [0..9 a..f]

   // accessor
   public int getMaxLength() { return maxLength; }
   public void setMaxLength(int max) { maxLength=max; }


   public FixedLengthDocument(int maxLength)
   {
      this( maxLength, ACCEPT_ANY, CASE_ANY);
   }
   public FixedLengthDocument(int maxLength, int bitmap)
   {
      this( maxLength, bitmap, CASE_ANY);
   }
   public FixedLengthDocument(int maxLength, int bitmap, int casemap)
   {
      super();
      this.maxLength = maxLength;
      this.bitmap = bitmap;
      this.casemap = casemap;
   }

   public void insertString(int offset, String str, AttributeSet attr)
      throws BadLocationException
   {
      if( str == null)
         return;

      // apply filtering here
      //----------------------------------------
      if( casemap == CASE_UPPER )
          str = str.toUpperCase();
      else if( casemap == CASE_LOWER )
          str = str.toLowerCase();

      if( maxLength != 0 && getLength()+str.length() > maxLength)
         return;

      //----------------------------------------
      if( bitmap == ACCEPT_ALPHA)
      {
         for(int i=0; i < str.length(); i++)
         {
            if( !Character.isLetter(str.charAt(i)) )
                throw new BadLocationException(str,i);
         }
      }
      else if( bitmap == ACCEPT_NUMERIC)
      {
         for(int i=0; i < str.length(); i++)
         {
            if( !Character.isDigit(str.charAt(i)) )
                throw new BadLocationException(str,i);
         }
      }
      else if( bitmap == ACCEPT_ALPHA_NUMERIC)
      {
         for(int i=0; i < str.length(); i++)
         {
            char c = str.charAt(i);
            if( !Character.isLetterOrDigit( c ) &&
                !Character.isSpaceChar( c ))
                throw new BadLocationException(str,i);
         }
      }
      else if( bitmap == ACCEPT_HEX_DIGIT)
      {
         // [0..9] [a..f]
      }

      //----------------------------------------
      super.insertString( offset, str, attr);
   }

   //=======================================================
   public static void main( String[] args) throws Exception
   {
      JFrame f = new JFrame("FixedLengthDocument");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      JTextField name = new JTextField(24);
      name.setDocument( new FixedLengthDocument(12,
                        FixedLengthDocument.ACCEPT_ALPHA_NUMERIC));
      f.getContentPane().add( name );
      f.pack();
      f.setVisible(true);
   }
}

