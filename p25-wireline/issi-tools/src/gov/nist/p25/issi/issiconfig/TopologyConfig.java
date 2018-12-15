//
package gov.nist.p25.issi.issiconfig;

import gov.nist.p25.issi.constants.ISSIDtdConstants;
import gov.nist.p25.issi.constants.XMLTagsAndAttributes;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;

import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * This class is responsible for reading the topology of the test system. 
 * We assume that the entire topology is known at the start of the test.
 * i.e. the assignments of SUs to Home and Serving RFSSs etc. Note that 
 * the ISSI specification does not specify how these mappings will be 
 * exported to the world so we have invented our own mapping scheme.
 * i.e. a simple XML document.
 */
public class TopologyConfig implements XMLTagsAndAttributes,
      java.io.Serializable {
   
   private static final long serialVersionUID = -1L;
   //private static Logger logger = Logger.getLogger("gov.nist.p25.issi.issiconfig");
   private static Logger logger = Logger.getLogger(TopologyConfig.class);
   private static final boolean verbose = false;

   /**
    * A table of SU configurations.
    */
   private Hashtable<String, SuConfig> suConfigTable;

   /**
    * A table of RFSS configuration records indexed by radical name
    */
   private Hashtable<String, RfssConfig> rfssConfigTable;

   /**
    * A table of system confiuration records.
    */
   private Hashtable<String, SystemConfig> systemTable;

   /**
    * A table of group configuration records.
    */
   private Hashtable<String, GroupConfig> groupTable;

   /**
    * File name from which this structure is generated.
    */
   private String configFile;

   /**
    * Group Name to group ID mapping.
    */
   private Hashtable<String, GroupConfig> groupNameTable;

   /**
    * SU Name to SU ID mapping
    */
   private Hashtable<String, SuConfig> suNameTable;

   /**
    * WACN Name to ID mapping
    */
   private Hashtable<String, WacnConfig> wacnNameTable;

   /**
    * RFSS Name to ID mapping
    */
   private Hashtable<String, RfssConfig> rfssNameTable;

   /**
    * System Name to ID mapping
    */
   private Hashtable<String, SystemConfig> systemNameTable;
   
   /**
    * List of names of the RFSS that appear in the trace 
    * if such information is available.
    */
   private List<String> traceOrder;

   // accessors
   public Hashtable<String, SuConfig> getSuConfigTable() {
      return suConfigTable;
   }
   public Hashtable<String, SuConfig> getSuNameTable() {
     return suNameTable;
   }
   public Hashtable<String, GroupConfig> getGroupNameTable() {
      return groupNameTable;
   }
   public Hashtable<String, GroupConfig> getGroupTable() {
      return groupTable;
   }
   public  Hashtable<String, WacnConfig> getWacnNameTable() {
      return wacnNameTable;
   }

   // constructor
   public TopologyConfig() {
      rfssConfigTable = new Hashtable<String, RfssConfig>();
      systemTable = new Hashtable<String, SystemConfig>();
      suConfigTable = new Hashtable<String, SuConfig>();
      groupTable = new Hashtable<String, GroupConfig>();
      groupNameTable = new Hashtable<String, GroupConfig>();
      wacnNameTable = new Hashtable<String, WacnConfig>();
      rfssNameTable = new Hashtable<String, RfssConfig>();
      systemNameTable = new Hashtable<String, SystemConfig>();
      suNameTable = new Hashtable<String, SuConfig>();
      traceOrder = new LinkedList<String>();
   }

   public RfssConfig getRfssConfigByTag(String tag) { 
      if( tag != null)
      for ( RfssConfig rfssConfig: getRfssConfigurations()) {
         if ( tag.equals(rfssConfig.getTag()))
            return rfssConfig;
      }
      return null;
   }
   public RfssConfig getRfssConfigByIpAddress(String ipStr) {
      if( ipStr != null)
      for ( RfssConfig rfssConfig: getRfssConfigurations()) {
         if ( ipStr.equals(rfssConfig.getIpAddress()))
            return rfssConfig;
      }
      return null;
   }
   public RfssConfig getRfssConfigByRfssName(String name) {
      if( name != null)
      for ( RfssConfig rfssConfig: getRfssConfigurations()) {
         if ( name.equals(rfssConfig.getRfssName()))
            return rfssConfig;
      }
      return null;
   }

   // for swapping RFSS
   public RfssConfig getRfssConfigByRfssId(int id) {
      for ( RfssConfig rfssConfig: getRfssConfigurations()) {
         if ( id == rfssConfig.getRfssId())
            return rfssConfig;
      }
      return null;
   }

   /**
    * Get the trace order.
    * 
    * @return a list of names of the trace ordering of this test.
    */
   public List<String> getTraceOrder() {
      return traceOrder;
   }
   
   /**
    * Set the trace order.
    * 
    * @param traceOrder -- trace order to set.
    */
   public void setTraceOrder(List<String> traceOrder) {
      this.traceOrder = traceOrder;
   }

   /**
    * Get the topology file name.
    * 
    * @return -- the topology file name.
    */
   public String getTopologyFile() {
      return configFile;
   }
   public void setTopologyFile(String configFile) {
      this.configFile = configFile;
   }

   /**
    * Retrieve the system configuration given a WacnID and systemID
    * 
    * @param wacnId
    * @param systemId
    * @return
    */
   public SystemConfig getSysConfig(int wacnId, int systemId) {
      String sysId = SystemConfig.getSystemName(wacnId, systemId);
      return (SystemConfig) systemTable.get(sysId);
   }

   /**
    * Retrieve the SU config given wacnId, systemId and suId
    * 
    * @param wacnId
    * @param systemId
    * @param suId
    * @return
    */
   public SuConfig getSuConfig(int wacnId, int systemId, int suId) {
      SystemConfig sysConfig = getSysConfig(wacnId, systemId);
      String radicalName = SuConfig.getRadicalName(sysConfig, suId);
      return (SuConfig) suConfigTable.get(radicalName);
   }

   public RfssConfig getRfssConfig(int wacnId, int systemId, int rfssId) {
      String domainName = RfssConfig.getDomainName(wacnId, systemId, rfssId);
      return rfssConfigTable.get(domainName);
   }

   public Collection<RfssConfig> getRfssConfigurations() {
      return rfssConfigTable.values();
   }

   public RfssConfig getRfssConfig(String rfssDomainName) {
      if(verbose)
      logger.debug("Getting RfssConfig for rfssDomainName=" + rfssDomainName);
      return (RfssConfig) rfssConfigTable.get(rfssDomainName.toLowerCase());
   }

   public SuConfig getSuConfig(String suRadicalName) {
      return (SuConfig) suConfigTable.get(suRadicalName);
   }

   public Collection<SuConfig> getSuConfigurations() {
      return suConfigTable.values();
   }

   public RfssConfig getRfssConfig(String ipAddress, int port) {
      for (RfssConfig rfss: rfssConfigTable.values()) {
         if (rfss.getIpAddress().equals(ipAddress) && rfss.getSipPort() == port)
            return rfss;
      }
      // try name table
      for (RfssConfig rfss: rfssNameTable.values()) {
         if (rfss.getIpAddress().equals(ipAddress) && rfss.getSipPort() == port)
            return rfss;
      }

      if(verbose)
      logger.error("Could not find rfss for ip=" +ipAddress +":" +port);
      if (verbose && logger.isDebugEnabled()) {
         logger.debug("Here is what is in the table - rfssConfigTable=");
         for (RfssConfig rfss: rfssConfigTable.values()) {
            logger.debug("stored ip address = " + rfss.getIpAddress()
                  + " stored port = " + rfss.getSipPort());
         }
      }
      if(verbose)
      logger.debug("----------------------------------- return null RfssConfig");
      return null;
   }

   public GroupConfig getGroupConfig(String groupRadicalName) {
      return groupTable.get(groupRadicalName);
   }

   public GroupConfig getGroupConfig(int wacnId, int systemId, int sgId) {
      logger.debug("getGroupConfig: wacnId = " + wacnId + "system Id = "
            + systemId + "sgId = " + sgId);
      SystemConfig sysConfig = getSysConfig(wacnId, systemId);
      if (sysConfig == null)
         return null;
      String radicalName = GroupConfig.getRadicalName(sysConfig, sgId);
      logger.debug("radicalName = " + radicalName);
      GroupConfig retval = getGroupConfig(radicalName);
      return retval;
   }

   /**
    * Get the RFSS ID given its name ( i.e. the symbolic name in the test ).
    * 
    * @param key
    * @return
    * @throws Exception
    *             if the id is not found.
    * 
    */
   public int getRfssIdByName(String key) throws Exception {
      if (!rfssNameTable.containsKey(key))
         throw new Exception("Could not getRfss Id for " + key);
      else
         return rfssNameTable.get(key).getRfssId();
   }

   /**
    * Return the system ID given its symbolic name.
    * 
    * @param key --
    *            the symbolic name.
    * @return -- the id if it exists.
    * @throws Exception
    */
   public int getSystemIdByName(String key) throws Exception {
      if (!systemNameTable.containsKey(key))
         throw new Exception("Could not get system ID for " + key);
      else
         return systemNameTable.get(key).getSystemId();
   }

   /**
    * Get the wacn ID given its symbolic name.
    * 
    * @param key -- the wacn symbolic name.
    * 
    * @return -- the wacn id.
    * @throws Exception
    */

   public int getWacnIdByName(String key) throws Exception {
      if (!wacnNameTable.containsKey(key))
         throw new Exception("Could not find wacn ID for " + key);
      else
         return wacnNameTable.get(key).getWacnId();
   }
   
   /**
    * Add a new WACN to the Topology
    * 
    * @param wacnConfig -- the wacn to add to the topology.
    */
   public void addWacnConfig(WacnConfig wacnConfig) throws Exception {
      if (wacnNameTable.contains(wacnConfig) ) 
         throw new Exception("Already exists! " + wacnConfig.getWacnName());
      wacnNameTable.put(wacnConfig.getWacnName(), wacnConfig);
   }

   /**
    * Get the SU ID given its symbolic name.
    * 
    * @param suName --
    *            the symbolic name of the SU for which the SUId is desired.
    * 
    * @return -- the suId of the SU with that name
    * 
    * @throws Exception --
    *             if no su by that name is configured.
    * 
    */
   public int getSuIdByName(String suName) throws Exception {
      if (!this.suNameTable.containsKey(suName))
         throw new Exception("Could not find SU ID for " + suName);
      else
         return suNameTable.get(suName).getSuId();
   }

   /**
    * Get the configuration for a given RFSS given its symbolic name.
    * 
    * @param name --
    *            the name for which to retrieve the configuration.
    * 
    * @return the configuration corresponding to the name.
    * 
    * @throws Exception --
    *             if no rfss corresponding to the given symbolic name is
    *             configured.
    */
   public RfssConfig getRfssConfigByName(String name) throws Exception {
      return rfssNameTable.get(name);
   }
   
   /**
    * Add an Rfss configuration.
    * 
    * @param rfssConfig -- rfss config to add.
    */
   public void addRfssConfig(RfssConfig rfssConfig) {
      logger.debug("Adding rfss:" + rfssConfig.getRfssName() 
         + " domain:" + rfssConfig.getDomainName() 
         + " ip:" + rfssConfig.getIpAddress() 
         + " port:" + rfssConfig.getSipPort());  
      rfssNameTable.put(rfssConfig.getRfssName(), rfssConfig);
      rfssConfigTable.put(rfssConfig.getDomainName(), rfssConfig);
   }

   /**
    * Get the configuration for a given SU given its name.
    * 
    * @param name --
    *            the name of the SU for which the configuration is desired.
    * 
    * @return the Su configuration corresponding to the su name.
    * 
    * @throws Exception --
    *             if no su by that name can be found
    * 
    */
   public SuConfig getSuConfigByName(String name) throws Exception {
      if (!suNameTable.containsKey(name))
         throw new Exception("SU configuration not found for " + name);
      return suNameTable.get(name);
   }

   /**
    * Get the System configuration corresponding to a given System name.
    * 
    * @param name -- the system name for which the configuration is desired
    * 
    * @return -- the system configuration for the system named.
    * 
    */
   public SystemConfig getSystemConfigByName(String name) {
      return systemNameTable.get(name);
   }
   
   /**
    * Add a new system configuration node.
    * 
    * @param systemConfig -- configuration to add.
    *
    */
   public void addSystemConfiguration( SystemConfig systemConfig) {
      systemNameTable.put(systemConfig.getSymbolicName(), systemConfig);
      systemTable.put(systemConfig.getSystemName(), systemConfig);
   }

   /**
    * Get the group configuraiton given its symbolic name.
    * 
    * @param name -- symbolic name of the group.
    * @return -- the group configuration for the group.
    * 
    */
   public GroupConfig getGroupConfigByName(String name)  {
      return groupNameTable.get(name);
   }

   /**
    * Get all the groups that are configured for the system.
    * 
    * @return a collection of groups.
    */
   public Collection<GroupConfig> getGroupConfigurations() {
      return groupTable.values();
   }

   /**
    * Get the wacn configuration given its name.
    * 
    * @param wacnName
    * @return
    */
   public WacnConfig getWacnConfig(String wacnName) {
      return wacnNameTable.get(wacnName);
   }

   public Collection<WacnConfig> getWacnConfigurations() {
      return wacnNameTable.values();
   }

   public Collection<SystemConfig> getSystemConfigurations() {
      return systemNameTable.values();
   }
   
   public String exportSystemTopology() {
      return exportSystemTopology(true);
   }
   public String exportSystemTopology(boolean header) {
      String preamble = "<?xml version=\"1.0\" ?>\n" +
         "<!DOCTYPE system-topology SYSTEM \"" + 
         ISSIDtdConstants.URL_ISSI_DTD_SYSTEM_TOPOLOGY + "\">\n";
      
      StringBuffer sbuf = new StringBuffer();
      if( header)
         sbuf.append(preamble);
      sbuf.append("<system-topology>\n");
      for ( WacnConfig wacnConfig: getWacnConfigurations()) {
         sbuf.append(wacnConfig.toString());
      }
      for ( SystemConfig systemConfig: getSystemConfigurations() ) {
         sbuf.append(systemConfig.toString() );
      }
      for ( RfssConfig rfssConfig: getRfssConfigurations()) {
         sbuf.append(rfssConfig.toString() );
      }
      sbuf.append("</system-topology>\n");
      return sbuf.toString();
   }

   public String exportGlobalTopology() {
      return exportGlobalTopology(true);
   }
   public String exportGlobalTopology(boolean header) {
      String preamble = "<?xml version=\"1.0\" ?>\n" +
         "<!DOCTYPE global-topology SYSTEM \"" + 
         ISSIDtdConstants.URL_ISSI_DTD_GLOBAL_TOPOLOGY + "\">\n";

      StringBuffer sbuf = new StringBuffer();
      if( header)
         sbuf.append(preamble);
      sbuf.append("<global-topology>\n");
      for ( RfssConfig rfssConfig: getRfssConfigurations()) {
         // this is just a partial info !!!
         sbuf.append("<rfssconfig\n");
         sbuf.append("\trfssName=\"" + rfssConfig.getRfssName() + "\"\n" );
         sbuf.append("\tselftestPort=\"" + rfssConfig.getSelfTestPort() +  "\"\n" );
         sbuf.append("/>\n");
      }
      for ( SuConfig suConfig: getSuConfigurations()) {
         sbuf.append(suConfig.toString() );
      }
      for (GroupConfig sgConfig: getGroupConfigurations()) {
         sbuf.append(sgConfig.toString());
      }
      sbuf.append("</global-topology>\n");
      return sbuf.toString();
   }

   /**
    * @param systemNameTable the systemNameTable to set
    */
   public void setSystemNameTable(Hashtable<String, SystemConfig> systemNameTable) {
      this.systemNameTable = systemNameTable;
   }

   /**
    * @return the systemNameTable
    */
   public Hashtable<String, SystemConfig> getSystemNameTable() {
      return systemNameTable;
   }

   /**
    * @param systemTable the systemTable to set
    */
   public void setSystemTable(Hashtable<String, SystemConfig> systemTable) {
      this.systemTable = systemTable;
   }

   /**
    * @return the systemTable
    */
   public Hashtable<String, SystemConfig> getSystemTable() {
      return systemTable;
   }

   /**
    * Add a reference alias in the RFSS name table.
    * 
    * @param id
    * @param rfssConfig
    */
   public void addRfssAlias(String id, RfssConfig rfssConfig) {
      rfssNameTable.put(id, rfssConfig);
   }
   
   /**
    * Rebind IP addresses for all the emulator nodes to conform to a new Tester config.
    * This method is called to dynamically change a tester topology.
    * 
    * @param testerconfig -- the tester config.
    */
   public void rebindIpAddresses(ISSITesterConfiguration testerconfig) {
      for ( RfssConfig rfssConfig: getRfssConfigurations()) {
         if ( rfssConfig.isEmulated()) {
            String tag = rfssConfig.getTag();
            //if ( testerconfig.getDaemonWebServerAddressByName(tag)!= null) {
            //   rfssConfig.setIpAddress(testerconfig.getDaemonWebServerAddressByName(tag).getIpAddress());
            //}
            DaemonWebServerAddress dwsa = testerconfig.getDaemonWebServerAddressByName(tag);
            if (dwsa != null) {
               rfssConfig.setIpAddress(dwsa.getIpAddress());
            }
         }
      }       
   }
   
   public String getDescription() {
      StringBuffer sbuf = new StringBuffer();
      sbuf.append("TEST CONFIGURATIONS FOR RFSS AND SUs");
      for (RfssConfig rfssConfig : getRfssConfigurations()) {
         sbuf.append("\n");
         sbuf.append( rfssConfig.getDescription());
      }
      for (SuConfig suConfig : getSuConfigurations()) {
         sbuf.append("\n");
         sbuf.append( suConfig.getDescription());
      }
      for (GroupConfig gConfig : getGroupConfigurations()) {
         sbuf.append("\n");
         sbuf.append( gConfig.getDescription());
      }
      return sbuf.toString();
   }
}
