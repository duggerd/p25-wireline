//
package gov.nist.p25.issi.issiconfig;

public interface WebServerAddress {
   public String getIpAddress();
   public int getHttpPort();
   public String getHttpControlUrl();
}
