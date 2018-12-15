//
package gov.nist.p25.issi.rfss.tester;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import gov.nist.p25.common.util.IpAddressUtility;
import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.DaemonWebServerAddress;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ISSI Tester Configuration parser.
 */
public class ISSITesterConfigurationParser extends DefaultHandler {

   private static Logger logger = Logger.getLogger(ISSITesterConfigurationParser.class);
   public static void showln(String s) { System.out.println(s); }
   
   private ISSITesterConfiguration retval;
   private String fileName;
   private XMLReader saxParser;
   private String ipAddress;
   private int httpPort;
   private DaemonWebServerAddress daemonWebServerAddress;
   
   // Entity resolver -- allows you to work offline
   class MyResolver implements EntityResolver {

      public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {

         if (ISSIDtdConstants.URL_ISSI_DTD_TESTER_CONFIG.equals(systemId)) {
            FileReader fileReader = new FileReader(new File("dtd/issi-tester-config.dtd"));
            InputSource inputSource = new InputSource(fileReader);
            return inputSource;
         } else {
            return null;
         }
      }
   }

   public ISSITesterConfiguration parse() throws Exception {
      logger.debug("parse(): fileName=" + fileName);
      // fix protocol error in URL of fileName
      saxParser.parse(new File(fileName).toURI().toURL().toString());
      return retval;
   }

   public ISSITesterConfigurationParser(String fileName) throws SAXException,
         ParserConfigurationException {
           
      //logger.debug("Parsing " + fileName);
      this.fileName = fileName;
      retval = new ISSITesterConfiguration();
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      saxParser = saxParserFactory.newSAXParser().getXMLReader();
      saxParser.setEntityResolver(new MyResolver());
      saxParser.setContentHandler(this);
      saxParser.setFeature("http://xml.org/sax/features/validation", false);
   }

   @Override
   public void startElement(String uri, String localName, String name,
         Attributes attrs) throws SAXException {

      //logger.debug("startElement: name=" + name);
      if ("diets-daemon".equals(name)) {

         ipAddress = attrs.getValue("ipAddress");
         if ( ipAddress == null) { 
            ipAddress = ISSITesterConstants.LOCALHOST_IP_ADDRESS;
         }

         //-----------------------------------------------------------------
         // check if ipAddress is 127.0.0.1 
         if( ISSITesterConstants.LOCALHOST_IP_ADDRESS.equals(ipAddress)) {
            String newIp = null;
            try {
               newIp = IpAddressUtility.getLocalHostAddress().getHostAddress();
               if( newIp!=null) {
                  ipAddress = newIp;
                  //showln("ISSITesterConfigurationParser(): replace 127.0.0.1 with "+newIp);
               }
            } catch(Exception ex) { }
         }
         //showln("ISSITesterConfigurationParser(): ipAddress="+ipAddress);

         //-----------------------------------------------------------------
         String nodeName = attrs.getValue("name");
         if( nodeName == null)
            nodeName = "Unknown";

         String param = attrs.getValue("httpPort");
         if (param != null) {
            httpPort = Integer.parseInt(param);
         } else {
            httpPort = ISSITesterConstants.DEFAULT_DIETS_HTTP_PORT;
         }

         param = attrs.getValue("isConformanceTester");
         boolean conformanceTester = (param == null ? false : Boolean.parseBoolean(param));

         param = attrs.getValue("isPacketMonitor");
         boolean packetMonitor = (param == null ? false : Boolean.parseBoolean(param));
         
         logger.debug("startElement: nodeName=" + nodeName + "  ip="+ipAddress);
         daemonWebServerAddress = new DaemonWebServerAddress(nodeName,ipAddress,
               httpPort,conformanceTester,packetMonitor);

         // note that there may not be a name associated with this address.
         retval.addDaemonWebServerAddress(daemonWebServerAddress);
                  
      } else if ("refid".equalsIgnoreCase(name)) {
         String id = attrs.getValue("id");
         if( id != null) {
            retval.addRefId(id,daemonWebServerAddress);
         }
      }       
   }
}
