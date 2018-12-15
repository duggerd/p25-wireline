//
package gov.nist.p25.issi.constants;

/**
 * These are ISSI specific constants.
 */
public class ISSIConstants {

   // place the constants by type and in order
   public static final int RTP_TIMEOUT = 4000;
   public static final int EXPIRES_TIME_81400 = 81400;
   public static final int EXPIRES_TIME_86400 = 86400;

   public static final String BODY_BOUNDARY = "P25 ISSI body boundary";
   public static final String CALLING_HOME_NAME = "TIA-P25-U2Uorig";
   public static final String CALLED_HOME_NAME = "TIA-P25-U2Udest";

   // The separator for content in a multi part mime message.
   public static final String CONTENT_BOUNDARY = "--" + BODY_BOUNDARY ;
   public static final String P25DR = "p25dr";
   public static final String P25_GROUP_CALL = "TIA-P25-Groupcall";
   public static final String P25_U2U_CALL = "TIA-P25-U2UCall";

   // Content-Type: type and subType
   public static final String APPLICATION = "application";
   public static final String SDP = "sdp";
   public static final String X_TIA_P25_ISSI = "x-tia-p25-issi";

   public static final String TAG_MIXED = "mixed";
   public static final String TAG_MULTIPART = "multipart";
   public static final String TAG_BOUNDARY = "boundary";
   public static final String TIA_P25_SU = "TIA-P25-SU";
   public static final String TIA_P25_SG = "TIA-P25-SG";

   public static final String USER = "user";
   public static final String U2UDEST = "TIA-P25-U2UDest";
   public static final String U2UORIG = "TIA-P25-U2UOrig";

   // Some standard warning messages.
   public static final String WARN_TEXT_MISSING_REQUIRED_PARAMETER = "Missing a required header parameter";
   public static final String WARN_TEXT_MISSING_REQUIRED_SDP_ANNOUNCE = "Missing SDP announce in body ";
}
