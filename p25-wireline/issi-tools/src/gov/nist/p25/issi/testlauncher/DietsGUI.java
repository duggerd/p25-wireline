//
package gov.nist.p25.issi.testlauncher;

import gov.nist.p25.common.swing.dialog.HTMLPaneDialog;
import gov.nist.p25.common.swing.util.ComponentUtils;
import gov.nist.p25.common.swing.util.PrintUtilities;
import gov.nist.p25.common.swing.util.LAFUtility;
import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.common.util.GetOpt;
import gov.nist.p25.common.util.IpAddressUtility;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.constants.ISSILogoConstants;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.GlobalTopologyParser;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfigurationParser;
import gov.nist.p25.issi.rfss.tester.RfssController;
import gov.nist.p25.issi.rfss.tester.TestRFSS;
import gov.nist.p25.issi.traceviewer.SipMessageFormatter;
import gov.nist.p25.issi.traceviewer.TracePanel;
//import gov.nist.p25.issi.traceverifier.PttTraceVerifier;
//import gov.nist.p25.issi.traceverifier.SipTraceVerifier;
import gov.nist.p25.issi.utils.Zipper;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

/**
 * This class implements the front end test controller and trace viewer GUI 
 * for ISSI Tester.
 *
 */
public class DietsGUI extends JFrame 
   implements ActionListener, MouseListener, TestStatusInterface
{
   private static final long serialVersionUID = -1L;

   static {
      PropertyConfigurator.configure("log4j.properties");
   }
   private static Logger logger = Logger.getLogger(DietsGUI.class);
   public static void showln(String s) { System.out.println(s); }

   private static final String VERSION = ISSILogoConstants.VERSION;
   private static final String BUILD = ISSILogoConstants.BUILD;
   private static int PREFERRED_WIDTH = 1250;
   private static int PREFERRED_HEIGHT = 850;   

   private static boolean isPrtMenu = true;
   private boolean evaluatePassFail = true;   
   private boolean standalone = false;
   private String consoleTitle;
   private String testerConfigurationFile;
   private String globalTopologyFileName;
   
   private String startupPropertiesFileName = ISSITesterConstants.STARTUP_PROPERTIES_FILENAME;
   private String systemTopologyFileName = ISSITesterConstants.getSystemTopologyFileName();

   private String testSuite = ISSITesterConstants.getTestSuite();   
   private ISSITesterConfiguration issiTesterConfiguration;
   private Collection<String> ipAddresses;
   private String traceSource;
   private Vector<TestCaseDescriptor> testcases;
   private TestControllerInterface remoteController;
   private TestExecutionPanel testExecutionPanel;

   private JCheckBoxMenuItem prompterCheckBox;  
   private JCheckBoxMenuItem showPttHeaderCheckBox;  
   private JCheckBoxMenuItem showAllPttCheckBox;  
   private JCheckBoxMenuItem evaluatePassFailCheckBox;  
   private JCheckBoxMenuItem issiMessageFormatCheckBox;  
   private JFileChooser fileChooser;
   private JLabel statusLabel;   
   private JMenuItem clearOutputDirMenuItem;  
   private JMenuItem clearRefMsgMenuItem;  
   private JMenuItem closeMenuItem;
   private JMenuItem closeAllMenuItem;
   private JMenuItem exitMenuItem;
   private JMenuItem openMenuItem;
   private JMenuItem documentationMenuItem;
   private JMenuItem aboutMenuItem;
   private JMenuItem saveMenuItem;   
   private JMenuItem createTestrunZip;
   //private JMenuItem verifyTraceMenuItem;
   private JMenuItem systemTopologyMenuItem;
   private JMenuItem startupPropertiesMenuItem;
   private JMenuItem loadPropertiesMenuItem;
   private JProgressBar statusProgressBar;
   private JTabbedPane tabbedPane;
   
   private ButtonGroup bgroup;
   private Object rbCurrentTest;
   private JRadioButtonMenuItem rbConformance;
   private JRadioButtonMenuItem rbCAP;
   private JMenuItem udpSetupItem;

   // accessors
   public String getConsoleTitle() {
      return consoleTitle;
   }

   public boolean getEvaluatePassFail() {
      return evaluatePassFail;
   }
   public void setEvaluatePassFail(boolean bflag) {
      evaluatePassFail = bflag;
   }

   public String getSystemTopologyFileName() {
      return systemTopologyFileName;
   }
   public String getGlobalTopologyFileName() {
      return globalTopologyFileName;
   }
   public String getTesterConfigurationFile() {
      return testerConfigurationFile;
   }

   public String getStartupPropertiesFileName() {
      return startupPropertiesFileName;
   }
   public void setStartupPropertiesFileName(String fileName) {
      this.startupPropertiesFileName = fileName;
   }

   public void resetRemoteController() {
      remoteController.reset();
   }
   public TestControllerInterface getRemoteController() {
      return remoteController;
   }

   public TestExecutionPanel getTestExecutionPanel() {
      return testExecutionPanel;
   }

   public String getTraceSource() {
      return traceSource;
   }
   public void setTraceSource(String traceSource) {
      this.traceSource = traceSource;
   }

   public void doSetupTest() {
      getTestExecutionPanel().doSetupTest();
   }

   // GUI components
   public JLabel getStatusLabel() {
      return statusLabel;
   }
   public void setStatusLabel(JLabel statusLabel) {
      this.statusLabel = statusLabel;
   }

   public JProgressBar getStatusProgressBar() {
      return statusProgressBar;
   }
   public void setStatusProgressBar(JProgressBar statusProgressBar) {
      this.statusProgressBar = statusProgressBar;
   }

   public JMenuItem getCloseMenuItem() {
      return closeMenuItem;
   }

   public JMenuItem getCloseAllMenuItem() {
      return closeAllMenuItem;
   }

   public JTabbedPane getJTabbedPane() {
      return tabbedPane;
   }

   public boolean getScenarioPrompterState() {
      return prompterCheckBox.getState();
   }
   public boolean getShowPttHeaderState() {
      return showPttHeaderCheckBox.getState();
   }
   public boolean getShowAllPttState() {
      return showAllPttCheckBox.getState();
   }

   public boolean getEvaluatePassFailState() {
      return evaluatePassFailCheckBox.getState();
   }
   public boolean getIssiMessageFormatState() {
      return issiMessageFormatCheckBox.getState();
   }

   // constructors
   //-----------------------------------------------------------------------------
   public DietsGUI(String propFile) throws Exception {
      this( propFile, FileUtility.loadProperties(propFile));
   }

   public DietsGUI(Properties props) throws Exception {
      this(ISSITesterConstants.STARTUP_PROPERTIES_FILENAME, props);
   }

   public DietsGUI(String propFile, Properties props) throws Exception {
      setStartupPropertiesFileName(propFile);
      doStaticReInitialization(props);
      if (!standalone) {
         Logger logger = Logger.getLogger("gov.nist.p25.issi.testlauncher");
         logger.addAppender(new FileAppender(new SimpleLayout(), "logs/guidebuglog.txt"));
      }
      initialize();
   }

   public DietsGUI() throws Exception {
      super();
      initialize();
   }

   public void initialize() throws Exception {
      initGUI();
      logger.debug("initialize: ...standalone="+standalone);
      if (standalone) {
         remoteController = new RfssController(ipAddresses);
      } else {
         remoteController = new RemoteTestController(this);
      }
   }

   public void initGUI() throws Exception {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

      // File chooser used to open the trace file.
      fileChooser = new JFileChooser(System.getProperty("user.dir"));
      fileChooser.setDialogTitle("Open trace directory in /testrun");
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      FileFilter traceFilter = new FileFilter() {
         public boolean accept(File pathname) {
            if (pathname.isDirectory())
               return true;
            else
               return false;
         }
         public String getDescription() {
            return "tracedir";
         }
      };
      fileChooser.setFileFilter(traceFilter);

      JMenuBar menuBar = new JMenuBar();

      // ---------------------- File ----------------------------
      JMenu fileMenu = new JMenu("File");

      openMenuItem = new JMenuItem("Open trace directory");
      openMenuItem.addActionListener(this);
      openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
            ActionEvent.CTRL_MASK));

      loadPropertiesMenuItem = new JMenuItem("Load startup properties");
      loadPropertiesMenuItem.addActionListener(this);
      loadPropertiesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
            ActionEvent.CTRL_MASK));

      saveMenuItem = new JMenuItem("Save current test trace");
      saveMenuItem.addActionListener(this);
      saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
            ActionEvent.CTRL_MASK));
      
      createTestrunZip = new JMenuItem("Create testrun zip");
      createTestrunZip.addActionListener(this);
      createTestrunZip.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
            ActionEvent.CTRL_MASK));

