//
package gov.nist.p25.issi.p25body.serviceprofile;

import java.text.ParseException;

/**
 * ServiceProfileLine
 * 
 */
public abstract class ServiceProfileLine {
   
   /**
    * Name of the profile line.
    */
   private String name;

   /**
    * Value of the profile line as a string.
    */
   private String value;

   public ServiceProfileLine(String name, String value) {
      this.name = name;
      this.value = value;
   }

   /**
    * Convert this line to a string.
    */
   @Override
   public String toString() {
      return new StringBuffer().append(name).append(":").append(value)
            .toString();
   }
   
   /**
    * Convert String to int throw parse exception if not convertable
    */
   protected int getIntFromString(String strVal) throws ParseException {
      try {
         return Integer.parseInt(strVal);
      } catch (NumberFormatException nfe) {
         throw new ParseException("Bad integer format [" + strVal + "]", 0);
      }
   }

   /**
    * Convert from String to boolean
    */
   protected static boolean getBoolFromString(String strval)
         throws ParseException {
      if ("1".equals(strval))
         return true;
      else if ("0".equals(strval))
         return false;
      else
         throw new ParseException("Invalid boolean argument " + strval, 0);
   }

   /**
    * Cosntructor when value is supplied as a boolean
    * 
    * @param name
    * @param defaultValue
    */
   protected ServiceProfileLine(String name, boolean defaultValue) {
      this.name = name;
      this.setValue(defaultValue);
   }

   /**
    * Constructor given name and value as a long.
    * 
    * @param name
    * @param value
    */
   protected ServiceProfileLine(String name, long value) {
      this.name = name;
      this.setValue(value);
   }

   /**
    * Constructor given name and value as int.
    */
   protected ServiceProfileLine(String name, int value) {
      this.name = name;
      this.setValue(value);
   }

   /**
    * @param name
    *            The name to set.
    */
   protected void setName(String name) {
      this.name = name;
   }

   /**
    * @return Returns the name.
    */
   public String getName() {
      return name;
   }

   /**
    * @param value
    *            The value to set.
    */
   protected void setValue(String value) {
      this.value = value;
   }

   /**
    * @return Returns the value.
    */
   public String getValue() {
      return value;
   }

   /**
    * Set the value to a boolean value
    * 
    * @param boolValue =
    *            boolean value to set.
    */
   protected void setValue(boolean boolValue) {
      this.value = (boolValue ? "1" : "0");
   }

   /**
    * Set the value to an integer value.
    */
   protected void setValue(int intVal) {
      this.value = new Integer(intVal).toString();
   }

   /**
    * Set the value to a long value formatted as a hex string.
    * 
    * @param groupId
    */
   protected void setValue(long groupId) {
      this.value = Long.toHexString(groupId);
   }

   /**
    * Get Name="value" type representation for xml representation of this
    * service profile line.
    */
   public String toXMLAttribute() {
      return new StringBuffer().append(name).append(
            "= \"" + this.value + "\"").toString();
   }
}
