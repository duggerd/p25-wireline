//
package gov.nist.p25.issi.packetmonitor;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Servlet that GETs the packet trace.
 * 
 */
public class SipTraceGetter extends HttpServlet {
   
   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(SipTraceGetter.class);
   
   public SipTraceGetter() {
   }

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws IOException, ServletException {
      
      logger.debug("SipTraceGetter: doGet(): getting sip traces");
      String retval = "";
      PacketMonitor packetMonitor = PacketMonitor.getCurrentInstance();
      if (packetMonitor != null) {
         retval = packetMonitor.getSipMessages();
      }
      logger.debug("SipTraceGetter: doGet(): retrieved sip traces\n" + retval);
      response.getWriter().print(retval);
      response.getWriter().close();
   }
}
