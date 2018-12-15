//
package gov.nist.p25.issi.p25body;

import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;

import javax.sip.header.*;
import javax.sip.message.Message;
import org.apache.log4j.Logger;

import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.utils.ProtocolObjects;

/**
 * Content list for multipart mime content type.
 */
public class ContentList extends java.util.LinkedList<Content> {

   private static final long serialVersionUID = -1L;
   private static Logger logger = Logger.getLogger(ContentList.class);
   
   private static boolean verbose = false;
   private static final ContentTypeHeader multipartMimeContentTypeHeader;

   static {
      try {
         multipartMimeContentTypeHeader = 
             ProtocolObjects.getHeaderFactory().createContentTypeHeader(
               ISSIConstants.TAG_MULTIPART, ISSIConstants.TAG_MIXED);
         multipartMimeContentTypeHeader.setParameter(ISSIConstants.TAG_BOUNDARY,
               "\"" + ISSIConstants.BODY_BOUNDARY + "\"");
         //logger.debug("multipartMimeContentTypeHeader=" +multipartMimeContentTypeHeader);
      } catch (ParseException ex) {
         throw new RuntimeException(ex);
      }
   }
   
   private Content unmatchedContent;
   
   // accessor
   public Content getUnmatchedContent() {
      return unmatchedContent;
   }

   // constructor
   public ContentList() {
      this.add(CallParamContent.createCallParamContent());
      this.add(RegisterParamContent.createRegisterParamContent());
   }

   private int packingSize() {
      int retval = 0;
      for (Content content : this) {
         if (!content.isDefault())
            retval++;
      }
      return retval;
   }

   @Override
   public boolean add(Content content) {
      for (Iterator<Content> ci = this.iterator(); ci.hasNext();) {
         if (ci.next().getClass().equals(content.getClass())) {
            ci.remove();
         }
      }
      boolean bflag = super.add(content);
      // sort content list
      Collections.sort( this);
      return bflag;
   }

   /**
    * Return the Content type header to assign to the outgoing sip meassage.
    * 
    * @return the ContentTypeHeader object
    */
   public ContentTypeHeader getContentTypeHeader() {
      if (this.packingSize() > 1) {
         return multipartMimeContentTypeHeader;
      } else {
         for (Content content : this) {
            if (!content.isDefault())
               return content.getContentTypeHeader();
         }
         return null;
      }
   }

   @Override
   public String toString() {
      if (this.packingSize() > 1) {
         for (Content content : this) {
            // for dynamic content removal 
            if (!content.isDefault()) 
               content.setBoundary( ISSIConstants.CONTENT_BOUNDARY);
	    else
               content.setBoundary( null);
         }
      }

      StringBuffer sbuf = new StringBuffer();
      for( int i=0; i < this.size(); i++) {
         Content content = (Content)get(i);
         if (!content.isDefault()) {
            sbuf.append(content.toString());
            //=== if( i > 0 && i < this.size()-1)
               sbuf.append("\r\n");
         }
      }
      if (this.packingSize() > 1) {
         // #364 CR-NL are not mandatory.
         //=== sbuf.append("\r\n");
         sbuf.append(ISSIConstants.CONTENT_BOUNDARY+"--");
      }
      return sbuf.toString();
   }

   /**
    * Get only the specified content from this list of contents. This is used
    * for in-place editing of the SDP
    */
   public Content getContent(String contentType, String contentSubtype) {
      Content retval = null;
      for (Content content : this) {
         ContentTypeHeader contentTypeHeader = content.getContentTypeHeader();
         if (contentTypeHeader.getContentType().equals(contentType) &&
             contentTypeHeader.getContentSubType().equals(contentSubtype)) {
            retval = content;
         }
      }
      return retval;
   }

