//
package gov.nist.p25.issi.rfss;

import gov.nist.core.net.AddressResolver;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.sip.address.Hop;
import org.apache.log4j.Logger;

public class RfssAddressResolver implements AddressResolver {
   private static Logger logger = Logger.getLogger(RfssAddressResolver.class);

   class MyHop implements Hop {
      private String host;
      private int port;

      public MyHop(String host, int port) {
         this.host = host;
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

      @Override
      public String toString() {
         return host + ":" + port + ":" + getTransport();
      }
   }

   private TopologyConfig topologyConfig;

   public RfssAddressResolver(TopologyConfig topologyConfig) {
      this.topologyConfig = topologyConfig;
   }

   /**
    * Allows us to map an Rfss radical name to IP address and port so we can
    * send request/response to it.
    */
   public Hop resolveAddress(Hop hop) {
      String radicalName = hop.getHost();
      RfssConfig rfssConfig = topologyConfig.getRfssConfig(radicalName);
      if (rfssConfig != null) {
         MyHop retval = new MyHop(rfssConfig.getIpAddress(), rfssConfig.getSipPort());
         logger.debug("resolveAddress : " + radicalName + " returning " + retval);
         return retval;
      } else
         return hop;
   }

   /**
    * Resolve the Radical name to an IP addres or throw unknown host if not
    * found.
    * 
    * @param address
    * @return the resolved address.
    */
   public InetAddress resolveAddress(String address) throws UnknownHostException {
      RfssConfig rfssConfig = topologyConfig.getRfssConfig(address);
      if (rfssConfig != null) {
         String ipAddress = rfssConfig.getIpAddress();
         return InetAddress.getByName(ipAddress);
      } else {
         return InetAddress.getByName(address);
      }
   }
}
