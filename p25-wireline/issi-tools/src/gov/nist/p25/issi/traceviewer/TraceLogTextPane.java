//
package gov.nist.p25.issi.traceviewer;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import org.apache.log4j.Logger;
import gov.nist.p25.issi.traceviewer.TraceFileFilter;

/**
 * Trace Log Text Pane 
 */
public class TraceLogTextPane extends JTextPane
   implements MouseListener
{
   private static final long serialVersionUID = 1L;
   private static Logger logger = Logger.getLogger(TraceLogTextPane.class);
   //public static void showln(String s) { System.out.println(s); }

   public static SimpleAttributeSet ATTRIBUTE_SIMPLE = new SimpleAttributeSet();
   public static String RTF_HEADER = "doc/rtf-header.txt";

   private String suggestedFile;
   private String rtfTrace;

   // accessor
   public String getSuggestedFile( ) {
      return suggestedFile;
   }
   public void setSuggestedFile( String suggestedFile) {
      this.suggestedFile = suggestedFile;
   }
   public String getRtfTrace( ) {
      return rtfTrace;
   }
   public void setRtfTrace( String rtfTrace) {
      this.rtfTrace = rtfTrace;
   }

   // constructor
   public TraceLogTextPane( ) {
      this( null);
   }
   public TraceLogTextPane( String suggestedFile) {

      super( );
      setSuggestedFile(suggestedFile);
      setEditable( false);
      setAutoscrolls( true);
      setToolTipText( "Right Click to Save ");
      addMouseListener( this);
   }

   public void logMessage(boolean isClear, String message) {
      logMessage(isClear, message, ATTRIBUTE_SIMPLE);
   }
   public void logMessage(boolean isClear, String message, AttributeSet attribute) {
      try
      {
         Document doc = getDocument();
         if( isClear)
            doc.remove(0, doc.getLength());
         doc.insertString(doc.getLength(), message, attribute);

         //setCaretPosition(doc.getLength());
         setCaretPosition( 0);
      }
      catch (Exception ex)
      {
         logger.error("Unexpected exception during logMessage: ", ex);
         JOptionPane.showMessageDialog(null,
            "Error in logging message: " + message,
            "Log Message Error",
            JOptionPane.INFORMATION_MESSAGE);
      }
   }

   //-----------------------------------------------------------------
   // implementation of MouseListener
   //-----------------------------------------------------------------
   public void mouseClicked(MouseEvent me) {

      if (me.getButton() == 3) {
         TraceFileFilter traceFilter = new TraceFileFilter();
         traceFilter.addExtension("txt");
         traceFilter.addExtension("rtf");
         traceFilter.setDescription("Text or RTF Files");
         traceFilter.setDescription("Text Files");

         JFileChooser fc = new JFileChooser();
         logger.debug("TraceLogTextPane: suggestedFile="+suggestedFile);
         if( getSuggestedFile() != null) {
            File file = new File( getSuggestedFile());
            fc.setSelectedFile( file);
         }
         fc.setDialogTitle("Save As");
         fc.setFileFilter(traceFilter);

         int returnVal = fc.showSaveDialog( null);
         if (returnVal != JFileChooser.APPROVE_OPTION) {
            logger.debug("Save command cancelled by user.");
            return;
         }

         File file = fc.getSelectedFile();
         String fullpath = file.getAbsolutePath();
         logger.debug("Saving to fullpath: " + fullpath);
         saveByFullpath(fullpath);
      }
   }
   public void mouseEntered(MouseEvent e) { }
   public void mouseExited(MouseEvent e) { }
   public void mousePressed(MouseEvent e) { }
   public void mouseReleased(MouseEvent e) { }

   //-----------------------------------------------------------------
   public static void saveAsFile( String fullpath, String message)
   {
      try {
         OutputStream out = new FileOutputStream(new File(fullpath));
         out.write(message.getBytes());
         out.close();
      }
      catch (Exception ex) {
         String msg = "An error occured while saving file: "+ fullpath;
         logger.error( msg, ex);
         JOptionPane.showMessageDialog(null,
            msg,
            "Save File Status", 
            JOptionPane.ERROR_MESSAGE);
      }
   }

   public static String buildRtfMessage(String rtf)
      throws FileNotFoundException, IOException {

      return buildRtfMessage(RTF_HEADER, rtf);
   }

   public static String buildRtfMessage(String headerFile, String rtf)
      throws FileNotFoundException, IOException {

      File rtfHeaderFile = new File( headerFile);
      int length = (int) rtfHeaderFile.length();
      char[] headerbytes = new char[length];
      FileReader fileReader = new FileReader(rtfHeaderFile);
      fileReader.read(headerbytes);
      fileReader.close();

      StringBuffer rtfBuffer = new StringBuffer();
      rtfBuffer.append(new String(headerbytes));

      rtfBuffer.append(rtf);
      rtfBuffer.append("}");
      return rtfBuffer.toString();
   }

   //-----------------------------------------------------------------
   public void autoSave() {
      String suggestFile = getSuggestedFile();
      //showln("autoSave: suggestFile="+suggestFile);
      saveByFullpath( suggestFile);
   }

   private void saveByFullpath(String fullpath) {
      String message = getText();
      // fullpath.endsWith("rtf") ? getRtfTrace() : getText();
            
      if (message == null || "".equals(message.trim())) {
         /***
         JOptionPane.showMessageDialog(null,
            "No data to save",
            "Save Trace Info",
            JOptionPane.INFORMATION_MESSAGE);
         return;
        ***/
         message = "*** no data available ***";
      }

      if(fullpath.endsWith("rtf"))
      {
         if(rtfTrace==null) {
            try {
               logger.debug("buildRtfMessage: ... ");
               message = buildRtfMessage(RTF_HEADER, message);
            }
            catch(Exception ex) { }
         }
         else {
            message = getRtfTrace();
            if( message==null)
               message = getText();
         }
      }
      saveAsFile( fullpath, message);
   }
}
