//
package gov.nist.p25.issi.setup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import gov.nist.p25.issi.issiconfig.DaemonWebServerAddress;
import gov.nist.p25.issi.issiconfig.GroupConfig;
import gov.nist.p25.issi.issiconfig.RfssConfig;
import gov.nist.p25.issi.issiconfig.SuConfig;
import gov.nist.p25.issi.issiconfig.TopologyConfig;
import gov.nist.p25.issi.rfss.tester.ISSITesterConfiguration;

import org.apache.log4j.Logger;

/**
 * Topology Configuration Helper.
 */
public class TopologyConfigHelper
{
   private static Logger logger = Logger.getLogger(TopologyConfigHelper.class);
   public static void showln(String s) { System.out.println(s); }

   private static boolean verbose = true;

   private List<RfssConfigSetup> rfssList;
   private List<GroupConfigSetup> gcsList;
   private List<SuConfigSetup> scsList;
   private List<String> nameList;
   private Map<String,String> nameMap;

   // accessor
   public List<RfssConfigSetup> getRfssConfigSetupList() {
      return rfssList;
   }
   public List<GroupConfigSetup> getGroupConfigSetupList() {
      return gcsList;
   }
   public List<SuConfigSetup> getSuConfigSetupList() {
      return scsList;
   }
   public List<String> getRfssNameList() {
      return nameList;
   }
   public Map<String,String> getRfssNameMap()
   {
      return nameMap;
   }

   // constructor
   public TopologyConfigHelper()
   {
   }

   public List<String> getRfssNameList(TopologyConfig topologyConfig)
   {
      nameList = new ArrayList<String>();
      for (RfssConfig rfssConfig: topologyConfig.getRfssConfigurations())
      {
         String rname = rfssConfig.getRfssName();
         nameList.add( rname);
      }
      return nameList;
   }

   public void buildSetupLists(TopologyConfig topologyConfig, TopologyConfig refTopologyConfig)
   {
      HashMap<String,String> nodesMap = new HashMap<String, String>();
      String parent;
      String node;
 
      nameList = new ArrayList<String>();
      rfssList = new ArrayList<RfssConfigSetup>();
      scsList = new ArrayList<SuConfigSetup>();
      gcsList = new ArrayList<GroupConfigSetup>();

      // RFSS Configuration
      for (RfssConfig rfssConfig: topologyConfig.getRfssConfigurations())
      {
         String ipAddress = rfssConfig.getIpAddress();
         int port = rfssConfig.getSipPort();
         String rname = rfssConfig.getRfssName();
         int rid = rfssConfig.getRfssId();
         int wacnId = rfssConfig.getWacnId();
         int systemId = rfssConfig.getSystemId();
         boolean emulated = rfssConfig.isEmulated();

         RfssConfigSetup ep = new RfssConfigSetup(ipAddress,port,rname,
                  rid,systemId,wacnId,emulated);
         ep.setReference( rfssConfig);
         // for input data validation 
         ep.setReferenceTopology( refTopologyConfig);

         rfssList.add( ep);
         nameList.add( rname);
         nodesMap.put( rname, "RfssConfig:"+rid);
      }

      // GROUP Configuration
      for (GroupConfig groupConfig: topologyConfig.getGroupConfigurations())
      {
         String rname = groupConfig.getHomeRfss().getRfssName();
         parent = nodesMap.get( rname);
         if( parent==null) continue;

         String gname = groupConfig.getGroupName();
         int gid = groupConfig.getGroupId();
         //int gwacnId =  groupConfig.getSysConfig().getWacnId();
         //int gsystemId = groupConfig.getSysConfig().getSystemId();
         String grfss = groupConfig.getHomeRfss().getRfssName();

         // delay instantiation
         nodesMap.put( gname, "GroupConfig:"+gid);

         boolean firstName = true;
         StringBuffer gsu = new StringBuffer();
         for (Iterator<SuConfig> it= groupConfig.getSubscribers(); it.hasNext();)
         {
            SuConfig suConfig = it.next();

            String sname = suConfig.getSuName();
            int sid = suConfig.getSuId();
            int swacnId = suConfig.getWacnId();
            int ssystemId = suConfig.getSystemId();
            String homeRfss = suConfig.getHomeRfss().getRfssName();
            int homeRfssId = suConfig.getHomeRfss().getRfssId();
            String servRfss = suConfig.getInitialServingRfss().getRfssName();
            boolean emulated = suConfig.isEmulated();

            SuConfigSetup scs = new SuConfigSetup(sname,sid,homeRfss,servRfss,
                  homeRfssId,ssystemId,swacnId,emulated);
            scs.setReference( suConfig);
            scsList.add( scs);

            // group-su
            if( firstName) {
               firstName = false;
            } else {
               gsu.append(",");
            }
            gsu.append(sname);
            nodesMap.put( sname, "SuConfig:"+sid);
         }

         GroupConfigSetup gcs = new GroupConfigSetup(gname,gid,grfss,gsu.toString());
         gcs.setReference( groupConfig);
         gcsList.add( gcs);
      }

      // SU Configuration
      for (SuConfig suConfig: topologyConfig.getSuConfigurations())
      {
         String rname = suConfig.getHomeRfss().getRfssName();
         parent = nodesMap.get( rname);
         if( parent==null) continue;

         String sname = suConfig.getSuName();

         // check if it is in group
         node = nodesMap.get( sname);
         if( node != null) continue;

         int sid = suConfig.getSuId();
         int swacnId = suConfig.getWacnId();
         int ssystemId = suConfig.getSystemId();
         String homeRfss = suConfig.getHomeRfss().getRfssName();
         int homeRfssId = suConfig.getHomeRfss().getRfssId();
         String servRfss = suConfig.getInitialServingRfss().getRfssName();
         boolean emulated = suConfig.isEmulated();

         SuConfigSetup scs = new SuConfigSetup(sname,sid,homeRfss,servRfss,
               homeRfssId,ssystemId,swacnId,emulated);
         scs.setReference( suConfig);
         scsList.add( scs);

         nodesMap.put( rname, "SuConfig:"+sid);
      }
   }

