//
package gov.nist.p25.issi.packetmonitor;

import org.apache.log4j.Logger;

import gov.nist.p25.common.util.ByteArrayUtil;
import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.p25.issi.p25payload.P25BlockException;
import gov.nist.p25.issi.p25payload.PacketType;
import gov.nist.rtp.RtpPacket;

/**
 * Represents a captured PTT message.
 */
public class CapturedPttMessage {

   private static Logger logger = Logger.getLogger(CapturedPttMessage.class);

   private int packetNumber;
   private long timeStamp;
   private byte[] rawContent;
   private MediaSession mediaSession;
   private P25Payload p25Payload;
   private RtpPacket rtpPacket;

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

   public byte[] getRawContent() {
      return rawContent;
   }
   public void setRawContent(byte[] rawContent) {
      this.rawContent = rawContent;
   }

   public MediaSession getMediaSession() {
      return mediaSession;
   }

   public P25Payload getP25Payload() {
      return p25Payload;
   }

   public RtpPacket getRtpPacket() {
      return rtpPacket;
   }

   // helper
   public String getShortDescription() {
      PacketType ptype = getP25Payload().getISSIPacketType().getPacketType();
      //int type = getP25Payload().getISSIPacketType().getPT();
      return ptype.toString();
   }

   // constructor
   /***
   public CapturedPttMessage(byte[] rawContent, long timeStamp,
         MediaSession mediaSession) throws P25BlockException
   {
      this( 0, rawContent, timeStamp, mediaSession);
   }
   **/
   public CapturedPttMessage(int packetNumber, byte[] rawContent, long timeStamp, 
      MediaSession mediaSession) throws P25BlockException
   {
      this.packetNumber = packetNumber;
      this.rawContent = rawContent;
      this.timeStamp = timeStamp;
      this.mediaSession = mediaSession;

      rtpPacket = new RtpPacket(rawContent, rawContent.length);
      byte[] payload = rtpPacket.getPayload();
      p25Payload = new P25Payload(payload);
   }

   /**
    * Compare a captured packet against a template packet. 
    * This is for confromance testing.
    * 
    * @param template
    * @return
    */
   public boolean match(CapturedPttMessage template) {
      //*** assert template != this;

      RtpPacket rtpPacket = template.rtpPacket;
      if (!(rtpPacket.getV() == this.rtpPacket.getV()
            && rtpPacket.getP() == this.rtpPacket.getP()
            && rtpPacket.getX() == this.rtpPacket.getX()
            && rtpPacket.getCC() == this.rtpPacket.getCC()
            && rtpPacket.getM() == this.rtpPacket.getM()
            && rtpPacket.getPT() == this.rtpPacket.getPT() 
            && rtpPacket.getSSRC() == this.rtpPacket.getSSRC())) {
         return false;
      }
      P25Payload templatePayload = template.p25Payload;
      boolean retval = false;
      try {
         if (!p25Payload.getISSIPacketType().equals(
               templatePayload.getISSIPacketType()))
            return retval = false;
         if (p25Payload.getISSIHeaderWord() == null
               ^ templatePayload.getISSIHeaderWord() == null)
            return retval = false;
         if (templatePayload.getISSIHeaderWord() != null
               && !templatePayload.getISSIHeaderWord().equals(
                     p25Payload.getISSIHeaderWord()))
            return retval = false;
         if (!p25Payload.getControlOctet().match(
               templatePayload.getControlOctet()))
            return retval = false;
         if (p25Payload.getPTTControlWord() == null
               ^ template.p25Payload.getPTTControlWord() == null)
            return retval = false;
         if (p25Payload.getPTTControlWord() != null
               && !p25Payload.getPTTControlWord().equals(
                     template.p25Payload.getPTTControlWord()))
            return retval = false;
         return retval = true;

      } finally {
         if ( retval ) {
            logger.debug("Match found for " + p25Payload.getISSIPacketType());
            logger.debug("templatePayload = " + templatePayload.getISSIPacketType());
         }
      }
   }

   @Override
   public String toString() {
      return toString(false);
   }
   public String toString(boolean incMessage) {
      StringBuffer sbuf = new StringBuffer();      
      //sbuf.append("\n<!-- PTT PACKET BEGIN -->");
      sbuf.append("\n<ptt-packet\n");
      //sbuf.append(" packetNumber=\"" + mediaSession.packetMonitor.packetNumber++);
      //sbuf.append(" packetNumber=\"" + mediaSession.getPacketNumber());
      sbuf.append(" packetNumber=\"" + getPacketNumber());
      sbuf.append("\"\n");
      sbuf.append(" receptionTime=\"" + timeStamp);
      sbuf.append("\"\n");
      sbuf.append(" receivingRfssId=\"" + mediaSession.getRemoteRfss().getDomainName());
      sbuf.append("\"\n");
      sbuf.append(" sendingRfssId=\"" + mediaSession.getOwningRfss().getDomainName());
      sbuf.append("\"\n");
      sbuf.append(" isSender=\"false\"\n");
      if( incMessage) {
         sbuf.append(" rawdata=\"");
         sbuf.append( ByteArrayUtil.toHexString( getRawContent(), 16));
         sbuf.append("\"\n");
      }
      sbuf.append(">\n");

      sbuf.append( mediaSession.toString());
      sbuf.append( rtpPacket.toString());
      sbuf.append( p25Payload.toString());
      sbuf.append("\n</ptt-packet>");
      return sbuf.toString();
   }
}
