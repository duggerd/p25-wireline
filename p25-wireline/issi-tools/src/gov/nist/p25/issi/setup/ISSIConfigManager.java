//
package gov.nist.p25.issi.setup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.GlobalTopologyParser;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfigParser;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfigurationParser;

import org.apache.log4j.Logger;

/**
 * ISSI Configuration Manager class.
 * 
 */
public class ISSIConfigManager {
   
   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(ISSIConfigManager.class);
   public static void showln(String s) { System.out.println(s); }

   private String startupFile;
   private Properties props;
   private ISSITesterConfiguration issiTesterConfiguration;
   private TopologyConfig systemTopologyConfig;

   // accessor
   public String getStartupFile()
   {
      return startupFile;
   }
   public void setStartupFile(String startupFile)
   {
      this.startupFile = startupFile;
   }
   public ISSITesterConfiguration getIssiTesterConfiguration() {
      return issiTesterConfiguration;
   }
   public TopologyConfig getSystemTopologyConfig() {
      return systemTopologyConfig;
   }

   //-----------------
   public String getSystemTopologyFile() throws FileNotFoundException
   {
      String name = props.getProperty( DietsConfigProperties.SYSTEM_TOPOLOGY_PROPERTY);
      if (name == null) {
         throw new FileNotFoundException("Bad or missing system topology file: "+name);
      }
      return name;
   }
   public void setSystemTopologyFile(String name) throws Exception
   {
      if (name == null || name.length() == 0) {
         throw new FileNotFoundException("Bad input system topology file: "+name);
      }
      props.setProperty( DietsConfigProperties.SYSTEM_TOPOLOGY_PROPERTY, name);
   }

   public String getTesterConfigurationFile() throws FileNotFoundException
   {
      String name = props.getProperty( DietsConfigProperties.DAEMON_CONFIG_PROPERTY);
      if (name == null) {
         throw new FileNotFoundException("Bad or missing tester config file: "+name);
      }
      return name;
   }
   public void setTesterConfigurationFile(String name) throws Exception
   {
      if (name == null || name.length() == 0) {
         throw new FileNotFoundException("Bad input tester config file: "+name);
      }
      props.setProperty( DietsConfigProperties.DAEMON_CONFIG_PROPERTY, name);
   }

   public String getTestSuite() throws FileNotFoundException
   {
      String name = props.getProperty( DietsConfigProperties.TESTSUITE_PROPERTY, "conformance");
      if (name == null) {
         throw new FileNotFoundException("Bad or missing testsuite name: "+name);
      }
      return name;
   }

   // constructor
   // --------------------------------------------------------------------------
   public ISSIConfigManager()
   {
      props = new Properties();
   }
   public ISSIConfigManager(String startupFile) throws Exception
   {
      initProperties(startupFile);
   }

   public void initProperties(String startupFile) throws Exception
   {
      // typical diets.properties
      // diets.tester.testsuite=conformance
      // diets.daemon.configuration=testerconfig/local-configuration.xml
      // diets.packetmonitor.systemTopology=testsuites/conformance/systemtopology.xml
      //
      this.startupFile = startupFile;
      props = new Properties();
      FileInputStream inStream = new FileInputStream(new File(startupFile));
      props.load(inStream);

      initTopologyConfig();
   }

   public void initTopologyConfig() throws Exception
   {
      // PacketMonitor: diets.properties
      //  testerConfigurationFile=DIETS-IWCE-CAPTURES/local-configuration.xml
      //  systemTopologyFile=DIETS-IWCE-CAPTURES/systemtopology.xml
      //      
      logger.debug("initTopologyConfig(): startupFile: "+startupFile);
      try {
         String testerConfigurationFile = getTesterConfigurationFile();
         logger.debug("  testerConfigurationFile="+testerConfigurationFile);
         issiTesterConfiguration = new ISSITesterConfigurationParser(
               testerConfigurationFile).parse();

         logger.debug("  getSystemTopologyFile(): ...");
         String systemTopologyFile = getSystemTopologyFile();
         logger.debug("  systemTopologyFile="+systemTopologyFile);
         systemTopologyConfig = new SystemTopologyParser(issiTesterConfiguration)
               .parse(systemTopologyFile);

      } catch (Exception ex) {
         logger.error("Error in parsing configuration files", ex);
         throw ex;
      }
   }

