//
package gov.nist.p25.issi.testlauncher;

import org.apache.log4j.Logger;

/**
 * Progress screen for ISSI Tester tools.
 */
public class ProgressMonitor implements Runnable {
   
   private static Logger logger = Logger.getLogger(ProgressMonitor.class);

   private TestStatusInterface gui;
   private Thread thread;
   
   public ProgressMonitor(TestStatusInterface gui) {
      this.gui = gui;      
      thread = new Thread( this);
      thread.start();
   }
   
   public void run() {      
      gui.setStatusProgressBarIndeterminate(true);      
      while (true) {
         synchronized ( this ) {
            try {
               wait();
            } catch ( InterruptedException ex) {                   
               break;                   
            }
         }          
         String msg = gui.getProgressScreenMessage();
         gui.setStatusLabelText(msg);
      }
      logger.info("Exitting the Progress Monitor thread");
      gui.setStatusProgressBarIndeterminate(false);      
   }
   
   public void done() {
      thread.interrupt();
   }
}
