package ext.emerson.validators;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.validators.EditAttributesStepValidator;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;

import ext.emerson.properties.CustomProperties;
import ext.emerson.util.BusinessObjectValidator;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.fc.WTReference;
import wt.log4j.LogR;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.util.WTException;

public class PREditAttributesStepValidator extends EditAttributesStepValidator {
	private static final String CERT_REQUIRED = "CERT_REQUIRED";
	private static final String SOURCING_REQUIRED = "SOURCING_REQUIRED";
	protected static final Logger LOGGER = LogR.getLogger(PREditAttributesStepValidator.class.getName());
	private final CustomProperties props = new CustomProperties(CustomProperties.PR_VALIDATE);
	protected static String FORM_TYPE = null;
	protected static String FORM_CONTAINER = null;

	@SuppressWarnings("deprecation")
	@Override
	public UIValidationResult validateFormSubmission(UIValidationKey vKey, UIValidationCriteria vCriteria,
			Locale locale) throws WTException {

		System.out.println("---------- PREditAttributesStepValidator Started---------------------");
		UIValidationResult result = super.validateFormSubmission(vKey, vCriteria, locale);

		FORM_CONTAINER = vCriteria.getInvokedFromContainer().getName();
		System.out.println("-------- formContainter: " + FORM_CONTAINER);

		FORM_TYPE = getPromotionTargetsType(vCriteria);
		System.out.println("Form Type to be check : " + FORM_TYPE);

		if (BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDTYPES), FORM_TYPE)
				&& BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDCONTEXTS),
						FORM_CONTAINER)) {
			System.out.println("Valide Container && Form Type");
			Map<String, List<String>> comboMap = vCriteria.getComboBox();
			Set<String> comboKeySet = comboMap.keySet();
			Iterator<String> comboIt = comboKeySet.iterator();

			while (comboIt.hasNext()) {
				String key = comboIt.next();
				if (key.contains(CERT_REQUIRED)) {
					String cert = comboMap.get(key).get(0);
					System.out.println("NEW CERT found: " + cert);
					if (cert.equalsIgnoreCase("false")) {
						result.setStatus(UIValidationStatus.DENIED);
						result.addFeedbackMsg(new UIValidationFeedbackMsg(
								"Certification Required is Set to NO. \nAre you Sure? If Yes, Press OK else Press Cancel",
								FeedbackType.CONFIRMATION));
					}
				} else if (key.contains(SOURCING_REQUIRED)) {
					String souring = comboMap.get(key).get(0);
					System.out.println("NEW Sourcing found: " + souring);
					if (souring.equalsIgnoreCase("false")) {
						result.setStatus(UIValidationStatus.DENIED);
						result.addFeedbackMsg(new UIValidationFeedbackMsg(
								"Sourcing Iterm is Set to NO. \nAre you Sure? If Yes, Press OK else Press Cancel",
								FeedbackType.CONFIRMATION));
					}
				}
			}
		}
		System.out.println("----------definePRAttributesStepValidator Ended ------------");
		return result;
	}

	private String getPromotionTargetsType(UIValidationCriteria validationCriteria) {
		try {
			WTReference pageReference = validationCriteria.getPageObject();
			LOGGER.info("Page WTReference: " + pageReference);
			Persistable persistableObject = pageReference.getObject();
			PromotionNotice pr = (PromotionNotice) persistableObject;
			QueryResult query = MaturityHelper.service.getPromotionTargets(pr);
			while (query.hasMoreElements()) {
				Object obj = query.nextElement();
				TypeIdentifier softType = TypeIdentifierHelper.getType(obj);
				FORM_TYPE = softType.getTypeInternalName();
				System.out.println("Page WTReference Current Form Type : " + FORM_TYPE);
				if (!BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDTYPES),
						FORM_TYPE)) {
					System.out.println("Invalid Form Type : " + FORM_TYPE);
					return FORM_TYPE;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error while processing reference for query: ", e);

		}
		return FORM_TYPE;
	}

}
