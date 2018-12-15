//
package gov.nist.p25.issi.packetmonitor;

//import gov.nist.p25.issi.utils.EndPointHelper;

/**
 * An end point represents an IP address and port of a RFSS.
 */
@SuppressWarnings("unchecked")
public class EndPoint {

   public static final int COL_ENABLED = 0;
   public static final int COL_HOST = 1;
   public static final int COL_PORT = 2;
   public static final int COL_NAME = 3;
   public static final int COL_DOMAIN_NAME = 4;
   public static final int COL_EMULATED = 5;

   public static final String columnNames[] = { 
         "Enabled",
         "Host",
         "Port",
         "Name",
         "DomainName",
         "Emulated",
   };
   public static final Class columnClasses[] = {
         Boolean.class,
         String.class,
         Integer.class,
         String.class,
         String.class,
         Boolean.class,
   };

   private boolean enabled = true;
   private int port;               // 5060
   private String host;            // 10.0.2.25
   private String name;            // VHF
   private String domainName;      // 02.ad5.bee07.p25dr
   private boolean emulated;
   

   // accessor
   public boolean getEnabled() {
      return enabled;
   }
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public String getHost() {
      return host;
   }
   public void setHost(String host) {
      this.host = host;
   }

   public int getPort() {
      return port;
   }
   public void setPort(int port) {
      this.port = port;
   }

   public String getDomainName() {
      return domainName;
   }
   public void setDomainName(String domainName) {
      this.domainName = domainName;
      // checking syntax
      //String[] parts = EndPointHelper.rangeCheckDomainName( domainName);
   }

   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }

   public boolean getEmulated() {
      return emulated;
   }
   public void setEmulated(boolean emulated) {
      this.emulated = emulated;
   }

   // constructor
   public EndPoint() {
   }
   public EndPoint(String host, int port) {
      this( host, port, "", "", false);
   }
   public EndPoint(String host, int port, String domainName) {
      this( host, port, domainName, "", false);
   }
   public EndPoint(String host, int port, String domainName, String name) {
      this( host, port, domainName, name, false);
   }
   public EndPoint(String host, int port, String domainName, String name,
      boolean emulated)
   {
      this.host = host;
      this.port = port;
      this.domainName = domainName;
      this.name = name;
      this.emulated = emulated;
   }

   @Override
   public boolean equals(Object that) {
      return this.getClass().equals(that.getClass())
         && this.getHost().equals(((EndPoint) that).getHost())
         && this.getPort() == ((EndPoint) that).getPort();
   }

   @Override
   public int hashCode() {
      return host.hashCode();
   }

   @Override
   public String toString() {
      return host + ":" + port;
   }
}

