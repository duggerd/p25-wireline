//
package gov.nist.p25.issi.p25body;

import java.text.ParseException;
import javax.sip.header.ContentTypeHeader;

import gov.nist.p25.issi.p25body.serviceprofile.group.GroupServiceProfile;

/**
 * Group Service Profile Content
 */
public class GroupServiceProfileContent extends Content {
   private GroupServiceProfile groupServiceProfile;

   // constructor
   GroupServiceProfileContent(ContentTypeHeader contentType,
         String groupServiceProfileContent) throws ParseException {
      super(contentType, groupServiceProfileContent);
      this.groupServiceProfile = GroupServiceProfile
            .createGroupServiceProfile(groupServiceProfileContent);
      super.setContent(groupServiceProfileContent);
   }

   public static GroupServiceProfileContent createGroupServiceProfileContent(
         String groupServiceProfileContent) throws ParseException {
      return new GroupServiceProfileContent(p25ContentTypeHeader,
         groupServiceProfileContent);
   }

   /**
    * @return Returns the groupServiceProfile.
    */
   public GroupServiceProfile getGroupServiceProfile() {
      return groupServiceProfile;
   }

   @Override
   public String toString() {
      if (super.getBoundary() == null) {
         return this.groupServiceProfile.toString();
      }
      else {
         return new StringBuffer().append(
            super.getBoundary() + "\r\n" + getContentTypeHeader() + "\r\n"
               + this.groupServiceProfile.toString()).toString();
      }
   }
   
   @Override
   public boolean isDefault() {
      return false;
   }

   @Override
   public boolean match(Content template) {
      return true;
   }
}
