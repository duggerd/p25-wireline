//
package gov.nist.p25.issi.traceviewer;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;

import gov.nist.p25.common.swing.jctree.ComponentTreeLayout;
import gov.nist.p25.common.swing.jctree.ComponentTreeNode;
import gov.nist.p25.common.swing.jctree.JComponentTree;
import gov.nist.p25.common.swing.popup.PopupCommandEvent;
import gov.nist.p25.common.swing.popup.PopupCommandListener;
import gov.nist.p25.common.swing.popup.PopupCommandMenu;
import gov.nist.p25.common.swing.popup.PopupCommandMouseListener;
import gov.nist.p25.common.util.FileUtility;

import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;

/**
 * Test Layout panel.
 */
public class TestLayoutPanel extends JPanel
   implements PopupCommandListener
{
   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }


   private static final String TIP_RFSS = "RFSS - name, id.sysId.wacnId, ip, port";
   private static final String TIP_GROUP = "GROUP - name, id(hex), home RFSS";
   private static final String TIP_SU = "SU - name, id(hex), serving RFSS";
   
   // test layout functions
   private static final String TAG_SAVE_AS = "Save As";

   private static final String TAG_SEPARATOR = "~Separator~";
   private static final String TAG_DIRECTION_NORTH = "North";
   private static final String TAG_DIRECTION_EAST  = "East";
   private static final String TAG_DIRECTION_SOUTH = "South";
   private static final String TAG_DIRECTION_WEST  = "West";
   private static final String TAG_ALIGNMENT_LEFT_TOP = "Left/Top";
   private static final String TAG_ALIGNMENT_CENTER = "Center";
   private static final String TAG_ALIGNMENT_RIGHT_BOTTOM = "Right/Bottom";
   private static final String TAG_LINETYPE_STRAIGHT = "Straight";
   private static final String TAG_LINETYPE_SQUARE = "Square";
   private static final String TAG_VERTICAL_GAP_PLUS_50 = "VGap: +50%";
   private static final String TAG_VERTICAL_GAP_MINUS_50 = "VGap: -50%";
   private static final String TAG_HORIZONTAL_GAP_PLUS_50 = "HGap: +50%";
   private static final String TAG_HORIZONTAL_GAP_MINUS_50 = "HGap: -50%";

   private static final String[] plist = {
      TAG_SAVE_AS,
      TAG_SEPARATOR,
      TAG_DIRECTION_NORTH,
      TAG_DIRECTION_EAST,
      TAG_DIRECTION_SOUTH,
      TAG_DIRECTION_WEST,
      TAG_SEPARATOR,
      TAG_ALIGNMENT_LEFT_TOP,
      TAG_ALIGNMENT_CENTER,
      TAG_ALIGNMENT_RIGHT_BOTTOM,
      TAG_SEPARATOR,
      TAG_LINETYPE_STRAIGHT,
      TAG_LINETYPE_SQUARE,
      TAG_SEPARATOR,
      TAG_VERTICAL_GAP_PLUS_50,
      TAG_VERTICAL_GAP_MINUS_50,
      TAG_HORIZONTAL_GAP_PLUS_50,
      TAG_HORIZONTAL_GAP_MINUS_50,
   };

   private String title;
   private String base;
   private TopologyConfig topologyConfig;
   private JComponentTree tree;
   private HashMap<String,ComponentTreeNode> nodesMap;
   private PopupCommandMenu popupMenu;
   private JScrollPane scrollTree;


   // constructor
   public TestLayoutPanel( )
   {
      super();
   }

   public void setTopologyConfig(String title, String base, 
      TopologyConfig topologyConfig)
   {
      this.title = title;
      this.base = base;
      this.topologyConfig = topologyConfig;
      buildGUI();
      updateUI();
   }

   public void buildGUI()
   {
      removeAll();
      setLayout( new BorderLayout());
      nodesMap = new HashMap<String, ComponentTreeNode>();

      tree = new JComponentTree(JComponentTree.SOUTH,
                   JComponentTree.CENTER,
                   JComponentTree.SQUARE);

      ComponentTreeNode root = tree.addNode(null, new JLabel(base));
      ComponentTreeNode parent = root;
      ComponentTreeNode node = null;
      JButton c = null;
      Icon icon = null;
 
      // RFSS Configuration
      //for (RfssConfig rfssConfig: topologyConfig.getRfssConfigurations())
      //
      ArrayList<RfssConfig> rfssList = new ArrayList<RfssConfig>(topologyConfig.getRfssConfigurations());
      Collections.sort( rfssList);
      
      for (RfssConfig rfssConfig: rfssList)
      {
         String rname = rfssConfig.getRfssName();
         String rid = rfssConfig.getRfssIdString();
         String systemId = rfssConfig.getSystemIdString();
         String wacnId = rfssConfig.getWacnIdString();
         String ipAddress = rfssConfig.getIpAddress();
         int port = rfssConfig.getSipPort();

         String text = "<html>" + rname;
         text += "<br>" + rid +"." + systemId +"." + wacnId +
                 "<br>" + ipAddress +
                 "<br>" + port + "</html>";
         c = new JButton( text);
         c.setToolTipText(TIP_RFSS);
         if(  rfssConfig.isEmulated()) {
            icon = new ImageIcon("icon/rfss_emulated.gif");
            c.setIcon( icon);
         }

         node = tree.addNode( parent, c);
         nodesMap.put( rname, node);
      }

      // GROUP Configuration
      for (GroupConfig gc: topologyConfig.getGroupConfigurations())
      {
         String rname = gc.getHomeRfss().getRfssName();
         parent = nodesMap.get( rname);
         if( parent==null) continue;

         String gname = gc.getGroupName();
         String gid = gc.getGroupIdString();

         //int gwacnId =  gc.getSysConfig().getWacnId();
         //int gsystemId = gc.getSysConfig().getSystemId();
         String grfss = gc.getHomeRfss().getRfssName();
         String gtext = "<html>" + gname + "<br>" + gid +
                        "<br>" + grfss + "</html>";
         
         c = new JButton( gtext);
         c.setToolTipText(TIP_GROUP);
         icon = new ImageIcon("icon/su_group.gif");
         c.setIcon( icon);

         node = tree.addNode( parent, c);
         nodesMap.put( gname, node);

         parent = node;
         for (Iterator<SuConfig> it = gc.getSubscribers(); it.hasNext();)
         {
            SuConfig suConfig = it.next();
            String sname = suConfig.getSuName();
            String sid = suConfig.getSuIdString();

            //int swacnId = suConfig.getWacnId();
            //int ssystemId = suConfig.getSystemId();
            String servRFSS = suConfig.getInitialServingRfss().getRfssName();
            String stext = "<html>" + sname + "<br>" + sid +
                           "<br>" + servRFSS + "</html>";
            c = new JButton( stext);
            c.setToolTipText(TIP_SU);
            if( suConfig.isEmulated()) {
               icon = new ImageIcon("icon/su_emulated.gif");
               c.setIcon( icon);
            }

            node = tree.addNode( parent, c);
            nodesMap.put( sname, node);
         }
      }

      // SU Configuration
      for (SuConfig suConfig: topologyConfig.getSuConfigurations())
      {
         String rname = suConfig.getHomeRfss().getRfssName();
         parent = nodesMap.get( rname);
         if( parent==null) continue;

         String sname = suConfig.getSuName();

         // check if it is in group
         node = nodesMap.get( sname);
         if( node != null) continue;

         String sid = suConfig.getSuIdString();
         //int swacnId = suConfig.getWacnId();
         //int ssystemId = suConfig.getSystemId();
         String servRFSS = suConfig.getInitialServingRfss().getRfssName();
         String stext = "<html>" + sname + "<br>" + sid +
                        "<br>" + servRFSS + "</html>";
         c = new JButton( stext);
         c.setToolTipText(TIP_SU);
         if( suConfig.isEmulated()) {
            icon = new ImageIcon("icon/su_emulated.gif");
            c.setIcon( icon);
         }
         node = tree.addNode( parent, c);
         nodesMap.put( sname, node);
      }

      // add tree to panel
      JPanel header = new JPanel();
      //header.add(new JLabel("<html><b>" +title +"</b></html>"));
      JLabel label = new JLabel(title);
      Font font = label.getFont();
      label.setFont( font.deriveFont( font.getStyle() ^ Font.BOLD));
      header.add(label);
      
      add("North", header);
      scrollTree = new JScrollPane(tree);
      add("Center", scrollTree);

      // build optional popupmenu
      PopupCommandListener listener = this;
      if( plist != null)
      {
         popupMenu = new PopupCommandMenu(this,plist);
         if( listener != null)
            popupMenu.addPopupCommandListener( listener);

         PopupCommandMouseListener ml =
            new PopupCommandMouseListener(this,popupMenu);

         tree.addMouseListener( ml);
         //scrollTree.addMouseListener( ml);
         addMouseListener( ml);
      }
   }

   //-----------------------------------------------------------
   // implementation of PopupCommandListener
   //-----------------------------------------------------------
   public void processPopupCommand( PopupCommandEvent evt)
   {
      Object obj = evt.getSource();
      if( obj instanceof JMenuItem )
      {
         JMenuItem item = (JMenuItem)obj;
         String cmd = item.getActionCommand();
         //showln( "CMD="+cmd);

         //-----------------------------------
         if( TAG_DIRECTION_NORTH.equals(cmd)) {
            tree.setDirection(JComponentTree.NORTH);
         }
         else if( TAG_DIRECTION_EAST.equals(cmd)) {
            tree.setDirection(JComponentTree.EAST);
         }
         else if( TAG_DIRECTION_SOUTH.equals(cmd)) {
            tree.setDirection(JComponentTree.SOUTH);
         }
         else if( TAG_DIRECTION_WEST.equals(cmd)) {
            tree.setDirection(JComponentTree.WEST);
         }
         //-----------------------------------
         else if( TAG_ALIGNMENT_LEFT_TOP.equals(cmd)) {
            tree.setAlignment(JComponentTree.LEFT);
         }
         else if( TAG_ALIGNMENT_CENTER.equals(cmd)) {
            tree.setAlignment(JComponentTree.CENTER);
         }
         else if( TAG_ALIGNMENT_RIGHT_BOTTOM.equals(cmd)) {
            tree.setAlignment(JComponentTree.RIGHT);
         }
         //-----------------------------------
         else if( TAG_LINETYPE_STRAIGHT.equals(cmd)) {
            tree.setLineType(JComponentTree.STRAIGHT);
         }
         else if( TAG_LINETYPE_SQUARE.equals(cmd)) {
            tree.setLineType(JComponentTree.SQUARE);
         }
         //-----------------------------------
         else if( TAG_VERTICAL_GAP_PLUS_50.equals(cmd))
         {
            ComponentTreeLayout layout = (ComponentTreeLayout)tree.getLayout();
            int gap = layout.getVgap();
            gap += (gap+1) * 5/10;
            if( gap % 2 == 1) gap += 1;
            layout.setVgap( gap);
            tree.refresh();
         }
         else if( TAG_VERTICAL_GAP_MINUS_50.equals(cmd))
         {
            ComponentTreeLayout layout = (ComponentTreeLayout)tree.getLayout();
            int gap = layout.getVgap();
            gap -= (gap+1) * 5/10;
            if( gap % 2 == 1) gap += 1;
            layout.setVgap( gap);
            tree.refresh();
         }
         else if( TAG_HORIZONTAL_GAP_PLUS_50.equals(cmd))
         {
            ComponentTreeLayout layout = (ComponentTreeLayout)tree.getLayout();
            int gap = layout.getHgap();
            gap += (gap+1) * 5/10;
            if( gap % 2 == 1) gap += 1;
            layout.setHgap( gap);
            tree.refresh();
         }
         else if( TAG_HORIZONTAL_GAP_MINUS_50.equals(cmd))
         {
            ComponentTreeLayout layout = (ComponentTreeLayout)tree.getLayout();
            int gap = layout.getHgap();
            gap -= (gap+1) * 5/10;
            if( gap % 2 == 1) gap += 1;
            layout.setHgap( gap);
            tree.refresh();
         }
         //-----------------------------------
         else if( TAG_SAVE_AS.equals(cmd))
         {
            String suggestFile = getSuggestFileName();
            //showln("suggestFile="+suggestFile);

            JFileChooser fc = new JFileChooser( );
            File file = new File(suggestFile);
            fc.setSelectedFile( file);
            //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            int retval = fc.showSaveDialog(this);
            if( retval == JFileChooser.APPROVE_OPTION)
            {
               File save_file = fc.getSelectedFile();
               saveByFullpath(save_file.getAbsolutePath());
            }
         } 
      }
   }

   //-----------------------------------------------------------
   private String getSuggestFileName() {
      String filename = "test-layout-" + base + ".jpg";
      String outDir = System.getProperty("user.dir") +
               File.separator + "output" +
               File.separator + base;
      // create output directory if it doesnot exists
      FileUtility.makeDir( outDir);
      String suggestFile = outDir +
               File.separator + filename;
      return suggestFile;
   }

   public void autoSave() {
      String suggestFile = getSuggestFileName();
      showln("autoSave: suggestFile="+suggestFile);
      saveByFullpath( suggestFile);
   }

   private void saveByFullpath(String fullpath) {
      File save_file = new File(fullpath);
      try {
         FileOutputStream fos = new FileOutputStream(save_file);
         BufferedImage save_image = new BufferedImage(
                  this.getWidth(), this.getHeight(),
                  BufferedImage.TYPE_INT_RGB);

         Graphics gc = save_image.getGraphics();
         this.paint( gc);
         ImageIO.write(save_image, "jpg", fos);
         fos.close();
      }
      catch(Exception ex) {
         JOptionPane.showMessageDialog(null,
            "Error in saving image file: "+save_file.getAbsolutePath(),
            "Save As Error", 
            JOptionPane.ERROR_MESSAGE);
      }
   }
}

