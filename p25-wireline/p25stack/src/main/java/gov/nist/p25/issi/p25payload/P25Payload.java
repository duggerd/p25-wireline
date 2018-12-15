//
package gov.nist.p25.issi.p25payload;

import java.io.Serializable;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * This class implements a P25 payload which encapsulates one or more P25 block
 * objects. The various forms of ISSI RTP Packets are shown below.
 * <p>
 * 
 * <pre>
 *  +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+
 *  |  RTP Hdr  | |  RTP Hdr  | |  RTP Hdr  | |  RTP Hdr  | |  RTP Hdr  |
 *  +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+
 *  |  Control  | |  Control  | |  Control  | |  Control  | |  Control  |
 *  +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+
 *  |Block Hdrs | |Block Hdrs | |Block Hdrs | |Block Hdrs | |Block Hdrs |
 *  +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+
 *  |Packet Type| |Packet Type| |Packet Type| |Packet Type| |Packet Type|
 *  | PTT Start | | PTT Grant | | PTT Rqst  | | PTT Mute  | | Heartbeat |
 *  |           | |    Wait   | | Progress  | |   Unmute  | | HB Query  |
 *  |           | |    Deny   | |           | |           | |           |
 *  |           | |    End    | |           | |           | |           |
 *  +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+
 *  | PTT Cntrl | |Opt Manufac| | PTT Cntrl | |Opt Manufac| |Opt Manufac|
 *  |   Word    | |  Specific | |   Word    | |  Specific | |  Specific |
 *  +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+
 *        :             :       |Opt ISSI   |       :             :      
 *        :             :       |Hdr Word Bl|       :             :    
 *  +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+ +-+-+-+-+-+-+
 *  |Opt Manufac| |Opt Manufac| | Opt IMBE  | |Opt Manufac| |Opt Manufac|
 *  |  Specific | |  Specific | +-+-+-+-+-+-+ |  Specific | |  Specific |
 *  +-+-+-+-+-+-+ +-+-+-+-+-+-+ | Opt IMBE  | +-+-+-+-+-+-+ +-+-+-+-+-+-+
 *                              +-+-+-+-+-+-+  
 *                              | Opt IMBE  |   
 *                              +-+-+-+-+-+-+ 
 *                              |Opt Manufac|   
 *                              |  Specific |
 *                              +-+-+-+-+-+-+ 
 *                                    :
 *                                    :
 *                              +-+-+-+-+-+-+                                   
 *                              |Opt Manufac|          
 *                              |  Specific |
 *                              +-+-+-+-+-+-+ 
 * </pre>
 * 
 */
public class P25Payload implements Serializable {

   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(P25Payload.class);;
   

   /** Minimum number of IMBE blocks per PTT PROGRESS packet type. */
   public static final int MIN_NUM_IMBE_BLOCKS_PER_PROGRESS_PACKET = 1;

   /** Maximum number of IMBE blocks per PTT PROGRESS packet type. */
   public static final int MAX_NUM_IMBE_BLOCKS_PER_PROGRESS_PACKET = 3;

   /**
    * Sample duration for an IMBE voice packet is 20ms represented in 125
    * microsecond resolution. That is, (20 * 1000) / 8 = 2500 (in 125
    * microseconds).
    */
   //public static final int IMBE_TIME_OFFSET = 2500;
   //see #617 per bward
   public static final int IMBE_TIME_OFFSET = 160;


   /** Control block. */
   private ControlOctet controlOctet;

   /**
    * Array list of block headers. Volatile because it is not used for matching.
    */
   private transient ArrayList<BlockHeader> blockHeaders = new ArrayList<BlockHeader>();

   /**
    * ISSI packet type. The first P25 Block in an RTP packet over the ISSI
    * SHALL be an ISSI Packet Type block (Block Type = 1).
    */
   private ISSIPacketType issiPacketType = null;

   /** Control word. */
   private PTTControlWord pttControlWord = null;

   /** ISSI header word block. */
   private ISSIHeaderWord issiHeaderWord = null;

