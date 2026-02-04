package ext.enersys.cm4.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.json.JSONObject;

import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.change2.WTChangeOrder2;
import wt.enterprise.RevisionControlled;
import wt.fc.Persistable;
import wt.fc.WTObject;
import wt.inf.container.WTContainer;
import wt.method.RemoteInterface;
import wt.org.WTUser;
import wt.project.Role;
import wt.util.WTException;

/**
 * @since Build v3.8
 * @author CGI
 *
 *         Modified the existing code in such way all data needed for the Change
 *         management Approval Matrix flow is fetched directly from the
 *         Attribute "ext.enersys.SETUP_PARTICIPANT_STR" of change management
 *         objects like (Change Request,Change Notice,Promotion
 *         Request,Deviation) , In order to ensure the future logic changes
 *         related to Approval matrix can be updated only in the
 *         "ext.enersys.SETUP_PARTICIPANT_STR" Attribute level.
 *
 *
 *         Setup Participant attribute Format :
 *
 *         OrderNo1=ROLENAME1|=|USER11^USER12^USER13::OPTIONAL::CONTAINER1%oid%OID1,CONTAINER11
 *         %oid% OID11
 *         |&|ROLENAME12|=|USER12^USER22^USER32::OPTIONAL::CONTAINER12%oid%OID12,CONTAINER21
 *         %oid% OID21|&&| OrderNo2=ROLENAME2
 *         |=|USER1^USER2^USER3::REQUIRED::CONTAINER2%oid%OID2,CONTAINER21%oid%OID21
 *         |&| ROLENAME21 |=| USER21^USER22^USER32::REQUIRED::CONTAINER24 %oid%
 *         OID24,CONTAINER25 %oid% OID25 |~~| OrderNo1
 *         =ROLENAME1:USER1,USER2,USER3:OPTIONAL:CONTAINER1|OID1,CONTAINER11|OID11;
 *         OrderNo2
 *         =ROLENAME2:USER1,USER2,USER3:REQUIRED:CONTAINER2|OID2,CONTAINER21|OID21;
 *
 */

@RemoteInterface
public interface CM4Service {
	public static final String SETUP_PARTICIPANTS_ATTR = "ext.enersys.SETUP_PARTICIPANT_STR";
	public static final String APPNOTIF_DELIMITER = "%~%";
	public static final String ORDERROLES_DELIMITER = "%&";
	public static final String EQUALROLES_DELIMITER = "=";
	public static final String SPLITROLENOS_DELIMITER = "&%";
	public static final String SPLITROLESINFO_DELIMITER = "%:%";
	public static final String SPLITUSERSINFO_DELIMITER = "^";
	public static final String SPLITCONTAINEROIDINFO_DELIMITER = "%oid%";
	public static final String SPLITCONTAINERINFO_DELIMITER = "~%";
	public static final String APPNOTIFUSER_DELIMITER = "~~";
	public static final String NOUSERASSIGNED = "N%U";

	public static final String REQUIRED_CONST = "Required";
	public static final String OPTIONAL_CONST = "Optional";
	public static final String REQUIRED_FLAGVALUE = "R";
	public static final String OPTIONAL_FLAGVALUE = "O";
	public static final String APPROVERROLES_PREFIX = "APPROVERS";
	public static final String NOTIFICATIONROLES_PREFIX = "NOTIFICATION";
	public static final String OPTIONALROLEDISPLAY_PREFIX = " (Opt.)";

	public static final String REFERENECE_OLDDATA_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static final String[] LIST_OF_ROLES_TO_PRESERVE = { "PRODUCT MANAGER", "DEVIATION AUTHORS", "CHANGE MANAGER",
			"DEVIATION APPROVER", "REVIEWER", "ASSIGNEE", "SUBMITTER", "NOTIFICATION ONLY", "PLANT_BOM_ENGINEER" };
	public static final Set<Role> ROLES_TO_PRESERVE_IN_TEAM = Arrays.stream(LIST_OF_ROLES_TO_PRESERVE).map(Role::toRole)
			.collect(Collectors.toSet());
	public static final int DEFAULT_SEQUENCE_NUMBER = 1;
	public static final String RVRFIRMWARECHANGETASK_NAME = "2. Release the Verification Report";
	public static final String RVRFIRMWARECHANGETASK_ROLE = "SOFTWARE QUALITY AUTHORITY";
	public static final String CONTEXT_PREF_INTR_NAME = "/ext/enersys/ENERSYS_SETUP_PARTICIPANT_WIZARD/HIDDEN_FOR_CONTEXT";
	public static final String SUBTYPE_PREF_INTR_NAME = "/ext/enersys/ENERSYS_SETUP_PARTICIPANT_WIZARD/HIDDEN_FOR_SUBTYPE";
	public static final String APPROVALFLOW_TAB_PREF_NAME = "/ext/enersys/APPROVALFLOW_TAB/REFERENCEDATETIME_FOR_NEW_SETUPPARTICIPANTVALUE_FORMAT";
	public static final String PREFERENCE_SEPARATOR = ";";

