//
package gov.nist.p25.issi.message;

import java.io.File;
import java.io.IOException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlException;

import gov.nist.p25.common.util.FileUtility;

import gov.nist.p25.issi.constants.IvsDelaysConstants;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays;
//import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsInput;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsStatus;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd;


public class XmlIvsDelays
{
   public static void showln(String s) { System.out.println(s); }

   private String xmlFile = "";
   private IvsDelaysDocument ivsDelaysDoc;
   private IvsDelays ivsDelays;

   // accessor
   public IvsDelays getIvsDelays() {
      return ivsDelays;
   }
   public String getXmlFile() {
      return xmlFile;
   }

   // constructor
   public XmlIvsDelays()
   {
      ivsDelaysDoc = IvsDelaysDocument.Factory.newInstance();
      ivsDelays = ivsDelaysDoc.addNewIvsDelays();
   }
   public XmlIvsDelays(String xmlFile)
      throws XmlException, IOException
   {
      this.xmlFile = xmlFile;
      loadIvsDelays( FileUtility.loadFromFileAsString(xmlFile));
   }

   // methods
   public IvsDelays loadIvsDelays(String xmlMsg)
      throws XmlException
   {
      ivsDelaysDoc = IvsDelaysDocument.Factory.parse(xmlMsg);
      ivsDelays = ivsDelaysDoc.getIvsDelays();
      return ivsDelays;   
   }   
   
   public IvsDelays loadIvsDelays(File msgFile)
      throws XmlException, IOException
   {
      ivsDelaysDoc = IvsDelaysDocument.Factory.parse(msgFile);
      ivsDelays = ivsDelaysDoc.getIvsDelays();
      return ivsDelays;   
   }   

   public void saveIvsDelays(String msgFilename)
      throws IOException
   {
      XmlOptions opts = new XmlOptions();
      opts.setSavePrettyPrint();
      opts.setSavePrettyPrintIndent(3);
      String xml = ivsDelaysDoc.xmlText(opts);
      //showln("pretty-msgDoc=\n"+xml);
      FileUtility.saveToFile( msgFilename, xml);
      //
      //File msgFile = new File( msgFilename);
      //ivsDelaysDoc.save( msgFile);
   }

   //-------------------------------------------------------------------------
   public void getAllParameters(String type, IvsDelays fromDelays)
   {
      IvsDelays toDelays = getIvsDelays();

      // copy external measuremenets
      if( IvsDelaysConstants.TAG_IVS_GCSD.equals(type)) {
         IvsGcsd fromGcsd = fromDelays.getIvsGcsd();
         IvsGcsd toGcsd = toDelays.getIvsGcsd();
         toGcsd.getParameterList().removeAll( fromGcsd.getParameterList());
         toGcsd.getParameterList().addAll( fromGcsd.getParameterList());
      }
      else if( IvsDelaysConstants.TAG_IVS_GMTD.equals(type)) {
         IvsGmtd fromGmtd = fromDelays.getIvsGmtd();
         IvsGmtd toGmtd = toDelays.getIvsGmtd();
         toGmtd.getParameterList().addAll( fromGmtd.getParameterList());
      }
      else if( IvsDelaysConstants.TAG_IVS_UCSD.equals(type)) {
         IvsUcsd fromUcsd = fromDelays.getIvsUcsd();
         IvsUcsd toUcsd = toDelays.getIvsUcsd();
         toUcsd.getParameterList().addAll( fromUcsd.getParameterList());
      }
      else if( IvsDelaysConstants.TAG_IVS_UMTD.equals(type)) {
         IvsUmtd fromUmtd = fromDelays.getIvsUmtd();
         IvsUmtd toUmtd = toDelays.getIvsUmtd();
         toUmtd.getParameterList().addAll( fromUmtd.getParameterList());
      }
   } 
  
   //-------------------------------------------------------------------------
   public void setIvsStatusComment(String title, String comment)
   {
      IvsStatus ivsStatus = ivsDelays.getIvsStatus();
      if( ivsStatus == null)
         ivsStatus = ivsDelays.addNewIvsStatus();
      ivsStatus.setTitle( title);
      ivsStatus.setComment( comment);
   }

   public String getIvsStatusComment()
   {
      IvsStatus ivsStatus = ivsDelays.getIvsStatus();
      String comment = "";
      if( ivsStatus != null)
         comment = ivsStatus.getComment();
      return comment;
   }
   public void setIvsStatusComment(String comment)
   {
      IvsStatus ivsStatus = ivsDelays.getIvsStatus();
      if( ivsStatus == null)
         ivsStatus = ivsDelays.addNewIvsStatus();
      ivsStatus.setComment( comment);
   }

   public String getIvsStatusTitle()
   {
      IvsStatus ivsStatus = ivsDelays.getIvsStatus();
      String title = "";
      if( ivsStatus != null)
         title = ivsStatus.getTitle();
      return title;
   }
   public void setIvsStatusTitle(String title)
   {
      IvsStatus ivsStatus = ivsDelays.getIvsStatus();
      if( ivsStatus == null)
         ivsStatus = ivsDelays.addNewIvsStatus();
      ivsStatus.setTitle( title);
   }


   //=========================================================================
   public static void main(String[] args) throws Exception
   {
      // Test-1
      String xmlFile = IvsDelaysConstants.USER_MEASUREMENTS_FILE;
      XmlIvsDelays xmldoc = new XmlIvsDelays(xmlFile);
      IvsDelays ivsDelays = xmldoc.getIvsDelays();
      IvsGcsd ivsGcsd = ivsDelays.getIvsGcsd();

      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd.Parameter gcsdParam;
      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg_ip99");
      gcsdParam.setValue( 99);
      xmldoc.saveIvsDelays( "logs/ivsdelays-1.xml");
   }
}
