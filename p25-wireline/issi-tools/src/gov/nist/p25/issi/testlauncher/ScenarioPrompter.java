// 
package gov.nist.p25.issi.testlauncher;

import gov.nist.p25.issi.constants.ISSITesterConstants;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.rfss.tester.AbstractScenario;
import gov.nist.p25.issi.rfss.tester.TestScript;

import java.util.PriorityQueue;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;

/**
 * The client side of the application that prompts the user to do the next test action.
 * This implements a barrier synchronization with prompting so that each test action on
 * the server will wait for the user to prompt it to go to the next action.
 * 
 * Put all the test actions in a priority queue and prompt until the priority
 * queue is empty. This is for interactive testing. The priority queue is
 * ordered by time. That is the head of the queue is all the ready tasks.
 * 
 */
public class ScenarioPrompter {
   
   private static Logger logger = Logger.getLogger(ScenarioPrompter.class);
   public static void showln(String s) { System.out.println(s); }
    
   private boolean isPromptMode = false;   
   private DietsGUI gui;
   private PriorityQueue<AbstractScenario> priorityQueue;
   private TestScript testScript;
   private TestControllerInterface remoteTestController;
   private AtomicInteger counter;

   class MyPrompterTask implements Runnable {
      private AbstractScenario scenario;

      MyPrompterTask(AbstractScenario scenario) {
         this.scenario = scenario;
      }

      public void run() {
         logger.debug("Waiting...scenario.id=" + scenario.getId());
         scenario.doWait();

         showln("Prompting...scenario.id=" + scenario.getId());
         logger.debug("Prompting for next action, id="+scenario.getId());
         int answer = JOptionPane.YES_OPTION;
         if( isPromptMode) {
             answer = JOptionPane.showConfirmDialog(gui,
                scenario.getDescription(),
                "Do this action",
                JOptionPane.YES_OPTION);
	 }
         try {
            if (answer == JOptionPane.YES_OPTION) {
               showln("MyPromptTask- run(): scenario="+scenario.getId());
               remoteTestController.signalNextScenario(scenario.getId());
            }
            else {
               remoteTestController.tearDownCurrentTest();
               gui.getTestExecutionPanel().enableAllButtons();
            }
         } catch (Exception e) {
            e.printStackTrace();
            try {  
               remoteTestController.tearDownCurrentTest();            
            } catch(Exception ex) { }
            logger.error("Error communicating with the remote tester", e);
            gui.logError("Error in communicating with the remote tester");
            priorityQueue.clear();
            return;
         }
         // The counter insures that the start prompting call is
         // implemented only once.
         if (ScenarioPrompter.this.counter.decrementAndGet() == 0) {
            startPrompting();
         }
      }
   }

   /**
    * A class that waits for test execution to complete. This is basically an
    * open loop timer. It is used in non-interactive execution.
    */
   class TestCompletionTimerTask extends TimerTask {

      private boolean flag;
      private boolean completed;
      private int secondsRemaining;
      private int completionSeconds;      
      private DietsGUI gui;

      public TestCompletionTimerTask(DietsGUI gui, int seconds,
            int completionSeconds) {
         this.gui = gui;
         this.secondsRemaining = seconds;
         this.completionSeconds = completionSeconds;
         gui.setStatusProgressBarIndeterminate(true);
         gui.getTestExecutionPanel().newProgressMonitor();
      }

      public void run() {
         try {
            secondsRemaining = secondsRemaining - 1000;
            if( secondsRemaining > 0) {
               String msg = "Time remaining: " + (secondsRemaining/1000) + " seconds";
               gui.setProgressScreenMessage(msg);
            }
            if (!completed)
               completed = remoteTestController.isTestCompleted();
            
            showln("===run: completed=" + completed +
               " remains=" + secondsRemaining + "  completionDelay=" + completionSeconds);

            if (secondsRemaining <= 0) {
               if (gui.getEvaluatePassFail()) {

                  boolean pass = remoteTestController.getTestResults();
                  String passStr = "Test Result: " + 
                     (pass ? ISSITesterConstants.PASSED : ISSITesterConstants.FAILED);

                  // Display result in status line
                  gui.setProgressScreenMessage(passStr);
                  if( isPromptMode) {
                     JOptionPane.showMessageDialog( null,
                        pass ? passStr : passStr
                           + "\nSee Error Messages for more info");
		  }
                  
                  // check for failed test
                  gui.getTestExecutionPanel().setSipTestError(null);
                  if (!pass) {
                     String errorLog = remoteTestController.getErrorLog();                  
                     gui.logError(errorLog);
                     gui.getTestExecutionPanel().setSipTestError(errorLog);                     
                     // NOTE: cannot log until TracePanel is created later
                  }
               } else {
                  // no-op
               }
               gui.setProgressScreenMessage("Getting SIP and PTT traces");
               gui.getTestExecutionPanel().getSipPttTraces(true);               
               gui.getTestExecutionPanel().enableAllButtons();
               cancel();
               
            } else if (completed) {
               if ( ! flag ) secondsRemaining = completionSeconds;
               flag = true;
               showln("*** TaskCompletionTimerTask delay- reset to: "+secondsRemaining);

            }
         } catch (Exception ex) {
            cancel();
         }
         //showln("*** TaskCompletionTimerTask ...DONE");
      }
   }

