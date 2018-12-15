//
package gov.nist.p25.common.swing.jctree;

/*
===================================================================
    JComponentTree.java
    Created by Claude Duguay
    Copyright (c) 1998
    JComponent Tree, Java Developers Journal, V3 Issue 10.
===================================================================
*/

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.CellRendererPane;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;

@SuppressWarnings("unchecked")
public class JComponentTree extends JPanel
   implements ComponentTreeConstants, TreeModelListener
{
   private static final long serialVersionUID = -1L;
   
   protected ComponentTreeLayout treeLayout;
   protected CellRendererPane pane;

   public ComponentTreeLayout getComponentTreeLayout() { return treeLayout; }
   public static void showln(String s) { System.out.println(s); }
   
   public JComponentTree()
   {
      //setBackground(Color.lightGray);
      //setForeground(Color.black);
      treeLayout = new ComponentTreeLayout(this);
      setLayout(treeLayout);
   }

   public JComponentTree(int hgap, int vgap)
   {
      treeLayout = new ComponentTreeLayout(this,hgap,vgap);
      setLayout(treeLayout);
   }

   public JComponentTree(
      int direction, int alignment, int linetype)
   {
      treeLayout = new ComponentTreeLayout(this,
         direction, alignment, linetype);
      setLayout(treeLayout);
   }

   public JComponentTree(
      int direction, int alignment, int linetype,
      int hgap, int vgap)
   {
      treeLayout = new ComponentTreeLayout(this,
         direction, alignment, linetype, hgap, vgap);
      setLayout(treeLayout);
   }

   //----------------------------------------------------------------
   public ComponentTreeNode addNode(
      ComponentTreeNode parent, Component child)
   {
      ComponentTreeNode node = new ComponentTreeNode(child);
      if (parent == null)
         setRoot(node);
      else
         treeLayout.addNode(parent, node);
      add(child);
      return node;
   }
   
   public void setDirection(int direction)
   {
      treeLayout.setDirection(direction);
      setSize(getPreferredSize());
      doLayout();
      repaint();
   }
    
   public int getDirection()
   {
      return treeLayout.getDirection();
   }
   
   public void setAlignment(int alignment)
   {
      treeLayout.setAlignment(alignment);
      doLayout();
      repaint();
   }
    
   public int getAlignment()
   {
      return treeLayout.getAlignment();
   }
   
   public void setLineType(int lineType)
   {
      treeLayout.setLineType(lineType);
      doLayout();
      repaint();
   }

   public int getLineType()
   {
      return treeLayout.getLineType();
   }
   
   public void setLineColor(Color linecolor)
   {
      treeLayout.setLineColor(linecolor);
      doLayout();
      repaint();
   }

   public Color getLineColor()
   {
      return treeLayout.getLineColor();
   }
   
   public void setLineShadow(Color lineshadow)
   {
      treeLayout.setLineShadow(lineshadow);
      doLayout();
      repaint();
   }

   public Color getLineShadow()
   {
      return treeLayout.getLineShadow();
   }
   
   public void setRoot(ComponentTreeNode root)
   {
      treeLayout.setRoot(root);
   }

   //----------------------------------------------------------------
   public ComponentTreeNode getRoot()
   {
      return treeLayout.getRoot();
   }

   public void setModel(DefaultTreeModel model)
   {
      treeLayout.setModel(model);
   }

   public DefaultTreeModel getModel()
   {
      return treeLayout.getModel();
   }

   public void paintComponent(Graphics g)
   {
      super.paintComponent(g);
      treeLayout.drawLines(this, g);
   }

   public Insets getInsets()
   {
      return new Insets(10, 10, 10, 10);
   }

   public void treeNodesChanged(TreeModelEvent event)
   {
      doLayout();
      repaint();
   }

   public void treeNodesInserted(TreeModelEvent event)
   {
      doLayout();
      repaint();
   }

   public void treeNodesRemoved(TreeModelEvent event)
   {
      doLayout();
      repaint();
   }

   public void treeStructureChanged(TreeModelEvent event)
   {
      doLayout();
      repaint();
   }

   public void refresh()
   {
      doLayout();
      repaint();
   }

   //----------------------------------------------------------------
   public ComponentTreeNode searchNode( ComponentTreeNode parent,
     ComponentTreeNode target)
   {
      List nodes = searchNodes( parent, target);

      ComponentTreeNode node = null;
      if( nodes.size() > 0)
         node = (ComponentTreeNode)nodes.get(0);
      return node;
   }

   //----------------------------------------------------------------
   public List searchNodes( ComponentTreeNode parent,
     ComponentTreeNode target)
   {
      ComponentTreeNode node = null;
      ComponentTreeNode root = parent;
      if(parent == null)
         root = (ComponentTreeNode)getModel().getRoot();

      //showln("searchNode(): compare obj ...");
      List list = new ArrayList();
      if(root != null)
      {
        Enumeration xenum = root.breadthFirstEnumeration();
        while(xenum.hasMoreElements())
        {
           node = (ComponentTreeNode)xenum.nextElement();

           matchNodes( list, node, target);

        }
      }
      return list;
   }

   protected void matchNodes(List list, ComponentTreeNode node, 
      ComponentTreeNode target)
   {
           JComponent cnode = (JComponent)node.getComponent();
           JComponent tnode = (JComponent)target.getComponent();

           if(cnode instanceof AbstractButton && 
              tnode instanceof AbstractButton)
           {
              if(((AbstractButton)tnode).getText().equals( 
                 ((AbstractButton)cnode).getText()))
              {
                 list.add( node);
              }
           }
           else if(cnode instanceof AbstractButton && 
                   tnode instanceof JLabel)
           {
              if(((JLabel)tnode).getText().equals( 
                 ((AbstractButton)cnode).getText()))
              {
                 list.add( node);
              }
           }
           else if(cnode instanceof JLabel &&
                   tnode instanceof JLabel)
           {
              if(((JLabel)tnode).getText().equals( 
                 ((JLabel)cnode).getText()))
              {
                 list.add( node);
              }
           }
           else if(cnode instanceof JLabel &&
                   tnode instanceof AbstractButton)
           {
              if(((AbstractButton)tnode).getText().equals( 
                 ((JLabel)cnode).getText()))
              {
                 list.add( node);
              }
           }
           else if(cnode instanceof JCheckBox && 
                   tnode instanceof JCheckBox)
           {
              if(((JCheckBox)tnode).getText().equals( 
                 ((JCheckBox)cnode).getText()))
              {
                 list.add( node);
              }
           }
           else if(cnode instanceof JComboBox && 
                   tnode instanceof JComboBox)
           {
              // we donot compare choices
              list.add( node);
           }
           else if(cnode instanceof JTextField && 
                   tnode instanceof JTextField)
           {
              // we donot compare field content
              list.add( node);
           }
           else
           {
              //showln("Unknown: cnode="+cnode.getClass().getName());
              //showln("Unknown: tnode="+tnode.getClass().getName());
              //showln("-------------");
           }
   }
}

