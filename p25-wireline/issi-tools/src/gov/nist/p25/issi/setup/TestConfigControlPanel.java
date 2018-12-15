//
package gov.nist.p25.issi.setup;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;

import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.common.swing.widget.GridBagConstraints2;


/**
 * Test Configuration Control panel.
 */
public class TestConfigControlPanel extends JPanel
   implements ActionListener
{
   private static final long serialVersionUID = -1L;
   public static void showln(String s) { System.out.println(s); }

   private TestConfigSetupPanel configPanel;
   private List<RfssConfigSetup> rfssList;
   private List<GroupConfigSetup> gcsList;
   private List<SuConfigSetup> scsList;

   private JButton okButton;
   private JButton cancelButton;
   private boolean answer = false;

   // accessor
   public boolean getAnswer() {
      return answer;
   }
   //public List<RfssConfigSetup> getRfssList() {
   //   return rfssList;
   //}

   // constructor
   public TestConfigControlPanel()
   {
      super();
   }

   public void setTopologyConfig(String title, TopologyConfig topologyConfig)
      throws Exception
   {
      removeAll();
      setLayout( new GridBagLayout());

      int mode = RfssConfigSetup.MODE_SETUP;
      configPanel = createTestConfigSetupPanel(mode, title, topologyConfig);

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
      add( configPanel, c);

      // row-1
      c.set( 6, 8, 2, 1, "e", "horz", 2, 2, 2, 2);
      add( buttonPanel, c);

      updateUI();
   }

   // implementation of ActionListener
   //-----------------------------------------------------------
   public void actionPerformed(ActionEvent evt)
   {
      Object src = evt.getSource();
      if( src == okButton) {
         TopologyConfigHelper helper = new TopologyConfigHelper();
         answer = helper.reconcile(rfssList, gcsList, scsList);
         //showln(" reconcile(): answer="+answer);
      }
      else if( src == cancelButton) {
         //showln(" Cancel Button.....");
         answer = false;
      }
   }

   //-----------------------------------------------------------------------------
   public static TestConfigSetupPanel createTestConfigSetupPanel(int mode, 
      String title, String startupFile, String curTopologyFile)
      throws Exception
   {
      ISSIConfigManager configMgr = new ISSIConfigManager(startupFile);
      TopologyConfig topologyConfig = configMgr.getTopologyConfig(true, curTopologyFile);

      showln("\n*** TestConfigControlPanel ***");
      showln("\nstartupFile="+configMgr.getStartupFile());
      showln("\nissiTesterConfig=" + configMgr.getIssiTesterConfiguration());
      showln("\nsystemTopologyConfig=" + configMgr.getSystemTopologyConfig());
      showln("\ncurTopologyFile=" + curTopologyFile);

      return createTestConfigSetupPanel(mode, title, topologyConfig);
   }

   //-----------------------------------------------------------------------------------
   public static TestConfigSetupPanel createTestConfigSetupPanel(int mode, String title,
      TopologyConfig topologyConfig) throws Exception
   {
      return createTestConfigSetupPanel(mode, title, topologyConfig, null);
   }
   public static TestConfigSetupPanel createTestConfigSetupPanel(int mode, String title,
      TopologyConfig topologyConfig, TopologyConfig refTopologyConfig) throws Exception
   {
      // convert TopologyConfig to Lists
      TopologyConfigHelper helper = new TopologyConfigHelper();

      // pass in the overall global topology for data validation
      helper.buildSetupLists(topologyConfig, refTopologyConfig);

      List<RfssConfigSetup> rfssList = helper.getRfssConfigSetupList();
      List<GroupConfigSetup> gcsList = helper.getGroupConfigSetupList();
      List<SuConfigSetup> scsList = helper.getSuConfigSetupList();

      //showln("TestConfigSetupPanel: rfssList.size="+rfssList.size());
      //showln("TestConfigSetupPanel: gcsList.size="+gcsList.size());
      //showln("TestConfigSetupPanel: scsList.size="+scsList.size());
 
      TestConfigSetupPanel panel =
         new TestConfigSetupPanel(mode,title,rfssList,scsList,gcsList);
      return panel;
   }

   public static TestConfigControlPanel createTestConfigControlPanel(String title,
      String startupFile, String curTopologyFile) throws Exception
   {
      ISSIConfigManager configMgr = new ISSIConfigManager(startupFile);
      TopologyConfig topologyConfig = configMgr.getTopologyConfig(true, curTopologyFile);

      TestConfigControlPanel panel = new TestConfigControlPanel();
      panel.setTopologyConfig(title, topologyConfig);
      return panel;
   }

   public static boolean reconcile( TestConfigSetupPanel panel, TopologyConfig ropologyConfig) 
   {
      List<RfssConfigSetup> rfssList = panel.getRfssConfigSetupList();
      List<GroupConfigSetup> gcsList = panel.getGroupConfigSetupList();
      List<SuConfigSetup> scsList = panel.getSuConfigSetupList();

      TopologyConfigHelper helper = new TopologyConfigHelper();
      return helper.reconcile(rfssList, gcsList, scsList);
   }

   //==============================================================================
   public static void main(String[] args) throws Exception
   {
      String startupFile = 
         "c:/research/p25-wireline/issi-tools/startup/diets-standalone.properties";
      // 4.1.1
      //String curTopologyFile="su_registration_successful_presence/topology1.xml";
      // 9.1.1
      String curTopologyFile= "group_call_setup_successful/topology1.xml";

      String title = "ISSI Conformance Test Case";
      TestConfigControlPanel panel = TestConfigControlPanel.createTestConfigControlPanel(
        title, startupFile, curTopologyFile);

      JFrame frame = new JFrame("TestConfigControlPanel");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add(panel);
      frame.setSize( 800, 740);
      frame.setVisible(true);
   }
}

