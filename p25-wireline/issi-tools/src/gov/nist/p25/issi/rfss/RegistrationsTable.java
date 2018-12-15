//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.ISSITimer;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfile;
import gov.nist.p25.issi.rfss.SipUtils;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import org.apache.log4j.Logger;

/**
 * Registration Table
 */
@SuppressWarnings("unchecked")
public class RegistrationsTable {

   private static Logger logger = Logger.getLogger(RegistrationsTable.class);

   private UnitToUnitMobilityManager mobilityManager;
   private Hashtable<String, ExpiresTask> expiresTaskTable;
   private Hashtable<String, Registration> registrations;
   private RFSS rfss;
   private TopologyConfig topologyConfig;

   // constructor
   public RegistrationsTable(UnitToUnitMobilityManager registrar, TopologyConfig topologyConfig) {
      this.mobilityManager = registrar;
      registrations = new Hashtable<String, Registration>();
      expiresTaskTable = new Hashtable<String, ExpiresTask>();
      this.rfss = registrar.getRFSS();
      this.topologyConfig = topologyConfig;
   }

   public Hashtable getRegistrations() {
      return registrations;
   }

   public Hashtable getExpiresTaskTable() {
      return expiresTaskTable;
   }

   public synchronized String getRegistryXMLTags() throws RemoteException
   {
      StringBuffer retval = new StringBuffer("<REGISTRATIONS>");
      Collection values = registrations.values();
      Iterator it = values.iterator();
      while (it.hasNext()) {
         Registration registration = (Registration) it.next();
         retval.append(registration.getXMLTags());
      }
      retval.append("</REGISTRATIONS>");
      return retval.toString();
   }

   public synchronized Vector getRegistryBindings() throws RemoteException
   {
      Vector retval = new Vector();
      Collection values = registrations.values();
      Iterator it = values.iterator();
      while (it.hasNext()) {
         Registration registration = (Registration) it.next();
         ExportedBinding be = registration.exportBinding();
         if (logger.isDebugEnabled())
            logger.debug("adding a binding " + be);

         if (be != null)
            retval.add(be);
      }
      return retval;
   }

   public synchronized int getRegistrySize() throws RemoteException
   {
      Collection values = registrations.values();
      return values.size();
   }

   public synchronized boolean hasRegistration(String key) {
      boolean res = registrations.containsKey(key.toLowerCase());
      logger.debug("RegistrationsTable, hasRegistration(), Checking registration for \""
         + key.toLowerCase() + "\" : " + (res ? "registered" : "not registered"));
      return res;
   }

   protected void addRegistration(String key, Request request) {
      Iterator it = request.getHeaders(ContactHeader.NAME);
      int expiresTimeHeader = -1;
      FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
      Registration registration = new Registration(key, fromHeader
            .getAddress().getURI(), it, null, this.topologyConfig);

      ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
      if (expiresHeader != null) {
         expiresTimeHeader = expiresHeader.getExpires();
         if (expiresTimeHeader > UnitToUnitMobilityManager.EXPIRES_TIME_MAX
               || expiresTimeHeader < UnitToUnitMobilityManager.EXPIRES_TIME_MIN)
            expiresTimeHeader = UnitToUnitMobilityManager.EXPIRES_TIME_MAX;
      } else {
         expiresTimeHeader = UnitToUnitMobilityManager.EXPIRES_TIME_MAX;
      }

      for (it = request.getHeaders(ContactHeader.NAME); it.hasNext();) {
         ContactHeader contactHeader = (ContactHeader) it.next();
         try {
            if (contactHeader.getExpires() == -1) {
               contactHeader.setExpires(expiresTimeHeader);
            }
         } catch (InvalidArgumentException ex) {
            throw new Error("Unexpected excepton", ex);
         }
         startTimer(key, contactHeader.getExpires(), contactHeader);
      }
      registrations.put(key, registration);
      logger.debug("RegistrationsTable, addRegistration(), registration "
            + " added for the key: " + key);
      printRegistrations();
   }

   protected void addRegistration(Registration registration) throws Exception {
      Vector contacts = registration.getContactsList();
      // ok to have empty contact list. This just means that the
      // registration is known to the mobilityManager but contact info
      // is not available.
      if (contacts == null) {
         throw new Exception("contact list is empty, registration not added!");
      }

      String key = registration.getKey();
      if (key == null) {
         throw new Exception("key is null, registration not added!");
      }

      for (int i = 0; i < contacts.size(); i++) {
         ContactHeader contactHeader = (ContactHeader) contacts.elementAt(i);
         if (contactHeader.getExpires() == -1) {
            contactHeader.setExpires(UnitToUnitMobilityManager.EXPIRES_TIME_MAX);
         }
         startTimer(key, contactHeader.getExpires(), contactHeader);
      }
      registrations.put(key, registration);
      logger.debug("RegistrationsTable, addRegistration(), registration "
            + " added for the key: " + key);
      printRegistrations();
   }

   public void removeRegistration(String key) {
      logger.debug("RegistrationsTable, removeRegistration(), "
            + " registration removed" + " for the key: " + key);
      registrations.remove(key);
      printRegistrations();
      //updateGUI(registration,true);
   }

