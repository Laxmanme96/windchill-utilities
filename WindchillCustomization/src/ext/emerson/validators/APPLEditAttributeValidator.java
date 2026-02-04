package ext.emerson.validators;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.validators.EditAttributesStepValidator;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;

import ext.emerson.properties.CustomProperties;
import ext.emerson.util.BusinessObjectValidator;
import wt.doc.WTDocument;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTReference;
import wt.log4j.LogR;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.part.WTPart;
import wt.part.WTPartDescribeLink;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.type.ClientTypedUtility;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

public class APPLEditAttributeValidator extends EditAttributesStepValidator {

	protected static final Logger logger = LogR.getLogger(APPLEditAttributeValidator.class.getName());
	private static final String CERT_REQUIRED = "CERT_REQUIRED";
	private static final String SOURCING_REQUIRED = "SOURCING_REQUIRED";
	private final CustomProperties props = new CustomProperties(CustomProperties.PR_VALIDATE);
	protected static String FORM_TYPE = null;
	protected static String FORM_CONTAINER = null;

	@SuppressWarnings("deprecation")
	@Override
	public UIValidationResult validateFormSubmission(UIValidationKey var1, UIValidationCriteria var2, Locale var3)
			throws WTException {
		logger.debug("ENTERING APPLEditAttributeValidator.validateFormSubmission");
		UIValidationResult result = super.validateFormSubmission(var1, var2, var3);
//		logger.debug("  validtionKey -> " + var1);
//		logger.debug("  validationCriteria -> " + var2.toString());
//		logger.debug("CURRENT " + result.toString());

		FORM_CONTAINER = var2.getInvokedFromContainer().getName();
		logger.debug("-------- FORM_CONTAINER: " + FORM_CONTAINER);
		String FORM_TYPE = ClientTypedUtility.getTypeIdentifier(var2.getContextObject().getObject()).toString();
		logger.debug("-------- FORM_TYPE : " + FORM_TYPE);

		if (FORM_TYPE.contains("PromotionNotice")) {
			FORM_TYPE = getPromotionTargetsType(var2);
		}
		if (BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDTYPES),
				FORM_TYPE)
				&& BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDCONTEXTS),
						FORM_CONTAINER)) {

			logger.debug("Valid FORM_TYPE : " + FORM_TYPE);
			logger.debug("Valid FORM_CONTAINER : " + FORM_CONTAINER);
			Persistable obj = var2.getContextObject().getObject();
			if (obj instanceof WTDocument) {
				WTDocument doc = null;
				doc = (WTDocument) obj;
				logger.debug("****** APPLEditAttributeValidator : Validation for Document Started ");
				/*
				 * if (BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.
				 * APPLETONVALIDTYPES), formType.getTypename()) &&
				 * BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.
				 * APPLETONVALIDCONTEXTS), formCont)) { logger.debug("Validation Started");
				 * 
				 * Map<String, List<String>> comboMap = var2.getComboBox(); Set<String>
				 * comboKeySet = comboMap.keySet(); Iterator<String> comboIt =
				 * comboKeySet.iterator();
				 * 
				 * while (comboIt.hasNext()) { String key = comboIt.next(); if
				 * (key.contains("CERT_REQUIRED")) { String cert = comboMap.get(key).get(0);
				 * logger.debug("NEW CERT found: " + cert); if (cert.equalsIgnoreCase("false"))
				 * { result.setStatus(UIValidationStatus.DENIED); result.addFeedbackMsg(new
				 * UIValidationFeedbackMsg(
				 * "Certification Required is Set to NO. \nAre you Sure? If Yes, Press OK else Press Cancel"
				 * , FeedbackType.CONFIRMATION)); } } } }
				 */
				Map<String, String> map = var2.getText();
				Set<String> keySet = map.keySet();
				Iterator<String> it = keySet.iterator();
				while (it.hasNext()) {
					String key = it.next();
					if (key.contains("BOM_NAME")) {
						String itemBomName = map.get(key);
						if (itemBomName != null) {
							logger.debug("BOM Name found: " + itemBomName);
							if (itemBomName.toUpperCase().startsWith("CAD")
									|| itemBomName.toUpperCase().startsWith("DOC")) {
								result.setStatus(UIValidationStatus.DENIED);
								result.addFeedbackMsg(new UIValidationFeedbackMsg(
										"Item BOM Name cannot start with CAD or DOC", FeedbackType.ERROR));
								continue;
							} else if (!itemBomName.matches("^[A-Za-z0-9-_]*$")) {
								result.setStatus(UIValidationStatus.DENIED);
								result.addFeedbackMsg(new UIValidationFeedbackMsg(
										"Item BOM Name can only have Alphabets, Numbers, Hyphen (-) and UnderScore (_)",
										FeedbackType.ERROR));
								continue;
							}
							WTPart part = getWTPartByNumber(itemBomName);
							if (part != null) {
								if (doc != null) {
									String partState = part.getLifeCycleState().getValue();
									logger.debug("Part state: " + partState);
									if (partState.equalsIgnoreCase("RELEASED") || partState.equalsIgnoreCase("OBSOLETE")
											|| partState.equalsIgnoreCase("PHASEOUT")) {
										result.setStatus(UIValidationStatus.DENIED);
										result.addFeedbackMsg(new UIValidationFeedbackMsg(
												"Part with Number: " + itemBomName + " must be revised to Create link",
												FeedbackType.ERROR));
									} else {
										boolean flag = false;
										QueryResult qr = PersistenceHelper.manager.navigate(part,
												WTPartDescribeLink.DESCRIBED_BY_ROLE, WTPartDescribeLink.class, false);
										while (qr.hasMoreElements()) {
											WTPartDescribeLink link = (WTPartDescribeLink) qr.nextElement();
											logger.debug("Found link ..........." + link);
											WTDocument newDoc = link.getDescribedBy();
											logger.debug("Described By Document: " + newDoc.getNumber());
											logger.debug("Original doc: " + doc.getNumber());
											if (newDoc.equals(doc)) {
												logger.debug("Making flag true");
												flag = true;
											}
										}
										logger.debug("Flag value: " + flag);
										if (!flag) {
											result.setStatus(UIValidationStatus.DENIED);
											result.addFeedbackMsg(new UIValidationFeedbackMsg("Part with Number: "
													+ itemBomName
													+ " already exists. \nPress OK to continue and link part else Press Cancel to change Item BOM Name value",
													FeedbackType.CONFIRMATION));
										}
									}
								}
							}
						}
					}
				}
			}
			else if (obj instanceof PromotionNotice) {
				logger.debug("****** APPLEditAttributeValidator : Validation for Promotion Notice Started ");
				Map<String, List<String>> comboMap = var2.getComboBox();
				Set<String> comboKeySet = comboMap.keySet();
				Iterator<String> comboIt = comboKeySet.iterator();
				while (comboIt.hasNext()) {
					String key = comboIt.next();
					if (key.contains(CERT_REQUIRED)) {
						String cert = comboMap.get(key).get(0);
						logger.debug("NEW CERT found: " + cert);
						if (cert.equalsIgnoreCase("false")) {
							result.setStatus(UIValidationStatus.DENIED);
							result.addFeedbackMsg(new UIValidationFeedbackMsg(
									"Certification Required is Set to NO. \nAre you Sure? If Yes, Press OK else Press Cancel",
									FeedbackType.CONFIRMATION));
						}
					} else if (key.contains(SOURCING_REQUIRED)) {
						String souring = comboMap.get(key).get(0);
						logger.debug("NEW Sourcing found: " + souring);
						if (souring.equalsIgnoreCase("false")) {
							result.setStatus(UIValidationStatus.DENIED);
							result.addFeedbackMsg(new UIValidationFeedbackMsg(
									"Sourcing Iterm is Set to NO. \nAre you Sure? If Yes, Press OK else Press Cancel",
									FeedbackType.CONFIRMATION));
						}
					}
				}
			}

		}
		logger.debug("---------EXITING APPLEditDocValidator.validateFormSubmission---------");
		return result;
}

	public static WTPart getWTPartByNumber(String partNumber) throws WTException {
		WTPart part = null;
		WTPart latest = null;
		QuerySpec qs = new QuerySpec(WTPart.class);
		// int queryIndex = qs.appendClassList(WTPart.class, true);
		qs.appendWhere(new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, partNumber),
				new int[] { 0 });
		// qs.appendAnd();
		// qs.appendWhere(new SearchCondition(WTPart.class,Iterated.LATEST_ITERATION,
		// SearchCondition.IS_TRUE), new int[]{0});
		QueryResult qr = PersistenceHelper.manager.find(qs);
		while (qr.hasMoreElements()) {
			part = (WTPart) qr.nextElement();
			logger.debug("Part found: " + part.getNumber() + " Version: "
					+ VersionControlHelper.getVersionIdentifier(part).getValue() + "."
					+ VersionControlHelper.getIterationIdentifier(part).getValue());
			latest = (WTPart) wt.vc.VersionControlHelper.service.allVersionsOf(part.getMaster()).nextElement();
			logger.debug("Latest Part found: " + latest.getNumber() + " Version: "
					+ VersionControlHelper.getVersionIdentifier(latest).getValue() + "."
					+ VersionControlHelper.getIterationIdentifier(latest).getValue());
			return latest;
		}

		return null;
	}

	private String getPromotionTargetsType(UIValidationCriteria validationCriteria) {
		try {
			WTReference pageReference = validationCriteria.getPageObject();
			logger.debug("Page WTReference: " + pageReference);
			Persistable persistableObject = pageReference.getObject();
			PromotionNotice pr = (PromotionNotice) persistableObject;
			QueryResult query = MaturityHelper.service.getPromotionTargets(pr);
			while (query.hasMoreElements()) {
				Object obj = query.nextElement();
				TypeIdentifier softType = TypeIdentifierHelper.getType(obj);
				FORM_TYPE = softType.getTypeInternalName();
				logger.debug("Current Form Type : " + FORM_TYPE);
				if (!BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDTYPES),
						FORM_TYPE)) {
					logger.debug("Invalid Form Type : " + FORM_TYPE);
					return FORM_TYPE;
				}
			}
		} catch (Exception e) {
			logger.error("Error while processing reference for query: ", e);

		}
		return FORM_TYPE;
	}

}