   /** Array of IMBE voice blocks. We dont test these for matching so it is transient.*/
   private transient IMBEVoiceBlock[] imbeVoiceBlocks = null;

   /** Array of manufacturer-specific blocks. (TBD) */
   // private ManufacturerSpecific[] mfrSpecificBlocks = null;


   /**
    * Contructs a P25 payload.
    * 
    * @param issiPacketType
    *            The ISSI Packet Type.
    * @param pttControlWord
    *            The PTT Control Word.
    * @param issiHeaderWord
    *            The ISSI Header Word (null if not present).
    * @param imbeVoiceBlocks
    *            The IMBE Voice Blocks (null if not present).
    * @param manufacturerSpecificBlocks
    *            The Mfr. Specific Blocks (null if not present).
    */
   public P25Payload(ISSIPacketType issiPacketType,
         PTTControlWord pttControlWord, ISSIHeaderWord issiHeaderWord,
         IMBEVoiceBlock[] imbeVoiceBlocks,
         ManufacturerSpecific[] manufacturerSpecificBlocks)
         throws P25BlockException {

      if (issiPacketType == null) {
         throw new NullPointerException("ISSIPacketType is null!");
      }
      
      // Create control octet
      controlOctet = new ControlOctet();
      controlOctet.setC(0);
      if (issiPacketType.getPT() == ISSIPacketType.PTT_TRANSMIT_START
            || issiPacketType.getPT() == ISSIPacketType.PTT_TRANSMIT_PROGRESS
            || issiPacketType.getPT() == ISSIPacketType.PTT_TRANSMIT_PROGRESS
            || issiPacketType.getPT() == ISSIPacketType.PTT_TRANSMIT_GRANT
            || issiPacketType.getPT() == ISSIPacketType.PTT_TRANSMIT_MUTE
            || issiPacketType.getPT() == ISSIPacketType.PTT_TRANSMIT_UNMUTE) {
         controlOctet.setS(1);
      }

      // Initialize block header count
      int blockHeaderCount = 0;

      // Set ISSI packet type and associated block header
      this.issiPacketType = issiPacketType;

      BlockHeader issiPacketTypeBlockHeader = new BlockHeader();
      issiPacketTypeBlockHeader.setE(PayloadType.PROFILE_SPECIFIC_TYPE.intValue());
      issiPacketTypeBlockHeader.setBT(BlockType.PACKET_TYPE_INDEX);
      issiPacketTypeBlockHeader.setBlockLength(ISSIPacketType.FIXED_LENGTH);
      blockHeaders.add(issiPacketTypeBlockHeader);
      blockHeaderCount++;

      // Set PTT Control Word and associated block header
      if (pttControlWord != null) {
         this.pttControlWord = pttControlWord;
         BlockHeader pttControlWordBlockHeader = new BlockHeader();
         pttControlWordBlockHeader.setE(PayloadType.PROFILE_SPECIFIC_TYPE.intValue());
         pttControlWordBlockHeader.setBT(BlockType.PTT_CONTROL_WORD_INDEX);
         pttControlWordBlockHeader.setBlockLength(PTTControlWord.FIXED_LENGTH);
         blockHeaders.add(pttControlWordBlockHeader);
         blockHeaderCount++;
      }

      // Set ISSI Header Word and associated block header
      if (issiHeaderWord != null) {
         this.issiHeaderWord = issiHeaderWord;
         BlockHeader issiHeaderWordBlockHeader = new BlockHeader();
         issiHeaderWordBlockHeader.setE(PayloadType.PROFILE_SPECIFIC_TYPE.intValue());
         issiHeaderWordBlockHeader.setBT(BlockType.ISSI_HEADER_INFO_INDEX);
         issiHeaderWordBlockHeader.setBlockLength(ISSIHeaderWord.FIXED_LENGTH);
         blockHeaders.add(issiHeaderWordBlockHeader);
         blockHeaderCount++;
      }

      // Set IMBE Voice Block array and associated block headers
      if (imbeVoiceBlocks != null) {

         if ((imbeVoiceBlocks.length < MIN_NUM_IMBE_BLOCKS_PER_PROGRESS_PACKET) ||
             (imbeVoiceBlocks.length > MAX_NUM_IMBE_BLOCKS_PER_PROGRESS_PACKET)) {
            throw new P25BlockException("IMBE voice array size incorrect.");
         }
	 //#615 assig VBB
         if( this.issiHeaderWord != null)
            this.issiHeaderWord.setVBB( imbeVoiceBlocks.length);

         this.imbeVoiceBlocks = imbeVoiceBlocks;
         for (int i = 0; i < imbeVoiceBlocks.length; i++) {

            IMBEVoiceBlock imbeVoiceBlock = imbeVoiceBlocks[i];
            BlockHeader imbeVoiceBlockHeader = new BlockHeader();
            imbeVoiceBlockHeader.setE(PayloadType.PROFILE_SPECIFIC_TYPE.intValue());
            imbeVoiceBlockHeader.setBT(BlockType.IMBE_VOICE_INDEX);
            imbeVoiceBlockHeader.setBlockLength(imbeVoiceBlock.size());
            imbeVoiceBlockHeader.setTimeStampOffset(i * IMBE_TIME_OFFSET);

            blockHeaders.add(imbeVoiceBlockHeader);
            blockHeaderCount++;
         }

      }

      // Set Manufacturer Specific blocks (TBD)
      if (manufacturerSpecificBlocks != null) {

         throw new P25BlockException("We don't handle Mfr Specific data yet.");

         // this.mfrSpecificBlocks = manufacturerSpecificBlocks;
         //         
         // for (int i = 0; i < manufacturerSpecificBlocks.length; i++) {
         //
         // ManufacturerSpecific ms = manufacturerSpecificBlocks[i];
         //            
         // BlockHeader mfrBlockHeader = new BlockHeader();
         // mfrBlockHeader.setE(PayloadType.PROFILE_SPECIFIC_TYPE.intValue());
         // // For testing, we use the min index for Mfr Specific Block
         // Header
         // mfrBlockHeader.setBT(BlockType.MFGSPEC_MIN_INDEX);
         // mfrBlockHeader.setBL(ms.size());
         //            
         // blockHeaders.add(mfrBlockHeader);
         // blockHeaderCount++;
         //
         // }

      }
      // Set total block header count
      controlOctet.setBHC(blockHeaderCount);
   }

