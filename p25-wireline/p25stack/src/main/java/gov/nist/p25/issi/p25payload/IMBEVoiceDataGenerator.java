//
package gov.nist.p25.issi.p25payload;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * This class handles IMBE voice data generation.
 */
public class IMBEVoiceDataGenerator {
   
   private static Logger logger = Logger.getLogger(IMBEVoiceDataGenerator.class);

   private static String IMBE_HEX_FILE = "voice/imbe-hex-test.txt";

   /**
    * This method opens a predefined IMBE file, parses the file,
    * and reads all blocks of voice data into an array list of
    * IMBEVoiceBlock objects, and returns the array list.
    *
    * @return An array list of IMBE voice blocks.
    */
   public static List<IMBEVoiceBlock> getIMBEVoiceData() {
      return readIMBEVoiceData(IMBE_HEX_FILE, -1);
   }

   /**
    * This method opens a predefined IMBE file, parses the file,
    * and reads N blocks of voice data into an array list of
    * IMBEVoiceBlock objects, and returns the array list.
    *
    * @param numBlocks
    *            The number of blocks to read from the IMBE file.
    * @return An array list of IMBE voice blocks.
    */
   public static List<IMBEVoiceBlock> getIMBEVoiceData(int numBlocks) {
      return readIMBEVoiceData(IMBE_HEX_FILE, numBlocks);
   }

   /**
    * This method opens an IMBE file, parses the file into an array list of
    * IMBEVoiceBlock objects, and returns the array list.
    * 
    * @param filePath
    *            The path to the IMBE file.
    * @param numBlocks
    *            The number of blocks to read from the IMBE file.
    * @return An array list of IMBE voice blocks.
    */
   public static List<IMBEVoiceBlock> getIMBEVoiceData(String filePath, int numBlocks) {
      return readIMBEVoiceData(filePath, numBlocks);
   }

   /**
    * This method opens an IMBE file, parses the file into an array list of
    * IMBEVoiceBlock objects, and returns the array list.
    * 
    * @param filePath
    *            The path to the IMBE file.
    * @return An array list of IMBE voice blocks.
    */
   public static List<IMBEVoiceBlock> getIMBEVoiceData(String filePath) {
      return readIMBEVoiceData(filePath, -1);
   }

