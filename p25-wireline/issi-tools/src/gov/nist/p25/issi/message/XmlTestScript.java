//
package gov.nist.p25.issi.message;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.xmlbeans.XmlOptions;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.xmlconfig.TestScriptDocument;
import gov.nist.p25.issi.xmlconfig.TestScriptDocument.TestScript;
import gov.nist.p25.issi.xmlconfig.TestScriptDocument.TestScript.RfssScript;


public class XmlTestScript
{
   private TestScriptDocument testscriptDoc;
   private TestScript testscript;

   public static void showln(String s) { System.out.println(s); }

   // accessor
   public TestScript getTestScript() {
      return testscript;
   }

   // constructor
   public XmlTestScript()
   {
   }

   public TestScript loadTestScript(String xmlMsg)
      throws Exception
   {
      testscriptDoc = TestScriptDocument.Factory.parse(xmlMsg);
      testscript = testscriptDoc.getTestScript();
      return testscript;   
   }   
   
   public TestScript loadTestScript(File msgFile)
      throws Exception
   {
      testscriptDoc = TestScriptDocument.Factory.parse(msgFile);
      testscript = testscriptDoc.getTestScript();
      return testscript;   
   }   

   public void saveTestScript(String msgFilename)
      throws Exception
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);
      //String xml = testscriptDoc.xmlText(opts);
      //showln("pretty-msgDoc=\n"+xml);
      
      File msgFile = new File( msgFilename);
      testscriptDoc.save( msgFile);
   }

   //-------------------------------------------------------------------------
   public boolean reconcileRfssName( Map<String,String> nameMap)
   {
      boolean mflag = false;

      // rfssScript.rfssName
      List<RfssScript> rfssScriptList = testscript.getRfssScriptList();
      for (RfssScript rfssScript: rfssScriptList)
      {
         // collect input arguments
         String rfssName = rfssScript.getRfssName();
         if( rfssName != null) {
            String newName = (String)nameMap.get( rfssName);
            if( newName != null) {
               //showln("M3: replace: "+rfssName+"  with "+newName);
               rfssScript.setRfssName( newName);
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
      String xmlFile = "schema/xml/testscript.xml";

      // Test-2
      //File msgFile = new File(xmlFile);         
      String xmlMsg = FileUtility.loadFromFileAsString(xmlFile);

      showln("XmlTestScript: MAIN...");
      XmlTestScript xmldoc = new XmlTestScript();
      xmldoc.loadTestScript(xmlMsg);

      // test RfssName changes
      Map<String,String> map = new HashMap<String,String>();
      map.put( "rfss_1", "Xrfss_1");
      map.put( "rfss_2", "Xrfss_2");
      map.put( "rfss_3", "Xrfss_3");
      boolean bflag = xmldoc.reconcileRfssName( map);
      showln("XmlTestScript: bflag="+bflag);
      xmldoc.saveTestScript( "logs/testscript-1.xml");
   }
}
