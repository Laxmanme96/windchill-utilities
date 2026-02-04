package ext.enersys.cm2.xml.service;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.fc.WTObject;
import wt.inf.container.WTContainer;
import wt.method.RemoteInterface;
import wt.project.Role;
import wt.util.WTException;

@RemoteInterface
public interface EnerSysApprovalMatrixDefinition {
	// For Build v3.1
	public String getChangeTrackInternalValue(TypeIdentifier objTI, Object obj);

	// For build v2.11
	public boolean isFastTrackCNCTpboBeanCheck(TypeIdentifier objTI, NmCommandBean commandBean);

	public boolean isFastTrackCNCTobjectCheck(WTObject obj);

	public void reloadFastTrackCNCTMappingFromMatrix();

	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getFastTrackCNCTParticipants(WTObject varObj, TypeIdentifier pboTI, Set<NmOid> affectedObj);

	// For Build v2.2 - Added for Strict-Auto-Population
	public boolean isStrictAutoPopulation(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMapToCheck, Role roleToCheck);

	// For Build v2.1 - Added for Wizard processing
	public boolean isFirmwareCRCNCTpbo(TypeIdentifier objTI);

	public String getSubSetOfOnlyRoles(int givenSequenceNumber, LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap);

	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getNotificationRoleMapWizard(Set<NmOid> affectedObj, TypeIdentifier pboTI, NmCommandBean commandBean)
			throws WTException;

	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMapWizard(Set<NmOid> affectedObj, TypeIdentifier pboTI, NmCommandBean commandBean)
			throws WTException;

	// For Build v1.13 - Deviation Workflow
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getDeviationFastTrackParticipants(WTObject varObj, TypeIdentifier pboTI, Set<NmOid> affectedObj);

	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getDeviationFullTrackParticipants(WTObject varObj, TypeIdentifier pboTI, Set<NmOid> affectedObj);

	public void reloadDeviationMappingFromMatrix();

	// For Build v1.13 - Firmware Workflow
	public void reloadFirmwareMappingFromMatrix();

	public boolean isFirmwareCRCNCTpbo(WTObject pbo);

	LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getFirmwareCRBParticipants(WTObject pbo, TypeIdentifier pboTI, Set<NmOid> affectedObj);

	// For Build v1.13 - For all workflows

	public String getNotificationEmailIds(final WTObject pbo, final String notificationRoleUserMap, final String DELIM) throws WTException;

	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getNotificationRoleMap(WTObject pbo);

	public boolean isBypassContextForUserSelection(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMapToCheck, Role roleToCheck);

	//
	public Set<String> getBlackListedStatesForObject(WTObject affectedItem);

	// 7th SEPT 2020
	public String getSubSetRoles_Lite(int givenSequenceNumber, String requiredRoleUserMap, String keyStr);

	public LinkedHashSet<String> getParticipantRolesForSequenceNumber_Lite(int givenSequenceNumber, Set<String> mapKeys);

	//
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMap(WTObject pbo) throws WTException;

	public LinkedHashSet<String> getParticipantRolesForSequenceNumber(int givenSequenceNumber, LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap);

	public Iterator<Integer> getSequencesInOrder(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap);

	public NodeList getParticipantNodeListForObject(WTObject affectedItem, WTObject pbo);

	public boolean isStateNodeDefinedForTypeAndChange(String lcState, String PBO_TYPE_TO_SEARCH, String subTypeInfo);

	public boolean isChangeNodeDefinedForType(String PBO_TYPE_TO_SEARCH, String subTypeInfo);

	public boolean isTypeNodePresent(String subTypeInfo);

	public Element getApprovalMatrixElementForObject(WTObject affectedItem);

	public String getCorrespondingPBOTag(String pboType);

	public Element getNodeForGivenType(String subTypeInfoOrDefault, boolean assignDefault);

	public void reloadTypeListMappingFromMatrix();

	public void loadApprovalMatrixDOMDocument();

	public String generateRoleWithSequencesToDisplayInUI(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap);

	public LinkedHashMap<String, WTContainer> getRoleContainerInfo(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap, String role);

	public String getSubSetRoles(int givenSequenceNumber, String requiredRoleUserMap, LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap);

