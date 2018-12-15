//
package gov.nist.p25.common.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * IP Address Utility.
 */
public class IpAddressUtility
{
   public static void showln(String s) { System.out.println(s); }

   //
   // http://www.jguru.com/faq/view.jsp?EID=15835
   //
   public static InetAddress getLocalHostAddress() throws Exception
   {
      String osname = System.getProperty("os.name");
      //showln("getLocalHostAddress: os.name=" + osname);
      InetAddress ia = null;
      if( "Linux".equals(osname)) {
         ia = getInetAddressByName("eth0");
      }
      else {
         ia = getNetworkInterfaceAddress();
      }
      return ia;
   }

   public static InetAddress getNetworkInterfaceAddress() throws Exception
   {
      InetAddress ia = null;
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while( ifaces.hasMoreElements())
      {
         NetworkInterface iface = (NetworkInterface)ifaces.nextElement();
         //showln("   --->"+iface.getName());

         for(Enumeration<InetAddress> ips=iface.getInetAddresses(); ips.hasMoreElements(); ) {
            ia = (InetAddress)ips.nextElement();
            if(!ia.isSiteLocalAddress() && 
               !ia.isLoopbackAddress() && 
                ia.getHostAddress().indexOf(":")==-1) {
               // found it
               //showln("Good Interface "+iface.getName());
               break;
            }
         }
      }
      return ia;
   }

   //
   // http://www.jguru.com/faq/view.jsp?EID=790132
   //
   public static InetAddress getEthernetAddress() throws Exception
   {
      InetAddress xia = null;
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while( ifaces.hasMoreElements())
      {
         NetworkInterface iface = (NetworkInterface)ifaces.nextElement();
         //showln("\n*** Interface:"+ iface.getDisplayName());

         for(Enumeration<InetAddress> ips=iface.getInetAddresses(); ips.hasMoreElements(); ) {
            InetAddress ia = (InetAddress)ips.nextElement();
            //showln("  "+ia.getCanonicalHostName()+"  "+ia.getHostAddress());

            // this compare may not be the best way, but it works
            if( ia.getCanonicalHostName().equals(ia.getHostAddress()) ) {
               xia = ia;
               break;
            }
         }
      }
      return xia;
   }

   public static InetAddress getInetAddressByName(String displayName)
      throws Exception
   {
      //showln("getInetAddressByName: os.name="+System.getProperty("os.name"));
      InetAddress xia = null;
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while( ifaces.hasMoreElements())
      {
         NetworkInterface iface = (NetworkInterface)ifaces.nextElement();
         //showln("\n*** Interface:"+ iface.getDisplayName());
         for(Enumeration<InetAddress> ips=iface.getInetAddresses(); ips.hasMoreElements(); ) {
            InetAddress ia = (InetAddress)ips.nextElement();
            //showln("  "+ia.getCanonicalHostName()+"  "+ia.getHostAddress());
            if( ia.getHostAddress().indexOf(".") != -1) {
               xia = ia;
               return xia;
            }
         }
      }
      return xia;
   }

   public static void dumpNetworkInterfaces() throws Exception
   {
      showln("------------------------");
      Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while( ifaces.hasMoreElements())
      {
         NetworkInterface iface = (NetworkInterface)ifaces.nextElement();
         showln("\n*** Interface:"+ iface.getDisplayName());
         InetAddress ia = null;
         for(Enumeration<InetAddress> ips=iface.getInetAddresses(); ips.hasMoreElements(); ) {
            ia = (InetAddress)ips.nextElement();
            showln("  "+ia.getCanonicalHostName()+"  "+ia.getHostAddress());
         }
      }
      showln("------------------------");
   }

   //=============================================================================
   public static void main(String[] args) throws Exception
   {
      dumpNetworkInterfaces();
      showln("\n+++ ipStr="+getEthernetAddress().getHostAddress());
      showln("\n+++ localHost="+getLocalHostAddress().getHostAddress());
      showln("\n+++ localHost="+getInetAddressByName("eth0").getHostAddress());
   }
}


