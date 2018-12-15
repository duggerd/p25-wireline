//
package gov.nist.p25.common.swing.fileexplorer;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import gov.nist.p25.common.swing.widget.GridBagConstraints2;
import gov.nist.p25.common.swing.widget.Filler;

@SuppressWarnings("unchecked")
public class SortedFileListDialog extends JDialog
   implements ActionListener
{
   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private boolean answer = false;
   private String startPath = "/";
   private FilenameFilter filter;
   private FileExplorerPanel filePanel;
   private SortedListModel dstModel;
   private JList dstList;

   private JButton addButton;
   private JButton removeButton;
   private JButton okButton;
   private JButton cancelButton;

   // accessor
   public boolean getAnswer() {
      return answer;
   }
   public SortedListModel getSortedListModel() {
      return dstModel;
   }
   
   // constructor
   public SortedFileListDialog(JFrame frame, boolean modal, 
      FilenameFilter filter, String startPath, String title)
      throws Exception
   {
      super( frame, modal);
      setTitle( title);
      this.filter = filter;
      this.startPath = startPath;
      buildGUI( frame);
   }

   protected void buildGUI( JFrame frame)
      throws Exception
   {
      setLayout( new GridBagLayout());

      // left pane
      filePanel = new FileExplorerPanel(filter,false,startPath);
      filePanel.setMinimumSize(new Dimension(400, 460));
      filePanel.setPreferredSize(new Dimension(400, 460));
      filePanel.setBorder(BorderFactory.createTitledBorder("Files List"));

      // middle button panel
      addButton = new JButton("Add");
      addButton.addActionListener(this);
      removeButton = new JButton("Remove");
      removeButton.addActionListener(this);
      okButton = new JButton("OK");
      okButton.addActionListener(this);
      cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(this);

      Filler filler1 = new Filler( 80, 100);

      JPanel buttonPanel = new JPanel(new GridLayout(10,1));
      buttonPanel.setPreferredSize(new Dimension(80, 220));
      buttonPanel.add( addButton);
      buttonPanel.add( new Filler(80,10));
      buttonPanel.add( removeButton);
      buttonPanel.add( new Filler(80,24));
      buttonPanel.add( new Filler(80,24));
      buttonPanel.add( new Filler(80,24));
      buttonPanel.add( new Filler(80,24));
      buttonPanel.add( okButton);
      buttonPanel.add( new Filler(80,10));
      buttonPanel.add( cancelButton);

      // right pane
      this.dstModel = new SortedListModel();
      dstList = new JList(dstModel);
      JScrollPane scrollPane2 = new JScrollPane(dstList);
      scrollPane2.setMinimumSize(new Dimension(400, 460));
      scrollPane2.setPreferredSize(new Dimension(400, 460));
      scrollPane2.setBorder(BorderFactory.createTitledBorder("Selected Files"));


      GridBagConstraints2 c = new GridBagConstraints2();
      // x, y, w, h, anchor, fill, l, t, r, b

      // layout
      c.set( 0, 0, 4, 8, "c", "both", 2, 2, 2, 2);
      add( filePanel, c);

      c.set( 4, 0, 1, 1, "c", "both", 2, 2, 2, 2);
      add( filler1, c);

      c.set( 4, 1, 1, 2, "c", "both", 2, 2, 2, 2);
      add( buttonPanel, c);

      c.set( 5, 0, 4, 8, "c", "both", 2, 2, 2, 2);
      add( scrollPane2, c);

      pack();
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
      if( src == addButton) {
         List<String> filesList = filePanel.getFilesList();
         //showln("ADD: "+ filesList);
         for( String fname: filesList) {
            dstModel.add( fname);
         }
      }
      else if( src == removeButton) {
         Object[] objs = dstList.getSelectedValues();
         //showln("REMOVE: " + objs);
         for( Object obj: objs) {
            dstModel.remove( obj);
         }
      }
      else if( src == okButton) {
         //showln("OK: ...generate XML");
         answer = true;
	 setVisible(false);
      }
      else if( src == cancelButton) {
         //showln("CANCEL: ");
         answer = false;
	 setVisible(false);
      }
   }

   //===============================================================
   public static void main(String args[]) throws Exception {

      // only allow *.pcap files
      FilenameFilter filter = new FilenameFilter() {
	 public boolean accept(File file, String name) {
	    String fpath = file.getAbsolutePath()+File.separator+name;
	    File xfile = new File(fpath);
	    boolean bflag = xfile.isDirectory() || name.endsWith("pcap") ||
               name.endsWith("PCAP");
	    //if( bflag)
            //   showln("accept(TRUE): fpath="+fpath+"  name="+name);
	    return bflag;
         }
      };

      //String startPath = "c:\\issi-data";
      String startPath = "/";
      String title = "Select a list of PCAP files:";
      JFrame frame = new JFrame("Select Files from Directory:");
      SortedFileListDialog dialog = new SortedFileListDialog(frame,true,
             filter,startPath,title);
      boolean answer = dialog.getAnswer();
      showln("answer="+answer);

      // display the selected files
      if( answer) {
         Iterator<String> iter = dialog.getSortedListModel().iterator();
         while( iter.hasNext()) {
            String fname = iter.next();
            showln("   -->"+ fname);
         }
      }
      System.exit(0);
   }
}
