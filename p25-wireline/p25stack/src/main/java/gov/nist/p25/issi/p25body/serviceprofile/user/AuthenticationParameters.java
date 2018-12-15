//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import java.text.ParseException;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Authentication Parameters attribute specifies the authentication
 * parameters associated with the authentication type specified in the
 * Authentication Type attribute.
 * 
 */
public class AuthenticationParameters extends ServiceProfileLine {

   public static String NAME = "u-authparam";
   public static String SHORTNAME = "u-apa";

   private String[] rs;
   private String[] ks;
   private String[] ksPrime;

   AuthenticationParameters(String params) throws ParseException {
      super(NAME, params);
      String[] vals = params.split(",");
      if (vals.length != 3)
         throw new ParseException("Illegal argument [" + params + "]", 0);
      rs = vals[0].split(" ");
      if (rs.length != 10)
         throw new ParseException("Illegal rs value [" + vals[0] + "]", 0);
      for (String str : rs) {
         try {
            int tryval = Integer.parseInt(str, 16);
            if (tryval > 0xff || tryval < 0)
               throw new ParseException("Bad val [" + str + "]", 0);
         } catch (NumberFormatException ex) {
            throw new ParseException("Bad val [" + str + "]", 0);
         }
      }
      this.ks = vals[1].split(" ");
      if (ks.length != 16)
         throw new ParseException("Illegal ks value [" + vals[0] + "]", 0);
      for (String str : ks) {
         try {
            int tryval = Integer.parseInt(str, 16);
            if (tryval > 0xff || tryval < 0)
               throw new ParseException("Bad val [" + str + "]", 0);
         } catch (NumberFormatException ex) {
            throw new ParseException("Bad val [" + str + "]", 0);
         }
      }

      this.ksPrime = vals[2].split(" ");
      for (String str : ksPrime) {
         try {
            int tryval = Integer.parseInt(str, 16);
            if (tryval > 0xff || tryval < 0)
               throw new ParseException("Bad val [" + str + "]", 0);
         } catch (NumberFormatException ex) {
            throw new ParseException("Bad val [" + str + "]", 0);
         }
      }
   }

   /**
    * @param rs
    *            The rs to set.
    */
   public void setRs(String[] rs) {
      this.rs = rs;
   }

   /**
    * @return Returns the rs.
    */
   public String[] getRs() {
      return rs;
   }

   /**
    * @param ks
    *            The ks to set.
    */
   public void setKs(String[] ks) {
      this.ks = ks;
   }

   /**
    * @return Returns the ks.
    */
   public String[] getKs() {
      return ks;
   }

   /**
    * @param ksPrime
    *            The ksPrime to set.
    */
   public void setKsPrime(String[] ksPrime) {
      this.ksPrime = ksPrime;
   }

   /**
    * @return Returns the ksPrime.
    */
   public String[] getKsPrime() {
      return ksPrime;
   }

   /**
    * To string.
    */
   @Override
   public String toString() {
      StringBuffer buff = new StringBuffer(NAME + ":");
      for (int i = 0; i < this.rs.length; i++) {
         buff.append(rs[i]);
         if (i < rs.length - 1)
            buff.append(" ");
         else
            buff.append(",");
      }

      for (int i = 0; i < ks.length; i++) {
         buff.append(ks[i]);
         if (i < ks.length - 1)
            buff.append(" ");
         else
            buff.append(",");
      }

      for (int i = 0; i < ksPrime.length; i++) {
         buff.append(ksPrime[i]);
         if (i < ksPrime.length - 1)
            buff.append(" ");

      }
      return buff.toString();
   }
}
