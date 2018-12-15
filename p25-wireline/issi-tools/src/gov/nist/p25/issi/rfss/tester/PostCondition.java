//
package gov.nist.p25.issi.rfss.tester;

import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.rfss.RFSS;

import java.util.ArrayList;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

/**
 * Each test case has a set of post conditions that must be evaluated 
 * at the end of the test. This class represents a post condition.
 * 
 */
@SuppressWarnings("unused")
public class PostCondition {
   
   private static Logger logger = Logger.getLogger(PostCondition.class);
   
   private String scriptingEngine;
   private String script;
   private String assertion;
   private String locationPredicate;
   private PythonInterpreter interpreter;

   // constructor
   protected PostCondition( String assertion, String script, String location) {
      this.scriptingEngine = "jython";      
      this.script = script;
      this.assertion = assertion;
      this.locationPredicate = location;
      this.interpreter = new PythonInterpreter();
      interpreter.exec("from java.lang import *");
      interpreter.exec("from java.util import *");
      interpreter.exec("from gov.nist.javax.sip import *");
      interpreter.exec("from gov.nist.javax.sip.message import *");
      interpreter.exec("from gov.nist.javax.sip.address import *");
      interpreter.exec("from gov.nist.p25.issi.issiconfig import *");
   }

   /**
    * @return true if the postcodition test passes. False otherwise. Test the
    *         post condition if one exists and return true.
    */
   public boolean testPostCondition(RFSS rfss, TestScript testScript,
         TopologyConfig topologyConfig) throws Exception {
      if (script == null) {
         return true;
      } else {

         ArrayList<Request> requests = rfss.getRequests();
         ArrayList<Response> responses = rfss.getResponses();
         interpreter.set("requests", requests);
         interpreter.set("responses", responses);
         interpreter.set("currentRfss", rfss.getRfssConfig());
         interpreter.set("testScript", testScript);
         interpreter.set("topologyConfig", topologyConfig);
         interpreter.exec(this.script);

         PyObject pobj = interpreter.eval(this.locationPredicate + "()");
         if (!pobj.isNumberType()) {
            throw new Exception("Invalid Return type. Should be int");
         }
         PyInteger pyInt = (PyInteger) pobj;
         boolean res = pyInt.getValue() != 0;
         if (!res) {
            return true;
         }

         logger.info("testing post condition : " + this.assertion + "()");
         PyObject retval = interpreter.eval(this.assertion + "()");
         if (!pobj.isNumberType()) {
            logger.fatal("Wrong type is returned from the assertion function");
            throw new Exception("Invalid return type -- should be integer");
         }
         return ((PyInteger) retval).getValue() != 0;
      }
   }
}
