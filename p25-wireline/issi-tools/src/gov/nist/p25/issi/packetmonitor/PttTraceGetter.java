//
package gov.nist.p25.issi.packetmonitor;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Servlet handler for the packet monitor (gets the ptt traces).
 * 
 */
public class PttTraceGetter extends HttpServlet {
   
   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(PttTraceGetter.class);
   
   public PttTraceGetter() {
   }

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws IOException, ServletException {
      
      logger.debug("PttTraceGetter: doGet(): PacketMonitor: getting ptt traces");
      String retval = "";
      PacketMonitor packetMonitor = PacketMonitor.getCurrentInstance();
      if (packetMonitor != null) {
         retval = packetMonitor.getPttMessages();
      }
      logger.debug("PttTraceGetter: doGet(): retrieved ptt traces\n" + retval);
      response.getWriter().print(retval);
      response.getWriter().close();
   }
}
