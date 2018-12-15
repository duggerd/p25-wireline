//
package gov.nist.p25.issi.transctlmgr.ptt;

import gov.nist.p25.issi.p25payload.IMBEVoiceBlock;
import gov.nist.p25.issi.p25payload.IMBEVoiceDataGenerator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import junit.framework.TestCase;
import java.util.List;

/**
 * Tests SMF to MMF audio send.
 */
public class Test_Smf2Mmf_RequestSelfGrantTest extends TestCase
      implements SmfTxListener, MmfRxListener {

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

   // SmfTx flags
   private boolean gotGrant = false;
   private boolean gotDeny = false;
   private boolean gotWait = false;

   // MmfRx flags
   private boolean gotRequest = false;
   private boolean gotRequestWithVoice = false;
   private boolean gotAudio = false;
   private int gotAudioPackets = 0;
   private boolean gotSpurtEnd = false;
   private boolean audioTimeout = false;


   public void test() {

      System.out.println("=============================================\n"
            + "File: Test_SmfTx_RequestUnstoppableTrigger\n"
            + "Description: Tests SMF Tx unstoppable TX trigger.  Note that\n"
            + "an MmfApplication is not started to facilitate request timeout:\n"
            + "Reference: Section 7.2.1\n\n" +

            "SMF -> PTT REQUEST -> NO MMF \n"
            + "SMF UNSTOPPABLE TRIGGER\n"
            + "SMF -> PTT PROGRESS -> NO MMF (x3)\n"
            + "SMF -> PTT TRANSMIT END -> NO MMF (x4)\n"
            + "SMF TERMINATED\n"
            + "---------------------------------------------");
      try {
         smfSession = pttManager.createTestSmfSession(SMF_RTP_RECV_PORT,
               "127.0.0.1", MMF_RTP_RECV_PORT,
               LinkType.UNIT_TO_UNIT_CALLING_SERVING_TO_CALLING_HOME);
         smfSession.smfTransmitter.addListener(this);

         mmfSession = pttManager.createTestMmfSession(MMF_RTP_RECV_PORT,
               "127.0.0.1", SMF_RTP_RECV_PORT,
               LinkType.UNIT_TO_UNIT_CALLING_HOME_TO_CALLING_SERVING,null);
         mmfSession.getMmfReceiver().addListener(this);
         mmfSession.getMmfReceiver().setIgnoreRequest();

      } catch (Exception ex) {
         ex.printStackTrace();
         fail("Unexpected exception");
      }

      // Start SMF normally with a spurt request
      try {
         List<IMBEVoiceBlock> imbeVoiceBlocks = IMBEVoiceDataGenerator
               .getIMBEVoiceData("voice/imbe-hex-test.txt", NUM_AUDIO_PACKETS);
         smfSession.smfTransmitter.setRqstUnstoppableTrigger();
         smfSession.smfTransmitter.sendSpurtRequest(imbeVoiceBlocks, 3, 0, imbeVoiceBlocks.size());

      } catch (Exception e) {
         e.printStackTrace();
         fail("Unexpected exception");
      }

      try {
         Thread.sleep(10000); // IMPORTANT! Make sure this is long enough!
      } catch (InterruptedException ie) {
         // ie.printStackTrace();
      }
   }

   public void tearDown() throws Exception {

      smfSession.shutDown();
      logger.debug("Checking assertions...");
      
      /* Check SmfTx assertions */
      assertTrue(gotGrant == false);
      assertTrue(gotDeny == false);
      assertTrue(gotWait == false);

      /* Check MmfRx assertions */
      assertTrue(gotRequest == false);
      assertTrue(gotRequestWithVoice == false);
      assertTrue(gotAudio == true);
      assertTrue(gotAudioPackets == NUM_AUDIO_PACKETS);
      assertTrue(gotSpurtEnd == true);
      assertTrue(audioTimeout == false);
      logger.debug("DONE: All assertions passed.");

   }

   /***************************************************************************
    * SmfTxListener Methods
    **************************************************************************/
   public void receivedPttGrant(SmfPacketEvent event) {
      logger.debug("[SmfTxListener] GOT GRANT");
      gotGrant = true;
   }

   public void receivedPttDeny(SmfPacketEvent event) {
      logger.debug("[SmfTxListener] GOT DENY");
      gotDeny = true;
   }

   public void receivedPttWait(SmfPacketEvent event) {
      logger.debug("[SmfTxListener] GOT WAIT");
      gotWait = true;
   }

   public void requestTimeout() {
      logger.debug("[SmfTxListener] Request Timeout");
   }

   public void waitTimeout() {
      logger.debug("[SmfTxListener] Wait Timeout");
   }

   /***************************************************************************
    * MmfRxListener Methods
    **************************************************************************/
   public void receivedPttRequest(MmfPacketEvent event) {
      logger.debug("[MmfRxListener] GOT REQUEST");
      gotRequest = true;
      mmfSession.getMmfReceiver().arbitrate(event);
   }
   
   public void receivedPttRequestWithVoice(MmfPacketEvent event) {
      logger.debug("[MmfRxListener] GOT REQUEST WITH VOICE");
      gotRequestWithVoice = true;
      mmfSession.getMmfReceiver().arbitrate(event);
   }

   public void receivedPttProgress(MmfPacketEvent event) {
      logger.debug("[MmfRxListener] GOT PROGRESS");
      gotAudio = true;
      gotAudioPackets++;
   }

   public void receivedPttEnd(MmfPacketEvent event) {
      logger.debug("[MmfRxListener] GOT SPURT END");
      gotSpurtEnd = true;
   }
   
   public void audioTimeout() {
      logger.debug("[MmfRxListener] AUDIO TIMEOUT");
      audioTimeout = true;
   }
}
