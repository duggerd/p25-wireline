//
package gov.nist.p25.issi.traceviewer;

import java.util.List;

/**
 * Trace Loader interface 
 */
public interface TraceLoader  {
   
   public List<MessageData> getRecords();
	
}
