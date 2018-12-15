//
package gov.nist.p25.issi.message;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.xmlbeans.XmlDocumentProperties;
import org.apache.xmlbeans.XmlOptions;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.packetmonitor.EndPoint;
import gov.nist.p25.issi.setup.RfssConfigSetup;

import gov.nist.p25.issi.xmlconfig.SystemTopologyDocument;
import gov.nist.p25.issi.xmlconfig.SystemTopologyDocument.SystemTopology;
import gov.nist.p25.issi.xmlconfig.SystemTopologyDocument.SystemTopology.Systemconfig;
import gov.nist.p25.issi.xmlconfig.SystemTopologyDocument.SystemTopology.WacnConfig;
import gov.nist.p25.issi.xmlconfig.SystemTopologyDocument.SystemTopology.Rfssconfig;


public class XmlSystemTopology
{
   public static void showln(String s) { System.out.println(s); }

   public static final String SYSTEM_TOPOLOGY_PROPERTY = "diets.packetmonitor.systemTopology";
   public static final String DAEMON_CONFIG_PROPERTY = "diets.daemon.configuration";

   private SystemTopologyDocument systemTopologyDoc;
   private SystemTopology systemTopology;

   // constructor
   public XmlSystemTopology()
   {
   }
   
   public SystemTopology loadSystemTopology(String topologyFilename)
      throws Exception
   {
      File topologyFile = new File(topologyFilename);         
      systemTopologyDoc = SystemTopologyDocument.Factory.parse(topologyFile);
      systemTopology = systemTopologyDoc.getSystemTopology();
      return systemTopology;   
   }   

   //-------------------------------------------------------------------------
   public boolean reconcile(List<RfssConfigSetup> rfssList)
   {
      boolean mflag = false;
      // rfssconfig.rfssName
      for (Rfssconfig rfssConfig: systemTopology.getRfssconfigList())
      {
         String rname = rfssConfig.getRfssName();
         int rid = Integer.parseInt(rfssConfig.getRfssId(),16);
         //String ipAddress = rfssConfig.getIpAddress();
         //int port = rfssConfig.getPort();
         //boolean emulated = Boolean.parseBoolean(rfssConfig.getEmulated());

         for( RfssConfigSetup rfssSetup: rfssList)
         {
            RfssConfig refConfig = (RfssConfig)rfssSetup.getReference();
            if( refConfig.getRfssName().equals(rname) && 
                refConfig.getRfssId()==rid)
            {
               showln("replace "+refConfig.getRfssName()+" with "+rfssSetup.getName());
               mflag = true;
               rfssConfig.setRfssName( rfssSetup.getName());
               rfssConfig.setRfssId( Integer.toHexString(rfssSetup.getId()));

               showln("replace "+refConfig.getIpAddress()+" with "+rfssSetup.getIpAddress());
               rfssConfig.setIpAddress( rfssSetup.getIpAddress());
               rfssConfig.setPort( rfssSetup.getPort());
               rfssConfig.setEmulated( Boolean.toString(rfssSetup.getEmulated()));

               for (Systemconfig systemconfig: systemTopology.getSystemconfigList())
               {
                  if( !systemconfig.getSystemName().equals(rfssConfig.getSystemName()))
                     continue;
                  showln(" --- set systemId "+rfssSetup.getSystemId());
                  systemconfig.setSystemId( Integer.toHexString(rfssSetup.getSystemId()));

                  for (WacnConfig wacnConfig: systemTopology.getWacnConfigList())
                  {
                     if( !wacnConfig.getWacnName().equals(systemconfig.getWacnName()))
                        continue;
                     showln(" --- set wacnId "+rfssSetup.getWacnId());
                     wacnConfig.setWacnId( Integer.toHexString(rfssSetup.getWacnId()));
                  }
               }
            }
         }
      }
      return mflag;
   }   

