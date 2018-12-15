//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.p25payload.P25Payload;
import gov.nist.rtp.RtpPacket;

/**
 * A template PTT message to test the actual run against the captured messages.
 * 
 */
public class TemplatePttMessage {
   /*
    * The Rtp header template
    */
   private RtpPacket rtpHeader;

   /*
    * The payload template
    */
   private P25Payload payLoad;

   /**
    * @param rtpHeader the rtpHeader to set
    */
   public void setRtpHeader(RtpPacket rtpHeader) {
      this.rtpHeader = rtpHeader;
   }

   /**
    * @return the rtpHeader
    */
   public RtpPacket getRtpHeader() {
      return rtpHeader;
   }

   /**
    * @param payLoad the payLoad to set
    */
    void setPayLoad(P25Payload payLoad) {
      this.payLoad = payLoad;
   }

   /**
    * @return the payLoad
    */
   public P25Payload getPayLoad() {
      return payLoad;
   }
}