   //------------------------------------------------------------------------
   //------------------------------------------------------------------------
   public Map<String,String> getRfssNameMap( List<RfssConfigSetup> rfssList)
   {
      //showln("getRfssNameMap: rfssList="+rfssList.size());
      Map<String,String> map = new HashMap<String,String>();

      // build RfssName changes map
      if( rfssList != null)
      for( RfssConfigSetup rfssSetup: rfssList)
      {
         RfssConfig rfssConfig = (RfssConfig)rfssSetup.getReference();
         String rname = rfssConfig.getRfssName();

         String newRfssName = rfssSetup.getName();
         if( !rname.equals( newRfssName)) {
            map.put( new String(rname), newRfssName);
         }
      }
      return map;
   }

   //------------------------------------------------------------------------
   public boolean reconcile( List<RfssConfigSetup> rfssList,
      List<GroupConfigSetup> gcsList, List<SuConfigSetup> scsList) 
      throws IllegalArgumentException
   {
      //showln("rfssList="+rfssList.size());
      //showln("gcsList="+gcsList.size());
      //showln("scsList="+scsList.size());

      // process changes in RfssConfig
      if( rfssList != null)
      for( RfssConfigSetup rfssSetup: rfssList)
      {
         RfssConfig rfssConfig = (RfssConfig)rfssSetup.getReference();
         /*
         String rname = rfssConfig.getRfssName();
         String ipAddress = rfssConfig.getIpAddress();
         int port = rfssConfig.getSipPort();
         int rid = rfssConfig.getRfssId();
         int wacnId = rfssConfig.getWacnId();
         int systemId = rfssConfig.getSystemId();
         boolean emulated = rfssConfig.isEmulated();
          */
         rfssConfig.setRfssName( rfssSetup.getName());
         rfssConfig.setIpAddress( rfssSetup.getIpAddress());
         rfssConfig.setSipPort( rfssSetup.getPort());
         rfssConfig.setRfssId( rfssSetup.getId());
         rfssConfig.setWacnId( rfssSetup.getWacnId());
         rfssConfig.setSystemId( rfssSetup.getSystemId());
         rfssConfig.setEmulated( rfssSetup.getEmulated());
      }

      if( gcsList != null)
      for( GroupConfigSetup gcsSetup: gcsList)
      {
         GroupConfig groupConfig = (GroupConfig)gcsSetup.getReference();

         String gname = groupConfig.getGroupName();
         int gid = groupConfig.getGroupId();
         int gwacnId =  groupConfig.getSysConfig().getWacnId();
         int gsystemId = groupConfig.getSysConfig().getSystemId();
         String grfss = groupConfig.getHomeRfss().getRfssName();

         logger.debug("GROUP-homeRfss="+grfss);
         //showln("GROUP-gwacnId="+gwacnId);
         //showln("GROUP-gsystemId="+gsystemId);

         // only homeRfss
         groupConfig.getHomeRfss().setRfssName( gcsSetup.getHomeRfssName());
      }

      if( scsList != null)
      for( SuConfigSetup scsSetup: scsList)
      {
         SuConfig suConfig = (SuConfig)scsSetup.getReference();
         int sid = suConfig.getSuId();
         int swacnId = suConfig.getWacnId();
         int ssystemId = suConfig.getSystemId();
         String homeRfss = suConfig.getHomeRfss().getRfssName();
         int homeRfssId = suConfig.getHomeRfss().getRfssId();
         String servRfss = suConfig.getInitialServingRfss().getRfssName();
         int servRfssId = suConfig.getInitialServingRfss().getRfssId();
         boolean emulated = suConfig.isEmulated();

         String xsid = Integer.toHexString(sid);
         String nsid = Integer.toHexString(scsSetup.getId());
         logger.debug("SU-ID="+xsid +" homeRfss="+homeRfss +" servRfss="+servRfss);
         showln("SU-ID="+xsid+" new-ID="+nsid+" new-Emulated="+scsSetup.getEmulated());

         // only SuId, homeRfss and ServingRfss    
         suConfig.setEmulated( scsSetup.getEmulated());
         suConfig.setSuId( scsSetup.getId());

         suConfig.getHomeRfss().setRfssName( scsSetup.getHomeRfssName());
         suConfig.getInitialServingRfss().setRfssName( scsSetup.getServingRfssName());
      }
      return true;
   }

