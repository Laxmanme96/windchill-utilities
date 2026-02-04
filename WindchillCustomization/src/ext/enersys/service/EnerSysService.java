package ext.enersys.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import wt.lifecycle.State;


import com.ptc.core.businessRules.validation.RuleValidationResultSet;
import com.ptc.core.businessRules.validation.RuleValidationStatus;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.doc.WTDocument;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.WTObject;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.filter.NavigationCriteria;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerRef;
import wt.lifecycle.LifeCycleManaged;
import wt.method.RemoteInterface;
import wt.org.WTGroup;
import wt.org.WTOrganization;
import wt.org.WTPrincipal;
import wt.part.WTPart;
import wt.util.WTException;
import wt.vc.Mastered;
import wt.vc.views.View;

@RemoteInterface
public interface EnerSysService {
	
	//Build 3.8 CONSTANTS
	public static final String WTPART_RULE_SET_ACTIVATE = "/ext/enersys/DATA_VERIFICATION/IS_ACTIVE_WTPART";
	public static final String WTPART_RULE_SET = "/ext/enersys/DATA_VERIFICATION/BUSINESSRULE_SET_WTPART";
	
	public static final String EPM_RULE_SET_ACTIVATE = "/ext/enersys/DATA_VERIFICATION/IS_ACTIVE_EPM";
	public static final String EPM_RULE_SET = "/ext/enersys/DATA_VERIFICATION/BUSINESSRULE_SET_EPMDOC";
	
	public static final String WTDOC_RULE_SET_ACTIVATE = "/ext/enersys/DATA_VERIFICATION/IS_ACTIVE_WTDOC";
	public static final String WTDOC_RULE_SET = "/ext/enersys/DATA_VERIFICATION/BUSINESSRULE_SET_WTDOC";
	
	public static final String PROMO_RULE_SET_ACTIVATE = "/ext/enersys/DATA_VERIFICATION/IS_ACTIVE_PROMOTION";
	public static final String PROMO_RULE_SET = "/ext/enersys/DATA_VERIFICATION/BUSINESSRULE_SET_PROMOTION";
	
	public static final String CR_RULE_SET_ACTIVATE = "/ext/enersys/DATA_VERIFICATION/IS_ACTIVE_CR";
	public static final String CR_RULE_SET = "/ext/enersys/DATA_VERIFICATION/BUSINESSRULE_SET_CR";
	
	public static final String CN_RULE_SET_ACTIVATE = "/ext/enersys/DATA_VERIFICATION/IS_ACTIVE_CN";
	public static final String CN_RULE_SET = "/ext/enersys/DATA_VERIFICATION/BUSINESSRULE_SET_CN";
	
	public static final String CT_RULE_SET_ACTIVATE = "/ext/enersys/DATA_VERIFICATION/IS_ACTIVE_CT";
	public static final String CT_RULE_SET = "/ext/enersys/DATA_VERIFICATION/BUSINESSRULE_SET_CT";
	
	public static final String DEVIATION_RULE_SET_ACTIVATE = "/ext/enersys/DATA_VERIFICATION/IS_ACTIVE_DEVIATION";
	public static final String DEVIATION_RULE_SET = "/ext/enersys/DATA_VERIFICATION/BUSINESSRULE_SET_DEVIATION";
	public static final String WH_PROBLEM_RULE_SET_ACTIVATE = "/ext/enersys/DATA_VERIFICATION/IS_ACTIVE_PROBLEM";
	// Build v3.2
	public String getLongDescription(WTObject obj);

	public HashMap<String, List<String>> getAMLInformation(WTObject obj);

	public WTCollection getEnerSysSourcingContexts();

	// Build v3.1
	public boolean isPBOTrackSOP(WTObject obj);
	// ADO: 14768
	public boolean isPromotionTrackMFG(WTObject obj);
	// Build v2.12
	public boolean isReleasedOrObsoleteStateMinusAReleased(String stateToCheck);

	public boolean isReleasedStateMinusAReleased(String stateToCheck);

	public boolean isLatestVersionObjectInAReleasedOrObsoleteOrCancelledOrInwork(Mastered masteredObj);

	public boolean isLatestVersionObjectInAReleasedOrCancelledOrInwork(Mastered masteredObj);

	// Build v2.6
	public boolean isCADAsm(EPMDocument epm);

	// Build v2.3
	public boolean isObsoleted(LifeCycleManaged obj);

	public boolean isCADPart(EPMDocument epm);

	// Build v2.2
	public String getDownloadUrl() throws WTException;

	/*
	 * Method Declaration for v1.12
	 */
	public boolean isDocumentHavingClearance(WTDocument doc);

