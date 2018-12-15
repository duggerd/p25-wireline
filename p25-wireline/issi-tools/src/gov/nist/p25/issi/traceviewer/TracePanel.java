//
package gov.nist.p25.issi.traceviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Collection;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import gov.nist.p25.common.swing.combobox.SteppedComboBox;
import gov.nist.p25.common.swing.transform.GraphicsInterface;
//import gov.nist.p25.common.swing.transform.TransformingCanvas;
import gov.nist.p25.common.swing.widget.Filler;
//import gov.nist.p25.issi.traceviewer.TraceLogTextPane;

/**
 * This class implements a trace panel.
 */
public class TracePanel extends JPanel implements GraphicsInterface
{
   private static final long serialVersionUID = -1L;
   
   private static final int DATA_AREA_0 = 0;
   private static final int DATA_AREA_1 = 1;
   private static final int DATA_AREA_2 = 2;
   private static final int DATA_AREA_3 = 3;
   
   public static final int DATA_ALL_MSG      = DATA_AREA_0;
   public static final int DATA_SELECTED_MSG = DATA_AREA_1;
   public static final int DATA_RAW_MSG      = DATA_AREA_2;
   public static final int DATA_ERROR_MSG    = DATA_AREA_3;
   
   private static final String TYPE_TABBEDPANE = "tabbed";
   private static final String TYPE_SPLITPANE = "split";

   public static final int MAX_DATA_AREAS = 4;
   private static String[] TITLE_DEFAULT = {
      "All Messages",
      "Selected Message",
      "All Raw Messages",
      "Selected Raw Message"      
   };   

   private String[] dataAreaTitle;
   private String title = "";
   private String testNumber = "";
   private String fileName = "";
   private List<MessageData> messageList;
   private Collection<RfssData> rfssList;   
   private DiagramPanel diagramPanel;   
   
   private String formatType = TYPE_SPLITPANE;
   private ImageIcon imageIcon;
   private JFrame frame;
   private JLabel testResultLabel;
   private JScrollPane scrollPane;
   private JTabbedPane tabbedPane;
   private SteppedComboBox combo;
   private JTextArea dataTextArea[] = new JTextArea[MAX_DATA_AREAS];
   private JScrollPane dataScrollPane[] = new JScrollPane[MAX_DATA_AREAS];
   
   //With JTextPane the SIP trace text wrap around yeaky !!!
   //private TraceLogTextPane dataTextArea[] = new TraceLogTextPane[MAX_DATA_AREAS];

   // Accessor
   //----------------------------------------------------------------------
   public String getFormatType() {
      return formatType;
   }
   public void setFormatType(String formatType) {
      this.formatType = formatType;
      setupGUI();
   }

   public String getFileName() {
      return fileName;
   }
   public void setFileName(String fileName) {
      this.fileName = fileName;
   }
   
   public String getTitle() {
      return title;
   }
   public void setTitle(String title) {
      this.title = title;
   }
   
   public String getTestNumber() {
      return testNumber;
   }
   public void setTestNumber(String testNumber) {
      this.testNumber = testNumber;
   }
      
   public List<MessageData> getMessageList() {
      return messageList;
   }
   public Collection<RfssData> getRfssList() {
      return rfssList;
   }
   
   public DiagramPanel getDiagramPanel() {
      return diagramPanel;
   }
   
   public ImageIcon getImageIcon() {
      return imageIcon;
   }   
   public void setImageIcon(ImageIcon  imageIcon) {
      this.imageIcon = imageIcon;
   }   

   public JFrame getFrame() {
      return frame;
   }   

   public String getTestResultLabel() {
      return testResultLabel.getText();
   }
   public void setTestResultLabel(String label) {
      testResultLabel.setText( label);
   }
   
   public JScrollPane getScrollPane() {
      return scrollPane;
   }
   public JTabbedPane getTabbedPane() {
      return tabbedPane;
   }

   public String[] getDataAreaTitle() {
      return dataAreaTitle;
   }
   public void setDataAreaTitle( String[] dataAreaTitle) {
      this.dataAreaTitle = dataAreaTitle;
   }

   //----------------------------------------------------------------------
   //public TraceLogTextPane getDataTextArea(int index) {
   public JTextArea getDataTextArea(int index) {
      return dataTextArea[index];
   }
   public void setDataTextArea(int index, String data) {
      JTextArea ta = getDataTextArea(index);
      ta.setText( data);
      ta.setCaretPosition(0);      
//      TraceLogTextPane ta = getDataTextArea(index);
//      ta.logMessage( true, data);
   }   

   // Constructor
   //----------------------------------------------------------------------
   public TracePanel(TracePanel tracePanel) {
      this( tracePanel.getFrame(), tracePanel.getTabbedPane(),
           tracePanel.getFileName(), tracePanel.getRfssList(),
           tracePanel.getMessageList(),
           tracePanel.getTitle(), tracePanel.getDataAreaTitle());   
      // share the same JTextArea
      for(int i=0; i < MAX_DATA_AREAS; i++) {
         setDataTextArea( i, tracePanel.getDataTextArea(i).getText());   
      }
      setTestNumber( tracePanel.getTestNumber());
      setTestResultLabel( tracePanel.getTestResultLabel());
      setImageIcon( tracePanel.getImageIcon());
   }

