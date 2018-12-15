//
package gov.nist.p25.issi.p25body.serviceprofile.group;

import java.text.ParseException;

import gov.nist.p25.issi.p25body.serviceprofile.ServiceProfileLine;

/**
 * The Announcement Group attribute defines the announcement group for the group.
 * It has the following format in ABNF notation: <br/> announcement-group =
 * g-agroup: / g-ag: 12HEX-DIGIT CRLF <br/> where the 12HEX-DIGIT represent the
 * fully-qualified hexadecimal form of the AGID. If no announcement group
 * attribute is included, then the group has no announcement group.
 * 
 */
public class AnnouncementGroup extends ServiceProfileLine {

   public static final String NAME = "g-agroup";
   public static final String SHORTNAME = "g-ag";

   private long groupId;

   /**
    * Constructor with supplied group Id as an int.
    */
   AnnouncementGroup() {
      super(NAME, 0L);
   }
   AnnouncementGroup(long groupId) {
      super(NAME, groupId);
      this.groupId = groupId;
   }
   /**
    * Constructor
    * 
    * @param groupId --
    *            group id for the announcement group.
    * @throws ParseException --
    *             if not a properly formatted hex string.
    */
   AnnouncementGroup(String groupId) throws ParseException {
      super(NAME, groupId);
      try {
         this.groupId = Long.parseLong(groupId, 16);
      } catch (NumberFormatException ex) {
         throw new ParseException("Invalid announcement group id [" +groupId +"]", 0);
      }
   }

   /**
    * get the group ID.
    */
   public long getGroupId() {
      return groupId;
   }

   /**
    * Set the group Id.
    * 
    * @param groupId
    */
   public void setGroupId(long groupId) {
      this.groupId = groupId;
      super.setValue(groupId);
   }

   @Override
   public String toString() {
      //System.out.println("+++ AnnouncementGroup: groupId="+groupId);
      if( groupId == 0L)
         return "";
      return super.toString();
   }
}
