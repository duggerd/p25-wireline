//
package gov.nist.p25.issi.rfss.tester;

/**
 * Time Trigger
 */
public class TimeTrigger extends Trigger {

   private int time;
   
   public TimeTrigger(int value ) {
      super( new Integer(value).toString());
      this.time = value;
   }

   @Override
   public String toString () {
      return new StringBuffer("time = " + time).toString();
   }

   /**
    * @return the time
    */
   public int getTime() {
      return time;
   }
   
   @Override 
   public boolean isReady(String rfssDomainName, Object matchObject) {
      return true;
   }
}
