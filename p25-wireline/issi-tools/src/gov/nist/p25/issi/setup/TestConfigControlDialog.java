//
package gov.nist.p25.issi.setup;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import gov.nist.p25.common.swing.widget.GridBagConstraints2;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;


/**
 * Test Configuration Control Dialog.
 */
public class TestConfigControlDialog extends JDialog
   implements ActionListener
{
   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private TestConfigSetupPanel configPanel;
   private JButton okButton;
   private JButton cancelButton;
   private boolean answer = false;

   // accessor
   public boolean getAnswer() {
      return answer;
   }
   public TestConfigSetupPanel getTestConfigSetupPanel() {
      return configPanel;
   }
   public List<RfssConfigSetup> getRfssConfigSetupList() {
      return configPanel.getRfssConfigSetupList();
   }
   public List<GroupConfigSetup> getGroupConfigSetupList() {
      return configPanel.getGroupConfigSetupList();
   }
   public List<SuConfigSetup> getSuConfigSetupList() {
      return configPanel.getSuConfigSetupList();
   }

   // constructor
   //------------------------------------------------------------------------
   public TestConfigControlDialog(JFrame frame, boolean modal, String title,
      TopologyConfig topologyConfig)
      throws Exception
   {
      this( frame, modal, title, topologyConfig, null);
   }
   public TestConfigControlDialog(JFrame frame, boolean modal, String title,
      TopologyConfig topologyConfig, TopologyConfig refTopologyConfig)
      throws Exception
   {
      super( frame, modal);
      int mode = RfssConfigSetup.MODE_SETUP;
      configPanel = TestConfigControlPanel.createTestConfigSetupPanel(
            mode, title, topologyConfig, refTopologyConfig);
      buildGUI(frame, configPanel);
   }

   public TestConfigControlDialog(JFrame frame, boolean modal,
      String title, String startupFile, String curTopologyFile)
      throws Exception
   {
      super( frame, modal);
      int mode = RfssConfigSetup.MODE_SETUP;
      configPanel = TestConfigControlPanel.createTestConfigSetupPanel(
            mode, title, startupFile, curTopologyFile);
      buildGUI(frame, configPanel);
   }

   //------------------------------------------------------------------------
   protected void buildGUI(JFrame frame, TestConfigSetupPanel panel)
   {
      setLayout( new GridBagLayout());
      JPanel buttonPanel = new JPanel();
      okButton = new JButton("OK");
      okButton.addActionListener(this);
      cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(this);
      buttonPanel.add( okButton);
      buttonPanel.add( cancelButton);

      GridBagConstraints2 c = new GridBagConstraints2();
      // x, y, w, h, anchor, fill, l, t, r, b

      // row-0
      c.set( 0, 0, 8, 8, "c", "horz", 2, 2, 2, 2);
      add( panel, c);

      // row-1
      c.set( 6, 8, 2, 1, "e", "horz", 2, 2, 2, 2);
      add( buttonPanel, c);

      pack();
      setLocationRelativeTo(frame);
      requestFocus();
      setAlwaysOnTop(true);
      setVisible(true);
   }

   public Map<String,String> getRfssNameMap()
      throws Exception
   {
      List<RfssConfigSetup> rfssList = configPanel.getRfssConfigSetupList();
      TopologyConfigHelper helper = new TopologyConfigHelper();
      return helper.getRfssNameMap(rfssList);
   }

   public List<String> getRfssIpAddressList()
   {
      List<String> list = new ArrayList<String>();
      List<RfssConfigSetup> rfssList = configPanel.getRfssConfigSetupList();
      for( RfssConfigSetup rfssSetup: rfssList) {
         list.add( rfssSetup.getIpAddress());
      }
      return list;
   }
   // check emulated before pingHost
   public void pingHost(boolean check) throws Exception
   {
      int timeout = 2000;
      List<RfssConfigSetup> rfssList = configPanel.getRfssConfigSetupList();
      for( RfssConfigSetup rfssSetup: rfssList) {
         String ipAddress = rfssSetup.getIpAddress();
         if( check && !rfssSetup.isEmulated()) {
            showln("pingHost(): SKIP- RFSS is not emulated: "+ipAddress);
            continue;
         }  
         if( !InetAddress.getByName( ipAddress).isReachable(timeout)) {
            String msg = "Unreachable host: "+ipAddress;
            throw new IllegalArgumentException(msg);
         }
      }
   }
   public void pingHost() throws Exception
   {
      int timeout = 2000;
      List<String> ipList = getRfssIpAddressList();
      for( String ipAddress: ipList) {
         if( !InetAddress.getByName( ipAddress).isReachable(timeout)) {
            String msg = "Unreachable host: "+ipAddress;
            throw new IllegalArgumentException(msg);
         }
      }
   }

   public Map<String,String> reconcile( ISSITesterConfiguration testerConfig)
      throws Exception
   {
      //
      // curTopologyFile = su_registration_successful_presence/topology1.xml
      //
      List<RfssConfigSetup> rfssList = configPanel.getRfssConfigSetupList();
      List<GroupConfigSetup> gcsList = configPanel.getGroupConfigSetupList();
      List<SuConfigSetup> scsList = configPanel.getSuConfigSetupList();

      TopologyConfigHelper helper = new TopologyConfigHelper();
      Map<String,String> nameMap = helper.getRfssNameMap(rfssList);
      //showln("reconcile: nameMap="+nameMap);

      // This will modify the rfssList reference object
      helper.reconcileAll(testerConfig, rfssList, gcsList, scsList);

      return nameMap;
   }

   // implementation of ActionListener
   //------------------------------------------------------------------------
   public void actionPerformed(ActionEvent evt)
   {
      Object src = evt.getSource();
      if( src == okButton) {
         // pickup the current user input
         configPanel.stopCellEditing();
         answer = true;
         setVisible(false);
      }
      else if( src == cancelButton) {
         answer = false;
         setVisible(false);
      }
   }

   // Unit Test-1
   //========================================================================
   public static void main1(String[] args) throws Exception
   {
      String startupFile = 
         "c:/research/p25-wireline/issi-tools/startup/diets-standalone.properties";
      // 4.1.1
      //String curTopologyFile="su_registration_successful_presence/topology1.xml";
      // 9.1.1
      String curTopologyFile= "group_call_setup_successful/topology1.xml";

      String title = "ISSI Conformance Test Case";
      TestConfigControlDialog dialog = new TestConfigControlDialog( new JFrame(), 
         true, title, startupFile, curTopologyFile);

      boolean answer = dialog.getAnswer();
      showln("answer="+answer);
   }

   // Unit Test-2
   //========================================================================
   public static void main2(String[] args) throws Exception
   {
      String startupFile = 
         "c:/research/p25-wireline/issi-tools/startup/diets-standalone.properties";

      ISSIConfigManager configMgr = new ISSIConfigManager(startupFile);
      boolean interactive = true;
      TopologyConfig globalTopology = configMgr.getGlobalTopologyConfig(interactive);

      String title = "ITT Conformance Test - Global Topology";
      TestConfigControlDialog dialog = new TestConfigControlDialog( null, true,
         title, globalTopology);
      boolean answer = dialog.getAnswer();
      showln("answer="+answer);
   }

   // Unit Test-3
   //========================================================================
   public static void main(String[] args) throws Exception
   {
      String startupFile = 
         "c:/research/p25-wireline/issi-tools/startup/diets-standalone.properties";

      ISSIConfigManager configMgr = new ISSIConfigManager(startupFile);
      boolean interactive = true;
      TopologyConfig globalTopology = configMgr.getGlobalTopologyConfig(interactive);

      // 9.1.1
      String curTopologyFile= "group_call_setup_successful/topology1.xml";
      ISSIConfigManager configMgr2 = new ISSIConfigManager(startupFile);
      TopologyConfig curTopology = configMgr2.getTopologyConfig(interactive,
            curTopologyFile);

      // activate the data validation in dialog
      String title = "ITT Conformance Test - Current/Global Topology";
      TestConfigControlDialog dialog = new TestConfigControlDialog( null, true,
         title, curTopology, globalTopology);
      boolean answer = dialog.getAnswer();
      showln("answer="+answer);
   }
}
