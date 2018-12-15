//
package gov.nist.p25.common.tool.file;

import java.awt.BorderLayout;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * System Properties List Pane
 */
public class SysPropListPane extends JPanel {

   private JTextArea textArea = new JTextArea(20, 40);

   public SysPropListPane() {

      setLayout(new BorderLayout());
      add(new JScrollPane(textArea));

      Properties prop = System.getProperties();
      TreeSet propKeys = new TreeSet( prop.keySet());
      for (Iterator it = propKeys.iterator(); it.hasNext(); ) {
         String key = (String)it.next();
         textArea.append("" + key + "=" + prop.get(key) + "\n");
      }
   }

   //==============================================================
   public static void main(String[] args) {

      JFrame frame = new JFrame("System Properties");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      frame.add( new SysPropListPane());
      frame.pack();
      frame.setVisible(true);
   }
}
