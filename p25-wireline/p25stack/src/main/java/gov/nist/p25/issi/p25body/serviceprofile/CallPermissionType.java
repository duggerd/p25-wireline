//
package gov.nist.p25.issi.p25body.serviceprofile;

/**
 * Defines a generic Call permission.
 * 
 */
public enum CallPermissionType {
   
   // ("None" | "Receive" | "Initiate" | "ReceiveAndInitiate" )
   NONE(0, "No  permission", "None"),
   RECEIVE(1, "Receive call permission","Receive"),
   INITIATE(2, "Initiate call permission","Initiate"),
   RECEIVE_AND_INITIATE(3,
         "Receive and Initiate Call permission","ReceiveAndInitiate");

   private int intValue;
   private String description;
   private String shortName ; 


   CallPermissionType(int intValue, String description, String shortName) {
      this.intValue = intValue;
      this.description = description;
      this.shortName = shortName;
   }

   public int getIntValue() {
      return intValue;
   }

   public static CallPermissionType getInstance(int intValue) {
      for (CallPermissionType p : CallPermissionType.values()) {
         if (p.intValue == intValue)
            return p;
      }
      throw new IllegalArgumentException("Illegal input arg: " + intValue);
   }

   @Override
   public String toString() {
      return description;
   }

   public static CallPermissionType getInstance(String name) {
      for (CallPermissionType p : CallPermissionType.values()) {
         if (p.shortName.equals(name))
            return p;
      }
      throw new IllegalArgumentException("Illegal input arg: " + name);
   }
}
