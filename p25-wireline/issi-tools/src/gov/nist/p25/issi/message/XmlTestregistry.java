//
package gov.nist.p25.issi.message;

import java.io.File;
import java.util.List;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.ListModel;
import org.apache.xmlbeans.XmlOptions;

import gov.nist.p25.common.swing.widget.CheckBoxList;
import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.xmlconfig.TestregistryDocument;
import gov.nist.p25.issi.xmlconfig.TestregistryDocument.Testregistry;
import gov.nist.p25.issi.xmlconfig.TestregistryDocument.Testregistry.Testcase;


public class XmlTestregistry
{
   public static void showln(String s) { System.out.println(s); }

   //private static String TAG_DEPRECATED = "Deprecated";
   //private static String TAG_CONFORMANCE = "Conformanace";
   private static String TAG_DEPRECATED = ISSITesterConstants.TEST_CLASS_DEPRECATED;
   private static String TAG_CONFORMANCE = ISSITesterConstants.TEST_CLASS_CONFORMANCE;

   private TestregistryDocument testregistryDoc;
   private Testregistry testregistry;
   private String cbtype;

   // accessor
   public Testregistry getTestregistry() {
      return testregistry;
   }

   // constructor
   public XmlTestregistry() {
   }

   public Testregistry loadTestregistry(String xmlMsg)
      throws Exception
   {
      testregistryDoc = TestregistryDocument.Factory.parse(xmlMsg);
      testregistry = testregistryDoc.getTestregistry();
      return testregistry;   
   }   
   
   public Testregistry loadTestregistry(File msgFile)
      throws Exception
   {
      testregistryDoc = TestregistryDocument.Factory.parse(msgFile);
      testregistry = testregistryDoc.getTestregistry();
      return testregistry;   
   }   

   public void saveTestregistry(String msgFilename)
      throws Exception
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);
      //String xml = testregistryDoc.xmlText(opts);
      //showln("pretty-msgDoc=\n"+xml);
      //
      File msgFile = new File( msgFilename);
      testregistryDoc.save( msgFile);
   }

   //-------------------------------------------------------------------------
   public boolean reconcileTestcase(CheckBoxList cbList)
   {
      boolean mflag = false;
      ListModel model = cbList.getModel();
      for( int i=0; i < model.getSize(); i++) {

         JCheckBox jcb = (JCheckBox)model.getElementAt(i);
         Testcase testcase = getTestcaseByDescriptor( jcb.getText());
         if( testcase == null)
            continue;

         String testClass = testcase.getTestClass();
         if( jcb.isSelected()) {
	    //showln("   jcb selected: "+jcb.getText());
	    String ctag = buildClassMember(testClass, TAG_CONFORMANCE, cbtype);
            testcase.setTestClass( ctag);
            mflag = true;
         } else {
	    if( testClass != null) {
	       //showln("   unset testcase..."+testcase);
               testcase.unsetTestClass();
               mflag = true;
            }
         }
      }
      return mflag;
   }

   public boolean isClassMemberOf(String testClass, String tag)
   {
      String[] parts = testClass.split(",");
      for( String part: parts) {
         if( part.trim().equals(tag))
            return true;
      }
      return false;
   }

   public String buildClassMember(String testClass, String defTag, String cbtype)
   {
      String selTag = defTag + "," + cbtype;
      if( testClass==null) {
         return selTag;
      }
      if( !isClassMemberOf(testClass, cbtype)) {
         return selTag;
      }
      return testClass;
   }

   public CheckBoxList getCheckBoxList( String cbtype)
   {
      this.cbtype = cbtype;
      Vector<JCheckBox> cbVec = new Vector<JCheckBox>();
      List<Testcase> testcaseList = testregistry.getTestcaseList();
      for (Testcase testcase: testcaseList)
      {
         String testDirectory = testcase.getTestDirectory();
         String testNumber = testcase.getTestNumber();
         String testTitle = testcase.getTestTitle();

         String tag = testNumber +" : " + testDirectory;

         // optional role field
         String role = testcase.getRole();
         if(role != null && role.length() > 0) {
            tag += " : " + role;
         }

         JCheckBox cb = new JCheckBox(tag);

         String testClass = testcase.getTestClass();
	 if( testClass != null) {
            if( !isClassMemberOf(testClass, TAG_DEPRECATED)) {
               if( isClassMemberOf(testClass, cbtype)) {
                  cb.setSelected(true);
                  cbVec.add( cb);
               }
            }
         } else {
            // default case
            cbVec.add( cb);
         }
      }
      CheckBoxList cbList = new CheckBoxList( cbVec);
      return cbList;
   }

   public Testcase getTestcaseByDescriptor(String descriptor)
   {
      List<Testcase> testcaseList = testregistry.getTestcaseList();
      for (Testcase testcase: testcaseList)
      {
         String testDirectory = testcase.getTestDirectory();
         String testNumber = testcase.getTestNumber();
         String testTitle = testcase.getTestTitle();
         String testClass = testcase.getTestClass();

          // This must match the TestCaseDescriptor.toString()
          String tag = testNumber +" : ";
          if( testTitle != null && testTitle.length() > 0) {
             tag += testTitle;
          } else {
             tag += testDirectory;
          }

          // optional role field
          String role = testcase.getRole();
          if(role != null && role.length() > 0) {
             tag += " : " + role;
          }
          if( tag.equals(descriptor)) { 
             if(testClass==null ||
                !isClassMemberOf(testClass, TAG_DEPRECATED)) {
                return testcase;
             }
          }    
      }
      return null;
   }

   //=========================================================================
   public static void main(String[] args) throws Exception
   {
      // Test-1
      //String xmlFile = "c:\research\p25-wireline\issi-tools\testsuites\testscripts\testregistry.xml";
      String xmlFile = "testregistry.xml";

      // Test-2
      //File msgFile = new File(xmlFile);         
      String xmlMsg = FileUtility.loadFromFileAsString(xmlFile);

      XmlTestregistry xmldoc = new XmlTestregistry();
      xmldoc.loadTestregistry(xmlMsg);

      // generated data
      CheckBoxList cbList = CheckBoxList.getCheckBoxList();
      ListModel model = cbList.getModel();
      for( int i=0; i < model.getSize(); i++) {
         JCheckBox jcb = (JCheckBox)model.getElementAt(i);
         jcb.setSelected(true);
      }

      boolean mflag = xmldoc.reconcileTestcase( cbList);
      showln("reconcileTestcase: mflag="+mflag);
      xmldoc.saveTestregistry( "testregistry2.xml");
   }
}
