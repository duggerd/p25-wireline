//
package gov.nist.p25.issi.transctlmgr.ptt;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;
import junit.framework.TestCase;

/**
 * Tests connection maintenance heartbeats for both SMF and MMF. 
 * Note that we do not conduct any timing tests.
 */
public class Test_HeartbeatTest extends TestCase 
     implements HeartbeatListener {

   private static Logger logger = Logger.getLogger("gov.nist.p25.issi.transctlmgr");

   /** SMF RTP receive port (WARNING: port may already be in use!) */
   private static final int SMF_RTP_RECV_PORT = 1842;

   /** MMF RTP receive port (WARNING: port may already be in use!) */
   private static final int MMF_RTP_RECV_PORT = 1844;

   private SmfSession smfSession = null;
   private PttSessionInterface mmfSession = null;
   private PttManager pttManager = new PttManager("127.0.0.1");

   static {
      PropertyConfigurator.configure("log4j.properties");
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }
   
   // HeartbeatListener flags
   private boolean gotHeartbeat = false;
   private boolean gotCorrectHeartbeatTSN = false;
   private boolean gotCorrectHeartbeatQueryTSN = false;
   private boolean gotHeartbeatTimeout = false;
   private boolean gotHeartbeatQuery = false;
   
   public void test() {
      
      System.out.println("=============================================\n" + 
         "File: Test_HeartbeatTest\n" +
         "Description: Tests connection maintenance heartbeats for both SMF and MMF\n" + 
         "Reference: Sections 7.4 and 7.5.1\n\n" + 
         
         "SMF  <- HEARTBEAT QUERY <- MMF \n" + 
         "SMF  -> HEARTBEAT TSN=0 -> MMF \n" + 
         "SMF  <- HEARTBEAT TSN=0 <- MMF (every Theartbeat seconds)\n" + 
         "SMF  -> HEARTBEAT TSN=0 -> MMF (every Theartbeat seconds)\n" +       
         "---------------------------------------------\n");
      try {
         smfSession = pttManager.createTestSmfSession(SMF_RTP_RECV_PORT,
               "127.0.0.1", MMF_RTP_RECV_PORT,
               LinkType.UNIT_TO_UNIT_CALLING_SERVING_TO_CALLING_HOME);

         smfSession.getHeartbeatReceiver().setHeartbeatListener(this);
         mmfSession = pttManager.createTestMmfSession(MMF_RTP_RECV_PORT,
               "127.0.0.1", SMF_RTP_RECV_PORT,
               LinkType.UNIT_TO_UNIT_CALLING_HOME_TO_CALLING_SERVING,this);
         
      } catch (Exception ex) {
         ex.printStackTrace();
         fail("Unexpected exception");
      }
      
      try {
         Thread.sleep(4000);  // IMPORTANT! Make sure this is long enough!
      } catch (InterruptedException ie) {
         ie.printStackTrace();
      }
   }
   
   public void tearDown() throws Exception {
      smfSession.shutDown();
      mmfSession.shutDown();
      logger.debug("Checking assertions...");

      /* Check Heartbeat assertions */
      assertTrue("did not get heartbeat ", gotHeartbeat == true);
      assertTrue("correct sequence number check", gotCorrectHeartbeatTSN == true);
      assertTrue("query" , gotHeartbeatQuery == true);
      assertTrue("query tsn ", gotCorrectHeartbeatQueryTSN == true);
      assertTrue("timeout", gotHeartbeatTimeout == false);
      logger.debug("DONE: All assertions passed.");
   }
   
   // HeartbeatListener Methods
   public void receivedHeartbeat(int TSN) {
      logger.debug("[HeartbeatListener] GOT HEARTBEAT");
      gotHeartbeat = true;
      if (TSN == 0)
         gotCorrectHeartbeatTSN = true;
   }
   
   public void receivedHeartbeatQuery(PttSessionInterface pttSession, int TSN) {
      logger.debug("[HeartbeatListener] GOT HEARTBEAT QUERY");
      if ("SMF".equals(pttSession.getSessionType()));  // MMF always sends HBQ on start
         gotHeartbeatQuery = true;
      if (TSN == 0)
         gotCorrectHeartbeatQueryTSN = true;
   }
   
   public void heartbeatTimeout(PttSessionInterface pttSession) {
      logger.debug("[HeartbeatListener] GOT HEARTBEAT TIMEOUT");
      gotHeartbeatTimeout = true;
   }
}
