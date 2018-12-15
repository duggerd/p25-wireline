//
package gov.nist.p25.issi.issiconfig;

import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfigurationParser;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

import junit.framework.TestCase;

public class ConfigurationTest extends TestCase {

   private static Logger logger = Logger.getLogger("gov.nist.p25");
   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }
   private TopologyConfig topologyConfig;

   public void setUp() throws Exception {
      ISSITesterConfiguration config = new ISSITesterConfigurationParser("unit-test/issi-tester-configuration.xml").parse();
      TopologyConfig systemTopology  = new SystemTopologyParser(config).parse("unit-test/systemtopology.xml");
      TopologyConfig globalTopology = new GlobalTopologyParser(false).parse(systemTopology,
             "unit-test/globaltopology.xml");
      topologyConfig = new TopologyConfigParser().parse(globalTopology, "unit-test/topologytest.xml");
   }

   public void testTopology() {
      int wacnId = 1;
      int systemId = 2;
      SystemConfig systemConfig = topologyConfig.getSysConfig(wacnId, systemId);
      assertNotNull(systemConfig);
      RfssConfig rfssConfig1 = topologyConfig.getRfssConfig(RfssConfig.getDomainName(systemConfig, 1));
      assertNotNull(rfssConfig1);
      RfssConfig testRfss = topologyConfig.getRfssConfig(rfssConfig1.getIpAddress(), rfssConfig1.getSipPort());
      assertSame("Identity check", testRfss , rfssConfig1);
      RfssConfig rfssConfig2 = topologyConfig.getRfssConfig(RfssConfig.getDomainName(2,3,1));
      assertNotNull("RFSS config should not be null", rfssConfig2);

      //assertTrue(rfssConfig2.getPort() == 4060);
      SuConfig suConfig = topologyConfig.getSuConfig(wacnId, systemId, 0x12);
      RfssConfig servingRfss = suConfig.getInitialServingRfss();
      assertTrue(servingRfss == rfssConfig2);
      RfssConfig homeRfss = suConfig.getHomeRfss();
      assertTrue(homeRfss == rfssConfig1);
      System.out.println("testTopology succeeded ");
   }
}
