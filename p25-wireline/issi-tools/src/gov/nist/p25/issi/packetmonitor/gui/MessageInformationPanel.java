//
package gov.nist.p25.issi.packetmonitor.gui;

import gov.nist.p25.common.util.HexDump;
import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.traceviewer.MessageData;
import gov.nist.p25.issi.traceviewer.PttSessionInfo;
import gov.nist.p25.issi.traceviewer.PttTraceLoader;
import gov.nist.p25.issi.traceviewer.RfssData;
import gov.nist.p25.issi.traceviewer.SipTraceLoader;
import gov.nist.p25.issi.traceviewer.TraceLogTextPane;
import gov.nist.p25.issi.traceviewer.TracePanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Logger;

public class MessageInformationPanel extends JPanel {
   
   private static final long serialVersionUID = -1L;   
   private static Logger logger = Logger.getLogger( MessageInformationPanel.class);
   
   public static SimpleAttributeSet ERROR_MESSAGE_TEXT = new SimpleAttributeSet();
   public static SimpleAttributeSet STACK_TRACE_TEXT = new SimpleAttributeSet();
   public static SimpleAttributeSet INFO_TEXT = new SimpleAttributeSet();
   public static SimpleAttributeSet FATAL_MESSAGE_TEXT = new SimpleAttributeSet();

   static {
      StyleConstants.setForeground(ERROR_MESSAGE_TEXT, Color.red);
      StyleConstants.setForeground(STACK_TRACE_TEXT, Color.blue);
      StyleConstants.setForeground(INFO_TEXT, Color.black);
      StyleConstants.setForeground(FATAL_MESSAGE_TEXT, Color.red);
      StyleConstants.setUnderline(FATAL_MESSAGE_TEXT, true);
      StyleConstants.setFontSize(FATAL_MESSAGE_TEXT, 14);
   }

   private PacketMonitorGUI gui; 
   private TraceLogTextPane pttTraceLogPane;
   private TraceLogTextPane sipTraceLogPane;  
   private TracePanel sipTracePanel;
   private TracePanel pttTracePanel;
   private String systemTopologyFileName;
   private int runTestNumber = 0;
   
   // accessor
   public TracePanel getSipTracePanel() {
      return sipTracePanel;
   }
   public TracePanel getPttTracePanel() {
      return pttTracePanel;
   }
   
   // constructor   
   public MessageInformationPanel( PacketMonitorGUI gui, String systemTopology) {
      this.gui = gui;
      this.systemTopologyFileName = systemTopology;
      
      setLayout(new BorderLayout());      
      setBorder(new TitledBorder("Message Data"));
      
      sipTraceLogPane = new TraceLogTextPane();
      JScrollPane sipLogScrollPane = new JScrollPane(sipTraceLogPane);

      pttTraceLogPane = new TraceLogTextPane();
      JScrollPane pttLogScrollPane = new JScrollPane(pttTraceLogPane);
      
      JTabbedPane jtabbedPane = new JTabbedPane();      
      jtabbedPane.add("SIP Traces",sipLogScrollPane);
      jtabbedPane.add("PTT Traces",pttLogScrollPane);
      add(jtabbedPane);      
   }
   
   public void renderSipPttTraces(String sipTraces, String pttTraces,
         Hashtable<String, HashSet<PttSessionInfo>> rfssSessionData) {

      runTestNumber++;
      boolean remoteRetrievalSucceeded = true;   
      String sipTab = "SIP-" + runTestNumber;
      String pttTab = "PTT-" + runTestNumber;
      String pcapFile = gui.getFileName();
      String pcapFileName = pcapFile;
      
      Collection<RfssData> rfssList = null;
      try {         
         pcapFileName = new File(pcapFile).getName();
         byte[] stringAsBytes = sipTraces.getBytes();
         ByteArrayInputStream bais = new ByteArrayInputStream(stringAsBytes);
         
         TopologyConfig systemTopology = new SystemTopologyParser(PacketMonitorGUI.getIssiTesterConfiguration())
               .parse(systemTopologyFileName);   
         SipTraceLoader traceLoader = new SipTraceLoader(bais,
               rfssSessionData, systemTopology);
         
         rfssList = traceLoader.getSortedRfssList();
         List<MessageData> records = traceLoader.getRecords();
         String msgData = traceLoader.getMessageData();
         sipTraceLogPane.logMessage( true, msgData);
 
         // Create a new tabbed panel
         sipTracePanel = new TracePanel(gui,gui.getJTabbedPane(), 
                  "", rfssList, records);

         sipTracePanel.setTitle( pcapFileName);
         sipTracePanel.setTestNumber( sipTab);
         sipTracePanel.setDataTextArea( TracePanel.DATA_ALL_MSG, msgData);
               
         if( pcapFile != null && pcapFile.length() > 0) {
            String rawData = FileUtility.loadFromFileAsString( pcapFile);
            String sbufHex = HexDump.dump( rawData.getBytes(), 0, 0);
            sipTracePanel.setDataTextArea( TracePanel.DATA_RAW_MSG, sbufHex);
         } else {           
            sipTracePanel.setDataTextArea( TracePanel.DATA_RAW_MSG, sipTraces);
         }

         gui.getJTabbedPane().removeAll();
         gui.getJTabbedPane().add( sipTab, sipTracePanel);
         gui.getJTabbedPane().setSelectedComponent(sipTracePanel);
         
      } catch (Exception ex) {

         ex.printStackTrace();
         remoteRetrievalSucceeded = false;            
         try {
            String msg = "Error Fetching SIP Trace From Packet Monitor \n"
               + ex.getMessage() + "\n";;
            sipTracePanel.setDataTextArea( TracePanel.DATA_ERROR_MSG, msg);

         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      // If remote retrieval of SIP traces failed, return here
      if (!remoteRetrievalSucceeded)
         return;

      try {
         if (pttTraces == null) {
            return;
         }

         if (logger.isDebugEnabled()) {
            logger.debug("pttTraces=\n" + pttTraces);
         }

         byte[] stringAsBytes = pttTraces.getBytes();
         ByteArrayInputStream bais = new ByteArrayInputStream(stringAsBytes);
         PttTraceLoader traceLoader = new PttTraceLoader(bais);
         String msgData = traceLoader.getMessageData();

         // Create a new tabbed panel
         pttTracePanel = new TracePanel(gui,gui.getJTabbedPane(),
               "", rfssList, traceLoader.getRecords());

         pttTracePanel.setTitle( pcapFileName);
         pttTracePanel.setTestNumber( pttTab);
         pttTracePanel.setDataTextArea( TracePanel.DATA_ALL_MSG, msgData);
         
         //Where to get PTT raw data ?
         //String sbufHex = HexDump.toHex( stringAsBytes, 16);
         //pttTracePanel.setDataTextArea( TracePanel.DATA_RAW_MSG, sbufHex);
         pttTracePanel.setDataTextArea( TracePanel.DATA_RAW_MSG, pttTraces);

         gui.getJTabbedPane().add( pttTab, pttTracePanel);
         gui.getJTabbedPane().setSelectedComponent(pttTracePanel);
         
         pttTraceLogPane.logMessage( true, msgData);

      } 
      catch (Exception ex) {
         try {
            String msg = "Error Fetching PTT Trace From Packet Monitor \n"
               + ex.getMessage() + "\n";
            pttTraceLogPane.logMessage( true, msg);

         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
}