   /**
    * Construct a P25 payload given its byte array representation.
    * 
    * @param bytes
    *            The byte array representation of this P25 payload.
    * @throws P25BlockException
    */
   public P25Payload(byte[] bytes) throws P25BlockException {

      int offset = 0;
      // Set Control Octet
      int controlOctetLength = 1;
      this.controlOctet = new ControlOctet(bytes[0]);
      offset += controlOctetLength;

      // Set Number of Header Blocks
      int numHeaderBlocks = this.controlOctet.getBHC();
      // Set Header Blocks
      for (int i = 0; i < numHeaderBlocks; i++) {

         byte[] blockHeaderBytes = new byte[BlockHeader.FIXED_LENGTH];
         System.arraycopy(bytes, offset, blockHeaderBytes, 0, blockHeaderBytes.length);
         BlockHeader blockHeader = new BlockHeader(blockHeaderBytes);
         this.blockHeaders.add(blockHeader);
         offset += blockHeaderBytes.length;
      }

      ArrayList<IMBEVoiceBlock> voiceBlocks = new ArrayList<IMBEVoiceBlock>();
      //ArrayList<ManufacturerSpecific> mfrBlocks = new ArrayList<ManufacturerSpecific>();
      // Set all Blocks
      for (int i = 0; i < this.blockHeaders.size(); i++) {

         BlockHeader blockHeader = blockHeaders.get(i);
         int BT = blockHeader.getBT();
         if (BT == BlockType.PACKET_TYPE_INDEX) {

            // p25Payload.issiPacketType = new ISSIPacketType();
            byte[] packetTypeBytes = new byte[ISSIPacketType.FIXED_LENGTH];
            System.arraycopy(bytes, offset, packetTypeBytes, 0, packetTypeBytes.length);
            this.issiPacketType = new ISSIPacketType(packetTypeBytes);
            offset += packetTypeBytes.length;

         } else if (BT == BlockType.PTT_CONTROL_WORD_INDEX) {

            byte[] pttControlWordBytes = new byte[PTTControlWord.FIXED_LENGTH];
            System.arraycopy(bytes, offset, pttControlWordBytes, 0, pttControlWordBytes.length);
            this.pttControlWord = new PTTControlWord(pttControlWordBytes);
            offset += pttControlWordBytes.length;

//CSSI - ConsolePTTControlWord

         } else if (BT == BlockType.ISSI_HEADER_INFO_INDEX) {

            byte[] issiHeaderWordBytes = new byte[ISSIHeaderWord.FIXED_LENGTH];
            System.arraycopy(bytes, offset, issiHeaderWordBytes, 0, issiHeaderWordBytes.length);
            this.issiHeaderWord = new ISSIHeaderWord(issiHeaderWordBytes);
            offset += issiHeaderWordBytes.length;

         } else if (BT == BlockType.IMBE_VOICE_INDEX) {

            byte[] imbeVoiceBytes = new byte[blockHeader.getBlockLength()];
            System.arraycopy(bytes, offset, imbeVoiceBytes, 0, imbeVoiceBytes.length);
            IMBEVoiceBlock imbeVoice = new IMBEVoiceBlock(imbeVoiceBytes);
            voiceBlocks.add(imbeVoice);
            offset += imbeVoiceBytes.length;

            // Set Mfr Specific (TBD)
            // } else if (blockHeader.BT == BlockType.MFGSPEC_MIN_INDEX) {
            //            
            // byte[] mfrSpecificBytes = new byte[blockHeader.getBL()];
            // System.arraycopy(bytes, offset, mfrSpecificBytes, 0,
            // mfrSpecificBytes.length);
            //
            // ManufacturerSpecific mfrSpecific =
            // new ManufacturerSpecific(mfrSpecificBytes);
            // mfrBlocks.add(mfrSpecific);
            //
            // offset += mfrSpecificBytes.length;
         }
      }

      // Set IMBE Voice Block array
      if (voiceBlocks.size() > 0) {
         this.imbeVoiceBlocks = new IMBEVoiceBlock[voiceBlocks.size()];
         voiceBlocks.toArray(this.imbeVoiceBlocks);

	 //#615 assig VBB
         if(this.issiHeaderWord != null)
            this.issiHeaderWord.setVBB( this.imbeVoiceBlocks.length);
      }

      // Set Mfr Block array (TBD)
      // if (mfrBlocks.size() > 0) {
      // this.mfrSpecificBlocks =
      // new ManufacturerSpecific[mfrBlocks.size()];
      // mfrBlocks.toArray(this.mfrSpecificBlocks);
      // }
   }

