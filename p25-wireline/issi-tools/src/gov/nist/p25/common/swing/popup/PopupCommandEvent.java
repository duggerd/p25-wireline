//
package gov.nist.p25.common.swing.popup;

import java.util.EventObject;

public class PopupCommandEvent extends EventObject
{
   private static final long serialVersionUID = -1L;
   
   private Object fstree;
   private Object datapath;
   private Object clientData;

   public Object getFSTree() { return fstree; }
   public Object getDataPath() { return datapath; }
   public Object getClientData() { return clientData; }

   public void setFSTree(Object obj) { fstree=obj; }
   public void setDataPath(Object path) { datapath=path; }
   public void setClientData(Object data) { clientData=data; }

   public PopupCommandEvent( Object src, Object fstree, Object path,
      Object data)
   {
      super( src );
      setFSTree( fstree );
      setDataPath( path );
      setClientData( data );
   }
}
