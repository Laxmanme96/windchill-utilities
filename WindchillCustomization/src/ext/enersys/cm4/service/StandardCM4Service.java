package ext.enersys.cm4.service;

import java.beans.PropertyVetoException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.ptc.core.businessRules.feedback.RuleFeedbackMessage;
import com.ptc.core.businessRules.validation.RuleValidationResult;
import com.ptc.core.businessRules.validation.RuleValidationStatus;
import com.ptc.core.components.util.TimeZoneHelper;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.CreateOperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.uwgm.common.pdm.retriever.RevisionIterationInfoHelper;
import com.ptc.wvs.common.ui.VisualizationHelper;

import ext.enersys.businessRule.EnerSysDepedenceValidationRule;
import ext.enersys.businessRule.EnerSysDistributionTargetValidationRule;
import ext.enersys.businessRule.ImmediateChildStateValidationRule;
import ext.enersys.cm2.CM2Helper;
import ext.enersys.cm2.xml.service.EnerSysApprovalMatrixDefinition;
import ext.enersys.cm3.CM3Helper;
import ext.enersys.cm4.CM4ServiceUtility;
import ext.enersys.listeners.RestrictedContextBOMControlListener;
import ext.enersys.poc.utility.properties.ExtractorPropertyHelper;
import ext.enersys.qualitygateframework.utility.QualityGateUtility;
import ext.enersys.rest.integration.businessrulevalidations.EnerSysBusinessRulesHelper;
import ext.enersys.service.EnerSysHelper;
import ext.enersys.utilities.Debuggable;
import ext.enersys.utilities.EnerSysLogUtils;
import ext.enersys.utilities.EnerSysSoftTypeHelper;
import wt.change2.ChangeException2;
import wt.change2.ChangeHelper2;
import wt.change2.SubjectProduct;
import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeIssue;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeRequest2;
import wt.change2.WTChangeReview;
import wt.change2.WTVariance;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.doc.WTDocument;
import wt.doc.WTDocumentHelper;
import wt.doc.WTDocumentMaster;
import wt.enterprise.RevisionControlled;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.inf.container.OrgContainer;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleHistory;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.State;
import wt.log4j.LogR;
import wt.maturity.PromotionNotice;
import wt.org.OrganizationServicesHelper;
import wt.org.WTOrganization;
import wt.org.WTPrincipal;
import wt.org.WTPrincipalReference;
import wt.org.WTUser;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.preference.PreferenceClient;
import wt.preference.PreferenceHelper;
import wt.project.Role;
import wt.representation.Representation;
import wt.services.StandardManager;
import wt.session.SessionContext;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.team.Team;
import wt.team.TeamHelper;
import wt.team.TeamManaged;
import wt.util.WTException;
import wt.vc.Iterated;
import wt.vc.VersionControlHelper;

/**
 * Service defined to help with CM4.0 changes.<br>
 * Mainly revolving around SetupParticipant processing in wizard, delegate
 * processing & TeamTemplate processing.
 * 
 * @since Build v3.8
 * @author CGI Team
 *
 *         Modified the existing code in such way all data needed for the Change
 *         management Approval Matrix flow is fetched directly from the
 *         Attribute "ext.enersys.SETUP_PARTICIPANT_STR" of change management
 *         objects like (Change Request,Change Notice,Promotion
 *         Request,Deviation) , In order to ensure the future logic changes
 *         related to Approval matrix can be updated only in the
 *         "ext.enersys.SETUP_PARTICIPANT_STR" Attribute level.
 *
 */

public class StandardCM4Service extends StandardManager implements Serializable, CM4Service, Debuggable {
	private static final long serialVersionUID = -5734677344718851750L;
	private static final String CLASSNAME = StandardCM4Service.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(CLASSNAME);
	static final String FIRMWARE_CT = "WCTYPE|wt.change2.WTChangeActivity2|ext.enersys.firmwareChangeActivity";
	static final String FIRMWARE_CT2 = "WCTYPE|wt.change2.WTChangeActivity2|ext.enersys.firmwareChangeActivity|ext.enersys.firmwareChangeActivityRVReport";

	static final TypeIdentifier FIRMWARE_CT_TI = TypeIdentifierHelper.getTypeIdentifier(FIRMWARE_CT);
	static final TypeIdentifier FIRMWARE_CT_TI2 = TypeIdentifierHelper.getTypeIdentifier(FIRMWARE_CT2);

	public static final String CHANGETRANS_PREF_INTR_NAME = "/ext/enersys/ENERSYS_FIRMWARE_CHANGE_TRANSITION/ENERSYS_FIRMWARE_CHANGE_TRANSITION";
	private static final String PREFERENCE_SEPARATOR = ";";
	private HashSet<Persistable> allParentSet = new HashSet<Persistable>();

	public StandardCM4Service() {
	}

	/**
	 * This method is used to merge role data from approval matrix and user data
	 * from UI && the merged data is store/persist in the "Setup Participant"
	 * attribute on the change objects
	 * 
	 * @param setAttributeOnChangeObjectsWithoutUsers - "Setupparticipant Attribute
	 *                                                Value" without user details
	 * @param userDetails                             - User Details
	 * @param changeObj                               - Changed Objects
	 * @return String - "Setupparticipant Attribute Value" with user details
	 * 
	 **/

