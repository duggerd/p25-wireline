//
package gov.nist.p25.issi.rfss;

import gov.nist.p25.issi.issiconfig.GroupConfig;

import java.util.EventObject;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;

/**
 * Group Call Setup Request Event
 */
public class GroupCallSetupRequestEvent extends EventObject {
   private static final long serialVersionUID = -1L;

   private RequestEvent requestEvent;
   private GroupConfig groupConfig;
   private ServerTransaction serverTransaction;

   // constructor
   public GroupCallSetupRequestEvent(GroupConfig groupConfig,
         RequestEvent requestEvent, ServerTransaction st) {
      super(groupConfig);
      this.requestEvent = requestEvent;
      this.groupConfig = groupConfig;
      this.serverTransaction = st;
   }

   public RequestEvent getRequestEvent() {
      return requestEvent;
   }

   public GroupConfig getGroupConfig() {
      return groupConfig;
   }

   public ServerTransaction getServerTransaction() {
      return serverTransaction;
   }
}
