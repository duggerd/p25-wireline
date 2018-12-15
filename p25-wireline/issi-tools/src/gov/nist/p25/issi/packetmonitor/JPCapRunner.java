//
package gov.nist.p25.issi.packetmonitor;

import jpcap.JpcapCaptor;

/**
 * The wrapper for JPCAP. This is what drives the packet capture.
 */
public class JPCapRunner implements Runnable {

   private MonitorWrapper monitorWrapper;
   private JpcapCaptor jpcap;
   
   public JPCapRunner(MonitorWrapper monitorWrapper, JpcapCaptor jpcap) {
      this.monitorWrapper = monitorWrapper;
      this.jpcap = jpcap;
   }
   public void run() {
      jpcap.loopPacket(-1, monitorWrapper);
   }
}

