//
package gov.nist.p25.issi.traceviewer;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

/**
 * SIP Message Formatter per ISSI spec.
 */
public class SipMessageFormatter {

   public static void showln(String s) { System.out.println(s); }

   private static final String TAG_CONTENT = "Content-Length:";
   private static final String TAG_ALLOW = "Allow:";
   private static final String TAG_VIA = "Via:";
   private static final String TAG_ROUTE = "Route:";
   private static final String TAG_RECORD_ROUTE = "Record-Route:";

   // template for sorting the request and response 
   public static final String[] msgTemplate = {
      "Via:",
      "Route:",
      "Record-Route:",
      "Max-Forwards:",
      "From:",
      "To:",
      "Call-ID:",
      "CSeq:",
      "Contact:",
      "Expires:",
      "Priority:",
      "Allow:",
      "Accept:",
      "Timestamp:",
      "Warning:",
      "MIME-Version:",
      "Content-Disposition:",
      "Content-Type:",
      "Content-Length:",
   };

   // global flag to control formatter
   private static boolean issiMessageFormat = true;
   public static boolean getIssiMessageFormat() {
      return issiMessageFormat;
   }
   public static void setIssiMessageFormat(boolean bflag) {
      issiMessageFormat = bflag;
   }

   //---------------------------------------------------------------------------
   public static String loadFromFileAsString( String fname)
      throws IOException
   { 
      File file = new File( fname );
      BufferedReader brdr= new BufferedReader(new FileReader(file));
      
      StringBuffer sbuf = new StringBuffer();
      while( brdr.ready())
      {
         String xcmd = brdr.readLine();
         if( xcmd == null)
            break;
         //showln("CMD: <"+xcmd+">");
         sbuf.append( xcmd );
         sbuf.append( "\n");
      }
      brdr.close();
      return sbuf.toString();
   }

   public static List<String> loadFromString( String data)
      throws IOException
   { 
      ByteArrayInputStream bais = new ByteArrayInputStream( data.getBytes());
      BufferedReader brdr= new BufferedReader( new InputStreamReader(bais));
      
      List<String> list = new ArrayList<String>();
      while( brdr.ready())
      {
         String xcmd = brdr.readLine();
         if( xcmd == null)
            break;
         //showln("CMD: <"+xcmd+">");
         //if( xcmd.startsWith("#") ) continue;
         list.add( xcmd );
      }
      brdr.close();
      return list;
   }

   public static String buildFromList( List<String> dataList)
   { 
      //String NEWLINE = System.getProperty("line.separator");
      StringBuffer sbuf = new StringBuffer();
      for( String item: dataList)
      {
         sbuf.append( item);
         sbuf.append( "\n");
      }
      return sbuf.toString();
   }

   //---------------------------------------------------------------------------
   public static String formatRoute(String header, String sipRoute) {
      // split up all items on list
      return formatRoute(header, sipRoute, false);
   }
   public static String formatRoute(String header, String sipRoute,
      boolean isFirstItem)
   {
      return formatRoute(header, sipRoute, isFirstItem, false);
   }
   //---------------------------------------------------------------------------
   public static String formatRoute(String header, String sipRoute,
      boolean isFirstItem, boolean reverseOrder)
   {
      String newRoute = sipRoute;
      int index = sipRoute.indexOf(":");
      if( index != -1) {

         String routes = sipRoute.substring(index+1);
         //showln("routes=["+routes+"]");

         routes = routes.trim();
         String[] route = routes.split(",");

         // reverse order 
         if( reverseOrder) {
            // "Route:"
            String[] route2 = new String[route.length];
            for(int i=0; i < route.length; i++) {
               route2[i] = route[route.length-i-1];
            }
            route = route2;
         }

         StringBuffer sb = new StringBuffer();
         for(int i=0; i < route.length; i++) {
            if(i > 0) {
               sb.append("\n");
            }
            //showln("idx="+i+"  str:"+route[i]);

            // "Route:" or "Record-Route:"
            sb.append(header);
            sb.append(" ");
            sb.append(route[i]);

            // keep the first item only
            if(isFirstItem)
               break;
         }
         newRoute = sb.toString();
      }
      return newRoute;
   }

   //---------------------------------------------------------------------------
   public static String format( String data) throws IOException
   {
      // skip if not selected
      if(!issiMessageFormat)
         return data;

      // convert the data into list of strings
      List<String> dataList = loadFromString( data);
      int len = dataList.size();
      //showln("%%%%%%%%% format: data="+data.length());
      //showln("%%%%%%%%% format: dataList="+len);

      boolean doReverse = false;
      if( data.trim().indexOf("180 Ringing") != -1) {
         // 12.1.1.x Record-Route:
         doReverse = true;
      }
      //showln("SipMessageFormatter-format: doReverse= "+doReverse + "\n"+data);

      List<String> outList = new ArrayList<String>();

      // copy the SIP response until 1st non-null
      int jlen = 4;
      for(int j = 0; j < len; j++) {
         String item = dataList.get(j);
         outList.add( item);
         if( item==null)
            continue;
         int ilen = item.trim().length();
         if( ilen > 0) {
            jlen = j;
            break;
         }
      }
      //showln("^^^^^ jlen="+jlen);

      // copy per order specified in template
      int klen = jlen;
      for( int i = 0; i < msgTemplate.length; i++)
      {
         String tag = msgTemplate[i];
         for( int j= jlen; j < len; j++)
         {
            String item = dataList.get(j);
            if( item==null)
               continue;

            if( item.startsWith(TAG_CONTENT)) {
               klen = j;
               break;
            }
            if( outList.contains(item))
               continue;

            if( item.startsWith(tag))
            {
               // special process for Allow:
               if( TAG_ALLOW.equals(tag)) {
                  // put in spaces between options
                  item = item.replaceAll(",", ", ");
               }
               else if( TAG_ROUTE.equals(tag)) {
                  // reverse order: 13.2.x
                  item = formatRoute(TAG_ROUTE, item, false, true);
               }
               else if( TAG_RECORD_ROUTE.equals(tag)) {
                  // 12.1.1.1
                  item = formatRoute(TAG_RECORD_ROUTE, item, false, doReverse);
               }
               else if( TAG_VIA.equals(tag)) {
                  item = formatRoute(TAG_VIA, item, true);
               }
               outList.add( item);
            }
         }
      }
      //showln(" ++++ klen="+klen);

      // copy the rest content to end of message
      for( int j = klen; j < len; j++)
      {
         String item = dataList.get(j);
         if( item==null)
            continue;

         //showln("    === output: "+item);
         outList.add( item);
      }

      // convert list of string to string
      String newData = buildFromList( outList);

      //showln("%%%%%%%%% format: newData="+newData.length());
      //showln("%%%%%%%%% format: outList="+outList.size());
      return newData;
   }

   //===========================================================================
   public static void main(String[] args) throws Exception
   {
      /**
      String[] sipTexts = {
        "rfss_sip1.txt",
        "rfss_sip2.txt",
      };
       **/
      String[] sipTexts2 = {
        "rfss_data.txt",
      };

      for( String sipText: sipTexts2)
      {
         showln("+++++++++ datafile: "+sipText); 
         String data = loadFromFileAsString( sipText);
         String newData = format( data);
         showln("%%%%%%%%% format: newData=\n"+newData);
      }
   }
}
