//
package gov.nist.p25.common.util;

import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;

//
// http://www.rgagnon.com/howto.html
//
public final class DateUtils {

  // NIST, Boulder, Colorado  (time-a.timefreq.bldrdoc.gov)
  public static final String ATOMICTIME_SERVER="http://132.163.4.101:13";

  // NIST, Gaithersburg, Maryland (time-a.nist.gov)
  // public static final String ATOMICTIME_SERVER="http://129.6.15.28:13";

  public final static GregorianCalendar getAtomicTime() 
       throws IOException
  {
     return getAtomicTime( ATOMICTIME_SERVER);
  }
  public final static GregorianCalendar getAtomicTime(String url) 
       throws IOException
  {
    BufferedReader in = null;
    try {
       URLConnection conn = new URL(url).openConnection();
       in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

       String atomicTime;
       while (true) {
          if ( (atomicTime = in.readLine()).indexOf("*") > -1) {
             break;
          }
       }
       System.out.println("DEBUG atomicTime: " + atomicTime);
       String[] fields = atomicTime.split(" ");
       GregorianCalendar calendar = new GregorianCalendar();

       String[] date = fields[1].split("-");
       calendar.set(Calendar.YEAR, 2000 +  Integer.parseInt(date[0]));
       calendar.set(Calendar.MONTH, Integer.parseInt(date[1])-1);
       calendar.set(Calendar.DATE, Integer.parseInt(date[2]));

       // deals with the timezone and the daylight-saving-time
       //TimeZone tz = TimeZone.getTimeZone("GMT");
       TimeZone tz = TimeZone.getDefault();
       int gmt = (tz.getRawOffset() + tz.getDSTSavings()) / 3600000;

       // 00: ST, 50: DST
       System.out.println("DEBUG fields[3]: " + fields[3]);
       if( "00".equals(fields[3])) {
          gmt = tz.getRawOffset() / 3600000;
       }
       System.out.println("DEBUG gmt: " + gmt);

       String[] time = fields[2].split(":");
       calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[0]) + gmt);
       calendar.set(Calendar.MINUTE, Integer.parseInt(time[1]));
       calendar.set(Calendar.SECOND, Integer.parseInt(time[2]));

       // leap second and health flag
       //System.out.println("DEBUG fields[4]: " + fields[4]);
       //System.out.println("DEBUG fields[5]: " + fields[5]);
       
       // network lag
       //System.out.println("DEBUG fields[6]: " + fields[6]);

       return calendar;
    }
    catch (IOException e) {
       throw e;
    }
    finally {
       if (in != null) {
         in.close();
       }
    }
  }

  public static long estimateSystemTimeDifference(String url)
    throws IOException
  {
    String format = "yyyy-MM-dd HH:mm:ss.SSS";
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    long t0 = DateUtils.getAtomicTime(url).getTime().getTime();
    long t1 = new Date().getTime();
    return (t1 - t0);
  }
  public static long estimateSystemTimeDifference()
    throws IOException
  {
    return estimateSystemTimeDifference(ATOMICTIME_SERVER);
  }
  //==================================================================
  public static void main(String args[]) throws IOException {

    String format = "yyyy-MM-dd HH:mm:ss.SSS";
    SimpleDateFormat sdf = new SimpleDateFormat(format);

    Date date = new Date();
    System.out.println("System date : " + sdf.format(date)); 
    System.out.println("Atomic time : " + 
        sdf.format(DateUtils.getAtomicTime().getTime()));
    date = new Date();
    System.out.println("System date : " + sdf.format(date)); 
    System.out.println("Estimate diff : " + estimateSystemTimeDifference());
  }

  /*
    ref : http://www.bldrdoc.gov/doc-tour/atomic_clock.html

                   49825 95-04-18 22:24:11 50 0 0 50.0 UTC(NIST) *

                       |     |        |     | | |  |      |      |
    These are the last +     |        |     | | |  |      |      |
    five digits of the       |        |     | | |  |      |      |
    Modified Julian Date     |        |     | | |  |      |      |
                             |        |     | | |  |      |      |
    Year, Month and Day <----+        |     | | |  |      |      |
                                      |     | | |  |      |      |
    Hour, minute, and second of the <-+     | | |  |      |      |
    current UTC at Greenwich.               | | |  |      |      |
                                            | | |  |      |      |
    DST - Daylight Savings Time code <------+ | |  |      |      |
    00 means standard time(ST), 50 means DST  | |  |      |      |
    99 to 51 = Now on ST, goto DST when local | |  |      |      |
    time is 2:00am, and the count is 51.      | |  |      |      |
    49 to 01 = Now on DST, goto ST when local | |  |      |      |
    time is 2:00am, and the count is 01.      | |  |      |      |
                                              | |  |      |      |
    Leap second flag is set to "1" when <-----+ |  |      |      |
    a leap second will be added on the last     |  |      |      |
    day of the current UTC month.  A value of   |  |      |      |
    "2" indicates the removal of a leap second. |  |      |      |
                                                |  |      |      |
    Health Flag.  The normal value of this    <-+  |      |      |
    flag is 0.  Positive values mean there may     |      |      |
    be an error with the transmitted time.         |      |      |
                                                   |      |      |
    The number of milliseconds ACTS is advancing <-+      |      |
    the time stamp, to account for network lag.           |      |
                                                          |      |
    Coordinated Universal Time from the National <--------+      |
    Institute of Standards & Technology.                         |
                                                                 |
    The instant the "*" appears, is the exact time. <------------+
  */
}
