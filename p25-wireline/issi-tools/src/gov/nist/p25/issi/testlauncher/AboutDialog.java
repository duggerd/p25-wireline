//
package gov.nist.p25.issi.testlauncher;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * This class implements the about dialog.
 */
public class AboutDialog extends JDialog implements ActionListener
{ 
   private static final long serialVersionUID = -1L;
   
   public AboutDialog(Frame frame, String version, String build) 
   {
      super(frame, true);
      setTitle("About");
      
      JLabel iconLabel = new JLabel(new ImageIcon("gfx/tester.jpg"));
      Dimension iconDimension = new Dimension(432, 180);
      iconLabel.setMinimumSize(iconDimension);
      iconLabel.setPreferredSize(iconDimension);
      
      String disclaimer = 
            "THIS SOFTWARE is provided \"AS IS\" WITHOUT ANY \n" +
            "WARRANTY OF ANY TYPE AS TO ANY MATTER WHATSOEVER,\n" +
            "EXPRESSED OR IMPLIED, INCLUDING NO IMPLIED \n" +
            "WARRANTIES OF MERCHANTABILITY AND NO WARRANTIES \n" +
            "OF FITNESS FOR A PARTICULAR PURPOSE. \n\n" +
            "Users acknowledge that the National Institute of\n" +
            "Standards and Technology (NIST) has no obligation\n" +
            "to correct any defect or problem with this \n" +
            "software or documentation. NIST is under no \n" +
            "obligation to provide future software updates, \n" +
            "and is under no obligation to provide assistance,\n" +
            "service, help or advice to User. NIST is under no\n" +
            "obligation to make or provide any modifications\n" +
            "to the Software. This software is not intended to\n" +
            "substitute for acquiring and application of \n" +
            "independent engineering, communications or other\n" +
            "expertise. Users of the software are solely\n" +
            "responsible for assuring the safety, reliability \n" +
            "and operability of any product developed out of\n" +
            "the use of the software.\n\n" +
            "NIST disclaims any liability whatsoever related\n" +
            "to the use or distribution of this software.\n\n" +
            "The Department of Homeland Security sponsored the\n" +
            "production of this material under an Interagency Agreement\n" +
            "with the Institute for Telecommunication Sciences.\n\n" +
            "For more information, visit:\n" +
            "https://p25-wireline.dev.java.net\n\n";

      JTextArea textArea = new JTextArea("ISSI-TESTER Version " + version + 
            ", Build " + build + "\n\n" + disclaimer);

      textArea.setLineWrap(true);
      JScrollPane scrollPane = new JScrollPane(textArea);
      Dimension scrollPaneDimension = new Dimension(432, 200);
      scrollPane.setMinimumSize(scrollPaneDimension);
      scrollPane.setPreferredSize(scrollPaneDimension);
      
      JPanel mainPanel = new JPanel();
      mainPanel.setLayout(new BorderLayout());
      mainPanel.add(iconLabel, BorderLayout.CENTER);
      
      JButton okButton = new JButton("OK");
      okButton.setActionCommand("Ok");
      okButton.addActionListener(this);
      
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new BorderLayout());
      buttonPanel.add(scrollPane, BorderLayout.CENTER);
      buttonPanel.add(okButton, BorderLayout.SOUTH);
      
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);
      setContentPane(mainPanel);

      pack();
      addWindowListener(new WindowAdapter() {

         public void windowClosing(WindowEvent e) {
            setVisible(false);
         }
      });
   }   
  
   public void actionPerformed(ActionEvent ae)
   {      
      if (ae.getActionCommand().equals("Ok"))
      {
         setVisible(false);
      }
   }
}
