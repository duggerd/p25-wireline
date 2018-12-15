//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;

import gov.nist.p25.issi.utils.ByteUtil;

/**
 * <p>
 * From Kameron's ISSI SFS v2 document:
 * <pre>
 *        0 1 2 3 4 5 6 7 
 *        +-+-+-+-+-+-+-+-+
 *        |E|P|D|M|R|P Lvl|
 *        +-+-+-+-+-+-+-+-+
 * </pre>
 *
 * Service Options In general the service requests and service grants will allow
 * for specific service extensions in the form of the Service Options field.
 * This allows great flexibility to tailor the requested service to the needs of
 * the requesting unit or process capability of the system.
 * <ul>
 * <li> Bit 7 = Emergency - this is the status indication to determine if this
 * service is to be specially processed as an emergency service.
 * <ul>
 * <li> 0 = Non-emergency indicates the normal processing status
 * <li> 1 = Emergency indicates special processing required.
 * </ul>
 * <li> Bit 6 = Protected - indicates whether the resources (other than control
 * channel resources) to be associated with this service should be presented in
 * protected mode (e.g. encrypted) or not.
 * <li>
 * <ul>
 * <li> 0 = not protected indicates normal mode presentation for the
 * resource(s).
 * <li> 1 = protected indicates protection mode presentation for the
 * resource(s).
 * <li>
 * </ul>
 * <li> Bit 5 = Duplex - indicates the way the channel resource is to be
 * utilized by the unit(s) involved in the call.
 * <ul>
 * <li> 0 = Half duplex indicates the unit will be capable of transmitting but
 * not simultaneously receiving on an assigned channel.
 * <li> 1 = Full duplex indicates the unit will be capable of simultaneous
 * transmit and receive on an assigned channel.
 * </ul>
 * <li> Bit 4 Mode - this is the indication of whether this service session
 * should be accomplished in a packet mode or circuit mode.
 * <ul>
 * <li> 0 = Circuit mode will utilize resources capable of supporting circuit
 * operation.
 * <li> 1 = Packet mode will utilize resources capable of supporting packet
 * operation.
 * </ul>
 * <li> Bit 3 Reserved - currently set to null (0)
 * <li> Bits 2-0 = Priority level - indicates the relative importance attributed
 * to this service
 * </ul>
 * 
 */
public class ServiceOptions implements Serializable {
   
   private static final long serialVersionUID = -1L;

   //TODO (steveq) Change the following to their alpha identifier
   // (e.g., E for emergency).
   
   /** Emergency (1 bit). */
   private boolean isEmergency = false;

   /** Protected (1 bit). */
   private boolean isProtected = false;

   /** Duplex (1 bit). */
   private boolean isDuplex = false;

   /** Mode (1 bit). */
   private boolean mode = false;

   /** Priority Level (3 bits). */
   private int priorityLevel = 4;      // Default is 100

   /** Service Options */
   public ServiceOptions() {
   }
   public ServiceOptions(int serviceOptions) {
      this.decode(serviceOptions);
   }

   /**
    * @param isEmergency
    *            The isEmergency to set.
    */
   public void setEmergency(boolean isEmergency) {
      this.isEmergency = isEmergency;
   }

   /**
    * @return Returns the isEmergency.
    */
   public boolean isEmergency() {
      return isEmergency;
   }

   /**
    * @param isProtected
    *            The isProtected to set.
    */
   public void setProtected(boolean isProtected) {
      this.isProtected = isProtected;
   }

   /**
    * @return Returns the isProtected.
    */
   public boolean isProtected() {
      return isProtected;
   }

   /**
    * @param isDuplex
    *            The isDuplex to set.
    */
   public void setDuplex(boolean isDuplex) {
      this.isDuplex = isDuplex;
   }

   /**
    * @return Returns the isDuplex.
    */
   public boolean isDuplex() {
      return isDuplex;
   }

   /**
    * @param mode
    *            The mode to set.
    */
   public void setMode(boolean mode) {
      this.mode = mode;
   }

   /**
    * @return Returns the isPacket.
    */
   public boolean isMode() {
      return mode;
   }

   /**
    * @param priorityLevel
    *            The priorityLevel to set.
    */
   public void setPriorityLevel(int priorityLevel) {
      if (priorityLevel > ByteUtil.getMaxIntValueForNumBits(3))
         throw new IllegalArgumentException("Value out of range"
               + priorityLevel);
      this.priorityLevel = priorityLevel;
   }

   /**
    * @return Returns the priorityLevel.
    */
   public int getPriorityLevel() {
      return priorityLevel;
   }

   // TODO (steveq)  Use conventional method name get() rather than encode().

   public int encode() {
      return (isEmergency ? 1 << 7 : 0) + (isProtected ? 1 << 6 : 0)
            + (isDuplex ? 1 << 5 : 0) + (mode ? 1 << 4 : 0)
            + priorityLevel;
   }

   // TODO (steveq)  Use conventional method name set() rather than decode().
   
   private void decode(int serviceOptions) throws IllegalArgumentException {
      if ((serviceOptions & (1 << 3)) == 1 << 3)
         throw new IllegalArgumentException(
               "Reserved bit is set -- illegal argument" + serviceOptions);
      isEmergency = (serviceOptions & (1 << 7)) == 1 << 7;
      isProtected = (serviceOptions & (1 << 6)) == 1 << 6;
      isDuplex = (serviceOptions & (1 << 5)) == 1 << 5;
      mode = (serviceOptions & (1 << 4)) == 1 << 4;
      // Bit 3 is ignored
      priorityLevel = serviceOptions & 0x07;
   }
   
   @Override 
   public boolean equals(Object other) {
      if (!(other instanceof ServiceOptions)) return false;
      ServiceOptions that = (ServiceOptions) other;
      return isEmergency == that.isEmergency && 
             isProtected == that.isProtected && 
             isDuplex == that.isDuplex && 
             mode == that.mode &&
             priorityLevel == that.priorityLevel;
   }
}
