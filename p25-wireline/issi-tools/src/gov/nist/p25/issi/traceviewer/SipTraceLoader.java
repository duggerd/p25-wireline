//
package gov.nist.p25.issi.traceviewer;

import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * This class implements a SIP XML trace file loader.
 */
public class SipTraceLoader implements TraceLoader {
   
   private static Logger logger = Logger.getLogger(SipTraceLoader.class);
   
   private HashSet<RfssData> rfssList = new HashSet<RfssData>();
   private Collection<RfssData> sortedRfssList;
   private TopologyConfig topologyConfig;
   private SipTraceFileParser sipTracefileParser;
   private List<SipMessageData> messageData;
   
   // constructor
   public SipTraceLoader(ByteArrayInputStream byteArrayInputStream,
         Hashtable<String, HashSet<PttSessionInfo>> runtimeData,
         TopologyConfig topologyConfig)
      throws Exception {

      this.topologyConfig = topologyConfig;
      sipTracefileParser = new SipTraceFileParser(byteArrayInputStream,
            runtimeData, getTopologyConfig());
      sipTracefileParser.parse();

      messageData = sipTracefileParser.getRecords();
      rfssList = sipTracefileParser.getRfssList();
      //logger.debug("SipTraceLoader: getRfssList="+rfssList.size());
      if( rfssList.size()==0) {
         logger.debug("SipTraceLoader: Zero rfssList SipTraceFileParser...");
         rfssList = sipTracefileParser.getRfssListFromTopologyConfig();
      }
      // for debug
      logger.debug("SipTraceLoader: rfssList --> RfssData=");
      for( RfssData rfssData: rfssList) {
         logger.debug("SipTraceLoader: ---> "+rfssData);
      }
      
      List<String> traceOrderList = topologyConfig.getTraceOrder();
      logger.debug("SipTraceLoader: traceOrderList="+traceOrderList.size());

      if (traceOrderList.size() != rfssList.size()) {
         //logger.debug("SipTraceLoader: addAll...");
         sortedRfssList = new TreeSet<RfssData>();
         sortedRfssList.addAll(rfssList);
      } else {
         //logger.debug("SipTraceLoader: user specifies order...");
         // User has specified a sorting order.
         sortedRfssList = new LinkedList<RfssData>();
         for (String name : traceOrderList) {
            boolean added = false;
            logger.debug("SipTraceLoader: sorting-name="+name);
            for (RfssData rfssData : rfssList) {
               RfssConfig rfssConfig = rfssData.getRfssConfig();
               if(rfssConfig == null) continue;

               //logger.debug("SipTraceLoader: rfssConfig="+rfssConfig);
               if (rfssConfig.getRfssName().equals(name)) {
                  sortedRfssList.add(rfssData);
                  added = true;
                  break;
               }
            }
            if (!added) {
               sortedRfssList = new TreeSet<RfssData>();
               sortedRfssList.addAll(rfssList);
               break;
            }
         }
      }
   }

   public Collection<RfssData> getSortedRfssList() {
      return sortedRfssList;
   }
   public TopologyConfig getTopologyConfig() {
      return topologyConfig;
   }

   @Override
   public List<MessageData> getRecords() {   
      ArrayList <MessageData> retval = new ArrayList<MessageData>();
      retval.addAll(messageData);
      return retval;
   }
   
   public String getMessageData() {
      StringBuffer sbuf = new StringBuffer();
      for (MessageData messageData : getRecords()) {
         sbuf.append("F" + messageData.getId() + ":\n");
         sbuf.append(messageData.getData().trim());
         sbuf.append("\n\n");
      }
      return sbuf.toString();
   }
}
