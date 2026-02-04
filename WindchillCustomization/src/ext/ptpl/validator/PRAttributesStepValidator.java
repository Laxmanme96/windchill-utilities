package ext.ptpl.validator;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.validators.SetAttributesStepValidator;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.log4j.LogR;
import wt.util.WTException;


public class PRAttributesStepValidator extends SetAttributesStepValidator {
	
	protected static final Logger logger = LogR.getLogger(PRAttributesStepValidator.class.getName());

	@SuppressWarnings("deprecation")
	@Override
	public UIValidationResult validateFormSubmission(UIValidationKey vKey, UIValidationCriteria vCriteria, Locale locale)
			throws WTException {

		System.out.println("---------- Customer Name AttributesStepValidator Validator Started---------------------");
		UIValidationResult result = super.validateFormSubmission(vKey, vCriteria, locale);

		Map<String, String> map = vCriteria.getText();
		Set<String> keySet = map.keySet();
		//System.out.println("All Keys for Map :" + keySet);
		
		Iterator<String> it = keySet.iterator();

		while (it.hasNext()) {
			String key = it.next();
			
			if (key.contains("customerName")) {
				String name = map.get(key);
				//System.out.println("Name value is :"+ name);
				if (!name.matches("^[A-Za-z0-9-_&%#!:;<>{}/?]*$")) {
					//System.out.println("Name value is :"+ name);
				result.setStatus(UIValidationStatus.DENIED);
				result.addFeedbackMsg(new UIValidationFeedbackMsg("Customer Name can not contain listed characters , .()",FeedbackType.CONFIRMATION));
				//System.out.println("CURRENT " + result.toString());
				//System.out.println("----------Validation Ended");
				return result;
				}
			}	
		}

		Map<String, List<String>> comboMap = vCriteria.getComboBox();
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

		//System.out.println("CURRENT " + result.toString());
		System.out.println("----------Validation Ended");
		return result;
	}
}


