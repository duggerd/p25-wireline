//
package gov.nist.p25.issi.setup;

import gov.nist.p25.common.swing.widget.MessageBox;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.utils.EndPointHelper;

import java.awt.Toolkit;
import org.apache.log4j.Logger;

/**
 * RFSS Configuration Setup class.
 */
@SuppressWarnings("unchecked")
public class RfssConfigSetup {

   private static Logger logger = Logger.getLogger(RfssConfigSetup.class);
   public static void showln(String s) { System.out.println(s); }

   public static final int MODE_SETUP = 0;
   public static final int MODE_PLAYBACK = 1;

   public static final int COL_EMULATED = 0;
   public static final int COL_IP_ADDRESS = 1;
   public static final int COL_PORT = 2;
   public static final int COL_NAME = 3;
   public static final int COL_ID = 4;
   public static final int COL_SYSTEM_ID = 5;
   public static final int COL_WACN_ID = 6;
   public static final int COL_DOMAIN_NAME = 7;
   public static final int COL_ENABLED = 8;

   public static final boolean swapIpAddress = true;
   public static final boolean swapName = true;
   public static final boolean swapId = false;   // DISABLED

   private static final String columnNames[] = { 
         "Emulated",
         "IpAddress",
         "Port",
         "RfssName",
         "RfssId",
         "SystemId",
         "WacnId",
         "DomainName",
         "Enabled",
   };
   private static final boolean columnEditable[] = { 
         true,
         true,
         true,
         true,
         true,
         true,
         true,
         true,
         true,
   };

   private static final String columnTips[] = { 
         "Set Emulated to designate the emulated RFS",
         "IP Address in form of ddd.ddd.ddd.ddd",
         "SIP Signalling Port in form of ddddd",
         "RFSS Name",
         "RFSS Id in hex (0xFF)",
         "System Id in hex (0xFFF)",
         "Wacn Id in hex (0xFFFFF)",
         "DomainName in form of <RfssId>.<SystemId>.<WacnId>.p25dr",
         "Set Enabled to include the RFSS",
   };
   private static final Class columnClasses[] = {
         Boolean.class,
         String.class,
         Integer.class,
         String.class,
         Integer.class,
         Integer.class,
         Integer.class,
         String.class,
         Boolean.class,
   };

   private TopologyConfig topologyConfig;
   // RfssConfig
   private Object reference;
   private boolean emulated = false;
   private String ipAddress;       // 10.0.2.25
   private int port = 5060;        // 5060
   private String name = "";       // VHF
   private int id;                 // 01(hex)
   private int systemId;           // ad5(hex)
   private int wacnId;             // bee07(hex)
   private boolean enabled = true;
   
   // accessors
   public TopologyConfig getReferenceTopology() {
      return topologyConfig;
   }
   public void setReferenceTopology(TopologyConfig topologyConfig) {
      this.topologyConfig = topologyConfig;
   }

   public Object getReference() {
      // RfssConfig
      return reference;
   }

   public void setReference(Object reference) {
      // RfssConfig
      this.reference = reference;
   }

   public String getIpAddress() {
      return ipAddress;
   }
   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public int getPort() {
      return port;
   }
   public void setPort(int port) {
      this.port = port;
   }