   //------------------------------------------------------------------------
   public boolean checkUniqueness( List<RfssConfigSetup> rfssList)
      throws IllegalArgumentException
   {
      HashMap<String,RfssConfigSetup> ipmap = new HashMap<String,RfssConfigSetup>();
      HashMap<String,RfssConfigSetup> idmap = new HashMap<String,RfssConfigSetup>();
      logger.debug("****** checkUniqueness(): START...");
            
      // verify the setup data
      for( RfssConfigSetup rfssSetup: rfssList)
      {
         String ipAddress = rfssSetup.getIpAddress();
         int port = rfssSetup.getPort();

         // ipAddress:port must be unique
         String key = ipAddress +":" + port;
         RfssConfigSetup xrfssSetup = ipmap.get( key);
         if( xrfssSetup == null) {
            ipmap.put( key, rfssSetup);
         } else {
            String msg = "Duplicate IP:port in RfssConfigSetup: "+key;
            logger.debug("checkUniqueness(): "+msg);
            throw new IllegalArgumentException(msg);
         }

         // idString must be unique
	 String idString = rfssSetup.getIdString();
         xrfssSetup = ipmap.get( idString);
         if( xrfssSetup == null) {
            idmap.put( idString, rfssSetup);
         } else {
            String msg = "Duplicate ID in RfssConfigSetup: "+idString;
            logger.debug("checkUniqueness(): "+msg);
            throw new IllegalArgumentException(msg);
         }
      }

      ipmap.clear();
      // rfssName must be unique
      for( RfssConfigSetup rfssSetup: rfssList)
      {
         String rfssName = rfssSetup.getName();
         RfssConfigSetup xrfssSetup = ipmap.get( rfssName);
         if( xrfssSetup == null) {
            ipmap.put( rfssName, rfssSetup);
         } else {
            String msg = "Duplicate rfssName in RfssConfigSetup: "+rfssName;
            logger.debug("checkUniqueness(): "+msg);
            throw new IllegalArgumentException(msg);
         }
      }

      idmap.clear();
      ipmap.clear();
      logger.debug("****** checkUniqueness(): DONE...");
      return true;
   }

