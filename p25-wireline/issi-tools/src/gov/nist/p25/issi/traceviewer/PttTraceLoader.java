//
package gov.nist.p25.issi.traceviewer;

import java.awt.Color;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Hashtable;
import java.util.TreeSet;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

//import org.apache.log4j.Logger;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;


/**
 * This class implements a PTT XML trace file loader.
 * 
 */
public class PttTraceLoader extends DefaultHandler 
   implements TraceLoader {

   //private static Logger logger = Logger.getLogger(PttTraceLoader.class);

   private Properties props = new Properties();
   private TreeSet<MessageData> messageList;
   private Hashtable<String, Color> colorMap;
   private String myIpAddress = "";
   private String myRtpRecvPort = "";
   private String myRfssId = "";
   private String remoteRtpRecvPort = "";
   private String remoteRfssId = "";
   private String messageType = "";
   private String pttMessageData = "";
   private String rtfData = "";
   private String receptionTime = "";
   private boolean isSender;
   private boolean inRtfFormat;
   private String sequenceNumber = "";
   private String rawdata = "";
   
   // accessors
   public String getMyIpAddress() {
      return myIpAddress;
   }

   // constructors
   public PttTraceLoader(InputStream inputStream)
      throws Exception
   {
      this( inputStream, new Hashtable<String, Color>());
   }
   public PttTraceLoader(InputStream inputStream,
         Hashtable<String, Color> colorMap)
      throws Exception
   {
      this.messageList = new TreeSet<MessageData>();
      this.colorMap = colorMap;

      SAXParserFactory factory = SAXParserFactory.newInstance();
      try {
         SAXParser saxParser = factory.newSAXParser();
         saxParser.parse(inputStream, this);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw ex;
      }
   }

   public void startElement(String namespaceURI, String localName,
         String qualifiedName, Attributes attrs) throws SAXException {

      String elementName = localName;
      if ("".equals(elementName)) {
         elementName = qualifiedName;
      }
      if (!"rtf-format".equals(elementName) && 
          !"trace".equals(elementName) &&
          !"pttmessages".equals(elementName)) {
         pttMessageData += elementName + "\n";
      }
      if ("rtf-format".equals(elementName)) {
         inRtfFormat = true;
      }

      if (attrs != null && !"rtf-format".equals(elementName)) {

         for (int i = 0; i < attrs.getLength(); i++) {

            String attributeName = attrs.getLocalName(i);
            if ("".equals(attributeName)) {
               attributeName = attrs.getQName(i);
            }

            // Here is the key:value pair that user sees
            pttMessageData += "\t" + attributeName + " = "
                  + attrs.getValue(i) + "\n";
            String propname = elementName + "." + attributeName;

            // Capture the fields for pattern matching.
            if (propname.equals("ptt-packet.receivingRfssId")
                  || propname.equals("ptt-packet.sessionType")
                  || propname.equals("rtp-header.version")
                  || propname.equals("rtp-header.padding")
                  || propname.equals("rtp-header.marker")
                  || propname.equals("rtp-header.headerExtension")
                  || propname.equals("rtp-header.csrcCount")
                  || propname.equals("rtp-header.payloadType")
                  || propname.equals("rtp-header.SSRC")
                  || propname.equals("control-octet.signalBit")
                  || propname.equals("control-octet.compactBit")
                  || propname.equals("block-header.payloadType")
                  || propname.equals("block-header.blockType")
                  || propname.equals("issi-packet-type.muteStatus")
                  || propname.equals("issi-packet-type.losingAudio")
                  || propname.equals("issi-packet-type.serviceOptions")
                  || propname.equals("issi-packet-type.packetType")) {
               props.setProperty(elementName + "." + attributeName,
                     attrs.getValue(i));
            }
            handleAttribute(attributeName, attrs.getValue(i));
         }
      }
   }

   public void handleAttribute(String attrName, String attrValue) {

      if ("myIpAddress".equals(attrName)) {
         myIpAddress = attrValue;
      } else if ("receivingRfssId".equals(attrName)) {
         myRfssId = attrValue;
      } else if ("isSender".equals(attrName)) {
         isSender = attrValue.equals("true");
      } else if ("rawdata".equals(attrName)) {
         rawdata = attrValue;
      } else if ("myRtpRecvPort".equals(attrName)) {
         myRtpRecvPort = attrValue;
      } else if ("sendingRfssId".equals(attrName)) {
         remoteRfssId = attrValue;
      } else if ("remoteRtpRecvPort".equals(attrName)) {
         remoteRtpRecvPort = attrValue;
      } else if ("packetType".equals(attrName)) {
         messageType = attrValue;
         if (colorMap != null) {
            if (!colorMap.containsKey(attrValue)) {
               colorMap.put(attrValue, Color.black);
            }
         }
      } else if ("packetNumber".equals(attrName)) {
         sequenceNumber = attrValue;
      } else if ("receptionTime".equals(attrName)) {
         receptionTime = attrValue;
      }
   }

   public void endElement(String namespaceURI, String simpleName,
         String qualifiedName) throws SAXException {

      if ("rtf-format".equals(qualifiedName)) {
         inRtfFormat = false;
      }
      if ("ptt-packet".equals(qualifiedName)) {

         if (!"".equals(pttMessageData)) {

            PttMessageData messageData = new PttMessageData(remoteRfssId,
                  myRfssId, myRtpRecvPort, remoteRtpRecvPort,
                  messageType, pttMessageData, 
                  receptionTime, sequenceNumber,
                  false, isSender, props);

            //logger.debug("setRawdata=<"+rawdata+">");
            messageData.setRawdata( rawdata);
            messageList.add(messageData);

            // Reset data
            props = new Properties();
            isSender = false;
            myIpAddress = "";
            myRfssId = "";
            myRtpRecvPort = "";
            remoteRtpRecvPort = "";
            messageType = "";
            pttMessageData = "";
            receptionTime = "";
            sequenceNumber = "";
            remoteRfssId = "";
            rtfData = "";
            rawdata = "";
         }
      }
   }

   public void characters(char buf[], int offset, int len) throws SAXException {
      String s = new String(buf, offset, len);
      if (!inRtfFormat) {
         if (s.length() > 8)
            pttMessageData += "\t" + s + "\n";
      } else {
         this.rtfData += s;
      }
   }

   public List<MessageData> getRecords(String rfssDomainName) {
      List<MessageData> alist = new ArrayList<MessageData>();
      for (MessageData mdata : messageList) {
         if ( mdata.getToRfssId().equals(rfssDomainName)) {
            alist.add(mdata);
         }
      }
      return alist;
   }

   public List<MessageData> getRecords() {
      int counter = 0;
      ArrayList<MessageData> alist = new ArrayList<MessageData>();
      for (MessageData mdata: messageList) {
         mdata.setId(++counter);
         alist.add(mdata);
      }
      return alist;
   }
   
   public String getMessageData() {
      StringBuffer sbuf = new StringBuffer();
      for (MessageData mdata: getRecords()) {
         sbuf.append("F" + mdata.getId() + ":\n");
         sbuf.append(mdata.getData().trim());
         sbuf.append("\n\n");
      }
      return sbuf.toString();
   }
}