   //----------------------------------------------------------------------
   public TracePanel(JFrame frame, JTabbedPane tabbedPane, String fileName, 
         Collection<RfssData> rfssList,
         List<MessageData> messageList) {
      this( frame, tabbedPane, fileName, rfssList, messageList, "", TITLE_DEFAULT);      
   }
   public TracePanel(JFrame frame, JTabbedPane tabbedPane, String fileName, 
         Collection<RfssData> rfssList,
         List<MessageData> messageList,
         String title,
	 String[] dataAreaTitle) {
      this.frame = frame;
      this.tabbedPane = tabbedPane;
      this.fileName = fileName;
      this.rfssList = rfssList;      
      this.messageList = messageList;
      this.title = title;
      this.dataAreaTitle = dataAreaTitle;
      
      testResultLabel = new JLabel(" Test Result:       ");
      testResultLabel.setPreferredSize(new Dimension(160, 22));
      
      diagramPanel = new DiagramPanel(this);
      int[] traceSize = diagramPanel.setData( rfssList, messageList);

      // zoom 1.30
      int percent = 130;
      traceSize[0] *= percent/100;
      traceSize[1] *= percent/100;
      diagramPanel.setPreferredSize(new Dimension(traceSize[0], traceSize[1]));
      diagramPanel.revalidate();

      //TransformingCanvas canvas = new TransformingCanvas(this);
      //canvas.add(diagramPanel);
      //scrollPane = new JScrollPane(canvas);
      
      //scrollPane = new JScrollPane();
      //scrollPane.setViewportView(diagramPanel);
      
      for(int i=0; i < MAX_DATA_AREAS; i++) {
         //TraceLogTextPane ta = new TraceLogTextPane();
         JTextArea ta = new JTextArea();
         ta.setEditable(false);
         ta.setTabSize(2);
         //ta.setFont(new Font("Tahoma", Font.PLAIN, 11));
         ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
//         if( i < 2) {
//            ta.setFont(new Font("Tahoma", Font.PLAIN, 11));
//         } else {
//           // only for hex dump
//           ta.setFont(new Font("Monospaced", Font.PLAIN, 11));
//         }
         dataTextArea[i] = ta;
         dataScrollPane[i] = new JScrollPane(ta);
         dataScrollPane[i].setBorder(new TitledBorder(dataAreaTitle[i]));
      }

      String[] types = { TYPE_SPLITPANE, TYPE_TABBEDPANE };
      combo = new SteppedComboBox(types);
      combo.setPreferredSize(new Dimension(100, 24));
      combo.setSelectedItem( types[0]);
      combo.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
            SteppedComboBox cb = (SteppedComboBox)evt.getSource();
	    String item = (String)cb.getSelectedItem();
            setFormatType( item);
         }
      });

      // setup GUI
      setFormatType( types[0]);      
   }

   public void setupGUI() {
      
      scrollPane = new JScrollPane();
      scrollPane.setViewportView(diagramPanel);

      // setup overall SplitPane
      JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      splitPane.setDividerSize(7);
      splitPane.setOneTouchExpandable(true);
      splitPane.setResizeWeight(0.70);
      splitPane.add(scrollPane);

      JPanel zoomPanel = new JPanel();
      zoomPanel.setLayout(new BorderLayout());
      zoomPanel.add( new Filler(200, 22), BorderLayout.WEST);
      zoomPanel.add( testResultLabel, BorderLayout.CENTER);
      zoomPanel.add( combo, BorderLayout.EAST);

      if( TYPE_SPLITPANE.equals(formatType)) {
         JSplitPane textSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
         textSplitPane.setDividerSize(7);
         textSplitPane.setOneTouchExpandable(true);
         textSplitPane.setResizeWeight(0.50);      
         textSplitPane.add( dataScrollPane[0]);
         textSplitPane.add( dataScrollPane[1]);
         
         JSplitPane hexSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
         hexSplitPane.setDividerSize(7);
         hexSplitPane.setOneTouchExpandable(true);
         hexSplitPane.setResizeWeight(0.50);
         hexSplitPane.add( dataScrollPane[2]);
         hexSplitPane.add( dataScrollPane[3]);
         
         // setup data splitPane
         JSplitPane dataSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
         dataSplitPane.setDividerSize(7);
         dataSplitPane.setOneTouchExpandable(true);
         dataSplitPane.setResizeWeight(0.50);
         dataSplitPane.add(textSplitPane);
         dataSplitPane.add(hexSplitPane);
         splitPane.add(dataSplitPane);
   
      } else {
         JTabbedPane xtabbedPane = new JTabbedPane();
         xtabbedPane.add( TITLE_DEFAULT[0], dataScrollPane[0]);
         xtabbedPane.add( TITLE_DEFAULT[1], dataScrollPane[1]);
         xtabbedPane.add( TITLE_DEFAULT[2], dataScrollPane[2]);
         xtabbedPane.add( TITLE_DEFAULT[3], dataScrollPane[3]);
         splitPane.add(xtabbedPane);
      }
   
      // setup GUI
      removeAll();
      setLayout(new BorderLayout());
      add(zoomPanel, BorderLayout.NORTH);
      add(splitPane, BorderLayout.CENTER);
      updateUI();
   }

   public void autoSave() {
      diagramPanel.autoSave();
   }

   public String getSortedMessageData() {
      return diagramPanel.getSortedMessageData();
   }

   // GraphicsInterface implementation
   //----------------------------------------------------------------------
   public void doGraphics(Graphics g) {
      diagramPanel.paintComponent( g);
   }
}
