//
package gov.nist.p25.issi.traceviewer;

import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;


public class PttMessageData extends MessageData 
      implements Comparable
{
   private static Logger logger = Logger.getLogger(PttMessageData.class);
   public static void showln(String s) { System.out.println(s); }

   private static final String TAG_HEARTBEAT = "Heartbeat";
   private static final String TAG_HEARTBEAT_QUERY = "Heartbeat Query";
   private static final String TAG_PTT_TRANSMIT_REQUEST = "PTT Transmit Request";
   private static final String TAG_PTT_TRANSMIT_GRANT = "PTT Transmit Grant";
   private static final String TAG_PTT_TRANSMIT_START = "PTT Transmit Start";
   private static final String TAG_PTT_TRANSMIT_PROGRESS = "PTT Transmit Progress";
   private static final String TAG_PTT_TRANSMIT_END = "PTT Transmit End";
   private static final String TAG_PTT_TRANSMIT_MUTE = "PTT Transmit Mute";
   private static final String TAG_PTT_TRANSMIT_UNMUTE = "PTT Transmit Unmute";

   private String sequenceNumber;   
   private Properties properties;

   // accessor
   public String getSequenceNumber() {
      return sequenceNumber;
   }
   public Properties getProperties() {
      return properties;
   }
   
   // constructor
   public PttMessageData( String remoteRfssId, String myRfssId, 
         String myRtpRecvPort, String remoteRtpRecvPort, 
         String messageType, String data, 
         String time, 
         String sequenceNumber,
         boolean selected, boolean isSender,
         Properties props)
   {
      this( remoteRfssId, myRfssId, 
         myRtpRecvPort, remoteRtpRecvPort, 
         messageType, data, 
         Long.parseLong(time), 
         sequenceNumber, selected, isSender,
         props);
   }
   public PttMessageData( String remoteRfssId, String myRfssId, 
         String myRtpRecvPort, String remoteRtpRecvPort, 
         String messageType, String data, 
         long timestamp, 
         String sequenceNumber,
         boolean selected, boolean isSender,
         Properties props)
   {
      super(remoteRfssId, myRfssId, remoteRtpRecvPort, myRtpRecvPort, 
            messageType, data, timestamp,
            selected, isSender);
      this.sequenceNumber = sequenceNumber;
      this.properties = props;   
   }

   // implementation of Comparable
   //---------------------------------------------------------------------------
   public int compareTo(Object obj) {

      if(!(obj instanceof PttMessageData)) {
	 return super.compareTo( obj);
      }
      PttMessageData m2 = (PttMessageData) obj;
      PttMessageData m1 = this;
      return computeQ( m1, m2);
   }

   public static int computeQ(PttMessageData m1, PttMessageData m2) {
      try {
         //+++long ts1 = Long.parseLong(m1.getTime());
         //+++long ts2 = Long.parseLong(m2.getTime());
         long ts1 = m1.getTimestamp();
         long ts2 = m2.getTimestamp();
	 long diff = Math.abs( ts1 - ts2);
         //logger.debug("compareQ(0): ts1="+ts1+" ts2="+ts2+" diff="+diff);
	 // time delta could be +/- 1
         if (diff > 2) {
            if (ts1 < ts2) {
               return -1;
            } else if (ts1 > ts2) {
               return 1;
            } 
            return 1;
         } else {
            return computePttQ( m1, m2);
         }
      } catch (NumberFormatException ex) {
         //ex.printStackTrace();
         logger.debug("computeQ(0): "+ex);
      }
      return 0;
   }

   public static int computePttQ(PttMessageData m1, PttMessageData m2) {
      // Q(-1,0,+1) m1 is less than, equals to, greater than m2
      int Q = 0;
      try {
            // timestamp are same
           /***
            logger.debug("computeQ(1): m1.seqNo: "+m1.getSequenceNumber()+
                         " type:"+m1.getMessageType()+
                         " time:"+m1.getTimestamp());
            logger.debug("computeQ(2): m2.seqNo: "+m2.getSequenceNumber()+
                         " type:"+m2.getMessageType()+
                         " time:"+m2.getTimestamp());
             **/
            int sq1 = Integer.parseInt(m1.getSequenceNumber());
            int sq2 = Integer.parseInt(m2.getSequenceNumber());
            //if ( sq1 < sq2) return -1;
            //else if (sq1 > sq2) return 1;
            //else return 0;
            if ( sq1 < sq2) Q = -1;
            else if (sq1 > sq2) Q = 1;
            else Q = 0;            

            // apply rules by message type 
	    //-----------------------------------------------------------------
            if( m1.getMessageType().equals(m2.getMessageType())) {
               logger.debug("computeQ(5): same msg/time -------------Q="+Q);
               return Q;
            } 
	    //---------------------
	    if( TAG_HEARTBEAT_QUERY.equals(m1.getMessageType()) &&
                TAG_HEARTBEAT.equals(m2.getMessageType())) {
                Q = -1; 
            }
	    else if( TAG_HEARTBEAT.equals(m1.getMessageType()) &&
                TAG_HEARTBEAT_QUERY.equals(m2.getMessageType())) {
                Q = 1; 
            }
	    //---------------------
	    else if( TAG_PTT_TRANSMIT_REQUEST.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_GRANT.equals(m2.getMessageType())) {
                Q = -1; 
            }
	    else if( TAG_PTT_TRANSMIT_GRANT.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_REQUEST.equals(m2.getMessageType())) {
                Q = 1; 
            }
	    //---------------------
	    else if( TAG_PTT_TRANSMIT_GRANT.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_START.equals(m2.getMessageType())) {
                Q = -1; 
            }
	    else if( TAG_PTT_TRANSMIT_START.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_GRANT.equals(m2.getMessageType())) {
                Q = 1; 
            }
	    //---------------------
	    else if( TAG_PTT_TRANSMIT_START.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_PROGRESS.equals(m2.getMessageType())) {
                Q = -1; 
            }
	    else if( TAG_PTT_TRANSMIT_PROGRESS.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_START.equals(m2.getMessageType())) {
                Q = 1; 
            }
	    //---------------------
	    else if( TAG_PTT_TRANSMIT_GRANT.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_PROGRESS.equals(m2.getMessageType())) {
                Q = -1; 
            }
	    else if( TAG_PTT_TRANSMIT_PROGRESS.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_GRANT.equals(m2.getMessageType())) {
                Q = 1; 
            }
	    //---------------------
	    else if( TAG_PTT_TRANSMIT_PROGRESS.equals(m1.getMessageType()) &&
                TAG_HEARTBEAT.equals(m2.getMessageType())) {
                Q = -1; 
            }
	    else if( TAG_HEARTBEAT.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_PROGRESS.equals(m2.getMessageType())) {
                Q = 1; 
            }
	    //---------------------
	    /*** DISABLE until ew need this !!!
	    else if( TAG_PTT_TRANSMIT_MUTE.equals(m1.getMessageType()) &&
                TAG_HEARTBEAT.equals(m2.getMessageType())) {
                Q = -1; 
            }
	    else if( TAG_HEARTBEAT.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_MUTE.equals(m2.getMessageType())) {
                Q = 1; 
            }
	    //---------------------
	    else if( TAG_PTT_TRANSMIT_UNMUTE.equals(m1.getMessageType()) &&
                TAG_HEARTBEAT.equals(m2.getMessageType())) {
                Q = -1; 
            }
	    else if( TAG_HEARTBEAT.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_UNMUTE.equals(m2.getMessageType())) {
                Q = 1; 
            }
	     ***/
	    //---------------------
	    else if( TAG_PTT_TRANSMIT_PROGRESS.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_END.equals(m2.getMessageType())) {
                Q = -1; 
            }
	    else if( TAG_PTT_TRANSMIT_END.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_PROGRESS.equals(m2.getMessageType())) {
                Q = 1; 
            }
	    //---------------------
	    else if( TAG_PTT_TRANSMIT_PROGRESS.equals(m1.getMessageType()) &&
                TAG_PTT_TRANSMIT_PROGRESS.equals(m2.getMessageType())) {
                Q = -1; 
            }
	    //-----------------------------------------------------------------
            logger.debug("computeQ(9): --------------------------------Q="+Q);
            return Q;

      } catch (NumberFormatException ex) {
         //ex.printStackTrace();
         logger.debug("computeQ(9): "+ex);
      }
      return 0;
   }

   public static void organizePttMessageData(List<MessageData> msgList) {
      //showln("organizeMessageData(0): *** START");
      for( int i=1; i < msgList.size(); i++) {
         MessageData m0 = msgList.get( i-1); 
         MessageData m1 = msgList.get( i); 
         if(!(m0 instanceof PttMessageData) ||
            !(m1 instanceof PttMessageData)) {
            continue;
         }
         // skip heartbeat message
	 /***
         if(TAG_HEARTBEAT.equals(m0.getMessageType()) ||
            TAG_HEARTBEAT.equals(m1.getMessageType()) ||
            TAG_HEARTBEAT_QUERY.equals(m0.getMessageType()) ||
            TAG_HEARTBEAT_QUERY.equals(m1.getMessageType())) {
            continue;
         }
	  **/

	 // donot skip the same message type
         //if(!m0.getMessageType().equals(m1.getMessageType()))
         {
            int Q = computeQ((PttMessageData)m0, (PttMessageData)m1);
            if( Q > 0) {
               logger.debug("organizeMessageData(1): swap at i="+i);
               msgList.set( i-1, m1);
               msgList.set( i, m0);
            }
         }
      }
      //showln("organizeMessageData(9): *** DONE");
   }
}
