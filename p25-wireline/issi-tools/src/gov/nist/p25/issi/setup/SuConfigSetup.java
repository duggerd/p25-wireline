//
package gov.nist.p25.issi.setup;

import gov.nist.p25.issi.utils.EndPointHelper;

/**
 * This class contains the SU Config Setup information.
 */
@SuppressWarnings("unchecked")
public class SuConfigSetup {

   public static final int COL_EMULATED = 0;
   public static final int COL_NAME = 1;
   public static final int COL_ID = 2;
   public static final int COL_HOME_RFSS_NAME = 3;
   public static final int COL_SERVING_RFSS_NAME = 4;

   public static final int COL_HOME_RFSS_ID = 5;
   public static final int COL_SYSTEM_ID = 6;
   public static final int COL_WACN_ID = 7;
   public static final int COL_RADICAL_NAME = 8;

   private static final String columnNames[] = { 
         "Emulated",
         "SuName",
         "SuId",
         "HomeRfssName",
         "ServingRfssName",
         //"HomeRfssId",
         //"SystemId",
         //"WacnId",
         //"RadicalName",
   };
   private static final boolean columnEditable[] = { 
         true,
         false,
         true,
         true,
         true,
         //true,
         //true,
         //true,
         //true,
   };
   private static final String columnTips[] = { 
         "Set Emulated to designate the emulated SU",
         "Subscriber Unit Name",
         "Subscriber Id in hex (0xFFFFFF)",
         "Home RFSS Name",
         "Serving RFSS Name",
         //"Home RFSS Id in hex (0xFF)",
         //"System Id in hex (0xFFF)",
         //"Wacn Id in hex (0xFFFFF)",
         //"Radical Name <wacnId><systemId><suId>(14 Hex)",
   };
   private static final Class columnClasses[] = {
         Boolean.class,
         String.class,
         Integer.class,
         String.class,
         String.class,
         //Integer.class,
         //Integer.class,
         //Integer.class,
         //String.class,
   };

   private Object reference;
   private String name;                 // su_21
   private int id;                      // abc111
   private String homeRfssName = "";    // rfss_01
   private String servingRfssName = ""; // rfss_02
   private int homeRfssId;              // 01
   private int systemId;                // ad5(hex)
   private int wacnId;                  // bee07(hex)
   private boolean emulated = true;
   
   // accessors
   public Object getReference() {
      return reference;
   }
   public void setReference(Object reference) {
      this.reference = reference;
   }

   public boolean isEmulated() {
      return emulated;
   }
   public boolean getEmulated() {
      return emulated;
   }
   public void setEmulated(boolean emulated) {
      this.emulated = emulated;
   }

   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }

   public int getId() {
      return id;
   }
   public void setId(int id) {
      EndPointHelper.rangeCheckSuId( id);
      this.id = id;
   }
   public String getIdString() {
      return EndPointHelper.encodeSuId( getId());
   }
   public void setIdString(String idString) {
      this.id = EndPointHelper.decodeSuId( idString);
   }

   // homeRfssName
   //-------------------------
   public String getHomeRfssName() {
      return homeRfssName;
   }
   public void setHomeRfssName(String homeRfssName) {
      this.homeRfssName = homeRfssName;
   }

   // servingRfssName
   //-------------------------
   public String getServingRfssName() {
      return servingRfssName;
   }
   public void setServingRfssName(String servingRfssName) {
      this.servingRfssName = servingRfssName;
   }

   // servingRfssId
   //-------------------------
   public int getHomeRfssId() {
      return homeRfssId;
   }
   public void setHomeRfssId(int id) {
      EndPointHelper.rangeCheckRfssId(id);
      this.homeRfssId = id;
   }
   public String getHomeRfssIdString() {
      return EndPointHelper.encodeRfssId( getHomeRfssId());
   }
   public void setHomeRfssIdString(String idString) {
      this.homeRfssId = EndPointHelper.decodeRfssId(idString);
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
   // radicalName - derived from ids
   public String getRadicalName() {
      return EndPointHelper.encodeRadicalName( getWacnId(), getSystemId(), getId());
   }
   public void setRadicalName(String radicalName) {
      String[] parts = EndPointHelper.decodeRadicalName( radicalName);
      if( parts.length==3) {
         setWacnIdString( parts[0]);
         setSystemIdString( parts[1]);
         setIdString( parts[2]);
      }
   }

   // GUI supports
   public static String[] getColumnNames() {
      return SuConfigSetup.columnNames;
   }
   public static Class[] getColumnClasses() {
      return SuConfigSetup.columnClasses;
   }
   public static String[] getColumnTips() {
      return SuConfigSetup.columnTips;
   }
   public static boolean[] getColumnEditable() {
      return SuConfigSetup.columnEditable;
   }

   // constructor
   //---------------------------------------------------------------------
   public SuConfigSetup() {
   }

   public SuConfigSetup(String name, int id, 
      String homeRfssName, String servingRfssName, int homeRfssId,
      int systemId, int wacnId, boolean emulated)
   {
      setName(name);
      setId(id);
      setHomeRfssName(homeRfssName);
      setServingRfssName(servingRfssName);
      setHomeRfssId(homeRfssId);
      setSystemId(systemId);
      setWacnId(wacnId);
      setEmulated(emulated);
   }

   @Override
   public boolean equals(Object that) {
      return this.getClass().equals(that.getClass())
         && this.getName().equals(((SuConfigSetup) that).getName())
         && this.getId() == ((SuConfigSetup) that).getId();
   }

   @Override
   public int hashCode() {
      return getName().hashCode();
   }

   @Override
   public String toString() {
      return getName() + ":" + getIdString();
   }

   // GUI support
   //-------------------------------------------------------
   public Object getValue(int column) {
      switch( column) {
      case COL_EMULATED:
         return new Boolean(getEmulated());
      case COL_NAME:
         return getName();
      case COL_ID:
         return getId();
      case COL_HOME_RFSS_NAME:
         return getHomeRfssName();
      case COL_SERVING_RFSS_NAME:
         return getServingRfssName();
      case COL_HOME_RFSS_ID:
         return getHomeRfssId();
      case COL_SYSTEM_ID:
         return getSystemId();
      case COL_WACN_ID:
         return getWacnId();
      case COL_RADICAL_NAME:
         return getRadicalName();
      default:
         return null;
      }
   }
   public void setValue(int column, Object value) {
      System.out.println("SuConfigSetup: value="+value);
      if(value == null) return;
      switch( column) {
      case COL_EMULATED:
         setEmulated( ((Boolean)value).booleanValue());
         break;
      case COL_NAME:
         setName( (String)value);
         break;
      case COL_ID:
         if( value instanceof Integer) {
            setId( ((Integer)value).intValue());
         }
         else if( value instanceof String) {
            try {
               setId( Integer.parseInt((String)value));
            } catch(Exception ex) { }
         }
         break;
      case COL_HOME_RFSS_NAME:
         setHomeRfssName( (String)value);
         break;
      case COL_SERVING_RFSS_NAME:
         setServingRfssName( (String)value);
         break;
      case COL_HOME_RFSS_ID:
         if( value instanceof Integer) {
            setHomeRfssId( ((Integer)value).intValue());
         }
         else if( value instanceof String) {
            try {
               setHomeRfssId( Integer.parseInt((String)value));
            } catch(Exception ex) { }
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
      case COL_RADICAL_NAME:
         setRadicalName( (String)value);
         break;
      }
   }
}
