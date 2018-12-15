//
package gov.nist.p25.issi.analyzer.gui;

import gov.nist.p25.common.swing.dialog.HTMLPaneDialog;
import gov.nist.p25.common.swing.util.ComponentUtils;
import gov.nist.p25.common.swing.util.PrintUtilities;
import gov.nist.p25.common.swing.util.LAFUtility;
//import gov.nist.p25.common.swing.fileexplorer.SortedFileListDialog;
import gov.nist.p25.common.swing.status.InfiniteProgressPanel;

import gov.nist.p25.common.util.Exec;
import gov.nist.p25.common.util.HexDump;
import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.common.util.IpAddressUtility;

import gov.nist.p25.issi.analyzer.bo.MessageCapturer;
import gov.nist.p25.issi.constants.ISSILogoConstants;
import gov.nist.p25.issi.constants.IvsDelaysConstants;
import gov.nist.p25.issi.fsm.IssiMessageAnalyzer;
import gov.nist.p25.issi.fsm.IssiMessageFsm;

import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.message.XmlIvsDelays;
import gov.nist.p25.issi.message.XmlIvsDelaysSpec;
import gov.nist.p25.issi.message.XmlPttmessages;
import gov.nist.p25.issi.message.XmlSystemTopology;
import gov.nist.p25.issi.packetmonitor.EndPoint;
import gov.nist.p25.issi.packetmonitor.PacketMonitor;
import gov.nist.p25.issi.packetmonitor.gui.LocalPacketMonitorController;
import gov.nist.p25.issi.packetmonitor.gui.MeasurementTable;
import gov.nist.p25.issi.packetmonitor.gui.PacketMonitorController;
import gov.nist.p25.issi.setup.ISSIConfigManager;
import gov.nist.p25.issi.setup.IvsDelaysMeasurementPanel;
import gov.nist.p25.issi.setup.IvsDelaysSetupDialog;

import gov.nist.p25.issi.traceviewer.MessageData;
import gov.nist.p25.issi.traceviewer.PttSessionInfo;
import gov.nist.p25.issi.traceviewer.RfssData;
import gov.nist.p25.issi.traceviewer.SipTraceLoader;
import gov.nist.p25.issi.traceviewer.TracePanel;
import gov.nist.p25.issi.utils.EndPointHelper;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.sip.PeerUnavailableException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

/**
 * Trace Analyzer GUI class.
 */
