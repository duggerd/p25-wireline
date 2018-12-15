//
package gov.nist.p25.issi.testlauncher;

import java.util.TimerTask;


/**
 * Usage:
 *  int nsecond = 30;
 *  JProgressBar pb = gui.getStatusProgressBar();
 *  indeterminateTimerTask = new IndeterminateTimerTask( pb, nseconds);
 *  getTimer().schedule( indeterminateTimerTask, 0, 1000);
 *  ...
 *  ... <long runing task>
 *  ...
 *  indeterminateTimerTask.done();  
 *
 */
public class IndeterminateTimerTask extends TimerTask {

   public static void showln(String s) { System.out.println(s); }

   private boolean completed = false;
   private int secondsRemaining;
   private TestStatusInterface gui;

   // accessor
   public void done() {
      completed = true;
   }

   // constructor
   public IndeterminateTimerTask(TestStatusInterface gui, int seconds) {
      this.gui = gui;
      this.secondsRemaining = seconds;
      gui.setStatusProgressBarIndeterminate(true);
      //newProgressMonitor();
   }

   public void run() {
      secondsRemaining = secondsRemaining - 1000;
      if (secondsRemaining > 0) {
         String msg = "Running.  Time remaining: "
               + (secondsRemaining / 1000) + " seconds";
         gui.setProgressScreenMessage(msg);
         showln( msg );
      }
      showln("MMM check completed="+completed+" remains="+secondsRemaining);        
      if (completed) secondsRemaining = 0;
      if (secondsRemaining == 0) {
         showln("CANCEL timer: secondsRemaining="+secondsRemaining);
         gui.setStatusProgressBarIndeterminate(false);
         gui.setProgressScreenMessage("Ready.");
         cancel();
      }
   }
}

