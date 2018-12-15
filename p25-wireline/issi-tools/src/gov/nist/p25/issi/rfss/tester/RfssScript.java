//
package gov.nist.p25.issi.rfss.tester;

import org.apache.log4j.Logger;
import org.python.util.PythonInterpreter;

import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.rfss.GroupHome;
import gov.nist.p25.issi.rfss.GroupServing;
import gov.nist.p25.issi.rfss.RFSS;
import gov.nist.p25.issi.utils.ThreadedExecutor;

/**
 * This defines the actions that can be initiated by an RFSS abstraction.
 */
public class RfssScript extends AbstractScenario
      implements RfssScriptInterface
{
   private static Logger logger = Logger.getLogger(RfssScript.class);

   private RfssConfig rfssConfig;
   private String script;
   private String method;
   private TestScript testScript;
   private PythonInterpreter interpreter;
   private TopologyConfig topologyConfig;

   // accessor
   public RfssConfig getRfssConfig() { 
      return rfssConfig;
   }
   public void setRfssConfig(RfssConfig rfssConfig) { 
      this.rfssConfig = rfssConfig;
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

   // constructor
   public RfssScript(TestScript testScript) {
      this.testScript = testScript;
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#stopHeartbeatTransmissionFromGroupHome(java.lang.String, java.lang.String)
    */
   public void stopHeartbeatTransmissionFromGroupHome(String groupName,
         String rfssId) throws Exception {

      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (rfssConfig != groupConfig.getHomeRfss()) {
         throw new Exception("Method can only be invoked from group home");
      }
      String radicalName = groupConfig.getRadicalName();
      GroupHome groupHome = rfssConfig.getRFSS().getHomeGroupByRadicalName( radicalName);
      RfssConfig rfssConfig = topologyConfig.getRfssConfigByName(rfssId);
      if (rfssConfig == null) {
         throw new Exception("Cannot find RFSS " + rfssId);
      }
      String domainName = rfssConfig.getDomainName();
      groupHome.stopHeartbeatTransmission(domainName);
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#stopHeartbeatTransmissionFromGroupServing(java.lang.String)
    */
   public void stopHeartbeatTransmissionFromGroupServing(String groupName)
         throws Exception {
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      String radicalName = groupConfig.getRadicalName();
      GroupServing gc = rfssConfig.getRFSS().getServedGroupByRadicalName(radicalName);
      if (gc == null) {
         throw new Exception("Could not find served group");
      }
      gc.stopHeartbeatTransmission();
   }
   
   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#pttHeartbeatQuery(java.lang.String)
    */
   public void pttHeartbeatQuery( String groupName) throws Exception {
      RFSS rfss = rfssConfig.getRFSS();
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      GroupServing gc = rfss.getServedGroupByRadicalName(groupConfig.getRadicalName());
      if (gc == null) { 
         throw new Exception("Could not find served group");
      }
      gc.sendHeartbeatQuery();
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#suHomeQuery(java.lang.String, java.lang.String, java.lang.String)
    */
   public void suHomeQuery(String suName, String forceString, String confirmString)
         throws Exception {

      RFSS rfss = rfssConfig.getRFSS();
      if (rfss != null) {
         SuConfig suConfig = topologyConfig.getSuConfigByName(suName);
         if (suConfig == null) {
            throw new Exception("SU not found " + suName);
         }

         boolean force = "force".equalsIgnoreCase(forceString);
         boolean confirm = "confirm".equalsIgnoreCase(confirmString);
         // Note that we do not explicitly check here for home because
         // we want to simulate configuraiton errors.
         rfss.doHomeQuerySu(suConfig, force, confirm);
      }
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#groupHomeQuery(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
    */
   public void groupHomeQuery(String groupName, String targetRfssName,
         String forceString, String confirmString) throws Exception {
      boolean force = "force".equals(forceString);
      boolean confirm = "confirm".equals(confirmString);
      RFSS rfss = rfssConfig.getRFSS();
      if (rfss != null) {
         GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
         if (groupConfig == null)
            throw new Exception("Named group " +groupName +" not found !");
         RfssConfig targetRfss = topologyConfig.getRfssConfigByName(targetRfssName);
         if (targetRfss == null)
            throw new Exception("Named RFSS " +targetRfssName +" not found !");
         rfss.doHomeQueryGroup(groupConfig, targetRfss, force, confirm);
      }
   }
   
   public void triggerRegistration(String suName, String presenceFlag) throws Exception {
      RFSS rfss = rfssConfig.getRFSS();
      if (rfss != null) {
         SuConfig suConfig = topologyConfig.getSuConfigByName(suName);
         if (suConfig == null)
            throw new Exception("SU not found " + suName);
         rfss.doRegisterRegister(suConfig, new Boolean(presenceFlag).booleanValue());
      }
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#querySu(java.lang.String)
    */
   public void querySu(String suName) throws Exception {
      RFSS rfss = rfssConfig.getRFSS();
      if (rfss != null) {
         SuConfig suConfig = topologyConfig.getSuConfigByName(suName);
         if (suConfig == null)
            throw new Exception("SU not found " + suName);
         rfss.doServingQuerySu(suConfig);
      }
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#queryGroup(java.lang.String)
    */
   public void queryGroup(String groupName) throws Exception {
      GroupConfig sgConfig = topologyConfig.getGroupConfigByName(groupName);
      if (sgConfig == null)
         throw new Exception("Group not found");
      RFSS rfss = rfssConfig.getRFSS();
      if (rfss != null) {
         rfss.doQueryGroup(sgConfig);
      }
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#reInviteRfssToGroup(java.lang.String, java.lang.String)
    */
   public void reInviteRfssToGroup(String groupName, String rfssName)
         throws Exception {
      RfssConfig rfssConfig = topologyConfig.getRfssConfigByName(rfssName);
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (rfssConfig.getRFSS() != null) {
         rfssConfig.getRFSS().doReInviteGroup( groupConfig.getRadicalName(), rfssConfig);
      }
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#changeGroupCallPriority(java.lang.String, int, java.lang.String)
    */
   public void changeGroupCallPriority(String groupName, int newPriority,
         String emergency) throws Exception {

      logger.info("changeGroupCallPriority: group="+groupName+
		  " priority="+newPriority+ " emergency="+emergency);
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (rfssConfig.getRFSS() != null) {
         rfssConfig.getRFSS().doChangeGroupCallPriority(
               groupConfig.getRadicalName(), newPriority,
               "emergency".equals(emergency));
      }
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#enableRfResources()
    */
   public void enableRfResources() throws Exception {
      if (rfssConfig.getRFSS() != null) {
         rfssConfig.getRFSS().doSetRfResourceAvailable(true);
      }
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#disableRfResources()
    */
   public void disableRfResources() throws Exception {
      if (rfssConfig.getRFSS() != null) {
         rfssConfig.getRFSS().doSetRfResourceAvailable(false);
      }
   }

   public void runScript(TopologyConfig topologyConfig) {
      try {
         this.topologyConfig = topologyConfig;
         if (rfssConfig.getRFSS() != null) {
            interpreter.set("topologyConfig", topologyConfig);
            interpreter.eval(this.method + "()");
         } else {
            logger.info("noting to run");
         }
      } catch (Throwable th) {
         rfssConfig.getRFSS().getTestHarness().fail(
               "Unexpected error -- check RFSS Script " + th);
      }
   }

   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#addRtpPort(int)
    */
   public void addRtpPort(int nports) {
      if (rfssConfig.getRFSS() != null) {
         rfssConfig.getRFSS().doAddRtpPort(nports);
      }
   }
   
   /* (non-Javadoc)
    * @see gov.nist.p25.issi.rfss.tester.RfssScriptInterface#removeRtpPort(int)
    */
   public void removeRtpPort ( int ports ) {
      if (rfssConfig.getRFSS() != null) { 
         rfssConfig.getRFSS().removeRtpPort(ports);
      }
   }

   @Override
   public void setDescription(String description) {
      super.setDescription("Perform the following action on the RFSS with the following Idenitity"
            + "\nrfssName = " 
            + this.rfssConfig.getRfssName()
            + "\nrfss Id = "
            + this.rfssConfig.getRfssId()
            + "\nwacnId = "
            + this.rfssConfig.getWacnId()
            + "\nsystemId = "
            + this.rfssConfig.getSystemId()
            + "\nAction to perform : " + description 
            + "\n-------------------------------");
   }

   void init() throws Exception {

      logger.info("initializing RfssScript");
      //logger.info("script = " + script);
      interpreter = new PythonInterpreter();
      interpreter.exec("from java.lang import *");
      interpreter.exec("from java.util import *");
      interpreter.exec("from gov.nist.p25.issi.issiconfig import *");
      interpreter.exec("from gov.nist.p25.issi.rfss.tester import *");
      interpreter.set("rfss", this);
      interpreter.set("testScript", testScript);
      interpreter.exec(script);
   }

   //----------------------------------
   class GroupHomeUnmuteTask implements Runnable {
      private GroupHome groupHome;
      private String domainName;
      private long msec;

      public GroupHomeUnmuteTask(GroupHome groupHome, String domainName, long msec) {
         this.groupHome = groupHome;
         this.domainName = domainName;
         this.msec = msec;
      }
      @Override
      public void run() {
         try {
            // impose delay 
            Thread.sleep(msec);
            groupHome.sendUnmute(domainName);
         } catch (Exception ex) {
            System.out.println("GroupHomeUnmuteTask: " + ex);
         }
      }
   }

   public void muteFromGroupHome(String groupName, String rfssId)
      throws Exception {

      logger.debug("muteFromGroupHome: groupName="+groupName+" rfssid="+rfssId);
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (rfssConfig != groupConfig.getHomeRfss()) {
         throw new Exception("muteFromGroupHome(2): can only be invoked from group home");
      }
      String radicalName = groupConfig.getRadicalName();
      GroupHome groupHome = rfssConfig.getRFSS().getHomeGroupByRadicalName(radicalName);
      RfssConfig rfssConfig = topologyConfig.getRfssConfigByName(rfssId);
      if (rfssConfig == null) {
         throw new Exception("muteFromGroupHome(2): Cannot find RFSS " + rfssId);
      }
      String domainName = rfssConfig.getDomainName();
      //logger.debug("muteFromGroupHome: domainName="+domainName);
      groupHome.sendMute(domainName);
   }

   public void unmuteFromGroupHome(String groupName, String rfssId, long msec)
      throws Exception {

      logger.debug("unmuteFromGroupHome: groupName="+groupName+" rfssid="+rfssId);
      GroupConfig groupConfig = topologyConfig.getGroupConfigByName(groupName);
      if (rfssConfig != groupConfig.getHomeRfss()) {
         throw new Exception("unmuteFromGroupHome(2): can only be invoked from group home");
      }
      String radicalName = groupConfig.getRadicalName();
      GroupHome groupHome = rfssConfig.getRFSS().getHomeGroupByRadicalName(radicalName);
      RfssConfig rfssConfig = topologyConfig.getRfssConfigByName(rfssId);
      if (rfssConfig == null) {
         throw new Exception("unmuteFromGroupHome(2): Cannot find RFSS " + rfssId);
      }
      String domainName = rfssConfig.getDomainName();
      //logger.debug("unmuteFromGroupHome: domainName="+domainName);

      //===Thread.sleep(msec);
      //===groupHome.sendUnmute(domainName);
      new ThreadedExecutor().execute(
          new GroupHomeUnmuteTask(groupHome, domainName, msec));
   }
}
