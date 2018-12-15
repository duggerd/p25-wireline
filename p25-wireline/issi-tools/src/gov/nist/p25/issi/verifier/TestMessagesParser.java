//
//package gov.nist.p25.issi.rfss.tester;
package gov.nist.p25.issi.verifier;

import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.utils.ProtocolObjects;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A parser for parsing the stored trace messages. These messages are compared
 * against the messages that are captured during test execution time. Each of
 * the messages in the stored trace file should match the trace messages
 * captured at test execution time.
 */
public class TestMessagesParser extends DefaultHandler {

   private static Logger logger = Logger.getLogger(TestMessagesParser.class);

   private StringBuffer cdatabuffer;
   private XMLReader saxParser;
   private TestMessages testMessages;
   private String testMessagesFile;

   // Entity resolver -- allows you to work offline
   class MyResolver implements EntityResolver {

      public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
         if (ISSIDtdConstants.URL_ISSI_DTD_EXPECTED_MESSAGES.equals(systemId)) {
            FileReader fileReader = new FileReader( new File("dtd/expected-messages.dtd"));
            InputSource inputSource  = new InputSource(fileReader);
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
   public void endElement(String namespaceURI, String local, String name) {
      try {
         if ("siprequest".equals(name)) {
            String requestString = cdatabuffer.toString();
            Request request = ProtocolObjects.messageFactory.createRequest(requestString);
            testMessages.addRequest(request);
            cdatabuffer = new StringBuffer();

         } else if ("sipresponse".equals(name)) {
            String responseString = cdatabuffer.toString();
            Response response = ProtocolObjects.messageFactory.createResponse(responseString);
            testMessages.addResponse(response);
            cdatabuffer = new StringBuffer();

         }
      } catch (Exception ex) {
         ex.printStackTrace();
         logger.fatal("Unexpected exception ", ex);
         //System.exit(0);
      }

   }
   @Override
   public void startElement(String namespaceURI, String local, String name,
         Attributes attrs) throws SAXException {
   }

   public TestMessagesParser(String testMessagesFile) {
      cdatabuffer = new StringBuffer();
      try {
         this.testMessagesFile = testMessagesFile;
         SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
         saxParser = saxParserFactory.newSAXParser().getXMLReader();
         saxParser.setEntityResolver ( new MyResolver() );
         saxParser.setContentHandler(this);
         saxParser.setFeature("http://xml.org/sax/features/validation", true);
         testMessages = new TestMessages();

      } catch (Exception ex) {
         logger.fatal("Unexpected exception", ex);
         ex.printStackTrace();
         System.exit(0);
      }
   }

   public TestMessages parse() {
      try {
         saxParser.parse(testMessagesFile);
         return testMessages;
      } catch (Exception ex) {
         logger.fatal("Unexpected exception", ex);
         ex.printStackTrace();
         System.exit(0);
      }
      return null;
   }
}
