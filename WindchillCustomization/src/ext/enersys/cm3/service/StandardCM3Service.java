package ext.enersys.cm3.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import wt.log4j.LogR;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.CreateOperationIdentifier;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.swlink.utils.NmCommandBeanUtils;

import ext.enersys.cm2.CM2Helper;
import ext.enersys.cm2.xml.EnerSysApprovalMatrixUtility;
import ext.enersys.cm2.xml.service.EnerSysApprovalMatrixDefinition;
import ext.enersys.delegate.EnerSysSetupParticipantAttrDelegate;
import ext.enersys.service.EnerSysHelper;
import ext.enersys.service.EnerSysService;
import ext.enersys.utilities.Debuggable;
import ext.enersys.utilities.EnerSysLogUtils;
import ext.enersys.utilities.EnerSysSoftTypeHelper;
import wt.change2.WTChangeOrder2;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.fc.collections.WTHashSet;
import wt.iba.definition.StringDefinition;
import wt.iba.value.IBAHolder;
import wt.iba.value.StringValue;
import wt.inf.container.OrgContainer;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.inf.team.ContainerTeam;
import wt.inf.team.ContainerTeamHelper;
import wt.inf.team.ContainerTeamManaged;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.State;
import wt.maturity.MaturityHelper;
import wt.maturity.Promotable;
import wt.maturity.PromotionNotice;
import wt.maturity.PromotionTarget;
import wt.org.OrganizationServicesHelper;
import wt.org.WTGroup;
import wt.org.WTOrganization;
import wt.org.WTPrincipal;
import wt.org.WTPrincipalReference;
import wt.org.WTUser;
import wt.part.WTPart;
import wt.pds.StatementSpec;
import wt.preference.PreferenceClient;
import wt.preference.PreferenceHelper;
import wt.project.Role;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.services.StandardManager;
import wt.session.SessionContext;
import wt.session.SessionHelper;
import wt.team.Team;
import wt.team.TeamHelper;
import wt.team.TeamManaged;
import wt.team.TeamTemplate;
import wt.util.WTException;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;

/**
 * Service defined to help with CM3.0 changes.<br>
 * Mainly revolving around SetupParticipant processing in wizard, delegate
 * processing & TeamTemplate processing.
 * 
 * @since Build v2.1
 * @author CGI Team
 *
 */
public class StandardCM3Service extends StandardManager implements Serializable, CM3Service, Debuggable {

	private static final long serialVersionUID = -4626066444910051750L;

	private static final String CLASSNAME = StandardCM3Service.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(StandardCM3Service.class.getName());
	private static final String APPROVER_ROLE = "APPROVER_ROLE";
	private static final String NOTIFICATION_ROLE = "NOTIFICATION_ROLE";

	private static final String ESG_CONTEXT_VISIBILITY_PREF_INTR_NAME = "/ext/enersys/ESG_SPECIFIC/ATTRIBUTE_CONTEXT_VISIBILITY";
	private static final String ESG_ERP_STATUS_INTRL_NAME = "ext.enersys.esg.ERP_STATUS";

	public static StandardCM3Service newStandardCM3Service() throws WTException {
		StandardCM3Service service = new StandardCM3Service();
		service.initialize();
		return service;
	}

	// FOR CHECKING SETUP PARTICIPANT STEP VISIBILITY //
	private static final String PREFERENCE_SEPARATOR = ";";
	public static final String SUBTYPE_PREF_INTR_NAME = "/ext/enersys/ENERSYS_SETUP_PARTICIPANT_WIZARD/HIDDEN_FOR_SUBTYPE";
	public static final String CONTEXT_PREF_INTR_NAME = "/ext/enersys/ENERSYS_SETUP_PARTICIPANT_WIZARD/HIDDEN_FOR_CONTEXT";

	public static final String MAP_PARAMETER_KEY_VAL = "ENERSYS_CT_AFFECTED_ITEMS_INFO";

	private static final Set<Role> ROLES_TO_PRESERVE_IN_TEAM;
	private static final String[] LIST_OF_ROLES_TO_PRESERVE = { "PRODUCT MANAGER", "DEVIATION AUTHORS",
			"CHANGE MANAGER", "DEVIATION APPROVER", "REVIEWER", "ASSIGNEE", "SUBMITTER", "NOTIFICATION ONLY" };
	static {
		ROLES_TO_PRESERVE_IN_TEAM = Arrays.stream(LIST_OF_ROLES_TO_PRESERVE).map(Role::toRole)
				.collect(Collectors.toSet());
	}
	private Set<TypeIdentifier> disabledTypes = null;
	private Set<String> disabledContexts = null;

	private static long lastRefresh = -1;
	private static final short REFRESH_TIME_INTERVAL = 30 * 1000; // 30 Second interval

	private boolean isPreferenceRefreshNeeded() {
		return (disabledTypes == null || disabledContexts == null
				|| ((System.currentTimeMillis() - lastRefresh) >= REFRESH_TIME_INTERVAL));
	}

