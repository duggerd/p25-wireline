//
package gov.nist.p25.issi.testlauncher;

import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;

public interface TestStatusInterface {

   public String getWebConfigurationFileName();
   public ISSITesterConfiguration getIssiTesterConfiguration();

   //-----------------------------
   public void logError(String failureMessage, String errorTrace);
   public void logError(String failureMessage);
   public void logFatal(String failureMessage, String stackTrace);
   public void logStatusMessage(String statusMessage);

   //-----------------------------
   public String getProgressScreenMessage();
   public void setProgressScreenMessage(String message);
   public void setStatusLabelText(String msg);
   public void setStatusProgressBarIndeterminate(boolean state);

   //-----------------------------
   public void saveTestFiles(boolean verbose) throws Exception;
}
