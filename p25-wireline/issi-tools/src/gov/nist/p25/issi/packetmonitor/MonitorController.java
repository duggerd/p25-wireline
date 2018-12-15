//
package gov.nist.p25.issi.packetmonitor;

import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Servlet for the control of the packet monitor.
 * 
 */
public class MonitorController extends HttpServlet {
   
   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(MonitorController.class);

   private static PacketMonitor packetMonitor;
   
   @Override
   public void doPost(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {
      
      String command = request.getParameter(ISSITesterConstants.COMMAND);
      logger.debug("MonitorController: doPost(): command= " + command);

      try {
         if ( ISSITesterConstants.START_PACKET_MONITOR.equals(command)) {
            String topologyName = request.getParameter(ISSITesterConstants.SYSTEM_TOPOLOGY);

            TopologyConfig topologyConfig = new SystemTopologyParser(PacketMonitor.getConfigurations())
                  .parse(topologyName);
            logger.debug("MonitorController: starting packet monitor " +  topologyConfig);
            packetMonitor = PacketMonitor.startPacketMonitor(topologyConfig);

         } else if ( ISSITesterConstants.GET_ERROR_FLAG.equals(command)) {
            if ( packetMonitor == null ) {
               response.getWriter().print(new Boolean(true).toString());
            } else {
               response.getWriter().print(new Boolean(packetMonitor.getErrorStatus()).toString());
            }
         } else if ( ISSITesterConstants.GET_ERROR_LOG.equals(command)) {
            if ( packetMonitor == null) {
               response.getWriter().print("Monitor not started");
            } else {
               response.getWriter().print(packetMonitor.getErrorMessage());
            }
         }
      } catch (Exception ex) {
         logger.error("MonitorController: Error in processing request: "+command, ex);
         response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
   }
}