	public Set<String> getSetOfReleasedState();

	public boolean isReleasedState(String stateToCheck);

	public NavigationCriteria getSMDUtilityReleasedFilter();

	public NavigationCriteria getBOMUtilityReleasedFilter();

	/*
	 * Method Declaration for V1.11
	 */
	public boolean isNonReleasedState(String stateToCheck);

	public WTDocument getLatestDocumentIfExistingWithNumber(String WTDOCUMENT_NUMBER);

	public WTPart getLatestPartIfExistingWithNumberAndView(String WTPART_NUMBER, String viewName);

	public WTOrganization getEnerSysOrgContainer();

	/*
	 * Method Declaration/Signature
	 */
	public boolean isStringNumerical(String str);

	public boolean isUserInCurrentContextRole(Object role, WTObject wtObject, String userStr);

	public boolean isOrgAdmin(WTPrincipal currentUser) throws WTException;

	public boolean isSiteAdmin(WTPrincipal currentUser) throws WTException;

	public NavigationCriteria getNavigationCriteriaByName(String ncName);

	public Map<String, String> getAssociatedNavigationCriteria(WTPrincipal usr);

	public NavigationCriteria getNavigationCriteriaByID(String id);

	public NavigationCriteria generateDefaultNavCriteria();
	
	// v3.4 --- Added for ticket 7496 -- Enable SMD export for Plant BOM
	public NavigationCriteria changingViewOfNavCriteria(View view,NavigationCriteria tartgetPartNavCriteria);
	// v3.4 --- Added for ticket 7496 -- Enable SMD export for Plant BOM
	
	// TODO: ADD TO EXCEL SHEET, the below methods
	public String getObjectDisplayState(WTObject obj);

	public String getIterationInformation(Persistable perObj);

	public String getVersionInformation(Persistable perObj);

	public String getNumberInformation(Persistable perObj);

	public String getName(WTObject o) throws WTException;

	public String getNumber(WTObject pbo) throws WTException;

	public void setSessionPrincipal(String principalName);

	public void setState(Object obj, String toState) throws WTException;

	public String getObjectState(WTObject obj);

	public WTHashSet fetchUsersInTeamRole(Object role, WTObject wtObject);

	public void fetchUsersRecursivelyFromGroup(WTGroup group, WTHashSet usrSet) throws WTException;

	// Since Build 2.8
	public boolean isCriticalPart(Persistable per);

	// Since Build 2.9
	public boolean isQualityCriticalPart(Persistable per);

	// Build v2.9
	public boolean isAttributeApplicableOnObject(Persistable per, String internalName);

	// Since Build 2.10
	public String getEnumDisplayValue(String internalValue, String globalEnumeration);

	// Build v2.10
	public boolean setIsCRRequired(WTObject perObj, boolean valuetoSet) throws WTException;

	// Build 3.3 - To get Default value applied on the given attr of given type in
	// Type & Attr Manager
	public String getAttrDefaultValue(TypeIdentifier ti, String attrName);

	// Build 3.3 - update incoming attribute value to its default value from Type &
	// Attribute Manager on the incoming part
	public WTPart updatePartAttrWithDefaultValue(WTPart part, String attrInternalName);

	/*
	 * Constants
	 */
	public static final String SMD_UTILITY_RELEASED_FILTER_ORG_PREF_KEY = "/ext/enersys/PCP/SMD_RELEASED_FILTER_NAME"; // Added
																														// for
																														// Build
																														// v1.12
	public static final String BOM_UTILITY_RELEASED_FILTER_ORG_PREF_KEY = "/ext/enersys/PCP/BOM_RELEASED_FILTER_NAME"; // Added
																														// for
																														// Build
																														// v1.12

