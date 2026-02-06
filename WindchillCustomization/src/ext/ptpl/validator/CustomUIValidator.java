package ext.ptpl.validator;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ptc.core.components.validators.SetAttributesStepValidator;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.DefaultUIComponentValidator;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationResultSet;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.fc.WTReference;
import wt.lifecycle.State;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.util.WTException;
import org.apache.logging.log4j.Logger;

/* Autho of this file is Class Owner
*/

public class CustomUIValidator extends DefaultUIComponentValidator {
	protected static final Logger logger = LogR.getLogger(CustomUIValidator.class.getName());

	
	@Override
	public UIValidationResultSet performFullPreValidation(UIValidationKey vKey, UIValidationCriteria vCriteria, Locale locale)
			throws WTException {
		// TODO Auto-generated method stub
		//System.out.println("---------- CustomUIValidator Validation Started-----------");
		UIValidationResult result = UIValidationResult.newInstance(vKey, UIValidationStatus.ENABLED);
		UIValidationResultSet resultset = UIValidationResultSet.newInstance();
		WTPart part = null;
		WTReference reference = vCriteria.getContextObject();
		Object obj = reference.getObject();
		
		if (obj instanceof WTPart) {
			
			part = (WTPart) obj;
			State state = part.getLifeCycleState();
			
			if(state.equals(State.toState("RELEASED"))) {				
				result = UIValidationResult.newInstance(vKey, UIValidationStatus.DISABLED);
			}
		}
		resultset.addResult(result);
		return resultset;
		
	}
}


