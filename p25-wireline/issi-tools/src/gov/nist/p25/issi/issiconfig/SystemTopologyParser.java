//
package gov.nist.p25.issi.issiconfig;

import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.constants.XMLTagsAndAttributes;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * System Topology Parser class.
 */
public class SystemTopologyParser extends DefaultHandler
      implements XMLTagsAndAttributes {

   private static Logger logger = Logger.getLogger(SystemTopologyParser.class);
   private static boolean verbose = false;
   
   private TopologyConfig topologyConfig;
   private int wacnId;
   private int systemId;

   private String configFile;
   private XMLReader saxParser;
   private ISSITesterConfiguration issiTesterConfiguration;

   // Entity resolver -- allows you to work offline
   class MyResolver implements EntityResolver {

      public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
         if (ISSIDtdConstants.URL_ISSI_DTD_SYSTEM_TOPOLOGY.equals(systemId)) {
            FileReader fileReader = new FileReader(new File( "dtd/systemtopology.dtd"));
            InputSource inputSource = new InputSource(fileReader);
            return inputSource;
         } else {
            return null;
         }
      }
   }

   public SystemTopologyParser(ISSITesterConfiguration config) throws Exception {
      if (config == null) {
         throw new NullPointerException("Null issiTesterConfiiguration arg !");
      }
      
      topologyConfig = new TopologyConfig();
      try {
         issiTesterConfiguration = config;
         SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
         saxParser = saxParserFactory.newSAXParser().getXMLReader();
         saxParser.setEntityResolver(new MyResolver());
         saxParser.setContentHandler(this);
         saxParser.setFeature("http://xml.org/sax/features/validation", true);
      }
      catch (Exception pce) {
         pce.printStackTrace();
         throw pce;
      }
   }

   @Override
   public void startElement(String namespaceURI, String local, String name,
         Attributes attrs) throws SAXException {
      try {
         if ("wacn-config".equalsIgnoreCase(name)) {

            String wacnName = attrs.getValue(WACN_NAME);
            if (wacnName == null) {
               throw new SAXException("Missing a required attribute: " + WACN_NAME);
            }

            String id = attrs.getValue(WACN_ID);
            if (id == null) {
               throw new SAXException("Missing a required attribute: " + WACN_ID);
            }

            this.wacnId = Integer.parseInt(id, 16);
            if (topologyConfig.getWacnNameTable().containsKey(wacnName)) {
               throw new SAXException("Duplicate WACN Name: " + wacnName);
            }
            WacnConfig wacnConfig = new WacnConfig(wacnName,wacnId);
            topologyConfig.getWacnNameTable().put(wacnName, wacnConfig);

         } else if ("systemconfig".equalsIgnoreCase(name)) {

            String sName = attrs.getValue(SYSTEM_NAME);
            if (sName == null) {
               throw new SAXException("Missing a required attribute: " + SYSTEM_NAME);
            }

            String sid = attrs.getValue(SYSTEM_ID);
            if (sid == null) {
               throw new SAXException("Missing a required attribute: " + SYSTEM_ID);
            }
            this.systemId = Integer.parseInt(sid, 16);
            
            String wacnName = attrs.getValue(WACN_NAME);
            if (wacnName == null && topologyConfig.getWacnNameTable().size() > 1) {
               throw new SAXException("Missing a required attribute: " + WACN_NAME);
            }
            WacnConfig wacnConfig = topologyConfig.getWacnConfig(wacnName);
            SystemConfig sysConfig = new SystemConfig(topologyConfig, sName, wacnConfig, systemId);
            String systemName = sysConfig.getSystemName();
            logger.debug("Putting " + systemName + " into systemTable");
            topologyConfig.addSystemConfiguration(sysConfig);
            
         } else if ("rfssconfig".equalsIgnoreCase(name)) {

            String rid = attrs.getValue(RFSS_ID);
            if (rid == null) {
               throw new SAXException("Missing a required attribute: " + RFSS_ID);
            }
            int rfssId = Integer.parseInt( rid, 16);

            String rfssName = attrs.getValue(RFSS_NAME);
            if (rfssName == null) {
               throw new SAXException("Missing a required attribute: " + RFSS_NAME);
            }
            String emulatedStr = attrs.getValue("emulated");
            boolean isEmulated = (emulatedStr==null ? true : "true".equals(emulatedStr));

            String ipAddress = attrs.getValue(IP_ADDRESS);
            String tag = RfssConfig.generateTag(rfssName);

            if(verbose) {
               logger.debug("ipAddress=" +ipAddress +" isEmulated=" +isEmulated);
               logger.debug("tag=" +tag +" rfssName=" +rfssName);
            }
            if (isEmulated) {
               ipAddress = issiTesterConfiguration.getIpAddressByName(tag);
            }
            if(verbose) 
            logger.debug("ipAddress=" +ipAddress +" tag=" +tag);            

            if (ipAddress == null) {
               ipAddress = "X.X.X.X";
               logger.error("Could not find an emulator node definition for RFSS " + rfssName + 
                  "\nMissing reference " + ipAddress + 
                  "\nCheck the systemtopology and RFSS tester configuration files. Assigning dummy address.");
            }
            
            String sPort = attrs.getValue(PORT);
            int port = (sPort == null ? 5060 : Integer.parseInt(sPort));

            String sysName = attrs.getValue(SYSTEM_NAME);
            if (sysName == null) {
               throw new SAXException("Missing a required attribute: " + SYSTEM_NAME);
            }

            SystemConfig sysconfig = topologyConfig.getSystemConfigByName(sysName);
            wacnId = sysconfig.getWacnId();
            
            RfssConfig rfssConfig = new RfssConfig(topologyConfig, 
                  sysconfig, rfssId, ipAddress, port, rfssName, isEmulated);
            rfssConfig.setTag(tag);

            // for google map !!!
            String address = attrs.getValue("address");
            rfssConfig.setAddress(address);

            RfssConfig duplicate = topologyConfig.getRfssConfig(ipAddress, port);
            if (duplicate != null) {
               throw new Exception("Duplicate Rfss found in the table: "+duplicate);
            }
            topologyConfig.addRfssConfig(rfssConfig);
            sysconfig.addRfss(rfssConfig);
         }
      } catch (Exception ex) {
         logger.error("Unexpected excepton ", ex);
         throw new SAXException("Exception parsing " + configFile, ex);
      }
   }
   
   public TopologyConfig parse(String fileName) throws IOException, SAXException {
      logger.debug("SystemTopologyParser: fileName=" + fileName);
      this.configFile = fileName;
      String fileURL = new File(fileName).toURI().toURL().toString();
      if(verbose)
         logger.debug("SystemTopologyParser: fileURL=" + fileURL);
      saxParser.parse( fileURL);
      return topologyConfig;
   }
}
