package ext.ptpl.validator;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.DefaultUIComponentValidator;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.util.WTException;

public class ValidateParticipantsStep extends DefaultUIComponentValidator {


	public UIValidationResult validateFormSubmission(UIValidationKey vKey, UIValidationCriteria vCriteria,
			Locale locale) throws WTException {
		System.out.println("ValidateParticipantsStep Started : 1 ");
		UIValidationResult result = super.validateFormSubmission(vKey, vCriteria, locale);

		Map<String, String> textAreaMap = vCriteria.getTextArea();
		Set<String> textKey = textAreaMap.keySet();

		for (String key : textKey) {
			System.out.println("ValidateParticipantsStep All Keys :  "+key.toString());
			if (key.contains("longDescription")) {
				String description = textAreaMap.get(key);
				if (description.isBlank() || description.isEmpty()) {
					System.out.println("Description value is :" + description+ "\n");

					result.setStatus(UIValidationStatus.DENIED);
					result.addFeedbackMsg(
							new UIValidationFeedbackMsg("Description is blank ", FeedbackType.CONFIRMATION));
				}
			}
			else {
				System.out.println("ValidateParticipantsStep Started : 2 ");
			}
		}
		result.setStatus(UIValidationStatus.DENIED);
		return result;
	}
}
