package gov.nist.p25.issi.utils;

import javax.sip.message.Response;

/**
 * Encodes the information in Table 21 - P25 Codition Code Summary, TIA-102.BACA, May 9, 2008.
 * 
 */
public class WarningCodes {

   // 0x00 Success

   // 606
   public static final WarningCodes NO_RTP_RESOURCE = new WarningCodes(
         0x0A,
         "No RTP Resource", 
         Response.SESSION_NOT_ACCEPTABLE,
         "U2U: The Unit to Unit Call MAY NOT be set-up due to a lack of RTP resource");

   public static final WarningCodes NO_RF_RESOURCE = new WarningCodes(
         0x0B,
         "No RF Resource",
         Response.SESSION_NOT_ACCEPTABLE,
         "U2U: The Unit to Unit Call MAY NOT be set up due to a lack of RF resource");

   public static final WarningCodes FORBIDDEN_CALLING_SU_RADIO_INHIBITED = new WarningCodes(
         0x13, 
         "Calling SU radio inhibited", 
         Response.FORBIDDEN,
         "U2U Call: the Calling Home RFSS detects that the Calling SU is radio inhibited");

   public static final WarningCodes FORBIDDEN_CALLING_SU_NOT_REGISTERED = new WarningCodes(
         0x14, 
         "Calling SU not registered", 
         Response.FORBIDDEN,
         "U2U Call: The Calling Home RFSS detects that the Calling "
               + "Serving RFSS is not registered for the Calling SUID");

   public static final WarningCodes FORBIDDEN_CALLED_SU_NOT_REGISTERED = new WarningCodes(
         0x15,
         "Called SU not registered",
         Response.FORBIDDEN,
         "U2U Call: The Called "
               + "Home RFSS detects that the Called SUID is not registered");

   public static final WarningCodes FORBIDDEN_CALLING_SU_NOT_AUTHORIZED = new WarningCodes(
         0x16,
         "Calling SU not authorized",
         Response.FORBIDDEN,
         "U2U Call: The Calling Home "
               + "RFSS detects that the Calling SU is not authorized to make this call");

   public static final WarningCodes FORBIDDEN_CALLED_SU_NOT_AUTHORIZED = new WarningCodes(
         0x17,
         "Called SU not authorized",
         Response.FORBIDDEN,
         "U2U Call: The Called Home "
               + "RFSS detects that the Called SU is not authorized to respond to this call");

   public static final WarningCodes FORBIDDEN_SU_NOT_AUTHORIZED = new WarningCodes(
         0x18, 
         "SU not authorized", 
         Response.FORBIDDEN,
         "Unit registration: The Home RFSS of the SU disallows "
               + "the registration of this unit");

   public static final WarningCodes FORBIDDEN_SG_NOT_REGISTERED = new WarningCodes(
         0x19, 
         "SG not registered", 
         Response.FORBIDDEN,
         "Group Call: SG's Home RFSS detects "
               + "that the Serving RFSS is not registered for the SGID");

   public static final WarningCodes FORBIDDEN_SG_NOT_AUTHORIZED = new WarningCodes(
         0x1A, 
         "SG not authorized", 
         Response.FORBIDDEN,
         "Group Registration: The SG's Home RFSS disallows the registration of this group");

   public static final WarningCodes FORBIDDEN_PRESENCE_REQUIRED = new WarningCodes(
         0x1B,
         "SU Presence Confirmation required", 
         Response.FORBIDDEN,
         "Unit Registration: The Home RFSS of the SU denies the 'SU Presence Not Confirmed' "
	       + "registration because the SU is currently registered at a different serving RFSS");

   public static final WarningCodes FORBIDDEN_REQUESTED_PROCEDURE_NOT_SUPPORTED = new WarningCodes(
         0x1C, 
         "Requested procedure not supported", 
         Response.FORBIDDEN,
         "Any requested procedure");

   public static final WarningCodes FORBIDDEN_CALLED_SU_RADIO_INHIBITED = new WarningCodes(
         0x1D, 
         "Called SU radio inhibited", 
         Response.FORBIDDEN,
         "U2U Call: the Calling Home RFSS detects that the Called SU is radio inhibited");

   public static final WarningCodes NOT_FOUND_UNKNOWN_CALLING_SU = new WarningCodes(
         0x1E, 
         "Unknown Calling SU", 
         Response.NOT_FOUND,
         "U2U Call: the Calling Home RFSS detects the Calling SU is unknown");

   public static final WarningCodes NOT_FOUND_UNKNOWN_CALLED_SU = new WarningCodes(
         0x1F, 
         "Unknown Called SU", 
         Response.NOT_FOUND,
         "U2U Call: the Called Home RFSS detects the Called SU is unknown");

