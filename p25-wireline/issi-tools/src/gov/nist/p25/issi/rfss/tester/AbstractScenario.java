//
package gov.nist.p25.issi.rfss.tester;

import java.util.Collection;
import java.util.HashSet;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

//import org.apache.log4j.Logger;

/**
 * Represents a node in the XML description for the test case. Each node is a
 * canned action that needs to be performed at a particular time.
 * 
 */
public abstract class AbstractScenario implements Comparable<AbstractScenario> {
   
   //private static Logger logger = Logger.getLogger(AbstractScenario.class);
   
   private Semaphore semaphore = new Semaphore(0);   
   private HashSet<TimeTrigger> postTrigger = new HashSet<TimeTrigger>();
   private String id;
   private Trigger trigger;

   /*
    * The timer task for the scenario.
    */
   private TimerTask timerTask;
   
   /*
    * Test case method to execute.
    */
   private String methodName;
   
   /*
    * The description (prompt string)
    */
   private String description;

   /**
    * @return true if this scenario is time triggered.
    */
   public boolean isTimeTriggered() {
      return trigger instanceof TimeTrigger && trigger.getPreConditions().size() == 0;
   }
   
   /**
    * @return the time trigger instance 
    * (warning -- will throw class cast if not a time trigger)
    */
   public TimeTrigger getTriggerAsTimeTrigger() {
      return (TimeTrigger) trigger;
   }
   
   /**
    * @param id
    *            The id to set.
    */
   public void setId(String id) {
      this.id = id;
   }

   /**
    * @return Returns the id.
    */
   public String getId() {
      return id;
   }

   /**
    * @param methodName
    *            The methodName to set.
    */
   void setMethodName(String methodName) {
      this.methodName = methodName;
   }

   /**
    * @return Returns the methodName.
    */
   public String getMethodName() {
      return methodName;
   }

   public void setTimerTask(TimerTask timerTask) {
      this.timerTask = timerTask;
   }

   public TimerTask getTimerTask() {
      return timerTask;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getDescription() {
      return description;
   }
   
   public  void doNotify() {
      semaphore.release();
   }
   
   public void doWait() {
      try {
         semaphore.acquire();
      } catch ( InterruptedException ex) {
      }
   }
   
   void setTrigger(Trigger trigger) {
      this.trigger = trigger;
   }
   
   public int compareTo(AbstractScenario other) {
      if((this.trigger instanceof TimeTrigger && 
          other.trigger instanceof TimeTrigger) ||
         (this.trigger instanceof MsecTimeTrigger && 
          other.trigger instanceof MsecTimeTrigger) 
        ) {
         TimeTrigger otherTrigger =  (TimeTrigger) other.trigger;
         TimeTrigger thisTrigger = (TimeTrigger) this.trigger;

         return thisTrigger.getTime() == otherTrigger.getTime() ? 0 :
            thisTrigger.getTime() < otherTrigger.getTime() ? -1 : 1;
      } else {
	  throw new UnsupportedOperationException ("Not comparable");
      }
   }

   public Trigger getTrigger() {
      return trigger;
   }
   
   public void addPostTrigger(TimeTrigger postTrigger) {
      this.postTrigger.add(postTrigger);
   }
   
   public Collection<TimeTrigger> getPostTrigger() {
      return postTrigger;
   }
}
