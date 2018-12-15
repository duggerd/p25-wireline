//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.rfss.RFSS;

/**
 * Call Setup Test
 */
public class CallSetupTest extends AbstractSipSignalingTest {

   private RfssConfig rfssConfig;
   private long creationTime;

   // constructor
   public CallSetupTest(RfssConfig rfssConfig, TestScript testScript, RFSS rfss)
         throws Exception {
      this.rfssConfig = rfssConfig;
      creationTime = System.currentTimeMillis();
      super.setUp(testScript, rfss.getProvider());
   }

   @Override
   public RfssConfig getRfssConfig() {
      return rfssConfig;
   }

   public long getStartTime() {
      return creationTime;
   }
}
