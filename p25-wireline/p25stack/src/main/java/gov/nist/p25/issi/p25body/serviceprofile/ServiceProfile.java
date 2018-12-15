//
package gov.nist.p25.issi.p25body.serviceprofile;

import java.text.ParseException;
import java.util.Hashtable;
import java.util.LinkedHashMap;

//import org.apache.log4j.Logger;

public abstract class ServiceProfile {
   //private static Logger logger = Logger.getLogger(GroupServiceProfile.class);

   //protected Hashtable<String, ServiceProfileLine> profileLines;
   protected LinkedHashMap<String, ServiceProfileLine> profileLines;

   /**
    * This table is used by the factory to create a structure of the
    * appropriate type when parsing a line.
    */
   protected static Hashtable<String, Class<? extends ServiceProfileLine>> profileLineClasses = 
         new Hashtable<String, Class<? extends ServiceProfileLine>>();

   protected static Class<? extends ServiceProfileLine> getProfileLineClass( String fieldName) {
      return profileLineClasses.get(fieldName.toLowerCase());
   }

   /**
    * Get a profile line of a specified name.
    * 
    * @param name 
    *            the name of the profile line.
    */
   protected ServiceProfileLine getByName(String name) {
      return this.profileLines.get(name.toLowerCase());
   }

   /**
    * Add a group service profile line.
    * 
    * @param serviceProfileLine --
    *            a profile line to add.
    */
   protected void addProfileLine(ServiceProfileLine serviceProfileLine) {
      profileLines.put(serviceProfileLine.getName().toLowerCase(), serviceProfileLine);
   }

   public String toXmlString() {
      StringBuffer sb = new StringBuffer();
      sb.append("<").append(getXMLTag());
      sb.append(">").append("\n");
      sb.append("<![CDATA[\n");
      sb.append(this.toString());
      sb.append("]]>");
      sb.append("\n</").append(getXMLTag());
      sb.append(">\n");
      return sb.toString();
   }

   protected static void parseProfileLines(ServiceProfile retval, String profile)
         throws ParseException {
      // Be generous in what you accept. Unixen use CR for line termination.
      // When stored in
      // a file and edited, it could easily replace CRLF with CR and hence we
      // accept both
      // CRLF and CR as terminators.
      String buffer = profile.replaceAll("\r\n", "\n");
      String[] profileLines = buffer.split("\n");
      for (String line : profileLines) {
         String trimmedLine = line.trim();
         if (!trimmedLine.equals("")) {
            ServiceProfileLine profileLine = retval
                  .createServiceProfileLine(line.trim());
            retval.addProfileLine(profileLine);
         }
      }
   }

   /**
    * Get the profile as an xml tag.
    */
   protected abstract String getXMLTag();
   
   /**
    * match the service profile line with a template.
    * 
    * @param template -- the template to match against.
    * 
    * @return true if the match succeeds.
    */
   public abstract boolean match(Object template);
   
   protected abstract ServiceProfileLine createServiceProfileLine(
         String serviceProfileLine) throws ParseException;
}
