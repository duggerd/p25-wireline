//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import gov.nist.p25.issi.p25body.serviceprofile.InterruptModeType;
import gov.nist.p25.issi.p25body.serviceprofile.SecurityLevelType;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

import junit.framework.TestCase;

/**
 * Test case for the classes in this package.
 */
public class GroupServiceProfileTest extends TestCase {

   private static Logger logger;

   static {
      PropertyConfigurator.configure("log4j.properties");
      logger = Logger.getLogger(GroupServiceProfile.class);
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   public static String gProfile1 = "g-access:1\r\n"
         + "g-agroup:0B4561A27271\r\n" + "g-pri:2\r\n" + "g-ecap:1\r\n"
         + "g-eprempt:0\r\n" + "g-rfhangT:0\r\n" + "g-ccsetupT:0\r\n"
         + "g-intmode:0\r\n" + "g-sec:1\r\n" + "g-ic:0\r\n"
         + "g-icsecstart:0\r\n";

   public static String gProfile2 = "g-as:1\r\n" + "g-ag:0B4561A27271\r\n"
         + "g-p:2\r\n" + "g-ec:1\r\n" + "g-ep:0\r\n" + "g-h:0\r\n"
         + "g-c:0\r\n" + "g-in:0\r\n" + "g-s:1\r\n" + "g-ic:0\r\n"
         + "g-ics:0\r\n";

   public void testParse() {
      try {
         GroupServiceProfile gsProfile = GroupServiceProfile
               .createGroupServiceProfile(gProfile1);
         assertEquals("g-access", gsProfile.getAccessPermission()
               .isNonEmergencyCallingAllowed(), true);
         assertEquals("g-agroup", gsProfile.getAnnouncementGroup()
               .getGroupId(), Long.parseLong("0B4561A27271", 16));
         assertEquals("g-pri", gsProfile.getGroupPriority().getPriority(), 2);
         assertEquals("g-ecap", gsProfile.getEmergencyCapable()
               .isEmergencyCapable(), true);
         assertEquals("g-eprempt", gsProfile.getEmergencyPreemption()
               .isRuthlessOnEmergency(), false);
         assertEquals("g-rfhant", gsProfile.getRfHangTime().getHangime(), 0);
         assertEquals("g-ccseupt", gsProfile.getConfirmedCallSetupTime()
               .getSetupTime(), 0);
         assertEquals("g-intmode", gsProfile.getInterruptMode().getMode(),
               InterruptModeType.AUDIO_INTERRRUPT_NEVER_ALLOWED);
         assertEquals("g-sec", gsProfile.getGroupSecurityLevel().getLevel(),
               SecurityLevelType.getInstance(1));
         assertEquals("g-ic", gsProfile.getInterconnectFlag()
               .isInterconnectAllowed(), false);
         assertEquals("g-icsecstart", gsProfile.getInterconnectSecurity()
               .isInterconnectCallsSecure(), false);
      } catch (Exception ex) {
         ex.printStackTrace();
         fail("Unexpected exception " + ex.getMessage());
      }
   }

   public void testParse1() {
      try {
         GroupServiceProfile gsProfile = GroupServiceProfile
               .createGroupServiceProfile(gProfile2);
         assertEquals("g-access", gsProfile.getAccessPermission()
               .isNonEmergencyCallingAllowed(), true);
         assertEquals("g-agroup", gsProfile.getAnnouncementGroup()
               .getGroupId(), Long.parseLong("0B4561A27271", 16));
         assertEquals("g-pri", gsProfile.getGroupPriority().getPriority(), 2);
         assertEquals("g-ecap", gsProfile.getEmergencyCapable()
               .isEmergencyCapable(), true);
         assertEquals("g-eprempt", gsProfile.getEmergencyPreemption()
               .isRuthlessOnEmergency(), false);
         assertEquals("g-rfhant", gsProfile.getRfHangTime().getHangime(), 0);
         assertEquals("g-ccseupt", gsProfile.getConfirmedCallSetupTime()
               .getSetupTime(), 0);
         assertEquals("g-intmode", gsProfile.getInterruptMode().getMode(),
               InterruptModeType.AUDIO_INTERRRUPT_NEVER_ALLOWED);
         assertEquals("g-sec", gsProfile.getGroupSecurityLevel().getLevel(),
               SecurityLevelType.getInstance(1));
         assertEquals("g-ic", gsProfile.getInterconnectFlag()
               .isInterconnectAllowed(), false);
         assertEquals("g-icsecstart", gsProfile.getInterconnectSecurity()
               .isInterconnectCallsSecure(), false);
      } catch (Exception ex) {
         ex.printStackTrace();
         fail("Unexpected exception " + ex.getMessage());
      }
   }

   public void testPack() {
      try {
         GroupServiceProfile gsProfile1 = GroupServiceProfile
               .createGroupServiceProfile(gProfile2);
         String profile = gsProfile1.toString();
         logger.debug(profile);
         GroupServiceProfile gsProfile = GroupServiceProfile
               .createGroupServiceProfile(profile);
         assertEquals("g-access", gsProfile.getAccessPermission()
               .isNonEmergencyCallingAllowed(), true);
         assertEquals("g-agroup", gsProfile.getAnnouncementGroup()
               .getGroupId(), Long.parseLong("0B4561A27271", 16));
         assertEquals("g-pri", gsProfile.getGroupPriority().getPriority(), 2);
         assertEquals("g-ecap", gsProfile.getEmergencyCapable()
               .isEmergencyCapable(), true);
         assertEquals("g-eprempt", gsProfile.getEmergencyPreemption()
               .isRuthlessOnEmergency(), false);
         assertEquals("g-rfhant", gsProfile.getRfHangTime().getHangime(), 0);
         assertEquals("g-ccseupt", gsProfile.getConfirmedCallSetupTime()
               .getSetupTime(), 0);
         assertEquals("g-intmode", gsProfile.getInterruptMode().getMode(),
               InterruptModeType.AUDIO_INTERRRUPT_NEVER_ALLOWED);
         assertEquals("g-sec", gsProfile.getGroupSecurityLevel().getLevel(),
               SecurityLevelType.getInstance(1));
         assertEquals("g-ic", gsProfile.getInterconnectFlag()
               .isInterconnectAllowed(), false);
         assertEquals("g-icsecstart", gsProfile.getInterconnectSecurity()
               .isInterconnectCallsSecure(), false);
      } catch (Exception ex) {
         ex.printStackTrace();
         fail("Unexpected exception " + ex.getMessage());
      }
   }
}
