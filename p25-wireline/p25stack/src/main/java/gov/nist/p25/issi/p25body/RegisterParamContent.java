//
package gov.nist.p25.issi.p25body;

import java.text.ParseException;
import javax.sip.header.ContentTypeHeader;
//import org.apache.log4j.Logger;

import gov.nist.p25.issi.p25body.params.RegisterParam;

/**
 * Register Param Content.
 */
public class RegisterParamContent extends Content {
   //private static Logger logger = Logger.getLogger(RegisterParamContent.class);
   
   private RegisterParam registerParam;

   // constructor
   RegisterParamContent(ContentTypeHeader contentType,
         String registerParamContent) throws ParseException {
      super(contentType, registerParamContent);
      this.registerParam = RegisterParam.createRegisterParam(registerParamContent);      
   }
   
   private RegisterParamContent(ContentTypeHeader contentTypeHeader,
         RegisterParam registerParam) {
      super(contentTypeHeader,registerParam.toString());
      this.registerParam = registerParam;
   }
   
   /**
    * @return Returns the registerParam.
    */
   public RegisterParam getRegisterParam() {
      return registerParam;
   }

   @Override
   public String toString() {
      if (super.getBoundary() == null) {
         return this.registerParam.toString();
      }
      else {
         return new StringBuffer().append(
            super.getBoundary() + "\r\n" + getContentTypeHeader() + "\r\n"
               + this.registerParam.toString()).toString();
      }
   }

   @Override
   public boolean match(Content template) {
      if ( !(template instanceof RegisterParamContent)) 
         return false;
      else  
         return  this.registerParam.equals(((RegisterParamContent)template).getRegisterParam());      
   }

   @Override
   public boolean isDefault() {
      return this.registerParam.isDefault();
   }

   public static RegisterParamContent createRegisterParamContent(
         String registerParamContent) throws ParseException {
      return new RegisterParamContent(p25ContentTypeHeader, registerParamContent);
   }

   public static Content createRegisterParamContent() {      
      return new RegisterParamContent(p25ContentTypeHeader,new RegisterParam());
   }
}
