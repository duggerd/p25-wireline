//
package gov.nist.p25.common.swing.fileexplorer;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.*;
import javax.swing.tree.TreePath;


public class FileExplorerPanel extends JPanel
{
   private static final long serialVersionUID = -1L;
   
   private boolean displayTable;
   private String startPath;
   private FilenameFilter filter;
   private FileSystemModel model;
   private DirectoryModel directoryModel;
   private JTable table;
   private FileSystemTreePanel fileTree;

   public static void showln(String s) { System.out.println(s); }

   // accessor
   public FileSystemTreePanel getFileSystemTreePanel() {
      return fileTree;
   }

   public List<String> getFilesList() {
      TreePath[] paths = fileTree.getTree().getSelectionPaths();
      List<String> list = new ArrayList<String>();
      for( TreePath path: paths) {
         Object[] objs = path.getPath();
	 list.add( objs[objs.length-1].toString());
      }
      return list;
   }

   // constructor
   public FileExplorerPanel(FilenameFilter filter, boolean displayTable, String startPath)
   {
      this.filter = filter;
      this.displayTable = displayTable;
      this.startPath = startPath;
      buildGUI();
   }

   public void buildGUI()
   {
      model = new FileSystemModel( filter, startPath);
      directoryModel = new DirectoryModel( filter, (File)model.getRoot() );

      if( displayTable) {
         table = new JTable( directoryModel );
         table.setShowHorizontalLines( false );
         table.setShowVerticalLines( false );
         table.setIntercellSpacing( new Dimension( 0, 2 ) );
         table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
         table.getColumn( "Type" ).setCellRenderer( new DirectoryRenderer() );
         table.getColumn( "Type" ).setMaxWidth( 32 );
         table.getColumn( "Type" ).setMinWidth( 32 );
      }

      fileTree = new FileSystemTreePanel( model );
      fileTree.getTree().addTreeSelectionListener( new TreeListener( directoryModel ) );

      JScrollPane treeScroller = new JScrollPane( fileTree);
      treeScroller.setMinimumSize( new Dimension( 0, 0));
      treeScroller.setPreferredSize( new Dimension( 400, 420));

      if( displayTable) {
         JScrollPane tableScroller = new JScrollPane( table);
         tableScroller.setMinimumSize( new Dimension( 0, 0));
         tableScroller.setPreferredSize( new Dimension( 400, 420));

         tableScroller.setBackground( Color.white );
         JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
                                    treeScroller,
                                    tableScroller );
         splitPane.setContinuousLayout( true );
         add( splitPane );
      }
      else {
         add( treeScroller );
      }
   }

   protected static class TreeListener implements TreeSelectionListener {
      DirectoryModel model;

      public TreeListener( DirectoryModel mdl ) {
         model = mdl;
      }
      public void valueChanged( TreeSelectionEvent e ) {
         File fileSysEntity = (File)e.getPath().getLastPathComponent();
         if ( fileSysEntity.isDirectory() ) {
            model.setDirectory( fileSysEntity );
         }
         else {
            model.setDirectory( null );
         }
      }
   }

   //=====================================================================
   public static void main( String[] argv ) {

      // only allow *.pcap files
      FilenameFilter filter = new FilenameFilter() {
	     public boolean accept(File file, String name) {
            //boolean bflag = file.isDirectory() || name.endsWith(".pcap");
	        String fpath = file.getAbsolutePath()+File.separator+name;
	        File xfile = new File(fpath);
	        boolean bflag = xfile.isDirectory() || name.endsWith("pcap");
	        //if( bflag)
               //showln("accept(TRUE): fpath="+fpath+"  name="+name);
	        return bflag;
         }
      };

      JFrame frame = new JFrame( "FileExplorerPanel" );
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      FileExplorerPanel panel = new FileExplorerPanel(filter,false,"/");
      frame.getContentPane().add( panel );
      frame.setSize( 440, 460);
      frame.pack();
      frame.setVisible(true);
   }
}
