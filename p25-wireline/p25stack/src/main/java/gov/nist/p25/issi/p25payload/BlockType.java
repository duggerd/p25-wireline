//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * A static class for the payload. Use this in getters and setters of
 * blockheader to make the code more readable and to avoid errors and range
 * checking at run time. Making this into an enum is painful because we need to
 * enumerate the 63 manufacturer specific types.
 * 
 */
//
// TODO (steveq) It is not clear that this class is necessary. This 
// information should be moved back into its original class, BlockHeader,
// in v2.
//
public class BlockType implements Serializable {

   private static final long serialVersionUID = -1L;

   public static final void showln(String s) { System.out.println(s); }
   public static final boolean verbose = false;

   /** Constant that identifies an IMBE Voice block type (7 bits). */
   public static final int IMBE_VOICE_INDEX = 0;
   public static final int CAI_VOICE_INDEX = IMBE_VOICE_INDEX;

   /** Constant that identifies a PacketType block type (7 bits). */
   public static final int PACKET_TYPE_INDEX = 1;

   /** Reserved block type (7 bits). */
   private static final int RSVD1_MIN_INDEX = 2; // 2..4
   private static final int RSVD1_MAX_INDEX = 4;

   //public static final int RESERVED_LINK_CONTROL_WORD_INDEX = 2;
   //public static final int RESERVED_ENCRYPTION_SYNC_INDEX = 3;
   //public static final int RESERVED_LOW_SPEED_DATA_INDEX = 4;

   /** Constant that identifies an ISSI Header Info block type (7 bits). */
   public static final int ISSI_HEADER_INFO_INDEX = 5;

   /** Another reserved block (7 bits). */
   private static final int RSVD2_MIN_INDEX = 6; // 6..10
   private static final int RSVD2_MAX_INDEX = 10;

   /** FSI Block PT */
   public static final int VOICE_HEADER_P1_INDEX = 6;
   public static final int VOICE_HEADER_P2_INDEX = 7;
   public static final int RSVD2_BLOCK_PT_INDEX = 8;
   public static final int START_OF_STREAM_INDEX = 9;
   public static final int END_OF_STREAM_INDEX = 10;

   /** Constant that identifies a PTTControlWord block type (7 bits). */
   public static final int PTT_CONTROL_WORD_INDEX = 11;

   /** Reserved for Fixed Station Interface (7 bits). */
   //private static final int FXDSTA_MIN_INDEX = 12; // 12..14
   //private static final int FXDSTA_MAX_INDEX = 14;

   /** FSI */
   public static final int VOTER_REPORT_INDEX = 12;
   public static final int VOTER_CONTROL_INDEX = 13;
   public static final int TX_KEY_ACKNOWLEDGE_INDEX = 14;

   /** CSSI Console PTT Control Word (7 bits). */
   public static final int CONSOLE_PTT_CONTROL_WORD_INDEX = 15;

   /** Reserved for Future Expansion (7 bits). */
   private static final int FUTEXP_MIN_INDEX = 16; // 16..62;
   private static final int FUTEXP_MAX_INDEX = 62;

   /** Reserved for manufacturer specific (7 bits). */
   public static final int MFGSPEC_MIN_INDEX = 63; // 63..127
   private static final int MFGSPEC_MAX_INDEX = 127;

   //----------------------------------------------------------------
   /** #370
      {IMBE_VOICE_INDEX, "IMBE Voice Block Type"},            // 0
      {PACKET_TYPE_INDEX, "Packet Block Type"},               // 1
      {PTT_CONTROL_WORD_INDEX, "PTT Control Word Block Type"},// 11
    */
   //----------------------------------------------------------------
   private static Object[][] blockDefs = {
      {IMBE_VOICE_INDEX, "IMBE Voice"},                       // 0
      {PACKET_TYPE_INDEX, "Packet Type"},                     // 1
      {RSVD1_MIN_INDEX, RSVD1_MAX_INDEX, "Reserved 1, Type"}, // 2..4
      {ISSI_HEADER_INFO_INDEX, "ISSI Header Information"},    // 5
      {VOICE_HEADER_P1_INDEX, "Voice Header Part 1"},         // 6
      {VOICE_HEADER_P2_INDEX, "Voice Header Part 2"},         // 7
      {RSVD2_BLOCK_PT_INDEX, "Reserved 2, Block PT"},         // 8
      {START_OF_STREAM_INDEX, "Start of Stream"},             // 9
      {END_OF_STREAM_INDEX, "End of Stream"},                 // 10

      {PTT_CONTROL_WORD_INDEX, "RF PTT Control Word"},        // 11
      {VOTER_REPORT_INDEX, "Voter Report"},                   // 12
      {VOTER_CONTROL_INDEX, "Voter Control"},                 // 13
      {TX_KEY_ACKNOWLEDGE_INDEX, "TX Key Acknowledge"},       // 14
      {CONSOLE_PTT_CONTROL_WORD_INDEX, "Console PTT Control Word Block Type"}, // 15

      // 16..62
      {FUTEXP_MIN_INDEX, FUTEXP_MAX_INDEX, "Future Expansion, Type"},
      // 63..127
      {MFGSPEC_MIN_INDEX, MFGSPEC_MAX_INDEX, "Manufacturer Specific, Type"},
   };

   //----------------------------------------------------------------
   /** A mapping between type indices and type objects. */
   private static Hashtable<Integer, BlockType> ptHash =
         new Hashtable<Integer, BlockType>();

   static {
      for (int i = 0; i < blockDefs.length; i++) {
         Object[] def = blockDefs[i];
         if( def.length == 2) {
            int index = (Integer)def[0];
            if( verbose)
               showln("L2: index=" +index +" T:" +(String)def[1]);
            BlockType bt = new BlockType( index, (String)def[1]);
            ptHash.put( index, bt);
	 }
	 else if( def.length == 3) {
            for (int j = (Integer)def[0]; j <= (Integer)def[1]; j++) {
               String tag = (String)def[2] + j;
               if( verbose)
                  showln("L3: index="+j+" T:"+tag);
	       BlockType bt = new BlockType( j, tag);
               ptHash.put( j, bt);
            }
	 }
      }
   }

   //----------------------------------------------------------------
   /** The block type as an int. */
   private int type;

   /** The block type description. */
   private String description;

   /**
    * Private constructor.
    * 
    * @param type
    *            the block type.
    * @param description
    *            the block type description.
    */
   private BlockType(int type, String description) {
      this.type = type;
      this.description = description;
   }

   public int getType() {
      return type;
   }
   public String getDescription() {
      return description;
   }

   /**
    * Get the int value of this block type.
    * 
    * @return the int value of this block type.
    */
   public int intValue() {
      return type;
   }

   /**
    * Get the string representation for this block type.
    * 
    * @return the string representation for this block type.
    */
   @Override
   public String toString() {
      return description;
   }

   /**
    * Get the block type for a given int.
    * 
    * @param blockType
    *            int representation of the block type.
    * @return the block type.
    */
   public static BlockType getInstance(int blockType) {

      if (!ptHash.containsKey(blockType)) {
         throw new IllegalArgumentException("Illegal block type: "+blockType);
      }
      return ptHash.get(blockType);
   }
   
   /**
    * Get the block type from the description
    */
   public static int getValueFromDescription(String description) {
      for (BlockType bt: ptHash.values()) {
         if ( bt.getDescription().equals(description)) {
            return bt.getType();
         }
      }
      throw new IllegalArgumentException("Bad description: " + description);
   }

   public static void addBlockType(int type, String description) {
      ptHash.put(type, new BlockType(type, description));
   }
}
