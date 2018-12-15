//
package gov.nist.p25.issi.analyzer.bo;

import gov.nist.javax.sip.message.SIPMessage;

import javax.sip.header.CallIdHeader;
import javax.sip.header.TimeStampHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;

import org.apache.log4j.Logger;

//===import gov.nist.p25.issi.analyzer.vo.EndPoint;
import gov.nist.p25.issi.packetmonitor.EndPoint;


/**
 * Represents a catptured SIP Message.
 */
public class CapturedSipMessage {

   private static Logger logger = Logger.getLogger(CapturedSipMessage.class);

   //private boolean verbose = false;
   //private boolean errorFlag;
   //private String errorMessage;

   private int packetNumber;
   private long timeStamp;
   private Message message;
   private EndPoint endPoint;
   private MessageProcessor processor;

   // accessor
   public int getPacketNumber() {
      return packetNumber;
   }
   public void setPacketNumber(int packetNumber) {
      this.packetNumber = packetNumber;
   }

   public long getTimeStamp() {
      return timeStamp;
   }
   public void setTimeStamp(long timeStamp) {
      this.timeStamp = timeStamp;
   }

   public Message getMessage() {
      return message;
   }
   public void setMessage(Message message) {
      this.message = message;
   }
   // raw data from SIP message
   public byte[] getRawContent() {
      return getMessage().getRawContent();
   }

   public EndPoint getEndPoint() {
      return endPoint;
   }

   // helper
   public String getShortDescription() {
      String firstLine = ((SIPMessage) message).getFirstLine();
      return firstLine;
   }

   // constructor
   public CapturedSipMessage( EndPoint endPoint, long timeStamp,
      Message message, MessageProcessor processor)
   {
      this.endPoint = endPoint;
      this.timeStamp = timeStamp;
      this.message = message;
      this.processor = processor;
   }

   public String toString() {

      String firstLine = ((SIPMessage) message).getFirstLine();
      String source = null;
      ViaHeader viaHeader = (ViaHeader) message.getHeader(ViaHeader.NAME);
      String tid = viaHeader.getBranch();

      String endPointRfssDomainName = processor.getRfssDomainName(
            endPoint.getHost(), endPoint.getPort());

      if (!firstLine.startsWith(("SIP"))) {
         source = viaHeader.getHost();
         // branch -> domainName
         processor.putTransactionTable(tid, endPointRfssDomainName);

         //if( verbose)
	 //   logger.info("CapturedSipMessage(11): tid=" +tid +" source=" +source +
         //   "  dstDN="+ endPointRfssDomainName);
      } else {
         source = processor.getTransactionTable(tid);
         if (source == null) {
            String msg = "Could not find source for transaction id "
                         + tid + "\nmessage = [" + message + "]";
            logger.error( msg);
            //errorFlag = true;
            //errorMessage = msg;
            return "";
         }
         //if( verbose)
	 //   logger.info("CapturedSipMessage(12): tid=" +tid +" source=" +source +
         //   "  dstDN="+ endPointRfssDomainName);
      }

      String sourceRfssDomainName = source;
      String sourceHostPort = processor.getRfssHostPort(source);

      boolean isSender = false;
      long timeStampHeaderValue = 0;
      TimeStampHeader tsHeader = (TimeStampHeader)
          message.getHeader(TimeStampHeader.NAME);
      if ( tsHeader != null) {
         timeStampHeaderValue = tsHeader.getTime();
      }
      String callId = ((CallIdHeader) message
         .getHeader(CallIdHeader.NAME)).getCallId();

      if (sourceRfssDomainName.equals(endPointRfssDomainName)) {
         logger.debug("Not logging self loop");
         return "";
      }

      String log = "<message\n from=\"" + sourceHostPort
            + "\"\n fromRfssId=\"" + sourceRfssDomainName
            + "\"\n to=\"" + endPoint
            + "\"\n toRfssId=\"" + endPointRfssDomainName
            + "\"\n time=\"" + timeStamp
            + "\"\n packetNumber=\"" + getPacketNumber()
            + "\""
            + (timeStampHeaderValue != 0 ? "\n timeStamp=\""
                  + timeStampHeaderValue + "\"" : "")
            + "\n isSender=\"" + isSender 
            + "\"\n transactionId=\"" + tid 
            + "\"\n callId=\"" + callId 
            + "\"\n firstLine=\"" + firstLine.trim() 
            + "\" \n>\n";
      log += "<![CDATA[\n";
      log += message;
      log += "]]>\n";
      log += "</message>\n";
      return log;
   }
}
