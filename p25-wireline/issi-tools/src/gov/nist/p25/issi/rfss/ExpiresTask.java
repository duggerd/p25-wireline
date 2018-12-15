//
package gov.nist.p25.issi.rfss;

import java.util.TimerTask;
import javax.sip.address.Address;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;

import org.apache.log4j.Logger;

/**
 * Class for removing the bindings after expires
 */
public class ExpiresTask extends TimerTask {
   private static Logger logger = Logger.getLogger(ExpiresTask.class);

   private String key;
   private ContactHeader contactHeader;
   private RegistrationsTable registrationsTable;

   // Constructor
   public ExpiresTask(String key, ContactHeader contactHeader,
         RegistrationsTable registrationsTable) {
      this.registrationsTable = registrationsTable;
      this.key = key;
      this.contactHeader = contactHeader;
   }

   public void run() {
      Address address = contactHeader.getAddress();
      URI cleanedUri = UnitToUnitMobilityManager.getCleanUri(address.getURI());
      String contactURI = cleanedUri.toString();
      registrationsTable.removeContact(key, contactHeader);
      registrationsTable.getExpiresTaskTable().remove(contactURI);
      if (logger.isDebugEnabled()) {
         logger.debug("ExpiresTask, run(), we  removed the contact: "
               + contactURI + " for the user: " + key);
      }
      synchronized (registrationsTable.getExpiresTaskTable()) {
         registrationsTable.getExpiresTaskTable().remove(key);
      }
   }
}
