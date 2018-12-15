//
package gov.nist.p25.issi.traceviewer;


import javax.sip.header.CSeqHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;

//import gov.nist.javax.sip.message.SIPMessage;
import org.apache.log4j.Logger;


public class SipMessageData extends MessageData 
      implements Comparable
{
   private static Logger logger = Logger.getLogger(SipMessageData.class);
   public static void showln(String s) { System.out.println(s); }

   // SIP message fragment
   private static final String MSG_REGISTER_SIP = "REGISTER sip:";
   private static final String MSG_INVITE_SIP = "INVITE sip:";
   private static final String MSG_ACK_SIP = "ACK sip:";
   private static final String MSG_MESSAGE_SIP = "MESSAGE sip:";

   private static final String MSG_100_TRYING = "100 Trying";
   private static final String MSG_180_RINGING = "180 Ringing";
   private static final String MSG_183_SESSION_PROGRESS = "183 Session progress";
   private static final String MSG_200_OK = "200 OK";
   private static final String MSG_486_BUSY_HERE = "486 Busy here";
   private static final String MSG_487_REQUEST_TERMINATED = "487 Request Terminated";
   private static final String MSG_603_DECLINE = "603 Decline";
   
   private String timeStamp;
   private String transactionId;
   private Message sipMessage;
   
   // accessors
   /***
   public String getTimeStamp() {
      return timeStamp;
   }
    **/

   public String getTransactionId() {
      return transactionId;
   }

   public Message getSipMessage() {
      return sipMessage;    
   }
   public void setSipMessage(Message sipMsg) {
      logger.debug("SipMessageData(): setSipMsg: "+sipMsg.getClass().getName());
      if(sipMsg == null) {
         throw new NullPointerException("SipMessageData: setSipMessage(null)");
      }
      this.sipMessage = sipMsg;
   }

   // constructor
   public SipMessageData(String fromRfssId, String toRfssId,
      String fromPort, String toPort,
      String messageType, String data,
      String time, String timeStamp,
      String transactionId, boolean selected)
   {
      super(fromRfssId, toRfssId, fromPort, toPort,
            messageType, data,
	    Long.parseLong(time),
            selected, false);
      
      if( data == null) {
         throw new NullPointerException("SipMessageData: null data.");
      }
      this.timeStamp = timeStamp;
      this.transactionId = transactionId;
   }

   @Override
   public boolean equals(Object other) {
      return this.hashCode() == other.hashCode();
   }

   @Override
   public int hashCode() {
      return super.hashCode();
   }

   // implemenatation of Comparable
   public int compareTo(Object obj) {

      if( !(obj instanceof SipMessageData)) {
	 return super.compareTo(obj);
      }

      SipMessageData m2 = (SipMessageData) obj;
      SipMessageData m1 = this;
      //logger.debug("compareTo(): m1="+m1.toString());
      //logger.debug("compareTo(): m2="+m2.toString());
      try {
         long ts1 = m1.getTimestamp();
         long ts2 = m2.getTimestamp();

//TODO: need to reconcile time vs timeStamp
         //long ts1 = Long.parseLong(m1.getTimeStamp());
         //long ts2 = Long.parseLong(m2.getTimeStamp());
         if (ts1 < ts2) {
            return -1;
	 } else if (ts1 > ts2) {
            return 1;
	 } else {
            // same timestamp
            Message m1msg = m1.getSipMessage();
            Message m2msg = m2.getSipMessage();
            //logger.debug("compareTo(): m1msg="+ m1msg);
            //logger.debug("compareTo(): m2msg="+ m2msg);
            logger.debug("compareTo(): m1msg="+
               (m1msg != null ? m1msg.getClass().getName() : "NULL"));
            logger.debug("compareTo(): m2msg="+
               (m2msg != null ? m2msg.getClass().getName() : "NULL"));
            //logger.debug("compareTo(): m1Tid/m2Tid="+
            //   m1.getTransactionId()+"/"+m2.getTransactionId());
	    //--------------------------------------------------
            if(m1msg instanceof Request &&
               m2msg instanceof Response &&
               m2.getTransactionId().equals(m1.getTransactionId())) {
               return -1;

            } else if (m1msg instanceof Response &&
                m2msg instanceof Request &&
                !((CSeqHeader) m1msg.getHeader(CSeqHeader.NAME)).getMethod()
                   .equals(m2msg.getHeader(CSeqHeader.NAME))) {
               return -1;

            } else if (m1msg instanceof Response &&
                  m2msg instanceof Response &&
                  m2.getTransactionId().equals(m1.getTransactionId())) {
               // #166
               // Request: INVITE
               // Response: 403 Forbidden followed by ACK
               logger.debug("compareTo(): Request, error response, ACK response...");
               return -1;

            } else if (m1msg instanceof Response &&
                  m2msg instanceof Response) {

               int m1Code = ((Response)m1msg).getStatusCode();
               int m2Code = ((Response)m2msg).getStatusCode();
               logger.debug("compareTo(): m1Code/m2Code="+m1Code+"/"+m2Code);
               if(m1Code == Response.TRYING &&
                  m2Code == Response.RINGING) {
                  return -1;
	       } else if(m1Code == Response.RINGING &&
                  m2Code == Response.TRYING) {
                  return 1;
	       } else if(m1Code == Response.RINGING &&
                  m2Code == Response.SESSION_PROGRESS) {
                  return -1;
	       } else if(m1Code == Response.SESSION_PROGRESS &&
                  m2Code == Response.RINGING) {
                  return 1;
	       } else {
                  return 1;
               }

            } else {

               String m1type = m1.getMessageType();
               String m2type = m2.getMessageType();
               logger.debug("compareTo(10): m1type/m2type="+m1type+"|"+m2type);

	       // NOTE: m1type or m2type could be null
               //-----------------------------------------------
               if( m1type != null && m1type.startsWith(Request.ACK)) {
	          if(m1type.startsWith(MSG_ACK_SIP) &&
	             m2type.startsWith(MSG_MESSAGE_SIP) ) {
                     logger.debug("compareTo(24): ACK/MESSAGE return of -1 !!!");
                     return -1;
                  }
                  logger.debug("compareTo(11): ACK sip default return of 1 !!!");
                  return 1;
	       } else if( m2type != null && m2type.startsWith(Request.ACK)) {
	          if(m1type.startsWith(MSG_MESSAGE_SIP) &&
	             m2type.startsWith(MSG_ACK_SIP)) {
                     logger.debug("compareTo(25): MESSAGE/ACK return of 1 !!!");
                     return 1;
                  }
                  logger.debug("compareTo(12): ACK sip default return of -1 !!!");
                  return -1;
               //-----------------------------------------------
	       } else if( m1type.equals(m2type)) {
	          if(m1type.indexOf(MSG_200_OK) > 0 ||
	             m1type.indexOf(MSG_180_RINGING) > 0 ||
	             m1type.indexOf(MSG_100_TRYING) > 0)
                  {
                     logger.debug("compareTo(13): 200 OK/Ringing return of 1 !!!");
                     return 1;
                  }
                  logger.debug("compareTo(13): m1type=m2type return of -1 !!!");
                  return -1;

               //-----------------------------------------------
	       } else if(m1type.indexOf(MSG_100_TRYING) > 0) {
                  logger.debug("compareTo(13): m1type=Trying return of -1 !!!");
                  return -1;
	       } else if(m2type.indexOf(MSG_100_TRYING) > 0) {
                  logger.debug("compareTo(13): m2type=Trying return of 1 !!!");
                  return 1;
	       } else if(m1type.indexOf(MSG_180_RINGING) > 0 &&
	                 m2type.indexOf(MSG_183_SESSION_PROGRESS) > 0) {
                  logger.debug("compareTo(14): Ringing/Session progress return of -1 !!!");
                  return -1;
	       } else if(m1type.indexOf(MSG_183_SESSION_PROGRESS) > 0 &&
	                 m2type.indexOf(MSG_180_RINGING) > 0) {
                  logger.debug("compareTo(15): Session/Ringing return of 1 !!!");
                  return 1;
               //-----------------------------------------------
	       } else if(m1type.indexOf(MSG_180_RINGING) > 0 &&
	                 m2type.indexOf(MSG_486_BUSY_HERE) > 0) {
                  logger.debug("compareTo(14): Ringing/Busy here return of -1 !!!");
                  return -1;
	       } else if(m1type.indexOf(MSG_486_BUSY_HERE) > 0 &&
	                 m2type.indexOf(MSG_180_RINGING) > 0) {
                  logger.debug("compareTo(15): Busy/Ringing return of 1 !!!");
                  return 1;
               //-----------------------------------------------
	       } else if(m1type.indexOf(MSG_200_OK) > 0 &&
	                 m2type.indexOf(MSG_487_REQUEST_TERMINATED) > 0) {
                  logger.debug("compareTo(24): 200 OK/Request Term return of -1 !!!");
                  return -1;
	       } else if(m1type.indexOf(MSG_487_REQUEST_TERMINATED) > 0 &&
	                 m2type.indexOf(MSG_200_OK) > 0) {
                  logger.debug("compareTo(25): Request Term/200 OK return of 1 !!!");
                  return 1;
               //-----------------------------------------------
	       } else if(m1type.startsWith(MSG_REGISTER_SIP) &&
	                 m2type.indexOf(MSG_200_OK) > 0) {
                  logger.debug("compareTo(24): REGISTER/200 OK here return of -1 !!!");
                  return -1;
	       } else if(m1type.indexOf(MSG_200_OK) > 0 &&
	                 m2type.startsWith(MSG_REGISTER_SIP)) {
                  logger.debug("compareTo(25): 200 OK/REGISTER return of 1 !!!");
                  return 1;
               //-----------------------------------------------
	       // 12.8.x.x
	       } else if(m1type.startsWith(MSG_INVITE_SIP) &&
	                 m2type.indexOf(MSG_603_DECLINE) > 0) {
                  logger.debug("compareTo(24): INVITE/603 Decline here return of -1 !!!");
                  return -1;
	       } else if(m1type.indexOf(MSG_603_DECLINE) > 0 &&
	                 m2type.startsWith(MSG_INVITE_SIP)) {
                  logger.debug("compareTo(25): 603 Decline/INVITE return of 1 !!!");
                  return 1;

               //-----------------------------------------------
	       } else if( m1msg == null) {
                  // m1:180 Ringing or 183 Session progress
		  if(m2type.indexOf(MSG_100_TRYING) > 0) {
                     logger.debug("compareTo(): Ringing/Trying return of -1 !!! ");
                     return -1;
                  }
                  logger.debug("compareTo(): m1msg null return of -1 !!! ");
                  return -1;
	       } else if( m2msg == null) {
                  // m2:180 Ringing or 183 Session progress
		  if(m1type.indexOf(MSG_100_TRYING) > 0) {
                     logger.debug("compareTo(): Trying/Ringing return of -1 !!! ");
                     return -1;
                  }
                  logger.debug("compareTo(): m2type null return of 1 !!! ");
                  return 1;
               //-----------------------------------------------
               } else {
                  // error reponse
                  logger.debug("compareTo(): default return of -1 !!! ");
                  return -1;
               }
            }
         }

      } catch (NumberFormatException ex) {
         logger.debug("compareTo(): ex="+ex);
         ex.printStackTrace();
         return 0;
      }
   }

   @Override
   public String getData() {
      String output = super.getData();
      try {
         output = SipMessageFormatter.format(output);
      } catch(Exception ex) {
         logger.debug("Failed to format SIP message to a fixed order, use native.");
      }
      return output;
   }
   
   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      /***
      if( getTimeStamp() != null) {
         sb.append(" timeStamp=\"" + getTimeStamp() + "\"\n");
      }
       **/
      sb.append(" fromRfssId=\"" + getFromRfssId() + "\"\n");
      sb.append(" toRfssId=\"" + getToRfssId() + "\"\n");
      sb.append(" transactionId=\"" + getTransactionId() + "\"\n");
      sb.append("<sip-message>\n");
      sb.append(getSipMessage() == null ? "": getSipMessage().toString());
      sb.append("</sip-message>\n");
      return sb.toString();
   }
}
