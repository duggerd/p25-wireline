//
package gov.nist.p25.issi.testlauncher;

import gov.nist.p25.issi.constants.ISSITesterConstants;

import org.mortbay.jetty.servlet.ServletHandler;

/**
 * The servlet handler for the client servlet. The client servlet
 * is used to post information from the server side of the tester
 * to the client. The client registers for notification from the
 * server on startup provided it is configured to do so.
 * 
 */
public class ClientServletHandler extends ServletHandler
{
   private static final long serialVersionUID = -1L;  
   
   public ClientServletHandler(TestControllerInterface remoteTestController) {
      setContextAttribute(ISSITesterConstants.CONTROLLER, remoteTestController);
   }
}
