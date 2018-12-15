//
package gov.nist.p25.issi.traceviewer;

import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This class implements an RFSS node and used to display both
 * SIP and PTT traces.
 */
public class RfssData implements Comparable<RfssData> {

   /** Radical name. */
   private String id = "";   
   
   /** Address. */
   private String ipAddress = "";     
   
   /** Incoming SIP or PTT (RTP) ports. */
   private List<String> ports = new ArrayList<String>();

   /** x coordinate. */
   private int x;

   /** y coordinate. */
   private int y;

   /** Size of RFSS image. */
   private Dimension dimension;

   /** Determines if this RFSS is selected on the screen. */
   private boolean selected = false;

   /** The time we receive the RFSS data. */
   private long timestamp;
   private int counter;
   
   /** The rfss configuration if there is one attached to this **/
   private RfssConfig rfssConfig;
   
   /** A set of Sessions retrieved at runtime. */
   private HashSet<PttSessionInfo> pttSessionInfo = new HashSet<PttSessionInfo>();


   // accessors
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }

   // derived from RfssConfig
   public String getDomainName() {
      String name = (rfssConfig != null ? rfssConfig.getDomainName() : getId());
      return name;
   }
   public String getName() {
      String name = (rfssConfig != null ? rfssConfig.getRfssName() : "");
      return name;
   }

   public String getIpAddress() {
      return ipAddress;
   }
   public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
   }

   public List<String> getPorts() {
      return ports;
   }
   public void setPorts(List<String> ports) {
      this.ports = ports;
   }

   public Dimension getDimension() {
      return dimension;
   }
   public void setDimension(Dimension dimension) {
      this.dimension = dimension;
   }

   public boolean isSelected() {
      return selected;
   }
   public boolean getSelected() {
      return selected;
   }
   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   public long getTimestamp() {
      return timestamp;
   }

   // see SipTraceFileParser
   public void setTimestamp(long timestamp, int counter) {
      this.timestamp = timestamp;
      this.counter = counter;
   }

   public int getX() {
      return x;
   }
   public void setX(int x) {
      this.x = x;
   }

   public int getY() {
      return y;
   }
   public void setY(int y) {
      this.y = y;
   }

   public RfssConfig getRfssConfig() {
      return rfssConfig;
   }
   public void setRfssConfig(RfssConfig rfssConfig) {
      this.rfssConfig = rfssConfig;
   }

   public HashSet<PttSessionInfo> getPttSessionInfo() {
      return pttSessionInfo;
   }
   public void setPttSessionInfo(HashSet<PttSessionInfo> pttSessionInfo) {
      this.pttSessionInfo = pttSessionInfo;
   }

   // constructors
   public RfssData(String id, String ipAddress, String port) {
      //setPorts( new ArrayList<String>());
      //setPttSessionInfo( new HashSet<PttSessionInfo>());
      setId(id);
      setIpAddress(ipAddress);
      addPort(port);      
   }

   public void addPort(String portToAdd) {
      for ( String p: ports) {
         if (p.equals(portToAdd)) return;
      }
      ports.add(portToAdd);      
   }

   // implementation of Comparable
   public int compareTo(RfssData rfssData) {
      if (rfssData.getTimestamp() == getTimestamp()) {
         return counter < rfssData.counter? -1 : 1;
      } else {
         return rfssData.getTimestamp() < getTimestamp() ? 1 : -1;
      }
   }

   // For debug only
   public String toString() {
      return getId() +":" + getIpAddress() +":" + getPorts();
   }

   // Use in DiagramPanel 
   public String getDescription() {

      StringBuffer sbuf = new StringBuffer();         
      sbuf.append("ID: " + getId());
      sbuf.append("\nIP Address: " + getIpAddress());
      for (int i = 0; i < getPorts().size(); i++) {
         if (i == 0)
            sbuf.append("\nSIP Recv Port: ");
         else
            sbuf.append("\nRTP Recv Port: ");
         sbuf.append(getPorts().get(i));
      }
      
      RfssConfig rfssConfig = getRfssConfig();
      if (rfssConfig != null) {
         sbuf.append("\nRfss Symbolic name: " + rfssConfig.getRfssName());
         sbuf.append("\nDomain name: " + rfssConfig.getDomainName());
         if (rfssConfig.getHomeSubsciberUnits().hasNext()) {
            sbuf.append("\nSu IDs homed at this RFSS: [");
            for (Iterator<SuConfig> it= rfssConfig.getHomeSubsciberUnits(); it.hasNext();) {
               SuConfig suConfig = it.next();
               sbuf.append("\n (SU Symbolic name=");
               sbuf.append(suConfig.getSuName());
               sbuf.append(", SU ID=");
               sbuf.append(suConfig.getSuId() + ")");
            }
            sbuf.append("\n]");
         }

         if (rfssConfig.getInitialServedSubscriberUnits().size() != 0) {
            sbuf.append("\nSu IDs initially served at this RFSS: [");
            for (SuConfig suConfig : rfssConfig.getInitialServedSubscriberUnits()) {
               sbuf.append("\n (SU Symbolic name=");
               sbuf.append(suConfig.getSuName());
               sbuf.append(", SU ID=");
               sbuf.append(Integer.toHexString(suConfig.getSuId()) + "(hex))");
            }
            sbuf.append("\n]");
         }

         if (rfssConfig.getAssignedGroups().hasNext()) {
            sbuf.append("\nGroup IDs homed at this RFSS: [");

            for (Iterator<GroupConfig> gcit= rfssConfig.getAssignedGroups(); gcit.hasNext();) {
               GroupConfig groupConfig = gcit.next();
               sbuf.append("\n (Group Symbolic Name=");
               sbuf.append(groupConfig.getGroupName());
               sbuf.append(", Group ID=");
               sbuf.append(Integer.toHexString(groupConfig.getGroupId()) + "(hex))");

               // SUs in group
               for (Iterator<SuConfig> suit = groupConfig.getSubscribers(); suit.hasNext();) {
                  SuConfig suConfig = suit.next();
                  sbuf.append("\n   (SU Symbolic Name=");
                  sbuf.append(suConfig.getSuName());
                  sbuf.append(", SU ID=");
                  sbuf.append(Integer.toHexString(suConfig.getSuId()) + "(hex))");
               }
            }
            sbuf.append("\n]");
         }

         sbuf.append("\nPTT Session Info: [");
         for (PttSessionInfo pttSessionInfo: getPttSessionInfo()) {
            sbuf.append("\n(");
            sbuf.append(pttSessionInfo.toString());
            sbuf.append(")");
         }
         sbuf.append("\n]");
      }
      return sbuf.toString();
   }
}
