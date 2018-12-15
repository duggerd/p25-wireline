//
package gov.nist.p25.common.swing.widget;

import java.awt.Component;
import javax.swing.JDialog;
import javax.swing.JOptionPane;


public class MessageBox
{
   // http://www.rgagnon.com/javadetails/java-0376.html
   /* 
   These are a list of STATIC MODAL dialogs

   int return codes of button pressed:

      -1 - WINDOW CLOSED - the X PRESSED
       0 - YES and OK
       1 - NO
       2 - CANCEL

   (thanks to flipside for the idea)
    */

   public static int OPTION_WINDOW_CLOSED = -1;
   public static int OPTION_YES_OK = 0;
   public static int OPTION_NO = 1;
   public static int OPTION_CANCEL = 2;


   //--------------------------------------------------------------------
   public static int doYesNo(String theMessage){
      int result = JOptionPane.showConfirmDialog((Component)
            null, theMessage, "Alert", JOptionPane.YES_NO_OPTION);
      return result;
   }

   public static int doYesNoCancel(String theMessage){
      int result = JOptionPane.showConfirmDialog((Component)
            null, theMessage, "Alert", JOptionPane.YES_NO_CANCEL_OPTION);
      return result;
   }

   public static int doOkCancel(String theMessage){
      int result = JOptionPane.showConfirmDialog((Component)
            null, theMessage, "Alert", JOptionPane.OK_CANCEL_OPTION);
      return result;
   }

   public static int doOk(int x, int y, String theMessage){
      JOptionPane optionPane = new JOptionPane(theMessage);
      JDialog dialog = optionPane.createDialog(null, "Alert");
      dialog.setAlwaysOnTop(true);
      dialog.setLocation( x, y);
      dialog.setVisible(true);
      Object value = optionPane.getValue();
      int result;
      if(value==null)
         result = JOptionPane.CLOSED_OPTION;
      else
         result = ((Integer)value).intValue();
      return result;
   }

   public static int doOk(String theMessage){
      int result = JOptionPane.showConfirmDialog((Component)
            null, theMessage, "Alert", JOptionPane.DEFAULT_OPTION);
      return result;
   }

   //====================================================================
   public static void main(String args[])
   {
      int i = MessageBox.doYesNo("Are your sure ?");
      System.out.println("ret : " + i );

      i = MessageBox.doYesNoCancel("Are your sure ?");
      System.out.println("ret : " + i );

      i = MessageBox.doOkCancel("Are your sure ?");
      System.out.println("ret : " + i );

      i = MessageBox.doOk("Done.");
      System.out.println("ret : " + i );

      i = MessageBox.doOk(200, 200, "Done at (200,200).");
      System.out.println("ret : " + i );
   }
}
