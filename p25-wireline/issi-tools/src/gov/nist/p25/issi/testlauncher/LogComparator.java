//
package gov.nist.p25.issi.testlauncher;

import gov.nist.p25.issi.traceviewer.MessageData;

import java.util.Comparator;

/**
 * A class that is used for comparing log records.
 * 
 */
public class LogComparator implements Comparator<MessageData> {

   public int compare(MessageData m1, MessageData m2) {
      try {
         //+++long ts1 = Long.parseLong(m1.getTime());
         //+++long ts2 = Long.parseLong(m2.getTime());
         long ts1 = m1.getTimestamp();
         long ts2 = m2.getTimestamp();
         if (ts1 < ts2)
            return -1;
         else if (ts1 > ts2)
            return 1;
         else {
            //return 0;
            return -m1.getToRfssId().compareTo(m2.getToRfssId()) ;
         }
      } catch (NumberFormatException ex) {
         ex.printStackTrace();
         //System.exit(0);
         return 0;
      }
   }

   public boolean equals(Object obj2) {
      return super.equals(obj2);
   }
}
