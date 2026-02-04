package ext.ptpl.validator;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.DefaultUIComponentValidator;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;
import com.ptc.netmarkets.model.NmOid;
import wt.util.WTException;

public class ValidateAffectedAndResultingObjectsCN extends DefaultUIComponentValidator {

	@SuppressWarnings({ "unchecked", "deprecation", "unlikely-arg-type" })
	public UIValidationResult validateFormSubmission(UIValidationKey vKey, UIValidationCriteria vCriteria,
			Locale locale) throws WTException {
		System.out.println("ValidateAffectedAndResultingObjectsCN Started : 1 ");
		UIValidationResult result = super.validateFormSubmission(vKey, vCriteria, locale);

		List<NmOid> affectedItemList = vCriteria.getInitialItemsByName("changeTask_affectedItems_table");
		// System.out.println("Affected Items List Size: " +affectedItemList.size());
		List<NmOid> resutltingItemList = vCriteria.getAddedItemsByName("changeTask_resultingItems_table");
		// System.out.println("Resulting Items List Size: " +resutltingItemList.size());

		// vCriteria.getFormData();
//		if (!affectedItemList.isEmpty() && !resutltingItemList.isEmpty()) {

		Map<String, List<String>> textAreaMap = vCriteria.getComboBox();
		// Set<String> textKey = textAreaMap.keySet();

		for (Map.Entry<String, List<String>> entry : textAreaMap.entrySet()) {
			String key = entry.getKey();
			// System.out.println("ValidateAffectedAndResultingObjectsCN All Keys :
			// "+key.toString());
			if (key.contains("finishedDisposition") || key.contains("inventoryDisposition")
					|| key.contains("onOrderDisposition")) {
				List<String> disposition = textAreaMap.get(key);
				System.out.println("ValidateAffectedAndResultingObjectsCN got Keys :  " + key.toString());
				System.out.println("disposition value is :" + disposition + "\n");
				if (disposition.get(0) == null || disposition.isEmpty()|| disposition.get(0) ==  "") {
					// listObjectBeans.isEmpty() || listObjectBeans.get(0).getObject() == null

					System.out.println(">>> EMPTY DISPOSITION DETECTED <<<");

					result.setStatus(UIValidationStatus.PROMPT_FOR_CONFIRMATION);
					result.addFeedbackMsg(
							new UIValidationFeedbackMsg("Disposition must be selected", FeedbackType.CONFIRMATION));

					return result;
					// }
				} else {
					System.out.println("ValidateAffectedAndResultingObjectsCN Started : 2 ");
				}
			}

			/*
			 * if (resutltingItemList.containsAll(affectedItemList)) {
			 * System.out.println("Lists do not match.");
			 * result.setStatus(UIValidationStatus.DENIED); result.addFeedbackMsg( new
			 * UIValidationFeedbackMsg("Resulting and Affects items are not same ",
			 * FeedbackType.CONFIRMATION)); } else {
			 * System.out.println("Both lists match exactly!") ; }
			 * System.out.println("ValidateAffectedAndResultingObjectsCN Check 2 "); }
			 */
		}
		return result;

	}
}
