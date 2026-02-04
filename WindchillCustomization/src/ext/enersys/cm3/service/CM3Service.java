package ext.enersys.cm3.service;

import java.util.LinkedHashMap;
import java.util.Set;

import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.change2.WTChangeOrder2;
import wt.fc.WTObject;
import wt.fc.collections.WTHashSet;
import wt.inf.container.WTContainer;
import wt.lifecycle.State;
import wt.maturity.Promotable;
import wt.maturity.PromotionNotice;
import wt.method.RemoteInterface;
import wt.team.TeamTemplate;
import wt.util.WTException;

@RemoteInterface
public interface CM3Service {
	public static final String LOCAL_CT_AFFECTED_OBJ_STR = "ENERSYS_CURRENT_CT_AF_OBJS";
	public static final String GLOBAL_CT_AFFECTED_OBJ_STR = "ENERSYS_CT_AF_OBJS";
	public static final String CRITICAL_PART_SELECTOR_ROLE_PREFERENCE = "/ext/enersys/CRITICAL_PART_ROLE_ORDER_CHANGE/CRITICAL_PART_SELECTOR";
	
	// Build v2.9 - 661
	public static final String LOCAL_CT_RESULTING_OBJ_STR = "ENERSYS_CURRENT_CT_RESULTING_OBJS";
	public static final String GLOBAL_CT_RESULTING_OBJ_STR = "ENERSYS_GLOBAL_CT_RESULTING_OBJS";

	// Build v2.5
	public void esgErpStatusAutoPromote(Promotable per, State startState, State targetState);

	public void esgErpStatusPerformAutoPromotion(PromotionNotice pn);

	// Build v2.9
	public void esgErpStatusPerformAutoPromotionForCN(WTChangeOrder2 cn);

	// Added as part of v2.1-HF1
	public LinkedHashMap<String, WTContainer> convertRoleContainerStringToRoleContainerObject(String eParticipantsStr) throws WTException;

	// Added as part of v2.1-HF1
	public String convertRoleContainerInfoToRoleContainerString(LinkedHashMap<String, WTContainer> eParticipants) throws WTException;

	public String performWizardValidations(NmCommandBean commandBean);

	public WTHashSet getParticipantsFromTeam(WTObject obj, String roleName);

	public void setParticipantStringOnObject(WTObject obj, String approvalStr, String notificationStr);

	public String getApproverRoleUserStr(WTObject pbo);

	public String getNotificationRoleUserStr(WTObject pbo);

	public Set<NmOid> getChangeAffectedObjects(NmCommandBean commandBean) throws WTException;

	public String getObjectTypeSelected(NmCommandBean commandBean);

	public String getTeamTemplateHTMLId(NmCommandBean commandBean);

	public boolean showSetupParticipantStep(NmCommandBean commandBean);

	public String getLifeCycleStateFromFirstObj(NmCommandBean commandBean) throws WTException;

	public boolean recomputeTeamTemplate(WTObject obj);

	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> convertRolesHashStringToRolesHashObject(String rolesHash) throws WTException;

	public String convertRolesHashObjectToRolesHashString(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> rolesHash) throws WTException;

	public String getParticipantRoleMapWizard(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap, TeamTemplate teamTemplateObj);

	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMap(NmCommandBean commandBean);
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMap(WTObject obj);
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getNotificationRoleMap(NmCommandBean commandBean);

	public TeamTemplate getSelectedTeamTemplateFromWizard(NmCommandBean commandBean);

	public String getSelectedTeamTemplateOidFromWizard(NmCommandBean commandBean);

	public String[] convertRoleStrToStrArray(String selectedRoleParticipantValue);

	public String convertRoleArrayToStr(Object[] arr);

	// Build 2.8
	public String getCriticalPartsInTask(WTObject pbo);

	// Build 2.9 - 661
	public Set<NmOid> getChangeTaskResultingObjects(NmCommandBean commandBean);
	
	public String gettypeSelection (String eParticipantsStr);

	LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getNotificationRoleMap(WTObject obj);
	
	LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMapForPromotionRequest(WTObject obj,
			NmCommandBean commandBean);
}
