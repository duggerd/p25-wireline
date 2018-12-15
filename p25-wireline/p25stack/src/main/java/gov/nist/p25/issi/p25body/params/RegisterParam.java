//
package gov.nist.p25.issi.p25body.params;

import java.text.ParseException;
import org.apache.log4j.Logger;

/**
 * RegisterParam class
 */
public class RegisterParam {
   private static Logger logger = Logger.getLogger(RegisterParam.class);
   
   private static final String CR_LF = "\r\n";
   private static final String FORCE_REGISTER = "r-force";   
   private static final String CONFIRM_PRESENCE = "r-presence";
   private static final String CONFIRM_REGISTER = "r-confirm";

   /*
    * This parameter is used by the Home RFSS in the Home Registration Query
    * procedure. The Force qualifier indicates that the Serving RFSS has to
    * re-register the mobility object if it still has interest.
    */
   private boolean forceRegister = false;
   private boolean forceRegisterFlag;

   /*
    * This parameter is used by the Home RFSS in the Home Registration Query
    * procedure. The Confirm qualifier requests that the serving RFSS confirm
    * its interest for the mobility object.
    */
   private boolean confirmRegister = false;
   private boolean confirmRegisterFlag;

   /*
    * SU Presence This parameter is used by the Serving RFSS in the
    * Registration procedure for an SU. The  Presence qualifier indicates
    * whether or not the Serving RFSS has actually confirmed the presence of
    * the SU at the Serving RFSS immediately prior to sending the Registration
    * request. See Section 5 for further details.
    * 
    */
   private boolean confirmPresence = false;
   private boolean confirmPresenceFlag;

   private static boolean getBooleanFromString(String str)
         throws ParseException {
      if ("1".equals(str))
         return true;
      else if ("0".equals(str))
         return false;
      else
         throw new ParseException("Bad token [" + str + "]", 0);
   }

   private static String getStringFromBoolean(boolean bool) {
      return (bool ? "1" : "0");
   }

   /**
    * Default constructor.
    * 
    * @return The RegisterParam object
    */
   public static RegisterParam createRegisterParam() {
      return new RegisterParam();
   }

   /**
    * Create a register param from a string.
    * 
    * @param registrationParam
    * @return The RegisterParam object
    * @throws ParseException
    */
   public static RegisterParam createRegisterParam(String registrationParam)
         throws ParseException {
      RegisterParam retval = new RegisterParam( registrationParam);
      logger.debug("createRegisterParam: created " + retval);
      return retval;
   }

   // constructor
   public RegisterParam() {
   }
   public RegisterParam(String registrationParam)
         throws ParseException {
      logger.debug("RegisterParam: " + registrationParam);

      String[] lines = registrationParam.split("\n");
      for (String line : lines) {
         String ln[] = line.trim().split(":");
         if (ln.length != 2) {
            throw new ParseException("Invalid line [" + line + "]", 0);
         }

         String tok = ln[1];
         if (ln[0].equalsIgnoreCase("r-force")
               || ln[0].equalsIgnoreCase("r-f")) {
            setForceRegister( getBooleanFromString(tok));

         } else if (ln[0].equalsIgnoreCase("r-confirm") ||
               ln[0].equalsIgnoreCase("r-c")) {
            setConfirmRegister( getBooleanFromString(tok));

         } else if (ln[0].equalsIgnoreCase("r-presence") ||
               ln[0].equalsIgnoreCase("r-p")) {
            setConfirmPresence( getBooleanFromString(tok));

         } else { 
            throw new ParseException("Unexpected token [" + ln[0] + "]", 0);
         }
      }
   }

   @Override
   public String toString() {
      StringBuffer retval =  new StringBuffer();
      if( forceRegisterFlag)
      {
         retval.append(FORCE_REGISTER).append(":").append(
            getStringFromBoolean(forceRegister));
         retval.append(CR_LF);
      }
         
      if( confirmRegisterFlag)
      {
         retval.append(CONFIRM_REGISTER).append(":").append(
            getStringFromBoolean(confirmRegister));
         retval.append(CR_LF);
      }
         
      if( confirmPresenceFlag)
      {
         retval.append(CONFIRM_PRESENCE).append(":").append(
            getStringFromBoolean(confirmPresence));
         retval.append(CR_LF);
      }
      return retval.toString();
   }

   /**
    * @param forceRegister
    *            The forceRegister to set.
    */
   public void setForceRegister(boolean forceRegister) {
      this.forceRegister = forceRegister;
      this.forceRegisterFlag = true;
   }

   /**
    * @return Returns the forceRegister.
    */
   public boolean isForceRegister() {
      return forceRegister;
   }
   public void setForceRegisterFlag(boolean bflag) {
      this.forceRegisterFlag = bflag;
   }

   /**
    * @param confirmRegister
    *            The confirmRegister to set.
    */
   public void setConfirmRegister(boolean confirmRegister) {
      this.confirmRegister = confirmRegister;
      this.confirmRegisterFlag = true;
   }

   /**
    * @return Returns the confirmRegister.
    */
   public boolean isConfirmRegister() {
      return confirmRegister;
   }
   public void setConfirmRegisterFlag(boolean bflag) {
      this.confirmRegisterFlag = bflag;
   }

   public void setConfirmPresence (boolean confirmPresence) {
      this.confirmPresence = confirmPresence;
      this.confirmPresenceFlag = true;
   }

   public boolean isConfirmPresence() {      
      return this.confirmPresence;
   }
   public void setConfirmPresenceFlag (boolean bflag) {
      this.confirmPresenceFlag = bflag;
   }

   @Override
   public boolean equals(Object that) {
      if (!(that instanceof RegisterParam)) {
         logger.debug("class mismatch " + that.getClass());
         return false;
      }
      RegisterParam other = (RegisterParam) that;
      return other.confirmRegister == this.confirmRegister &&
             other.forceRegister == this.forceRegister &&
             other.confirmPresence == this.confirmPresence;
   }

   public boolean isDefault() {
      return this.equals(new RegisterParam());
   }
}
