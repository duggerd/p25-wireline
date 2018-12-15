//
package gov.nist.p25.issi.testlauncher;

import gov.nist.p25.common.swing.util.ComponentUtils;
import gov.nist.p25.common.swing.util.PrintUtilities;
import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.common.util.StackTraceUtility;

import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.GlobalTopologyParser;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfigParser;
import gov.nist.p25.issi.message.ConformanceTestConfigManager;
import gov.nist.p25.issi.message.XmlGlobalTopology;
import gov.nist.p25.issi.message.XmlPttmessages;
import gov.nist.p25.issi.message.XmlSystemTopology;

import gov.nist.p25.issi.rfss.tester.TestScript;
import gov.nist.p25.issi.rfss.tester.TestScriptParser;
import gov.nist.p25.issi.setup.ISSIConfigManager;
import gov.nist.p25.issi.setup.TestConfigControlDialog;
import gov.nist.p25.issi.setup.TopologyConfigHelper;
import gov.nist.p25.issi.traceviewer.MessageData;
import gov.nist.p25.issi.traceviewer.PttMessageData;
import gov.nist.p25.issi.traceviewer.PttSessionInfo;
//import gov.nist.p25.issi.traceviewer.PttTraceLoader;
import gov.nist.p25.issi.traceviewer.RfssData;
import gov.nist.p25.issi.traceviewer.RfssRuntimeDataParser;
import gov.nist.p25.issi.traceviewer.SipTraceLoader;
import gov.nist.p25.issi.traceviewer.TestLayoutPanel;
import gov.nist.p25.issi.traceviewer.TraceLogTextPane;
import gov.nist.p25.issi.traceviewer.TracePanel;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane; 
//import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Test execution panel.
 */
