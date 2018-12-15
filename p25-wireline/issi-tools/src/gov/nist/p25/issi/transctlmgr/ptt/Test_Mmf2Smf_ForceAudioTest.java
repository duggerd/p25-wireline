//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.p25payload.IMBEVoiceBlock;
import gov.nist.p25.issi.p25payload.IMBEVoiceDataGenerator;
import gov.nist.p25.issi.p25payload.TransmitPriorityType;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import junit.framework.TestCase;
import java.util.List;

/**
 * Tests SMF to MMF audio send.
 */
public class Test_Mmf2Smf_ForceAudioTest extends TestCase
      implements SmfRxListener {

   private static Logger logger = Logger.getLogger("gov.nist.p25.issi.transctlmgr");

   /** SMF RTP receive port (WARNING: port may already be in use!) */
   private static final int SMF_RTP_RECV_PORT = 1842;

   /** MMF RTP receive port (WARNING: port may already be in use!) */
   private static final int MMF_RTP_RECV_PORT = 1844;

   /** The number of audio packets sent. */
   private static final int NUM_AUDIO_PACKETS = 2;
   private SmfSession smfSession = null;
   private MmfSession mmfSession = null;
   private PttManager pttManager = new PttManager("127.0.0.1");

   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   // SmfRx flags
   private boolean gotSpurtStart = false;
   private boolean gotAudio = false;
   private int gotAudioPackets = 0;
   private boolean gotSpurtEnd = false;

   /* Note no MmfTx flags are available. */
   public void test() {

      System.out.println("=============================================\n"
            + "File: Test_MmfTx_ForcedAudioSend\n"
            + "Description: MMF self-grant transmit to SMF with no"
            + " PTT START burst:\n" + "Reference: Section 7.2.1-7.2.2\n\n" +
            "SMF <-PTT TRANSMIT PROGRESS <- MMF (audio packets)\n"
            + "SMF <-PTT TRANSMIT END <- MMF (x4)\n" + "MMF TERMINATED\n"
            + "SMF DONE\n"
            + "---------------------------------------------\n");

      try {
         smfSession = pttManager.createTestSmfSession(SMF_RTP_RECV_PORT,
               "127.0.0.1", MMF_RTP_RECV_PORT,
               LinkType.UNIT_TO_UNIT_CALLING_SERVING_TO_CALLING_HOME);
         smfSession.smfReceiver.addListener(this);
         mmfSession = pttManager.createTestMmfSession(MMF_RTP_RECV_PORT,
               "127.0.0.1", SMF_RTP_RECV_PORT,
               LinkType.UNIT_TO_UNIT_CALLING_HOME_TO_CALLING_SERVING,null);

      } catch (Exception ex) {
         ex.printStackTrace();
         fail("Unexpected exception");
      }

      // Start MMF normally with a spurt start
      try {
         List<IMBEVoiceBlock> imbeVoiceBlocks = IMBEVoiceDataGenerator
               .getIMBEVoiceData("voice/imbe-hex-test.txt", NUM_AUDIO_PACKETS);

         for (int i = 0; i < imbeVoiceBlocks.size(); i++) {
            IMBEVoiceBlock[] a = new IMBEVoiceBlock[1];
            a[0] = imbeVoiceBlocks.get(i);
            mmfSession.getMmfTransmitter().forceAudio(a,5,0,4,TransmitPriorityType.NORMAL, 1);
         }
         mmfSession.getMmfTransmitter().sendSpurtEndNotification(4);

      } catch (Exception e) {
         e.printStackTrace();
         fail("Unexpected exception");
      }

      try {
         Thread.sleep(4000); // IMPORTANT! Make sure this is long enough!
      } catch (InterruptedException ie) {
         // ie.printStackTrace();
      }
   }

   public void tearDown() throws Exception {
      smfSession.shutDown();
      mmfSession.shutDown();
      logger.debug("Checking assertions...");

      /* Check SmfTx assertions */
      assertTrue(gotSpurtStart == false);
      assertTrue(gotAudio == true);
      assertTrue(gotSpurtEnd == true);
      assertTrue(gotAudioPackets == NUM_AUDIO_PACKETS);
      logger.debug("DONE: All assertions passed.");
   }

   /***************************************************************************
    * SmfRxListener Methods
    **************************************************************************/
   public void receivedPttStart(SmfPacketEvent event) {
      logger.debug("[SmfRxListener] GOT SPURT START");
      gotSpurtStart = true;
   }

   public void receivedPttEnd(SmfPacketEvent event) {
      logger.debug("[SmfRxListener] GOT SPURT END");
      gotSpurtEnd = true;
   }

   public void receivedPttProgress(SmfPacketEvent event) {
      logger.debug("[SmfRxListener] GOT AUDIO");
      gotAudio = true;
      gotAudioPackets++;
   }
}
