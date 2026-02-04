package ext.enersys.cm2.service;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import javax.validation.constraints.NotNull;

import org.apache.logging.log4j.Logger;

import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.windchill.enterprise.change2.commands.RelatedChangesQueryCommands;

import ext.enersys.service.EnerSysHelper;
import ext.enersys.service.EnerSysService;
import ext.enersys.utilities.Debuggable;
import ext.enersys.utilities.EnerSysLogUtils;
import wt.associativity.EquivalenceLink;
import wt.change2.ChangeHelper2;
import wt.change2.ChangeRecord2;
import wt.change2.Changeable2;
import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeIssue;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeRequest2;
import wt.change2.WTChangeReview;
import wt.change2.WTVariance;
import wt.enterprise.RevisionControlled;
import wt.fc.ObjectIdentifier;
import wt.fc.ObjectReference;
import wt.fc.PersistInfo;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTKeyedHashMap;
import wt.fc.collections.WTSet;
import wt.inf.container.OrgContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.State;
import wt.lifecycle.Transition;
import wt.log4j.LogR;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.org.OrganizationServicesHelper;
import wt.org.WTGroup;
import wt.org.WTOrganization;
import wt.org.WTPrincipalReference;
import wt.org.WTUser;
import wt.part.WTPart;
import wt.preference.PreferenceClient;
import wt.preference.PreferenceHelper;
import wt.project.Role;
import wt.services.StandardManager;
import wt.session.SessionHelper;
import wt.team.Team;
import wt.team.TeamManaged;
import wt.team.TeamReference;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.Iterated;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;
import wt.workflow.definer.WfAssignedActivityTemplate;
import wt.workflow.definer.WfTemplateObjectReference;
import wt.workflow.engine.ProcessData;
import wt.workflow.engine.WfProcess;
import wt.workflow.work.WfActorRoleAssignee;
import wt.workflow.work.WfAssignedActivity;
import wt.workflow.work.WfPrincipalAssignee;
import wt.workflow.work.WfRoleAssignee;
import wt.part.WTPart;
import wt.preference.PreferenceClient;
import wt.preference.PreferenceHelper;
public class StandardCM2Service extends StandardManager implements Serializable, CM2Service, Debuggable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3919896655032633404L;

	private static final String CLASSNAME = StandardCM2Service.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(StandardCM2Service.class.getName());

	private final String COMMA_DELIM = ",";
	private final String COLON_DELIM = ":";
	private final String EQUALS_DELIM = "=";

	private final String DA_ROLE = "DESIGN AUTHORITY";
	private final String ASIGNEE_ROLE = "ASSIGNEE";

	private static final String PERSIST_KEY_ID = Persistable.PERSIST_INFO + "." + PersistInfo.OBJECT_IDENTIFIER + "."
			+ ObjectIdentifier.ID;

	public static StandardCM2Service newStandardCM2Service() throws WTException {
		LOGGER.debug("--StandardCM2Service--Starting");
		StandardCM2Service service = new StandardCM2Service();
		service.initialize();
		LOGGER.debug("--StandardCM2Service--INITIALIZED");
		return service;
	}

	/**
	 * Used in CN WF
	 * 
	 * ext.enersys.cm2.CM2Helper.service.restoreStateMapOfAffectedObjects(primaryBusinessObject,CURRENT_AFT_OBJS_STATE);
	 */
	@Override
	public void restoreStateMapOfAllAffectedObjects(WTObject pbo, WTKeyedHashMap map) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#restoreStateMapOfAllAffectedObjects", " pbo: ", pbo, " map: ", map);
		if (pbo != null) {
			WTHashSet hs = getAllAffectedObjects(pbo);
			Iterator<?> f = hs.persistableIterator();
			while (f.hasNext()) {
				WTObject obj = (WTObject) f.next();
				String stateToCheck = (String) map.get(obj);
				if (stateToCheck != null && !stateToCheck.isEmpty()) {
					EnerSysHelper.service.setState(obj, stateToCheck);
				}
			}
		}
		checkAndWriteDebug(Debuggable.END, "#restoreStateMapOfAllAffectedObjects");
	}

	@Override
	public Set<String> getDisplayNamesOfStates(Set<String> stateSet) {
		HashSet<String> retSet = new HashSet<String>();
		for (String st : stateSet) {
			try {
				retSet.add(State.toState(st).getDisplay(Locale.US));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return retSet;
	}

	@Override
	public WTHashSet getAllAffectedObjects(WTObject changeObj) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#getAllAffectedObjects -->", " changeObj: ", changeObj);
		String strType = getChangeTypeString(changeObj);
		checkAndWriteDebug(Debuggable.LINE, "#getAllAffectedObjects -->", " strType: ", strType);
		WTHashSet a = new WTHashSet();
		try {
			switch (strType) {
			case EnerSysService.ACTIVITY_CHANGEREQUEST:
				LOGGER.debug("#ECR Number: " + ((WTChangeRequest2) changeObj).getNumber());
				a.addAll(ChangeHelper2.service.getChangeables((WTChangeRequest2) changeObj, true));
				return a;
			case EnerSysService.ACTIVITY_CHANGENOTICE:
				a.addAll(ChangeHelper2.service.getChangeablesAfter((WTChangeOrder2) changeObj, true));
				a.addAll(ChangeHelper2.service.getChangeablesBefore((WTChangeOrder2) changeObj, true));
				return a;
			case EnerSysService.ACTIVITY_CHANGEACTIVITY:
				a.addAll(ChangeHelper2.service.getChangeablesAfter((WTChangeActivity2) changeObj, true));
				a.addAll(ChangeHelper2.service.getChangeablesBefore((WTChangeActivity2) changeObj, true));
				return a;
			case EnerSysService.ACTIVITY_PROMOTIONREQUEST:
				a.addAll(MaturityHelper.service.getPromotionTargets((PromotionNotice) changeObj, true));
				return a;

			case EnerSysService.ACTIVITY_DEVIATION:
				a.addAll(ChangeHelper2.service.getChangeables((WTVariance) changeObj, true));
				return a;

			case EnerSysService.ACTIVITY_DOCUMENTAPPROVAL:
				a.addAll(ChangeHelper2.service.getChangeables((WTChangeReview) changeObj, true));
				return a;

			default:
				throw new WTException(CLASSNAME + ".getAllAffectedObjects() - Type not Found!! - " + changeObj);
			}

		} finally {
			checkAndWriteDebug(Debuggable.END, "#getAllAffectedObjects");
		}
	}

	/**
	 * Used in CN WF
	 * 
	 * CURRENT_AFT_OBJS_STATE =
	 * ext.enersys.cm2.CM2Helper.service.returnCurrentStateMapOfAffectedObjects(primaryBusinessObject);
	 */
	@Override
	public WTKeyedHashMap returnCurrentStateMapOfAllAffectedObjects(WTObject pbo) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#returnCurrentStateMapOfAffectedObjects", " pbo: ", pbo);
		WTKeyedHashMap retMap = new WTKeyedHashMap();
		if (pbo != null) {
			WTHashSet hs = getAllAffectedObjects(pbo);
			Iterator<?> f = hs.persistableIterator();
			while (f.hasNext()) {
				WTObject obj = (WTObject) f.next();
				String currentState = EnerSysHelper.service.getObjectState(obj);
				retMap.put(obj, currentState);
			}
		}
		checkAndWriteDebug(Debuggable.END, "#returnCurrentStateMapOfAffectedObjects", " retMap: ", retMap);
		return retMap;
	}

	@Override
	public String selectDesignAuthorityUser(String roleMap) {
		// TODO: Add Loggers
		String retStr = null;
		if (roleMap != null && !roleMap.isEmpty()) {
			String mapRoles[] = roleMap.split(COMMA_DELIM);
			for (int i = 0; i < mapRoles.length; ++i) {
				if (mapRoles[i].contains(DA_ROLE)) {
					retStr = mapRoles[i];
					break;
				}
			}
		}
		return retStr;
	}

	/**
	 * 
	 * Checks if the Participants are available in the Team Instance.<br>
	 * If not then Checks if the Responsible Role is available in the Team. <br>
	 * Otherwise, assigns the Activity to Creator.
	 * 
	 * @param process
	 * @param pbo
	 * @param roleUserMap
	 * @param required
	 * @throws WTException
	 * @since Build v2.1
	 */
	@Override
	public void addParticipantsOnActivityBasedOnTeamInstance(ObjectReference process, WTObject pbo, String roleStr,
			String required) throws WTException {
		if (process != null && process.getObject() instanceof WfAssignedActivity && roleStr != null
				&& !roleStr.isEmpty()) {
			WfAssignedActivity activityObj = (WfAssignedActivity) process.getObject();
			LOGGER.debug("#activityObj name: " + activityObj.getName());

			WfProcess wfProcessObj = activityObj.getParentProcess();
			// Identifies a team to use for role resolution for the associated activities.
			WfAssignedActivityTemplate waTemplate = (WfAssignedActivityTemplate) activityObj.getTemplate().getObject();
			LOGGER.debug(CLASSNAME + ".addParticipantsOnActivityBasedOnTeamInstance(): template = " + waTemplate);

			LOGGER.debug("#waTemplate name: " + waTemplate.getName());

			Enumeration<?> actors = waTemplate.getActorRoleAssignees();
			while (actors.hasMoreElements()) {
				waTemplate.removeActorRoleAssignee((WfActorRoleAssignee) actors.nextElement());
			}

			TeamReference teamReference = ((TeamManaged) pbo).getTeamId();
			LOGGER.debug("#teamReference name: " + teamReference.getName());

			Team team = (Team) teamReference.getObject();
			LOGGER.debug("#Team name: " + team.getName());

			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(activityObj);

			// Delete default assignee role since it creates a conflict with Change Activity
			// Review task; (By default, OOTB unnecessarily adds ASSIGNEE role to approvers)
			if (getChangeTypeString(pbo).equals(EnerSysService.ACTIVITY_CHANGEACTIVITY)) {
				Role wfRoleDel = Role.toRole(ASIGNEE_ROLE);
				LOGGER.debug("#wfRoleDel name: " + wfRoleDel.getDisplay());

				Enumeration<?> membersEnum = team.getPrincipalTarget(wfRoleDel);
				while (membersEnum.hasMoreElements()) {
					String name = ((WTPrincipalReference) membersEnum.nextElement()).getName();
					LOGGER.debug(CLASSNAME + ".addRolesAndParticipantsOnActivity(): Member = " + name + " Role = "
							+ ASIGNEE_ROLE);
					WTUser oldUser = OrganizationServicesHelper.manager.getUser(name,
							orgContainer.getContextProvider());
					team.deletePrincipalTarget(wfRoleDel, oldUser);
				}
			}

			// SPLIT INPUT USING COMMA (,)
			String[] rolesString = roleStr.split(COMMA_DELIM);
			LOGGER.debug("rolesString" + rolesString);
			if (required == null || required.isEmpty()
					|| !required.equalsIgnoreCase("ANY") && !required.equalsIgnoreCase("ALL")) {
				required = "ANY";
			}
			required = required.toUpperCase();

			for (int i = 0; i < rolesString.length; i++) {
				String roleName = rolesString[i].split(COLON_DELIM)[0].trim();
				LOGGER.debug("#roleName: " + roleName);

				Role wfRoleDel = Role.toRole(roleName);
				Enumeration<?> membersEnum = team.getPrincipalTarget(wfRoleDel);
				if (membersEnum.hasMoreElements()) {
					boolean roleAssigneeAdded = false;
					while (membersEnum.hasMoreElements()) {
						String userToAdd = ((WTPrincipalReference) membersEnum.nextElement()).getName();
						LOGGER.debug("userToAdd=" + userToAdd);
						if(!userToAdd.equalsIgnoreCase("Administrators")) {
						boolean isUser = OrganizationServicesHelper.manager.getUser(userToAdd,
								orgContainer.getContextProvider()) != null;
						// IF USER IS FOUND THEN ASSIGN THE ROLE TO ACTIVITY; OTHERWISE CHECK IF THE
						// USER IS A GROUP THEN ASSIGN IT TO THE ACTIVITY
						if (isUser) {
							if (!roleAssigneeAdded) {

								if (getChangeTypeString(pbo).equals(EnerSysService.ACTIVITY_DOCUMENTAPPROVAL)) {
									if (roleName.equalsIgnoreCase("CHANGE MANAGER")
											|| roleName.equalsIgnoreCase("MFG_TEST_ENGINEERING")
											|| roleName.equalsIgnoreCase("PRODUCT MANAGEMENT"))
										required = "ANY";
								}

								WfRoleAssignee roleAssignee = new WfRoleAssignee("R:" + required + ".0,R:" + roleName);
								LOGGER.debug("R:" + required + ".0,R:" + roleName);
								waTemplate.addRoleAssignee(roleAssignee);
								roleAssigneeAdded = true;
								LOGGER.debug("roleAssignee=" + roleAssignee);
							}
						} else {
							WTGroup assignedGroup = OrganizationServicesHelper.manager.getGroup(userToAdd,
									orgContainer.getContextProvider());
							String assignedGroupId = assignedGroup.getPersistInfo().getObjectIdentifier().toString();
							WfPrincipalAssignee principalAssignee = new WfPrincipalAssignee(
									"R:ANY.0,OR:" + assignedGroupId);
							waTemplate.addPrincipalAssignee(principalAssignee);
							LOGGER.debug("principalAssignee=" + principalAssignee);
						}
						}
					}
				} else {
					// NOT USED due to confusion with Responsible roles for an activity.
					// Below code is executed if the Team Template does not have Selected
					// Participants defined in the Team
					// Check if the Responsible role is available in the Team
					Enumeration<?> responsibleEnum = null;
					// COMMENTED OUT BELOW CODE TO DISABLE RESPONSIBLE ROLE FEATURE
					// responsibleEnum = team.getPrincipalTarget(waTemplate.getResponsibleRole());
					// If Responsible roles is not available then assign to CREATOR
					/*
					 * if (responsibleEnum != null && !responsibleEnum.hasMoreElements()) { try { //
					 * ASSIGN TO CREATOR IF NO ONE SELECTED WfRoleAssignee roleAssignee = new
					 * WfRoleAssignee("N:" + required + ".0,R:" + roleName);
					 * waTemplate.addRoleAssignee(roleAssignee); WTUser creator =
					 * OrganizationServicesHelper.manager.getUser(wfProcessObj.getCreator().
					 * getPrincipal().getName(), orgContainer.getContextProvider()); Role wfRole =
					 * Role.toRole(roleName); team.addPrincipal(wfRole, creator); } catch (Exception
					 * e) { LOGGER.error(e); } }
					 */
				}
			}
			activityObj.setTemplate(WfTemplateObjectReference.newWfTemplateObjectReference(waTemplate));
			PersistenceHelper.manager.save(activityObj);
			LOGGER.debug(CLASSNAME + ".addParticipantsOnActivityBasedOnTeamInstance(): template = " + waTemplate);
		}
	}

	/**
	 * Logic to delete existing users, roles then map it according to selected
	 * participants.
	 */
	@Override
	public void addRolesAndParticipantsOnActivity(ObjectReference process, WTObject pbo, String roleUserMap,
			String required) throws WTException {
		// TODO:Modify Loggers
		// LOGGER.debug((new
		// StringBuilder()).append(CLASSNAME).append(".addRolesAndParticipantsOnActivity():
		// process = ").append(process.toString()).toString());
		if (process != null && process.getObject() instanceof WfAssignedActivity && roleUserMap != null && !roleUserMap.isEmpty()) {
			WfAssignedActivity act = (WfAssignedActivity) process.getObject();
			WfProcess wfprocess = act.getParentProcess();
			// Identifies a team to use for role resolution for the associated activities.
			WfAssignedActivityTemplate waTemplate = (WfAssignedActivityTemplate) act.getTemplate().getObject();
			LOGGER.debug(CLASSNAME + ".addRolesAndParticipantsOnActivity(): template = " + waTemplate);

			Enumeration<?> actors = waTemplate.getActorRoleAssignees();
			while (actors.hasMoreElements()) {
				waTemplate.removeActorRoleAssignee((WfActorRoleAssignee) actors.nextElement());
			}

			TeamReference teamReference = ((TeamManaged) pbo).getTeamId();
			Team team = (Team) teamReference.getObject();
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(act);

			if (getChangeTypeString(pbo).equals(EnerSysService.ACTIVITY_CHANGEACTIVITY)) {
				Role wfRoleDel = Role.toRole(ASIGNEE_ROLE);
				Enumeration<?> membersEnum = team.getPrincipalTarget(wfRoleDel);
				while (membersEnum.hasMoreElements()) {
					String name = ((WTPrincipalReference) membersEnum.nextElement()).getName();
					LOGGER.debug(CLASSNAME + ".addRolesAndParticipantsOnActivity(): Member = " + name + " Role = "
							+ ASIGNEE_ROLE);
					WTUser oldUser = OrganizationServicesHelper.manager.getUser(name,
							orgContainer.getContextProvider());
					team.deletePrincipalTarget(wfRoleDel, oldUser);
				}
			}
			// SPLIT INPUT USING COMMA (,)
			String userRoleString[] = roleUserMap.split(COMMA_DELIM);

			if (required == null || required.isEmpty()
					|| !required.equalsIgnoreCase("ANY") && !required.equalsIgnoreCase("ALL")) {
				required = "ANY";
			}
			required = required.toUpperCase();

			for (int i = 0; i < userRoleString.length; i++) {
				LOGGER.debug((new StringBuilder()).append(CLASSNAME)
						.append(".addRolesAndParticipantsOnActivity(): role with user to add = ")
						.append(userRoleString[i]).toString());
				// for EACH entry, split it further by EQUALS-SIGN (=)
				String usersAndRole[] = userRoleString[i].split(EQUALS_DELIM);
				// ROLE = [0] & USER_LIST = [1]
				String roleName = usersAndRole[0];
				if (usersAndRole.length > 1) {
					// USER_LIST = [1]
					String assignedUsersStr = usersAndRole[1];
					// EACH USER CAN BE Split using COLON (:)
					String userTokens[] = assignedUsersStr.split(COLON_DELIM);
					boolean roleAssigneeAdded = false;
					Role wfRoleDel = Role.toRole(roleName);
					Enumeration<?> membersEnum = team.getPrincipalTarget(wfRoleDel);
					// DELETE PARTICIPANTS FROM THE SPECIFIC ROLE
					while (membersEnum.hasMoreElements()) {
						String name = ((WTPrincipalReference) membersEnum.nextElement()).getName();
						LOGGER.debug((new StringBuilder()).append(CLASSNAME)
								.append(".addRolesAndParticipantsOnActivity(): Member = ").append(name)
								.append(" Role = ").append(roleName).toString());
						WTUser oldUser = OrganizationServicesHelper.manager.getUser(name,
								orgContainer.getContextProvider());
						team.deletePrincipalTarget(wfRoleDel, oldUser);
					}
					Role wfRoleToAddUnder = Role.toRole(roleName);
					for (int k = 0; k < userTokens.length; ++k) {
						String singleUser = assignedUsersStr.split(COLON_DELIM)[k];
						LOGGER.debug((new StringBuilder()).append(CLASSNAME)
								.append(".addRolesAndParticipantsOnActivity(): singleUser=").append(singleUser)
								.toString());

						boolean isUser = OrganizationServicesHelper.manager.getUser(singleUser,
								orgContainer.getContextProvider()) != null;
						// IF USER IS FOUND THEN ASSIGN TO ROLE; OTHERWISE CHECK IF THE USER IS A GROUP
						// THEN ASSIGN IT UNDER THE ROLE
						if (isUser) {
							if (!roleAssigneeAdded) {
								WfRoleAssignee roleAssignee = new WfRoleAssignee("R:" + required + ".0,R:" + roleName);
								waTemplate.addRoleAssignee(roleAssignee);
								roleAssigneeAdded = true;
							}
							WTUser assignedUser = OrganizationServicesHelper.manager.getUser(singleUser,
									orgContainer.getContextProvider());
							team.addPrincipal(wfRoleToAddUnder, assignedUser);
						} else {
							// IF USER IS NOT FOUND THEN ASSIGN TO
							WTGroup assignedGroup = OrganizationServicesHelper.manager.getGroup(singleUser,
									orgContainer.getContextProvider());
							String assignedGroupId = assignedGroup.getPersistInfo().getObjectIdentifier().toString();
							WfPrincipalAssignee principalAssignee = new WfPrincipalAssignee(
									"R:ANY.0,OR:" + assignedGroupId);
							waTemplate.addPrincipalAssignee(principalAssignee);
						}
					}
				} else {
					// ASSIGN TO CREATOR IF NO ONE SELECTED
					try {
						WfRoleAssignee roleAssignee = new WfRoleAssignee("N:" + required + ".0,R:" + roleName);
						waTemplate.addRoleAssignee(roleAssignee);
						WTUser creator = OrganizationServicesHelper.manager.getUser(
								wfprocess.getCreator().getPrincipal().getName(), orgContainer.getContextProvider());
						if (creator != null)
							LOGGER.debug(
									CLASSNAME + ".addRolesAndParticipantsOnActivity(): user name to add (Creator) = "
											+ creator.getName() + " in role = " + roleName);
						Role wfRole = Role.toRole(roleName);
						team.addPrincipal(wfRole, creator);
					} catch (Exception e) {
						LOGGER.error(e);
					}
				}
			}

			// TODO: REFINE the below LOGGER Statement
			for (Enumeration temp = waTemplate.getRoles(); temp.hasMoreElements(); LOGGER
					.debug(CLASSNAME + ".addRolesAndParticipantsOnActivity(): role = " + temp.nextElement()))
				;
			act.setTemplate(WfTemplateObjectReference.newWfTemplateObjectReference(waTemplate));
			PersistenceHelper.manager.save(act);
			LOGGER.debug(CLASSNAME + ".addRolesAndParticipantsOnActivity(): template = " + waTemplate);
		}
	}

	@Override
	public boolean isParticipantInTask(ObjectReference process) throws WTException {

		if (process != null && process.getObject() instanceof WfAssignedActivity) {
			WfAssignedActivity activityObj = (WfAssignedActivity) process.getObject();

			WfAssignedActivityTemplate waTemplate = (WfAssignedActivityTemplate) activityObj.getTemplate().getObject();
			LOGGER.debug(CLASSNAME + ".isParticipantInTask(): template = " + waTemplate);
			Enumeration<?> rolesAssignees = waTemplate.getRoleAssignees();
			if (rolesAssignees.hasMoreElements()) {
				LOGGER.debug("Participant Present - Returning True");
				return true;
			}
		}
		LOGGER.debug("No Participant - Returning False");
		return false;
	}

	/**
	 * @deprecated ??
	 */
	@Override
	public void validateRoleUserNotEmpty(ObjectReference self) throws WTException {
		// TODO:ADD Loggers
		Persistable f = self.getObject();
		if (f instanceof WfAssignedActivity) {
			WfAssignedActivity activity = (WfAssignedActivity) f;
			activity = (WfAssignedActivity) PersistenceHelper.manager.refresh(activity);
			ProcessData actData = activity.getContext();
			String requiredRoleUserMap = (String) actData.getValue("requiredRoleUserMap");
			if (requiredRoleUserMap == null || requiredRoleUserMap.trim().isEmpty()) {
				throw new WTException(new Throwable("Go to Action Menu, Setup participant and select Approvers."));
			}
		}
	}

	/**
	 * For all affected objects associated to the PBO, Calculate the Released State
	 * from the current state, <br>
	 * then perform set-state operation on the object.<br>
	 * 
	 * Update: 17th June 2021: Only Resulting objects should be updated, not all
	 * affected objects else.<br>
	 * Code is commented out as of v1.11-HF2.
	 * 
	 */
	@Deprecated
	@Override
	public void releaseEnerSysObject(WTObject pbo) throws WTException {
		/*
		 * WTObject wtObject; String releasedState; QueryResult qr =
		 * getAffectedObjects(pbo); while (qr.hasMoreElements()) { wtObject = (WTObject)
		 * qr.nextElement(); String initialState =
		 * EnerSysHelper.service.getObjectState(wtObject); releasedState =
		 * getReleasedState(initialState); EnerSysHelper.service.setState(wtObject,
		 * releasedState); }
		 */
	}

	@Override
	public boolean isEveryObjectInSameState(WTObject pbo) throws WTException {
		boolean retBool = true;
		QueryResult qr = getAffectedObjects(pbo);
		String prevState = null;
		while (qr.hasMoreElements()) {
			WTObject g = (WTObject) qr.nextElement();
			String stateToCheck = EnerSysHelper.service.getObjectState(g);
			if (prevState == null) {
				prevState = stateToCheck;
			} else if (!prevState.equalsIgnoreCase(stateToCheck)) {
				retBool = false;
				break;
			}
		}
		return retBool;
	}

	@Override
	public String getLifeCycleDisplayStateFromFirstObj(WTObject pbo) throws WTException {
		String retStr = "";
		QueryResult qr = getAffectedObjects(pbo);
		if (qr.hasMoreElements()) {
			WTObject obj = (WTObject) qr.nextElement();
			if (obj instanceof LifeCycleManaged) {
				retStr = ((LifeCycleManaged) obj).getLifeCycleState().getDisplay();
			}
		}
		return retStr;
	}

	@Override
	public String getLifeCycleStateFromFirstObj(WTObject pbo) throws WTException {
		QueryResult qr = getAffectedObjects(pbo);
		String partName = "";
		if (qr.hasMoreElements()) {
			WTObject obj = (WTObject) qr.nextElement();
			partName = EnerSysHelper.service.getObjectState(obj);
		}
		return partName;
	}

	@Override
	public void commentsRequiredTask(WTObject pbo) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#commentsRequiredTask -->", " pbo: ", pbo);
		if (pbo instanceof WfAssignedActivity) {
			WfAssignedActivity activity = (WfAssignedActivity) pbo;
			ProcessData actData = activity.getContext();
			String comments = actData.getTaskComments();
			checkAndWriteDebug(Debuggable.LINE, "#commentsRequiredTask -->", " comments: ", comments);
			if (comments == null || comments.trim().isEmpty())
				throw new WTException(new Throwable("Comments are required."));
		}
		checkAndWriteDebug(Debuggable.END, "#commentsRequiredTask");
	}

	/**
	 * This method is to be used with Wizard customizations introduced for Setup
	 * Participants.
	 * 
	 * @return Returns a Standardized String denoting the type of object. If type is
	 *         not recognized then an exception is thrown.
	 * @since Build v2.1
	 */
	@Override
	public String getChangeTypeString(TypeIdentifier objTI) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#getChangeTypeString -->", " objTI: ", objTI);
		try {
			if (objTI.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.change2.WTChangeRequest2")))
				return EnerSysService.ACTIVITY_CHANGEREQUEST;
			else if (objTI.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.change2.WTChangeOrder2")))
				return EnerSysService.ACTIVITY_CHANGENOTICE;
			else if (objTI
					.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.change2.WTChangeActivity2")))
				return EnerSysService.ACTIVITY_CHANGEACTIVITY;
			else if (objTI
					.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.maturity.PromotionNotice")))
				return EnerSysService.ACTIVITY_PROMOTIONREQUEST;
			else if (objTI.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.change2.WTVariance")))
				return EnerSysService.ACTIVITY_DEVIATION;
			else if (objTI.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.change2.WTChangeReview")))
				return EnerSysService.ACTIVITY_DOCUMENTAPPROVAL;
			else
				throw new WTException(CLASSNAME + "#getChangeTypeString --> UNDEFINED TYPE !! - " + objTI);
		} finally {
			checkAndWriteDebug(Debuggable.END, "#getChangeTypeString");
		}
	}

	@Override
	public String getChangeTypeString(Object changeObj) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#getChangeTypeString -->", " changeObj: ", changeObj);
		try {
			if (changeObj instanceof TypeIdentifier)
				return getChangeTypeString((TypeIdentifier) changeObj); // Added in Build v2.1 : to reuse important
			// sections of code in other utilities
			else if (changeObj instanceof WTChangeRequest2)
				return EnerSysService.ACTIVITY_CHANGEREQUEST;
			else if (changeObj instanceof WTChangeOrder2)
				return EnerSysService.ACTIVITY_CHANGENOTICE;
			else if (changeObj instanceof WTChangeActivity2)
				return EnerSysService.ACTIVITY_CHANGEACTIVITY;
			else if (changeObj instanceof PromotionNotice)
				return EnerSysService.ACTIVITY_PROMOTIONREQUEST;
			else if (changeObj instanceof WTVariance)
				return EnerSysService.ACTIVITY_DEVIATION; // Added for Build v1.13
			else if (changeObj instanceof WTChangeIssue)
				return EnerSysService.ACTIVITY_PROBLEMREPORT;
			else if (changeObj instanceof WTChangeReview)
				return EnerSysService.ACTIVITY_DOCUMENTAPPROVAL;
			else
				throw new WTException(
						CLASSNAME + "#getChangeTypeString --> UNDEFINED TYPE !! - " + changeObj.toString());
		} finally {
			checkAndWriteDebug(Debuggable.END, "#getChangeTypeString");
		}
	}

	/**
	 * Used by Promotion Request Helper & Other places.</br>
	 * Conditions : </br>
	 * </br>
	 * case ACTIVITY_CHANGEREQUEST: return
	 * ChangeHelper2.service.getChangeables((WTChangeRequest2) changeObj,
	 * true);</br>
	 * case ACTIVITY_CHANGENOTICE: return
	 * ChangeHelper2.service.getChangeablesBefore((WTChangeOrder2) changeObj, true);
	 * </br>
	 * case ACTIVITY_CHANGEACTIVITY: return
	 * ChangeHelper2.service.getChangeablesBefore((WTChangeActivity2) changeObj,
	 * true); </br>
	 * case ACTIVITY_PROMOTIONREQUEST: return
	 * MaturityHelper.service.getPromotionTargets((PromotionNotice) changeObj);</br>
	 */
	@Override
	public QueryResult getAffectedObjects(WTObject changeObj) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#getAffectedObjects -->", " changeObj: ", changeObj);
		String strType = getChangeTypeString(changeObj);
		checkAndWriteDebug(Debuggable.LINE, "#getAffectedObjects -->", " strType: ", strType);
		try {
			switch (strType) {
			case EnerSysService.ACTIVITY_CHANGEREQUEST:
				return ChangeHelper2.service.getChangeables((WTChangeRequest2) changeObj, true);
			case EnerSysService.ACTIVITY_CHANGENOTICE:
				return ChangeHelper2.service.getChangeablesBefore((WTChangeOrder2) changeObj, true);
			case EnerSysService.ACTIVITY_CHANGEACTIVITY:
				return ChangeHelper2.service.getChangeablesBefore((WTChangeActivity2) changeObj, true);
			case EnerSysService.ACTIVITY_PROMOTIONREQUEST:
				return MaturityHelper.service.getPromotionTargets((PromotionNotice) changeObj, true);
			case EnerSysService.ACTIVITY_DEVIATION:
				return ChangeHelper2.service.getChangeables((WTVariance) changeObj, true); // Jira: 518
			case EnerSysService.ACTIVITY_PROBLEMREPORT:
				return ChangeHelper2.service.getChangeables((WTChangeIssue) changeObj, true);
			case EnerSysService.ACTIVITY_DOCUMENTAPPROVAL:
				return ChangeHelper2.service.getChangeables((WTChangeReview) changeObj, true);
			}
			throw new WTException(CLASSNAME + ".getAffectedObjects() - Type not Found!! - " + changeObj);
		} finally {
			checkAndWriteDebug(Debuggable.END, "#getAffectedObjects");
		}
	}

	/**
	 * Jira - 641 Used in Admin CR WF
	 * 
	 * Sets all latest affected objects to Released states
	 */
	@Override
	public void restoreStateMapOfAllLatestAffObjects(WTObject pbo, WTKeyedHashMap map) {
		checkAndWriteDebug(Debuggable.START, "#restoreStateMapOfAllLatestAffObjects", " pbo: ", pbo, " map: ", map);
		if (pbo != null) {
			try {
				WTSet storedAffObjKeySet = map.wtKeySet();
				Iterator<?> storedAffObjKeySetItr = storedAffObjKeySet.persistableIterator();
				while (storedAffObjKeySetItr.hasNext()) {
					WTObject storedAffObj = (WTObject) storedAffObjKeySetItr.next();
					// Checking if the Stored item on KeyMap is actually on the Affect Object List.
					if (storedAffObj instanceof Iterated) {
						WTObject latestIteration = (WTObject) VersionControlHelper.service
								.getLatestIteration((Iterated) storedAffObj, false);
						if (!WorkInProgressHelper.isCheckedOut((Workable) latestIteration)) {
							String stateToCheck = (String) map.get(storedAffObj);
							if (stateToCheck != null && !stateToCheck.isEmpty()) {
								EnerSysHelper.service.setState(latestIteration, stateToCheck);
							}
						} else {
							// Item is checked out!
						}
					}
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		checkAndWriteDebug(Debuggable.END, "#restoreStateMapOfAllLatestAffObjects");
	}

	/**
	 * Jira - 641 Used in Admin CR WF
	 * 
	 * calculates initial states of all affected objects
	 */
	@Override
	public WTKeyedHashMap returnInitialStateMapOfAllAffectedObjects(WTObject pbo, WTKeyedHashMap map) {
		checkAndWriteDebug(Debuggable.START, "#returnInitialStateMapOfAllAffectedObjects", " map: ", map);
		WTKeyedHashMap retMap = new WTKeyedHashMap();
		try {
			if (pbo != null) {
				WTHashSet hs = getAllAffectedObjects(pbo);
				Iterator<?> f = hs.persistableIterator();
				while (f.hasNext()) {
					WTObject obj = (WTObject) f.next();
					String currentState = (String) map.get(obj);
					if (currentState != null && !currentState.isEmpty()) {
						retMap.put(obj, getInitialState(currentState));
					}
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#returnInitialStateMapOfAllAffectedObjects", " retMap: ", retMap);
		return retMap;
	}

	/**
	 * Critical method, used to map released states to corresponding in-work states.
	 */
	@Override
	public String getInitialState(String currentState) {
		checkAndWriteDebug(Debuggable.START, "#getInitialState -->", " currentState: ", currentState);

		String suggestedInitialState = currentState;
		if (currentState != null) {
			if (currentState.equalsIgnoreCase(EnerSysService.STATE_INWORK)
					|| currentState.equalsIgnoreCase(EnerSysService.STATE_A_RELEASE_CONCEPT)
					|| currentState.equalsIgnoreCase(EnerSysService.STATE_RELEASED)) {
				suggestedInitialState = "INWORK";
			} else if (currentState.equalsIgnoreCase(EnerSysService.STATE_B_RELEASE_CONCEPT)) {
				suggestedInitialState = "B_INWORK";
			} else if (currentState.equalsIgnoreCase(EnerSysService.STATE_C_RELEASE_CONCEPT)) {
				suggestedInitialState = "C_INWORK";
			} else if (currentState.equalsIgnoreCase(EnerSysService.STATE_PRODUCTION_RELEASED)) {
				suggestedInitialState = "PRODUCTION_INWORK";
			}
			// TODO: Write Default case
		}
		checkAndWriteDebug(Debuggable.END, "#getInitialState -->", " suggestedInitialState: ", suggestedInitialState);
		return suggestedInitialState;
	}

	@Override
	public HashSet<String> getSetOfDistinctStates(WTObject pbo) {
		checkAndWriteDebug(Debuggable.START, "#getSetOfDistinctStates -->", " pbo: ", pbo);
		HashSet<String> retHM = new HashSet<>();
		try {
			QueryResult qr = getAffectedObjects(pbo);
			checkAndWriteDebug(Debuggable.LINE, "#getSetOfDistinctStates -->", " qr.size(): ", qr.size());
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof LifeCycleManaged) {
					// GET THE CURRENT STATE
					retHM.add(((LifeCycleManaged) obj).getLifeCycleState().toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#getSetOfDistinctStates -->", " retHM: ", retHM);
		}
		return retHM;
	}

	@Override
	public boolean setStateAllAffectedObjects(WTObject pbo, String toState) {
		checkAndWriteDebug(Debuggable.START, "#setStateAllAffectedObjects -->", " pbo: ", pbo, " toState: ", toState);
		boolean retBool = true;
		try {
			if (pbo != null && toState != null) {
				WTHashSet hs = getAllAffectedObjects(pbo);
				Iterator<?> f = hs.persistableIterator();
				while (f.hasNext()) {
					WTObject obj = (WTObject) f.next();
					checkAndWriteDebug(Debuggable.LINE, "#setStateAllAffectedObjects -->", " Captured Changeables: ",
							obj);
					EnerSysHelper.service.setState(obj, toState);
				}
			}
		} catch (Exception e) {
			retBool = false;
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#setStateAllAffectedObjects -->", " retBool: ", retBool);
		}
		return retBool;
	}

	@Override
	public boolean setStateAllResultingObjects(WTObject pbo, String toState) {
		checkAndWriteDebug(Debuggable.START, "#setStateAllResultingObjects -->", " pbo: ", pbo, " toState: ", toState);
		boolean retBool = true;
		try {
			if (pbo != null && toState != null) {
				WTHashSet hs = getAllResultingObjects(pbo);
				Iterator<?> f = hs.persistableIterator();
				while (f.hasNext()) {
					WTObject obj = (WTObject) f.next();
					checkAndWriteDebug(Debuggable.LINE, "#setStateAllResultingObjects -->", " Captured Changeables: ",
							obj);
					EnerSysHelper.service.setState(obj, toState);
				}
			}
		} catch (Exception e) {
			retBool = false;
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#setStateAllResultingObjects -->", " retBool: ", retBool);
		}
		return retBool;
	}

	@Override
	public WTKeyedHashMap returnCurrentStateMapOfAllResultingObjects(WTObject pbo) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#returnCurrentStateMapOfAllResultingObjects", " pbo: ", pbo);
		WTKeyedHashMap retMap = new WTKeyedHashMap();
		if (pbo != null) {
			WTHashSet hs = getAllResultingObjects(pbo);
			Iterator<?> f = hs.persistableIterator();
			while (f.hasNext()) {
				WTObject obj = (WTObject) f.next();
				String currentState = EnerSysHelper.service.getObjectState(obj);
				retMap.put(obj, currentState);
			}
		}
		checkAndWriteDebug(Debuggable.END, "#returnCurrentStateMapOfAllResultingObjects", " retMap: ", retMap);
		return retMap;
	}

	@Override
	public void restoreStateMapOfAllResultingObjects(WTObject pbo, WTKeyedHashMap map) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#restoreStateMapOfAllResultingObjects", " pbo: ", pbo, " map: ", map);
		if (pbo != null) {
			WTHashSet hs = getAllResultingObjects(pbo);
			Iterator<?> f = hs.persistableIterator();
			while (f.hasNext()) {
				WTObject obj = (WTObject) f.next();
				String stateToCheck = (String) map.get(obj);
				if (stateToCheck != null && !stateToCheck.isEmpty()) {
					EnerSysHelper.service.setState(obj, stateToCheck);
				}
			}
		}
		checkAndWriteDebug(Debuggable.END, "#restoreStateMapOfAllResultingObjects");
	}

	@Override
	public WTHashSet getAllResultingObjects(WTObject changeObj) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#getAllResultingObjects -->", " changeObj: ", changeObj);
		String strType = getChangeTypeString(changeObj);
		checkAndWriteDebug(Debuggable.LINE, "#getAllResultingObjects -->", " strType: ", strType);
		WTHashSet a = new WTHashSet();
		try {
			switch (strType) {
			case EnerSysService.ACTIVITY_CHANGEREQUEST:
				// TODO: Not Supported
				a.addAll(ChangeHelper2.service.getChangeables((WTChangeRequest2) changeObj, true));
				return a;
			case EnerSysService.ACTIVITY_CHANGENOTICE:
				a.addAll(ChangeHelper2.service.getChangeablesAfter((WTChangeOrder2) changeObj, true));
				return a;
			case EnerSysService.ACTIVITY_CHANGEACTIVITY:
				a.addAll(ChangeHelper2.service.getChangeablesAfter((WTChangeActivity2) changeObj, true));
				return a;
			case EnerSysService.ACTIVITY_PROMOTIONREQUEST:
				a.addAll(MaturityHelper.service.getPromotionTargets((PromotionNotice) changeObj, true));
				return a;
			case EnerSysService.ACTIVITY_DOCUMENTAPPROVAL:
				a.addAll(ChangeHelper2.service.getChangeables((WTChangeReview) changeObj, true));
				return a;
			/*
			 * case EnerSysService.ACTIVITY_DEVIATION: // TODO: Not Supported
			 * a.addAll(ChangeHelper2.service.getChangeables((WTVariance) changeObj, true));
			 * // Added for Build v1.12 return a;
			 */
			default:
				throw new WTException(CLASSNAME + ".getAllResultingObjects() - Type not Found!! - " + changeObj);
			}

		} finally {
			checkAndWriteDebug(Debuggable.END, "#getAllResultingObjects");
		}
	}

	@Override
	public boolean setStateLatestVerAffectedObjects(WTObject pbo, String toState) {
		checkAndWriteDebug(Debuggable.START, "#setStateAffectedLatestVerObjects -->", " pbo: ", pbo, " toState: ",
				toState);
		boolean retBool = true;
		try {
			QueryResult qr = getAffectedObjects(pbo);
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				checkAndWriteDebug(Debuggable.LINE, "#setStateAffectedLatestVerObjects -->", " Captured Changeables: ",
						obj);
				if (obj instanceof LifeCycleManaged && obj instanceof RevisionControlled) {
					QueryResult qr2 = VersionControlHelper.service
							.allVersionsOf(((RevisionControlled) obj).getMaster());
					if (qr2 != null && qr2.hasMoreElements()) {
						EnerSysHelper.service.setState(qr2.nextElement(), toState);
					}
				}
			}
		} catch (Exception e) {
			retBool = false;
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#setStateAffectedLatestVerObjects -->", " retBool: ", retBool);
		}
		return retBool;
	}

	@Override
	public boolean setStateAffectedObjects(WTObject pbo, String toState) {
		checkAndWriteDebug(Debuggable.START, "#setStateAffectedObjects -->", " pbo: ", pbo, " toState: ", toState);
		boolean retBool = true;
		try {
			QueryResult qr = getAffectedObjects(pbo);
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				checkAndWriteDebug(Debuggable.LINE, "#setStateAffectedObjects -->", " Captured Changeables: ", obj);
				if (obj instanceof LifeCycleManaged) {
					EnerSysHelper.service.setState(obj, toState);
				}
			}
		} catch (Exception e) {
			retBool = false;
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#setStateAffectedObjects -->", " retBool: ", retBool);
		}
		return retBool;
	}

	@Override
	public String getReleasedState(String intialState) {
		checkAndWriteDebug(Debuggable.START, "#getReleasedState -->", " intialState: ", intialState);
		String retStr = "";
		// FOR DOCUMENT LIFECYCLE
		if (intialState.equalsIgnoreCase("INWORK") || intialState.equalsIgnoreCase("RELEASED")) {
			retStr = "RELEASED";
		} else if (intialState.equalsIgnoreCase("B_INWORK") || intialState.equalsIgnoreCase("B_RELEASE_CONCEPT")) {
			retStr = "B_RELEASE_CONCEPT";
		} else if (intialState.equalsIgnoreCase("C_INWORK") || intialState.equalsIgnoreCase("C_RELEASE_CONCEPT")) {
			retStr = "C_RELEASE_CONCEPT";
		} else if (intialState.equalsIgnoreCase("PRODUCTION_INWORK")
				|| intialState.equalsIgnoreCase("PRODUCTIONRELEASED")) {
			retStr = "PRODUCTIONRELEASED";
		}
		checkAndWriteDebug(Debuggable.END, "#getReleasedState -->", " retStr: ", retStr);
		return retStr;
	}

	@Override
	public String getAffectedObjectsName(WTObject pbo) throws WTException {
		StringBuilder sb = new StringBuilder();
		QueryResult qr = getAffectedObjects(pbo);
		while (qr.hasMoreElements()) {
			WTObject f = (WTObject) qr.nextElement();
			sb.append(EnerSysHelper.service.getName(f)).append("\n");
		}
		return sb.toString();
	}

	@Override
	public String getAffectedObjectsNumber(WTObject pbo) throws WTException {
		StringBuilder sb = new StringBuilder();
		QueryResult qr = getAffectedObjects(pbo);
		while (qr.hasMoreElements()) {
			WTObject f = (WTObject) qr.nextElement();
			sb.append(EnerSysHelper.service.getNumber(f)).append("\n");
		}
		return sb.toString();
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

	@Override
	public void setStateOfAssociatedCT(@NotNull WTChangeOrder2 changeNotice, @NotNull String toState) {
		try {

			QueryResult associatedCT = ChangeHelper2.service.getChangeActivities(changeNotice);
			while (associatedCT.hasMoreElements()) {
				WTChangeActivity2 changeActivity = (WTChangeActivity2) associatedCT.nextElement();
				EnerSysHelper.service.setState(changeActivity, toState);
			}

		} catch (WTException e) {
			e.printStackTrace();
		}
	}

	@Override
	public WTSet getAssociatedCT(@NotNull WTChangeOrder2 changeNotice, @NotNull State state) {
		WTSet changeActivitySet = new WTHashSet();
		try {

			QueryResult associatedCT = ChangeHelper2.service.getChangeActivities(changeNotice);
			while (associatedCT.hasMoreElements()) {
				WTChangeActivity2 changeActivity = (WTChangeActivity2) associatedCT.nextElement();
				if (state.equals(changeActivity.getLifeCycleState())) {
					changeActivitySet.add(changeActivity);
				}

			}

			if (LOGGER.isDebugEnabled()) {
				changeActivitySet.inflate();
				Iterator<?> baselineIterator = changeActivitySet.persistableIterator();
				while (baselineIterator.hasNext()) {
					Persistable persistable = (Persistable) baselineIterator.next();
					LOGGER.debug(EnerSysLogUtils.format(persistable));
				}
			}
		} catch (WTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub
		return changeActivitySet;
	}

	@Override
	public void copyAllAffectedObjectIntoResultingObject(WTChangeActivity2 changeActivity)
			throws WTPropertyVetoException {
		try {
			QueryResult affectedObjectQR = getAffectedObjects(changeActivity);
			WTSet affectedObjects = new WTHashSet();
			affectedObjects.addAll(affectedObjectQR);

			// QueryResult resultingObjectQR =
			WTSet resultingObjects = new WTHashSet();
			resultingObjects.addAll(getAllResultingObjects(changeActivity));

			affectedObjects.removeAll(resultingObjects);

			Vector<WTObject> vector = new Vector <WTObject> ();
			affectedObjects.inflate();

			Iterator<?> affectedObjectIterator = affectedObjects.persistableIterator();
			while (affectedObjectIterator.hasNext()) {
				WTObject affectedObject = (WTObject) affectedObjectIterator.next();
				// Link resulting object to the change task
				vector.add(affectedObject);
				// ChangeHelper2.service.getChangeablesAfter(changeActivity);
			}
			// Creating record in one go
			vector = ChangeHelper2.service.storeAssociations(ChangeRecord2.class, changeActivity, vector);

			// Navigate ChangeRecord2
			// Update attribute of release target update to CHANGE
			Iterator<?> changerecordItr = vector.iterator();
			Vector<WTObject> updatedVector = new Vector <WTObject> ();
			while (changerecordItr.hasNext()) {
				ChangeRecord2 changeRecord = (ChangeRecord2) changerecordItr.next();

				changeRecord.setTargetTransition(Transition.CHANGE);
				updatedVector.add(changeRecord);
				// PersistenceHelper.manager.modify(changeRecord);

			}
			// Saving updated one
			ChangeHelper2.service.saveChangeRecord(updatedVector);

			changeActivity = (WTChangeActivity2) PersistenceHelper.manager.refresh(changeActivity);
			PersistenceServerHelper.manager.update(changeActivity);

			// used for debug

			if (LOGGER.isDebugEnabled()) {
				WTSet resultingObj = getAllResultingObjects(changeActivity);
				resultingObj.inflate();
				Iterator<?> baselineIterator = resultingObj.persistableIterator();
				while (baselineIterator.hasNext()) {
					Persistable persistable = (Persistable) baselineIterator.next();
					LOGGER.debug(EnerSysLogUtils.format(persistable));
				}

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOGGER.error("Error occured while copying Objects from Affected to Resulting for changeActivity ");
			LOGGER.error(EnerSysLogUtils.format(changeActivity));
			e.printStackTrace();
		}

	}

	/**
	 * Used in CN WF
	 * 
	 * CURRENT_AFT_OBJS_STATE =
	 * ext.enersys.cm2.CM2Helper.service.returnCurrentStateMapOfNewlyAddedAffectedObjects(primaryBusinessObject);
	 */
	@Override
	public WTKeyedHashMap returnCurrentStateMapOfNewlyAddedAffectedObjects(WTObject pbo) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#returnCurrentStateMapOfNewlyAddedAffectedObjects", " pbo: ", pbo);
		WTKeyedHashMap retMap = new WTKeyedHashMap();
		if (pbo != null) {
			WTHashSet hs = getAllAffectedObjects(pbo);
			hs.removeAll(getAllResultingObjects(pbo));
			Iterator<?> f = hs.persistableIterator();
			while (f.hasNext()) {
				WTObject obj = (WTObject) f.next();
				String currentState = EnerSysHelper.service.getObjectState(obj);
				retMap.put(obj, currentState);
			}
		}
		checkAndWriteDebug(Debuggable.END, "#returnCurrentStateMapOfNewlyAddedAffectedObjects", " retMap: ", retMap);
		return retMap;
	}

	/**
	 * Jira - 641 Used in Admin CR WF
	 * 
	 * calculates initial states of all affected objects
	 */
	@Override
	public WTKeyedHashMap returnInitialStateMapOfNewlyAddedAffectedObjects(WTObject pbo, WTKeyedHashMap map) {
		checkAndWriteDebug(Debuggable.START, "#returnInitialStateMapOfNewlyAddedAffectedObjects", " map: ", map);
		WTKeyedHashMap retMap = new WTKeyedHashMap();
		try {
			if (pbo != null) {
				WTHashSet hs = getAllAffectedObjects(pbo);
				hs.removeAll(getAllResultingObjects(pbo));
				Iterator<?> f = hs.persistableIterator();
				while (f.hasNext()) {
					WTObject obj = (WTObject) f.next();
					String currentState = (String) map.get(obj);
					if (currentState != null && !currentState.isEmpty()) {
						retMap.put(obj, getInitialState(currentState));
					}
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#returnInitialStateMapOfNewlyAddedAffectedObjects", " retMap: ", retMap);
		return retMap;
	}

	/**
	 * Used in CN WF
	 * 
	 * ext.enersys.cm2.CM2Helper.service.restoreStateMapOfNewlyAddedAllAffectedObjects(primaryBusinessObject,CURRENT_AFT_OBJS_STATE);
	 */
	@Override
	public void restoreStateMapOfNewlyAddedAllAffectedObjects(WTObject pbo, WTKeyedHashMap map) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#restoreStateMapOfNewlyAddedAllAffectedObjects", " pbo: ", pbo, " map: ",
				map);
		if (pbo != null) {
			WTHashSet hs = getAllAffectedObjects(pbo);
			hs.removeAll(getAllResultingObjects(pbo));
			Iterator<?> f = hs.persistableIterator();
			while (f.hasNext()) {
				WTObject obj = (WTObject) f.next();
				String stateToCheck = (String) map.get(obj);
				if (stateToCheck != null && !stateToCheck.isEmpty()) {
					EnerSysHelper.service.setState(obj, stateToCheck);
				}
			}
		}
		checkAndWriteDebug(Debuggable.END, "#restoreStateMapOfNewlyAddedAllAffectedObjects");
	}

	// #ADO:14768
	@Override
	public boolean checkViewAndContainer(WTObject changeObj) throws WTException {
		// TODO Auto-generated method stub
		LOGGER.debug("checkView method started");
		WTHashSet hs = getAllAffectedObjectsForMCNCreation(changeObj);
		Iterator<?> f = hs.persistableIterator();
		while (f.hasNext()) {
			WTObject obj = (WTObject) f.next();
			if (obj instanceof WTPart) {
				WTPart affectedPart = (WTPart) obj;
				QueryResult equilaentParts = PersistenceHelper.manager.navigate(affectedPart,
						EquivalenceLink.DOWNSTREAM_ROLE, EquivalenceLink.class);
				while (equilaentParts.hasMoreElements()) {
					WTPart equilaentpart = (WTPart) equilaentParts.nextElement();
					LOGGER.debug("affectedPart=" + equilaentpart.getNumber());
					String view = equilaentpart.getViewName();
					LOGGER.debug("view" + view);

					WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
					OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);

					String preferenceValue = (String) PreferenceHelper.service.getValue(
							"/ext/enersys/ENERSYS_MCN_PREFERENCES/MCNCONTEXTPREFERENCE",
							PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);

					LOGGER.debug("Preference Value: " + preferenceValue);
					LOGGER.debug("Affected Part Container Name: " + affectedPart.getContainerName());

					if (preferenceValue == null || preferenceValue.isEmpty()) {
						LOGGER.warn("Preference value is null or empty");
						return false;
					}

					// Split the preference value into context names
					String[] contextNames = preferenceValue.split(";");

					// Convert array to a HashSet for faster lookup (optional but recommended if
					// this check happens often)
					Set<String> contextSet = new HashSet<>(Arrays.asList(contextNames));

					// Check if the affected part's container name is in the context set
					if (!view.equalsIgnoreCase("Design") && contextSet.contains(affectedPart.getContainerName())) {
						LOGGER.debug("Container Name Match Found.");
						return true;
					}
				}
			}

		}
		return false;

	}

	// #ADO:14768
	@Override
	public boolean isECNStandalone(WTObject pbo) throws WTException {
		boolean isStandalone = true;
		if (pbo instanceof WTChangeOrder2) {

			QueryResult changeRequests = ChangeHelper2.service.getChangeRequest((WTChangeOrder2) pbo);
			LOGGER.debug("changeRequests size=" + changeRequests.size());
			if (changeRequests.size() > 0) {
				isStandalone = false;
			}

		}
		LOGGER.debug("isStandalone=" + isStandalone);

		return isStandalone;

	}

	// #ADO:14768
	// Collecting all downstream part views in a set
	@Override
	public HashSet<String> getDownStreamPartViews(WTObject changeObj) throws WTException {
		// TODO Auto-generated method stub
		LOGGER.debug("#start - StandardCM2Service - getDownStreamPartViews");

		WTHashSet hs = getAllAffectedObjectsForMCNCreation(changeObj);
		HashSet<String> downstreamPartViews = new HashSet<String>();
		Iterator<?> f = hs.persistableIterator();
		while (f.hasNext()) {
			WTObject obj = (WTObject) f.next();
			if (obj instanceof WTPart) {
				WTPart affectedPart = (WTPart) obj;
				QueryResult equilaentParts = PersistenceHelper.manager.navigate(affectedPart,
						EquivalenceLink.DOWNSTREAM_ROLE, EquivalenceLink.class);
				LOGGER.debug("#equilaentParts size: " + equilaentParts.size());
				while (equilaentParts.hasMoreElements()) {
					WTPart equilaentpart = (WTPart) equilaentParts.nextElement();
					String view = equilaentpart.getViewName();

					LOGGER.debug("Equivalent Part View: " + view);

					WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
					OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);

					String preferenceValue = (String) PreferenceHelper.service.getValue(
							"/ext/enersys/ENERSYS_MCN_PREFERENCES/MCNCONTEXTPREFERENCE",
							PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);

					LOGGER.debug("Preference Value: " + preferenceValue);
					LOGGER.debug("Affected Part Container Name: " + affectedPart.getContainerName());

					if (preferenceValue != null || !preferenceValue.isEmpty()) {
						LOGGER.warn("Preference value is not null or empty");
						// Split the preference value into context names
						String[] contextNames = preferenceValue.split(";");

						// Convert array to a HashSet for faster lookup (optional but recommended if
						// this check happens often)
						Set<String> contextSet = new HashSet<>(Arrays.asList(contextNames));

						if (!view.equalsIgnoreCase("Design") && contextSet.contains(affectedPart.getContainerName())) {
							LOGGER.debug("Container Name Match Found.");
							LOGGER.debug("#Equivalent Part: " + equilaentpart.getDisplayIdentifier());
							LOGGER.debug("#Equivalent Part View Name: " + view);
							downstreamPartViews.add(view);
						}
					}

				}
			}
		}
		LOGGER.debug("#Set of Downstream Part Views: " + downstreamPartViews);
		LOGGER.debug("#end - StandardCM2Service - getDownStreamPartViews");
		return downstreamPartViews;
	}

	// ADO: 14769
	@Override
	public WTHashSet getAllAffectedObjectsForMCNCreation(WTObject changeObj) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#getAllAffectedObjectsForMCNCreation -->", " changeObj: ", changeObj);
		String strType = getChangeTypeString(changeObj);
		checkAndWriteDebug(Debuggable.LINE, "#getAllAffectedObjectsForMCNCreation -->", " strType: ", strType);
		WTHashSet a = new WTHashSet();
		try {
			switch (strType) {
			case EnerSysService.ACTIVITY_CHANGEREQUEST:
				LOGGER.debug("#ECR Number: " + ((WTChangeRequest2) changeObj).getNumber());
				QueryResult qr = ChangeHelper2.service.getChangeables((WTChangeRequest2) changeObj, true);
				while (qr.hasMoreElements()) {
					Object obj = qr.nextElement();
					if (obj instanceof WTPart) {
						WTPart part = (WTPart) obj;
						LOGGER.debug("#ECR Affected Part: " + part.getDisplayIdentifier());
						QueryResult affectedChangeNotices = RelatedChangesQueryCommands
								.getRelatedAffectingChangeNotices((Changeable2) part);
						if (affectedChangeNotices.hasMoreElements()) {
							WTChangeOrder2 changeNotice = (WTChangeOrder2) affectedChangeNotices.nextElement();
							LOGGER.debug("#ECN Number: " + changeNotice.getNumber());
							a.addAll(ChangeHelper2.service.getChangeablesBefore(changeNotice, true));
							break;
						}
					}
				}
				return a;
			case EnerSysService.ACTIVITY_CHANGENOTICE:
				a.addAll(ChangeHelper2.service.getChangeablesAfter((WTChangeOrder2) changeObj, true));
				a.addAll(ChangeHelper2.service.getChangeablesBefore((WTChangeOrder2) changeObj, true));
				return a;
			case EnerSysService.ACTIVITY_CHANGEACTIVITY:
				a.addAll(ChangeHelper2.service.getChangeablesAfter((WTChangeActivity2) changeObj, true));
				a.addAll(ChangeHelper2.service.getChangeablesBefore((WTChangeActivity2) changeObj, true));
				return a;
			case EnerSysService.ACTIVITY_PROMOTIONREQUEST:
				a.addAll(MaturityHelper.service.getPromotionTargets((PromotionNotice) changeObj, true));
				return a;

			case EnerSysService.ACTIVITY_DEVIATION:
				a.addAll(ChangeHelper2.service.getChangeables((WTVariance) changeObj, true));
				return a;

			default:
				throw new WTException(
						CLASSNAME + ".getAllAffectedObjectsForMCNCreation() - Type not Found!! - " + changeObj);
			}

		} finally {
			checkAndWriteDebug(Debuggable.END, "#getAllAffectedObjectsForMCNCreation");
		}
	}
}
