//
package gov.nist.p25.issi.fsm;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

//import gov.nist.p25.common.util.FileUtility;

import gov.nist.p25.issi.constants.IvsDelaysConstants;
import gov.nist.p25.issi.message.XmlAllmessages;
import gov.nist.p25.issi.message.XmlIvsDelays;
import gov.nist.p25.issi.message.XmlIvsDelaysSpec;
import gov.nist.p25.issi.p25payload.PacketType;
import gov.nist.p25.issi.setup.IvsDelaysHelper;

import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.Message;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.PttPacket;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.PttPacket.RtpHeader;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.PttPacket.P25Payload;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.PttPacket.P25Payload.BlockHeader;
import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.PttPacket.P25Payload.IssiPacketType;

import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd;

/**
 * ISSI Message Analyzer
 */
@SuppressWarnings("deprecation")
public class IssiMessageAnalyzer
{
   private static Logger logger = Logger.getLogger(IssiMessageAnalyzer.class);
   public static void showln(String s) { System.out.println(s); }

   private static String KEY_ANY = "*";
   private static int MAX_FSMS = 3;

   private boolean verbose = true;
   private int mask;
   private String userTag;
   private LinkedHashMap<String,IssiMessageFsm> fsmMap;
   private LinkedHashMap<String,Long> sumMap;
   private long[] avg = new long[4];

   private boolean isSrcConsole = true;
   private boolean isDstConsole = false;
   private String perfType;
   private String keySIP;
   private PacketType keyPTT;

   // accessor
   public int getMask() { return mask; }
   public String getUserTag() { return userTag; }
   public void setUserTag(String userTag) { this.userTag=userTag; }

   public Map<String,IssiMessageFsm> getFsmMap() { return fsmMap; }
   public Map<String,Long> getSummaryMap() { return sumMap; }

   public boolean getSrcConsole() { return isSrcConsole; }
   public void setSrcConsole(boolean bflag) { isSrcConsole=bflag; }

   public boolean getDstConsole() { return isDstConsole; }
   public void setDstConsole(boolean bflag) { isDstConsole=bflag; }

   public String getPerfType() { return perfType; }
   public void setPerfType(String perfType) { this.perfType=perfType; }

   // constructor
   //public IssiMessageAnalyzer() {
   //   this( KEY_ANY);
   //}
   public IssiMessageAnalyzer(String keySIP) {
      this( IvsDelaysConstants.TAG_TYPE_ISSI, 
            keySIP, PacketType.PTT_TRANSMIT_REQUEST);
   }
   public IssiMessageAnalyzer(String perfType, String keySIP) {
      this( perfType, keySIP, PacketType.PTT_TRANSMIT_REQUEST);
   }
   public IssiMessageAnalyzer(String perfType, String keySIP, PacketType keyPTT) {
      sumMap = new LinkedHashMap<String,Long>();
      this.perfType = perfType;
      this.keySIP = keySIP;
      this.keyPTT = keyPTT;
   }
   
   public IssiMessageFsm findFsmById( String fsmId) {
      IssiMessageFsm fsm = fsmMap.get( fsmId);
      if( fsm == null) {
         fsm = new IssiMessageFsm( fsmId);
         // NOTE: 0-relative index
         fsm.setIndex( fsmMap.size());
         fsmMap.put( fsmId, fsm);
      }
      return fsm;
   }

   public IssiMessageFsm findFsmByIndex( int index) {
      for( IssiMessageFsm fsm: fsmMap.values()) {
         showln("findFsmByIndex: fsm-Id: "+fsm.getId());
	 if( index == fsm.getIndex()) {
            return fsm;
	 }
      }
      return null;
   }


   public void setupFsmMap(Allmessages allmessages)
   {
      IssiMessageFsm fromFsm = null;
      IssiMessageFsm toFsm = null;

      showln("setupFsmMap(): START...");
      Message[] messageArray = allmessages.getMessageArray();
      for( int i=0; i < messageArray.length; i++)
      {
         Message message = messageArray[i];
         if( message==null) continue;
         long time = message.getTime();
         String firstLine = message.getFirstLine();
	 if( KEY_ANY.equals(keySIP) || firstLine.startsWith(keySIP))
         {
            showln("setupFsmMap(): keySIP="+keySIP);
            findFsmById( message.getFromRfssId());
            findFsmById( message.getToRfssId());
         }
      }  // for all-SIP

      //-------------------------------------------------------------------
      PttPacket[] pttPacketArray = allmessages.getPttPacketArray();
      for( int i=0; i < pttPacketArray.length; i++) {
         PttPacket pttPacket = pttPacketArray[i];
         if( pttPacket==null) continue;

         RtpHeader rtpHeader = pttPacket.getRtpHeader();
         P25Payload p25Payload = pttPacket.getP25Payload();
         IssiPacketType issiPacketType = p25Payload.getIssiPacketType();
         PacketType ptype = PacketType.getInstance(
            PacketType.getValueFromString( issiPacketType.getPacketType()));

         if( keyPTT == ptype) {
            findFsmById( pttPacket.getSendingRfssId());
            findFsmById( pttPacket.getReceivingRfssId());
         }
	 else if( keyPTT == null) {
            if((ptype == PacketType.PTT_TRANSMIT_REQUEST) ||
	       (ptype == PacketType.PTT_TRANSMIT_PROGRESS))
	    {
               showln("setupFsmMap(): PTT_TRANSMIT_REQUEST or PROGRESS");
               findFsmById( pttPacket.getSendingRfssId());
               findFsmById( pttPacket.getReceivingRfssId());
	    }
	 }
      }  // for all-PTT
      showln("setupFsmMap(): DONE...\n"+fsmMap);
   }

