//
package gov.nist.p25.issi.transctlmgr.ptt;

/**
 * This class defines the listener methods for an MMF.
 */
public interface MmfRxListener {

   /**
    * Process an incoming spurt request.
    * 
    * @param event
    */
   public void receivedPttRequest(MmfPacketEvent event);
   
   /**
    * Process an incoming spurt request.
    * 
    * @param event
    */
   public void receivedPttRequestWithVoice(MmfPacketEvent event);
   
   /**
    * Indicates to the listener that an audio packet has arrived.
    * 
    * @param event
    *            The received PTT packet event.
    */
   public abstract void receivedPttProgress(MmfPacketEvent event);

   /**
    * Process a spurt end event.
    * 
    * @param event
    */
   public abstract void receivedPttEnd(MmfPacketEvent event);
   
   /**
    * Indicates a timeout waiting for PROGRESS (audio) packets.
    */
   public abstract void audioTimeout();
}
