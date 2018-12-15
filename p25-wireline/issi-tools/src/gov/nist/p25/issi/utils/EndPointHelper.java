//
package gov.nist.p25.issi.utils;

/**
 * EndPoint Helper Utility
 */
public class EndPointHelper
{
   public static void showln(String s) { System.out.println(s); }
   public static final String P25_HOLDER = "p25dr";

   // rfssId is expressed as 2 or 4 hexadecimal digits (8-bits or 16-bits).
   // Console subsystems that donot have RF sites may be assigned a 4Hex digit.
   // Console subsystems that have RF sites shall be assigned a 2Hex digit.
   //---------------------------------------------------------------------------
   public static String rangeCheckRfssId( int rfssId)
   {
      String rfssIdName = Integer.toHexString( rfssId);
      //if (rfssId > 0xff || rfssId < 0) {
      if (rfssId < 0 || rfssId > 0xffff) {
         throw new IllegalArgumentException(
            "RfssId out of range should be <= 0xffff, was 0x" + rfssIdName);
      }
      return rfssIdName;
   }

   public static String encodeRfssId( int rfssId)
   {
      String rfssIdName = rangeCheckRfssId( rfssId);
      int klen = 2;
      if( rfssId > 0xff) klen = 4;

      int diff = klen - rfssIdName.length();
      for (int i = 0; i < diff; i++) {
         rfssIdName = "0" + rfssIdName;
      }
      return rfssIdName.toLowerCase();
   }

   public static int decodeRfssId( String rfssIdName)
      throws NumberFormatException
   {
      int klen = 4;
      if( rfssIdName.length() > klen) {
         throw new IllegalArgumentException(
            "The rfssId length must be <= 4 : "+rfssIdName);
      }
      int rfssId = Integer.parseInt( rfssIdName, 16);
      if (rfssId < 0 || rfssId > 0xffff) {
         throw new IllegalArgumentException(
            "RfssId out of range should be <= 0xffff was 0x" + rfssIdName);
      }
      return rfssId;
   }

   //---------------------------------------------------------------------------
   public static String rangeCheckSystemId( int systemId)
   {
      String systemIdName = Integer.toHexString(systemId);
      if (systemId > 0x0fff || systemId < 0) {
         throw new IllegalArgumentException(
            "SystemId out of range should be <= 0xfff was 0x" + systemIdName);
      }
      return systemIdName;
   }

   public static String encodeSystemId( int systemId)
   {
      String systemIdName = rangeCheckSystemId(systemId);
      int diff = 3 - systemIdName.length();
      for (int i = 0; i < diff; i++) {
         systemIdName = "0" + systemIdName;
      }
      return systemIdName.toLowerCase();
   }

   public static int decodeSystemId( String systemIdName)
   {
      if( systemIdName.length() > 3) {
         throw new IllegalArgumentException(
            "The systemId length must be <= 3 : "+systemIdName);
      }
      int systemId = Integer.parseInt( systemIdName, 16);
      // the following check may not needed !!!
      if (systemId > 0xfff || systemId < 0) {
         throw new IllegalArgumentException(
            "SystemId out of range should be <= 0xfff was 0x" + systemIdName);
      }
      return systemId;
   }

   //---------------------------------------------------------------------------
   public static String rangeCheckGroupId( int groupId)
   {
      String groupIdName = Integer.toHexString(groupId);
      if (groupId > 0xffff || groupId < 0) {
         throw new IllegalArgumentException(
            "GroupId out of range should be <= 0xffff was 0x" + groupIdName);
      }
      return groupIdName;
   }

   public static String encodeGroupId( int groupId)
   {
      String groupIdName = rangeCheckGroupId(groupId);
      int diff = 4 - groupIdName.length();
      for (int i = 0; i < diff; i++) {
         groupIdName = "0" + groupIdName;
      }
      return groupIdName.toLowerCase();
   }

   public static int decodeGroupId( String groupIdName)
   {
      if( groupIdName.length() > 4) {
         throw new IllegalArgumentException(
            "The groupId length must be <= 4 : "+groupIdName);
      }
      int groupId = Integer.parseInt( groupIdName, 16);
      if (groupId > 0xffff || groupId < 0) {
         throw new IllegalArgumentException(
            "GroupId out of range should be <= 0xffff was 0x" + groupIdName);
      }
      return groupId;
   }

   //---------------------------------------------------------------------------
   public static String rangeCheckWacnId( int wacnId)
   {
      String wacnIdName = Integer.toHexString(wacnId);
      if( wacnId > 0xfffff || wacnId < 0) {
         throw new IllegalArgumentException(
            "WacnId out of range should be <= 0xfffff was 0x" + wacnIdName);
      }
      return wacnIdName;
   }

   public static String encodeWacnId( int wacnId)
   {
      String wacnIdName = rangeCheckWacnId(wacnId);
      int diff = 5 - wacnIdName.length();
      for (int i = 0; i < diff; i++) {
         wacnIdName = "0" + wacnIdName;
      }
      return wacnIdName.toLowerCase();
   }

   public static int decodeWacnId( String wacnIdName)
   {
      if( wacnIdName.length() > 5) {
         throw new IllegalArgumentException(
            "The wacnId length must be <= 5 : "+wacnIdName);
      }

      int wacnId = Integer.parseInt(wacnIdName, 16);
      if( wacnId > 0xfffff || wacnId < 0) {
         throw new IllegalArgumentException(
            "WacnId out of range should be <= 0xfffff was 0x" + wacnIdName);
      }
      return wacnId;
   }

   //---------------------------------------------------------------------------
   public static String rangeCheckSuId( int suId)
   {
      String suIdName = Integer.toHexString(suId);
      if (suId > 0xffffff || suId < 0) {
         throw new IllegalArgumentException(
            "SuId out of range should be <= 0xffffff was 0x" + suIdName);
      }
      return suIdName;
   }

