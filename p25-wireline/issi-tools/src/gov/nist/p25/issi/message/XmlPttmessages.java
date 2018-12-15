//
package gov.nist.p25.issi.message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import org.apache.xmlbeans.XmlOptions;

import gov.nist.p25.common.util.DataCoder;
import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.p25payload.BlockType;
import gov.nist.p25.issi.p25payload.IMBEVoiceBlock;
import gov.nist.p25.issi.p25payload.PacketType;
import gov.nist.p25.issi.p25payload.PayloadType;
import gov.nist.p25.issi.p25payload.TransmitPriorityType;
import gov.nist.p25.issi.traceviewer.MessageData;
import gov.nist.p25.issi.traceviewer.PttMessageData;
import gov.nist.p25.issi.utils.ByteUtil;

import gov.nist.p25.issi.xmlconfig.PttmessagesDocument;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages.PttPacket;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages.PttPacket.PttSession;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages.PttPacket.RtpHeader;

import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages.PttPacket.P25Payload;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages.PttPacket.P25Payload.ControlOctet;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages.PttPacket.P25Payload.BlockHeader;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages.PttPacket.P25Payload.IssiHeaderWord;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages.PttPacket.P25Payload.IssiPacketType;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages.PttPacket.P25Payload.PttControlWord;
import gov.nist.p25.issi.xmlconfig.PttmessagesDocument.Pttmessages.PttPacket.P25Payload.ImbeVoice;


public class XmlPttmessages
{
   public static void showln(String s) { System.out.println(s); }

   private PttmessagesDocument pttmsgDoc;
   private Pttmessages pttmsg;
   private TreeSet<MessageData> messageList = new TreeSet<MessageData>();

   // accessor
   public Pttmessages getPttmessages() {
      return pttmsg;
   }

   // constructor
   public XmlPttmessages() {
   }

   public Pttmessages loadPttmessages(String xmlMsg)
      throws Exception
   {
      pttmsgDoc = PttmessagesDocument.Factory.parse(xmlMsg);
      pttmsg = pttmsgDoc.getPttmessages();
      return pttmsg;   
   }   
   
   public Pttmessages loadPttmessages(File msgFile)
      throws Exception
   {
      pttmsgDoc = PttmessagesDocument.Factory.parse(msgFile);
      pttmsg = pttmsgDoc.getPttmessages();
      return pttmsg;   
   }   

