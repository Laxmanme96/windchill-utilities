package ext.ptpl.businessRule;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.businessRules.feedback.RuleFeedbackMessage;
import com.ptc.core.businessRules.feedback.RuleFeedbackType;
import com.ptc.core.businessRules.validation.RuleValidation;
import com.ptc.core.businessRules.validation.RuleValidationCriteria;
import com.ptc.core.businessRules.validation.RuleValidationKey;
import com.ptc.core.businessRules.validation.RuleValidationObject;
import com.ptc.core.businessRules.validation.RuleValidationResult;
import com.ptc.core.businessRules.validation.RuleValidationStatus;

import wt.change2.ChangeException2;
import wt.change2.ChangeHelper2;
import wt.change2.WTChangeOrder2;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.fc.WTReference;
import wt.part.WTPart;
import wt.util.WTException;
import wt.util.WTMessage;
import wt.log4j.LogR;

public class CheckResultingObjectsBRule implements RuleValidation {
	private static final Logger LOGGER = LogR.getLoggerInternal(CheckResultingObjectsBRule.class.getName());
	private static final String RESOURCE = "ext.ptpl.businessRule.PluralBusinessRuleRB";

	@Override
	public Class[] getSupportedClasses(RuleValidationKey arg0) {
		// TODO Auto-generated method stub
		return new Class[] { WTPart.class };
	}

	@Override
	public boolean isConfigurationValid(RuleValidationKey arg0) throws WTException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public RuleValidationResult performValidation(RuleValidationKey arg0, RuleValidationObject arg1,
			RuleValidationCriteria arg2) throws WTException {
		// TODO Auto-generated method stub
		System.out.println("******* CheckResultingObjectsBRule Started***********");
		RuleValidationStatus rulevalidationstatus = RuleValidationStatus.SUCCESS;
		RuleValidationResult ruleValidationResult = new RuleValidationResult(rulevalidationstatus);
		Persistable targetObject = (WTPart) arg1.getTargetObject().getObject();

		if (targetObject instanceof WTPart) {
			WTPart part = (WTPart) targetObject;
			LOGGER.debug("WTPart" + part.getNumber());
			ObjectReference objRef = ObjectReference.newObjectReference(part);
			WTChangeOrder2 cn = (WTChangeOrder2) arg2.getPrimaryBusinessObject();
			System.out.println("Primary Bussiness Objects i.e. Change Notice:" + cn);
			if ((!isChangeablesMatch(cn))) {
				String[] errorMessage = { part.getNumber() };
				ruleValidationResult = getValidationResult(objRef, arg0, errorMessage,
						"VALIDATE_BOM_CHILD_CANCELED_STATE");
			}
		}
		System.out.println("\"******* CheckResultingObjectsBRule ENDED");
		return ruleValidationResult;
	}

	public boolean isChangeablesMatch(WTChangeOrder2 cn) throws ChangeException2, WTException {
		QueryResult qr = ChangeHelper2.service.getChangeablesAfter(cn);
		Set<String> resultingObjects = new HashSet<>();
		// Collect part names from the "after" query into a set
		while (qr.hasMoreElements()) {
			WTPart part = (WTPart) qr.nextElement();
			resultingObjects.add(part.getNumber());
			System.out.println("********Reslting Objects: " + part.getNumber());
		}
		if (resultingObjects == null || resultingObjects.isEmpty()) {
			LOGGER.warn("Resulting objects set is null or empty for Change Notice: " + cn);
			return false;
		}
		QueryResult qr2 = ChangeHelper2.service.getChangeablesBefore(cn);
		// Check if any parts in the "before" query were also in the "after" query
		while (qr2.hasMoreElements()) {
			WTPart part2 = (WTPart) qr2.nextElement();
			System.out.println("********Affected Objects: " + part2.getNumber());
			if (!resultingObjects.contains(part2.getNumber())) {

				return false;
			}
		}
		return true;
	}
	public RuleValidationResult getValidationResult(WTReference localWTReference,
			RuleValidationKey paramRuleValidationkey, String[] errorMessage, String validationMseeage) {
		LOGGER.debug("Inside getValidationResult()");
		RuleValidationStatus rulevalidationstatus = RuleValidationStatus.FAILURE;
		RuleValidationResult ruleValidationResult = new RuleValidationResult(rulevalidationstatus);
		ruleValidationResult.setTargetObject(localWTReference);
		ruleValidationResult.setValidationKey(paramRuleValidationkey);

		RuleFeedbackMessage feedbackMessage = new RuleFeedbackMessage(
				new WTMessage(RESOURCE, validationMseeage, errorMessage), RuleFeedbackType.ERROR);
		ruleValidationResult.addFeedbackMessage(feedbackMessage);
		return ruleValidationResult;
	}

	@Override
	public void prepareForValidation(RuleValidationKey arg0, RuleValidationCriteria arg1) throws WTException {
		// TODO Auto-generated method stub

	}

}