   /**
    * unpack a multipart mime packet and return a list of content packets.
    * 
    * @param body -- The body of content
    *
    * @return -- an iterator of Content blocks.
    */
   private static ContentList createContentList(String body)
         throws ParseException {

      String delimiter = ISSIConstants.CONTENT_BOUNDARY;
      String[] fragments = body.split(delimiter);
      if (verbose && logger.isDebugEnabled()) {
         logger.debug("nFragments = " + fragments.length);
         logger.debug("delimiter = " + delimiter);
         logger.debug("body = " + body);
      }
      ContentList llist = new ContentList();

      for (String nextPart: fragments) {

         // NOTE - we are not hanlding line folding for the sip header here.
         StringBuffer strbuf = new StringBuffer(nextPart);
         while (strbuf.length() > 0 &&
               (strbuf.charAt(0) == '\r' || strbuf.charAt(0) == '\n'))
            strbuf.deleteCharAt(0);

         if (strbuf.length() == 0)
            continue;
         nextPart = strbuf.toString();
         int position = nextPart.indexOf("\r\n");
         int off = 4;
         if (position == -1) {
            position = nextPart.indexOf("\n");
            off = 2;
         }
         //logger.debug("position="+position+"  nextPart=["+nextPart+"]");
         if (position == -1 || position == 2) {
            if( nextPart.startsWith("--"))
               break;  // all done ?
            throw new ParseException("no content type header found in " + nextPart, 0);
         }

         String rest = nextPart.substring(position + off);
         if (rest == null)
            throw new ParseException("No content [" + nextPart + "]", 0);

         //logger.debug("rest="+rest.length()+" [[" + rest + "]]");

         String contentType = nextPart.substring(0, position);
         int pos = contentType.indexOf(":");
         ContentTypeHeader ctHeader = null;
         try {
            ctHeader = (ContentTypeHeader) ProtocolObjects.getHeaderFactory().createHeader(
                  contentType.substring(0, pos), contentType.substring(pos+1).trim());
         } catch (ClassCastException ex) {
            logger.debug("body = " + body);
            throw new ParseException( "Expecting a ContentTypeHeader got [" + contentType + "]", 0);
         }

         Content content = null;
         if (ISSIConstants.APPLICATION.equals(ctHeader.getContentType()) &&
             ISSIConstants.SDP.equalsIgnoreCase(ctHeader.getContentSubType())) {
            content = new SdpContent(ctHeader, rest);

            // Set the delimiter - this is to be set for a multipart message
            // so that the pack routine will know what to do with it.
            // content.setBoundary( delimiter);

         } else if (ISSIConstants.APPLICATION.equals(ctHeader.getContentType()) &&
               ISSIConstants.X_TIA_P25_ISSI.equalsIgnoreCase(ctHeader.getContentSubType())) {
            if (rest.startsWith("u-")) {
               content = new UserServiceProfileContent(ctHeader, rest);
            } else if (rest.startsWith("g-")) {
               content = new GroupServiceProfileContent(ctHeader, rest);
            } else if (rest.startsWith("c-")) {
               //logger.debug("c-rest= [" + rest + "]");
               content = new CallParamContent(ctHeader, rest);
               //logger.debug("c-content= [" + content + "]");
            } else if (rest.startsWith("r-")) {
               content = new RegisterParamContent(ctHeader, rest);
            } else if (rest.length() == 0) {
               // empty: nothing to do
               logger.debug("rest=[empty]");
            } else {
               // 20.4.x
               if( rest.trim().length() > 0) 
                  throw new ParseException("Unrecognized content:<" +rest+">", 0);
               //content.setBoundary( delimiter);
	    }
         } else {
            throw new ParseException("Unknown content type [" + ctHeader + "]", 0);
         }
         if( content != null)
            llist.add(content);
      }  // for-loop

      //#211: sort content list
      Collections.sort( llist);
      return llist;
   }

   /**
    * Get a desired content with a given content type/subtype from a list of Content
    * 
    * @param contents --
    *            Unpacked list of contents.
    * @param contentType --
    *            Content type we are looking for (i.e. application)
    * @param contentSubtype --
    *            content subtype we are looking for (ie. sdp)
    * @return the Content unpacked or null
    */
   public static Content getContentByType(ContentList contents,
         String contentType, String contentSubtype) {
      Content retval = null;
      for (Content content : contents) {
         ContentTypeHeader ctHdr = content.getContentTypeHeader();
         if (ctHdr.getContentType().equalsIgnoreCase(contentType) &&
             ctHdr.getContentSubType().equalsIgnoreCase(contentSubtype)) {
            retval = content;
            break;
         }
      }
      return retval;
   }

   /**
    * Set the content by its type.
    * 
    * @param type --
    *            the type of the content
    * @param subType --
    *            the subtype of the content.
    * @param content
    *            the content
    */
   public void setContent(String type, String subType, Content content) {

      Iterator<Content> it = this.iterator();
      while (it.hasNext()) {
         Content ct = it.next();
         ContentTypeHeader ctHdr = ct.getContentTypeHeader();
         if (ctHdr.getContentType().equalsIgnoreCase(type) &&
             ctHdr.getContentSubType().equalsIgnoreCase(subType)) {
            it.remove();
         }
      }
      this.add(content);
   }

   /**
    * Get the SDP Content from this content list.
    */
   public SdpContent getSdpContent() {
      for (Content content : this) {
         if (content instanceof SdpContent)
            return (SdpContent) content;
      }
      return null;
   }

   /**
    * Get the Group Service Profile content from this list.
    */
   public GroupServiceProfileContent getGroupServiceProfileContent() {
      for (Content content : this) {
         if (content instanceof GroupServiceProfileContent)
            return (GroupServiceProfileContent) content;
      }
      return null;
   }

   /**
    * Get the registration parameters content from this .
    */
   public RegisterParamContent getRegisterParamContent() {
      for (Content content : this) {
         if (content instanceof RegisterParamContent)
            return (RegisterParamContent) content;
      }
      return null;
   }

   /**
    * Get the call param content form this list.
    */
   public CallParamContent getCallParamContent() {
      for (Content content : this) {
         if (content instanceof CallParamContent)
            return (CallParamContent) content;
      }
      return null;
   }

