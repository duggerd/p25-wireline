//
package gov.nist.p25.issi.startup;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import gov.nist.p25.common.util.Exec;
import gov.nist.p25.common.util.FileUtility;

/**
 * A platform independent process launcher
 * 
 */
public class ProcessLauncher {

   public static String TESTER_JAR = "diets.jar";

   public static void showln(String s) { System.out.println(s); }

   public static void writeProcessOutput(Process process) throws Exception {
      InputStreamReader tempReader = new InputStreamReader(
         new BufferedInputStream(process.getInputStream()));
      BufferedReader reader = new BufferedReader(tempReader);
      while (true) {
         String line = reader.readLine();
         if (line == null) break;
         System.out.println(line);
      }     
   }

   public static Process runCommand(String workDir, String option, String className,
      String argument) throws Exception {

      String opts = "";
      if( option != null && option.length() > 0)
          opts = option;

      String userpath = System.getProperty("user.dir");
      if( workDir != null && workDir.length() > 0)
          userpath = workDir;
      String classpath = "\"" + userpath + File.separator + "lib/*" + 
              File.pathSeparator + userpath + File.separator +
              TESTER_JAR + "\"";

      String args = "";
      if( argument != null && argument.length() > 0)
          args = argument;

      showln("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
      showln("ProcessLauncher: workDir="+workDir);
      showln("ProcessLauncher: option="+option);
      showln("ProcessLauncher: className="+className);
      showln("ProcessLauncher: args="+args);

      //System.out.println("ProcessLauncher: classpath="+classpath);
      //String program = System.getenv().get("JAVA_HOME") + "/bin/java.exe";
      String program = "java.exe";
      
      // -Djava.library.path=./bin/native/jpcap
      String libpath = "-Djava.library.path=\""+ userpath + "/bin/native/jpcap\"";

      // build command
      String zcmd = opts +" -classpath " +classpath +" "  +
                   libpath +" " +className +" " +args;
      String cmd = program +" " +zcmd;

      String osname = System.getProperty("os.name");
      /**
      if( "Linux".equals(osname)) {
         cmd = "cd "+userpath +";" + cmd;
      }
       */

      // for debug only
      FileUtility.saveToFile( "command.txt", cmd);

      Process p = null;
      if( "Linux".equals(osname)) {
         showln("ProcessLauncher: cd-workDir="+userpath);
         p = Runtime.getRuntime().exec(cmd, null, new File(userpath));
         writeProcessOutput(p);
      }
      else {
         p = Runtime.getRuntime().exec(cmd);
      }
      
      //boolean bflag = Exec.exec( cmd, true, true);
      //System.out.println("execWait: "+bflag);

      //p = new ProcessBuilder(cmd, zcmd).start();
      //writeProcessOutput( p);

      return p;
   }

   //=========================================================================
   public static void main(String[] argv) throws Exception {

      try {         
         String workDir = System.getProperty("workDir");
         String option = System.getProperty("option");
         String className = System.getProperty("className"); 
         String args = System.getProperty("args");

         Process p = ProcessLauncher.runCommand(workDir,option,className,args);
         if( p != null) {
            // need to fetch output first
            //p.waitFor();
            //showln("runCommand: exitValue="+p.exitValue());
         }
      } catch (Exception ex) {
         ex.printStackTrace();
	 showln("ProcessLauncher: MAIN-"+ex);
      }
   }
}
