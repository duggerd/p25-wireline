//
package gov.nist.p25.issi.packetmonitor.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.TimerTask;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.constants.ISSILogoConstants;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.packetmonitor.PacketMonitor;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfigurationParser;
import gov.nist.p25.issi.testlauncher.AboutDialog;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

/**
 * A GUI front end for the packet monitor.
 */
public class PacketMonitorGUI extends JFrame
   implements ActionListener {
   
   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(PacketMonitorGUI.class);
   static {
      try {
         PropertyConfigurator.configure("log4j.properties");
         logger.addAppender(new FileAppender(new SimpleLayout(),
               "logs/itatguidebug.txt"));
      } catch (Exception ex) {
         // ignore... :-)
      }
   }
   public static void showln(String s) { System.out.println(s); }

   private static final String VERSION = ISSILogoConstants.VERSION;
   private static final String BUILD = ISSILogoConstants.BUILD;

   private static int PREFERRED_WIDTH = 1050;
   private static int PREFERRED_HEIGHT = 850;   
   private static String systemTopologyFileName;
   private static String testerConfigurationFile;
   private static ISSITesterConfiguration issiTesterConfiguration;
   private static TopologyConfig systemTopology;

   private boolean remoteController;
   private String fileName;
   private ControlPanel controlPanel;
   private JLabel statusLabel;
   private JMenuItem aboutMenuItem;
   private JTabbedPane tabbedPane = new JTabbedPane();
   private MessageInformationPanel messageInformationPanel;

   // accessor
   public static ISSITesterConfiguration getIssiTesterConfiguration() {
      return issiTesterConfiguration;
   }
   
   public boolean getRemoteController() {
      return remoteController;
   }
   public String getFileName() {
      return fileName;
   }
   
   public JLabel getStatusLabel() {
      return this.statusLabel;
   }
   public void setStatusLabel(JLabel label) {
      this.statusLabel = label;
   }

   public JTabbedPane getJTabbedPane() {
       return tabbedPane;
   }

   public MessageInformationPanel getMessageInformationPanel() {
      return messageInformationPanel;
   }

   // constructor
   //-------------------------------------------------------------------------
   public PacketMonitorGUI() throws Exception {
      this(true, null);
   }

   public PacketMonitorGUI(boolean remoteController, String fileName)
      throws Exception {
      super();
      initGUI(remoteController, fileName);
   }

   public void initGUI(boolean remoteController, String fileName)
         throws Exception {
      
      this.remoteController = remoteController;
      this.fileName = fileName;
      logger.info("GUI system initializing: remoteController= " + remoteController);
      
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      getContentPane().setLayout(new BorderLayout());
      
      messageInformationPanel = new MessageInformationPanel(this,
            systemTopologyFileName);
      getMessageInformationPanel().setMinimumSize(
            new Dimension(PREFERRED_WIDTH, 200));
      getMessageInformationPanel().setPreferredSize(
            new Dimension(PREFERRED_WIDTH, 225));
      getMessageInformationPanel().setMaximumSize(
            new Dimension(PREFERRED_WIDTH, 325));

      if (remoteController) {
         // remove reference to this
         RemotePacketMonitorController remotePMC = 
            new RemotePacketMonitorController(testerConfigurationFile, systemTopologyFileName);
         controlPanel = new ControlPanel(this, getMessageInformationPanel(), remotePMC, false);

      } else {
         PacketMonitor packetMonitor = new PacketMonitor(systemTopology);

         //boolean keepDup = showAllPttCheckBox.getState();
         boolean keepDup = false;
         packetMonitor.setKeepDuplicateMessage( keepDup);

         // File name specified on startup.
         if (fileName != null) {
            packetMonitor.readTraceFromFile(fileName);
         }
         controlPanel = new ControlPanel(this, getMessageInformationPanel(),
               new LocalPacketMonitorController(packetMonitor), true);
      }

      JPanel tabbedPanel = new JPanel();
      tabbedPanel.setLayout(new BorderLayout());
      tabbedPanel.setBorder(new TitledBorder("Test Traces"));
      tabbedPanel.add(tabbedPane, BorderLayout.CENTER);

//      JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
//            getMessageInformationPanel(), tabbedPanel);
//      getContentPane().add(splitPane, BorderLayout.CENTER);

      getContentPane().add(tabbedPanel, BorderLayout.CENTER);
      getContentPane().add(controlPanel, BorderLayout.SOUTH);
      
      JPanel statusPanel = new JPanel();
      statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
      setStatusLabel(new JLabel("Ready"));
      getStatusLabel().setMinimumSize(new Dimension(300, 16));
      getStatusLabel().setPreferredSize(new Dimension(700, 16));
      getStatusLabel().setMaximumSize(new Dimension(900, 16));

      setTitle("ITT Packet Monitor & Trace Analysis v" + VERSION);
      setIconImage(new ImageIcon(ISSILogoConstants.ISSI_TESTER_LOGO).getImage());
      setAlwaysOnTop(true);
      setSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
      setVisible(true);

      try {
         ISSITimer.getTimer().schedule(new TimerTask() {
            @Override
            public void run() {
               PacketMonitorGUI.this.setAlwaysOnTop(false);
            }
         }, 1000);
      }
      catch(IllegalStateException ex) {
         showln("schedule timer: ex="+ex);
         logger.error("Failed to schedule timer: ", ex);
      }
      addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            PacketMonitorGUI.this.dispose();
         }
      });
   }

   public static void initProperties(Properties props) {
      systemTopologyFileName = props.getProperty(
            DietsConfigProperties.SYSTEM_TOPOLOGY_PROPERTY,
            systemTopologyFileName);
      testerConfigurationFile = props.getProperty(
            DietsConfigProperties.DAEMON_CONFIG_PROPERTY,
            testerConfigurationFile);

      PacketMonitorGUI.PREFERRED_WIDTH = props
            .getProperty(DietsConfigProperties.DIETSGUI_WIDTH) != null ? Integer
            .parseInt(props.getProperty(DietsConfigProperties.DIETSGUI_WIDTH))
            : PacketMonitorGUI.PREFERRED_WIDTH;
      PacketMonitorGUI.PREFERRED_HEIGHT = props
            .getProperty(DietsConfigProperties.DIETSGUI_HEIGHT) != null ? Integer
            .parseInt(props.getProperty(DietsConfigProperties.DIETSGUI_HEIGHT))
            : PacketMonitorGUI.PREFERRED_HEIGHT;

      if (testerConfigurationFile == null || systemTopologyFileName == null) {
         JOptionPane.showMessageDialog(null,
               "Bad or missing startup properties",
               "Startup Error",
               JOptionPane.ERROR_MESSAGE);
         return;
      }

      //showln("PM: testerConfigurationFile="+testerConfigurationFile);
      //showln("PM: systemTopologyFileName="+systemTopologyFileName);
      try {
         issiTesterConfiguration = new ISSITesterConfigurationParser(
               testerConfigurationFile).parse();

         systemTopology = new SystemTopologyParser(issiTesterConfiguration)
               .parse(systemTopologyFileName);
      } catch (Exception ex) {
         logger.error("Bad or missing config file", ex);
         JOptionPane.showMessageDialog(null,
            "Error in parsing startup configuration file.\n" +
            "Config file must be in DIETS installed directory.\n" + ex.getMessage(),
            "Startup Parser Error",
            JOptionPane.ERROR_MESSAGE);
         
         return;
      }
   }

   //-------------------------------------------------------------------------
   public void actionPerformed(ActionEvent ae) {

      if (ae.getSource() == this.aboutMenuItem) {
         AboutDialog about_dialog = new AboutDialog(this, VERSION, BUILD);
         // about_dialog.setSize(350, 175);
         about_dialog.setLocationRelativeTo(this);
         about_dialog.setVisible(true);
      }
   }

   //==========================================================================
   public static void main(String[] args) throws Exception {
      try {
         String captureFileName = null;
         String startupFileName = null;
         Properties props = new Properties();
         for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-startup")) {
               startupFileName = args[++i];
               InputStream inStream = new FileInputStream(new File(
                     startupFileName));
               props.load(inStream);
               
            } else if (args[i].equals("-capfile")) {
               captureFileName = args[++i];
               
            } else {
               JOptionPane.showMessageDialog(null,
                     "Bad startup flag " + args[i],
                     "Startup Error",
                     JOptionPane.ERROR_MESSAGE);
            }
         }

         if (startupFileName == null || captureFileName == null) {
            StartupFileDialog startupDialog = new StartupFileDialog(
                  startupFileName, captureFileName);
            startupDialog.setLocation(300,300);            
            startupDialog.setVisible(true);
            if (startupDialog.isCancelButtonPressed()) {
               return;
            }
            startupFileName = startupDialog.getStartupFileName();
            captureFileName = startupDialog.getPcapFileName();
            InputStream inStream = new FileInputStream(new File(
                  startupFileName));
            props.load(inStream);
         }

         if (startupFileName == null || captureFileName == null) {
            showln("Missing parameter: ");
            showln("  PacketMonitorGUI -Dstartup=startupFileName -Dcapfile=captureFileName");
            return;
         }
         PacketMonitorGUI.initProperties(props);

         // Schedule a job for the event-dispatching thread
         javax.swing.SwingUtilities.invokeLater(new Runnable() {
            private String filename;

            public Runnable setFlag(String fileName) {
               this.filename = fileName;
               return this;
            }
            public void run() {
               try {
                  final PacketMonitorGUI gui = new PacketMonitorGUI(false,filename);
                  gui.addWindowListener(new WindowAdapter() {
                     public void windowClosing(WindowEvent e) {
                        gui.dispose();
                        System.exit(0);
                     }
                  });
               } catch (Exception ex) {
                  logger.error("Exception starting the GUI", ex);
               }
            }
         }.setFlag(captureFileName));
         
      } catch (Exception ex) {
         
         JOptionPane.showMessageDialog(null,
            "Error starting packet monitor GUI -- check startup parameters.",
            "Startup Error",
            JOptionPane.ERROR_MESSAGE);
      }
   }
}