   private static List<IMBEVoiceBlock> readIMBEVoiceData(String filePath, int numBlocks) {

      ArrayList<IMBEVoiceBlock> imbeVoices = new ArrayList<IMBEVoiceBlock>();

      // Open media file
      BufferedReader inputStream = null;
      try {
         /*
          * TODO (steveq): Hardcode this until we decide where voice files
          * will go and how they will be retrieved.
          */
         //inputStream = new BufferedReader(new FileReader(IMBE_HEX_FILE));
         inputStream = new BufferedReader(new FileReader(filePath));

         String l;
         int i = 0;
         int imbePacketIndex = 0;
         int superFrameIndex = 0; // 18 IMBE packets
         while ((l = inputStream.readLine()) != null) {

            i++;
            imbePacketIndex++;
            IMBEVoiceBlock imbeVoice = new IMBEVoiceBlock();

            // Get first 6 bytes in hex string format
            String firstHexSubStr = l.substring(0, 12);
            // Get next 5 bytes in hex string format (11 bytes total)
            String secondHexSubStr = l.substring(12, 22);

            // The following was verified by Kameron
            int frameType = IMBEVoiceDataGenerator.getFrameType(imbePacketIndex);
            imbeVoice.setFT(frameType);
            long U0_U3 = Long.parseLong(firstHexSubStr, 16);
            long U4_U7 = Long.parseLong(secondHexSubStr, 16);

            imbeVoice.setU0((int) (U0_U3 >>> 0x24) & 0xFFF);
            imbeVoice.setU1((int) (U0_U3 >>> 0x18) & 0xFFF);
            imbeVoice.setU2((int) (U0_U3 >>> 0x0C) & 0xFFF);
            imbeVoice.setU3((int) U0_U3 & 0xFFF);
            imbeVoice.setU4((int) (U4_U7 >>> 0x1D) & 0x7FF);
            imbeVoice.setU5((int) (U4_U7 >>> 0x12) & 0x7FF);
            imbeVoice.setU6((int) (U4_U7 >>> 0x07) & 0x7FF);
            imbeVoice.setU7((int) U4_U7 & 0x7F);

            imbeVoice.setEt(0);
            imbeVoice.setEr(0);
            imbeVoice.setM(0);
            imbeVoice.setL(0);
            imbeVoice.setE4(0);
            imbeVoice.setE1(0);
            imbeVoice.setSF(superFrameIndex);
            imbeVoice.setReserved(0);

            // create additional frame by ISSI frame type
            if (frameType == IMBEVoiceBlock.IMBEFRAME9
                  || frameType == IMBEVoiceBlock.IMBEFRAME18) {

               // Low Speed Data
               byte[] additionalFrameData = new byte[3];
               imbeVoice.setAdditionalFrameData(additionalFrameData);

            } else if (frameType == IMBEVoiceBlock.IMBEFRAME12
                  || frameType == IMBEVoiceBlock.IMBEFRAME13
                  || frameType == IMBEVoiceBlock.IMBEFRAME14) {

               // Encryption Sync
               byte[] additionalFrameData = new byte[4];
               imbeVoice.setAdditionalFrameData(additionalFrameData);

            } else if (frameType == IMBEVoiceBlock.IMBEFRAME15) {

               byte[] additionalFrameData = new byte[4];
               additionalFrameData[0] = (byte) 0x80;
               imbeVoice.setAdditionalFrameData(additionalFrameData);

            } else if (frameType == IMBEVoiceBlock.IMBEFRAME16) {

               byte[] additionalFrameData = new byte[4];
               additionalFrameData[0] = (byte) 0xac;
               additionalFrameData[1] = (byte) 0xb8;
               additionalFrameData[2] = (byte) 0xa4;
               imbeVoice.setAdditionalFrameData(additionalFrameData);

            } else if (frameType == IMBEVoiceBlock.IMBEFRAME17) {

               byte[] additionalFrameData = new byte[4];
               additionalFrameData[0] = (byte) 0x9b;
               additionalFrameData[1] = (byte) 0xdc;
               additionalFrameData[2] = 0x75;
               imbeVoice.setAdditionalFrameData(additionalFrameData);
            }

            // Add to IMBE voice array
            imbeVoices.add(imbeVoice);
            if (imbePacketIndex == 18) {
               imbePacketIndex = 0;
               if (superFrameIndex < 3)
                  superFrameIndex++;
               else
                  superFrameIndex = 0;
            }

            // We break if i = numBlocks. Note that if i < 0, we
            // only break if we reach EOF.
            if (i == numBlocks)
               break;
         }
         if (inputStream != null) {
            inputStream.close();
         }

      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
      return imbeVoices;
   }

   /**
    * STEVEQ: Increment the frame type on each outgoing packet, and loop after
    * each set of 18. Frame types are defined in IMBEVoiceBlock.
    * 
    * @param i
    *            The index of the IMBE voice block.
    * @return The frame type.
    */
   private static int getFrameType(int i) {
      // NOTE: if (i % 18) thus [0..17], use direct lookup
      if(i < 1 || i > 18) {
         logger.error("ERROR: Invalid IMBE Frame Type - index: "+i);
         return 0;
      }
      return IMBEVoiceBlock.getFrameType( i );
   }

   private static int getCAIFrameType(int i) {
      if(i < 1 || i > 18) {
         logger.error("ERROR: Invalid CAI Frame Type - index: "+i);
         return 0;
      }
      return IMBEVoiceBlock.getCAIFrameType( i );
   }
}
