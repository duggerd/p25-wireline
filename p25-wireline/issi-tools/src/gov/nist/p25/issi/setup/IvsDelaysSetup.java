//
package gov.nist.p25.issi.setup;

/**
 * This class contains the IVS Delay Setup information.
 */
@SuppressWarnings("unchecked")
public class IvsDelaysSetup {

   public static final int TYPE_SPEC = 0;
   public static final int TYPE_TEST = 1;

   public static final int COL_PARAMETER_NAME = 0;
   public static final int COL_AVERAGE_TIME = 1;
   public static final int COL_MAXIMUM_TIME = 2;
   public static final int COL_CALCULATED = 3;

   private static final String columnNames[] = { 
         "Parameter",
         "Average (ms)",
         "Maximum (ms)",
         "Calculated",
   };
   private static final boolean columnEditable[] = { 
         false,
         true,
         true,
         false,
   };
   private static final String columnTips[] = { 
         "Parameters defined in TIA-102.CACB",
         "Measured Average time(ms)",
         "Measured Maximum time(ms)",
         "Calculated parameter",
   };
   private static final Class columnClasses[] = {
         String.class,
         Integer.class,
         Integer.class,
         Boolean.class,
   };

   private String parameterName;
   private int averageTime;
   private int maximumTime = -1;
   private boolean calculated;
   private boolean cellSelectionEnabled;   // for maximum N/A
   
   // accessors
   public String getParameterName() {
      return parameterName;
   }
   public void setParameterName(String parameterName) {
      this.parameterName = parameterName;
   }

   public int getAverageTime() {
      return averageTime;
   }
   public void setAverageTime(int averageTime) {
      this.averageTime = averageTime;
   }

   public int getMaximumTime() {
      return maximumTime;
   }
   public void setMaximumTime(int maximumTime) {
      this.maximumTime = maximumTime;
      setCellSelectionEnabled(true);
   }

   public boolean getCalculated() {
      return calculated;
   }
   public void setCalculated(boolean calculated) {
      this.calculated = calculated;
   }

   public boolean getCellSelectionEnabled() {
      return cellSelectionEnabled;
   }
   public void setCellSelectionEnabled(boolean cellSelectionEnabled) {
      this.cellSelectionEnabled = cellSelectionEnabled;
   }

   // GUI supports
   //-------------------------
   public static String[] getColumnNames() {
      return IvsDelaysSetup.columnNames;
   }
   public static Class[] getColumnClasses() {
      return IvsDelaysSetup.columnClasses;
   }
   public static String[] getColumnTips() {
      return IvsDelaysSetup.columnTips;
   }
   public static boolean[] getColumnEditable() {
      return IvsDelaysSetup.columnEditable;
   }

   // constructor
   //---------------------------------------------------------------------
   public IvsDelaysSetup() {
   }

   public IvsDelaysSetup(String parameter, int average) 
   {
      setParameterName(parameter);
      setAverageTime(average);
   }

   public IvsDelaysSetup(String parameter, int average, int maximum) 
   {
      setParameterName(parameter);
      setAverageTime(average);
      setMaximumTime(maximum);
   }

   @Override
   public String toString() {
      return getParameterName();
   }

   // GUI support
   //-------------------------------------------------------
   public Object getValue(int column) {
      switch( column) {
      case COL_PARAMETER_NAME:
         return getParameterName();
      case COL_AVERAGE_TIME:
         return getAverageTime();
      case COL_MAXIMUM_TIME:
         return getMaximumTime();
      case COL_CALCULATED:
         return new Boolean(getCalculated());
      default:
         return null;
      }
   }

   public void setValue(int column, Object value) {
      //System.out.println("IvsDelaysSetup: setValue="+value);
      if(value == null) return;
      switch( column) {
      case COL_PARAMETER_NAME:
         setParameterName( (String)value);
         break;
      case COL_AVERAGE_TIME:
         if( value instanceof Integer) {
            setAverageTime( ((Integer)value).intValue());
         }
         else if( value instanceof String) {
            try {
               setAverageTime( Integer.parseInt((String)value));
            } catch(Exception ex) { }
         }
         break;
      case COL_MAXIMUM_TIME:
         if( value instanceof Integer) {
            setMaximumTime( ((Integer)value).intValue());
         }
         else if( value instanceof String) {
            try {
               setMaximumTime( Integer.parseInt((String)value));
            } catch(Exception ex) { }
         }
         break;
      case COL_CALCULATED:
         Boolean eValue = (Boolean)value;
         setCalculated( eValue.booleanValue());
         break;
      }
   }
}
