//
package gov.nist.p25.issi.fsm;

import java.text.ParseException;

public interface MessageClassifier {

   public IssiState parseGCSD(long time, IssiMessageFsm fsm)
      throws ParseException;

   public IssiState parseUCSD(long time, IssiMessageFsm fsm)
      throws ParseException;

   public IssiState parseGMTD(long time, IssiMessageFsm fsm)
      throws ParseException;

   public IssiState parseUMTD(long time, IssiMessageFsm fsm)
      throws ParseException;
}

