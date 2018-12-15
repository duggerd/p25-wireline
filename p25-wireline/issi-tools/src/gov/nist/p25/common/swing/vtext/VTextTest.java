//
package gov.nist.p25.common.swing.vtext;

import java.awt.*;
import javax.swing.*;

//
// http://www.oreillynet.com/pub/a/mac/2002/03/22/vertical_text.html
// 
public class VTextTest implements SwingConstants {

   static String[] sEnglish = {"Apple", "Java", "OS X"};
   static String[] sJapanese = {"\u65e5\u672c\u8a9e", "\u5c45\u3068\u3001\u304d\u3087\u3068"};

   public VTextTest() {
      JFrame fr = new JFrame("VTextTest");
      fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      fr.getContentPane().add(testComponent());
      fr.setSize(new Dimension(600, 400));
      fr.setVisible(true);
   }
   JComponent testComponent() {
      JPanel panel = new JPanel();
      panel.setLayout(new GridLayout(0,3));
      panel.add(makeTabpane(null, sEnglish, VTextIcon.ROTATE_DEFAULT));
      panel.add(makeTabpane(new Font("Osaka", 0, 12), sJapanese, VTextIcon.ROTATE_DEFAULT));
      panel.add(makeTabpane(null, sEnglish, VTextIcon.ROTATE_NONE));
      return panel;
   }
   
   JTabbedPane makeTabpane(Font font, String[] strings, int rotateHint) {
      JTabbedPane panel = new JTabbedPane(LEFT);
      Icon graphicIcon = UIManager.getIcon("FileView.computerIcon");
      if (font != null)
         panel.setFont(font);
      for (int i = 0; i < strings.length; i++) {
         VTextIcon textIcon = new VTextIcon(panel, strings[i], rotateHint);
         CompositeIcon icon = new CompositeIcon(graphicIcon, textIcon);
         panel.addTab(null, icon, makePane());
      }
      return panel;
   }
   
   JPanel makePane() {
      JPanel p = new JPanel();
      p.setOpaque(false);
      return p;
   }

   //=======================================================================
   public static void main(String[] args) {
      new VTextTest();
   }
}
