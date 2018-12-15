//
package gov.nist.p25.common.swing.fileexplorer;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;


public class FileExplorer {

   //=================================================================
   public static void main1( String[] argv ) {
      JFrame frame = new JFrame( "File Explorer" );
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      FileSystemModel model = new FileSystemModel("/");
      DirectoryModel directoryModel = new DirectoryModel( (File)model.getRoot() );
      JTable table = new JTable( directoryModel );
      table.setShowHorizontalLines( false );
      table.setShowVerticalLines( false );
      table.setIntercellSpacing( new Dimension( 0, 2 ) );
      table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
      table.getColumn( "Type" ).setCellRenderer( new DirectoryRenderer() );
      table.getColumn( "Type" ).setMaxWidth( 32 );
      table.getColumn( "Type" ).setMinWidth( 32 );

      FileSystemTreePanel fileTree = new FileSystemTreePanel( model );
      fileTree.getTree().addTreeSelectionListener( new TreeListener( directoryModel ) );

      JScrollPane treeScroller = new JScrollPane( fileTree );
      treeScroller.setMinimumSize( new Dimension( 0, 0 ) );

      JScrollPane tableScroller = new JScrollPane( table );
      tableScroller.setMinimumSize( new Dimension( 0, 0 ) );
      tableScroller.setBackground( Color.white );
      JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
                                    treeScroller,
                                    tableScroller );
      splitPane.setContinuousLayout( true );

      frame.getContentPane().add( splitPane );
      frame.setSize( 400, 400 );
      frame.pack();
      frame.setVisible(true);
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

   //=================================================================
   public static void main2( String[] argv ) {
      JFrame frame = new JFrame( "FileExplorer" );
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      FileExplorerPanel panel = new FileExplorerPanel(null,true,"/");
      frame.getContentPane().add( panel );
      frame.setSize( 700, 640 );
      frame.pack();
      frame.setVisible(true);
   }

   //=================================================================
   public static void main( String[] argv ) {
      main1( argv);
      //main2( argv);
   }
}
