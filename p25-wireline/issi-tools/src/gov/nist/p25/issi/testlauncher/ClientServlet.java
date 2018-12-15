//
package gov.nist.p25.issi.testlauncher;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import gov.nist.p25.issi.constants.ISSITesterConstants;

/**
 * Handles HTTP post status updates from the RFSS's.
 */
public class ClientServlet extends HttpServlet {
   
   private static final long serialVersionUID = -1L;  
   private static Logger logger = Logger.getLogger(ClientServlet.class);
   static {
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
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

      RemoteTestController remoteController = (RemoteTestController) this
            .getServletConfig().getServletContext().getAttribute(
                  ISSITesterConstants.CONTROLLER);

      String command = request.getParameter(ISSITesterConstants.COMMAND);
      logger.debug("ClientServlet: command="+command);
      ServletInputStream sin = request.getInputStream();
      try {
         int length = request.getContentLength();
         byte[] readbuf = new byte[length];
         sin.read(readbuf);

         String message = new String(readbuf);
         logger.debug("ClientServlet: message="+message);
         if (message != null) {
            remoteController.getTestStatusInterface().logStatusMessage(message);      
	 }
           
      } catch (Exception ex) {
         throw new ServletException("ClientServlet exception: ", ex);
      } finally {
         // sin.close();
         response.setStatus(HttpServletResponse.SC_OK);
      }
   }
}