   /***************************************************************************
    * Methods
    **************************************************************************/

   /**
    * Get the P25 payload as a byte array.
    * 
    * @return The P25 payload byte array.
    */
   public byte[] getBytes() throws P25BlockException {

      if (controlOctet == null) {
         throw new P25BlockException(P25BlockException.INCOMPLETE+" No control octet.");
      }

      int numBlockHeaders = blockHeaders.size();
      if (controlOctet.getBHC() != numBlockHeaders) {
         throw new P25BlockException( P25BlockException.INCONSISTENT_BLOCK_COUNT);
      }

      // Initialize total byte array length of this P25 payload
      byte[] controlOctetBytes = controlOctet.getBytes();
      int totalLength = controlOctetBytes.length;

      // Get Block Headers
      if (numBlockHeaders < 1) {
         throw new P25BlockException(P25BlockException.INCOMPLETE+" No block header.");
      }

      int blockHeaderArrayLength = 0;
      for (int i = 0; i < numBlockHeaders; i++) {
         blockHeaderArrayLength += blockHeaders.get(i).size();
      }

      byte[] blockHeaderBytes = new byte[blockHeaderArrayLength];
      int offset = 0;
      int blockHeaderLength = 0;
      for (int i = 0; i < numBlockHeaders; i++) {
         blockHeaderLength = blockHeaders.get(i).size();
         System.arraycopy(blockHeaders.get(i).getBytes(), 0,
               blockHeaderBytes, offset, blockHeaderLength);
         offset += blockHeaderLength;
      }
      totalLength += blockHeaderArrayLength;

      // Get ISSI Packet Type block
      if (issiPacketType == null) {
         throw new P25BlockException(P25BlockException.INCOMPLETE+" No packet type.");
      }

      byte[] issiPacketTypeBytes = issiPacketType.getBytes();
      totalLength += issiPacketTypeBytes.length;

      // Get PTT Control Word block
      byte[] pttControlWordBytes = null;
      if (pttControlWord == null
            && (issiPacketType.getPacketType().equals(
                  PacketType.PTT_TRANSMIT_REQUEST)
                  || issiPacketType.getPacketType().equals(
                        PacketType.PTT_TRANSMIT_START) || issiPacketType
                  .getPacketType().equals(
                        PacketType.PTT_TRANSMIT_PROGRESS)))
         throw new P25BlockException("PTT control word is NULL! \n "
               + "The second block in PTT Request, "
               + "PTT Start and PTT Progress packets SHALL \n"
               + " be the PTT Control Word Block (Block Type = 11).");

      if (pttControlWord != null) {
         pttControlWordBytes = pttControlWord.getBytes();
         totalLength += pttControlWordBytes.length;
      } else {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("PTT control word is NULL!");
      }

      // Get ISSI Header Word block
      byte[] issiHeaderWordBytes = null;
      if (issiHeaderWord != null) {
         if (!issiPacketType.getPacketType().equals(
               PacketType.PTT_TRANSMIT_REQUEST)
               && !issiPacketType.getPacketType().equals(
                     PacketType.PTT_TRANSMIT_PROGRESS)) {

            throw new P25BlockException(
                  "ISSI Header Word Blocks  SHALL only be carried "
                        + "in PTT Request or PTT Progress packets.");

         }
         issiHeaderWordBytes = issiHeaderWord.getBytes();
         totalLength += issiHeaderWordBytes.length;

      } else {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("ISSI header word is NULL WARNING!");
      }

      // Get IMBE Voice Blocks
      int numIMBEVoiceBlocks = imbeVoiceBlocks == null ? 0 : imbeVoiceBlocks.length;
      int imbeVoiceBlocksLength = 0;
      byte[] imbeVoiceBytes = null;
      if (numIMBEVoiceBlocks != 0) {
         for (int i = 0; i < numIMBEVoiceBlocks; i++) {
            imbeVoiceBlocksLength += imbeVoiceBlocks[i].size();
         }
         imbeVoiceBytes = new byte[imbeVoiceBlocksLength];
         offset = 0;
         int imbeVoiceLength = 0;
         for (int i = 0; i < numIMBEVoiceBlocks; i++) {
            imbeVoiceLength = imbeVoiceBlocks[i].size();
            System.arraycopy(imbeVoiceBlocks[i].getBytes(), 0,
                  imbeVoiceBytes, offset, imbeVoiceLength);
            offset += imbeVoiceLength;

         }
         totalLength += imbeVoiceBlocksLength;
      }

      // Create byte array representation
      byte[] bytes = new byte[totalLength];
      offset = 0;
      System.arraycopy(controlOctetBytes, 0, bytes, offset,
            controlOctetBytes.length);
      offset += controlOctetBytes.length;

      System.arraycopy(blockHeaderBytes, 0, bytes, offset,
            blockHeaderBytes.length);
      offset += blockHeaderBytes.length;

      System.arraycopy(issiPacketTypeBytes, 0, bytes, offset,
            issiPacketTypeBytes.length);
      offset += issiPacketTypeBytes.length;

      if (pttControlWordBytes != null) {
         System.arraycopy(pttControlWordBytes, 0, bytes, offset,
               pttControlWordBytes.length);
         offset += pttControlWordBytes.length;
      } else {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("WARNING PTT control is NULL!");
      }

      if (issiHeaderWordBytes != null) {
         System.arraycopy(issiHeaderWordBytes, 0, bytes, offset,
               issiHeaderWordBytes.length);
         offset += issiHeaderWordBytes.length;
      } else {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("WARNING Header Word is NULL!");
      }

      if (imbeVoiceBytes != null) {
         if (!issiPacketType.getPacketType().equals(
               PacketType.PTT_TRANSMIT_REQUEST)
               && !issiPacketType.getPacketType().equals(
                     PacketType.PTT_TRANSMIT_PROGRESS)) {
            throw new P25BlockException(
                  "IMBE Voice Blocks  SHALL only be carried in PTT "
                        + "Request or PTT Progress packets.");
         }
         System.arraycopy(imbeVoiceBytes, 0, bytes, offset,
               imbeVoiceBytes.length);
         offset += imbeVoiceBytes.length;

      } else {
         if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
            logger.debug("WARNING IMBE Voice is NULL!");
      }
      return bytes;
   }

