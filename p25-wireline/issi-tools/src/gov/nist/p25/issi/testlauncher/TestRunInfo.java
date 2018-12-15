//
package gov.nist.p25.issi.testlauncher;

/**
 * Current test case information
 */
public class TestRunInfo {

   private boolean interactive;
   private String date;
   private String directory;
   private String testDirectory;
   private String topology;
   private String testNumber;

   public boolean isInteractive() {
      return interactive;
   }
   public boolean getInteractive() {
      return interactive;
   }
   public void setInteractive(boolean interactive) {
      this.interactive = interactive;
   }

   public String getDate() {
      return date;
   }
   public void setDate(String date) {
      this.date = date;
   }

   public String getDirectory() {
      return directory;
   }
   public void setDirectory(String directory) {
      this.directory = directory;
   }

   public String getTestDirectory() {
      return testDirectory;
   }
   public void setTestDirectory(String testDirectory) {
      this.testDirectory = testDirectory;
   }

   public String getTopology() {
      return topology;
   }
   public void setTopology(String topology) {
      this.topology = topology;
   }

   public String getTestNumber() {
      return testNumber;
   }
   public void setTestNumber(String testNumber) {
      this.testNumber = testNumber;
   }
}
