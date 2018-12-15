//
//package gov.nist.p25.issi.rfss.tester;
package gov.nist.p25.issi.verifier;

import java.util.LinkedList;

import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * A bunch of requests, responses and PTT messages that are used to test against
 * the incoming requests and responses. This is generated from the captured
 * templates. During an actual run we test the captured messages against
 * the template files.
 */
public class TestMessages {
   
   private LinkedList<Request> requests = new LinkedList<Request>();
   private LinkedList<Response> responses = new LinkedList<Response>();


   public void addRequest(Request request) {
      requests.add(request);
   }
   
   public void addResponse(Response response) {
      responses.add(response);
   }
   
   public LinkedList<Request> getRequests() {
      return requests;
   }
   
   public LinkedList<Response> getResponses() {
      return responses;
   }
   
   public Request getFirstRequest() {
      return requests.getFirst();
   }
   
   public Response getFirstResponse() {
      return responses.getFirst();
   }
   
   public void removeRequest(Request template) {
      requests.remove(template);
   }
   
   public void removeResponse(Response template) {
      responses.remove(template);
   }
   
   public boolean hasMoreRequests() {
      return !requests.isEmpty();
   }
   
   public boolean hasMoreResponses() {
      return !responses.isEmpty();
   }
}
