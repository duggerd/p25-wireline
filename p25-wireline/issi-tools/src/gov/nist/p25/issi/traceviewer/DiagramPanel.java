//
package gov.nist.p25.issi.traceviewer;

import gov.nist.p25.common.util.ByteArrayUtil;
import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.common.util.HexDump;
//import gov.nist.p25.issi.issiconfig.RfssConfig;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;

import org.apache.log4j.Logger;

/**
 * This class implements 2D graphics for the trace viewer sequence diagram.
 */
public class DiagramPanel extends JPanel
   implements ActionListener, MouseListener, MouseMotionListener,
              MouseWheelListener
{
   private static final long serialVersionUID = -1L;

   private static Logger logger = Logger.getLogger(DiagramPanel.class);
   public static void showln(String s) { System.out.println(s); }

   private static final String CMD_NEW_WINDOW = "New Window";
   private static final String CMD_AUTO_SAVE = "Auto Save";
   private static final String CMD_SAVE_AS = "Save As";
   private static final String CMD_CLOSE = "Close";
   private static final String CMD_CLOSE_ALL = "Close All";
   private static final String CMD_MARK_TIME = "Mark Time";
   private static final String CMD_RESET_TIME = "Reset Time";
   private static final String FONT_NAME = "Tahoma";

   private static final int ARC_HEIGHT = 60;
   private static final int ARC_WIDTH = 60;
   private static final int ARROW_WIDTH = 10;
   private static final int ARROW_TOP_HEIGHT = 5;
   private static final int FONT_PT_SIZE = 11;
   private static final int MESSAGE_SEPARATOR_PAD = 40;
   private static final int MESSAGE_LABEL_SEPARATOR_PAD = 10;
   private static final int MESSAGE_SELECT_HEIGHT = 20;
   private static final int NODE_SEPARATOR_PAD = 60;
   private static final int NODE_WIDTH = 130;
   private static final int NODE_HEIGHT = 60;
   private static final int TOP_PAD = 60;

   private final static BasicStroke stroke = new BasicStroke(1.0f);

   private TracePanel tracePanel;
   private JPopupMenu rightClickPopupMenu;
   private int mousePressedXPos;
   private int mousePressedYPos;
   private long clickedTime;
   private long timeZero;
   
   // constructor
   public DiagramPanel(TracePanel tracePanel) {

      this.tracePanel = tracePanel;
      setDoubleBuffered(true);
      setBackground(Color.white);
      addMouseListener(this);
      addMouseMotionListener(this);
      addMouseWheelListener( this);

      rightClickPopupMenu = new JPopupMenu();
      JMenuItem newWindowClickMenuItem = new JMenuItem(CMD_NEW_WINDOW);
      newWindowClickMenuItem.setActionCommand(CMD_NEW_WINDOW);
      newWindowClickMenuItem.addActionListener(this);
      
      JMenuItem autoSaveClickMenuItem = new JMenuItem(CMD_AUTO_SAVE);
      autoSaveClickMenuItem.setActionCommand(CMD_AUTO_SAVE);
      autoSaveClickMenuItem.addActionListener(this);

      JMenuItem saveAsClickMenuItem = new JMenuItem(CMD_SAVE_AS);
      saveAsClickMenuItem.setActionCommand(CMD_SAVE_AS);
      saveAsClickMenuItem.addActionListener(this);
      
      JMenuItem closeRightClickMenuItem = new JMenuItem(CMD_CLOSE);
      closeRightClickMenuItem.setActionCommand(CMD_CLOSE);
      closeRightClickMenuItem.addActionListener(this);
      
      JMenuItem closeAllMenuItem = new JMenuItem(CMD_CLOSE_ALL);
      closeAllMenuItem.setActionCommand(CMD_CLOSE_ALL);
      closeAllMenuItem.addActionListener(this);
      
      JMenuItem markTimeMenuItem = new JMenuItem(CMD_MARK_TIME);
      markTimeMenuItem.setActionCommand(CMD_MARK_TIME);
      markTimeMenuItem.addActionListener(this);

      JMenuItem resetTimeMenuItem = new JMenuItem(CMD_RESET_TIME);
      resetTimeMenuItem.setActionCommand(CMD_RESET_TIME);
      resetTimeMenuItem.addActionListener(this);

      rightClickPopupMenu.add(newWindowClickMenuItem);
      rightClickPopupMenu.addSeparator();
      rightClickPopupMenu.add(autoSaveClickMenuItem);
      rightClickPopupMenu.add(saveAsClickMenuItem);
      rightClickPopupMenu.addSeparator();
      rightClickPopupMenu.add(closeRightClickMenuItem);
      rightClickPopupMenu.add(closeAllMenuItem);
      rightClickPopupMenu.addSeparator();
      rightClickPopupMenu.add(markTimeMenuItem);
      rightClickPopupMenu.add(resetTimeMenuItem);
   }

   public int[] setData(Collection<RfssData> rfssList, List<MessageData> messageList) {

      int rfssSize = (rfssList == null ? 0 : rfssList.size());
      int totalWidth = rfssSize * (NODE_SEPARATOR_PAD + NODE_WIDTH);
      totalWidth += 2 * NODE_SEPARATOR_PAD;

      int messageListSize = (messageList == null ? 0 : messageList.size());
      int totalHeight = messageListSize * MESSAGE_SEPARATOR_PAD;
      totalHeight += TOP_PAD + NODE_HEIGHT + 2 * MESSAGE_SEPARATOR_PAD;
      //showln("setData(0): totalWidth="+totalWidth +" totalHeight="+totalHeight);

      int[] traceSize = new int[2];
      traceSize[0] = totalWidth;
      traceSize[1] = totalHeight;
      return traceSize;
   }

   private void disableMessageSelected() {
      List<MessageData> messageList = tracePanel.getMessageList();
      for (MessageData messageData : messageList) {
         if (messageData == null) continue;
         if (messageData.isSelected()) {
            messageData.setSelected(false);
            break;
         }
      }
   }

   private void disableRfssSelected() {
      Collection<RfssData> rfssList = tracePanel.getRfssList();
      if( rfssList != null)
      for (RfssData rfssData : rfssList) {
         if (rfssData == null) continue;
         if (rfssData.isSelected()) {
            rfssData.setSelected(false);
            break;
         }
      }
   }

   //----------------------------------------------------------------------
   private RfssData getRfssByMessageData(MessageData messageData, boolean isFrom) {
      //
      // This method will replace getRfssById and getRfssByPort
      //
      RfssData foundRfss = null;
      Collection<RfssData> rfssList = tracePanel.getRfssList();
      if( rfssList == null)
          return foundRfss;

      // assume from=0 and to=1
      String id = messageData.getFromRfssId();
      String port = messageData.getFromPort();
      if( !isFrom) {
         id = messageData.getToRfssId();
         port = messageData.getToPort();
      }

      //showln("getRfssByMessageData: id=" + id +" port="+port);
      if( id != null) {
         // 1. find Rfss by Id
         foundRfss = getRfssById( id);
      }
      if( foundRfss == null) {
         // 2. find Rfss by port
         foundRfss = getRfssByPort( port);
	 if( foundRfss == null) {
            // 3. find Rfss by id:port
            foundRfss = getRfssById( id+":"+port);
         }
      }
      return foundRfss;
   }
   
   //----------------------------------------------------------------------
   private RfssData getRfssById(String id) {
      //logger.debug("getRfssById: id=" + id);
      RfssData foundRfss = null;
      Collection<RfssData> rfssList = tracePanel.getRfssList();
      if( rfssList != null)
      for (RfssData rfssData: rfssList) {
         // Note: RfssData will represent RFSS, BSI and FSI
	 // Use getId() instead of RfssConfig and getDomainName()
         //logger.debug("getRfssById: getId=" + rfssData.getId());
         if (rfssData.getId().equals(id)) {
            foundRfss = rfssData;
            break;
         }
      }
      return foundRfss;
   }

   private RfssData getRfssByPort(String seekPort) {
      //logger.debug("getRfssByPort: seekPort=" + seekPort);
      RfssData foundRfss = null;
      Collection<RfssData> rfssList = tracePanel.getRfssList();
      if( rfssList != null)
      for (RfssData rfssData: rfssList) {
         for (String port: rfssData.getPorts()) {
            if (port.equals(seekPort)) {
               foundRfss = rfssData;
               break;
            }
         }
         if (foundRfss != null) {
            break;
         }
      }
      if (foundRfss == null) {
         logger.debug("List of rfss: " + rfssList);
         if( rfssList != null)
         for (RfssData rfssData: rfssList) {
            logger.debug("ports = " + rfssData.getPorts());
         }
      }
      return foundRfss;
   }

   private RfssData getRfssByLocation(int mouseX, int mouseY) {
      RfssData foundRfss = null;
      Collection<RfssData> rfssList = tracePanel.getRfssList();
      if( rfssList != null)
      for (RfssData rfssData: rfssList) {
         if ((mouseX >= rfssData.getX() && 
              mouseX <= (rfssData.getX() + rfssData.getDimension().width)) &&
             (mouseY >= rfssData.getY() &&
              mouseY <= (rfssData.getY() + rfssData.getDimension().height))) {
            foundRfss = rfssData;
            break;
         }
      }
      return foundRfss;
   }

   private MessageData getMessageByLocation(int mouseX, int mouseY) {
      MessageData foundMessage = null;
      List<MessageData> messageList = tracePanel.getMessageList();
      for (MessageData messageData: messageList) {
	 if( messageData == null) continue;
	 if( messageData.getDimension() == null) continue;
         if ((mouseX >= messageData.getX() &&
              mouseX <= (messageData.getX() + messageData.getDimension().width)) &&
             (mouseY >= messageData.getY() && 
              mouseY <= (messageData.getY() + messageData.getDimension().height))) {
            foundMessage = messageData;
            break;
         }
      }
      return foundMessage;
   }

   public String getSortedMessageData() {
      //showln("getSortedMessageData: START...");
      StringBuffer sbuf = new StringBuffer();
      List<MessageData> messageList = tracePanel.getMessageList();
      for (MessageData messageData: messageList) {
	 if( messageData == null) continue;
         sbuf.append("F"+messageData.getId()+":\n");
	 sbuf.append(messageData.getData().trim());
         sbuf.append( "\n\n");
      }
      //showln("getSortedMessageData: sbuf="+sbuf);
      return sbuf.toString();
   }

   //----------------------------------------------------------------------
   protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g;
      double zoomValue = 1.0D;
      int newFontSize = (int) (FONT_PT_SIZE * zoomValue);
      g2d.setFont(new Font(FONT_NAME, Font.PLAIN, newFontSize));
      FontMetrics metrics = g2d.getFontMetrics();
      drawNodes(g2d, zoomValue, metrics);
      drawMessages(g2d, zoomValue, metrics);
   }

   public void saveAs(String fileName) {
      logger.debug("saveAs(): fileName=" +fileName +".jpg");
      try {
         // Compute the size of the image first
         int rfssListSize = tracePanel.getRfssList().size();
         int messageListSize = tracePanel.getMessageList().size();

         int width = NODE_SEPARATOR_PAD 
               + rfssListSize * (NODE_WIDTH + NODE_SEPARATOR_PAD);
         int height = TOP_PAD + NODE_HEIGHT + 2 * MESSAGE_SEPARATOR_PAD
               + messageListSize * MESSAGE_SEPARATOR_PAD;

         BufferedImage bufferedImage = new BufferedImage(width, height,
               BufferedImage.TYPE_INT_RGB);

         Graphics2D g2d = bufferedImage.createGraphics();
         double zoomValue = 1.0D;
         int newFontSize = (int) (FONT_PT_SIZE * zoomValue);
         g2d.setFont(new Font(FONT_NAME, Font.PLAIN, newFontSize));
         FontMetrics metrics = g2d.getFontMetrics();
         g2d.setColor(Color.white);
         g2d.fillRect(0, 0, width, height);
         drawNodes(g2d, zoomValue, metrics);
         drawMessages(g2d, zoomValue, metrics);

         OutputStream out = new FileOutputStream(fileName);
         ImageIO.write(bufferedImage, "jpg", out);
         out.close();
      } 
      catch (Exception e) {
         logger.error("An exception occured while trying to save file", e);
         JOptionPane.showMessageDialog(null,
            "An Error Occured while saving the file",
            "Save File Error", JOptionPane.ERROR_MESSAGE);
      }
   }

   private String getSuggestFileName() {
      String testNo = tracePanel.getTestNumber();
      String outDir = System.getProperty("user.dir") + 
            File.separator + "output" + 
            File.separator + testNo;
      // create output directory if it doesnot exists
      FileUtility.makeDir( outDir);
      String suggestFile = outDir + File.separator + 
            tracePanel.getTitle() + "-" + testNo;
      //showln("suggestFile="+suggestFile);
      return suggestFile;
   }

   public void saveAs() {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Save As");
      String testNo = tracePanel.getTestNumber();
      String suggestFile = System.getProperty("user.dir") + 
            File.separator + "output" + 
            File.separator + testNo +
            File.separator + tracePanel.getTitle();
      //showln("suggestFile="+suggestFile+"  testNo="+testNo);
      
      if( testNo != null && testNo.length() > 0) {
         suggestFile += "-" + testNo;
      }
      File sfile = new File(suggestFile);
      fileChooser.setSelectedFile( sfile);
      
      TraceFileFilter traceFilter = new TraceFileFilter();
      traceFilter.addExtension("jpg");
      traceFilter.setDescription("JPG Files");
      fileChooser.setFileFilter(traceFilter);

      int returnVal = fileChooser.showSaveDialog(tracePanel.getTabbedPane());
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         File file = fileChooser.getSelectedFile();
         String fullpath = file.getAbsolutePath();
         logger.debug("Saving: " + fullpath);
         saveByFullpath( fullpath);
      }
   }

   public void autoSave() {
      String suggestFile = getSuggestFileName();
      saveByFullpath( suggestFile);
   }

   private void saveByFullpath(String fullpath) {
      //logger.debug("saveByFullpath: fullpath="+fullpath);
      saveAs(fullpath + ".jpg");
      
      // save translated message and raw message
      String data = tracePanel.getDataTextArea(TracePanel.DATA_ALL_MSG).getText();
      saveAsFile(fullpath + ".all.txt", data);
      
      data = tracePanel.getDataTextArea(TracePanel.DATA_RAW_MSG).getText();
      saveAsFile(fullpath + ".raw.txt", data);
      
      data = tracePanel.getDataTextArea(TracePanel.DATA_ERROR_MSG).getText();
      saveAsFile(fullpath + ".selraw.txt", data);
         
      data = tracePanel.getDataTextArea(TracePanel.DATA_SELECTED_MSG).getText();
      saveAsFile(fullpath + ".selmsg.txt", data);
   }
   
   private void saveAsFile( String fullpath, String message) {
      try {
         OutputStream out = new FileOutputStream(new File(fullpath));
         out.write(message.getBytes());
         out.close();
      }
      catch (Exception ex) {
         String msg = "An error occured while saving file: "+ fullpath;
         logger.error( msg, ex);
         JOptionPane.showMessageDialog(null, msg,
            "Save File Status", 
            JOptionPane.ERROR_MESSAGE);
      }
   }

   private void drawNodes(Graphics2D g2d, double zoomValue, FontMetrics metrics) {
      Collection<RfssData> rfssList = tracePanel.getRfssList();
      g2d.setColor(Color.black);
      
      String titleString = tracePanel.getTestNumber() +" : " + tracePanel.getTitle();
      Font g2dFont = g2d.getFont();
      Font titleFont = new Font(FONT_NAME, Font.BOLD, 12);
      g2d.setFont( titleFont);

      int rfssSize = (rfssList == null ? 1 : rfssList.size());
      int totalWidth = rfssSize * (NODE_SEPARATOR_PAD + NODE_WIDTH);
      int kx = totalWidth / 3 + NODE_SEPARATOR_PAD / 2;
      g2d.drawString( titleString, kx, TOP_PAD / 2);    
      g2d.setFont( g2dFont);

      //logger.error("drawNodes(): rfssList="+rfssList);
      int i = 0;
      if( rfssList != null)
      for (RfssData rfssData : rfssList) {

         Color color = Color.black;
	 /*** Donot use RfssConfig
         RfssConfig rfssConfig = rfssData.getRfssConfig();
         String idString = (rfssConfig != null ? rfssConfig.getDomainName() : rfssData.getId());
         String nameString = (rfssConfig != null ? rfssConfig.getRfssName()  : "");
         String ipAddrString = (rfssData.getIpAddress() != null ? rfssData.getIpAddress() : "");      
	  **/
         String idString = rfssData.getId();
         String nameString = rfssData.getName();
         String ipAddrString = rfssData.getIpAddress();

         int idStringWidth = metrics.stringWidth(idString);
         int idStringHeight = metrics.getHeight();
         int nameStringWidth = metrics.stringWidth(nameString);
         int ipAddrStringWidth = metrics.stringWidth(ipAddrString);
         
         /*** 
	 // encode SIP and PTT ports
         String portNumberString = "";
         for (int j = 0; j < rfssData.getPorts().size(); j++) {
            portNumberString += rfssData.getPorts().get(j);
            if (j < (rfssData.getPorts().size() - 1)) {
               portNumberString += ",";
            }
         }
          **/
         String portNumberString = rfssData.getPorts().get(0);
         int portNumberStringWidth = metrics.stringWidth(portNumberString);

         int x = NODE_SEPARATOR_PAD + i * (NODE_WIDTH + NODE_SEPARATOR_PAD);
         x = (int)(x * zoomValue);
         int y = (int) (TOP_PAD * zoomValue);

         double dx = zoomValue * NODE_WIDTH;
         double dy = zoomValue * NODE_HEIGHT;
	 int idx2 = (int) dx / 2;
	 int idy2 = (int) dy / 2;

         g2d.setPaint(new Color(255, 235, 205));
         g2d.fill(new Rectangle2D.Double(x, y, dx, dy));
         if (rfssData.isSelected())
            g2d.setPaint(Color.red);
         else
            g2d.setPaint(color);
         g2d.setStroke(stroke);
         g2d.draw(new Rectangle2D.Double(x, y, dx, dy + 2));
         
         g2d.drawString( nameString,
               x + (idx2 - nameStringWidth / 2),
               y + (idy2 - idStringHeight));
         g2d.drawString( idString,
               x + (idx2 - idStringWidth / 2),
               y + (idy2));
         g2d.drawString( ipAddrString,
               x + (idx2- ipAddrStringWidth / 2),
               y + (idy2 + idStringHeight));
         g2d.drawString( portNumberString,
               x + (idx2 - portNumberStringWidth / 2),
               y + (idy2 + idStringHeight * 2));

         List<MessageData> messageList = tracePanel.getMessageList();
         int messageListSize = messageList.size();
         g2d.drawLine(x + idx2,
               (int) (y + dy),
                      x + idx2,
               (int) (y + dy + MESSAGE_SEPARATOR_PAD 
                     + MESSAGE_SEPARATOR_PAD * messageListSize * zoomValue));
         rfssData.setX(x);
         rfssData.setY(y);
         rfssData.setDimension(new Dimension((int) dx, (int) dy));
         i++;
      }
   }

   private void drawMessages(Graphics2D g2d, double zoomValue, FontMetrics metrics)
   {
      List<MessageData> messageList = tracePanel.getMessageList();
      int messageListSize = messageList.size();
      int startX = 0;
      int startY = (int) (zoomValue * (TOP_PAD + NODE_HEIGHT));
      int endX = 0;
      int endY = startY;

      for (int i = 0; i < messageListSize; i++)
      {
         MessageData messageData = (MessageData) messageList.get(i);
         RfssData fromRfss = getRfssByMessageData(messageData,true);
        /***
         RfssData fromRfss = messageData.getFromRfssId() != null ? 
               getRfssById(messageData.getFromRfssId())
               : getRfssByPort(messageData.getFromPort());
         ***/
         if (fromRfss == null) {
            logger.error("Could not find an RFSS (fromRfss) "
                  + messageData.getFromRfssId() + ":"
                  + messageData.getFromPort());
            return;
         }
         RfssData toRfss = getRfssByMessageData(messageData,false);
        /***
         RfssData toRfss = messageData.getToRfssId() != null ? 
               getRfssById(messageData.getToRfssId())
               : getRfssByPort(messageData.getToPort());
         ***/
         if (toRfss == null) {
            logger.error("Could not find an RFSS "
                  + messageData.getFromRfssId() + ":"
                  + messageData.getToPort());
            return;
         }
         startX = fromRfss.getX() + (fromRfss.getDimension().width / 2);
         startY += (MESSAGE_SEPARATOR_PAD * zoomValue);
         endX = toRfss.getX() + (toRfss.getDimension().width / 2);
         endY = startY;

         // Set message color
         Color color = Color.black;
         if (messageData.isSelected()) {
            color = Color.red;
         }
         g2d.setColor(color);

         if (startX < endX) {

            // ----->
            messageData.setDirection(1);
            g2d.drawLine(startX, startY, endX, endY);

            int topX = endX - (int) (ARROW_WIDTH * zoomValue);
            int topY = endY - (int) (ARROW_TOP_HEIGHT * zoomValue);
            int bottomX = endX - (int) (ARROW_WIDTH * zoomValue);
            int bottomY = endY + (int) (ARROW_TOP_HEIGHT * zoomValue);
            int pointX = endX;
            int pointY = endY;
            int[] xPoints = { topX, bottomX, pointX };
            int[] yPoints = { topY, bottomY, pointY };
            int numPoints = 3;

            Polygon arrowHead = new Polygon(xPoints, yPoints, numPoints);
            g2d.fillPolygon(arrowHead);

            messageData.setX(startX);
            messageData.setY(startY - (int) (MESSAGE_SELECT_HEIGHT * zoomValue));
            messageData.setDimension(new Dimension((int) (endX - startX),
                  (int) (MESSAGE_SELECT_HEIGHT * 2 * zoomValue)));

            String msg = messageData.getMessageType();
            String message = "F" + messageData.getId() + " : " + msg;
            int messageStringWidth = metrics.stringWidth(message);

            int midX = endX - ((endX - startX) / 2);
            int stringX = midX - (messageStringWidth / 2);
            int stringY = startY - (int) (MESSAGE_LABEL_SEPARATOR_PAD * zoomValue);

            g2d.drawString(message, stringX, stringY);
	    
         } else if (startX > endX) {

            // <-----
            messageData.setDirection(-1);
            g2d.drawLine(startX, startY, endX, endY);

            int topX = endX + (int) (ARROW_WIDTH * zoomValue);
            int topY = endY - (int) (ARROW_TOP_HEIGHT * zoomValue);
            int bottomX = endX + (int) (ARROW_WIDTH * zoomValue);
            int bottomY = endY + (int) (ARROW_TOP_HEIGHT * zoomValue);
            int pointX = endX;
            int pointY = endY;
            int[] xPoints = { topX, bottomX, pointX };
            int[] yPoints = { topY, bottomY, pointY };
            int numPoints = 3;

            Polygon arrowHead = new Polygon(xPoints, yPoints, numPoints);
            g2d.fillPolygon(arrowHead);

            messageData.setX(endX);
            messageData.setY(endY - (int) (MESSAGE_SELECT_HEIGHT * zoomValue));
            messageData.setDimension(new Dimension((int) (startX - endX),
                  (int) (MESSAGE_SELECT_HEIGHT * 2 * zoomValue)));

            String msg = messageData.getMessageType();
            String message = "F" + messageData.getId() + " : " + msg;
            int messageStringWidth = metrics.stringWidth(message);

            int midX = endX - (endX - startX) / 2;
            int stringX = midX - messageStringWidth / 2;
            int stringY = startY - (int) (MESSAGE_LABEL_SEPARATOR_PAD * zoomValue);

            g2d.drawString(message, stringX, stringY);

         } else if (startX == endX) {

            g2d.draw(new Arc2D.Double((double) startX - (0.5D * ARC_WIDTH * zoomValue),
                        (double) startY,
                        ARC_WIDTH * zoomValue, 
		        ARC_HEIGHT * zoomValue,
		        270.0D, 180.0D,
                        Arc2D.OPEN));

            int topX = endX + (int) (zoomValue * ARROW_WIDTH);
            int topY = endY + (int) (zoomValue * (MESSAGE_SEPARATOR_PAD - ARROW_TOP_HEIGHT));
            int bottomX = endX + (int) (zoomValue * ARROW_WIDTH);
            int bottomY = endY + (int) (zoomValue * (MESSAGE_SEPARATOR_PAD + ARROW_TOP_HEIGHT));
            int pointX = endX;
            int pointY = endY + (int) (zoomValue * MESSAGE_SEPARATOR_PAD);
            int[] xPoints = { topX, bottomX, pointX };
            int[] yPoints = { topY, bottomY, pointY };
            int numPoints = 3;

            Polygon arrowHead = new Polygon(xPoints, yPoints, numPoints);
            g2d.fillPolygon(arrowHead);

            String message = messageData.getMessageType();
            int messageStringWidth = metrics.stringWidth(message);

            int stringX = (int) (startX - messageStringWidth / 2);
            messageData.setY(endY - (int) (MESSAGE_SELECT_HEIGHT * zoomValue));
            messageData.setDimension(new Dimension(
                  (int) messageStringWidth,
                  (int) (ARC_HEIGHT * zoomValue + MESSAGE_SELECT_HEIGHT * 2 * zoomValue)));

            int stringY = startY - (int) (MESSAGE_LABEL_SEPARATOR_PAD * zoomValue);
            g2d.drawString(message, stringX, stringY);

            int fontHeight = metrics.getHeight();
            g2d.drawString(new Integer(i).toString(),
                  (int) (NODE_SEPARATOR_PAD * zoomValue) + (int) (0.3D * NODE_WIDTH * zoomValue),
		  startY + (fontHeight / 2));
         }
      }
   }

   // implementation of MouseListener
   //--------------------------------------------------------------------
   public void mouseClicked(MouseEvent e) {

      if (e.getClickCount() == 2) {
         createNewWindow();
      }

      //showln("mouseClicked(): "+e.getX()+","+e.getY());
      RfssData rfssData = getRfssByLocation(e.getX(), e.getY());
      if (rfssData != null) {

         disableMessageSelected();
         disableRfssSelected();
         rfssData.setSelected(true);
         repaint();

         //showln("RFSS-time="+rfssData.getTimestamp());
	 clickedTime = rfssData.getTimestamp();
         setToolTipText("Time(ms): "+clickedTime +"  delta: "+(clickedTime-timeZero));

         //JTextArea textArea = tracePanel.getTextArea();
         String rfssInfo = rfssData.getDescription();
         //showln("mouseClicked: rfssInfo="+rfssInfo);
         tracePanel.setDataTextArea( TracePanel.DATA_SELECTED_MSG, rfssInfo);

      } else {

         disableRfssSelected();

         // Try messages
         MessageData messageData = getMessageByLocation(e.getX(), e.getY());
         if (messageData != null) {

            disableMessageSelected();
            messageData.setSelected(true);
            repaint();
   
	    // It could be SIP or PTT message
            //showln("MSG-time(2)="+messageData.getTimestamp());
	    try {
               clickedTime = messageData.getTimestamp();
               setToolTipText("Time(ms): "+clickedTime +"  delta: "+(clickedTime-timeZero));
	    } catch(Exception ex) { }

            //showln("PTT using getId():"+messageData.getId());
            String msg = "F"+messageData.getId()+":\n"+messageData.getData().trim();            
            tracePanel.setDataTextArea( TracePanel.DATA_SELECTED_MSG, msg);

            // display in hex: 80 04 1A 2B 04  ...
            String rawdata = messageData.getRawdata();
            //showln("PTT SELECTED - rawdata=<"+ rawdata + ">");

            int nparts = (rawdata.length() + 1)/3;
	    String[] parts = rawdata.split(" ");
            //showln("PTT SELECTED - nparts="+nparts +" parts.length="+parts.length);
	    if( nparts != parts.length) {
               String sbHex = HexDump.dump(rawdata.getBytes(), 0, 0);
               tracePanel.setDataTextArea( TracePanel.DATA_ERROR_MSG, sbHex);
            } 
            else {
               byte[] hexArray = ByteArrayUtil.fromHexString( rawdata);
               if( hexArray != null) {
                  String sbHex = HexDump.dump(hexArray, 0, 0);
   	          if( sbHex != null)
                     tracePanel.setDataTextArea( TracePanel.DATA_ERROR_MSG, sbHex);
   	       }
            }

         } else {
            disableMessageSelected();
            repaint();
         }
      }
   }

   public void mousePressed(MouseEvent e) {
      int buttonNumber = e.getButton();
      if (buttonNumber == 3) {
         rightClickPopupMenu.show(this, e.getX(), e.getY());
      }
      else {
         mousePressedXPos = e.getX();
         mousePressedYPos = e.getY();
         setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      }
   }

   public void mouseReleased(MouseEvent e) {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
   }

   public void mouseEntered(MouseEvent e) {
      //showln("Mouse location: "+e.getX()+","+e.getY());
      RfssData rfssData = getRfssByLocation(e.getX(), e.getY());
      if (rfssData != null) {
         setToolTipText("Time(ms): "+rfssData.getTimestamp());
      }
      else {
         MessageData messageData = getMessageByLocation(e.getX(), e.getY());
         if( messageData != null) {
            setToolTipText("Time(ms): "+messageData.getTimestamp());
         }
	 else {
            setToolTipText(null);
	 }
      }
   } 
   public void mouseExited(MouseEvent e) { }
   public void mouseMoved(MouseEvent arg0) { }

   public void mouseDragged(MouseEvent e) {
     Container c = getParent();
     if( c instanceof JViewport) {
        JViewport jv = (JViewport) c;
        Point p = jv.getViewPosition();
        int newX = p.x - (e.getX() - mousePressedXPos);
        int newY = p.y - (e.getY() - mousePressedYPos);
  
        int maxX = this.getWidth() - jv.getWidth();
        int maxY = this.getHeight() - jv.getHeight();
        if (newX < 0)
           newX = 0;
        if (newX > maxX)
           newX = maxX;
        if (newY < 0)
           newY = 0;
        if (newY > maxY)
           newY = maxY;
        jv.setViewPosition(new Point(newX, newY));
     }
   }

   // implementation of ActionListener
   //--------------------------------------------------------------------
   public void actionPerformed(ActionEvent ae) {
      String cmd = ae.getActionCommand();
      if (CMD_NEW_WINDOW.equals(cmd)) {
         createNewWindow();

      } else if (CMD_SAVE_AS.equals(cmd)) {
         saveAs();

      } else if (CMD_AUTO_SAVE.equals(cmd)) {
         autoSave();

      } else if (CMD_CLOSE.equals(cmd)) {
         int selectedIndex = tracePanel.getTabbedPane().getSelectedIndex();
         tracePanel.getTabbedPane().remove(selectedIndex);
         
      } else if (CMD_CLOSE_ALL.equals(cmd)) {
         tracePanel.getTabbedPane().removeAll();
      
      } else if (CMD_MARK_TIME.equals(cmd)) {
         timeZero = clickedTime;

      } else if (CMD_RESET_TIME.equals(cmd)) {
         timeZero = 0;
      }
   }

   private void createNewWindow() {
      JFrame frame = new JFrame();
      frame.setLayout(new BorderLayout());
      frame.setMinimumSize(new Dimension(750, 600));
      frame.setPreferredSize(new Dimension(800, 600));
      frame.setMaximumSize(new Dimension(850, 600));
      frame.setSize(800, 600);

      //showln("createNewWindow: imageIcon="+tracePanel.getImageIcon());
      if( tracePanel.getImageIcon() != null)
         frame.setIconImage(tracePanel.getImageIcon().getImage());

      frame.setTitle(tracePanel.getTitle()+" <> "+tracePanel.getFileName());

      TracePanel newTracePanel = new TracePanel(tracePanel);      
      frame.add(newTracePanel, BorderLayout.CENTER);
      frame.setLocationRelativeTo(tracePanel.getFrame());
      frame.setVisible(true);
   }   

   //--------------------------------------------------------------
   public void mouseWheelMoved(MouseWheelEvent e) {
      if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
         // do a fast vertical scroll 1/20 of total height
         int incy = e.getWheelRotation();
         Container c = getParent();
         if (c instanceof JViewport)
         {
            JViewport jv = (JViewport) c;
            Point p = jv.getViewPosition();
            int maxX = getWidth() - jv.getWidth();
            int maxY = getHeight() - jv.getHeight();
            int newX = p.x;
            int newY = p.y + maxY/20 * incy;

            if (newX < 0)
               newX = 0;
            if (newX > maxX)
               newX = maxX;
            if (newY < 0)
               newY = 0;
            if (newY > maxY)
               newY = maxY;
            jv.setViewPosition(new Point(newX, newY));
         }
      }
   }
}