   /*** NOT USED
   public void analyze(int mask, String xmlMsg)
      throws XmlException, IOException, ParseException
   {
      this.mask = mask;
      this.userTag = "";
      fsmMap = new LinkedHashMap<String,IssiMessageFsm>();

      XmlAllmessages xmldoc = new XmlAllmessages();
      xmldoc.loadAllmessages( xmlMsg);
      Allmessages allmessages = xmldoc.getAllmessages();
      Message[] messageArray = allmessages.getMessageArray();

      // establish the order of FSM
      setupFsmMap(allmessages);

      boolean fromFlag;
      boolean toFlag;
      IssiMessageFsm fromFsm = null;
      IssiMessageFsm toFsm = null;
      IssiState currentState;

      for( int i=0; i < messageArray.length; i++)
      {
         Message message = messageArray[i];
         if( message==null) continue;
         long time = message.getTime();
         String firstLine = message.getFirstLine().trim();

         if( verbose) {
            logger.debug("Message: i="+i);
            logger.debug("    packetNumber: "+message.getPacketNumber());
            logger.debug("    from: "+message.getFrom());
            logger.debug("    to: "+message.getTo());
            logger.debug("    fromRfssId: "+message.getFromRfssId());
            logger.debug("    toRfssId: "+message.getToRfssId());
            logger.debug("    time: "+time);
            logger.debug("    firstLine: "+firstLine);
         }
	
         fromFsm = findFsmById( message.getFromRfssId());
         toFsm = findFsmById( message.getToRfssId());

         //--------------------------------------------------
	 // we need to check the FSM index to limit to 4
	 if( fromFsm.getIndex() > 3 || toFsm.getIndex() > 3) {
            logger.debug( "FSM index out of range: "+fromFsm.getId()+":"+toFsm.getId());
            continue;
         }
         //--------------------------------------------------
         if( firstLine.startsWith("INVITE")) {
            userTag = XmlAllmessages.getUserTag( firstLine);
            //showln("    userTag: "+userTag);
            showln("INVITE-firstLine=["+ firstLine+"]");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.INVITE_EXIT);
            showln("INVITE-fromFlag="+ fromFlag);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.INVITE_ENTRY);
               logger.debug("INVITE-toFlag: "+toFlag);
               showln("INVITE-toFlag: "+toFlag);
            }
         }
         else if( firstLine.startsWith("SIP")) {

            currentState = fromFsm.getCurrentState();
            showln("SIP-currentState="+ currentState);
            //showln("SIP-firstLine=["+ firstLine+"]");
            if( firstLine.endsWith("OK") ) {
               showln("SIP-OK-firstLine=["+ firstLine+"]");
               //if(currentState == IssiState.INVITE_ENTRY  || 
               //   currentState == IssiState.INVITE_EXIT ||
               //   currentState == IssiState.OK_ENTRY
	       // ) { 
                  fromFlag = fromFsm.processMessage(mask, time, IssiState.OK_EXIT);
                  showln("SIP-fromFlag="+ fromFlag);
                  if(fromFlag) {
                     toFlag = toFsm.processMessage(mask, time, IssiState.OK_ENTRY);
                     logger.debug("SIP-toFlag: "+toFlag);
                     showln("SIP-toFlag="+ toFlag);
                  }
	       //}
            }
            else if( firstLine.endsWith("Trying")) {
               // nothing to do
               showln("SIP-Trying");
            }
            else if( firstLine.endsWith("Ringing")) {
               // nothing to do
               showln("SIP-Ringing");
            }
         }
         else if( firstLine.startsWith("ACK")) {
            currentState = IssiState.ACK_EXIT;
            showln("ACK-currentState="+ currentState);
            showln("ACK-firstLine=["+ firstLine+"]");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.ACK_EXIT);
            showln("ACK-fromFlag="+ fromFlag);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.ACK_ENTRY);
               logger.debug("ACK-toFlag: "+toFlag);
               showln("ACK-toFlag="+ toFlag);
            }
         }
         //--------------------------------------------------
         if( verbose) {
            logger.debug("----------------------");
            logger.debug("fromFsm-Id: "+fromFsm.getId());
            logger.debug("fromFsm-CurrentState: "+fromFsm.getCurrentState());
            logger.debug("fromFsm-tgxMap: "+fromFsm.getDataMap());
            logger.debug("----------------------");
            logger.debug("toFsm-Id: "+toFsm.getId());
            logger.debug("toFsm-CurrentState: "+toFsm.getCurrentState());
            logger.debug("toFsm-tgxMap: "+toFsm.getDataMap());
            logger.debug("----------------------");
         }
         showln("-------------------------");

      }  // allmessages

      //-------------------------------------------------------------------
      PttPacket[] pttPacketArray = allmessages.getPttPacketArray();
      for( int i=0; i < pttPacketArray.length; i++) {
         PttPacket pttPacket = pttPacketArray[i];
         if( pttPacket==null) continue;

         RtpHeader rtpHeader = pttPacket.getRtpHeader();
         P25Payload p25Payload = pttPacket.getP25Payload();
         IssiPacketType issiPacketType = p25Payload.getIssiPacketType();

         fromFsm = findFsmById( pttPacket.getSendingRfssId());
         toFsm = findFsmById( pttPacket.getReceivingRfssId());

         if( verbose) {
            logger.debug("PttPacket: i="+i);
            logger.debug("    packetNumber: "+pttPacket.getPacketNumber());
            logger.debug("    receptionTime: "+pttPacket.getReceptionTime());
            logger.debug("    sendingRfssId: "+pttPacket.getSendingRfssId());
            logger.debug("    receivingRfssId: "+pttPacket.getReceivingRfssId());
            logger.debug("    rtp-payloadType: "+rtpHeader.getPayloadType());
            BlockHeader[] blockHeaderArray = p25Payload.getBlockHeaderArray();
            for( BlockHeader blockHeader: blockHeaderArray) {
               logger.debug("    p25-blockHeader-blockType: "+blockHeader.getBlockType());
            }
            logger.debug("    p25-issiPacketType-packetType: "+issiPacketType.getPacketType());
         }   // verbose

	 // we need to check the FSM index to limit to 4
	 if( fromFsm.getIndex() > 3 || toFsm.getIndex() > 3) {
             logger.debug( "FSM index out of range: "+fromFsm.getId()+":"+toFsm.getId());
             continue;
         }

         //--------------------------------------
         long time = pttPacket.getReceptionTime();
         PacketType ptype = PacketType.getInstance(
            PacketType.getValueFromString( issiPacketType.getPacketType()));
         if( ptype == PacketType.HEARTBEAT) {
            //showln("see PttPacket: HEARTBEAT");
         }
         else if( ptype == PacketType.HEARTBEAT_QUERY) {
            //showln("see PttPacket: HEARTBEAT_QUERY");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_REQUEST) {
            //showln("see PttPacket: PTT_TRANSMIT_REQUEST");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_REQUEST_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_REQUEST_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_GRANT) {
            //showln("see PttPacket: PTT_TRANSMIT_GRANT");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_GRANT_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_GRANT_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_PROGRESS) {
            //showln("see PttPacket: PTT_TRANSMIT_PROGRESS");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_START) {
            //showln("see PttPacket: PTT_TRANSMIT_START => PTT_TRANSMIT_PROGRESS");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_END) {
            //showln("see PttPacket: PTT_TRANSMIT_END");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_MUTE) {
            //showln("see PttPacket: PTT_TRANSMIT_MUTE");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_UNMUTE) {
            //showln("see PttPacket: PTT_TRANSMIT_UNMUTE");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_WAIT) {
            //showln("see PttPacket: PTT_TRANSMIT_WAIT");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_DENY) {
            //showln("see PttPacket: PTT_TRANSMIT_DENY");
         }
         //--------------------------------------------------
         if( verbose) {
            logger.debug("----------------------");
            logger.debug("fromFsm-Id: "+fromFsm.getId());
            logger.debug("fromFsm-CurrentState: "+fromFsm.getCurrentState());
            logger.debug("fromFsm-tgxMap: "+fromFsm.getDataMap());
            logger.debug("----------------------");
            logger.debug("toFsm-Id: "+toFsm.getId());
            logger.debug("toFsm-CurrentState: "+toFsm.getCurrentState());
            logger.debug("toFsm-tgxMap: "+toFsm.getDataMap());
            logger.debug("----------------------");
         }
      }
   }
    ****/

   public void showResults() {
      showln("\n===============================");
      showln("showResults: userTag: "+getUserTag());
      for( IssiMessageFsm fsm: fsmMap.values()) {
         showln("  fsm-Id: "+fsm.getId());
         showln("  fsm-index: "+fsm.getIndex());
         showln("  fsm-mapSize: "+fsm.getDataMap().size());
         showln("  fsm-tgxMap: "+fsm.getDataMap());
         showln("-------------");
      }
   }

