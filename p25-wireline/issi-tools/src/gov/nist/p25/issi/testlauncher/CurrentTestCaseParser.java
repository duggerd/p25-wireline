//
package gov.nist.p25.issi.testlauncher;

import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML parser for current test case record ( stored in the trace directory
 * so people can ship us the entire directory ).
 *
 */
public class CurrentTestCaseParser extends DefaultHandler {

   private TestRunInfo testRunInfo = new TestRunInfo();
   private XMLReader saxParser;

   public CurrentTestCaseParser() throws Exception {
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      saxParser = saxParserFactory.newSAXParser().getXMLReader();
      saxParser.setContentHandler(this);
      saxParser.setFeature("http://xml.org/sax/features/validation", false);
   }

   public TestRunInfo parse(String fileName) throws Exception {
      saxParser.parse(fileName);
      return testRunInfo;
   }

   @Override
   public void startElement(String namespaceURI, String local, String name, 
         Attributes attrs) throws SAXException {
      if ("testcase".equals(name)) {
         String date = attrs.getValue("date");
         String directory = attrs.getValue("testDirectory");
         if (date == null || directory == null)
            throw new SAXException("Missing attribute");
         testRunInfo.setTopology( attrs.getValue("topology"));
         testRunInfo.setDate(date);
         testRunInfo.setDirectory( directory);
         testRunInfo.setTestNumber( attrs.getValue("testNumber"));
         testRunInfo.setTestDirectory( attrs.getValue("scenarioDir"));
         String interactive = attrs.getValue("interactive");
         testRunInfo.setInteractive( "true".equalsIgnoreCase(interactive));
      }
   }
}
