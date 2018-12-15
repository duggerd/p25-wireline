//
package gov.nist.p25.issi.packetmonitor.gui;

import gov.nist.p25.issi.packetmonitor.PacketMonitor;


public class LocalPacketMonitorController implements PacketMonitorController {
   
   private PacketMonitor packetMonitor;

   public LocalPacketMonitorController (PacketMonitor packetMonitor) {
      this.packetMonitor = packetMonitor;      
   }

   @Override
   public boolean fetchErrorFlag() throws Exception {
      return false;
   }

   @Override
   public String fetchErrorString() throws Exception {
      return null;
   }

   @Override
   public String fetchPttTraces() throws Exception {
      StringBuilder sbuf = new StringBuilder();
      sbuf.append("<pttmessages>\n");
      String messages =  packetMonitor.getPttMessages();
      if ( messages != null) { 
         sbuf.append(messages);
      }
      sbuf.append("</pttmessages>\n");
      return sbuf.toString();
   }

   @Override
   public String fetchAllTraces() throws Exception {
      StringBuilder sbuf = new StringBuilder();
      sbuf.append("<allmessages>\n");
      String messages =  packetMonitor.getAllMessages();
      if ( messages != null) { 
         sbuf.append(messages);
      }
      sbuf.append("\n</allmessages>\n");
      return sbuf.toString();
   }


   @Override
   public String fetchResult() throws Exception {
      StringBuilder sbuf = new StringBuilder();
      sbuf.append("<messages>\n");
      String result = packetMonitor.getResultTable();
      if( result != null) {
         sbuf.append( result);
      }
      sbuf.append("\n</messages>\n");
      return sbuf.toString();
   }

   @Override
   public String fetchSipTraces() throws Exception {
      StringBuilder sbuf = new StringBuilder();
      sbuf.append("<messages>\n");
      String messages =  packetMonitor.getSipMessages();
      if ( messages != null) {
         sbuf.append(messages);
      }
      sbuf.append("</messages>");
      return sbuf.toString();
   }

   @Override
   public void startCapture() throws Exception {
      return; 
   }
}
