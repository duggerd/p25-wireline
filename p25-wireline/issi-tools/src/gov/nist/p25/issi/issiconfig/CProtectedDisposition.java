//
package gov.nist.p25.issi.issiconfig;

public enum CProtectedDisposition {

   FORWARD_PROTECTED,CLEAR_PROTECTED,REJECT_PROTECTED,REJECT_UNPROTECTED;
   
   public static CProtectedDisposition getValue(String directive) {
      if ("forward_protected".equals(directive)) {
         return FORWARD_PROTECTED;
      } else if ("clear_protected".equals(directive)) {
         return CLEAR_PROTECTED;
      } else if ("reject_protected".equals(directive)) {
         return REJECT_PROTECTED;
      } else if ("reject_unprotected".equals(directive)) {
         return REJECT_UNPROTECTED;
      } else 
         throw new IllegalArgumentException("Invalid argument: " + directive);
   }
}
