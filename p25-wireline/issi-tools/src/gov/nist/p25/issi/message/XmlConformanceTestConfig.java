//
package gov.nist.p25.issi.message;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.xmlbeans.XmlOptions;

import gov.nist.p25.common.util.FileUtility;

import gov.nist.p25.issi.xmlconfig.ConformanceTestConfigDocument;
import gov.nist.p25.issi.xmlconfig.ConformanceTestConfigDocument.ConformanceTestConfig;
import gov.nist.p25.issi.xmlconfig.ConformanceTestConfigDocument.ConformanceTestConfig.Rfssconfig;
import gov.nist.p25.issi.xmlconfig.ConformanceTestConfigDocument.ConformanceTestConfig.Suconfig;
import gov.nist.p25.issi.xmlconfig.ConformanceTestConfigDocument.ConformanceTestConfig.Sgconfig;


public class XmlConformanceTestConfig
{
   public static void showln(String s) { System.out.println(s); }

   private ConformanceTestConfigDocument testConfigDoc;
   private ConformanceTestConfig testConfig;

   // accessor
   public ConformanceTestConfig getConformanceTestConfig() {
      return testConfig;
   }

   // constructor
   public XmlConformanceTestConfig()
   {
   }

   public ConformanceTestConfig loadConformanceTestConfig(String xmlMsg)
      throws Exception
   {
      testConfigDoc = ConformanceTestConfigDocument.Factory.parse(xmlMsg);
      testConfig = testConfigDoc.getConformanceTestConfig();
      return testConfig;   
   }   
   
   public ConformanceTestConfig loadConformanceTestConfig(File msgFile)
      throws Exception
   {
      testConfigDoc = ConformanceTestConfigDocument.Factory.parse(msgFile);
      testConfig = testConfigDoc.getConformanceTestConfig();
      return testConfig;   
   }   

   public void saveConformanceTestConfig(String msgFilename)
      throws Exception
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);
      //String xml = testConfigDoc.xmlText(opts);
      //showln("pretty-msgDoc=\n"+xml);
      
      File msgFile = new File( msgFilename);
      testConfigDoc.save( msgFile);
   }

   //-------------------------------------------------------------------------
   public boolean reconcileRfssName( Map<String,String> nameMap)
   {
      boolean mflag = false;

      // traceOrder
      String traceOrder = testConfig.getTraceOrder();
      if( traceOrder != null) {
         String[] parts = traceOrder.split(",");
         for( int i = 0; i < parts.length; i++) {
            String newName = (String)nameMap.get( parts[i]);
            if( newName != null) {
               //showln("M1: replace: "+parts[i]+"  with "+newName);
               parts[i] = newName;
               mflag = true;
            }
         }
         StringBuffer sb = new StringBuffer();
         for( int i = 0; i < parts.length; i++) {
            if( i > 0) sb.append(",");
            sb.append( parts[i]);
         }
         testConfig.setTraceOrder( sb.toString());
      }
      
      // rfssconfig.rfssName
      List<Rfssconfig> rfssconfigList = testConfig.getRfssconfigList();
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

      gov.nist.p25.issi.xmlconfig.ConformanceTestConfigDocument.ConformanceTestConfig.Sgconfig.Forbidden sgForbidden;
      // sgconfig.forbidden.rfssName
      List<Sgconfig> sgconfigList = testConfig.getSgconfigList();
      for (Sgconfig sgconfig: sgconfigList)
      {
         sgForbidden = sgconfig.getForbidden();
         if( sgForbidden != null) {
            String rfssName = sgForbidden.getRfssName();
            if( rfssName != null) {
               String newName = (String)nameMap.get( rfssName);
               if( newName != null) {
                  sgForbidden.setRfssName( newName);
                  mflag = true;
               }
            }
         }
      }

      gov.nist.p25.issi.xmlconfig.ConformanceTestConfigDocument.ConformanceTestConfig.Suconfig.Forbidden suForbidden;
      // suconfig.forbidden.rfssName
      List<Suconfig> suconfigList = testConfig.getSuconfigList();
      for (Suconfig suconfig: suconfigList)
      {
         suForbidden = suconfig.getForbidden();
         if( suForbidden != null) {
            String rfssName = suForbidden.getRfssName();
            if( rfssName != null) {
               String newName = (String)nameMap.get( rfssName);
               if( newName != null) {
                  suForbidden.setRfssName( newName);
                  mflag = true;
               }
            }
         }
      }
      return mflag;
   }

   /***
   // used for unit test only
   //-------------------------------------------------------------------------
   public void generateConformanceTestConfig(String msgFileName)
      throws Exception
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);

      //----------------------
      testConfigDoc = ConformanceTestConfigDocument.Factory.newInstance();
      testConfig = testConfigDoc.addNewConformanceTestConfig();

      //----------------------
      testConfig.setTraceOrder("Xrfss-1,Xrfss_2");
      testConfig.setTHEARTBEAT(550);

      //----------------------
      Rfssconfig rfssconfig = testConfig.addNewRfssconfig();
      rfssconfig.setRfssName( "Xrfss_1");

      Suconfig suconfig = testConfig.addNewSuconfig();
      suconfig.setSuName("su_12");

      Sgconfig sgconfig = testConfig.addNewSgconfig();
      sgconfig.setGroupName("group_1");

      //----------------------
      String xml = testConfigDoc.xmlText(opts);
      showln("pretty-msgDoc=\n"+xml);
      File msgFile = new File( msgFileName);
      testConfigDoc.save( msgFile);
   }
    ***/

   //=========================================================================
   public static void main(String[] args) throws Exception
   {
      // Test-1
      //String xmlFile = "schema/xml/currenttopology.xml";
      String xmlFile = 
         "testsuites/conformance/testscripts/su_registration_successful_presence/topology1.xml";

      // Test-2
      //File msgFile = new File(xmlFile);         
      String xmlMsg = FileUtility.loadFromFileAsString(xmlFile);

      XmlConformanceTestConfig xmldoc = new XmlConformanceTestConfig();
      xmldoc.loadConformanceTestConfig(xmlMsg);

      // test RfssName changes
      Map<String,String> map = new HashMap<String,String>();
      map.put( "Xrfss_2", "rfss_2");
      xmldoc.reconcileRfssName( map);
      xmldoc.saveConformanceTestConfig( "logs/curTopConfig-1.xml");
   }
}