   // RFSS Name
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }

   // RFSS Id
   //-------------------------
   public int getId() {
      return id;
   }
   public void setId(int id) {
      EndPointHelper.rangeCheckRfssId(id);
      this.id = id;
   }
   public String getIdString() {
      return EndPointHelper.encodeRfssId( getId());
   }
   public void setIdString(String idString) {
      this.id = EndPointHelper.decodeRfssId(idString);
   }

   // systemId
   //-------------------------
   public int getSystemId() {
      return systemId;
   }
   public void setSystemId(int systemId) {
      EndPointHelper.rangeCheckSystemId( getSystemId());
      this.systemId = systemId;
   }
   public String getSystemIdString() {
      return EndPointHelper.encodeSystemId( getSystemId());
   }
   public void setSystemIdString(String systemIdString) {
      this.systemId = EndPointHelper.decodeSystemId(systemIdString);
   }

   // wacnId
   //-------------------------
   public int getWacnId() {
      return wacnId;
   }
   public void setWacnId(int wacnId) {
      EndPointHelper.rangeCheckWacnId( getWacnId());
      this.wacnId = wacnId;
   }
   public String getWacnIdString() {
      return EndPointHelper.encodeWacnId( getWacnId());
   }
   public void setWacnIdString(String wacnIdString) {
      this.wacnId = EndPointHelper.decodeWacnId(wacnIdString);
   }

   // emulated
   public boolean isEmulated() {
      return emulated;
   }
   public boolean getEmulated() {
      return emulated;
   }
   public void setEmulated(boolean emulated) {
      this.emulated = emulated;
   }

   public boolean isEnabled() {
      return enabled;
   }
   public boolean getEnabled() {
      return enabled;
   }
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   // domain name derived from ids
   // 02.ad5.bee07.p25dr
   //-------------------------
   public String getDomainName() {
      return EndPointHelper.encodeDomainName( getId(), getSystemId(), getWacnId());
   }
   public void setDomainName(String domainName) {
      // 02.ad5.bee07.p25dr
      if( domainName==null || domainName.length()==0)
         return;
      String[] parts = EndPointHelper.rangeCheckDomainName( domainName);

      // validate each components
      setIdString( parts[0]);
      setSystemIdString( parts[1]); 
      setWacnIdString( parts[2]); 
   }

   // GUI supports
   public static String[] getColumnNames() {
      return RfssConfigSetup.columnNames;
   }
   public static Class[] getColumnClasses() {
      return RfssConfigSetup.columnClasses;
   }
   public static String[] getColumnTips() {
      return RfssConfigSetup.columnTips;
   }
   public static boolean[] getColumnEditable() {
      return RfssConfigSetup.columnEditable;
   }

   // constructor
   //---------------------------------------------------------------------
   public RfssConfigSetup() {
   }

   public RfssConfigSetup(String ipAddress, int port, String name, 
      String domainName, boolean emulated)
   {
      setIpAddress(ipAddress);
      setPort(port);
      setName(name);
      setEmulated(emulated);
      setDomainName(domainName);
   }
   public RfssConfigSetup(String ipAddress, int port, String name, int id, 
      int systemId, int wacnId, boolean emulated)
   {
      setIpAddress(ipAddress);
      setPort(port);
      setName(name);
      setEmulated(emulated);
      setId(id);
      setSystemId( systemId);
      setWacnId( wacnId);
   }

   @Override
   public boolean equals(Object that) {
      return this.getClass().equals(that.getClass())
         && this.getIpAddress().equals(((RfssConfigSetup) that).getIpAddress())
         && this.getPort() == ((RfssConfigSetup) that).getPort();
   }

   @Override
   public int hashCode() {
      return getIpAddress().hashCode();
   }

   @Override
   public String toString() {
      return getIpAddress() + ":" + getPort();
   }

   // GUI support
   //-------------------------------------------------------
   public Object getValue(int column) {
      switch( column) {
      case COL_IP_ADDRESS:
         return getIpAddress();
      case COL_PORT:
         return new Integer(getPort());
      case COL_NAME:
         return getName();
      case COL_ID:
         return getId();
      case COL_SYSTEM_ID:
         return getSystemId();
      case COL_WACN_ID:
         return getWacnId();
      case COL_DOMAIN_NAME:
         return getDomainName();
      case COL_EMULATED:
         return new Boolean(getEmulated());
      case COL_ENABLED:
         return new Boolean(getEnabled());
      default:
         return null;
      }
   }
   private void beep() {
      Toolkit.getDefaultToolkit().beep();
   }
   public void setValue(int column, Object value) {
      showln("RfssConfigSetup: setValue(): value="+value);
      if(value == null) return;

      TopologyConfig topologyConfig = getReferenceTopology();
      RfssConfig refRfssConfig = (RfssConfig)getReference();

      switch( column) {
      case COL_IP_ADDRESS:
         String ipStr = (String)value;
         EndPointHelper.rangeCheckIpAddress( ipStr);
	 
	 // check for existing IP
	 //------------------------------------------------
         if(topologyConfig != null && swapIpAddress) { 
            // check if value changed
            if( refRfssConfig.getIpAddress().equals(ipStr)) {
               showln("RfssConfigSetup: ip equals");
               setIpAddress( (String)value);
               return;
            }

            RfssConfig rfssConfig = topologyConfig.getRfssConfigByIpAddress(ipStr);
            if( rfssConfig != null) {
               /***
               RfssConfigSetup ep = getRfssConfigSetup( rfssConfig);
               ep.setReferenceTopology( topologyConfig);
               copyObject( ep);
               logger.debug("RfssConfigSetup: copyObject-ipStr="+ipStr);
	        ***/
               beep();
               String msg = ipStr +": RFSS ipAddress already exists in database.\n" +
                   "Use Setup Topology to reconfigure the RFSS.\n";
	       MessageBox.doOk(200, 200, msg);

            } else {
               setIpAddress( (String)value);
            }
         } else {
            setIpAddress( (String)value);
         }
         break;
      case COL_PORT:
         if( value instanceof Integer) {
            setPort( ((Integer)value).intValue());
         }
         else if( value instanceof String) {
            try {
               setPort( Integer.parseInt((String)value));
            } catch(Exception ex) { }
         }
         break;
      case COL_NAME:
         String name = (String)value;

	 // check existing RFSS name
	 //------------------------------------------------
         if(topologyConfig != null && swapName) { 
            // check if value changed
            if( refRfssConfig.getRfssName().equals(name)) {
               showln("RfssConfigSetup: name equals");
               setName( name);
               return;
            }

            RfssConfig rfssConfig = topologyConfig.getRfssConfigByRfssName(name);
            if( rfssConfig != null) {
               /***
               RfssConfigSetup ep = getRfssConfigSetup( rfssConfig);
               ep.setReferenceTopology( topologyConfig);
               copyObject( ep);
               logger.debug("RfssConfigSetup: copyObject-name="+name);
                ***/
               beep();
               String msg = name +": RFSS name already exists in database.\n" +
                   "Use Setup Topology to reconfigure the RFSS.\n";
	       MessageBox.doOk(200, 200, msg);

            } else {
               setName( name);
            }
         } else {
            setName( name);
         }
         break;
      case COL_ID:
         int newId = 0;
         if( value instanceof Integer) {
            newId = ((Integer)value).intValue();
         }
         else if( value instanceof String) {
            try {
               newId = Integer.parseInt((String)value);
            } catch(Exception ex) {
               logger.debug("RfssConfigSetup: BAD input rfssId="+value);
	    }
         }
         logger.debug("RfssConfigSetup: decimal newId="+newId);
         if(topologyConfig != null && swapId) { 
            // check if changed
            if( refRfssConfig.getRfssId() == newId)
               return;

            RfssConfig rfssConfig = topologyConfig.getRfssConfigByRfssId(newId);
            if( rfssConfig != null) {
               beep();
               String msg = ""+value +": RFSS Id already exists in database.\n" +
                   "Use Setup Topology to reconfigure the RFSS.\n";
	       MessageBox.doOk(200, 200, msg);

            } else {
               setId( newId);
            }
         } else {
            setId( newId);
         }
         break;
      case COL_SYSTEM_ID:
         if( value instanceof Integer) {
            setSystemId( ((Integer)value).intValue());
         }
         else if( value instanceof String) {
            try {
               setSystemId( Integer.parseInt((String)value));
            } catch(Exception ex) { }
         }
         break;
      case COL_WACN_ID:
         if( value instanceof Integer) {
            setWacnId( ((Integer)value).intValue());
         }
         else if( value instanceof String) {
            try {
               setWacnId( Integer.parseInt((String)value));
            } catch(Exception ex) { }
         }
         break;
      case COL_DOMAIN_NAME:
         setDomainName( (String)value);
         break;
      case COL_EMULATED:
         setEmulated( ((Boolean)value).booleanValue());
         break;
      case COL_ENABLED:
         setEnabled( ((Boolean)value).booleanValue());
         break;
      }
   }

   public void copyObject(RfssConfigSetup rcs)
   {
      this.setIpAddress( rcs.getIpAddress());
      this.setPort( rcs.getPort());
      this.setName( rcs.getName());
      this.setDomainName( rcs.getDomainName());
      this.setEmulated( rcs.getEmulated());
      this.setEnabled( rcs.getEnabled());

      this.setReference( rcs.getReference());
      this.setReferenceTopology( rcs.getReferenceTopology());
   }

   public static RfssConfigSetup getRfssConfigSetup(RfssConfig rfssConfig)
   {
      String ipAddress = rfssConfig.getIpAddress();
      int port = rfssConfig.getSipPort();
      String rname = rfssConfig.getRfssName();
      int rid = rfssConfig.getRfssId();
      int wacnId = rfssConfig.getWacnId();
      int systemId = rfssConfig.getSystemId();
      boolean emulated = rfssConfig.isEmulated();

      RfssConfigSetup ep = new RfssConfigSetup(ipAddress,port,rname,
         rid,systemId,wacnId,emulated);
      ep.setReference( rfssConfig);
      return ep;
   }
}
