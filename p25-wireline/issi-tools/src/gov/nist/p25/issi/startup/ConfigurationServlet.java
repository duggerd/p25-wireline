//
package gov.nist.p25.issi.startup;

import gov.nist.p25.common.util.Grep2;
import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.packetmonitor.PacketMonitor;

// 14.4.x
import gov.nist.p25.issi.rfss.servlets.ControlServlet;
import gov.nist.p25.issi.rfss.tester.RfssController;

import gov.nist.p25.issi.rfss.tester.TestRFSS;
import gov.nist.p25.issi.setup.ISSIConfigManager;
import gov.nist.p25.issi.testlauncher.DietsService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mortbay.jetty.Server;


public class ConfigurationServlet extends HttpServlet {

   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(ConfigurationServlet.class);
   
   private static Server server;
   private static DietsService dietsService;

   // accessor
   public static void setServer(Server server) {
      ConfigurationServlet.server = server;
   }

   public static void setDietsService(DietsService dietsService) {
      ConfigurationServlet.dietsService = dietsService;
   }
   // 14.4.x
   public static DietsService getDietsService() {
      return ConfigurationServlet.dietsService;
   }

   // constructor
   public ConfigurationServlet() {      
   }

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {
      //TODO: to support CSSI
      doPost(request, response);
   }

