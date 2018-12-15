//
package gov.nist.p25.issi.traceviewer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;

import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import gov.nist.p25.issi.constants.ISSIDtdConstants;

public class RfssRuntimeDataParser extends DefaultHandler {

   private static Logger logger = Logger.getLogger(RfssRuntimeDataParser.class);

   private Hashtable <String, HashSet<PttSessionInfo> >rfssSessionInfoTable;
   private XMLReader saxParser;
   private InputSource iSource;
   private HashSet<PttSessionInfo> currentPttSessionInfo;      
   private String currentRfssId;
   
   // Entity resolver -- allows you to work offline
   class MyResolver implements EntityResolver {

      public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {

         if (ISSIDtdConstants.URL_ISSI_DTD_RUNTIME_DATA.equals(systemId)) {
            FileReader fileReader = new FileReader( new File("dtd/issi-runtime-data.dtd"));
            InputSource inputSource  = new InputSource(fileReader);
            return inputSource;

         } else {
            return null;
         }
      }
   }
   
   public RfssRuntimeDataParser(InputSource inputSource) {
      try {
         SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
         saxParser = saxParserFactory.newSAXParser().getXMLReader();
         saxParser.setEntityResolver(new MyResolver())   ;
         rfssSessionInfoTable = new Hashtable<String,HashSet<PttSessionInfo>>();
         saxParser.setContentHandler(this);
         this.iSource = inputSource;
         saxParser.setFeature("http://xml.org/sax/features/validation", true);
         
      } catch (Exception ex) {
         ex.printStackTrace();
         logger.fatal("Could not create parser: ", ex);
	 throw new RuntimeException("Error in configuring parser: ", ex);
      }
   }
   
   public Hashtable<String,HashSet<PttSessionInfo>> parse() 
      throws IOException, SAXException  {
      saxParser.parse(iSource);
      return rfssSessionInfoTable;
   }
   
   @Override
   public void startElement(String namespaceURI, String local, String name,
         Attributes attrs) throws SAXException {
      
      if ( "rfss-session-info".equals(name)) {
         currentPttSessionInfo = new HashSet<PttSessionInfo>();
         currentRfssId = attrs.getValue("rfssId");
         
      } else if ("ptt-session".equals(name)) {
         PttSessionInfo pttSessionInfo = new PttSessionInfo
            (attrs.getValue("sessionType"),
             attrs.getValue("rfssId"),
             attrs.getValue("myRtpRecvPort"),
             attrs.getValue("remoteRtpRecvPort"),
             attrs.getValue("remoteIpAddress"),
             attrs.getValue("unitId"),
             attrs.getValue("linkType")
            );
         currentPttSessionInfo.add(pttSessionInfo);
      }
   }

   @Override
   public void endElement(String namespaceURI, String local, String name)
         throws SAXException {
      if ( "rfss-session-info".equals(name)) {
         rfssSessionInfoTable.put(currentRfssId, currentPttSessionInfo);
      }
   }
}