   public void replaceContact(String key, ContactHeader contactHeader) throws Exception {
      logger.debug("RegistrationsTable, removeContact(), "
         + " contact removed for the key: " +key +" replacement contact = " +contactHeader);
      Registration registration = (Registration) registrations.get(key);
      // Should only be called at the home RFSS.
      this.rfss.getTestHarness().assertTrue(registration != null);
      registration.clearContacts();
      registration.addContactHeader(contactHeader);
   }

   public void removeContact(String key, ContactHeader contactHeader) {
      Registration registration = (Registration) registrations.get(key);
      if (registration != null) {
         registration.removeContactHeader(contactHeader);
         printRegistrations();
         if (!registration.hasContacts()) {
            logger.debug("RegistrationsTable, removeContact(), the registration: "
                  + key + " does not contain any contacts, we remove it");
            removeRegistration(key);
         }
      }
   }

   public void updateRegistration(String key, Request request) throws Exception {
      logger.debug("RegistrationsTable, updateRegistration(), registration updated"
                  + " for the key: " + key);
      Registration registration = (Registration) registrations.get(key);
      int expiresTime = UnitToUnitMobilityManager.EXPIRES_TIME_MAX;
      for (Iterator it = request.getHeaders(ContactHeader.NAME); it.hasNext();) {
         ContactHeader contactHeader = (ContactHeader) it.next();
         if (contactHeader.getExpires() != -1) {
            expiresTime = contactHeader.getExpires();
         } else {
            ExpiresHeader expiresHeader = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
            if (expiresHeader != null) {
               expiresTime = expiresHeader.getExpires();
            }
         }
         if (expiresTime == 0) {
            replaceContact(key, contactHeader);
         } else {
            if (expiresTime > UnitToUnitMobilityManager.EXPIRES_TIME_MAX
                  || expiresTime < UnitToUnitMobilityManager.EXPIRES_TIME_MIN)
               expiresTime = UnitToUnitMobilityManager.EXPIRES_TIME_MAX;
            // contactHeader.setExpires(expiresTime);

            if (registration.hasContactHeader(contactHeader))
               registration.updateContactHeader(contactHeader);
            else
               registration.addContactHeader(contactHeader);
            startTimer(key, expiresTime, contactHeader);
            expiresTime = UnitToUnitMobilityManager.EXPIRES_TIME_MAX;
         }
      }
      printRegistrations();
   }

   /**
    * Create an entry in the registration table ( wiping out any existing
    * record).
    * 
    * @param uri
    * @param contactHeader
    */
   public void createRegistration(URI uri, ContactHeader contactHeader, ServiceProfile serviceProfile) {
      SipURI cleanUri = (SipURI) UnitToUnitMobilityManager.getCleanUri(uri);
      String key = SipUtils.getRadicalName(cleanUri);
      Registration registration = new Registration(key,uri,contactHeader,serviceProfile,topologyConfig);
      if (logger.isDebugEnabled()) {
         logger.debug("adding registration " + key + " contactHeader = " + contactHeader);
      }
      registrations.put(key, registration);
   }

   public Registration getRegistration(String key) {
      return registrations.get(key);
   }

   /**
    * Start a timer. This will be a slee timer task.
    * 
    * @param key
    * @param expiresTime
    * @param contactHeader
    */
   public void startTimer(String key, int expiresTime, ContactHeader contactHeader) {
      // we kill the precedent timer related to this key if there is one:
      Address address = contactHeader.getAddress();
      javax.sip.address.URI cleanedUri = UnitToUnitMobilityManager.getCleanUri(address.getURI());
      String contactURI = cleanedUri.toString();

      ExpiresTask oldTimerTask = null;
      synchronized (expiresTaskTable) {
         oldTimerTask = (ExpiresTask) expiresTaskTable.get(contactURI);
      }

      if (oldTimerTask != null) {
         logger.debug("RegistrationsTable, startTimer(), An old timer has "
               + " been stopped for the contact: " + contactURI);
         oldTimerTask.cancel();
      }

      // Let's start a timer for this contact...
      ExpiresTask expiresTask = new ExpiresTask(key, contactHeader, this);
      ISSITimer.getTimer().schedule(expiresTask, expiresTime * 1000);

      synchronized (expiresTaskTable) {
         expiresTaskTable.put(contactURI, expiresTask);
      }
      logger.debug("RegistrationsTable, startTimer(), timer started "
            + " for the contact: " + contactURI + " , expiresTime:" + expiresTime);
   }

   protected void printRegistrations() {
      logger.debug("*********  Registration record *****************");
      for (Enumeration e = registrations.keys(); e.hasMoreElements();) {
         String keyTable = (String) e.nextElement();
         Registration registration = (Registration) registrations.get(keyTable);
         logger.debug("registered user: \"" + keyTable + "\"");
         registration.print();
      }
      logger.debug("************************************************");
   }

   public String getXMLTags() {
      StringBuffer retval = new StringBuffer();
      retval.append("<?xml version='1.0' encoding='us-ascii'?> \n");
      retval.append("<REGISTRATIONS> \n");
      for (Enumeration e = registrations.keys(); e.hasMoreElements();) {
         String keyTable = (String) e.nextElement();
         Registration registration = (Registration) registrations.get(keyTable);
         retval.append(registration.getXMLTags());
      }
      retval.append("</REGISTRATIONS> \n");
      return retval.toString();
   }
}
