//
package gov.nist.p25.issi.testlauncher;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.constants.ISSITesterConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;


public class TesterHelper
{
   private static Logger logger = Logger.getLogger(TesterHelper.class);
   public static void showln(String s) { System.out.println(s); }

   //----------------------------------------------------------------------
   public static void saveFiles(boolean verbose) throws Exception {

      logger.debug("saveFiles(): verbose="+verbose);         
      // file doesnot exist until test is run
      String currentTestCaseFile = "";
      try {
         String directoryName = ISSITesterConstants.DEFAULT_LOGS_DIR;
         logger.debug("saveFiles(): directoryName="+directoryName);         

         File logDir = new File(directoryName);
         currentTestCaseFile =  new File(logDir.getCanonicalPath()
            + "/" + ISSITesterConstants.CURRENT_TEST_CASE_XML).toURI().toURL().toString();
         logger.debug("currentTestCaseFileName " + currentTestCaseFile);
         
         // FileNotFoundException if test is not run yet
         TestRunInfo testRun = new CurrentTestCaseParser().parse(currentTestCaseFile);
         boolean interactive = testRun.isInteractive();
         String testSuiteName = new File(testRun.getTestDirectory())
               .getParentFile().getName();

	 // testrun or refmessages
         String targetDirName = (interactive ? ISSITesterConstants.DEFAULT_TEST_RUN_DIRECTORY
            : ISSITesterConstants.DEFAULT_TRACES_DIR)
            + "/" + testSuiteName + "/"
            + testRun.getTopology().substring(0, testRun.getTopology().indexOf(".xml")) 
            + ".logs";

         logger.debug("testRun.interactive = " + interactive);
         logger.debug("saving to refmessages directory " + targetDirName);
         File targetDir = new File(targetDirName);
         targetDir.mkdirs();

         // Copy *.ptt , *.xml, *.sip to the target location 
         //showln("*** saveFiles: copy .ptt .xml .sip to refmsg dir...");
         //showln("*** saveFiles: from logDir="+directoryName);
         for (File file : logDir.listFiles(new FilenameFilter() {

            public boolean accept(File file, String name) {
               return name.endsWith("sip") || name.endsWith("ptt")
                     || name.endsWith("xml");
            }
         })) {

            //logger.debug("copyFile="+file.getAbsolutePath());
            File targetFile = new File(targetDirName + "/" + file.getName());
            if (targetFile.exists()) {
               targetFile.delete();
            }

            // NOTE: the SipStack owns the message files, cannot rename
            FileUtility.copyFile( file, targetFile);

	    // keep logs/current-test-case.xml
	    if( file.getName().indexOf(ISSITesterConstants.CURRENT_TEST_CASE_XML) == -1) {
               file.delete();
               //logger.debug("AFTER COPY: delete="+file.getAbsolutePath());
	    }
         }
         if( verbose) {
            JOptionPane.showMessageDialog( null,
               "Completed saving test traces to:\n\n"+targetDir.getAbsolutePath());
	 }

         // cleanup
         targetDir = null;
         logDir = null;
         
      } catch (FileNotFoundException ex) {
         String msg = "Cannot find file: " + currentTestCaseFile;            
         logger.error( msg, ex);
         if( verbose) {
            JOptionPane.showMessageDialog( null,
               msg + "\n\nYou must run or re-run at least one test...\n\n",
               "Save Current Test Trace",
               JOptionPane.ERROR_MESSAGE);
	 }

      } catch (Exception ex) {
         logger.error("saveFiles(): exception " , ex);
         ex.printStackTrace();
         if( verbose) {
           JOptionPane.showMessageDialog( null,
               "Error in saveFiles: "+ex,
               "saveFiles Exception",
               JOptionPane.ERROR_MESSAGE);
	 }
      }
   }
}
