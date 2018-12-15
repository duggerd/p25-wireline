//
package gov.nist.p25.issi.traceverifier;

public enum StatusFlag {

   PASS, FAIL, NOT_TESTED;
   
   public String toString() {
      if ( this == PASS)
	 return "PASS";
      else if ( this == FAIL )
	 return "FAIL";
      else if ( this == NOT_TESTED )
	 return "NOT TESTED";
      else 
	 throw new IllegalArgumentException("Invalid StatusFlag");
   }
}
