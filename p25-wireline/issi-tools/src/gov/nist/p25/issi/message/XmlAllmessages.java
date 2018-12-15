//
package gov.nist.p25.issi.message;

import java.io.File;
import java.io.IOException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlException;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.Message;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.PttPacket;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.PttPacket.RtpHeader;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.PttPacket.P25Payload;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.PttPacket.P25Payload.BlockHeader;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.PttPacket.P25Payload.IssiPacketType;

@SuppressWarnings("deprecation")
public class XmlAllmessages
{
   public static void showln(String s) { System.out.println(s); }

   private AllmessagesDocument allmessagesDoc;
   private Allmessages allmessages;

   // accessor
   public Allmessages getAllmessages() {
      return allmessages;
   }

   // constructor
   public XmlAllmessages()
   {
      allmessagesDoc = AllmessagesDocument.Factory.newInstance();
      allmessages = allmessagesDoc.addNewAllmessages();
   }
   public XmlAllmessages(String xmlFile)
      throws XmlException, IOException
   {
      loadAllmessages( FileUtility.loadFromFileAsString(xmlFile));
   }

   // methods
   public Allmessages loadAllmessages(String xmlMsg)
      throws XmlException
   {
      allmessagesDoc = AllmessagesDocument.Factory.parse(xmlMsg);
      allmessages = allmessagesDoc.getAllmessages();
      return allmessages;   
   }   
   
   public Allmessages loadAllmessages(File msgFile)
      throws XmlException, IOException
   {
      allmessagesDoc = AllmessagesDocument.Factory.parse(msgFile);
      allmessages = allmessagesDoc.getAllmessages();
      return allmessages;   
   }   

   public void saveAllmessages(String msgFilename)
      throws IOException
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);
      String xml = allmessagesDoc.xmlText(opts);
      //showln("pretty-msgDoc=\n"+xml);
      FileUtility.saveToFile( msgFilename, xml);
      //
      //File msgFile = new File( msgFilename);
      //allmessagesDoc.save( msgFile);
   }

   //-------------------------------------------------------------------------
   public static String getUserTag(String firstLine)
   {
      String userTag = "";
      if( firstLine.startsWith("INVITE")) {
         // INVITE sip:bee07ad50333@p25dr;user=TIA-P25-SG SIP/2.0
         String[] parts = firstLine.split(";");
         if( parts.length == 2) {
             parts = parts[1].split("=");
             if( parts.length == 2) {
                parts = parts[1].split(" ");
                if( parts.length == 2) {
                   userTag = parts[0];
                }
             }
         }
      }
      return userTag;
   }

   public void showMessages()
   {
      Message[] messageArray = allmessages.getMessageArray();
      //showln("messages:\n"+allmessages);
      for( int i=0; i < messageArray.length; i++) {
         Message message = messageArray[i];
         if( message==null) continue;
         showln("Message: i="+i);
	 showln("    packetNumber: "+message.getPacketNumber());
	 showln("    time: "+message.getTime());
	 showln("    from: "+message.getFrom());
	 showln("    to: "+message.getTo());
	 showln("    fromRfssId: "+message.getFromRfssId());
	 showln("    toRfssId: "+message.getToRfssId());
	 String firstLine = message.getFirstLine();
	 showln("    firstLine: "+firstLine);
	 showln("    userTag: "+getUserTag( firstLine));
      }
   } 

   public void showPttPackets()
   {
      PttPacket[] pttPacketArray = allmessages.getPttPacketArray();
      //showln("pttPacketArray:\n"+pttPacketArray);
      for( int i=0; i < pttPacketArray.length; i++) {
         PttPacket pttPacket = pttPacketArray[i];
	 if( pttPacket==null) continue;
         showln("PttPacket: i="+i);
	 showln("    packetNumber: "+pttPacket.getPacketNumber());
	 showln("    receptionTime: "+pttPacket.getReceptionTime());
	 showln("    sendingRfssId: "+pttPacket.getSendingRfssId());
	 showln("    receivingRfssId: "+pttPacket.getReceivingRfssId());

	 RtpHeader rtpHeader = pttPacket.getRtpHeader();
	 showln("    rtp-payloadType: "+rtpHeader.getPayloadType());
	 P25Payload p25Payload = pttPacket.getP25Payload();
	 BlockHeader[] blockHeaderArray = p25Payload.getBlockHeaderArray();
	 for( BlockHeader blockHeader: blockHeaderArray) {
	    showln("    p25-blockHeader-blockType: "+blockHeader.getBlockType());
	 }

	 IssiPacketType issiPacketType = p25Payload.getIssiPacketType();
	 showln("    p25-issiPacketType-packetType: "+issiPacketType.getPacketType());
      }
   } 

   //=========================================================================
   public static void main(String[] args) throws Exception
   {
      // Test-1
      String xmlFile = "logs/allmessages.xml";
      XmlAllmessages xmldoc = new XmlAllmessages(xmlFile);
      //Allmessages allmessages = xmldoc.getAllmessages();
      xmldoc.showMessages();
      xmldoc.showPttPackets();
   }
}
