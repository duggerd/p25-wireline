//
package gov.nist.p25.issi.message;

import java.io.IOException;
import org.apache.xmlbeans.XmlException;

import gov.nist.p25.issi.constants.IvsDelaysConstants;
import gov.nist.p25.issi.setup.IvsDelaysHelper;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd;
import gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd;


public class XmlIvsDelaysSpec
{
   public static void showln(String s) { System.out.println(s); }

   private static String DOC_PERFORMANCE_SPEC = "Performanace specifications based on TIA-102.CACB, April 2007.";
   // standard template
   public static final String DIAGRAM_GCSD = "gfx/diagrams/IVS-GCSD.JPG";
   public static final String DIAGRAM_GMTD = "gfx/diagrams/IVS-GMTD.JPG";
   public static final String DIAGRAM_UCSD = "gfx/diagrams/IVS-UCSD.JPG";
   public static final String DIAGRAM_UMTD = "gfx/diagrams/IVS-UMTD.JPG";

   //-------------------------------------------------------------------------
   private static XmlIvsDelays getSpecIvsDelays(String perfType, boolean isSrcConsole, boolean isDstConsole)
      throws XmlException, IOException
   {
      String xmlFile = IvsDelaysConstants.SPEC_MEASUREMENTS_FILE;
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
         // ISSI/CSSI use the same IP delays
	 // xml/spec-ivsdelays.xml
         xmlFile = IvsDelaysConstants.SPEC_MEASUREMENTS_FILE;
      }
      showln("getSpecIvsDelays(): xmlFile="+xmlFile);
      return new XmlIvsDelays( xmlFile);
   }

   public static XmlIvsDelays getUserIvsDelays(String perfType, boolean isSrcConsole, boolean isDstConsole)
      throws XmlException, IOException
   {
      String xmlFile = IvsDelaysConstants.USER_MEASUREMENTS_FILE;
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
         // ISSI/CSSI use the same IP delays
	 // xml/user-ivsdelays.xml
         xmlFile = IvsDelaysConstants.USER_MEASUREMENTS_FILE;
      }
      showln("getUserIvsDelays(): xmlFile="+xmlFile);
      return new XmlIvsDelays( xmlFile);
   }

   // For CSSI, assume the source is the console
   //-------------------------------------------------------------------------
   public static XmlIvsDelays generateIvsGcsd(String perfType, boolean isSrcConsole, 
         boolean isDstConsole, boolean isConfirmed) throws Exception
   {
      XmlIvsDelays xmldoc = new XmlIvsDelays();
      IvsDelays ivsDelays = xmldoc.getIvsDelays();

      IvsGcsd ivsGcsd = ivsDelays.addNewIvsGcsd();
      ivsGcsd.setTitle("Performance Specifications for Group Call Setup Delay(GCSD)");
      ivsGcsd.setDiagram( DIAGRAM_GCSD);

      // measured parameters
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGcsd.Parameter gcsdParam;
      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg1");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isSrcConsole) {
         gcsdParam.setValue(0);
      } else {
         gcsdParam.setValue(200);
      }
      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg2");
      gcsdParam.setValue(10);
      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg3");
      gcsdParam.setValue(10);
      gcsdParam.setMaximum(50);
      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg4");
      gcsdParam.setValue(60);
      gcsdParam.setMaximum(200);

      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg5");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isSrcConsole) {
         gcsdParam.setValue(60);
      } else {
         gcsdParam.setValue(10);
      }

      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg6");
      gcsdParam.setValue(110);
      gcsdParam.setMaximum(350);
      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg7");
      gcsdParam.setValue(110);
      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg8");
      gcsdParam.setValue(60);
      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg9");
      gcsdParam.setValue(60);

      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg10");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isSrcConsole) {
         gcsdParam.setValue(0);
      } else {
         gcsdParam.setValue(170);
      }

      gcsdParam.setMaximum(550);
      gcsdParam = ivsGcsd.addNewParameter();
      gcsdParam.setKey("Tg11");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isDstConsole) {
         gcsdParam.setValue(0);
      } else {
         gcsdParam.setValue(220);
      }

      // get external measurements
      IvsDelays extDelays = getSpecIvsDelays(perfType,isSrcConsole,isDstConsole).getIvsDelays();
      xmldoc.getAllParameters(IvsDelaysConstants.TAG_IVS_GCSD, extDelays);

      // confirmed group call
      IvsDelaysHelper.setGCSDComposite(perfType, isSrcConsole, isDstConsole, xmldoc, isConfirmed);

      // comment
      String title = "Performanace Recommendations for "+perfType+" Voice Services";
      String comment = DOC_PERFORMANCE_SPEC;
      comment += (isConfirmed ? " Confirmed Group Call." : " Unconfirmed Group Call.");
      comment += "\n" + getCondition(perfType, isSrcConsole, isDstConsole);

      xmldoc.setIvsStatusComment( title, comment);
      return xmldoc;
   }

   public static XmlIvsDelays generateIvsGmtd(String perfType, boolean isSrcConsole, 
         boolean isDstConsole) throws Exception
   {
      XmlIvsDelays xmldoc = new XmlIvsDelays();
      IvsDelays ivsDelays = xmldoc.getIvsDelays();

      IvsGmtd ivsGmtd = ivsDelays.addNewIvsGmtd();
      ivsGmtd.setTitle("Performance Specifications for Group Call Message Transfer Delay(GMTD)");
      ivsGmtd.setDiagram(DIAGRAM_GMTD);

      // measured parameters
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsGmtd.Parameter gmtdParam;
      gmtdParam = ivsGmtd.addNewParameter();
      gmtdParam.setKey("Tg_ad1");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isSrcConsole) {
         gmtdParam.setValue(0);
      } else {
         gmtdParam.setValue(200);
      }
      gmtdParam.setMaximum(600);

      gmtdParam = ivsGmtd.addNewParameter();
      gmtdParam.setKey("Tg_ad2");
      gmtdParam.setValue(10);
      gmtdParam.setMaximum(50);
      gmtdParam = ivsGmtd.addNewParameter();
      gmtdParam.setKey("Tg_ad3");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isDstConsole) {
         gmtdParam.setValue(0);
      } else {
         gmtdParam.setValue(170);
      }

      // get external measurements
      IvsDelays extDelays = getSpecIvsDelays(perfType,isSrcConsole,isDstConsole).getIvsDelays();
      xmldoc.getAllParameters(IvsDelaysConstants.TAG_IVS_GMTD, extDelays);

      // calculated parameters
      IvsDelaysHelper.setGMTDComposite(perfType, isSrcConsole, isDstConsole, xmldoc);

      // comment
      String title = "Performanace Recommendations for "+perfType+" Voice Services";
      String comment = DOC_PERFORMANCE_SPEC;
      comment += "\n" + getCondition(perfType, isSrcConsole, isDstConsole);

      xmldoc.setIvsStatusComment( title, comment);
      return xmldoc;
   }

   public static XmlIvsDelays generateIvsUcsd(String perfType, boolean isSrcConsole, 
      boolean isDstConsole, boolean isDirectCall) throws Exception
   {
      XmlIvsDelays xmldoc = new XmlIvsDelays();
      IvsDelays ivsDelays = xmldoc.getIvsDelays();

      IvsUcsd ivsUcsd = ivsDelays.addNewIvsUcsd();
      ivsUcsd.setTitle("Performance Specifications for SU-to-SU Call Setup Delay(UCSD)");
      ivsUcsd.setDiagram(DIAGRAM_UCSD);

      // measured parameters
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUcsd.Parameter ucsdParam;
      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu1");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isSrcConsole) {
         ucsdParam.setValue(0);
      } else {
         ucsdParam.setValue(170);
      }

      // Tu2, Tu3
      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu2");
      ucsdParam.setValue(10);
      ucsdParam.setMaximum(50);
      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu3");
      ucsdParam.setValue(10);
      ucsdParam.setMaximum(50);

      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu4");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isDstConsole) {
         ucsdParam.setValue(0);
      } else {
         ucsdParam.setValue(210);
      }

      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu5");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isDstConsole) {
         ucsdParam.setValue(0);
      } else {
         ucsdParam.setValue(170);
      }
      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu6");
      ucsdParam.setValue(60);
      ucsdParam.setMaximum(200);
      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu7");
      ucsdParam.setValue(10);
      ucsdParam.setMaximum(50);
      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu8");
      ucsdParam.setValue(10);
      ucsdParam.setMaximum(50);

      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu9");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isSrcConsole) {
         ucsdParam.setValue(0);
      } else {
         ucsdParam.setValue(290);
      }
      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu10");
      ucsdParam.setValue(60);
      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu11");
      ucsdParam.setValue(10);
      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu12");
      ucsdParam.setValue(10);
      ucsdParam = ivsUcsd.addNewParameter();
      ucsdParam.setKey("Tu13");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isDstConsole) {
         ucsdParam.setValue(0);
      } else {
         ucsdParam.setValue(240);
      }

      // get external measurements
      IvsDelays extDelays = getSpecIvsDelays(perfType,isSrcConsole,isDstConsole).getIvsDelays();
      xmldoc.getAllParameters(IvsDelaysConstants.TAG_IVS_UCSD, extDelays);

      // calculated parameters
      IvsDelaysHelper.setUCSDComposite(perfType, isSrcConsole, isDstConsole, xmldoc, isDirectCall);

      // comment
      String title = "Performanace Recommendations for "+perfType+" Voice Services";
      String comment = DOC_PERFORMANCE_SPEC;
      comment += (isDirectCall ? " Direct Call." : " Availability Check.");
      comment += "\n" + getCondition(perfType, isSrcConsole, isDstConsole);

      xmldoc.setIvsStatusComment( title, comment);
      return xmldoc;
   }

   public static XmlIvsDelays generateIvsUmtd(String perfType, boolean isSrcConsole, 
         boolean isDstConsole) throws Exception
   {
      XmlIvsDelays xmldoc = new XmlIvsDelays();
      IvsDelays ivsDelays = xmldoc.getIvsDelays();

      IvsUmtd ivsUmtd = ivsDelays.addNewIvsUmtd();
      ivsUmtd.setTitle("Performance Specifications for SU-to-SU Call Message Transfer Delay(UMTD)");
      ivsUmtd.setDiagram(DIAGRAM_UMTD);

      // measured parameters
      gov.nist.p25.issi.xmlconfig.IvsDelaysDocument.IvsDelays.IvsUmtd.Parameter umtdParam;
      umtdParam = ivsUmtd.addNewParameter();
      umtdParam.setKey("Tu_ad1");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isSrcConsole) {
         umtdParam.setValue(0);
      } else {
         umtdParam.setValue(200);
      }

      umtdParam = ivsUmtd.addNewParameter();
      umtdParam.setKey("Tu_ad2");
      umtdParam.setValue(10);
      umtdParam = ivsUmtd.addNewParameter();
      umtdParam.setKey("Tu_ad3");
      umtdParam.setValue(10);
      umtdParam = ivsUmtd.addNewParameter();
      umtdParam.setKey("Tu_ad4");
      umtdParam.setValue(170);
      umtdParam = ivsUmtd.addNewParameter();
      umtdParam.setKey("Tu_ad5");
      umtdParam.setValue(200);
      umtdParam = ivsUmtd.addNewParameter();
      umtdParam.setKey("Tu_ad6");
      umtdParam.setValue(10);
      umtdParam = ivsUmtd.addNewParameter();
      umtdParam.setKey("Tu_ad7");
      umtdParam.setValue(10);
      umtdParam = ivsUmtd.addNewParameter();
      umtdParam.setKey("Tu_ad8");
      if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType) && isSrcConsole) {
         umtdParam.setValue(0);
      } else {
         umtdParam.setValue(170);
      }

      // get external measurements
      IvsDelays extDelays = getSpecIvsDelays(perfType,isSrcConsole,isDstConsole).getIvsDelays();
      xmldoc.getAllParameters(IvsDelaysConstants.TAG_IVS_UMTD, extDelays);

      // calculated parameters
      IvsDelaysHelper.setUMTDComposite(perfType, isSrcConsole, isDstConsole, xmldoc);

      // comment
      String title = "Performanace Recommendations for "+perfType+" Voice Services";
      String comment = DOC_PERFORMANCE_SPEC;
      comment += "\n" + getCondition(perfType, isSrcConsole, isDstConsole);

      xmldoc.setIvsStatusComment( title, comment);
      return xmldoc;
   }

   public static String getCondition(String perfType, boolean isSrcConsole, boolean isDstConsole)
   {
      String comment = "";
      if( IvsDelaysConstants.TAG_TYPE_ISSI.equals(perfType)) {
	 comment = "ISSI Performance: ";
      }
      else if( IvsDelaysConstants.TAG_TYPE_CSSI.equals(perfType)) {
	 comment = "CSSI Performance: ";
         comment += (isSrcConsole ? "Source Console " : "");
	 comment += (isDstConsole ? "Destination Console" : "");
      }
      return comment;
   }

   //=========================================================================
   public static void main(String[] args) throws Exception
   {
      XmlIvsDelays xmldoc;
      boolean isSrcConsole = false;
      boolean isDstConsole = false;

      //=== ISSI
      //--------------------------------------------------------
      String perfType = IvsDelaysConstants.TAG_TYPE_ISSI;

      // confirmed group call
      xmldoc = XmlIvsDelaysSpec.generateIvsGcsd(perfType, isSrcConsole, isDstConsole, true);
      xmldoc.saveIvsDelays("xml/issi-spec-ivs-gcsd.xml");

      xmldoc = XmlIvsDelaysSpec.generateIvsGmtd(perfType, isSrcConsole, isDstConsole);
      xmldoc.saveIvsDelays("xml/issi-spec-ivs-gmtd.xml");

      // direct Call
      xmldoc = XmlIvsDelaysSpec.generateIvsUcsd(perfType, isSrcConsole, isDstConsole, true);
      xmldoc.saveIvsDelays("xml/issi-spec-ivs-ucsd.xml");

      xmldoc = XmlIvsDelaysSpec.generateIvsUmtd(perfType, isSrcConsole, isDstConsole);
      xmldoc.saveIvsDelays("xml/issi-spec-ivs-umtd.xml");

      //=== CSSI
      //--------------------------------------------------------
      perfType = IvsDelaysConstants.TAG_TYPE_CSSI;
      isSrcConsole = true;
      isDstConsole = false;

      // confirmed group call
      xmldoc = XmlIvsDelaysSpec.generateIvsGcsd(perfType, isSrcConsole, isDstConsole, true);
      xmldoc.saveIvsDelays("xml/cssi-srcconsole-spec-ivs-gcsd.xml");

      xmldoc = XmlIvsDelaysSpec.generateIvsGmtd(perfType, isSrcConsole, isDstConsole);
      xmldoc.saveIvsDelays("xml/cssi-srcconsole-spec-ivs-gmtd.xml");

      // direct Call
      xmldoc = XmlIvsDelaysSpec.generateIvsUcsd(perfType, isSrcConsole, isDstConsole, true);
      xmldoc.saveIvsDelays("xml/cssi-srcconsole-spec-ivs-ucsd.xml");

      xmldoc = XmlIvsDelaysSpec.generateIvsUmtd(perfType, isSrcConsole, isDstConsole);
      xmldoc.saveIvsDelays("xml/cssi-srcconsole-spec-ivs-umtd.xml");

      //-----------------------
      isSrcConsole = false;
      isDstConsole = true;

      // confirmed group call
      xmldoc = XmlIvsDelaysSpec.generateIvsGcsd(perfType, isSrcConsole, isDstConsole, true);
      xmldoc.saveIvsDelays("xml/cssi-dstconsole-spec-ivs-gcsd.xml");

      xmldoc = XmlIvsDelaysSpec.generateIvsGmtd(perfType, isSrcConsole, isDstConsole);
      xmldoc.saveIvsDelays("xml/cssi-dstconsole-spec-ivs-gmtd.xml");

      // direct Call
      xmldoc = XmlIvsDelaysSpec.generateIvsUcsd(perfType, isSrcConsole, isDstConsole, true);
      xmldoc.saveIvsDelays("xml/cssi-dstconsole-spec-ivs-ucsd.xml");

      xmldoc = XmlIvsDelaysSpec.generateIvsUmtd(perfType, isSrcConsole, isDstConsole);
      xmldoc.saveIvsDelays("xml/cssi-dstconsole-spec-ivs-umtd.xml");
   }
}
