//
package gov.nist.p25.issi.traceverifier;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.ContentList;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
//import gov.nist.p25.issi.rfss.tester.TestMessages;
//import gov.nist.p25.issi.rfss.tester.TestMessagesParser;
import gov.nist.p25.issi.traceviewer.SipMessageData;
import gov.nist.p25.issi.traceviewer.SipTraceFileParser;
import gov.nist.p25.issi.verifier.TestMessages;
import gov.nist.p25.issi.verifier.TestMessagesParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import javax.sip.header.CallIdHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;


public class SipTraceVerifier {

   private static Logger logger = Logger.getLogger(SipTraceVerifier.class);

   public static VerificationRecord testCapturedMessages(
         ISSITesterConfiguration config, String basedir,
         String testSuiteName,
         String testName, String topologyFileName) {

      VerificationRecord record = new VerificationRecord();
      record.testName = testName;
      record.suiteName = testSuiteName;
      String dirName = basedir + "testrun/"
            + testSuiteName
            + "/"
            + topologyFileName.substring(0, topologyFileName
                  .indexOf(".xml")) + ".logs";
      String systemTopologyName = dirName + "/systemtopology.xml";
      try {
         TopologyConfig systemTopology = new SystemTopologyParser(config).parse(systemTopologyName);
         for (RfssConfig rfssConfig : systemTopology.getRfssConfigurations()) {

            String fileName = rfssConfig.getRfssName() + ".messagelog.sip";
            String dirFileName = dirName + "/" + fileName;

            logger.debug("SipTraceVerifier: Directory for captured message = " + dirName);
            File dirFile = new File(dirFileName);
            if (!dirFile.exists()) {
               logger.debug("Not verifying " + rfssConfig.getRfssName());
               logger.debug("File = "  + dirFileName);
               continue;
            }
            logger.debug("trace file name  = " + dirFileName);
            FileReader freader = new FileReader(dirFile);
            BufferedReader breader = new BufferedReader(freader);

            StringBuffer sbuf = new StringBuffer("<sipmessages>\n");
            String line = null;
            while ((line = breader.readLine()) != null) {
               sbuf.append(line);
               sbuf.append("\n");
            }
            sbuf.append("</sipmessages>\n");

            logger.debug("SIP message to parse \n" + sbuf);
            SipTraceFileParser parser = new SipTraceFileParser(
                  new ByteArrayInputStream(sbuf.toString().getBytes()),
                  null, systemTopology);
            parser.parse();
            logger.debug("Parsed!");

            // use List
            List<SipMessageData> records = parser.getRecords();
            testCapturedMessages(records, record, rfssConfig,
                  testSuiteName, testName, topologyFileName);

            logger.info("Status for " + rfssConfig.getRfssName()  + 
                        " status= " + record.statusFlag);
            if ( record.statusFlag == StatusFlag.FAIL)
               return record;

         }
      } catch (Exception ex) {
         record.statusFlag = StatusFlag.NOT_TESTED;
         record.failureReason = "Unexepcted exception occured";
         logger.error("Unexepcted exception occured", ex);
         return record;

      }
      record.statusFlag = StatusFlag.PASS;
      return record;
   }

