package ext.ptpl.validator;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.validators.EditAttributesStepValidator;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.log4j.LogR;
import wt.util.WTException;

public class EditSAValidator extends EditAttributesStepValidator {

	protected static final Logger logger = LogR.getLogger(EditAttributesStepValidator.class.getName());

	@SuppressWarnings("deprecation")
	public UIValidationResult validateFormSubmission(UIValidationKey vKey, UIValidationCriteria vCriteria,
			Locale locale) throws WTException {
		// TODO Auto-generated method stub
		//System.out.println("---------- Edit Validation Started----------");
		UIValidationResult result = super.validateFormSubmission(vKey, vCriteria, locale);
		
		Map<String, String> map = vCriteria.getTextArea();
		Set<String> keySet = map.keySet();
		//System.out.println("All Keys of TextAreas:" + keySet);
		Iterator<String> it = keySet.iterator();

		Map<String, String> map2 = vCriteria.getText();
		Set<String> keySet2 = map.keySet();
		//System.out.println("All Keys of Text:" + keySet2);
		Iterator<String> it2 = keySet.iterator();
		
		
		while (it.hasNext()) {
			String key = it.next();
			if (key.contains("customerName")) {
				//System.out.println("Key for Customer Name " + key);
				String customerName = map.get(key);
				//System.out.println("Cusomer Name value is 1:" + customerName);
				if (!customerName.matches("^[A-Za-z0-9-_&%#!:;<>{}/?]*$")) {
					//System.out.println("Customer Name value is 2 :" + customerName);
					result.setStatus(UIValidationStatus.DENIED);
					result.addFeedbackMsg(new UIValidationFeedbackMsg(
							"Customer Name can not contain listed characters , .() \n Are you sure ",
							FeedbackType.CONFIRMATION));
					//System.out.println("CURRENT " + result.toString());
					//System.out.println("----------Validation Ended");
					return result;
				}
			}
		}
		//System.out.println("CURRENT " + result.toString());
		//System.out.println("----------Validation Ended");
		return result;
	}
}
