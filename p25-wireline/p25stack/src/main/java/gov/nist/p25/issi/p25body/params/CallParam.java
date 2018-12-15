//
package gov.nist.p25.issi.p25body.params;

import java.text.ParseException;
import org.apache.log4j.Logger;

import gov.nist.p25.issi.p25payload.ConsoleTransmissionRequestPriority;

/**
 * These attributes are used in conjunction with an SDP announcement. All these
 * parameters are optional. Each parameter is a boolean value represented by 0
 * or 1.
 */
public class CallParam {

   private static Logger logger = Logger.getLogger(CallParam.class);

   /*
    * A flag that records whether the c-resavail flag was explicitly set.
    */
    private boolean resAvailIsSet;
    
   /*
    * This parameter indicates the first transmitter in a call. It is named
    * c-initrans or c-i in its compact form. Two values are allowed: 0 if the
    * calling SU is chosen to transmit first or 1 if it is the called SU. Note
    * that if unset, the Intial Transmitter is null, then the field is unset.
    */
   private Boolean callingSuInitialTransmitter;

   /*
    * This parameter indicates whether RF resources are available in an RFSS
    * area. A value of 0, which is the default value, indicates that no RF
    * resource is available. This field SHALL be present in confirmed group
    * calls: - in the first INVITE message from a serving RFSS to the Home
    * RFSS. - in the re-INVITE messages from any serving RFSS to the Home RFSS
    * when the RF resource allocation state changes. - in the 200 OK response
    * from each serving RFSS that provides a Type 1 SDP answer to the Home
    * RFSSs SIP (Re-)INVITE request.
    * 
    * This field SHOULD be present in SU-to-SU calls in all the INVITE, 200 OK
    * and ACK messages.
    */
   private boolean rfResourceAvailable;

   /*
    * This parameter indicates to the Called Serving RFSS whether it SHOULD
    * perform an availability check with the called SU prior to granting the
    * request and allocating a traffic channel, or to process the call as a
    * direct call. The called SU is expected to respond to the availability
    * check with a refusal or an acceptance.
    */
   private boolean availabilityCheckRequested;
   private boolean availCheckRequestIsSet;

   /*
    * This parameter indicates the desired duplexity.
    */
   private boolean fullDuplexRequested;
   private boolean fullDuplexRequestedIsSet;
   public void setFullDuplexRequestedIsSet(boolean bflag) { fullDuplexRequestedIsSet=bflag; }
   
   /*
    * Protected mode call setup requested.
    */
   private boolean protectedMode;
   private boolean protectedModeIsSet;
   public boolean isProtectedModeIsSet() { return protectedModeIsSet; }
   public void setProtectedModeIsSet(boolean bflag) { protectedModeIsSet=bflag; }

   /*
    * This parameter indicates the roaming of an SU in an SU-to-SU Call. It is
    * named c-incallroaming with a compact form c-icr.
    */
   private boolean incallRoaming;

   /*
    * Group call type.
    */
   private boolean confirmedGroupCall;
   private boolean confirmedGroupCallIsSet;
   public boolean isConfirmedGroupCallIsSet() { return confirmedGroupCallIsSet; }
   public void setConfirmedGroupCallIsSet(boolean bflag) { confirmedGroupCallIsSet=bflag; }

   /* 
    * This parameter indicates the console transmission request priority
    */
   private int consoleTransmitRequestPriority = 1;

   //-------------------------------------------------------------------------
   private static int getPriorityFromString(String st) {
      String str = st.trim();
      return ConsoleTransmissionRequestPriority.getInstance(str).intValue();
   }
   private static String getStringFromPriority(int priority) {
      return ConsoleTransmissionRequestPriority.getInstance(priority).hexStringValue();
   }

