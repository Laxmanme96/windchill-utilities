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

/*Author of this file is Class owner
*/


/* Local comment*/

public class AttributesStepValidator extends SetAttributesStepValidator {
	
	protected static final Logger logger = LogR.getLogger(AttributesStepValidator.class.getName());

	@SuppressWarnings("deprecation")
	@Override
	public UIValidationResult validateFormSubmission(UIValidationKey vKey, UIValidationCriteria vCriteria, Locale locale)
			throws WTException {
		// TODO Auto-generated method stub
		//System.out.println("----------Validation Started---------------------");
		UIValidationResult result = super.validateFormSubmission(vKey, vCriteria, locale);

		Map<String, String> map = vCriteria.getText();
		Set<String> keySet = map.keySet();
		//System.out.println("All Keys are :" + keySet);
		Iterator<String> it = keySet.iterator();

		while (it.hasNext()) {
			String key = it.next();
			
			if (key.contains("name_col_name")) {
				String name = map.get(key);
				//System.out.println("Name value is :"+ name);
				if (!name.matches("^[A-Za-z0-9-_&%#!:;<>{}/?]*$")) {
					System.out.println("Name value is :"+ name);
				result.setStatus(UIValidationStatus.DENIED);
				result.addFeedbackMsg(new UIValidationFeedbackMsg("Name can not contain listed characters , .() \n Are you sure ",FeedbackType.CONFIRMATION));
				//System.out.println("CURRENT " + result.toString());
				//System.out.println("----------Validation Ended");
				return result;
				}
			}	
		}
		System.out.println("CURRENT " + result.toString());
		System.out.println("----------Validation Ended");
		return result;
	}
}


