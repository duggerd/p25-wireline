//
package gov.nist.p25.issi;

import gov.nist.p25.issi.constants.DietsConfigProperties;
import gov.nist.p25.issi.issiconfig.DaemonWebServerAddress;
import gov.nist.p25.issi.packetmonitor.PacketMonitor;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfigurationParser;
import gov.nist.p25.issi.rfss.tester.TestRFSS;

import java.util.Properties;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;


public class DietsDaemon {

   private static Logger logger = Logger.getLogger(DietsDaemon.class);

   //===================================================================
   public static void main(String[] args) throws Exception {

      PropertyConfigurator.configure("log4j.properties");
      String fileName = null;
      for (int i = 0; i < args.length; i++) {
         if (args[i].equals("-startup")) {
            fileName = args[++i];         
         }
      }
      if (fileName == null)
         fileName = System.getProperty("diets.startup");

      if (fileName == null) {
         throw new Exception("Missing startup properties");
      }
      System.out.println("Using startup property " + fileName);
      Properties props = new DietsConfigProperties(fileName);

      String testerConfigFile = props.getProperty(DietsConfigProperties.DAEMON_CONFIG_PROPERTY);

      ISSITesterConfiguration testerConfiguration = null;
      if (testerConfigFile != null)
         testerConfiguration = new ISSITesterConfigurationParser(testerConfigFile).parse();

      if (testerConfiguration == null)
         throw new Exception("Missing a required parameter -testerConfig");

      if (testerConfiguration.getLocalConfigurations().size() == 0) {
         JOptionPane.showMessageDialog(null,
               "No httpPort specified for this address -- " +
               "cannot start daemon \nCheck config file\n" + testerConfigFile,
               "Startup error", JOptionPane.ERROR_MESSAGE);
         System.exit(0);
      }

      for (DaemonWebServerAddress address : testerConfiguration.getLocalConfigurations()) {

         Server httpServer = new Server();
         SocketListener socketListener = new SocketListener();
         socketListener.setMaxThreads(10);
         socketListener.setMinThreads(3);
         socketListener.setHost(address.getIpAddress());
         socketListener.setPort(address.getHttpPort());
         httpServer.addListener(socketListener);

         int count1 = TestRFSS.configure(httpServer, testerConfigFile);
         int count2 = PacketMonitor.configure(httpServer, testerConfigFile);
         logger.info("tester count " + count1);
         logger.info("Packet monitor count " + count2);

         try {
            httpServer.start();
         }
         catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
               "Error starting the http server - check the assigned daemon port in " +
                  testerConfigFile,
               "Startup error",
               JOptionPane.ERROR_MESSAGE);
         }
      }
   }
}
