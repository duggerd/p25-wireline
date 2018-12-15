//
package gov.nist.p25.issi.packetmonitor.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

/**
 * Panel where the control buttons go. This class also talks to the "back end".
 * 
 */
public class ControlPanel extends JPanel implements ActionListener {
   
   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(ControlPanel.class);
   public static void showln(String s) { System.out.println(s); }

   private JFrame packetMonitor;
   private MessageInformationPanel msgInfoPanel;
   private PacketMonitorController packetMonitorController;
   //private MeasurementTable measurementTable = null;

   private JButton startCaptureButton;
   private JButton fetchTraceButton;
   //private JButton evaluateButton;   
   
   // constructor
   public ControlPanel(JFrame packetMonitor, MessageInformationPanel msgInfoPanel,
         PacketMonitorController packetMonitorController, boolean drawTrace)
         throws Exception {

      this.packetMonitor = packetMonitor;
      this.msgInfoPanel = msgInfoPanel;
      this.packetMonitorController = packetMonitorController;
      
      if (packetMonitorController instanceof RemotePacketMonitorController){
         startCaptureButton = new JButton("Start Packet Capture");
         startCaptureButton.addActionListener(this);
      }
      
      fetchTraceButton = new JButton("Fetch Trace");
      //evaluateButton = new JButton("Evaluate Trace");

      fetchTraceButton.addActionListener(this);
      //evaluateButton.addActionListener(this);
      
      if (packetMonitorController instanceof RemotePacketMonitorController) {
         super.add(startCaptureButton);
         super.add(fetchTraceButton);
         //super.add(evaluateButton);
      } else {      
         //super.add(evaluateButton);
         fetchTraceButton.doClick();
      }      
   }   
   
   private void getSipPttTraces() throws Exception {
      
      boolean errorFlag = packetMonitorController.fetchErrorFlag();
      //showln("getSipPttTraces(): errorFlag="+errorFlag);
      logger.debug("getSipPttTraces(): errorFlag="+errorFlag);
      
      if (errorFlag) {
         String errorCause = this.packetMonitorController.fetchErrorString();
         JOptionPane.showConfirmDialog(null, errorCause,
            "Capture Error",
            JOptionPane.ERROR_MESSAGE);
      }
      else {
         String sipTraces = packetMonitorController.fetchSipTraces();
         String pttTraces = packetMonitorController.fetchPttTraces();        
         String result = packetMonitorController.fetchResult();
         //showln("getSipPttTraces: fetchResult="+result);
         logger.debug("getSipPttTraces(): fetchResult="+result);
         
         msgInfoPanel.renderSipPttTraces( sipTraces, pttTraces, null);

         /*** DISABLE_MEASUREMENT_TABLE
         if (measurementTable == null) {
            logger.debug("getSipPttTraces(): new MeasurementTable...");
            measurementTable = new MeasurementTable(packetMonitor);
	 }
         logger.debug("getSipPttTraces(): setData()...");
         measurementTable.setData(result);
	  ***/
      }
   }

   // implementation of ActionListener
   //--------------------------------------------------------------------
   public void actionPerformed(ActionEvent ae) {
      try {
         if (ae.getSource() == startCaptureButton) {
            packetMonitorController.startCapture();
         }
         /*** DISABLE_MEASUREMENT_TABLE
         else if (ae.getSource() == evaluateButton ) {
            // check the traces and evaluate if they are available
            if (measurementTable == null) {
               measurementTable = new MeasurementTable(packetMonitor);
            }
            measurementTable.showTable();
         }
	  ***/
         else if (ae.getSource() == fetchTraceButton) {
            getSipPttTraces();
         }
      }
      catch (Exception ex) {
         logger.error("Error communicating with the monitor", ex);
         JOptionPane.showMessageDialog(null,
            "Error communicating with the monitor: "+ex.getMessage(),
            "Unexpected Error",
            JOptionPane.ERROR_MESSAGE);
      }
   }
}
