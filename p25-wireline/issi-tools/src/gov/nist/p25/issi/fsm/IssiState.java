//
package gov.nist.p25.issi.fsm;

import java.text.ParseException;
import java.util.Map;

//import gov.nist.p25.issi.xmlconfig.AllmessagesDocument.Allmessages.Message;

public enum IssiState implements MessageClassifier 
{
   // states for SIP and PTT messages
   //-----------------------------------------
   INVITE_EXIT(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
	 // NOTE: the test requires 3 RFSSes, we will pad to 4
         int lookup[] = { 1, 2, 0, 0};
         int k = lookup[fsm.getIndex()];
         if( k > 0) 
            tgxMap.put( "end_Tg"+k, new Long( time));
         showln("INVITE_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 1}, { 2}, { 3}, { 4, 6}};
         int[] index = lookup[fsm.getIndex()];
         for(int k: index) {
            if( k > 0) 
               tgxMap.put( "end_Tu"+k, new Long( time));
         }
         showln("INVITE_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
   }),
   INVITE_ENTRY(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 0}, { 2, 3}, { 4}, { 0}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0)
               tgxMap.put( "start_Tg"+k, new Long( time));
         }
         showln("INVITE_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { {0}, {2}, {3}, { 4, 6}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0)
               tgxMap.put( "start_Tu"+k, new Long( time));
         }
         showln("INVITE_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
   }),

   //-----------------------------------------
   OK_EXIT(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[] = { 0, 3, 4, 0};
         int k = lookup[fsm.getIndex()];
         if( k > 0)
            tgxMap.put( "end_Tg"+k, new Long( time));
         showln("OK_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 9, 10}, { 8}, { 7}, { 5, 6}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0) {
               //Long obj = tgxMap.get( "end_Tu"+k);
               //if( obj==null)
                  tgxMap.put( "end_Tu"+k, new Long( time));
            }
         }
         showln("OK_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
   }),
   OK_ENTRY(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 5}, { 8, 9}, { 0}, { 0}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0) 
               tgxMap.put( "start_Tg"+k, new Long( time));
         }
         showln("OK_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 9, 10}, { 8}, { 7}, { 0}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0) {
               //Long obj = tgxMap.get( "start_Tu"+k);
               //if( obj==null)
                  tgxMap.put( "start_Tu"+k, new Long( time));
            }
         }
         showln("OK_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
   }),

   //-----------------------------------------
   ACK_EXIT(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[] = { 10, 11, 12, 13};
         int k = lookup[fsm.getIndex()];
         if( k > 0)
            tgxMap.put( "end_Tu"+k, new Long( time));
         showln("ACK_EXIT ... " +fsm.getIndex() +" " +tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
   }),
   ACK_ENTRY(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[] = { 0, 11, 12, 13};
         int k = lookup[fsm.getIndex()];
         if( k > 0)
            tgxMap.put( "start_Tu"+k, new Long( time));
         showln("ACK_ENTRY ... "+fsm.getIndex() +" " +tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
   }),

   //-----------------------------------------
   PTT_TRANSMIT_REQUEST_EXIT(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 5}, { 7, 8}, { 11}, { 0}};
         int[] index = lookup[fsm.getIndex()];
         for(int k: index) {
            if( k > 0)
               tgxMap.put( "end_Tg"+k, new Long( time));
	 }
         showln("PTT_TRANSMIT_REQUEST_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[] = { 1, 2, 3, 0};
         int k = lookup[fsm.getIndex()];
         if( k > 0)
            tgxMap.put( "end_Tg_ad"+k, new Long( time));
         showln("PTT_TRANSMIT_REQUEST_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 1}, { 2, 7}, { 3, 6}, { 4, 5}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0)
               tgxMap.put( "end_Tu_ad"+k, new Long( time));
         }
         showln("PTT_TRANSMIT_REQUEST_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
   }),
   PTT_TRANSMIT_REQUEST_ENTRY(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 0}, { 6, 7}, { 11}, { 0}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0)
               tgxMap.put( "start_Tg"+k, new Long( time));
         }
         showln("PTT_TRANSMIT_REQUEST_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[] = { 0, 2, 3, 0};
         int k = lookup[fsm.getIndex()];
         if( k > 0)
            tgxMap.put( "start_Tg_ad"+k, new Long( time));
         showln("PTT_TRANSMIT_REQUEST_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 8}, { 2, 7}, { 3, 6}, { 4, 5}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0)
               tgxMap.put( "start_Tu_ad"+k, new Long( time));
         }
         showln("PTT_TRANSMIT_REQUEST_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
   }),
   //-----------------------------------------
   //PTT_TRANSMIT_PROGRESS
   //PTT_TRANSMIT_START
   PTT_TRANSMIT_PROGRESS_EXIT(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 0}, { 7, 8}, { 11}, { 0}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0)
               tgxMap.put( "end_Tg"+k, new Long( time));
         }
         showln("PTT_TRANSMIT_PROGRESS_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[] = { 1, 2, 3, 0};
         int k = lookup[fsm.getIndex()];
         if( k > 0)
            tgxMap.put( "end_Tg_ad"+k, new Long( time));
         showln("PTT_TRANSMIT_PROGRESS_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 1}, { 2, 7}, { 3, 6}, { 4, 5}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0)
               tgxMap.put( "end_Tu_ad"+k, new Long( time));
         }
         showln("PTT_TRANSMIT_PROGRESS_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
   }),
   PTT_TRANSMIT_PROGRESS_ENTRY(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 0}, { 0}, { 11}, { 0}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0)
               tgxMap.put( "start_Tg"+k, new Long( time));
         }
         showln("PTT_TRANSMIT_PROGRESS_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[] = { 0, 2, 3, 0};
         int k = lookup[fsm.getIndex()];
         if( k > 0)
            tgxMap.put( "start_Tg_ad"+k, new Long( time));
         showln("PTT_TRANSMIT_PROGRESS_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 8}, { 2, 7}, { 3, 6}, { 4, 5}};
         int[] index = lookup[fsm.getIndex()];
         for( int k: index) {
            if( k > 0)
               tgxMap.put( "start_Tu_ad"+k, new Long( time));
         }
         showln("PTT_TRANSMIT_PROGRESS_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
   }),

   //-----------------------------------------
   //PTT_TRANSMIT_END
   //-----------------------------------------
   PTT_TRANSMIT_GRANT_EXIT(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[][] = { { 0}, { 6, 9}, { 0}, { 0}};
         int[] index = lookup[fsm.getIndex()];
         for(int k: index) {
            if( k > 0)
               tgxMap.put( "end_Tg"+k, new Long( time));
         }
         showln("PTT_TRANSMIT_GRANT_EXIT ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
   }),
   PTT_TRANSMIT_GRANT_ENTRY(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         Map<String,Long> tgxMap = fsm.getDataMap();
         int lookup[] = { 10, 0, 0, 0};
         int k = lookup[fsm.getIndex()];
         if( k > 0)
            tgxMap.put( "start_Tg"+k, new Long( time));
         showln("PTT_TRANSMIT_GRANT_ENTRY ... "+ tgxMap);
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
   }),
   //-----------------------------------------
   //-----------------------------------------
   STATE_READY(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
   }),
   STATE_CHANGED(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_CHANGED;
      }
   }),
   STATE_FIN(new MessageClassifier() {
      public IssiState parseGCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_FIN;
      }
      public IssiState parseUCSD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_FIN;
      }
      public IssiState parseGMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_FIN;
      }
      public IssiState parseUMTD(long time, IssiMessageFsm fsm) throws ParseException {
         return STATE_FIN;
      }
   });

   public static void showln(String s) {
      // disabled
      System.out.println(s);
   }
   public static void sleep(long msecs) {
      try {
         Thread.sleep( msecs);
      } catch(InterruptedException ex) { }
   }

   //-------------------------------------------------------------
   private final MessageClassifier classifier;

   // constructor
   private IssiState(MessageClassifier classifier) {
      this.classifier = classifier;
   }

   // implementation of MessageClassifier
   //-------------------------------------------------------------
   public IssiState parseGCSD(long time, IssiMessageFsm fsm)
      throws ParseException {
      return classifier.parseGCSD( time, fsm);
   }

   public IssiState parseUCSD(long time, IssiMessageFsm fsm)
      throws ParseException {
      return classifier.parseUCSD( time, fsm);
   }

   public IssiState parseGMTD(long time, IssiMessageFsm fsm)
      throws ParseException {
      return classifier.parseGMTD( time, fsm);
   }

   public IssiState parseUMTD(long time, IssiMessageFsm fsm)
      throws ParseException {
      return classifier.parseUMTD( time, fsm);
   }
}
