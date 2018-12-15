//
package gov.nist.p25.issi.testlauncher;

/**
 * This describes a stanza in the test case registry. Each test case has 
 * a directory and is described by some text (based on the test document)
 * and has a test number which corresponds to the test document.
 */
public class TestCaseDescriptor {
   
   private String category;
   private String directoryName;
   private String testTitle;
   private String testNumber;
   private String testDescription;   
   private String topologyName;
   private String role;

   // conformance,capp
   private String testClass;

   // accessors
   public String getCategory() {
      return category;
   }

   public String getDirectoryName() {
      return directoryName;
   }
   public String getFileName() {
      return directoryName + "/testscript.xml";
   }

   public String getTestNumber() {
      return testNumber;
   }

   public String getTestTitle() {
      return testTitle;
   }
   public void setTestTitle(String testTitle) {
      this.testTitle = testTitle;
   }

   public String getTestDescription() {
      String tag = testDescription;
      if(role != null && role.length() > 0) {
         tag += "\n\nIUT Role: "+role;
      }
      return tag +
         "\n\nNOTE: Additional signaling for test setup and teardown\n" +
         "may be included in call flow.";
   }
   public void setTestDescription(String testDescription) {
      this.testDescription = testDescription.trim();
   }

   public String getTopologyFileName() {
      return directoryName + "/" + topologyName;
   }   

   public String getRole() {
      return role;
   }

   public String getTestClass() {
      return testClass;
   }
   public void setTestClass(String testClass) {
      this.testClass = testClass;
   }

   // constructor
   public TestCaseDescriptor(String directory, String topologyName,
      String testNumber, String category) {
      this(directory, topologyName, testNumber, category, "conformance", "");
   }
   public TestCaseDescriptor(String directory, String topologyName, 
      String testNumber,
      String category, 
      String role) {
      //String testClass) {
      this(directory, topologyName, testNumber, category, "conformance", role);
      //this(directory, topologyName, testNumber, category, testClass, "");
   }
   public TestCaseDescriptor(String directory, String topologyName, 
      String testNumber,
      String category, 
      String testClass,
      String role)
   {
      this.directoryName = directory;
      this.testNumber = testNumber;
      this.category = category;
      this.topologyName = topologyName;   
      this.testClass = testClass;   
      this.role = role;   
   }

   public String toString() {
      String tag = testNumber +" : ";
      if( testTitle != null && testTitle.length() > 0) {
         tag += testTitle;
      } else {
         tag += directoryName;
      }
      // optional role field
      if(role != null && role.length() > 0) {
         tag += " : " +role;
      }
      return tag;
   }
}