	@Override
	public void setAttributeOnAddingUserDetails(String setAttributeOnChangeObjectsWithoutUsers, String userDetails,
			WTObject changeObj) {
		checkAndWriteDebug(Debuggable.START, "#setAttributeOnAddingUserDetails");
		try {
			String approverUserRoleDetails = "";
			String notificationUserRoleDetails = "";
			String approverParticipantRoles = null;
			String notificationParticipantRoles = null;
			String approverUserRoles = null;
			String notificationUserRoles = null;
			LinkedList<String> setUpParticpantAttrList = new LinkedList<String>();
			LOGGER.debug("setAttributeOnChangeObjectsWithoutUsers:" + setAttributeOnChangeObjectsWithoutUsers);
			LOGGER.debug("userDetails:" + userDetails);
			LOGGER.debug("changeObj:" + changeObj.getIdentity());
			if (setAttributeOnChangeObjectsWithoutUsers != null && userDetails != null
					&& !setAttributeOnChangeObjectsWithoutUsers.isEmpty() && !userDetails.isEmpty()
					&& changeObj != null) {
				String[] particpantRoles = setAttributeOnChangeObjectsWithoutUsers.split(APPNOTIF_DELIMITER);
				approverParticipantRoles = particpantRoles[0];
				if (particpantRoles.length > 1)
					notificationParticipantRoles = particpantRoles[1];
				String[] userRoles = userDetails.split(APPNOTIFUSER_DELIMITER);
				approverUserRoles = userRoles[0];
				if (particpantRoles.length > 1)
					notificationUserRoles = userRoles[1];
				if (approverParticipantRoles != null && approverUserRoles != null) {
					approverUserRoleDetails = addingUserRolesDetails(approverParticipantRoles, approverUserRoles);
					LOGGER.debug("approverUserRoleDetails :" + approverUserRoleDetails);
					setUpParticpantAttrList = changingSetUpPartiAttrMultiValue(setUpParticpantAttrList,
							approverUserRoleDetails, APPROVERROLES_PREFIX);
					LOGGER.debug("setUpParticpantAttrList :" + setUpParticpantAttrList);

				}
				if (notificationParticipantRoles != null && notificationUserRoles != null) {
					notificationUserRoleDetails = addingUserRolesDetails(notificationParticipantRoles,
							notificationUserRoles);
					setUpParticpantAttrList = changingSetUpPartiAttrMultiValue(setUpParticpantAttrList,
							notificationUserRoleDetails, NOTIFICATIONROLES_PREFIX);
				}
				String[] setUpParticpantAttr = convertSetUpPartiAttrToArray(setUpParticpantAttrList);
				checkAndWriteDebug(Debuggable.LINE,
						"#setAttributeOnAddingUserDetails : Calling setParticipantAttributeOnObject() Storing Attribute in Change Object");
				setParticipantAttributeOnObject(changeObj, setUpParticpantAttr);
			} else {
				LOGGER.error(
						"The Error in setAttributeOnAddingUserDetails () method while fetching User Details and Storing in "
								+ SETUP_PARTICIPANTS_ATTR + " Attribute");
				throw new WTException(
						"The Error in addingUserRolesDetails () method while fetching User Details and Storing in "
								+ SETUP_PARTICIPANTS_ATTR + " Attribute");
			}
		} catch (Exception e) {
			LOGGER.error(
					"The Error in setAttributeOnAddingUserDetails () method while fetching User Details and Storing in "
							+ SETUP_PARTICIPANTS_ATTR + " Attribute");
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#setAttributeOnAddingUserDetails");
	}

	private LinkedList<String> changingSetUpPartiAttrMultiValue(LinkedList<String> setUpParticpantAttrList,
			String setAttrSetupParticipant, String approverFlag) {
		checkAndWriteDebug(Debuggable.START, "#changingSetUpPartiAttrMultiValue");
		String[] orderWiseRoles = setAttrSetupParticipant.split(ORDERROLES_DELIMITER);
		for (String currentOrderRoleData : orderWiseRoles) {
			String currentOrderRoleDataArr[] = currentOrderRoleData.split(EQUALROLES_DELIMITER);
			String orderValue = currentOrderRoleDataArr[0];
			for (String currentRoleData : currentOrderRoleDataArr[1].split(SPLITROLENOS_DELIMITER)) {
				setUpParticpantAttrList.add(orderValue + EQUALROLES_DELIMITER + currentRoleData
						+ SPLITROLESINFO_DELIMITER + APPNOTIF_DELIMITER + approverFlag);
			}
		}
		checkAndWriteDebug(Debuggable.LINE,
				"#changingSetUpPartiAttrMultiValue : OUTPUT : " + setUpParticpantAttrList.size());
		checkAndWriteDebug(Debuggable.END, "#changingSetUpPartiAttrMultiValue");
		return setUpParticpantAttrList;
	}

	public String[] convertSetUpPartiAttrToArray(LinkedList<String> setUpParticpantAttrList) {
		checkAndWriteDebug(Debuggable.START, "#convertSetUpPartiAttrToArray");
		checkAndWriteDebug(Debuggable.END, "#convertSetUpPartiAttrToArray");
		return setUpParticpantAttrList.toArray(new String[setUpParticpantAttrList.size()]);
	}

	public LinkedList<String> convertSetUpPartiAttrToList(String[] setUpParticpantAttr) {
		checkAndWriteDebug(Debuggable.START, "#convertSetUpPartiAttrToList");
		checkAndWriteDebug(Debuggable.END, "#convertSetUpPartiAttrToList");
		return new LinkedList<>(Arrays.asList(setUpParticpantAttr));
	}

	/**
	 * This method is used to add user details to the respective roles
	 * 
	 * @param setAttributeOnChangeObjectsWithoutUsers - "Setupparticipant Attribute
	 *                                                Value" without user details
	 * @param userDetails                             - User Details
	 * @return String - "Setupparticipant Attribute Value" with user details
	 * 
	 **/

	private String addingUserRolesDetails(String setAttributeOnChangeObjectsWithoutUsers, String userDetails)
			throws WTException {
		checkAndWriteDebug(Debuggable.START, "#addingUserRolesDetails");
		String roleUserDetails = "";
		String[] userDetailsArray = userDetails.split(SPLITROLENOS_DELIMITER);
		LOGGER.debug("userDetailsArray:" + userDetailsArray.length);
		for (String currentRoleDetail : userDetailsArray) {
			String[] userRoleValue = currentRoleDetail.split(SPLITROLESINFO_DELIMITER);
			LOGGER.debug("userRoleValue:" + userRoleValue.length);
			if (userRoleValue.length == 2) {
				String splitingExpression = userRoleValue[0] + SPLITROLESINFO_DELIMITER;
				LOGGER.debug("splitingExpression :" + splitingExpression);
				String[] roleDetailsArray = setAttributeOnChangeObjectsWithoutUsers.split(splitingExpression);
				LOGGER.debug("splitingExpression :" + roleDetailsArray.length);
				LOGGER.debug("roleDetailsArray:" + roleDetailsArray.length);
				if (splitingExpression.equalsIgnoreCase("DESIGN AUTHORITY%:%") && roleDetailsArray.length == 3) {
					if (!roleDetailsArray[0].contains("FIRMWARE"))
						roleUserDetails = roleDetailsArray[0] + splitingExpression + userRoleValue[1]
								+ SPLITROLESINFO_DELIMITER + roleDetailsArray[1] + userRoleValue[0]
								+ SPLITROLESINFO_DELIMITER + roleDetailsArray[2];
					else
						roleUserDetails = roleDetailsArray[0] + splitingExpression + roleDetailsArray[1]
								+ splitingExpression + userRoleValue[1] + SPLITROLESINFO_DELIMITER
								+ roleDetailsArray[2];
					LOGGER.debug("#roleUserDetails: " + roleUserDetails);
					setAttributeOnChangeObjectsWithoutUsers = roleUserDetails;
				} else if (roleDetailsArray.length == 2) {
					roleUserDetails = roleDetailsArray[0] + splitingExpression + userRoleValue[1]
							+ SPLITROLESINFO_DELIMITER + roleDetailsArray[1];
					setAttributeOnChangeObjectsWithoutUsers = roleUserDetails;
					LOGGER.debug(roleUserDetails);
				} else {
					throw new WTException(
							"The Error in loopingThroughRolesDetails () method while fetching User Details and Storing in "
									+ SETUP_PARTICIPANTS_ATTR + " Attribute : userDetails : " + userDetails
									+ " : setAttributeOnChangeObjectsWithoutUsers : "
									+ setAttributeOnChangeObjectsWithoutUsers);
				}
			} else {
				LOGGER.error("The Error in addingUserRolesDetails () method while fetching User Details and Storing in "
						+ SETUP_PARTICIPANTS_ATTR + " Attribute : userDetails : " + userDetails
						+ " : setAttributeOnChangeObjectsWithoutUsers : " + setAttributeOnChangeObjectsWithoutUsers);
				throw new WTException(
						"The Error in addingUserRolesDetails () method while fetching User Details and Storing in "
								+ SETUP_PARTICIPANTS_ATTR + " Attribute : userDetails : " + userDetails
								+ " : setAttributeOnChangeObjectsWithoutUsers : "
								+ setAttributeOnChangeObjectsWithoutUsers);
			}
		}
		checkAndWriteDebug(Debuggable.LINE, "#addingUserRolesDetails : OUTPUT : " + roleUserDetails);
		checkAndWriteDebug(Debuggable.END, "#addingUserRolesDetails");
		return roleUserDetails;
	}

	/**
	 * This method is used to get the order values in Iterator<Integer>
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return Iterator<Integer> - Gives the order values in Iterator
	 * 
	 **/

	@Override
	public Iterator<Integer> getOrderSequenceValues(WTObject changeObj) {
		checkAndWriteDebug(Debuggable.START, "#getOrderSequenceValues");
		LinkedList<String> setUpParticipantAttList = getParticipantAttributeOnObject(changeObj);
		TreeSet<Integer> orderValues = new TreeSet<Integer>();
		if (setUpParticipantAttList.size() > 0) {
			for (String currentValue : setUpParticipantAttList) {
				String[] strArray = currentValue.split(EQUALROLES_DELIMITER);
				int orderSeq = DEFAULT_SEQUENCE_NUMBER;
				try {
					orderSeq = Integer.parseInt(strArray[0]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} finally {
					if (orderSeq < 1) {
						// DONT ADD ROLES FOR INVALID SEQUENCES!!!
						checkAndWriteDebug(Debuggable.LINE,
								"#getSequencesInOrder --> sequence has an invalid non-positive/or zero sequence (Not a Natural number), so, assigning default order : ",
								DEFAULT_SEQUENCE_NUMBER);
						orderSeq = DEFAULT_SEQUENCE_NUMBER;
					} else {
						orderValues.add(orderSeq);
					}
				}
			}
		}
		checkAndWriteDebug(Debuggable.LINE, "#getOrderSequenceValues : Output : " + orderValues.size());
		checkAndWriteDebug(Debuggable.END, "#getOrderSequenceValues");
		return orderValues.iterator();
	}

	/**
	 * This method is used to get the order values in String
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return String - Gives the order values in format 1,2
	 * 
	 **/
	@Override
	public String getOrderSequenceValues_String(WTObject changeObj) {
		checkAndWriteDebug(Debuggable.START, "#getOrderSequenceValues_String");
		Iterator<Integer> itr = getOrderSequenceValues(changeObj);
		String outputString = StringUtils.join(itr, COMMA_DELIM);
		checkAndWriteDebug(Debuggable.LINE, "#getOrderSequenceValues_String : Output : " + outputString);
		checkAndWriteDebug(Debuggable.END, "#getOrderSequenceValues_String");
		return outputString;
	}

	/**
	 * This method is used to get the Approver Roles for specific order
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return String - Specific RoleValues for given order number
	 * 
	 **/

	@Override
	public String gettingRolesForSpecificOrder(WTObject changeObj, int orderNumber) {
		checkAndWriteDebug(Debuggable.START, "#gettingRolesForSpecificOrder");
		LOGGER.debug("#start CM4Service gettingRolesForSpecificOrder");
		String ouputRoleValue = "";
		LinkedList<String> setUpParticipantAttList = getParticipantAttributeOnObject(changeObj);
		if (setUpParticipantAttList.size() > 0) {
			LinkedList<TreeSet<String>> roleValuesInOrder[] = gettingRoleValuesInTheOrderWithRO(
					setUpParticipantAttList);
			LinkedList<TreeSet<String>> approverRolesInOrder = roleValuesInOrder[0];
			if (approverRolesInOrder != null && approverRolesInOrder.size() > 0) {
				TreeSet<String> currentRoleValues = approverRolesInOrder.get(orderNumber - 1);
				if (currentRoleValues != null) {
					ouputRoleValue = StringUtils.join(currentRoleValues, COMMA_DELIM);
					LOGGER.debug("#ouputRoleValue: " + ouputRoleValue);
				}
			}
		}
		checkAndWriteDebug(Debuggable.LINE,
				"#gettingRolesForSpecificOrder : " + ouputRoleValue + " : For order number : " + orderNumber);
		checkAndWriteDebug(Debuggable.END, "#gettingRolesForSpecificOrder");
		return ouputRoleValue;
	}

	/**
	 * This method is used to get the Notification Roles for specific order
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return String - Specific RoleValues for given order number
	 * 
	 **/

	@Override
	public String gettingNotificationRolesForSpecificOrder(WTObject changeObj, int orderNumber) {
		checkAndWriteDebug(Debuggable.START, "#gettingNotificationRolesForSpecificOrder");
		String ouputRoleValue = "";
		LinkedList<String> setUpParticipantAttList = getParticipantAttributeOnObject(changeObj);
		LinkedList<TreeSet<String>> roleValuesInOrder[] = gettingRoleValuesInTheOrderWithRO(setUpParticipantAttList);
		LinkedList<TreeSet<String>> notificationRolesInOrder = roleValuesInOrder[1];
		if (notificationRolesInOrder != null && notificationRolesInOrder.size() > 0) {
			TreeSet<String> currentRoleValues = notificationRolesInOrder.get(orderNumber - 1);
			if (currentRoleValues != null) {
				ouputRoleValue = StringUtils.join(currentRoleValues, COMMA_DELIM);
			}
		}
		checkAndWriteDebug(Debuggable.LINE, "#gettingNotificationRolesForSpecificOrder : " + ouputRoleValue
				+ " : For order number : " + orderNumber);
		checkAndWriteDebug(Debuggable.END, "#gettingNotificationRolesForSpecificOrder");
		return ouputRoleValue;
	}

	/**
	 * This method is used to get the order values from "Set Up Participant"
	 * Attribute Values
	 * 
	 * @param setUpParticipantAttList - Values of "Set Up Participant" Attributes
	 * @return int - Integer - Max Value of Order in roles selected
	 * 
	 **/

	@Override
	public int getOrderSequenceValues(LinkedList<String> setUpParticipantAttList) {
		checkAndWriteDebug(Debuggable.START, "#getOrderSequenceValues");
		Set<Integer> orderValues = new HashSet<Integer>();
		for (String currentValue : setUpParticipantAttList) {
			String[] strArray = currentValue.split(EQUALROLES_DELIMITER);
			orderValues.add(Integer.parseInt(strArray[0]));
		}
		checkAndWriteDebug(Debuggable.LINE, "#getOrderSequenceValues : " + orderValues.size());
		checkAndWriteDebug(Debuggable.END, "#getOrderSequenceValues");
		return orderValues.size();
	}

	/**
	 * This method is used to set the attribute "Set Up Participant" Stored in the
	 * Change Object
	 * 
	 * @param per                          - WTObject - Change Objects
	 * @param selectedRoleParticipantValue - Values of selected roles and users
	 * @return per - WTObject - Change Objects
	 * 
	 **/
	@Override
	public Persistable setParticipantAttributeOnObject(Persistable per, String[] selectedRoleParticipantValue) {
		checkAndWriteDebug(Debuggable.START, "#setParticipantAttributeOnObject");
		try {
			PersistableAdapter pa = new PersistableAdapter(per, null, Locale.US, new CreateOperationIdentifier());
			pa.load(SETUP_PARTICIPANTS_ATTR);
			pa.set(SETUP_PARTICIPANTS_ATTR, selectedRoleParticipantValue);
			pa.persist();
			per = PersistenceHelper.manager.refresh(per);
		} catch (WTException e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#setParticipantAttributeOnObject");
		return per;
	}

	/**
	 * This method is used to get the attribute "Set Up Participant" Stored in the
	 * Change Object
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return LinkedList<String> - Value of Set Up Participant Attribute
	 * 
	 **/

	@Override
	public LinkedList<String> getParticipantAttributeOnObject(Persistable per) {
		checkAndWriteDebug(Debuggable.START, "#getParticipantAttributeOnObject");
		LinkedList<String> attrValue = new LinkedList<>();
		try {
			Object attrValueObj = EnerSysSoftTypeHelper.getAttributesValues(per, SETUP_PARTICIPANTS_ATTR);
			if (attrValueObj != null) {
				if (attrValueObj instanceof Object[]) {
					Object[] attrValueObjArr = (Object[]) attrValueObj;
					for (Object obj : attrValueObjArr) {
						if (obj instanceof String) {
							attrValue.add((String) obj);
						}
					}
				} else {
					attrValue.add((String) attrValueObj);
				}
			}
		} catch (WTException e) {
			LOGGER.error("The Error in getParticipantAttributeOnObject() method ");
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.LINE, "#getParticipantAttributeOnObject : Output " + attrValue);
		checkAndWriteDebug(Debuggable.END, "#getParticipantAttributeOnObject");
		return attrValue;
	}

	/**
	 * This method is used to check the change object is created before or after
	 * build V3.8
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return - Status of issetUPparticipantAttOldData
	 * 
	 **/
	@Override
	public boolean issetUPparticipantAttOldData(WTObject changeObj) {
		checkAndWriteDebug(Debuggable.START, "#setAttributeOnChangeObjects");
		boolean returnValue = true;
		Timestamp referenceTimeStamp = getOldDataReferenceTimeStamp();
		if (changeObj != null) {
			if (changeObj.getCreateTimestamp().compareTo(referenceTimeStamp) > 0) {
				returnValue = false;
				// New Data of SetUpParticipant Attribute.
			}
		}
		checkAndWriteDebug(Debuggable.LINE, "#setAttributeOnChangeObjects : Output " + returnValue);
		checkAndWriteDebug(Debuggable.END, "#setAttributeOnChangeObjects");
		return returnValue;
	}

	/**
	 * This Method generateRoleInSequenceToDisplayInUI used by Approval Tab In UI
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return - String - Role and Required/Optional Data in Format
	 *         "role1:user1,user2"
	 * 
	 **/

	@Override
	public String generateRoleInSequenceToDisplayInUI(WTObject changeObj) throws WTException {
		String returnString = "";
		checkAndWriteDebug(Debuggable.START, "#generateRoleInSequenceToDisplayInUI -->");
		LinkedList<String> setUpParticipantAttList = getParticipantAttributeOnObject(changeObj);
		LinkedList<String> rolesInOrder = new LinkedList<String>();
		if (setUpParticipantAttList.size() > 0) {
			int orderSize = getOrderSequenceValues(setUpParticipantAttList);
			for (int i = 1; i <= orderSize; i++) {
				TreeSet<String> currentRolesInOrder = new TreeSet<String>();
				for (String currentValue : setUpParticipantAttList) {
					String[] strArray = currentValue.split(SPLITROLESINFO_DELIMITER);
					String[] roleValuesArray = strArray[0].split(EQUALROLES_DELIMITER);
					// May fail Here
					if (roleValuesArray.length == 2 && strArray.length == 4) {
						String currentOrderValue = roleValuesArray[0];
						String currentRoleValue = roleValuesArray[1];
						String currentFlagValue = strArray[2];
						String currentApprover = strArray[3];
						if (currentOrderValue.equals(i + "") && currentApprover.contains(APPROVERROLES_PREFIX)) {
							String roleDisplayName = Role.toRole(currentRoleValue).getDisplay(Locale.US);
							if (currentFlagValue.equals(OPTIONAL_FLAGVALUE)) {
								roleDisplayName += OPTIONALROLEDISPLAY_PREFIX;
							}
							currentRolesInOrder.add(roleDisplayName);
						}
					}
				}

				if (currentRolesInOrder.size() > 0) {
					rolesInOrder.add(QUAOTE_ROLE_MAP_DELIM + StringUtils.join(currentRolesInOrder, COLON_ROLE_MAP_DELIM)
							+ QUAOTE_ROLE_MAP_DELIM);
				}
			}
		} else {
			return returnString;
		}
		returnString = StringUtils.join(rolesInOrder, COMMA_DELIM);
		checkAndWriteDebug(Debuggable.END, "#generateRoleInSequenceToDisplayInUI -->", " returnString:", returnString);
		return returnString;
	}

	/*
	 * LinkedList Size Max Order value TreeSet<String> Index Order Value
	 * TreeSet<String> Role count in that order
	 * 
	 * This method gettingRoleValuesInTheOrderWithRO gives roles data with
	 * REQURIED/OPTIONAL Data.
	 * 
	 */
	@Override
	public LinkedList<TreeSet<String>>[] gettingRoleValuesInTheOrderWithRO(LinkedList<String> setUpParticipantAttList) {
		checkAndWriteDebug(Debuggable.START, "#gettingRoleValuesInTheOrder");
		LOGGER.debug("#start CM4service gettingRoleValuesInTheOrder");
		LinkedList<TreeSet<String>>[] rolesInOrder = (LinkedList<TreeSet<String>>[]) new LinkedList[2];
		LinkedList<TreeSet<String>> approverRolesInOrder = new LinkedList<TreeSet<String>>();
		LinkedList<TreeSet<String>> notificationRolesInOrder = new LinkedList<TreeSet<String>>();
		if (setUpParticipantAttList.size() > 0) {
			int orderSize = getOrderSequenceValues(setUpParticipantAttList);
			LOGGER.debug("#order size: " + orderSize);
			for (int i = 1; i <= orderSize; i++) {
				TreeSet<String> currentRolesInOrderApprovers = new TreeSet<String>();
				TreeSet<String> currentRolesInOrderNotification = new TreeSet<String>();
				for (String currentValue : setUpParticipantAttList) {
					LOGGER.debug("#currentValue: " + currentValue);
					String[] strArray = currentValue.split(SPLITROLESINFO_DELIMITER);
					String[] roleValuesArray = strArray[0].split(EQUALROLES_DELIMITER);
					// May fail Here
					if (roleValuesArray.length == 2 && strArray.length == 4) {
						String currentOrderValue = roleValuesArray[0];
						LOGGER.debug("#currentOrderValue: " + currentOrderValue);
						String currentRoleValue = roleValuesArray[1];
						LOGGER.debug("#currentRoleValue: " + currentRoleValue);
						String currentApprover = strArray[3];
						LOGGER.debug("#currentApprover: " + currentApprover);
						String currentReqOptional = (strArray[2].equals(REQUIRED_FLAGVALUE)) ? REQUIRED_CONST
								: OPTIONAL_CONST;
						if (currentOrderValue.equals(i + "")) {
							if (currentApprover.contains(APPROVERROLES_PREFIX)) {
								LOGGER.debug("#currentRoleValue + COLON_ROLE_MAP_DELIM + currentReqOptional: "
										+ currentRoleValue + COLON_ROLE_MAP_DELIM + currentReqOptional);
								currentRolesInOrderApprovers
										.add(currentRoleValue + COLON_ROLE_MAP_DELIM + currentReqOptional);
							} else {
								currentRolesInOrderNotification
										.add(currentRoleValue + COLON_ROLE_MAP_DELIM + currentReqOptional);
							}
						}
					}
				}
				if (currentRolesInOrderApprovers.size() > 0) {
					approverRolesInOrder.add(currentRolesInOrderApprovers);
				} else if (currentRolesInOrderNotification.size() > 0) {
					notificationRolesInOrder.add(currentRolesInOrderNotification);
				}
			}
			rolesInOrder[0] = approverRolesInOrder;
			rolesInOrder[1] = notificationRolesInOrder;
		}
		checkAndWriteDebug(Debuggable.END, "#gettingRoleValuesInTheOrder -->", " rolesInOrder:",
				rolesInOrder.toString());
		return rolesInOrder;
	}

	/**
	 * This Method used to reference Timestamp to check the data is whether old data
	 * before Build V3.8
	 * 
	 **/
	private Timestamp getOldDataReferenceTimeStamp() {
		checkAndWriteDebug(Debuggable.START, "#getOldDataReferenceTimeStamp");
		Timestamp timestamp = null;
		try {
			String prefernceValue = TimeZoneHelper.getLocalTimeZone().getID();
			if (prefernceValue != null) {
				LOGGER.debug("The Value of Preference is " + prefernceValue);
				WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
				OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);
				String REFERENECE_OLDDATA_DATE_TIME = (String) PreferenceHelper.service
						.getValue(APPROVALFLOW_TAB_PREF_NAME, PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);
				String dateTimeString = REFERENECE_OLDDATA_DATE_TIME;
				SimpleDateFormat sdf = new SimpleDateFormat(REFERENECE_OLDDATA_DATE_FORMAT);
				sdf.setTimeZone(TimeZone.getTimeZone(prefernceValue));
				Date date = sdf.parse(dateTimeString);
				timestamp = new Timestamp(date.getTime());
			}

		} catch (WTException e) {
			LOGGER.error("The Error in getOldDataReferenceTimeStamp () method ");
			e.printStackTrace();
		} catch (ParseException e) {
			LOGGER.error("The Error in getOldDataReferenceTimeStamp () method ");
			e.printStackTrace();
		}
		if (timestamp == null) {
			LOGGER.error(
					"Error in method getOldDataReferenceTimeStamp() while Fetching prefernceValue for the Local Timezone, The Value is null");
		}
		checkAndWriteDebug(Debuggable.LINE, "#getOldDataReferenceTimeStamp : Output : " + timestamp);
		checkAndWriteDebug(Debuggable.END, "#getOldDataReferenceTimeStamp");
		return timestamp;
	}

	/**
	 * This method used to add the users to respective roles in the team instance of
	 * the respective changeobject , and delete the unnecessary roles without users
	 * from team instance this method is called from the workflow at the start
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return boolean- Status of recomputeTeamTemplate method execution
	 **/

	@Override
	public boolean recomputeTeamTemplate(@NotNull WTObject obj) {
		LOGGER.debug("#Start StandardCM4Service recomputeTeamTemplate");
		boolean ret = false;
		checkAndWriteDebug(Debuggable.START, "#recomputeTeamTemplate");
		try {
			HashMap<Role, Set<WTUser>>[] arrayOfRoleUserMap = getRoleUsersMap(obj);
			HashMap<Role, Set<WTUser>> approverRoleUserMap = arrayOfRoleUserMap[0];
			HashMap<Role, Set<WTUser>> notificationRoleUserMap = arrayOfRoleUserMap[1];
			checkAndWriteDebug(Debuggable.LINE, "#recomputeTeamTemplate Values - WTObject : " + obj);
			checkAndWriteDebug(Debuggable.LINE,
					"#recomputeTeamTemplate Values - approverRoleUserMap : " + approverRoleUserMap);
			checkAndWriteDebug(Debuggable.LINE,
					"#recomputeTeamTemplate Values - notificationRoleUserMap : " + notificationRoleUserMap);
			boolean hasApprover = false;
			if (approverRoleUserMap != null && notificationRoleUserMap != null
					&& (approverRoleUserMap.size() > 0 || notificationRoleUserMap.size() > 0)) {
				hasApprover = true;
			}
			checkAndWriteDebug(Debuggable.LINE,
					"#recomputeTeamTemplate Values - approverRoleUserMap.size() : " + approverRoleUserMap.size());
			checkAndWriteDebug(Debuggable.LINE, "#recomputeTeamTemplate Values - notificationRoleUserMap.size() : "
					+ notificationRoleUserMap.size());
			if (obj instanceof TeamManaged) {
				checkAndWriteDebug(Debuggable.LINE, "#recomputeTeamTemplate Values - TeamManaged : " + obj);
				WTPrincipal adminPrincipal = SessionHelper.manager.getAdministrator();
				WTPrincipal oldPrincipal = SessionContext.setEffectivePrincipal(adminPrincipal);
				try {
					Team teamInstanceObject = TeamHelper.service.getTeam((TeamManaged) obj);
					Map mp = teamInstanceObject.getRolePrincipalMap();
					LOGGER.debug("#mp getRolePrincipalMap: " + mp);

					OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer((WTContained) obj);
					if (hasApprover) {
						checkAndWriteDebug(Debuggable.LINE,
								"#recomputeTeamTemplate : Since no separate logic for Approver and Notification merging it ");
						// Since no separate logic for Approver and Notification merging it
						approverRoleUserMap.putAll(notificationRoleUserMap);
						checkAndWriteDebug(Debuggable.LINE,
								"#recomputeTeamTemplate Values - approverRoleUserMap Inside 2nd-Try Catch: "
										+ approverRoleUserMap);

						checkAndWriteDebug(Debuggable.LINE,
								"#recomputeTeamTemplate : Replace Approval Roles & Notifcation roleas");
						// Replace Approval Roles & Notifcation roleas
						deletingRolesTeamInstance(teamInstanceObject, mp, approverRoleUserMap);
						checkAndWriteDebug(Debuggable.LINE, "#recomputeTeamTemplate : Delete roles from Team Instance");
					}
					// Delete roles from Team Instance
					deleteTeamInstanceRoles(teamInstanceObject);
					checkAndWriteDebug(Debuggable.LINE, "#recomputeTeamTemplate : deleteTeamInstanceRoles");
					if (hasApprover) {
						// Use below API as it will create appropriate links
						TeamHelper.service.addRolePrincipalMap(approverRoleUserMap, teamInstanceObject);
						checkAndWriteDebug(Debuggable.LINE,
								"#recomputeTeamTemplate : Completed API as it will create appropriate links");
					}
					ret = true;
				} catch (Exception e) {
					LOGGER.error("The Error in recomputeTeamTemplate () method ");
					e.printStackTrace();
				} finally {
					SessionContext.setEffectivePrincipal(oldPrincipal);
				}
			}

		} catch (Exception e) {
			LOGGER.error("The Error in recomputeTeamTemplate () method ");
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.LINE, "#recomputeTeamTemplate : Output Data : " + ret);
		checkAndWriteDebug(Debuggable.END, "#recomputeTeamTemplate");
		return ret;
	}

	private void deletingRolesTeamInstance(@NotNull Team tm, @NotNull Map mp,
			@NotNull HashMap<Role, Set<WTUser>> newMap) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#deletingRolesTeamInstance");
		LOGGER.debug("##newMap: " + newMap);
		for (Role roleToAdd : newMap.keySet()) {
			// Delete the role only if there are users selected in the role
			LOGGER.debug("##roleToAdd: " + roleToAdd.getFullDisplay());
			if (mp.containsKey(roleToAdd)) {
				LOGGER.debug("##Removing " + roleToAdd.getFullDisplay());
				TeamHelper.service.deleteRole(roleToAdd, tm);
			}
		}
		checkAndWriteDebug(Debuggable.END, "#deletingRolesTeamInstance");
	}

	/**
	 * Delete Role map prior to loading values. Make sure minimum roles are
	 * maintained.
	 * 
	 * @param teamInstanceObject
	 * @throws WTException
	 */
	private void deleteTeamInstanceRoles(Team teamInstanceObject) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#deleteTeamInstanceRoles");
		Map mp = teamInstanceObject.getRolePrincipalMap();
		LOGGER.debug("#mp getRolePrincipalMap: " + mp);
		for (Object obj : mp.keySet()) {
			Role roleObj = (Role) obj;
			LOGGER.debug("#roleObj: " + roleObj.getFullDisplay());
			if (!ROLES_TO_PRESERVE_IN_TEAM.contains(roleObj)) {
				LOGGER.debug("##Removing " + roleObj.getFullDisplay());
				TeamHelper.service.deleteRole(roleObj, teamInstanceObject);
			}
		}
		checkAndWriteDebug(Debuggable.END, "#deleteTeamInstanceRoles");
	}

