package ext.emerson.windchill.promote.businessRules.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.ptc.core.businessRules.feedback.RuleFeedbackMessage;
import com.ptc.core.businessRules.feedback.RuleFeedbackType;
import com.ptc.core.businessRules.validation.RuleValidation;
import com.ptc.core.businessRules.validation.RuleValidationCriteria;
import com.ptc.core.businessRules.validation.RuleValidationKey;
import com.ptc.core.businessRules.validation.RuleValidationObject;
import com.ptc.core.businessRules.validation.RuleValidationResult;
import com.ptc.core.businessRules.validation.RuleValidationStatus;
import com.ptc.core.validation.FeedbackMsg;

import ext.emerson.access.CustomAccess;
import ext.emerson.properties.CustomProperties;
import ext.emerson.windchill.iba.CustomBusinessObjectHelper;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTReference;
import wt.fc.collections.WTKeyedHashMap;
import wt.maturity.PromotionNotice;
import wt.project.Role;
import wt.team.Team;
import wt.team.TeamException;
import wt.team.TeamHelper;
import wt.util.WTException;
import wt.util.WTMessage;
import wt.util.WTRuntimeException;

public class EmptyRolesValidatorForDesigner implements RuleValidation {
	private static Logger logger = CustomProperties.getlogger("ext.emerson.windchill.promote.businessRules.validator");
	private WTKeyedHashMap invalidObjectsMap = new WTKeyedHashMap();
	private final CustomProperties props = new CustomProperties(CustomProperties.PROMOTE);

	@Override
	public Class<?>[] getSupportedClasses(RuleValidationKey paramRuleValidationKey) {
		return new Class[] { Persistable.class };
	}

	@Override
	public boolean isConfigurationValid(RuleValidationKey paramRuleValidationKey) throws WTException {
		return true;
	}

	@Override
	public RuleValidationResult performValidation(RuleValidationKey paramRuleValidationKey,
			RuleValidationObject paramRuleValidationObject, RuleValidationCriteria paramRuleValidationCriteria)
			throws WTException {
		RuleValidationStatus localRuleValidationStatus = RuleValidationStatus.SUCCESS;
		FeedbackMsg localFeedbackMsg = null;
		ReferenceFactory refFact = new ReferenceFactory();
		WTReference targetRefewrence = refFact
				.getReference((Persistable) paramRuleValidationCriteria.getPrimaryBusinessObject());

		logger.debug(" new localWTReference : " + targetRefewrence);
		logger.debug(" invalidObjectsMap : " + invalidObjectsMap);

		if (!invalidObjectsMap.isEmpty()) {
			localRuleValidationStatus = RuleValidationStatus.FAILURE;
			localFeedbackMsg = (FeedbackMsg) invalidObjectsMap.get(targetRefewrence);
		}
		logger.debug("EmptyRolesValidatorForDesigner result  : " + localRuleValidationStatus + " Message : "
				+ localFeedbackMsg);
		RuleValidationResult localRuleValidationResult = new RuleValidationResult(localRuleValidationStatus);
		localRuleValidationResult.setTargetObject(targetRefewrence);
		localRuleValidationResult.setValidationKey(paramRuleValidationKey);
		if (localFeedbackMsg != null) {
			localRuleValidationResult.addFeedbackMessage(localFeedbackMsg);
		}
		return localRuleValidationResult;
	}

	@Override
	public void prepareForValidation(RuleValidationKey paramRuleValidationKey,
			RuleValidationCriteria paramRuleValidationCriteria) throws WTException {
		boolean isCustomisationEnabled = Boolean
				.valueOf(props.getProperty("ext.emerson.windchill.promote.validators.emptyRole.enabled", "true"))
				.booleanValue();

		if (isCustomisationEnabled) {
			logger.debug("BEGIN validator : EmptyRoleRuleForDesigner. PBO is : "
					+ paramRuleValidationCriteria.getPrimaryBusinessObject());
			CustomAccess access = CustomAccess.newInstance();
			try {
				access.switchToAdministrator();
				long l1 = System.currentTimeMillis();

				logger.debug("PBO is : " + paramRuleValidationCriteria.getPrimaryBusinessObject());
				getInvalidObjects(paramRuleValidationCriteria.getPrimaryBusinessObject());

				long l2 = System.currentTimeMillis();
				if (logger.isTraceEnabled()) {
					logger.trace("Total execution time of EmptyRoleRule.prepareForValidation is : " + (l2 - l1));
				}
			} finally {
				access.switchToPreviousUser();
			}
		}
	}

	private WTKeyedHashMap getInvalidObjects(Object targetObject) throws WTException {

		WTKeyedHashMap localWTKeyedHashMap = new WTKeyedHashMap();
		PromotionNotice pn = (PromotionNotice) targetObject;

		logger.debug("Target Object is : " + pn.getIdentity());

		if (!isValid(pn)) {
			logger.debug("Invalid objects found for " + pn.getIdentity());

		}

		return localWTKeyedHashMap;
	}

	private boolean isValid(PromotionNotice lcManaged) throws WTException {
		List<String> noParticipantList = getEmptyRoles(lcManaged);
		logger.debug("noParticipantList is : " + noParticipantList);

		ReferenceFactory refFact = new ReferenceFactory();
		WTReference targetRefewrence = refFact.getReference(lcManaged);

		if (!noParticipantList.isEmpty()) {
			Object[] localObject1 = new Object[] { String.join(", ", noParticipantList) };
			invalidObjectsMap.put(targetRefewrence,
					new RuleFeedbackMessage(
							new WTMessage("ext.emerson.windchill.promote.PromoteResource", "EMPTY_ROLE_FOR_DESIGNER", localObject1),
							RuleFeedbackType.ERROR));

			return false;
		}
		return false;
	}

	public List<String> getEmptyRoles(PromotionNotice prObject) throws TeamException, WTException {

		List<String> emptyRoleDisplay = new ArrayList<String>();
		List<String> checkroles = new ArrayList<String>();

		checkroles = props.getProperties("ext.emerson.windchill.promote.emptyRoleCheckForDesigner");
		// if flag is Yes or No

		logger.debug("checkroles = " + checkroles);
		Team team = TeamHelper.service.getTeam(prObject);
		if (team != null) {
			// Finding All the Roles in the team
			HashMap map = TeamHelper.service.findAllParticipantsByRole(team);
			logger.debug("Promotion role map: " + map);
			for (String roleToCheck : checkroles) {

				Role roleObj = Role.toRole(roleToCheck);
				// Getting the user assigned to the role into array
				if (map.containsKey(roleObj)) {
					ArrayList array = (ArrayList) map.get(roleObj);
					if (array != null && array.isEmpty()) {
						emptyRoleDisplay.add(roleObj.getFullDisplay());
					}
				}
			}
		}
		return emptyRoleDisplay;
	}

}
