//
package gov.nist.p25.issi.rfss.servlets;

import gov.nist.p25.common.util.DateUtils;
import gov.nist.p25.common.util.IpAddressUtility;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.rfss.tester.RfssController;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * The control servlet allows you to remotely control the RFSS via HTTP POST.
 */
public class ControlServlet extends HttpServlet {

   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(ControlServlet.class);
   private static Collection<String> ipAddress;

   // 14.4.x use static
   private static RfssController rfssController;
   //private RfssController rfssController = new RfssController(ipAddress);
   public static RfssController getRfssController() {
      return rfssController;
   }

   // called by TestRFSS
   public static void setIpAddress(Collection<String> ipAddress) {

      logger.debug("ControlServlet(): setIpAddress() - ipAddress="+ipAddress);
      ControlServlet.ipAddress = ipAddress;

      rfssController = new RfssController(ipAddress);
      //logger.debug("ControlServlet(): new RfssController()...");
   }

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {
      // TODO: to support CSSI
      _doPost(true, request, response);
   }

   @Override
   public void doPost(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {
      _doPost(false, request, response);
   }

   protected void _doPost(boolean header, HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {
      String command = request.getParameter(ISSITesterConstants.COMMAND);
      if (command == null) {
         throw new ServletException("ControlServlet: Missing " + ISSITesterConstants.COMMAND);
      }

      String testCase = request.getParameter(ISSITesterConstants.TEST_CASE);

      String param = request.getParameter(ISSITesterConstants.INTERACTIVE);
      //logger.debug("ControlServlet: interactive="+param);
      boolean interactive = (param == null ? false : "true".equals(param));

      param = request.getParameter(ISSITesterConstants.EVALUATE_POST_CONDITION);
      boolean evalPassFail = (param == null ? true : "true".equals(param));

      String topology = request.getParameter(ISSITesterConstants.TEST_TOPOLOGY);
      String globalTopology = request.getParameter(ISSITesterConstants.GLOBAL_TOPOLOGY);
      String systemTopology = request.getParameter(ISSITesterConstants.SYSTEM_TOPOLOGY);
      
      //logger.debug("globalTopology = " + globalTopology);
      String scenarioDir = request.getParameter(ISSITesterConstants.SCENARIO_DIR_KEY);
      String testNumber = request.getParameter(ISSITesterConstants.TEST_NUMBER);

      //logger.debug("ControlSerlet: update RfssControler-ipAddress="+ipAddress);
      rfssController.setIpAddress(ipAddress);

      logger.debug("ControlSerlet: command="+command);
      try {
         if (ISSITesterConstants.LOAD.equals(command)) {
            logger.info("ControlServlet- load: scenarioDir=" + scenarioDir);
            logger.info("   testCase=" + testCase);
            logger.info("   testNumber=" + testNumber);
            logger.info("   topology=" + topology);
            logger.info("   globalTopology=" + globalTopology);
            logger.info("   systemTopology=" + systemTopology);
            logger.info("   interactive=" + interactive);
            rfssController.loadTest(scenarioDir,testCase,testNumber,topology,globalTopology,
               systemTopology,interactive,evalPassFail);

         } else if (ISSITesterConstants.RUN.equals(command)) {
            logger.info("ControlServlet- runTest...");
            rfssController.runTest();

         } else if (ISSITesterConstants.GET_RFSS_STATUS_INFO.equals(command)) {
            String retval = rfssController.getRfssPttSessionInfo( header);
            logger.info("ControlServlet- getRfssStatusInfo: retval=\n" + retval);
            response.setContentType("text/xml");
            response.setContentLength(retval.length());
            response.getOutputStream().print(retval.toString());
            response.getOutputStream().flush();

         } else if (ISSITesterConstants.EXECUTE_NEXT_SCENARIO.equals(command)) {
            String scenario = request.getParameter(ISSITesterConstants.SCENARIO);
            logger.info("ControlServlet- signalNextScenario=" + scenario);
            rfssController.signalNextScenario(scenario);

         } else if (ISSITesterConstants.TEAR_DOWN_TEST.equals(command)) {
            logger.info("ControlServlet- tearDownCurrentTest...");
            rfssController.tearDownCurrentTest();

         } else if (ISSITesterConstants.TEST_COMPLETED.equals(command)) {
            rfssController.signalTestCompletion();

         } else if (ISSITesterConstants.GET_TEST_RESULTS.equals(command)) {
            boolean passed = rfssController.getTestResults();
            String retval = passed ? ISSITesterConstants.PASSED : ISSITesterConstants.FAILED;
            logger.info("ControlServlet- getTestResults: retval=\n" + retval);
            response.setContentLength(retval.length());
            response.getOutputStream().print(retval);
            response.getOutputStream().flush();

         } else if (ISSITesterConstants.GET_SIP_TRACE.equals(command)) {
            String retval = rfssController.getStackSipLog();
            logger.info("ControlServlet- getStackSipLog: retval=\n" + retval);
            if (retval != null) {
               if( header)
                  retval = "<sip-messages>\n"+retval+"\n</sip-messages>";
               response.setContentType("text/xml");
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }
         } else if (ISSITesterConstants.GET_PTT_TRACE.equals(command)) {
            String retval = rfssController.getStackPttLog();
            logger.info("ControlServlet- getStackPttLog: retval=\n" + retval);
            if (retval != null) {
               if( header)
                  retval = "<ptt-messages>\n"+retval+"\n</ptt-messages>";
               response.setContentType("text/xml");
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }
         } else if (ISSITesterConstants.GET_ERROR_LOG.equals(command)) {
            String retval = rfssController.getErrorLog();
            logger.info("ControlServlet- getErrorLog: retval=\n" + retval);
            if (retval != null) {
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }

         } else if (ISSITesterConstants.EXIT_EMULATOR.equals(command)) {
            logger.info("ControlServlet: Exitting emulated RFSS!");
            response.flushBuffer();
            response.getWriter().close();
            //System.exit(0);
	    
         } else if (ISSITesterConstants.IS_TEST_COMPLETED.equals(command)) {
            String retval = Boolean.toString(rfssController.isTestCompleted());
            response.setContentLength(retval.length());
            response.getOutputStream().print(retval.toString());
            response.getOutputStream().flush();

         //-------------------------------------------------------------------
         } else if (ISSITesterConstants.GET_RFSS_CONFIG.equals(command)) {
            logTopologyConfig();
            String retval = rfssController.getRfssConfigs();
            logger.info("ControlServlet- getRfssConfigs: retval=\n" + retval);
            if (retval != null) {
               if( header)
                  retval = "<rfssconfigs>\n"+retval+"\n</rfssconfigs>";
               response.setContentType("text/xml");
               response.setContentLength(retval.length());
               response.getOutputStream().print(retval.toString());
               response.getOutputStream().flush();
            } else {
               response.setContentLength(0);
               response.getOutputStream().flush();
            }

         } else if (ISSITesterConstants.GET_DATE_TIME.equals(command)) {

            //String ip = request.getRemoteAddr();
            String ip = IpAddressUtility.getLocalHostAddress().getHostAddress();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

            Date date = new Date();
            String retval = "<date-time>\n";
            retval += "<ip>" +ip +"</ip>\n";
            retval += "<system-t0>" +sdf.format(date) +"</system-t0>\n";
            date = DateUtils.getAtomicTime().getTime();
            retval += "<atomic-date>" +sdf.format(date) +"</atomic-date>\n";
            long tick0 = date.getTime();
            date = new Date();
            retval += "<system-t1>" +sdf.format(date) +"</system-t1>\n";
            long tick1 = date.getTime();
            retval += "<time-tick>" + (tick1-tick0) +"</time-tick>\n";
            retval += "</date-time>";

            logger.info("ControlServlet- getDateTime: retval=\n" + retval);
            response.setContentType("text/xml");
            response.setContentLength(retval.length());
            response.getOutputStream().print(retval);
            response.getOutputStream().flush();

         //-------------------------------------------------------------------
         } else if (ISSITesterConstants.HEARTBEAT_QUERY.equals(command)) {
            logger.info("ControlServlet- doHeartbeatQuery...");
            rfssController.doHeartbeatQuery();

         //-------------------------------------------------------------------
         } else {
            throw new ServletException("ControlServlet: Unknown command: " + command);
         }

      } catch (Exception ex) {
         logger.error("ControlServlet: Error in loading test case", ex);
         String exception = ex.getStackTrace().toString();
         response.getWriter().print(exception);
         response.sendError(HttpServletResponse.SC_BAD_REQUEST);
         response.flushBuffer();
         return;
      }
   }
   private void logTopologyConfig() {
   }
}
