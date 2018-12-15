//
package gov.nist.p25.issi.issiconfig;

/**
 * Wide Area Communication Network configuration.
 */
public class WacnConfig {
   private String wacnName;
   private int wacnId;
   private boolean assigned;
   
   // accessors
   public String getWacnName() {
      return wacnName;
   }
   public void setWacnName(String wacnName) {
      this.wacnName = wacnName;
   }

   public int getWacnId() {
      return wacnId;
   }
   public void setWacnId(int wacnId) {
      if (wacnId > (1 << 21 ) - 1)
         throw new IllegalArgumentException(
               "Value too large only 20 bits for wacnId " + wacnId);
      this.wacnId = wacnId;
   }

   public boolean isAssigned() {
      return this.assigned;
   }
   public boolean getAssigned() {
      return this.assigned;
   }
   public void setAssigned(boolean assigned) {
      this.assigned = assigned;
   }
   
   // constructor
   public WacnConfig() {
   }
   public WacnConfig(String wacnName, int wacnId) {
      setWacnName(wacnName);
      setWacnId(wacnId);
   }

   public String toString() {
      return "<wacn-config\n" +
            "\twacnName=\""+ wacnName + "\"\n" +
            "\twacnId=\"" + Integer.toHexString(wacnId) + "\"\n" +
            "/>\n";
   }
}
