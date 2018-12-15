//
package gov.nist.p25.common.swing.popup;

import java.util.EventListener;

public interface PopupCommandListener extends EventListener
{
   public void processPopupCommand( PopupCommandEvent evt);
}
