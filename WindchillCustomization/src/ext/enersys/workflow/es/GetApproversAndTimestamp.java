package ext.enersys.workflow.es;

import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ext.enersys.service.ESBusinessHelper;
import ext.enersys.service.ESPropertyHelper;
import wt.change2.ChangeException2;
import wt.change2.ChangeHelper2;
import wt.change2.ChangeOrder2;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeReview;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

/**
 * This class handles the retrieval and setting of approvers and timestamps for
 * Change Orders, Promotion Notices, and Change Reviews in Windchill.
 */
public class GetApproversAndTimestamp {

	private static final Logger logger = LogManager.getLogger(GetApproversAndTimestamp.class);
	private static final ESPropertyHelper props = new ESPropertyHelper(ESPropertyHelper.ES_EPM_ATTRIBUTES);
	private static String userAndTime = "";

	/**
	 * Retrieves the approvers for the given PBO.
	 *
	 * @param pbo The process-based object (Change Order, Change Review, Promotion
	 *            Notice)
	 * @return A string containing approvers separated by commas
	 * @throws ChangeException2 If an error occurs in the Change process
	 * @throws WTException      If a Windchill-related exception occurs
	 * @throws IOException      If an I/O exception occurs
	 * @throws ParseException
	 */
	public static String getApprovers(Object pbo) throws ChangeException2, WTException, IOException, ParseException {
		logger.info("Entering getApprovers method for PBO: {}", pbo);

		String approvers = null;
		userAndTime = GetUserAndTimeActivity.getApproversNameAndTimestamp(pbo);

		logger.debug("Retrieved user and time: {}", userAndTime);

		if (userAndTime != null && userAndTime.contains("#")) {
			String[] split = userAndTime.split("#");
			if (split.length == 1 && userAndTime.endsWith("#")) {
				approvers = split[0].replaceAll(",$", ""); // Remove trailing comma
				logger.info("Approvers: {}", approvers);
			} else if (split.length == 2) {
				approvers = split[0].replaceAll(",$", ""); // Remove trailing comma
				logger.info("Approvers: {}", approvers);
			}
		}
		return approvers;
	}

	/**
	 * Retrieves the timestamps for the given PBO.
	 *
	 * @param pbo The process-based object (Change Order, Change Review, Promotion
	 *            Notice)
	 * @return A string containing timestamps separated by commas
	 * @throws ChangeException2 If an error occurs in the Change process
	 * @throws WTException      If a Windchill-related exception occurs
	 * @throws IOException      If an I/O exception occurs
	 */
	public static String getTimestamp(Object pbo) throws ChangeException2, WTException, IOException {
		logger.info("Entering getTimestamp method for PBO: {}", pbo);

		String timestamp = null;
		logger.debug("Retrieved user and time: {}", userAndTime);

		if (userAndTime != null && userAndTime.contains("#")) {
			String[] split = userAndTime.split("#");
			if (split.length == 1 && userAndTime.startsWith("#")) {
				timestamp = split[0].replaceAll(",$", ""); // Remove trailing comma
				logger.info("Timestamps: {}", timestamp);
			} else if (split.length == 2) {
				timestamp = split[1].replaceAll(",$", ""); // Remove trailing comma
				logger.info("Timestamps: {}", timestamp);
			}
		}
		return timestamp;
	}

	/**
	 * Sets the approvers and timestamps as IBA values for the given PBO.
	 *
	 * @param pbo The process-based object (Change Order, Change Review, Promotion
	 *            Notice)
	 * @throws Exception
	 * @throws WTPropertyVetoException
	 */

