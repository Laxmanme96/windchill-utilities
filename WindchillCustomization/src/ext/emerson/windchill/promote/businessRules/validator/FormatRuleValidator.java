package ext.emerson.windchill.promote.businessRules.validator;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTKeyedHashMap;
import wt.maturity.MaturityException;
import wt.type.TypedUtilityService;
import wt.util.WTException;
import wt.util.WTMessage;

public class FormatRuleValidator implements RuleValidation {
	private static final Logger logger = CustomProperties
			.getlogger("ext.emerson.windchill.promote.businessRules.validator");
	private WTKeyedHashMap invalidObjectsMap = new WTKeyedHashMap();
	private final CustomProperties propsP = new CustomProperties(CustomProperties.PROMOTE);
	private final CustomProperties propsV = new CustomProperties(CustomProperties.VALIDATE);

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
		if (!invalidObjectsMap.isEmpty() && invalidObjectsMap.containsKey(localWTReference.getObject())) {
			localRuleValidationStatus = RuleValidationStatus.FAILURE;

			localFeedbackMsg = (FeedbackMsg) invalidObjectsMap.get(localWTReference.getObject());
			logger.debug("localFeedbackMsg!!!!!!!!!!!!! " + localFeedbackMsg);

		}
		logger.debug("FormatRuleValidator result  : " + localRuleValidationStatus);
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
				.valueOf(propsP.getProperty("ext.emerson.windchill.promote.validators.formatRule.enabled", "true"))
				.booleanValue();
		logger.debug("BEGIN emerson validator : FormatRule. isCustomisationEnabled : " + isCustomisationEnabled
				+ " PBO is : " + paramRuleValidationCriteria.getPrimaryBusinessObject());

		if (isCustomisationEnabled) {
			CustomAccess access = CustomAccess.newInstance();
			try {
				access.switchToAdministrator();
				long l1 = System.currentTimeMillis();

				logger.debug("PBO is : " + paramRuleValidationCriteria.getPrimaryBusinessObject());
				invalidObjectsMap = getInvalidObjects(paramRuleValidationCriteria.getTargetObjects());

				long l2 = System.currentTimeMillis();
				if (logger.isTraceEnabled()) {
					logger.trace("Total execution time of FormatRuleValidator.prepareForValidation is : " + (l2 - l1));
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
							new RuleFeedbackMessage(new WTMessage("ext.emerson.windchill.promote.PromoteResource",
									"FORMAT_RULE_MESSAGE", localObject1), RuleFeedbackType.ERROR));
				}
			}
		}
		return localWTKeyedHashMap;
	}

	private boolean isValid(WTObject targetObject) throws WTException {

		String type = propsV.getProperty(CustomProperties.APPLETONVALIDTYPES);
		String typeArr[] = type.split(",");
		TypedUtilityService service = wt.services.ServiceFactory.getService(TypedUtilityService.class);
		String typeName = service.getTypeIdentifier(targetObject).getTypename();
		if (Arrays.stream(typeArr).anyMatch(typeName::contains)) {
			logger.debug("Object is of Appleton Type. Proceed to validate...");
			return getNumberFormatValidity(targetObject);

		}
		// bypass validation
		return true;
	}

	public static boolean getNumberFormatValidity(WTObject wto) throws MaturityException, WTException {

		String displayIdentity = wto.getDisplayIdentity().toString();
		String strObjectNumber = (String) displayIdentity.subSequence(displayIdentity.indexOf('-') + 1,
				displayIdentity.indexOf(','));
		strObjectNumber = strObjectNumber.trim();
		logger.debug("strObjectNumber : " + strObjectNumber);
		logger.debug("wtObject : " + wto.getDisplayIdentity());

		// Array of prefixes
		String[] arr = { "CAD", "DOC", "NCC", "cad", "doc", "ncc" };

		if (!Stream.of(arr).anyMatch(strObjectNumber::startsWith)) {
			logger.debug("Object not starting with CAD,DOC,NCC so dont validate");
			return true;
		}
		
		if (wto instanceof EPMDocument ) {
			EPMDocument epmDoc = (EPMDocument) wto;
			strObjectNumber = epmDoc.getNumber();
		}
		
		if (strObjectNumber.length() != 13) {
			logger.debug("Number length incorrect");
			return false;
		}
		String format = "\\D{3}\\d{6}\\D{1}\\w";
		Pattern pattern = Pattern.compile(format);
		Matcher m = pattern.matcher(strObjectNumber);
		if (!m.find()) {
			logger.debug("Invalid Object number format");
			return false;
		}

		return true;
	}

}
