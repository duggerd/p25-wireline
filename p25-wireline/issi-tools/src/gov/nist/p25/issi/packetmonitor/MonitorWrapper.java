//
package gov.nist.p25.issi.packetmonitor;

import jpcap.PacketReceiver;
import jpcap.packet.Packet;

/**
 * Monitor Wrapper
 */
public class MonitorWrapper implements PacketReceiver {
   
   private PacketMonitor actualMonitor;

   // accessor
   public PacketMonitor getActualMonitor() {
      return actualMonitor;
   }
   public void setActualMonitor(PacketMonitor actualMonitor) {
      this.actualMonitor = actualMonitor;
   }
   
   // constructor
   public MonitorWrapper() {
   }
   
   public MonitorWrapper(PacketMonitor monitor) {
      this.actualMonitor = monitor;
   }
   
   public void receivePacket(Packet packet) {
      if (actualMonitor != null) {
         actualMonitor.receivePacket(packet);
      }
   }
}

