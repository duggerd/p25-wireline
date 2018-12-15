//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfile;

import java.util.Iterator;
import java.util.Vector;
import javax.sip.address.*;
import javax.sip.header.*;

import org.apache.log4j.Logger;

/**
 * Registration
 */
@SuppressWarnings("unchecked")
public class Registration {
   private static Logger logger = Logger.getLogger(Registration.class);

   private boolean toExport = true;
   private String key;
   private URI uri;
   private ServiceProfile serviceProfile;
   private TopologyConfig topologyConfig;
   private Vector<ContactHeader> contactsList = new Vector<ContactHeader>();

   public Registration(String key, URI uri, ContactHeader contactHeader,
         ServiceProfile serviceProfile, TopologyConfig topologyConfig) {
      this.uri = uri;
      this.contactsList.add(contactHeader);
      this.serviceProfile = serviceProfile;
      this.topologyConfig = topologyConfig;
   }

   public Registration(String key, URI uri, Iterator contacts,
         ServiceProfile serviceProfile, TopologyConfig topologyConfig) {
      this.uri = uri;
      this.key = key;
      while (contacts.hasNext()) {
         ContactHeader contactHeader = (ContactHeader) contacts.next();
         contactsList.add(contactHeader);
      }
      this.serviceProfile = serviceProfile;
      this.topologyConfig = topologyConfig;
   }

   protected ExportedBinding exportBinding() {
      if (!toExport)
         return null;
      ExportedBinding retval = new ExportedBinding();
      if (contactsList != null && contactsList.size() > 0) {
         String address = ((ContactHeader) contactsList.elementAt(0)).getAddress().toString();
         retval.setContactAddress( address);
      }
      retval.setKey( uri);
      toExport = false;
      return retval;
   }

   public Vector<ContactHeader> getContactsList() {
      return contactsList;
   }

   public ContactHeader getContact() {
      if (contactsList != null && contactsList.size() > 0) {
         return contactsList.get(0);
      }
      return null;
   }
   
   public RfssConfig getCurrentRfss() {
      ContactHeader contact = getContact();
      if( contact != null) {
         String radicalName = ((SipURI)contact.getAddress().getURI()).getHost();
         return topologyConfig.getRfssConfig(radicalName);
      }
      return null;
   }

   public void addContactHeader(ContactHeader contactHeader) {
      contactsList.addElement(contactHeader);
   }

   public String getKey() {
      return key;
   }

   public boolean hasContacts() {
      return !contactsList.isEmpty();
   }

   public void removeContactHeader(ContactHeader contactParameter) {
      Address addressParam = contactParameter.getAddress();
      javax.sip.address.URI cleanUri = UnitToUnitMobilityManager.getCleanUri(addressParam.getURI());
      String contactParam = cleanUri.toString();

      for (int i = 0; i < contactsList.size(); i++) {
         ContactHeader contactHeader = (ContactHeader) contactsList.elementAt(i);
         Address address = contactHeader.getAddress();
         javax.sip.address.URI cleanedUri = UnitToUnitMobilityManager.getCleanUri(address.getURI());
         String contact = cleanedUri.toString();

         if (contact.equals(contactParam)) {
            contactsList.remove(i);
            if (logger.isDebugEnabled())
               logger.debug("DEBUG, Registration, removeContactHeader():"
                     + " The contact: " + contact + " has been removed for the key: " + key);
            break;
         }
      }
   }

   public void updateContactHeader(ContactHeader contactParameter) {

      Address addressParam = contactParameter.getAddress();
      javax.sip.address.URI cleanUri = UnitToUnitMobilityManager.getCleanUri(addressParam.getURI());
      String contactParam = cleanUri.toString();

      for (int i = 0; i < contactsList.size(); i++) {
         ContactHeader contactHeader = (ContactHeader) contactsList.elementAt(i);
         Address address = contactHeader.getAddress();
         javax.sip.address.URI cleanedUri = UnitToUnitMobilityManager.getCleanUri(address.getURI());
         String contact = cleanedUri.toString();

         if (contact.equals(contactParam)) {
            contactsList.remove(i);
            contactsList.add(i, contactParameter);
            logger.debug("DEBUG, Registration, updateContactHeader():"
                  + " The contact: " + contact + " has been updated for the key: " + key);
            break;
         }
      }
   }

   public boolean hasContactHeader(ContactHeader contactParameter) {
      Address addressParam = contactParameter.getAddress();
      javax.sip.address.URI cleanUri = UnitToUnitMobilityManager.getCleanUri(addressParam.getURI());
      String contactParam = cleanUri.toString();
      logger.debug("Contact to add:" + contactParam + " ?");
      for (int i = 0; i < contactsList.size(); i++) {
         ContactHeader contactHeader = (ContactHeader) contactsList.elementAt(i);
         Address address = contactHeader.getAddress();
         javax.sip.address.URI cleanedUri = UnitToUnitMobilityManager.getCleanUri(address.getURI());
         String contact = cleanedUri.toString();

         logger.debug("Contact in the list:" + contact);
         if (contact.equals(contactParam)) {
            logger.debug("Contact already in the list");
            return true;
         }
      }
      return false;
   }

   public void print() {
      logger.debug("- contacts: ");
      for (int i = 0; i < contactsList.size(); i++) {
         ContactHeader contactHeader = (ContactHeader) contactsList.elementAt(i);
         logger.debug("  contact " + (i + 1) + " : " + contactHeader.toString());
      }
   }

   public String getXMLTags() {
      StringBuffer retval = new StringBuffer();
      retval.append("<REGISTRATION ");
      retval.append(" uri=\"" + key + "\" ");

      for (int i = 0; i < contactsList.size(); i++) {
         retval.append("     <CONTACT ");
         ContactHeader contactHeader = (ContactHeader) contactsList.elementAt(i);
         Address address = contactHeader.getAddress();
         javax.sip.address.URI cleanedUri = UnitToUnitMobilityManager.getCleanUri(address.getURI());
         String contact = cleanedUri.toString();
         if (address.getDisplayName() != null) {
            retval.append("display_name=\"" + address.getDisplayName() + "\"");
         }

         retval.append(" uri=\"" + contact + "\" ");
         if (contactHeader.getExpires() != -1) {
            retval.append(" expires=\"" + contactHeader.getExpires() + "\" ");
         } else {
            retval.append(" expires=\"" + UnitToUnitMobilityManager.EXPIRES_TIME_MAX + "\" ");
         }
         retval.append(" />\n");
      }
      retval.append("</REGISTRATION>\n");
      return retval.toString();
   }

   public ServiceProfile getServiceProfile() {
      return serviceProfile;
   }

   public void clearContacts() {
      this.contactsList.removeAllElements();
   }

   public URI getUri() {
      return uri;
   }
}
