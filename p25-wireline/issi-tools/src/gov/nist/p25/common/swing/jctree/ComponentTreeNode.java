//
package gov.nist.p25.common.swing.jctree;

/*
===================================================================
    ComponentTreeNode.java
    Created by Claude Duguay
    Copyright (c) 1998
    JComponent Tree, Java Developers Journal, V3 Issue 10.
===================================================================
*/

import java.awt.Component;
import javax.swing.tree.DefaultMutableTreeNode;


public class ComponentTreeNode extends DefaultMutableTreeNode
{
   private static final long serialVersionUID = -1L;
   
   public ComponentTreeNode(Component obj)
   {
      super(obj);
   }
   
   public ComponentTreeNode(Component obj, boolean allowsChildren)
   {
      super(obj, allowsChildren);
   }
   
   public Component getComponent()
   {
      return (Component)getUserObject();
   }
}
