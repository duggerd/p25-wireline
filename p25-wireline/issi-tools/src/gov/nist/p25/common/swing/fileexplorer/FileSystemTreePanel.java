//
package gov.nist.p25.common.swing.fileexplorer;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JPanel;
import javax.swing.JTree;


public class FileSystemTreePanel extends JPanel {
	
   private static final long serialVersionUID = -1L;
   private JTree tree;

   // accessor
   public JTree getTree() {
      return tree;
   }

   // constructor
   public FileSystemTreePanel() {
      this( new FileSystemModel() );
   }

   public FileSystemTreePanel( String startPath ) {
      this( new FileSystemModel( startPath ) );
   }
   public FileSystemTreePanel( FilenameFilter filter, String startPath ) {
      this( new FileSystemModel( filter, startPath ) );
   }

   public FileSystemTreePanel( FileSystemModel model ) {
      tree = new JTree( model ) {
         private static final long serialVersionUID = -1L;
         public String convertValueToText(Object value, boolean selected,
                       boolean expanded, boolean leaf, int row,
                       boolean hasFocus) {
            return ((File)value).getName();
         }
      };

      //tree.setLargeModel( true );      
      tree.setRootVisible( false );
      tree.setShowsRootHandles( true );
      tree.putClientProperty( "JTree.lineStyle", "Angled" );

      setLayout( new BorderLayout() );
      add( tree, BorderLayout.CENTER );
   }
}
