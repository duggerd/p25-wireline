//
package gov.nist.p25.issi.traceviewer;

/**
 * Trace Panel Zoom enumeration class.
 *
 */
public enum ZoomLevel {

   ZOOM_500(0, "500", 5.0), ZOOM_200(1, "200", 2.0), 
   ZOOM_150(2, "150", 1.5), ZOOM_100(3, "100", 1.0), 
   ZOOM_75(4, "75", 0.75), ZOOM_50(5, "50", 0.50), 
   ZOOM_25(6, "25", 0.25), ZOOM_10(7, "10", 0.10), 
   ZOOM_MAX(0, "500", 5.0), ZOOM_NORMAL(3, "100", 1.0), 
   ZOOM_MIN(7, "10", 0.10);

   private int index;
   private String indexName;
   private double value;

   ZoomLevel(int index, String indexName, double value) {
      this.index = index;
      this.indexName = indexName;
      this.value = value;
   }

   public int getIndex() {
      return index;
   }
   public double value() {
      return value;
   }

   public String toString() {
      return indexName;
   }
}
