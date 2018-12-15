//
package gov.nist.p25.issi.analyzer.bo;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.sip.PeerUnavailableException;
import jpcap.JpcapCaptor;
import jpcap.packet.Packet;
import jpcap.packet.UDPPacket;
import jpcap.PacketReceiver;

import org.apache.log4j.Logger;

//import gov.nist.p25.issi.analyzer.vo.CapturedPacket;
//import gov.nist.p25.issi.analyzer.vo.EndPoint;
import gov.nist.p25.issi.message.XmlSystemTopology;
import gov.nist.p25.issi.packetmonitor.CapturedPacket;
import gov.nist.p25.issi.packetmonitor.EndPoint;


public class MessageCapturer implements PacketReceiver
{
   public static Logger log = Logger.getLogger(MessageCapturer.class);
   public static void showln(String s) { System.out.println(s); }

   private int packetNumber = 0;
   private LinkedList<CapturedPacket> capturedPackets;
   private MessageProcessor processor;
   private int[] ports;

   // accessor
   public int getPacketNumber() {
      return packetNumber;
   }
   public int[] getPortsRnage() {
      return ports;
   }
   public void setPortsRange(int[] ports) {
      this.ports = ports;
      processor.setPortsRange(ports);
   }

   // constructor
   //--------------------------------------------------------
   public MessageCapturer()
      throws PeerUnavailableException
   {
      // default to SIP ports
      this( new int[] { 5000, 6100} );
   }

   public MessageCapturer(int[] ports)
      throws PeerUnavailableException
   {
      this.capturedPackets = new LinkedList<CapturedPacket>();
      processor = new MessageProcessor();
      setPortsRange(ports);
   }

   public void captureAndProcess(int index)
      throws IOException 
   {
      packetNumber = 0;
      JpcapCaptor jpcap = JpcapCaptor.openDevice(
         JpcapCaptor.getDeviceList()[index], 10000, false, 20);
      jpcap.loopPacket(-1, this);
   }

   public void processPcapFile( String pcapFile)
      throws IOException
   {
      packetNumber = 0;
      capturedPackets.clear();
      JpcapCaptor captor = JpcapCaptor.openFile(pcapFile);
      while (true)
      {
         Packet packet = captor.getPacket();
         if (packet==null || packet==Packet.EOF)
            break;
         receivePacket( packet);
      }
      log.debug("processPcapFile: packetNumber="+packetNumber);
   }

   public List<EndPoint> getTargetList() // throws Exception
   {
      log.info("getTargetList(): size=" + capturedPackets.size());
      processor.setCapturedPackets( capturedPackets);
      return processor.getTargetList();
   }
  
   public String getSipMessages(List<EndPoint> endPointList)
      throws Exception
   {
      log.info("getSipMessages(): size=" + capturedPackets.size());
      processor.setCapturedPackets( capturedPackets);
      processor.setEndPointList( endPointList);

      String msg = processor.getSipMessages();
      //processor.displayTableStatus();
      return msg;
   }

   public String getPttMessages(List<EndPoint> endPointList)
      throws Exception
   {
      log.info("getPttMessages(): size=" + capturedPackets.size());
      processor.setCapturedPackets( capturedPackets);
      processor.setEndPointList( endPointList);

      String msg = processor.getPttMessages();
      //processor.displayTableStatus();
      return msg;
   }

   // implementation of PacketReceiver
   //----------------------------------------------------------------
   public void receivePacket(Packet packet)
   {
      packetNumber++;
      if (packet instanceof UDPPacket) 
      {
         UDPPacket udp = (UDPPacket) packet;
         CapturedPacket capturedPacket = new CapturedPacket(packetNumber,udp);
         capturedPackets.add(capturedPacket);
      }
   }

   //================================================================
   public static void main(String[] args) throws Exception
   {
      int index = 1;
      int[] ports = { 1, 32765 };

      MessageCapturer capturer = new MessageCapturer(ports);
      if( args.length==0)
      {
         capturer.captureAndProcess(index);
      }
      else
      {
         String pcapFile = args[0];
         showln(" --- pcapFile="+pcapFile);

         capturer.processPcapFile( pcapFile);
         List<EndPoint> epList = capturer.getTargetList();
         showln("getTargeList():\n"+ epList);

         // generate the config and properties files
         XmlSystemTopology config = new XmlSystemTopology();
         String outProp = config.generateSystemTopology( pcapFile, epList);
         showln("output prop="+outProp); 

         showln("\n=========================================");
         showln("SIP-MESSAGE:\n"+capturer.getSipMessages(epList));

         //showln("\n=========================================");
         //showln("PTT-MESSAGE:\n"+capturer.getPttMessages(epList));

      }
   }
}
