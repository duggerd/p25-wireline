//
package gov.nist.p25.issi.traceviewer;

import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.p25body.SdpContent;
import gov.nist.p25.issi.utils.ProtocolObjects;
import gov.nist.javax.sip.parser.StringMsgParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.sdp.MediaDescription;
import javax.sdp.SessionDescription;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class SipTraceFileParser extends DefaultHandler {

   private static Logger logger = Logger.getLogger(SipTraceFileParser.class);
   public static void showln(String s) { System.out.println(s); }
   private static boolean verbose = false;
   
   private TopologyConfig topologyConfig;
   private TreeSet<SipMessageData> messageList;
   private HashSet<RfssData> rfssList = new HashSet<RfssData>();
   private String myIpAddress;
   private String mySipRecvPort;
   private String myRfssId;
   private String remoteRfssId;
   private String remoteSipRecvPort;
   private String messageType;
   private String time;
   private String timeStamp;
   private String transactionId;
   private StringBuffer sipMessageData = new StringBuffer();
   private Hashtable<String, HashSet<PttSessionInfo>> runtimeData;
   private int counter;
   private SAXParser saxParser;
   private InputStream inputStream;
   
   // accessor
   public HashSet<RfssData> getRfssList() {
      return rfssList;
   }

   // constructor
   public SipTraceFileParser(InputStream inputStream,
         Hashtable<String, HashSet<PttSessionInfo>> runtimeData,
         TopologyConfig topologyConfig) {
      this.topologyConfig = topologyConfig;
      this.messageList = new TreeSet<SipMessageData>();
      this.runtimeData = runtimeData;      
      this.inputStream = inputStream;

      try {
         SAXParserFactory factory = SAXParserFactory.newInstance();
         saxParser = factory.newSAXParser();
      } catch (Exception ex) {
         throw new RuntimeException("Error in configuring parser: ", ex);
      }
   }
   
   public void parse() throws Exception {
      StringMsgParser.setComputeContentLengthFromMessage(true);
      saxParser.parse(inputStream,this);
   }

   public void startElement(String namespaceURI, String localName,
         String qualifiedName, Attributes attrs) 
      throws SAXException {

      String elementName = localName;
      if ("".equals(elementName)) {
         elementName = qualifiedName;
      }
      if (attrs != null) {
         for (int i = 0; i < attrs.getLength(); i++) {
            String attributeName = attrs.getLocalName(i);
            if ("".equals(attributeName)) {
               attributeName = attrs.getQName(i);
            }
            handleAttribute(attributeName, attrs.getValue(i));
         }
      }
   }

   public void handleAttribute(String attrName, String attrValue) {
      // Check if we need to add a new RFSS object
      if( verbose) {
         logger.debug("handleAttribute(): attrName=["+attrName+"]"+"  attrValue=["+attrValue+"]");
      }
      if ("from".equals(attrName)) {
         int portIndex = attrValue.indexOf(':');
         myIpAddress = attrValue.substring(0, portIndex);
         mySipRecvPort = attrValue.substring(portIndex + 1);
      } else if ("fromRfssId".equals(attrName)) {
         myRfssId = attrValue;
      } else if ("toRfssId".equals(attrName)) {
         remoteRfssId = attrValue;
      } else if ("to".equals(attrName)) {
         int portIndex = attrValue.indexOf(':');
         remoteSipRecvPort = attrValue.substring(portIndex + 1);
      } else if ("firstLine".equals(attrName)) {
         messageType = attrValue;
      } else if ("time".equals(attrName)) {
         time = attrValue;
      } else if ("timeStamp".equals(attrName)) {
         timeStamp = attrValue;
      } else if ("transactionId".equals(attrName)) {
         transactionId = attrValue;      
      }
   }

   public void endElement(String namespaceURI, String simpleName,
         String qualifiedName) throws SAXException {
      if(verbose) {
         logger.debug("endElement(): qualifiedName="+qualifiedName);
      }

      Message message = null;
      try {
         if ("message".equals(qualifiedName)) {

            String msgData = sipMessageData.toString();
            logger.debug("+++++msgData: timeStamp="+timeStamp+" messageType="+messageType);
            logger.debug( msgData+"\n---------\n");
            if (msgData.length() != 0) {

               SipMessageData messageData = new SipMessageData(myRfssId,
                  remoteRfssId, mySipRecvPort, remoteSipRecvPort,
                  messageType, msgData,
                  time, timeStamp,
                  transactionId, false);

               messageData.setRawdata( msgData.trim());
               messageList.add(messageData);
               logger.debug("Added " + messageData.getFromRfssId()
                  + " messageType " + messageType + " tid " + transactionId);
               
               // Set RFSSs list here
               if (!RfssUtil.rfssExists(rfssList, myRfssId)) {

                  RfssData rfssData = new RfssData(myRfssId, myIpAddress, mySipRecvPort);
                  RfssConfig rfssConfig = topologyConfig.getRfssConfig(myIpAddress,
                            Integer.parseInt(mySipRecvPort));
                  if (rfssConfig == null) {
                     logger.error( "Cannot find RfssConfig for " +myIpAddress +":" +mySipRecvPort);
                     return;
                  }
                  rfssData.setRfssConfig(rfssConfig);
                  rfssData.setTimestamp(Long.parseLong(time), 0);

                  rfssList.add(rfssData);
                  HashSet<PttSessionInfo> sessionInfo = runtimeData != null ? 
                        runtimeData.get(rfssConfig.getDomainName()) : null;
                  if (sessionInfo != null)
                     rfssData.setPttSessionInfo(sessionInfo);
               } else {
                  RfssData rfssData = RfssUtil.getRfss(rfssList, myRfssId);
                  long t = Long.parseLong(time);
                  if (t < rfssData.getTimestamp()) {
                     rfssData.setTimestamp(t, 0);
                  }
               }

               RfssConfig remoteRfss = topologyConfig.getRfssConfig(remoteRfssId);
               if (remoteRfss != null && !RfssUtil.rfssExists(rfssList, remoteRfssId)) {
                  RfssData rfssData = new RfssData(remoteRfssId,
                        remoteRfss.getIpAddress(), remoteSipRecvPort);
                  // The remote RFSS is the destination of the message. 
                  // It is delayed by 1 for the sorting order.
                  rfssData.setTimestamp(Long.parseLong(time), 1);
                  rfssData.setRfssConfig(remoteRfss);
                  rfssList.add(rfssData);
                  HashSet<PttSessionInfo> sessionInfo = runtimeData != null ? 
                        runtimeData.get(remoteRfss.getDomainName()) : null;
                  if (sessionInfo != null)
                     rfssData.setPttSessionInfo(sessionInfo);
                  
               } else if (RfssUtil.rfssExists(rfssList, remoteRfssId)) {
                  RfssData rfssData = RfssUtil.getRfss(rfssList,remoteRfssId);
                  long t = Long.parseLong(time);
                  if (t < rfssData.getTimestamp()) {
                     rfssData.setTimestamp(t, 1);
                  }
               } else {
                  logger.error("Could not find remoteRfssId" + remoteRfssId);
               }

               // We now attempt to assign ports to the Rfss using
               // the sdp data in the signaling messages.
               if (msgData.trim().startsWith("SIP")) {
                  logger.debug(">>>>> msgData startsWiths SIP: "+msgData);

                  // Looking at a response here.
                  Response sipResponse = ProtocolObjects.messageFactory.createResponse(msgData);
                  message = sipResponse;
                  messageData.setSipMessage(message);
                  //logger.debug("SipTraceFileParser: createResponse=\n"+sipResponse);
                  //====================
                  logger.debug("sipResponse.getStatusCode="+sipResponse.getStatusCode());

                  if (sipResponse.getStatusCode() == Response.OK &&
                      ((CSeqHeader) sipResponse.getHeader(CSeqHeader.NAME))
                              .getMethod().equals(Request.INVITE)) {
                     logger.debug("Found an OK - assigning ports");

                     for (MessageData mdata: messageList) {
                        SipMessageData smData = (SipMessageData) messageData;
                        if (mdata==null) continue;
                        if (mdata.getMessageType().startsWith(Request.INVITE)) {
                           logger.debug("checking TID "+ smData.getTransactionId());
                           if (smData.getTransactionId().equals(transactionId)) {
                              String fromRfssId = smData.getFromRfssId();
                              // Now get the SdpPort.
                              ContentList contentList = ContentList.getContentListFromMessage(sipResponse);
                              SdpContent sdpContent = (SdpContent) ContentList.getContentByType(contentList, 
                                    ISSIConstants.APPLICATION, ISSIConstants.SDP);
                              SessionDescription sd = sdpContent.getSessionDescription();
                              int port = ((MediaDescription) sd.getMediaDescriptions(false)
                                    .get(0)).getMedia().getMediaPort();
                              boolean added = false;
                              for (RfssData rd: this.rfssList) {
                                 logger.debug("RfssDataDomain name= " + fromRfssId+ " checking "
                                       + rd.getRfssConfig().getDomainName());
                                 if (rd.getRfssConfig().getDomainName().equals(fromRfssId)) {
                                    added = true;
                                    rd.addPort(new Integer(port).toString());
                                 }
                              }
                              if (!added) {
                                 logger.error("Could not find rfss "+ fromRfssId);
                              }
                           }
                        }
                     }
                  }
                  //messageData.setSipMessage(sipResponse);

               } else {

                  logger.debug(">>>>> msgData doesnot startsWiths SIP: "+msgData);
                  Request sipRequest = ProtocolObjects.messageFactory.createRequest(msgData);
                  message = sipRequest;
                  messageData.setSipMessage(message);
                  //logger.debug("SipTraceFileParser: createRequest=\n"+sipRequest);
                  //====================

                  String method = sipRequest.getMethod();
                  if (Request.INVITE.equals(method)) {
                     // Now get the SdpPort.
                     ContentList contentList = ContentList.getContentListFromMessage(sipRequest);
                     SdpContent sdpContent = (SdpContent) ContentList.getContentByType(contentList,
                              ISSIConstants.APPLICATION, ISSIConstants.SDP);
                     //logger.debug(">>> sdpContent="+sdpContent);

                     SessionDescription sd = sdpContent.getSessionDescription();
                     logger.debug(">>> sd="+sd);
                     int port = ((MediaDescription) sd.getMediaDescriptions(false).get(0))
                           .getMedia().getMediaPort();
                     logger.debug(">>> port="+port);

                     ViaHeader via = (ViaHeader) sipRequest.getHeader(ViaHeader.NAME);
                     String rfssId = via.getHost();
                     boolean added = false;
                     for (RfssData rd : rfssList) {
                        if (rd.getRfssConfig().getDomainName().equals( rfssId)) {
                           rd.addPort(new Integer(port).toString());
                           added = true;
                        }
                     }
                     if (!added) {
                        logger.debug("Did not add port " + port + " rfssId = " + rfssId);
                     }

                  }  // INVITE

                  //---------------------------------------------------------------------
                  // How about just REGISTER - OK only
                  //else {
                  /***
                  ToHeader toHeader = (ToHeader) sipRequest.getHeader(ToHeader.NAME);
                  SipURI toURI = (SipURI) toHeader.getAddress().getURI();
                  String radicalName = toURI.getUser();
                  ContactHeader contactHeader = (ContactHeader) sipRequest.getHeader(ContactHeader.NAME);
                   ***/
                  //}
                  //---------------------------------------------------------------------
                  //messageData.setSipMessage(sipRequest);
               }

               if (time == null ||
                   myIpAddress == null ||
                   remoteSipRecvPort == null ||
                   "".equals(sipMessageData)) {
                  throw new SAXException("missing a required attribute");
               }

               // Reset data
               myIpAddress = null;
               mySipRecvPort = null;
               myRfssId = null;
               remoteRfssId = null;
               remoteSipRecvPort = null;
               messageType = null;
               sipMessageData = new StringBuffer();
               time = null;
               timeStamp = null; // NOT a required attribute.
               // Note -- default value MUST be NULL not ""
            }
         }
      }
      catch (Exception ex) {
         //showln("SipTraceFileParser: EXCEPTION-message=\n"+message);
	 ex.printStackTrace();
         logger.fatal("Error in parsing retrieved message " + message, ex);
         throw new SAXException(ex);
      }
   }

   public void characters(char buf[], int offset, int len) throws SAXException {
      sipMessageData.append(buf, offset, len);
   }

   public List<SipMessageData> getRecords() {
      List<SipMessageData> alist = new ArrayList<SipMessageData>();
      for (Iterator<SipMessageData> it = messageList.iterator(); it.hasNext();) {
         SipMessageData mdata = it.next();
         if(mdata == null) continue;
         mdata.setId(++counter);
         alist.add(mdata);
      }
      return alist;
   }

   // Handle the case when there is no INVITE in messages
   public HashSet<RfssData> getRfssListFromTopologyConfig() {
      HashSet<RfssData> xrfssList = new HashSet<RfssData>();
      for(RfssConfig rfssConfig: topologyConfig.getRfssConfigurations()) {
         logger.debug("getRfssListFromTopologyConfig: "+ rfssConfig);
         RfssData rfssData = new RfssData(rfssConfig.getDomainName(), 
             rfssConfig.getIpAddress(), 
             Integer.toString(rfssConfig.getSipPort()));
         xrfssList.add( rfssData);
      }
      return xrfssList;
   }
}