   public void savePttmessages(String msgFilename)
      throws Exception
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);
      //String xml = pttmsgDoc.xmlText(opts);
      //showln("pretty-msgDoc=\n"+xml);
      //
      File msgFile = new File( msgFilename);
      pttmsgDoc.save( msgFile);
   }

   //-------------------------------------------------------------------------
   public String toISSIString(String title, PttPacket packet)
   {
      StringBuffer sb = new StringBuffer();
      sb.append(title);
      sb.append("\nPacketNumber: "+packet.getPacketNumber());
      sb.append("\nReceptionTime: "+packet.getReceptionTime());
      sb.append("\nReceivingRfssId: "+packet.getReceivingRfssId());
      sb.append("\nSendingRfssId: "+packet.getSendingRfssId());
      sb.append("\nisSender: "+packet.getIsSender());
      sb.append("\nrawdata: "+packet.getRawdata());
      return sb.toString();
   }
   public String toISSIString(String title, PttSession session)
   {
      StringBuffer sb = new StringBuffer();
      sb.append(title);
      sb.append("\nRfssId: "+session.getRfssId());
      sb.append("\nMyIpAddress: "+session.getMyIpAddress());
      sb.append("\nMyRtpRecvPort: "+session.getMyRtpRecvPort());
      sb.append("\nRemoteIpAddress: "+session.getRemoteIpAddress());
      sb.append("\nRemoteRtpRecvPort: "+session.getRemoteRtpRecvPort());
      return sb.toString();
   }

   public String toISSIString(String title, RtpHeader rtp)
   {
      StringBuffer sb = new StringBuffer();
      sb.append(title);
      sb.append("\nVersion: "+ DataCoder.toIntegerBinary(rtp.getVersion(),2));
      sb.append("\nPadding: " + DataCoder.toBooleanBinary(rtp.getPadding()));
      sb.append("\nExtension: " + DataCoder.toBooleanBinary(rtp.getHeaderExtension()));
      sb.append("\nContributing source identifiers count: " + 
          DataCoder.toIntegerBinary(rtp.getCsrcCount(),4));
      sb.append("\nMarker: " + DataCoder.toBooleanBinary(rtp.getMarker()));
      sb.append("\nPayload type: " + DataCoder.toIntegerBinary(rtp.getPayloadType(),7));
      sb.append("\nSequence number: " + DataCoder.toIntegerHex(rtp.getSequenceNumber(),4));
      sb.append("\nTimestamp: " + DataCoder.toLongHex(rtp.getTimeStamp(),8));
      sb.append("\nSSRC: " + DataCoder.toLongHex(rtp.getSSRC(),8));
      return sb.toString();
   }

   public String toISSIString(String title, ControlOctet octet)
   {
      StringBuffer sb = new StringBuffer();
      sb.append(title);
      sb.append("\n\tSignal bit S-bit: " + DataCoder.toBinary(octet.getSignalBit()));
      sb.append("\n\tCompact: " + DataCoder.toBinary(octet.getCompactBit()));
      sb.append("\n\tBlock header count: "+ DataCoder.toIntegerBinary(octet.getBlockHeaderCount(),6));
      return sb.toString();
   }

   public String toISSIString(String title, BlockHeader bh) {
      StringBuffer sb = new StringBuffer();
      sb.append(title);
      sb.append("\n\t");
      sb.append(bh.getPayloadType().toString() + " E-bit: ");
      sb.append(DataCoder.toBinary( PayloadType.getValueFromDescription(bh.getPayloadType())));
      sb.append("\n\tBlock Type: ");
      sb.append(DataCoder.toTextIntegerBinary(bh.getBlockType(),
         BlockType.getValueFromDescription(bh.getBlockType()),7));
      sb.append("\n\tTimestamp offset: ");
      sb.append(DataCoder.toLongBinary(bh.getTimeStampOffset(),14));
      sb.append("\n\tLength: " + DataCoder.toIntegerBinary(bh.getBlockLength(),10));
      return sb.toString();
   }

   public String toISSIString(String title, IssiHeaderWord hw)
   {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append(title);
      sbuf.append("\n\tMessage Indicator: ");
      sbuf.append(DataCoder.toHex(hw.getMessageIndicator().getBytes(),18));
      sbuf.append("\n\tAlgID: ");
      sbuf.append(DataCoder.toHex(hw.getAlgId(),2));
      sbuf.append("\n\tKeyID: ");
      sbuf.append(DataCoder.toHex(hw.getKeyId(),4));
      sbuf.append("\n\tMFID: ");
      sbuf.append(DataCoder.toHex(hw.getManufacturerID(),2));
      sbuf.append("\n\tGroupID: ");
      sbuf.append(DataCoder.toHex(hw.getGroupID(),4));
      sbuf.append("\n\tNetworkID: ");
      sbuf.append(DataCoder.toHex(hw.getNetworkID(),4));

      sbuf.append("\n\tFree Running Super Frame Counter: ");
      sbuf.append(DataCoder.toBinary(hw.getSuperFrameCounter(),2));
      sbuf.append("\n\tVBB: ");
      sbuf.append(DataCoder.toBinary(hw.getVbb(),2));
      sbuf.append("\n\tReserved: ");
      sbuf.append(DataCoder.toBinary(hw.getReserved(),4));
      return sbuf.toString();
   }

   public String toISSIString(String title, IssiPacketType pt)
   {
      StringBuffer sb = new StringBuffer();
      sb.append(title);
      sb.append("\n\tMute status M-bit: ");
      int M = ("true".equalsIgnoreCase(pt.getMuteStatus()) ? 1 : 0);
      sb.append(DataCoder.toBinary(M));
      sb.append("\n\tPacket type: ");
      sb.append(pt.getPacketType());
      sb.append(" ");
      sb.append(DataCoder.toIntegerBinary(
         PacketType.getValueFromString(pt.getPacketType()),7));
      sb.append(DataCoder.toTextBinary("\n\tService options: ",
         pt.getServiceOptions(),8));
      sb.append("\n\tTransmission sequence number: ");
      sb.append(DataCoder.toIntegerHex(pt.getTransmissionSequenceNumber(),4));
      sb.append("\n\tLosing audio L-bit: ");

      int L = ("true".equalsIgnoreCase(pt.getLosingAudio()) ? 1 : 0);
      sb.append(DataCoder.toBinary(L));
      sb.append("\n\tInterval: ");
      sb.append(DataCoder.toHex(pt.getInterval(),2));
      return sb.toString();
   }

   public String toISSIString(String title, PttControlWord cw)
   {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append(title);
      sbuf.append("\n\tWide Area Communication Network: ");
      sbuf.append(DataCoder.toHex(cw.getWacnId(),5));
      sbuf.append("\n\tSystem ID: ");
      sbuf.append(DataCoder.toHex(cw.getSystemId(),3));
      sbuf.append("\n\tUnit ID: ");
      sbuf.append(DataCoder.toHex(cw.getUnitId(),6));
      sbuf.append("\n\tTransmit Priority: ");
      sbuf.append(DataCoder.toHex(
         TransmitPriorityType.valueFromString(cw.getTransmitPriority()),2));
      // #369
      //sbuf.append("\n\tTransmit Level: ");
      //sbuf.append(DataCoder.toHex(cw.getTransmitPriorityLevel(),2));
      return sbuf.toString();
   }

