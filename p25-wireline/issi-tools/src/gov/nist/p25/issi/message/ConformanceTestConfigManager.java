//
package gov.nist.p25.issi.message;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

import gov.nist.p25.common.util.FileUtility;
import gov.nist.p25.issi.message.XmlConformanceTestConfig;


public class ConformanceTestConfigManager
{
   private static Logger logger = Logger.getLogger(ConformanceTestConfigManager.class);
   public static void showln(String s) { System.out.println(s); }

   private List<String> fileList;

   // accessor
   /***
   public List<String> getFileList() {
      return fileList;
   }
    **/

   // constructor
   public ConformanceTestConfigManager() {
   }

   public List<String> getFileList(File dir, String nametag)
      throws IOException {
      fileList = new ArrayList<String>();
      visitAllFiles( dir, nametag);
      return fileList;
   }

   public List<String> reconcileRfssName(Map<String,String> nameMap, File dir,
      boolean save) throws Exception
   {
      logger.debug("reconcileRfssName(): "+nameMap);
      List<String> modList = new ArrayList<String>();

      logger.debug("reconcileRfssName(): fix topologyX.xml");
      List<String> xfileList = getFileList(dir, "topology");
      for( String fullpath: xfileList) {
         //showln(" -- "+fullpath);
         String xmlMsg = FileUtility.loadFromFileAsString(fullpath);

         XmlConformanceTestConfig xmldoc = new XmlConformanceTestConfig();
         xmldoc.loadConformanceTestConfig(xmlMsg);
         boolean mflag = xmldoc.reconcileRfssName(nameMap);
         if( mflag) {
            // for testing only, use false
            if( save) {
               logger.debug("Save: "+fullpath);
               xmldoc.saveConformanceTestConfig(fullpath);
            }
            modList.add(fullpath);
         }
      }

      logger.debug("reconcileRfssName(): fix testscript.xml");
      xfileList = getFileList(dir, "testscript");
      for( String fullpath: xfileList) {
         //showln(" -- "+fullpath);
         String xmlMsg = FileUtility.loadFromFileAsString(fullpath);

         XmlTestScript xmldoc = new XmlTestScript();
         xmldoc.loadTestScript(xmlMsg);
         boolean mflag = xmldoc.reconcileRfssName(nameMap);
         if( mflag) {
            // for testing only, use false
            if( save) {
               logger.debug("Save: "+fullpath);
               xmldoc.saveTestScript(fullpath);
            }
            modList.add(fullpath);
         }
      }

      logger.debug("modList.size(): "+modList.size());
      return modList;
   }

   // Visit all files under a given directory
   // dir:   testsuites/conformance/testscripts/
   // file:  su_registration_successful_presence/topology1.xml
   //
   protected void visitAllFiles(File dir, String nametag) throws IOException {
      if (dir.isDirectory()) {
         String[] children = dir.list();
         for (int i=0; i<children.length; i++) {
            visitAllFiles(new File(dir, children[i]),nametag);
         }
      } else {
         process(dir,nametag);
      }
   }

   protected void process(File file, String nametag) throws IOException {
      String name = file.getName();
      // nametag: [topology|testscript]
      if(name.startsWith( nametag) && name.endsWith(".xml")) {
         String fullpath = file.getCanonicalPath();
         fileList.add( fullpath);
      }
   }

   //=====================================================================
   public static void main(String[] args) throws Exception {

      String dirname = "testsuites/conformance/testscripts";
      File dir = new File( dirname);

      ConformanceTestConfigManager manager = new ConformanceTestConfigManager();
      List<String> fileList = manager.getFileList(dir, "topology");
      for( String filename: fileList) {
         showln("** process: "+filename);
      }

      showln("===============================");
      Map<String,String> nameMap = new HashMap<String,String>();
      //nameMap.put("rfss_1", "Xrfss_1");
      nameMap.put("Xrfss_1", "rfss_1");

      // for testing only, save is false
      boolean save = false;
      List<String> modList = manager.reconcileRfssName(nameMap, dir, save);
      for( String filename: modList) {
         showln("** mod: "+filename);
      }
      showln("** Total Topology size: "+fileList.size());
      showln("** Total Modified size: "+modList.size());
   }
}