   //--------------------------------------------------------------------------
   public XmlIvsDelays calculate(int mask, boolean isConfirmed, boolean isDirectCall) 
      throws Exception
   {
      XmlIvsDelays alldoc = new XmlIvsDelays();
      IvsDelays ivsDelays = alldoc.getIvsDelays();
      String title = perfType + " Performance Analysis";
      String comment = "Process on: " + new Date().toString() + "\n";
      alldoc.setIvsStatusComment( title, comment);

//EHC: need a new ftn to determine isSrcConsole, isDstConsole !!!
//===showResults();

      XmlIvsDelays xmldoc = null;
      if( (mask & IssiMessageFsm.MASK_GCSD) == IssiMessageFsm.MASK_GCSD) { 
         xmldoc = calculateGCSD( isConfirmed);
	 ivsDelays.setIvsGcsd( xmldoc.getIvsDelays().getIvsGcsd());
         
	 comment = xmldoc.getIvsStatusComment();
	 alldoc.setIvsStatusComment( comment);
      }
      if( (mask & IssiMessageFsm.MASK_UCSD) == IssiMessageFsm.MASK_UCSD) { 
         xmldoc = calculateUCSD( isDirectCall);
	 ivsDelays.setIvsUcsd( xmldoc.getIvsDelays().getIvsUcsd());

	 comment = xmldoc.getIvsStatusComment();
	 alldoc.setIvsStatusComment( comment);
      }
      if( (mask & IssiMessageFsm.MASK_GMTD) == IssiMessageFsm.MASK_GMTD) { 
         xmldoc = calculateGMTD( );
	 ivsDelays.setIvsGmtd( xmldoc.getIvsDelays().getIvsGmtd());

	 comment = xmldoc.getIvsStatusComment();
	 alldoc.setIvsStatusComment( comment);
      }
      if( (mask & IssiMessageFsm.MASK_UMTD) == IssiMessageFsm.MASK_UMTD) { 
         xmldoc = calculateUMTD( );
	 ivsDelays.setIvsUmtd( xmldoc.getIvsDelays().getIvsUmtd());

	 comment = xmldoc.getIvsStatusComment();
	 alldoc.setIvsStatusComment( comment);
      }
      return alldoc;
   }

   //--------------------------------------------------------------------------
   public void calculateGCSDTgx( IvsGcsd ivsGcsd, StringBuffer sbuf,
         IssiMessageFsm fsm, String tgname)
   {
      String id = fsm.getId();
      Map<String,Long> map = fsm.getDataMap();
      Long t0 = map.get("start_"+tgname); 
      Long t1 = map.get("end_"+tgname); 
      if( t0 != null && t1 != null) {
         long tt = t1.longValue() - t0.longValue();
         doSummation( sumMap, tgname, tt, avg);
         IvsDelaysHelper.setGCSDParameter(ivsGcsd, tgname, (int)avg[0], (int)avg[1], true);
	 if( tt < 0) {
            sbuf.append( id+": *** Negative time - "+tgname +", error in calculation ?\n");
         }
      } else {
         sbuf.append( id+": Missing "+tgname +", use spec value.\n");
      }
   }
   public void calculateGMTDTgx( IvsGmtd ivsGmtd, StringBuffer sbuf,
         IssiMessageFsm fsm, String tgname)
   {
      String id = fsm.getId();
      Map<String,Long> map = fsm.getDataMap();
      Long t0 = map.get("start_"+tgname); 
      Long t1 = map.get("end_"+tgname); 
      if( t0 != null && t1 != null) {
         long tt = t1.longValue() - t0.longValue();
         doSummation( sumMap, tgname, tt, avg);
         IvsDelaysHelper.setGMTDParameter(ivsGmtd, tgname, (int)avg[0], (int)avg[1], true);
	 if( tt <= 0) {
            sbuf.append( id+": *** Negative time - "+tgname +", error in calculation ?\n");
         }
      } else {
         sbuf.append( id+": Missing "+tgname +", use spec value.\n");
      }
   }
   //------------------------
   public void calculateUCSDTux( IvsUcsd ivsUcsd, StringBuffer sbuf,
         IssiMessageFsm fsm, String tgname)
   {
      String id = fsm.getId();
      Map<String,Long> map = fsm.getDataMap();
      Long t0 = map.get("start_"+tgname); 
      Long t1 = map.get("end_"+tgname); 
      if( t0 != null && t1 != null) {
         long tt = t1.longValue() - t0.longValue();
         doSummation( sumMap, tgname, tt, avg);
         IvsDelaysHelper.setUCSDParameter(ivsUcsd, tgname, (int)avg[0], (int)avg[1], true);
	 if( tt <= 0) {
            sbuf.append( id+": *** Negative time - "+tgname +", error in calculation ?\n");
         }
      } else {
         sbuf.append( id+": Missing "+tgname +", use spec value.\n");
      }
   }
   public void calculateUMTDTux( IvsUmtd ivsUmtd, StringBuffer sbuf,
         IssiMessageFsm fsm, String tgname)
   {
      String id = fsm.getId();
      Map<String,Long> map = fsm.getDataMap();
      Long t0 = map.get("start_"+tgname); 
      Long t1 = map.get("end_"+tgname); 
      if( t0 != null && t1 != null) {
         long tt = t1.longValue() - t0.longValue();
         doSummation( sumMap, tgname, tt, avg);
         IvsDelaysHelper.setUMTDParameter(ivsUmtd, tgname, (int)avg[0], (int)avg[1], true);
	 if( tt <= 0) {
            sbuf.append( id+": *** Negative time - "+tgname +", error in calculation ?\n");
         }
      } else {
         sbuf.append( id+": Missing "+tgname +", use spec value.\n");
      }
   }