   public TopologyConfig getGlobalTopologyConfig(boolean interactive)
      throws Exception {
      String testSuite = getTestSuite();
      String globalTopologyFileName = ISSITesterConstants.getGlobalTopologyFileName(testSuite);
      showln("\n*** ISSIConfigManager: getTopologyConfig ***");
      showln("  interactive: " + interactive);
      showln("  testSuite: " + testSuite);
      showln("  globalTopologyName: " + globalTopologyFileName);

      GlobalTopologyParser globalParser = new GlobalTopologyParser(interactive);
      TopologyConfig globalTopology = globalParser.parse( systemTopologyConfig, globalTopologyFileName);
      return globalTopology;
   }

   //-------------------------------------------------------------------------------------
   // Based on TestExecutionPanel
   //
   // interactive: true
   // testerConfiguration: testerconfig/standalone-configuration.xml
   // SystemTopologyName:  testsuites/conformance/systemtopology.xml
   // GlobalTopologyName:  testsuites/conformance/testscripts/globaltopology.xml
   // topologyFileName  :  testsuites/conformance/testscripts/su_registration_successful_presence/topology1.xml
   //
   //-------------------------------------------------------------------------------------
   public TopologyConfig getTopologyConfig(boolean interactive, String curTopologyName)
      throws Exception {
      String topologyFileName = ISSITesterConstants.getScenarioDir() +"/" +curTopologyName;

      String testSuite = getTestSuite();
      String systemTopologyFileName = ISSITesterConstants.getSystemTopologyFileName(testSuite);
      String globalTopologyFileName = ISSITesterConstants.getGlobalTopologyFileName(testSuite);

      showln("\n*** ISSIConfigManager: getTopologyConfig ***");
      showln("  interactive: " + interactive);
      showln("  testSuite: " + testSuite);
      showln("  testerConfiguration: " + getTesterConfigurationFile());
      showln("  systemTopologyName:  " + systemTopologyFileName);
      showln("  globalTopologyName:  " + globalTopologyFileName);
      showln("  topologyFileName  :  " + topologyFileName);

      GlobalTopologyParser globalParser = new GlobalTopologyParser(interactive);
      TopologyConfig globalTopology = globalParser.parse( systemTopologyConfig, globalTopologyFileName);

      TopologyConfigParser configParser = new TopologyConfigParser();
      TopologyConfig testTopology = configParser.parse(globalTopology, topologyFileName);
      return testTopology;
   }

   //==========================================================================
   public static void main(String[] args) throws Exception 
   {
      String startupFile; 
      //startupFile = "c:/project/issi-emulator/DIETS-IWCE-CAPTURES/diets.properties";
      startupFile = "c:/research/p25-wireline/issi-tools/startup/diets-emulator.properties";
      if( args.length > 0) {
         startupFile = args[0];
      }

      ISSIConfigManager configMgr = new ISSIConfigManager(startupFile);
      showln("\nstartupFile="+configMgr.getStartupFile());
      showln("\nIssiTesterConfig=\n" + configMgr.getIssiTesterConfiguration());

      TopologyConfig systemTopology = configMgr.getSystemTopologyConfig();
      showln("\nsystemTopology=\n" + systemTopology.exportSystemTopology());

      boolean interactive = true;
      TopologyConfig globalTopology = configMgr.getGlobalTopologyConfig(interactive);
      showln("\nglobalTopology=\n" + globalTopology.exportGlobalTopology());
   }
}
