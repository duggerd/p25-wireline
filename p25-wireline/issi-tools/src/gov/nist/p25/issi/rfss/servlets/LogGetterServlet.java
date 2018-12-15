//
package gov.nist.p25.issi.rfss.servlets;

import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.rfss.tester.TestScript;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.apache.log4j.Logger;

public class LogGetterServlet extends HttpServlet {
   
   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(LogGetterServlet.class);

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException {

      RfssConfig rfssConfig = (RfssConfig) getServletConfig().getServletContext().getAttribute(
                  ISSITesterConstants.RFSSCONFIG_PARAM);
      String debugFile = request.getParameter(ISSITesterConstants.FILE);
      try {
         if ( ISSITesterConstants.SIPDEBUG.equalsIgnoreCase(debugFile)) {
            String fileName = TestScript.getSipDebugLog();
            PrintWriter printWriter = response.getWriter();
            File file = new File(fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
               printWriter.write(line + "\n");
            }
            printWriter.flush();
            printWriter.close();
         } else if ( ISSITesterConstants.PTTDEBUG.equalsIgnoreCase(debugFile)) {
            String fileName = TestScript.getRtpDebugLog();
            PrintWriter printWriter = response.getWriter();
            File file = new File(fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
               printWriter.write(line + "\n");
            }
            printWriter.flush();
            printWriter.close();
         } else if ( ISSITesterConstants.SIPMESSAGES.equalsIgnoreCase(debugFile)) {
            File directory = new File(ISSITesterConstants.DEFAULT_LOGS_DIR);
            for (String fileName : directory.list(new FilenameFilter() {
               public boolean accept(File dir, String name) {
                  return name.endsWith(".sip");
               }
            })) {

               // OutputStream outputStream = response.getOutputStream();
               PrintWriter printWriter = response.getWriter();
               File file = new File(fileName);
               response.setBufferSize((int) file.length());
               FileReader freader = new FileReader(file);
               BufferedReader reader = new BufferedReader(freader);
               // printWriter.println("<body><pre>");
               String line;
               while ((line = reader.readLine()) != null) {
                  printWriter.println(line);
               }
               // printWriter.println("</pre></body>");
               freader.close();
               reader.close();
               printWriter.flush();
               printWriter.close();
            }
         } else if ( ISSITesterConstants.PTTMESSAGES.equalsIgnoreCase(debugFile)) {
            String fileName = TestScript.getRtpMessageLog();
            PrintWriter printWriter = response.getWriter();
            File file = new File(fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
               printWriter.write(line + "\n");
            }
            reader.close();
            reader.close();
            printWriter.flush();
            printWriter.close();
         } else if ( ISSITesterConstants.CONSOLE.equalsIgnoreCase(debugFile)) {
            String fileName = rfssConfig.getConsoleLog();
            PrintWriter printWriter = response.getWriter();
            File file = new File(fileName);
            BufferedReader reader = new BufferedReader(new FileReader(
                  "logs/" + file));
            String line;
            while ((line = reader.readLine()) != null) {
               printWriter.write(line + "\n");
            }
            reader.close();
            printWriter.flush();
            printWriter.close();

         } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter printWriter = response.getWriter();
            printWriter.print("expecting ?file=sipdebug or ?file=pttdebug url parameter ");
            printWriter.flush();
            printWriter.close();
            response.flushBuffer();
         }

      } catch (IOException ex) {
         response.sendError(HttpServletResponse.SC_NOT_FOUND);
         PrintWriter printWriter = response.getWriter();
         printWriter.print("The Resource " + debugFile + " was not found!");
         printWriter.flush();
         printWriter.close();
         response.flushBuffer();
      }
   }
}
