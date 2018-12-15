//
package gov.nist.p25.common.swing.transform;
 
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;

import javax.swing.JPanel;

//
// http://www.javalobby.org/java/forums/t19387.html
//
public class TransformingCanvas extends JPanel {

   private static final long serialVersionUID = -1L;
   private double translateX;
   private double translateY;
   private double scale;
   private GraphicsInterface graphicsIF;
 
   public TransformingCanvas( GraphicsInterface graphicsIF) {
      this.graphicsIF = graphicsIF;
      translateX = 0;
      translateY = 0;
      scale = 1.0d;
      setOpaque(true);
      setDoubleBuffered(true);

      TranslateHandler translater = new TranslateHandler(this);
      addMouseListener(translater);
      addMouseMotionListener(translater);
      addMouseWheelListener(new ScaleHandler(this));
   }
 
   @Override
   public void paint(Graphics g) {
 
      AffineTransform tx = new AffineTransform();
      tx.translate(translateX, translateY);
      tx.scale(scale, scale);
      Graphics2D g2d = (Graphics2D) g;
      g2d.setColor(Color.WHITE);
      g2d.fillRect(0, 0, getWidth(), getHeight());
      g2d.setTransform(tx);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);        

      // draw graphics
      graphicsIF.doGraphics( g);

      //g2d.setColor(Color.BLACK);
      //g2d.drawRect(50, 50, 50, 50);
      //g2d.fillOval(100, 100, 100, 100);
      //g2d.drawString("Test Affine Transform", 50, 30);
      //=== super.paint(g);
   }
 
   //-------------------------------------------------------------
   class TranslateHandler implements MouseListener, 
         MouseMotionListener {
      private int lastOffsetX;
      private int lastOffsetY;
      private TransformingCanvas canvas;

      TranslateHandler(TransformingCanvas canvas) {
         this.canvas = canvas;
      }
 
      public void mousePressed(MouseEvent e) {
         // capture starting point
         lastOffsetX = e.getX();
         lastOffsetY = e.getY();
      }
 
      public void mouseDragged(MouseEvent e) {
         
         // new x and y are defined by current mouse location 
         // subtracted by previously processed mouse location
         int newX = e.getX() - lastOffsetX;
         int newY = e.getY() - lastOffsetY;
 
         // increment last offset to last processed by drag event.
         lastOffsetX += newX;
         lastOffsetY += newY;
 
         // update the canvas locations
         canvas.translateX += newX;
         canvas.translateY += newY;
         
         // schedule a repaint.
         canvas.repaint();
      }
 
      public void mouseClicked(MouseEvent e) {}
      public void mouseEntered(MouseEvent e) {}
      public void mouseExited(MouseEvent e) {}
      public void mouseMoved(MouseEvent e) {}
      public void mouseReleased(MouseEvent e) {}
   }
 
   //-------------------------------------------------------------
   class ScaleHandler implements MouseWheelListener {
      private TransformingCanvas canvas;

      ScaleHandler(TransformingCanvas canvas) {
         this.canvas = canvas;
      }

      public void mouseWheelMoved(MouseWheelEvent e) {
         if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            
            // make it a reasonable amount of zoom
            // .1 gives a nice slow transition
            //canvas.scale += (0.1 * e.getWheelRotation());
            canvas.scale += (0.05 * e.getWheelRotation());

            // don't cross negative threshold.
            // also, setting scale to 0 has bad effects
            canvas.scale = Math.max(0.00001, canvas.scale); 
            canvas.repaint();
         }
      }
   }
}

