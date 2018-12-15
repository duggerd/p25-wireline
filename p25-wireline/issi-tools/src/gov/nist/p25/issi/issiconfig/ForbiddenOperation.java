//
package gov.nist.p25.issi.issiconfig;

/**
 *  Forbidden Operation class.
 */
public class ForbiddenOperation {
   private String operation;
   private RfssConfig rfssConfig;
   
   // accessor
   public String getOperation() {
      return operation;
   }
   public RfssConfig getRfssConfig() {
      return rfssConfig;
   }
   
   public ForbiddenOperation(String operation, RfssConfig rfssConfig) {
      this.operation = operation;
      this.rfssConfig = rfssConfig;
   }
   
   public boolean isOperationForbidden(String xoperation, RfssConfig xrfssConfig) {
      return rfssConfig==xrfssConfig && operation.equals(xoperation);
   }
}