   public static String encodeSuId( int suId)
   {
      String suIdName = rangeCheckSuId(suId);
      int diff = 6 - suIdName.length();
      for (int i = 0; i < diff; i++) {
         suIdName = "0" + suIdName;
      }
      return suIdName.toLowerCase();
   }

   public static int decodeSuId( String suIdName)
   {
      if( suIdName.length() > 6) {
         throw new IllegalArgumentException(
            "The suId length must be <= 6 : "+suIdName);
      }
      int suId = Integer.parseInt(suIdName, 16);
      if (suId > 0xffffff || suId < 0) {
         throw new IllegalArgumentException(
            "SuId out of range should be <= 0xffffff was 0x" + suIdName);
      }
      return suId;
   }

   //---------------------------------------------------------------------------
   public static String[] rangeCheckDomainName(String domainName)
   {
      if( domainName.length() > 18) {
         throw new IllegalArgumentException(
            "The <rfssid>.<sysId>.<wacnId>.p25dr length must be <= 18 in domainName: "+domainName);
      }
      String[] parts = domainName.split("\\.");
      if( parts.length == 3) {
         // assume <rfssId>.<sysId>.<wacnId> 
      }
      else if( parts.length == 4) {
         if( !P25_HOLDER.equals( parts[3])) {
            throw new IllegalArgumentException(
            "The place holder must be 'p25dr' in domainName: "+domainName);
         }
      }
      else {
         throw new IllegalArgumentException(
            "The total number of components must be 4 in domainName: "+domainName);
      }
      return parts;
   }

   public static String encodeDomainName(int rfssId, int systemId, int wacnId)
   {
      String rfssIdName = encodeRfssId( rfssId);
      String systemIdName = encodeSystemId( systemId);
      String wacnIdName = encodeWacnId( wacnId);

      String domain = rfssIdName + "." + systemIdName + "." + wacnIdName +
                      "." + P25_HOLDER;
      return domain.toLowerCase();
   }

   public static String[] decodeDomainName(String domainName)
   {
      String[] parts = rangeCheckDomainName( domainName);

      // validate each components
      decodeRfssId( parts[0]);
      decodeSystemId( parts[1]);
      decodeWacnId( parts[2]);
      return parts;
   }

   //---------------------------------------------------------------------------
   public static String[] rangeCheckRadicalName(String radicalName)
   {
      // wacnId, systemId, suId (5+3+6)
      if( radicalName.length() != 14) {
         throw new IllegalArgumentException(
            "The radicalName length must be 14. "+radicalName);
      }
      String[] parts = new String[3];
      parts[0] = radicalName.substring( 0, 5);
      parts[1] = radicalName.substring( 5, 8);
      parts[2] = radicalName.substring( 8, 14);
      return parts;
   }
   public static String encodeRadicalName(int wacnId, int systemId, int suId)
   {
      String wacnIdName = encodeWacnId( wacnId);
      String systemIdName = encodeSystemId( systemId);
      String suIdName = encodeSuId( suId);

      String radicalName = wacnIdName + systemIdName + suIdName;
      return radicalName.toLowerCase();
   }

   public static String[] decodeRadicalName(String radicalName)
   {
      // wacnId, systemId, suId (5+3+6)
      String[] parts = rangeCheckRadicalName( radicalName);

      // validate each components
      decodeWacnId( parts[0]);
      decodeSystemId( parts[1]);
      decodeSuId( parts[2]);
      return parts;
   }

   //---------------------------------------------------------------------------
   public static String rangeCheckIpAddress(String ipStr)
   {
      String[] parts = ipStr.split("\\.");
      if( parts.length != 4) {
         throw new NumberFormatException(
            "IpAddress must be in form of ddd.ddd.ddd.ddd instead of: "+ipStr);
      }
      for(int i=0; i < 4; i++) {
         int a1 = Integer.parseInt(parts[i]);
         if( a1 < 0 || a1 > 255) {
            throw new NumberFormatException(
            "IpAddress component: "+parts[i]+" must be in range of 0..255");
	 }
      }
      return ipStr;
   }
   //===========================================================================
   public static void main(String[] argv) 
   {
      //int groupId = 0x0123;
      int rfssId = 0x02;
      int systemId = 0x0ad5;
      int wacnId = 0x0bee07;
      int suId = 32500;
      String id;
      String[] parts;

      id = EndPointHelper.encodeRfssId(rfssId);
      showln("rfssId(1)="+id);
      showln("rfssId(2)="+EndPointHelper.decodeRfssId(id));

      id = EndPointHelper.encodeSystemId(systemId);
      showln("systemId(1)="+id);
      showln("systemId(2)="+EndPointHelper.decodeSystemId(id));

      id = EndPointHelper.encodeWacnId(wacnId);
      showln("wacnId(1)="+id);
      showln("wacnId(2)="+EndPointHelper.decodeWacnId(id));

      id = EndPointHelper.encodeWacnId(suId);
      showln("suId(1)="+id);
      showln("suId(2)="+EndPointHelper.decodeSuId(id));


      String domainName  = EndPointHelper.encodeDomainName(rfssId,systemId,wacnId); 
      showln("domainName(1)=" + domainName);
      parts = EndPointHelper.decodeDomainName(domainName);
      showln("domainName(2)=" + parts[0]+":"+parts[1]+":"+parts[2]);

      String radicalName = EndPointHelper.encodeRadicalName(wacnId,systemId,suId); 
      showln("radicalName(1)=" + radicalName);
      parts = EndPointHelper.decodeRadicalName(radicalName);
      showln("radicalName(2)=" + parts[0]+":"+parts[1]+":"+parts[2]);
   }
}