   //----------------------------------------------------------------------
   public void printSystemTopology(SystemTopology systemTopology, String prefix)
   {
      showln("--- WacnConfig ---");
      for (WacnConfig wacnConfig: systemTopology.getWacnConfigList())
      {
         showln(prefix + "wacnName="+wacnConfig.getWacnName());
         showln(prefix + "wacnId="+wacnConfig.getWacnId());
         showln(prefix + "--------------");
      }

      showln("--- Systemconfig ---");
      for (Systemconfig systemconfig: systemTopology.getSystemconfigList())
      {
         showln(prefix + "systemName="+systemconfig.getSystemName());
         showln(prefix + "systemId="+systemconfig.getSystemId());
         showln(prefix + "wacnName="+systemconfig.getWacnName());
         showln(prefix + "--------------");
      }
      showln("--- Rfssconfig ---");
      for (Rfssconfig rfssconfig: systemTopology.getRfssconfigList())
      {
         showln(prefix + "rfssName="+rfssconfig.getRfssName());
         showln(prefix + "rfssId="+rfssconfig.getRfssId());
         showln(prefix + "systemName="+rfssconfig.getSystemName());
         showln(prefix + "ipAddress="+rfssconfig.getIpAddress());
         showln(prefix + "port="+rfssconfig.getPort());
         showln(prefix + "emulated="+rfssconfig.getEmulated());
         showln(prefix + "--------------");
      }
   }
   
