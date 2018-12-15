package gov.nist.p25.issi.traceverifier;

import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfigurationParser;
import gov.nist.p25.issi.rfss.tester.RfssController;
import gov.nist.p25.issi.rfss.tester.TestRFSS;
import gov.nist.p25.issi.testlauncher.CurrentTestCaseParser;
import gov.nist.p25.issi.testlauncher.TesterHelper;
import gov.nist.p25.issi.testlauncher.TestRunInfo;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Verifier {

   private static Logger logger = Logger.getLogger(Verifier.class);

   public static final String OUTPUT_DIR = "unzipped";
   private static String testSuiteName = "conformance";
   private static String testRunDir = "testrun";
   private static RfssController rfssController;
   static {
      PropertyConfigurator.configure("log4j.properties");
   }
   

   public static final void copyInputStream(InputStream in, OutputStream out)
         throws IOException {
      byte[] buffer = new byte[1024];
      int len;
      while ((len = in.read(buffer)) >= 0) {
         out.write(buffer, 0, len);
      }
      in.close();
      out.close();
   }

//   private static Point centerComponent(Component c) {
//
//      Rectangle rc = new Rectangle();
//      rc = c.getBounds(rc);
//
//      Rectangle rs = new Rectangle(Toolkit.getDefaultToolkit()
//            .getScreenSize());
//
//      return new Point((int) ((rs.getWidth() / 2) - (rc.getWidth() / 2)),
//            (int) ((rs.getHeight() / 2) - (rc.getHeight() / 2)));
//
//   }
   
   private static String selectTraceFile() {
      // File chooser used to open the trace file.
      JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
      fileChooser.setDialogTitle("Open trace zip file");
      fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      FileFilter traceFilter = new FileFilter() {
         @Override
         public boolean accept(File pathname) {
            if (pathname.getName().endsWith(".zip")
                  || pathname.isDirectory())
               return true;
            else
               return false;
         }

         @Override
         public String getDescription() {
            return "Startup Properties";
         }
      };
      fileChooser.setFileFilter(traceFilter);

      int returnValue = fileChooser.showOpenDialog(null);
      if (returnValue == JFileChooser.APPROVE_OPTION) {
         return fileChooser.getSelectedFile().getAbsolutePath();
      } else {
         return null;
      }
   }

   //================================================================
   public static void main(String[] args) throws Exception {
      String zipFileName = null;
      if (args.length == 0) {
         zipFileName = selectTraceFile();
      } else {
         for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-traces")) {
               zipFileName = args[++i];
            } else if (args[i].equals("-interactive")) {
               zipFileName = selectTraceFile();
               break;
            }
         }
      }

      if (zipFileName == null || !new File(zipFileName).exists()) {
         logger.error("nothing to verify");
         return;
      }
      logger.info("trace file name = " + zipFileName);
      if (! new File(zipFileName).exists() ) {
         logger.info("Cannot find the file " + zipFileName);
      }
      StringBuffer page = verifyRecords(zipFileName);
      File result = new File("result.html");
      BufferedWriter output = new BufferedWriter(new FileWriter(result));
      output.append(page);
      output.close();
      System.exit(0);
      //showResults();
   }
   
