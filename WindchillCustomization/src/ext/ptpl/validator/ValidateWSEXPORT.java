package ext.ptpl.validator;

import java.util.Locale;

import org.apache.logging.log4j.Logger;

import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.DefaultUIComponentValidator;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentItem;
import wt.epm.EPMDocument;
import wt.fc.ObjectReference;
import wt.fc.WTReference;
import wt.log4j.LogR;
import wt.util.WTException;

public class ValidateWSEXPORT extends DefaultUIComponentValidator {
	protected static final Logger logger = LogR.getLogger(ValidateWSEXPORT.class.getName());

	@SuppressWarnings("deprecation")
	@Override
	public UIValidationResult validateSelectedAction(UIValidationKey vKey, UIValidationCriteria vCriteria,
			Locale locale) throws WTException {

		// TODO Auto-generated method stub
		System.out.println("---------- ValidateWSEXPORT Validation Started-----------");
		UIValidationResult result = super.validateSelectedAction(vKey, vCriteria, locale);

		WTReference reference = vCriteria.getContextObject();
		Object obj = reference.getObject();
		float fileSizeKB = checkPrimaryContent(obj);
		if (fileSizeKB >= 100.00) {
			result.setStatus(UIValidationStatus.PROMPT_FOR_CONFIRMATION);
			result.addFeedbackMsg(new UIValidationFeedbackMsg(
					"File Size is more than 100 KB \n Click OK to proceed or Cancel", FeedbackType.CONFIRMATION));
			System.out.println("---------- WSEXPORT action is DISABLED-----------");
		}
		return result;
	}

	@SuppressWarnings("null")
	public static float checkPrimaryContent(Object wtobject) throws WTException {
		float sizeInKb = 0;
		System.out.println("In checkPrimaryContent ");
		EPMDocument epmDoc = null;

		// ContentRoleType roleType = ContentRoleType.PRIMARY;
		if (wtobject instanceof EPMDocument) {
			epmDoc = (EPMDocument) wtobject;
			System.out.println("Check EPM Docuemnt: " + epmDoc.getNumber());

			ObjectReference or = ObjectReference.newObjectReference(epmDoc);
			System.out.println("ObjectReference : " + or);
			ContentItem contentItem = ContentHelper.service.getPrimaryContent(or);
			System.out.println("ContentItem : ");
			if (contentItem instanceof ApplicationData) { // as opposed to ExternalData or URLData
				System.out.println("In ApplicationData : ");
				sizeInKb = ((ApplicationData) contentItem).getFileSizeKB();
			}
		}
		System.out.println("Check Size in KB " + sizeInKb);
		return sizeInKb;
	}

}
