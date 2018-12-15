//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.issiconfig.DaemonWebServerAddress;
import gov.nist.p25.issi.issiconfig.PacketMonitorWebServerAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * This class defines where the web servers that launch the emulated RFSSs live.
 * 
 */
public class ISSITesterConfiguration {

   private static Logger logger = Logger.getLogger(ISSITesterConfiguration.class);
   //public static void showln(String s) { System.out.println(s); }

   private Hashtable<String, DaemonWebServerAddress> addressMap = 
      new Hashtable<String, DaemonWebServerAddress>();
   private HashSet<DaemonWebServerAddress> addresses = new HashSet<DaemonWebServerAddress>();

   // constructor
   public ISSITesterConfiguration() {
   }

   public Collection<EmulatorWebServerAddress> getEmulatorConfigurations() {
      HashSet<EmulatorWebServerAddress> retval = new HashSet<EmulatorWebServerAddress>();
      for (DaemonWebServerAddress dwsa: addresses) {
         if (dwsa.isConformanceTester()) {
            retval.add(new EmulatorWebServerAddress(dwsa.getIpAddress(), dwsa.getHttpPort()));
         }
      }
      //logger.debug("getEmulatorConfigurations():\n"+retval);
      return retval;
   }

   //public Collection<PacketMonitorWebServerAddress> getPacketMonitors() {
   public HashSet<PacketMonitorWebServerAddress> getPacketMonitors() {
      HashSet<PacketMonitorWebServerAddress> retval = new HashSet<PacketMonitorWebServerAddress>();
      for (DaemonWebServerAddress dwsa: addresses) {
         if (dwsa.isPacketMonitor()) {
            retval.add(new PacketMonitorWebServerAddress(dwsa.getIpAddress(), dwsa.getHttpPort()));
         }
      }
      return retval;
   }

   public PacketMonitorWebServerAddress getPacketMonitor(String ipAddress) {
      for (DaemonWebServerAddress dwsa: addresses) {
         if (ipAddress.trim().equals(dwsa.getIpAddress()) && dwsa.isPacketMonitor()) {
            logger.debug("ISSITesterConfiguration: Found configuration for " + ipAddress);
            return new PacketMonitorWebServerAddress(dwsa.getIpAddress(), dwsa.getHttpPort());
         }
      }
      return null;
   }

   public void printPacketMonitorTable() {
      logger.info(getPacketMonitors().toString());
   }

   public String getIpAddressByName(String name) {
      DaemonWebServerAddress dwsa = getDaemonWebServerAddressByName(name);
      return (dwsa == null ? null : dwsa.getIpAddress());
   }

   public DaemonWebServerAddress getDaemonWebServerAddressByName(String name) {
      return addressMap.get(name);
   }

   //public Collection<DaemonWebServerAddress> getDaemonAddresses() {
   public HashSet<DaemonWebServerAddress> getDaemonWebServerAddresses() {
      return addresses;
   }

   public List<DaemonWebServerAddress> getDaemonWebServerAddressByIpAddress(String ipAddress) {
      ArrayList<DaemonWebServerAddress> list = new ArrayList<DaemonWebServerAddress>();
      for (DaemonWebServerAddress dwsa: addresses) {
         if (dwsa.getIpAddress().equals(ipAddress)) {
            logger.debug("ISSITesterConfiguration: Found DWSA for " + ipAddress);
            list.add( dwsa);
         }
      }
      return list;
   }

   public Collection<String> getLocalAddresses() {
      HashSet<String> retval = new HashSet<String>();
      for (DaemonWebServerAddress dwsa: addresses) {
         for (int port = (int) (Math.random() * 10000), j = 0; j < 10; j++) {
            String ipStr = dwsa.getIpAddress();
            //showln("getLocalAddresses(): ipStr="+ipStr +" port="+port);
            if (ipStr != null) {
               try {
                  Socket sock = new Socket();
                  sock.bind(new InetSocketAddress( ipStr, port));
                  sock.close();
                  retval.add( ipStr);
                  break;
               } catch (IOException ex) {
                  // ignore 
                  //showln("getLocalAddresses(): ex="+ex);
               }
            }
         }
      }
      logger.debug("Local addresses:\n" + retval);
      return retval;
   }

   public Collection<DaemonWebServerAddress> getLocalConfigurations() {
      HashSet<DaemonWebServerAddress> retval = new HashSet<DaemonWebServerAddress>();
      for (DaemonWebServerAddress dwsa: addresses) {
         if (dwsa.getHttpPort() > 0) {
            for (int port = (int) (Math.random() * 10000), j = 0; j < 10; j++) {
               try {
                  Socket sock = new Socket();
                  sock.bind(new InetSocketAddress(dwsa.getIpAddress(), port));
                  sock.close();
                  retval.add(dwsa);
                  break;
               } catch (IOException ex) {
                  // ignore 
                  //showln("getLocalConfigurations(): ex="+ex);
               }
            }
         }
      }
      return retval;
   }

   public void addDaemonWebServerAddress(DaemonWebServerAddress dwsa) {
      addresses.add(dwsa);
      addressMap.put(dwsa.getName(), dwsa);
   }
   public void removeDaemonWebServerAddress(DaemonWebServerAddress dwsa) {
      addresses.remove(dwsa);
      addressMap.remove(dwsa.getName());
   }


   public void addRefId(String id, DaemonWebServerAddress dwsa) {
      //TODO: why ?
      addressMap.put(id, dwsa);
      dwsa.addRefId(id);
   }
   public void removeRefId(String id) {
      // id = rfss_4.daemon
      for (DaemonWebServerAddress dwsa: addresses) {
         dwsa.removeRefId(id);
      }
      //TODO: why ?
      addressMap.remove(id);      
   }
   
   @Override
   public String toString() {
      return toString(true);
   }
   public String toString(boolean header) {
      String preamble = "<?xml version=\"1.0\"?>\n" +
         "<!DOCTYPE system-topology SYSTEM \"" + 
         ISSIDtdConstants.URL_ISSI_DTD_TESTER_CONFIG + "\">\n";

      StringBuffer sbuf = new StringBuffer();
      if( header)
         sbuf.append( preamble);
      sbuf.append("<issi-tester-config>\n");      
      for ( DaemonWebServerAddress dwsa: addresses) {
         sbuf.append(dwsa.toString());
      }      
      sbuf.append("</issi-tester-config>\n");
      return sbuf.toString();      
   }
}
