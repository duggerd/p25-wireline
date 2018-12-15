//
package gov.nist.p25.issi.traceverifier;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

public class ProgressIndicator  {
   
   private JFrame jframe;
   private JProgressBar jprogressBar;
   private JTextField jtext;
   
   // accessor
//   private JProgressBar getJProgreeBar() {
//      return jprogressBar;
//   }
   
   // constructor
   public ProgressIndicator() {
      
      jframe = new JFrame();
      jframe.setTitle("Verifying traces");
      jframe.setLayout(new GridLayout(2,1));
      jprogressBar = new JProgressBar();
      jtext = new JTextField();
      jframe.add(jtext);
      jframe.add(jprogressBar);
      jprogressBar.setIndeterminate(true);
      
      jtext.setSize(800,200);
      jprogressBar.setSize(600,10);
      jframe.setLocation(centerComponent(jframe));
      jframe.pack();
      jframe.setVisible(true);
   }

   private Point centerComponent(Component c) {
      Rectangle rc = new Rectangle();
      rc = c.getBounds(rc);
      Rectangle rs = new Rectangle(Toolkit.getDefaultToolkit()
            .getScreenSize());
      return new Point((int) ((rs.getWidth() / 2) - (rc.getWidth() / 2)),
            (int) ((rs.getHeight() / 2) - (rc.getHeight() / 2)));
   }

   public void setText(String text) {
      jtext.setText(text);
   }
   
   public void done() {
      jframe.dispose();
   }
}
