//
package gov.nist.p25.issi.rfss;

import java.util.Properties;
import javax.sip.*;
import org.apache.log4j.Logger;

import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.utils.ProtocolObjects;

public class SipStackFactory {
   private static Logger logger = Logger.getLogger("gov.nist.javax.sip");
   
   public static SipStack getSipStack(String rfssName, TopologyConfig topologyConfig) {
      try {
         Properties properties = ProtocolObjects.getProperties();

         // Create SipStack object
         // Log only messages here.         
         properties.setProperty("javax.sip.STACK_NAME", "rfss."+rfssName);
      
         // Log the message contents of the message.
         properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "true");
         properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
               "logs/"+ rfssName + ".messagelog.sip");

         properties.setProperty("javax.sip.ROUTER_PATH", RfssRouter.class.getName());
         //"gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY"
         properties.setProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", "true");

         SipStack sipStack = ProtocolObjects.getSipFactory().createSipStack(properties);
         RfssRouter rfssRouter = (RfssRouter)sipStack.getRouter();
         rfssRouter.setTopologyConfig(topologyConfig);

         logger.debug("createSipStack " + sipStack);
         ProtocolObjects.addToSipStacks( sipStack);

         return sipStack;
      } 
      catch (PeerUnavailableException e)
      {
         // could not find in the classpath
         e.printStackTrace();
         throw new RuntimeException("Bad initialization ", e);
      }      
   }
}