public class TestExecutionPanel extends JPanel 
      implements MouseListener, ActionListener {

   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(TestExecutionPanel.class);
   public static void showln(String s) { System.out.println(s); }

   //public static final int TEST_TIMEOUT = ISSITesterConstants.TEST_RUNS_FOR;
   public static SimpleAttributeSet ERROR_MESSAGE_TEXT = new SimpleAttributeSet();
   public static SimpleAttributeSet STACK_TRACE_TEXT = new SimpleAttributeSet();
   public static SimpleAttributeSet INFO_TEXT = new SimpleAttributeSet();
   public static SimpleAttributeSet FATAL_MESSAGE_TEXT = new SimpleAttributeSet();
   
   private static String baseDir = System.getProperty("user.dir");
   private static boolean verbose = false;
   private static boolean isPrtMenu = true;

   static {
      StyleConstants.setForeground(ERROR_MESSAGE_TEXT, Color.red);
      StyleConstants.setForeground(STACK_TRACE_TEXT, Color.blue);
      StyleConstants.setForeground(INFO_TEXT, Color.black);
      StyleConstants.setForeground(FATAL_MESSAGE_TEXT, Color.red);
      StyleConstants.setUnderline(FATAL_MESSAGE_TEXT, true);
      StyleConstants.setFontSize(FATAL_MESSAGE_TEXT, 14);
   }
   
   // The timer for TimerTasks associated with this receiver
   private static Timer timer = new Timer();

   private TestExecutionTimerTask testExecutionTimerTask;
   private DietsService testerService;

   private boolean isSetupTest;
   private DietsGUI gui;
   private String testTitle = "Conformance Test";
   private String currentTestName = "";
   private String currentTopologyName = "";

   private TitledBorder testBorder;
   private JList jlist;
   private JButton testButton;
   private JButton setupButton;
   private JButton generateButton;
   private JButton runInteractiveButton;

   private TraceLogTextPane descriptionPane;
   private TraceLogTextPane systemTopologyPane;
   private TraceLogTextPane globalTopologyPane;
   private TraceLogTextPane testTopologyPane;
   private TraceLogTextPane testConfigPane;
   private TraceLogTextPane testScriptPane;
   private TraceLogTextPane testStatusPane;
   private TestLayoutPanel testLayoutPane;

   private TracePanel sipTracePanel;
   private TracePanel pttTracePanel;
   private String progressScreenMessage = "";
   private ProgressMonitor progressMonitor;

   private JTabbedPane tabbedPane;
   private JScrollPane statusLogScrollPane;

   private int refTestNumber = 0;
   private int runTestNumber = 0;
   private int numOfSipTraces = 0;
   private int numOfPttTraces = 0;

   private String sipErrorMsg = "";
   private String pttErrorMsg = "";

   // accessors
   // --------------------------------------------------------------------------
   public Timer getTimer() {
      return timer;
   }

   public String getCurrentTopologyName() {
      return currentTopologyName;
   }
   public void setCurrentTopologyName(String currentTopologyName) {
      this.currentTopologyName = currentTopologyName;
   }

   public TestCaseDescriptor getCurrentTestCaseDescriptor() {
      return (TestCaseDescriptor) jlist.getSelectedValue();
   }

   public void setCurrentTest(String testDir, String topologyFileName) {
      this.currentTestName = testDir + "/testscript.xml";
      setCurrentTopologyName( topologyFileName);
   }

   public String getStartupPropertiesFileName() {
      return gui.getStartupPropertiesFileName();
   }

   public void doStaticReInitialization(String startupFile)
      throws Exception
   {
      clearTopologyConfig("doStaticReInitialization");
      gui.doStaticReInitialization( startupFile);
   }

   public void newProgressMonitor() {
      setProgressMonitor(new ProgressMonitor(gui));
   }

   public ProgressMonitor getProgressMonitor() {
      return progressMonitor;
   }

   public void setProgressMonitor(ProgressMonitor monitor) {
      this.progressMonitor = monitor;
   }

   public void stopProgressMonitor() {
      setProgressScreenMessage("Done.");
      if (getProgressMonitor() != null) {
         getProgressMonitor().done();
         setProgressMonitor(null);
      }
      enableAllButtons();
   }

   public String getProgressScreenMessage() {
      return progressScreenMessage;
   }

   public void setProgressScreenMessage(String progressScreenMessage) {
      this.progressScreenMessage = progressScreenMessage;
      if (getProgressMonitor() != null) {
         synchronized (getProgressMonitor()) {
            getProgressMonitor().notify();
         }
      }
   }

   public TracePanel getSipTracePanel() {
      return sipTracePanel;
   }

   public TracePanel getPttTracePanel() {
      return pttTracePanel;
   }

   public String getSipTestError() {
      return sipErrorMsg;
   }
   public void setSipTestError(String errorMsg) {
      this.sipErrorMsg = errorMsg;
   }

   public String getPttTestError() {
      return pttErrorMsg;
   }
   public void setPttTestError(String errorMsg) {
      this.pttErrorMsg = errorMsg;
   }

   public void incrementRunTestNumber() {
      runTestNumber++;
      setSipTestError(null);
      setPttTestError(null);
   }

   public void incrementRefTestNumber() {
      refTestNumber++;
      setSipTestError(null);
      setPttTestError(null);
   }

   public void clearTestTraces() {
      refTestNumber = 0;
      runTestNumber = 0;
      setSipTestError(null);
      setPttTestError(null);
      gui.getJTabbedPane().removeAll();
      // for Pass/Fail grading
      numOfSipTraces = 0;
      numOfPttTraces = 0;
   }

   // logging
   public void logSystemTopologyPane(boolean isClear, String message) {
      systemTopologyPane.logMessage(isClear, message);
   }

   public void logGlobalTopologyPane(boolean isClear, String message) {
      globalTopologyPane.logMessage(isClear, message);
   }

   public void logTestConfigPane(boolean isClear, String message) {
      testConfigPane.logMessage(isClear, message);
   }

   public void logTestStatusPane(boolean isClear, String message) {
      testStatusPane.logMessage(isClear, message);
   }

   // buttons
   public void disableAllButtons() {
      testButton.setEnabled(false);
      setupButton.setEnabled(false);
      generateButton.setEnabled(false);
      runInteractiveButton.setEnabled(false);
      jlist.setEnabled(false);
   }

   public void enableAllButtons() {
      testButton.setEnabled(true);
      setupButton.setEnabled(true);
      generateButton.setEnabled(true);
      runInteractiveButton.setEnabled(true);
      jlist.setEnabled(true);
   }

   public void enableSetupButton() {
      disableAllButtons();
      testButton.setEnabled(true);
      setupButton.setEnabled(true);
      jlist.setEnabled(true);
   }

   public void doSetupTest() {
      setupButton.requestFocus();
      setupButton.doClick();
   }

   // --------------------------------------------------------------------------
   /**
    * A class that waits for test execution to complete. This is basically 
    * an open loop timer. It is used in non-interactive execution.
    */
   class TestExecutionTimerTask extends TimerTask {

      private boolean interactive;
      private int secondsRemaining;
      private DietsGUI gui;

      public int getSecondsRemaining() {
         return secondsRemaining;
      }

      public TestExecutionTimerTask(DietsGUI gui, int seconds, boolean interactive) {
         this.gui = gui;
         this.secondsRemaining = seconds;
         this.interactive = interactive;
         gui.getStatusProgressBar().setIndeterminate(true);
         newProgressMonitor();
      }

      public void run() {
         secondsRemaining = secondsRemaining - 1000;
         if (secondsRemaining > 0) {
            setProgressScreenMessage("Running.  Time remaining: "
                  + (secondsRemaining / 1000) + " seconds");
         }
         
         boolean completed = false;
         try {
            Thread.sleep(300L);
            completed = gui.getRemoteController().isTestCompleted();
            // New
            //completed &= gui.getRemoteController().isSaveTraceCompleted();           
         } 
         catch(Exception ex) {}

         showln("MMM check for test completed="+completed+" remains="+secondsRemaining);        
         if (completed) secondsRemaining = 0;
         if (secondsRemaining == 0) {
            showln("CANCEL timer: secondsRemaining="+secondsRemaining);
            cancel();
            if (!interactive) {
               getSipPttTraces(interactive);
            }
         }
      }
   }


   // constructor
   // --------------------------------------------------------------------------
   public TestExecutionPanel(Vector<TestCaseDescriptor> tests, DietsGUI gui)
   {
      super();
      this.gui = gui;

      setLayout(new BorderLayout());
      testBorder = new TitledBorder(testTitle);
      setBorder( testBorder);

      JPanel listPanel = new JPanel();
      listPanel.setPreferredSize(new Dimension(520, 300));
      listPanel.setLayout(new BorderLayout());

      //jlist = new JList(tests);
      jlist = new JList(tests) {
         public String getToolTipText(MouseEvent evt) {
            int index = locationToIndex(evt.getPoint());
            TestCaseDescriptor tc = (TestCaseDescriptor)getModel().getElementAt(index);
            return "Index: "+ index +" <=> "+tc.getTestNumber();
         }
      };
      jlist.addMouseListener(this);
      jlist.setSelectedIndex(0);
      JScrollPane scrollList = new JScrollPane(jlist);

      testButton = new JButton("Setup Topology");
      testButton.setToolTipText("setup Global/System Topology");
      testButton.addActionListener(this);

      setupButton = new JButton("Setup Test");
      setupButton.setToolTipText("Setup RFSS configuration");
      setupButton.addActionListener(this);

      generateButton = new JButton("Generate Trace");
      generateButton.setToolTipText("Generate Reference Trace");
      generateButton.addActionListener(this);

      runInteractiveButton = new JButton("Run Test");
      runInteractiveButton.setToolTipText(
         "Run in interactive mode - simulates actual operation");
      runInteractiveButton.addActionListener(this);

      JPanel buttonPanel = new JPanel(new GridLayout(1, 4));
      buttonPanel.add(testButton);
      buttonPanel.add(setupButton);
      buttonPanel.add(generateButton);
      buttonPanel.add(runInteractiveButton);

      listPanel.add(scrollList, BorderLayout.CENTER);
      listPanel.add(buttonPanel, BorderLayout.SOUTH);

      //TestCaseDescriptor selectedTest = getCurrentTestCaseDescriptor();
      //String testDesc = selectedTest.toString();
      //String testNo = selectedTest.getTestNumber();
      
      // Description
      descriptionPane = new TraceLogTextPane( );
      JScrollPane descriptionScrollPane = new JScrollPane(descriptionPane);
      
      // System
      systemTopologyPane = new TraceLogTextPane( );
      //===JScrollPane systemTopologyScrollPane = new JScrollPane(systemTopologyPane);

      // Global
      globalTopologyPane = new TraceLogTextPane( );
      //===JScrollPane globalTopologyScrollPane = new JScrollPane(globalTopologyPane);

      // Test Topology
      testTopologyPane = new TraceLogTextPane( );
      JScrollPane testTopologyScrollPane = new JScrollPane(testTopologyPane);

      // Test Configuration   
      testConfigPane = new TraceLogTextPane( );
      JScrollPane configureScrollPane = new JScrollPane(testConfigPane);

      // Test Script
      testScriptPane = new TraceLogTextPane( );
      JScrollPane scriptScrollPane = new JScrollPane(testScriptPane);

      // Test Status
      testStatusPane = new TraceLogTextPane( );
      statusLogScrollPane = new JScrollPane(testStatusPane);

      // Test Layout
      testLayoutPane = new TestLayoutPanel( );
      JScrollPane testLayoutScrollPane = new JScrollPane(testLayoutPane);

      // populate the tabs
      updateTestTabbedPane( jlist.getSelectedIndex());
      
      // setup tabbed pane
      //----------------------------------------------------
      tabbedPane = new JTabbedPane();
      tabbedPane.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            tabbedPaneClicked(e);
         }
      });

      tabbedPane.add("Description", descriptionScrollPane);
      tabbedPane.setSelectedComponent(descriptionScrollPane);
      // Disabled
      //tabbedPane.add("System Topology", systemTopologyScrollPane);
      //tabbedPane.add("Global Topology", globalTopologyScrollPane);
      //
      tabbedPane.add("Test Topology", testTopologyScrollPane);
      tabbedPane.add("Test Configuration", configureScrollPane);
      tabbedPane.add("Test Script", scriptScrollPane);
      tabbedPane.add("Test Layout", testLayoutScrollPane);
      tabbedPane.add("Test Status", statusLogScrollPane);

      JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            listPanel, tabbedPane);
      splitPane.setOneTouchExpandable(true);
      splitPane.setDividerLocation(0.50);
      splitPane.setDividerSize(7);
      add(splitPane, BorderLayout.CENTER);

      enableSetupButton();
      try {
         startServices();
      }
      catch(Exception ex) {
         logger.debug("startService: "+ex);
         String msg = "Failed to startServices: " + ex.getMessage() +
            "\nPlease exit the application and configure ipaddress in " +
            "\nstartup/diets.daemon.localhost.properties and" +
            "\ntesterconfig/standalone-configuration.xml\n";
         JOptionPane.showMessageDialog(null, msg,
            "Start Services Error",
            JOptionPane.ERROR_MESSAGE);
         System.exit(-1);
      }
   }

   public void updateTestCases(String testClass, Vector<TestCaseDescriptor> testVec) {
      //showln("TestExecutionPanel: updateTestCases="+testClass);
      testTitle = testClass+" Test";
      testBorder.setTitle(testTitle);
      jlist.removeAll();
      jlist.setListData(testVec);
      updateUI();
   } 
   public void updateTestCases(String testClass) throws Exception {
      gui.doClickTestClass(testClass);
   } 

   //------------------------------------------------------------------------------
   public void logError(String failureMessage, String errorTrace) {
      testStatusPane.logMessage(true, failureMessage);
      testStatusPane.logMessage(false, errorTrace);
      tabbedPane.setSelectedComponent(statusLogScrollPane);
   }

   public void logError(String failureMessage) {
      testStatusPane.logMessage(true, failureMessage + "\n");
      tabbedPane.setSelectedComponent(statusLogScrollPane);
   }

   public void logFatal(String failureMessage, String stackTrace) {
      testStatusPane.logMessage(true, failureMessage + "\n");
      testStatusPane.logMessage(false, stackTrace);
      tabbedPane.setSelectedComponent(statusLogScrollPane);
   }

   public void logStatusMessage(String statusMessage) {
      testStatusPane.logMessage(true, statusMessage + "\n");
   }
   
   public void updateSuggestedFile(String baseDir, String testNo) { 
      // make testcase specific output/testNo directory
      String outDir = baseDir + File.separator + "output" + File.separator + testNo;
      FileUtility.makeDir( outDir);
      
      String suggestFile;      
      // Description
      suggestFile = outDir + File.separator + "test-description-" +testNo +".txt";
      descriptionPane.setSuggestedFile( suggestFile);
      
      // System Topology
      suggestFile = outDir + File.separator + "system-topology-" +testNo +".txt";
      systemTopologyPane.setSuggestedFile( suggestFile);
      
      // Global Topology
      suggestFile = outDir + File.separator + "global-topology-"+testNo+".txt";
      globalTopologyPane.setSuggestedFile( suggestFile);      

      // Test Topology
      suggestFile = outDir + File.separator + "test-topology-" +testNo +".txt";
      testTopologyPane.setSuggestedFile( suggestFile);
      
      // Test Configuration
      suggestFile = outDir + File.separator + "test-configuration-"+testNo+".txt";
      testConfigPane.setSuggestedFile( suggestFile);

      // Test Script
      suggestFile = outDir + File.separator + "test-script-"+testNo+".txt";
      testScriptPane.setSuggestedFile( suggestFile);
      
      // Test Status
      suggestFile = outDir + File.separator + "test-status-"+testNo+".txt";
      testStatusPane.setSuggestedFile( suggestFile);     
   }

   public void startServices() throws Exception {
      //showln("\nstartServices(): START....");
      if( testerService != null) {
          stopServices();
      }

      String propertiesFileName = getStartupPropertiesFileName();
      DietsConfigProperties props = null;
      String testSuiteName = null;
      String testerTopologyFileName = null;
      String systemTopologyFileName = null;

      //TODO: This all should be replaced with ISSIConfigManager !!! 
      props = new DietsConfigProperties(propertiesFileName);
      testSuiteName = props.getProperty(DietsConfigProperties.TESTSUITE_PROPERTY);
      testerTopologyFileName = props.getProperty(DietsConfigProperties.DAEMON_CONFIG_PROPERTY);
      systemTopologyFileName = props.getProperty(DietsConfigProperties.SYSTEM_TOPOLOGY_PROPERTY);

      showln("  propertiesFileName="+propertiesFileName);
      showln("  testSuiteName="+testSuiteName);
      showln("  testerTopologyFileName="+testerTopologyFileName);
      showln("  systemTopologyFileName="+systemTopologyFileName);
      showln("----------------------------------------------");

      testerService = new DietsService(props, testSuiteName,
         testerTopologyFileName, systemTopologyFileName, this);

      testerService.startDaemon();
      //showln("startServices(): startDaemon() DONE....");
   }

   public void stopServices() {
      //showln("stopServices(): START....");
      if( testerService == null) return;
      try {
         testerService.stopDaemon();
      }
      catch(Throwable ex) { 
         // ignore
      }
      finally {
         testerService = null;
      }
      //showln("stopServices(): DONE....");
   }

   public void closeShop() {
      //showln("TestExectionPanel: closeShop(): START....");
      try {
         testerService.stopTestServices();
      } 
      catch(Exception ex) { }
      stopServices();
      //showln("TestExectionPanel: closeShop(): DONE....");
   }

   //------------------------------------------------------------------------------
   public void sendSetConformanceTest(boolean verbose)
   {
      String testClass = gui.getTestClass();
      try {
         logger.debug("sendSetConformanceTest(): testClass/index="+testClass+"/"+jlist.getSelectedIndex());
         logger.debug("sendSetConformanceTest(): testValue="+jlist.getSelectedValue());
         testerService.sendSetConformanceTest(jlist.getSelectedIndex(), testClass);
      } 
      catch(Throwable ex) {
         logger.debug("sendSetConfrmanceTest(): "+ex); 
         if( verbose) {
            String msg = "Error in sendSetConformanceTest:\n"+ ex +
               "\nPlease Setup Test and check IP addresseses before Generate Trace\n";
            JOptionPane.showMessageDialog( null, msg,
               "sendSetConformanceTest Error", 
               JOptionPane.ERROR_MESSAGE);
         }
      }
   }

   //------------------------------------------------------------------------------
   public void setupByTopologyConfig(String testDesc, TopologyConfig testTopology, 
      TopologyConfig refTopology)
      throws Exception
   {
         String topoDir = ISSITesterConstants.getScenarioDir();
         showln("setupByTopologyConfig: "+testDesc);
         showln("   currentTopologyDir: "+topoDir);

         Map<String,String> nameMap = null;
         //------------------------------------------------------------
         //try
         {
            //--------------------
            showln("TestExecutionPanel: NumberOfRFSS="+ testTopology.getRfssConfigurations().size());
            //--------------------
            TestConfigControlDialog dialog = new TestConfigControlDialog(null, true,
               testDesc, testTopology, refTopology);

            boolean answer = dialog.getAnswer();
            //showln("TestExecutionPanel: dialog-answer: "+ answer);
            //isSetupTest = true;
            if( answer) {

               nameMap = dialog.getRfssNameMap();
               showln("TestExecutionPanel: nameMap: "+ nameMap);

               String stFile = gui.getSystemTopologyFileName();
               String gtFile = gui.getGlobalTopologyFileName();
               showln("TestExecutionPanel: stFile: "+ stFile);
               showln("TestExecutionPanel: gtFile: "+ gtFile);

               // fix system config
               XmlSystemTopology stxmlDoc = new XmlSystemTopology();
               stxmlDoc.loadSystemTopology( stFile);
               stxmlDoc.reconcile( dialog.getRfssConfigSetupList());
               stxmlDoc.saveSystemTopology( "logs/systemtopology.xml");

               // fix global config: RfssName and SuId
               String gtxmlMsg = FileUtility.loadFromFileAsString( gtFile);
               XmlGlobalTopology gtxmlDoc = new XmlGlobalTopology();
               gtxmlDoc.loadGlobalTopology( gtxmlMsg);
               gtxmlDoc.reconcileRfssName( nameMap);
               gtxmlDoc.reconcileSuConfig( dialog.getSuConfigSetupList());
               gtxmlDoc.saveGlobalTopology( "logs/globaltopology.xml");

               // fixup the daemon IP-Address
               dialog.reconcile(gui.getIssiTesterConfiguration());

               // check host is reachable
               dialog.pingHost(true); 

               // save a copy of testerConfig
               showln(">>>SAVE testerConfig="+gui.getTesterConfigurationFile());
               FileUtility.saveToFile( "logs/tester-configuration.xml",
                  gui.getIssiTesterConfiguration().toString());

               showln("TEP-systemTopology=\n"+testTopology.exportSystemTopology());
               showln("TEP-globalTopology=\n"+testTopology.exportGlobalTopology());
               showln("TEP-testerConfig=\n"+gui.getIssiTesterConfiguration());

               int yesno = JOptionPane.showConfirmDialog( null,
                  "Would you like to save and distribute configuration files ?\n" +
                  "Please be patience, save operation will take ~30 seconds...\n" +
                  "The configuration files will be distributed accordingly....\n\n",
                  "Save and Start Test Service",
                  JOptionPane.YES_NO_OPTION);
               if( yesno == JOptionPane.YES_OPTION) {

                  //NOTE: doesnot save system and global topology
                  testerService.saveConfiguration(gui.getIssiTesterConfiguration(),testTopology);

                  // move system and global config
                  FileUtility.copyFile(new File("logs/systemtopology.xml"), new File(stFile));
                  FileUtility.copyFile(new File("logs/globaltopology.xml"), new File(gtFile));

                  // fix current topology configs
                  boolean saveTopo = true;
                  showln("*** reconsileRfssName()...save="+saveTopo);
                  ConformanceTestConfigManager manager = new ConformanceTestConfigManager();
                  List<String> modList = manager.reconcileRfssName(nameMap,
                     new File(topoDir), saveTopo);

                  // add glabal and system
                  modList.add( stFile);
                  modList.add( gtFile);
                  try {
                     // ship testscript topology.xml
                     testerService.shipConfigFilesList( modList);

                     // ship properties, config and redraw commands
                     testerService.shipConfigFiles();

                  } catch(Throwable ex) {
                     // change in IP, but host is not running
                     showln("Connect error: "+ex); 
                  }

                  //--------------------------------------------------
                  try {
                     showln("*** sendStaticReinitialization()...");
                     testerService.sendStaticReinitialization();
                     Thread.sleep(1000L);
                  } catch(Throwable ex) {
                     showln("sendStaticReinitialization: "+ex); 
                  }
                  showln("*** doStaticReInitialization()...");
                  doStaticReInitialization( getStartupPropertiesFileName());

                  showln("*** startService()...BOUNCE DAEMON");
                  startServices();
                  updateTestTabbedPane( jlist.getSelectedIndex());

		  sendSetConformanceTest(false);

                  String msg = "Test Configuration Setup is successful.\n" +
                     "Total number of files modified: " + modList.size() +"\n" +
                     "Before testing, please sync clock time in all RFSSes\n";
                  JOptionPane.showMessageDialog(null, msg,
                     "Test Setup Summary",
                     JOptionPane.INFORMATION_MESSAGE);
               }   // save-config

            }  // answer=yes
            else
            {
               // answer=no
               // check host is reachable
               dialog.pingHost(true);

               sendSetConformanceTest(false);

            }  // answer=no
         } 
        /*****
         catch(Exception ex) {
            String msg = "Error in updating Test Configuration based on user input:\n";
            logger.debug( msg, ex);
            JOptionPane.showMessageDialog(null, 
               msg + ex.getMessage(),
               "Test Config Setup Error",
               JOptionPane.ERROR_MESSAGE);
            isSetupTest = false;
            throw ex;
         }
         finally {
            // start test service
            try {
               showln("*** stopTestServices()...");
               testerService.stopTestServices();

               showln("*** startTestServices()...");
               testerService.startTestServices();
               if( isSetupTest) {
                  enableAllButtons();
               } else {
                  enableSetupButton();
               } 
            } 
            catch(Exception ex) {
               int response = JOptionPane.showConfirmDialog(null, 
                  ex.getMessage()+"\nDo you want to continue ?",
                  "Test Config Setup Error(2)",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE);
               enableSetupButton();
               if(response == JOptionPane.YES_OPTION ||
                  response == JOptionPane.CLOSED_OPTION)
                  setupButton.doClick();
            }
         }  //finally
          *****/
   }

   //--------------------------------------------------------------------------
   // implementation of ActionListener
   //--------------------------------------------------------------------------
   public void actionPerformed(ActionEvent actionEvent) {

      gui.updateGlobalTopologyValues();
      Object source = actionEvent.getSource();
      //-----------------------------------------------------------------------
      if (source == testButton) {

         clearTopologyConfig("Setup Topology");
         logger.debug(">>>>>>>>>>>>>>> Setup Topology >>>>>>>>>>>>>>>>");
         disableAllButtons();
         clearTestTraces();

         try {
            ISSIConfigManager configMgr = new ISSIConfigManager(getStartupPropertiesFileName());
            boolean interactive= true;
            TopologyConfig globalTopology = configMgr.getGlobalTopologyConfig( interactive);

            isSetupTest = true;
            setupByTopologyConfig("Global Topology Setup", globalTopology, null);
         }
         catch(Exception ex) {
            String msg = "Error in updating Test Configuration based on user input:\n";
            logger.debug( msg, ex);
            JOptionPane.showMessageDialog(null, 
               msg + ex.getMessage(),
               "Test Config Setup Error",
               JOptionPane.ERROR_MESSAGE);
            isSetupTest = false;
         }
         finally {
            // start test service
            try {
               showln("*** stopTestServices()...");
               testerService.stopTestServices();

               showln("*** startTestServices()...");
               testerService.startTestServices();
               if( isSetupTest) {
                  enableAllButtons();
               } else {
                  enableSetupButton();
               } 
            } 
            catch(Exception ex) {
               int response = JOptionPane.showConfirmDialog(null, 
                  ex.getMessage()+"\nDo you want to continue ?",
                  "Test Config Setup Error(0)",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE);
               enableSetupButton();
               if(response == JOptionPane.YES_OPTION ||
                  response == JOptionPane.CLOSED_OPTION)
                  testButton.doClick();
            }
         }  //finally
      }
      //-----------------------------------------------------------------------
      else if (source == setupButton) {

         clearTopologyConfig("Setup Test");
         logger.debug(">>>>>>>>>>>>>>> Setup Test >>>>>>>>>>>>>>>>");
         disableAllButtons();
         clearTestTraces();

         String topoDir = ISSITesterConstants.getScenarioDir();
         String testDesc = getCurrentTestCaseDescriptor().toString();
         showln(">>>>> Setup Test Config: "+testDesc);
         showln("     currentTopologyDir: "+topoDir);
         showln("    currentTopologyName: "+getCurrentTopologyName());

         //------------------------------------------------------------
         try {
            TopologyConfig testTopology = getTopologyConfig(getCurrentTopologyName());

            TopologyConfigHelper helper = new TopologyConfigHelper();
            helper.reconfigureDaemonTesterService( testTopology, gui.getIssiTesterConfiguration());

            // global topology is used for data validation of test topology
            ISSIConfigManager configMgr = new ISSIConfigManager( getStartupPropertiesFileName());
            TopologyConfig globalTopology = configMgr.getGlobalTopologyConfig(true);

            isSetupTest = true;
            setupByTopologyConfig(testDesc, testTopology, globalTopology); 
         } 
         catch(Exception ex) {
            String msg = "Error in updating Test Configuration based on user input:\n";
            logger.debug( msg, ex);
            JOptionPane.showMessageDialog(null, 
               msg + ex.getMessage(),
               "Test Config Setup Error",
               JOptionPane.ERROR_MESSAGE);
            isSetupTest = false;
         }
         finally {
            // start test service
            try {
               showln("*** stopTestServices()...");
               testerService.stopTestServices();

               showln("*** startTestServices()...");
               testerService.startTestServices();
               if( isSetupTest) {
                  enableAllButtons();
               } else {
                  enableSetupButton();
               } 
            } 
            catch(Exception ex) {
               int response = JOptionPane.showConfirmDialog(null, 
                  ex.getMessage()+"\nDo you want to continue ?",
                  "Test Config Setup Error(2)",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE);
               enableSetupButton();
               if(response == JOptionPane.YES_OPTION ||
                  response == JOptionPane.CLOSED_OPTION)
                  setupButton.doClick();
            }
         }  //finally
      }
      //-----------------------------------------------------------------------
      else if (source == generateButton) {

         clearTopologyConfig("Generate Ref Trace");
         logger.debug( gui.getConsoleTitle());
         logger.debug(">>>>>>>>>>>>>>> Generate Ref Trace >>>>>>>>>>>>>>>>");
         disableAllButtons();
         incrementRefTestNumber();

         try {
            testerService.sendPingHost();
         } catch(Throwable ex) {
            String msg = "Error in sendPingHost:\n"+ ex +
               "\nPlease Setup Test and check IP addresseses before Generate Trace and Run Test\n";
            JOptionPane.showMessageDialog( null, msg,
               "sendPingHost Error", 
               JOptionPane.ERROR_MESSAGE);
            return;
         }

         try {
            TestCaseDescriptor selectedTest = getCurrentTestCaseDescriptor();
            currentTestName = (String) selectedTest.getFileName();
            setCurrentTopologyName( selectedTest.getTopologyFileName());
            
            showln(">>>>>>>>>>>>>> Generate Ref >>>>>>>>>>>>>>>>>>>>");
            logger.debug("globalTopology = " + gui.getGlobalTopologyFileName());
            logger.debug("systemTopology = " + gui.getSystemTopologyFileName());
            logger.debug("currentTestName = " + currentTestName);
            logger.debug("currentTopologyName = " + getCurrentTopologyName());

            showln("********* tearDown ********");
            gui.getRemoteController().tearDownCurrentTest();

            showln("********* loadTest ********");
            TestScriptParser parser = new TestScriptParser(
                  ISSITesterConstants.getScenarioDir() + "/" + currentTestName,
                  ISSITesterConstants.getScenarioDir() + "/" + getCurrentTopologyName(),
                  gui.getGlobalTopologyFileName(), 
                  gui.getSystemTopologyFileName(), false);
            parser.parse();

            logger.debug("loadTest(): currentTestName=" + currentTestName);
            gui.getRemoteController().loadTest(
                  ISSITesterConstants.getScenarioDir(), currentTestName,
                  selectedTest.getTestNumber(), getCurrentTopologyName(),
                  gui.getGlobalTopologyFileName(), gui.getSystemTopologyFileName(),
                  false, gui.getEvaluatePassFail());

            logger.debug("runTest()...");
            gui.getRemoteController().runTest();

            //startTestExecutionTimerTask(TEST_TIMEOUT, false);
            startTestExecutionTimerTask(ISSITesterConstants.TEST_RUNS_FOR, false);

         } catch (Exception e) {
            e.printStackTrace();
            logger.debug("Error communicating with the tester !", e);
            JOptionPane.showMessageDialog(
               null,
               "Error occured in generating trace. \nCheck the tester configuration file",
               "Communication Error", 
               JOptionPane.ERROR_MESSAGE);
            enableAllButtons();
         }

      //-----------------------------------------------------------------------
      } else if (source == runInteractiveButton) {

         clearTopologyConfig("Run Test");
         logger.debug( gui.getConsoleTitle());
         logger.debug(">>>>>>>>>>>>>>>>> Run Test >>>>>>>>>>>>>>>>>>>>");
         disableAllButtons();
         incrementRunTestNumber();

         try {
            TestCaseDescriptor selectedTest = getCurrentTestCaseDescriptor();
            currentTestName = (String) selectedTest.getFileName();
            setCurrentTopologyName( selectedTest.getTopologyFileName());

            showln(">>>>>>>>>>>>>>>>> Run Test >>>>>>>>>>>>>>>>>>>>");
            showln("RT  scenarioDir=" + ISSITesterConstants.getScenarioDir());
            showln("RT  currentTestName=" + currentTestName);
            showln("RT  currentTopologyName=" + getCurrentTopologyName());
            showln("-----------------------------------");
                        
            showln("********* tearDown ********");
            gui.getRemoteController().tearDownCurrentTest();

            // After generate traces, it takes time to create trace files
            // see refmessages/conformance/<testname>/...
            showln("********* loadTest ********");
            gui.getRemoteController().loadTest(
                  ISSITesterConstants.getScenarioDir(), currentTestName,
                  selectedTest.getTestNumber(), getCurrentTopologyName(),
                  gui.getGlobalTopologyFileName(), gui.getSystemTopologyFileName(),
                  true, gui.getEvaluatePassFail());

            showln("********* sleep 3 secs...");
            Thread.sleep(3000L);

            showln("********* runTest...");
            gui.getRemoteController().runTest();

            //showln("********* new TestScriptParser...");
            TestScript testScript = new TestScriptParser(
                  ISSITesterConstants.getScenarioDir() + "/" + currentTestName,
                  ISSITesterConstants.getScenarioDir() + "/" + getCurrentTopologyName(),
                  gui.getGlobalTopologyFileName(), 
                  gui.getSystemTopologyFileName(), true).parse();

            showln("********* ScenarioPrompter ********");
            new ScenarioPrompter(gui, gui.getRemoteController(), testScript,
                  gui.getScenarioPrompterState()).startPrompting();

            // Wait TEST_TIMEOUT before trying to retrieve traces.
            // testExecutionTimerTask = new TestExecutionTimerTask(gui);
            // timer.schedule(testExecutionTimerTask, 0, 1000);

         } catch (Exception ex) {
            try {
               showln("********* tearDown ********");
               gui.getRemoteController().tearDownCurrentTest();
            } catch(Exception ex2) { }
            
            logger.error("Communication error in running test !", ex);
            gui.showWarning(
               "Error Communicaticating with the tester or running the test !\n"
                  + "Exception message : " + ex.getMessage() + "\n"
                  + "Make sure test daemons are running (check tester configuration)\n"
                  + "Make sure reference traces are generated before running the test!",
               "Test Execution Error!");
            enableAllButtons();
         }
      }
      //-----------------------------------------------------------------------
   }

   public void mouseClicked(MouseEvent me) { }
   public void mouseEntered(MouseEvent me) { }
   public void mouseReleased(MouseEvent me) { }
   public void mouseExited(MouseEvent me) { }
   
   private int lastIndex = -1;
   public void mousePressed(MouseEvent me)
   {

      clearTopologyConfig("mousePressed");
      showln(">>>>>>>>>>>>>> mousePressed >>>>>>>>>>>>>>>>>>>>");
      logger.debug(">>>>>>>>>>>>>> mousePressed >>>>>>>>>>>>>>>>>>>>");
      clearTestTraces();

      int index = jlist.locationToIndex(me.getPoint());
      jlist.setSelectedIndex(index);
      boolean bflag = updateTestTabbedPane( index);

      if( lastIndex != index) {
         // force a Setup Test path
         lastIndex = index;
         enableSetupButton();
      }

      if( bflag)
         sendSetConformanceTest(true);
      /***
      try {
         showln("sendSetConformanceTest(): testClass/index="+testClass+"/"+jlist.getSelectedIndex());
         showln("sendSetConformanceTest(): testValue="+jlist.getSelectedValue());
         testerService.sendSetConformanceTest(jlist.getSelectedIndex(),testClass);
      } catch(Throwable ex) {
         String msg = "Error in sendSetConformanceTest:\n"+ ex +
            "\nPlease Setup Test and check IP addresseses before Generate Trace\n";
         JOptionPane.showMessageDialog( null, msg,
            "sendSetConformanceTest Error", 
            JOptionPane.ERROR_MESSAGE);
         return;
      }
       ***/

      /***
      logger.debug(">>>>> mousePressed: DISABLE renderRefTraceLogs()...");
      try {
         gui.renderRefTraceLogs();
      } catch (Exception ex) {
         logger.error("An unexpected execution error occured", ex);
         JOptionPane.showMessageDialog( null,
            "Error in rendering reference trace logs: "+currentTestName,
            "Render Reference Trace Error", 
            JOptionPane.ERROR_MESSAGE);
      }
       ***/
   }

   public boolean setConformanceTestByIndex(int index)
   {
      jlist.setSelectedIndex(index);
      return updateTestTabbedPane(index);
   }

   public void setTestLayoutPane()
   {
      //tabbedPane.setSelectedComponent(testLayoutPane);
      tabbedPane.setSelectedIndex(4);
   }

   public boolean updateTestTabbedPane(int index)
   {
      boolean okFlag = true;
      TestCaseDescriptor descriptor = (TestCaseDescriptor)
         jlist.getModel().getElementAt(index);

      // Description
      String text = "Category : " + descriptor.getCategory() +
             "\nTest Number : " + descriptor.getTestNumber();
      if( descriptor.getTestTitle() != null)
         text += "\nTest Title : " + descriptor.getTestTitle();

      text += "\nTest Directory : " + descriptor.getDirectoryName() +
         "\n\n" + descriptor.getTestDescription();
      descriptionPane.setText( text);

      // System Topology
      // Global Topology

      TestCaseDescriptor selectedTest = getCurrentTestCaseDescriptor();
      currentTestName = (String) selectedTest.getFileName();
      setCurrentTopologyName( selectedTest.getTopologyFileName());

      String testFileName = ISSITesterConstants.getScenarioDir() + "/" + currentTestName;
      String msg = "?";
      try {
         // Test Script
         String ftext = FileUtility.loadFromFileAsString(testFileName); 
         if( ftext != null && ftext.length() > 0)
            testScriptPane.logMessage(true, ftext);

         // initialize the output filename
         updateSuggestedFile( baseDir, selectedTest.getTestNumber());
      }
      catch (Exception ex) {
         okFlag = false;
         msg = "Error in open/reading Test Script: " + testFileName;
         logger.debug( msg, ex);
         JOptionPane.showMessageDialog(null, msg,
               "Test Script Status",
               JOptionPane.ERROR_MESSAGE);
      }

      try {
         // Test Configuration
         String ftext = formatLocalTopology(false);
         if( ftext != null && ftext.length() > 0)
            testConfigPane.logMessage(true, ftext);
         else
            return false;

         // Test Status
         String xmlftext = formatLocalTopology(true);
         if( xmlftext != null && xmlftext.length() > 0)
            testTopologyPane.logMessage(true, xmlftext);
         else
            return false;
         //testStatusPane.logMessage(true, xmlftext);
      
      } catch(Exception ex) { 
         okFlag = false;
         msg = "Error in formatLocalTopology: " + ex.toString();
         logger.debug( msg, ex);
	 return okFlag;
	 /**
         JOptionPane.showMessageDialog(null, msg,
               "Topology Format Error",
               JOptionPane.ERROR_MESSAGE);
	       **/
      }

      try {
         // Test Layout
         TopologyConfig testTopology = getTopologyConfig(getCurrentTopologyName());
         String testDesc = getCurrentTestCaseDescriptor().toString();
         String testNo = getCurrentTestCaseDescriptor().getTestNumber();
         testLayoutPane.setTopologyConfig( testDesc, testNo, testTopology);

         TopologyConfigHelper helper = new TopologyConfigHelper();
         helper.reconfigureDaemonTesterService( testTopology, gui.getIssiTesterConfiguration());
         
      } catch(Exception ex) { 
         // ignore
         okFlag = false;
         msg = "Error in open/reading Topology Config: " + testFileName;
         logger.debug( msg, ex);
         JOptionPane.showMessageDialog(null, msg,
               "Topology Config Error",
               JOptionPane.ERROR_MESSAGE);
      }
      return okFlag;
   }

   /**
    * Get the PTT mmf/smf allocation information from the Emulated RFSS.
    */
   public Hashtable<String, HashSet<PttSessionInfo>> getPttSessionInfo() {

      Hashtable<String, HashSet<PttSessionInfo>> retval = null;
      String urlStr = ISSIDtdConstants.URL_ISSI_DTD_RUNTIME_DATA;
      try {
         StringBuffer toParse = new StringBuffer();
         toParse.append("<?xml version=\"1.0\" ?>\n");
         toParse.append("<!DOCTYPE issi-runtime-data SYSTEM \"" + urlStr +"\">\n");
         toParse.append(gui.getRemoteController().getRfssPttSessionInfo(true));

         InputSource inputSource = new InputSource(new ByteArrayInputStream(
               toParse.toString().getBytes()));
         RfssRuntimeDataParser parser = new RfssRuntimeDataParser(inputSource);
         retval = parser.parse();
//TEMP
//FileUtility.saveToFile( "logs/issi-runtime-data.xml", toParse.toString());

      } catch (Exception e) {
         logger.fatal("An unexpected parse exception occured", e);
      }
      return retval;
   }

   public void getSipPttTraces(boolean isInteractive) {
      //showln("getSipPttTraces: isInteractive=" + isInteractive);
      
      // conformance or packet-monitor
      String source = gui.getTraceSource();
      logger.debug("getSipPttTraces: source=" + source);

      setProgressScreenMessage("Getting SIP traces...");

      String sipTraces = null;
      StringBuffer sbuf = new StringBuffer("<messages>\n");
      if (isInteractive) {
         try {
            if ("packet-monitor".equals(source)) {
               sbuf.append(gui.getRemoteController().getSipTraces());
            } else {
               sbuf.append(gui.getRemoteController().getStackSipLogs());
            }
         } catch (Exception e) {
            logger.error("Error in fetching SIP traces", e);
            gui.showError("Error in fetching SIP traces: " + e.getMessage(), "Trace fetch error");
            return;
         }

      } else {
         try {
            sbuf.append(gui.getRemoteController().getStackSipLog());
         } catch (Exception e) {
            logger.error("Error in fetching SIP traces", e);
            gui.showError("Error in fetching SIP traces: " + e.getMessage(), "Trace fetch error");
            return;
         }
      }
      sbuf.append("</messages>");
      sipTraces = sbuf.toString();

      if(verbose) {
         logger.debug("=== SIP TRACES: START ===\n" +sipTraces);
      }

      String pttTraces = null;
      sbuf = new StringBuffer("<pttmessages>\n");
      if (isInteractive) {
         try {
            if ("packet-monitor".equals(source)) {
               sbuf.append(gui.getRemoteController().getPttTraces());
            } else {
               // source=conformance-tester
               if(verbose)
               logger.error("getRemoteController().getStackPttLogs(0)...");
               sbuf.append(gui.getRemoteController().getStackPttLogs());
            }
         } catch (Exception e) {
            logger.error("trace fetch error", e);
            gui.showError("Error in fetching PTT traces " + e.getMessage(),
                  "Trace fetch error");
            return;
         }
      } else {
         logger.error("getRemoteController().getStackPttLog(3)...");
         try {
            sbuf.append(gui.getRemoteController().getStackPttLog());
         } catch (Exception ex) {
            logger.error("Error in fetching PTT traces", ex);
            gui.showError("Error in fetching SIP traces: " + ex.getMessage(), "Trace fetch error");
            return;
         }
      }
      sbuf.append("</pttmessages>\n");
      pttTraces = sbuf.toString();

      if( verbose) {
         logger.debug("=== PTT TRACES: START ===\n" + pttTraces);
      }

      Hashtable<String, HashSet<PttSessionInfo>> rfssSessionData = getPttSessionInfo();
      renderSipPttTraces(sipTraces, pttTraces, rfssSessionData, isInteractive, !isInteractive);
      gui.resetRemoteController();
      setProgressScreenMessage("Done.");
   }

   public void renderSipPttTraces(String sipTraces, String pttTraces,
         Hashtable<String, HashSet<PttSessionInfo>> rfssSessionData,
         boolean isInteractive, boolean toSave) {

      showln("TestExecutionPanel: renderSipPttTraces(): interactive="+isInteractive);
      //showln(" gui-global="+gui.getGlobalTopologyFileName());
      //showln(" gui-system="+gui.getSystemTopologyFileName());
      
      if( verbose) {
         logger.debug("renderSipPttTraces: isInteractive=" +isInteractive +" toSave=" +toSave);
         //logger.debug("renderSipPttTraces: >>>>> sipTraces=\n" +sipTraces);
         //logger.debug("renderSipPttTraces: >>>>> pttTraces=\n" +pttTraces);
      }

      boolean remoteRetrievalSucceeded = true;

      // First cancel the polling task
      if (testExecutionTimerTask != null) {
         testExecutionTimerTask.cancel();
      }

      Collection<RfssData> rfssList = null;
      String sipTab = "SIP-Ref-" + refTestNumber;
      String pttTab = "PTT-Ref-" + refTestNumber;
      if(isInteractive) {
         sipTab = "SIP-Test-" + runTestNumber;
         pttTab = "PTT-Test-" + runTestNumber;
      }

      boolean errFlag = false;
      try {
         try {
            byte[] stringAsBytes = sipTraces.getBytes();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                  stringAsBytes);

            boolean interactive = isInteractive;
            TopologyConfig topologyConfig = getTopologyConfig(interactive,getCurrentTopologyName());
            
            SipTraceLoader traceLoader = new SipTraceLoader( byteArrayInputStream, 
                  rfssSessionData, topologyConfig);

            rfssList = traceLoader.getSortedRfssList();
            logger.debug("renderSipPttTraces: rfssList=\n"+rfssList);

            if( rfssList.size()==0) {
               // RFSS sorting problem or timing problem 
               logger.debug("*** NOTE: Zero rfssList size, problem in sorting RFSS");
            }

            List<MessageData> records = traceLoader.getRecords();
            String msgData = traceLoader.getMessageData();

            if(!isInteractive) {
               numOfSipTraces = records.size();
               logger.debug("renderSipPttTraces: SIP Ref-records.size="+numOfSipTraces);
            }

            // Create a new tabbed panel
            sipTracePanel = new TracePanel(gui, gui.getJTabbedPane(),
                  currentTestName, rfssList, records);

            sipTracePanel.setTitle(sipTab);
            sipTracePanel.setTestNumber(getCurrentTestCaseDescriptor().getTestNumber());
            sipTracePanel.setDataTextArea(TracePanel.DATA_ALL_MSG, msgData);

            // String sbufHex = HexDump.toHex( stringAsBytes, 16);
            // sipTracePanel.setDataTextArea( TracePanel.DATA_RAW_MSG, sbufHex);
            sipTracePanel.setDataTextArea(TracePanel.DATA_RAW_MSG, sipTraces);
            if (getSipTestError() != null) {
               sipTracePanel.setDataTextArea(TracePanel.DATA_ERROR_MSG, getSipTestError());
            }

            if (gui.getRemoteController() != null) {
               boolean tpass = gui.getRemoteController().getTestResults();
               String tlabel = gradeTestResults(isInteractive, numOfSipTraces, tpass,
                  sipTracePanel.getRfssList().size(),
		  sipTracePanel.getMessageList().size());
               sipTracePanel.setTestResultLabel( tlabel);
            }
            addTraceToTabbedPanel(gui.getJTabbedPane(), sipTab, sipTracePanel);

            gui.getCloseMenuItem().setEnabled(true);
            gui.getCloseAllMenuItem().setEnabled(true);

         } catch (Exception ex) {
            logger.debug("Error in fetching SIP Trace: ", ex);
            ex.printStackTrace();
            remoteRetrievalSucceeded = false;

            String msg = "Error Fetching SIP Trace From Packet Monitor. "
                  + "Use File/Open to open SIP and PTT messages manually.\n";
            // sipTraceLogPane.logMessage( true, msg);
            testStatusPane.logMessage(true, msg);

            logger.debug("User changed the rfssName since the last test run.");
            logger.debug("The testerconfig is out of sync with refmessages.");
            logger.debug("Force a new Test Setup and Test Run.");
            errFlag = true;
         }
         //logger.debug("remoteRetrievalSucceeded="+remoteRetrievalSucceeded);

         // If remote retrieval of SIP traces failed, return here
         if (!remoteRetrievalSucceeded) {
            stopProgressMonitor();
            enableSetupButton();
            return;
         }

         // Now get PTT traces
         setProgressScreenMessage("Getting PTT Traces...");
         try {
            if( verbose) {
               logger.debug("renderSipPttTraces: PTT Traces=\n" + pttTraces);
	    }
            if (pttTraces == null) {
               pttTraces = "<pttmessages></pttmessages>";
            }

            //byte[] stringAsBytes = pttTraces.getBytes();
            //ByteArrayInputStream bais = new ByteArrayInputStream(stringAsBytes);
            //PttTraceLoader traceLoader = new PttTraceLoader(bais);
            //List<MessageData> records = traceLoader.getRecords();
            //String msgData = traceLoader.getMessageData();

            // NOTE: use new PTT format
            //----------------------------
            //boolean keepHeader = false;
            boolean keepHeader = gui.getShowPttHeaderState();

            String title = "*** PTT Messages ***";
            XmlPttmessages xmlmsg = new XmlPttmessages();
            Pttmessages pttmsg = xmlmsg.loadPttmessages(pttTraces);
            xmlmsg.getPttMessageData(pttmsg, keepHeader);

            List<MessageData> records = xmlmsg.getRecords();
	    PttMessageData.organizePttMessageData( records);
            String msgData = xmlmsg.toISSIString( title, pttmsg, keepHeader);
            //----------------------------

            logger.debug("renderSipPttTraces: rfssList=\n"+rfssList);
            if(!isInteractive) {
               numOfPttTraces = records.size();
               logger.debug("renderSipPttTraces: PTT Ref-records.size="+numOfPttTraces);
	    }

            // Create a new tabbed panel
            pttTracePanel = new TracePanel(gui, gui.getJTabbedPane(),
                  currentTestName, rfssList, records);

            pttTracePanel.setTitle(pttTab);
            pttTracePanel.setTestNumber(getCurrentTestCaseDescriptor().getTestNumber());

	    //NOTE: the msgData is unsorted
            //+++pttTracePanel.setDataTextArea(TracePanel.DATA_ALL_MSG, msgData);
	    msgData = pttTracePanel.getSortedMessageData();
            //logger.debug("renderSipPttTraces: PTT sorted-msgData="+msgData);
            pttTracePanel.setDataTextArea(TracePanel.DATA_ALL_MSG, msgData);

            // String sbufHex = HexDump.toHex( stringAsBytes, 16);
            // pttTracePanel.setDataTextArea( TracePanel.DATA_RAW_MSG, sbufHex);
            pttTracePanel.setDataTextArea(TracePanel.DATA_RAW_MSG, pttTraces);

            if (gui.getRemoteController() != null) {
               boolean tpass = gui.getRemoteController().getTestResults();
               String tlabel = gradeTestResults(isInteractive, numOfPttTraces, tpass,
                  pttTracePanel.getRfssList().size(),
		  pttTracePanel.getMessageList().size());
               pttTracePanel.setTestResultLabel( tlabel);
            }
            addTraceToTabbedPanel(gui.getJTabbedPane(), pttTab, pttTracePanel);

            gui.getCloseMenuItem().setEnabled(true);
            gui.getCloseAllMenuItem().setEnabled(true);

         } catch (Exception ex) {

            errFlag = true;
            String msg = "Error fetching PTT trace from PacketMonitor or Tester";
            setProgressScreenMessage(msg);
            msg += "\n" + ex.toString() + "\n";

	    // ArrayIndexOutOfBoundsException: 4
            logger.debug(msg);
            String trace = StackTraceUtility.stack2string(ex);
            logger.debug(trace);
            logError(msg, trace);
         }

      } catch (Throwable ex) {
         logger.debug("renderSipPttTraces: Throwable ex= " + ex);
         errFlag = true;

      } finally {

         logger.debug("renderSipPttTraces: finally saveFiles()... ");
         stopProgressMonitor();
         try {
            if (toSave) {
               showln("TestExecutionPanel: *** TestHelper.saveFiles(): ...");
               TesterHelper.saveFiles(false);
            }
         } catch (Exception ex) {
            logger.error("Error saving files", ex);
         }
         if( errFlag) {
            enableSetupButton();
         }
      }
   }

   private String gradeTestResults(boolean isInteractive, int refSize,
      boolean tpass, int rfssSize, int msgSize)
   {
      String tlabel = "No of RFSS: " +rfssSize +"    Total Messages: " +msgSize;
      String result = "    Test Result: ";
      String plabel = "";

      if( gui.getEvaluatePassFail()) {
         if( msgSize > 0) {
	   plabel += result + (tpass ? ISSITesterConstants.PASSED : ISSITesterConstants.FAILED);
         }
      }

      if(!isInteractive) {
         // reference traces
         //tlabel += plabel;
	 return tlabel;
      }

      // test traces
      // set allowable difference
      int delta = 1;
      if(msgSize > 10) {
	 delta =  msgSize / 10 + 1;
      }

      // append ref message size
      tlabel += "/" + refSize;

      if((msgSize == refSize) ||
         (msgSize >= (refSize - delta) && 
          msgSize <= (refSize + delta))) {
         tlabel += plabel;
	 return tlabel;
      }
      else {
         // test traces must be +/- delta
         if( gui.getEvaluatePassFail()) {
            if( tpass) {
               // test pass - check messages number
               tlabel += result + ISSITesterConstants.FAILED +
                  "   Unmatched message number";
            } else {
               // test fail - donot check messages number
               tlabel += result +(tpass ? ISSITesterConstants.PASSED : ISSITesterConstants.FAILED); 
            }
	 }
      }
      return tlabel;
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

   // --------------------------------------------------------------------------
   private TopologyConfig topologyConfig = null;

   private void clearTopologyConfig(String owner) {
      logger.debug("clearTopologyConfig: ===========OWNER="+owner);
      topologyConfig = null;
   } 
   // --------------------------------------------------------------------------
   // ConfigurationServlet calls this 
   public TopologyConfig getTopologyConfig() throws Exception {
      return getTopologyConfig( getCurrentTopologyName());
   }

   private TopologyConfig getTopologyConfig(String curTopologyName) throws Exception {
      return getTopologyConfig( true, curTopologyName);
   }
   private TopologyConfig getTopologyConfig(boolean isInteractive, String curTopologyName)
      throws Exception {
      if( topologyConfig == null) {
         topologyConfig = _getTopologyConfig(isInteractive, curTopologyName);
      }
      return topologyConfig; 
   }

   private TopologyConfig _getTopologyConfig(boolean isInteractive, String curTopologyName)
      throws Exception {

      String topologyFileName = ISSITesterConstants.getScenarioDir() + "/" + curTopologyName;
      showln("*** TestExecPanel: getTopologyConfig *** interactive="+isInteractive);
      /***
      //showln("  interactive: " + isInteractive);
      showln("  testerConfiguration: " + gui.getTesterConfigurationFile());
      showln("  systemTopologyName:  " + gui.getSystemTopologyFileName());
      showln("  globalTopologyName:  " + gui.getGlobalTopologyFileName());
      showln("  topologyFileName:    " + topologyFileName);
      //showln("  issiTesterConfig:    " + gui.getIssiTesterConfiguration().toString());
       ***/

      TopologyConfig systemTopology = new SystemTopologyParser(
            gui.getIssiTesterConfiguration()).parse(gui.getSystemTopologyFileName());

      GlobalTopologyParser globalTopologyParser = new GlobalTopologyParser(isInteractive);
      TopologyConfig globalTopology = globalTopologyParser.parse(
            systemTopology, gui.getGlobalTopologyFileName());

      TopologyConfigParser parser = new TopologyConfigParser();
      TopologyConfig testTopology = parser.parse(globalTopology, topologyFileName);

      return testTopology;
   }
         
   public String formatLocalTopology(boolean xmlform) throws Exception
   {
      String xmlStr = "";
      String currentName = getCurrentTopologyName();
      try {
         TopologyConfig testTopology = getTopologyConfig( currentName);
         if( xmlform ) {
            xmlStr = "<!-- Global Topology -->\n";
            xmlStr += testTopology.exportGlobalTopology();
            xmlStr += "\n\n<!-- System Topology -->\n";
            xmlStr += testTopology.exportSystemTopology();
            return xmlStr;
         } 
         else {
            return testTopology.getDescription();
         }
      } catch (SAXException ex) {
         String msg = "Could not parse topology config file: " + currentName +
            "\n" + ex.toString();
         logger.error(msg, ex);
         JOptionPane.showMessageDialog(null, msg,
               "Parse File Status",
               JOptionPane.ERROR_MESSAGE);

      } catch (Exception ex) {
         String msg = "Could not parse topology config file: " + currentName;
         logger.error(msg, ex);
         JOptionPane.showMessageDialog(null, msg,
               "Parse File Status",
               JOptionPane.ERROR_MESSAGE);
      }
      return xmlStr;
   }

   private void startTestExecutionTimerTask(int millisec, boolean interactive) {
      testExecutionTimerTask = new TestExecutionTimerTask(gui,millisec,interactive);
      getTimer().schedule(testExecutionTimerTask, 0, 1000);
   }

   public void shipConfigFilesList(List<String> filesList) throws Exception
   {
      testerService.shipConfigFilesList( filesList);
   }

   public void reconfigureDietsService() throws Exception {
      if( testerService != null)
         testerService.reconfigure();
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

	 // display the popup
         popupMenu.show(e.getComponent(), e.getX(), e.getY() - 10);
      }
   }

   public void autoSave() {
      ArrayList compList = new ArrayList();
      ComponentUtils.getAllComponents( tabbedPane, compList);
      for(int i=0; i < compList.size(); i++) {
         Object tpobj = compList.get(i);
	 if(tpobj instanceof TestLayoutPanel) {
            TestLayoutPanel tp = (TestLayoutPanel)tpobj;
            tp.autoSave();
         }
      }
      for(int i=0; i < compList.size(); i++) {
         Object tpobj = compList.get(i);
         if(tpobj instanceof TraceLogTextPane) {
            TraceLogTextPane tp = (TraceLogTextPane)tpobj;
            tp.autoSave();
         }
      }
   }

   // Auto print SIP/PTT traces
   public void autoPrint() {
      ArrayList compList = new ArrayList();
      ComponentUtils.getAllComponents( tabbedPane, compList);
      for(int i=0; i < compList.size(); i++) {
         Object tpobj = compList.get(i);
	 if(tpobj instanceof TestLayoutPanel) {
            TestLayoutPanel tp = (TestLayoutPanel)tpobj;
            PrintUtilities.printComponent(tp);
         }
      }
      for(int i=0; i < compList.size(); i++) {
         Object tpobj = compList.get(i);
         if(tpobj instanceof TraceLogTextPane) {
            TraceLogTextPane tp = (TraceLogTextPane)tpobj;
            PrintUtilities.printComponent(tp);
         }
      }
   }
}
