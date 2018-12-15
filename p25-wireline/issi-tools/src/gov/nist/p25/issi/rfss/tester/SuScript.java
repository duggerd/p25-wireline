//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.SuState;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25payload.TransmitPriorityType;

// 14.4.x
import gov.nist.p25.issi.startup.ConfigurationServlet;

import org.apache.log4j.Logger;
import org.python.util.PythonInterpreter;

/**
 * This defines an event for an SU. An SU can specify a script to be executed at
 * a certain time during the run. It gets access to all the methods that are
 * defined in the TestSuInterface.
 * 
 */
public class SuScript extends AbstractScenario
      implements SuScriptInterface
{
   private static Logger logger = Logger.getLogger(SuScript.class);

   private PythonInterpreter interpreter;
   private SuConfig suConfig;
   private String scriptingEngine;
   private String script;
   private String method;
   private String description;

   private TestScript testScript;
   private TopologyConfig topologyConfig;

   // accessor
   public SuConfig getSuConfig() {
      return suConfig;
   }
   public void setSuConfig(SuConfig suConfig) {
      this.suConfig = suConfig;
   }

   public String getScriptingEngine() {
      return scriptingEngine;
   }
   public void setScriptingEngine(String scriptingEngine) {
      this.scriptingEngine = scriptingEngine;
   }

   public String getScript() {
      return script;
   }
   public void setScript(String script) {
      this.script = script;
   }

   public String getMethod() {
      return method;
   }
   public void setMethod(String method) {
      this.method = method;
   }
   
   public String getDescription() {
      return description;
   }

   // constructor
   //-------------------------------------------------------------
   SuScript(TestScript testScript) {
      this.testScript = testScript;
   }

   private TestSU getSU() {
      return (TestSU) suConfig.getSU();
   }

   void init() throws Exception {
      if (scriptingEngine == null) {
         scriptingEngine = "jython";
      }
      if ((!"jython".equals(scriptingEngine)) &&
          (!"python".equals(scriptingEngine))) {
         throw new Exception("Interpreter not supported " + scriptingEngine);
      }
      
      // logger.info("script = " + script);
      interpreter = new PythonInterpreter();
      interpreter.exec("from java.lang import *");
      interpreter.exec("from java.util import *");
      interpreter.exec("from gov.nist.p25.issi.issiconfig import *");
      interpreter.exec("from gov.nist.p25.issi.rfss.tester import *");
      interpreter.exec("global su");
      interpreter.set("su", this);
      interpreter.set("testScript", testScript);
      interpreter.exec(script);
   }

   @Override
   public void setDescription(String description) {
      // %x (hex)
      super.setDescription("Perform the following action on the following SU:\n"
            + "SU ID = " +  Integer.toHexString(suConfig.getSuId()) + "(hex)\n"
	    + "isEmulated : " + suConfig.isEmulated() +"\n" 
	    + description + "\n");
   }
   
   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.SuScriptInterface#terminateCallSegment()
    */
   public void terminateCallSegment() {
      getSU().terminateUnitToUnitCall();
   }

   public void sendHeartbeatQuery() {
      //getSU().getCurrentUnitToUnitCall().getPttSession().getHeartbeatTransmitter().sendHeartbeatQuery();

      // http://host:8763/rfss/control?command=heartbeatQuery
      //logger.debug("SuScript: HTTP sendHeartbeatQuery()...");
      ConfigurationServlet.getDietsService().sendHeartbeatQuery();
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.SuScriptInterface#sendTalkSpurt(int)
    */
   public void sendTalkSpurt(int nblocks) {
      getSU().sendTalkSpurt(nblocks);
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.SuScriptInterface#sendTalkSpurt()
    */
   public void sendTalkSpurt() {
      getSU().sendTalkSpurt();
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.SuScriptInterface#forceTalkSpurt(int)
    */
   public void forceTalkSpurt(int nblocks) {
      getSU().forceTalkSpurt(nblocks);
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.SuScriptInterface#setState(gov.nist.p25.issi.issiconfig.SuState)
    */
   public void setState(SuState suState) {
      getSU().setState(suState);
   }

   public void runScript(TopologyConfig topologyConfig) {
      // We run the script for an emulated Su instance.
      try {
         if (suConfig.getSU() != null &&  suConfig.isEmulated() ) {
            this.topologyConfig = topologyConfig;
            interpreter.set("topologyConfig", topologyConfig);
            interpreter.eval(method + "()");
         } else {
            logger.info("nothing to run");
         }
      } catch (Throwable th) {
         System.err.println("Unexpected error caught , check the SU Script " + th);
      }
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.SuScriptInterface#sendTalkSpurt(java.lang.String, int)
    */
   public void sendTalkSpurt(String groupName, int nblocks) throws Exception {
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (groupConfig == null) {
         throw new Exception("sendTalkSpurt: Group not found " + groupName);
      }
      getSU().sendTalkSpurt(groupConfig.getRadicalName(), nblocks);

   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.SuScriptInterface#sendTalkSpurt(java.lang.String)
    */
   public void sendTalkSpurt(String groupName) throws Exception {
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (groupConfig == null) {
         throw new Exception("sendTalkSpurt: Group not found " + groupName);
      }
      getSU().sendTalkSpurt(groupConfig.getRadicalName());
   }
   
   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.SuScriptInterface#forceTalkSpurt(java.lang.String, int)
    */
   public void forceTalkSpurt(String groupName, int nblocks ) throws Exception {
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (groupConfig == null) {
         throw new Exception("forceTalkSpurt: Group not found " + groupName);
      }
      getSU().forceTalkSpurt(groupConfig.getRadicalName(), nblocks);
   }
   
   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.SuScriptInterface#forceTalkSpurt(java.lang.String)
    */
   public void forceTalkSpurt(String groupName ) throws Exception {
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (groupConfig == null) {
         throw new Exception("forceTalkSpurt: Group not found " + groupName);
      }
      getSU().forceTalkSpurt(groupConfig.getRadicalName());
   }
   
   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.SuScriptInterface#setTransmitPriority(java.lang.String, int)
    */
   public void setTransmitPriority(String type, int level) throws Exception {
      if (level > (2<<4) -1 ) {
         throw new Exception ("setTransmitPriority: Bad level [0..15]: "+level);
      }
      TransmitPriorityType transmitPriority = TransmitPriorityType.getInstance(type);
      getSU().setTransmitPriorityType(transmitPriority);
      getSU().setTransmissionPriorityLevel(level);
   }

   //----------------------------------------------------------------------------
   public void sendMute(String groupName) throws Exception {
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (groupConfig == null) {
         throw new Exception("sendTalkSpurt: Group not found " + groupName);
      }
      getSU().sendMute(groupConfig.getRadicalName());
   }
   public void sendUnmute(String groupName, long msec) throws Exception {
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (groupConfig == null) {
         throw new Exception("sendTalkSpurt: Group not found " + groupName);
      }
      getSU().sendUnmute(groupConfig.getRadicalName(), msec);
   }
   public void sleep(long msec) {
      logger.debug("SuScript: sleep(): "+msec);
      try {
         Thread.sleep(msec);
      } catch(InterruptedException ex) { }
   }
}