@SuppressWarnings("unchecked")
public class TraceAnalyzerGUI extends JPanel
   implements ActionListener
{
   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private static Logger logger = Logger.getLogger(TraceAnalyzerGUI.class);
   static {
      try {
         PropertyConfigurator.configure("log4j.properties");
         logger.addAppender(new FileAppender(new SimpleLayout(),
               "logs/traceanalyzer.txt"));
      } catch (Exception ex) {
         // ignore... :-)
      }
   }

   private static final String WIRESHARK_EXE = "/ext/WiresharkPortable/WiresharkPortable.exe";
   private static final String JPCAPDUMPER_JAR = "./lib/JpcapDumper-0.3.jar";

   private static final String TAG_PCAP_TEST = "pcap-test";
   private static final String TAG_FILE_OPEN = "Open";
   private static final String TAG_FILE_EXIT = "Exit";

   private static final String TAG_ACTION_ANALYZE = "Analyze";
   private static final String TAG_ACTION_AUTO_SAVE = "Auto Save All";
   private static final String TAG_ACTION_EVALUATE = "Evaluate";
   private static final String TAG_ACTION_CLEAR = "Clear";
   private static final String TAG_SPEC_CLEAR = "Clear";

   private static final String TAG_CAL_GCSD = "GCSD Confirmed";
   private static final String TAG_CAL_GCSD_UNCONFIRMED = "GCSD Unconfirmed";
   private static final String TAG_CAL_GMTD = "GMTD";
   private static final String TAG_CAL_UCSD = "UCSD Direct_Call";
   private static final String TAG_CAL_UCSD_CHECK = "UCSD Avail_Check";
   private static final String TAG_CAL_UMTD = "UMTD";

   // also in IvsDelaysConstants
   private static final boolean topIpSetup =  true;
   private static final String TAG_SETUP_IPDELAY = "Setup IP-Delays";
   private static final String TAG_SETUP_GCSD = "Setup GCSD";
   private static final String TAG_SETUP_GMTD = "Setup GMTD";
   private static final String TAG_SETUP_UCSD = "Setup UCSD";
   private static final String TAG_SETUP_UMTD = "Setup UMTD";

   private static final String TAG_SPEC_GCSD = "Spec GCSD Confirmed";
   private static final String TAG_SPEC_GCSD_UNCONFIRMED = "Spec GCSD Unconfirmed";
   private static final String TAG_SPEC_GMTD = "Spec GMTD";
   private static final String TAG_SPEC_UCSD = "Spec UCSD Direct_Call";
   private static final String TAG_SPEC_UCSD_CHECK = "Spec UCSD Avail_Check";
   private static final String TAG_SPEC_UMTD = "Spec UMTD";

   private static final String TAG_TOOL_DOCUMENTATION = "Documentation";
   private static final String TAG_TOOL_WIRESHARK = "WireShark";
   private static final String TAG_TOOL_JPCAPDUMPER = "JpcapDumper";

   private String[ ] fileItems = new String[] { 
      TAG_FILE_OPEN, 
      TAG_FILE_EXIT,
   };
   private String[ ] editItems = new String[] {
      TAG_ACTION_ANALYZE,
      TAG_ACTION_AUTO_SAVE,
      //TAG_ACTION_EVALUATE,
      TAG_ACTION_CLEAR,
   };
   private String[ ] calItems = new String[] {
      TAG_CAL_GCSD,
      TAG_CAL_GCSD_UNCONFIRMED,
      TAG_CAL_GMTD,
      TAG_CAL_UCSD,
      TAG_CAL_UCSD_CHECK,
      TAG_CAL_UMTD,
   };

   private String[ ] setupItems = new String[] {
      //TAG_SETUP_IPDELAY,
      TAG_SETUP_GCSD,
      TAG_SETUP_GMTD,
      TAG_SETUP_UCSD,
      TAG_SETUP_UMTD,
   };
   private String[ ] specItems = new String[] {
      TAG_SPEC_GCSD,
      TAG_SPEC_GCSD_UNCONFIRMED,
      TAG_SPEC_GMTD,
      TAG_SPEC_UCSD,
      TAG_SPEC_UCSD_CHECK,
      TAG_SPEC_UMTD,
   };
   private String[ ] toolItems = new String[] {
      TAG_TOOL_DOCUMENTATION,
      TAG_TOOL_WIRESHARK,
      TAG_TOOL_JPCAPDUMPER,
   };
   private char[] fileShortcuts = { 'O', 'X' };
   private char[] editShortcuts = { 'A','S','R' };
   //private char[] editShortcuts = { 'A','E','R' };
   //private char[] specShortcuts = { 'C', 'I' };
   //private char[] perfShortcuts = { 'G','S', 'M','U', 'K', 'T' };
   private char[] toolShortcuts = { 'D', 'W','J' };

   private boolean isPrtMenu = true;

   // CSSI - source and destination console
   private boolean isSrcConsole = true;
   private boolean isDstConsole = false;
   private String perfType = IvsDelaysConstants.TAG_TYPE_ISSI;
   
   private InfiniteProgressPanel progressPane;
   private JMenuBar menuBar;
   private JTextField pcapTextField;
   private JTextField wireSharkTextField;

   private JSplitPane splitPane;
   private JTabbedPane tabbedPane;
   private TracePanel sipTracePanel;
   private TracePanel pttTracePanel;
   private PacketMonitorController pmController;
   //private MeasurementTable measurementTable;

   private int runTestNumber = 0;
   private int numOfSipTraces = 0;
   private int numOfPttTraces = 0;

   private MessageCapturer capturer;
   private List<EndPoint> endPointList;
   private JCheckBoxMenuItem showAllPttCheckBox;  
   private JCheckBoxMenuItem showPttHeaderCheckBox;  
   private JCheckBoxMenuItem showRawMsgCheckBox;  
   private JCheckBoxMenuItem udpPortCheckBox;
   private JCheckBoxMenuItem validateInputCheckBox; 

   // accessor
   public JMenuBar getJMenuBar() {
      return menuBar;
   }
   public JTabbedPane getJTabbedPane() {
      return tabbedPane;
   }

   // constructor
   public TraceAnalyzerGUI(InfiniteProgressPanel progressPane)
      throws PeerUnavailableException, IOException
   {
      this.progressPane = progressPane;
      capturer = new MessageCapturer();
      menuBar = createJMenuBar();
      pcapTextField = new JTextField( );
      wireSharkTextField = new JTextField( WIRESHARK_EXE);

      setLayout( new BorderLayout());
      tabbedPane = new JTabbedPane();
      tabbedPane.setBorder(BorderFactory.createTitledBorder("Test Traces"));
      tabbedPane.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            tabbedPaneClicked(e);
         }
      });

      String pcapFile = pcapTextField.getText();
      splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      splitPane.setTopComponent(createEndPointPanel(pcapFile));
      splitPane.setBottomComponent( tabbedPane);
      splitPane.setOneTouchExpandable(true);
      splitPane.setDividerLocation(0.25);
      splitPane.setDividerSize(7);
      add(splitPane, BorderLayout.CENTER);
   }

   public EndPointPanel createEndPointPanel(String pcapFile) 
      throws IOException
   {
      if( pcapFile==null || pcapFile.length()==0) {
         return new EndPointPanel();
      }
      capturer.processPcapFile( pcapFile);

      checkUDPPortRange();
      endPointList = capturer.getTargetList();
      return new EndPointPanel( pcapFile, endPointList);
   }

   private JMenuBar createJMenuBar()
   {
      JMenuItem item;
      JMenu fileMenu = new JMenu("File");
      JMenu editMenu = new JMenu("Action");
      JMenu configMenu = new JMenu("Config");
      JMenu perfMenu = new JMenu("Performance");
      JMenu lafMenu = new JMenu("LAF");
      JMenu helpMenu = new JMenu("Help");

      // performance ->
      JMenu issiMenu = new JMenu(IvsDelaysConstants.TAG_TYPE_ISSI);
      JMenu cssiMenu = new JMenu(IvsDelaysConstants.TAG_TYPE_CSSI);
      perfMenu.add(issiMenu);
      perfMenu.add(cssiMenu);

      // ISSI Performance ->
      JMenu issiCalMenu = new JMenu("Calculate Delays");
      JMenu issiSetupMenu = new JMenu("Setup Delays");
      JMenu issiSpecMenu = new JMenu("Specifications");

      JMenu cssiCalMenu = new JMenu("Calculate Delays");
      JMenu cssiSetupMenu = new JMenu("Setup Delays");
      JMenu cssiSpecMenu = new JMenu("Specifications");

      ActionListener printListener = this;

      // setup File menus with mnemonics
      //---------------------------------------------------------
      for (int i=0; i < fileItems.length; i++) {
         item = new JMenuItem(fileItems[i]);
         item.setAccelerator(KeyStroke.getKeyStroke(fileShortcuts[i],
               Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false));
         item.addActionListener(printListener);
         fileMenu.add(item);
      }

      // setup Action menus with keyboard accelerators
      //---------------------------------------------------------
      for (int i=0; i < editItems.length; i++) {
         item = new JMenuItem(editItems[i]);
         item.setAccelerator(KeyStroke.getKeyStroke(editShortcuts[i],
              Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false));
         item.addActionListener(printListener);
         editMenu.add(item);
      }
      
      // setup Calulate Delays menus 
      //---------------------------------------------------------
      for (int i=0; i < calItems.length; i++) {
         final String wkey = calItems[i];
         item = new JMenuItem(calItems[i]);
         issiCalMenu.add(item);
         item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               perfType = IvsDelaysConstants.TAG_TYPE_ISSI;
               doCalculateDelays(perfType, wkey);
            }
         });
      }
      for (int i=0; i < calItems.length; i++) {
         final String xkey = calItems[i];
         item = new JMenuItem(calItems[i]);
         cssiCalMenu.add(item);
         item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               perfType = IvsDelaysConstants.TAG_TYPE_CSSI;
               doCalculateDelays(perfType, xkey);
            }
         });
      }
      
      // setup Setup Delays menus 
      //---------------------------------------------------------
      for (int i=0; i < setupItems.length; i++) {
         final String ikey = setupItems[i];
         item = new JMenuItem(setupItems[i]);
         issiSetupMenu.add(item);
         item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               perfType = IvsDelaysConstants.TAG_TYPE_ISSI;
               doSetupDelays(perfType, ikey);
            }
         });
      }
      for (int i=0; i < setupItems.length; i++) {
         final String ckey = setupItems[i];
         item = new JMenuItem(setupItems[i]);
         cssiSetupMenu.add(item);
         item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               perfType = IvsDelaysConstants.TAG_TYPE_CSSI;
               doSetupDelays(perfType, ckey);
            }
         });
      }

      // setup Specifications menus 
      //---------------------------------------------------------
      for (int i=0; i < specItems.length; i++) {
         final String skey = specItems[i];
         item = new JMenuItem(specItems[i]);
         issiSpecMenu.add(item);
         item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               perfType = IvsDelaysConstants.TAG_TYPE_ISSI;
               doSpecifications(perfType, skey);
            }
         });
      }
      for (int i=0; i < specItems.length; i++) {
         final String tkey = specItems[i];
         item = new JMenuItem(specItems[i]);
         cssiSpecMenu.add(item);
         item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               perfType = IvsDelaysConstants.TAG_TYPE_CSSI;
               doSpecifications(perfType, tkey);
            }
         });
      }

      //---------------------------------------------------------
      // append cascaded menu 
      issiMenu.add(issiCalMenu);
      issiMenu.add(issiSetupMenu);
      issiMenu.add(issiSpecMenu);

      cssiMenu.add(cssiCalMenu);
      cssiMenu.add(cssiSetupMenu);
      cssiMenu.add(cssiSpecMenu);

      // Setup IP-Delay at top level
      //---------------------------------------------------------
      if( topIpSetup) {
         item = new JMenuItem(TAG_SETUP_IPDELAY);
         item.setAccelerator(KeyStroke.getKeyStroke('S',
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false));
         item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               // IP-Delays is same for ISSI, CSSI
               perfType = IvsDelaysConstants.TAG_TYPE_ISSI;
               doSetupDelays(perfType, TAG_SETUP_IPDELAY);
            }
         });
         perfMenu.add(item);
      }

      // Clear
      item = new JMenuItem(TAG_SPEC_CLEAR);
      item.setAccelerator(KeyStroke.getKeyStroke('R',
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false));
      item.addActionListener(printListener);
      perfMenu.add(item);


      // setup Config menus
      //---------------------------------------------------------
      showAllPttCheckBox = new JCheckBoxMenuItem("Show all PTT");
      configMenu.add( showAllPttCheckBox);
      showAllPttCheckBox.addActionListener(printListener);

      showPttHeaderCheckBox = new JCheckBoxMenuItem("Show PTT Header");
      configMenu.add( showPttHeaderCheckBox);
      showPttHeaderCheckBox.addActionListener(printListener);

      showRawMsgCheckBox = new JCheckBoxMenuItem("Show Raw Message");
      configMenu.add( showRawMsgCheckBox);
      showRawMsgCheckBox.addActionListener(printListener);
      configMenu.addSeparator();

      udpPortCheckBox = new JCheckBoxMenuItem("Open UDP Port range");
      configMenu.add( udpPortCheckBox);
      udpPortCheckBox.addActionListener(printListener);
      configMenu.addSeparator();      
      validateInputCheckBox = new JCheckBoxMenuItem("Validate Input Data");
      configMenu.add( validateInputCheckBox);
      validateInputCheckBox.addActionListener(printListener);

     /*** save for future 
      configMenu.addSeparator();
      ButtonGroup buttonGroup = new ButtonGroup();
      configMenu.add(item = new JRadioButtonMenuItem("Radio 1"));
      item.addActionListener(printListener);
      buttonGroup.add(item);
      configMenu.add(item = new JRadioButtonMenuItem("Radio 2"));
      item.addActionListener(printListener);
      buttonGroup.add(item);
      item.addActionListener(printListener);
       ***/

      // Setup LAF
      List<JRadioButtonMenuItem> lafList = LAFUtility.getLAFRadioButtonMenuItem();
      ButtonGroup buttonGroup = new ButtonGroup();
      for(JRadioButtonMenuItem radioBtn: lafList) {
         buttonGroup.add(radioBtn);
         lafMenu.add(radioBtn);
         radioBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
               JRadioButtonMenuItem btn = (JRadioButtonMenuItem)evt.getSource();
	       String name = btn.getText();
               String className = LAFUtility.getLAFClassName(name);
	       //showln("LAF: name="+name+" class="+className);
               try {
                  UIManager.setLookAndFeel(className);
                  SwingUtilities.updateComponentTreeUI(TraceAnalyzerGUI.this);
               } catch (Exception ex) {
                  //showln("LAF: "+ex);
               }
            }
         });
      }

      // setup Tool menus with keyboard accelerators
      //---------------------------------------------------------
      for (int i=0; i < toolItems.length; i++) {
         item = new JMenuItem(toolItems[i]);
         item.setAccelerator(KeyStroke.getKeyStroke(toolShortcuts[i],
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false));
         item.addActionListener(printListener);
         helpMenu.add(item);
      }

      // setup menubar
      //---------------------------------------------------------
      JMenuBar menuBar = new JMenuBar();
      menuBar.add(fileMenu);
      menuBar.add(editMenu);
      menuBar.add(configMenu);
      menuBar.add(perfMenu);
      menuBar.add(lafMenu);
      menuBar.add(helpMenu);
      return menuBar;
   }

   public void getSipPttTrace(ISSIConfigManager configMgr, String pcapFile,
      boolean isRendered)
      throws Exception
   {
      TopologyConfig systemTopology = configMgr.getSystemTopologyConfig();
      PacketMonitor packetMonitor = new PacketMonitor( systemTopology);

      boolean keepDup = showAllPttCheckBox.getState();
      packetMonitor.setKeepDuplicateMessage( keepDup);
      packetMonitor.readTraceFromFile( pcapFile);
      pmController = new LocalPacketMonitorController( packetMonitor);
      
      boolean errorFlag = pmController.fetchErrorFlag();   
      if (errorFlag)
      {
         String errorCause = pmController.fetchErrorString();
         JOptionPane.showMessageDialog( null,
               errorCause,
               "Capture Error",
               JOptionPane.ERROR_MESSAGE);
      }
      else
      {
         String sipTraces = pmController.fetchSipTraces();
         String pttTraces = pmController.fetchPttTraces();

         String allTraces = pmController.fetchAllTraces();
         FileUtility.saveToFile("logs/allmessages.xml", allTraces);
         //showln("getSipPttTrace(): getAllTraces=\n"+allTraces);
        
         // save for xmlbeans
         //FileUtility.saveToFile("logs/sipmessages.xml", sipTraces);
         //FileUtility.saveToFile("logs/pttmessages.xml", pttTraces);
         
	 if( isRendered) {
            renderSipPttTrace( configMgr, pcapFile, sipTraces, pttTraces, null);
         
	    /*** DISABLE_MEASUREMENT_TABLE
            String result = pmController.fetchResult();
            //showln("getSipPttTrace: result=\n"+result);
            //FileUtility.saveToFile("logs/result.xml", result);

            if (measurementTable == null) {
               measurementTable = new MeasurementTable(new JFrame());
            }
            measurementTable.setData(result);
	     ***/
	 }
      }
   }

   public void renderSipPttTrace( ISSIConfigManager configMgr,
         String pcapFile, String sipTraces, String pttTraces,
         Hashtable<String, HashSet<PttSessionInfo>> rfssSessionData)
      throws Exception
   {
      String sipTab = "SIP-" + runTestNumber;
      String pttTab = "PTT-" + runTestNumber;
      String pcapFileName = pcapFile;
      String rawData = null;
      String sbufHex = null;
      
      boolean rawMsg = showRawMsgCheckBox.getState();
      JTabbedPane tabbedPane = getJTabbedPane();
      Collection<RfssData> rfssList = null;
      try
      {
         pcapFileName = new File(pcapFile).getName();
         byte[] stringAsBytes = sipTraces.getBytes();
         ByteArrayInputStream bais = new ByteArrayInputStream(stringAsBytes);

         TopologyConfig systemTopology = configMgr.getSystemTopologyConfig();   
         SipTraceLoader traceLoader = new SipTraceLoader( bais,
               rfssSessionData, systemTopology);
         
         rfssList = traceLoader.getSortedRfssList();
         List<MessageData> records = traceLoader.getRecords();
         String msgData = traceLoader.getMessageData();
         
         numOfSipTraces = records.size();
         logger.debug("renderSipPttTraces: SIP Traces-records.size="+numOfSipTraces);
         //showln("sysTopology=\n"+systemTopology.getDescription());
         //showln("rfssList=\n"+rfssList);

         // Create a new tabbed panel
         sipTracePanel = new TracePanel( null, tabbedPane,
               "", rfssList, records);

         sipTracePanel.setTitle( sipTab+"-"+pcapFileName);
         sipTracePanel.setTestNumber( TAG_PCAP_TEST);
         sipTracePanel.setFileName( pcapTextField.getText());
         sipTracePanel.setDataTextArea( TracePanel.DATA_ALL_MSG, msgData);
	 sipTracePanel.setTestResultLabel( "Total Messages: "+numOfSipTraces);

         // dump raw message in hex
         if( rawMsg && pcapFile != null && pcapFile.length() > 0)
         {
            rawData = FileUtility.loadFromFileAsString( pcapFile);
            sbufHex = HexDump.dump( rawData.getBytes(), 0, 0);
            sipTracePanel.setDataTextArea( TracePanel.DATA_RAW_MSG, sbufHex);
         }
         else
         {           
            sipTracePanel.setDataTextArea( TracePanel.DATA_RAW_MSG, sipTraces);
         }
         addTraceToTabbedPanel(tabbedPane, sipTab, sipTracePanel);
         
      } catch (Exception ex) {
         ex.printStackTrace();
         String msg = "Error Fetching SIP Trace From Packet Monitor \n"
                + ex.getMessage();
         if( sipTracePanel != null)
            sipTracePanel.setDataTextArea( TracePanel.DATA_ERROR_MSG, msg);
	 //NPE
	 throw new Exception(msg);
      }

      try
      {
         if (pttTraces == null || pttTraces.length()==0) {
            return;
         }

         boolean keepHeader = showPttHeaderCheckBox.getState();
         String title = "*** PTT Messages ***";
         XmlPttmessages xmlmsg = new XmlPttmessages();
         Pttmessages pttmsg = xmlmsg.loadPttmessages(pttTraces);
         xmlmsg.getPttMessageData(pttmsg, keepHeader);
         List<MessageData> records = xmlmsg.getRecords();
         String msgData = xmlmsg.toISSIString( title, pttmsg, keepHeader);

         numOfPttTraces = records.size();
         logger.debug("renderSipPttTraces: PTT Traces-records.size="+numOfPttTraces);

         // Create a new tabbed panel
         pttTracePanel = new TracePanel( null, tabbedPane,
               "", rfssList, records);

         pttTracePanel.setTitle( pttTab+"-"+pcapFileName);
         pttTracePanel.setTestNumber( TAG_PCAP_TEST);
         pttTracePanel.setFileName( pcapTextField.getText());
         pttTracePanel.setDataTextArea( TracePanel.DATA_ALL_MSG, msgData);
	 pttTracePanel.setTestResultLabel( "Total Messages: "+numOfPttTraces);
         
         //showln("sbufHex="+sbufHex);
	 if( rawMsg && sbufHex != null)  {
            pttTracePanel.setDataTextArea( TracePanel.DATA_RAW_MSG, sbufHex);
         } else {
            pttTracePanel.setDataTextArea( TracePanel.DATA_RAW_MSG, pttTraces);
	 }
         addTraceToTabbedPanel(tabbedPane, pttTab, pttTracePanel);
      } 
      catch (Exception ex)
      {
         ex.printStackTrace();
         String msg = "Error Fetching PTT Trace From Packet Monitor \n"
                      + ex.getMessage();
         if( pttTracePanel != null)
            pttTracePanel.setDataTextArea( TracePanel.DATA_ERROR_MSG, msg);
	 //NPE
	 throw new Exception(msg);
      }
   }

   private void addTraceToTabbedPanel(JTabbedPane tabbedPane, String tab,
         TracePanel tracePanel) {
   
      //logger.debug("addTraceToTabbedPanel: ...");
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
         private JTabbedPane tabbedPane;
         private String tab;
         private TracePanel tracePanel;

         public Runnable setParams(JTabbedPane tabbedPane, String tab,
               TracePanel tracePanel) {
            this.tabbedPane = tabbedPane;
            this.tab = tab;
            this.tracePanel = tracePanel;
            return this;
         }
         public void run() {
            tabbedPane.add(tab, tracePanel);
            tabbedPane.setSelectedComponent(tracePanel);
            tabbedPane.updateUI();
         }
      }.setParams(tabbedPane, tab, tracePanel));
   }   

   public List<EndPoint> filterEndPoint(List<EndPoint> epList)
   {
      List<EndPoint> list = new ArrayList<EndPoint>();
      if( epList != null)
      for( EndPoint ep: epList) {
         if( ep.getEnabled()) {
            list.add( ep);
         }
      }
      return list;
   }

   public String generateStartupProperties( String pcapFile)
      throws Exception
   {
      logger.debug("generateStartupProperties: pcapFile="+pcapFile);

      // Keep only the enabled RFSS
      List<EndPoint> epList = filterEndPoint(endPointList);
      logger.debug("filterEndPoint():\n"+ epList);
     
      // generate the config and properties files     
      XmlSystemTopology config = new XmlSystemTopology();
      String outProp = config.generateSystemTopology( pcapFile, epList);
      logger.debug("generateSystemTopology(): outProp="+ outProp);

      return outProp;
   }

   public void checkUDPPortRange()
   {
      boolean state = udpPortCheckBox.getState();
      if( state )
      {
         int[] ports = new int[] { 1, 32765 };
         capturer.setPortsRange( ports);
      } 
      else {
         capturer.setPortsRange( null);
      }
   }
   
   public void validateEndPoint()
   {
      boolean state = validateInputCheckBox.getState();
      if( !state) return;
      for( EndPoint ep: endPointList) {
         String domainName = ep.getDomainName();
         // validate the domain name
         EndPointHelper.decodeDomainName( domainName);
      }     
   }

   public void rangeCheckEndPoint()
   {
      StringBuffer sb = new StringBuffer();
      for( EndPoint ep: endPointList) {
         String domainName = ep.getDomainName();
         try {
            EndPointHelper.rangeCheckDomainName( domainName);
         } catch(Exception ex) {
            sb.append( ex.getMessage());
            sb.append( "\n");
         }
      }  // for-loop
      if( sb.toString().length() > 0) {
         throw new IllegalArgumentException( sb.toString());
      }   
   }

   public void displayHtmlFile(String htmlFile)
   {
         //String htmlFile = "doc/tadocumentation.html";
         try {
            URL url = new File(htmlFile).toURI().toURL();
            HTMLPaneDialog htmlDialog = new HTMLPaneDialog(new JFrame(), 
               "Trace Analyzer", url);
            htmlDialog.setSize(640, 540);
            htmlDialog.setLocationRelativeTo(this);
            htmlDialog.setVisible(true);
         } 
         catch(Throwable ex) 
         {
            JOptionPane.showMessageDialog( null,
               "Failed to load html due to:\n"+ ex.getMessage(),
               "Message", 
               JOptionPane.INFORMATION_MESSAGE);
         }
   }

   public void autoSave() 
   {
      ArrayList compList = new ArrayList();
      ComponentUtils.getAllComponents( tabbedPane, compList);
      for(int i=0; i < compList.size(); i++) {
         Object tpobj = compList.get(i);
         if(tpobj instanceof TracePanel) {
            TracePanel tp = (TracePanel)tpobj;
            tp.autoSave();
         }
	 else if(tpobj instanceof IvsDelaysMeasurementPanel) {
            IvsDelaysMeasurementPanel mp = (IvsDelaysMeasurementPanel)tpobj;
            mp.autoSave();
         }
      }
   }

   public void autoPrint() {
      ArrayList compList = new ArrayList();
      ComponentUtils.getAllComponents( tabbedPane, compList);
      for(int i=0; i < compList.size(); i++) {
         Object tpobj = compList.get(i);
showln("autoPrint: tpobj="+tpobj.getClass().getName());
	 if(tpobj instanceof TracePanel) {
            TracePanel tp = (TracePanel)tpobj;
	    // may need to compute number of pages
            PrintUtilities.printComponent(tp.getDiagramPanel());
         }
	 /***
	 else if(tpobj instanceof JTextArea) {
            JTextArea tp = (JTextArea)tpobj;
	    if( tp.getText().length() > 0)
               PrintUtilities.printComponent(tp);
         } 
	  **/
      }
   }

   private void tabbedPaneClicked(MouseEvent e)
   {
     if(e.getButton() != MouseEvent.BUTTON1 && e.getClickCount() == 1)
     {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Auto Save All");
        menuItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              autoSave();
           }
        });
        popupMenu.add(menuItem);

        // Auto Print
        JMenuItem prtmenuItem = new JMenuItem("Auto Print All");
        prtmenuItem.addActionListener( new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              autoPrint();
           }
        });
        if( isPrtMenu)
        popupMenu.add(prtmenuItem);
        popupMenu.show(e.getComponent(), e.getX(), e.getY() - 10);
     }
   }

   // implementation of ActionListener
   //------------------------------------------------------------------------
   public void actionPerformed(ActionEvent event)
   {
      String xcmd = event.getActionCommand();
      //showln("Menu item [" + xcmd + "] was pressed.");

      String userdir = System.getProperty( "user.dir");
      //-------------------------------------------------------------------
      if( TAG_FILE_EXIT.equals(xcmd)) {
         // all done
         System.exit(0);
      }
      else if( TAG_FILE_OPEN.equals(xcmd)) {
         // Open pcap file
         JFileChooser fc = new JFileChooser( userdir);
         FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "PCAP file", "pcap");
         fc.setFileFilter( filter);

         int ret = fc.showOpenDialog(null);
         if( ret == JFileChooser.APPROVE_OPTION) {
            logger.debug("Open: APPROVE_OPTION ...");
            File file = fc.getSelectedFile();
            if( file != null) {
               logger.debug("Open: file="+file.getName());
               String pcapFile = file.getAbsolutePath();
               pcapTextField.setText( pcapFile);
               showln( pcapFile);
               //if(sipTracePanel != null) sipTracePanel.setFileName(pcapFile);
               //if(pttTracePanel != null) pttTracePanel.setFileName(pcapFile);

               try { 
                  splitPane.setTopComponent(createEndPointPanel(pcapFile));
                  splitPane.setDividerLocation(0.25);
                  updateUI();

                  //check input from pcap file
		  rangeCheckEndPoint();

               } catch(Exception ex) { 
                  logger.debug("Error in open pcap file: "+ex);
                  JOptionPane.showMessageDialog( null,
                     "Error in opening pcap file:\n" + ex,
                     "File Open Error",
                     JOptionPane.ERROR_MESSAGE);
               } catch(Error err) { 
                  logger.debug("Unexpected Linkage Error: "+err);
                  JOptionPane.showMessageDialog( null,
                     "Unexpected Error in opening pcap file:\n" + err,
                     "Unexpected Error",
                     JOptionPane.ERROR_MESSAGE);
               }
            }
         }
         else {
            logger.debug("Open: not APPROVE_OPTION");
         }
      }
      //-------------------------------------------------------------------
      else if( TAG_ACTION_ANALYZE.equals(xcmd))
      {
//~~~~~~~~~~
     //logger.debug("ANALYZE: progressPane.start()...");
     progressPane.start();

     Thread worker = new Thread(new Runnable() {
       public void run() {
         runTestNumber++;
         String pcapFile = pcapTextField.getText();
         if(pcapFile==null || pcapFile.length()==0) {
            progressPane.interrupt();
            return;
         }
         try {
            validateEndPoint();

            String startupFile = generateStartupProperties( pcapFile);
	    logger.debug("ANALYZE: startupFile="+startupFile);
            ISSIConfigManager configMgr = new ISSIConfigManager(startupFile);
            //logger.debug("ANALYZE: getSipPttTrace()...");
            getSipPttTrace(configMgr, pcapFile, true);

            //logger.debug("ANALYZE: progressPane.stop()...");
            progressPane.stop();
         }
         catch(Exception ex) {
            //logger.debug("ANALYZE: exception...");
            ex.printStackTrace();
	    logger.debug( ex.toString());
            progressPane.stop();
            JOptionPane.showMessageDialog( null,
               "Error in analyzing pcap: "+pcapFile +"\n" + ex,
               "Trace Analysis Error",
               JOptionPane.ERROR_MESSAGE);
         }
       }
     });
     worker.start();
//~~~~~~~~~~
      }
      else if( TAG_ACTION_AUTO_SAVE.equals(xcmd))
      {
         logger.debug("ANALYZE: saving results to output/pcap-test...");
         autoSave();
      }
      //-------------------------------------------------------------------
      /*** DISABLE_MEASUREMENT_TABLE
      else if( TAG_ACTION_EVALUATE.equals(xcmd))
      {
         try {
            if (measurementTable == null)
               measurementTable = new MeasurementTable(new JFrame());
            measurementTable.showTable();
         }
         catch(Exception ex) {
            JOptionPane.showMessageDialog( null,
               "Error in fetching measurement results:\n" + ex,
               "Trace Measurements Error",
               JOptionPane.ERROR_MESSAGE);          
         }
      }
       ***/
      else if( TAG_ACTION_CLEAR.equals(xcmd) ||
               TAG_SPEC_CLEAR.equals(xcmd))
      {
         getJTabbedPane().removeAll();
         runTestNumber = 0;
      }
      //-------------------------------------------------------------------
      else if( TAG_TOOL_DOCUMENTATION.equals(xcmd))
      {
         String htmlFile = "doc/tadocumentation.html";
         try {
            URL url = new File(htmlFile).toURI().toURL();
            HTMLPaneDialog htmlDialog = new HTMLPaneDialog(new JFrame(), 
               "Trace Analyzer", url);
            htmlDialog.setSize(640, 540);
            htmlDialog.setLocationRelativeTo(this);
            htmlDialog.setVisible(true);
         } 
         catch(Throwable ex) 
         {
            JOptionPane.showMessageDialog( null,
               "Failed to load html due to:\n"+ ex.getMessage(),
               "Message", 
               JOptionPane.INFORMATION_MESSAGE);
         }
      }
      else if( TAG_TOOL_WIRESHARK.equals(xcmd))
      {
         // popup file chooser to define WireShark.exe
         String loc = wireSharkTextField.getText();
         JFileChooser fc = new JFileChooser( loc);
         FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "WireShark executable file", "exe");
         fc.setFileFilter( filter);
         try {
            fc.setSelectedFile( new File(loc));
         } catch(Exception ex) { }

         int ret = fc.showOpenDialog(null);
         if( ret == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if( file != null) {
               wireSharkTextField.setText( file.getAbsolutePath());
               showln( file.getAbsolutePath());

               String cmd= wireSharkTextField.getText() +" " +pcapTextField.getText();
               Exec.exec( cmd);
            }
         }
      }
      else if( TAG_TOOL_JPCAPDUMPER.equals(xcmd))
      {
         String cmd= "java -jar " + JPCAPDUMPER_JAR;
         Exec.exec( cmd);
      }
      //-------------------------------------------------------------------
   }

   //------------------------------------------------------------------------
   private void doCalculateDelays(String perfType, String xcmd)
   {
      logger.debug("doCalculateDelays(): perfType="+perfType);
      if( TAG_CAL_GCSD.equals(xcmd) ||
          TAG_CAL_GCSD_UNCONFIRMED.equals(xcmd) ||
          TAG_CAL_GMTD.equals(xcmd) ||
          TAG_CAL_UCSD.equals(xcmd) ||
          TAG_CAL_UCSD_CHECK.equals(xcmd) ||
          TAG_CAL_UMTD.equals(xcmd) )
      {
         runTestNumber++;
         String specTab = "PERF-"+runTestNumber;

         // only allow *.pcap files
         FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File file, String name) {
               String fpath = file.getAbsolutePath()+File.separator+name;
               File xfile = new File(fpath);
               boolean bflag = xfile.isDirectory() || name.endsWith("pcap") ||
                  name.endsWith("PCAP");
               return bflag;
            }
         };
	 boolean isConfirmed = true;
	 boolean isDirectCall = true;
	 int mask = IssiMessageFsm.MASK_GCSD;
         String type = IvsDelaysConstants.TAG_IVS_GCSD;
	 String subtitle = "";
         String keyMsg = "INVITE";
         if( TAG_CAL_GCSD.equals(xcmd)) {
	    isConfirmed = true;
	    mask = IssiMessageFsm.MASK_GCSD;
            type = IvsDelaysConstants.TAG_IVS_GCSD;
	    subtitle = "IVS Group Call Setup Delay - Confirmed Group Call";
            keyMsg = "INVITE";
         }
	 else if( TAG_CAL_GCSD_UNCONFIRMED.equals(xcmd)) {
	    isConfirmed = false;
	    mask = IssiMessageFsm.MASK_GCSD;
            type = IvsDelaysConstants.TAG_IVS_GCSD;
	    subtitle = "IVS Group Call Setup Delay - Unconfirmed Group Call";
            keyMsg = "INVITE";
         }
	 else if( TAG_CAL_GMTD.equals(xcmd)) {
	    mask = IssiMessageFsm.MASK_GMTD;
            type = IvsDelaysConstants.TAG_IVS_GMTD;
	    subtitle = "IVS Group Call Message Transfer Delay";
            keyMsg = "XYZ";   // PTT_TX_REQUEST
         }
	 else if( TAG_CAL_UCSD.equals(xcmd)) {
	    isDirectCall = true;
	    mask = IssiMessageFsm.MASK_UCSD;
            type = IvsDelaysConstants.TAG_IVS_UCSD;
	    subtitle = "IVS SU-to-SU Call Setup Delay - Direct Call";
            keyMsg = "INVITE";
         }
	 else if( TAG_CAL_UCSD_CHECK.equals(xcmd)) {
	    isDirectCall = false;
	    mask = IssiMessageFsm.MASK_UCSD;
            type = IvsDelaysConstants.TAG_IVS_UCSD;
	    subtitle = "IVS SU-to-SU Call Setup Delay - Availability Check";
            keyMsg = "INVITE";
         }
	 else if( TAG_CAL_UMTD.equals(xcmd)) {
	    mask = IssiMessageFsm.MASK_UMTD;
            type = IvsDelaysConstants.TAG_IVS_UMTD;
	    subtitle = "IVS SU-to-SU Call Message Transfer Delay";
            keyMsg = "XYZ";   // PTT_TX_REQUEST
         }
         String title = "Select PCAP files:  " +perfType +"- " +subtitle;
         try {
            //SortedFileListDialog dialog = new SortedFileListDialog(null,true,
            //    filter,"/",title);
            PerformanceFileListDialog dialog = new PerformanceFileListDialog(null,true,
                filter,"/",title,perfType);
            if( dialog.getAnswer()) {
               Iterator iter = dialog.getSortedListModel().iterator();
               boolean errFile = false;
               XmlIvsDelays xmldoc = null;

               // NOTE; we need to order FSM by the SIP message type:
	       // INVITE, PTT_TX__REQUEST or PTT_TX_PROGRESS based on
	       // Performance Calculations type
               IssiMessageAnalyzer analyzer = new IssiMessageAnalyzer(perfType,keyMsg);
	       //TODO: for now get srcConsole and dstConsole from dialog
               analyzer.setSrcConsole( dialog.getSrcConsole());
               analyzer.setDstConsole( dialog.getDstConsole());

               StringBuffer sbuf = new StringBuffer();
               while( iter.hasNext()) {
                  String pcapFile = (String) iter.next();
                  logger.debug("  *** PROCESS -->"+ pcapFile);
                  try {
                     createEndPointPanel( pcapFile);
                     String startupFile = generateStartupProperties(pcapFile);
                     ISSIConfigManager configMgr = new ISSIConfigManager(startupFile);
                     getSipPttTrace(configMgr, pcapFile, false);

                     String xmlMsg = pmController.fetchAllTraces();
                     analyzer.analyze(mask, xmlMsg);
                     xmldoc = analyzer.calculate(mask, isConfirmed, isDirectCall);
                  }
		  catch(Exception ex) {
                     logger.debug("Error in processing: "+pcapFile +" due to\n"+ex);
		     sbuf.append( "Error in processing: "+pcapFile +" thus skipped.\n");
                     errFile = true;
                  }
               }  // while-loop

               try {
                  if( xmldoc==null)
                     throw new Exception("Performance Analysis Error: null XmlIvsDelays.");

                  logger.debug("summary=\n"+analyzer.getSummaryMap());
                  if( errFile) {
                     String comment = xmldoc.getIvsStatusComment();
                     comment += sbuf.toString();
                     xmldoc.setIvsStatusComment( comment);
                  }

                  String xtitle = perfType+" Performance Analysis";
                  IvsDelaysMeasurementPanel gcsdPanel = 
                     new IvsDelaysMeasurementPanel(xmldoc,xtitle,type,true,true);
                  JScrollPane gcsdScroll = new JScrollPane(gcsdPanel);
                  getJTabbedPane().add( specTab, gcsdScroll);
                  getJTabbedPane().setSelectedComponent(gcsdScroll);
               } 
	       catch(Exception ex) { 
                  ex.printStackTrace();
                  JOptionPane.showMessageDialog( null,
                     "Error in calculating performance parameters:\n" + ex,
                     "Performance Calculation Error",
                     JOptionPane.ERROR_MESSAGE);          
               }
	       //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            }
         } catch(Exception ex) { 
            ex.printStackTrace();
            JOptionPane.showMessageDialog( null,
               "Error in calculating Performance Delays:\n" + ex,
               "Performance Parameters Calculation Error",
               JOptionPane.ERROR_MESSAGE);          
         }
      }
   }

   private boolean getPerformanceTypeConsole(String perfType) 
   {
      try {
         String header = "Define the Performance Type Console:";
         PerformanceTypeDialog dialog =
            new PerformanceTypeDialog(new JFrame(),true,header,perfType);
         if( !dialog.getAnswer())
            return false;
         isSrcConsole = dialog.getSrcConsole();
         isDstConsole = dialog.getDstConsole();

      } catch(Exception ex) {
         JOptionPane.showMessageDialog( null,
            "Error in fetching performance type console:\n" + ex,
            "Performance Type Console Error",
            JOptionPane.ERROR_MESSAGE);          
         return false;
      }
      logger.debug("doSpecifications(): isSrcConsole="+isSrcConsole);
      logger.debug("doSpecifications(): isDstConsole="+isDstConsole);
      return true;
   }

   private void doSetupDelays(String perfType, String xcmd)
   {
      logger.debug("doSetupDelays(): perfType="+perfType +" xcmd="+xcmd);
      boolean retval = getPerformanceTypeConsole(perfType);
      if( !retval) return;

      String title = IvsDelaysConstants.IVS_PERFORMANCE_PARAMETERS_SETUP;
      String xmlFile = IvsDelaysConstants.getUserIvsDelaysFile(perfType,
         isSrcConsole, isDstConsole, xcmd);
      logger.debug("doSetupDelays(): xmlFile="+xmlFile);

      try {
         IvsDelaysSetupDialog dialog = new IvsDelaysSetupDialog(new JFrame(),
            true, title, xmlFile);
         logger.debug("IvsDelaysSetupDialog: answer="+dialog.getAnswer());
      } catch(Exception ex) { 
         ex.printStackTrace();
         JOptionPane.showMessageDialog( null,
            "Error in Delays Setup:\n" + ex,
            "Delays Setup Error",
            JOptionPane.ERROR_MESSAGE);          
      }
   }

   private void doSpecifications(String perfType, String xcmd)
   {
      logger.debug("doSpecifications(): perfType="+perfType);
      boolean retval = getPerformanceTypeConsole(perfType);
      if( !retval) return;

      if( TAG_SPEC_GCSD.equals(xcmd) ||
          TAG_SPEC_GCSD_UNCONFIRMED.equals(xcmd))
      {
         runTestNumber++;
         String specTab = "SPEC-"+runTestNumber;
         String type = IvsDelaysConstants.TAG_IVS_GCSD;
         String title = perfType+"- "+IvsDelaysConstants.IVS_PERFORMANCE_RECOMMENDATIONS;
	 boolean isConfirmed = (TAG_SPEC_GCSD.equals(xcmd) ? true : false);
         try {
            XmlIvsDelays xmldoc = XmlIvsDelaysSpec.generateIvsGcsd(perfType,isSrcConsole,isDstConsole,isConfirmed);
            IvsDelaysMeasurementPanel gcsdPanel = 
               new IvsDelaysMeasurementPanel(xmldoc,title,type,false,true);
            JScrollPane gcsdScroll = new JScrollPane(gcsdPanel);
            getJTabbedPane().add( specTab, gcsdScroll);
            getJTabbedPane().setSelectedComponent(gcsdScroll);
         } catch(Exception ex) { 
            ex.printStackTrace();
            JOptionPane.showMessageDialog( null,
               "Error in fetching performance spec:\n" + ex,
               "Performance Specifications Error",
               JOptionPane.ERROR_MESSAGE);          
         }
      }
      else if( TAG_SPEC_GMTD.equals(xcmd))
      {
         runTestNumber++;
         String specTab = "SPEC-"+runTestNumber;
         String type = IvsDelaysConstants.TAG_IVS_GMTD;
         String title = perfType+"- "+IvsDelaysConstants.IVS_PERFORMANCE_RECOMMENDATIONS;
	 //NOTE: the spec is generated dynamically !!!
         try {
            XmlIvsDelays xmldoc = XmlIvsDelaysSpec.generateIvsGmtd(perfType,isSrcConsole,isDstConsole);
            IvsDelaysMeasurementPanel gmtdPanel = 
               new IvsDelaysMeasurementPanel(xmldoc,title,type);
            JScrollPane gmtdScroll = new JScrollPane(gmtdPanel);
            getJTabbedPane().add( specTab, gmtdScroll);
            getJTabbedPane().setSelectedComponent(gmtdScroll);
         } catch(Exception ex) { 
            ex.printStackTrace();
            JOptionPane.showMessageDialog( null,
               "Error in fetching performance spec:\n" + ex,
               "Performance Specifications Error",
               JOptionPane.ERROR_MESSAGE);          
         }
      }
      else if( TAG_SPEC_UCSD.equals(xcmd) ||
               TAG_SPEC_UCSD_CHECK.equals(xcmd))
      {
         runTestNumber++;
         String specTab = "SPEC-"+runTestNumber;
         String type = IvsDelaysConstants.TAG_IVS_UCSD;
         String title = perfType+"- "+IvsDelaysConstants.IVS_PERFORMANCE_RECOMMENDATIONS;
	 boolean isDirect = (TAG_SPEC_UCSD.equals(xcmd) ? true : false);
         try {
            XmlIvsDelays xmldoc = XmlIvsDelaysSpec.generateIvsUcsd(perfType,isSrcConsole,isDstConsole,isDirect);
            IvsDelaysMeasurementPanel ucsdPanel = 
               new IvsDelaysMeasurementPanel(xmldoc,title,type);
            JScrollPane ucsdScroll = new JScrollPane(ucsdPanel);
            getJTabbedPane().add( specTab, ucsdScroll);
            getJTabbedPane().setSelectedComponent(ucsdScroll);
         } catch(Exception ex) { 
            JOptionPane.showMessageDialog( null,
               "Error in fetching performance spec:\n" + ex,
               "Performance Specifications Error",
               JOptionPane.ERROR_MESSAGE);          
         }
      }
      else if( TAG_SPEC_UMTD.equals(xcmd))
      {
         runTestNumber++;
         String specTab = "SPEC-"+runTestNumber;
         String type = IvsDelaysConstants.TAG_IVS_UMTD;
         String title = perfType+"- "+IvsDelaysConstants.IVS_PERFORMANCE_RECOMMENDATIONS;
         try {
            XmlIvsDelays xmldoc = XmlIvsDelaysSpec.generateIvsUmtd(perfType,isSrcConsole,isDstConsole);
            IvsDelaysMeasurementPanel umtdPanel = 
               new IvsDelaysMeasurementPanel(xmldoc,title,type);
            JScrollPane umtdScroll = new JScrollPane(umtdPanel);
            getJTabbedPane().add( specTab, umtdScroll);
            getJTabbedPane().setSelectedComponent(umtdScroll);
         } catch(Exception ex) { 
            JOptionPane.showMessageDialog( null,
               "Error in fetching performance spec:\n" + ex,
               "Performance Specifications Error",
               JOptionPane.ERROR_MESSAGE);          
         }
      }
   }

   //========================================================================
   public static void main(String[] args) throws Exception
   {
      String host = "";
      try {
         host = IpAddressUtility.getLocalHostAddress().getHostAddress();
      }
      catch(Exception ex) { }
      String tag = ISSILogoConstants.VERSION+"-"+ISSILogoConstants.BUILD +
             "      Host: " +host;

      JFrame frame = new JFrame("TraceAnalyzerGUI v"+tag);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setIconImage(new ImageIcon(ISSILogoConstants.ISSI_TESTER_LOGO).getImage());

      InfiniteProgressPanel progressPane = new InfiniteProgressPanel();
      frame.setGlassPane( progressPane);

      TraceAnalyzerGUI panel = new TraceAnalyzerGUI(progressPane);
      frame.setJMenuBar( panel.getJMenuBar());
      frame.getContentPane().add(panel);
      frame.setSize( 900, 740);
      frame.setVisible(true);
   }
}
