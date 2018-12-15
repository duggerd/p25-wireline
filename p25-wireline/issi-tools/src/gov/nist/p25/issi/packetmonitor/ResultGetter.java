//
package gov.nist.p25.issi.packetmonitor;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Servlet that GETs the measurement table result.
 * 
 */
public class ResultGetter extends HttpServlet {
   
   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(ResultGetter.class);

   public ResultGetter() {
   }

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws IOException, ServletException {

      logger.debug("ResultGetter: doGet(): getting performance");
      String retval = "";
      PacketMonitor packetMonitor = PacketMonitor.getCurrentInstance();
      if (packetMonitor != null) {
         retval = packetMonitor.getResultTable();
      }
      logger.debug("ResultGetter: doGet(): retrieved performance=\n"+retval);
      response.getWriter().print(retval);
      response.getWriter().close();
   }
}
