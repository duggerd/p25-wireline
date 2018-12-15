//
package gov.nist.p25.issi.analyzer.bo;

import java.util.LinkedList;
import org.apache.log4j.Logger;

//===import gov.nist.p25.issi.analyzer.vo.EndPoint;
import gov.nist.p25.issi.packetmonitor.EndPoint;
import gov.nist.p25.issi.p25payload.P25BlockException;

/**
 * Represents a Media session. Media sessions are inferred from
 * the SDP information of the SIP request and matching response.
 * A media session is an RTP session between two EndPoints.
 * The owningRfss field represents the RFSS that created the
 * media session.
 *
 */
public class MediaSession {

   public static Logger logger = Logger.getLogger(MediaSession.class);
   //public static void showln(String s) { System.out.println(s); }

   private String owningRfssDomainName = "";
   private String remoteRfssDomainName = "";
   private EndPoint source;
   private EndPoint destination;
   private LinkedList<CapturedPttMessage> capturedPttMessages =
         new LinkedList<CapturedPttMessage>();
   
   // accessor
   public String getOwningRfssId() {
      String[] parts = owningRfssDomainName.split("\\.");
      //showln("parts.length="+parts.length+" owningDN="+owningRfssDomainName);
      if( parts.length==4) {
         String rfssId = "" + Integer.parseInt(parts[0], 16);
         return rfssId;
      }
      return owningRfssDomainName;
   }
   //public void setOwningRfss(String owningRfss) {
   //   this.owningRfss = owningRfss;
   //}

   public String getOwningRfssDomainName() {
      return owningRfssDomainName;
   }
   public void setOwningRfssDomainName(String owningRfssDomainName) {
      this.owningRfssDomainName = owningRfssDomainName;
   }

   public String getRemoteRfssId() {
      String[] parts = remoteRfssDomainName.split("\\.");
      //showln("parts.length="+parts.length+" remoteDN="+remoteRfssDomainName);
      if( parts.length==4) {
         String rfssId = "" + Integer.parseInt(parts[0], 16);
         return rfssId;
      }
      return remoteRfssDomainName;
   }
   //public void setRemoteRfss(String remoteRfss) {
   //   this.remoteRfss = remoteRfss;
   //}

   public String getRemoteRfssDomainName() {
      return remoteRfssDomainName;
   }
   public void setRemoteRfssDomainName(String remoteRfssDomainName) {
      this.remoteRfssDomainName= remoteRfssDomainName;
   }

   public EndPoint getSource() {
      return source;
   }
   public void setSource(EndPoint source) {
      this.source = source;
   }

   public EndPoint getDestination() {
      return destination;
   }
   public void setDestination(EndPoint destination) {
      this.destination = destination;
   }

   public LinkedList<CapturedPttMessage> getCapturedPttMessages() {
      return capturedPttMessages;
   }

   // constructor
   private MediaSession( ) {
      // used locally
   }

   public MediaSession( EndPoint destination) {
      setDestination( destination);
   }

   public CapturedPttMessage addPttMessage(int packetNumber, byte[] message, long time)
      throws P25BlockException
   {
      CapturedPttMessage newMessage = new CapturedPttMessage(packetNumber, message, time, this);
      if (capturedPttMessages.size() == 0) {
         capturedPttMessages.add(newMessage);
      }
      else {
         CapturedPttMessage previousMessage = capturedPttMessages.peekLast();
         if (previousMessage.match(newMessage)) {
            logger.debug("Previous PTT matches current -- dropping message");
	       
            //if( packetMonitor.getKeepDuplicateMessage()) {
            //   capturedPttMessages.add(newMessage);                  
            //}
         } 
         else {
            capturedPttMessages.add(newMessage);
         }
      }
      return newMessage;
   }

   public MediaSession reverse()
   {
      MediaSession retval = new MediaSession();
      retval.source = this.destination;
      retval.destination = this.source;
      //retval.owningRfss = this.remoteRfss;
      retval.owningRfssDomainName = this.remoteRfssDomainName;
      //retval.remoteRfss = this.owningRfss;
      retval.remoteRfssDomainName = this.owningRfssDomainName;
      return retval;
   }

   public String toString() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("<ptt-session");
      sbuf.append("\n rfssId=\"" + getRemoteRfssId());
      sbuf.append("\"\n");
      sbuf.append(" myIpAddress=\"" + (destination != null ? destination.getHost() : ""));
      sbuf.append("\"\n");
      sbuf.append(" myRtpRecvPort=\"" + (destination != null ? destination.getPort() : -1));
      sbuf.append("\"\n");
      sbuf.append(" remoteIpAddress=\"" + (source != null ? source.getHost() : ""));
      sbuf.append("\"\n");
      sbuf.append(" remoteRtpRecvPort=\"" + (source != null ? source.getPort() : -1));
      sbuf.append("\"\n");
      sbuf.append("/>\n");
      return sbuf.toString();
   }
}