//   private static void showResults() {
//      JEditorPane jep = new JEditorPane();
//
//      jep.setEditable(false);
//      try {
//         jep.setPage("file:./result.html");
//      } catch (IOException e) {
//         logger.error("Unexpected error - could not read readme!",e);
//         return;
//      }
//      JFrame jframe = new JFrame("ISSI Tester Verification Table");
//      jframe.addWindowListener(new WindowAdapter(){   
//         @Override
//         public void windowClosing(WindowEvent arg0) {
//            System.exit(0);
//            
//         }
//      } );
//      JScrollPane jscrollPane = new JScrollPane(
//            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
//            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//      jscrollPane.setViewportView(jep);
//      jframe.getContentPane().add(jscrollPane);
//      jframe.setSize(640, 480);
//      jframe.setLocation(centerComponent(jframe));
//      jframe.setVisible(true);         
//   }
   
   private static StringBuffer verifyRecords(String zipFileName) {
      StringBuffer page =  new StringBuffer("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" dir=\"ltr\" lang=\"en\">\n"+
            "<head><title>ISSI Tester Verfication Result</title><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head>\n"+
            "<body><table frame=\"border\" border=\"1\" align=\"center\" bgcolor=\"#FFFC9B\"><tbody align=\"center\" valign=\"middle\">\n" +
            "<tr><td colspan=\"6\">ISSI Tester Verification Table</td></tr>\n" +
            "<tr><td>Suite Name</td><td>Test Number</td><td>Test Name</td> <td>Date </td> <td>Result</td><td>Failure Reason</td></tr>\n");

      ProgressIndicator progressIndicator =  new ProgressIndicator();      
      ISSITesterConstants.setTestSuite(testSuiteName);

      try {
         ZipFile zipFile = new ZipFile(zipFileName);

         File outputDir = new File(OUTPUT_DIR);
         if (outputDir.exists()) {
            outputDir.delete();
         }
         outputDir.mkdir();
         // outputDir.deleteOnExit();
         // Unzip the given zip file into a directory.
         Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
         while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();

            if (entry.isDirectory()) {
               // Assume directories are stored parents first then
               // children.
               logger.debug("Extracting directory: " + entry.getName());
               // This is not robust, just for demonstration purposes.
               (new File(OUTPUT_DIR + "/" + entry.getName())).mkdir();

            }
         }
         zipEntries = zipFile.entries();
         while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            if (!entry.isDirectory()) {

               logger.debug("Extracting file: " + entry.getName());
               String parentDir = new File(OUTPUT_DIR + "/" + entry.getName()).getParent();
               if (parentDir != null) {
                  if (!new File(parentDir).exists()) {
                     new File(parentDir).mkdirs();
                  }
               }
               copyInputStream(zipFile.getInputStream(entry),
                     new BufferedOutputStream(
                           new FileOutputStream(new File(OUTPUT_DIR
                                 + "/" + entry.getName()))));
            }

         }

         // Parse the Tester configuration. This needs to be defined in a
         // metafile.

         String startupPropsFileName = new File(OUTPUT_DIR + "/startup").list()[0];
         Properties startupProps = new Properties();
         startupProps.load(new FileReader(new File(OUTPUT_DIR + "/startup/"
               + startupPropsFileName)));
         String testerConfigName = OUTPUT_DIR + "/"
               + startupProps.getProperty(DietsConfigProperties.DAEMON_CONFIG_PROPERTY);

         testSuiteName = startupProps.getProperty(DietsConfigProperties.TESTSUITE_PROPERTY);

         // For each directory in the unzipped stuff
         ISSITesterConfiguration config = new ISSITesterConfigurationParser(
               testerConfigName).parse();
         TestRFSS.setTesterConfiguration(config);

         Collection<String> ipAddresses = config.getLocalAddresses();
         rfssController = new RfssController(ipAddresses);
         String testTraceDirs = OUTPUT_DIR + "/" + testRunDir + "/" + testSuiteName;
         File testTracesDirsFile = new File(testTraceDirs);

         String scenarioDir = ISSITesterConstants.getScenarioDir();
         if (testTracesDirsFile.isDirectory()) {
            File[] dirs = testTracesDirsFile.listFiles();
            for (File file : dirs) {
               if (file.isDirectory()) {
                  String testName = file.getName();
                  progressIndicator.setText( testName);
                  
                  logger.debug("TestName = " + testName);
                  String[] topologies = file.list();
                  for (String topology : topologies) {
                     String topologyFileName = testName + "/"
                        + topology.replace(".logs", ".xml");
                     logger.debug("topologyFileName " + topologyFileName);

                     String globalTopologyName = OUTPUT_DIR + "/"
                        + testRunDir + "/" + testSuiteName + "/"
                        + testName + "/" + topology
                        + "/globaltopology.xml";
                     String systemTopologyName = OUTPUT_DIR + "/"
                        + testRunDir + "/" + testSuiteName + "/"
                        + testName + "/" + topology
                        + "/systemtopology.xml";
                     String currentTestCaseName = OUTPUT_DIR + "/"
                        + testRunDir + "/" + testSuiteName + "/"
                        + testName + "/" + topology + "/current-test-case.xml";
                     
                     TestRunInfo record = new CurrentTestCaseParser().parse(currentTestCaseName);
                     
                     String scriptName = testName + "/testscript.xml";
                     // Generate reference trace here.
                     rfssController.loadTest(scenarioDir, scriptName,
                           record.getTestNumber(),
                           topologyFileName, globalTopologyName,
                           systemTopologyName, false, false);
                     rfssController.runTest();

                     for (int i = 0; i < (ISSITesterConstants.TEST_RUNS_FOR + 4000) / 1000; i++) {
                        System.out.print(".");
                        Thread.sleep(1000);
                     }
                     logger.debug("\nSaving Files");
		     TesterHelper.saveFiles(false);

                     logger.debug("Verifying traces....");

                     VerificationRecord verficationRecord = SipTraceVerifier
                           .testCapturedMessages(config, OUTPUT_DIR
                                 + "/", testSuiteName, testName,
                                 topologyFileName);                  
                     
                     if(verficationRecord != null) {
                        verficationRecord.generateRecordPage(page,record);
                     }
                     
                     logger.debug("verification status = " + verficationRecord);
                     Thread.sleep(1000);
                  }
               }
            }
         }                        
         page.append("</tbody></table></body></html>");         
         progressIndicator.done();
         return page;

      } catch (Exception e) {
         return page.append("</tbody></table></body></html>");
      }   
   }
}