   //------------------------------------------------------------------------
   public boolean reconcileTesterConfiguration( ISSITesterConfiguration testerConfig,
      List<RfssConfigSetup> rfssList)
      throws IllegalArgumentException
   {
      // build name map
      nameMap = getRfssNameMap( rfssList);

      // verify setup data first
      checkUniqueness( rfssList);

      // this will modify the RfssConfig stored in the reference
      HashMap<String,RfssConfigSetup> rfssIpMap = new HashMap<String,RfssConfigSetup>();
      HashMap<String,RfssConfigSetup> rfssNameMap = new HashMap<String,RfssConfigSetup>();
      HashMap<String,Boolean> rfssEmulatedMap = new HashMap<String,Boolean>();

      // process changes in RfssConfig
      for( RfssConfigSetup rfssSetup: rfssList)
      {
         RfssConfig rfssConfig = (RfssConfig)rfssSetup.getReference();
         // save a copy
         String rfssName = rfssConfig.getRfssName();
         String ipAddress = rfssConfig.getIpAddress();
         int port = rfssConfig.getSipPort();
         /*
         int rid = rfssConfig.getRfssId();
         int wacnId = rfssConfig.getWacnId();
         int systemId = rfssConfig.getSystemId();
          */
         boolean emulated = rfssConfig.isEmulated();

         // it is safe to update here
         rfssConfig.setRfssName( rfssSetup.getName());
         rfssConfig.setIpAddress( rfssSetup.getIpAddress());
         rfssConfig.setSipPort( rfssSetup.getPort());
         rfssConfig.setRfssId( rfssSetup.getId());
         rfssConfig.setWacnId( rfssSetup.getWacnId());
         rfssConfig.setSystemId( rfssSetup.getSystemId());
         rfssConfig.setEmulated( rfssSetup.getEmulated());

         String newRfssName = rfssSetup.getName();
         String newIpAddress = rfssSetup.getIpAddress();
         int newPort = rfssSetup.getPort();
         boolean newEmulated = rfssSetup.getEmulated();

         // detect rfssName changes
         //---------------------------------
         if( !rfssName.equals( newRfssName)) {
            logger.debug("reconcileTesterConfig: rfssName="+rfssName+" newRfssName="+newRfssName);
            rfssNameMap.put( rfssName, rfssSetup);
         }
         //---------------------------------
         if( !ipAddress.equals( newIpAddress) ) { 
            logger.debug("reconcileTesterConfig: ipAddress="+ipAddress+" newIpAddress="+newIpAddress);
            rfssIpMap.put( ipAddress, rfssSetup);
         }
         else if (port != newPort) {
            // for emulator
            logger.debug("reconcileTesterConfig: EM-ipAddress="+ipAddress);
            rfssIpMap.put( ipAddress+":"+port, rfssSetup);
         }
         //---------------------------------
         if( emulated != newEmulated) {
            logger.debug("reconcileTesterConfig: emulated="+emulated+" newEmulated="+newEmulated);
            rfssIpMap.put( ipAddress, rfssSetup);
            rfssEmulatedMap.put( rfssName, new Boolean(newEmulated));
         }
      }  // rfssList

      logger.debug("reconcileTesterConfig: rfssIpMap.size="+rfssIpMap.size());
      logger.debug("reconcileTesterConfig: rfssNameMap.size="+rfssNameMap.size());
      logger.debug("reconcileTesterConfig: rfssEmulatedMap.size="+rfssEmulatedMap.size());
      logger.debug("reconcileTesterConfig: rfssIpMap="+rfssIpMap);

      // fixup in one-pass 
      logger.debug("reconcileTesterConfig: ===============FIXUP in one pass");
      //--------------------------------------------------------------------
      HashSet<DaemonWebServerAddress> dwsaAddrs = 
         (HashSet<DaemonWebServerAddress>)testerConfig.getDaemonWebServerAddresses().clone();
      for( DaemonWebServerAddress dwsa: dwsaAddrs) {

         // clear isConformanceTester
	 boolean bflag = dwsa.isConformanceTester();
         String ipAddress = dwsa.getIpAddress();
         logger.debug("reconcileTesterConfig: =====dwsa.ip="+ipAddress+" isConformanceTester: "+bflag);

	 if( dwsa.getRefIdCount() == 1) {
	    dwsa.setConformanceTester(false);
logger.debug("reconcileTesterConfig: "+ipAddress+" FIXUP setConformanceTester(F) from "+bflag);
         }

         RfssConfigSetup  rfssSetup = rfssIpMap.get( ipAddress);
logger.debug("reconcileTesterConfig: =====RfssConfigSetup: rfssSetup="+rfssSetup);

         if( rfssSetup != null) {
            String rfssName = rfssSetup.getName();
            //String newIpAddress = rfssSetup.getIpAddress();
            logger.debug("reconcileTesterConfig: ipAddress="+ipAddress+" rfssName="+rfssName);
            logger.debug("reconcileTesterConfig: ===> newIpAddress="+rfssSetup.getIpAddress());
            // fixup ipAddress
            //dwsa.setIpAddress( rfssSetup.getIpAddress());

//Check RFSS RefIdCount, referenced, owned 
logger.debug("reconcileTesterConfig: rfssName="+rfssName);
logger.debug("reconcileTesterConfig: getRefIdCount="+dwsa.getRefIdCount());
logger.debug("reconcileTesterConfig: isRefByRfssName="+dwsa.isReferencedByRfssName(rfssName));

//EMULATOR_SPLIT_DWSA
	    if(dwsa.isReferencedByRfssName(rfssName) && dwsa.getRefIdCount() > 1) {
logger.debug("reconcileTesterConfig: ++++++++++++++++++++++++++++++");
               if(!ipAddress.equals( rfssSetup.getIpAddress())) {
                  logger.debug("reconcileTesterConfig: REMOVE tag from oldDWSA, create a newDWSA...");
                  String nodeName = rfssName+".daemon";
                  DaemonWebServerAddress ndwsa = new DaemonWebServerAddress(nodeName,
                     rfssSetup.getIpAddress(), dwsa.getHttpPort(),
                     rfssSetup.getEmulated(), dwsa.isPacketMonitor());
                  testerConfig.removeRefId( nodeName);
                  testerConfig.addDaemonWebServerAddress(ndwsa);
                  testerConfig.addRefId(nodeName, ndwsa);
                  logger.debug("reconcileTesterConfig: testerConfig.addRefId: nodeName="+nodeName);
               } else {
                  logger.debug("reconcileTesterConfig: will not REMOVE, same ip...");
               }
logger.debug("reconcileTesterConfig: ++++++++++++++++++++++++++++++");
            }
            else 
            {
logger.debug("reconcileTesterConfig: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
logger.debug("reconcileTesterConfig: ?? MERGE into existing DWSA for ip="+
              rfssSetup.getIpAddress()+":"+rfssSetup.getPort());
logger.debug("reconcileTesterConfig: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

               List<DaemonWebServerAddress> dwsaList = 
                  testerConfig.getDaemonWebServerAddressByIpAddress(rfssSetup.getIpAddress());
logger.debug("reconcileTesterConfig: dwsaList.size="+dwsaList.size());
logger.debug("reconcileTesterConfig: dwsaList="+dwsaList);
               if(dwsaList.size()==1) {
                  DaemonWebServerAddress mergedwsa = dwsaList.get(0);
                  // if ip not same, remove from testerconfig
		  if( !dwsa.getName().equals( mergedwsa.getName())) {
logger.debug("reconcileTesterConfig: remove DWSA from testerConfig: "+dwsa.getName());
                     testerConfig.removeDaemonWebServerAddress(dwsa);

logger.debug("reconcileTesterConfig: addRefId to mergedwsa="+mergedwsa);
                     //testerConfig.addRefId(dwsa.getName(), mergedwsa);
                     mergedwsa.addRefId(dwsa.getName());

                     boolean cflag = mergedwsa.isConformanceTester();
//EMULATOR_SPLIT
                     rfssSetup.setEmulated( cflag);
                     RfssConfig rfssConfig = (RfssConfig)rfssSetup.getReference();
logger.debug("reconcileTesterConfig: rfssConfig.setEmulated(): "+cflag+" rfssConfig="+rfssConfig);
		     rfssConfig.setEmulated( cflag);
                     // force a change in emulated flag
                     rfssEmulatedMap.put( rfssName, new Boolean(cflag));
		  }
		  else {
                     logger.debug("reconcileTesterConfig: ip is same - no change");
                  }
               }
	       else {
logger.debug("reconcileTesterConfig: dwsa.setIpAddress(): "+rfssSetup.getIpAddress());
                  dwsa.setIpAddress( rfssSetup.getIpAddress());
               }
            }

            // fixup isConformanceTester based on RFSS emulated 
            //dwsa.setConformanceTester( rfssSetup.getEmulated());
            //logger.debug("reconcileTesterConfig(1): FIXUP isConformanceTester="+
            //   rfssSetup.getName()+":"+rfssSetup.getEmulated());
         }

         logger.debug("reconcileTesterConfig: ===============process tag loop");
//ConcurrentModificationException
         //for( String tag: dwsa.getRefIds()) {
         HashSet<String> tagSet = new HashSet<String>(dwsa.getRefIds());
         for( String tag: tagSet) {
            // tag= rfss_2.domain
            logger.debug("reconcileTesterConfig: tag="+tag);
            String[] parts = tag.split("\\.");
            if( parts.length==2) {
               rfssSetup = rfssNameMap.get(parts[0]);
               if( rfssSetup != null) {
                  dwsa.removeRefId( tag);
                  String newTag = rfssSetup.getName() +"." +parts[1];
                  logger.debug("reconcileTesterConfig: newTag="+newTag);
                  dwsa.addRefId( newTag);

                  //logger.debug("reconcileTesterConfig(2): FIXUP isConformanceTester="+
                  //   rfssSetup.getName()+":"+rfssSetup.getEmulated());
                  //dwsa.setConformanceTester( rfssSetup.getEmulated());
               }
            }
         }

         logger.debug("reconcileTesterConfig(3): FIXUP isConformanceTester in loop-size: "+rfssList.size());
	 // fixup isConformanceTester on a separate loop
         for( RfssConfigSetup xrfssSetup: rfssList)
         {
            String xipAddress = xrfssSetup.getIpAddress();
            if( xipAddress.equals( dwsa.getIpAddress())) {
               logger.debug("reconcileTesterConfig(3): "+xipAddress+":"+
                  xrfssSetup.getName()+":"+xrfssSetup.getEmulated());
               // fixup emulated flag
               Boolean dflag = (Boolean)rfssEmulatedMap.get(xipAddress);
               if( dflag != null) {
logger.debug("reconcileTesterConfig(3): setEmulated="+dflag);
                   xrfssSetup.setEmulated(dflag.booleanValue());
               }

               //TODO: for emulator, the DWSA is shared by RFSSes
logger.debug("reconcileTesterConfig(3): getRefIdCount="+dwsa.getRefIdCount());
               if(dwsa.getRefIdCount() == 1) {
                  boolean xflag = dwsa.isConformanceTester();
                  dwsa.setConformanceTester( xrfssSetup.getEmulated());
                  logger.debug("reconcileTesterConfig(3): "+xipAddress+ " FIXUP "+
                               "isConformanceTester="+xflag+" to "+xrfssSetup.getEmulated());
               }
            }
         }
      }  // all-dwsa

      rfssIpMap.clear();
      rfssNameMap.clear();
      rfssEmulatedMap.clear();
      logger.debug("reconcileTesterConfig: done...");
      return true;
   }

   //------------------------------------------------------------------------
   public void reconcileAll( ISSITesterConfiguration testerConfig,
      List<RfssConfigSetup> rfssList,
      List<GroupConfigSetup> gcsList, List<SuConfigSetup> scsList) 
      throws IllegalArgumentException
   {
      reconcileTesterConfiguration( testerConfig, rfssList);
      reconcile( rfssList, gcsList, scsList);
   }

   //------------------------------------------------------------------------
   public void reconfigureDaemonTesterService(TopologyConfig topologyConfig,
      ISSITesterConfiguration testerConfig)
   {
      // when user runs a test(3 RFSS), switch to different test(2 RFSS)
      // in the Setup Test,  select Cancel option, then Generate Trace
      // the previous active Daemons needed to be reconfigue(turn off)
      if(verbose)
      logger.debug("reconfigureDaemonTS(): START...");

      // deduce the isConformanceTester from topology
      // it may also need to look at the emulated flag
      List<String> rfssList = getRfssNameList(topologyConfig);
      for( DaemonWebServerAddress dwsa: testerConfig.getDaemonWebServerAddresses())
      {
         // clear isConformanceTester
	 boolean bflag = dwsa.isConformanceTester();
         if(verbose)
            logger.debug("reconfigureDaemonTS: isConformanceTester="+bflag);

	 if( dwsa.getRefIdCount() == 1) {
	    dwsa.setConformanceTester( false);
            if(verbose)
               logger.debug("reconfigureDaemonTS: FIXUP-loop setConformanceTester(F) from "+bflag);
         }

	 // RFSS Configuration
         for (RfssConfig rfssConfig: topologyConfig.getRfssConfigurations())
         {
            String ipAddress = rfssConfig.getIpAddress();
            String rfssName = rfssConfig.getRfssName();
            if(verbose)
               logger.debug("reconfigureDaemonTS: checking ipAddress="+ipAddress+" rfssName="+rfssName);
            if( ipAddress.equals( dwsa.getIpAddress())) {
               if(verbose)
               logger.debug("reconfigureDaemonTS: FIXUP isConformanceTester "+
                  ipAddress+":"+rfssConfig.getEmulated());
	       dwsa.setConformanceTester( rfssConfig.getEmulated());

/***
//check RFSS RefIdsCount
showln("reconfigureDaemonTS: rfssName="+rfssName);
showln("reconfigureDaemonTS: getRefIdCount="+dwsa.getRefIdCount());
showln("reconfigureDaemonTS: isRefByRfssName="+dwsa.isReferencedByRfssName(rfssName));
	       if(dwsa.isReferencedByRfssName(rfssName) && dwsa.getRefIdCount() > 1) {
showln("reconfigureDaemonTS: remove tag from oldDWSA, create a newDWSA...");
               }
 ***/
            }
         }

         /*** old code
         // deduce the isConformanceTester from the refid only
         HashSet<String> tagSet = new HashSet<String>(dwsa.getRefIds());
         for( String tag: tagSet) {
            logger.debug("reconfigureDaemonTesterService: tag="+tag);
            String[] parts = tag.split("\\.");
            if( parts.length==2) {
               for( String rname: rfssList) {
                  if( rname.equals( parts[0])) {
                     logger.debug("reconfigureDaemonTesterService: matched="+rname);
		     // set isCOnformanceTester
                     dwsa.setConformanceTester(true);
                  }
               }
            }
         }
	  ***/
      }
      if(verbose)
      logger.debug("reconfigureDaemonTS(): DONE...");
   }
   //------------------------------------------------------------------------
}
