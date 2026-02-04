package ext.ptpl.validator;

import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationStatus;
import com.ptc.windchill.enterprise.part.validators.EditPartCommonAttrsActionValidator;

import wt.fc.Persistable;

public class PartEditCommonAttributeValidator extends EditPartCommonAttrsActionValidator {

	public UIValidationStatus performFullPreValidation(UIValidationKey var1, UIValidationCriteria var2) {
		UIValidationStatus status = super.preValidateAttribute(var1, var2);
		System.out.println("---- PartEditCommonAttributeValidator Validator Started-------------");

		Persistable launchedFrom = var2.getContextObject().getObject();
		System.out.println("launchedFrom" + launchedFrom);
		System.out.println("validationKey : " + var1);

		status = UIValidationStatus.ATTR_HIDDEN_VALUE;
		System.out.println("---- Validator Status : " + status);

		return status;

	}


}