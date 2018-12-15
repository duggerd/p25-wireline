//
//package gov.nist.p25.issi.transctlmgr;
package gov.nist.p25.issi.rfss;

import java.io.IOException;
import java.util.List;

import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.p25payload.IMBEVoiceBlock;
import gov.nist.p25.issi.p25payload.TransmitPriorityType;
import gov.nist.p25.issi.rfss.GroupHome;
import gov.nist.p25.issi.rfss.GroupServing;
import gov.nist.p25.issi.rfss.RFSS;
import gov.nist.p25.issi.transctlmgr.ptt.PttSession;
import gov.nist.p25.issi.transctlmgr.ptt.SmfSession;

import org.apache.log4j.Logger;

/**
 * The Transmission control SAP.
 * 
 */
public class TransmissionControlSAP {

   private static Logger logger = Logger.getLogger(TransmissionControlSAP.class);

   private PttSession pttSession;
   private RFSS rfss;
   //private SuConfig suConfig;

   public TransmissionControlSAP(RFSS rfss, SuConfig suConfig) {
      this.rfss = rfss;
      //this.suConfig = suConfig;
   }

   public void setPttSession(PttSession pttSession) {
      logger.debug("TrasnmissionControlManager: setPttSession " + pttSession);
      this.pttSession = pttSession;
      assert pttSession.getRfss() == this.rfss;
   }

   /**
    * This is a placeholder method. The PTT state machine will be used to send
    * out the talk spurt.
    * 
    * @param talkSpurt --
    *            byte array to transmit.
    * @throws IOException
    */
   public void sendTalkSpurt(List<IMBEVoiceBlock> imbeVoices, 
      int systemId, int unitId,
      TransmitPriorityType priorityType, int priorityLevel, int nblocks)
      throws IOException {

      try {
         if (logger.isDebugEnabled())
            logger.debug("TransmissionControlSAP : sendTalkSpurt "
                  + priorityType + " priorityLevel " + priorityLevel);
         SmfSession smfSession = (SmfSession) pttSession;
         smfSession.getSmfTransmitter().sendSpurtRequest(imbeVoices, systemId, unitId,
               nblocks);
         smfSession.setTransmitPriorityType(priorityType);
         smfSession.setTransmitPriorityLevel(priorityLevel);

      } catch (Exception ex) {
         logger.error("Unexpected error sending message", ex);
         throw new IOException(ex.getMessage());
      }
   }

   /**
    * Force a talk spurt to a peer SU.
    * 
    * @param imbeVoiceBlocks --
    *            blocks to send.
    * @param suId --
    *            the su id.
    * @param priorityType --
    *            priority type.
    * @param priorityLevel --
    *            priority level.
    * @param nblocks -
    *            number of blocks to send.
    * @throws IOException
    */
   public void forceTalkSpurt(List<IMBEVoiceBlock> imbeVoiceBlocks,
      int systemId, int suId, TransmitPriorityType priorityType, 
      int priorityLevel, int nblocks)
      throws IOException {

      try {
         SmfSession smfSession = (SmfSession) pttSession;
         assert smfSession != null;
         smfSession.getSmfTransmitter().forceAudio(imbeVoiceBlocks, systemId,
               suId, nblocks);
         smfSession.setTransmitPriorityType(priorityType);
         smfSession.setTransmitPriorityLevel(priorityLevel);

      } catch (Exception ex) {
         logger.error("Unexpected error sending message", ex);
         throw new IOException(ex.getMessage());
      }
   }

   /**
    * Send a talk spurt to the given group Id (given by its radical name )
    * 
    * @param talkSpurt -
    *            a list of voice blocks read from a file.
    * @param groupId -
    *            the group radical name.
    * @param ptype -
    *            the transmit priority type.
    * @param plevel -
    *            the priority level.
    */
   public void sendTalkSpurt(List<IMBEVoiceBlock> imbeVoices,
         String groupRadicalName, SuConfig suConfig,
         TransmitPriorityType ptype, int plevel, int nblocks)
         throws Exception {
      // Construct an ISSI header word
      GroupServing groupServing = rfss.getServedGroupByRadicalName(groupRadicalName);
      if (groupServing == null)
         throw new IOException("Group Not found " + groupRadicalName);

      if (!groupServing.getMultiplexer().isSuSubscribed( suConfig.getRadicalName())) {
         rfss.subscribeSuToServedGroup(groupRadicalName, suConfig);
      }
      int tsn = groupServing.getMultiplexer().getTsn( suConfig.getRadicalName());
      SmfSession smfSession = groupServing.getSmfSession(tsn);
      if (logger.isDebugEnabled()) {

         logger.debug("Sending talk spurt from RFSS "
               + rfss.getRfssConfig().getRfssName());
         logger.debug("TransmissionControlSAP : sendTalkSpurt " + ptype
               + " priorityLevel " + plevel);
      }
      if (smfSession == null)
         throw new IOException("No SMF session in table !");

      smfSession.setTransmitPriorityLevel(plevel);
      smfSession.setTransmitPriorityType(ptype);
      smfSession.getSmfTransmitter().sendSpurtRequest(imbeVoices,
            suConfig.getSystemId(), suConfig.getSuId(), nblocks);
   }
   
