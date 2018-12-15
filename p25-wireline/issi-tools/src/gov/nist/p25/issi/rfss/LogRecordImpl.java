//
package gov.nist.p25.issi.rfss;

import gov.nist.javax.sip.LogRecord;

/**
 * The record is used by the SIP stack to record messages to the trace log.
 * 
 */
public class LogRecordImpl implements LogRecord {

   private boolean isSender;
   private long timeStamp;
   private long timeStampHeaderValue;
   private String fromRfssId;
   private String toRfssId;
   private String message;
   private String source;
   private String destination;
   private String firstLine;
   private String tid;
   private String callId;
   
   public void setFromRfssId (String fromRfssId) {
      this.fromRfssId = fromRfssId;
   }
   
   public void setToRfssId(String toRfssId) {
      this.toRfssId = toRfssId;
   }
   
   public boolean equals(Object other) {
      if (!(other instanceof LogRecordImpl)) {
         return false;
      } else {
         LogRecordImpl otherLog = (LogRecordImpl) other;
         return otherLog.message.equals(message)
            && otherLog.timeStamp == timeStamp;
      }
   }
   
   // constructor
   public LogRecordImpl(String message,
         String source, 
         String destination, 
         long timeStamp, 
         boolean isSender, 
         String firstLine, 
         String tid, 
         String callId, 
         long timeStampHeaderVal) {
      this.message = message;
      this.source = source;
      this.destination = destination;
      this.timeStamp = timeStamp;
      this.firstLine = firstLine;
      this.tid = tid;
      this.callId = callId;
      this.timeStampHeaderValue = timeStampHeaderVal;
      this.timeStamp = timeStamp;
      this.isSender = isSender;
   }
   
   public String toString() {
      String log = "<message\n from=\"" + source
            + "\"\n fromRfssId=\"" + fromRfssId
            + "\"\n to=\"" + destination
            + "\"\n toRfssId=\"" + toRfssId
            + "\"\n time=\"" + timeStamp
            + "\"" + 
            (timeStampHeaderValue != 0 ? "\n timeStamp=\"" 
                + timeStampHeaderValue + "\"": "")   
            +"\n isSender=\"" + isSender
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
