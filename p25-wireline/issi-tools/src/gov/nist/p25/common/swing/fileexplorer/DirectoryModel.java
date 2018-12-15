//
package gov.nist.p25.common.swing.fileexplorer;

import java.io.File;
import java.io.FilenameFilter;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("unchecked")
public class DirectoryModel extends AbstractTableModel 
{
   private static final long serialVersionUID = -1L;
   
   protected FilenameFilter filter;
   protected File directory;
   protected String[] children;
   protected int rowCount;
   protected Object dirIcon;
   protected Object fileIcon;

   public DirectoryModel() {
      init();
   }
   public DirectoryModel( File dir ) {
      this( null, dir);
   }
   public DirectoryModel( FilenameFilter filter, File dir ) {
      this.filter = filter;
      init();
      directory = dir;
      children = dir.list( filter);
      rowCount = children.length;
   }

   protected void init() {
      dirIcon = UIManager.get( "DirectoryPane.directoryIcon" );
      fileIcon = UIManager.get( "DirectoryPane.fileIcon" );
   }

   public void setDirectory( File dir ) {
      if ( dir != null ) {
         directory = dir;
         children = dir.list( filter);
         rowCount = children.length;
      }
      else {
         directory = null;
         children = null;
         rowCount = 0;
      }
      fireTableDataChanged();
   }

   public int getRowCount() {
      return children != null ? rowCount : 0;
   }

   public int getColumnCount() {
      return children != null ? 3 :0;
   }

   public Object getValueAt(int row, int column){
      if ( directory == null || children == null ) {
         return null;
      }

      File fileSysEntity = new File( directory, children[row] );
      switch ( column ) {
      case 0:
         return fileSysEntity.isDirectory() ? dirIcon : fileIcon;
      case 1:
         return fileSysEntity.getName();
      case 2:
         if ( fileSysEntity.isDirectory() ) {
            return "--";
         }
         else {
            return new Long( fileSysEntity.length() );
         }
      default:
         return "";
      }
   }

   public String getColumnName( int column ) {
      switch ( column ) {
      case 0:
         return "Type";
      case 1:
         return "Name";
      case 2:
         return "Bytes";
      default:
         return "unknown";
      }
   }

   public Class getColumnClass( int column ) {
      if ( column == 0 ) {
         return getValueAt( 0, column).getClass();
      }
      else {
         return super.getColumnClass( column );
      }
   }
}               

