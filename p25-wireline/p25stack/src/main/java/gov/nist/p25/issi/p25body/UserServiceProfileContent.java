//
package gov.nist.p25.issi.p25body;

import java.text.ParseException;
import javax.sip.header.ContentTypeHeader;

import gov.nist.p25.issi.p25body.serviceprofile.user.UserServiceProfile;

/**
 * User Service Profile Content class
 */
public class UserServiceProfileContent extends Content {
   private UserServiceProfile userServiceProfile;

   // constructors
   UserServiceProfileContent(ContentTypeHeader contentType,
         String userServiceProfileContent) throws ParseException {
      super(contentType, userServiceProfileContent);
      this.userServiceProfile = UserServiceProfile
            .createUserServiceProfile(userServiceProfileContent);
      super.setContent(userServiceProfileContent);
   }

   /**
    * @return Returns the UserServiceProfile.
    */
   public UserServiceProfile getUserServiceProfile() {
      return userServiceProfile;
   }

   /**
    * create an instance of this object.
    */
   public UserServiceProfileContent createUserServiceProfileContent(
         String uspcontent) throws ParseException {
      return new UserServiceProfileContent(p25ContentTypeHeader, uspcontent);
   }

   @Override
   public String toString() {
      if (super.getBoundary() == null) {
         return this.userServiceProfile.toString();
      }
      else {
         return new StringBuffer().append(
            super.getBoundary() + "\r\n" + getContentTypeHeader() + "\r\n"
               + this.userServiceProfile.toString()).toString();
      }
   }

   @Override
   public boolean match(Content other) {
      if ( !(other instanceof UserServiceProfileContent))
         return false;
      UserServiceProfileContent that = (UserServiceProfileContent) other;
      return this.userServiceProfile.match(that.userServiceProfile);
   }

   @Override
   public boolean isDefault() {
      return false;
   }
}
