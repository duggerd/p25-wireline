//
package gov.nist.p25.issi.message;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.xmlbeans.XmlOptions;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.setup.RfssConfigSetup;
import gov.nist.p25.issi.setup.SuConfigSetup;

import gov.nist.p25.issi.xmlconfig.GlobalTopologyDocument;
import gov.nist.p25.issi.xmlconfig.GlobalTopologyDocument.GlobalTopology;
import gov.nist.p25.issi.xmlconfig.GlobalTopologyDocument.GlobalTopology.Rfssconfig;
import gov.nist.p25.issi.xmlconfig.GlobalTopologyDocument.GlobalTopology.Suconfig;
import gov.nist.p25.issi.xmlconfig.GlobalTopologyDocument.GlobalTopology.Sgconfig;


public class XmlGlobalTopology
{
   private GlobalTopologyDocument globalConfigDoc;
   private GlobalTopology globalConfig;

   public static void showln(String s) { System.out.println(s); }

   // accessor
   public GlobalTopology getGlobalTopology() {
      return globalConfig;
   }

   // constructor
   public XmlGlobalTopology()
   {
   }

   public GlobalTopology loadGlobalTopology(String xmlMsg)
      throws Exception
   {
      globalConfigDoc = GlobalTopologyDocument.Factory.parse(xmlMsg);
      globalConfig = globalConfigDoc.getGlobalTopology();
      return globalConfig;   
   }   
   
   public GlobalTopology loadGlobalTopology(File msgFile)
      throws Exception
   {
      globalConfigDoc = GlobalTopologyDocument.Factory.parse(msgFile);
      globalConfig = globalConfigDoc.getGlobalTopology();
      return globalConfig;   
   }   

   public void saveGlobalTopology(String msgFilename)
      throws Exception
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);
      //String xml = globalConfigDoc.xmlText(opts);
      //showln("pretty-msgDoc=\n"+xml);
      
      File msgFile = new File( msgFilename);
      globalConfigDoc.save( msgFile);
   }

   //-------------------------------------------------------------------------
   public boolean reconcileRfssConfig( List<RfssConfigSetup> rfssConfigSetupList)
   {
      Map<String,String> map = new HashMap<String,String>();
      for( RfssConfigSetup rfssSetup: rfssConfigSetupList)
      {
         RfssConfig rfssConfig = (RfssConfig)rfssSetup.getReference();
         String rfssName = rfssConfig.getRfssName();
         String newRfssName = rfssSetup.getName();
         if( !rfssName.equals( newRfssName)) {
            map.put( new String(rfssName), newRfssName);
         }
      }
      return reconcileRfssName( map);
   }

   public boolean reconcileRfssName( Map<String,String> nameMap)
   {
      boolean mflag = false;

      // rfssconfig.rfssName
      List<Rfssconfig> rfssconfigList = globalConfig.getRfssconfigList();
      for (Rfssconfig rfssconfig: rfssconfigList)
      {
         // collect input arguments
         String rfssName = rfssconfig.getRfssName();
         if( rfssName != null) {
            String newName = (String)nameMap.get( rfssName);
            if( newName != null) {
               //showln("M2: replace: "+rfssName+"  with "+newName);
               rfssconfig.setRfssName( newName);
               mflag = true;
            }
         }
      }

      // suconfig.homeRfssName and suconfig.servingRfssName
      List<Suconfig> suconfigList = globalConfig.getSuconfigList();
      for (Suconfig suconfig: suconfigList)
      {
         String homeRfss = suconfig.getHomeRfssName();
         if( homeRfss != null) {
            String newName = (String)nameMap.get( homeRfss);
            if( newName != null) {
               suconfig.setHomeRfssName( newName);
               mflag = true;
            }
         }
         String servingRfss = suconfig.getServingRfssName();
         if( servingRfss != null) {
            String newName = (String)nameMap.get( servingRfss);
            if( newName != null) {
               suconfig.setServingRfssName( newName);
               mflag = true;
            }
         }
      }

      // sgconfig.homeRfssName
      List<Sgconfig> sgconfigList = globalConfig.getSgconfigList();
      for (Sgconfig sgconfig: sgconfigList)
      {
         String homeRfss = sgconfig.getHomeRfssName();
         if( homeRfss != null) {
            String newName = (String)nameMap.get( homeRfss);
            if( newName != null) {
               sgconfig.setHomeRfssName( newName);
               mflag = true;
            }
         }
      }
      return mflag;
   }

   //-------------------------------------------------------------------------
   public boolean reconcileSuConfig( List<SuConfigSetup> suConfigSetupList)
   {
      boolean mflag = false;
      // suconfig.suid 
      List<Suconfig> suconfigList = globalConfig.getSuconfigList();
      for (Suconfig suconfig: suconfigList)
      {
         String suName = suconfig.getSuName();
         for( SuConfigSetup suConfigSetup: suConfigSetupList) {
            SuConfig refConfig = (SuConfig)suConfigSetup.getReference();
            if( refConfig.getSuName().equals( suName)) {
               showln("  replace SUID=" + suconfig.getSuId() +
                      " with " + Integer.toHexString(suConfigSetup.getId()));
               suconfig.setSuId( Integer.toHexString(suConfigSetup.getId()));
               mflag = true;
            }
         }
      }
      return mflag;
   }

   //=========================================================================
   public static void main(String[] args) throws Exception
   {
      // Test-1
      String xmlFile = "schema/xml/globaltopology.xml";

      // Test-2
      //File msgFile = new File(xmlFile);         
      String xmlMsg = FileUtility.loadFromFileAsString(xmlFile);

      XmlGlobalTopology xmldoc = new XmlGlobalTopology();
      xmldoc.loadGlobalTopology(xmlMsg);

      // test RfssName changes
      Map<String,String> map = new HashMap<String,String>();
      map.put( "rfss_1", "Xrfss_1");
      map.put( "rfss_2", "Xrfss_2");
      xmldoc.reconcileRfssName( map);
      xmldoc.saveGlobalTopology( "logs/globalTopo-1.xml");
   }
}
