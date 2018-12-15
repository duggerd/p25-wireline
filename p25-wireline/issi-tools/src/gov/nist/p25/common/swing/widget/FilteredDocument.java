//
package gov.nist.p25.common.swing.widget;

import java.awt.Container;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/** 
 * Filtered document to be used with JTextField
 *
 *  JTextField name = new JTextField();
 *  FilteredDocument doc = new FilteredDocument(12,FilteredDocument.UPPERCASE);
 *  name.setDocument( doc );
 *
 */
public class FilteredDocument extends PlainDocument
{
   private static final long serialVersionUID = -1L;

   public static final String ANY = "*";
   public static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
   public static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
   public static final String DIGIT = "0123456789";
   public static final String HEX_DIGIT = DIGIT + "ABCDEFabcdef";
   public static final String BINARY_DIGIT = "01";

   public static final String SPACE = " ";
   public static final String NEG_SIGN = "-";
   public static final String PLUS_SIGN = "+";
   public static final String DOT = ".";
   public static final String UNDER_SCORE = "_";

   public static final String NUMERIC = DIGIT;
   public static final String FLOAT = NUMERIC + DOT;
   public static final String ALPHA = LOWERCASE + UPPERCASE;
   public static final String ALPHA_NUMERIC = ALPHA + NUMERIC + SPACE;
   public static final String UPPERCASE_ALPHA = UPPERCASE+SPACE;
   public static final String UPPERCASE_ALPHA_NUMERIC = UPPERCASE+NUMERIC+SPACE;

   public static final String IP_ADDRESS = DIGIT+DOT +DIGIT+DOT +DIGIT+DOT +DIGIT;

   private int maxLength;
   private boolean negativeAccepted;
   private String acceptedChars;

   // accessor
   public int getMaxLength() { return maxLength; }
   public String getMaxLengthString() { return ""+maxLength; }
   public String getAcceptedChars() { return acceptedChars; }

   public void setMaxLength(int max) { maxLength=max; }
   public void setAcceptedChars(String s) { acceptedChars=s; }

   // constructors
   public FilteredDocument()
   {
      super();
   }
   public FilteredDocument(int maxLength, String allow)
   {
      this( maxLength, allow, false);
   }
   public FilteredDocument(int maxLength, String allow, boolean negative)
   {
      super();
      setFilter( maxLength, allow, negative);
   }

   public void setFilter(int maxLength, String allow, boolean negative)
   {
      setMaxLength( maxLength );
      setAcceptedChars( allow );
      setNegativeAccepted( negative );
   }

   public void setNegativeAccepted( boolean negative)
   {
      if( acceptedChars.equals(NUMERIC) ||
          acceptedChars.equals(FLOAT) ||
          acceptedChars.equals(ALPHA_NUMERIC) )
      {
         negativeAccepted = negative;
      }
   }

   public void insertString(int offset, String str, AttributeSet attr)
      throws BadLocationException
   {
      if( str == null)
         return;

      // check composite length
      if( maxLength != 0 && getLength()+str.length() > maxLength)
         return;

      // apply filtering here
      if( acceptedChars.equals(ANY))
      {
         super.insertString( offset, str, attr);
         return;
      }
      //----------------------------------------
      if( acceptedChars.equals(UPPERCASE))
          str = str.toUpperCase();
      else if( acceptedChars.equals(LOWERCASE))
          str = str.toLowerCase();

      //----------------------------------------
      String valid = acceptedChars;
      if( negativeAccepted )
         valid += NEG_SIGN;

      //System.out.println("valid=<"+valid+">");
      for(int i=0; i < str.length(); i++)
      {
         if( valid.indexOf(str.charAt(i)) == -1)
            throw new BadLocationException(str,i);
      }
      // for float, check DOT position
      if( acceptedChars.equals(FLOAT) ||
          (acceptedChars.equals(FLOAT+"-") && negativeAccepted))
      {
         if( str.indexOf(DOT) != -1)
         {
            if( getText(0,getLength()).indexOf(DOT) != -1)
               return;
         }
      }

      // for negative item, check sign position
      int pos = str.indexOf( NEG_SIGN );
      if( negativeAccepted && pos != -1)
      {
         if( pos != 0 || offset != 0)
            return;
      }
      super.insertString( offset, str, attr);
   }

   //====================================================================
   public static void main( String[] args) throws Exception
   {
      JFrame f = new JFrame("FilteredDocument");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      Container c = f.getContentPane();

      JPanel p = new JPanel();
      p.setLayout( new GridLayout( 7, 2) );

      //-----------------------------------------------------
      JLabel a0 = new JLabel("ALPHA_NUMERIC(0)");
      p.add( a0 );
      JTextField t0 = new JTextField();
      t0.setDocument( new FilteredDocument(0,FilteredDocument.ALPHA_NUMERIC));
      p.add( t0 );

      //-----------------------------------------------------
      JLabel a1 = new JLabel("only numeric(8)");
      p.add( a1 );
      JTextField t1 = new JTextField();
      t1.setDocument( new FilteredDocument(8,FilteredDocument.NUMERIC));
      p.add( t1 );
      
      //-----------------------------------------------------
      JLabel a2 = new JLabel("Only Float(0)");
      p.add( a2 );
      JTextField t2 = new JTextField();
      t2.setDocument( new FilteredDocument(0,FilteredDocument.FLOAT));
      p.add( t2 );

      //-----------------------------------------------------
      JLabel a3 = new JLabel("Neg Float (12)");
      p.add( a3 );
      JTextField t3 = new JTextField();
      t3.setDocument( new FilteredDocument(12,FilteredDocument.FLOAT,true));
      p.add( t3 );

      //-----------------------------------------------------
      JLabel a4 = new JLabel("only UPPER(0)");
      p.add( a4 );
      JTextField t4 = new JTextField();
      t4.setDocument( new FilteredDocument(0,FilteredDocument.UPPERCASE));
      p.add( t4 );

      //-----------------------------------------------------
      JLabel a5 = new JLabel("only 'abcd1234-%$'");
      p.add( a5 );
      JTextField t5 = new JTextField();
      t5.setDocument( new FilteredDocument(20,"abcd1234-%$"));
      p.add( t5 );

      //-----------------------------------------------------
      JLabel a6 = new JLabel("hex digit(8)");
      p.add( a6 );
      JTextField t6 = new JTextField();
      t6.setDocument( new FilteredDocument(8,FilteredDocument.HEX_DIGIT));
      p.add( t6 );

      //-----------------------------------------------------
      c.add( p );
      f.pack();
      f.setVisible(true);
   }
}

