//
package gov.nist.p25.issi.constants;

/**
 * IvsDelays Constants
 */
public class IvsDelaysConstants {

   public static final String IVS_PERFORMANCE_RECOMMENDATIONS = "IVS Performance Recommendations";
   public static final String IVS_PERFORMANCE_PARAMETERS_SETUP = "IVS Performance Parameters Setup";

   public static final String SPEC_MEASUREMENTS_FILE = "xml/spec-ivsdelays.xml";
   public static final String USER_MEASUREMENTS_FILE = "xml/user-ivsdelays.xml";

   public static final String TAG_TYPE_CSSI = "CSSI";
   public static final String TAG_TYPE_ISSI = "ISSI";

   public static final String TAG_SETUP_IPDELAY = "Setup IP-Delays";
   public static final String TAG_SETUP_GCSD = "Setup GCSD";
   public static final String TAG_SETUP_GMTD = "Setup GMTD";
   public static final String TAG_SETUP_UCSD = "Setup UCSD";
   public static final String TAG_SETUP_UMTD = "Setup UMTD";

   // NOTE: this must match the xml/xxx-ivsdelays.xml 
   public static final String TAG_IVS_GCSD = "ivs-gcsd";
   public static final String TAG_IVS_GMTD = "ivs-gmtd";
   public static final String TAG_IVS_UCSD = "ivs-ucsd";
   public static final String TAG_IVS_UMTD = "ivs-umtd";

   // attribute keys
   public static final String KEY_TITLE = "title";
   public static final String KEY_DIAGRAM = "diagram";

   // calculated parameters
   public static final String KEY_T_GRANT_CALLING_CAI = "Tgrant_CallingCAI";
   public static final String KEY_T_GRANT_CALLED_CAI = "Tgrant_CalledCAI";
   public static final String KEY_T_GRANT_CALLING_ISSI = "Tgrant_CallingISSI";
   public static final String KEY_T_GRANT_CALLED_ISSI = "Tgrant_CalledISSI";

   public static final String KEY_TG_AUDIO_DELAY_CAI = "Tg_AudioDelay_CAI";
   public static final String KEY_TG_AUDIO_DELAY_ISSI = "Tg_AudioDelay_ISSI";
   public static final String KEY_TG_AUDIO_DELAY1_CAI = "Tg_AudioDelay1_CAI";
   public static final String KEY_TG_AUDIO_DELAY1_ISSI = "Tg_AudioDelay1_ISSI";
   public static final String KEY_TG_AUDIO_DELAY2_CAI = "Tg_AudioDelay2_CAI";
   public static final String KEY_TG_AUDIO_DELAY2_ISSI = "Tg_AudioDelay2_ISSI";

   public static final String KEY_TU_GRANT_CALLING_CAI = "Tugrant_CallingCAI";
   public static final String KEY_TU_GRANT_CALLED_CAI = "Tugrant_CalledCAI";
   public static final String KEY_TU_GRANT_CALLING_ISSI = "Tugrant_CallingISSI";
   public static final String KEY_TU_GRANT_CALLED_ISSI = "Tugrant_CalledISSI";

   public static final String KEY_TU_AUDIO_DELAY1_CAI = "Tu_AudioDelay1_CAI";
   public static final String KEY_TU_AUDIO_DELAY1_ISSI = "Tu_AudioDelay1_ISSI";
   public static final String KEY_TU_AUDIO_DELAY2_CAI = "Tu_AudioDelay2_CAI";
   public static final String KEY_TU_AUDIO_DELAY2_ISSI = "Tu_AudioDelay2_ISSI";


   public static String getUserIvsDelaysFile(String perfType, boolean isSrcConsole, 
      boolean isDstConsole, String xcmd)
   {
      if(TAG_SETUP_IPDELAY.equals(xcmd)) {
         // ISSI and CSSI share the same IP-delays
	 return USER_MEASUREMENTS_FILE;
      }

      //---------------
      String xmlFile = "unknown.xml";
      String prefix = "xml/" + perfType.toLowerCase();
      if(TAG_TYPE_CSSI.equals(perfType)) {
         // xml/cssi-srcconsole
	 // xml/cssi-dstconsole
	 if(isSrcConsole) prefix += "-srcconsole";
	 if(isDstConsole) prefix += "-dstconsole";
      }

      if(TAG_SETUP_GCSD.equals(xcmd)) xmlFile="user-ivs-gcsd.xml";
      if(TAG_SETUP_GMTD.equals(xcmd)) xmlFile="user-ivs-gmtd.xml";
      if(TAG_SETUP_UCSD.equals(xcmd)) xmlFile="user-ivs-ucsd.xml";
      if(TAG_SETUP_UMTD.equals(xcmd)) xmlFile="user-ivs-umtd.xml";

      xmlFile = prefix +"-" +xmlFile;
      System.out.println("getUserIvsDelaysFile: xmlFile="+xmlFile);
      return xmlFile;
   }
}
