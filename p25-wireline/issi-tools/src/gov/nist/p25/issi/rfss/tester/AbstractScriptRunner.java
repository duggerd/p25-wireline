//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.RfssConfig;


public abstract class AbstractScriptRunner extends AbstractSipSignalingTest {

   //TODO: use getter/setter
   protected static String scriptName;
   protected static String localTopologyName;
   
   /***
   private String scriptName;
   private String localTopologyName;

   // accessor
   public String getScriptName() {
      return scriptName;
   }
   public void setScriptName(String scriptName) {
      this.scriptName = scriptName;
   }

   public String getLocalTopologyName() {
      return localTopologyName;
   }
   public void setLocalTopologyName(String localTopologyName) {
      this.localTopologyName = localTopologyName;
   }
    ***/
   
   public void setUp() throws Exception {

      String systemtopology = ISSITesterConstants.getScenarioDir() + "/../systemtopology.xml";
      TestScript testConfig = new TestScriptParser(
         ISSITesterConstants.getScenarioDir() + "/" + scriptName + "/testscript.xml",
         ISSITesterConstants.getScenarioDir() + "/" + scriptName +  "/" + localTopologyName,
         ISSITesterConstants.getGlobalTopologyFileName(),
         systemtopology, false).parse();

      testConfig.setTraceCaptureEnabled(true);
      //assertTrue(testConfig != null);
      super.setUp(testConfig);
   }

   @Override
   public RfssConfig getRfssConfig() {
      return null;
   }
}
