//
package gov.nist.p25.issi.packetmonitor;

import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.packetmonitor.EndPoint;

import javax.sip.header.CallIdHeader;
import javax.sip.header.TimeStampHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;

/**
 * Represents a catptured SIP Message.
 */
public class CapturedSipMessage {

   private int packetNumber;
   private long timeStamp;
   private Message message;
   private EndPoint destination;
   private PacketMonitor packetMonitor;

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

   // helper
   public String getShortDescription() {
      String firstLine = ((SIPMessage) message).getFirstLine();
      return firstLine;
   }

   // constructor
   public CapturedSipMessage(PacketMonitor packetMonitor, EndPoint destination,
         long timeStamp, Message message) {
      this.packetMonitor = packetMonitor;
      this.destination = destination;
      this.timeStamp = timeStamp;
      this.message = message;
   }

   public String toString() {

      String firstLine = ((SIPMessage) message).getFirstLine();
      String source = null;
      ViaHeader viaHeader = (ViaHeader) message.getHeader(ViaHeader.NAME);
      String tid = viaHeader.getBranch();
      RfssConfig destinationRfss = packetMonitor.getTopologyConfig().getRfssConfig(
            destination.getHost(), destination.getPort());
      String destinationRfssDomainName = destinationRfss.getDomainName();
      //int destinationRfssId = destinationRfss.getRfssId();

      if (!firstLine.startsWith(("SIP"))) {
         source = viaHeader.getHost();
         packetMonitor.getTransactionTable().put(tid, destinationRfssDomainName);
         //System.out.println("CapturedSipMessage(1): tid=" +tid +" source=" +source);
      } else {
         source = packetMonitor.getTransactionTable().get(tid);
         if (source == null) {
            String msg = "Could not find source for transaction id " + tid +
                         "\nmessage = [" + message + "]";
            //PacketMonitor.logger.error( msg);
            //this.packetMonitor.errorFlag = true;
            //this.packetMonitor.errorString = msg;
	    packetMonitor.logError( msg);
            return "";
         }
         //System.out.println("CapturedSipMessage(2): tid=" +tid +" source=" +source);
      }

      RfssConfig rfssConfig = packetMonitor.getTopologyConfig().getRfssConfig(source);

      if (rfssConfig == null) {
         String msg = "Could not find rfss config for " + source;
         //PacketMonitor.logger.error( msg);
         //this.packetMonitor.errorString = msg;
         //this.packetMonitor.errorFlag = true;
	 packetMonitor.logError( msg);
         return "";
      }
      String sourceHostPort = rfssConfig.getIpAddress() + ":"
            + rfssConfig.getSipPort();

      String sourceRfssDomainName = rfssConfig.getDomainName();
      // int sourceRfssId = rfssConfig.getRfssId();
      boolean isSender = false;

      long timeStampHeaderValue = 0;
      if (message.getHeader(TimeStampHeader.NAME) != null) {
         TimeStampHeader tsHeader = (TimeStampHeader) message
               .getHeader(TimeStampHeader.NAME);
         timeStampHeaderValue = tsHeader.getTime();
      }
      String callId = ((CallIdHeader) message
            .getHeader(CallIdHeader.NAME)).getCallId();

      if (sourceRfssDomainName.equals(destinationRfssDomainName)) {
         PacketMonitor.logger.debug("Not logging self loop");
         return "";
      }
      String log = "<message\n from=\"" + sourceHostPort
            + "\"\n fromRfssId=\"" + sourceRfssDomainName
            + "\"\n to=\"" + destination
            + "\"\n toRfssId=\"" + destinationRfssDomainName
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
