//
package gov.nist.p25.issi.packetmonitor;

import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.p25payload.ISSIPacketType;
import gov.nist.p25.issi.rfss.SipUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Vector;

import javax.sip.header.FromHeader;
import javax.sip.address.SipURI;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

/**
 * This class implements an packet analyzer.
 */
public class PacketAnalyzer {

   public static Logger logger = Logger.getLogger(PacketAnalyzer.class);
   static {
      try {
         logger.addAppender(new FileAppender(new SimpleLayout(),
               "logs/packetdebug.txt"));
         logger.setLevel(Level.DEBUG);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   public static void showln(String s) { System.out.println(s); }

   private static final String XML_DOC_HEADER = "<?xml version=\"1.0\"?>\n"
      + "<!DOCTYPE performance-parameter SYSTEM \"" 
      + ISSIDtdConstants.URL_ISSI_DTD_PARAMETER + "\">\n";

   private String resultTableString = XML_DOC_HEADER;
   private Vector<Long> transactionTimeRecords = new Vector<Long>();
   private Vector<Long> dialogTimeRecords = new Vector<Long>();
   private Vector<Long> msgRelay = new Vector<Long>();
   private Vector<Long> pttSessionTimeRecords = new Vector<Long>();

   private final PacketMonitor packetMonitor;

   // constructor
   public PacketAnalyzer(PacketMonitor packetMonitor) {
      this.packetMonitor = packetMonitor;
   }

   public void clear() {
      resultTableString = XML_DOC_HEADER;
      transactionTimeRecords.clear();
      dialogTimeRecords.clear();
      msgRelay.clear();
      pttSessionTimeRecords.clear();
   }

   public String getResultString() {
      clear();
      measure();
      return resultTableString;
   }

   private void measure() {
      logger.debug("PacketAnalyzer: measure()...");

      // record the timestamp of all INVITEs, OKs and ACKs
      int count = 0;
      long n = -1;
      long time = 0;

      // create and initialize working array
      int nInvites = packetMonitor.getInviteTransactions().size();
      logger.debug("=====inviteTransactions list: " + nInvites + "======");
      if(nInvites < 2) nInvites = 2;

      long[][] temp = new long[3][ nInvites];
      for(int i=0; i < 3; i++) {
         for(int j=0; j < nInvites; j++) {
            temp[i][j] = n;
         }
      }

      logger.debug("#####inviteTransactions list: " + nInvites + "######");
      String userTag = "";
      for (LinkedList<CapturedSipMessage> sipMessages: packetMonitor.getInviteTransactions().values()) {

         // test the call type, group call or su-to-su
         userTag += ((SipURI) ((FromHeader) sipMessages.get(0).getMessage()
               .getHeader("From")).getAddress().getURI()).getParameter("user");
         resultTableString += "<call type=\"" + userTag
               + "\" />\n<parameter-list>\n";
         break;
      }

      // calculate the time difference of all INVITE transactions
      for (LinkedList<CapturedSipMessage> sipMessages: packetMonitor.getInviteTransactions().values()) {
         if (sipMessages.size() < 2) {
            logger.error("INVITE transaction is incomplete, drop !!!");
            continue;
         }
         for (CapturedSipMessage sipMsg: sipMessages) {
            logger.debug("####SIP msg: \n" + sipMsg.toString());
         }

         temp[0][count] = sipMessages.getFirst().getTimeStamp();
         temp[1][count] = sipMessages.get(1).getTimeStamp();
         time = temp[1][count] - temp[0][count];

         transactionTimeRecords.add(time);
         logger.debug("----------Time of the transaction: " + time);
         showln("----------Time of the transaction: " + time);

         count++;
      }

      // calculate the time different between neighbor INVITEs and OKs
      for (int j = 0; j < 2; j++) {
         for (int i = 0; i < (nInvites - 1); i++) {
	    if( temp[j][i] == -1) continue;
	    if( temp[j][i+1] == -1) continue;
            msgRelay.add(new Long(temp[j][i+1] - temp[j][i]));
         }
      }

      // calculate the time difference of all ACK dialogs for SU-to-SU call
      count = 0;
      //showln("TIA_P25_SU: userTag="+userTag);
      if ( SipUtils.isSubscriberUnit(userTag)) 
      {
         logger.debug("###########ackDialog list: "
               + packetMonitor.getAckDialog().size() + "############");
         for (LinkedList<CapturedSipMessage> sipMessages: packetMonitor.getAckDialog().values()) {
            if (sipMessages.size() < 2) {
               logger.error("ACK dialog is incomplete, drop !!!");
               continue;
            }
            for (CapturedSipMessage sipMsg: sipMessages) {
               logger.debug("####SIP msg: \n" + sipMsg.toString());
            }

            //NOTE: this is NOT correct if there are Trying...then OK
            time = sipMessages.get(1).getTimeStamp() - sipMessages.getFirst().getTimeStamp();

            //temp[0][count] = sipMessages.getFirst().getTimeStamp();
            temp[2][count] = sipMessages.get(1).getTimeStamp();
            dialogTimeRecords.add(time);
            logger.debug("----------Time of the dialog: " + time);
            count++;
         }
         // calculate the time different between neighbor ACKs
         for (int i = 0; i < (packetMonitor.getAckDialog().size() - 1); i++) {
	    if( temp[2][i] == -1) continue;
	    if( temp[2][i+1] == -1) continue;
            msgRelay.add(new Long(temp[2][i+1] - temp[2][i]));
         }
      }

      //showln("TIA_P25_SG: userTag="+userTag);
      // calculate the time difference of PTT-specific parameters for group call
      //------------------------------------------------------------------------
      if ( SipUtils.isGroup(userTag))
      {
         LinkedHashMap<Integer, CapturedPttMessage> pttSessions = packetMonitor.getPttSessions();
         logger.debug("#####pttSession list: " + pttSessions.size() + "#####");
         long value = 0;
         if (pttSessions.size() < 3) {
            logger.error("PTT session is incomplete, drop!!");
         } else {
            // Tg5
            try {
               if( temp[1][0] != -1)
               value = pttSessions.get(ISSIPacketType.PTT_TRANSMIT_REQUEST).getTimeStamp()
                     - temp[1][0];
            } catch ( Exception ex) {
               value = n;
            }
            pttSessionTimeRecords.add( value);
            // Tg6
            try {
               value = pttSessions.get(ISSIPacketType.PTT_TRANSMIT_GRANT).getTimeStamp()
                     - pttSessions.get(ISSIPacketType.PTT_TRANSMIT_REQUEST).getTimeStamp();
            } catch (Exception ex) {
               value = n;
            }
            pttSessionTimeRecords.add( value);
            // Tg7
            try {
               value = pttSessions.get(ISSIPacketType.PTT_TRANSMIT_START).getTimeStamp()
                     - pttSessions.get(ISSIPacketType.PTT_TRANSMIT_REQUEST).getTimeStamp();
            } catch (Exception ex) {
               value = n;
            }
            pttSessionTimeRecords.add( value);
            // Tg8
            try {
               if( temp[1][1] != -1)
               value = pttSessions.get(ISSIPacketType.PTT_TRANSMIT_START).getTimeStamp()
                     - temp[1][1];
            } catch ( Exception ex) {
               value = n;
            }
            pttSessionTimeRecords.add( value);
            // Tgrant_CallingISSI
            try {
               if( temp[0][0] != -1)
               value = pttSessions.get(ISSIPacketType.PTT_TRANSMIT_GRANT).getTimeStamp()
                     - temp[0][0];
            } catch ( Exception ex) {
               value = n;
            }
            pttSessionTimeRecords.add( value);
            // Tgrant_CalledISSI
            try {
               if( temp[0][0] != -1)
               value = pttSessions.get(ISSIPacketType.PTT_TRANSMIT_START).getTimeStamp() 
                     - temp[0][0];
            } catch ( Exception ex) {
               value = n;
            }
            pttSessionTimeRecords.add( value);
         }
      }

      //showln("TIA_P25_SG: msgRelay.size="+msgRelay.size());
      // construct XML format result respectively according to the call type
      if ( SipUtils.isGroup(userTag)) 
      {
         resultTableString += "<param name=\"Tg2\" value=\""
            +( msgRelay.size() > 0 ? msgRelay.firstElement() : n) +"\" />\n";
         resultTableString += "<param name=\"Tg3\" value=\""
            + (transactionTimeRecords.size() > 0 ? transactionTimeRecords.firstElement(): n) +"\" />\n";
//TODO: should this > 1 ?
         resultTableString += "<param name=\"Tg4\" value=\""
            + (transactionTimeRecords.size() > 1 ? transactionTimeRecords.lastElement() : n) +"\" />\n";

showln("transactionTimeRecords.size="+transactionTimeRecords.size());
showln("pttSessionTimeRecords.size="+pttSessionTimeRecords.size());

         int nptts = pttSessionTimeRecords.size();
         if (nptts != 0) {
            resultTableString += "<param name=\"Tg5\" value=\""
               + pttSessionTimeRecords.get(0) + "\" />\n";
            if( nptts > 1)
            resultTableString += "<param name=\"Tg6\" value=\""
               + pttSessionTimeRecords.get(1) + "\" />\n";
            if( nptts > 2)
            resultTableString += "<param name=\"Tg7\" value=\""
               + pttSessionTimeRecords.get(2) + "\" />\n";
            if( nptts > 3)
            resultTableString += "<param name=\"Tg8\" value=\""
               + pttSessionTimeRecords.get(3) + "\" />\n";
            if( nptts > 4)
            resultTableString += "<param name=\"Tgrant_CallingISSI\" value=\""
               + pttSessionTimeRecords.get(4) + "\" />\n";
            if( nptts > 5)
            resultTableString += "<param name=\"Tgrant_CalledISSI\" value=\""
               + pttSessionTimeRecords.get(5) + "\" />\n";
         } else {
            resultTableString += "<param name=\"Tg5\" value=\"" + n + "\" />\n";
            resultTableString += "<param name=\"Tg6\" value=\"" + n + "\" />\n";
            resultTableString += "<param name=\"Tg7\" value=\"" + n + "\" />\n";
            resultTableString += "<param name=\"Tg8\" value=\"" + n + "\" />\n";
            resultTableString += "<param name=\"Tgrant_CallingISSI\" value=\"" + n + "\" />\n";
            resultTableString += "<param name=\"Tgrant_CalledISSI\" value=\"" + n + "\" />\n";
         }
showln("   resultTableString="+resultTableString);
      } 
      else if ( SipUtils.isSubscriberUnit(userTag))
      {
	 long value = n;
	 if( temp[2][0] != -1 && temp[0][0] != -1)
            value = temp[2][0] - temp[0][0];

//TODO: should check the size of msgRelay before get() ?
         if( msgRelay.size() > 0)
         resultTableString += "<param name=\"Tu2\" value=\""
               + msgRelay.firstElement() + "\" />\n";

         if( msgRelay.size() > 1)
         resultTableString += "<param name=\"Tu3\" value=\""
               + msgRelay.get(1) + "\" />\n";

         if( transactionTimeRecords.size() > 0)
         resultTableString += "<param name=\"Tu6\" value=\""
               + transactionTimeRecords.lastElement() + "\" />\n";

         if( msgRelay.size() > 2)
         resultTableString += "<param name=\"Tu7\" value=\""
               + msgRelay.get(2) + "\" />\n";

         if( msgRelay.size() > 3)
         resultTableString += "<param name=\"Tu8\" value=\""
               + msgRelay.get(3) + "\" />\n";

         if( dialogTimeRecords.size() > 0)
         resultTableString += "<param name=\"Tu10\" value=\""
               + dialogTimeRecords.lastElement() + "\" />\n";

         if( msgRelay.size() > 4)
         resultTableString += "<param name=\"Tu11\" value=\""
               + msgRelay.get(4) + "\" />\n";

         if( msgRelay.size() > 5)
         resultTableString += "<param name=\"Tu12\" value=\""
               + msgRelay.lastElement() + "\" />\n";

         if( transactionTimeRecords.size() > 0)
         resultTableString += "<param name=\"Tugrant_CallingISSI\" value=\""
               + transactionTimeRecords.firstElement() + "\" />\n";
         resultTableString += "<param name=\"Tugrant_CalledISSI\" value=\""
               + value + "\" />\n";
      }
      resultTableString += "</parameter-list>\n";
showln("   resultTableString="+resultTableString);
   }
}
