//
package gov.nist.p25.issi.p25payload;

/**
 * Enumeration of the packet types. This enumerated type is passed in as an
 * argument to the methods of ISSIPacketType
 * 
 */
//
// TODO (steveq)  Not clear that this class is really needed.  All
// of this information is already available in ISSIPacketType.  Recommendation:
// remove this class and modify ISSIPacketType to handle all packet types.
//
public enum PacketType {

   PTT_TRANSMIT_REQUEST(ISSIPacketType.PTT_TRANSMIT_REQUEST,
         "PTT Transmit Request"),

   PTT_TRANSMIT_GRANT(ISSIPacketType.PTT_TRANSMIT_GRANT, "PTT Transmit Grant"),

   PTT_TRANSMIT_PROGRESS(ISSIPacketType.PTT_TRANSMIT_PROGRESS,
         "PTT Transmit Progress"),

   PTT_TRANSMIT_END(ISSIPacketType.PTT_TRANSMIT_END, "PTT Transmit End"),

   PTT_TRANSMIT_START(ISSIPacketType.PTT_TRANSMIT_START, "PTT Transmit Start"),

   PTT_TRANSMIT_MUTE(ISSIPacketType.PTT_TRANSMIT_MUTE, "PTT Transmit Mute"),

   PTT_TRANSMIT_UNMUTE(ISSIPacketType.PTT_TRANSMIT_UNMUTE,
         "PTT Transmit Unmute"),

   PTT_TRANSMIT_WAIT(ISSIPacketType.PTT_TRANSMIT_WAIT, "PTT Transmit Wait"),

   PTT_TRANSMIT_DENY(ISSIPacketType.PTT_TRANSMIT_DENY, "PTT Transmit Deny"),

   // PTT HEARTBEAT
   HEARTBEAT(ISSIPacketType.HEARTBEAT, "Heartbeat"),

   // PTT HEARTBEAT QUERY
   HEARTBEAT_QUERY(ISSIPacketType.HEARTBEAT_QUERY, "Heartbeat Query"),

   // Used only for local indication of connection-related heartbeats
   HEARTBEAT_CONNECTION(ISSIPacketType.HEARTBEAT_CONNECTION,
         "Heartbeat Connection"),

   // Used only for local indication of mute transmission-related heartbeats
   HEARTBEAT_MUTE_TRANSMISSION(ISSIPacketType.HEARTBEAT_MUTE_TRANSMISSION,
         "Heartbeat Mute Transmission"),

   // Used only for local indication of mute transmission-related heartbeats
   HEARTBEAT_UNMUTE_TRANSMISSION(ISSIPacketType.HEARTBEAT_UNMUTE_TRANSMISSION,
         "Heartbeat Unmute Transmission");

   /** For quick conversion of type to enumerated type */
   private static PacketType[] typeArray = {
      PacketType.PTT_TRANSMIT_REQUEST, PacketType.PTT_TRANSMIT_GRANT,
      PacketType.PTT_TRANSMIT_PROGRESS, PacketType.PTT_TRANSMIT_END,
      PacketType.PTT_TRANSMIT_START, PacketType.PTT_TRANSMIT_MUTE,
      PacketType.PTT_TRANSMIT_UNMUTE, PacketType.PTT_TRANSMIT_WAIT,
      PacketType.PTT_TRANSMIT_DENY, PacketType.HEARTBEAT,
      PacketType.HEARTBEAT_QUERY, PacketType.HEARTBEAT_CONNECTION,
      PacketType.HEARTBEAT_MUTE_TRANSMISSION,
      PacketType.HEARTBEAT_UNMUTE_TRANSMISSION
   };

   // Do a little static check to make sure that the array is
   // correctly initialized.
   static {
      for (int i = 0; i < typeArray.length; i++) {
         assert (typeArray[i].intValue() == i);
      }
   }

   /**
    * The type that is stored in the field of this the ISSI packet type
    * structure as a byte.
    */
   private int type;

   /**
    * A description of the type.
    */
   private String typeName;

   /**
    * Construct a packet type.
    * 
    * @param type
    *            the packet type as it appears in ISSIPacketType
    * @param typeName
    *            the descriptive name for the packet type.
    */
   PacketType(int type, String typeName) {
      this.type = type;
      this.typeName = typeName;
   }

   /**
    * Get the int representation of the packet type.
    * 
    * @return int corresponding to this packet type
    */
   public int intValue() {
      return type;
   }

   /**
    * Descriptive name for this packet type.
    */
   public String toString() {
      return typeName;
   }

   /**
    * Convert from int form to the Enumerated type.
    * 
    * @param actualType
    * @return corresponding enumerated type.
    */
   public static PacketType getInstance(int actualType) {
      if (actualType > typeArray.length)
         throw new IllegalArgumentException("out of range arg");
      return typeArray[actualType];
   }

   public static int getValueFromString(String value) {
      for (PacketType pt : PacketType.typeArray) {
         if ( pt.typeName.equals(value)) return pt.type;
      }
      throw new IllegalArgumentException("Illegal packet type description " + value);
   }
}