//>>>>>
   public String getIMBEVoiceLabel(int index)
   {
      String label = "IMBE Voice " + index;
      if( index == 9 || index == 18) {
         label += " + Low Speed Data";
      } else if( index >= 12 && index <= 17) {
         label += " + Encryption Sync";
      }
      return label;
   }
   public String getAdditionalFrameDataDescription(int index, byte[] data)
   {
      StringBuffer sbuf = new StringBuffer();
      //--------------------------------------------
      if( index == 9 && data.length >= 3) {
         // low speed data
         // LSD0, LSD1, Reserved, S0, S1
         sbuf.append("\n\tLSD0: "+DataCoder.toHex(data[0],2));
         sbuf.append("\n\tLSD1: "+DataCoder.toHex(data[1],2));
         sbuf.append("\n\tReserved: ");
         sbuf.append(DataCoder.toIntegerBinary((data[2] & 0xF0)>>4,4));
         sbuf.append("\n\tS0: ");
         sbuf.append(DataCoder.toIntegerBinary((data[2] & 0x0C)>>2,2));
         sbuf.append("\n\tS1: ");
         sbuf.append(DataCoder.toIntegerBinary((data[2] & 0x03),2));

      } else if( index == 18 && data.length >= 3) {
         // low speed data
         // LSD2, LSD3, Reserved, S2, S3
         sbuf.append("\n\tLSD2: "+DataCoder.toHex(data[0],2));
         sbuf.append("\n\tLSD3: "+DataCoder.toHex(data[1],2));
         sbuf.append("\n\tReserved: ");
         sbuf.append(DataCoder.toIntegerBinary((data[2] & 0xF0)>>4,4));
         sbuf.append("\n\tS2: ");
         sbuf.append(DataCoder.toIntegerBinary((data[2] & 0x0C)>>2,2));
         sbuf.append("\n\tS3: ");
         sbuf.append(DataCoder.toIntegerBinary((data[2] & 0x03),2));
	 
      //--------------------------------------------
      } else if( index >= 12 && index <= 17 && data.length >= 4) {

         int start = 0;
         if (index == 12) start = 0;
         if (index == 13) start = 4;
         if (index == 14) start = 8;
         if (index == 15) start = 12;
         if (index == 16) start = 16;
         if (index == 17) start = 20;

         // 12: ES0, ES1, ES2, ES3, Reserved, Status 
         // 13: ES4, ES5, ES6, ES7, Reserved, Status 
         // 14: ES8, ES9, ES10, ES11, Reserved, Status 
         // 15: ES12, ES13, ES14, ES15, Reserved, Status 
         // 16: ES16, ES17, ES18, ES19, Reserved, Status 
         // 17: ES20, ES21, ES22, ES23, Reserved, Status 
         int sync = ((data[0]&0xFF)<<16 | (data[1]&0xFF)<<8 | (data[2]&0xFF));
	 int sync1 = (sync & 0x00FC0000) >> 18;
	 int sync2 = (sync & 0x0003F000) >> 12;
	 int sync3 = (sync & 0x00000FC0) >> 6;
	 int sync4 = (sync & 0x0000003F);

         sbuf.append("\n\tES"+start+": "+DataCoder.toBinary(sync1,6));
         start++;
         sbuf.append("\n\tES"+start+": "+DataCoder.toBinary(sync2,6));
         start++;
         sbuf.append("\n\tES"+start+": "+DataCoder.toBinary(sync3,6));
         start++;
         sbuf.append("\n\tES"+start+": "+DataCoder.toBinary(sync4,6));
         sbuf.append("\n\tReserved: ");
         sbuf.append(DataCoder.toIntegerBinary((data[3] & 0x80)>>7,1));
         sbuf.append("\n\tStatus: ");
         sbuf.append(DataCoder.toIntegerBinary((data[3] & 0x7F),7));

      //--------------------------------------------
      } else {
         // just raw hex
         sbuf.append( "AdditionalFrameData=\"" +
            ByteUtil.writeBytes(data) + "\"");
      }
      return sbuf.toString();
   }
