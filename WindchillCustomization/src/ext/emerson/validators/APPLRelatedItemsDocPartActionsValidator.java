package ext.emerson.validators;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationResultSet;
import com.ptc.core.ui.validation.UIValidationStatus;
import com.ptc.windchill.enterprise.doc.validators.RelatedItemsDocPartActionsValidator;

import ext.emerson.properties.CustomProperties;
import ext.emerson.util.BusinessObjectValidator;
import wt.log4j.LogR;
import wt.util.WTException;

public class APPLRelatedItemsDocPartActionsValidator extends RelatedItemsDocPartActionsValidator {
	private final CustomProperties props = new CustomProperties(CustomProperties.VALIDATE);
	protected static final Logger logger = LogR.getLogger(APPLRelatedItemsDocPartActionsValidator.class.getName());
	@Override
	public UIValidationResultSet performFullPreValidation(UIValidationKey var1, UIValidationCriteria var2, Locale var3)
			throws WTException {
		// TODO Auto-generated method stub
		return super.performFullPreValidation(var1, var2, var3);
	}

	@SuppressWarnings("deprecation")
	@Override
	public UIValidationResult validateFormSubmission(UIValidationKey var1, UIValidationCriteria var2, Locale var3)
			throws WTException {
		logger.debug("ENTERING APPLRelatedItemsDocPartActionsValidator.validateFormSubmission");
		UIValidationResult result = super.validateFormSubmission(var1, var2, var3);
		logger.debug("  validtionKey -> " + var1);
		logger.debug("  validationCriteria -> " + var2.toString());
		logger.debug("CURRENT " + result.toString());

		String formType = var2.getObjectTypeBeingCreated().getTypename();
		String formCont = var2.getInvokedFromContainer().getName();
		logger.debug("Form Type: " + formType);
		logger.debug("Form Container: " + formCont);

		if (formType.contains(BusinessObjectValidator.MECHANICALPART) && BusinessObjectValidator
				.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDCONTEXTS), formCont)) {
			Map<String, String> map = var2.getText();
			Set<String> keySet = map.keySet();
			Iterator<String> it = keySet.iterator();

			while (it.hasNext()) {
				String key = it.next();
				// logger.debug("Key: " + key+" Value: "+map.get(key));
				if (key.contains("_col_number")) {
					String number = map.get(key);
					logger.debug("Number found: " + number);
					if (number.toUpperCase().startsWith("CAD") || number.toUpperCase().startsWith("DOC")) {
						result.setStatus(UIValidationStatus.DENIED);
						result.addFeedbackMsg(
								new UIValidationFeedbackMsg("Number cannot start with CAD or DOC", FeedbackType.ERROR));
						return result;
					} else if (!number.matches("^[A-Za-z0-9-_]*$") && !number.contains("(Generated)")) {
						result.setStatus(UIValidationStatus.DENIED);
						result.addFeedbackMsg(new UIValidationFeedbackMsg(
								"Number can only have Alphabets, Numbers, Hyphen (-) and UnderScore (_)",
								FeedbackType.ERROR));
						return result;
					}
				}
			}
		}
		logger.debug("EXITING APPLRelatedItemsDocPartActionsValidator.validateFormSubmission");
		return result;
	}

}
