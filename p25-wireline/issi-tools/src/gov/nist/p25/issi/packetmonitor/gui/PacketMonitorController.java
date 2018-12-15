//
package gov.nist.p25.issi.packetmonitor.gui;

/**
 * Control interface for the packet monitor.
 */
public interface PacketMonitorController {

   /**
    * Start packet capture.
    */
   public void startCapture() throws Exception;

   /**
    * Fetch and display SIP traces.
    */
   public String fetchSipTraces() throws Exception;

   /**
    * Fetch and display PTT traces.
    */
   public String fetchPttTraces() throws Exception;

   public String fetchAllTraces() throws Exception;

   
   /**
    * Fetch result table string
    * @return
    * @throws Exception
    */
   public String fetchResult() throws Exception;
   
   /**
    * Fetch error status.
    */
   public boolean fetchErrorFlag() throws Exception;
   
   /**
    * Fetch the error String.
    */
   public String fetchErrorString() throws Exception;
}
