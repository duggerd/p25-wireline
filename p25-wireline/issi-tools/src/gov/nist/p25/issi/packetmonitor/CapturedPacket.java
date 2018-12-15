//
package gov.nist.p25.issi.packetmonitor;

import jpcap.packet.Packet;

/**
 * Represents a raw captured packet. This is post processed to extract 
 * the relevant information. This is the only class that is instantiated
 * during run time. It is timestamped when the packet is captured
 * and stored internally until the packet monitor is asked to 
 * sort and classify packets.
 * 
 */
public class CapturedPacket {
   
   private int packetNumber;
   //private long timeStamp;
   private Packet packet;
   
   // accessor
   public int getPacketNumber() {
      return packetNumber;
   }
   public void setPacketNumber(int packetNumber) {
      this.packetNumber = packetNumber;
   }
   
   public long getTimeStamp() {
      //return timeStamp;
      return packet.sec*1000+packet.usec/1000;
   }
   /**
   public void setTimeStamp(long timeStamp) {
      this.timeStamp = timeStamp;
   }
    */
   
   public Packet getPacket() {
      return packet;
   }
   public void setPacket(Packet packet) {
      this.packet = packet;
   }

   // constructor
   public CapturedPacket(Packet packet) {
      this( -1, packet);
   }

   public CapturedPacket(int packetNumber, Packet packet) {
      this.packetNumber = packetNumber;
      this.packet = packet;

      // current TS + captured sec and micro-sec
      //timeStamp = System.currentTimeMillis()+packet.sec*1000+packet.usec/1000;
      //timeStamp = packet.sec*1000+packet.usec/1000;
   }
}
