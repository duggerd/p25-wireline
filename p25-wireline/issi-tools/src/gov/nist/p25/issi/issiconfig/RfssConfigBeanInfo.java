//
package gov.nist.p25.issi.issiconfig;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class RfssConfigBeanInfo extends SimpleBeanInfo {
   @Override
   public PropertyDescriptor[] getPropertyDescriptors() {
      try {
         PropertyDescriptor colorDescriptor = new PropertyDescriptor("color",
               RfssConfig.class, "getColor", null);
         PropertyDescriptor[] props = {
               new PropertyDescriptor("rfssId", RfssConfig.class,
                     "getRfssId", null),
               new PropertyDescriptor("rfssName", RfssConfig.class,
                     "getRfssName", null),
               new PropertyDescriptor("ipAddress", RfssConfig.class,
                     "getIpAddress", "setIpAddress"),
               new PropertyDescriptor("sipPort", RfssConfig.class,
                     "getSipPort", "setSipPort"),
               new PropertyDescriptor("systemName", RfssConfig.class,
                     "getSystemName", "setSystemName"),
               colorDescriptor
         };
         return props;

      } catch (IntrospectionException ex) {
         throw new Error("Unexpected exception occured", ex);
      }
   }
}
