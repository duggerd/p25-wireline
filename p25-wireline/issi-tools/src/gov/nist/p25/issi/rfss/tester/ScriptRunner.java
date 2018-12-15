//
package gov.nist.p25.issi.rfss.tester;

import java.util.Vector;
import org.apache.log4j.Logger;

import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.testlauncher.TestCaseDescriptor;
import gov.nist.p25.issi.testlauncher.TestRegistryParser;

public class ScriptRunner extends AbstractScriptRunner {

   private static Logger logger = Logger.getLogger(ScriptRunner.class);

   //======================================================================
   public static void main(String[] args) throws Exception {

      // Check input:
      if (args.length != 1) {
         System.out.println("Usage: ScriptRunner -registry");
         System.out.println("Usage: ScriptRunner -test=<scriptName> ");
         System.exit(-1);
      }

      if ( args[0].equals("-registry" ) ) {
         String fileName = args[1];
         TestRegistryParser testRegistryParser = new TestRegistryParser(fileName);
         testRegistryParser.parse();
         Vector<TestCaseDescriptor> testRegistry = testRegistryParser.getTestCaseList();
         for ( TestCaseDescriptor testCaseDescriptor : testRegistry) {
            scriptName = testCaseDescriptor.getDirectoryName();
            logger.info("Running test case " + scriptName);
            junit.textui.TestRunner.run(ScriptRunner.class);
         }
      } else {
         scriptName = args[0];
         junit.textui.TestRunner.run(ScriptRunner.class);
      }
      Thread.sleep(ISSITesterConstants.TEST_RUNS_FOR*1000);
   }
}
