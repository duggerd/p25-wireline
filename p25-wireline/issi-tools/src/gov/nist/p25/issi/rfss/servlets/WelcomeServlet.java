//
package gov.nist.p25.issi.rfss.servlets;

import gov.nist.p25.issi.issiconfig.WebServerAddress;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.apache.log4j.Logger;
import org.mortbay.util.Resource;

/**
 * This is the servlet that gets invoked at initally when you point your web
 * browser at the RFSS http port. This will be the entry point for the
 * web based test system.
 */
public class WelcomeServlet extends HttpServlet {

   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(WelcomeServlet.class);
   
   private static ISSITesterConfiguration issiTesterConfig;
   
   public static void setISSITestLauncherConfig(ISSITesterConfiguration issiTesterConfig) {
      WelcomeServlet.issiTesterConfig = issiTesterConfig;
   }

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {
      Resource resource = Resource.newResource("webpages/index.html");
      File file = resource.getFile();
      BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

      PrintWriter printWriter = response.getWriter();
      printWriter.println("<html>");      
      printWriter.println("<body>");
      printWriter.println("<h1>Get your test logs here!</h1>");
      printWriter.println("<ul> ");
      
      String line;
      while ((line = bufferedReader.readLine()) != null) {
         printWriter.println(line);
      }         
      for ( WebServerAddress launcher : issiTesterConfig.getEmulatorConfigurations()) {
         printWriter.println("<li><a href = \"http://" + launcher.getIpAddress()
               + ":" + launcher.getHttpPort() + "/logs/\">Log Files</a>");
         printWriter.println("</ul>");
      }
      printWriter.println("</body>");
      printWriter.println("</html>");
      printWriter.flush();
      printWriter.close();
   }
}
