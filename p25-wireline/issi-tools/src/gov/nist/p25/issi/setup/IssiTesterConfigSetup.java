//
package gov.nist.p25.issi.setup;

/**
 * This class contains the ISSI Tester Config Setup information.
 */
@SuppressWarnings("unchecked")
public class IssiTesterConfigSetup {

   public static final int COL_NAME = 0;
   public static final int COL_IP_ADDRESS = 1;
   public static final int COL_HTTP_PORT = 2;
   public static final int COL_CONFORMANCE_TESTER = 3;
   public static final int COL_PACKET_MONITOR = 4;
   public static final int COL_REFID = 5;

   private static final String columnNames[] = { 
         "Name",
         "IpAddress",
         "HttpPort",
         "isConformanceTester",
         "isPacketMonitor",
         "RefId",
   };
   private static final boolean columnEditable[] = { 
         false,
         true,
         false,
         true,
         false,
         false,
   };
   private static final String columnTips[] = { 
         "Name of WebServer",
         "IP Address of WebServer",
         "HTTP Port",
         "Set true to designate Conformance Tester",
         "Set true to designate Packet Monitor",
         "Reference IDs of associated RFSS (Names separated by comma)",
   };
   private static final Class columnClasses[] = {
         String.class,
         String.class,
         Integer.class,
         Boolean.class,
         Boolean.class,
         String.class,
   };

   private Object reference;
   private String name;                 // daemon2
   private String ipAddress;            // 132.163.65.206
   private int httpPort;                // 8763
   private boolean conformanceTester;   // true/false
   private boolean packetMonitor;       // false
   private String refid;                // rfss_1.daemon,rfss_2.daemon
   
   // accessors
   public Object getReference() {
      return reference;
   }
   public void setReference(Object reference) {
      this.reference = reference;
   }

   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }

   public String getIpAddress() {
      return ipAddress;
   }
   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public int getHttpPort() {
      return httpPort;
   }
   public void setHttpPort(int httpPort) {
      this.httpPort = httpPort;
   }

   //------------------------------------------
   public boolean isConformanceTester() {
      return conformanceTester;
   }
   public boolean getConformanceTester() {
      return conformanceTester;
   }
   public void setConformanceTester(boolean conformanceTester) {
      this.conformanceTester = conformanceTester;
   }

   public boolean isPacketMonitor() {
      return packetMonitor;
   }
   public boolean getPacketMonitor() {
      return packetMonitor;
   }
   public void setPacketMonitor(boolean packetMonitor) {
      this.packetMonitor = packetMonitor;
   }

   public String getRefid() {
      return refid;
   }
   public void setRefid(String refid) {
      this.refid = refid;
   }
   //-------------------------

   // GUI supports
   public static String[] getColumnNames() {
      return IssiTesterConfigSetup.columnNames;
   }
   public static Class[] getColumnClasses() {
      return IssiTesterConfigSetup.columnClasses;
   }
   public static String[] getColumnTips() {
      return IssiTesterConfigSetup.columnTips;
   }
   public static boolean[] getColumnEditable() {
      return IssiTesterConfigSetup.columnEditable;
   }

   // constructor
   //---------------------------------------------------------------------
   public IssiTesterConfigSetup() {
   }

   @Override
   public boolean equals(Object that) {
      return this.getClass().equals(that.getClass())
         && this.getIpAddress().equals(((IssiTesterConfigSetup) that).getIpAddress())
         && this.getHttpPort() == ((IssiTesterConfigSetup) that).getHttpPort();
   }

   @Override
   public int hashCode() {
      return getIpAddress().hashCode() + getHttpPort()*13;
   }

   @Override
   public String toString() {
      return getIpAddress() + ":" + getHttpPort();
   }

   // GUI support
   //-------------------------------------------------------
   public Object getValue(int column) {
      switch( column) {
      case COL_NAME:
         return getName();
      case COL_IP_ADDRESS:
         return getIpAddress();
      case COL_HTTP_PORT:
         return new Integer(getHttpPort());
      case COL_CONFORMANCE_TESTER:
         return new Boolean(getConformanceTester());
      case COL_PACKET_MONITOR:
         return new Boolean(getPacketMonitor());
      case COL_REFID:
         return getRefid();
      default:
         return null;
      }
   }

   public void setValue(int column, Object value) {
      System.out.println("IssiTesterConfigSetup: value="+value);
      if(value == null) return;

      switch( column) {
      case COL_NAME:
         setName( (String)value);
         break;
      case COL_IP_ADDRESS:
         setIpAddress( (String)value);
         break;
      case COL_HTTP_PORT:
         if( value instanceof Integer) {
            setHttpPort( ((Integer)value).intValue());
         }
         else if( value instanceof String) {
            try {
               setHttpPort( Integer.parseInt((String)value));
            } catch(Exception ex) { }
         }
         break;
      case COL_CONFORMANCE_TESTER:
         setConformanceTester( ((Boolean)value).booleanValue());
         break;
      case COL_PACKET_MONITOR:
         setPacketMonitor( ((Boolean)value).booleanValue());
         break;
      case COL_REFID:
         setRefid( (String)value);
         break;
      }
   }
}
