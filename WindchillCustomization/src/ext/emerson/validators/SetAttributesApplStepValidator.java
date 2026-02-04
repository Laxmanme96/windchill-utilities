package ext.emerson.validators;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ptc.core.components.validators.SetAttributesStepValidator;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;

import ext.emerson.properties.CustomProperties;
import ext.emerson.util.BusinessObjectValidator;
import wt.util.WTException;

public class SetAttributesApplStepValidator extends SetAttributesStepValidator {
	private final CustomProperties props = new CustomProperties(CustomProperties.VALIDATE);

	@SuppressWarnings("deprecation")
	@Override
	public UIValidationResult validateFormSubmission(UIValidationKey var1, UIValidationCriteria var2, Locale var3)
			throws WTException {
		// TODO Auto-generated method stub
		System.out.println("ENTERING SetAttributesApplStepValidator.validateFormSubmission");
		UIValidationResult result = super.validateFormSubmission(var1, var2, var3);
		System.out.println("  validtionKey -> " + var1);
		System.out.println("  validationCriteria -> " + var2.toString());
		System.out.println("CURRENT " + result.toString());

		String formType = var2.getObjectTypeBeingCreated().getTypename();
		String formCont = var2.getInvokedFromContainer().getName();
		System.out.println("Form Type: " + formType);
		System.out.println("Form Container: " + formCont);

		if (BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDTYPES), formType)
				&& BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDCONTEXTS),
						formCont)) {
			System.out.println("Validation Started");
			Map<String, String> map = var2.getText();
			Set<String> keySet = map.keySet();
			Iterator<String> it = keySet.iterator();

			while (it.hasNext()) {
				String key = it.next();
				if (key.contains("number_col_number")) {
					String number = map.get(key);
					System.out.println("Number found: " + number);
					if (number.toUpperCase().startsWith("CAD") || number.toUpperCase().startsWith("DOC")
							|| number.toUpperCase().startsWith("NCC")) {
						result.setStatus(UIValidationStatus.DENIED);
						result.addFeedbackMsg(new UIValidationFeedbackMsg("Number cannot start with CAD,DOC or NCC",
								FeedbackType.ERROR));
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

			Map<String, List<String>> comboMap = var2.getComboBox();
			Set<String> comboKeySet = comboMap.keySet();
			Iterator<String> comboIt = comboKeySet.iterator();

			while (comboIt.hasNext()) {
				String key = comboIt.next();
				if (key.contains("CERT_REQUIRED")) {
					String cert = comboMap.get(key).get(0);
					System.out.println("NEW CERT found: " + cert);
					if (cert.equalsIgnoreCase("false")) {
						result.setStatus(UIValidationStatus.DENIED);
						result.addFeedbackMsg(new UIValidationFeedbackMsg(
								"Certification Required is Set to NO. \nAre you Sure? If Yes, Press OK else Press Cancel",
								FeedbackType.CONFIRMATION));
					}
				}
			}
		}
		System.out.println("EXITING SetAttributesApplStepValidator.validateFormSubmission");
		return result;
	}

}
