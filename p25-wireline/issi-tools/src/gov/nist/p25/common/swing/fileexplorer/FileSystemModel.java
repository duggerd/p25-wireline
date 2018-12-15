//
package gov.nist.p25.common.swing.fileexplorer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import javax.swing.tree.TreePath;

//
// http://java.sun.com/products/jfc/tsc/articles/jtree/
// Understanding the TreeModel, Eric Armstrong, Tom Santos, and Steve Wilson.
// Sun Developer's Network.
//
public class FileSystemModel extends AbstractTreeModel implements Serializable {
	
   private static final long serialVersionUID = -1L;

   private String root;
   private FilenameFilter filter;

   // constructor
   public FileSystemModel() {
      this( null, null);
   }
   public FileSystemModel( String startPath ) {
      this( null, startPath);
   }
   public FileSystemModel( FilenameFilter filter, String startPath ) {
      this.filter = filter;
      root = (startPath==null ? System.getProperty("user.home") : startPath);
   }

   public Object getRoot() {
      return new File( root );
   }

   public Object getChild( Object parent, int index ) {
      File directory = (File)parent;
      String[] children = directory.list( filter);
      return new File( directory, children[index] );
   }

   public int getChildCount( Object parent ) {
      File fileSysEntity = (File)parent;
      if ( fileSysEntity.isDirectory() ) {
         String[] children = fileSysEntity.list( filter);
         return children.length;
      }
      else {
         return 0;
      }
   }

   public boolean isLeaf( Object node ) {
      return ((File)node).isFile();
   }

   public void valueForPathChanged( TreePath path, Object newValue ) {
   }

   public int getIndexOfChild( Object parent, Object child ) {
      File directory = (File)parent;
      File fileSysEntity = (File)child;
      String[] children = directory.list( filter);
      int result = -1;

      for ( int i = 0; i < children.length; ++i ) {
         if ( fileSysEntity.getName().equals( children[i] ) ) {
            result = i;
            break;
         }
      }
      return result;
   }
}