   @Override
   public void doPost(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {

      // NOTE: the clusterManaerStartup should be interface !!!
      try {
         
         String cmd = request.getParameter(ISSITesterConstants.COMMAND);
         logger.debug("Server Command: " + cmd);

         if ( ISSITesterConstants.START_SERVICES.equals(cmd)) {
            // This contains a fresh copy of tester configuration file.
            logger.debug("Starting services ");
            String topologyFileName = request.getParameter(ISSITesterConstants.TESTER_DAEMON_TOPOLOGY);
            logger.debug("topology file name = " + topologyFileName);
                  
            if (PacketMonitor.isConfigured()) {
               logger.debug("Uncofiguring packet monitor");
               PacketMonitor.unconfigure();
            }
            logger.debug("About to configure packet monitor!");
            int count = PacketMonitor.configure(server, topologyFileName);
            logger.debug("configure(): count="+count);

            if ( TestRFSS.isConfigured()) {
               TestRFSS.unconfigure();
            }
            count = TestRFSS.configure(server, topologyFileName);
   
         }  else if ( ISSITesterConstants.STOP_SERVICES.equals(cmd)) {

            if (PacketMonitor.isConfigured()) {
               PacketMonitor.unconfigure();
            }
            if ( TestRFSS.isConfigured()) {
               TestRFSS.unconfigure();
            }
            
         } else if ( ISSITesterConstants.UPDATE_CONFIGURATION_FILE.equals(cmd)) {

            InputStream inputStream = request.getInputStream();
            int length = request.getContentLength();
            
            byte[] content = new byte[length];            
            int b;         
            for  ( int i = 0; (b = inputStream.read()) != -1  ; i ++) {
               content[i] = (byte)   b;            
            }
            
            String topologyFileContents = new String(content);         
            String topologyFileName = request.getParameter(ISSITesterConstants.FILENAME_TO_UPDATE);
            logger.debug("Update configuration file: fileNameToUpdate="+topologyFileName);
            
            FileWriter fwriter = new FileWriter( new File( topologyFileName));
            fwriter.write(topologyFileContents);
            fwriter.close();
               
         }
         else if ( ISSITesterConstants.REDRAW.equals(cmd)) {

            String propertiesFileName = request.getParameter(ISSITesterConstants.KEY_STARTUP_FILE);
            logger.debug("ConfigurationServlet: REDRAW command: "+propertiesFileName);

            DietsConfigProperties props = null;
            String testSuite = null;
            String dietsConfigFileName = null;
            String systemTopologyFileName = null;
            if (propertiesFileName == null || !new File(propertiesFileName).exists()) {
               return;

            } else {
               props = new DietsConfigProperties(propertiesFileName);
               testSuite = props.getProperty(DietsConfigProperties.TESTSUITE_PROPERTY);
               dietsConfigFileName = props.getProperty(DietsConfigProperties.DAEMON_CONFIG_PROPERTY);
               if (testSuite != null) {
                  //systemTopologyFileName = "testsuites/" + testSuite + "/systemtopology.xml";
                  systemTopologyFileName = ISSITesterConstants.getSystemTopologyFileName(testSuite);
	       } else {
                  systemTopologyFileName = null;
               }
               
               if (props.getProperty(DietsConfigProperties.SYSTEM_TOPOLOGY_PROPERTY) != null) {
                  systemTopologyFileName = props.getProperty(DietsConfigProperties.SYSTEM_TOPOLOGY_PROPERTY);
               }
            }
            logger.debug("ConfigurationServlet: testSuite="+testSuite);
            logger.debug("ConfigurationServlet: systemTopologyFileName="+systemTopologyFileName);
            logger.debug("ConfigurationServlet: dietsConfigFileName="+dietsConfigFileName);
         }
         else if ( ISSITesterConstants.STATIC_REINITIALIZATION.equals(cmd)) {

            String startupFile = request.getParameter(ISSITesterConstants.KEY_STARTUP_FILE);
            logger.debug("ConfigurationServlet: REINITIALIZE command: "+startupFile);
            dietsService.getTestExecutionPanel().doStaticReInitialization(startupFile);

         }
         else if ( ISSITesterConstants.SET_CONFORMANCE_TEST.equals(cmd)) {

            String testIndex = request.getParameter(ISSITesterConstants.KEY_TEST_INDEX);
            String testClass = request.getParameter(ISSITesterConstants.KEY_TEST_CLASS);
            logger.debug("Set conformance test: index="+testIndex+" class="+testClass);
            dietsService.getTestExecutionPanel().updateTestCases(testClass);

            int index = Integer.parseInt( testIndex);
            boolean okFlag = dietsService.getTestExecutionPanel().setConformanceTestByIndex(index);
            logger.debug("Set conformance test by index: okFlag="+okFlag);
	    if( okFlag) {
               dietsService.getTestExecutionPanel().setTestLayoutPane();
	    }
            dietsService.getTestExecutionPanel().validate();
         }
         else if ( ISSITesterConstants.SAVE_TEST_FILES.equals(cmd)) {
            logger.debug("SaveTestFiles: ");
            //dietsService.getTestExecutionPanel().setTestFiles(false);
            DietsService.saveTestFiles(false);
         }
	 //--------------------------------------------------------------------
         else if ( ISSITesterConstants.GET_GLOBAL_TOPOLOGY.equals(cmd)) {

            String retval = dietsService.getTestExecutionPanel().getTopologyConfig().exportGlobalTopology(false);
            logger.debug("ConfigurationServlet- getGlobalTopology: retval=\n" + retval);
            if (retval != null) {
               response.setContentType("text/xml");
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }
         }
         else if ( ISSITesterConstants.GET_SYSTEM_TOPOLOGY.equals(cmd)) {

            String retval = dietsService.getTestExecutionPanel().getTopologyConfig().exportSystemTopology(false);
            logger.debug("ConfigurationServlet- getSystemTopology: retval=\n" + retval);
            if (retval != null) {
               response.setContentType("text/xml");
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }
         }
         else if ( ISSITesterConstants.GET_TESTER_CONFIG.equals(cmd)) {

            logger.debug("getTesterConfig: ");
            //String retval = TestRFSS.getTesterConfiguration().toString(false);
            String retval = dietsService.getTesterConfiguration().toString(false);
            logger.debug("ConfigurationServlet- getTesterConfig: retval=\n" + retval);
            if (retval != null) {
               response.setContentType("text/xml");
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }
         }
	 //--------------------------------------------------------------------
         else if ( "getAllGlobalTopology".equals(cmd)) {

            String startupFile = dietsService.getStartupFile();
            logger.debug("getAllGlobalTopology: startupFile="+startupFile);
            ISSIConfigManager configMgr = new ISSIConfigManager(startupFile);
            String retval = configMgr.getGlobalTopologyConfig(true).exportGlobalTopology(false);

            logger.debug("ConfigurationServlet- getAllGlobalTopology: retval=\n" + retval);
            if (retval != null) {
               response.setContentType("text/xml");
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }
         }
         else if ( "getAllSystemTopology".equals(cmd)) {

            String startupFile = dietsService.getStartupFile();
            logger.debug("getAllSystemTopology: startupFile="+startupFile);
            ISSIConfigManager configMgr = new ISSIConfigManager(startupFile);
            String retval = configMgr.getSystemTopologyConfig().exportSystemTopology(false);
            logger.debug("ConfigurationServlet- getAllSystemTopology: retval=\n" + retval);
            if (retval != null) {
               response.setContentType("text/xml");
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }
         }
         else if ( "getAllTesterConfig".equals(cmd)) {

            String startupFile = dietsService.getStartupFile();
            logger.debug("getAllTesterConfig: startupFile="+startupFile);
            ISSIConfigManager configMgr = new ISSIConfigManager(startupFile);
            String retval = configMgr.getIssiTesterConfiguration().toString(false);
            logger.debug("ConfigurationServlet- getAllTesterCofnig: retval=\n" + retval);
            if (retval != null) {
               response.setContentType("text/xml");
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }
         }
	 //--------------------------------------------------------------------
         else if ( ISSITesterConstants.GET_RUNTIME_EXCEPTION.equals(cmd)) {
            String retval = "No Results";
            String pattern = "xception";
            try {
               File xfile = new File("logs/debuglog.txt");
               Grep2.compile(pattern);
               Grep2.grep( xfile);
	       retval = Grep2.getGrepBuffer();
            } catch(IOException ex) {
               retval = ex.toString();
            }

            //logger.debug("ConfigurationServlet- getRuntimeException: retval=\n" + retval);
            if (retval != null) {
               //response.setContentType("text/xml");
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }
         }
	 //--------------------------------------------------------------------
         else if ( ISSITesterConstants.HEARTBEAT_QUERY.equals(cmd)) {
            logger.debug("ConfigurationServlet- heartbeatQuery");
            ControlServlet.getRfssController().doHeartbeatQuery();
         }
	 //--------------------------------------------------------------------
      }
      catch (Exception ex) {

         ex.printStackTrace();
         logger.error("Unexpected exception: ", ex);
         throw new ServletException("Unexpected exception: " + ex.getMessage());
      }
   }
}
