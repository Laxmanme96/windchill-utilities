package ext.emerson.windchill.promote.businessRules.validator;

import java.util.List;
import java.util.stream.Stream;

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
import wt.content.ContentHelper;
import wt.content.ContentRoleType;
import wt.doc.WTDocument;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTKeyedHashMap;
import wt.type.TypedUtilityService;
import wt.util.WTException;
import wt.util.WTMessage;

public class EmptyContentValidator implements RuleValidation {
private static final Logger		logger				= CustomProperties.getlogger("ext.emerson.windchill.promote.businessRules.validator");
private WTKeyedHashMap			invalidObjectsMap	= new WTKeyedHashMap();
private final CustomProperties	props				= new CustomProperties(CustomProperties.PROMOTE);

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
	logger.debug("performValidation");

	RuleValidationStatus localRuleValidationStatus = RuleValidationStatus.SUCCESS;
	FeedbackMsg localFeedbackMsg = null;
	WTReference localWTReference = paramRuleValidationObject.getTargetObject();
	if (!invalidObjectsMap.isEmpty()&& invalidObjectsMap.containsKey(localWTReference.getObject())) {
		localRuleValidationStatus = RuleValidationStatus.FAILURE;

		localFeedbackMsg = (FeedbackMsg) invalidObjectsMap.get(localWTReference.getObject());

	}
	logger.debug("EmptyContentValidator result  : " + localRuleValidationStatus);
	RuleValidationResult localRuleValidationResult = new RuleValidationResult(localRuleValidationStatus);
	localRuleValidationResult.setTargetObject(localWTReference);
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
			.valueOf(props.getProperty("ext.emerson.windchill.promote.validators.emptyContentRule.enabled", "true"))
			.booleanValue();
	logger.debug(
			"BEGIN emerson validator : EmptyContentRule. isCustomisationEnabled : " + isCustomisationEnabled + " PBO is : " + paramRuleValidationCriteria.getPrimaryBusinessObject());

	if (isCustomisationEnabled) {
		CustomAccess access = CustomAccess.newInstance();
		try {
			access.switchToAdministrator();
			long l1 = System.currentTimeMillis();

			logger.debug("PBO is : " + paramRuleValidationCriteria.getPrimaryBusinessObject());
			invalidObjectsMap = getInvalidObjects(paramRuleValidationCriteria.getTargetObjects());

			long l2 = System.currentTimeMillis();
			if (logger.isTraceEnabled()) {
				logger.trace("Total execution time of EmptyContentValidator.prepareForValidation is : " + (l2 - l1));
			}
		} finally {
			access.switchToPreviousUser();
		}
	}
}

private WTKeyedHashMap getInvalidObjects(WTCollection targetObjects) throws WTException {

	WTKeyedHashMap localWTKeyedHashMap = new WTKeyedHashMap();

	if (targetObjects != null && !targetObjects.isEmpty()) {

		for (Object promotable : targetObjects.persistableCollection()) {
			logger.debug("Target Object is : " + ((Persistable) promotable).getIdentity());
			if (!isValid((WTObject) promotable)) {

				Object[] localObject1 = new Object[] { ((Persistable) promotable).getIdentity() };
				localWTKeyedHashMap.put(promotable,
						new RuleFeedbackMessage(
								new WTMessage("ext.emerson.windchill.promote.PromoteResource", "EMPTYCONTENT_RULE_MESSAGE", localObject1),
								RuleFeedbackType.ERROR));
			}
		}
	}
	return localWTKeyedHashMap;
}

private boolean isValid(WTObject targetObject) throws WTException {

	List<String> typeArr  = props.getProperties("ext.emerson.windchill.promote.validators.emptyContentRule.validTypes");

	TypedUtilityService service = wt.services.ServiceFactory.getService(TypedUtilityService.class);
	String typeName = service.getTypeIdentifier(targetObject).getTypename();
	logger.debug("type name from object : " +typeName );

	if (Stream.of(typeArr.stream().toArray(String[]::new)).anyMatch(word -> typeName.contains(word))) {
		logger.debug("Object is of Appleton Document Type to validate. Proceed to validate...");
		return checkPrimaryContent(targetObject);
	}
	//bypass validation
	return true;
}

public static boolean checkPrimaryContent(WTObject  wtobject) throws WTException {

	logger.debug("In getPrimaryContent() ");
	EPMDocument epmDocument =null;
	WTDocument document =null;
	boolean contentflag =false;


	ContentRoleType roleType = ContentRoleType.PRIMARY;
	if (wtobject instanceof EPMDocument) {
		epmDocument = (EPMDocument)wtobject;

		QueryResult tempResult =ContentHelper.service.getContentsByRole(epmDocument,roleType);
			logger.debug("tempResult.size()"+tempResult.size());
		if (tempResult.size() != 0)
			contentflag = true;
		else
			contentflag = false;

	}else if (wtobject instanceof WTDocument) {
		 document= (WTDocument)wtobject;

	QueryResult tempResult =ContentHelper.service.getContentsByRole(document,roleType);
		logger.debug("tempResult.size()"+tempResult.size());
	if (tempResult.size() != 0)
		contentflag = true;
	else
		contentflag = false;
	}

	logger.debug("contentflag)"+contentflag);


	return 	contentflag;


}


}
