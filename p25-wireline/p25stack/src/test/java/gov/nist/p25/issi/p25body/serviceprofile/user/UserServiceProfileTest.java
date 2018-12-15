//
package gov.nist.p25.issi.p25body.serviceprofile.user;

import gov.nist.p25.issi.p25body.serviceprofile.*;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

import junit.framework.TestCase;

/**
 * UserService Profile Test
 * 
 */
public class UserServiceProfileTest extends TestCase {

   private static Logger logger;

   static {
      PropertyConfigurator.configure("log4j.properties");
      logger = Logger.getLogger(UserServiceProfileTest.class);
      logger.addAppender(new ConsoleAppender(new SimpleLayout()));
   }

   private static String profile1 = "u-access:1\r\n" + "u-dup:0\r\n"
         + "u-sec:0\r\n" + "u-gcall:3\r\n" + "u-ucall:0\r\n"
         + "u-upri:10\r\n" + "u-iccall:0\r\n" + "u-icsec:1\r\n"
         + "u-IcprI:10\r\n" + "u-authtype:0\r\n" + "u-availcheck:1\r\n"
         + "u-prefsetup:0\r\n";

   public void testParse() {
      try {
         UserServiceProfile usp = UserServiceProfile
               .createUserServiceProfile(profile1);
         assertTrue("u-dup", usp.getDuplexity().getDuplex().equals(
               DuplexityType.getInstance(0)));
         assertTrue("u-sec", usp.getSecureCapable().isSecure() == false);
         assertTrue( "u-gcall",
               usp.getGroupCallPermissions()
                  .getPermission()
                  .equals(GroupCallPermissionType.EMERGENCY_AND_NON_EMERGENCY));
         assertTrue("u-ucall", usp.getUnitToUnitCallPermission()
               .getPermission().equals(CallPermissionType.NONE));
         assertTrue("u-upri",
               usp.getUnitToUnitCallPriority().getPriority() == 10);
         assertTrue("u-iccall", usp.getInterconnectPermission()
               .getPermission().equals(CallPermissionType.NONE));
         assertTrue("u-icsec", usp.getInterconnectSecurity()
               .getSecurityLevel().equals(
                     SecurityLevelType.CLEAR_MODE_ONLY_CALLS));
         assertTrue("u-icpri", usp.getInterconnectCallPriority()
               .getPriority() == 10);
         assertTrue("u-authtype", usp.getAuthenticationCapability()
               .getAuthenticationType().equals(AuthenticationType.NONE));
         assertTrue("u-availcheck", usp.getAvailabilityCheck()
               .getAvailabilityCheckType().equals(
                     AvailabilityCheckType.AVAIL_CHECK_ONLY));
         assertTrue("u-prefsetup",
               usp.getCallSetupPreference().getCallSetupPreferenceType() == CallSetupPreferenceType.CALLER_PREFERS_AVAILABILITY_CHECK);

      } catch (Exception ex) {
         ex.printStackTrace();
         fail("Unexpected exception");
      }
   }
}