	// Constants used in String manipulations
	String COLON_ROLE_MAP_DELIM = ":";
	String QUAOTE_ROLE_MAP_DELIM = "\"";
	String COMMA_DELIM = ",";
	String SEMICOLON_DELIM = ";";
	String USERINFOSPLIT_DELIM = "\\^";
	int ROLE_ORDER_INDEX = 0;
	int REQUIRED_ORDER_INDEX = 1;
	int SEQUENCE_ORDER_INDEX = 2;
	int CONTAINER_ORDER_INDEX = 3;

	public Iterator<Integer> getOrderSequenceValues(WTObject changeObj);

	public String getOrderSequenceValues_String(WTObject changeObj);

	public LinkedList<String> getParticipantAttributeOnObject(Persistable per);

	public Persistable setParticipantAttributeOnObject(Persistable per, String[] selectedRoleParticipantValue);

	public boolean issetUPparticipantAttOldData(WTObject changeObj);

	public String generateRoleInSequenceToDisplayInUI(WTObject changeObj) throws WTException;

	public LinkedList<TreeSet<String>>[] gettingRoleValuesInTheOrderWithRO(LinkedList<String> setUpParticipantAttList);

	public String setAttributeOnChangeObjectsWithoutUsers(
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> rolesHash,
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> rolesNotificationHash);

	public void setAttributeOnAddingUserDetails(String setAttributeOnChangeObjectsWithoutUsers, String userDetails,
			WTObject changeObj);

	public HashMap<Role, Set<WTUser>>[] getRoleUsersMap(WTObject changeObj);

	boolean recomputeTeamTemplate(@NotNull WTObject obj);

	public int getOrderSequenceValues(LinkedList<String> setUpParticipantAttList);

	public String getNotificationRoleUserStr(WTObject pbo);

	public String[] getRoleUsersMapInString(WTObject changeObj);

	public String getApproverRoleUserStr(WTObject changeObj);

	public String gettingNotificationRolesForSpecificOrder(WTObject changeObj, int orderNumber);

	public String gettingRolesForSpecificOrder(WTObject changeObj, int orderNumber);

	public boolean isReleaseVerifRepoTask(WTObject changeObj);

	public WTObject getChangeRVRTaskForOrder(WTChangeOrder2 obj);

	public void setAttributeOnRVRTask(WTObject obj);

	public RevisionControlled getPreviousRevisionObject(WTObject obj);

	public String getChangeTransitionState(RevisionControlled currentObj);

	public boolean showSetupParticipantStepReleaseTargetState(NmCommandBean cBean);

	public boolean isTeamRecomputeNeededFromUsers(WTObject obj);

	public HashMap<Persistable, HashMap<WTContainer, HashSet<Persistable>>> getWhereUsedContainers(
			WTObject changeObject, int threshold);

	public HashMap<String, HashMap<String, String>> getWhereUsedContainersString(
			HashMap<Persistable, HashMap<WTContainer, HashSet<Persistable>>> affectedObjContainerMapping,
			String parentContainerName);

	public String getContainerName(WTObject obj);

	public ArrayList getObjectDetails(WTObject obj) throws WTException;

	public JSONObject getEnerSysDistributionTargetValidationRuleDetails(WTObject obj, int threshold) throws WTException;

	public JSONObject getEnerSysImmediateParentStateValidationRuleDetails(WTObject obj, int threshold)
			throws WTException;

	public JSONObject getEnerSysPartCADAssociationValidationRule(WTObject changeObj, int threshold) throws WTException;

	public JSONObject getEnerSysDepedenceValidationRule(WTObject changeObject, int threshold) throws WTException;

	public JSONObject getResultingObjectValidationRule(WTObject changeObject, int threshold) throws WTException;

	public JSONObject getObjectVisualizationDetails(WTObject changeObject, int threshold) throws WTException;

	public void addEndItemstoTheCR(WTObject changeObject);

}
