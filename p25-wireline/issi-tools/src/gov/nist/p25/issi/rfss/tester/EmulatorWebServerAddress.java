//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.WebServerAddress;


/**
 * Emulator Web Server Address
 */
public class EmulatorWebServerAddress implements WebServerAddress {

   private String ipAddress;
   private int httpPort;

   public EmulatorWebServerAddress(String ipAddress, int port) {
      this.ipAddress = ipAddress;
      this.httpPort = port;
   }

   public String getIpAddress() {
      return ipAddress;
   }

   public int getHttpPort() {
      return  httpPort;      
   }

   public String getHttpControlUrl() {
      return "http://" + ipAddress + ":" + httpPort + "/rfss/control";
   }
}
