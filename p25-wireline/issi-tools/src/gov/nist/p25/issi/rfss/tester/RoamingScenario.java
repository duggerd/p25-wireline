//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;

/**
 * Roaming Scenario
 * 
 */
public class RoamingScenario extends AbstractScenario {

   private RfssConfig destinationRfssConfig;
   private SuConfig suConfig;   
   private boolean initiatedByHome;

   public RoamingScenario(RfssConfig destinationRfssConfig, 
         SuConfig suConfig, boolean initiatedByHome) throws Exception {

      this.destinationRfssConfig = destinationRfssConfig;
      this.suConfig = suConfig;
      this.initiatedByHome = initiatedByHome;
      
      setDescription( "Perform the following mobility action \n" +
         "Su to move = " + suConfig.getSuId() + "\n" +
         "Destination Rfss to move to " + destinationRfssConfig.getRfssId() + "\n");   
   }

   /**
    * @return Returns the targetRfss.
    */
   public RfssConfig getDestinationRfssConfig() {
      return destinationRfssConfig;
   }

   /**
    * @return Returns the suConfig.
    */
   public SuConfig getSuConfig() {
      return suConfig;
   }
   
   public boolean isIntiatedByHome() {
      return initiatedByHome;
   }
}
