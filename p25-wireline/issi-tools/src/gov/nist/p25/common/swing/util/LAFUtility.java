//
package gov.nist.p25.common.swing.util;


import java.util.ArrayList;
import java.util.List;
//import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;

/**
 * Swing Look And Feel utility
 */
public class LAFUtility {

   public static List<String> getLAFNames() {
      UIManager.LookAndFeelInfo[] laf = UIManager.getInstalledLookAndFeels();

      ArrayList<String> list = new ArrayList<String>();
      for (int i = 0; i < laf.length; i++) {
          list.add(laf[i].getName());
      }
      return list;
   }

   public static String getLAFClassName(String name) {

      UIManager.LookAndFeelInfo[] laf = UIManager.getInstalledLookAndFeels();
      for (int i = 0; i < laf.length; i++) {
         if(laf[i].getName().equals(name))
            return laf[i].getClassName();
      }
      // no match, use default
      return laf[0].getClassName();
   }

   public static List<JRadioButtonMenuItem> getLAFRadioButtonMenuItem() {
      List<String> lafList = getLAFNames();
      ArrayList<JRadioButtonMenuItem> list = new ArrayList<JRadioButtonMenuItem>();
      for (String name: lafList) {
         list.add( new JRadioButtonMenuItem(name));
      }
      return list;
   }

   public static List<JRadioButton> getLAFRadioButton() {
      List<String> lafList = getLAFNames();
      ArrayList<JRadioButton> list = new ArrayList<JRadioButton>();
      for (String name: lafList) {
         list.add( new JRadioButton(name));
      }
      return list;
   }

   //=============================================================================
   public static void main(String[] args) {

      List<String> lafList = LAFUtility.getLAFNames();
      System.out.println("\nLAFName:");
      for (String name: lafList) {
          System.out.println(name);
      }
      System.out.println("\nClassName:");
      for (String name: lafList) {
         System.out.println( name+"-clasName=" + LAFUtility.getLAFClassName(name));
      }
      System.out.println("\nCurrent-LAF=" + UIManager.getLookAndFeel().getName());
   }
}