   /**
    * Get the control octet.
    * 
    * @return the control octet.
    */
   public ControlOctet getControlOctet() {
      return controlOctet;
   }

   /**
    * Get the block header vector.
    * 
    * @return the block header vector.
    */
   public ArrayList<BlockHeader> getBlockHeaders() {
      return blockHeaders;
   }

   /**
    * Get the ISSI packet type.
    * 
    * @return the ISSI packet type.
    */
   public ISSIPacketType getISSIPacketType() {
      return issiPacketType;
   }

   /**
    * Get the PTT control word.
    * 
    * @return the PTT control word.
    */
   public PTTControlWord getPTTControlWord() {
      return pttControlWord;
   }

   /**
    * Set the ISSI header word block.
    * 
    * @return the ISSI header word block.
    */
   public ISSIHeaderWord getISSIHeaderWord() {
      return issiHeaderWord;
   }

   /**
    * Get the IMBE voice blocks.
    * 
    * @return the IMBE voice blocks.
    */
   public IMBEVoiceBlock[] getIMBEVoiceBlockArray() {
      return imbeVoiceBlocks;
   }

   /**
    * Get the manufacturer-specific blocks (TBD)
    * 
    * @return the manufacturer-specific blocks.
    */
   // public ManufacturerSpecific[] getManufacturerSpecificArray() {
   // return mfrSpecificBlocks;
   // }
   /**
    * Compare this object with another object.
    * 
    * @param other
    *            The other object to be compared to.
    * @return True if this object is equivalent to the other object.
    */
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof P25Payload))
         return false;
      P25Payload pl = (P25Payload) other;
      if (this.imbeVoiceBlocks == null ^ pl.imbeVoiceBlocks == null)
         return false;
      if (this.imbeVoiceBlocks != null) {
         for (int i = 0; i < this.imbeVoiceBlocks.length; i++) {
            if (!(this.imbeVoiceBlocks[i].equals(pl.imbeVoiceBlocks[i])))
               return false;
         }
      }
      return this.blockHeaders.equals(pl.blockHeaders)
            && issiPacketType.equals(pl.issiPacketType)
            && pttControlWord.equals(pl.pttControlWord)
            && issiHeaderWord.equals(pl.issiHeaderWord);
   }

   /**
    * Get the XML formatted string representation.
    * 
    * @return The XML formatted string representation.
    */
   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("<p25-payload>\n");
      sb.append(controlOctet.toString());
      sb.append("\n");

      for (BlockHeader bh : blockHeaders) {
         sb.append(bh.toString());
         sb.append("\n");
      }

      if (issiHeaderWord != null) {
         sb.append(issiHeaderWord.toString()).append("\n");
      }

      if (issiPacketType != null) {
         sb.append(issiPacketType.toString()).append("\n");
      }

      if (pttControlWord != null) {
         sb.append(pttControlWord.toString()).append("\n");
      }

      if (imbeVoiceBlocks != null) {
         for (IMBEVoiceBlock b : imbeVoiceBlocks) {
            sb.append(b.toString());
            sb.append("\n");
         }   
      }
      sb.append("</p25-payload>");
      return sb.toString();
   }
}
