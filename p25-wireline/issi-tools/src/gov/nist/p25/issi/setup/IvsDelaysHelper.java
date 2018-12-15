//
package gov.nist.p25.issi.setup;

import java.util.ArrayList;
import java.util.List;

import gov.nist.p25.issi.constants.IvsDelaysConstants;
import gov.nist.p25.issi.message.XmlIvsDelays;
import gov.nist.p25.issi.setup.IvsDelaysSetup;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsInput;


/**
 * IvsDelaysSetup panel.
 */
public class IvsDelaysHelper 
{
   public static void showln(String s) { System.out.println(s); }

   // CSSI - source and destination console
   //private static boolean isSrcConsole = true;
   //private static boolean isDstConsole = false;

   //-------------------------------------------------------------------------
   public static List<IvsDelaysSetup> getIvsDelaysSetupList(XmlIvsDelays xmldoc, String type)
   {
      List<IvsDelaysSetup> delaysList = new ArrayList<IvsDelaysSetup>();

      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsDelaysSetup delaysSetup = null;

      if( IvsDelaysConstants.TAG_IVS_GCSD.equals(type)) {
         IvsGcsd ivsGcsd = ivsDelays.getIvsGcsd();
         if( ivsGcsd != null) 
         for(gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd.Parameter gcsdParam:
            ivsGcsd.getParameterList()) {
            String key = gcsdParam.getKey();
            int value = gcsdParam.getValue();
            if( gcsdParam.isSetMaximum()) {
               int max = gcsdParam.getMaximum();
               delaysSetup = new IvsDelaysSetup( key, value, max);
            }
            else {
               delaysSetup = new IvsDelaysSetup( key, value);
            }
            // pass along the calculated
            if( gcsdParam.isSetCalculated()) {
               delaysSetup.setCalculated( gcsdParam.getCalculated());
       }
            delaysList.add( delaysSetup);
         }
      }
      else if( IvsDelaysConstants.TAG_IVS_GMTD.equals(type)) {
         IvsGmtd ivsGmtd = ivsDelays.getIvsGmtd();
         if( ivsGmtd != null) 
         for(gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd.Parameter gmtdParam:
            ivsGmtd.getParameterList()) {
            String key = gmtdParam.getKey();
            int value = gmtdParam.getValue();
            if( gmtdParam.isSetMaximum()) {
               int max = gmtdParam.getMaximum();
               delaysSetup = new IvsDelaysSetup( key, value, max);
            }
            else {
               delaysSetup = new IvsDelaysSetup( key, value);
            }
            // pass along the calculated
            if( gmtdParam.isSetCalculated()) {
               delaysSetup.setCalculated( gmtdParam.getCalculated());
       }
            delaysList.add( delaysSetup);
         }
      }
      else if( IvsDelaysConstants.TAG_IVS_UCSD.equals(type)) {
         IvsUcsd ivsUcsd = ivsDelays.getIvsUcsd();
         if( ivsUcsd != null) 
         for(gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd.Parameter ucsdParam:
            ivsUcsd.getParameterList()) {
            String key = ucsdParam.getKey();
            int value = ucsdParam.getValue();
            if( ucsdParam.isSetMaximum()) {
               int max = ucsdParam.getMaximum();
               delaysSetup = new IvsDelaysSetup( key, value, max);
            }
            else {
               delaysSetup = new IvsDelaysSetup( key, value);
            }
            // pass along the calculated
            if( ucsdParam.isSetCalculated()) {
               delaysSetup.setCalculated( ucsdParam.getCalculated());
       }
            delaysList.add( delaysSetup);
         }
      }
      else if( IvsDelaysConstants.TAG_IVS_UMTD.equals(type)) {
         IvsUmtd ivsUmtd = ivsDelays.getIvsUmtd();
         if( ivsUmtd != null) 
         for(gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd.Parameter umtdParam:
            ivsUmtd.getParameterList()) {
            String key = umtdParam.getKey();
            int value = umtdParam.getValue();
            if( umtdParam.isSetMaximum()) {
               int max = umtdParam.getMaximum();
               delaysSetup = new IvsDelaysSetup( key, value, max);
            }
            else {
               delaysSetup = new IvsDelaysSetup( key, value);
            }
            // pass along the calculated
            if( umtdParam.isSetCalculated()) {
               delaysSetup.setCalculated( umtdParam.getCalculated());
       }
            delaysList.add( delaysSetup);
         }
      }
      return delaysList;
   }

