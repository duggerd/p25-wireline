//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * Timer values are in miliseconds. These are settable using the topologyconfig. 
 * For conveniance they are public values here.
 */
public final class TimerValues {
   /**
    * Packet loss timeout.
    */
   public static  int END_LOSS_TIMEOUT = 10000;

   /**
    * Preferred Minimum time between the end of a previous transmission and the
    * transmission of an subsequent PTT Request.
    */
   public static  int MIN_KEYBACK_TIME = 60;

   /**
    * Number of retransmissions for PTT Transmit End packets.
    */
   public static  int REND = 4;

   /**
    * PTT Request Timeout.
    */
   public static  int REQUEST_TIMEOUT = 1000;

   /**
    * Number of retransmissions for PTT Start packets.
    */
   public static int RSTART = 4;

   /**
    * (Optional) Arbitration window.
    */
   public static int TELECTION = 4000;

   /**
    * Retransmission Time for PTT Transmit End Packets.
    */
   public static int TEND = 20;

   /**
    * Normal EndLossTimeout.
    * 200
    */
   public static int TENDLOSS = 200;

   /**
    * EndLoss Timeout for the first Packet of a transmission.
    * 
    */
   public static int TFIRSTPACKETTIME = 500;

   /**
    * Maximum time interval which when exceeded will entail sending of a
    * re-INVITE message from the home RFSS to the serving RFSS to synchronize
    * the SIP states between the home RFSS and serving RFSS when there is no
    * RTP session.
    */
   public static int TGCHCONFIRMSIP = 4000;

   /**
    * Maximum time interval (i.e., ISSI hang time) during which there is no
    * voice activity over the ISSI among RFSSs participating in a group call,
    * which when exceeded will entail group call release.
    */
   public static int TGCHHANGTIME = 30000;

   /**
    * Maximum time interval the home RFSS waits before starting a confirmed
    * group call.
    */
   public static int TGCHSTARTCONFIRMED = 5000;

   /**
    * Heartbeat interval.
    */
   public static int THEARTBEAT = 10000;

   /**
    * EndLossTimeout when the RFSS has muted the transmission.
    */
   public static int TMUTEENDLOSS = 2000;

   /**
    * Rate at which a Muted RFSS sends Heartbeat messages with M bit set to 1
    * and the TSN of the transmission.
    */
   public static int TMUTEPROGRESS = 500;

   /**
    * Retransmission time for PTT Transmit Request packets.
    */
   public static int TREQUEST = 500;

   /**
    * Retransmission Time for PTT Start Packets.
    */
   public static int TSTART = 360;

   /**
    * Maximum time interval during which there is no voice activity among RFSSs
    * participating in an SU-to-SU call, which when exceeded will entail
    * SU-to-SU call release.
    */
   public static int TU2UHANGTIME = 30000;

   /**
    * Unmute Retransmission Interval.
    */
   public static int TUNMUTE = 250;

   /**
    * Time to wait following receipt of PTT Wait before retrying a request.
    */
   public static int WAITTIMEOUT = 5000;
   

   public static void resetToDefaults() {
         END_LOSS_TIMEOUT = 10000;
         MIN_KEYBACK_TIME = 60;
         REND = 4;
         REQUEST_TIMEOUT = 1000;
         RSTART = 4;
         TELECTION = 4000;
         TEND = 20;
         TENDLOSS = 200;
         TFIRSTPACKETTIME = 500;
         TGCHCONFIRMSIP = 4000;
         TGCHHANGTIME = 30000;
         TGCHSTARTCONFIRMED = 5000;
         THEARTBEAT = 10000;
         TMUTEENDLOSS = 2000;
         TMUTEPROGRESS = 500;
         TREQUEST = 500;
         TSTART = 360;
         TU2UHANGTIME = 30000;
         TUNMUTE = 250;
         WAITTIMEOUT = 5000;
   }

   /**
    * Set the heartbeat value. This is used in testing automatic call teardown on heartbeat loss.
    * 
    * @param heartbeat
    */
   public static void setHeartbeat(int heartbeat) {
      THEARTBEAT = heartbeat;
   }
}