	public String getSequencesInOrder_String(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap);

	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> additionalRolesAdded(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap,Set<NmOid> affectedItemSet);
	
	// CONSTANTS DEFINITIONS BELOW:
	String fileSeperator = File.separator;
	String APPROVAL_MATRIX_FILE_PATH = fileSeperator + "ext" + fileSeperator + "enersys" + fileSeperator + "properties" + fileSeperator;
	String APPROVAL_MATRIX_FILENAME = "EnerSys CM Approval Matrix.xml";

	String WTDOCUMENT_APPROVAL_MATRIX_DOCU_NUMBER = "ENERSYS_CM_APPROVAL_MATRIX";
	String WTDOCUMENT_APPROVAL_MATRIX_DOCU_NAME = "EnerSys Change Management Approval Matrix Document";

	String HEAD_TAG = "enersys-cm-matrix";
	String APPR_MAT_TAG = "approval-matrix";
	String APPR_MAT_TYPE_ATTR = "type";
	String APPR_MAT_AO_BLACKLISTED_STATE_ATTR = "ao-blacklisted-states";
	String APPR_MAT_DESCENT_ATTR = "apply-to-descendant";
	String CHANGE_REQUEST_TAG = "change-request";
	String CHANGE_NOTICE_TAG = "change-notice";
	String CHANGE_ACTIVITY_TAG = "change-activity";
	String DOCUMENT_APPROVAL_TAG = "document-approval";
	String FAST_TRACK_CN_CT_TAG = "fast-track-cn-ct-approval-matrix"; // Added for Build v2.11
	String DEVIATION_TAG = "deviation-approval-matrix"; // Added for Build v1.12
	String DEVIATION_FULL_TAG = "deviation-full-track"; // Added for Build v1.13
	String DEVIATION_FAST_TAG = "deviation-fast-track"; // Added for Build v1.13
	String FIRMWARE_TAG = "firmware-approval-matrix"; // Added for Build v1.13
	String FIRMWARE_CRB_TAG = "crb-team"; // Added for Build v1.13
	String NOTIFICATION_TEAM_TAG = "notification-team"; // Added for Build v1.13
	String FETCH_ALL_PARTICIPANTS_ATTR = "fetch-all-participants"; // Added for Build v1.13
	String STRICT_AUTO_POPULATE_FROM_CONTEXT_TEAM_ATTR = "strict-auto-populate-from-context-team-for";// Added for Build v2.2
	String MAKE_MANDATORY_FOR_ATTR = "make-mandatory-for";// Added for Build v2.2
	String MAKE_OPTIONAL_FOR_ATTR = "make-optional-for";// Added for Build v2.5
	String MAKE_MANDATORY_FOR_CHANGE_TRACK = "make-mandatory-for-change-track"; // Added for build v3.1
	String PROMOTION_REQUEST_TAG = "promotion-request";
	String PARTICIPANT_TAG = "participant";
	String PARTICIPANT_TYPE_ATTR = "type";
	String PARTICIPANT_PARTICIPANTS_ATTR = "participants";
	String PARTICIPANT_DEST_ROLE_ATTR = "destRole";
	String PARTICIPANT_ORDER_ATTR = "order";
	String PARTICIPANT_APPROVAL_ATTR = "approval";
	String DEFAULT_TAG = "DEFAULT";
	String APPR_REQUIRED_VALUE = "Required";
	String APPR_OPTIONAL_VALUE = "Optional";
	String TYPE_SELECTION = "type-selection";
	// Added indices for Build v2.2
	int PARTICIPANT_ROLE_INDEX = 0;
	int IS_REQUIRED_INDEX = 1;
	int SEQUENCE_ORDER_INDEX = 2;
	int IS_FETCH_ALL_PARTICIPANTS_INDEX = 3;
	int STRICT_AUTO_POP_INDEX__ROLETYPE = 2;
	int __MAKE_MANDATORY_FOR_INDEX_NOT_INCLUDED_IN_MAP__ = 5;

	// Constants used in String manipulations
	String COLON_ROLE_MAP_DELIM = ":";
	String COMMA_DELIM = ",";
	String SEMICOLON_DELIM = ";";
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMapCN(
			Set<NmOid> affectedItemSet, TypeIdentifier creatObjTI, WTObject obj) throws WTException;
}
