package ext.emerson.validators;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.DefaultUIComponentValidator;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.uwgm.cadx.newcaddoc.forms.CADDocumentFormProcessorHelper;

import ext.emerson.properties.CustomProperties;
import ext.emerson.util.BusinessObjectValidator;
import wt.log4j.LogR;
import wt.util.WTException;

public class EmersonNewCADDocValidator extends DefaultUIComponentValidator {
	private static final String CADNUMBER = "number_col_number";
	private static final String CADDRAWING = "CADDRAWING";
	private static final String CADASSEMBLY = "CADASSEMBLY";
	private static final String CADCOMPONENT = "CADCOMPONENT";

	protected static final Logger Logger = LogR.getLogger(EmersonNewCADDocValidator.class.getName());
	private final CustomProperties props = new CustomProperties(CustomProperties.VALIDATE);

	@SuppressWarnings("deprecation")
	@Override
	public UIValidationResult validateFormSubmission(UIValidationKey vKey, UIValidationCriteria vCriteria,
			Locale locale) throws WTException {

		Logger.debug("---------- EmersonNewCADDocValidator Started---------------------");
		UIValidationResult result = super.validateFormSubmission(vKey, vCriteria, locale);

		String formContainter = vCriteria.getInvokedFromContainer().getName();
		Logger.debug("-------- formContainter: " + formContainter);

		if (BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDCONTEXTS),
				formContainter)) {

			NmCommandBean nmcbean = NmCommandBean.getNmCommandBean(vCriteria.getFormData());
			String formType = CADDocumentFormProcessorHelper.getComboBoxValue(nmcbean, "docType");
			Logger.debug("-------- formType : " + formType);
			Map<String, String> map = vCriteria.getText();
			Set<String> keySet = map.keySet();
			Iterator<String> it = keySet.iterator();

			while (it.hasNext()) {
				String key = it.next();
				if (key.contains(CADNUMBER)) {
					String number = map.get(key);
					Logger.debug("Number found: " + number);
					if (formType.equalsIgnoreCase(CADDRAWING)
							&& (number.toUpperCase().startsWith("NCC") || number.toUpperCase().startsWith("DOC"))) {
						Logger.debug("CAD Drawing found: " + number);
						result.setStatus(UIValidationStatus.DENIED);
						result.addFeedbackMsg(
								new UIValidationFeedbackMsg("Number cannot start with NCC or DOC", FeedbackType.ERROR));
						return result;
					} else if ((formType.equalsIgnoreCase(CADASSEMBLY) || formType.equalsIgnoreCase(CADCOMPONENT))
							&& (number.toUpperCase().startsWith("CAD") || number.toUpperCase().startsWith("DOC"))) {
						Logger.debug("CAD Part/assembly found: " + number);
						result.setStatus(UIValidationStatus.DENIED);
						result.addFeedbackMsg(
								new UIValidationFeedbackMsg("Number cannot start with CAD or DOC",
										FeedbackType.ERROR));
						return result;
					} else if (!number.matches("^[A-Za-z0-9-_]*$") && !number.contains("(Generated)")) {
						result.setStatus(UIValidationStatus.DENIED);
						result.addFeedbackMsg(new UIValidationFeedbackMsg(
								"Number can only have Alphabets, Numbers, Hyphen (-) and UnderScore (_)",
								FeedbackType.ERROR));
						return result;
					}
					else if (number.matches("^[0-9]{6,6}$")) {
						result.setStatus(UIValidationStatus.DENIED);
						result.addFeedbackMsg(new UIValidationFeedbackMsg("Number cannot be pure 6 digit Number.",
								FeedbackType.ERROR));
						return result;
					}
				}
			}

		}
		// Logger.debug("CURRENT " + result.toString());
		Logger.debug("----------EmersonNewCADDocValidator Ended ------------");
		return result;

	}
}
