//
//package gov.nist.p25.issi.rfss;
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.p25.issi.transctlmgr.ptt.PttSessionInterface;
import gov.nist.rtp.RtpPacket;

import java.io.Serializable;

//import org.apache.log4j.Logger;

/**
 * Represents a captured packet. This is a serializable object that is
 * written to disk during the trace gathering phase and read from disk 
 * during the testing phase.
 * 
 */
public class CapturedPttPacket implements Serializable {
   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger(CapturedPttPacket.class);

   /** Remote port. **/
   private int remotePort ; // record here as this may change.
    
   /** RTP packet that we are recording ( we header information ) **/   
   private RtpPacket rtpPacket;
   
   /** Payload for P25 packet **/
   private P25Payload p25Payload;

   /** True if the packet was recorded when it was sent else false */
   private boolean isSender;

   /** The session to which we belong */
   transient PttSessionInterface pttSession;

   /** When we were captured ( the time ) */
   private long captureTime;

   private int packetNumber;

   /* The remote IP address */
   private String remoteIpAddress;


   private String remoteRfssDomainName;
   
   /** The time the last packet was recorded. 
    * We increment by one on clashes to avoid issues for the trace visualizer
    */
   private static int counter;
   
   public CapturedPttPacket(RtpPacket rtpPacket, P25Payload payload,
         boolean isSender, PttSessionInterface session) {
      this.rtpPacket = rtpPacket;
      this.p25Payload = payload;
      this.isSender = isSender;
      this.pttSession = session;
      this.captureTime = System.currentTimeMillis();
      this.packetNumber = counter++;
      this.remotePort = session.getRtpSession().getRemoteRtpRecvPort();
      this.remoteIpAddress = session.getRtpSession().getRemoteIpAddress();
      this.remoteRfssDomainName = session.getRemoteRfssDomainName();
   }
   
   public int getRemotePort() {
      return this.remotePort;
   }
   public String getRemoteIpAddress() {
      return this.remoteIpAddress;
   }
   public String getRemoteRfssDomainName() {
      return this.remoteRfssDomainName;
   }

   public void flush() {
      pttSession.logPttPacket(rtpPacket, p25Payload, isSender, 
         captureTime, packetNumber,
         remoteRfssDomainName, remoteIpAddress, remotePort);
   }

   /**
    * Compare a captured packet against a template packet.
    * This is for confromance testing.
    * 
    * @param template
    * @return
    */
   public boolean match(CapturedPttPacket template) {
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
      
      if (!p25Payload.getISSIPacketType().equals(
            templatePayload.getISSIPacketType()))
         return false;
      if (p25Payload.getISSIHeaderWord() == null ^ templatePayload.getISSIHeaderWord() == null) 
         return false;
      if (templatePayload.getISSIHeaderWord() != null &&
            !templatePayload.getISSIHeaderWord().equals( p25Payload.getISSIHeaderWord()))
         return false;
      if (!p25Payload.getControlOctet().match(
            templatePayload.getControlOctet()))
         return false;
      if ( p25Payload.getPTTControlWord() == null ^ template.p25Payload.getPTTControlWord() == null)
         return false;
      if ( p25Payload.getPTTControlWord() != null && 
          !p25Payload.getPTTControlWord().equals(template.p25Payload.getPTTControlWord()))
         return false;
      return true;
   }
}