	public static void setApproversAndTimestamp(Object pbo) throws WTPropertyVetoException, Exception {
		boolean isCustomisationEnabled = Boolean
				.valueOf(props.getProperty("ext.enersys.es.epm.approverName.enabled", "true")).booleanValue();
		isCustomisationEnabled = Boolean
				.valueOf(props.getProperty("ext.enersys.es.epm.approverTimestamp.enabled", "true")).booleanValue();

		if (isCustomisationEnabled) {
			String approversAttributeInternalName = props.getProperty("ext.enersys.es.epm.approverName");
			String timestampAttributeInternalName = props.getProperty("ext.enersys.es.epm.approverTimestamp");

			// getting Approvers to set into EPM Attribute
			String approvers = getApprovers(pbo);
			String timestamp = getTimestamp(pbo);

			if (pbo instanceof WTChangeReview) {
				WTChangeReview cr = (WTChangeReview) pbo;
				String designReviewInternalName = props.getProperty("ext.enersys.es.DAInternalName");
				String typeIdentifier = TypedUtilityServiceHelper.service.getLocalizedTypeName(cr, Locale.US);
				if (typeIdentifier.equalsIgnoreCase(designReviewInternalName)) {
					QueryResult changeables = ChangeHelper2.service.getChangeables(cr);
					while (changeables.hasMoreElements()) {
						Object obj = changeables.nextElement();
						if (approvers != null && timestamp != null && GetUserAndTimeActivity.checkDocType(obj)) {
							ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj,
									approversAttributeInternalName, "");
							ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj,
									timestampAttributeInternalName, "");

							ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj,
									approversAttributeInternalName, approvers);
							ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj,
									timestampAttributeInternalName, timestamp);
						} else if (approvers != null && GetUserAndTimeActivity.checkDocType(obj)) {
							ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj,
									approversAttributeInternalName, "");
							ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj,
									timestampAttributeInternalName, "");

							ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj,
									approversAttributeInternalName, approvers);
						} else if (timestamp != null && GetUserAndTimeActivity.checkDocType(obj)) {
							ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj,
									approversAttributeInternalName, "");
							ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj,
									timestampAttributeInternalName, "");

							ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj,
									timestampAttributeInternalName, timestamp);
						}
					}
				}
			} else if (pbo instanceof PromotionNotice) {
				PromotionNotice pr = (PromotionNotice) pbo;
				QueryResult promotionObjects = MaturityHelper.service.getPromotionTargets(pr);
				while (promotionObjects.hasMoreElements()) {
					Object obj = promotionObjects.nextElement();
					if (approvers != null && timestamp != null && GetUserAndTimeActivity.checkDocType(obj)) {
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, approversAttributeInternalName,
								"");
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, timestampAttributeInternalName,
								"");

						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, approversAttributeInternalName,
								approvers);
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, timestampAttributeInternalName,
								timestamp);
					} else if (approvers != null && GetUserAndTimeActivity.checkDocType(obj)) {
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, approversAttributeInternalName,
								"");
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, timestampAttributeInternalName,
								"");

						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, approversAttributeInternalName,
								approvers);
					} else if (timestamp != null && GetUserAndTimeActivity.checkDocType(obj)) {
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, approversAttributeInternalName,
								"");
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, timestampAttributeInternalName,
								"");

						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, timestampAttributeInternalName,
								timestamp);
					}
				}
			} else if (pbo instanceof ChangeOrder2) {
				WTChangeOrder2 cn = (WTChangeOrder2) pbo;
				QueryResult changeObjects = ChangeHelper2.service.getChangeablesAfter(cn);
				while (changeObjects.hasMoreElements()) {
					Object obj = changeObjects.nextElement();
					if (approvers != null && timestamp != null && GetUserAndTimeActivity.checkDocType(obj)) {
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, approversAttributeInternalName,
								"");
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, timestampAttributeInternalName,
								"");

						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, approversAttributeInternalName,
								approvers);
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, timestampAttributeInternalName,
								timestamp);
					} else if (approvers != null && GetUserAndTimeActivity.checkDocType(obj)) {
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, approversAttributeInternalName,
								"");
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, timestampAttributeInternalName,
								"");

						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, approversAttributeInternalName,
								approvers);
					} else if (timestamp != null && GetUserAndTimeActivity.checkDocType(obj)) {
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, approversAttributeInternalName,
								"");
						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, timestampAttributeInternalName,
								"");

						ESBusinessHelper.updateIBAWithoutIteration((Persistable) obj, timestampAttributeInternalName,
								timestamp);
					}
				}
			}
		}
	}
}