	/**
	 * This method used to get the role & user data for Notification roles &
	 * Approver roles The output is a HashMap<Role, Set<WTUser>>[] with index 0 --->
	 * has Approver roles and user details with index 1 ----> has Notification roles
	 * and user details
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return HashMap<Role,Set<WTUser>>[] - the role and user data for Notification
	 *         roles & Approver roles
	 **/
	@Override
	public HashMap<Role, Set<WTUser>>[] getRoleUsersMap(WTObject changeObj) {
		HashMap<Role, Set<WTUser>>[] arrayOfRoleUserMap = new HashMap[2];
		HashMap<Role, Set<WTUser>> approverRoleUserMap = new HashMap<>();
		HashMap<Role, Set<WTUser>> notificationRoleUserMap = new HashMap<>();

		LOGGER.debug("#start getRoleUsersMap");
		checkAndWriteDebug(Debuggable.START, "#getRoleUsersMap");

		try {
			LinkedList<String> setUpParticipantAttList = getParticipantAttributeOnObject(changeObj);
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer((WTContained) changeObj);
			if (setUpParticipantAttList.size() > 0) {
				for (String currentValue : setUpParticipantAttList) {
					LOGGER.debug("#getRoleUsersMap currentValue: " + currentValue);

					String[] strArray = currentValue.split(SPLITROLESINFO_DELIMITER);
					LOGGER.debug("#getRoleUsersMap strArray: " + strArray);

					String[] roleValuesArray = strArray[0].split(EQUALROLES_DELIMITER);
					LOGGER.debug("#getRoleUsersMap roleValuesArray: " + roleValuesArray);

					if (roleValuesArray.length == 2 && strArray.length == 4) {
						// String currentOrderValue = roleValuesArray[0];
						String currentRoleValue = roleValuesArray[1];
						LOGGER.debug("#getRoleUsersMap currentRoleValue: " + currentRoleValue);

						// String currentReqOptional = strArray[2];
						String currentApprover = strArray[3];
						LOGGER.debug("#getRoleUsersMap currentApprover: " + currentApprover);

						String userStringValue = strArray[1];
						LOGGER.debug("#getRoleUsersMap userStringValue: " + userStringValue);

						String[] usersArray = userStringValue.split(USERINFOSPLIT_DELIM);
						LOGGER.debug("#getRoleUsersMap usersArray: " + usersArray);

						Role roleToAdd = Role.toRole(currentRoleValue);
						if (usersArray.length > 0 && !userStringValue.equalsIgnoreCase(NOUSERASSIGNED)) {
							LOGGER.debug("#getRoleUsersMap users selected on setup participants");
							HashSet<WTUser> userList = new HashSet<>();
							for (String userName : usersArray) {
								WTUser userValue = OrganizationServicesHelper.manager.getUser(userName,
										orgContainer.getContextProvider());
								LOGGER.debug("##userName: " + userName);

								WTPrincipalReference refPrincipal = WTPrincipalReference
										.newWTPrincipalReference(userValue);
								if (!refPrincipal.isDisabled()) {
									userList.add(userValue);
								}
							}
							if (currentApprover.contains(APPROVERROLES_PREFIX)) {
								LOGGER.debug("#getRoleUsersMap Adding users to role");
								LOGGER.debug("#getRoleUsersMap roleToAdd: " + roleToAdd);
								LOGGER.debug("#getRoleUsersMap userList: " + userList);
								addUserToMap(approverRoleUserMap, roleToAdd, userList);
							} else {
								addUserToMap(notificationRoleUserMap, roleToAdd, userList);
							}
						}
					}
				}
				arrayOfRoleUserMap[0] = approverRoleUserMap;
				LOGGER.debug("#getRoleUsersMap approverRoleUserMap: " + approverRoleUserMap);

				arrayOfRoleUserMap[1] = notificationRoleUserMap;
			}
		} catch (Exception e) {
			LOGGER.error("The Error in getRoleUsersMap () method ");
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#getRoleUsersMap");
		LOGGER.debug("#getRoleUsersMap arrayOfRoleUserMap: " + arrayOfRoleUserMap);
		return arrayOfRoleUserMap;
	}

	/**
	 * This method used to get the role & user data for Notification roles &
	 * Approver roles
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return String - the role and user data for Notification roles & Approver
	 *         roles in String Format
	 * 
	 **/

	@Override
	public String[] getRoleUsersMapInString(WTObject changeObj) {
		String[] outputString = new String[2];
		Set<String> approverRoleUsersString = new HashSet<String>();
		Set<String> notificationRoleUsersString = new HashSet<String>();
		checkAndWriteDebug(Debuggable.START, "#getRoleUsersMapInString");
		try {
			LinkedList<String> setUpParticipantAttList = getParticipantAttributeOnObject(changeObj);
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer((WTContained) changeObj);
			if (setUpParticipantAttList.size() > 0) {
				for (String currentValue : setUpParticipantAttList) {
					String[] strArray = currentValue.split(SPLITROLESINFO_DELIMITER);
					String[] roleValuesArray = strArray[0].split(EQUALROLES_DELIMITER);
					if (roleValuesArray.length == 2 && strArray.length == 4) {
						String currentRoleValue = roleValuesArray[1];
						String roleUserStringValue = strArray[1].replace(NOUSERASSIGNED, "");
						roleUserStringValue = strArray[1].replace(USERINFOSPLIT_DELIM, COLON_ROLE_MAP_DELIM);
						roleUserStringValue = currentRoleValue + EQUALROLES_DELIMITER + roleUserStringValue;
						if (strArray[3].contains(APPROVERROLES_PREFIX)) {
							approverRoleUsersString.add(roleUserStringValue);
						} else {
							notificationRoleUsersString.add(roleUserStringValue);
						}
					}
				}
				if (approverRoleUsersString.size() > 0) {
					outputString[0] = StringUtils.join(approverRoleUsersString, COMMA_DELIM);
				}
				if (notificationRoleUsersString.size() > 0) {
					outputString[1] = StringUtils.join(notificationRoleUsersString, COMMA_DELIM);
				}
			}
		} catch (Exception e) {
			LOGGER.error("The Error in getRoleUsersMapInString () method ");
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.LINE, "#getRoleUsersMapInString : Output : " + outputString);
		checkAndWriteDebug(Debuggable.END, "#getRoleUsersMapInString");
		return outputString;
	}

	/**
	 * This method used to add role and user list to role Map
	 * 
	 * @param roleMap   - to which the role and user list needs to be added
	 * @param roleToAdd - role to be added
	 * @param userList  - User List needs to be added to role Map
	 * @return HashMap with Role and User List
	 **/

	private HashMap<Role, Set<WTUser>> addUserToMap(HashMap<Role, Set<WTUser>> roleMap, Role roleToAdd,
			HashSet<WTUser> userList) {
		checkAndWriteDebug(Debuggable.START, "#addUserToMap");
		if (!userList.isEmpty()) {
			if (roleMap.containsKey(roleToAdd)) {
				userList.addAll(roleMap.get(roleToAdd));
			}
			roleMap.put(roleToAdd, userList);
		}
		checkAndWriteDebug(Debuggable.END, "#addUserToMap");
		return roleMap;
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
	 * This method used to fetch the order , roles details from the approval matrix
	 * 
	 * @param rolesHash             - the Approvers roles details from approval
	 *                              matrix
	 * @param rolesNotificationHash - the Notification roles details from approval
	 *                              matrix
	 * @return data of Role and order details from Approval matrix without user
	 *         details
	 **/

	@Override
	public String setAttributeOnChangeObjectsWithoutUsers(
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> rolesHash,
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> rolesNotificationHash) {
		checkAndWriteDebug(Debuggable.START, "#setAttributeOnChangeObjectsWithoutUsers");
		String setAttrSetupParticipant = "";
		String approvedSetAttrSetupParticipant = "";
		String notificationSetAttrSetupParticipant = "";
		if (rolesHash != null) {
			approvedSetAttrSetupParticipant = getRoleDataOrderWise(rolesHash);
		}
		if (rolesNotificationHash != null) {
			notificationSetAttrSetupParticipant = getRoleDataOrderWise(rolesNotificationHash);
		}
		setAttrSetupParticipant = approvedSetAttrSetupParticipant + CM4Service.APPNOTIF_DELIMITER
				+ notificationSetAttrSetupParticipant;
		checkAndWriteDebug(Debuggable.LINE, "#setAttributeOnChangeObjectsWithoutUsers : " + setAttrSetupParticipant);
		checkAndWriteDebug(Debuggable.END, "#setAttributeOnChangeObjectsWithoutUsers");
		return setAttrSetupParticipant;
	}

	@Override
	public boolean isTeamRecomputeNeededFromUsers(WTObject obj) {
		String allRoles = null;
		try {
			try {
				allRoles = generateRoleInSequenceToDisplayInUI(obj);
				if (allRoles == null || allRoles.isEmpty()) {
					return true;
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
			// Step 1: Remove quotes from the input string
			allRoles = allRoles != null ? allRoles.replaceAll("\"", "") : "";
			String[] splitParts = allRoles.split("[:,]");
			ArrayList<String> formattedList = new ArrayList<>();
			ArrayList<String> currentRequiredList = new ArrayList<>();
			ArrayList<String> currentOptionalList = new ArrayList<>();
			ArrayList<String> tempCurrentRequiredList = new ArrayList<>();
			for (String part : splitParts) {
				formattedList.add(part.replace("(Opt.)", "").trim());
				if (part.trim().contains("(Opt.)")) {
					currentOptionalList.add(part.replace("(Opt.)", "").trim());
				} else {
					currentRequiredList.add(part.trim().trim());
				}
			}

			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> rolesHash = CM3Helper.service
					.getChangeApprovalRoleMap((WTObject) obj);
			ArrayList<String> requiredRoles = new ArrayList<>();
			ArrayList<String> optionalRoles = new ArrayList<>();
			// ---Iterate through main role map
			Iterator itMap = rolesHash.entrySet().iterator();
			while (itMap.hasNext()) {
				// --Get Entry Set with Role Name (KEY) and List of related participants (VALUE)
				Entry<String, Set<String>> roleEntry = (Entry<String, Set<String>>) itMap.next();
				String roleKey = roleEntry.getKey();
				String[] roleApproval = roleKey.split(EnerSysApprovalMatrixDefinition.COLON_ROLE_MAP_DELIM);
				String roleName = roleApproval[EnerSysApprovalMatrixDefinition.PARTICIPANT_ROLE_INDEX];
				String required = roleApproval[EnerSysApprovalMatrixDefinition.IS_REQUIRED_INDEX];
				if (required.equalsIgnoreCase(EnerSysApprovalMatrixDefinition.APPR_REQUIRED_VALUE)) {
					requiredRoles.add(Role.toRole(roleName).getDisplay().trim());
				} else {
					optionalRoles.add(Role.toRole(roleName).getDisplay().trim());
				}
			}
			tempCurrentRequiredList = currentRequiredList;
			for (String roleName : requiredRoles) {
				boolean isRoleAbsent = true;
				for (String currentRequredRoleName : currentRequiredList) {
					if (roleName.trim().equalsIgnoreCase(currentRequredRoleName.trim())) {
						isRoleAbsent = false;
						tempCurrentRequiredList.remove(currentRequredRoleName);
						break;
					}
				}
				if (isRoleAbsent) {
					return true;
				}
			}
			if (!tempCurrentRequiredList.isEmpty()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * This method used to store the roles data in order
	 * 
	 * @param rolesHash - the Approvers and Notification roles details from approval
	 *                  matrix
	 * @return data of Role in order after fetched from Approval matrix
	 **/

	private String getRoleDataOrderWise(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> rolesHash) {
		String setAttrSetupParticipant = "";
		checkAndWriteDebug(Debuggable.START, "#getRoleDataOrderWise");
		LinkedHashMap<Integer, String> orderWiseRoles = new LinkedHashMap<Integer, String>();
		for (Map.Entry<String, LinkedHashMap<String, WTContainer>> outerEntry : rolesHash.entrySet()) {
			String roleHashKey = outerEntry.getKey();
			String[] keys = roleHashKey.split(CM4Service.COLON_ROLE_MAP_DELIM);
			String currentRoleInfo = "";
			String roleStr = keys[CM4Service.ROLE_ORDER_INDEX];
			String reqStr = keys[CM4Service.REQUIRED_ORDER_INDEX];
			reqStr = reqStr.equals(CM4Service.REQUIRED_CONST.toUpperCase()) ? CM4Service.REQUIRED_FLAGVALUE
					: CM4Service.OPTIONAL_FLAGVALUE;
			String seqStr = keys[CM4Service.SEQUENCE_ORDER_INDEX];
			int seqInt = Integer.parseInt(seqStr);
			String containerInfo = "";
			LinkedHashMap<String, WTContainer> containerValues = outerEntry.getValue();
			for (Map.Entry<String, WTContainer> innerEntry : containerValues.entrySet()) {
				String containerValue = "";
				String innerKey = innerEntry.getKey();
				WTContainer container = innerEntry.getValue();
				String containerOid = container.getContainerReference().getObjectId().idAsString();
				String[] contkeys = innerKey.split(CM4Service.COLON_ROLE_MAP_DELIM);
				String containerName = contkeys[CM4Service.CONTAINER_ORDER_INDEX];
				containerValue = containerName + CM4Service.SPLITCONTAINEROIDINFO_DELIMITER + containerOid;
				containerInfo = containerValue + CM4Service.SPLITCONTAINERINFO_DELIMITER + containerInfo;
			}
			if (containerInfo.endsWith(CM4Service.SPLITCONTAINERINFO_DELIMITER)) {
				containerInfo = containerInfo.substring(0, containerInfo.length() - 2);
			}
			// Due to Length issue temporarily disabling the container data.
			// currentRoleInfo = roleStr + SPLITROLESINFO_DELIMITER + reqStr +
			// SPLITROLESINFO_DELIMITER + containerInfo;
			currentRoleInfo = roleStr + CM4Service.SPLITROLESINFO_DELIMITER + reqStr;
			if (orderWiseRoles.containsKey(seqInt)) {
				String previousRoleData = orderWiseRoles.get(seqInt);
				previousRoleData = previousRoleData + CM4Service.SPLITROLENOS_DELIMITER + currentRoleInfo;
				orderWiseRoles.put(seqInt, previousRoleData);
			} else {
				String previousRoleData = currentRoleInfo;
				orderWiseRoles.put(seqInt, previousRoleData);
			}
		}
		checkAndWriteDebug(Debuggable.LINE,
				"#getRoleDataOrderWise : The Size of Orderwise Safed Role " + orderWiseRoles.size());
		if (orderWiseRoles != null) {
			for (int i = 1; i <= orderWiseRoles.size(); i++) {
				String currentOrderWiseRoleValue = i + CM4Service.EQUALROLES_DELIMITER + orderWiseRoles.get(i);
				setAttrSetupParticipant = setAttrSetupParticipant + CM4Service.ORDERROLES_DELIMITER
						+ currentOrderWiseRoleValue;
			}
			if (setAttrSetupParticipant.startsWith(CM4Service.ORDERROLES_DELIMITER)) {
				setAttrSetupParticipant = setAttrSetupParticipant.substring(2, setAttrSetupParticipant.length());
			}
		}
		checkAndWriteDebug(Debuggable.LINE, "#getRoleDataOrderWise : Output Data  " + setAttrSetupParticipant);
		checkAndWriteDebug(Debuggable.END, "#getRoleDataOrderWise");
		return setAttrSetupParticipant;
	}

	/**
	 * This method used to get the role & user data for Notification roles only
	 * (Called from Workflow)
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return String - the role and user data for Notification roles only Format
	 *         role=user1:user2,role2=user1
	 **/

	@Override
	public String getNotificationRoleUserStr(WTObject changeObj) {
		checkAndWriteDebug(Debuggable.START, "#getNotificationRoleUserStr");
		String outputString = "";
		try {
			String[] outputStringArr = getRoleUsersMapInString(changeObj);
			if (outputStringArr.length == 2) {
				outputString = outputStringArr[1];
				if (outputString != null) {
					return outputString;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.LINE, "#getNotificationRoleUserStr : Output : " + outputString);
		checkAndWriteDebug(Debuggable.END, "#getNotificationRoleUserStr");
		return outputString;
	}

	/**
	 * This method used to get the role & user data for Approver roles only (Called
	 * from Workflow)
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return String - the role and user data for Approver roles only Format
	 *         role=user1:user2,role2=user1
	 **/

	@Override
	public String getApproverRoleUserStr(WTObject changeObj) {
		checkAndWriteDebug(Debuggable.START, "#getApproverRoleUserStr");
		String outputString = "";
		try {
			String[] outputStringArr = getRoleUsersMapInString(changeObj);
			if (outputStringArr.length == 2) {
				outputString = outputStringArr[0];
				if (outputString != null) {
					return outputString;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.LINE, "#getApproverRoleUserStr : Output : " + outputString);
		checkAndWriteDebug(Debuggable.END, "#getApproverRoleUserStr");
		return outputString;
	}

	/**
	 * This method used to check the given change object is a firmware change task
	 * and the task should be 2. Release the Verification Report" Since this is the
	 * only task without setup particpant page and approval flow in workflow
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * @return boolean - Status
	 **/

	@Override
	public boolean isReleaseVerifRepoTask(WTObject changeObj) {
		checkAndWriteDebug(Debuggable.START, "#isReleaseVerifRepoTask");
		boolean isReleaseVerifRepoTask = false;
		/*
		 * Logic for the New data if "2. Release the Verification Report" with type from
		 * build v 3.8 it is changed to type FIRMWARE_CT_TI2
		 */
		if (FIRMWARE_CT_TI2.equals(TypeIdentifierHelper.getType(changeObj))) {
			isReleaseVerifRepoTask = true;
		}
		/*
		 * Logic for the old data if any existing "2. Release the Verification Report"
		 * with type FIRMWARE_CT_TI , from build v 3.8 it is changed to type
		 * FIRMWARE_CT_TI2
		 */
		else if (TypeIdentifierHelper.getType(changeObj).equals(FIRMWARE_CT_TI)) {
			checkAndWriteDebug(Debuggable.LINE,
					"#isReleaseVerifRepoTask : The given change task is a : Firmware Change task : "
							+ FIRMWARE_CT_TI.toString());
			if (changeObj instanceof WTChangeActivity2) {
				WTChangeActivity2 currentCA = (WTChangeActivity2) changeObj;
				String currentCAName = currentCA.getName();
				checkAndWriteDebug(Debuggable.LINE,
						"#isReleaseVerifRepoTask : The given change task name is : " + currentCAName);
				if (currentCAName.contains(RVRFIRMWARECHANGETASK_NAME)) {
					isReleaseVerifRepoTask = true;
				}
			}
		}
		checkAndWriteDebug(Debuggable.LINE, "#isReleaseVerifRepoTask : Output data : " + isReleaseVerifRepoTask);
		checkAndWriteDebug(Debuggable.END, "#isReleaseVerifRepoTask");
		return isReleaseVerifRepoTask;
	}

	/**
	 * This method used to get the change Order/change Notice from the change Task
	 * 
	 * @param changeObj - WTObject - Change Objects
	 * 
	 **/
	@Override
	public WTObject getChangeRVRTaskForOrder(WTChangeOrder2 changeObj) {
		checkAndWriteDebug(Debuggable.START, "#getChangeOrderForTask");
		WTObject outputObject = null;
		try {
			QueryResult qr;
			qr = ChangeHelper2.service.getChangeActivities(changeObj);
			while (qr.hasMoreElements()) {
				Object obj = (Object) qr.nextElement();
				if (obj instanceof WTChangeActivity2) {
					outputObject = (WTChangeActivity2) obj;
					if (isReleaseVerifRepoTask(outputObject)) {
						return outputObject;
					}
				}
			}
		} catch (ChangeException2 e) {

			e.printStackTrace();
		} catch (WTException e) {

			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#getChangeOrderForTask");
		return null;
	}

	@Override
	public void setAttributeOnRVRTask(WTObject obj) {
		/*
		 * Build 3.8 Logic included for the 2. Release the Verification Report Setup
		 * participate Attribute stored is not used for approval task So make the role
		 * RVRFIRMWARECHANGETASK_ROLE Mandatory for Firmware CN
		 */
		if (obj instanceof WTChangeOrder2) {
			LOGGER.debug("#EnerSysSetupParticipantAttrDelegate : Entering isReleaseVerifRepoTask Logic");
			WTObject changeOrderRVR = getChangeRVRTaskForOrder((WTChangeOrder2) obj);
			if (changeOrderRVR != null) {
				LinkedList<String> setUpParticipantAttList = getParticipantAttributeOnObject(obj);
				for (String currentValue : setUpParticipantAttList) {
					if (currentValue.contains(RVRFIRMWARECHANGETASK_ROLE)) {
						LOGGER.debug("#EnerSysSetupParticipantAttrDelegate : Value of " + RVRFIRMWARECHANGETASK_ROLE
								+ " is : " + currentValue);
						String[] currentValueRole = currentValue.split(EQUALROLES_DELIMITER);
						String outputString = "";
						if (currentValueRole.length == 2) {
							outputString = "1" + EQUALROLES_DELIMITER + currentValueRole[1];
						}
						String[] currentValueArray = new String[1];
						currentValueArray[0] = outputString;
						CM4ServiceUtility.getInstance().setParticipantAttributeOnObject(changeOrderRVR,
								currentValueArray);
					}
				}
			}
		}
	}

	@Override
	public RevisionControlled getPreviousRevisionObject(WTObject currentObj) {
		LOGGER.debug("getPreviousRevisionObject() Method started");
		RevisionControlled outputRetValue = null;
		if (currentObj instanceof RevisionControlled && currentObj != null) {
			RevisionControlled retValue = (RevisionControlled) currentObj;
			String currentRev = retValue.getVersionIdentifier().getVersionSortId();
			try {
				QueryResult allVersions = VersionControlHelper.service.allVersionsFrom(retValue);
				RevisionControlled prevRevision = null;
				while (allVersions.hasMoreElements()) {
					Object obj = allVersions.nextElement();
					if (obj instanceof RevisionControlled) {
						String checkRevison = ((RevisionControlled) obj).getVersionIdentifier().getVersionSortId();
						if (!(checkRevison.equals(currentRev))) {
							prevRevision = (RevisionControlled) obj;
							break;
						}
					}
				}
				if (prevRevision != null) {
					outputRetValue = prevRevision;
				}
			} catch (WTException e) {
				LOGGER.error("The Error in getPreviousRevisionObject() method ");
				e.printStackTrace();
			}

		}
		LOGGER.debug("getPreviousRevisionObject() Method ended");
		return outputRetValue;
	}

	@Override
	public String getChangeTransitionState(RevisionControlled currentObj) {
		String returnValue = "";
		try {
			String currentObjState = currentObj.getState().getState().toString();
			if (currentObjState.equalsIgnoreCase("UNDERREVIEW")) {
				currentObjState = getPreviousState(currentObj).getValue();
			}
			WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);
			String changeTransitionPrefValue = (String) PreferenceHelper.service.getValue(CHANGETRANS_PREF_INTR_NAME,
					PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);
			String[] arrayOfTransitions = changeTransitionPrefValue.split(SEMICOLON_DELIM);
			for (String currentValue : arrayOfTransitions) {
				if (currentValue.contains(currentObjState)) {
					String[] currenttransitionArr = currentValue.split(COLON_ROLE_MAP_DELIM);
					if (currenttransitionArr.length == 2) {
						returnValue = currenttransitionArr[1];
						break;
					}
				}
			}
		} catch (WTException e) {
			LOGGER.error("The Error in getChangeTransitionState() method ");
			e.printStackTrace();
		}
		return returnValue;
	}

	static State getPreviousState(LifeCycleManaged lcm) throws WTException {

		QueryResult qr = LifeCycleHelper.service.getHistory(lcm);
		ArrayList<LifeCycleHistory> lch_list = new ArrayList<LifeCycleHistory>();
		while (qr.hasMoreElements()) {
			LifeCycleHistory lch = (LifeCycleHistory) qr.nextElement();
			if (lch.getAction().equalsIgnoreCase("Enter_Phase") || lch.getAction().equalsIgnoreCase("Promote")) {
				lch_list.add(lch);
			}
		}

		int lch_size = lch_list.size();
		if (lch_size > 1) {
			Collections.sort(lch_list, new Comparator<LifeCycleHistory>() {
				public int compare(LifeCycleHistory lh1, LifeCycleHistory lh2) {
					return (lh1.getPersistInfo().getCreateStamp()).compareTo(lh2.getPersistInfo().getCreateStamp());
				}
			});
			return lch_list.get(lch_size - 2).getState();
		} else
			return null;
	}

	@Override
	public boolean showSetupParticipantStepReleaseTargetState(NmCommandBean cBean) {
		boolean returnValue = false;
		LOGGER.debug("Start StandardCM4$Service#showSetupParticipantStepReleaseTargetState");
		try {
			HashSet<String> blackListedContainers = new HashSet<>();
			HashSet<TypeIdentifier> blackListedTIs = new HashSet<>();
			initializeSetUpParticipantsPrefValues(blackListedContainers, blackListedTIs);
			String containerName = cBean.getViewingContainer().getName();
			NmOid coid = cBean.getPrimaryOid();
			Object obj = coid.getRefObject();
			// Edit Page & Create Page
			TypeIdentifier objTI = TypeIdentifierHelper.getType(obj);
			if (objTI != null && containerName != null) {
				if (StringUtils.isNotEmpty(objTI.getTypeInternalName())) {
					if (!blackListedTIs.contains(objTI) && !blackListedContainers.contains(containerName)) {
						returnValue = true;
					}
				}
			}
		} catch (WTException e) {
			LOGGER.error("Error in " + CLASSNAME + " validateReleasedTargetStates method");
			e.printStackTrace();
		}
		LOGGER.debug("END StandardCM4$Service#showSetupParticipantStepReleaseTargetState");
		return returnValue;
	}

	private void initializeSetUpParticipantsPrefValues(HashSet<String> blackListedContainers,
			HashSet<TypeIdentifier> blackListedTIs) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("START EnerSysChangeTaskValidator#initializeSetUpParticipantsPrefValues");
		}
		try {
			WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);

			// process blacklisted containers
			String contextPreferenceValue = (String) PreferenceHelper.service.getValue(CONTEXT_PREF_INTR_NAME,
					PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
						"EnerSysChangeTaskValidator#initializeSetUpParticipantsPrefValues-->Blacklisted Containers: "
								+ contextPreferenceValue);
			}
			if (contextPreferenceValue != null && !contextPreferenceValue.isEmpty()) {
				for (String str : contextPreferenceValue.split(PREFERENCE_SEPARATOR)) {
					blackListedContainers.add(str);
				}
			}

			// process blacklisted sub types
			String subTypePreferenceValue = (String) PreferenceHelper.service.getValue(SUBTYPE_PREF_INTR_NAME,
					PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("EnerSysChangeTaskValidator#initializeSetUpParticipantsPrefValues-->Blacklisted Types: "
						+ subTypePreferenceValue);
			}
			if (subTypePreferenceValue != null && !subTypePreferenceValue.isEmpty()) {
				for (String str : subTypePreferenceValue.split(PREFERENCE_SEPARATOR)) {
					blackListedTIs.add(TypeIdentifierHelper.getTypeIdentifier(str));
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("END EnerSysChangeTaskValidator#initializeSetUpParticipantsPrefValues");
		}
	}

	@Override
	public HashMap<String, HashMap<String, String>> getWhereUsedContainersString(
			HashMap<Persistable, HashMap<WTContainer, HashSet<Persistable>>> affectedObjContainerMapping,
			String parentContainerName) {
		HashMap<String, HashMap<String, String>> outerStringMapping = new HashMap<>();
		for (Map.Entry<Persistable, HashMap<WTContainer, HashSet<Persistable>>> outerEntry : affectedObjContainerMapping
				.entrySet()) {
			String outerString1 = "";
			Persistable currentOuterObj = outerEntry.getKey();
			HashMap<WTContainer, HashSet<Persistable>> currentInnerObj = outerEntry.getValue();
			if (currentOuterObj instanceof WTPart) {
				WTPart currentOuterPart = (WTPart) currentOuterObj;
				outerString1 = currentOuterPart.getNumber();

			} else if (currentOuterObj instanceof WTDocument) {
				WTDocument currentOuterDoc = (WTDocument) currentOuterObj;
				outerString1 = currentOuterDoc.getNumber();
			}
			HashMap<String, String> innerStringMapping = new HashMap<>();
			for (Map.Entry<WTContainer, HashSet<Persistable>> InnerEntry : currentInnerObj.entrySet()) {
				String containerName = InnerEntry.getKey().getContainerInfo().getName();
				StringBuilder objectNumbers = new StringBuilder();
				HashSet<Persistable> currentObjects = InnerEntry.getValue();
				if (!parentContainerName.equalsIgnoreCase(containerName)) {
					for (Persistable currentObject : currentObjects) {
						if (currentObject instanceof WTPartMaster) {
							objectNumbers.append(((WTPartMaster) currentObject).getNumber());
						} else if (currentObject instanceof WTDocumentMaster) {
							objectNumbers.append(((WTDocumentMaster) currentObject).getNumber());
						}
					}
					innerStringMapping.put(containerName, objectNumbers.toString());
				}
			}
			if (!innerStringMapping.isEmpty()) {
				outerStringMapping.put(outerString1, innerStringMapping);
			}
		}
		return outerStringMapping;
	}

	@Override
	public HashMap<Persistable, HashMap<WTContainer, HashSet<Persistable>>> getWhereUsedContainers(
			WTObject changeObject, int threshold) {
		LOGGER.debug(CLASSNAME + "#getWhereUsedContainers method started");
		HashMap<Persistable, HashMap<WTContainer, HashSet<Persistable>>> finalMapping = new HashMap<Persistable, HashMap<WTContainer, HashSet<Persistable>>>();
		WTPrincipal currentUser = null;
		boolean enforce = SessionServerHelper.manager.isAccessEnforced();
		try {
			currentUser = SessionHelper.getPrincipal();
			currentUser = RestrictedContextBOMControlListener.setAdminPermissions();
			QueryResult qr = CM2Helper.service.getAffectedObjects(changeObject);
			int count = 0;
			if (threshold == -1 || threshold > qr.size()) {
				threshold = qr.size();
			}
			while (qr.hasMoreElements() && count < threshold) {
				ArrayList<Persistable> affectedObjectlist = new ArrayList<Persistable>();
				Object qrObj = qr.nextElement();
				Persistable currentAffectedObj = null;
				if (qrObj instanceof WTPart) {
					WTPart existPart = (WTPart) qrObj;
					currentAffectedObj = existPart;
					count++;
				} else if (qrObj instanceof WTDocument) {
					WTDocument existDoc = (WTDocument) qrObj;
					currentAffectedObj = existDoc;
					count++;
				}
				if (currentAffectedObj != null) {
					affectedObjectlist.add(currentAffectedObj);
					HashMap<WTContainer, HashSet<Persistable>> currentPartContMap = getParentPartContainers(
							currentAffectedObj);
					finalMapping.put(currentAffectedObj, currentPartContMap);
					finalMapping.size();
				}
			}
		} catch (Exception e) {
			LOGGER.error(CLASSNAME + "Error in #getWhereUsedContainers method");
			e.printStackTrace();
		} finally {
			SessionServerHelper.manager.setAccessEnforced(enforce);
			if (currentUser != null) {
				RestrictedContextBOMControlListener.removeAdminPermissions(currentUser);
			}
		}

		LOGGER.debug(CLASSNAME + "#getWhereUsedContainers method ended");
		return finalMapping;
	}

	public HashMap<WTContainer, HashSet<Persistable>> getParentPartContainers(Persistable wtObject) throws Exception {
		allParentSet.clear();
		HashMap<WTContainer, HashSet<Persistable>> currentObjectContainerMapping = new HashMap<WTContainer, HashSet<Persistable>>();
		if (wtObject instanceof WTPart) {
			WTPart currentPart = (WTPart) wtObject;
			allParentSet.add(currentPart.getMaster());
		} else if (wtObject instanceof WTDocument) {
			WTDocument currentDoc = (WTDocument) wtObject;
			allParentSet.add(currentDoc.getMaster());
		}
		getParents(wtObject);

		for (Object currentObj : allParentSet) {
			if (currentObj instanceof WTPartMaster) {
				WTPartMaster currPart = (WTPartMaster) currentObj;
				WTContainer currContainer = currPart.getContainer();
				if (currentObjectContainerMapping.containsKey(currContainer)) {
					HashSet<Persistable> currentPartSet = currentObjectContainerMapping.get(currContainer);
					currentPartSet.add(currPart);
					currentObjectContainerMapping.put(currContainer, currentPartSet);
				} else {
					HashSet<Persistable> currentPartSet = new HashSet<>();
					currentPartSet.add(currPart);
					currentObjectContainerMapping.put(currContainer, currentPartSet);
				}
			} else if (currentObj instanceof WTDocumentMaster) {
				WTDocumentMaster currDoc = (WTDocumentMaster) currentObj;
				WTContainer currContainer = currDoc.getContainer();
				if (currentObjectContainerMapping.containsKey(currContainer)) {
					HashSet<Persistable> currentDocSet = currentObjectContainerMapping.get(currContainer);
					currentDocSet.add(currDoc);
					currentObjectContainerMapping.put(currContainer, currentDocSet);
				} else {
					HashSet<Persistable> currentDocSet = new HashSet<>();
					currentDocSet.add(currDoc);
					currentObjectContainerMapping.put(currContainer, currentDocSet);
				}
			}
		}
		return currentObjectContainerMapping;
	}

	/**
	 * @param obj
	 * @return
	 */
	@Override
	public String getContainerName(WTObject obj) {
		String parentContainerName = "";
		if (obj instanceof WTChangeOrder2) {
			parentContainerName = ((WTChangeOrder2) obj).getContainerName();
		} else if (obj instanceof WTChangeRequest2) {
			parentContainerName = ((WTChangeRequest2) obj).getContainerName();
		} else if (obj instanceof WTChangeIssue) {
			parentContainerName = ((WTChangeIssue) obj).getContainerName();
		} else if (obj instanceof WTVariance) {
			parentContainerName = ((WTVariance) obj).getContainerName();
		} else if (obj instanceof WTChangeActivity2) {
			parentContainerName = ((WTChangeActivity2) obj).getContainerName();
		} else if (obj instanceof PromotionNotice) {
			parentContainerName = ((PromotionNotice) obj).getContainerName();
		} else if (obj instanceof WTChangeReview) {
			parentContainerName = ((WTChangeReview) obj).getContainerName();
		}
		return parentContainerName;
	}

	/**
	 * Gets all the parents of an object and return them in a vector (navigating by
	 * usage links)
	 *
	 * @param currObj part whose parents must be found
	 *
	 * @return vector with all the parents found
	 */
	/*
	 * public Set<Persistable> getParents(Persistable currObj) throws WTException {
	 * if (currObj instanceof WTPart) { WTPart currentParentPart = (WTPart) currObj;
	 * ArrayList allParents =
	 * queryResultToList(WTPartHelper.service.getUsedByWTParts((WTPartMaster)
	 * currentParentPart.getMaster())); if (allParents.size() != 0) { for(Object
	 * currentObj : allParents) { if (currentObj instanceof WTPart) { WTPart
	 * currentPart = (WTPart)currentObj; allParentSet.add(currentPart.getMaster());
	 * getParents(currentPart); } } } else { return allParentSet; } }else if
	 * (currObj instanceof WTDocument) { WTDocument currentParentDoc = (WTDocument)
	 * currObj; ArrayList allParents =
	 * queryResultToList(WTDocumentHelper.service.getUsedByWTDocuments((
	 * WTDocumentMaster)currentParentDoc.getMaster())); if (allParents.size() != 0)
	 * { for(Object currentObj : allParents) { if (currentObj instanceof WTDocument)
	 * { WTDocument currentDocument = (WTDocument)currentObj;
	 * allParentSet.add(currentDocument.getMaster()); getParents(currentDocument); }
	 * } } else { return allParentSet; } } return allParentSet; }
	 */

	public Set<Persistable> getParents(Persistable currObj) throws WTException {
		Queue<Persistable> queue = new LinkedList<>();
		queue.add(currObj);
		while (!queue.isEmpty()) {
			Persistable currentObj = queue.poll();
			if (currentObj instanceof WTPart) {
				WTPart currentPart = (WTPart) currentObj;
				List<WTPart> allParents = queryResultToList(
						WTPartHelper.service.getUsedByWTParts((wt.part.WTPartMaster) currentPart.getMaster()));
				for (WTPart parentPart : allParents) {
					if (allParentSet.add(parentPart.getMaster())) { // Add only if not present
						queue.add(parentPart);
					}
				}
			} else if (currentObj instanceof WTDocument) {
				WTDocument currentDoc = (WTDocument) currentObj;
				List<WTDocument> allParents = queryResultToList(WTDocumentHelper.service
						.getUsedByWTDocuments((wt.doc.WTDocumentMaster) currentDoc.getMaster()));
				for (WTDocument parentDoc : allParents) {
					if (allParentSet.add(parentDoc.getMaster())) { // Add only if not present
						queue.add(parentDoc);
					}
				}
			}
		}
		return allParentSet;
	}

	/**
	 * 
	 * @param QueryResult qr
	 * @return LinkedList<Object>
	 * @throws WTException
	 */

	public static ArrayList queryResultToList(QueryResult qr) throws WTException {
		ArrayList<Object> outputList = new ArrayList<Object>();
		if (qr != null) {
			while (qr.hasMoreElements()) {
				Object obj = qr.nextElement();
				if (obj instanceof WTPart || obj instanceof WTDocument) {
					outputList.add(obj);
				}
			}
		}
		return outputList;
	}

	/**
	 * @param obj
	 * @return
	 * @throws WTException switch (changeObjType) { case "ChangeRequest": return
	 *                     WTChangeRequest2.class; case "ChangeNotice": return
	 *                     WTChangeOrder2.class; case "ChangeActivity": return
	 *                     WTChangeActivity2.class; case "ProblemReport": return
	 *                     WTChangeIssue.class; case "PromotionRequest": return
	 *                     PromotionNotice.class;
	 */
	@Override
	public ArrayList getObjectDetails(WTObject obj) throws WTException {
		ArrayList<String> objectDetails = new ArrayList<>();
		if (obj instanceof WTChangeRequest2) {
			objectDetails.add(((WTChangeRequest2) obj).getNumber());
			objectDetails.add("Change Request");
		} else if (obj instanceof WTChangeOrder2) {
			objectDetails.add(((WTChangeOrder2) obj).getNumber());
			objectDetails.add("Change Notice");
		} else if (obj instanceof WTChangeActivity2) {
			objectDetails.add(((WTChangeActivity2) obj).getNumber());
			objectDetails.add("Change Task");
		} else if (obj instanceof WTChangeIssue) {
			objectDetails.add(((WTChangeIssue) obj).getNumber());
			objectDetails.add("Problem Report");
		} else if (obj instanceof WTVariance) {
			objectDetails.add(((WTVariance) obj).getNumber());
			objectDetails.add("Deviation");
		} else if (obj instanceof PromotionNotice) {
			objectDetails.add(((PromotionNotice) obj).getNumber());
			objectDetails.add("Promotion Request");
		} else if (obj instanceof WTChangeReview) {
			objectDetails.add(((WTChangeReview) obj).getNumber());
			objectDetails.add("Document Approval");
		} else {
			throw new WTException("Unable to get Object Type");
		}
		return objectDetails;
	}

	@Override
	public JSONObject getEnerSysDistributionTargetValidationRuleDetails(WTObject changeObject, int threshold)
			throws WTException {
		JSONObject validationFeedbackMessage = new JSONObject();
		RuleValidationStatus ruleValidationStatus = RuleValidationStatus.SUCCESS;
		RuleValidationResult ruleValidationResult = new RuleValidationResult(ruleValidationStatus);
		try {
			QueryResult qr = CM2Helper.service.getAffectedObjects(changeObject);
			int count = 0;
			if (threshold == -1 || threshold > qr.size()) {
				threshold = qr.size();
			}
			while (qr.hasMoreElements() && count < threshold) {
				Object qrObj = qr.nextElement();
				if (qrObj instanceof WTPart) {
					WTPart spokeanePart = (WTPart) qrObj;
					boolean isManufacturerPart = EnerSysSoftTypeHelper.isDecendentFrom(spokeanePart,
							EnerSysSoftTypeHelper.ENERSYS_SUPPLIER_PART_TI);
					if (EnerSysBusinessRulesHelper.isESGContext(spokeanePart.getContainerName())
							&& !isManufacturerPart) {
						EnerSysDistributionTargetValidationRule.validateESITarget(spokeanePart, ruleValidationResult);
						if (ruleValidationResult.getStatus() != null
								&& ruleValidationResult.getStatus().equals(RuleValidationStatus.FAILURE)) {
							validationFeedbackMessage.append(spokeanePart.getNumber(),
									EnerSysHelper.service.getVersionInformation(spokeanePart) + " ("
											+ spokeanePart.getViewName() + ")");
							count++;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return validationFeedbackMessage;
	}

	@Override
	public JSONObject getEnerSysImmediateParentStateValidationRuleDetails(WTObject changeObject, int threshold)
			throws WTException {
		JSONObject validationFeedbackMessage = new JSONObject();
		RuleValidationStatus ruleValidationStatus = RuleValidationStatus.SUCCESS;
		RuleValidationResult ruleValidationResult = new RuleValidationResult(ruleValidationStatus);
		WTPrincipal currentUser = QualityGateUtility.setAdminPermissions();
		boolean enforce = SessionServerHelper.manager.isAccessEnforced();
		try {
			int count = 0;
			if (changeObject instanceof PromotionNotice) {
				PromotionNotice selectedPR = (PromotionNotice) changeObject;
				State targetStateSelected = selectedPR.getMaturityState();
				WTCollection promotionCollection = new WTHashSet();
				promotionCollection.addAll(QualityGateUtility.collectLatestPromotables(selectedPR));
				promotionCollection.inflate();
				if (threshold == -1 || threshold > promotionCollection.size()) {
					threshold = promotionCollection.size();
				}
				for (Iterator<?> i = promotionCollection.persistableIterator(); i.hasNext();) {
					Persistable persistable = (Persistable) i.next();
					if (persistable instanceof WTPart && count < threshold) {
						WTPart part = (WTPart) persistable;
						LOGGER.debug("part details :: " + part.getDisplayIdentifier());
						ImmediateChildStateValidationRule immediateChildStateValidationRule = new ImmediateChildStateValidationRule();
						immediateChildStateValidationRule.immediateChildStateValidation(part, ruleValidationResult,
								targetStateSelected, promotionCollection);
						LOGGER.debug("ruleValidationResult.getStatus() :: " + ruleValidationResult.getStatus());
						boolean validationStatus = ruleValidationResult.getStatus() != null
								&& ruleValidationResult.getStatus().equals(RuleValidationStatus.FAILURE);
						LOGGER.debug("validationStatus :: " + validationStatus);
						if (validationStatus) {
							for (RuleFeedbackMessage mes : ruleValidationResult.getFeedbackMessages()) {
								LOGGER.debug("mes :: " + mes.toString());
								if (mes.toString().contains(part.getNumber())) {
									validationFeedbackMessage
											.append(part.getNumber(),
													mes.toString()
															.replace("of " + part.getNumber() + " "
																	+ RevisionIterationInfoHelper.displayInfo(part),
																	""));
									count++;
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			SessionServerHelper.manager.setAccessEnforced(enforce);
			QualityGateUtility.removeAdminPermissions(currentUser);
		}
		return validationFeedbackMessage;
	}

	@Override
	public JSONObject getEnerSysPartCADAssociationValidationRule(WTObject changeObject, int threshold)
			throws WTException {
		LOGGER.debug("getEnerSysPartCADAssociationValidationRule :: START ");

		JSONObject validationFeedbackMessage = new JSONObject();
		HashMap<String, String> validationsResultMap = new HashMap<>();
		try {
			int count = 0;
			WTCollection objectsToBeValidated = new WTHashSet();

			if (changeObject instanceof PromotionNotice) {
				LOGGER.debug("changeObject :: Promotion Notice ");
				PromotionNotice selectedPR = (PromotionNotice) changeObject;
				objectsToBeValidated.addAll(QualityGateUtility.collectLatestPromotables(selectedPR));
				objectsToBeValidated.inflate();
			} else if (changeObject instanceof WTChangeOrder2) {
				LOGGER.debug("changeObject :: WTChangeOrder2 ");
				WTChangeOrder2 cn = (WTChangeOrder2) changeObject;
				objectsToBeValidated.addAll(QualityGateUtility.collectAOROObjects(cn));
				objectsToBeValidated.inflate();
			}

			LOGGER.debug(" objectsToBeValidated size ::  " + objectsToBeValidated.size());
			LOGGER.debug(" threshold ::  " + threshold);

			if (threshold == -1 || threshold > objectsToBeValidated.size()) {
				threshold = objectsToBeValidated.size();
			}
			while (count < threshold) {

				for (Iterator<?> i = objectsToBeValidated.persistableIterator(); i.hasNext();) {
					count++;
					Persistable persistable = (Persistable) i.next();
					if (persistable instanceof WTPart) {
						WTPart part = (WTPart) persistable;
						LOGGER.debug(" Part Details ::  " + part.getDisplayIdentifier());
						EnerSysBusinessRulesHelper.validatePart(part, objectsToBeValidated, validationsResultMap);
					} else if (persistable instanceof EPMDocument) {
						EPMDocument epm = (EPMDocument) persistable;
						LOGGER.debug(" EPM Document Details ::  " + epm.getDisplayIdentifier());
						EnerSysBusinessRulesHelper.validateEPMDoc(epm, validationsResultMap);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			for (Map.Entry<String, String> entry : validationsResultMap.entrySet()) {
				LOGGER.debug("Key ::  " + entry.getKey());
				LOGGER.debug("Value :: " + entry.getValue());
				// Add or merge part numbers into validationFeedbackMessage
				validationFeedbackMessage.append(entry.getKey(), entry.getValue());
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return validationFeedbackMessage;
	}

	@Override
	public JSONObject getEnerSysDepedenceValidationRule(WTObject changeObject, int threshold) throws WTException {

		JSONObject validationFeedbackMessage = new JSONObject();
		RuleValidationStatus ruleValidationStatus = RuleValidationStatus.SUCCESS;
		RuleValidationResult ruleValidationResult = new RuleValidationResult(ruleValidationStatus);
		try {
			int count = 0;

			if (changeObject instanceof PromotionNotice) {
				WTCollection promotablesCollection = new WTHashSet();
				PromotionNotice selectedPR = (PromotionNotice) changeObject;
				State targetPromotionState = selectedPR.getMaturityState();

				promotablesCollection.addAll(QualityGateUtility.collectLatestPromotables(selectedPR));
				promotablesCollection.inflate();
				if (threshold == -1) {
					threshold = promotablesCollection.size();
				}
				for (Iterator<?> i = promotablesCollection.persistableIterator(); i.hasNext();) {
					Persistable persistable = (Persistable) i.next();
					if (persistable instanceof WTPart) {
						WTPart part = (WTPart) persistable;
						EnerSysDepedenceValidationRule.validateWTPart(promotablesCollection, part, targetPromotionState,
								ruleValidationResult);
						for (RuleFeedbackMessage mes : ruleValidationResult.getFeedbackMessages()) {
							if (mes.toString().contains(part.getNumber()) && count < threshold) {
								validationFeedbackMessage.append(part.getNumber(), mes.toString());
								count++;
							}
						}
					}
				}
			} else if (changeObject instanceof WTChangeOrder2) {
				WTChangeOrder2 cn = (WTChangeOrder2) changeObject;
				WTCollection objectsToBeValidated = QualityGateUtility.collectAOROObjects(cn);
				objectsToBeValidated.inflate();
				if (threshold == -1) {
					threshold = objectsToBeValidated.size();
				}

				for (Iterator<?> i = objectsToBeValidated.persistableIterator(); i.hasNext();) {
					Persistable persistable = (Persistable) i.next();
					if (persistable instanceof WTPart) {
						WTPart aoPart = (WTPart) persistable;
						EnerSysDepedenceValidationRule.validateWTPart(objectsToBeValidated, aoPart,
								aoPart.getState().getState(), ruleValidationResult);
						for (RuleFeedbackMessage mes : ruleValidationResult.getFeedbackMessages()) {
							if (mes.toString().contains(aoPart.getNumber()) && count < threshold) {
								validationFeedbackMessage.append(aoPart.getNumber(), mes.toString());
								count++;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return validationFeedbackMessage;
	}

	@Override
	public JSONObject getResultingObjectValidationRule(WTObject changeObject, int threshold) throws WTException {
		JSONObject validationMes = new JSONObject();
		try {
			int count = 0;
			if (changeObject instanceof WTChangeOrder2) {
				WTCollection objectsToBeValidated = QualityGateUtility.collectROObjects((WTChangeOrder2) changeObject);
				objectsToBeValidated.inflate();

				if (threshold == -1) {
					threshold = objectsToBeValidated.size();
				}

				for (Iterator<?> i = objectsToBeValidated.persistableIterator(); i.hasNext();) {
					Persistable persistable = (Persistable) i.next();
					if (persistable instanceof EPMDocument) {
						RevisionIterationInfoHelper.displayInfo((Iterated) persistable);
						EPMDocument epmDocument = (EPMDocument) persistable;
						if (Integer.valueOf(epmDocument.getIterationInfo().getIdentifier().getValue()) == 1
								&& count < threshold) {
							validationMes.put(epmDocument.getNumber(), "CAD must be iterated at least once");
							count++;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return validationMes;
	}

	@Override
	public JSONObject getObjectVisualizationDetails(WTObject changeObject, int threshold) throws WTException {
		LOGGER.debug("get getObjectVisualizationDetails ::  START ");
		JSONObject validationFeedbackMessage = new JSONObject();
		WTPrincipal currentUser = QualityGateUtility.setAdminPermissions();
		boolean enforce = SessionServerHelper.manager.isAccessEnforced();
		HashMap<String, String> resultMap = new HashMap<>();
		try {
			int count = 0;

			WTCollection objectsToBeValidated = new WTHashSet();

			if (changeObject instanceof PromotionNotice) {
				LOGGER.debug("getObjectVisualizationDetails :: changeObject :: Promotion Notice ");
				PromotionNotice selectedPR = (PromotionNotice) changeObject;
				objectsToBeValidated.addAll(QualityGateUtility.collectLatestPromotables(selectedPR));
				objectsToBeValidated.inflate();
			} else if (changeObject instanceof WTChangeOrder2) {
				LOGGER.debug("getObjectVisualizationDetails ::  changeObject :: WTChangeOrder2 ");
				WTChangeOrder2 cn = (WTChangeOrder2) changeObject;
				objectsToBeValidated.addAll(QualityGateUtility.collectROObjects(cn));
				objectsToBeValidated.inflate();
			} else if (changeObject instanceof WTChangeRequest2) {
				LOGGER.debug("getObjectVisualizationDetails ::  changeObject :: WTChangeRequest2 ");
				objectsToBeValidated.addAll(CM2Helper.service.getAffectedObjects(changeObject));
			}
			LOGGER.debug(
					" getObjectVisualizationDetails :: objectsToBeValidated size ::  " + objectsToBeValidated.size());
			if (threshold == -1 || threshold > objectsToBeValidated.size()) {
				threshold = objectsToBeValidated.size();
			}

			LOGGER.debug(" getObjectVisualizationDetails :: threshold ::  " + threshold);
			while (count < threshold) {
				for (Iterator<?> i = objectsToBeValidated.persistableIterator(); i.hasNext();) {
					count++;
					Persistable persistable = (Persistable) i.next();
					if (persistable instanceof WTPart) {
						WTPart part = (WTPart) persistable;
						LOGGER.debug("getObjectVisualizationDetails :: part details :: " + part.getDisplayIdentifier());
						boolean isPDFRepresentationAvailable = checkIfPDFRepresentationAvailability(part);
						LOGGER.debug("isPDFRepresentationAvailable :: " + isPDFRepresentationAvailable);
						if (!isPDFRepresentationAvailable) {
							String partDetails = part.getNumber() + " " + RevisionIterationInfoHelper.displayInfo(part);
							resultMap.put(partDetails, "PDF Representation is not Available.");
							count++;
						}
					} else if (persistable instanceof EPMDocument) {
						EPMDocument epmDoc = (EPMDocument) persistable;
						LOGGER.debug(
								"getObjectVisualizationDetails :: epmDoc details :: " + epmDoc.getDisplayIdentifier());
						boolean isPDFRepresentationAvailable = checkIfPDFRepresentationAvailability(epmDoc);
						LOGGER.debug("isPDFRepresentationAvailable :: " + isPDFRepresentationAvailable);
						if (!isPDFRepresentationAvailable) {
							String epmDetails = epmDoc.getNumber() + " "
									+ RevisionIterationInfoHelper.displayInfo(epmDoc);
							resultMap.put(epmDetails, "PDF Representation is not Available.");
							count++;
						}
					}
				}
			}
			LOGGER.debug("getObjectVisualizationDetails :: Result Map size ::  " + resultMap.size());
			LOGGER.debug(" getObjectVisualizationDetails :: Result Map ::  " + resultMap);

			for (Map.Entry<String, String> entry : resultMap.entrySet()) {
				LOGGER.debug("getObjectVisualizationDetails :: Key ::  " + entry.getKey());
				LOGGER.debug("getObjectVisualizationDetails :: Value :: " + entry.getValue());
				validationFeedbackMessage.append(entry.getKey(), entry.getValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			SessionServerHelper.manager.setAccessEnforced(enforce);
			QualityGateUtility.removeAdminPermissions(currentUser);
		}
		LOGGER.debug("getObjectVisualizationDetails :: validation Feedback Message::  " + validationFeedbackMessage);
		LOGGER.debug("getObjectVisualizationDetails :: END ");
		return validationFeedbackMessage;
	}

	/**
	 * @param part
	 * @throws WTException
	 * @throws PropertyVetoException
	 */
	private boolean checkIfPDFRepresentationAvailability(Persistable part) throws WTException, PropertyVetoException {
		LOGGER.debug("checkIfRepresentationIsAvailable>>>Representation content started");
		VisualizationHelper visualizationHelper = new VisualizationHelper();
		Representation defaultRep = visualizationHelper.getRepresentation(part);
		LOGGER.debug("checkIfPDFRepresentationAvailability :: defaultRep :: " + defaultRep);
		if (defaultRep != null) {
			defaultRep = (Representation) ContentHelper.service.getContents(defaultRep);
			if (defaultRep != null && defaultRep.isDefaultRepresentation()) {
				Enumeration<?> e = ContentHelper.getApplicationData(defaultRep).elements();
				while (e.hasMoreElements()) {
					Object appObject = e.nextElement();
					if (appObject instanceof ApplicationData) {
						ApplicationData appData = (ApplicationData) appObject;
						String fileName = appData.getFileName();
						LOGGER.debug("checkIfPDFRepresentationAvailability :: fileName :: " + fileName
								+ " :: Extension :: " + FilenameUtils.getExtension(fileName));
						if (FilenameUtils.getExtension(fileName).matches("(?i)pdf")) {
							return true; // PDF found
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * This method is called from enersys and firmware CR workflow based on the
	 * affected objects , its corressponding End items are added
	 * 
	 * @param Object - Checks whether it is a CR
	 * @throws WTException
	 */
	@Override
	public void addEndItemstoTheCR(WTObject changeObject) {
		boolean enforce = SessionServerHelper.manager.isAccessEnforced();
		try {
			if (changeObject instanceof WTChangeRequest2) {
				WTChangeRequest2 changeRequestCurrent = (WTChangeRequest2) changeObject;
				QueryResult qr = CM2Helper.service.getAffectedObjects(changeObject);
				HashSet<WTPart> allRootLevelParentPartSet = new HashSet<WTPart>();
				while (qr.hasMoreElements()) {
					ArrayList<Persistable> affectedObjectlist = new ArrayList<Persistable>();
					Object qrObj = qr.nextElement();
					if (qrObj instanceof WTPart) {
						WTPart existPart = (WTPart) qrObj;
						allRootLevelParentPartSet = getRootLevelParentParts(existPart, allRootLevelParentPartSet);
					}
				}
				for (WTPart currentEndItemPart : allRootLevelParentPartSet) {
					SubjectProduct subProd = SubjectProduct.newSubjectProduct(currentEndItemPart.getMaster(),
							changeRequestCurrent);
					PersistenceServerHelper.manager.insert(subProd);
				}
			}

		} catch (Exception e) {
			LOGGER.error(CLASSNAME + "Error in #getWhereUsedContainers method");
			e.printStackTrace();
		} finally {
			SessionServerHelper.manager.setAccessEnforced(enforce);
		}
	}

	private HashSet<WTPart> getRootLevelParentParts(WTPart currentPart, HashSet<WTPart> allRootLevelParentPartSet)
			throws WTException {
		List<WTPart> allParents = queryResultToList(
				WTPartHelper.service.getUsedByWTParts((wt.part.WTPartMaster) currentPart.getMaster()));
		if (allParents.size() > 0) {
			for (WTPart parentPart : allParents) {
				if (parentPart.isEndItem()) {
					allRootLevelParentPartSet.add(parentPart);
				}
				getRootLevelParentParts(parentPart, allRootLevelParentPartSet);
			}
		} else {
			if (currentPart.isEndItem()) {
				allRootLevelParentPartSet.add(currentPart);
			}
		}

		return allRootLevelParentPartSet;
	}
}
