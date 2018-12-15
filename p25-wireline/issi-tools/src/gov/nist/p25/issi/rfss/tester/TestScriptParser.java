//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.constants.XMLTagsAndAttributes;
import gov.nist.p25.issi.issiconfig.GlobalTopologyParser;
import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SystemTopologyParser;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfigParser;
import gov.nist.p25.issi.p25payload.PacketType;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for the XML test script.
 */
@SuppressWarnings("unused")
public class TestScriptParser extends DefaultHandler
      implements XMLTagsAndAttributes {
   
   private static Logger logger = Logger.getLogger(TestScriptParser.class);
   public static void showln(String s) { System.out.println(s); }
   
   private TestScript retval;
   private XMLReader saxParser;
   private String testScriptName;
   private boolean inPostCondition;
   private StringBuffer cdatabuffer;
   private TopologyConfig topologyConfig;
   private String location;
   private String assertion;

   private SuScript testSuEventScenario;   
   private AbstractScenario currentScenario;
   private boolean inTestSuEvent;
   private boolean inDescription;
   private boolean inTestRfssEvent;
   private RfssScript testRfssEventScenario;
   private String dirName;
   private Trigger currentTrigger;
   private boolean interactive;
   private String topologyFileName;
   private String globalTopology;
   private String systemTopology;

   // Entity resolver -- allows you to work offline
   class MyResolver implements EntityResolver {

      public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
         //logger.info("public id = " + publicId);
         //logger.info("system id = " + systemId);
         if ( ISSIDtdConstants.URL_ISSI_DTD_TESTSCRIPT.equals(systemId)) {
            FileReader fileReader = new FileReader(new File("dtd/testscript.dtd"));
            InputSource inputSource = new InputSource(fileReader);
            return inputSource;
         } else {
            return null;
         }
      }
   }

   public TestScript parse() throws Exception {
      logger.debug("TestScriptParser: parse(): testScriptName="+testScriptName);
      saxParser.parse(testScriptName);
      return retval;
   }

   @Override
   public void startElement(String namespaceURI, String local, String name,
         Attributes attrs) throws SAXException {
      String param = null;
      try {
         //-------------------------------------------------------------------------------
         if (DESCRIPTION.equals(name)) {
            inDescription = true;
            cdatabuffer = new StringBuffer();
            
         //-------------------------------------------------------------------------------
         } else if (TEST_SCRIPT.equals(name)) {

            ISSITesterConfiguration config = TestRFSS.getTesterConfiguration();
            TopologyConfig xsystemTopology = new SystemTopologyParser(config).parse(systemTopology);
            TopologyConfig xglobalTopology = new GlobalTopologyParser(interactive)
                  .parse( xsystemTopology, globalTopology);

            topologyConfig = new TopologyConfigParser().parse( xglobalTopology, topologyFileName);
            retval.setTopologyConfig(topologyConfig);
            String testName = attrs.getValue(TEST_NAME);
            retval.setTestName(testName);
            retval.setTestDirectory(dirName);
            
            param = attrs.getValue(TEST_COMPLETION_DELAY);
            int testCompletionDelay = (param == null ? 0 : Integer.parseInt(param));                  
            retval.setTestCompletionDelay( testCompletionDelay);
            //showln("TestScriptParser: compDelay="+testCompletionDelay);
            
            int base = 10;            
            param = attrs.getValue(TRACE_GENERATION_TIME);
            int traceGenerationTime = (param == null ? base : Integer.parseInt(param));

            ISSITesterConstants.TEST_RUNS_FOR = traceGenerationTime*1000;
            retval.setTraceGenerationTime( traceGenerationTime*1000);
         
            if (null != attrs.getValue(RTP_DEBUG_LOG)) {
               TestScript.rtpDebuglog = attrs.getValue(RTP_DEBUG_LOG);
            }

            if (null != attrs .getValue(RTP_MESSAGE_LOG)) {
               TestScript.rtpMessagelog = attrs.getValue(RTP_MESSAGE_LOG);
            }

            param = attrs.getValue(GENERATE_SIP_TRACE);
            TestScript.generateSipTrace = ("no".equals(param) ? false : true);

            param = attrs.getValue(GENERATE_PTT_TRACE);
            TestScript.generatePttTrace = ("no".equals(param) ? false : true);

         //-------------------------------------------------------------------------------
         } else if (ROAMING.equals(name)) {
            
            String suName = attrs.getValue(SU_NAME);
            if (suName == null) {
               throw new SAXException("Missing required attribute " + SU_NAME);
            }
            SuConfig suConfig = topologyConfig.getSuConfigByName( suName);
	    if (suConfig == null) {
               throw new SAXException("Could not find a global suName " + suName);
            }

            param = attrs.getValue(DESTINATION_RFSS_NAME);
            if (param == null) {
               throw new SAXException("Missing a required attribute "
                     + DESTINATION_RFSS_NAME + " tag = " + name);
            }

            String initiatedBy = attrs.getValue(INITIATED_BY);
            boolean flag = (initiatedBy==null ? false : "home".equals(initiatedBy));

            RfssConfig rfssConfig = topologyConfig.getRfssConfigByName(param);
	    if( rfssConfig == null) {
               throw new SAXException("Missing RfssConfig for " + param);
            }

            RoamingScenario roamingScenario = new RoamingScenario( rfssConfig, suConfig, flag);
            currentScenario = roamingScenario;

            String testId = attrs.getValue(ID);
            roamingScenario.setId(testId);
            if (retval.getScenarios().containsKey(testId)) {
               throw new Exception("Duplicate ID for test action: "+testId);
            }
            retval.getScenarios().put(testId, roamingScenario);
            
         //-------------------------------------------------------------------------------
         } else if (TRIGGER.equals(name)) {

            String type = attrs.getValue(TRIGGER_TYPE);
            String value = attrs.getValue(TRIGGER_VALUE);
            String dependentScenarioName = attrs.getValue(REF_ID);
            //logger.debug("refId for trigger = " + dependentScenarioName);
	    
            AbstractScenario precondition = null;
            if ( dependentScenarioName != null && 
               retval.getScenario(dependentScenarioName) == null) {
               throw new SAXException ("Cannot find dependent scenario: "+dependentScenarioName);
            
            } else if ( dependentScenarioName != null && !TIME.equals(type)) {
               throw new SAXException("ONLY time triggers currently supported with dependencies: " +
                  dependentScenarioName);
            } else if ( dependentScenarioName != null) {
               precondition = retval.getScenario(dependentScenarioName);
               if ( precondition == null) {
                  throw new SAXException("Cannot find precondition: " + dependentScenarioName);
               }
            }
            if ( type == null || value == null) {
               throw new SAXException ("Missing a required attribute TYPE or VALUE");
            }

            if ( MSEC_TIME.equals(type)) {
               currentTrigger = new MsecTimeTrigger(Integer.parseInt(value));

	    } else if ( TIME.equals(type)) {
               currentTrigger = new TimeTrigger(Integer.parseInt(value));
               
            } else if ( type.equalsIgnoreCase("PTT_MESSAGE")) {

               if (value.equalsIgnoreCase("PTT_REQUEST"))
                  currentTrigger = new PttMessageTrigger(PacketType.PTT_TRANSMIT_REQUEST);
	       else if (value.equalsIgnoreCase("PTT_GRANT"))
                  currentTrigger = new PttMessageTrigger(PacketType.PTT_TRANSMIT_GRANT);
	       else if (value.equalsIgnoreCase("PTT_START"))
                  currentTrigger = new PttMessageTrigger(PacketType.PTT_TRANSMIT_START);
	       else if (value.equalsIgnoreCase("PTT_PROGRESS"))
                  currentTrigger = new PttMessageTrigger(PacketType.PTT_TRANSMIT_PROGRESS);
               else if (value.equalsIgnoreCase("PTT_END"))
                  currentTrigger = new PttMessageTrigger(PacketType.PTT_TRANSMIT_END);
               else if (value.equalsIgnoreCase("PTT_MUTE")) 
                  currentTrigger = new PttMessageTrigger(PacketType.PTT_TRANSMIT_MUTE);
               else if (value.equalsIgnoreCase("PTT_UNMUTE")) 
                  currentTrigger = new PttMessageTrigger(PacketType.PTT_TRANSMIT_UNMUTE);
               else if (value.equalsIgnoreCase("PTT_DENY"))
                  currentTrigger = new PttMessageTrigger(PacketType.PTT_TRANSMIT_DENY);
               else if (value.equalsIgnoreCase("PTT_WAIT"))
                  currentTrigger = new PttMessageTrigger(PacketType.PTT_TRANSMIT_WAIT);
               else if (value.equalsIgnoreCase("PTT_HEARTBEAT_QUERY"))
                  currentTrigger = new PttMessageTrigger(PacketType.HEARTBEAT_QUERY);
               else if (value.equalsIgnoreCase("PTT_HEARTBEAT"))
                  currentTrigger = new PttMessageTrigger(PacketType.HEARTBEAT);
               else 
                  throw new SAXException("Unknown PTT message trigger " + value);

            } else if ( type.equalsIgnoreCase("SIP_REQUEST") || 
                        type.equalsIgnoreCase("SIP_RESPONSE")) {
               currentTrigger = new SipMessageTrigger(value);
            } else {
               throw new SAXException ("Unknown tritter type " + type);
            }
            currentScenario.setTrigger(currentTrigger);
            if ( precondition != null ) {
               currentTrigger.addPrecondition(precondition);
            }
            currentTrigger.setScenario( currentScenario);
            
         //-------------------------------------------------------------------------------
         } else if (SU_TO_SU_CALL.equals(name)) {
            
            String id = attrs.getValue(ID);
            if (id == null) {
               throw new SAXException("Missing required attribute " + ID);
            }

            param = attrs.getValue(CALLING_SU_NAME);
            if (param == null) {
               throw new SAXException("Missing required attribute " + CALLING_SU_NAME);
            }
            SuConfig callingSu = topologyConfig.getSuConfigByName(param);

            param = attrs.getValue(CALLED_SU_NAME);
            if (param == null) {
               throw new SAXException("Missing required attribute " + CALLED_SU_NAME);
            }
            SuConfig calledSu = topologyConfig.getSuConfigByName(param);
            
            param = attrs.getValue(IS_EMERGENCY);
            boolean isEmergency = (param == null ? false : Boolean.parseBoolean(param));
         
            int cancelAfter = attrs.getValue(CANCEL_AFTER) == null ||
               attrs.getValue(CANCEL_AFTER).equalsIgnoreCase(UNBOUNDED)? -1 : 
                  Integer.parseInt(attrs.getValue(CANCEL_AFTER));
            int terminateAfter = attrs.getValue(TERMINATE_AFTER) == null
               || attrs.getValue(TERMINATE_AFTER).equalsIgnoreCase(
                  UNBOUNDED) ? -1 : Integer.parseInt(attrs.getValue(TERMINATE_AFTER));
   
            param = attrs.getValue(IS_TERMINATED_BY_CALLED_SERVING);
            boolean isTerminatedByCalledParty = (param == null ? false : Boolean.parseBoolean(param));
            
            String talkSpurtSenderName = attrs.getValue(TALK_SPURT_SENDER);
            
            SuConfig talkSpurtSender = null;
            if (talkSpurtSenderName == null ) {
               talkSpurtSender = null;
            } else if ( talkSpurtSenderName.equals(attrs.getValue(CALLING_SU_NAME))) {
               talkSpurtSender = callingSu;
            } else if ( talkSpurtSenderName.equals(attrs.getValue(CALLED_SU_NAME))) {
               talkSpurtSender = calledSu;
            }
            
            param = attrs.getValue(IS_TALK_SPURT_FORCED);
            boolean forceFlag = (param == null ? false : Boolean.parseBoolean(param));
            
            param = attrs.getValue(IS_PROTECTED_CALL);
            boolean protectionFlag = (param == null ? false : Boolean.parseBoolean(param));
            
            SuToSuCallSetupScenario suToSuCallSetupScenario = new SuToSuCallSetupScenario(
                  callingSu, calledSu, isEmergency, cancelAfter, terminateAfter,
                  isTerminatedByCalledParty, talkSpurtSender, forceFlag, protectionFlag);
            if (talkSpurtSender == callingSu) {
               if ( talkSpurtSender.getHomeRfss().isEmulated())
                  talkSpurtSender.getHomeRfss().setCallingSuInitialTransmitter(true);
            } else if ( talkSpurtSender == calledSu) {
               if ( talkSpurtSender.getHomeRfss().isEmulated())
                  talkSpurtSender.getHomeRfss().setCallingSuInitialTransmitter(false);
            }
            suToSuCallSetupScenario.setTrigger( currentTrigger);
            suToSuCallSetupScenario.setId(id);

            if (retval.getScenarios().containsKey(id)) {
               throw new Exception("Duplicate key for test action " + id);
            }
            retval.getScenarios().put(id, suToSuCallSetupScenario);
            currentScenario = suToSuCallSetupScenario;
            
            if (terminateAfter != -1) {
               SuConfig terminatingSu = isTerminatedByCalledParty ? calledSu : callingSu;
               CallTerminator callTerminator = new CallTerminator(terminatingSu);
               String scenarioName = id+"-call-terminator";
               callTerminator.setId(scenarioName);
               TimeTrigger timeTrigger = new TimeTrigger(terminateAfter);
               timeTrigger.addPrecondition(suToSuCallSetupScenario);
               callTerminator.setTrigger(timeTrigger);
               timeTrigger.setScenario(callTerminator);
               callTerminator.setDescription("Hang up the SU to SU call from SU %x" +
                     Integer.toHexString( terminatingSu.getSuId()) + 
                     " (isEmulated = " + terminatingSu.isEmulated() + ")");
               retval.getScenarios().put(scenarioName,callTerminator);
            }

         //-------------------------------------------------------------------------------
         } else if (GROUP_CALL.equals(name)) {

            String id = attrs.getValue(ID);         
            if (id == null) {
               throw new SAXException("Missing required attribute " + ID);
            }
            String calledGroupName = attrs.getValue(CALLED_GROUP_NAME);
            if (calledGroupName == null) {
               throw new SAXException("Missing required attribute " + CALLED_GROUP_NAME);
            }
            GroupConfig calledGroup = topologyConfig.getGroupConfigByName(calledGroupName);
            if (calledGroup == null) {
               throw new SAXException("Missing GroupConfig for calledGroup: " + calledGroupName);
            }

            String callingSuName = attrs.getValue(CALLING_SU_NAME);
            if (callingSuName == null) {
               throw new SAXException("Missing required attribute " + CALLING_SU_NAME);
            }
            SuConfig callingSu = topologyConfig.getSuConfigByName(callingSuName);
            if (callingSu == null) {
               throw new SAXException("Missing SuConfig for callingSuName: " + callingSuName);
            }

            param = attrs.getValue(PRIORITY);
            int priority = (param == null ? 1 : Integer.parseInt(param));

            param = attrs.getValue(IS_EMERGENCY);
            boolean isEmergency = (param == null ? false : Boolean.parseBoolean(param));

            param = attrs.getValue(IS_CONFIRMED);
            boolean isConfirmed = (param == null ? false : Boolean.parseBoolean(param));

            int cancelAfter = attrs.getValue(CANCEL_AFTER) == null
                  || attrs.getValue(CANCEL_AFTER).equalsIgnoreCase( UNBOUNDED) ? -1 : 
                     Integer.parseInt(attrs.getValue(CANCEL_AFTER));
            int terminateAfter = attrs.getValue(TERMINATE_AFTER) == null
                  || attrs.getValue(TERMINATE_AFTER).equalsIgnoreCase( UNBOUNDED) ? -1 : 
                     Integer.parseInt(attrs.getValue(TERMINATE_AFTER));
   
            param = attrs.getValue(IS_TALK_SPURT_FORCED);
            boolean talkSpurtForced = (param == null ? false : Boolean.parseBoolean(param));
            
	    param = attrs.getValue(IS_TALK_SPURT_SENT_AFTER_CALL_SETUP);
            boolean isTalkSpurtSentAfterCallSetup = (param == null ? false : Boolean.parseBoolean(param));
            
            param = attrs.getValue(IS_PROTECTED_CALL);
            boolean isProtected = (param == null ? false : Boolean.parseBoolean(param));

            GroupCallSetupScenario scenario = new GroupCallSetupScenario(
                  callingSu, calledGroup, priority, cancelAfter,
                  terminateAfter, isEmergency, isConfirmed, isTalkSpurtSentAfterCallSetup,
                  talkSpurtForced, isProtected);
               
            scenario.setTrigger(currentTrigger);
            scenario.setId(id);
            if (retval.getScenarios().containsKey(id)) {
               throw new Exception("Duplicate id for test action " + id);
            }
            retval.getScenarios().put(id, scenario);
            currentScenario = scenario;
            
         } else if (POST_CONDITION.equalsIgnoreCase(name)) {

            inPostCondition = true;
            location = attrs.getValue("locationSelector");
            assertion = attrs.getValue("assertion");
            cdatabuffer = new StringBuffer();

         //-------------------------------------------------------------------------------
         } else if (SU_SCRIPT.equalsIgnoreCase(name)) {

            String id = attrs.getValue(ID);
            if (id == null) {
               throw new Exception("Missing a required attribute : " + ID);
            }
            if (retval.getScenarios().containsKey(id)) {
               throw new Exception("Duplicate key for test action " + id);
            }
            
            String suName = attrs.getValue(SU_NAME);
            if (suName == null) {
               throw new Exception("Missing required attribute : " + SU_NAME);
            }

            String method = attrs.getValue(METHOD);
            if (method == null) {
               throw new Exception("Missing a required attribute : " + METHOD);
            }
            
            testSuEventScenario = new SuScript(retval);
            inTestSuEvent = true;
            cdatabuffer = new StringBuffer();
            String description = attrs.getValue(DESCRIPTION);

            SuConfig suConfig = topologyConfig.getSuConfigByName(suName);
            testSuEventScenario.setId(id);
            testSuEventScenario.setSuConfig(suConfig);
            testSuEventScenario.setMethod(method);
            testSuEventScenario.setTrigger(currentTrigger);
            if (description != null) {
               testSuEventScenario.setDescription(description);
            }
            currentScenario = testSuEventScenario;
            
         //-------------------------------------------------------------------------------
         } else if (RFSS_SCRIPT.equalsIgnoreCase(name)) {

            String id = attrs.getValue(ID);
            String description = attrs.getValue(DESCRIPTION);

            if (id == null) {
               throw new Exception("Missing a required attribute : " + ID);
            }
            if (retval.getScenarios().containsKey(id)) {
               throw new Exception("Duplicate key for test action " + id);
            }
            
            String rfssname = attrs.getValue(RFSS_NAME);
            if (rfssname == null) {
               throw new Exception("Missing a required attribute : " + RFSS_NAME);
            }
            if (rfssname.indexOf(":") != -1) {
            
               String[] parts = rfssname.split(":");
               String gname = parts[0];
               String operation = parts[1];

               if ("groupHome".equals(operation)) {
                  GroupConfig groupConfig = topologyConfig.getGroupConfigByName(gname);
                  if (groupConfig == null)
                     throw new Exception("Group not found " + gname);
                  rfssname = groupConfig.getHomeRfss().getRfssName();

               } else if ("homeRfss".equals(operation)) {
                  SuConfig suConfig = topologyConfig.getSuConfigByName(gname);
                  if (suConfig == null)
                     throw new Exception("SU not found " + gname);
                  rfssname = suConfig.getHomeRfss().getRfssName();

               } else if ("initialServingRfss".equals(operation)) {
                  SuConfig suConfig = topologyConfig.getSuConfigByName(gname);
                  if (suConfig == null)
                     throw new Exception("SU not found " + gname);
                  rfssname = suConfig.getInitialServingRfss().getRfssName();
               } else {
                  throw new Exception("Unknown operation "+operation);
               }
            }

            String method = attrs.getValue(METHOD);
            if (method == null) {
               throw new Exception("Missing a required attribute : " + METHOD);
            }
            
            testRfssEventScenario = new RfssScript(retval);
            inTestRfssEvent = true;
            cdatabuffer = new StringBuffer();
            RfssConfig rfssConfig = topologyConfig.getRfssConfigByName(rfssname);
            if (rfssConfig == null) {
               throw new Exception("Rfss not found " + rfssname);
            }
            testRfssEventScenario.setId(id);
            testRfssEventScenario.setRfssConfig(rfssConfig);
            testRfssEventScenario.setMethod(method);
            testRfssEventScenario.setTrigger(currentTrigger);
            testRfssEventScenario.setDescription(description);
            currentScenario = testRfssEventScenario;

         //-------------------------------------------------------------------------------
         } else if ("script".equalsIgnoreCase(name)) {
            logger.debug("script");
         }
      } catch (Exception ex) {
         logger.fatal("Error Parsing Test Script", ex);
         throw new SAXException( "Missing required information in confiuration file ", ex);
      }
   }

   @Override
   public void characters(char[] ch, int start, int length) {
      if (inPostCondition || inTestSuEvent || inTestRfssEvent || inDescription)
         cdatabuffer.append(ch, start, length);
   }

   @Override
   public void endElement(String namespaceURI, String local, String name)
         throws SAXException {
      //---------------------------------------------------------------------------
       if (POST_CONDITION.equalsIgnoreCase(name)) {
         inPostCondition = false;
         try {
            if (cdatabuffer.length() > 0) {
               PostCondition pc = new PostCondition(
                     assertion, cdatabuffer.toString(), location);
               retval.addPostCondition(pc);
            }
         } catch (Exception ex) {
            logger.fatal("Error parsing test script", ex);
            throw new SAXException(
                  "Could not instantiate scripting engine for predicate", ex);
         }
      //---------------------------------------------------------------------------
      } else if (SU_SCRIPT.equalsIgnoreCase(name)) {
         testSuEventScenario.setTrigger(currentTrigger);         
         inTestSuEvent = false;
         if (cdatabuffer.length() > 0) {
            testSuEventScenario.setScript(cdatabuffer.toString());
            try {
               testSuEventScenario.init();
               retval.getScenarios().put(testSuEventScenario.getId(),
                     testSuEventScenario);
            } catch (Exception e) {
               e.printStackTrace();
               throw new SAXException("Could not instantiate the su script ", e);
            }
         }
      //---------------------------------------------------------------------------
      } else if (RFSS_SCRIPT.equalsIgnoreCase(name)) {
         inTestRfssEvent = false;
         testRfssEventScenario.setTrigger(currentTrigger);
         if (cdatabuffer.length() > 0) {
            try {
               testRfssEventScenario.setScript( cdatabuffer.toString());
               testRfssEventScenario.init();
               retval.getScenarios().put(testRfssEventScenario.getId(),
                     testRfssEventScenario);
            } catch (Exception e) {
               e.printStackTrace();
               throw new SAXException("Could not instantiate the rfss script ", e);
            }
         }
      }
   }

   //------------------------------------------------------------------------------
   // constructor
   public TestScriptParser(String testScriptFileName, String topologyFileName,
      String globalTopology, String systemTopology, boolean interactive) 
      throws Exception {

      try {
         this.retval = new TestScript();
         this.interactive = interactive;
         this.retval.setTraceCaptureEnabled(!interactive);
         this.globalTopology = globalTopology;
         this.systemTopology = systemTopology;
         this.testScriptName = testScriptFileName;
         this.topologyFileName = topologyFileName;

         logger.debug("Parsing " + testScriptFileName);
	 /***
         logger.debug("  topologyFileName=" + topologyFileName);
         logger.debug("  GlobalTopology=" + globalTopology);
         logger.debug("  systemTopology=" + systemTopology);
         logger.debug("  interactive=" + interactive);
	  ***/
         
         File file = new File(testScriptFileName);
         this.dirName = file.getParent();
         if ( !new File(globalTopology).exists() || !new File(globalTopology).isFile()) {
            logger.error("File not found " + globalTopology);
            throw new IOException ("File not found " + globalTopology);
         }
         
         if ( !new File(systemTopology).exists() || !new File(systemTopology).isFile()) {
            logger.error("File not found " + systemTopology);
            throw new IOException ("File not found " + systemTopology);
         }
         
         if ( !new File(testScriptFileName).exists()|| !new File(testScriptFileName).isFile()) {
            logger.error("File not found " + testScriptFileName);
            throw new IOException ("File not found " + testScriptFileName);
         }
         SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
         saxParser = saxParserFactory.newSAXParser().getXMLReader();
         saxParser.setEntityResolver(new MyResolver());
         saxParser.setContentHandler(this);
         saxParser.setFeature("http://xml.org/sax/features/validation", true);
         
      } catch (Exception pce) {
         // Parser with specified options can't be built
         pce.printStackTrace();
         logger.error("TestScriptParser: " + pce.toString());
         throw pce;
      }
   }
}
