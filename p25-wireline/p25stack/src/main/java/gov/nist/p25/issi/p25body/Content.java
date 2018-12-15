// 
package gov.nist.p25.issi.p25body;

import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.utils.ProtocolObjects;

import java.text.ParseException;
import javax.sip.header.ContentTypeHeader;

/**
 * P25 content for sip signaling messages is multipart mime. Each part is
 * represented by one of these structures.
 * 
 */
public abstract class Content implements Comparable<Content>
{
   public final static String CR_NL = "\r\n";

   /*
    * The content type header for P25 content.
    */
   protected static ContentTypeHeader p25ContentTypeHeader;

   /*
    * The content type header for sdp content;
    */
   protected static ContentTypeHeader sdpContentTypeHeader;

   /*
    * The content type header for this chunk of content.
    */
   private ContentTypeHeader contentTypeHeader;
   private String content;
   private String boundary;

   static {
      try {
         p25ContentTypeHeader = ProtocolObjects.getHeaderFactory().createContentTypeHeader(
               ISSIConstants.APPLICATION, ISSIConstants.X_TIA_P25_ISSI);
         sdpContentTypeHeader = ProtocolObjects.getHeaderFactory().createContentTypeHeader(
               ISSIConstants.APPLICATION, ISSIConstants.SDP);
      } catch (ParseException ex) {
         throw new RuntimeException(ex);
      }
   }

   // constructor
   public Content(ContentTypeHeader ctHeader, String content) {
      this.content = content;
      this.contentTypeHeader = ctHeader;
   }

   // accessors
   public ContentTypeHeader getContentTypeHeader() {
      return contentTypeHeader;
   }
   public void setContentTypeHeader(ContentTypeHeader ctHeader) {
      this.contentTypeHeader = ctHeader;
   }

   public String getContent() {
      return content;
   }
   public void setContent(String content) {
      this.content = content;
   }

   public String getBoundary() {
      return boundary;
   }
   public void setBoundary(String boundary) {
      this.boundary = boundary;
   }

   /**
    * The default packing method. This packs the content to be appended
    * to the sip message.
    */
   public String toString() {
      // This is not part of a multipart message.
      if (boundary == null) {
         return content;
      } else {
         return boundary +CR_NL +contentTypeHeader +CR_NL +content;
      }
   }
   
   /**
    * Return true if this object has all default parameters set 
    * (so it does not get converted to string when packing).
    */
   public abstract boolean isDefault();
   
   /**
    * Match with a given content.
    */
   public abstract boolean match(Content template);

   // implementation of Comparable
   //--------------------------------------------------------------------
   public int compareTo( Content other) {
      String key = this.getContentTypeHeader().getContentType() +":" +
            this.getContentTypeHeader().getContentSubType();
      String key2 = other.getContentTypeHeader().getContentType() +":" +
            other.getContentTypeHeader().getContentSubType();
      return key.compareTo( key2);
   }
}
