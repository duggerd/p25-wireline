//
package gov.nist.p25.issi.packetmonitor.gui;

import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JPanel;


public class StartupFileDialog extends JDialog {
   
   private static final long serialVersionUID = -1L;

   private boolean okButtonPressed;
   private boolean cancelButtonPressed;
   private String pcapFileName;
   private String startupFileName;

   private JTextField prop;
   private JTextField pcap;
   private JButton okButton;
   private JButton cancelButton;

   // accessor
   public String getPcapFileName() {
      return pcapFileName;
   }
   public void setPcapFileName(String pcapFileName) {
      this.pcapFileName = pcapFileName;
   }

   public String getStartupFileName() {
      return startupFileName;
   }
   public void setStartupFileName(String startupFileName) {
      this.startupFileName = startupFileName;
   }

   public boolean isOkButtonPressed() {
      return okButtonPressed;
   }
   public void setOkButtonPressed(boolean okButtonPressed) {
      this.okButtonPressed = okButtonPressed;
   }

   public boolean isCancelButtonPressed() {
      return cancelButtonPressed;
   }
   public void setCancelButtonPressed(boolean cancelButtonPressed) {
      this.cancelButtonPressed = cancelButtonPressed;
   }
   
   public String loadFile(String title, String extension) {
      JFileChooser fileChooser = new JFileChooser(".");
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setVisible(true);
      JFrame frame = new JFrame(title);

      fileChooser.showOpenDialog(frame);
      File file = fileChooser.getSelectedFile();
      if(file != null) {
         String ext = getExtension(file);
         if(ext.equals(extension))
            return file.getPath();
      }
      JOptionPane.showMessageDialog(null,
         "Bad file type, "+extension+" file reqired",
         "Startup Error",
         JOptionPane.ERROR_MESSAGE);
      return "";
   }
   
   /**
    * Method to get the extension of the file, in lowercase
    */
   private String getExtension(File f) {
      String s = f.getName();
      int i = s.lastIndexOf('.');
      if (i > 0 && i < s.length() - 1)
         return s.substring(i + 1).toLowerCase();
      return "";
   }
   
   class LoadStartupFileActionListener implements ActionListener {
      
      public LoadStartupFileActionListener( ) {
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         String startupFileName = StartupFileDialog.this.loadFile("Select Startup File", "properties");
         StartupFileDialog.this.startupFileName = startupFileName;
         StartupFileDialog.this.prop.setText(startupFileName);
      }
   }
   
   class LoadPCapFileActionListener implements ActionListener {

      @Override
      public void actionPerformed(ActionEvent e) {
         String startupFileName = StartupFileDialog.this.loadFile("Select PCAP File", "pcap");
         StartupFileDialog.this.pcapFileName = startupFileName;
         StartupFileDialog.this.pcap.setText(startupFileName);
      }
   }
   
   class OKActionListener implements ActionListener {

      @Override
      public void actionPerformed(ActionEvent arg0) {
         setOkButtonPressed(true);
         setCancelButtonPressed(false);
         setVisible(false);
      }
   }
   
   class CancelActionListener implements ActionListener {

      @Override
      public void actionPerformed(ActionEvent arg0) {
         setOkButtonPressed(false);
         setCancelButtonPressed(true);
         setVisible(false);
      }
   }

   // constructor
   public StartupFileDialog(String pcapFileName, String startupFileName) {
      super.setTitle("Select input parameters");
      super.setBounds( new Rectangle(600,150));
      super.setModalityType(ModalityType.APPLICATION_MODAL);
      
      this.pcapFileName = pcapFileName;
      this.startupFileName = startupFileName;
      setLayout(new GridLayout(3,1));
      JPanel jp = new JPanel();
      jp.setLayout(new GridLayout(1,2));
      JButton jbutton = new JButton("Select startup file");
      jbutton.addActionListener(new LoadStartupFileActionListener());   
      prop = new JTextField(30);
      prop.setEditable(false);
      jp.add(jbutton);
      jp.add(prop);
      add(jp);
      
      jp = new JPanel();
      jp.setLayout(new GridLayout(1,2));
      JButton jbutton1 = new JButton("Select PCAP file");
      jbutton1.addActionListener(new LoadPCapFileActionListener());   
      pcap = new JTextField(30);
      pcap.setEditable(false);
      jp.add(jbutton1);
      jp.add(pcap);
      add(jp);
      
      jp = new JPanel();
      jp.setLayout(new GridLayout(1,2));
      okButton = new JButton("OK");
      jp.add(okButton);
      okButton.addActionListener( new OKActionListener());
      
      cancelButton = new JButton("CANCEL");
      jp.add(cancelButton);
      add(jp);
      cancelButton.addActionListener( new CancelActionListener());
      
      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      addWindowListener( new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            cancelButtonPressed = true;
            okButtonPressed = false;
            setVisible(false);
         }
      });
   }
}
