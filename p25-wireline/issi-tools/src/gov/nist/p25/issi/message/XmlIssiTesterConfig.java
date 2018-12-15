//
package gov.nist.p25.issi.message;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.xmlbeans.XmlOptions;

import gov.nist.p25.common.util.FileUtility;

import gov.nist.p25.issi.xmlconfig.IssiTesterConfigDocument;
import gov.nist.p25.issi.xmlconfig.IssiTesterConfigDocument.IssiTesterConfig;
import gov.nist.p25.issi.xmlconfig.IssiTesterConfigDocument.IssiTesterConfig.DietsDaemon;
import gov.nist.p25.issi.xmlconfig.IssiTesterConfigDocument.IssiTesterConfig.DietsDaemon.Refid;


public class XmlIssiTesterConfig
{
   public static void showln(String s) { System.out.println(s); }

   private IssiTesterConfigDocument testerConfigDoc;
   private IssiTesterConfig testerConfig;

   // accessor
   public IssiTesterConfig getIssiTesterConfig() {
      return testerConfig;
   }

   // constructor
   public XmlIssiTesterConfig()
   {
   }

   public IssiTesterConfig loadIssiTesterConfig(String xmlMsg)
      throws Exception
   {
      testerConfigDoc = IssiTesterConfigDocument.Factory.parse(xmlMsg);
      testerConfig = testerConfigDoc.getIssiTesterConfig();
      return testerConfig;   
   }   
   
   public IssiTesterConfig loadIssiTesterConfig(File msgFile)
      throws Exception
   {
      testerConfigDoc = IssiTesterConfigDocument.Factory.parse(msgFile);
      testerConfig = testerConfigDoc.getIssiTesterConfig();
      return testerConfig;   
   }   