   public void saveSystemTopology(String topologyFilename)
      throws Exception
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);
      //String xml = systemTopologyDoc.xmlText(opts);
      //showln("pretty-doc(1)=\n"+xml);

      File topologyFile = new File( topologyFilename);
      systemTopologyDoc.save( topologyFile);
   }

   //----------------------------------------------------------------------
   public static String stripLeadingZero( String id)
   {
      boolean isZero = true;
      StringBuffer sb = new StringBuffer();
      for(int i=0; i < id.length(); i++) {
         char c = id.charAt(i);
         if( c != '0')
            isZero = false;
         if( isZero)
            continue;
         sb.append( c);
      }
      return sb.toString();
   } 

   //----------------------------------------------------------------------
   public static void buildSystemTopology(List<EndPoint> endPointList,
      String topologyFilename)
      throws IOException
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);

      SystemTopologyDocument doc= SystemTopologyDocument.Factory.newInstance();
      SystemTopology systemTopology = doc.addNewSystemTopology();
      
      // setup DOCTYPE
      //<!DOCTYPE system-topology SYSTEM
      //  "http://www-x.antd.nist.gov:8080/p25/issi-emulator/dtd/systemtopology.dtd">
      //
      XmlDocumentProperties docProps = doc.documentProperties();
      docProps.setDoctypeName( "system-topology");
      docProps.setDoctypeSystemId(ISSIDtdConstants.URL_ISSI_DTD_SYSTEM_TOPOLOGY);

      boolean match;
      WacnConfig wacnConfig;
      Systemconfig systemconfig;
      Rfssconfig rfssconfig;

      for( EndPoint ep: endPointList)
      {
         String domainName = ep.getDomainName();
         //showln("build: domainName="+domainName);
         //showln("build: name="+ep.getName());

         String[] parts = domainName.split("\\.");
         if( parts.length != 4) continue;

         String rfssId = stripLeadingZero( parts[0]);
         String systemId = stripLeadingZero( parts[1]);
         String systemName = "system_"+systemId;
         //showln("rfssid="+parts[0]+"  strip:"+rfssId);
         //showln("systemId="+parts[1]+"  strip:"+systemId);

         String wacnId = parts[2];
         String wacnName = wacnId;

         // check if already exists
         match = false;
         for (WacnConfig wc: systemTopology.getWacnConfigList())
         {
            if( wc.getWacnName().equals(wacnName) &&
                wc.getWacnId().equals(wacnId))
            {
                match = true;
                break;
            }
         }

         //showln("wacnId-match="+match);
         if( !match) {
            wacnConfig = systemTopology.addNewWacnConfig();
            wacnConfig.setWacnName( wacnName);
            wacnConfig.setWacnId( wacnId);
         }

         match = false;
         for (Systemconfig sc: systemTopology.getSystemconfigList())
         {
            if( sc.getSystemName().equals(systemName) &&
                sc.getSystemId().equals(systemId) &&
                sc.getWacnName().equals(wacnName)) 
            {
                match = true;
                break;
            }
         }

         //showln("systemId-match="+match);
         if( !match) {
            systemconfig = systemTopology.addNewSystemconfig();
            systemconfig.setSystemName( systemName);
            systemconfig.setSystemId( systemId);
            systemconfig.setWacnName( wacnName);
         }

         rfssconfig = systemTopology.addNewRfssconfig();
         if( ep.getName().length() > 0)
            rfssconfig.setRfssName( ep.getName());
         else
            rfssconfig.setRfssName( "rfss_"+rfssId);            
         rfssconfig.setRfssId( rfssId);
         rfssconfig.setSystemName( systemName);
         rfssconfig.setIpAddress( ep.getHost());
         rfssconfig.setPort( ep.getPort());
         rfssconfig.setColor( "GREEN");
         rfssconfig.setEmulated( Boolean.toString(ep.getEmulated()));

      }  // for-loop

      //String xml = doc.xmlText(opts);
      //showln("pretty-doc(2)=\n"+xml);
      File topologyFile = new File( topologyFilename);
      doc.save( topologyFile);
   }

   //----------------------------------------------------------------------
   public String generateSystemTopology(String pcapFileName, 
      List<EndPoint> epList)
      throws IOException
   {
      // === c:/project/issi-emulator/Lab_Traces/subnet_test.pcap
      File pcapFile =  new File( pcapFileName);
      //showln("pcapFileName="+pcapFileName);
      //showln("pcapName="+pcapFile.getName());
      //showln("pathName="+pcapFile.getPath());
      //showln("parent="+pcapFile.getParent());
      //showln("parentPath="+pcapFile.getParentFile().getAbsolutePath());
      String parentPath = pcapFile.getParentFile().getAbsolutePath();
      String filename = FileUtility.getFilenameWOExtension( pcapFile.getName());

      // === generate SystemTopology file
      String outTopo = parentPath + File.separator +
           filename + "-systemTopology.xml";
      showln("outTopo="+outTopo);
      XmlSystemTopology.buildSystemTopology( epList, outTopo);

      // === generate local-cofiguration file
      // just copy the template for now
      String localXml = "xml/local-configuration.xml";
      String localStr = FileUtility.loadFromFileAsString(localXml);
      String outLocal = parentPath + File.separator +
           filename +"-localConfiguration.xml";
      FileUtility.saveToFile( outLocal, localStr);
      showln("outLocal="+outLocal);

      // === generate pcap proeprties file
      Properties props = new Properties();
      props.setProperty( DAEMON_CONFIG_PROPERTY, outLocal);
      props.setProperty( SYSTEM_TOPOLOGY_PROPERTY, outTopo);

      String outProp = filename + ".properties";
      String outFull = parentPath + File.separator + outProp;
      File dir = new File( parentPath);
      FileUtility.writeProperties( dir, outProp, props);
      showln("outProp="+outProp);

      return outFull;
   }

   //==================================================================
   public static void main(String[] args) throws Exception
   {
      XmlSystemTopology config = new XmlSystemTopology();

      // test existing SystemTopology
      String prefix = "  ";
      String topoFile = "xml/systemtopology.xml";
      SystemTopology systemTopology = config.loadSystemTopology(topoFile);
      config.printSystemTopology( systemTopology, prefix);
      config.saveSystemTopology( "sample.xml");

      List<EndPoint> epList = new ArrayList<EndPoint>();

      // test new SystemTopology
      //String pcapFileName = "Lab_Traces/subnet_test.pcap";
      //epList.add( new EndPoint("10.0.0.24",5060,"02.ad5.bee07.p25dr","XHF"));
      //epList.add( new EndPoint("10.0.2.25",5060,"03.ad5.bee07.p25dr","YHF"));

      String pcapFileName = "AZgate/issi-call.pcap";
      epList.add( new EndPoint("10.8.121.8", 5060, "03.002.abcde.p25dr", "R3"));
      epList.add( new EndPoint("10.8.121.9", 5060, "02.003.bcdef.p25dr", "R2"));

      String outProp = config.generateSystemTopology( pcapFileName, epList);
      showln("output prop="+outProp); 
   }
}
