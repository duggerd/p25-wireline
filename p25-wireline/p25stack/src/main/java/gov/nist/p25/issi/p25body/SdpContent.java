//
package gov.nist.p25.issi.p25body;

import java.text.ParseException;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.header.ContentTypeHeader;
import org.apache.log4j.Logger;

import gov.nist.javax.sdp.fields.AttributeField;
import gov.nist.javax.sdp.fields.OriginField;

/**
 *  SDP Content
 */
public class SdpContent extends Content {
   private static Logger logger = Logger.getLogger(SdpContent.class);
   
   private SessionDescription sessionDescription;

   // constructor
   public SdpContent(ContentTypeHeader contentTypeHeader, String content)
         throws ParseException {
      super(contentTypeHeader, content);
      SdpFactory sdpFactory = SdpFactory.getInstance();
      try {
         this.sessionDescription = sdpFactory.createSessionDescription(content);
      } catch (SdpParseException ex) {
         throw new ParseException(content, 0);
      }
      if (!this.validate()) {
         throw new ParseException("SDP announcement does not conform to standard ", 0);
      }
   }

   public SdpContent(ContentTypeHeader contentTypeHeader) {
      super(contentTypeHeader, null);
   }

   public static SdpContent createSdpContent() {
      SdpContent retval = new SdpContent(sdpContentTypeHeader);
      return retval;
   }

   public static SdpContent createSdpContent(String sdpcontent)
         throws ParseException {
      return new SdpContent(sdpContentTypeHeader, sdpcontent);
   }

   public SessionDescription getSessionDescription() {
      return sessionDescription;
   }

   public void setSessionDescription(SessionDescription sessionDescription) {
      this.sessionDescription = sessionDescription;
   }

   public String toString() {
      if (super.getBoundary() == null) {
         return this.sessionDescription.toString();
      }
      else {
         return new StringBuffer().append(
            super.getBoundary() + "\r\n" + getContentTypeHeader() + "\r\n"
               + this.sessionDescription.toString()).toString();
      }
   }

   @Override
   public boolean match(Content template) {
      if (!(template instanceof SdpContent))
         return false;
      try {
         SessionDescription matchSdp = ((SdpContent) template).getSessionDescription();
         MediaDescription md1 = (MediaDescription) matchSdp.getMediaDescriptions(false).get(0);
         MediaDescription md2 = (MediaDescription) this.sessionDescription
               .getMediaDescriptions(false).get(0);
         if ( md2.getAttributes(false).size() == 0)
            return false;

         AttributeField af1 = (AttributeField) md1.getAttributes(false).get(0);
         AttributeField af2 = (AttributeField) md2.getAttributes(false).get(0);
         if ( ! af1.getAttribute().getValue().equals(af2.getAttribute().getValue())) {
            logger.debug("Could not match attribute " + af2 + " " + af1 );
            return false;
         }
               
         return matchSdp.getVersion().getVersion() == this.sessionDescription.getVersion().getVersion()
               && matchSdp.getSessionName().getValue().equalsIgnoreCase(
                     this.sessionDescription.getSessionName().getValue())
               && md1.getMedia().getMediaType().equalsIgnoreCase(md2.getMedia().getMediaType())
               && md1.getMedia().getProtocol().equalsIgnoreCase(md2.getMedia().getProtocol());
      } catch (Exception ex) {
         logger.error("Unexpected exception ", ex);
         return false;
      }
   }
   
   public boolean isDefault() {
      return false;
   }

   /**
    * Validate the incoming SDP announce and return true if validation passes.
    * 
    * @return
    */
   private boolean validate() {
      //TODO: validatation
      return true;
   }
}
