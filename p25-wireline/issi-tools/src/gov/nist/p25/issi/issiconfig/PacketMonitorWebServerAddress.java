//
package gov.nist.p25.issi.issiconfig;

/**
 * Configuration file for packet monitor.
 */
public class PacketMonitorWebServerAddress implements WebServerAddress {
   private String host;
   private int port;

   public PacketMonitorWebServerAddress(String httpAddress, int port) {
      this.host = httpAddress;
      this.port = port;
   }

   public String getIpAddress() {
      return host;
   }

   public int getHttpPort() {
      return port;
   }

   public String getHttpControlUrl() {
      return "http://" + host + ":" + port + "/sniffer/controller";
   }
   
   @Override
   public String toString() {
      return "[ HOST = " + host + " HTTP Port = " + port + " ]";
   }
}