   /**
    * Signal that the test is complete and that the server should evaluate the
    * predicates.
    */
   public void signalTestCompletion() throws Exception {
      
      if( remoteTestController != null)
          remoteTestController.signalTestCompletion();

      int compDelay = testScript.getTestCompletionDelay() * 1000;
      showln("*** ScenarioPrompter: compDelay=" + compDelay);

      gui.getTestExecutionPanel().getTimer().schedule(new TestCompletionTimerTask(
            gui, ISSITesterConstants.TEST_RUNS_FOR + 5000,
            compDelay),0, 1000);      
   }

   // Constructor
   public ScenarioPrompter(DietsGUI gui, TestControllerInterface remoteTestController,
      TestScript testScript)
      throws Exception
   {
      this( gui, remoteTestController, testScript, false);       
   }
       
   public ScenarioPrompter(DietsGUI gui, TestControllerInterface remoteTestController,
      TestScript testScript, boolean isPromptMode)
      throws Exception
   {
      this.gui = gui;
      this.testScript = testScript;
      this.isPromptMode = isPromptMode;
      this.priorityQueue = new PriorityQueue<AbstractScenario>();
      this.remoteTestController = remoteTestController;
      this.counter = new AtomicInteger(0);
      
      for (SuConfig suConfig : testScript.getTopologyConfig().getSuConfigurations()) {
         int answer = JOptionPane.YES_OPTION;
         if( isPromptMode) {
             answer = JOptionPane.showConfirmDialog(gui,
               "Turn on the SU : " + Integer.toHexString(suConfig.getSuId()) + "(hex)\n",
               "Do this action", 
               JOptionPane.YES_OPTION);
         }
         if (answer == JOptionPane.NO_OPTION) {
            if( remoteTestController != null) {
               logger.debug("ScenarioPrompter: tear down current test");
               remoteTestController.tearDownCurrentTest();
            }
            return;
         }
      }

      if (testScript.getScenarios().size() != 0) {
         for (AbstractScenario scenario : testScript.getScenariosValues()) {
            if (scenario.isTimeTriggered()) {
               priorityQueue.offer(scenario);
               new Thread(new MyPrompterTask(scenario)).start();
            }
         }
      }
   }

   private boolean evaluatePostCondition() throws Exception {
      
      // No more events to schedule.
      logger.debug("Test is completed -- signal the server to complete the test ");
      if (gui.getEvaluatePassFail()) {
         int answer = JOptionPane.YES_OPTION;
         if( isPromptMode) {
             answer = JOptionPane.showConfirmDialog( null,
                "Test setup is completed. Complete and evaluate PASS/FAIL?",
                "Test Completion Dialog",
                JOptionPane.YES_OPTION);
	 }
         if (answer == JOptionPane.YES_OPTION) {
            gui.getTestExecutionPanel().newProgressMonitor();
            gui.setProgressScreenMessage("Completing test and evaluating predicate");
            signalTestCompletion();

         } else {
            if( remoteTestController != null )
               remoteTestController.tearDownCurrentTest();
            gui.getTestExecutionPanel().enableAllButtons();
         }
      } else {
         // Just using DIETS as a call generator.
         gui.getTestExecutionPanel().newProgressMonitor();
         gui.setProgressScreenMessage("Completing test...");
         signalTestCompletion();
      }
      return false;
   }

   public boolean startPrompting() {
      try {
         if (priorityQueue.isEmpty()) {
            return evaluatePostCondition();
         }

         if (priorityQueue.isEmpty()) {
            logger.debug("scenario queue is empty -- returning");
            return false;
         }
         AbstractScenario scenario = priorityQueue.poll();
         logger.info("notifying : " + scenario);
         this.counter.incrementAndGet();
         logger.info("Time = " + scenario.getTriggerAsTimeTrigger().getTime());
         scenario.doNotify();
         int time = scenario.getTriggerAsTimeTrigger().getTime();
         while (!priorityQueue.isEmpty()
               && priorityQueue.peek().getTriggerAsTimeTrigger() .getTime() == time) {
            scenario = priorityQueue.poll();
            logger.info("notifying : " + scenario);
            this.counter.incrementAndGet();
            logger.info("Time = " + scenario.getTriggerAsTimeTrigger().getTime());
            scenario.doNotify();
            break;
         }
         return !priorityQueue.isEmpty();
         
      } catch (Exception ex) {
         logger.debug("startPrompting: ex=\n" + ex);
         ex.printStackTrace();
         return false;
      }
   }
}
