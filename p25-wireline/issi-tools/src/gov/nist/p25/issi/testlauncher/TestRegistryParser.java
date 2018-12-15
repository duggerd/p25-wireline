//
package gov.nist.p25.issi.testlauncher;

import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.constants.ISSITesterConstants;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * An XML parser to parse the test registry. The test registry stores
 * a seuqence of tags - one for every test case.
 * 
 */
public class TestRegistryParser extends DefaultHandler {

   private XMLReader saxParser;
   private StringBuffer cdatabuffer = new StringBuffer();
   private Hashtable<String,TestCaseDescriptor> testCases; 
   private Vector<TestCaseDescriptor> testCaseList;
   private TestCaseDescriptor testCaseDescriptor;
   private String testRegistryName;
   private String testType;

   // accessor
   public String getTestRegistryName() {
      return testRegistryName;
   }
   public Vector<TestCaseDescriptor> getTestCaseList() {
      return testCaseList;
   }

   class MyResolver implements EntityResolver {
      public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
         if (ISSIDtdConstants.URL_ISSI_DTD_TEST_REGISTRY.equals(systemId)) {
            FileReader fileReader = new FileReader( new File("dtd/testregistry.dtd"));
            InputSource inputSource = new InputSource(fileReader);
            return inputSource;
         } else {
            return null;
         }
      }
   }
   
   @Override
   public void characters(char[] ch, int start, int length) {
      cdatabuffer.append(ch, start, length);
   }

   @Override
   public void endElement(String namespaceURI, String local, String name)
         throws SAXException {
      if ("description".equals(name)) {
         if(testCaseDescriptor != null) {
            testCaseDescriptor.setTestDescription(cdatabuffer.toString());
	 }
         // Reset to ""
         cdatabuffer.setLength(0);
      } 
      else if ("testcase".equals(name)) {
         if(testCaseDescriptor != null) {
            testCases.put(testCaseDescriptor.toString(),testCaseDescriptor);
            testCaseList.add(testCaseDescriptor);
	 }
      }
   }

   @Override
   public void startElement(String namespaceURI, String local, String name,
         Attributes attrs) throws SAXException {
      if ("testcase".equals(name)) {
         String dir= attrs.getValue("testDirectory");
         String testNumber = attrs.getValue("testNumber");
         String testTitle = attrs.getValue("testTitle");
         String category = attrs.getValue("category");
         String topologyName = attrs.getValue("topology");
         String role = attrs.getValue("role");
         if(role == null) 
            role = "";
         String testClass = attrs.getValue("testClass");
         if (topologyName == null)
            topologyName= ISSITesterConstants.TOPOLOGY_XML;

         testCaseDescriptor = null;
         if( testClass == null) {
            // testType: Conformance or CAP or UDP
            if( ISSITesterConstants.TEST_CLASS_CONFORMANCE.equals(testType)) {
               testCaseDescriptor = new TestCaseDescriptor(dir,topologyName,testNumber,category,role);
	    }
	 } else {
            if( testClass.indexOf(testType) != -1) {
               testCaseDescriptor = new TestCaseDescriptor(dir,topologyName,testNumber,category,role);
               testCaseDescriptor.setTestClass(testClass);
	    }
         }
         testCaseDescriptor.setTestTitle(testTitle);
      }
   }
   
   public Hashtable<String,TestCaseDescriptor> parse() throws Exception {
      saxParser.parse(testRegistryName);
      return testCases;
   }
   
   // constructor
   public TestRegistryParser(String testRegistryName) throws Exception {
      this( testRegistryName, "Conformance");
   }
   public TestRegistryParser(String testRegistryName, String testType) throws Exception {
      try {
         this.testRegistryName = testRegistryName;
         this.testType = testType;
         SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
         saxParser = saxParserFactory.newSAXParser().getXMLReader();
         saxParser.setEntityResolver(new MyResolver())   ;
         testCases = new Hashtable<String,TestCaseDescriptor>();
         testCaseList = new Vector<TestCaseDescriptor>();
         saxParser.setContentHandler(this);
         saxParser.setFeature("http://xml.org/sax/features/validation", true);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw ex;
      }
   }
}