   //-------------------------------------------------------------------------
   public static void setGCSDComposite(String perfType, boolean isSrcConsole, boolean isDstConsole,
      XmlIvsDelays xmldoc, boolean isConfirmed)
   {
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd.Parameter gcsdParam;

      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsGcsd ivsGcsd = ivsDelays.getIvsGcsd();
      if( ivsGcsd == null) {
         throw new IllegalArgumentException("setGCSComposite: Missing IvsGcsd document.");
      }

      // fetch all tgx values
      int[] tgx = new int[12];
      for(int i=1; i < 12; i++) {
         gcsdParam = findGCSDParameter( ivsGcsd, "Tg"+i);
         tgx[i] = gcsdParam.getValue();
      }

      // fetch all tg_ipx values
      int[] tgipx = new int[8];
      for(int i=1; i < 8; i++) {
         gcsdParam = findGCSDParameter( ivsGcsd, "Tg_ip"+i);
         tgipx[i] = gcsdParam.getValue();
      }

      int tga, tgb, tgmax;
      int[] tgcal = new int[4];
      if( isConfirmed) {
         // confirmed group call
         tga = tgx[3] + tgipx[2] + tgx[5] + tgipx[5] + tgx[6]; 
         tgb = tgx[2] + tgipx[3] + tgx[4] + tgipx[4] + tgx[9]; 
         tgmax = Math.max( tga, tgb);

         // Tgrant_CallingISSI
         tgcal[0] = tgipx[1] + tgmax + tgipx[6];

         // Tgrant_CallingCAI
         tgcal[1] = tgx[1] + tgcal[0] + tgx[10];

         tga = tgx[3] + tgipx[2] + tgx[5] + tgipx[5] + tgx[7]; 
         tgb = tgx[2] + tgipx[3] + tgx[4] + tgipx[4] + tgx[8]; 
         tgmax = Math.max( tga, tgb);

         // Tgrant_CalledISSI
         tgcal[2] = tgipx[1] + tgmax + tgipx[7];

         // Tgrant_CalledCAI
         tgcal[3] = tgx[1] + tgcal[2] + tgx[11];

      }
      else {
         // unconfirmed group call
         tga = tgx[3] + tgipx[2] + tgx[5] + tgipx[5] + tgx[6]; 

         // Tgrant_CallingISSI
         tgcal[0] = tgipx[1] + tga + tgipx[6];

         // Tgrant_CallingCAI
         tgcal[1] = tgx[1] + tgcal[0] + tgx[10];

         tga = tgx[3] + tgipx[2] + tgx[5] + tgipx[5] + tgx[7]; 
         tgb = tgx[2] + tgipx[3] + tgx[4] + tgipx[4] + tgx[8]; 
         tgmax = Math.max( tga, tgb);

         // Tgrant_CalledISSI
         tgcal[2] = tgipx[1] + tgmax + tgipx[7];

         // Tgrant_CalledCAI
         tgcal[3] = tgx[1] + tgcal[2] + tgx[11];
      }
      //showln("GCSD: tgcal="+tgcal[0]+" "+tgcal[1]+" "+tgcal[2]+" "+tgcal[3]);

      // transfer calculated values
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
         if( !isSrcConsole) 
            setGCSDParameter( ivsGcsd, IvsDelaysConstants.KEY_T_GRANT_CALLING_CAI, tgcal[1], -1, true);
         if( (!isSrcConsole || isDstConsole)) 
            setGCSDParameter( ivsGcsd, IvsDelaysConstants.KEY_T_GRANT_CALLED_CAI, tgcal[3], -1, true);
      }
      else {
         setGCSDParameter( ivsGcsd, IvsDelaysConstants.KEY_T_GRANT_CALLING_CAI, tgcal[1], -1, true);
         setGCSDParameter( ivsGcsd, IvsDelaysConstants.KEY_T_GRANT_CALLED_CAI, tgcal[3], -1, true);
      }
      setGCSDParameter( ivsGcsd, IvsDelaysConstants.KEY_T_GRANT_CALLING_ISSI, tgcal[0], -1, true);
      setGCSDParameter( ivsGcsd, IvsDelaysConstants.KEY_T_GRANT_CALLED_ISSI, tgcal[2], -1, true);
   }

   //--------------------
   public static void setGMTDComposite(String perfType, boolean isSrcConsole, boolean isDstConsole,
      XmlIvsDelays xmldoc)
   {
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd.Parameter gmtdParam;

      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsGmtd ivsGmtd = ivsDelays.getIvsGmtd();
      if( ivsGmtd == null) {
         throw new IllegalArgumentException("setGMTDComposite: Missing IvsGmtd document.");
      }

      // fetch all tgx values
      int[] tgx = new int[4];
      for(int i=1; i < 4; i++) {
         gmtdParam = findGMTDParameter( ivsGmtd, "Tg_ad"+i);
         tgx[i] = gmtdParam.getValue();
      }

      // fetch all tg_ipx values
      int[] tgipx = new int[3];
      for(int i=1; i < 3; i++) {
         gmtdParam = findGMTDParameter( ivsGmtd, "Tg_ipad"+i);
         tgipx[i] = gmtdParam.getValue();
      }

      int[] tgcal = new int[2];
      // Tg_AudiaDelay_ISSI
      tgcal[0] = tgipx[1] + tgx[2] + tgipx[2];

      // Tg_AudioDelay_CAI
      tgcal[1] = tgx[1] + tgcal[0] + tgx[3]; 

      //showln("GMTD: tgcal="+tgcal[0]+" "+tgcal[1]);

      // transfer calculated values
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
         if( (!isSrcConsole || isDstConsole)) 
            setGMTDParameter( ivsGmtd, IvsDelaysConstants.KEY_TG_AUDIO_DELAY_CAI, tgcal[1], -1, true);
      } else {
         setGMTDParameter( ivsGmtd, IvsDelaysConstants.KEY_TG_AUDIO_DELAY_CAI, tgcal[1], -1, true);
      }
      setGMTDParameter( ivsGmtd, IvsDelaysConstants.KEY_TG_AUDIO_DELAY_ISSI, tgcal[0], -1, true);
   }

   //--------------------
   public static void setUCSDComposite(String perfType, boolean isSrcConsole, boolean isDstConsole,
      XmlIvsDelays xmldoc, boolean isDirectCall)
   {
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd.Parameter ucsdParam;

      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsUcsd ivsUcsd = ivsDelays.getIvsUcsd();
      if( ivsUcsd == null) {
         throw new IllegalArgumentException("setUCSComposite: Missing IvsUcsd document.");
      }

      // fetch all tgx values
      int[] tgx = new int[14];
      for(int i=1; i < 14; i++) {
         ucsdParam = findUCSDParameter( ivsUcsd, "Tu"+i);
         tgx[i] = ucsdParam.getValue();
      }

      // fetch all tg_ipx values
      int[] tgipx = new int[10];
      for(int i=1; i < 10; i++) {
         ucsdParam = findUCSDParameter( ivsUcsd, "Tu_ip"+i);
         tgipx[i] = ucsdParam.getValue();
      }

      int tgx_ac = 0;
      int[] tgcal = new int[4];
      if( isDirectCall) {
         // direct call
         // Tugrant_CallingISSI
         tgcal[0] = tgipx[1] + tgx[2] + tgipx[2] + tgx[3] + tgipx[3] + tgx[6] +
                    tgipx[4] + tgx[7] + tgipx[5] + tgx[8] + tgipx[6];

         // Tugrant_CallingCAI
         tgcal[1] = tgx[1] + tgcal[0] + tgx[9];

         // Tugrant_CalledISSI
         tgcal[2] = tgcal[0] + tgx[10] + tgipx[7] + tgx[11] + tgipx[8] + tgx[12] + tgipx[9];

         // Tugrant_CalledCAI
         tgcal[3] = tgx[1] + tgcal[2] + tgx[13];

      }
      else {
         // availability check
         // Tugrant_CallingISSI
         tgcal[0] = tgipx[1] + tgx[2] + tgipx[2] + tgx[3] + tgipx[3] + 
                    tgx[4] + tgx_ac + tgx[5] +
                    tgipx[4] + tgx[7] + tgipx[5] + tgx[8] + tgipx[6];

         // Tugrant_CallingCAI
         tgcal[1] = tgx[1] + tgcal[0] + tgx[9];

         // Tugrant_CalledISSI
         tgcal[2] = tgcal[0] + tgx[10] + tgipx[7] + tgx[11] + tgipx[8] + tgx[12] + tgipx[9];

         // Tugrant_CalledCAI
         tgcal[3] = tgx[1] + tgcal[2] + tgx[13];
      }
      //showln("UCSD: tgcal="+tgcal[0]+" "+tgcal[1]+" "+tgcal[2]+" "+tgcal[3]);

      // transfer calculated values
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {

         if(!isSrcConsole)
            setUCSDParameter(ivsUcsd, IvsDelaysConstants.KEY_TU_GRANT_CALLING_CAI, tgcal[1], -1, true);
         if(!(isSrcConsole || isDstConsole))
            setUCSDParameter(ivsUcsd, IvsDelaysConstants.KEY_TU_GRANT_CALLED_CAI, tgcal[3], -1, true);
      }
      else {
         setUCSDParameter(ivsUcsd, IvsDelaysConstants.KEY_TU_GRANT_CALLING_CAI, tgcal[1], -1, true);
         setUCSDParameter(ivsUcsd, IvsDelaysConstants.KEY_TU_GRANT_CALLED_CAI, tgcal[3], -1, true);
      }
      setUCSDParameter(ivsUcsd, IvsDelaysConstants.KEY_TU_GRANT_CALLING_ISSI, tgcal[0], 1000, true);
      setUCSDParameter(ivsUcsd, IvsDelaysConstants.KEY_TU_GRANT_CALLED_ISSI, tgcal[2], -1, true);
   }

   //--------------------
   public static void setUMTDComposite(String perfType, boolean isSrcConsole, boolean isDstConsole, 
      XmlIvsDelays xmldoc)
   {
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd.Parameter umtdParam;

      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsUmtd ivsUmtd = ivsDelays.getIvsUmtd();
      if( ivsUmtd == null) {
         throw new IllegalArgumentException("setUMTDComposite: Missing IvsUmtd document.");
      }

      // fetch all tgx values
      int[] tgx = new int[9];
      for(int i=1; i < 9; i++) {
         umtdParam = findUMTDParameter( ivsUmtd, "Tu_ad"+i);
         tgx[i] = umtdParam.getValue();
      }

      // fetch all tg_ipx values
      int[] tgipx = new int[7];
      for(int i=1; i < 7; i++) {
         umtdParam = findUMTDParameter( ivsUmtd, "Tu_ipad"+i);
         tgipx[i] = umtdParam.getValue();
      }

      int[] tgcal = new int[4];

      // Tu_AudiaDelay1_ISSI
      tgcal[0] = tgipx[1] + tgx[2] + tgipx[2] + tgx[3] + tgipx[3];

      // Tu_AudioDelay1_CAI
      tgcal[1] = tgx[1] + tgcal[0] + tgx[4]; 

      // Tu_AudiaDelay2_ISSI
      tgcal[2] = tgipx[4] + tgx[6] + tgipx[5] + tgx[7] + tgipx[6];

      // Tu_AudioDelay2_CAI
      tgcal[3] = tgx[5] + tgcal[0] + tgx[8]; 

      //showln("UMTD: tgcal="+tgcal[0]+" "+tgcal[1]+" "+tgcal[2]+" "+tgcal[3]);

      // transfer calculated values
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
         if(!(isSrcConsole || isDstConsole)) {
            setUMTDParameter(ivsUmtd, IvsDelaysConstants.KEY_TU_AUDIO_DELAY1_CAI, tgcal[1], -1, true);
            setUMTDParameter(ivsUmtd, IvsDelaysConstants.KEY_TU_AUDIO_DELAY2_CAI, tgcal[3], -1, true);
         }

      } else {
         setUMTDParameter(ivsUmtd, IvsDelaysConstants.KEY_TU_AUDIO_DELAY1_CAI, tgcal[1], -1, true);
         setUMTDParameter(ivsUmtd, IvsDelaysConstants.KEY_TU_AUDIO_DELAY2_CAI, tgcal[3], -1, true);
      }
      setUMTDParameter(ivsUmtd, IvsDelaysConstants.KEY_TU_AUDIO_DELAY1_ISSI, tgcal[0], -1, true);
      setUMTDParameter(ivsUmtd, IvsDelaysConstants.KEY_TU_AUDIO_DELAY2_ISSI, tgcal[2], -1, true);
   }

   //-------------------------------------------------------------------------
   public static void reconcile(XmlIvsDelays xmldoc, List<IvsDelaysSetup> delaysList, String type)
   {
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd.Parameter gcsdParam;
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd.Parameter gmtdParam;
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd.Parameter ucsdParam;
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd.Parameter umtdParam;

      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      if( IvsDelaysConstants.TAG_IVS_GCSD.equals(type)) {
         IvsGcsd ivsGcsd = ivsDelays.getIvsGcsd();
         if( ivsGcsd != null) 
         for( IvsDelaysSetup delaysSetup: delaysList) {
            gcsdParam = findGCSDParameter( ivsGcsd, delaysSetup.getParameterName());
            if( gcsdParam != null) {
               gcsdParam.setValue( delaysSetup.getAverageTime());
               if( delaysSetup.getCellSelectionEnabled()) {
                  gcsdParam.setMaximum( delaysSetup.getMaximumTime());
               }
            }
         }
      }
      else if( IvsDelaysConstants.TAG_IVS_GMTD.equals(type)) {
         IvsGmtd ivsGmtd = ivsDelays.getIvsGmtd();
         if( ivsGmtd != null) 
         for( IvsDelaysSetup delaysSetup: delaysList) {
            gmtdParam = findGMTDParameter( ivsGmtd, delaysSetup.getParameterName());
            if( gmtdParam != null) {
               gmtdParam.setValue( delaysSetup.getAverageTime());
               if( delaysSetup.getCellSelectionEnabled()) {
                  gmtdParam.setMaximum( delaysSetup.getMaximumTime());
               }
            }
         }
      }
      else if( IvsDelaysConstants.TAG_IVS_UCSD.equals(type)) {
         IvsUcsd ivsUcsd = ivsDelays.getIvsUcsd();
         if( ivsUcsd != null) 
         for( IvsDelaysSetup delaysSetup: delaysList) {
            ucsdParam = findUCSDParameter( ivsUcsd, delaysSetup.getParameterName());
            if( ucsdParam != null) {
               ucsdParam.setValue( delaysSetup.getAverageTime());
               if( delaysSetup.getCellSelectionEnabled()) {
                  ucsdParam.setMaximum( delaysSetup.getMaximumTime());
               }
            }
         }
      }
      else if( IvsDelaysConstants.TAG_IVS_UMTD.equals(type)) {
         IvsUmtd ivsUmtd = ivsDelays.getIvsUmtd();
         if( ivsUmtd != null) 
         for( IvsDelaysSetup delaysSetup: delaysList) {
            umtdParam = findUMTDParameter( ivsUmtd, delaysSetup.getParameterName());
            if( umtdParam != null) {
               umtdParam.setValue( delaysSetup.getAverageTime());
               if( delaysSetup.getCellSelectionEnabled()) {
                  umtdParam.setMaximum( delaysSetup.getMaximumTime());
               }
            }
         }
      }
   }

   //-------------------------------------------------------------------------
   public static gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd.Parameter 
      findGCSDParameter(IvsGcsd ivsGcsd, String parameterName)
   {
      for(gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd.Parameter gcsdParam:
         ivsGcsd.getParameterList()) {
         String key = gcsdParam.getKey();
         if( key != null && key.equals(parameterName)) {
            //showln("findGCSD: key="+key+"  parm="+parameterName);
            return gcsdParam;
         }
      }
      throw new IllegalArgumentException("IvsGcsd: Missing parameter "+parameterName);
   }
   //---------------------
   public static void setGCSDParameter(IvsGcsd ivsGcsd, String parameterName, int value) {

      setGCSDParameter(ivsGcsd, parameterName, value, -1, false);
   }
   public static void setGCSDParameter(IvsGcsd ivsGcsd, String parameterName, int value,
      int maxValue, boolean marked) {
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd.Parameter gcsdParam;
      try {
         gcsdParam = findGCSDParameter(ivsGcsd, parameterName);
         gcsdParam.setValue( value);
         if( maxValue != -1) gcsdParam.setMaximum( maxValue);
         gcsdParam.setCalculated( marked);
      }
      catch(Exception ex) {
         gcsdParam = ivsGcsd.addNewParameter(); 
         gcsdParam.setKey( parameterName);
         gcsdParam.setValue( value);
         if( maxValue != -1) gcsdParam.setMaximum( maxValue);
         gcsdParam.setCalculated( marked);
      }
   }

   //------------------
   public static gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd.Parameter
      findGMTDParameter( IvsGmtd ivsGmtd, String parameterName)
   {
      for(gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd.Parameter gmtdParam:
         ivsGmtd.getParameterList()) {
         String key = gmtdParam.getKey();
         if( key != null && key.equals(parameterName)) {
            //showln("findGMTD: key="+key+"  parm="+parameterName);
            return gmtdParam;
         }
      }
      throw new IllegalArgumentException("IvsGmtd: Missing parameter "+parameterName);
   }
   public static void setGMTDParameter(IvsGmtd ivsGmtd, String parameterName, int value) {
      setGMTDParameter(ivsGmtd, parameterName, value, -1, false);
   }
   public static void setGMTDParameter(IvsGmtd ivsGmtd, String parameterName, int value,
      int maxValue, boolean marked) {
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd.Parameter gmtdParam;
      try {
         gmtdParam = findGMTDParameter(ivsGmtd, parameterName);
         gmtdParam.setValue( value);
         if( maxValue != -1) gmtdParam.setMaximum( maxValue);
         gmtdParam.setCalculated( marked);
      } 
      catch(Exception ex) {
         gmtdParam = ivsGmtd.addNewParameter(); 
         gmtdParam.setKey( parameterName);
         gmtdParam.setValue( value);
         if( maxValue != -1) gmtdParam.setMaximum( maxValue);
         gmtdParam.setCalculated( marked);
      }
   }

   //------------------
   public static gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd.Parameter
      findUCSDParameter( IvsUcsd ivsUcsd, String parameterName)
   {
      for(gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd.Parameter ucsdParam:
         ivsUcsd.getParameterList()) {
         String key = ucsdParam.getKey();
         if( key != null && key.equals(parameterName)) {
            //showln("findUCSD: key="+key+"  parm="+parameterName);
            return ucsdParam;
         }
      }
      throw new IllegalArgumentException("IvsUcsd: Missing parameter "+parameterName);
   }
   public static void setUCSDParameter(IvsUcsd ivsUcsd, String parameterName, int value) {
      setUCSDParameter(ivsUcsd, parameterName, value, -1, false);
   }
   public static void setUCSDParameter(IvsUcsd ivsUcsd, String parameterName, int value,
      int maxValue, boolean marked) {
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd.Parameter ucsdParam;
      try {
         ucsdParam = findUCSDParameter(ivsUcsd, parameterName);
         ucsdParam.setValue( value);
         if( maxValue != -1) ucsdParam.setMaximum( maxValue);
         ucsdParam.setCalculated( marked);
      } 
      catch(Exception ex) {
         ucsdParam = ivsUcsd.addNewParameter(); 
         ucsdParam.setKey( parameterName);
         ucsdParam.setValue( value);
         if( maxValue != -1) ucsdParam.setMaximum( maxValue);
         ucsdParam.setCalculated( marked);
      }
   }

   //------------------
   public static gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd.Parameter
      findUMTDParameter( IvsUmtd ivsUmtd, String parameterName)
   {
      for(gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd.Parameter umtdParam:
         ivsUmtd.getParameterList()) {
         String key = umtdParam.getKey();
         if( key != null && key.equals(parameterName)) {
            //showln("findUMTD: key="+key+"  parm="+parameterName);
            return umtdParam;
         }
      }
      throw new IllegalArgumentException("IvsUmtd: Missing parameter "+parameterName);
   }
   public static void setUMTDParameter(IvsUmtd ivsUmtd, String parameterName, int value) {
      setUMTDParameter(ivsUmtd, parameterName, value, -1, false);
   }
   public static void setUMTDParameter(IvsUmtd ivsUmtd, String parameterName, int value, 
      int maxValue, boolean marked) {
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd.Parameter umtdParam;
      try {
         umtdParam = findUMTDParameter(ivsUmtd, parameterName);
         umtdParam.setValue( value);
         if( maxValue != -1) umtdParam.setMaximum( maxValue);
         umtdParam.setCalculated( marked);
      }
      catch(Exception ex) {
         umtdParam = ivsUmtd.addNewParameter(); 
         umtdParam.setKey( parameterName);
         umtdParam.setValue( value);
         if( maxValue != -1) umtdParam.setMaximum( maxValue);
         umtdParam.setCalculated( marked);
      }
   }

   //-------------------------------------------------------------------------
   public static String getIvsDelaysData(XmlIvsDelays xmldoc, String type, String attribute)
   {
      IvsDelays ivsDelays = xmldoc.getIvsDelays();

      String header = "";
      if( IvsDelaysConstants.TAG_IVS_GCSD.equals(type)) {
         IvsGcsd ivsGcsd = ivsDelays.getIvsGcsd();
         if( ivsGcsd != null)
         if( IvsDelaysConstants.KEY_TITLE.equals(attribute)) {
            header = ivsGcsd.getTitle();
         }
         else if( IvsDelaysConstants.KEY_DIAGRAM.equals(attribute)) {
            header = ivsGcsd.getDiagram();
         }
      }
      else if( IvsDelaysConstants.TAG_IVS_GMTD.equals(type)) {
         IvsGmtd ivsGmtd = ivsDelays.getIvsGmtd();
         if( ivsGmtd != null)
         if( IvsDelaysConstants.KEY_TITLE.equals(attribute)) {
            header = ivsGmtd.getTitle();
         }
         else if( IvsDelaysConstants.KEY_DIAGRAM.equals(attribute)) {
            header = ivsGmtd.getDiagram();
         }
      }
      else if( IvsDelaysConstants.TAG_IVS_UCSD.equals(type)) {
         IvsUcsd ivsUcsd = ivsDelays.getIvsUcsd();
         if( ivsUcsd != null)
         if( IvsDelaysConstants.KEY_TITLE.equals(attribute)) {
            header = ivsUcsd.getTitle();
         }
         else if( IvsDelaysConstants.KEY_DIAGRAM.equals(attribute)) {
            header = ivsUcsd.getDiagram();
         }
      }
      else if( IvsDelaysConstants.TAG_IVS_UMTD.equals(type)) {
         IvsUmtd ivsUmtd = ivsDelays.getIvsUmtd();
         if( ivsUmtd != null)
         if( IvsDelaysConstants.KEY_TITLE.equals(attribute)) {
            header = ivsUmtd.getTitle();
         }
         else if( IvsDelaysConstants.KEY_DIAGRAM.equals(attribute)) {
            header = ivsUmtd.getDiagram();
         }
      }
      return header;
   }

   //-------------------------------------------------------------------------
   //-------------------------------------------------------------------------
   public static void setIvsInput(XmlIvsDelays xmldoc, String title, List<String> fileList)
   {
      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsInput ivsInput = ivsDelays.getIvsInput();
      if( ivsInput == null)
         ivsInput = ivsDelays.addNewIvsInput();
      ivsInput.setTitle( title);

      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsInput.Datafile datafile;
      for( String filename: fileList) {
         datafile = ivsInput.addNewDatafile();
         datafile.setName( filename);
      }
   }

   public static List<String> getIvsInputDatafile(XmlIvsDelays xmldoc)
   {
      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsInput ivsInput = ivsDelays.getIvsInput();
      List<String> fileList = new ArrayList<String>();

      if( ivsInput != null) {
         for( gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsInput.Datafile datafile:  ivsInput.getDatafileList()) {
            fileList.add( datafile.getName());
         }
      }
      return fileList;
   }
   //-------------------------------------------------------------------------
   /***
   public static void setIvsStatusComment(XmlIvsDelays xmldoc, String title, String comment)
   {
      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsStatus ivsStatus = ivsDelays.getIvsStatus();
      if( ivsStatus == null)
         ivsStatus = ivsDelays.addNewIvsStatus();
      ivsStatus.setTitle( title);
      ivsStatus.setComment( comment);
   }

   public static String getIvsStatusComment(XmlIvsDelays xmldoc)
   {
      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsStatus ivsStatus = ivsDelays.getIvsStatus();
      String comment = "";
      if( ivsStatus != null)
         comment = ivsStatus.getComment();
      return comment;
   }
   public static String getIvsStatusTitle(XmlIvsDelays xmldoc)
   {
      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsStatus ivsStatus = ivsDelays.getIvsStatus();
      String title = "";
      if( ivsStatus != null)
         title = ivsStatus.getTitle();
      return title;
   }
    ***/
   //-------------------------------------------------------------------------
}
