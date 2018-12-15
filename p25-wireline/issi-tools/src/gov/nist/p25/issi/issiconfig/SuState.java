//
package gov.nist.p25.issi.issiconfig;

/**
 * States of Subscriber Unit.
 */
public enum SuState {

   ON, OFF;

   @Override
   public String toString() {
      if (this == ON)
         return "ON";
      else
         return "OFF";
   }
}
