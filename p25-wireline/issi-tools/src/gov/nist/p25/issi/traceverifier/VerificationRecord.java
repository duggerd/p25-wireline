//
package gov.nist.p25.issi.traceverifier;

import gov.nist.p25.issi.testlauncher.TestRunInfo;


public class VerificationRecord {
   
   String suiteName;
   String testName;
   StatusFlag statusFlag = StatusFlag.NOT_TESTED;
   String failureReason;
   
   public StatusFlag getStatus() {
      return statusFlag;
   }
   
   public String getFailureReason() {
      return failureReason;
   }
   
   public String toString() {
      return new StringBuffer()
      .append("suiteName= " + suiteName + "\n")
      .append("testName=" + testName + "\n")
      .append("statusFlag=" + statusFlag + "\n")
      .append("failureReason=" + failureReason + "\n").toString();
   }

   public void generateRecordPage(StringBuffer page, TestRunInfo testRunInfo) {
      
      try {
         String fontColor = "BLACK";
         if (this.statusFlag.equals(StatusFlag.PASS)) {
            fontColor = "GREEN";
         } else if(this.statusFlag.equals(StatusFlag.FAIL)) {
            fontColor = "RED";
         } else if(this.statusFlag.equals(StatusFlag.NOT_TESTED)) {
            fontColor = "BLUE";
         }
            
         page.append("<tr><td>"+this.suiteName+"</td><td>"+testRunInfo.getTestNumber()+"</td><td>"+this.testName+"</td><td>" +
         testRunInfo.getDate()+"</td><td><font color=\""+fontColor+"\">"+this.statusFlag+"</td><td>"+ 
            (this.failureReason == null ? "N/A" :failureReason)+"</td></tr>\n");
            
         
      } catch (Exception e) {
         System.err.println("generate result page failed");
         e.printStackTrace();
      }
   }
}