   private static void testCapturedMessages(
         List<SipMessageData> sipMessageData,
         VerificationRecord record, RfssConfig rfssConfig,
         String testSuiteName, String testName, String topologyFileName) {
      try {

         if ( sipMessageData.size() == 0 ) {
            logger.error("0 size sipMessageData!");
         }
         
         logger.debug("SipMessageData = " + sipMessageData);
         // load the traces for this test.
         String fname = rfssConfig.getRfssName() + ".xml";

	 /**
         String traceFileDir = ISSITesterConstants.DEFAULT_TRACES_DIR
            + "/" + testSuiteName + "/"
            + topologyFileName.substring(0, topologyFileName.indexOf(".xml")) + ".testmessages"; 
	  **/
         String traceFileDir = ISSITesterConstants.getTraceDirectory(testSuiteName,
            topologyFileName.substring(0, topologyFileName.indexOf(".xml"))+".testmessages"); 
         String traceFileName = traceFileDir + "/" + fname;

         TestMessages testMessages;
         if (!new File(traceFileName).exists()) {
            if (logger.isDebugEnabled()) {
               logger.error("Reference Trace file not found for the emulated RFSS "
                     + rfssConfig.getRfssName());
               logger.error("Trace file name = " + traceFileName);
               logger.error("Configured to receive SIP requests at the following address "
                     + rfssConfig.getHostPort());
            }
            return;
         } else {
            testMessages = new TestMessagesParser(traceFileName).parse();
         }

         for (Request request : testMessages.getRequests()) {
            SIPRequest testSipRequest = (SIPRequest) request.clone();
            // Get the template sip request content list.
            ContentList contentList = ContentList.getContentListFromMessage(testSipRequest);
            /*
             * We dont care about the call ID header in making the
             * comparison. However call id header is mandatory and if the
             * request did not have a call id header it would fail in
             * transaction matching anyways.
             */
            testSipRequest.removeHeader(CallIdHeader.NAME);
            /*
             * The contents of the expires header are arbitrary so remove it
             * from the match template.
             */
            boolean expiresRemoved = (ExpiresHeader)request.getHeader(ExpiresHeader.NAME) != null;
            testSipRequest.removeHeader(ExpiresHeader.NAME);
            boolean matchFound = false;

            for (SipMessageData smd : sipMessageData) {

               if (!smd.getToRfssId().equals(rfssConfig.getDomainName())) {
                  logger.debug("rfssId is not matching  conntinue");
                  continue;
               }

               if (smd.getSipMessage() instanceof SIPRequest) {
                  
                  SIPRequest capturedRequest = (SIPRequest) smd.getSipMessage();
                  ContentList contentList1 = ContentList.getContentListFromMessage(capturedRequest);

                  if (((SIPRequest) capturedRequest).match(testSipRequest)
                        && ((!expiresRemoved) || capturedRequest.getExpires() != null)
                        && contentList.match(contentList1)) {
                     matchFound = true;
                     break;
                  }
               }
            }

            if (!matchFound) {
               logger.error("Could not find a match for this message :\n" + testSipRequest);
               logger.debug("messages to search =  " + sipMessageData);
               record.failureReason = rfssConfig.getRfssName() 
                     + ": Could not find a match for this message "
                     + testSipRequest;
               record.statusFlag = StatusFlag.FAIL;
               return;
            }
         }
         for (Response response : testMessages.getResponses()) {

            SIPResponse testSipResponse = (SIPResponse) response.clone();
            testSipResponse.removeHeader(CallIdHeader.NAME);

            ContentList testContentList = ContentList.getContentListFromMessage(testSipResponse);

            boolean expiresRemoved = testSipResponse.getHeader(ExpiresHeader.NAME) != null;
            testSipResponse.removeHeader(ExpiresHeader.NAME);
            response.removeHeader(ExpiresHeader.NAME);
            boolean matchFound = false;
            for (SipMessageData capturedMessage : sipMessageData) {

               if (!capturedMessage.getToRfssId().equals( rfssConfig.getDomainName()))
                  continue;

               SIPResponse capturedResponse = (SIPResponse) capturedMessage.getSipMessage();
               ContentList contentList = ContentList.getContentListFromMessage(capturedResponse);
               testSipResponse.removeHeader(CallIdHeader.NAME);

               if (((SIPResponse) capturedResponse).match(testSipResponse)
                     && ((!expiresRemoved) || capturedResponse.getExpires() != null)
                     && testContentList.match(contentList)) {
                  matchFound = true;
                  break;
               }
            }
            if (!matchFound) {
               record.failureReason = rfssConfig.getRfssName() 
                     + ": Could not find a match for this message:\n "
                     + testSipResponse;
               logger.error(record.failureReason);
               record.statusFlag = StatusFlag.FAIL;
               return;
            }
         }
         record.statusFlag = StatusFlag.PASS;

      } catch (Exception ex) {
         record.failureReason = "A parse error occured while trying to test messages";
         logger.error(record.failureReason, ex);
         return;
      }
   }
}
