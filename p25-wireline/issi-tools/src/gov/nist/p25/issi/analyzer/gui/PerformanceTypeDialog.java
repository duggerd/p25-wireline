//
package gov.nist.p25.issi.analyzer.gui;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import gov.nist.p25.issi.constants.IvsDelaysConstants;
import gov.nist.p25.common.swing.widget.GridBagConstraints2;
import gov.nist.p25.common.swing.widget.Filler;

/**
 * ISSI/CSSI Performance Type dialog
 */
public class PerformanceTypeDialog extends JDialog
   implements ActionListener
{
   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private boolean answer = false;
   private JButton okButton;
   private JButton cancelButton;

   private String perfType;
   private JCheckBox srcConsoleCB;
   private JCheckBox dstConsoleCB;
   private JPanel typePanel;

   // accessor
   public boolean getAnswer() {
      return answer;
   }
   public boolean getSrcConsole() {
      return srcConsoleCB.isSelected();
   }
   public boolean getDstConsole() {
      return dstConsoleCB.isSelected();
   }
   
   // constructor
   public PerformanceTypeDialog(JFrame frame, boolean modal, 
      String title, String perfType)
      throws Exception
   {
      super( frame, modal);
      setTitle( title);
      this.perfType = perfType;
      buildGUI( frame);
   }

   protected void buildGUI( JFrame frame) throws Exception
   {
      setLayout( new GridBagLayout());

      // type panel
      typePanel = new JPanel();

      // ISSI/CSSI  isSrcConsole, isDstConsole checkbox
      JLabel perfLabel = new JLabel( perfType);
      srcConsoleCB = new JCheckBox("Source Console");
      dstConsoleCB = new JCheckBox("Destination Console");
      typePanel.add( perfLabel);
      typePanel.add( new Filler(8,8));

      if( IvsDelaysConstants.TAG_TYPE_ISSI.equals(perfType)) {
         srcConsoleCB.setSelected(false);
         srcConsoleCB.setEnabled(false);
         dstConsoleCB.setSelected(false);
         dstConsoleCB.setEnabled(false);

         // short-circuit ISSI
         answer = true;
         return;
      }
      else {
         // CSSI
         srcConsoleCB.setSelected(true);
         srcConsoleCB.addActionListener(this);
         dstConsoleCB.setSelected(false);
         dstConsoleCB.addActionListener(this);
      }
      typePanel.add( srcConsoleCB);
      typePanel.add( dstConsoleCB);

      GridBagConstraints2 c = new GridBagConstraints2();
      // x, y, w, h, anchor, fill, l, t, r, b
      //
      // middle button panel
      okButton = new JButton("OK");
      okButton.setPreferredSize( new Dimension(80, 24));
      okButton.addActionListener(this);
      cancelButton = new JButton("Cancel");
      cancelButton.setPreferredSize( new Dimension(80, 24));
      cancelButton.addActionListener(this);

      JPanel buttonPanel = new JPanel(new GridBagLayout());
      c.set( 0, 0, 1, 1, "c", "horz", 2, 2, 2, 2);
      buttonPanel.add( new Filler(10,10), c);
      c.set( 1, 0, 1, 1, "c", "horz", 2, 2, 2, 2);
      buttonPanel.add( okButton, c);
      c.set( 2, 0, 1, 1, "c", "horz", 2, 2, 2, 2);
      buttonPanel.add( cancelButton, c);

      // layout
      c.set( 0, 0, 6, 1, "c", "horz", 4, 4, 4, 4);
      add( typePanel, c);

      c.set( 0, 1, 1, 1, "c", "none", 4, 4, 4, 4);
      add( new Filler(20,20), c);

      c.set( 3, 2, 3, 1, "e", "horz", 4, 4, 4, 4);
      add( buttonPanel, c);

      c.set( 0, 3, 1, 1, "c", "none", 4, 4, 4, 4);
      add( new Filler(20,20), c);

      pack();
      setSize(480, 220);
      setLocationRelativeTo(frame);
      requestFocus();
      setAlwaysOnTop(true);
      setVisible(true);
   }

   // implementation of ActionListener
   //-------------------------------------------------------------
   public void actionPerformed(ActionEvent evt)
   {
      Object src = evt.getSource();
      if( src == okButton) {
         //showln("OK: ...");
         answer = true;
	 setVisible(false);
      }
      else if( src == cancelButton) {
         //showln("CANCEL: ");
         answer = false;
	 setVisible(false);
      }
      else if( src == srcConsoleCB) {
         boolean isSel = srcConsoleCB.isSelected();
         //showln("srcConsoleCB: "+isSel);
         if( isSel)
            dstConsoleCB.setSelected(false);
	 else
            dstConsoleCB.setSelected(true);
      }
      else if( src == dstConsoleCB) {
         boolean isSel = dstConsoleCB.isSelected();
         //showln("dstConsoleCB: "+isSel);
         if( isSel)
            srcConsoleCB.setSelected(false);
	 else
            srcConsoleCB.setSelected(true);
      }
   }

   //===============================================================
   public static void main(String args[]) throws Exception {

      String title = "Define the Performance Type:";
      String perfType = IvsDelaysConstants.TAG_TYPE_CSSI;
      //String perfType = IvsDelaysConstants.TAG_TYPE_ISSI;
      JFrame frame = new JFrame("Select Files from Directory:");
      PerformanceTypeDialog dialog = 
         new PerformanceTypeDialog(frame,true,title,perfType);
      boolean answer = dialog.getAnswer();
      showln("answer="+answer);
      boolean isSrcConsole = dialog.getSrcConsole();
      showln("isSrcConsole="+isSrcConsole);
      boolean isDstConsole = dialog.getDstConsole();
      showln("isDstConsole="+isDstConsole);
      System.exit(0);
   }
}
