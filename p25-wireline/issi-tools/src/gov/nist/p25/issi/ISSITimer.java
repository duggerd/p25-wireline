//
package gov.nist.p25.issi;

import java.util.Timer;

/**
 * Global timer to be used everywhere. When the test is stopped 
 * the timer is killed.
 */
public  class ISSITimer {
   
   private static Timer timer = new Timer();
   
   /**
    * get the timer.
    * 
    * @return the current timer instance
    */
   public static Timer getTimer() {
      return timer;
   }
   
   /**
    * Reset the timer to a new timer instance.
    */
   public static void resetTimer() {
      synchronized (ISSITimer.class) {
         timer.cancel();
         timer= new Timer();
      }
   }
}
