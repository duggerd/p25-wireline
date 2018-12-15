//
package gov.nist.p25.issi.traceviewer;

import java.awt.Dimension;
import java.io.Serializable;

/**
 * This class serves as the base class for SIP and PTT Message data.
 */
public class MessageData implements Serializable, Comparable
{
   private static final long serialVersionUID = -1L;

   private boolean selected;   
   private boolean isSender;
   private int id = -1;
   private int x;
   private int y;
   private int direction = 1;

   private String fromRfssId;
   private String toRfssId;
   private String fromPort;
   private String toPort;
   private String messageType;
   private String data;
   private String rawdata = "";
   private long timestamp;

   private Dimension dimension = new Dimension( 10, 10);
   
   // constructor
   public MessageData(String fromRfssId, String toRfssId,
         String fromPort, String toPort,
         String messageType, String data,
         long timestamp,
         boolean selected, boolean isSender)
   {
      this.fromRfssId = fromRfssId;
      this.toRfssId = toRfssId;
      this.fromPort = fromPort;
      this.toPort = toPort;
      this.messageType = messageType;
      this.data = data;
      setTimestamp( timestamp);
      setSelected(selected);
      setSender(isSender);
   }

   // accessors
   public String getFromRfssId() {
      return  !isSender() ? fromRfssId : toRfssId;
   }

   public String getToRfssId() {
      return !isSender() ? toRfssId : fromRfssId;   
   }

   public String getFromPort() {
      return !isSender() ? fromPort : toPort;
   }

   public String getToPort() {
      return !isSender() ? toPort : fromPort;
   }

   public String getMessageType() {
      return messageType;
   }

   public String getData() {
      return data;
   }

   public String getRawdata() {
      return rawdata;
   }
   public void setRawdata(String rawdata) {
      this.rawdata = rawdata;
   }

   public long getTimestamp() {
      return timestamp;
   }
   public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
   }

   public int getId() {
      return id;
   }
   public void setId(int id) {
      this.id = id;
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

   public int getDirection() {
      return direction;
   }
   public void setDirection(int direction) {
      // +1 : left to right   ------>
      // -1 : right to left   <------
      this.direction = direction;
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

   public boolean isSender() {
      return isSender;
   }
   public boolean getSender() {
      return isSender;
   }
   public void setSender(boolean isSender) {
      this.isSender = isSender;
   }

   // implementation of Comparable
   //---------------------------------------------------------------
   public int compareTo( Object obj) {
      MessageData other = (MessageData) obj;
      if( getTimestamp() < other.getTimestamp()) {
         return -1;
      } else if( getTimestamp() == other.getTimestamp()) {
         return 0;
      } else {
         return 1;
      }
   }
}