   //-------------------------------------------------------------------------
   private static boolean getBooleanFromString(String st) throws ParseException {
      String str = st.trim();
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
    * Default constructor (initialized with the default values from Table 25 BACA).
    */
   public CallParam() {
      this.callingSuInitialTransmitter = null;
      this.rfResourceAvailable = false;
      this.availabilityCheckRequested = true;
      this.fullDuplexRequested = false;
      this.incallRoaming = false;
      this.setProtectedMode(false);
      //this.protectedMode = false;
   }

   /**
    * @param callingSuInitialTransmitter
    *            The callingSuInitialTransmitter to set.
    */
   public void setCallingSuInitialTransmitter(Boolean callingSuInitialTransmitter) {
      this.callingSuInitialTransmitter = callingSuInitialTransmitter;      
   }

   /**
    * @return Returns true if calling su is set to be the initial transmitter.
    * 
    */
   public boolean isCallingSuInitialTransmitter() {
      return this.callingSuInitialTransmitter != null
            && callingSuInitialTransmitter;
   }

   public boolean isCalledSuInitialTransmitter() {
      return this.callingSuInitialTransmitter != null
            && !this.callingSuInitialTransmitter;
   }

   /**
    * @param rfResourceAvailable
    *            The rfResourceAvailable to set.
    */
   public void setRfResourceAvailable(boolean rfResourceAvailable) {
      logger.debug("setRfResourceAvailable: confirmedGroupCall="+confirmedGroupCall);
      // to support group call and U2U call
      //if( confirmedGroupCall) 
      {
         this.rfResourceAvailable = rfResourceAvailable;
         this.resAvailIsSet = true;
      }
   }

   /**
    * @return Returns the rfResourceAvailable.
    */
   public boolean isRfResourceAvailable() {
      return rfResourceAvailable;
   }
   public void setRfResourceAvailableIsSet(boolean bflag) {
      this.resAvailIsSet = bflag;
   }

   /**
    * @param availabilityCheckRequested
    *            The availabilityCheckRequested to set.
    */
   public void setAvailabilityCheckRequested(boolean availabilityCheckRequested) {
      this.availabilityCheckRequested = availabilityCheckRequested;
      this.availCheckRequestIsSet = true;
   }

   /**
    * @return Returns the availabilityCheckRequested.
    */
   public boolean isAvailabilityCheckRequested() {
      return availabilityCheckRequested;
   }

   /**
    * @param fullDuplexRequested
    *            The fullDuplexRequested to set.
    */
   public void setFullDuplexRequested(boolean fullDuplexRequested) {
      this.fullDuplexRequested = fullDuplexRequested;
      setFullDuplexRequestedIsSet(true);
   }

   /**
    * @return Returns the fullDuplexRequested.
    */
   public boolean isFullDuplexRequested() {
      return fullDuplexRequested;
   }

   /**
    * @param incallRoaming
    *            The incallRoaming to set.
    */
   public void setIncallRoaming(boolean incallRoaming) {
      this.incallRoaming = incallRoaming;
   }

   /**
    * @return Returns the incallRoaming.
    */
   public boolean isIncallRoaming() {
      return incallRoaming;
   }

   /**
    * @param confirmedGroupCall
    *            The confirmedGroupCall to set.
    */
   public void setConfirmedGroupCall(boolean confirmedGroupCall) {
      this.confirmedGroupCall = confirmedGroupCall;
      setConfirmedGroupCallIsSet( true);
   }

   /**
    * @return Returns the confirmedGroupCall.
    */
   public boolean isConfirmedGroupCall() {
      return confirmedGroupCall;
   }

   /**
    * @param protectedMode the protectedMode to set
    */
   public void setProtectedMode(boolean protectedMode) {
      this.protectedMode = protectedMode;
      //setProtectedModeIsSet( true);
   }

   /**
    * @return the protectedMode
    */
   public boolean isProtectedMode() {
      return protectedMode;
   }

   public int getConsoleTransmitRequestPriority() {
      return consoleTransmitRequestPriority;
   }
   public void setConsoleTransmitRequestPriority(int priority) {
      this.consoleTransmitRequestPriority = priority;
   }

   /**
    * Parse and return a call param string.
    * 
    * @param cParam  The call param string
    * @return the CallParam object.
    * @throws ParseException
    */
   public static CallParam createCallParam(String cParam) throws ParseException {

      //logger.debug("createCallParam: [" +cParm +"]");
      StringBuffer sbuf = new StringBuffer(cParam);
      for (int i = 0; i < cParam.length(); i++) {
         if (cParam.charAt(i) != '\r')
            sbuf.append(cParam.charAt(i));
      }
      String callParam = sbuf.toString();
      String[] lines = callParam.split("\n");
      if (lines.length == 0)
         throw new ParseException("Bad Param set [" + callParam + "]", 0);

      CallParam retval = new CallParam();
      for (String lin : lines) {
         lin = lin.trim();
         if ("".equals(lin)) continue;
	 if ("\r\n".equals(lin) || "\n".equals(lin)) continue;

         String li[] = lin.split(":");
         if (li.length != 2)
            throw new ParseException("Bad param [" + lin + "]", 0);
         String name = li[0].trim();
         String val = li[1].trim();
         //logger.debug("createCallParam: " +lin +" name=" +name +" val=" +val);

         if (name.equalsIgnoreCase("c-initrans")
               || name.equalsIgnoreCase("c-i")) {
            retval.callingSuInitialTransmitter = new Boolean( !getBooleanFromString(val));

	 } else if (name.equalsIgnoreCase("c-resavail")
               || name.equalsIgnoreCase("c-r")) {
            retval.rfResourceAvailable = getBooleanFromString(val);
            retval.resAvailIsSet = true;

	 } else if (name.equalsIgnoreCase("c-pref")
               || name.equalsIgnoreCase("c-prf")) {
            retval.availabilityCheckRequested = getBooleanFromString(val);
            retval.availCheckRequestIsSet = true;

	 } else if (name.equalsIgnoreCase("c-duplex")
               || name.equalsIgnoreCase("c-d")) {
            retval.fullDuplexRequested = getBooleanFromString(val);
            retval.setFullDuplexRequestedIsSet(true);

	 } else if (name.equalsIgnoreCase("c-incallroaming")
               || name.equalsIgnoreCase("c-icr")) {
            retval.incallRoaming = getBooleanFromString(val);

	 } else if (name.equalsIgnoreCase("c-groupcalltype")
               || name.equalsIgnoreCase("c-gct")) {
            //retval.confirmedGroupCall = getBooleanFromString(val);
            retval.setConfirmedGroupCall( getBooleanFromString(val));

         } else if (name.equalsIgnoreCase("c-protected") 
               || name.equalsIgnoreCase("c-p")) {
            retval.setProtectedMode(getBooleanFromString(val));
            retval.setProtectedModeIsSet(true);

         } else if (name.equalsIgnoreCase("c-consoletransmitrequestpriority") 
               || name.equalsIgnoreCase("c-ctxrp")) {
            retval.setConsoleTransmitRequestPriority( getPriorityFromString(val));

         } else {
            throw new ParseException("Unrecognised field [" + lin + "]", 0);
         }
      }
      //logger.debug("createCallParam: parsed [" + retval + "]");
      return retval;
   }

   private static void appendField(String fieldName, boolean value, StringBuffer sb) {
      sb.append(fieldName);
      sb.append(":");
      sb.append(getStringFromBoolean(value));
      sb.append("\r\n");
   }

   private static void appendField(String fieldName, String value, StringBuffer sb) {
      sb.append(fieldName);
      sb.append(":");
      sb.append(value);
      sb.append("\r\n");
   }

   //------------------------------------------
   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      if (this.callingSuInitialTransmitter != null) {
         appendField("c-initrans", ! this.callingSuInitialTransmitter, sb);
      }
      CallParam reference = new CallParam();
      //if (resAvailIsSet ||
      //    reference.rfResourceAvailable != this.rfResourceAvailable) {
      if (resAvailIsSet) {
         // check if confirmed group call
         //if( confirmedGroupCall) 
            appendField("c-resavail", this.rfResourceAvailable, sb);
      }

      logger.debug("toString(): availCheckRequestIsSet="+availCheckRequestIsSet);
      if (availCheckRequestIsSet || 
          reference.availabilityCheckRequested != this.availabilityCheckRequested) {
         appendField("c-pref", this.availabilityCheckRequested, sb);
      }

      // #xxx 12.3.2.x  need to show c-duplex:0
      // #158 12.13.1 no need to encode c-duplex ??
      //if (reference.fullDuplexRequested != this.fullDuplexRequested)   
      //   appendField("c-duplex", this.fullDuplexRequested, sb);
      //
      if (fullDuplexRequestedIsSet)
         appendField("c-duplex", this.fullDuplexRequested, sb);

      if (reference.incallRoaming != this.incallRoaming)   
         appendField("c-incallroaming", this.incallRoaming, sb);

      //if (reference.confirmedGroupCall != this.confirmedGroupCall)
      //if (isConfirmedGroupCallIsSet() ||
      //   reference.confirmedGroupCall != this.confirmedGroupCall)
      if (isConfirmedGroupCallIsSet())
         appendField("c-groupcalltype", this.confirmedGroupCall, sb);

      if (isProtectedModeIsSet() ||
         reference.isProtectedMode() != this.isProtectedMode())
         appendField("c-protected", this.isProtectedMode(),sb);

      if (reference.getConsoleTransmitRequestPriority() != this.getConsoleTransmitRequestPriority()) {
         appendField("c-consoletransmitrequestpriority",
            getStringFromPriority( this.getConsoleTransmitRequestPriority()),sb);
      }
      return sb.toString();
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof CallParam))
         return false;
      CallParam template = (CallParam) other;
      if ( template.callingSuInitialTransmitter == null ^ this.callingSuInitialTransmitter == null )
         return false;
      return (( template.callingSuInitialTransmitter == null  && this.callingSuInitialTransmitter == null ||
               template.callingSuInitialTransmitter.equals(this.callingSuInitialTransmitter) ) &&
            template.availabilityCheckRequested == this.availabilityCheckRequested
            && template.confirmedGroupCall == this.confirmedGroupCall
      //===      && template.fullDuplexRequested == this.fullDuplexRequested
            && template.incallRoaming == this.incallRoaming
            && template.rfResourceAvailable == this.rfResourceAvailable 
            && template.protectedMode == this.protectedMode
//CSSI - do we need to check priority ?
            && template.consoleTransmitRequestPriority == this.consoleTransmitRequestPriority);
   }

   public boolean isDefault() {
      return (new CallParam().equals(this)) && 
              !this.resAvailIsSet && !this.availCheckRequestIsSet;
   }
   
   /**
    * Almost the same opertation as equals except that initialTransmitter is not compared ( except for
    * existence).
    * 
    * @param other -- the template to compare with.
    * 
    * @return true if a match occured.
    */
   public boolean match(CallParam other) {
      if (!(other instanceof CallParam))
         return false;
      CallParam template = (CallParam) other;
      if ( template.callingSuInitialTransmitter == null ^ this.callingSuInitialTransmitter == null )
         return false;
      return ( ( template.callingSuInitialTransmitter == this.callingSuInitialTransmitter ||
            template.callingSuInitialTransmitter != null && this.callingSuInitialTransmitter != null ) &&
            template.availabilityCheckRequested == this.availabilityCheckRequested
            && template.confirmedGroupCall == this.confirmedGroupCall
            && template.fullDuplexRequested == this.fullDuplexRequested
            && template.incallRoaming == this.incallRoaming
            && template.rfResourceAvailable == this.rfResourceAvailable 
            && template.protectedMode == this.protectedMode
//CSSI - do we need to check priority ?
            && template.consoleTransmitRequestPriority == this.consoleTransmitRequestPriority);
   }
}
