//
package gov.nist.p25.issi.testlauncher;

import org.apache.log4j.Logger;

/**
 * The class that logs errors etc. It can communicate with a remote monitor if 
 * such a monitor is registered.
 */
public class TestHarness {

   public TestHarness() {
   }

   private Logger getLogger() {
      StackTraceElement[] stackTrace = new Exception().getStackTrace();
      StackTraceElement caller = stackTrace[1];
      Logger logger = Logger.getLogger(caller.getClass());
      return logger;
   }

   private void logSuccess() {
      getLogger().info("Test succeeded");
   }

   private void logFailure(String failure) {
      Logger.getLogger("gov.nist.p25.issi").error(failure);
   }

   private void logFailure(String failure, Exception ex) {
      getLogger().error(failure, ex);
   }

   public void logTestCompleted(String completionMessage) {
      getLogger().info("Test Completed!");
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#assertTrue(boolean)
    */
   public void assertTrue(boolean cond) {
      if (cond) {
         logSuccess();
      } else {
         logFailure("assertTrue failed");
      }
      if (!cond) {
         fail("assertion failure");
      }
      // TestCase.assertTrue(cond);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#assertTrue(java.lang.String,
    *      boolean)
    */
   public void assertTrue(String diagnostic, boolean cond) {
      if (cond) {
         logSuccess();
      } else {
         logFailure(diagnostic);
      }
      if (!cond) {
         new Exception(diagnostic).printStackTrace();
         fail(diagnostic + " : Assertion Failure ");
      }
      // TestCase.assertTrue(diagnostic, cond);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#assertEquals(java.lang.Object,
    *      java.lang.Object)
    */
   public void assertEquals(Object me, Object him) {
      if (me == him) {
         logSuccess();
      } else if (me == null && him != null) {
         logFailure("assertEquals failed");

      } else if (me != null && him == null) {
         logFailure("assertEquals failed");

      } else if (!me.equals(him)) {
         logFailure("assertEquals failed");

      }
      // TestCase.assertEquals(me, him);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#assertEquals(java.lang.String,
    *      java.lang.String)
    */
   public void assertEquals(String me, String him) {
      if (me == him) {
         logSuccess();
      } else if (me == null && him != null) {
         logFailure("assertEquals failed");

      } else if (me != null && him == null) {
         logFailure("assertEquals failed");

      } else if (!me.equals(him)) {
         logFailure("assertEquals failed");

      }
      // TestCase.assertEquals(me, him);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#assertEquals(java.lang.String,
    *      java.lang.Object, java.lang.Object)
    */
   public void assertEquals(String reason, Object me, Object him) {
      if (me == him) {
         logSuccess();
      } else if (me == null && him != null) {
         logFailure("assertEquals failed");
      } else if (me != null && him == null) {
         logFailure("assertEquals failed");
      } else if (!me.equals(him)) {
         logFailure(reason);
      }
      // TestCase.assertEquals(reason, me, him);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#assertEquals(java.lang.String,
    *      java.lang.String, java.lang.String)
    */
   public void assertEquals(String reason, String me, String him) {
      if (me == him) {
         logSuccess();
      } else if (me == null && him != null) {
         logFailure("assertEquals failed");
      } else if (me != null && him == null) {
         logFailure("assertEquals failed");
      } else if (!me.equals(him)) {
         logFailure("assertEquals failed");
      }
      // TestCase.assertEquals(reason, me, him);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#assertNotNull(java.lang.String,
    *      java.lang.Object)
    */
   public void assertNotNull(String reason, Object thing) {
      if (thing != null) {
         logSuccess();
      } else {
         logFailure(reason);
      }
      // TestCase.assertNotNull(reason, thing);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#assertNull(java.lang.String,
    *      java.lang.Object)
    */
   public void assertNull(String reason, Object thing) {
      if (thing == null) {
         logSuccess();
      } else {
         logFailure(reason);
      }
      // TestCase.assertNull(reason, thing);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#assertSame(java.lang.String,
    *      java.lang.Object, java.lang.Object)
    */
   public void assertSame(String diagnostic, Object thing, Object thingie) {
      if (thing == thingie) {
         logSuccess();
      } else {
         logFailure(diagnostic);
      }
      // TestCase.assertSame(diagnostic, thing, thingie);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#fail(java.lang.String)
    */
   public void fail(String message) {
      logFailure(message);
      //M1001
      //+++new Exception().printStackTrace();
      // TestCase.fail(message);
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#fail()
    */
   public void fail() {
      logFailure("Unknown reason for failure. Check logs for more info.");
      new Exception().printStackTrace();
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.p25.issi.testlauncher.TestHarnessIF#fail(java.lang.String,
    *      java.lang.Exception)
    */
   public void fail(String message, Exception ex) {
      logFailure(message, ex);
   }
}
