//
package gov.nist.p25.issi.constants;


public interface XMLTagsAndAttributes {

   public static final String LOOPBACK_ADDRESS = "127.0.0.1";

   // place the constants in order 
   //-----------------------------------------------
   public static final String AVAILABILITY = "isAvailable";

   public static final String CANCEL_AFTER = "cancelAfter";
   public static final String CALLED_GROUP_NAME = "calledGroupName";
   public static final String CALLED_SU_NAME = "calledSuName";
   public static final String CALLING_SU_NAME = "callingSuName";
   public static final String CALLING_SU_INITRANS = "callingSuInitrans";
   public static final String CALLED_GROUP = "calledGroup";
   public static final String CHECK_SU_PRESENCE_ON_REGISTER = "checkSuPresenceOnRegister";

   public static final String COLOR = "color";
   public static final String DESCRIPTION = "description";
   public static final String DESTINATION_RFSS_NAME = "destinationRfssName";

   public static final String GENERATE_SIP_TRACE = "generateSipTrace";
   public static final String GENERATE_PTT_TRACE = "generatePttTrace";

   public static final String GLOBAL_TOPOLOGY_CONFIG = "global-topology-config";
   public static final String GRANT_PTT_REQUEST = "grantPttRequest";

   public static final String GROUP_CALL = "group-call-setup-scenario";
   public static final String GROUP_CALL_INVITE_PROCESSING_TIME = "groupCallInviteProcessingTime";
   public static final String GROUP_ID = "sgId";
   public static final String GROUP_NAME = "groupName";
   public static final String GROUP_REGISTRATION_EXPIRES_TIME = "groupRegistrationExpiresTime";
   //public static final String GROUP_SERVICE_PROFILE = "group-service-profile";

   public static final String HOME_RFSS_KNOWS_ABOUT_ME_FOR = "homeRfssKnowsAboutMeFor";
   public static final String HOME_RFSS_NAME = "homeRfssName";
   public static final String HOME_RFSS_REGISTERS_ME_FOR = "homeRfssRegistersMeFor";
   public static final String HTTP_CONFIGURATION_PORT = "httpPort";
   
   public static final String ID = "id";
   public static final String IP_ADDRESS = "ipAddress";
   public static final String INITIAL_STATE = "initialState";
   public static final String INITIATED_BY = "initiatedBy";
   public static final String INVITE_PROCESSING_DELAY = "inviteProcessingDelay";

   public static final String IS_ADVANCED_RTP_RESOURCE_MANAGEMENT_SUPPORTED = "isAdvancedRtpResourceManagementSupported";
   public static final String IS_CALLING_SU_INITRANS = "isCallingSuInitrans";
   public static final String IS_CONFIRMED = "isConfirmed";
   public static final String IS_EMULATED = "emulated";
   public static final String IS_EMERGENCY = "isEmergency";
   public static final String IS_FULL_DUPLEX = "isFullDuplex";
   public static final String IS_INTERESTED_IN_LOSING_AUDIO = "isInterestedInLosingAudio";
   public static final String IS_PROTECTED_CALL = "isProtected";
   public static final String IS_PROTECTION_SUPPORTED = "isProtectionSupported";
   public static final String IS_SU_TO_SU_CALL_ALLOWED = "isSuToSuCallAllowed";
   public static final String IS_TALK_SPURT_FORCED = "isTalkSpurtForced";
   public static final String IS_TALK_SPURT_SENT_AFTER_CALL_SETUP = "isTalkSpurtSentAfterCallSetup";
   public static final String IS_TERMINATED_BY_CALLED_SERVING = "isTerminatedByCalledServing";
   
   public static final String SELFTEST_PORT = "selftestPort";
   public static final String METHOD = "method";
   public static final String MAX_RTP_PORTS = "maxRtpPorts";
   public static final String OPERATION = "operation";
   public static final String PORT = "port";
   public static final String PRIORITY = "priority";
   public static final String PROTECTION_DISPOSITION = "protectionDisposition";

   public static final String QUERY_GROUP_CREDENTIALS = "queryGroupCredentialsBeforeRegistration";
   public static final String QUERY_SU_CREDENTIALS = "querySuCredentialsBeforeRegistration";

   public static final String REF_ID = "refId";
   public static final String REGISTER_CONFIRM_TIME = "registerConfirmTime";
   public static final String REGISTER_ON_FORCE = "registerOnForce";
   public static final String REGISTRATION = "registration-scenario";
   public static final String RFSS_ID = "rfssId";
   public static final String RFSS_NAME = "rfssName";
   public static final String RFSS_SCRIPT = "rfss-script";
   public static final String RFSSCONFIG = "rfssconfig";
   public static final String RF_RESOURCES_AVAILABLE = "rfResourcesAvailable";
   public static final String ROAMING = "roaming-scenario";
   public static final String RTP_DEBUG_LOG = "rtpdebugLog";
   public static final String RTP_MESSAGE_LOG = "rtpmessageLog";
   public static final String POST_CONDITION = "post-condition";

   public static final String SERVED_GROUP_LEASE_TIME = "servedGroupLeaseTime"   ;
   public static final String SERVED_SU_LEASE_TIME = "servedSuLeaseTime";
   public static final String SERVING_RFSS_NAME = "servingRfssName";
   public static final String SERVING_RFSS_REFERENCES_ME_FOR = "servingRfssReferencesMeFor";

   public static final String SIP_MESSAGE_LOG = "sipmessageLog";
   public static final String SU_ID = "suId";
   public static final String SU_NAME = "suName";
   public static final String SU_SCRIPT = "su-script";
   public static final String SU_TO_SU_CALL = "su-to-su-call-setup-scenario";
   public static final String SYSTEM_ID = "systemId";
   public static final String SYSTEM_NAME = "systemName";

   public static final String TALK_SPURT_SENDER = "talkSpurtSentBy";
   public static final String TERMINATE_AFTER = "terminateAfter";
   public static final String TERMINATED_BY = "terminatedBy";
   public static final String TEST_CASE = "test-case";
   public static final String TEST_COMPLETION_DELAY = "testCompletionDelay";
   public static final String TEST_NAME = "testName";
   public static final String TEST_SCRIPT = "test-script";
   public static final String TOPOLOGY = "topology";
   public static final String TOPOLOGY_CONFIG = "topologyConfig";
   public static final String TRIGGER = "trigger";
   public static final String TRIGGER_TYPE = "type";
   public static final String TRIGGER_VALUE = "value";

   public static final String TIME = "time";
   public static final String MSEC_TIME = "msectime";
   public static final String TRACE_GENERATION_TIME = "traceGenerationTime";
   public static final String UNBOUNDED = "UNBOUNDED";
   //public static final String USER_SERVICE_PROFILE = "user-service-profile";

   public static final String WACN_NAME = "wacnName";
   public static final String WACN_ID = "wacnId";
}