   /**
    * Force a talk spurt to the group.
    * 
    * @param imbeVoices -- canned list of blocks.
    * @param groupRadicalName -- radical name of group to send the talk spurt to.
    * @param suConfig -- sending SU.
    * @param ptype -- priorty type.
    * @param plevel -- priority level.
    * @param nblocks -- number of blocks to send.
    * @throws Exception
    */
   public void forceTalkSpurt(List<IMBEVoiceBlock> imbeVoices,
         String groupRadicalName, SuConfig suConfig,
         TransmitPriorityType ptype, int plevel, int nblocks)
         throws Exception {

      GroupServing groupServing = rfss.getServedGroupByRadicalName(groupRadicalName);
      if (groupServing == null)
         throw new IOException("Group Not found " + groupRadicalName);

      if (!groupServing.getMultiplexer().isSuSubscribed( suConfig.getRadicalName())) {
         rfss.subscribeSuToServedGroup(groupRadicalName, suConfig);
      }
      int tsn = groupServing.getMultiplexer().getTsn( suConfig.getRadicalName());
      SmfSession smfSession = groupServing.getSmfSession(tsn);
      if (logger.isDebugEnabled()) {

         logger.debug("Sending talk spurt from RFSS "
               + rfss.getRfssConfig().getRfssName());
         logger.debug("TransmissionControlSAP : sendTalkSpurt " + ptype
               + " priorityLevel " + plevel);
      }
      if (smfSession == null)
         throw new IOException("No SMF session in table !");

      smfSession.setTransmitPriorityLevel(plevel);
      smfSession.setTransmitPriorityType(ptype);
      smfSession.getSmfTransmitter().forceAudio(imbeVoices,
         suConfig.getSystemId(), suConfig.getSuId(), nblocks);
   }

   /**
    * Tear down the RTP session.
    */ 
   public void teardownRtpSession() {
      logger.debug("teardownRtpSession");
      if( pttSession != null)
         pttSession.shutDown();
   }

   //========================================
   public void sendMute(String groupRadicalName, SuConfig suConfig)
      throws Exception
   {
      GroupServing groupServing = rfss.getServedGroupByRadicalName(groupRadicalName);
      if (groupServing == null) {
         throw new IOException("sendMute: GroupServing not found " + groupRadicalName);
      }
      int tsn = groupServing.getMultiplexer().getTsn( suConfig.getRadicalName());
      logger.debug("sendMute: TSN="+tsn+" suConfig="+suConfig);
      
      int currentTSN = groupServing.getMultiplexer().getCurrentTSN();
      logger.debug("sendMute: currentTSN="+currentTSN);

      SmfSession smfSession = groupServing.getSmfSession(tsn);
      assert smfSession != null;
      smfSession.sendMute(currentTSN);
   }

   public void sendUnmute(String groupRadicalName, SuConfig suConfig) 
      throws Exception
   {
      GroupServing groupServing = rfss.getServedGroupByRadicalName(groupRadicalName);
      if (groupServing == null) {
         throw new IOException("sendUnmute: Group Not found " + groupRadicalName);
      }
      int tsn = groupServing.getMultiplexer().getTsn( suConfig.getRadicalName());
      logger.debug("sendMute: TSN="+tsn+" suConfig="+suConfig);

      int currentTSN = groupServing.getMultiplexer().getCurrentTSN();
      logger.debug("sendMute: currentTSN="+currentTSN);

      SmfSession smfSession = groupServing.getSmfSession(tsn);
      assert smfSession != null;
      smfSession.sendUnmute(currentTSN);
   }
}
