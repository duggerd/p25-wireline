//
package gov.nist.p25.issi.testlauncher;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Logger;

/**
 * Configuration dialog.
 */
public class ConfigDialog extends JDialog
   implements ActionListener {
   
   private static final long serialVersionUID = -1L;   
   private static Logger logger = Logger.getLogger(ConfigDialog.class);

   private boolean saved = false;
   private File file;   
   private JTextArea textArea;
   private JButton saveButton;
   private JButton cancelButton;
   
   // accessor
   public void setSaved(boolean saved) {
      this.saved = saved;
   }
   public boolean isSaved() {
      return saved;
   }

   // constructor
   public ConfigDialog(File configFile, String title) {
      
      super();
      setModal(true);
      this.file = configFile;

      try {
         textArea = new JTextArea();
         textArea.setEditable(true);

         FileReader fr = new FileReader(configFile);
         char[] contents = new char[(int) configFile.length()];
         fr.read(contents);
         fr.close();

         String str = new String(contents);
         textArea.setText(str);

         JScrollPane scrollPane = new JScrollPane(textArea,
               JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
               JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
         setTitle(title);

         JPanel panel = new JPanel();
         panel.setLayout(new BorderLayout());

         saveButton = new JButton("Save");
         saveButton.addActionListener(this);
         saveButton.setEnabled(true);
         cancelButton = new JButton("Cancel");
         cancelButton.addActionListener(this);

         JPanel buttonPanel = new JPanel();
         buttonPanel.setLayout(new GridLayout(1, 2));
         buttonPanel.add(saveButton);
         buttonPanel.add(cancelButton);

         panel.add(scrollPane, BorderLayout.CENTER);
         panel.add(buttonPanel, BorderLayout.SOUTH);

         setContentPane(panel);
         setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
         setPreferredSize(new Dimension(640, 700));
         setSize(680, 740);

      } catch (Exception e) {
         
         logger.error("Unexpected exception ", e);
         JOptionPane.showMessageDialog( null, 
               "Cannot read file: "+configFile.getName(),
               "Error in reading file",
               JOptionPane.ERROR_MESSAGE);
      }
   }

   //----------------------------------------------------------------
   public void actionPerformed(ActionEvent ae) {

      Object source = ae.getSource();
      if (source == cancelButton) {

         setSaved(false);
         setVisible(false);

      } else if (source == saveButton) {
         
         try {
            /*
             * Note that we do not check to see if the file is dirty, we
             * just simply rewrite the file even if no changes were made.
             */
            String textToSave = textArea.getText();
            FileWriter fw = new FileWriter(file);
            fw.write(textToSave);
            fw.close();
            
            setSaved(true);
            setVisible(false);
         
         } catch (Exception ex) {
            
            logger.error("Unexpected exception occured", ex);
            JOptionPane.showMessageDialog( null, 
                  "Cannot save to file: "+ file.getName(),
                  "Error in saving file",
                  JOptionPane.ERROR_MESSAGE);
         }
      }
   }
}
