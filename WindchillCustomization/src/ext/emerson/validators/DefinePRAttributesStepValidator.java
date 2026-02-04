package ext.emerson.validators;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.validators.SetAttributesStepValidator;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;
import com.ptc.netmarkets.model.NmOid;

import ext.emerson.properties.CustomProperties;
import ext.emerson.util.BusinessObjectValidator;
import wt.fc.Persistable;
import wt.fc.WTReference;
import wt.log4j.LogR;
import wt.util.WTException;

public class DefinePRAttributesStepValidator extends SetAttributesStepValidator {
	private static final String CERT_REQUIRED = "CERT_REQUIRED";
	private static final String SOURCING_REQUIRED = "SOURCING_REQUIRED";
	private static final String Appleton_SOLA_Promotion_Request = "wt.maturity.PromotionNotice|wt.maturity.Appleton_SOLA_Promotion_Notice";
	protected static final Logger logger = LogR.getLogger(DefinePRAttributesStepValidator.class.getName());
	private final CustomProperties props = new CustomProperties(CustomProperties.PR_VALIDATE);
	String promotionTargetsType = null;
	String formContainer = null;

	@SuppressWarnings("deprecation")
	@Override
	public UIValidationResult validateFormSubmission(UIValidationKey vKey, UIValidationCriteria vCriteria,
			Locale locale) throws WTException {

		logger.debug("---------- definePRAttributesStepValidator Started---------------------");
		UIValidationResult result = super.validateFormSubmission(vKey, vCriteria, locale);
		
		String formType = vCriteria.getObjectTypeBeingCreated().getTypename();
		logger.debug("Form to be check : " + formType);
		if (formType.equals(Appleton_SOLA_Promotion_Request)) {
			List<NmOid> selectedOidList = vCriteria.getSelectedOidForPopup(); // Change List<String> to List<NmOid>
			if (selectedOidList == null || selectedOidList.isEmpty()) {
				processPageObject(vCriteria);
			} else {
				processSelectedOidList(selectedOidList);
			}
			logger.debug("PR Targets type to be check : " + promotionTargetsType);
			formContainer = vCriteria.getInvokedFromContainer().getName();
			logger.debug("-------- formContainter: " + formContainer);
			if (BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDTYPES),
					promotionTargetsType)
					&& BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDCONTEXTS),
							formContainer)) {
				logger.debug("Valid Container && Form Type");
				Map<String, List<String>> comboMap = vCriteria.getComboBox();
				Set<String> comboKeySet = comboMap.keySet();
				Iterator<String> comboIt = comboKeySet.iterator();

				while (comboIt.hasNext()) {
					String key = comboIt.next();
					if (key.contains(CERT_REQUIRED)) {
						logger.debug("In CERT_REQUIRED ");
						String cert = comboMap.get(key).get(0);
						logger.debug("NEW CERT found: " + cert);
						if (cert.equalsIgnoreCase("false")) {
							result.setStatus(UIValidationStatus.DENIED);
							result.addFeedbackMsg(new UIValidationFeedbackMsg(
									"Certification Required is Set to NO. \nAre you Sure? If Yes, Press OK else Press Cancel",
									FeedbackType.CONFIRMATION));
						}
					}
					if (key.contains(SOURCING_REQUIRED)) {
						logger.debug("In SOURCING_REQUIRED ");
						String souring = comboMap.get(key).get(0);
						logger.debug("NEW Sourcing found: " + souring);
						if (souring.equalsIgnoreCase("false")) {
							result.setStatus(UIValidationStatus.DENIED);
							result.addFeedbackMsg(new UIValidationFeedbackMsg(
									"Sourcing Iterm is Set to NO. \nAre you Sure? If Yes, Press OK else Press Cancel",
									FeedbackType.CONFIRMATION));
						}
					}
				}
			}
		}
		logger.debug("----------definePRAttributesStepValidator Ended ------------");
		return result;
	}
	private String processSelectedOidList(List<NmOid> selectedOidList) {
		logger.info("Selected OID List: " + selectedOidList);

		for (NmOid nmOid : selectedOidList) {
			try {
				WTReference wtReference = nmOid.getWtRef();
				Persistable persistableObject = wtReference.getObject();
				TypeIdentifier softType = TypeIdentifierHelper.getType(persistableObject);
				promotionTargetsType = softType.getTypeInternalName();
				logger.debug("processSelectedOidList Current PR Target : " + promotionTargetsType);
				if (!BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDTYPES),
						promotionTargetsType)) {
					logger.debug("Invalid PR Target : " + promotionTargetsType);
					return promotionTargetsType;
				}
			} catch (Exception e) {
				logger.debug("Error while processing reference for NmOid: " + nmOid.toString());
			}
		}
		return promotionTargetsType;
	}

	private String processPageObject(UIValidationCriteria validationCriteria) {
		try {
			WTReference pageReference = validationCriteria.getPageObject();
			logger.info("Page WTReference: " + pageReference);
			Persistable persistableObject = pageReference.getObject();
			TypeIdentifier softType = TypeIdentifierHelper.getType(persistableObject);
			promotionTargetsType = softType.getTypeInternalName();

			logger.debug("PR Targets Type : " + promotionTargetsType);
		} catch (Exception e) {
			logger.debug("Error while accessing page object.");
		}
		return promotionTargetsType;
	}

}