   //--------------------------------------------------------------------------
   //--------------------------------------------------------------------------
   public XmlIvsDelays calculateGCSD( boolean isConfirmed) throws Exception
   {
      logger.debug("calculateGCSD: isConfirmed="+isConfirmed);
      //gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd.Parameter gcsdParam;
      //showResults();

      String xmlFile = IvsDelaysConstants.getUserIvsDelaysFile(perfType,
         isSrcConsole, isDstConsole, IvsDelaysConstants.TAG_SETUP_GCSD);
      showln("calculateGCSD: xmlFile="+xmlFile);
      XmlIvsDelays xmldoc = new XmlIvsDelays( xmlFile);
      IvsDelays ivsDelays = xmldoc.getIvsDelays();

      IvsGcsd ivsGcsd = ivsDelays.getIvsGcsd();
      ivsGcsd.setTitle("Performance Calculations for Group Call Setup Delay(GCSD)");

      StringBuffer sbuf = new StringBuffer("\n");

      // replace the calculated parameters
      // check if measurements are valid
      if( fsmMap.size() != 3) {
         sbuf.append("Number of RFSSes: "+fsmMap.size());
         sbuf.append("\nRFSSes is not equal to 3. Measurements may be invalid.\n");
      }

      // process each FSM, mark the parameters as calculated
      for( IssiMessageFsm fsm: fsmMap.values()) {

         //Map<String,Long> map = fsm.getDataMap();
         String id = fsm.getId();
         //showln("fsm-Id: "+id);
         //showln("fsm-mapSize: "+map.size());
         //showln("fsm-tgxMap: "+map);

         // calculate measurements
         if( fsm.getIndex() == 0) {
            // tg1, tg5, tg10
            if( IvsDelaysConstants.TAG_TYPE_ISSI.equals(perfType)) {
               sbuf.append( id+": Missing Tg1, use spec value.\n");
               sbuf.append( id+": Missing Tg10, use spec value.\n");
            }
            else if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
               // CSSI- isSrcConsole tg1, tg10 = 0
               if( isSrcConsole) {
                  IvsDelaysHelper.setGCSDParameter(ivsGcsd, "Tg1", 0, -1, true);
                  IvsDelaysHelper.setGCSDParameter(ivsGcsd, "Tg10", 0, -1, true);
               }
            }
            calculateGCSDTgx( ivsGcsd, sbuf, fsm, "Tg5");
	 }
         else if( fsm.getIndex() == 1) {
            // tg2, tg3, tg6, tg7, tg8, tg9
            calculateGCSDTgx( ivsGcsd, sbuf, fsm, "Tg2");
            calculateGCSDTgx( ivsGcsd, sbuf, fsm, "Tg3");
            calculateGCSDTgx( ivsGcsd, sbuf, fsm, "Tg6");
            calculateGCSDTgx( ivsGcsd, sbuf, fsm, "Tg7");
            calculateGCSDTgx( ivsGcsd, sbuf, fsm, "Tg8");
            calculateGCSDTgx( ivsGcsd, sbuf, fsm, "Tg9");
	 }
         else if( fsm.getIndex() == 2) {
            // tg4, tg11
            if( IvsDelaysConstants.TAG_TYPE_ISSI.equals(perfType)) {
               sbuf.append( id+": Missing Tg11, use spec value.\n");
            }
            else if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
               // CSSI- isDstConsole tg11 = 0
               if( isDstConsole) {
                  IvsDelaysHelper.setGCSDParameter(ivsGcsd, "Tg11", 0, -1, true);
               }
            }
            calculateGCSDTgx( ivsGcsd, sbuf, fsm, "Tg4");
         }
      }

      // replace the user measurements
      IvsDelays extDelays = XmlIvsDelaysSpec.getUserIvsDelays(perfType,isSrcConsole,isDstConsole).getIvsDelays();
      xmldoc.getAllParameters(IvsDelaysConstants.TAG_IVS_GCSD, extDelays);

      // confirmed group call
      IvsDelaysHelper.setGCSDComposite(perfType, isSrcConsole, isDstConsole, xmldoc, isConfirmed);

      // comment + missing parameters
      String title = "Performanace Calculations for "+perfType+" Voice Services";
      String comment = "Performanace Calculations: "+ new Date();
      comment += (isConfirmed ? " Confirmed Group Call." : " Unconfirmed Group Call.");
      comment += "\n" + XmlIvsDelaysSpec.getCondition(perfType, isSrcConsole, isDstConsole);

      // append the errorText
      comment += sbuf.toString();
      xmldoc.setIvsStatusComment( title, comment);
      return xmldoc;
   }

   public XmlIvsDelays calculateGMTD( ) throws Exception
   {
      logger.debug("calculateGMTD: START...");
      //gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd.Parameter gmtdParam;

      String xmlFile = IvsDelaysConstants.getUserIvsDelaysFile(perfType,
         isSrcConsole, isDstConsole, IvsDelaysConstants.TAG_SETUP_GMTD);
      showln("calculateGMTD: xmlFile="+xmlFile);
      XmlIvsDelays xmldoc = new XmlIvsDelays( xmlFile);
      IvsDelays ivsDelays = xmldoc.getIvsDelays();

      IvsGmtd ivsGmtd = ivsDelays.getIvsGmtd();
      ivsGmtd.setTitle("Performance Calculations for Group Call Message Transfer Delay(GMTD)");

      StringBuffer sbuf = new StringBuffer("\n");

      // replace the calculated parameters
      // check if measurements are valid
      if( fsmMap.size() != 3) {
         sbuf.append("Number of RFSSes: "+fsmMap.size());
         sbuf.append("\nRFSSes is not equal to 3. Measurements may be invalid.\n");
      }

      // process each FSM, mark the parameters as calculated
      for( IssiMessageFsm fsm: fsmMap.values()) {

         //Map<String,Long> map = fsm.getDataMap();
	 String id = fsm.getId();
         //showln("fsm-Id: "+id);
         //showln("fsm-mapSize: "+map.size());
         //showln("fsm-tgxMap: "+map);

         // calculate measurements
         if( fsm.getIndex() == 0) {
            // tg_ad1
            if( IvsDelaysConstants.TAG_TYPE_ISSI.equals(perfType)) {
               sbuf.append( id+": Missing Tg_ad1, use spec value.\n");
            }
            else if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
               showln("GMTD: set Tg_ad1=0");
               // CSSI- isSrcConsole tg_ad1 = 0
               if( isSrcConsole)
               IvsDelaysHelper.setGMTDParameter(ivsGmtd, "Tg_ad1", 0, -1, true);
            }
	 }
         else if( fsm.getIndex() == 1) {
            // tg_ad2
            calculateGMTDTgx( ivsGmtd, sbuf, fsm, "Tg_ad2");
	 }
         else if( fsm.getIndex() == 2) {
            // tg_ad3
            if( IvsDelaysConstants.TAG_TYPE_ISSI.equals(perfType)) {
               sbuf.append( id+": Missing Tg_ad3, use spec value.\n");
            }
            else if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
               showln("GMTD: set Tg_ad3=0");
               // CSSI- isDstConsole tg_ad3 = 0
               if( isDstConsole)
               IvsDelaysHelper.setGMTDParameter(ivsGmtd, "Tg_ad3", 0, -1, true);
            }
         }
      }

      // replace the user measurements
      IvsDelays extDelays = XmlIvsDelaysSpec.getUserIvsDelays(perfType,isSrcConsole,isDstConsole).getIvsDelays();
      xmldoc.getAllParameters(IvsDelaysConstants.TAG_IVS_GMTD, extDelays);

      // calculated parameters
      IvsDelaysHelper.setGMTDComposite(perfType, isSrcConsole, isDstConsole, xmldoc);

      // comment + missing parameters
      String title = "Performanace Calculations for "+perfType+" Voice Services";
      String comment = "Performanace Calculations: "+ new Date();

      comment += "\n" + XmlIvsDelaysSpec.getCondition(perfType, isSrcConsole, isDstConsole);

      // append the errorText
      comment += sbuf.toString();
      xmldoc.setIvsStatusComment( title, comment);
      return xmldoc;
   }

   //--------------------------------------------------------------------------
   public XmlIvsDelays calculateUCSD( boolean isDirectCall) throws Exception
   {
      logger.debug("calculateUCSD: isDirectCall="+isDirectCall);
      //gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd.Parameter ucsdParam;

      String xmlFile = IvsDelaysConstants.getUserIvsDelaysFile(perfType,
          isSrcConsole, isDstConsole, IvsDelaysConstants.TAG_SETUP_UCSD);
      showln("calculateUCSD: xmlFile="+xmlFile);
      XmlIvsDelays xmldoc = new XmlIvsDelays( xmlFile);
      IvsDelays ivsDelays = xmldoc.getIvsDelays();

      IvsUcsd ivsUcsd = ivsDelays.getIvsUcsd();
      ivsUcsd.setTitle("Performance Calculations for SU-to-SU Call Setup Delay(UCSD)");

      StringBuffer sbuf = new StringBuffer("\n");

      // replace the calculated parameters
      // check if measurements are valid
      if( fsmMap.size() != 4) {
         sbuf.append("Number of RFSSes: "+fsmMap.size());
         sbuf.append("\nRFSSes is not equal to 4. Measurements may be invalid.\n");
      }

      // process each FSM, mark the parameters as calculated
      for( IssiMessageFsm fsm: fsmMap.values()) {

         String id = fsm.getId();
         Map<String,Long> map = fsm.getDataMap();
         showln("fsm-Id: "+id);
         showln("fsm-mapSize: "+map.size());
         showln("fsm-tgxMap: "+map);

         // calculate measurements
         if( fsm.getIndex() == 0) {
            // tu1, tu9, tu10
            if( IvsDelaysConstants.TAG_TYPE_ISSI.equals(perfType)) {
               sbuf.append( id+": Missing Tu1, use spec value.\n");
               sbuf.append( id+": Missing Tu9, use spec value.\n");
            }
            else if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
               // CSSI- isSrcConsole tu1, tu9 = 0
               if( isSrcConsole) {
                  IvsDelaysHelper.setUCSDParameter(ivsUcsd, "Tu1", 0, -1, true);
                  IvsDelaysHelper.setUCSDParameter(ivsUcsd, "Tu9", 0, -1, true);
               }
            }
            calculateUCSDTux( ivsUcsd, sbuf, fsm, "Tu10");
	 }
         else if( fsm.getIndex() == 1) {
            // tu2, tu8, tu11
            calculateUCSDTux( ivsUcsd, sbuf, fsm, "Tu2");
            calculateUCSDTux( ivsUcsd, sbuf, fsm, "Tu8");
            calculateUCSDTux( ivsUcsd, sbuf, fsm, "Tu11");
	 }
         else if( fsm.getIndex() == 2) {
            // tu3, tu7, tu12
            calculateUCSDTux( ivsUcsd, sbuf, fsm, "Tu3");
            calculateUCSDTux( ivsUcsd, sbuf, fsm, "Tu7");
            calculateUCSDTux( ivsUcsd, sbuf, fsm, "Tu12");
	 }
         else if( fsm.getIndex() == 3) {
            // tu4, tu5, tu6, tu13
            if( IvsDelaysConstants.TAG_TYPE_ISSI.equals(perfType)) {
               sbuf.append( id+": Missing Tu4, use spec value.\n");
               sbuf.append( id+": Missing Tu5, use spec value.\n");
               sbuf.append( id+": Missing Tu13, use spec value.\n");
            }
            else if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
               // CSSI- isDstConsole tu4, tu5, tu13 = 0
               if( isDstConsole) {
                  IvsDelaysHelper.setUCSDParameter(ivsUcsd, "Tu4", 0, -1, true);
                  IvsDelaysHelper.setUCSDParameter(ivsUcsd, "Tu5", 0, -1, true);
                  IvsDelaysHelper.setUCSDParameter(ivsUcsd, "Tu13", 0, -1, true);
               }
            }
            calculateUCSDTux( ivsUcsd, sbuf, fsm, "Tu6");
         }
      }

      // replace the user measurements
      IvsDelays extDelays = XmlIvsDelaysSpec.getUserIvsDelays(perfType,isSrcConsole,isDstConsole).getIvsDelays();
      xmldoc.getAllParameters(IvsDelaysConstants.TAG_IVS_UCSD, extDelays);

      // calculated parameters
      IvsDelaysHelper.setUCSDComposite(perfType, isSrcConsole, isDstConsole, xmldoc, isDirectCall);

      // comment + missing parameters
      String title = "Performanace Calculations for "+perfType+" Voice Services";
      String comment = "Performanace Calculations: "+ new Date();
      comment += (isDirectCall ? " Direct Call." : " Availability Check.");
      comment += "\n" + XmlIvsDelaysSpec.getCondition(perfType, isSrcConsole, isDstConsole);

      // append the errorText
      comment += sbuf.toString();
      xmldoc.setIvsStatusComment( title, comment);
      return xmldoc;
   }

   public XmlIvsDelays calculateUMTD( ) throws Exception
   {
      logger.debug("calculateUMTD: START...");
      //gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd.Parameter umtdParam;

      String xmlFile = IvsDelaysConstants.getUserIvsDelaysFile(perfType,
         isSrcConsole, isDstConsole, IvsDelaysConstants.TAG_SETUP_UMTD);
      showln("calculateUMTD: xmlFile="+xmlFile);
      XmlIvsDelays xmldoc = new XmlIvsDelays( xmlFile);
      IvsDelays ivsDelays = xmldoc.getIvsDelays();

      IvsUmtd ivsUmtd = ivsDelays.getIvsUmtd();
      ivsUmtd.setTitle("Performance Calculations for SU-to-SU Call Message Transfer Delay(UMTD)");

      StringBuffer sbuf = new StringBuffer("\n");

      // replace the calculated parameters
      // check if measurements are valid
      if( fsmMap.size() != 4) {
         sbuf.append("Number of RFSSes: "+fsmMap.size());
         sbuf.append("\nRFSSes is not equal to 4. Measurements may be invalid.\n");
      }

      // process each FSM, mark the parameters as calculated
      for( IssiMessageFsm fsm: fsmMap.values()) {

         //Map<String,Long> map = fsm.getDataMap();
         String id = fsm.getId();
         //showln("fsm-Id: "+id);
         //showln("fsm-mapSize: "+map.size());
         //showln("fsm-tgxMap: "+map);

         // calculate measurements
         if( fsm.getIndex() == 0) {
            // tu_ad1, tu_ad8
            if( IvsDelaysConstants.TAG_TYPE_ISSI.equals(perfType)) {
               sbuf.append( id+": Missing Tu_ad1, use spec value.\n");
               sbuf.append( id+": Missing Tu_ad8, use spec value.\n");
            }
            else if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
               // CSSI- isSrcConsole tu_ad1, tu_ad8 = 0
               if( isSrcConsole) {
                  IvsDelaysHelper.setUMTDParameter(ivsUmtd, "Tu_ad1", 0, -1, true);
                  IvsDelaysHelper.setUMTDParameter(ivsUmtd, "Tu_ad8", 0, -1, true);
               }
            }
	 }
         else if( fsm.getIndex() == 1) {
            // tu_ad2, tu_ad7
            calculateUMTDTux( ivsUmtd, sbuf, fsm, "Tu_ad2");
            calculateUMTDTux( ivsUmtd, sbuf, fsm, "Tu_ad7");
	 }
         else if( fsm.getIndex() == 2) {
            // tu_ad3, tu_ad6
            calculateUMTDTux( ivsUmtd, sbuf, fsm, "Tu_ad3");
            calculateUMTDTux( ivsUmtd, sbuf, fsm, "Tu_ad6");
	 }
         else if( fsm.getIndex() == 3) {
            // tu_ad4, tu_ad5
            if( IvsDelaysConstants.TAG_TYPE_ISSI.equals(perfType)) {
               sbuf.append( id+": Missing Tu_ad4, use spec value.\n");
               sbuf.append( id+": Missing Tu_ad5, use spec value.\n");
            }
            else if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
               // CSSI- isDstConsole tu_ad4, tu_ad5 = 0
               if( isDstConsole) {
                  IvsDelaysHelper.setUMTDParameter(ivsUmtd, "Tu_ad4", 0, -1, true);
                  IvsDelaysHelper.setUMTDParameter(ivsUmtd, "Tu_ad5", 0, -1, true);
               }
            }
         }
      }

      // replace the user measurements
      IvsDelays extDelays = XmlIvsDelaysSpec.getUserIvsDelays(perfType,isSrcConsole,isDstConsole).getIvsDelays();
      xmldoc.getAllParameters(IvsDelaysConstants.TAG_IVS_UMTD, extDelays);

      // calculated parameters
      IvsDelaysHelper.setUMTDComposite(perfType, isSrcConsole, isDstConsole, xmldoc);

      // comment + missing parameters
      String title = "Performanace Calculations for "+perfType+" Voice Services";
      String comment = "Performanace Calculations: "+ new Date();
      comment += "\n" + XmlIvsDelaysSpec.getCondition(perfType, isSrcConsole, isDstConsole);

      // append the errorText
      comment += sbuf.toString();
      xmldoc.setIvsStatusComment( title, comment);
      return xmldoc;
   }

   //-------------------------------------------------------------------
   // do summation of delays and calculate average/maximum inline
   public static void doSummation(Map<String,Long> sumMap, String tagname,
      long time, long[] avg)
   {
      //TODO: use the getter/setter for average/maximum ?
      // current output:
      // avg[0] = average
      // avg[1] = maximum
      //
      // look for sum_tagname
      String key_max = "max"+tagname;
      String key_sum = "sum_"+tagname;
      String key_count = "count_"+tagname;

      Long max = sumMap.get( key_max);
      Long sum = sumMap.get( key_sum);
      Long count = sumMap.get( key_count);
      if( sum == null) {
         max =  new Long(time);
         sum =  new Long(time);
         count = new Long(1L);
         avg[0] = time;
         avg[1] = time;
      }
      else {
         if( time > max.longValue()) {
            max =  new Long( time);
         }
         sum =  new Long( sum.longValue()+time);
         count = new Long( count.longValue()+1);
         avg[0] = (long)(((double)sum.longValue()/(double)count.longValue())+0.5);
         avg[1] = max.longValue();
      }
      sumMap.put( key_max, max);
      sumMap.put( key_sum, sum);
      sumMap.put( key_count, count);
   }

   //-----------------------------------------------------------------------
   //-----------------------------------------------------------------------
   public void analyze(int mask, String xmlMsg)
      throws XmlException, IOException, ParseException
   {
      if( (mask & IssiMessageFsm.MASK_GCSD) == IssiMessageFsm.MASK_GCSD) { 
         analyzeGCSD( mask, xmlMsg);
      }
      if( (mask & IssiMessageFsm.MASK_UCSD) == IssiMessageFsm.MASK_UCSD) { 
         analyzeUCSD( mask, xmlMsg);
      }
      if( (mask & IssiMessageFsm.MASK_GMTD) == IssiMessageFsm.MASK_GMTD) { 
         analyzeGMTD( mask, xmlMsg);
      }
      if( (mask & IssiMessageFsm.MASK_UMTD) == IssiMessageFsm.MASK_UMTD) { 
         analyzeUMTD( mask, xmlMsg);
      }
   }
   //-----------------------------------------------------------------------
   public void analyzeGCSD(int mask, String xmlMsg)
      throws XmlException, IOException, ParseException
   {
      this.mask = mask;
      this.userTag = "";
      fsmMap = new LinkedHashMap<String,IssiMessageFsm>();

      XmlAllmessages xmldoc = new XmlAllmessages();
      xmldoc.loadAllmessages( xmlMsg);
      Allmessages allmessages = xmldoc.getAllmessages();
      Message[] messageArray = allmessages.getMessageArray();

      // establish the order of FSM
      setupFsmMap(allmessages);

      boolean fromFlag;
      boolean toFlag;
      IssiMessageFsm fromFsm = null;
      IssiMessageFsm toFsm = null;
      IssiState currentState;

      for( int i=0; i < messageArray.length; i++)
      {
         Message message = messageArray[i];
         if( message==null) continue;
         long time = message.getTime();
         String firstLine = message.getFirstLine();

         if( verbose) {
            logger.debug("analyzeGCSD(): Message: i="+i);
            logger.debug("    packetNumber: "+message.getPacketNumber());
            logger.debug("    from: "+message.getFrom());
            logger.debug("    to: "+message.getTo());
            logger.debug("    fromRfssId: "+message.getFromRfssId());
            logger.debug("    toRfssId: "+message.getToRfssId());
            logger.debug("    time: "+time);
            logger.debug("    firstLine: "+firstLine);
         }

         fromFsm = findFsmById( message.getFromRfssId());
         toFsm = findFsmById( message.getToRfssId());
         //--------------------------------------------------
        // check the FSM index limit 
        if( fromFsm.getIndex() >  MAX_FSMS || toFsm.getIndex() >  MAX_FSMS) {
             logger.debug( "FSM index out of range: "+fromFsm.getId()+":"+toFsm.getId());
             continue;
         }
         //--------------------------------------------------
         if( firstLine.startsWith("INVITE")) {
            userTag = XmlAllmessages.getUserTag( firstLine);
            //showln("    userTag: "+userTag);
            fromFlag = fromFsm.processMessage(mask, time, IssiState.INVITE_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.INVITE_ENTRY);
               logger.debug("    tpFlag: "+toFlag);
            }
         }
         else if( firstLine.startsWith("SIP")) {

            currentState = fromFsm.getCurrentState();
            //showln("fromFsm: currentState="+ currentState);
            if( firstLine.endsWith("OK") ) {
               if(currentState == IssiState.INVITE_ENTRY) {
                  fromFlag = fromFsm.processMessage(mask, time, IssiState.OK_EXIT);
                  if(fromFlag) {
                     toFlag = toFsm.processMessage(mask, time, IssiState.OK_ENTRY);
                  }
               }
            }
            else if( firstLine.endsWith("Trying")) {
               // nothing to do
            }
            else if( firstLine.endsWith("Ringing")) {
               // nothing to do
            }
         }
         else if( firstLine.startsWith("ACK")) {
            currentState = IssiState.ACK_EXIT;
            fromFlag = fromFsm.processMessage(mask, time, IssiState.ACK_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.ACK_ENTRY);
            }
         }
         //--------------------------------------------------
         if( verbose) {
            logger.debug("----------------------");
            logger.debug("fromFsm-Id: "+fromFsm.getId());
            logger.debug("fromFsm-CurrentState: "+fromFsm.getCurrentState());
            logger.debug("fromFsm-tgxMap: "+fromFsm.getDataMap());
            logger.debug("----------------------");
            logger.debug("toFsm-Id: "+toFsm.getId());
            logger.debug("toFsm-CurrentState: "+toFsm.getCurrentState());
            logger.debug("toFsm-tgxMap: "+toFsm.getDataMap());
            logger.debug("----------------------");
         }

      }  // allmessages

      //-------------------------------------------------------------------
      PttPacket[] pttPacketArray = allmessages.getPttPacketArray();
      for( int i=0; i < pttPacketArray.length; i++) {
         PttPacket pttPacket = pttPacketArray[i];
         if( pttPacket==null) continue;

         RtpHeader rtpHeader = pttPacket.getRtpHeader();
         P25Payload p25Payload = pttPacket.getP25Payload();
         IssiPacketType issiPacketType = p25Payload.getIssiPacketType();

         fromFsm = findFsmById( pttPacket.getSendingRfssId());
         toFsm = findFsmById( pttPacket.getReceivingRfssId());

         if( verbose) {
            logger.debug("analyzeGCSD(): PttPacket: i="+i);
            logger.debug("    packetNumber: "+pttPacket.getPacketNumber());
            logger.debug("    receptionTime: "+pttPacket.getReceptionTime());
            logger.debug("    sendingRfssId: "+pttPacket.getSendingRfssId());
            logger.debug("    receivingRfssId: "+pttPacket.getReceivingRfssId());
            logger.debug("    rtp-payloadType: "+rtpHeader.getPayloadType());
            BlockHeader[] blockHeaderArray = p25Payload.getBlockHeaderArray();
            for( BlockHeader blockHeader: blockHeaderArray) {
               logger.debug("    p25-blockHeader-blockType: "+blockHeader.getBlockType());
            }
            logger.debug("    p25-issiPacketType-packetType: "+issiPacketType.getPacketType());
         }   // verbose

         // check the FSM index limit 
         if( fromFsm.getIndex() > MAX_FSMS || toFsm.getIndex() > MAX_FSMS) {
            logger.debug( "FSM index out of range: "+fromFsm.getId()+":"+toFsm.getId());
            continue;
         }

         //--------------------------------------
         long time = pttPacket.getReceptionTime();
         PacketType ptype = PacketType.getInstance(
            PacketType.getValueFromString( issiPacketType.getPacketType()));
         if( ptype == PacketType.HEARTBEAT) {
            //showln("see PttPacket: HEARTBEAT");
         }
         else if( ptype == PacketType.HEARTBEAT_QUERY) {
            //showln("see PttPacket: HEARTBEAT_QUERY");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_REQUEST) {
            //showln("see PttPacket: PTT_TRANSMIT_REQUEST");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_REQUEST_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_REQUEST_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_GRANT) {
            //showln("see PttPacket: PTT_TRANSMIT_GRANT");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_GRANT_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_GRANT_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_PROGRESS) {
            //showln("see PttPacket: PTT_TRANSMIT_PROGRESS");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_START) {
            //showln("see PttPacket: PTT_TRANSMIT_START => PTT_TRANSMIT_PROGRESS");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_END) {
            //showln("see PttPacket: PTT_TRANSMIT_END");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_MUTE) {
            //showln("see PttPacket: PTT_TRANSMIT_MUTE");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_UNMUTE) {
            //showln("see PttPacket: PTT_TRANSMIT_UNMUTE");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_WAIT) {
            //showln("see PttPacket: PTT_TRANSMIT_WAIT");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_DENY) {
            //showln("see PttPacket: PTT_TRANSMIT_DENY");
         }
         //--------------------------------------------------
         if( verbose) {
            logger.debug("----------------------");
            logger.debug("fromFsm-Id: "+fromFsm.getId());
            logger.debug("fromFsm-CurrentState: "+fromFsm.getCurrentState());
            logger.debug("fromFsm-tgxMap: "+fromFsm.getDataMap());
            logger.debug("----------------------");
            logger.debug("toFsm-Id: "+toFsm.getId());
            logger.debug("toFsm-CurrentState: "+toFsm.getCurrentState());
            logger.debug("toFsm-tgxMap: "+toFsm.getDataMap());
            logger.debug("----------------------");
         }
      }
   }

   //-----------------------------------------------------------------------
   public void analyzeUCSD(int mask, String xmlMsg)
      throws XmlException, IOException, ParseException
   {
      this.mask = mask;
      this.userTag = "";
      fsmMap = new LinkedHashMap<String,IssiMessageFsm>();

      XmlAllmessages xmldoc = new XmlAllmessages();
      xmldoc.loadAllmessages( xmlMsg);
      Allmessages allmessages = xmldoc.getAllmessages();
      Message[] messageArray = allmessages.getMessageArray();

      // establish the order of FSM
      setupFsmMap( allmessages);

      boolean fromFlag;
      boolean toFlag;
      IssiMessageFsm fromFsm = null;
      IssiMessageFsm toFsm = null;
      IssiState currentState = IssiState.STATE_READY;

      for( int i=0; i < messageArray.length; i++)
      {
         Message message = messageArray[i];
         if( message==null) continue;
         long time = message.getTime();
         String firstLine = message.getFirstLine().trim();

         if( verbose) {
            logger.debug("analyzeUCSD(): Message: i="+i);
            logger.debug("    packetNumber: "+message.getPacketNumber());
            logger.debug("    from: "+message.getFrom());
            logger.debug("    to: "+message.getTo());
            logger.debug("    fromRfssId: "+message.getFromRfssId());
            logger.debug("    toRfssId: "+message.getToRfssId());
            logger.debug("    time: "+time);
            logger.debug("    firstLine: "+firstLine);
         }
   
         fromFsm = findFsmById( message.getFromRfssId());
         toFsm = findFsmById( message.getToRfssId());

         //--------------------------------------------------
         // check the FSM index limit 
         if( fromFsm.getIndex() > MAX_FSMS || toFsm.getIndex() > MAX_FSMS) {
            logger.debug( "FSM index out of range: "+fromFsm.getId()+":"+toFsm.getId());
            continue;
         }
         //--------------------------------------------------
         if( firstLine.startsWith("INVITE")) {
            userTag = XmlAllmessages.getUserTag( firstLine);
            //showln("    userTag: "+userTag);
            showln("INVITE-firstLine=["+ firstLine+"]");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.INVITE_EXIT);
            showln("INVITE-fromFlag="+ fromFlag);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.INVITE_ENTRY);
               logger.debug("INVITE-toFlag: "+toFlag);
               showln("INVITE-toFlag: "+toFlag);
            }
         }
         else if( firstLine.startsWith("SIP")) {

            currentState = fromFsm.getCurrentState();
            showln("SIP-currentState="+ currentState);
            //showln("SIP-firstLine=["+ firstLine+"]");

	    if( currentState != IssiState.STATE_FIN)
            if( firstLine.endsWith("OK") ) {
               showln("SIP-OK-firstLine=["+ firstLine+"]");
               if(currentState == IssiState.INVITE_ENTRY  || 
                  currentState == IssiState.INVITE_EXIT ||
                  currentState == IssiState.OK_ENTRY
                 ) { 
                  fromFlag = fromFsm.processMessage(mask, time, IssiState.OK_EXIT);
                  showln("SIP-fromFlag="+ fromFlag);
                  if(fromFlag) {
                     toFlag = toFsm.processMessage(mask, time, IssiState.OK_ENTRY);
                     logger.debug("SIP-toFlag: "+toFlag);
                     showln("SIP-toFlag="+ toFlag);
                  }
               }
            }
            else if( firstLine.endsWith("Trying")) {
               // nothing to do
               showln("SIP-Trying");
            }
            else if( firstLine.endsWith("Ringing")) {
               // nothing to do
               showln("SIP-Ringing");
            }
         }
         else if( firstLine.startsWith("ACK")) {
            currentState = IssiState.ACK_EXIT;
            showln("ACK-currentState="+ currentState);
            showln("ACK-firstLine=["+ firstLine+"]");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.ACK_EXIT);
            showln("ACK-fromFlag="+ fromFlag);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.ACK_ENTRY);
               logger.debug("ACK-toFlag: "+toFlag);
               showln("ACK-toFlag="+ toFlag);
	       // === all done
	       fromFsm.setCurrentState(IssiState.STATE_FIN);
            }
         }
         //--------------------------------------------------
         if( verbose) {
            logger.debug("----------------------");
            logger.debug("fromFsm-Id: "+fromFsm.getId());
            logger.debug("fromFsm-CurrentState: "+fromFsm.getCurrentState());
            logger.debug("fromFsm-tgxMap: "+fromFsm.getDataMap());
            logger.debug("----------------------");
            logger.debug("toFsm-Id: "+toFsm.getId());
            logger.debug("toFsm-CurrentState: "+toFsm.getCurrentState());
            logger.debug("toFsm-tgxMap: "+toFsm.getDataMap());
            logger.debug("----------------------");
         }
         showln("-------------------------");

      }  // allmessages
   }

   //-----------------------------------------------------------------------
   public void analyzeGMTD(int mask, String xmlMsg)
      throws XmlException, IOException, ParseException
   {
      analyzeUMTD(mask, xmlMsg);
   }

   public void analyzeUMTD(int mask, String xmlMsg)
      throws XmlException, IOException, ParseException
   {
      this.mask = mask;
      this.userTag = "";
      fsmMap = new LinkedHashMap<String,IssiMessageFsm>();

      XmlAllmessages xmldoc = new XmlAllmessages();
      xmldoc.loadAllmessages( xmlMsg);
      Allmessages allmessages = xmldoc.getAllmessages();
      //Message[] messageArray = allmessages.getMessageArray();

      // establish the order of FSM
      setupFsmMap( allmessages);

      boolean fromFlag;
      boolean toFlag;
      IssiMessageFsm fromFsm = null;
      IssiMessageFsm toFsm = null;
      //IssiState currentState;

      // process only PTT messages
      //-------------------------------------------------------------------
      PttPacket[] pttPacketArray = allmessages.getPttPacketArray();
      for( int i=0; i < pttPacketArray.length; i++) {
         PttPacket pttPacket = pttPacketArray[i];
         if( pttPacket==null) continue;

         RtpHeader rtpHeader = pttPacket.getRtpHeader();
         P25Payload p25Payload = pttPacket.getP25Payload();
         IssiPacketType issiPacketType = p25Payload.getIssiPacketType();

         fromFsm = findFsmById( pttPacket.getSendingRfssId());
         toFsm = findFsmById( pttPacket.getReceivingRfssId());

         if( verbose) {
            logger.debug("anayzeUMTD(): PttPacket: i="+i);
            logger.debug("    packetNumber: "+pttPacket.getPacketNumber());
            logger.debug("    receptionTime: "+pttPacket.getReceptionTime());
            logger.debug("    sendingRfssId: "+pttPacket.getSendingRfssId());
            logger.debug("    receivingRfssId: "+pttPacket.getReceivingRfssId());
            logger.debug("    rtp-payloadType: "+rtpHeader.getPayloadType());
            BlockHeader[] blockHeaderArray = p25Payload.getBlockHeaderArray();
            for( BlockHeader blockHeader: blockHeaderArray) {
               logger.debug("    p25-blockHeader-blockType: "+blockHeader.getBlockType());
            }
            logger.debug("    p25-issiPacketType-packetType: "+issiPacketType.getPacketType());
         }   // verbose

         // check the FSM index limit
         if( fromFsm.getIndex() > MAX_FSMS || toFsm.getIndex() > MAX_FSMS) {
             logger.debug( "FSM index out of range: "+fromFsm.getId()+":"+toFsm.getId());
             continue;
         }

         //--------------------------------------
         long time = pttPacket.getReceptionTime();
         PacketType ptype = PacketType.getInstance(
            PacketType.getValueFromString( issiPacketType.getPacketType()));
         if( ptype == PacketType.HEARTBEAT) {
            //showln("see PttPacket: HEARTBEAT");
         }
         else if( ptype == PacketType.HEARTBEAT_QUERY) {
            //showln("see PttPacket: HEARTBEAT_QUERY");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_REQUEST) {
            //showln("see PttPacket: PTT_TRANSMIT_REQUEST");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_REQUEST_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_REQUEST_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_GRANT) {
            //showln("see PttPacket: PTT_TRANSMIT_GRANT");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_GRANT_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_GRANT_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_PROGRESS) {
            //showln("see PttPacket: PTT_TRANSMIT_PROGRESS");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_START) {
            //showln("see PttPacket: PTT_TRANSMIT_START => PTT_TRANSMIT_PROGRESS");
            fromFlag = fromFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_EXIT);
            if(fromFlag) {
               toFlag = toFsm.processMessage(mask, time, IssiState.PTT_TRANSMIT_PROGRESS_ENTRY);
            }
         }
         else if( ptype == PacketType.PTT_TRANSMIT_END) {
            //showln("see PttPacket: PTT_TRANSMIT_END");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_MUTE) {
            //showln("see PttPacket: PTT_TRANSMIT_MUTE");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_UNMUTE) {
            //showln("see PttPacket: PTT_TRANSMIT_UNMUTE");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_WAIT) {
            //showln("see PttPacket: PTT_TRANSMIT_WAIT");
         }
         else if( ptype == PacketType.PTT_TRANSMIT_DENY) {
            //showln("see PttPacket: PTT_TRANSMIT_DENY");
         }
         //--------------------------------------------------
         if( verbose) {
            logger.debug("----------------------");
            logger.debug("fromFsm-Id: "+fromFsm.getId());
            logger.debug("fromFsm-CurrentState: "+fromFsm.getCurrentState());
            logger.debug("fromFsm-tgxMap: "+fromFsm.getDataMap());
            logger.debug("----------------------");
            logger.debug("toFsm-Id: "+toFsm.getId());
            logger.debug("toFsm-CurrentState: "+toFsm.getCurrentState());
            logger.debug("toFsm-tgxMap: "+toFsm.getDataMap());
            logger.debug("----------------------");
         }
      }
   }

   //=======================================================================
   /****
   public static void main(String[] args) throws Exception
   {
      String xmlFile = "logs/allmessages.xml";
      int mask =  IssiMessageFsm.MASK_GCSD;

      String xmlMsg = FileUtility.loadFromFileAsString(xmlFile);
      IssiMessageAnalyzer analyzer = 
         new IssiMessageAnalyzer( IssiMessageAnalyzer.KEY_ANY);

      // unit-test: simulate the ensemble averaging
      for(int i=0; i < 3; i++) {
         analyzer.analyze( mask, xmlMsg);
         analyzer.showResults();
         showln(" ============ ensemble: "+i);
         analyzer.calculateGCSD( true);
         showln("summary: "+analyzer.getSummaryMap());
      }
   } 
    ****/
}
