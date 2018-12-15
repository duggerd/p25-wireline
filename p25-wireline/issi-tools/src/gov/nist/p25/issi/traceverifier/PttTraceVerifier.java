//
package gov.nist.p25.issi.traceverifier;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.apache.log4j.Logger;

import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.traceviewer.MessageData;
import gov.nist.p25.issi.traceviewer.PttMessageData;
import gov.nist.p25.issi.traceviewer.PttTraceLoader;

@SuppressWarnings("unused")
public class PttTraceVerifier {
   
   private static final Logger logger = Logger.getLogger(PttTraceVerifier.class);

   public static VerificationRecord testCapturedMessages(
         ISSITesterConfiguration config, String basedir, String testSuiteName,
         String testName, String topologyFileName) {

      VerificationRecord record = new VerificationRecord();
      record.testName = testName;
      record.suiteName = testSuiteName;
      try {
         String fileName = "messagelog.ptt";
         String dirName = basedir + "testrun/" + testSuiteName + "/"
               + topologyFileName.substring(0, topologyFileName
                     .indexOf(".xml")) + ".logs";
         String dirFileName = dirName + "/" + fileName;

         logger.debug("PttTraceVerifier: Directory for captured message = " + dirName);
         File dirFile = new File(dirFileName);
         if (!dirFile.exists())
            return record;

         logger.debug("trace file name  = " + dirFileName);
         FileReader freader = new FileReader(dirFile);
         BufferedReader breader = new BufferedReader(freader);
         String systemTopologyName = dirName + "/systemtopology.xml";
         TopologyConfig systemTopology = new SystemTopologyParser(config)
               .parse(systemTopologyName);
         
         StringBuffer sbuf = new StringBuffer().append("<pttmessages>\n");
         String line = null;
         while ((line = breader.readLine()) != null) {
            sbuf.append(line + "\n");
         }     
         sbuf.append("</pttmessages>\n");
         freader.close();
         
         byte[] bytes = sbuf.toString().getBytes();
         ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
         PttTraceLoader pttTraceLoader = new PttTraceLoader(bais);

         String refDirName = ISSITesterConstants.getTraceDirectory(testSuiteName,
            topologyFileName.substring(0, topologyFileName.indexOf(".xml"))+".logs");
         dirFileName = refDirName + "/" + fileName;

         if (!new File(dirFileName).exists()) {
            logger.debug("Reference messages not found -- returning");
            return record;
         }
         logger.debug("trace file name  = " + dirFileName);

         freader = new FileReader(dirFile);
         breader = new BufferedReader(freader);
         systemTopologyName = refDirName + "/systemtopology.xml";

         sbuf = new StringBuffer().append("<pttmessages>\n");
         line = null;
         while ((line = breader.readLine()) != null) {
            sbuf.append(line + "\n");
         }
         sbuf.append("</pttmessages>\n");
         bytes = sbuf.toString().getBytes();
         bais = new ByteArrayInputStream(bytes);
         PttTraceLoader pttRefTraceLoader = new PttTraceLoader(bais);

         for (RfssConfig rfssConfig : systemTopology.getRfssConfigurations()) {

            List<MessageData> refMessages = pttRefTraceLoader
                  .getRecords(rfssConfig.getDomainName());
            if (refMessages.isEmpty()) {
               logger.debug("No reference messages found for " + rfssConfig.getRfssName());
               continue;
            }

            List<MessageData> capturedMessages = pttTraceLoader.getRecords(rfssConfig.getDomainName());
            for (MessageData messageData : refMessages) {
               PttMessageData pttMessageData = (PttMessageData) messageData;
               record.statusFlag = StatusFlag.FAIL;
               
               for (MessageData messageData1 : capturedMessages) {
                  PttMessageData pttMessageData1 = (PttMessageData) messageData;
                  //+++if (pttMessageData1.match(pttMessageData)) {
                  if ( match( pttMessageData1, pttMessageData)) {
                     record.statusFlag = StatusFlag.PASS;
                     break;
                  }
               }

               if (record.statusFlag != StatusFlag.PASS) {
                  break;
               }
            }
            logger.debug("Ptt Verification status for Rfss " + rfssConfig.getRfssName()
               + " Status " + record.statusFlag);
            if ( record.statusFlag != StatusFlag.PASS)
               break;
         }
         return record;
         
      } catch (Exception ex) {
         logger.error("Unexepcted exception occured", ex);
         return record;
      }
   }

   // transfer from PttMessageData to here
   private static boolean match(PttMessageData ref, PttMessageData that) {
      for ( String name: that.getProperties().stringPropertyNames())
      {
         if ( ref.getProperties().get(name) == null) 
            return false;
         if ( !ref.getProperties().get(name).equals(that.getProperties().get(name)))
            return false;
      }
      return true;
   }
}
