//
package gov.nist.p25.common.swing.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Generic HTMLPane dialog.
 */
public class HTMLPaneDialog extends JDialog 
{ 
   private static final long serialVersionUID = -1L;
   
   public HTMLPaneDialog(Frame frame, String title, URL url)
      throws Exception
   {
      super(frame, true);
      setTitle( title);

      JEditorPane epane = new JEditorPane();
      epane.setPage( url);
      JScrollPane scrollPane = new JScrollPane(epane);
      scrollPane.setPreferredSize(new Dimension(640, 480));
      
      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(scrollPane, BorderLayout.CENTER);
      setContentPane(mainPanel);
      pack();

      addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            setVisible(false);
         }
      });
   }   
}