   /**
    * Get the User Service Profile from this message.
    */
   public UserServiceProfileContent getUserServiceProfileContent() {
      for (Content content : this) {
         if (content instanceof UserServiceProfileContent)
            return (UserServiceProfileContent) content;
      }
      return null;
   }

   /**
    * Extracts a list of content from the Request.
    * 
    * @param message --
    *            message with a multipart mime attachment.
    * @return -- a ContentList with a list of Content - one for each content
    *         type/subtype.
    * 
    */
   public static ContentList getContentListFromMessage(Message message)
         throws ParseException {

      if (message.getContentLength().getContentLength() == 0)
         return new ContentList();

      ContentTypeHeader contentTypeHeader = (ContentTypeHeader) message.getHeader(ContentTypeHeader.NAME);
      if (contentTypeHeader == null)
         throw new ParseException("Missing ContentType Header in message \n" + message.toString(), 0);
      byte[] rawContent = message.getRawContent();
      String body = new String(rawContent); // .toLowerCase();

      if(verbose)
         logger.debug("getContentListFromMesge(): body=[" + body + "]");

      ContentList contents = null;
      String contentType = contentTypeHeader.getContentType();
      String contentSubType = contentTypeHeader.getContentSubType();
      //logger.debug("getContentListFromMesge(): contentType=" + contentType);
      //logger.debug("getContentListFromMesge(): contentSubType=" + contentSubType);

      if (ISSIConstants.TAG_MULTIPART.equalsIgnoreCase(contentType) &&
          ISSIConstants.TAG_MIXED.equalsIgnoreCase(contentSubType)) {
         try {
            contents = createContentList(body);
         } catch (ParseException ex) {
            logger.error("Error parsing " + message, ex);
            throw ex;
         }

      } else if (ISSIConstants.APPLICATION.equalsIgnoreCase(contentType) &&
            ISSIConstants.SDP.equalsIgnoreCase(contentSubType)) {

         SdpContent sdpContent = new SdpContent(contentTypeHeader, body);
         //logger.debug("getContentListFromMessage: sdpContent="+sdpContent);
	 
         contents = new ContentList();
	 if(sdpContent != null)
            contents.add(sdpContent);

      } else if (ISSIConstants.APPLICATION.equalsIgnoreCase(contentType) &&
            ISSIConstants.X_TIA_P25_ISSI.equalsIgnoreCase(contentSubType)) {
         contents = new ContentList();
         Content content;
         if (body.startsWith("u-")) {
            content = new UserServiceProfileContent(contentTypeHeader, body);
         } else if (body.startsWith("g-")) {
            content = new GroupServiceProfileContent(contentTypeHeader, body);
         } else if (body.startsWith("c-")) {
            content = new CallParamContent(contentTypeHeader, body);
         } else if (body.startsWith("r-")) {
            content = new RegisterParamContent(contentTypeHeader, body);
         } else {
            throw new ParseException("Unrecognized content ===>> " + body, 0);
         }
         contents.add(content);

      } else {
         throw new ParseException("Unrecognized Content Type [ " +contentTypeHeader +"]", 0);
      }
      return contents;
   }

   public void sort() {
      Collections.sort( this);
   }

   /**
    * Compare a content list with another content list.
    * 
    * @param template 
    *            The content list to compare with.
    * @return the boolean match result.
    */
   public boolean match(ContentList template) {
      boolean compareCallParam = true;
      boolean compareRegisterParam = true;
      boolean matchFound = false;

      // Check to see if the template is the default. If so then dont encode it.
      if (template.getCallParamContent() != null
            && template.getCallParamContent().getCallParam().isDefault()
            && this.getCallParamContent() == null) {
         compareCallParam = false;
      }
      if (template.getRegisterParamContent() != null
            && template.getRegisterParamContent().getRegisterParam()
                  .isDefault() && this.getRegisterParamContent() == null) {
         compareRegisterParam = false;
      }
      for (Content content : template) {
         if (content instanceof RegisterParamContent && !compareRegisterParam) {
            continue;
         }
         if (content instanceof CallParamContent && !compareCallParam) {
            continue;
         }
         logger.debug("currentContent=" + content);
         if( content==null || 
             "\r\n".equals(content.toString()) ||
             content.toString().trim().length()==0) {
             logger.debug("   ...ignore currentContent.");
             continue;
         }
	 //---------------------------------------------
         matchFound = false;
         for (Content matchContent : this) {
            logger.debug("matchContent = " + matchContent);
            if (content.match(matchContent)) {
               matchFound = true;
               break;
            }
         }
         if (!matchFound) {
            logger.error("Match not found for " + content);
            logger.error("Content to check = " + this);
            unmatchedContent = content;
            return false;
         } else {
            logger.debug("Found a match");
            logger.debug("Match found for " + content);
         }
      }
      return true;
   }
}
