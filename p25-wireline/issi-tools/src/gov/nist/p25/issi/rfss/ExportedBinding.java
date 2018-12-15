//
package gov.nist.p25.issi.rfss;

import javax.sip.address.URI;

/**
 * Exported binding. You can use RMI to get the bindings as an array of such
 * values.
 */
public class ExportedBinding implements java.io.Serializable {
   private static final long serialVersionUID = -1L;
   
   private String requestURI;
   private String contactAddress;
   private long expiryTime;
   private URI key;

   // accessor
   public String getRequestURI() {
      return requestURI;
   }
   public void setRequestURI(String requestURI) {
      this.requestURI = requestURI;
   }

   public String getContactAddress() {
      return contactAddress;
   }
   public void setContactAddress(String contactAddress) {
      this.contactAddress = contactAddress;
   }

   public long getExpiryTime() {
      return expiryTime;
   }
   public void setExpiryTime(long expiryTime) {
      this.expiryTime = expiryTime;
   }

   public URI getKey() {
      return key;
   }
   public void setKey(URI key) {
      this.key = key;
   }
}