	private void refreshValuesFromPreferences() {
		try {
			if (disabledTypes == null) {
				disabledTypes = new HashSet<>();
			}
			if (disabledContexts == null) {
				disabledContexts = new HashSet<>();
			}
			disabledTypes.clear();
			disabledContexts.clear();

			WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);
			String disabledTypePrefValue = (String) PreferenceHelper.service.getValue(SUBTYPE_PREF_INTR_NAME,
					PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);
			if (disabledTypePrefValue != null && !disabledTypePrefValue.isEmpty()) {
				for (String str : disabledTypePrefValue.split(PREFERENCE_SEPARATOR)) {
					disabledTypes.add(TypeIdentifierHelper.getTypeIdentifier(str.trim()));
				}
			}

			String disabledContextPrefValue = (String) PreferenceHelper.service.getValue(CONTEXT_PREF_INTR_NAME,
					PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);
			if (disabledContextPrefValue != null && !disabledContextPrefValue.isEmpty()) {
				for (String str : disabledContextPrefValue.split(PREFERENCE_SEPARATOR)) {
					disabledContexts.add(str.trim());
				}
			}
			lastRefresh = System.currentTimeMillis();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * TODO: Perform validations on Objects selected for<br>
	 * Promotion Request\Deviation\Change Management Create Wizards.
	 * 
	 * @since build v2.1
	 */
	@Override
	public String performWizardValidations(NmCommandBean commandBean) {
		StringBuilder sb = new StringBuilder();

		return sb.toString();
	}

	@Override
	public String getObjectTypeSelected(NmCommandBean commandBean) {
		String ret = null;
		try {
			ret = NmCommandBeanUtils.getComboBoxValue(commandBean, "createType");
			if (ret == null && commandBean.getParameterMap().get("createType") != null) {
				ret = (String) ((Object[]) commandBean.getParameterMap().get("createType"))[0];
			}
			if (ret == null) {
				Iterator itr = commandBean.getComboBox().keySet().iterator();
				while (itr.hasNext()) {
					String keyVal = (String) itr.next();
					if (keyVal != null && keyVal.contains("createType")) {
						ArrayList aList = (ArrayList) commandBean.getComboBox().get(keyVal);
						if (aList != null && !aList.isEmpty() && StringUtils.isNotBlank((String) aList.get(0))) {
							ret = ((String) aList.get(0)).trim();
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	private String getContainerString(NmCommandBean commandBean) {
		String containerOidStr = (String) ((Object[]) commandBean.getParameterMap().get("ContainerOid"))[0];
		containerOidStr = (containerOidStr != null && !containerOidStr.equalsIgnoreCase("null")) ? containerOidStr
				: null;
		return containerOidStr;
	}

	@Override
	public boolean showSetupParticipantStep(NmCommandBean commandBean) {
		if (isPreferenceRefreshNeeded()) {
			refreshValuesFromPreferences();
		}

		String objTypeSelected = getObjectTypeSelected(commandBean);
		String containerOidStr = getContainerString(commandBean);

		boolean res = false;
		if (StringUtils.isNotEmpty(objTypeSelected)) {
			try {
				objTypeSelected = "WCTYPE|" + objTypeSelected;
				String currentContainerName = "";

				if (containerOidStr != null) {
					ReferenceFactory rf = new ReferenceFactory();
					WTReference refObj = rf.getReference(containerOidStr);
					WTContainerRef contRefObj = WTContainerRef.newWTContainerRef((WTContainer) refObj.getObject());
					currentContainerName = contRefObj.getName();
				}

				TypeIdentifier contextObjectType = TypeIdentifierHelper.getTypeIdentifier(objTypeSelected);
				if (!disabledTypes.contains(contextObjectType) && !disabledContexts.contains(currentContainerName)) {
					res = true;
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		return res;
	}

	/**
	 * @deprecated from build V3.8 CM 4.0 This method is used to fetch the value of
	 *             role and user details from attribute
	 *             "ext.enersys.SETUP_PARTICIPANT_STR" in the change objects
	 * 
	 * 
	 * @param obj Change object
	 */

	private String getParticipantStringFromObject(WTObject obj) throws WTException {
		Object value = null;
		String val = null;
		// Get roles vs participants from IBA
		PersistableAdapter pa = new PersistableAdapter(obj, null, SessionHelper.getLocale(),
				new DisplayOperationIdentifier());
		pa.load(EnerSysSetupParticipantAttrDelegate.PARTICIPANT_ATTR_ON_OBJ);
		try {
			value = pa.get(EnerSysSetupParticipantAttrDelegate.PARTICIPANT_ATTR_ON_OBJ);
			if (value == null) {
				val = "";
			}
			if (value instanceof Object[]) {
				Object[] valueList = (Object[]) value;
				val = convertRoleArrayToStr(valueList);
			} else if (value instanceof String) {
				val = (String) value;
			}
		} catch (WTException e) {
			LOGGER.error("Error in method getParticipantStringFromObject() : " + e);
			if (LOGGER.isDebugEnabled()) {
				e.printStackTrace();
			}
		}
		return val;
	}

	/**
	 * @deprecated from build V3.8 CM 4.0
	 */
	@Override
	public void setParticipantStringOnObject(WTObject obj, String approvalStr, String notificationStr) {
		String val = generateRoleUserStringCombination(approvalStr, notificationStr);
		// Get roles vs participants from IBA
		try {
			PersistableAdapter pa = new PersistableAdapter(obj, null, SessionHelper.getLocale(),
					new CreateOperationIdentifier());
			pa.load(EnerSysSetupParticipantAttrDelegate.PARTICIPANT_ATTR_ON_OBJ);
			pa.set(EnerSysSetupParticipantAttrDelegate.PARTICIPANT_ATTR_ON_OBJ, val);
			pa.persist();
		} catch (WTException e) {
			// If an Internal value is not found it throws an exception; capture & discard
			// it silently
		}
	}

	/**
	 * Return a WTHashSet of Participants from a Team Instance associated to a given
	 * PBO & Role name.
	 * 
	 * @param obj
	 * @param roleName
	 * @return
	 */
	@Override
	public WTHashSet getParticipantsFromTeam(WTObject obj, String roleName) {
		WTHashSet retSet = new WTHashSet();
		try {
			if (obj instanceof TeamManaged) {
				Team teamObj = TeamHelper.service.getTeam((TeamManaged) obj);

				Role roleToFetch = Role.toRole(roleName);

				Enumeration<?> principalVec = teamObj.getPrincipalTarget(roleToFetch);
				if (principalVec.hasMoreElements()) {
					while (principalVec.hasMoreElements()) {
						Object genObj = principalVec.nextElement();
						if (genObj instanceof WTPrincipalReference) {
							WTPrincipalReference prncRefObj = (WTPrincipalReference) genObj;
							retSet.add(prncRefObj);
						}
					}
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return retSet;
	}

	/**
	 *
	 * @deprecated from build V3.8 CM 4.0
	 *
	 *             Get roles vs participants list from an IBA on the Change
	 *             Object.<br>
	 *             Get the related Template Instance object. Remove Roles which are
	 *             overlapping with roles saved in the IBA's.<br>
	 * 
	 * 
	 * @param obj Change object
	 * @since Build v2.1
	 */
	@Override
	public boolean recomputeTeamTemplate(@NotNull WTObject obj) {
		boolean ret = false;
		try {
			String val = getParticipantStringFromObject(obj);
			String approverRole = "";
			String notificationRole = "";
			String[] approverNotificationStrArr = getRoleUserStringSplitArray(val);
			approverRole = approverNotificationStrArr[0];
			notificationRole = approverNotificationStrArr[1];

			if (obj instanceof TeamManaged) {
				WTPrincipal adminPrincipal = SessionHelper.manager.getAdministrator();
				WTPrincipal oldPrincipal = SessionContext.setEffectivePrincipal(adminPrincipal);
				try {
					Team teamInstanceObject = TeamHelper.service.getTeam((TeamManaged) obj);
					Map mp = teamInstanceObject.getRolePrincipalMap();
					HashMap<Role, Set<WTUser>> newMap = new HashMap<>();
					OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer((WTContained) obj);
					// Replace Approval Roles
					processTeamInstance(teamInstanceObject, mp, newMap, approverRole, orgContainer);
					// Replace Notification Roles
					processTeamInstance(teamInstanceObject, mp, newMap, notificationRole, orgContainer);
					// Delete roles from Team Instance
					deleteTeamInstanceRoles(teamInstanceObject);
					// Use below API as it will create appropriate links
					TeamHelper.service.addRolePrincipalMap(newMap, teamInstanceObject);
					ret = true;
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					SessionContext.setEffectivePrincipal(oldPrincipal);
				}
			}
		} catch (Exception e) {

		}
		return ret;
	}

	/**
	 *
	 * @deprecated from build V3.8 CM 4.0
	 *
	 *             Delete Role map prior to loading values. Make sure minimum roles
	 *             are maintained.
	 * 
	 * @param teamInstanceObject
	 * @throws WTException
	 */
	private void deleteTeamInstanceRoles(Team teamInstanceObject) throws WTException {
		Map mp = teamInstanceObject.getRolePrincipalMap();
		for (Object obj : mp.keySet()) {
			Role roleObj = (Role) obj;
			if (!ROLES_TO_PRESERVE_IN_TEAM.contains(roleObj)) {
				TeamHelper.service.deleteRole(roleObj, teamInstanceObject);
			}
		}
	}

	/**
	 * 
	 *
	 * @deprecated from build V3.8 CM 4.0
	 *
	 *             Check if approverRole parameter is non-empty. Then, split it
	 *             using "," character.<br>
	 *             For each role-user string, split it further by "=". Then check if
	 *             there are indeed users associated to the role (not empty).<br>
	 *             If so, then convert it to a Role object, check if the Role is
	 *             present in the Team Instance and if so, delete it from Team
	 *             Instance.<br>
	 *             After which, call splitAndProcessUsersIntoMap method for further
	 *             processing.
	 * 
	 * @param tm
	 * @param mp
	 * @param newMap
	 * @param approverRole
	 * @param orgContainer
	 * @throws WTException
	 * @since Build v2.1
	 */
	private void processTeamInstance(@NotNull Team tm, @NotNull Map mp, @NotNull HashMap<Role, Set<WTUser>> newMap,
			@NotNull String approverRole, @NotNull OrgContainer orgContainer) throws WTException {
		if (!approverRole.isEmpty()) {
			for (String roles : approverRole.split(",")) {
				String[] roleUsers = roles.split("=");
				if (roleUsers.length == 2 && !roleUsers[1].isEmpty()) {
					// process variable since users are present
					Role roleToAdd = Role.toRole(roleUsers[0]);
					// Delete the role only if there are users selected in the role
					if (mp.containsKey(roleToAdd)) {
						TeamHelper.service.deleteRole(roleToAdd, tm);
					}
					splitAndProcessUsersIntoMap(roleToAdd, roleUsers[1], orgContainer, newMap);
				}
			}
		}
	}

	/**
	 * Split roleUsers by ":" character and get WTUser object for each corresponding
	 * user.<br>
	 * For each user, checks if they are disabled otherwise adds them to a list
	 * which is saved in newMap parameter.
	 * 
	 * @param roleToAdd
	 * @param roleUsers
	 * @param orgContainer
	 * @param newMap
	 * @throws WTException
	 * @since Build v2.1
	 */
	private void splitAndProcessUsersIntoMap(Role roleToAdd, String roleUsers, @NotNull OrgContainer orgContainer,
			@NotNull HashMap<Role, Set<WTUser>> newMap) throws WTException {
		HashSet<WTUser> userList = new HashSet<>();
		for (String userName : roleUsers.split(":")) {
			WTUser oldUser = OrganizationServicesHelper.manager.getUser(userName, orgContainer.getContextProvider());
			WTPrincipalReference refPrincipal = WTPrincipalReference.newWTPrincipalReference(oldUser);
			if (!refPrincipal.isDisabled()) {
				userList.add(oldUser);
			}
		}
		if (!userList.isEmpty()) {
			// Added in Build v2.7 - ENERSYS-474
			if (newMap.containsKey(roleToAdd)) {
				userList.addAll(newMap.get(roleToAdd));
			}
			newMap.put(roleToAdd, userList);
		}
	}

	/**
	 * Converts String to LinkedHashMap<String, WTContainer>.
	 * 
	 * @since Build v2.1-HF1
	 * @param eParticipants
	 * @throws WTException
	 */
	@Override
	public LinkedHashMap<String, WTContainer> convertRoleContainerStringToRoleContainerObject(String eParticipantsStr)
			throws WTException {
		LinkedHashMap<String, WTContainer> eParticipants = new LinkedHashMap<>();
		ReferenceFactory rf = new ReferenceFactory();
		if (eParticipantsStr != null && !eParticipantsStr.isEmpty()) {
			String[] DNs = eParticipantsStr.split("=DN=");
			for (String DN : DNs) {
				if (!DN.isEmpty()) {
					String[] lastMap = DN.split("=CONT=");
					eParticipants.put(lastMap[0], (WTContainer) rf.getReference(lastMap[1]).getObject());
				}
			}
		}
		return eParticipants;
	}

	/**
	 * Converts LinkedHashMap<String, WTContainer> to String.
	 * 
	 * @since Build v2.1-HF1
	 * @param eParticipants
	 * @throws WTException
	 */
	@Override
	public String convertRoleContainerInfoToRoleContainerString(LinkedHashMap<String, WTContainer> eParticipants)
			throws WTException {
		StringBuilder sb = new StringBuilder();
		ReferenceFactory rf = new ReferenceFactory();
		ObjectReference objRef = null;
		if (eParticipants != null && !eParticipants.isEmpty()) {
			for (Entry<String, WTContainer> io : eParticipants.entrySet()) {
				sb.append(io.getKey());
				objRef = ObjectReference.newObjectReference(io.getValue());
				sb.append("=CONT=" + rf.getReferenceString(objRef));
				sb.append("=DN=");
			}
		}
		return sb.toString();
	}

	/**
	 * {DESIGN AUTHORITY:REQUIRED:1:false={DESIGN AUTHORITY:ROLE:CM3.0 Test
	 * Product=wt.pdmlink.PDMLinkProduct:3252036},
	 * MANUFACTURING:REQUIRED:1:false={MANUFACTURING:ROLE:CM3.0 Test
	 * Product=wt.pdmlink.PDMLinkProduct:3252036}}
	 * 
	 * @param rolesHash
	 * @throws WTException
	 */
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> convertRolesHashStringToRolesHashObject(
			String rolesHash) throws WTException {
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> rolesHashObj = new LinkedHashMap<>();
		ReferenceFactory rf = new ReferenceFactory();
		String[] NEWDNs = rolesHash.split("=NEWDN=");
		for (String NEWDN : NEWDNs) {
			String[] firstMap = NEWDN.split("=NEW=");
			LinkedHashMap<String, WTContainer> lastMapObj = new LinkedHashMap<>();
			if (firstMap.length > 0) {
				String[] DNs = firstMap[1].split("=DN=");
				for (String DN : DNs) {
					if (!DN.isEmpty()) {
						String[] lastMap = DN.split("=CONT=");
						lastMapObj.put(lastMap[0], (WTContainer) rf.getReference(lastMap[1]).getObject());
					}
				}
			}
			rolesHashObj.put(firstMap[0], lastMapObj);
		}
		return rolesHashObj;
	}

	/**
	 * 
	 * @param rolesHash
	 * @return
	 * @throws WTException
	 */
	@Override
	public String convertRolesHashObjectToRolesHashString(
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> rolesHash) throws WTException {
		StringBuilder sb = new StringBuilder();
		ReferenceFactory rf = new ReferenceFactory();
		ObjectReference objRef = null;
		if (rolesHash != null && !rolesHash.isEmpty()) {
			for (Entry<String, LinkedHashMap<String, WTContainer>> s : rolesHash.entrySet()) {
				sb.append(s.getKey() + "=NEW=");
				for (Entry<String, WTContainer> io : s.getValue().entrySet()) {
					sb.append(io.getKey());
					objRef = ObjectReference.newObjectReference(io.getValue());
					sb.append("=CONT=" + rf.getReferenceString(objRef));
					sb.append("=DN=");
				}
				sb.append("=NEWDN=");
			}
		}
		return sb.toString();
	}

	@Override
	public String getParticipantRoleMapWizard(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap,
			TeamTemplate teamTemplateObj) {
		String ret = null;
		StringBuilder sb = new StringBuilder();
		try {
			if (roleMap != null) {
				for (Entry<String, LinkedHashMap<String, WTContainer>> entryObj : roleMap.entrySet()) {
					if (entryObj.getKey() != null) {
						String roleName = entryObj.getKey().split(
								EnerSysApprovalMatrixDefinition.COLON_ROLE_MAP_DELIM)[EnerSysApprovalMatrixDefinition.PARTICIPANT_ROLE_INDEX];

						boolean isStrictAutoPop = EnerSysApprovalMatrixUtility.getInstance()
								.isStrictAutoPopulation(roleMap, Role.toRole(roleName));
						if (isStrictAutoPop) {
							LinkedHashMap<String, WTContainer> val = entryObj.getValue();
							for (Entry<String, WTContainer> eParticipantEntryObj : val.entrySet()) {
								// Only process containers with STRICT-AUTO-POP enabled
								if (Boolean.parseBoolean(eParticipantEntryObj.getKey().split(
										EnerSysApprovalMatrixDefinition.COLON_ROLE_MAP_DELIM)[EnerSysApprovalMatrixDefinition.STRICT_AUTO_POP_INDEX__ROLETYPE])) {
									WTContainer cont = eParticipantEntryObj.getValue();
									Enumeration containerUsers = null;
									ContainerTeam cteam = ContainerTeamHelper.service
											.getContainerTeam((ContainerTeamManaged) cont);
									WTGroup rolegroupDoc = ContainerTeamHelper.service.findContainerTeamGroup(cteam,
											ContainerTeamHelper.ROLE_GROUPS, roleName);
									if (rolegroupDoc != null) {
										// Set false if you want to get the Groups
										// Set true if you want to get the actual participants (WTUsers) that are inside
										// the group
										containerUsers = OrganizationServicesHelper.manager.members(rolegroupDoc, true);
									}
									if (containerUsers != null && containerUsers.hasMoreElements()) {
										sb.append(roleName + "=");
										while (containerUsers.hasMoreElements()) {
											Object nextElement = containerUsers.nextElement();
											if (nextElement instanceof WTUser) {
												WTUser user = (WTUser) nextElement;
												sb.append(user.getName() + ":");
											}
										}
										sb.append(",");
									}
								}
							}
						} else if (teamTemplateObj != null) {
							Enumeration principalVec = teamTemplateObj.getPrincipalTarget(Role.toRole(roleName));
							if (principalVec.hasMoreElements()) {
								sb.append(roleName + "=");
								while (principalVec.hasMoreElements()) {
									Object genObj = principalVec.nextElement();
									if (genObj instanceof WTPrincipalReference) {
										WTPrincipalReference prncRefObj = (WTPrincipalReference) genObj;
										sb.append(prncRefObj.getName() + ":");
									}
								}
								sb.append(",");
							}
						} else {
							LinkedHashMap<String, WTContainer> val = entryObj.getValue();
							for (Entry<String, WTContainer> eParticipantEntryObj : val.entrySet()) {
								int userCount = 0;
								WTContainer cont = eParticipantEntryObj.getValue();
								Enumeration containerUsers = null;
								ContainerTeam cteam = ContainerTeamHelper.service
										.getContainerTeam((ContainerTeamManaged) cont);
								WTGroup rolegroupDoc = ContainerTeamHelper.service.findContainerTeamGroup(cteam,
										ContainerTeamHelper.ROLE_GROUPS, roleName);
								if (rolegroupDoc != null) {
									containerUsers = OrganizationServicesHelper.manager.members(rolegroupDoc, true);
									Enumeration tempEnum = OrganizationServicesHelper.manager.members(rolegroupDoc,
											true);
									if (tempEnum != null && tempEnum.hasMoreElements()) {
										while (tempEnum.hasMoreElements()) {
											Object nextElement = tempEnum.nextElement();
											if (nextElement instanceof WTUser) {
												userCount++;
											}
										}
									}
									if (userCount == 1 && entryObj.getKey().contains("REQUIRED")) {
										if (containerUsers != null && containerUsers.hasMoreElements()) {
											sb.append(roleName + "=");
											while (containerUsers.hasMoreElements()) {
												Object nextElement = containerUsers.nextElement();
												if (nextElement instanceof WTUser) {
													WTUser user = (WTUser) nextElement;
													sb.append(user.getName() + ":");
												}
											}
											sb.append(",");
										}
									}
								}

							}
						}
					}
				}
				if (sb.length() > 0) {
					ret = sb.toString();
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 *
	 * @deprecated from build V3.8 CM 4.0
	 *
	 * @param pbo
	 * 
	 * @return
	 * @since Build v2.1
	 */
	@Override
	public String getApproverRoleUserStr(WTObject pbo) {
		try {
			String setupParticipantStr = getParticipantStringFromObject(pbo);
			return getRoleUserStringSplitArray(setupParticipantStr)[0];
		} catch (Exception e) {
		}
		return "";
	}

	/**
	 * combines 2 string using Delimiter ~~
	 * 
	 * @deprecated from build V3.8 CM 4.0
	 *
	 * @param approverStr
	 * @param notificationStr
	 * @return
	 */
	private String generateRoleUserStringCombination(String approverStr, String notificationStr) {
		approverStr = (approverStr == null) ? "" : approverStr;
		notificationStr = (notificationStr == null) ? "" : notificationStr;
		return approverStr + "~~" + notificationStr;
	}

	/**
	 * Index 0 --> Approver Role String<br>
	 * Index 1 --> Notification Role String<br>
	 * 
	 * @deprecated from build V3.8 CM 4.0
	 *
	 * @param setupParticipantStr
	 * @return
	 */
	private String[] getRoleUserStringSplitArray(String setupParticipantStr) {
		// Index 0 --> Approver Role String
		// Index 1 --> Notification Role String

		String[] strArray = new String[2];
		strArray[0] = "";
		strArray[1] = "";
		if (setupParticipantStr != null) {
			String[] valSplt = setupParticipantStr.split("~~");
			if (valSplt.length == 2) {
				strArray[0] = valSplt[0];
				strArray[1] = valSplt[1];
			} else if (setupParticipantStr.endsWith("~~")) {
				strArray[0] = valSplt[0];
				strArray[1] = "";
			} else {
				strArray[0] = "";
				strArray[1] = valSplt[0];
			}
		}
		return strArray;
	}

	/**
	 * 
	 * @param pbo
	 *
	 * @deprecated from build V3.8 CM 4.0
	 *
	 * @return
	 * @since Build v2.1
	 */
	@Override
	public String getNotificationRoleUserStr(WTObject pbo) {
		try {
			String setupParticipantStr = getParticipantStringFromObject(pbo);
			return getRoleUserStringSplitArray(setupParticipantStr)[1];
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getNotificationRoleMap(NmCommandBean commandBean) {
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> retMap = null;
		try {
			String createTypeVal = getObjectTypeSelected(commandBean);
			TypeIdentifier creatObjTI = TypeIdentifierHelper.getTypeIdentifier("WCTYPE|" + createTypeVal);
			Set<NmOid> affectedItemSet = getAffectedObjectsForApprovals(creatObjTI, commandBean);
			retMap = EnerSysApprovalMatrixUtility.getInstance().getNotificationRoleMapWizard(affectedItemSet,
					creatObjTI, commandBean);
		} catch (WTException e) {
			e.printStackTrace();
		}

		return retMap;
	}

	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getNotificationRoleMap(WTObject obj) {
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> retMap = null;
		try {
			TypeIdentifier creatObjTI = TypeIdentifierHelper.getType(obj);
			WTHashSet resObjects = CM2Helper.service.getAllAffectedObjects(obj);
			Set<NmOid> affectedItemSet = new HashSet();
			Iterator resObjItr = resObjects.iterator();
			while (resObjItr.hasNext()) {
				Persistable per = ((WTReference) resObjItr.next()).getObject();
				affectedItemSet.add(new NmOid(per));
			}
			retMap = EnerSysApprovalMatrixUtility.getInstance().getNotificationRoleMapWizard(affectedItemSet,
					creatObjTI, null);
		} catch (WTException e) {
			e.printStackTrace();
		}

		return retMap;
	}

	@Override
	public String getLifeCycleStateFromFirstObj(NmCommandBean commandBean) throws WTException {
		// TODO: Verify for Promotion Request/Deviation/Problem Report/CN/CT
		String createTypeVal = getObjectTypeSelected(commandBean);
		TypeIdentifier creatObjTI = TypeIdentifierHelper.getTypeIdentifier("WCTYPE|" + createTypeVal);

		Set<NmOid> affectedItemSet = getAffectedObjectsForApprovals(creatObjTI, commandBean);

		for (NmOid obj : affectedItemSet) {
			Persistable per = (Persistable) (obj).getRefObject();
			return EnerSysHelper.service.getObjectState((WTObject) per);
		}
		return null;
	}

	/**
	 * TODO: Add more Types, CN, CT, DEVIATION
	 * 
	 * @param ti
	 * @return
	 * @throws WTException
	 */
	private Set<NmOid> getAffectedObjectsForApprovals(TypeIdentifier ti, NmCommandBean commandBean) throws WTException {
		final String AFFECTED_DATA_TABLE_ID = "changeRequest_affectedData_table";
		final String PROMOTION_REQ_TABLE_ID = "promotionRequest.promotionObjects";
		final String ACTIVITY_DATA_TABLE_ID = "changeTask_affectedItems_table";
		final String CN_DATA_TABLE_ID = "changeNotice.wizardImplementationPlan.table";
		final String DEVIATION_DATA_TABLE_ID = "change_affectedData_table";
		final String DOCUMENT_APPROVAL_DATA_TABLE_ID= "changeReview_affectedData_table";

		String changeTypeStr = CM2Helper.service.getChangeTypeString(ti);
		switch (changeTypeStr) {
		case EnerSysService.ACTIVITY_CHANGEREQUEST:
			return processAndFetchAffectedObjectsTable(AFFECTED_DATA_TABLE_ID, commandBean);
		case EnerSysService.ACTIVITY_PROMOTIONREQUEST:
			return processAndFetchAffectedObjectsTable(PROMOTION_REQ_TABLE_ID, commandBean);
		case EnerSysService.ACTIVITY_CHANGENOTICE:
			return processAndFetchChangeNoticeAffectedObjects(CN_DATA_TABLE_ID, commandBean);
		case EnerSysService.ACTIVITY_CHANGEACTIVITY:
			return processAndFetchAffectedObjectsTable(ACTIVITY_DATA_TABLE_ID, commandBean);
		case EnerSysService.ACTIVITY_DEVIATION:
			return processAndFetchAffectedObjectsTable(DEVIATION_DATA_TABLE_ID, commandBean);
		case EnerSysService.ACTIVITY_DOCUMENTAPPROVAL:
			return processAndFetchAffectedObjectsTable(DOCUMENT_APPROVAL_DATA_TABLE_ID, commandBean);
		default:
			return Collections.emptySet();
		}
	}

	/**
	 * Build v2.9 - 661 Fetches resulting objects present in Change Task
	 */
	@Override
	public Set<NmOid> getChangeTaskResultingObjects(NmCommandBean commandBean) {
		final String CT_RESULTING_ITEMS_TABLE_ID = "changeTask_resultingItems_table";
		try {
			String createTypeVal = getObjectTypeSelected(commandBean);
			TypeIdentifier creatObjTI = TypeIdentifierHelper.getTypeIdentifier("WCTYPE|" + createTypeVal);
			String changeTypeStr = CM2Helper.service.getChangeTypeString(creatObjTI);
			if (changeTypeStr.equals(EnerSysService.ACTIVITY_CHANGEACTIVITY)) {
				return processAndFetchAffectedObjectsTable(CT_RESULTING_ITEMS_TABLE_ID, commandBean);
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return Collections.emptySet();
	}

	/**
	 * Build v2.9 - 661 Fetches Change Notice's resulting objects
	 */
	public static Set<Persistable> processAndFetchChangeNoticeResultingObjects(NmCommandBean commandBean) {
		try {
			Set<String> resultingObjStr = new HashSet<String>();
			for (Entry<String, Object> obj : commandBean.getParameterMap().entrySet()) {
				if (obj.getKey().startsWith(GLOBAL_CT_RESULTING_OBJ_STR) && obj.getValue() instanceof String[]
						&& ((String[]) obj.getValue()).length > 0) {
					String[] val = (String[]) obj.getValue();
					if (!val[0].isEmpty())
						resultingObjStr
								.addAll(Arrays.asList(val[0].replace("[", "").replace("]", "").split("\\s*,\\s*")));
				}
			}
			ReferenceFactory rf = new ReferenceFactory();
			HashSet<Persistable> resultingObjSet = new HashSet<>();
			Iterator<String> itr = resultingObjStr.iterator();

			while (itr.hasNext()) {
				String o = itr.next();
				resultingObjSet.add(rf.getReference(o).getObject());
			}
			return resultingObjSet;
		} catch (WTException e) {
			e.printStackTrace();
		}
		return Collections.emptySet();
	}

	private Set<NmOid> processAndFetchAffectedObjectsTable(final String TABLE_ID, NmCommandBean commandBean) {
		List<NmOid> affectedItemList = commandBean.getAddedItemsByName(TABLE_ID);
		affectedItemList.addAll(commandBean.getInitialItemsByName(TABLE_ID));
		affectedItemList.removeAll(commandBean.getRemovedItemsByName(TABLE_ID));
		return new HashSet<>(affectedItemList);
	}

	private Set<NmOid> processAndFetchChangeNoticeAffectedObjects(final String TABLE_ID, NmCommandBean commandBean) {
		try {
			// CreateAndEditWizBean dd;
			// NmContextBean d;
			Set<String> affectedObjStr = new HashSet<String>();
			for (Entry<String, Object> obj : commandBean.getParameterMap().entrySet()) {
				if (obj.getKey().startsWith(GLOBAL_CT_AFFECTED_OBJ_STR) && obj.getValue() instanceof String[]
						&& ((String[]) obj.getValue()).length > 0) {
					String[] val = (String[]) obj.getValue();
					affectedObjStr.addAll(Arrays.asList(val[0].replace("[", "").replace("]", "").split("\\s*,\\s*")));
				}
			}
			// List<NmOid> changeActivities = commandBean.getAddedItemsByName(TABLE_ID);
			// changeActivities.addAll(commandBean.getInitialItemsByName(TABLE_ID));
			// changeActivities.removeAll(commandBean.getRemovedItemsByName(TABLE_ID));

			// EffectivityAwareIframeFormProcessorController
			// NmWorkItemCommands.
			// System.out.println(commandBean.getParameterMap())
			// commandBean.getSharedContextOid()
			// commandBean.getParameterMap().get("addRows_changeNotice.wizardImplementationPlan.table")
			// CreateChangeTaskTag -->
			// paramNmCommandBean.getTextParameter("change_selectedItems")
			// --> paramNmCommandBean.getTextParameter("initialImpactItems")
			// DefaultChangeTaskTypeTag
			// InStockAffectedItemsTableBuilder
			// UICollectionContext
			// RequestHelper.
			// SelectedItemsTag
			// ChangeWizardInitializeTag
			ReferenceFactory rf = new ReferenceFactory();

			HashSet<NmOid> affObjNmOidSet = new HashSet<>();
			Iterator<String> itr = affectedObjStr.iterator();
			while (itr.hasNext()) {
				String o = itr.next();
				affObjNmOidSet.add(new NmOid(rf.getReference(o).getObject()));
			}

			// commandBean.getAddedItemsByName("changeTask_affectedItems_table")
			// commandBean.getInitialItemsByName("changeTask_affectedItems_table")
			// ChangeManagementFormProcessorHelper.getAddedOids(
			// ChangeManagementFormProcessorHelper.getTableIdFromParameters("changeTask_affectedItems_table",
			// commandBean)
			return affObjNmOidSet;
		} catch (WTException e) {
			e.printStackTrace();
		}
		return Collections.emptySet();
	}

	@Override
	public Set<NmOid> getChangeAffectedObjects(NmCommandBean commandBean) throws WTException {
		String createTypeVal = getObjectTypeSelected(commandBean);
		TypeIdentifier creatObjTI = TypeIdentifierHelper.getTypeIdentifier("WCTYPE|" + createTypeVal);
		return getAffectedObjectsForApprovals(creatObjTI, commandBean);
	}

	private void pushAffectedObjectTableIntoBean(NmCommandBean commandBean, Set<NmOid> list) {
		Object obj = commandBean.getMap().get(MAP_PARAMETER_KEY_VAL);
		HashMap<String, Set<NmOid>> oidMapObj;
		// Retrieve object
		if (obj == null) {
			oidMapObj = new HashMap<>();
			commandBean.getMap().put(MAP_PARAMETER_KEY_VAL, oidMapObj);
		} else {
			oidMapObj = (HashMap<String, Set<NmOid>>) obj;
		}
		try {
			// Add List
			oidMapObj.put(commandBean.getActionOid().toString(), list);
		} catch (WTException e) {
			e.printStackTrace();
		}
	}

	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMap(
			NmCommandBean commandBean) {
		// commandBean.getTextParameter("name_here");
		// System.out.println(commandBean.getParameterMap())
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> retMap = null;
		// Get State Information based on affected objects from create page
		// Get the corresponding roles from Change Matrix
		// Intersection of Matrix roles with Team Template Roles
		// Populate participants as per team template
		try {
			// initialStatePart =
			// ext.enersys.cm2.CM2Helper.service.getLifeCycleStateFromFirstObj(primaryBusinessObject);
			// java.util.LinkedHashMap<String, java.util.LinkedHashMap<String,
			// wt.inf.container.WTContainer>>
			// map=ext.enersys.cm2.xml.EnerSysApprovalMatrixUtility.getInstance().getChangeApprovalRoleMap((WTObject)primaryBusinessObject);
			// SEQUENCE_ORDER_STRING =
			// ext.enersys.cm2.xml.EnerSysApprovalMatrixUtility.getInstance().getSequencesInOrder_String(map);

			// TODO: Partition the data based on ENERSYS CR-CN-CT/FIRMWARE
			// CR-CN-CT/Deviation/Promotion Request
			// TODO: Get selected type from combobox --> CreateType
			// commandBean.getTextParameter("requiredRoleUserMap");
			// commandBean.getMap().put(AFFECTED_DATA_TABLE_ID, retMap)
			// commandBean.getMap().set(AFFECTED_DATA_TABLE_ID, retMap)
			// below logic is implemented to take care of initially selected item (i.e,
			// right-click on folder view), since its not returned by getAddedItemsByName

			boolean orderChngCriticalPartPrefValue = (boolean) PreferenceHelper.service.getValue(
					commandBean.getContainerRef(), CRITICAL_PART_SELECTOR_ROLE_PREFERENCE,
					PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());
			String createTypeVal = getObjectTypeSelected(commandBean);
			TypeIdentifier creatObjTI = TypeIdentifierHelper.getTypeIdentifier("WCTYPE|" + createTypeVal);

			Set<NmOid> affectedItemSet = getAffectedObjectsForApprovals(creatObjTI, commandBean);

			retMap = EnerSysApprovalMatrixUtility.getInstance().getChangeApprovalRoleMapWizard(affectedItemSet,
					creatObjTI, commandBean);
			if (orderChngCriticalPartPrefValue
					&& (createTypeVal != null && !createTypeVal.startsWith("wt.change2.WTChangeActivity2"))) {
				if (createTypeVal != null && createTypeVal.startsWith("wt.change2.WTChangeOrder2")) {
					affectedItemSet = processAndFetchChangeNoticeResultingObjects(
							"changeNotice.wizardImplementationPlan.table", commandBean);
				}
				retMap = EnerSysApprovalMatrixUtility.getInstance().additionalRolesAdded(retMap, affectedItemSet);
			}

			// Check if the Bean is for Change Activity, then push the data
			/*
			 * String changeTypeStr = CM2Helper.service.getChangeTypeString(creatObjTI); if
			 * (EnerSysService.ACTIVITY_CHANGEACTIVITY.equalsIgnoreCase(changeTypeStr)) {
			 * pushAffectedObjectTableIntoBean(commandBean, affectedItemSet); }
			 */

			/*
			 * System.out.println("retMap:" + retMap); Vector<String> roleVector =
			 * teamTemplateObj.getRoles(); System.out.println("Template Name : " +
			 * teamTemplateObj.getName()); Map rolePrincipalMap =
			 * teamTemplateObj.getRolePrincipalMap();
			 *
			 */
		} catch (WTException e) {
			e.printStackTrace();
		}
		return retMap;
	}

	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMap(WTObject obj) {
		// commandBean.getTextParameter("name_here");
		// System.out.println(commandBean.getParameterMap())
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> retMap = null;
		// Get State Information based on affected objects from create page
		// Get the corresponding roles from Change Matrix
		// Intersection of Matrix roles with Team Template Roles
		// Populate participants as per team template
		try {
			// initialStatePart =
			// ext.enersys.cm2.CM2Helper.service.getLifeCycleStateFromFirstObj(primaryBusinessObject);
			// java.util.LinkedHashMap<String, java.util.LinkedHashMap<String,
			// wt.inf.container.WTContainer>>
			// map=ext.enersys.cm2.xml.EnerSysApprovalMatrixUtility.getInstance().getChangeApprovalRoleMap((WTObject)primaryBusinessObject);
			// SEQUENCE_ORDER_STRING =
			// ext.enersys.cm2.xml.EnerSysApprovalMatrixUtility.getInstance().getSequencesInOrder_String(map);

			// TODO: Partition the data based on ENERSYS CR-CN-CT/FIRMWARE
			// CR-CN-CT/Deviation/Promotion Request
			// TODO: Get selected type from combobox --> CreateType
			// commandBean.getTextParameter("requiredRoleUserMap");
			// commandBean.getMap().put(AFFECTED_DATA_TABLE_ID, retMap)
			// commandBean.getMap().set(AFFECTED_DATA_TABLE_ID, retMap)
			// below logic is implemented to take care of initially selected item (i.e,
			// right-click on folder view), since its not returned by getAddedItemsByName
			boolean orderChngCriticalPartPrefValue = (boolean) PreferenceHelper.service.getValue(
					((WTContained) obj).getContainerReference(), CRITICAL_PART_SELECTOR_ROLE_PREFERENCE,
					PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());
			// String createTypeVal = getObjectTypeSelected(commandBean);
			// TypeIdentifier creatObjTI = TypeIdentifierHelper.getTypeIdentifier("WCTYPE|"
			// + createTypeVal);
			TypeIdentifier creatObjTI = TypeIdentifierHelper.getType(obj);
			Set<NmOid> affectedItemSet = new HashSet();
			WTHashSet resObjects = CM2Helper.service.getAllResultingObjects(obj);
			Iterator resObjItr = resObjects.iterator();
			while (resObjItr.hasNext()) {
				Persistable per = ((WTReference) resObjItr.next()).getObject();
				affectedItemSet.add(new NmOid(per));
			}
			retMap = EnerSysApprovalMatrixUtility.getInstance().getChangeApprovalRoleMapCN(affectedItemSet, creatObjTI,
					obj);
			if (orderChngCriticalPartPrefValue) {
				retMap = EnerSysApprovalMatrixUtility.getInstance().additionalRolesAdded(retMap, affectedItemSet);
			}

			// Check if the Bean is for Change Activity, then push the data
			/*
			 * String changeTypeStr = CM2Helper.service.getChangeTypeString(creatObjTI); if
			 * (EnerSysService.ACTIVITY_CHANGEACTIVITY.equalsIgnoreCase(changeTypeStr)) {
			 * pushAffectedObjectTableIntoBean(commandBean, affectedItemSet); }
			 */

			/*
			 * System.out.println("retMap:" + retMap); Vector<String> roleVector =
			 * teamTemplateObj.getRoles(); System.out.println("Template Name : " +
			 * teamTemplateObj.getName()); Map rolePrincipalMap =
			 * teamTemplateObj.getRolePrincipalMap();
			 *
			 */
		} catch (WTException e) {
			e.printStackTrace();
		}
		return retMap;
	}

	@Override
	public TeamTemplate getSelectedTeamTemplateFromWizard(NmCommandBean commandBean) {
		TeamTemplate teamTemplateObj = null;
		String teamTemplateOidVal = getSelectedTeamTemplateOidFromWizard(commandBean);
		if (teamTemplateOidVal != null && !teamTemplateOidVal.isEmpty()) {
			try {
				ReferenceFactory rf = new ReferenceFactory();
				// "WCTYPE|wt.team.TeamTemplate~~WCP|3253634|3". Format must be:
				// "classname:idValue"
				WTReference objRef = rf.getReference(teamTemplateOidVal);
				teamTemplateObj = (TeamTemplate) objRef.getObject();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return teamTemplateObj;
	}

	@Override
	public String getTeamTemplateHTMLId(NmCommandBean commandBean) {
		if (commandBean.getComboBox() != null) {
			Iterator itr = commandBean.getComboBox().keySet().iterator();
			while (itr.hasNext()) {
				String keyVal = (String) itr.next();
				if (keyVal != null && keyVal.contains("teamTemplateId")) {
					return keyVal;
				}
			}
		}
		return "";
	}

	@Override
	public String getSelectedTeamTemplateOidFromWizard(NmCommandBean commandBean) {
		String selectedValue = "";
		if (commandBean.getComboBox() != null) {
			Iterator itr = commandBean.getComboBox().keySet().iterator();
			while (itr.hasNext()) {
				String keyVal = (String) itr.next();
				if (keyVal != null && keyVal.contains("teamTemplateId")) {
					ArrayList<?> aList = (ArrayList<?>) commandBean.getComboBox().get(keyVal);
					if (aList != null && !aList.isEmpty() && StringUtils.isNotBlank((String) aList.get(0))) {
						selectedValue = ((String) aList.get(0)).trim();
						String teamTemplateClassName = selectedValue.split("~~")[0].split("\\|")[1];
						String teamTemplateIdVal = selectedValue.split("~~")[1].split("\\|")[1];
						selectedValue = teamTemplateClassName + ":" + teamTemplateIdVal;
						break;
					}
				}
			}
		}
		return selectedValue;
	}

	@Override
	public void checkAndWriteDebug(String prefix, String middle, Object... args) {
		if (LOGGER.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append(prefix);
			sb.append(CLASSNAME);
			sb.append(middle);
			if (args != null) {
				for (Object o : args) {
					if (o instanceof String) {
						sb.append(o);
					} else if (o instanceof Persistable) {
						sb.append(EnerSysLogUtils.format((Persistable) o));
					} else {
						sb.append(o);
					}
				}
			}
			LOGGER.debug(sb);
		}
	}

	/**
	 * Converts setup participant role String to String[].
	 * 
	 * @since Build v2.4
	 * @param selectedRoleParticipantValue
	 * @return String[]
	 */
	@Override
	public String[] convertRoleStrToStrArray(String selectedRoleParticipantValue) {
		String approverRolesString = "";
		String notificationRolesString = "";
		if (selectedRoleParticipantValue.startsWith("~~")) {
			notificationRolesString = selectedRoleParticipantValue.split("~~")[1];
		} else if (selectedRoleParticipantValue.endsWith("~~")) {
			approverRolesString = selectedRoleParticipantValue.split("~~")[0];
		} else {
			String[] approverAndNotificationRolesString = selectedRoleParticipantValue.split("~~");
			approverRolesString = approverAndNotificationRolesString[0];
			notificationRolesString = approverAndNotificationRolesString[1];
		}
		List<String> setupParticipantsList = new ArrayList<>();
		String setupParticipantsString = "";
		if (approverRolesString.length() >= 1) {
			String[] approverRolesList = approverRolesString.split(",");
			for (int i = 0; i < approverRolesList.length; i++) {
				String[] roleAndUsers = approverRolesList[i].split("=");
				String role = roleAndUsers[0];
				if (roleAndUsers.length == 1) {
					setupParticipantsString = APPROVER_ROLE + "~~" + role + "=";
					setupParticipantsList.add(setupParticipantsString);
				} else if (roleAndUsers.length > 1) {
					String users = roleAndUsers[1];
					if (users.length() > 0 && users.contains(":")) {
						String[] usersList = roleAndUsers[1].split(":");
						for (int j = 0; j < usersList.length; j++) {
							setupParticipantsString = APPROVER_ROLE + "~~" + role + "=" + usersList[j];
							setupParticipantsList.add(setupParticipantsString);
						}
					} else {
						setupParticipantsString = APPROVER_ROLE + "~~" + role + "=" + roleAndUsers[1];
						setupParticipantsList.add(setupParticipantsString);
					}
				}
			}
		}
		if (notificationRolesString.length() >= 1) {
			String[] notificationRolesList = notificationRolesString.split(",");
			for (int i = 0; i < notificationRolesList.length; i++) {
				String[] roleAndUsers = notificationRolesList[i].split("=");
				String role = roleAndUsers[0];
				if (roleAndUsers.length == 1) {
					setupParticipantsString = NOTIFICATION_ROLE + "~~" + role + "=";
					setupParticipantsList.add(setupParticipantsString);
				} else if (roleAndUsers.length > 1) {
					String users = roleAndUsers[1];
					if (users.length() > 0 && users.contains(":")) {
						String[] usersList = roleAndUsers[1].split(":");
						for (int j = 0; j < usersList.length; j++) {
							setupParticipantsString = NOTIFICATION_ROLE + "~~" + role + "=" + usersList[j];
							setupParticipantsList.add(setupParticipantsString);
						}
					} else {
						setupParticipantsString = NOTIFICATION_ROLE + "~~" + role + "=" + roleAndUsers[1];
						setupParticipantsList.add(setupParticipantsString);
					}
				}
			}
		}
		return Arrays.copyOf(setupParticipantsList.toArray(), setupParticipantsList.toArray().length, String[].class);
	}

	/**
	 * Converts setup participant role object array to String.
	 * 
	 * Converts the ouptut format to ROLE = user1:user2 ,
	 * 
	 * @since Build v2.4
	 * @param roleArray
	 * @return String
	 */
	@Override
	public String convertRoleArrayToStr(Object[] roleArray) {
		List<String> setupParticipantsList = Arrays.asList(Arrays.copyOf(roleArray, roleArray.length, String[].class));
		StringBuilder setupPartcipantString = new StringBuilder();
		Map<String, String> approverRolesList = new HashMap<>();
		Map<String, String> notificationsRolesList = new HashMap<>();
		for (String obj : setupParticipantsList) {
			if (obj.startsWith("APPROVER_ROLE~~")) {
				String[] roleAndUser = obj.split("~~")[1].split("=");
				String role = roleAndUser[0];
				if (roleAndUser.length > 1) {
					boolean roleAlreadyAddedToList = false;
					Set<String> addedRolesList = approverRolesList.keySet();
					for (String addedRole : addedRolesList) {
						if (addedRole.equals(role)) {
							roleAlreadyAddedToList = true;
							break;
						}
					}
					String user = roleAndUser[1];
					if (roleAlreadyAddedToList) {
						String userValue = approverRolesList.get(role);
						String newUser = userValue + ":" + user;
						approverRolesList.put(role, newUser);
					} else {
						approverRolesList.put(role, user);
					}
				} else if (roleAndUser.length == 1) {
					approverRolesList.put(role, "");
				}
			} else if (obj.startsWith("NOTIFICATION_ROLE~~")) {
				String[] roleAndUser = obj.split("~~")[1].split("=");
				String role = roleAndUser[0];
				if (roleAndUser.length > 1) {
					boolean roleAlreadyAddedToList = false;
					Set<String> addedRolesList = notificationsRolesList.keySet();
					for (String addedRole : addedRolesList) {
						if (addedRole.equals(role)) {
							roleAlreadyAddedToList = true;
							break;
						}
					}
					String user = roleAndUser[1];
					if (roleAlreadyAddedToList) {
						String userValue = notificationsRolesList.get(role);
						String newUser = userValue + ":" + user;
						notificationsRolesList.put(role, newUser);
					} else {
						notificationsRolesList.put(role, user);
					}
				} else if (roleAndUser.length == 1) {
					notificationsRolesList.put(role, "");
				}
			}
		}
		if (approverRolesList.size() >= 1) {
			for (Map.Entry<String, String> entry : approverRolesList.entrySet()) {
				setupPartcipantString.append(entry.getKey() + "=" + entry.getValue() + ",");
			}
			setupPartcipantString.append("~~");
		} else if (approverRolesList.size() == 0) {
			setupPartcipantString.append("~~");

		}
		if (notificationsRolesList.size() >= 1) {
			for (Map.Entry<String, String> entry : notificationsRolesList.entrySet()) {
				setupPartcipantString.append(entry.getKey() + "=" + entry.getValue() + ",");
			}
		}
		return setupPartcipantString.toString();
	}

	/**
	 * 
	 * @param contextName
	 * @return
	 * @since Build v2.5
	 */
	private boolean isESGContext(@NotNull String contextName) {
		try {
			WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);
			String preferenceValue = (String) PreferenceHelper.service.getValue(ESG_CONTEXT_VISIBILITY_PREF_INTR_NAME,
					PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);

			String[] splitValues = preferenceValue.split(PREFERENCE_SEPARATOR);
			List<String> productList = Arrays.asList(splitValues);
			return productList.contains(contextName.trim());
		} catch (WTException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * As of Build v2.7, Z1 & Z2 are replaced by ZJ & ZK respectively.<br>
	 * New matrix uploaded on JIRA-609<br>
	 * 
	 * @throws WTException
	 * @since Build v2.5
	 */
	public void esgErpStatusAutoPromote(Promotable prom, State startState, State targetState) {
		try {
			if (prom instanceof Persistable && startState != null && targetState != null) {
				Persistable per = prom;
				// 1. Check if Part is from ESG Context
				// 2. Check if part is not checked-out
				String contextName = ((WTContained) per).getContainerName();
				if (isESGContext(contextName) && !WorkInProgressHelper.isCheckedOut((Workable) per)
						&& EnerSysHelper.service.isAttributeApplicableOnObject(per, ESG_ERP_STATUS_INTRL_NAME)) {

					String targetERPStatus = null;

					String startStateStr = startState.toString();
					String targetStateStr = targetState.toString();

					if (startStateStr.equals(EnerSysService.STATE_INWORK)) {
						// If Start State is IN WORK
						if (targetStateStr.equals(EnerSysService.STATE_A_RELEASE_CONCEPT)) {
							// ZN
							// targetERPStatus = "ZN";//commented for User Story 9181: ERP Status on
							// A-Released should point to Z1 instead of ZN
							targetERPStatus = "Z1";
						} else if (targetStateStr.equals(EnerSysService.STATE_B_RELEASE_CONCEPT)) {
							// ZJ
							// targetERPStatus = "ZJ";
							targetERPStatus = "Z1";
						} else if (targetStateStr.equals(EnerSysService.STATE_C_RELEASE_CONCEPT)) {
							// ZK
							// targetERPStatus = "ZK";
							targetERPStatus = "Z2";
						} else if (targetStateStr.equals(EnerSysService.STATE_PRODUCTION_RELEASED)
								|| targetStateStr.equals(EnerSysService.STATE_RELEASED)) {
							// Z3
							targetERPStatus = "Z3";
						} else if (targetStateStr.equals(EnerSysService.STATE_OBSOLETE)) {
							// Z6
							targetERPStatus = "Z5";
						} else if (targetStateStr.equals(EnerSysService.STATE_WARRANTY_INACTIVE)) {
							// Z5
							targetERPStatus = "Z4";
						}

					} else if (startStateStr.equals(EnerSysService.STATE_A_RELEASE_CONCEPT)) {
						// If Start State is A-RELEASED
						if (targetStateStr.equals(EnerSysService.STATE_B_RELEASE_CONCEPT)) {
							// ZJ
							// targetERPStatus = "ZJ";
							targetERPStatus = "Z1";
						} else if (targetStateStr.equals(EnerSysService.STATE_C_RELEASE_CONCEPT)) {
							// ZK
							// targetERPStatus = "ZK";
							targetERPStatus = "Z2";
						} else if (targetStateStr.equals(EnerSysService.STATE_PRODUCTION_RELEASED)) {
							// Z3
							targetERPStatus = "Z3";
						} else if (targetStateStr.equals(EnerSysService.STATE_OBSOLETE)) {
							// Z6
							targetERPStatus = "Z5";
						} else if (targetStateStr.equals(EnerSysService.STATE_WARRANTY_INACTIVE)) {
							// Z4
							targetERPStatus = "Z4";
						}
					} else if (startStateStr.equals(EnerSysService.STATE_B_RELEASE_CONCEPT)) {
						// If Start State is B-RELEASED
						if (targetStateStr.equals(EnerSysService.STATE_C_RELEASE_CONCEPT)) {
							// ZK
							// targetERPStatus = "ZK";
							targetERPStatus = "Z2";
						} else if (targetStateStr.equals(EnerSysService.STATE_PRODUCTION_RELEASED)) {
							// Z3
							targetERPStatus = "Z3";
						} else if (targetStateStr.equals(EnerSysService.STATE_OBSOLETE)) {
							// Z6
							targetERPStatus = "Z5";
						} else if (targetStateStr.equals(EnerSysService.STATE_WARRANTY_INACTIVE)) {
							// Z4
							targetERPStatus = "Z4";
						}
					} else if (startStateStr.equals(EnerSysService.STATE_C_RELEASE_CONCEPT)) {
						// If Start State is C-RELEASED
						if (targetStateStr.equals(EnerSysService.STATE_PRODUCTION_RELEASED)) {
							// Z3
							targetERPStatus = "Z3";
						} else if (targetStateStr.equals(EnerSysService.STATE_OBSOLETE)) {
							// Z6
							targetERPStatus = "Z5";
						} else if (targetStateStr.equals(EnerSysService.STATE_WARRANTY_INACTIVE)) {
							// Z4
							targetERPStatus = "Z4";
						}
					} else if (startStateStr.equals(EnerSysService.STATE_PRODUCTION_RELEASED)) {
						// If Start State is PRODUCTION-RELEASED
						if (targetStateStr.equals(EnerSysService.STATE_OBSOLETE)) {
							// Z6
							targetERPStatus = "Z5";
						} else if (targetStateStr.equals(EnerSysService.STATE_WARRANTY_INACTIVE)) {
							// Z4
							targetERPStatus = "Z4";
						}
					} else if (startStateStr.equals(EnerSysService.STATE_WARRANTY_INACTIVE)) {
						// If Start State is PRODUCTION-RELEASED
						if (targetStateStr.equals(EnerSysService.STATE_OBSOLETE)) {
							// Z6
							targetERPStatus = "Z5";
						}
					} else if (startStateStr.equals(EnerSysService.STATE_RELEASED)) {
						if (targetStateStr.equals(EnerSysService.STATE_OBSOLETE)
								|| targetStateStr.equals(EnerSysService.STATE_END_OF_LIFE)) {
							// Z6
							targetERPStatus = "Z5";
						} else if (targetStateStr.equals(EnerSysService.STATE_WARRANTY_INACTIVE)) {
							// Z4
							targetERPStatus = "Z4";
						}
					}
					if (targetERPStatus != null) {
						// Check ERP Status and override, if needed
						String erpStatusVal = "";
						try {
							PersistableAdapter pers = new PersistableAdapter(per, null, Locale.US,
									new DisplayOperationIdentifier());
							pers.load(ESG_ERP_STATUS_INTRL_NAME);
							erpStatusVal = (String) pers.getAsString(ESG_ERP_STATUS_INTRL_NAME);
						} catch (Exception e) {
						}
						if (!targetERPStatus.equalsIgnoreCase(erpStatusVal)) {
							setStringIBAValue(per, ESG_ERP_STATUS_INTRL_NAME, targetERPStatus);
						}
					}
				}
			}
		} catch (Exception e) {

		}
	}

	/**
	 * 
	 * @param prtObj
	 * @param attrInternalName
	 * @param attrValue
	 * @return
	 * @since Build v2.5
	 */
	private boolean setStringIBAValue(Persistable prtObj, String attrInternalName, String attrValue) {
		boolean result = false;
		try {
			// GET Existing IBA Values, if any
			StringDefinition sDef = getStringIBADefinition(attrInternalName);
			StringValue svObj = getStringIBAValue(prtObj, sDef, attrInternalName);
			if (svObj != null) {
				PersistenceHelper.manager.delete(svObj);
			}
			StringValue svAttrValue = StringValue.newStringValue(sDef, (IBAHolder) prtObj, attrValue);
			PersistenceHelper.manager.save(svAttrValue);
			result = true;
		} catch (WTException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 
	 * @param attrInternalName
	 * @return
	 * @since Build v2.5
	 */
	private StringDefinition getStringIBADefinition(String attrInternalName) {
		StringDefinition sDef = null;
		try {
			QuerySpec qSpec = new QuerySpec(StringDefinition.class);
			SearchCondition sCondition = new SearchCondition(StringDefinition.class, StringDefinition.NAME,
					SearchCondition.EQUAL, attrInternalName);
			qSpec.appendWhere(sCondition, new int[] { 0 });

			QueryResult qRes = PersistenceHelper.manager.find((StatementSpec) qSpec);
			if (qRes != null && qRes.hasMoreElements()) {
				sDef = (StringDefinition) qRes.nextElement();
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return sDef;
	}

	/**
	 * 
	 * @param prtObj
	 * @param sDef
	 * @param attrInternalName
	 * @return
	 * @since Build v2.5
	 */
	private StringValue getStringIBAValue(Persistable prtObj, StringDefinition sDef, String attrInternalName) {
		StringValue sVal = null;

		try {
			if (sDef == null && attrInternalName != null && !attrInternalName.isEmpty()) {
				sDef = getStringIBADefinition(attrInternalName);
			}

			if (sDef != null && prtObj != null && attrInternalName != null) {
				QuerySpec qSpec = new QuerySpec(StringValue.class);
				SearchCondition sCondition1 = new SearchCondition(StringValue.class,
						StringValue.DEFINITION_REFERENCE + ".key", SearchCondition.EQUAL,
						sDef.getPersistInfo().getObjectIdentifier());
				SearchCondition sCondition2 = new SearchCondition(StringValue.class,
						StringValue.IBAHOLDER_REFERENCE + ".key", SearchCondition.EQUAL,
						prtObj.getPersistInfo().getObjectIdentifier());
				qSpec.appendWhere(sCondition1, new int[] { 0 });
				qSpec.appendAnd();
				qSpec.appendWhere(sCondition2, new int[] { 0 });

				QueryResult qRes = PersistenceHelper.manager.find((StatementSpec) qSpec);
				if (qRes != null && qRes.hasMoreElements()) {
					sVal = (StringValue) qRes.nextElement();
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return sVal;
	}

	/**
	 * Sets ERP Status attribute for Promotion Targets from a Promotion Notice.<br>
	 * It checks the Promotion Target Links to determine the "Promoted From
	 * State".<br>
	 * Hence, can be called at any point in the workflow (objects should be
	 * unlocked).
	 * 
	 * @since Build v2.5
	 */
	@Override
	public void esgErpStatusPerformAutoPromotion(PromotionNotice pn) {
		try {
			QueryResult qr = MaturityHelper.service.getPromotionTargets(pn, false);
			while (qr.hasMoreElements()) {
				Object o = qr.nextElement();
				if (o instanceof PromotionTarget) {
					PromotionTarget pt = (PromotionTarget) o;
					State startState = pt.getCreateState();
					State targetState = pn.getMaturityState();
					Promotable promotableObj = pt.getPromotable();
					esgErpStatusAutoPromote(promotableObj, startState, targetState);
				}
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Sets ERP Status attribute for Resulting Objects from a Change Notice.<br>
	 * Should be called after the Change Notice Approvals are finished, but before
	 * the ESI trigger (set state method robot).
	 * 
	 * @param cn
	 * @since Build v2.9
	 */
	@Override
	public void esgErpStatusPerformAutoPromotionForCN(WTChangeOrder2 cn) {
		try {
			WTHashSet set = CM2Helper.service.getAllResultingObjects(cn);
			Iterator<?> f = set.persistableIterator();
			while (f.hasNext()) {
				WTObject obj = (WTObject) f.next();
				esgErpStatusAutoPromote((Promotable) obj, State.INWORK, ((LifeCycleManaged) obj).getLifeCycleState());
			}
		} catch (Exception e) {
			//
		}
	}

	/**
	 * Gets called from Promotion Request/Change Request/Change Notice/Change
	 * Task/Deviation workflows
	 * 
	 * @since Build v2.8
	 */
	@Override
	public String getCriticalPartsInTask(WTObject pbo) {
		List<String> prtInfoList = new ArrayList<>();
		Set<WTPart> criticalParts = new HashSet<>();
		Set<WTPart> finalCriticalParts = new HashSet<>();
		// Jira: 784
		String moreCriticalPartsMainText = "";
		String moreCriticalPartsSubText = "";
		TypeIdentifier pbo_TI = TypeIdentifierHelper.getType(pbo);
		try {
			WTHashSet affectedObjects = CM2Helper.service.getAllAffectedObjects(pbo);
			Iterator affObjItr = affectedObjects.iterator();
			while (affObjItr.hasNext()) {
				Persistable per = ((WTReference) affObjItr.next()).getObject();
				if (EnerSysHelper.service.isCriticalPart(per)) {
					criticalParts.add((WTPart) per);
				}
			}
			// Iterate through all critical parts and list latest critical part by comparing
			// idA2A2
			for (WTPart criticalPart : criticalParts) {
				WTPart finalCriticalPart = criticalPart;
				for (WTPart innerCriticalPart : criticalParts) {
					if (criticalPart.getNumber().equals(innerCriticalPart.getNumber())) {
						Long objId1 = criticalPart.getPersistInfo().getObjectIdentifier().getId();
						Long objId2 = innerCriticalPart.getPersistInfo().getObjectIdentifier().getId();
						if (objId1 > objId2) {
							finalCriticalPart = criticalPart;
						} else if (objId2 > objId1) {
							finalCriticalPart = innerCriticalPart;
						}
					}
				}
				finalCriticalParts.add(finalCriticalPart);
			}

			if (!finalCriticalParts.isEmpty()) {
				for (WTPart criticalPart : finalCriticalParts) {
					prtInfoList.add(
							"<tr><td style='border-collapse:collapse;border: 2px solid #000000;text-align:center;padding:3px;'>"
									+ criticalPart.getNumber()
									+ "</td><td style='border-collapse:collapse;border: 2px solid #000000;text-align:center;padding:3px;'>"
									+ criticalPart.getName()
									+ "</td><td style='border-collapse:collapse;border: 2px solid #000000;text-align:center;padding:3px;'>"
									+ EnerSysHelper.service.getVersionInformation(criticalPart) + "</td></tr>");
					// Jira: 784
					if (prtInfoList.size() == 4 && finalCriticalParts.size() > 4) {
						moreCriticalPartsMainText = "& " + (finalCriticalParts.size() - 4)
								+ " more critical part(s) are included.";
						EnerSysSoftTypeHelper.isExactlyType(pbo, "wt.change2.WTChangeRequest2");
						if (CM2Helper.service.getChangeTypeString(pbo_TI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEREQUEST)) {
							moreCriticalPartsSubText = "Go to details page of Change Request & review the Critical Parts from Affected Objects table";
						} else if (CM2Helper.service.getChangeTypeString(pbo_TI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEACTIVITY)) {
							moreCriticalPartsSubText = "Go to details page of Change Activity & review the Critical Parts from Affected/Resulting Objects table";
						} else if (CM2Helper.service.getChangeTypeString(pbo_TI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {
							moreCriticalPartsSubText = "Go to details page of Change Notice & review the Critical Parts from Change Summary table";
						} else if (CM2Helper.service.getChangeTypeString(pbo_TI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)) {
							moreCriticalPartsSubText = "Go to details page of Promotion Request & review the Critical Parts from Promotion Objects table";
						} else if (CM2Helper.service.getChangeTypeString(pbo_TI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_DEVIATION)) {
							moreCriticalPartsSubText = "Go to details page of Deviation & review the Critical Parts from Affected Objects table";
						} else if (CM2Helper.service.getChangeTypeString(pbo_TI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_DOCUMENTAPPROVAL)) {
							moreCriticalPartsSubText = "Go to details page of Document Approval & review the Critical Parts from Review Objects table";
						}
						prtInfoList.add("<tr><td colspan='3'><br>" + moreCriticalPartsMainText + "<br><br>"
								+ moreCriticalPartsSubText + "</td></tr>");
						break;
					}
				}
			}
			if (prtInfoList.isEmpty()) {
				prtInfoList.add(
						"<tr><td colspan='3' style='border-collapse:collapse;border: 2px solid #000000;text-align:center;padding:3px;'>No Critical Parts identified on this process</td></tr>");
			}

			if (!prtInfoList.isEmpty()) {
				// Add basic HTML Table format, headers etc.
				prtInfoList.add(0,
						"<table style='border-collapse:collapse;border: 2px solid #000000;'><thead><tr><th style='border-collapse:collapse;border: 2px solid #000000;text-align:center;padding:5px;'>Number</th><th style='border-collapse:collapse;border: 2px solid #000000;text-align:center;padding:5px;'>Name</th><th style='border-collapse:collapse;border: 2px solid #000000;text-align:center;padding:5px;'>Version</th></tr></thead><tbody>");
				prtInfoList.add(0, "<br>");
				prtInfoList.add("</tbody></table>");
			}

		} catch (WTException e) {
			e.printStackTrace();
		}
		return StringUtils.join(prtInfoList, "");

	}

	public String gettypeSelection(String eParticipantsStr) {
		String typeValue = "single";
		String selectionBoxUI = "radio";
		if (eParticipantsStr != null && !eParticipantsStr.isEmpty()) {
			String[] DNs = eParticipantsStr.split("=DN=");
			for (String DN : DNs) {
				if (!DN.isEmpty()) {
					String[] lastMap = DN.split("=CONT=");
					String approvalMatrixAttributeValues[] = lastMap[0]
							.split(EnerSysApprovalMatrixDefinition.COLON_ROLE_MAP_DELIM);
					typeValue = approvalMatrixAttributeValues[approvalMatrixAttributeValues.length - 1];
				}
			}
		}

		if (typeValue.equals("multiple")) {
			selectionBoxUI = "checkbox";
		} else {
			selectionBoxUI = "radio";
		}
		return selectionBoxUI;
	}

	private Set<NmOid> processAndFetchChangeNoticeResultingObjects(final String TABLE_ID, NmCommandBean commandBean) {
		try {
			// CreateAndEditWizBean dd;
			// NmContextBean d;
			Set<String> affectedObjStr = new HashSet<String>();
			for (Entry<String, Object> obj : commandBean.getParameterMap().entrySet()) {
				if (obj.getKey().startsWith(GLOBAL_CT_RESULTING_OBJ_STR) && obj.getValue() instanceof String[]
						&& ((String[]) obj.getValue()).length > 0) {
					String[] val = (String[]) obj.getValue();
					affectedObjStr.addAll(Arrays.asList(val[0].replace("[", "").replace("]", "").split("\\s*,\\s*")));
				}
			}
			ReferenceFactory rf = new ReferenceFactory();

			HashSet<NmOid> affObjNmOidSet = new HashSet<>();
			Iterator<String> itr = affectedObjStr.iterator();
			while (itr.hasNext()) {
				String o = itr.next();
				affObjNmOidSet.add(new NmOid(rf.getReference(o).getObject()));
			}
			return affObjNmOidSet;
		} catch (WTException e) {
			e.printStackTrace();
		}
		return Collections.emptySet();
	}
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMapForPromotionRequest(WTObject obj,NmCommandBean commandBean) {
		LOGGER.debug("START: getChangeApprovalRoleMapForPromotionRequest");
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> retMap = null;

		try {
			
			boolean orderChngCriticalPartPrefValue = (boolean) PreferenceHelper.service.getValue(
					((WTContained) obj).getContainerReference(), CRITICAL_PART_SELECTOR_ROLE_PREFERENCE,
					PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());
			TypeIdentifier creatObjTI = TypeIdentifierHelper.getType(obj);
			Set<NmOid> affectedItemSet = new HashSet();
			LOGGER.debug("affectedItemSet="+affectedItemSet);
		        affectedItemSet.clear();
			affectedItemSet = getAffectedObjectsForApprovals(creatObjTI, commandBean);
			if(affectedItemSet.size()==0){
			WTHashSet resObjects = CM2Helper.service.getAllResultingObjects(obj);
		    
			Iterator resObjItr = resObjects.iterator();
			while (resObjItr.hasNext()) {
				Persistable per = ((WTReference) resObjItr.next()).getObject();
				affectedItemSet.add(new NmOid(per));
			}
			}
			
			
			LOGGER.debug("affectedItemSet="+affectedItemSet);
			retMap = EnerSysApprovalMatrixUtility.getInstance().getChangeApprovalRoleMapCN(affectedItemSet, creatObjTI,
					obj);
			LOGGER.debug("retMap="+retMap);
			if (orderChngCriticalPartPrefValue) {
				retMap = EnerSysApprovalMatrixUtility.getInstance().additionalRolesAdded(retMap, affectedItemSet);
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return retMap;
	}
}