/***** DISABLED
      verifyTraceMenuItem = new JMenuItem("Verify saved trace");
      verifyTraceMenuItem.addActionListener(this);
      verifyTraceMenuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_V, ActionEvent.CTRL_MASK));
 ***/

      closeMenuItem = new JMenuItem("Close");
      closeMenuItem.setEnabled(false);
      closeMenuItem.addActionListener(this);
      closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
            ActionEvent.CTRL_MASK));

      closeAllMenuItem = new JMenuItem("Close All");
      closeAllMenuItem.setEnabled(false);
      closeAllMenuItem.addActionListener(this);
      closeAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
            ActionEvent.CTRL_MASK));

      exitMenuItem = new JMenuItem("Exit");
      exitMenuItem.addActionListener(this);
      exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
            ActionEvent.CTRL_MASK));

      fileMenu.add(loadPropertiesMenuItem);
      fileMenu.addSeparator();
      fileMenu.add(openMenuItem);
      fileMenu.addSeparator();
      fileMenu.add(saveMenuItem);
      // Disabled for now
      //fileMenu.addSeparator();
      //fileMenu.add(verifyTraceMenuItem);
      fileMenu.addSeparator();
      fileMenu.add(createTestrunZip);
      fileMenu.addSeparator();

      // ---------------------- Options ----------------------------
      JMenu preferencesMenu = new JMenu("Edit");
      preferencesMenu.setEnabled(true);
      startupPropertiesMenuItem = new JMenuItem("Startup Properties");
      startupPropertiesMenuItem.addActionListener(this);
      
      systemTopologyMenuItem = new JMenuItem("System Topology");
      systemTopologyMenuItem.addActionListener(this);

      preferencesMenu.add(startupPropertiesMenuItem);
      preferencesMenu.add(systemTopologyMenuItem);
      
      fileMenu.add(preferencesMenu);
      fileMenu.addSeparator();
      fileMenu.add(exitMenuItem);
      
      // ---------------------- Test --------------------------
      JMenu testMenu = new JMenu("Test");
      bgroup = new ButtonGroup();
      rbConformance = new JRadioButtonMenuItem(ISSITesterConstants.TEST_CLASS_CONFORMANCE);
      rbConformance.setSelected(true);
      rbConformance.addActionListener(this);
      bgroup.add(rbConformance);
      testMenu.add(rbConformance);
      rbCurrentTest = rbConformance;

      rbCAP = new JRadioButtonMenuItem(ISSITesterConstants.TEST_CLASS_UDP);
      rbCAP.addActionListener(this);
      bgroup.add(rbCAP);
      testMenu.add(rbCAP);

      testMenu.addSeparator();
      udpSetupItem = new JMenuItem("User Defined Profile Setup");
      udpSetupItem.addActionListener(this);
      testMenu.add(udpSetupItem);

      // ---------------------- Config --------------------------
      JMenu configMenu = new JMenu("Config");
      prompterCheckBox = new JCheckBoxMenuItem("Scenario Prompter");
      prompterCheckBox.addActionListener(this);
      configMenu.add( prompterCheckBox);
      configMenu.addSeparator();

      showPttHeaderCheckBox = new JCheckBoxMenuItem("Show PTT Header");
      showPttHeaderCheckBox.addActionListener(this);
      configMenu.add( showPttHeaderCheckBox);

      //showAllPttCheckBox = new JCheckBoxMenuItem("Show All PTT");
      //showAllPttCheckBox.addActionListener(this);
      //configMenu.add( showAllPttCheckBox);
      configMenu.addSeparator();

      evaluatePassFailCheckBox = new JCheckBoxMenuItem("Evaluate Pass/Fail");
      evaluatePassFailCheckBox.setSelected( getEvaluatePassFail());
      evaluatePassFailCheckBox.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            setEvaluatePassFail(getEvaluatePassFailState());
         }
      });
      configMenu.add( evaluatePassFailCheckBox);
      configMenu.addSeparator();

      issiMessageFormatCheckBox = new JCheckBoxMenuItem("ISSI Message Format");
      issiMessageFormatCheckBox.setSelected( SipMessageFormatter.getIssiMessageFormat());
      issiMessageFormatCheckBox.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            SipMessageFormatter.setIssiMessageFormat(
		    getIssiMessageFormatState());
         }
      });
      configMenu.add( issiMessageFormatCheckBox);
      configMenu.addSeparator();



      clearRefMsgMenuItem = new JMenuItem("Clear Current Ref Trace");
      clearRefMsgMenuItem.addActionListener(this);
      configMenu.add( clearRefMsgMenuItem);
      
      clearOutputDirMenuItem = new JMenuItem("Clear Current Output Dir");
      clearOutputDirMenuItem.addActionListener(this);
      configMenu.add( clearOutputDirMenuItem);

      // ---------------------- LAF ----------------------------
      JMenu lafMenu = new JMenu("LAF");

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
               try {
                  UIManager.setLookAndFeel(className);
                  SwingUtilities.updateComponentTreeUI(DietsGUI.this);
               } catch (Exception ex) {
                  // ignore
               }
            }
         });
      }

      // ---------------------- Help ----------------------------
      JMenu helpMenu = new JMenu("Help");
      helpMenu.setEnabled(true);

      documentationMenuItem = new JMenuItem("Documentation");
      documentationMenuItem.setActionCommand("Documentation");
      documentationMenuItem.addActionListener(this);

      aboutMenuItem = new JMenuItem("About ISSI Tester");
      aboutMenuItem.setActionCommand("About");
      aboutMenuItem.addActionListener(this);

      helpMenu.add(documentationMenuItem);
      helpMenu.add(aboutMenuItem);

      menuBar.add(fileMenu);
      menuBar.add(testMenu);
      menuBar.add(configMenu);
      menuBar.add(lafMenu);
      menuBar.add(helpMenu);
      setJMenuBar(menuBar);

      showln("DietsGUI: startupFileName="+startupPropertiesFileName);
      testExecutionPanel = new TestExecutionPanel(testcases, this);
      updateGlobalTopologyPane();
      updateSystemTopologyPane();

      testExecutionPanel.setMinimumSize(new Dimension( DietsGUI.PREFERRED_WIDTH, 200));
      testExecutionPanel.setPreferredSize(new Dimension( DietsGUI.PREFERRED_WIDTH, 225));
      testExecutionPanel.setMaximumSize(new Dimension( DietsGUI.PREFERRED_WIDTH, 325));

      JPanel tabbedPanel = new JPanel();
      tabbedPanel.setLayout(new BorderLayout());
      tabbedPanel.setBorder(new TitledBorder("Test Traces"));

      tabbedPane = new JTabbedPane();
      tabbedPanel.add(tabbedPane, BorderLayout.CENTER);
      tabbedPane.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            tabbedPaneClicked(e);
         }
      });

      setIconImage(new ImageIcon(ISSILogoConstants.ISSI_TESTER_LOGO).getImage());
      String host = "";
      try {
         host = IpAddressUtility.getLocalHostAddress().getHostAddress();
      }
      catch(Exception ex) { }

      consoleTitle = "ISSI Tester v" +VERSION +"-" +BUILD +"   Host: "+host +"   "+testSuite;
      setTitle( consoleTitle);

      getContentPane().setLayout(new BorderLayout());
      getContentPane().add(testExecutionPanel, BorderLayout.NORTH);

      JPanel statusPanel = new JPanel();
      statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
      statusPanel.setPreferredSize(new Dimension(PREFERRED_WIDTH, 22));
      
      setStatusLabel(new JLabel("Ready"));
      getStatusLabel().setMinimumSize(new Dimension(300, 16));
      getStatusLabel().setPreferredSize(new Dimension(700, 16));
      getStatusLabel().setMaximumSize(new Dimension(900, 16));
      
      setStatusProgressBar(new JProgressBar());
      getStatusProgressBar().setMinimumSize(new Dimension(100, 16));
      getStatusProgressBar().setPreferredSize(new Dimension(150, 16));
      getStatusProgressBar().setMaximumSize(new Dimension(200, 16));
      getStatusProgressBar().setIndeterminate(false);

      statusPanel.add(getStatusLabel());
      statusPanel.add(getStatusProgressBar());

      JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            testExecutionPanel, tabbedPanel);
      splitPane.setOneTouchExpandable(true);
      splitPane.setDividerLocation(0.4);
      splitPane.setDividerSize(7);
      
      getContentPane().add(splitPane, BorderLayout.CENTER);
      getContentPane().add(statusPanel, BorderLayout.SOUTH);

      setSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
      Point upperLeftPoint = centerComponent(this);
      setLocation(upperLeftPoint);
      setAlwaysOnTop(true);

      ISSITimer.getTimer().schedule(new TimerTask() {
         @Override
         public void run() {
            setAlwaysOnTop(false);
         }
      }, 1000);

      setVisible(true);
      Dimension z = getStatusProgressBar().getSize();
      double height = z.getHeight();
      double width = z.getWidth();
      //logger.info("height: " + height + ", width: " + width);

      addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            DietsGUI.this.closeShop();
         }
      });
   }

   public void updateGlobalTopologyPane() throws IOException {
      // Disabled = not used
      //String ftext = FileUtility.loadFromFileAsString(globalTopologyFileName);
      //testExecutionPanel.logGlobalTopologyPane(true, ftext);
   }

   public void updateSystemTopologyPane() throws IOException {
      // Disabled = not used
      //String ftext = FileUtility.loadFromFileAsString(systemTopologyFileName); 
      //testExecutionPanel.logSystemTopologyPane(true, ftext);
   }

   private Point centerComponent(Component c) {
      Rectangle rc = new Rectangle();
      rc = c.getBounds(rc);
      Rectangle rs = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
      return new Point((int) ((rs.getWidth()/2) - (rc.getWidth()/2)),
            (int) ((rs.getHeight()/2) - (rc.getHeight()/2)));
   }

   public Vector<TestCaseDescriptor> getAvailableTestCases(String testClass)
         throws Exception {
      String testRegistry = ISSITesterConstants.getTestRegistry();
      TestRegistryParser parser = new TestRegistryParser(testRegistry,testClass);
      parser.parse();
      return parser.getTestCaseList();
   }

   private void closeShop() {
      getTestExecutionPanel().closeShop();
      dispose();
   }

   public void updateTestCases(String testClass) {
      try {
         testcases = getAvailableTestCases(testClass);
         getTestExecutionPanel().updateTestCases(testClass,testcases);

      } catch (Exception e) {
         logger.error("Error in getting test cases", e);
         JOptionPane.showMessageDialog(null,
            "Error in getting test cases for "+testClass+" Test",
            "ISSI Tester startup/update error",
            JOptionPane.ERROR_MESSAGE);
      }
   }

   public void doClickTestClass(String testClass) {
      logger.debug("DietsGUI: doClickTestClass: "+testClass);
      if( ISSITesterConstants.TEST_CLASS_CONFORMANCE.equals(testClass)) {
         rbConformance.doClick(); 
      }
      else if( ISSITesterConstants.TEST_CLASS_UDP.equals(testClass)) {
         rbCAP.doClick(); 
      }
   }
   public String getTestClass() {
      for (Enumeration e = bgroup.getElements(); e.hasMoreElements(); ) {
         JRadioButtonMenuItem b = (JRadioButtonMenuItem)e.nextElement();
         if (b.getModel() == bgroup.getSelection()) {
            return b.getText();
         }
      }
      return ISSITesterConstants.TEST_CLASS_CONFORMANCE;
   }

   // implementation of ActionListener
   //---------------------------------------------------------------------------
   public void actionPerformed(ActionEvent ae) {

      Object obj = ae.getSource();
      if (obj == rbConformance) {
         if( rbCurrentTest != obj) {
            updateTestCases( ISSITesterConstants.TEST_CLASS_CONFORMANCE);
            rbCurrentTest = obj;
         }
      } else if (obj == rbCAP) {
         if( rbCurrentTest != obj) {
            updateTestCases( ISSITesterConstants.TEST_CLASS_UDP);
            rbCurrentTest = obj;
         }
      } else if (obj == udpSetupItem) {
         String xmlFile = ISSITesterConstants.getTestRegistry();
         String cbtype = ISSITesterConstants.TEST_CLASS_UDP;
         String title = cbtype + " Setup";
         //showln( title +" : " + xmlFile);

         TestRegistrySetupDialog dialog = 
            new TestRegistrySetupDialog(xmlFile, title, cbtype);
         if( dialog.isSaved()) {
            // propagate testregistry.xml
            try {
               List<String> filesList = new ArrayList<String>();
               filesList.add( xmlFile);
	       showln("update config: shipConfigFilesList(): "+filesList);
               getTestExecutionPanel().shipConfigFilesList(filesList);

               rbCurrentTest = null;
               doClickTestClass(cbtype);
            } catch(Throwable ex) {
               logger.error("Failed to update: "+xmlFile, ex);
               showError("Failed to update: "+xmlFile+" due to " +ex.getMessage(),
                  "Update config error");
            }
         }

      } else if (obj == exitMenuItem) {
         getTestExecutionPanel().closeShop();
         closeShop();
      } else if (obj == openMenuItem) {
         try {
            openFile();
         } catch (Exception ex) {
            logger.error("Error in open file: ", ex);
            showError("Error in opening trace directory: "+ex.getMessage(),
                  "Load trace error");
         }
      } else if (obj == loadPropertiesMenuItem) {
         try {
            loadProperties();
         } catch (Exception ex) {
            logger.error("Error in loading properties:", ex);
            showError("Error in loading proeprties: " + ex.getMessage(),
                  "Load properties error");
         }
      } else if (obj == saveMenuItem) {
         try {
            saveTestFiles(true);
         } catch (Exception ex) {
            logger.error("Error in saving traces:", ex);
            showError("Error in saving traces: " + ex.getMessage(),         
               "Save trace error");
         }

      } else if ( obj == createTestrunZip) {
         try {
            zipFiles();
         } catch (Exception ex) {
            logger.error("Error in zipping testrun files:", ex);
            showError("Error in zipping testrun: " + ex.getMessage(),
               "Zip Error");
         }
/******* DISABLED
      } else if (obj == verifyTraceMenuItem) {
         try {
            TestCaseDescriptor descriptor = testExecutionPanel.getCurrentTestCaseDescriptor();
            String topologyFileName = descriptor.getTopologyFileName();
            ISSITesterConfiguration config = new ISSITesterConfigurationParser(
                  testerConfigurationFile).parse();

            String testName = descriptor.getDirectoryName();
            SipTraceVerifier.testCapturedMessages(config, "./",
                  testSuite, testName, topologyFileName);

            PttTraceVerifier.testCapturedMessages(config, "./",
                  testSuite, testName, topologyFileName);
            
            JOptionPane.showMessageDialog( null,          
               "Completed verifying test trace:\n\n" + descriptor + "\n\n\n");
            
         } catch (Exception ex) {
            logger.error("Error in verifying test trace: ", ex);
            showError("Error in verifying test trace: " + ex.getMessage(),
               "Verification Error");
         }
 *****/

      } else if (obj == closeMenuItem) {

         int showing = tabbedPane.getSelectedIndex();
         tabbedPane.remove(showing);
         if (tabbedPane.getComponentCount() == 0) {
            getCloseMenuItem().setEnabled(false);
            getCloseAllMenuItem().setEnabled(false);
         }

      } else if (obj == clearRefMsgMenuItem) {

         try {
            TestCaseDescriptor descriptor = testExecutionPanel.getCurrentTestCaseDescriptor();
            String topologyFileName = descriptor.getTopologyFileName();
            String refDirName = ISSITesterConstants.getTraceDirectory(testSuite,
                  topologyFileName.substring(0, topologyFileName.indexOf("/")));

            showln("Clear Ref Msg: topologyFileName="+topologyFileName);
            showln("Clear Ref Msg: refDirName="+refDirName);
            FileUtility.deleteDir( new File(refDirName), true);

         } catch(Exception ex) {
            // ignore
         }

      } else if (obj == clearOutputDirMenuItem) {

         try {
            TestCaseDescriptor descriptor = testExecutionPanel.getCurrentTestCaseDescriptor();
            String topologyFileName = descriptor.getTopologyFileName();
            String refDirName = ISSITesterConstants.getTraceDirectory(testSuite,
                  topologyFileName.substring(0, topologyFileName.indexOf("/")));

            String outDir = "output" + File.separator + descriptor.getTestNumber();
            showln("Clear Output directory: "+outDir);
            FileUtility.deleteDir( new File(outDir), true);

         } catch(Exception ex) {
            // ignore
         }

      //---------------------------------------------------------------------------------
      } else if (obj == documentationMenuItem) {

         String htmlFile = "doc/documentation.html";
         try {
            URL url = new File(htmlFile).toURI().toURL();
            HTMLPaneDialog htmlDialog = new HTMLPaneDialog(this, 
               "ISSI Tester Tools", url);
            htmlDialog.setSize(640, 540);
            htmlDialog.setLocationRelativeTo(this);
            htmlDialog.setVisible(true);
         } 
         catch(Throwable ex) {
            JOptionPane.showMessageDialog( null,
               "Failed to load html due to:\n"+ ex.getMessage(),
               "Message", 
               JOptionPane.INFORMATION_MESSAGE);
         }

      } else if (obj == aboutMenuItem) {

         AboutDialog about_dialog = new AboutDialog(this, VERSION, BUILD);
         about_dialog.setSize(600, 500);
         about_dialog.setLocationRelativeTo(this);
         about_dialog.setVisible(true);

      } else if (obj == systemTopologyMenuItem) {

         String topologyFileName = getSystemTopologyFileName();
         try {
            testExecutionPanel.disableAllButtons();
            File file = new File(topologyFileName);
            ConfigDialog customDialog = new ConfigDialog(file,
                  "System Topology: " + topologyFileName);            
            customDialog.setLocationRelativeTo(this);
            customDialog.setVisible(true);

            if (customDialog.isSaved()) {               
               String ftext = FileUtility.loadFromFileAsString(topologyFileName);
               testExecutionPanel.logTestConfigPane(true,ftext);
            }
            updateSystemTopologyPane();

         } catch (Exception ex) {
            ex.printStackTrace();
            showWarning("Make sure the file is writable: "+topologyFileName,
                  "Edit file error");
         } finally {
            testExecutionPanel.enableAllButtons();
         }

      } else if (obj == startupPropertiesMenuItem) {

         String fileName = getStartupPropertiesFileName();
         char[] rbuf = null;
         try {
            FileReader reader = new FileReader( fileName);
            File topologyFile = new File( fileName);
            int length = (int) topologyFile.length();
            rbuf = new char[length];
            reader.read(rbuf);
            reader.close();
         } catch (Exception ex) {
            showError("Could not read startup props file: "+fileName,
               "File read error");
            return;
         }

         try {
            testExecutionPanel.disableAllButtons();

            File file = new File(fileName);
            ConfigDialog customDialog = new ConfigDialog(file,
                  "Startup Properties: " + fileName);
            customDialog.setLocationRelativeTo(this);
            customDialog.setVisible(true);

            if (customDialog.isSaved()) {
               dispose();

               showln("startupProperties(): fileName="+fileName);
               javax.swing.SwingUtilities.invokeLater(new Runnable() {
                  String propFileName;
		  public Runnable setPropFileName(String propFileName) {
                     this.propFileName = propFileName;
                     return this;
                  }
                  public void run() {
                     try {
                        DietsGUI gui = new DietsGUI(propFileName);
			gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                     } catch (Exception ex) {
                        logger.error("Error in starting Tester GUI", ex);
                     }
                  }
               }.setPropFileName(fileName));
            }

         } catch (Exception ex) {

            try {
               FileWriter fwriter = new FileWriter(new File( fileName));
               fwriter.write(rbuf);
               fwriter.close();

               doStaticReInitialization(fileName);
               testExecutionPanel.enableAllButtons();

            } catch (Exception ex1) {
               ex1.printStackTrace();
               showError("Could not restore to old state: "+fileName,
                  "Fatal error");
               System.exit(0);
            }

         } finally {
             // nothing todo ?
         }
      }   
   }

   /**
    * zip the current traces into a directory for shipment to 
    * the ISSI remote verifier.
    */
   public void zipFiles() throws Exception {
      // These are the files to include in the ZIP file
      String[] filenames = new String[] { 
            "testrun",
            testerConfigurationFile , 
            startupPropertiesFileName
      };
      //logger.debug("zipFiles(): filenames="+filenames);
      Zipper.zipAll(filenames,"testrun.zip");
         
      // NOT SUPPORTED:
      // Visit http://antd.nist.gov/submit to verify.
      String msg = "Completed creating test run zip file.\n\nOutput file: testrun.zip.\n\n";
      JOptionPane.showMessageDialog(null, msg);
   }
   
   private void loadProperties() {

      String suggestDir = System.getProperty("user.dir") + File.separator + "startup";
      JFileChooser fileChooser = new JFileChooser(suggestDir);
      fileChooser.setDialogTitle("Load startup properties");
      fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      FileFilter traceFilter = new FileFilter() {
         public boolean accept(File pathname) {
            if (pathname.getName().endsWith(".properties")
                  || pathname.isDirectory())
               return true;
            else
               return false;
         }
         public String getDescription() {
            return "Startup Properties";
         }
      };
      fileChooser.setFileFilter(traceFilter);

      int returnValue = fileChooser.showOpenDialog(this);
      String savedName = getStartupPropertiesFileName();
      try {
         if (returnValue == JFileChooser.APPROVE_OPTION) {
            String selFileName = fileChooser.getSelectedFile().getAbsolutePath();
            dispose();

            showln("loadProperties(): selFileNAme="+selFileName);
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
               String propFileName;
               public Runnable setPropFileName(String propFileName) {
                  this.propFileName = propFileName;
                  return this;
               }
               public void run() {
                  try {
                     DietsGUI gui = new DietsGUI(propFileName);
                     gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                  } catch (Exception ex) {
                     logger.error("Eroor in starting Tester GUI", ex);
                  }
               }
            }.setPropFileName(selFileName));
         }
      } catch (Exception ex) {

         showError("Error loading startup properties: "+ ex.getMessage(),
            "Reconfiguration Error");
         setStartupPropertiesFileName( savedName);
         testExecutionPanel.enableAllButtons();
      }
   }

   private void renderTraces(String directoryName, boolean toSave)
         throws Exception {
      
      logger.debug(">>>>>>>>>>>> renderTraces >>>>>>>>>>>>");
      logger.debug("renderTraces: directoryName="+directoryName);
      showln("renderTraces: directoryName="+directoryName);
      File directoryFile = new File(directoryName);
      String dirpath = directoryFile.getAbsolutePath();
      logger.debug("renderTraces: dirpath="+dirpath);


      // switch to test-specific configuration files
      //+++testerConfigurationFile = dirpath +File.separator +"tester-configuration.xml";
      globalTopologyFileName = dirpath +File.separator +ISSITesterConstants.GLOBAL_TOPOLOGY_XML;
      systemTopologyFileName = dirpath +File.separator +ISSITesterConstants.SYSTEM_TOPOLOGY_XML;

      //showln("renderTraces: tester="+getTesterConfigurationFile());
      //showln("renderTraces: global="+getGlobalTopologyFileName());
      //showln("renderTraces: system="+getSystemTopologyFileName());

      logger.debug("renderTraces: tester="+getTesterConfigurationFile());
      logger.debug("renderTraces: global="+getGlobalTopologyFileName());
      logger.debug("renderTraces: system="+getSystemTopologyFileName());

      if (!new File(globalTopologyFileName).exists()) {
         String msg = "renderTraces: Cannot find " + systemTopologyFileName;
         logger.error( msg);
         throw new FileNotFoundException(msg);
      }

      if (!new File(systemTopologyFileName).exists()) {
         String msg = "renderTraces: Cannot find " + systemTopologyFileName;
         logger.error( msg);
         throw new FileNotFoundException(msg);        
      }
      
      String testCaseXmlName = ISSITesterConstants.getCurrentTestCaseFileName( dirpath);
      if (!new File( testCaseXmlName).exists()) {
         String msg = "renderTraces: Cannot find " + testCaseXmlName;
         logger.error( msg);
         throw new FileNotFoundException(msg); 
      }

      updateGlobalTopologyPane();
      updateSystemTopologyPane();
      TestRunInfo testRunInfo = new CurrentTestCaseParser()
            .parse(new File (testCaseXmlName).toURI().toURL().toString());
      String currentTestDir = testRunInfo.getDirectory();
      String currentTopology = testRunInfo.getTopology();

      testExecutionPanel.incrementRunTestNumber();
      testExecutionPanel.setCurrentTest(currentTestDir, currentTopology);

      // Add XML begin and end tags to the selected file since these
      // are not generated by the ISSI emulator
      StringBuffer sbuf = new StringBuffer("<sipmessages>\n");
      String c = null;
      for (File file : directoryFile.listFiles(new FilenameFilter() {
         public boolean accept(File dir, String name) {
            return name.endsWith("sip");
         }
      })) {
         BufferedReader fileReader = new BufferedReader(new FileReader(file));
         while ((c = fileReader.readLine()) != null) {
            sbuf.append(c);
	    sbuf.append("\n");
	 }
         fileReader.close();
      }
      sbuf.append("</sipmessages>\n");

      String pttMsgFile = directoryName + "/messagelog.ptt";
      File pttTrace = new File( pttMsgFile);
      boolean isInteractive = testRunInfo.isInteractive();
      if (pttTrace.exists()) {
         StringBuffer tracebuf = new StringBuffer("<pttmessages>\n");
         String ftext = FileUtility.loadFromFileAsString(pttMsgFile);
         tracebuf.append( ftext);
         tracebuf.append("</pttmessages>\n");

         testExecutionPanel.renderSipPttTraces(sbuf.toString(),
               tracebuf.toString(), null, isInteractive, toSave);
      } else {
         testExecutionPanel.renderSipPttTraces(sbuf.toString(), 
               null, null, isInteractive, toSave);
      }
      getCloseMenuItem().setEnabled(true);
      getCloseAllMenuItem().setEnabled(true);
   }

   /**
    * Open and render the current saved log information in the ref traces
    * directory.
    */
   public void renderRefTraceLogs() {
      //String testName = testExecutionPanel.getCurrentTest().getDirectoryName();
      
      //showln("renderRefTraceLogs(1): "+ testExecutionPanel.getCurrentTopologyName());
      String topology = testExecutionPanel.getCurrentTopologyName().substring( 0,
               testExecutionPanel.getCurrentTopologyName() .indexOf(".xml"));
      logger.debug("renderRefTraceLogs(2): topology="+ topology);

      String dir = ISSITesterConstants.getTraceDirectory( testSuite, topology+".logs");
      logger.debug("renderRefTraceLogs(3): dir=" + dir);
      
      if (!new File(dir).exists()) {
         logger.debug("renderRefTraceLogs(): no trace dir found " + dir);
         tabbedPane.removeAll();
         return;
      } 
      try {
         //Could not find an RFSS (fromRfss) 01.002.00002.p25dr:25002
         renderTraces( dir, false);
      } 
      catch (Exception ex) {
         ex.printStackTrace();
         logger.debug("Error rendering reference trace -- need to generate one.",ex);
      }
   }

   /**
    * Open a previously stored trace. The trace directory contains
    * <ul>
    * <li> the current test name in a file called current-test-case.xml
    * <li> the global topology used to run the test in a file called
    * globaltopology.xml
    * <li> the sip logs.
    * <li> the ptt logs.
    * </ul>
    * 
    * @throws Exception --
    *             if problems were encountered in opening the log.
    */
   public void openFile() throws Exception {
      int rc = fileChooser.showOpenDialog(this);
      if (rc == JFileChooser.APPROVE_OPTION) {
         String dirName = fileChooser.getSelectedFile().getAbsolutePath();
         renderTraces(dirName, false);
      }
   }

   public void showWarning(String text, String title) {
      JOptionPane.showMessageDialog(this, text, title, JOptionPane.WARNING_MESSAGE);
   }

   public void showError(String text, String title) {
      JOptionPane.showMessageDialog(this, text, title, JOptionPane.ERROR_MESSAGE);
   }

   // implemenation of MouseListener
   //------------------------------------------------------------------------------
   public void mouseClicked(MouseEvent e) { }
   public void mousePressed(MouseEvent e) { }
   public void mouseReleased(MouseEvent e) { }

   public void mouseEntered(MouseEvent e) {
      JButton button = (JButton) e.getSource();
      button.setBorderPainted(true);
   }

   public void mouseExited(MouseEvent e) {
      JButton button = (JButton) e.getSource();
      button.setBorderPainted(false);
   }

   //----------------------------------------------------------------------------
   public void updateGlobalTopologyValues() {
      globalTopologyFileName = ISSITesterConstants.getGlobalTopologyFileName();
      systemTopologyFileName = ISSITesterConstants.getSystemTopologyFileName();
      logger.debug("updateGlobalTopologyValues: global="+globalTopologyFileName);
      logger.debug("updateGlobalTopologyValues: system="+systemTopologyFileName);
   }

   //----------------------------------------------------------------------------
   public void doStaticReInitialization(String fileName) throws Exception {
      setStartupPropertiesFileName(fileName);
      Properties props = FileUtility.loadProperties( fileName);
      doStaticReInitialization(props);
   }

   public void doStaticReInitialization(Properties props) throws Exception {

      testerConfigurationFile = props.getProperty(DietsConfigProperties.DAEMON_CONFIG_PROPERTY);
      if (testerConfigurationFile == null) {
         JOptionPane.showMessageDialog( null,
            "Missing Property! tester configuration file not specified",
            "ISSI Tester startup error",
            JOptionPane.ERROR_MESSAGE);
         throw new Exception("Missing a required Parameter "
            + DietsConfigProperties.DAEMON_CONFIG_PROPERTY);
      }

      testSuite = props.getProperty(DietsConfigProperties.TESTSUITE_PROPERTY, "conformance");
      ISSITesterConstants.setTestSuite(testSuite);
      logger.debug("doStaticReInitialization: setTestSuite="+testSuite);

      systemTopologyFileName = props.getProperty(DietsConfigProperties.SYSTEM_TOPOLOGY_PROPERTY);
      if (systemTopologyFileName == null) {
         systemTopologyFileName = ISSITesterConstants.getSystemTopologyFileName(testSuite);
      }
      showln("doStaticInitialization: systemTopologyFileName="+systemTopologyFileName);

      standalone = "true".equals( props.getProperty(DietsConfigProperties.STANDALONE_PROPERTY));
      logger.debug("doStaticInitialization: standalone="+standalone);

      // allow GUI to set this flag
      //String param = props.getProperty("diets.evaluatePassFail");
      //evaluatePassFail = (param == null ? false : "true".equals(param));
      logger.debug("doStaticInitialization: diets.evaluatePassFail="+getEvaluatePassFail());
      
      String widthStr = props.getProperty(DietsConfigProperties.DIETSGUI_WIDTH);
      DietsGUI.PREFERRED_WIDTH = widthStr != null ? Integer.parseInt(widthStr)
            : DietsGUI.PREFERRED_WIDTH;

      String heightStr = props.getProperty(DietsConfigProperties.DIETSGUI_HEIGHT);
      DietsGUI.PREFERRED_HEIGHT = heightStr != null ? Integer.parseInt(heightStr)
            : DietsGUI.PREFERRED_HEIGHT;

      //String testRegistry = ISSITesterConstants.getTestRegistry();
      //showln("doStaticReInitialization: testRegistry="+testRegistry);

      setTraceSource( props.getProperty("dietsgui.traceSource", "conformance-tester"));
      //+++updateGlobalTopologyValues();

      try {
         issiTesterConfiguration = new ISSITesterConfigurationParser(testerConfigurationFile).parse();
      }
      catch (Exception ex) {
         logger.error("Error parsing config file ", ex);
         JOptionPane.showMessageDialog(null,
            "Error parsing configuration file " + testerConfigurationFile,
            "ISSI Tester startup error",
            JOptionPane.ERROR_MESSAGE);
         throw ex;
      }
      logger.debug("testerConfigurationFile=" + testerConfigurationFile);
      logger.debug("issiTesterConfiguration=" + issiTesterConfiguration.toString());

      TestRFSS.setTesterConfiguration(issiTesterConfiguration);
      updateGlobalTopologyValues();

      // At this point, the Daemon is not running yet
      // the getLocalAddresses will fail when binding to socket
      ipAddresses = issiTesterConfiguration.getLocalAddresses();
      logger.debug("doStaticReInitialization: ipAddress="+ipAddresses);

      if ((ipAddresses == null || ipAddresses.isEmpty()) && standalone) {
         JOptionPane.showMessageDialog( null,
            "No local ip addresses could be configured, cannot run standalone !",
            "ISSI Tester startup error", 
	    JOptionPane.ERROR_MESSAGE);
         throw new Exception("Configuration Exception -- startup");
      }

      logger.debug("doStaticReInitialization: init remoteController...standalone="+standalone);
      if (standalone) {
         remoteController = new RfssController(ipAddresses);
      } else {
         remoteController = new RemoteTestController(this);
      }

      SystemTopologyParser systemTopologyParser = new SystemTopologyParser(issiTesterConfiguration);
      showln("DietsGUI: testerConfiguration="+getTesterConfigurationFile());
      showln("DietsGUI: systemTopologyName="+getSystemTopologyFileName());
      showln("DietsGUI: globalTopologyName="+getGlobalTopologyFileName());

      TopologyConfig systemTopology = null;
      try {
         systemTopology = systemTopologyParser.parse(systemTopologyFileName);

      } catch (Exception ex) {
         logger.error("Error occured while parsing system topology " + systemTopologyFileName);
         JOptionPane.showMessageDialog( null,
            "Error in system topology " + systemTopologyFileName
            + "\nException Message is : " + ex.getMessage()
            + "\nCheck Tester Configuration file " + testerConfigurationFile
            + "\nCheck System topology file and make sure node identifiers are mapped for the entire file",
            "ISSI Tester startup error",
            JOptionPane.ERROR_MESSAGE);
         throw ex;
      }

      TopologyConfig globalTopology = null;
      try {
         globalTopology = new GlobalTopologyParser(true).parse(systemTopology, globalTopologyFileName);

      } catch (Exception ex) {
         logger.error( "Error occured while parsing test suite global topology "
               + globalTopologyFileName, ex);
         JOptionPane.showMessageDialog( null,
            "Error occured while parsing test suite global topology "
               + globalTopologyFileName
               + "\nException type is : " + ex.getClass().getName()
               + "\nException message is :" + ex.getMessage()
               + "\nMake sure that the file exists, is valid syntax "
               + " RFSSs referenced in " + globalTopologyFileName
               + "\nare defined in " + systemTopologyFileName,
            "ISSI Tester startup error",
            JOptionPane.ERROR_MESSAGE);
         throw ex;
      }
      if( testExecutionPanel != null) {
         logger.debug("doStaticReInitialization: reconfigure DietsService...");
         testExecutionPanel.reconfigureDietsService();
      }

      try {
         testcases = getAvailableTestCases(ISSITesterConstants.TEST_CLASS_CONFORMANCE);
         //showln("getAvailableTestCases.size="+testcases.size());
      } catch (Exception ex) {
         logger.error("Unexpected error getting test cases", ex);
         JOptionPane.showMessageDialog(null,
            "Error in getting test cases - check test suite",
            "ISSI Tester startup error",
            JOptionPane.ERROR_MESSAGE);
         throw ex;
      }

      if (issiTesterConfiguration.getEmulatorConfigurations().size() == 0
            && !standalone) {
         JOptionPane.showMessageDialog( null,
            "Error in configuration file - no web servers were defined so must specify standalone",
            "ISSI Tester startup error",
            JOptionPane.ERROR_MESSAGE);
         throw new Exception( "Error in configuration file - should specify standalone flag");
      }
   }

   // implementation of TestStatusInterface
   //------------------------------------------------------------------
   public String getWebConfigurationFileName() {
      return testerConfigurationFile;
   }
   public ISSITesterConfiguration getIssiTesterConfiguration() {
      return issiTesterConfiguration;
   }

   public void logError(String failureMessage, String errorTrace) {
      getTestExecutionPanel().logError(failureMessage, errorTrace);
   }
   public void logError(String failureMessage) {
      getTestExecutionPanel().logError(failureMessage);
   }
   public void logFatal(String failureMessage, String stackTrace) {
      getTestExecutionPanel().logFatal(failureMessage, stackTrace);
   }

   public void logStatusMessage(String statusMessage) {
      getTestExecutionPanel().logStatusMessage( statusMessage);
   }

   public String getProgressScreenMessage() {
      return getTestExecutionPanel().getProgressScreenMessage();
   }
   public void setProgressScreenMessage(String progressMessage) {
      getTestExecutionPanel().setProgressScreenMessage(progressMessage);
   }

   public void setStatusLabelText( String msg) {
      getStatusLabel().setText(msg);
   }

   public void setStatusProgressBarIndeterminate( boolean state) {
      getStatusProgressBar().setIndeterminate(state);
   }

   public void saveTestFiles(boolean verbose) throws Exception {
      TesterHelper.saveFiles(verbose);
   }

   public void autoSave() {
      ArrayList compList = new ArrayList();
      ComponentUtils.getAllComponents( tabbedPane, compList);
      for(int i=0; i < compList.size(); i++) {
         Object tpobj = compList.get(i);
         if(tpobj instanceof TracePanel) {
            TracePanel tp = (TracePanel)tpobj;
            tp.autoSave();
//showln("autoSave: Size="+tp.getSize());
//showln("autoSave: W/H="+tp.getDiagramPanel().getWidth()+"/"+tp.getDiagramPanel().getHeight());
         }
      }
   }

   // Auto print SIP/PTT traces
   public void autoPrint() {
      ArrayList compList = new ArrayList();
      ComponentUtils.getAllComponents( tabbedPane, compList);
      for(int i=0; i < compList.size(); i++) {
         Object tpobj = compList.get(i);
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
      //showln("tabbedPaneClicked(): ...");
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

   //=========================================================================
   public static void main(String[] args) throws Exception {

      String STARTUP_PROP = "startup/diets-standalone.properties";
      GetOpt opts = new GetOpt(args);
      if( !opts.isDefined("startup")) {
         showln("Usage: java  DietsGUI -startup startup.properties");
         System.exit(-1);
      }

      String startupFileName = opts.getStringOpt("startup", STARTUP_PROP);
      showln("DietsGUI: MAIN-startupFileName="+ startupFileName);

      DietsGUI gui = new DietsGUI( startupFileName);
      gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      // force Setup Test on default test
      //===gui.doSetupTest();
   }
}
