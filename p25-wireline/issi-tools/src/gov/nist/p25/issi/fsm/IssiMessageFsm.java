//
package gov.nist.p25.issi.fsm;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.log4j.Logger;

//import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.Message;

/**
 * ISSI Message finite state machine 
 */
public class IssiMessageFsm
{
   private static Logger logger = Logger.getLogger(IssiMessageFsm.class);
   //public static void showln(String s) { System.out.println(s); }

   public static int MASK_GCSD = 0x0001;
   public static int MASK_UCSD = 0x0002;
   public static int MASK_GMTD = 0x0004;
   public static int MASK_UMTD = 0x0008;

   private int index;
   private String id;
   private IssiState currentState;
   private LinkedHashMap<String, Long> tgxMap;

   // accessor
   public int getIndex() { return index; }
   public void setIndex(int index) { this.index=index; }

   public String getId() { return id; }
   public void setId(String id) { this.id=id; }

   public IssiState getCurrentState() { return currentState; }
   public void setCurrentState(IssiState currentState) { this.currentState=currentState; }

   public Map<String, Long> getDataMap() { return tgxMap; }

   // constructor
   public IssiMessageFsm(String id) {
      this.id = id;
      initialize();
   }

   public void initialize() {
      tgxMap = new LinkedHashMap<String, Long>();
      currentState = IssiState.STATE_READY;
   }

   public boolean processMessage(int mask, long time, IssiState nextState)
      throws ParseException
   {
      boolean b = true;
      if( (mask & MASK_GCSD) == MASK_GCSD) { 
         b = b && processGCSD(time, nextState);
      }
      if( (mask & MASK_UCSD) == MASK_UCSD) { 
         b = b && processUCSD(time, nextState);
      }
      if( (mask & MASK_GMTD) == MASK_GMTD) { 
         processGMTD(time, nextState);
      }
      if( (mask & MASK_UMTD) == MASK_UMTD) { 
         processUMTD(time, nextState);
      }
      //showln("===== processMessage: b="+b);
      return b;
   }

   //-------------------------------------------------------------------
   public boolean processGCSD(long time, IssiState nextState)
      throws ParseException
   {
      logger.debug("IssiMessageFsm: processGCSD: currentState="+currentState);
      boolean bflag = false;
      IssiState state = nextState.parseGCSD( time, this);
      if( state == IssiState.STATE_CHANGED) { 
         currentState = nextState;
         bflag = true;
      }
      return bflag;
   }

   public boolean processUCSD(long time, IssiState nextState)
      throws ParseException
   {
      logger.debug("processUCSD: currentState="+currentState);
      boolean bflag = false;
      IssiState state = nextState.parseUCSD( time, this);
      if( state == IssiState.STATE_CHANGED) { 
         currentState = nextState;
         bflag = true;
      }
      return bflag;
   }

   public boolean processGMTD(long time, IssiState nextState)
      throws ParseException
   {
      logger.debug("processGMTD: currentState="+currentState);
      boolean bflag = false;
      IssiState state = nextState.parseGMTD( time, this);
      if( state == IssiState.STATE_CHANGED) { 
         currentState = nextState;
         bflag = true;
      }
      return bflag;
   }

   public boolean processUMTD(long time, IssiState nextState)
      throws ParseException
   {
      logger.debug("processUMTD: currentState="+currentState);
      boolean bflag = false;
      IssiState state = nextState.parseUMTD( time, this);
      if( state == IssiState.STATE_CHANGED) { 
         currentState = nextState;
         bflag = true;
      }
      return bflag;
   }
   //-------------------------------------------------------------------
}
