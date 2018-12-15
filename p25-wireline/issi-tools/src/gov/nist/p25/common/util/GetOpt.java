//
package gov.nist.p25.common.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class GetOpt
{
   public static void showln(String s) { System.out.println(s); }

   private Hashtable<String,String> optionDict = new Hashtable<String,String>();
   private List<String> parameters = new ArrayList<String>();

   // accessors
   public List<String> getParameters() { return parameters; }

   /**
    * Parse an argument list and store the values.  The syntax supported is:
    * <pre>
    *     -name        // boolean flag
    *     -name value  // a parameter that takes a value
    *     --           // mark the end of the options
    *     filenames    // any words after the options are 'filenames'
    * </pre>
    * <p>
    * For example "-foo -bar xxx" indicates foo is boolean = true, and bar has a value
    * of xxx.  "-foo xxx yyy zzz" means foo has the value xxx and the names are
    * yyy and zzz.  "-foo -- xxx yyy" means foo is boolean and the names are xxx and yyy.
    *
    * @param  paramName  paramDesc
    *
    * @return  returnDesc
    **/
   public GetOpt(String argv[])
   {
      int argNo;
      for (argNo = 0; argNo < argv.length; argNo++) {
         String arg = argv[argNo];
         if (arg.startsWith("-")) {
            if (arg.equals("--")) {
               argNo++;
               break;
            }
            String name = arg.substring(1);
            String value = "1";

            int k = argNo+1;
            if ((k < argv.length)  && !argv[k].startsWith("-")) {
               argNo++;
               value = argv[argNo];
            }
            optionDict.put(name, value);
            //showln("Setting argument: " +name +" = " +value);

         } else {
            break;
         }
      }

      if (argNo < argv.length) {
         // there are 'filenames'
         for ( ; argNo < argv.length; argNo++) {
            parameters.add( argv[argNo] );
         }
      }
   }

   /**
    * See if an option appeared on the command line.
    *
    * @param  optionName  The name of the option of interest.
    *
    * @return  True if the option was present on the command line, false otherwise.
    *     Note: there is no indication if the option was boolean or had a value.
    **/
   public boolean isDefined(String optionName)
   {
      boolean ret = optionDict.containsKey(optionName);
      //showln("Looking up " + optionName + " = " + ret);
      return ret;
   }

   /**
    * Get the value of an option.
    *
    * @param  optionName  The name of the value we are interested in.
    * @param  defaultValue The value to return if the option isn't present.
    * @return  Returns the value that was specified on the command line or null
    *     if the argument was not present.  Boolean arguments are returned as "1".
    **/
   public String getStringOpt(String optionName, String defaultValue)
   {
      String ret = defaultValue;
      if (isDefined(optionName)) {
         ret = (String)optionDict.get(optionName);
      }
      return ret;
   }

   public boolean getBooleanOpt(String optionName)
   {
      boolean ret = false;
      if (isDefined(optionName)) {
         ret = true;
      }
      return ret;
   }

   /**
    * Get an argument and convert it's value to an integer.
    *
    * @param  optionName  The name of the option we are looking for.
    * @param  defaultValue The value to return if the option cannot
    *     be converted or is not present.
    * @return  The integer value if the option was present and could be converted
    *     to an integer.  Returns the default value in all other cases.
    **/
   public int getIntOpt(String optionName, int defaultValue)
   {
      int ret = defaultValue;
      String value = getStringOpt(optionName,null);
      if (value != null) {
         try {
            ret = Integer.parseInt(value);
         } catch (NumberFormatException e) { }
      }
      return ret;
   }

   /**
    * Get an argument and convert it's value to a long.
    *
    * @param  optionName  The name of the option we are looking for.
    * @param  defaultValue The value to return if the option cannot
    *     be converted or is not present.
    * @return  The long value if the option was present and could be converted
    *     to a long.  Returns the default value in all other cases.
    **/
   public long getLongOpt(String optionName, long defaultValue)
   {
      long ret = defaultValue;
      String value = getStringOpt(optionName,null);
      if (value != null) {
         try {
            ret = Long.parseLong(value);
         } catch (NumberFormatException e) { }
      }
      return ret;
   }

   //=====================================================================
   public static void main( String[] argv)
   {
      showln("Usage: java GetOpt -f filename -noAuditWait -n 50 -- myFile.txt");

      GetOpt opts = new GetOpt(argv);
      if( opts.isDefined("f"))
      {
         showln("Found: f="+ opts.getStringOpt("f","myFile"));
      }
      if( opts.isDefined("noAuditWait"))
      {
         showln("Found: noAuditWait=" + opts.getStringOpt("noAuditWait","default"));
      }
      if( opts.isDefined("n"))
      {
         showln("Found: n="+ opts.getIntOpt("n",0));
      }
      showln("parameters="+ opts.getParameters());
   }
}
