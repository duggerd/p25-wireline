//
package gov.nist.p25.issi.issiconfig;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * A class that defines the identity of a system.
 * 
 */
@SuppressWarnings("unchecked")
public class SystemConfig implements Serializable {
   private static final long serialVersionUID = -1L;
   
   private WacnConfig wacnConfig;   
   private int systemId;   
   private String symbolicName;   
   private boolean assigned;

   /**
    * The topology of the system.
    */
   private TopologyConfig topologyConfig;

   /**
    * A table of all the RFSSs configured for which I am the system.
    */
   private Hashtable<String, RfssConfig> rfssTable;

   // accessors
   public WacnConfig getWacnConfig() {
      return wacnConfig;
   }
   public void setWacnConfig(WacnConfig wacnConfig) {
      this.wacnConfig = wacnConfig;
   }

   public int getSystemId() {
      return systemId;
   }
   public void setSystemId(int systemId) {
      if (systemId > (1 << 13) - 1)
         throw new IllegalArgumentException(
               "value too large only 12 bits for systemId");
      this.systemId = systemId;      
   }

   public String getSymbolicName() {
      return symbolicName;
   }
   public void setSymbolicName(String symbolicName) {
      this.symbolicName = symbolicName;
   }

   public boolean isAssigned() {
      return assigned;
   }
   public boolean getAssigned() {
      return assigned;
   }
   public void setAssigned(boolean assigned) {
      this.assigned = assigned;
   }

   public TopologyConfig getTopologyConfig() {
      return topologyConfig;
   }
   public void setTopologyConfig(TopologyConfig topologyConfig) {
      this.topologyConfig = topologyConfig;
   }

   // constructor
   public SystemConfig(TopologyConfig topologyConfig, String symbolicName, 
      WacnConfig wacnConfig, int systemId) {
      setSymbolicName( symbolicName);
      setWacnConfig( wacnConfig);
      this.wacnConfig.setAssigned(true);
      setSystemId(systemId);
      this.rfssTable = new Hashtable<String, RfssConfig>();
      setTopologyConfig( topologyConfig);
   }

   public static String getSystemName(int wacnId, int systemId) {
      String wacnIdName = Integer.toHexString(wacnId);
      int diff = 5 - wacnIdName.length();

      // prepend 0's to the right length.
      for (int i = 0; i < diff; i++) {
         wacnIdName = "0" + wacnIdName;
      }

      String systemIdName = Integer.toHexString(systemId);
      diff = 3 - systemIdName.length();
      for (int i = 0; i < diff; i++) {
         systemIdName = "0" + systemIdName;
      }
      return systemIdName + "." + wacnIdName;
   }

   /**
    * Get the system domain name (as defined in 3.4.1.1 of the spec ).
    * 
    * @return formatted system name of the system.
    */
   public String getSystemName() {
      return getSystemName(wacnConfig.getWacnId(), systemId);
   }
   
   public void addRfss(RfssConfig rfssConfig) {
      String key = rfssConfig.getDomainName();
      rfssTable.put(key, rfssConfig);
   }

   public RfssConfig getRfssConfig(int rfssId) {
      String key = RfssConfig.getDomainName(this, rfssId);
      return (RfssConfig) rfssTable.get(key);
   }

   public Iterator getRfssConfigurations() {
      return rfssTable.values().iterator();
   }

   public int getWacnId() {
      return wacnConfig.getWacnId();
   }
   public void setWacnId(int wacnId) {
      wacnConfig.setWacnId(wacnId);
   }

   public String getWacnName() {
      return wacnConfig.getWacnName();
   }
   public void setWacnName(String wacnName) {
      WacnConfig wacnConfig = topologyConfig.getWacnConfig(wacnName);
      if ( wacnConfig == null )
         throw new IllegalArgumentException("Bad wacn name " + wacnName);
      else
         wacnConfig = topologyConfig.getWacnConfig(wacnName);      
   }
   
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("<systemconfig\n");
      sb.append("\tsystemName=\"" + getSymbolicName() + "\"\n");
      sb.append("\tsystemId=\"" + Integer.toHexString(getSystemId()) + "\"\n"); 
      sb.append("\twacnName=\"" + wacnConfig.getWacnName() +"\"\n/>\n");
      return sb.toString();
   }
}
