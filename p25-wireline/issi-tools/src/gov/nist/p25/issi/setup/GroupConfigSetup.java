//
package gov.nist.p25.issi.setup;

import gov.nist.p25.issi.utils.EndPointHelper;

/**
 * This class contains the Group Config Setup information.
 */
@SuppressWarnings("unchecked")
public class GroupConfigSetup {

   public static final int COL_NAME = 0;
   public static final int COL_ID = 1;
   public static final int COL_HOME_RFSS_NAME = 2;
   public static final int COL_SU_NAME_LIST = 3;

   private static final String columnNames[] = { 
         "GroupName",
         "GroupId",
         "HomeRfssName",
         "SuNameList",
   };
   private static final boolean columnEditable[] = { 
         false,
         false,
         true,
         false,
   };
   private static final String columnTips[] = { 
         "Group Name",
         "Group Id in hex (0xFFFF)",
         "Home RFSS Name",
         "SU Name List",
   };
   private static final Class columnClasses[] = {
         String.class,
         Integer.class,
         String.class,
         String.class,
   };

   private Object reference;
   private String name;               // group_1
   private int id;                    // 1234
   private String homeRfssName = "";  // rfss_01
   private String suNameList = "";    // su_22,su_33
   
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

   public int getId() {
      return id;
   }
   public void setId(int id) {
      EndPointHelper.rangeCheckGroupId( id);
      this.id = id;
   }
   public String getIdString() {
      return EndPointHelper.encodeGroupId( getId());
   }
   public void setIdString(String idString) {
      this.id = EndPointHelper.decodeGroupId( idString);
   }

   // homeRfssName
   //-------------------------
   public String getHomeRfssName() {
      return homeRfssName;
   }
   public void setHomeRfssName(String homeRfssName) {
      this.homeRfssName = homeRfssName;
   }

   // suNameList
   //-------------------------
   public String getSuNameList() {
      return suNameList;
   }
   public void setSuNameList(String suNameList) {
      this.suNameList = suNameList;
   }

   // GUI supports
   //-------------------------
   public static String[] getColumnNames() {
      return GroupConfigSetup.columnNames;
   }
   public static Class[] getColumnClasses() {
      return GroupConfigSetup.columnClasses;
   }
   public static String[] getColumnTips() {
      return GroupConfigSetup.columnTips;
   }
   public static boolean[] getColumnEditable() {
      return GroupConfigSetup.columnEditable;
   }

   // constructor
   //---------------------------------------------------------------------
   public GroupConfigSetup() {
   }

   public GroupConfigSetup(String name, int id, String homeRfssName, 
      String suNameList) 
   {
      setName(name);
      setId(id);
      setHomeRfssName(homeRfssName);
      setSuNameList(suNameList);
   }

   @Override
   public boolean equals(Object that) {
      return this.getClass().equals(that.getClass())
         && this.getName().equals(((GroupConfigSetup) that).getName())
         && this.getId() == ((GroupConfigSetup) that).getId();
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
      case COL_NAME:
         return getName();
      case COL_ID:
         return getId();
      case COL_HOME_RFSS_NAME:
         return getHomeRfssName();
      case COL_SU_NAME_LIST:
         return getSuNameList();
      default:
         return null;
      }
   }

   public void setValue(int column, Object value) {
      System.out.println("GroupConfigSetup: value="+value);
      if(value == null) return;
      switch( column) {
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
      case COL_SU_NAME_LIST:
         setSuNameList( (String)value);
         break;
      }
   }
}
