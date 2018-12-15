//
package gov.nist.p25.issi.traceviewer;

/**
 * Corresponds to information retrieved from the Ptt session
 * 
 */
public class PttSessionInfo {
   
   private String sessionType;
   private String rfssId;
   private String myRtpRecvPort;
   private String remoteIpAddress;
   private String remoteRtpRecvPort;
   private String unitId;
   private String linkType;
   
   // accessors
   public String getSessionType() {
      return sessionType;
   }
   public void setSessionType(String sessionType) {
      this.sessionType = sessionType;
   }

   public String getRfssId() {
      return rfssId;
   }
   public void setRfssId(String rfssId) {
      this.rfssId = rfssId;
   }

   public String getMyRtpRecvPort() {
      return myRtpRecvPort;
   }
   public void setMyRtpRecvPort(String myRtpRecvPort) {
      this.myRtpRecvPort = myRtpRecvPort;
   }

   public String getRemoteRtpRecvPort() {
      return remoteRtpRecvPort;
   }
   public void setRemoteRtpRecvPort(String remoteRtpRecvPort) {
      this.remoteRtpRecvPort = remoteRtpRecvPort;
   }

   public String getRemoteIpAddress() {
      return remoteIpAddress;
   }
   public void setRemoteIpAddress(String remoteIpAddress) {
      this.remoteIpAddress = remoteIpAddress;
   }

   public String getUnitId() {
      return unitId;
   }
   public void setUnitId(String unitId) {
      this.unitId = unitId;
   }

   public String getLinkType() {
      return linkType;
   }
   public void setLinkType(String linkType) {
      this.linkType = linkType;
   }

   // constructor
   public PttSessionInfo() {
   }

   public PttSessionInfo(String sessionType, String rfssId,
         String myRtpRecvPort, String remoteRtpRecvPort,
         String remoteIpAddress, String unitId, String linkType ) {
      this.sessionType = sessionType;
      this.rfssId = rfssId;
      this.myRtpRecvPort = myRtpRecvPort;
      this.remoteRtpRecvPort = remoteRtpRecvPort;
      this.remoteIpAddress = remoteIpAddress;
      this.unitId = unitId;
      this.linkType = linkType;
   }
   
   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("\n  sessionType=" + sessionType);
      sb.append("\n  myRtpRecvPort=" + myRtpRecvPort);
      sb.append("\n  remoteRtpRecvPort=" + remoteRtpRecvPort);
      sb.append("\n  remoteIpAddress=" + remoteIpAddress);
      sb.append("\n  unitId=" + unitId);
      sb.append("\n  linkType=" + linkType);
      return sb.toString();
   }

}
