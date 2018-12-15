//
package gov.nist.p25.issi.rfss.servlets;

import gov.nist.p25.issi.issiconfig.RfssConfig;
import org.mortbay.jetty.servlet.ServletHandler;

public class MyServletHandler extends ServletHandler {
   private static final long serialVersionUID = -1L;

   public MyServletHandler(RfssConfig rfssConfig) {
      this.setContextAttribute("rfssconfig", rfssConfig);
   }
}
