package ext.ptpl.validator;

import java.util.Locale;

import com.ptc.core.ui.validation.DefaultUIComponentValidator;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationResultSet;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.fc.Persistable;
import wt.util.WTException;

public class PromotionNoticeAttributeValidator extends DefaultUIComponentValidator {

	@Override
	public UIValidationResultSet performFullPreValidation(UIValidationKey validationKey,
			UIValidationCriteria validationCriteria, Locale locale) throws WTException {

		return performValidation(validationKey, validationCriteria, locale);
	}

	@Override
	public UIValidationResultSet performLimitedPreValidation(UIValidationKey validationKey,
			UIValidationCriteria validationCriteria, Locale locale) throws WTException {

		System.out.println("---- Customer Name ChangeNoticeAttributeValidator Validator Started-------------");
		UIValidationResultSet rs = UIValidationResultSet.newInstance();
		Persistable launchedFrom = validationCriteria.getContextObject().getObject();
		System.out.println("launchedFrom" + launchedFrom);
		System.out.println("validationKey" + validationKey);

		UIValidationStatus status = UIValidationStatus.ENABLED;
		status = UIValidationStatus.ATTR_READ_ONLY;

		UIValidationResult result = UIValidationResult.newInstance(validationKey, status);
		rs.addResult(result);
		return rs;

	}

	private UIValidationResultSet performValidation(UIValidationKey validationKey,
			UIValidationCriteria validationCriteria, Locale locale) throws WTException {

		return performValidation(validationKey, validationCriteria, locale);
	}
}