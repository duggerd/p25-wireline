//
package gov.nist.p25.issi.issiconfig;

import java.util.Collection;
import java.util.HashSet;

/**
 * This class represents a machine on which an emulator or packet monitor
 * (or both) runs. The emulator is signaled using http.
 */
public class DaemonWebServerAddress implements WebServerAddress {
   public static void showln(String s) { System.out.println(s); }

   private String name;
   private String ipAddress;
   private int httpPort;
   private boolean conformanceTester;
   private boolean packetMonitor;
   private HashSet<String> refId;

   // accessors
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }

   public String getIpAddress() {
      return ipAddress;
   }
   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public int getHttpPort() {
      return httpPort;
   }
   public void setHttpPort(int httpPort) {
      this.httpPort = httpPort;
   }

   public boolean isConformanceTester() {
      return conformanceTester;
   }
   public void setConformanceTester(boolean conformanceTester) {
      this.conformanceTester = conformanceTester;
   }

   public boolean isPacketMonitor() {
      return packetMonitor;
   }
   public void setPacketMonitor(boolean packetMonitor) {
      this.packetMonitor = packetMonitor;
   }

   public int getRefIdCount() {
      return refId.size();
   }
   public HashSet<String> getRefIds() {
      return refId;
   }
   public void addRefId(String id) {
      refId.add(id);
   }
   public void removeRefId(String id) {
      refId.remove(id);
   }
   public void clearRefIds() {
      refId.clear();
   }

   public boolean isReferencedByRfssName(String rfssName) {
      // tag= rfss_2.domain
      for(String tag: getRefIds()) {
         //showln("   isReferencedByRfssName: tag="+tag);
         String[] parts = tag.split("\\.");
         if(parts.length == 2) {
            if(parts[0].equals(rfssName)) {
               return true;
            }
         }
      }
      return false;
   }
   
   // constructor
   public DaemonWebServerAddress(String name, String ipAddress, int httpPort,
      boolean conformanceTester, boolean packetMonitor) {
      this.name = name;
      this.ipAddress = ipAddress;
      this.httpPort = httpPort;
      this.conformanceTester = conformanceTester;
      this.packetMonitor = packetMonitor;
      this.refId = new HashSet<String>();
   }
   public DaemonWebServerAddress(DaemonWebServerAddress dwsa) {
      this.name = new String(dwsa.getName());
      this.ipAddress = new String(dwsa.getIpAddress());
      this.httpPort = dwsa.getHttpPort();
      this.conformanceTester = dwsa.isConformanceTester();
      this.packetMonitor = dwsa.isPacketMonitor();
      this.refId = new HashSet<String>(dwsa.getRefIds());
   }

   public String getHttpControlUrl() {
      return "http://" + ipAddress + ":" + httpPort + "/diets/controller";
   }
   
   @Override
   public String toString() {
      String retval = "<diets-daemon\n" +
         "\tipAddress=\"" + getIpAddress() + "\"\n" +
         "\thttpPort=\"" + getHttpPort() + "\"\n" +
         "\tname=\"" + getName() + "\"\n" +
         "\tisConformanceTester=\"" + isConformanceTester()+ "\"\n" +
         "\tisPacketMonitor=\"" + isPacketMonitor() + "\"\n" +
         ">\n";
      for ( String id: refId) {
         retval += "<refid\n" + "\tid=\"" + id + "\"\n" + "/>\n";
      }
      retval += "</diets-daemon>\n";
      return retval;
   }
}