   public void saveIssiTesterConfig(String msgFilename)
      throws Exception
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);
      //String xml = testerConfigDoc.xmlText(opts);
      //showln("pretty-msgDoc=\n"+xml);
      //
      File msgFile = new File( msgFilename);
      testerConfigDoc.save( msgFile);
   }

   //-------------------------------------------------------------------------
   public boolean checkUniqueness() 
   {
      boolean mflag = true;

      // diets-daemon.ipAddress
      List<DietsDaemon> daemonList = testerConfig.getDietsDaemonList();

      HashMap<String,Object> ipmap = new HashMap<String,Object>();
      for (DietsDaemon daemon: daemonList)
      {
         // diets-daemon.ipAddress
         String ipAddress = daemon.getIpAddress();
         int port = daemon.getHttpPort();
         if( ipAddress != null) {
            String key = ipAddress +":" + port;
            Object obj = ipmap.get( key);
            if( obj == null) {
               ipmap.put( key, daemon);
            } else {
               String msg = "Duplicate ip:port in issi-tester-config: "+key;
               showln("checkUniqueness(): "+msg);
               throw new IllegalArgumentException(msg);
            }
         }
      }

      HashMap<String,Object> namemap = new HashMap<String,Object>();
      for (DietsDaemon daemon: daemonList)
      {
         // diets-daemon.name
         String name = daemon.getName();
         if( name != null) {
            Object obj = namemap.get( name);
            if( obj == null) {
               namemap.put( name, daemon);
            } else {
               String msg = "Duplicate daemon name in issi-tester-config: "+name;
               showln("checkUniqueness(): "+msg);
               throw new IllegalArgumentException(msg);
            }
         }
      }
      namemap.clear();

      // diets-daemon.refid.id
      for (DietsDaemon daemon: daemonList)
      {
         List<Refid> refidList = daemon.getRefidList();
         for( Refid refid: refidList)
         {
            String id = refid.getId();
            if( id != null) {
               // split out the rfssName from rfss_2.daemon
               String[] parts = id.split("\\.");
               if( parts.length == 2) {
                  String rfssName = parts[0];
                  Object obj = namemap.get( rfssName);
                  if( obj == null) {
                     namemap.put( rfssName, daemon);
                  } else {
                     String msg = "Duplicate rfssName in issi-tester-config: "+rfssName;
                     showln("checkUniqueness(): "+msg);
                     throw new IllegalArgumentException(msg);
                  }
               }
            }
         }
      }

      ipmap.clear();
      namemap.clear();
      return mflag;
   }

   //-------------------------------------------------------------------------
   public boolean reconcileIpAddress( Map<String,String> map)
   {
      boolean mflag = false;

      // diets-daemon.ipAddress
      List<DietsDaemon> daemonList = testerConfig.getDietsDaemonList();
      for (DietsDaemon daemon: daemonList)
      {
         String ipAddress = daemon.getIpAddress();
         if( ipAddress != null) {
            String newIp = map.get( ipAddress);
            if( newIp != null) {
               mflag = true;
               showln("substitution-ipAddress: "+ipAddress+":"+newIp);
               daemon.setIpAddress( newIp);
            }
         }
      }
      return mflag;
   }

   //-------------------------------------------------------------------------
   public boolean reconcileRfssName( Map<String,String> map)
   {
      boolean mflag = false;

      // NOTE; we donot deconcile diets-daemon.name from rfssName
      // diets-daemon.refid.id
      List<DietsDaemon> daemonList = testerConfig.getDietsDaemonList();
      for (DietsDaemon daemon: daemonList)
      {
         List<Refid> refidList = daemon.getRefidList();
         for( Refid refid: refidList)
         {
            String id = refid.getId();
            if( id != null) {
               // split out the rfssName from rfss_2.daemon
               String[] parts = id.split("\\.");
               if( parts.length == 2) {
                  String rfssName = parts[0];
                  String newRfssName = map.get(rfssName);
                  if( newRfssName != null) {
                     mflag = true;
                     showln("substitution-refid: "+rfssName+":"+newRfssName);
                     refid.setId( newRfssName+"."+parts[1]);
                  }
               }
            }
         }
      }
      return mflag;
   }
   //-------------------------------------------------------------------------
   /*** NOT USED
   public boolean reconcileTesterConfiguration( List<RfssConfigSetup> rfssList)
      throws IllegalArgumentException
   {
      // assume caller verified input data
      // verify setup data first
      //TopologyConfigHelper helper = new TopologyConfigHelper();
      //helper.checkUniqueness( rfssList); 
   
      // More to come TBD
   }
    ***/
   //-------------------------------------------------------------------------
   /***
   // used for unit test only
   //-------------------------------------------------------------------------
   public void generateIssiTesterConfig(String msgFileName)
      throws Exception
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);

      //----------------------
      testerConfigDoc = IssiTesterConfigDocument.Factory.newInstance();
      testerConfig = testerConfigDoc.addNewIssiTesterConfig();

      //----------------------
      DietsDaemon daemon = testerConfig.addNewDietsDaemon();
      daemon.setIpAddress("132.163.65.190");
      daemon.setName("daemon2");

      Refid refid = daemon.addNewRefid();
      refid.setId("rfss_2.daemon");

      refid = daemon.addNewRefid();
      refid.setId("rfss_3.daemon");

      refid = daemon.addNewRefid();
      refid.setId("rfss_4.daemon");

      //----------------------
      String xml = testerConfigDoc.xmlText(opts);
      showln("pretty-msgDoc=\n"+xml);
      File msgFile = new File( msgFileName);
      testerConfigDoc.save( msgFile);
   }
     ***/

   //=========================================================================
   public static void main(String[] args) throws Exception
   {
      // Test-1
      String xmlFile = "testerconfig/standalone-configuration.xml";

      // Test-2
      //File msgFile = new File(xmlFile);         
      String xmlMsg = FileUtility.loadFromFileAsString(xmlFile);

      XmlIssiTesterConfig xmldoc = new XmlIssiTesterConfig();
      xmldoc.loadIssiTesterConfig(xmlMsg);

      // test ip changes and swapping
      Map<String,String> map = new HashMap<String,String>();
      map.put( "132.163.65.190", "129.163.65.111");  // replace
      map.put( "132.163.65.207", "132.163.65.209");  // swap
      map.put( "132.163.65.209", "132.163.65.207");  // swap

      boolean mflag = xmldoc.reconcileIpAddress( map);
      showln("reconcileIpAddress: mflag="+mflag);

      // test name changes and swapping
      Map<String,String> nameMap = new HashMap<String,String>();
      nameMap.put( "rfss_1", "RFSS_9");   // replace
      nameMap.put( "rfss_2", "rfss_3");   // swap
      //nameMap.put( "rfss_3", "rfss_2");   // swap
      
      mflag = xmldoc.reconcileRfssName( nameMap);
      showln("reconcileRfssName: mflag="+mflag);
      
      // save it so we can verify 
      xmldoc.saveIssiTesterConfig( "logs/standaone-1.xml");

      // check for duplicate
      mflag = xmldoc.checkUniqueness();    
      showln("checkUniqueness: mflag="+mflag);
   }
}
