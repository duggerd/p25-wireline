//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.SuConfig;

@SuppressWarnings("unused")
public class CallTerminator extends AbstractScenario {

   private SuConfig suConfig;
   //private String scriptingEngine;
   //private String script;
   
   public CallTerminator( SuConfig suConfig ) {
      this.suConfig = suConfig;
   }
   
   private TestSU getSU() {
      return (TestSU) suConfig.getSU();
   }
   
   public void terminateCallSegment() {
      getSU().terminateUnitToUnitCall();
   }

   public SuConfig getSuConfig() {
      return suConfig;
   }
}
