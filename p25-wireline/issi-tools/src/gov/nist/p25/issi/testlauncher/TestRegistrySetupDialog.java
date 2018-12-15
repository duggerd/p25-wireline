//
package gov.nist.p25.issi.testlauncher;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;

//import org.apache.log4j.Logger;
import gov.nist.p25.common.swing.widget.CheckBoxList;
import gov.nist.p25.issi.message.XmlTestregistry;
import gov.nist.p25.common.util.FileUtility;
//import gov.nist.p25.issi.xmlconfig.TestregistryDocument;
//import gov.nist.p25.issi.xmlconfig.TestregistryDocument.Testregistry;
//import gov.nist.p25.issi.xmlconfig.TestregistryDocument.Testregistry.Testcase;

/**
 * Test Registry Setup dialog for User Defined Profile.
 */
public class TestRegistrySetupDialog extends JDialog implements ActionListener
{
   private static final long serialVersionUID = -1L;   
   //private static Logger logger = Logger.getLogger(TestRegistrySetupDialog.class);
   public static void showln(String s) { System.out.println(s); }

   private boolean saved = false;
   private String xmlFile;

   private JButton selectAllButton;
   private JButton clearAllButton;
   private JButton saveButton;
   private JButton cancelButton;
   private CheckBoxList cbList;
   private XmlTestregistry xmldoc;
   
   // accessor
   public void setSaved(boolean saved) {
      this.saved = saved;
   }
   public boolean isSaved() {
      return saved;
   }

   // constructor
   public TestRegistrySetupDialog(String xmlFile, String title, String cbtype) {
      super();
      setModal(true);
      this.xmlFile = xmlFile;
      try {
         String xmlMsg = FileUtility.loadFromFileAsString(xmlFile);
         xmldoc = new XmlTestregistry();
         xmldoc.loadTestregistry(xmlMsg);
         cbList = xmldoc.getCheckBoxList(cbtype);

         JScrollPane scrollPane = new JScrollPane(cbList,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
         setTitle(title);

         JPanel buttonPanel = new JPanel();
         //buttonPanel.setLayout(new GridLayout(1, 2));
	 
         selectAllButton = new JButton("Select All");
         selectAllButton.addActionListener(this);
         clearAllButton = new JButton("Clear All");
         clearAllButton.addActionListener(this);

         saveButton = new JButton("Save");
         saveButton.addActionListener(this);
         cancelButton = new JButton("Cancel");
         cancelButton.addActionListener(this);

         buttonPanel.add(selectAllButton);
         buttonPanel.add(clearAllButton);
         buttonPanel.add(saveButton);
         buttonPanel.add(cancelButton);

         JPanel panel = new JPanel(new BorderLayout());
         panel.add(scrollPane, BorderLayout.CENTER);
         panel.add(buttonPanel, BorderLayout.SOUTH);

         setContentPane(panel);
         setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
         setPreferredSize(new Dimension(640, 700));
         setSize(480, 640);
         setVisible(true);
      } 
      catch (Exception e) {
         //logger.error("TestRegistry Setup Error: ", e);
         JOptionPane.showMessageDialog( null, 
            "Cannot read test registry file: "+xmlFile,
            "Error in reading test registry file",
            JOptionPane.ERROR_MESSAGE);
      }
   }
   private void setCheckBoxList(boolean selection)
   {
      ListModel model = cbList.getModel();
      for( int i=0; i < model.getSize(); i++) {
         JCheckBox jcb = (JCheckBox)model.getElementAt(i);
	 jcb.setSelected( selection);
	 jcb.updateUI();
      }
      setVisible(false);
      setVisible(true);
   }

   // implementation of ActionListener
   //---------------------------------------------------------------------
   public void actionPerformed(ActionEvent ae)
   {
      Object source = ae.getSource();
      if (source == selectAllButton) {
         setCheckBoxList( true);
      }
      else if (source == clearAllButton) {
         setCheckBoxList( false);
      }
      else if (source == cancelButton) {
         setSaved(false);
         setVisible(false);
      } 
      else if (source == saveButton) {
         try {
            boolean mflag = xmldoc.reconcileTestcase( cbList);
            showln("reconcileTestcase: mflag="+mflag);
	    if( mflag) {
               xmldoc.saveTestregistry( xmlFile);
               setSaved(true);
	    }
            setVisible(false);
         } 
	 catch (Exception ex) {
            JOptionPane.showMessageDialog( null, 
               "Cannot save test registry file: "+xmlFile,
               "Error in saving test registry file",
               JOptionPane.ERROR_MESSAGE);
         }
      }
   }

   //==============================================================
   public static void main(String args[]) throws Exception
   {
      String xmlFile = "testregistry2.xml";
      String cbtype = "User Defined Profile";
      String title = cbtype + " Setup";

      TestRegistrySetupDialog dialog = 
         new TestRegistrySetupDialog(xmlFile, title, cbtype);
      showln("dialog: isSaved="+dialog.isSaved());
      System.exit(0);
   }
}