	public static final String ACTIVITY_CHANGEREQUEST = "CHANGEREQUEST";
	public static final String ACTIVITY_CHANGENOTICE = "CHANGENOTICE";
	public static final String ACTIVITY_CHANGEACTIVITY = "CHANGEACTIVITY";
	public static final String ACTIVITY_PROMOTIONREQUEST = "PROMOTIONREQUEST";
	public static final String ACTIVITY_DEVIATION = "DEVIATION"; // Added for Build v1.12
	public static final String ACTIVITY_PROBLEMREPORT = "PROBLEMREPORT";
	public static final String ACTIVITY_DOCUMENTAPPROVAL = "DOCUMENTAPPROVAL"; // Added for Document Approval - ES Migration
	public static final String STATE_INWORK = "INWORK";
	public static final String STATE_CANCELLED = "CANCELLED";
	public static final String STATE_OBSOLETE = "OBSOLETE";
	public static final String STATE_UNDERREVIEW = "UNDERREVIEW";
	public static final String STATE_END_OF_LIFE = "END_OF_LIFE";
	public static final String STATE_DRAFT = "DRAFT";
	public static final String STATE_RELEASED = "RELEASED";
	public static final String STATE_PRODUCTION_RELEASED = "PRODUCTIONRELEASED";
	public static final String STATE_PROTOTYPE_A = "PROTOTYPE_A";
	public static final String STATE_PROTOTYPE_B = "PROTOTYPE_B";
	public static final String STATE_PROTOTYPE_C = "PROTOTYPE_C";
	public static final String STATE_A_RELEASE_CONCEPT = "A_RELEASE_CONCEPT";
	public static final String STATE_B_RELEASE_CONCEPT = "B_RELEASE_CONCEPT";
	public static final String STATE_C_RELEASE_CONCEPT = "C_RELEASE_CONCEPT";
	public static final String STATE_B_INWORK = "B_INWORK";
	public static final String STATE_C_INWORK = "C_INWORK";
	public static final String STATE_PRODUCTION_INWORK = "PRODUCTION_INWORK";
	public static final String IBA_DATA_CLASSIFICATION = "ext.enersys.SIMPLE_DATA_CLASSIFICATION";
	public static final String STATE_WARRANTY_INACTIVE = "WARRANTY";

	// Build 3.3 Check if user belongs to the group at organization level
	public boolean IsUserPartOfGroup(WTPrincipal currentUser, String groupName);

	/**
	 * @param updatedPart     - Persistable for which String attribute need to be
	 *                        updated
	 * @param sourceAttribute - Internal name of source Attribute
	 * @param targetAttribute - Internal name of target Attribute
	 * @param override        - If true Simple copy values from Source to target
	 *                        attribute, overriding existing target attr values
	 */
	public void checkAndCopyAttribute(Persistable updatedPart, String sourceAttribute, String targetAttribute,
			boolean override);

	public Set<String> getAttributeValues(Set<String> attributeValues, Persistable updatedPart,
			String attrInternalName);

	public void updateAttribute(Persistable part, String[] attrValue, String attrName);

	public boolean isDataVerificationEnabled(WTPrincipal currentUser, WTContained object);

	public RuleValidationResultSet getValidationResults(Persistable persistable) throws WTException;
	//Build 3.8
	public RuleValidationResultSet getValidationResults(Persistable persistable, String delegate) throws WTException;

	public void updateAttribute(Persistable updatedPart, Object attrValue, String attrName);

	public int validateObject(Persistable part) throws WTException;

	public void sendEmailWithAttachment(String recipientAddress, String subject, String messageText, File aFile);

	public boolean hasResultsByStatus(RuleValidationStatus ruleValidationStatus, RuleValidationResultSet resultSet);

	public RuleValidationResultSet allowWarningOnce(Object[] rulesToSKip, RuleValidationResultSet resultSet,
			boolean setToFailure);

	public RuleValidationResultSet allowWarningOnce(Object[] rulesToSKip, RuleValidationResultSet resultSet);

	//Build 3.4
	public boolean areAllPartsOfSameView(List<WTPart> parts);

	public boolean isRestrictedContainer(WTContainer container);
	
	public boolean isAllowedContainerforRestricted(WTContainer restrictedContainer , WTContained currentObj);
	
	public boolean isAuthorizedRestrictedContainer(WTContainer restrictedContainer,
			WTContainer unRestrictedContainer);
	
	//Build 3.4: ADO-7441
	public boolean isMFGViewPartAtHigherMaturityThanDesignPart(WTPart mfgEquivalentDesignPart, State mfgViewPartState);

	public Map<String, ArrayList<WTPart>> sortPart(Map<String, ArrayList<WTPart>> numberViewSortMap, WTPart equivalentPart);

	public ArrayList<WTPart> getSortedPart(Map<String, ArrayList<WTPart>> numberViewSortMap);

	public String getAttributeValues(Persistable obj, String interName) throws WTException;
	//Build 3.10: ADO-9108
	public boolean isValidationCriteriaHasExactType(UIValidationCriteria criteria, String toBeComparedTypeInternalName);
	
	//Build 3.10: ADO-9216
	public Set<String> getSelectedReleaseTargetStateSet(Set<NmOid> resultedItemSet, NmCommandBean commandBean);
	
	public String getDisplayNameForTransition (String internalNameOfTransition);
	
	public String getInternalNameForState (String displayNameOfState);
	
	public String getDisplayNameForState (String internalNameOfState);
	
	public void setAttributeValues(Persistable obj, String internalName, String attributeValue) throws WTException;
}
