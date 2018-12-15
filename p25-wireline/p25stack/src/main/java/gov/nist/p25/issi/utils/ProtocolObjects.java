//
package gov.nist.p25.issi.utils;

//import gov.nist.p25.issi.constants.ISSITesterConstants;

import java.util.HashSet;
import java.util.Properties;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import org.apache.log4j.Logger;

/**
 * This class contains the Protocol Objects -- i.e. SIP and eventually RTP.
 */
public class ProtocolObjects {
   
   static Logger logger = Logger.getLogger("gov.nist.javax.sip");
   public static final String DEBUG_LOG = "logs/debuglog.txt";
   
   public static final HeaderFactory headerFactory;
   public static final MessageFactory messageFactory;   
   public static final AddressFactory addressFactory;   
   public static final SipFactory sipFactory;
   
   private static Properties properties;   
   private static HashSet<SipStack> sipStacks ;
   private static boolean running;
   
   static {
      sipStacks = new HashSet<SipStack>();
      sipFactory = SipFactory.getInstance();
      sipFactory.setPathName("gov.nist");
      properties = new Properties();

      //properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "16");
      if ( logger.isDebugEnabled() ) {
         properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
      } else {
         properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "16");
      }      
      //properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", ISSITesterConstants.DEBUG_LOG);
      properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", DEBUG_LOG);
         
      try {
         headerFactory = sipFactory.createHeaderFactory();
         addressFactory = sipFactory.createAddressFactory();
         messageFactory = sipFactory.createMessageFactory();

      } catch (Exception x) {
   
         throw new RuntimeException("Bad initialization ", x);
      }
   }
   
   //-------------------------------------------------------------------
   public static MessageFactory getMessageFactory() {
      return messageFactory;
   }

   public static HeaderFactory getHeaderFactory() {
      return headerFactory;
   }

   public static AddressFactory getAddressFactory() {
      return addressFactory;
   }
   
   public static boolean isRunning() {
      return running;
   }
   
   public static void start() throws Exception {
      logger.debug("Start sip stacks!");
      for (SipStack sipStack: sipStacks) {
         sipStack.start();
      }
      ProtocolObjects.running = true;     
   }

   public static synchronized void stop() {
      logger.debug("Stopping stacks!");
      for (SipStack sipStack: sipStacks) {         
         sipStack.stop();
      }
      sipStacks.clear();
      ProtocolObjects.running = false;    
   }
   
   public static Properties getProperties() {
      return properties;
   }
   public static SipFactory getSipFactory() {
      return sipFactory;
   }
   
   public static void addToSipStacks( SipStack sipStack) {
      sipStacks.add(sipStack);      
   }

}
