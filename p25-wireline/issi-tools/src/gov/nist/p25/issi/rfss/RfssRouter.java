//
package gov.nist.p25.issi.rfss;

import gov.nist.javax.sip.stack.DefaultRouter;
import gov.nist.p25.issi.constants.ISSIConstants;
import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.rfss.SipUtils;
import gov.nist.p25.issi.p25body.serviceprofile.RadioInhibitType;

import javax.sip.SipException;
import javax.sip.SipStack;
import javax.sip.address.Hop;
import javax.sip.address.Router;
import javax.sip.address.SipURI;
import javax.sip.header.FromHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;

import org.apache.log4j.Logger;

/**
 * RFSS Router class
 */
//public class RfssRouter extends DefaultRouter implements Router {
public class RfssRouter extends DefaultRouter {

   private static Logger logger = Logger.getLogger(RfssRouter.class);

   private static boolean verbose = false;
   private TopologyConfig topologyConfig;
   private RFSS rfss;

   // constructor
   public RfssRouter(SipStack sipStack, String outboundProxy) {
      super(sipStack, outboundProxy);
   }
   public RfssRouter(TopologyConfig topologyConfig, SipStack sipStack) {
      super(sipStack, null);
   }

   // accessor
   public void setTopologyConfig(TopologyConfig topologyConfig) {
      this.topologyConfig = topologyConfig;
   }
   
   public void setRfss(RFSS rfss) {
      this.rfss = rfss;
   }

   public Hop getNextHop(Request request) throws SipException {
      SipURI requestUri = (SipURI) request.getRequestURI();
      try {
         String user = requestUri.getUserParam();
         RouteHeader routeHeader = (RouteHeader) request.getHeader(RouteHeader.NAME);
         String method = request.getMethod();
         //if(verbose)
         logger.debug("KKK getNextHop: user=" +user +" method=" +method +" request=\n" +request);

         if ( routeHeader == null) {
            if(verbose)
               logger.debug("KKK getNextHop: null routeHeader...");
            if (Request.REGISTER.equals(method)) {
               return super.getNextHop(request);

            } else if (SipUtils.isGroup(user)
                  && SipUtils.isHostP25Dr(requestUri.getHost()) ) {
               String radicalName = requestUri.getUser();
               if(verbose)
                  logger.debug("KKK getNextHop: SG-1: radicalName="+radicalName);
               GroupConfig groupConfig = topologyConfig.getGroupConfig(radicalName);
               String ipAddress = groupConfig.getHomeRfss().getIpAddress();
               int port = groupConfig.getHomeRfss().getSipPort();
               //logger.debug("KKK getNextHop: "+groupConfig.getGroupName()+" ip="+ipAddress+":"+port);
               return new HopImpl(ipAddress, port);

            } else if (SipUtils.isGroup(user) &&
                  (Request.ACK.equals(method) || Request.BYE.equals(method)) ) {
                  //#705 10.2.x add BYE check
               // This ugly hack is here because the ISSI spec is in violation of 
               // RFC 3261.
               String rfssDomainName = requestUri.getHost();
               if(verbose)
                  logger.debug("KKK getNextHop: SG-2: rfssDomainName="+rfssDomainName);
               RfssConfig rfssConfig = topologyConfig.getRfssConfig(rfssDomainName);
               String ipAddress = rfssConfig.getIpAddress();
               int port = rfssConfig.getSipPort();
               requestUri.setHost( ISSIConstants.P25DR);
               //logger.debug("KKK getNextHop: "+rfssConfig.getRfssName()+" ip="+ipAddress+":"+port);
               return new HopImpl(ipAddress,port);
               
            } else if (SipUtils.isSubscriberUnit(user)
                  && SipUtils.isHostP25Dr(requestUri.getHost()) ) {
               String radicalName = requestUri.getUser();
               if(verbose)
                  logger.debug("KKK getNextHop: SU-3: radicalName="+radicalName);
               SuConfig suConfig = topologyConfig.getSuConfig(radicalName);
               String host = suConfig.getHomeRfss().getIpAddress();
               int port = suConfig.getHomeRfss().getSipPort();
               return new HopImpl(host, port);

            } else {
               if(verbose)
                  logger.debug("getNextHop: super.getNextHop()...");
               return super.getNextHop(request);
            }

         } else {

            // Non-null RouteHeader
            boolean u2ucall = SipUtils.isU2UCall(request);
            if(verbose) 
               logger.debug("KKK getNextHop: non-null routeHeader-u2ucall="+ u2ucall);

	    // check SG 9.1.x and SU 12.18.x
            if (u2ucall && Request.INVITE.equals(method)) {
               if(verbose)
                  logger.debug("KKK getNextHop: check radio-inhibit, request="+request);
               FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
               SipURI fromUri = (SipURI) fromHeader.getAddress().getURI();
               String callingRadicalName = fromUri.getUser();
               SuConfig callingSu = topologyConfig.getSuConfig(callingRadicalName);

               ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
               SipURI toUri = (SipURI) toHeader.getAddress().getURI();
               String calledRadicalName = toUri.getUser();
               SuConfig calledSu = topologyConfig.getSuConfig(calledRadicalName);

               // check callingSu radioInhibit
               RadioInhibitType callingRadioInhibitType = callingSu.getUserServiceProfile()
                  .getRadioInhibit().getRadioInhibitType();
               RadioInhibitType calledRadioInhibitType = calledSu.getUserServiceProfile()
                  .getRadioInhibit().getRadioInhibitType();

               if(verbose) {
                  logger.debug("KKK getNextHop: callingRadicalName="+callingRadicalName+":"+callingRadioInhibitType);
                  logger.debug("KKK getNextHop: calledRadicalName="+calledRadicalName+":"+calledRadioInhibitType);
               }
               if(callingSu.getHomeRfss() == rfss.getRfssConfig()) {
                  if(verbose)
                  logger.debug("KKK getNextHop: calling home...continue");

	       } else if(calledSu.getHomeRfss() == rfss.getRfssConfig()) {

                  if(verbose)
                  logger.debug("KKK getNextHop: called home...");
                  if(callingRadioInhibitType == RadioInhibitType.RADIO_INHIBITED) {
                     // 12.18.x
                     logger.debug("KKK getNextHop: called home callingSu Radioinhibit: new HopImpl");
                     String host = calledSu.getHomeRfss().getIpAddress();
                     int port = calledSu.getHomeRfss().getSipPort();
                     return new HopImpl(host, port);
                   }
               }
            }

	    // 12.3.1.1, 13.1.1.1 AllowHeader
	    /*** DISABLED: donot belong in here
            if (!Request.ACK.equals(method)) {
               logger.debug("KKK getNextHop: non-null routeHeader...addAllowHeaders");
	       SipUtils.addAllowHeaders( request);
            }
	     ***/

            //logger.debug("getNextHop: non-null routeHeader...may remove");
            Hop hop = super.getNextHop(request);
            if (((SipURI) routeHeader.getAddress().getURI()).getParameter("remove") != null) {
               //logger.debug("KKK getNextHop: remove Route Header...");
               request.removeHeader(RouteHeader.NAME);
            }
            logger.debug("KKK getNextHop: isDialogTerminated="+rfss.isDialogTerminated());
            //logger.debug("KKK getNextHop: request="+request);
            return hop;
         }
      } catch (Exception ex) {
         rfss.logError("Error occured in computing the next hop: ", ex);
         rfss.logError("Request for which this error occured is \n" + request);
         return null;
      }
   }

   public Hop getOutboundProxy() {
      return null;
   }
}