   public static final WarningCodes NOT_FOUND_UNKNOWN_CALLED_HOME_RFSS = new WarningCodes(
         0x20,
         "Unknown Called Home RFSS ID",
         Response.NOT_FOUND,
         "U2U Call: The Calling Home "
               + "RFSS detects that the RFSS ID of the Called Home RFSS is not known");

   public static final WarningCodes NOT_FOUND_UNKNOWN_TARGET_GROUP = new WarningCodes(
         0x21, 
         "Unknown target group", 
         Response.NOT_FOUND,
         "Group Call and group registration: the Home or the "
               + "Serving RFSS detects that the SG is unknown");

   public static final WarningCodes NOT_FOUND_UNKNOWN_SU = new WarningCodes(
         0x22, 
         "Unknown SU", 
         Response.NOT_FOUND,
         "Unit Registration: the Home or the Serving RFSS detects that the SU is unknown");

   public static final WarningCodes NOT_FOUND_CALLED_SUID_NOT_REGISTERED = new WarningCodes(
         0x23, 
         "Called Serving RFSS has not registered Called SUID with Called Home RFSS", 
         Response.NOT_FOUND,
         "U2U Registration: the Called Serving RFSS detects that it has not registered "
	       + "the call SUID with the Called Home RFSS");
   
   public static final WarningCodes DECLINE_COLLIDING_CALL = new WarningCodes(
         0x28,
         "Colliding Call",
         Response.DECLINE,
         "U2U: The Call is rejected because the same call is "
               + "proceeding in the opposite direction Group Call: Home RFSS (resp. Serving RFSS) rejects "
               + "the INVITE request from the serving RFSS (resp. Home RFSS) because it is already "
               + "inviting the Serving RFSS");
   
   // TODO: needs a better description
   public static final WarningCodes BUSY_HERE_CALLED_SU_BUSY = new WarningCodes(
         0x29,
         "Called SU is busy", 
         Response.BUSY_HERE,
         "U2U: The Call is rejected because the Called SU is busy or does not respond");

   public static final WarningCodes BUSY_HERE_CALLED_SU_REFUSES_THE_CALL = new WarningCodes(
         0x2A,
         "Called SU refuses the call", 
         Response.BUSY_HERE,
         "U2U: The Call is rejected because the Called SU has cancelled the call "
	       + "while it was waiting for RF resources availability");


   public static final WarningCodes FORBIDDEN_PROTECTED_MODE_AUDIO_CANNOT_BE_GRANTED = new WarningCodes(
         0x2B,
         "Protected audio call not granted",
         Response.FORBIDDEN,
         "U2U or Group Call: The Called Home or SG Home RFSS detects that " +
	       "a request for a protected audio call cannot be granted");
   
   
   public static final WarningCodes FORBIDDEN_UNPROTECTED_MODE_AUDIO_CANNOT_BE_GRANTED = new WarningCodes(
         0x2C,
         "Unprotected audio call not granted",
         Response.FORBIDDEN,
         "U2U or Group Call: The Called Home or SG Home RFSS detects that " +
	       "a request for an unprotected audio call cannot be granted");


   public static final WarningCodes SESSION_PROGRESS_NO_RF_RESOURCES = new WarningCodes(
         0x50, 
         "RF resource not available", 
         Response.SESSION_PROGRESS,
         "U2U call: the called RFSS has no RF resource");

   public static final WarningCodes SESSION_PROGRESS_QUERY_PROCESSING = new WarningCodes(
         0x55,
         "Query processing in progress",
         Response.SESSION_PROGRESS,
         "Home Query: to inform the Home RFSS that the Serving RFSS " 
	       + "is attempting to fulfill the received query request.");

   public static final WarningCodes NON_ISSI_HEADER = new WarningCodes(
         0x64,
         "Non-ISSI header(s)ignored",
         Response.OK,
         "Any");

   public static final WarningCodes MANUFACTURER_SPECIFIC_BODY_IGNORED = new WarningCodes(
         0x65,
         "Manufacturer specific body ignored",
         Response.OK,
         "Any");

   // 0x70 - 0x7F  Reserved
   // 0xFF Manufacturer's Discretion

   //-------------------------------------------------------------------------------------------
   // attributes
   private int warnCode;
   private int rc;
   private String warnText;
   private String usage;

   // constructor
   WarningCodes(int warnCode, String warnText, int rc, String usage) {
      this.warnCode = warnCode;
      this.rc = rc;
      this.warnText = warnText;
      this.usage = usage;
   }

   public int getWarnCode() {
      return 399;
   }

   public int getRc() {
      return rc;
   }

   public String getWarnText() {
      return "TIA-P25-ISSI-" + Integer.toHexString(warnCode).toUpperCase()
	      + ": " + warnText;
   }

   public String getUsage() {
      return usage;
   }
}
