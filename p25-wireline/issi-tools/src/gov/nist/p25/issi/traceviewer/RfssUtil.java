//
package gov.nist.p25.issi.traceviewer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Utilities for RFSS data.
 */
public class RfssUtil {

   /**
    * Check if the RFSS exists for a given rfss ID.
    * 
    * @param rfssList
    * @param rfssId
    * @return -- true if RFSS exists, otherwise false
    */
   public static boolean rfssExists(Set<RfssData> rfssList, String rfssId) {
      boolean exists = false;
      for (RfssData data : rfssList) {   
         if (data.getId().equals(rfssId)) {
            exists = true;
	    break;  // Optimize
         } 
      }
      return exists;
   }

   /**
    * Get the RFSS configuration data corresponding to a given rfss ID.
    * 
    * @param rfssList
    * @param rfssId
    * @return -- RfssData
    */
   public static RfssData getRfss(Collection<RfssData> rfssList, String rfssId) {
      boolean exists = false;
      RfssData retval = null;      
      for (RfssData data : rfssList) {               
         if (data.getRfssConfig() != null  &&
            data.getRfssConfig().getDomainName().equals(rfssId) ) {
            exists = true;
            retval = data;            
	    break;  // Optimize
         } else if (data.getId().equals(rfssId)) {
            exists = true;
            retval =  data;      
	    break;  // Optimize
         } 
      }
      return exists ? retval : null;
   }

   /*** Not used ?
   public static boolean portExists(List<String> ports, String seekPort) {
      boolean exists = false;
      for (int i = 0; i < ports.size(); i++) {
         String port = (String) ports.get(i);
         if (port.equals(seekPort)) {
            exists = true;
            break;
         } 
      }
      return exists;
   }
    **/
}
