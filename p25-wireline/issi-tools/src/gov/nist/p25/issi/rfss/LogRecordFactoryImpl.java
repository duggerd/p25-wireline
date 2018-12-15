//
package gov.nist.p25.issi.rfss;

import org.apache.log4j.Logger;

import gov.nist.javax.sip.LogRecord;
import gov.nist.javax.sip.LogRecordFactory;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;

/**
 * This hooks into the jain sip stack and is invoked whenever a log record
 * is to be written out. 
 */
public class LogRecordFactoryImpl implements LogRecordFactory {
   private static Logger logger = Logger.getLogger(LogRecordFactoryImpl.class);

   private TopologyConfig topologyConfig;

   // accessor
   public TopologyConfig getTopologyConfig() { return topologyConfig; } 
   public void setTopologyConfig(TopologyConfig topologyConfig) {
      this.topologyConfig = topologyConfig;
   }
   
   // constructor
   public LogRecordFactoryImpl(TopologyConfig topologyConfig) {
      this.topologyConfig = topologyConfig;
   }
   
   public LogRecord createLogRecord(String message, String source, String destination,
         long timeStamp, boolean isSender, String firstLine, 
         String tid, String callId, long timeStampHeaderVal) {

      if ( source.equals(destination)) {
         if ( logger.isDebugEnabled())
            logger.debug("Rejecting log (selfloop) " + message);
         return null;
      }
      LogRecordImpl logRecordImpl = new LogRecordImpl(message,source,destination,
            timeStamp,isSender,firstLine,tid,callId, timeStampHeaderVal);
      
      String[] src = source.split(":");
      String fromIpAddress  = src[0];
      String fromPortString = src[1];
      String[] dest = destination.split(":");
      String destIpAddress = dest[0];
      String destPortString = dest[1];
   
      // Log only incoming arcs if we have both the From and to ip addersses.
      // This means we can see messages from both sides.
      if ( fromIpAddress.equals(destIpAddress) && isSender ) {
         if ( logger.isDebugEnabled() ) {
            logger.debug("Rejecting from sender -- we own both from and to addresses. ");
         }
         return null;
      } 
       
      int fromPort = Integer.parseInt(fromPortString);
      RfssConfig fromRfss = topologyConfig.getRfssConfig(fromIpAddress,fromPort);
      if ( fromRfss == null ) {
         logger.error("Could not resolve " + fromIpAddress + ":" + fromPort);
         return null;
      }
      logRecordImpl.setFromRfssId(fromRfss.getDomainName());

      int destPort = Integer.parseInt(destPortString);
      RfssConfig toRfss = topologyConfig.getRfssConfig(destIpAddress,destPort);
      if ( toRfss.isEmulated() && isSender) {
         return null;
      }
      logRecordImpl.setToRfssId(toRfss.getDomainName());
      return logRecordImpl;
   }
}
