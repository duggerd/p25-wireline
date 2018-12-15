//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines the listener methods for an MMF.
 * 
 * @see ISSI spec v8, Section 7.2, Figure 65 Summary of Transitions
 */
public interface SmfRxListener {

   /**
    * Process an incoming spurt request.
    * 
    * @param event
    */
   public void receivedPttStart(SmfPacketEvent event);
   
   /**
    * Indicates to the listener that an audio packet has arrived.
    * 
    * @param event
    *            The received PTT packet event.
    */
   public abstract void receivedPttProgress(SmfPacketEvent event);

   /**
    * Process a spurt end event.
    * 
    * @param event
    */
   public abstract void receivedPttEnd(SmfPacketEvent event);
   
}