//>>>>>

   public String toISSIString(String title, ImbeVoice imbe)
   {
      int index = IMBEVoiceBlock.getIMBEVoiceIndex(imbe.getFrameType());

      StringBuffer sbuf = new StringBuffer();
      sbuf.append(title);

      sbuf.append("\n\tFrame Type: "+ getIMBEVoiceLabel( index));
      sbuf.append(" ("+DataCoder.toHex(imbe.getFrameType(),2)+")");
//>>>>>
      sbuf.append("\n\tVoice Frame: ");
      sbuf.append(DataCoder.toHex(imbe.getVoiceFrame().getBytes(),22));
      sbuf.append("\n\tET: ");
      sbuf.append(DataCoder.toIntegerBinary(imbe.getET(),3));
      sbuf.append("\n\tER: ");
      sbuf.append(DataCoder.toIntegerBinary(imbe.getER(),3));
      sbuf.append("\n\tMute Frame: ");
      sbuf.append(DataCoder.toBinary(imbe.getMuteFrame()));
      sbuf.append("\n\tLost Frame: ");
      sbuf.append(DataCoder.toBinary(imbe.getLostFrame()));
      sbuf.append("\n\tE4: ");
      sbuf.append(DataCoder.toBinary(imbe.getE4()));
      sbuf.append("\n\tE1: ");
      sbuf.append(DataCoder.toIntegerBinary(imbe.getE1(),3));
      sbuf.append("\n\tFree Running Super Frame Counter: ");
      sbuf.append(DataCoder.toIntegerBinary(imbe.getSF(),2));
      sbuf.append("\n\tReserved: ");
      sbuf.append(DataCoder.toIntegerBinary(imbe.getReserved(),2));

      String data = imbe.getAdditionalFrameData();
      if( data != null) {
//>>>>>
         sbuf.append( getAdditionalFrameDataDescription(index, data.getBytes()));
         /***
         sbuf.append("\n\tAdditionalFrameData=\"");
         //sbuf.append(DataCoder.toHex(data.getBytes(),data.length()) +"\"");
         sbuf.append(ByteUtil.writeBytes(data.getBytes()) + "\"");
	  **/
      }
      return sbuf.toString();
   }

   //-------------------------------------------------------------------------
   public String toISSIString(String xtitle, PttPacket packet, boolean keepHeader)
   {
      StringBuffer sb = new StringBuffer();
      String title;
      {
         if( keepHeader)
         {
            //title = "*** PTT Packet ***";
            sb.append( toISSIString(xtitle, packet));

            title = "*** PTT Session ***";
            PttSession session = packet.getPttSession();
            sb.append("\n");
            sb.append( toISSIString(title, session));
         }

         title = "*** RTP Header ***";
         RtpHeader rtp = packet.getRtpHeader();
         sb.append("\n");
         sb.append( toISSIString(title, rtp));

         P25Payload p25 = packet.getP25Payload();

         // process P25Payload
         ControlOctet octet = p25.getControlOctet();
         title = "Transmission Control Protocol";
         sb.append("\n");
         sb.append( toISSIString( title, octet));

         List<BlockHeader> blockHeaderList = p25.getBlockHeaderList();
         for( int i=0; i <  blockHeaderList.size(); i++)
         {
            BlockHeader blockHeader = blockHeaderList.get(i);
            title = "Payload Header: "+i;
            sb.append("\n");
            sb.append( toISSIString( title, blockHeader));
         }

         int block = 0;
// #371 Reorder the payload blocks
// Block: 1
         IssiPacketType packetType = p25.getIssiPacketType();
         if( packetType != null) {
            title = "Payload Block: "+block++;
            sb.append("\n");
            sb.append( toISSIString( title, packetType));
         }

// Block: 2
         PttControlWord controlWord = p25.getPttControlWord();
         if( controlWord != null) {
            title = "Payload Block: "+block++;
            sb.append("\n");
            sb.append( toISSIString( title, controlWord));
         }

// Block: 0
         IssiHeaderWord headerWord = p25.getIssiHeaderWord();
         if( headerWord != null) {
            title = "Payload Block: "+block++;
            sb.append("\n");
            sb.append( toISSIString( title, headerWord));
         }

// Block: 3..N
         for(ImbeVoice imbeVoice: p25.getImbeVoiceList())
         {
            title = "Payload Block: "+block++;
            sb.append("\n");
            sb.append( toISSIString( title, imbeVoice));
         }

      }  // all-packets
      return sb.toString();
   }

   //-------------------------------------------------------------------------
   public String toISSIString(String xtitle, Pttmessages pttmsg, boolean keepHeader)
   {
      StringBuffer sb = new StringBuffer();
      sb.append(xtitle);

      String title = "*** PTT Packet ***";
      List<PttPacket> pttPacketList =  pttmsg.getPttPacketList();
      //#262 create a tag- Fx:
      int i = 0;
      for (PttPacket packet: pttPacketList)
      {
         i++;
         sb.append("\n\nF"+ i +":");
         //sb.append("\n");
         sb.append( toISSIString( title, packet, keepHeader));
      }
      return sb.toString();
   }

   //-------------------------------------------------------------------------
   public void getPttMessageData(Pttmessages pttmsg, boolean keepHeader)
   {
      String title = "*** PTT Packet ***";

      List<PttPacket> pttPacketList =  pttmsg.getPttPacketList();
      for (PttPacket packet: pttPacketList)
      {
         // skip the properties for now
         Properties props = new Properties();

         // collect input arguments
         String myRfssId = packet.getReceivingRfssId();
         String remoteRfssId = packet.getSendingRfssId();

         //+++String receptionTime = "" + packet.getReceptionTime();
         long receptionTime = packet.getReceptionTime();
         String sequenceNumber = "" + packet.getPacketNumber();
         boolean isSender = "true".equalsIgnoreCase(packet.getIsSender());
         String rawdata = packet.getRawdata();

         PttSession session = packet.getPttSession();
         //String rfssId = session.getRfssId());
         //String myIpAddress = session.getMyIpAddress();
         String myRtpRecvPort = "" + session.getMyRtpRecvPort();
         //String remoteIpAddress = session.getRemoteIpAddress();
         String remoteRtpRecvPort = "" + session.getRemoteRtpRecvPort();

         P25Payload p25 = packet.getP25Payload();
         IssiPacketType packetType = p25.getIssiPacketType();
         String messageType = packetType.getPacketType();
         //showln("getPttMessageData: messageType="+messageType);
         
         String msgData = toISSIString( title, packet, keepHeader);

         PttMessageData messageData = new PttMessageData(remoteRfssId,
            myRfssId, myRtpRecvPort, remoteRtpRecvPort,
            messageType, msgData, receptionTime, sequenceNumber,
            false, isSender, props);
         messageData.setRawdata( rawdata==null ? "" : rawdata);
         messageList.add( messageData);
      }
   }

   public String getMessageData()
   {
      StringBuffer sbuf = new StringBuffer();
      for (MessageData mdata: getRecords()) {
         sbuf.append("F" + mdata.getId() + ":\n");
         sbuf.append(mdata.getData().trim());
         sbuf.append("\n\n");
      }
      return sbuf.toString();
   }

   // implementation of TraceLoader
   //-----------------------------------------------------------------------
   public List<MessageData> getRecords()
   {
      int counter = 0;
      List<MessageData> alist = new ArrayList<MessageData>();
      for (MessageData mdata: messageList) {
         mdata.setId(++counter);
         alist.add(mdata);
      }
      return alist;
   }


   /*****
   // used for unit test only
   //-------------------------------------------------------------------------
   public void generatePttmessages(String msgFileName)
      throws Exception
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);

      //----------------------
      pttmsgDoc = PttmessagesDocument.Factory.newInstance();
      pttmsg = pttmsgDoc.addNewPttmessages();

      //------------
      PttPacket packet = pttmsg.addNewPttPacket();
      PttSession session = packet.addNewPttSession();
      RtpHeader rtp = packet.addNewRtpHeader();
      P25Payload p25 = packet.addNewP25Payload();

      //------------
      ControlOctet octet = p25.addNewControlOctet();
      BlockHeader blockHeader = p25.addNewBlockHeader();
      IssiHeaderWord headerWord = p25.addNewIssiHeaderWord();
      IssiPacketType packetType = p25.addNewIssiPacketType();
      PttControlWord controlWord = p25.addNewPttControlWord();
      ImbeVoice imbeVoice = p25.addNewImbeVoice();

      //----------------------
      String xml = pttmsgDoc.xmlText(opts);
      //showln("pretty-msgDoc=\n"+xml);
      File msgFile = new File( msgFileName);
      pttmsgDoc.save( msgFile);
   }
    *****/

   //=========================================================================
   public static void main(String[] args) throws Exception
   {
      boolean keepHeader = false;
      String title = "*** PTT Messages ***";
      //String xmlFile = "xml/pttmessages.xml";
      String xmlFile = "/public/logs/ptt-19_1_1.xml";
      showln("XmlPttmessages: input xmlFile="+xmlFile);

      //File msgFile = new File(xmlFile);         
      String xmlMsg = FileUtility.loadFromFileAsString(xmlFile);

      XmlPttmessages xmlmsg = new XmlPttmessages();
      Pttmessages pttmsg = xmlmsg.loadPttmessages(xmlMsg);

      String text = xmlmsg.toISSIString( title, pttmsg, keepHeader);
      showln( text);
      xmlmsg.savePttmessages( "pttmsg-1.xml");

      // test messageList
      xmlmsg.getPttMessageData(pttmsg, keepHeader);
      List<MessageData> recordList = xmlmsg.getRecords();
      showln("recordList="+recordList);
   }
}
