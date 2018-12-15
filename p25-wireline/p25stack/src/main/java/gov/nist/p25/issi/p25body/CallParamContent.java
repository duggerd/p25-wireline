//
package gov.nist.p25.issi.p25body;

import java.text.ParseException;
import javax.sip.header.ContentTypeHeader;

import gov.nist.p25.issi.p25body.params.CallParam;

/**
 * CallParamContent
 */
public class CallParamContent extends Content {

   private CallParam callParam;

   // constructor
   CallParamContent(ContentTypeHeader contentType, String callParamContent)
         throws ParseException {
      super(contentType, callParamContent);
      this.callParam = CallParam.createCallParam(callParamContent);
      super.setContent(callParamContent);
   }
   
   CallParamContent(ContentTypeHeader contentType) {
      super(contentType,null);
      this.callParam = new CallParam();
   }

   /**
    * Create an instance of this object given a string containing the call
    * param body.
    * 
    * @param callParamContent
    * @return The CallParamContent object.
    * @throws ParseException
    */
   public static CallParamContent createCallParamContent(String callParamContent)
         throws ParseException {
      return new CallParamContent(p25ContentTypeHeader, callParamContent);
   }
   
   /**
    * Create a default call param content header.
    * This has a default call param assigned.
    */
   public static CallParamContent createCallParamContent() {
      return new CallParamContent(p25ContentTypeHeader);
   }

   /**
    * @return Returns the callParam.
    */
   public CallParam getCallParam() {
      return callParam;
   }

   @Override
   public String toString() {
      if (super.getBoundary() == null) {
         return this.callParam.toString();
      }
      else {
         return new StringBuffer().append(
            super.getBoundary() + "\r\n" + getContentTypeHeader() + "\r\n" +
            this.callParam.toString()).toString();
      }
   }

   @Override
   public boolean match(Content template) {
      if (!(template instanceof CallParamContent))
         return false;
      CallParam templateContent = ((CallParamContent) template).callParam;
      return callParam.match( templateContent);
   }

   @Override
   public boolean isDefault() {
      return this.callParam.isDefault();
   }
}
