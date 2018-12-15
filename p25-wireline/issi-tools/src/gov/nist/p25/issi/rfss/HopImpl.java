//
package gov.nist.p25.issi.rfss;

import javax.sip.address.Hop;

public class HopImpl implements Hop {
   private String host;
   private int port;

   public HopImpl(String ipAddress, int port) {
      this.host = ipAddress;
      this.port = port;
   }

   public String getHost() {
      return host;
   }

   public int getPort() {
      return port;
   }

   public String getTransport() {      
      return "udp";
   }
}
