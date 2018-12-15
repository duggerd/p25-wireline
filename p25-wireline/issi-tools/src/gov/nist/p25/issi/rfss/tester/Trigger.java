//
package gov.nist.p25.issi.rfss.tester;

import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;

/**
 * A trigger that fires an abstract scripting action
 * 
 */
public abstract class Trigger {

   private static Logger logger = Logger.getLogger(Trigger.class);

   private boolean oneShot;
   private Object matchValue;
   private AbstractScenario scenario;
   private HashSet<AbstractScenario> preconditions;
   
   // constructor
   public Trigger( Object value) {
      this.matchValue = value;
      oneShot  = true;
      preconditions = new HashSet<AbstractScenario>();
   }
   
   public boolean isOneShot() {
      return oneShot;
   }

   public Object getMatchValue() {
      return matchValue;
   }
   
   public void addPrecondition(AbstractScenario scenario) {
      logger.debug("adding precondition " + scenario.getId());
      preconditions.add(scenario);
      scenario.addPostTrigger((TimeTrigger)this);
   }
   
   public Collection <AbstractScenario> getPreConditions() {
      return preconditions;
   }
   
   public void setScenario(AbstractScenario scenario) {
      this.scenario = scenario;
   }
   
   public AbstractScenario getScenario() {
      return scenario;
   }

   public abstract boolean isReady(String rfssDomainName, Object matchObject);

}
