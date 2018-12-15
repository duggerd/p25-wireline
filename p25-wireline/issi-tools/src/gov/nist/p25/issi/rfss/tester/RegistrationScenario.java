//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;


public class RegistrationScenario extends AbstractScenario {

   private SuConfig suConfig;

   // constructor
   public RegistrationScenario(TopologyConfig topology, int wacnId, int systemId, int suId) {
      suConfig = topology.getSuConfig(wacnId, systemId, suId);
   }

   public SuConfig getSuConfig() {
      return suConfig;
   }
}
