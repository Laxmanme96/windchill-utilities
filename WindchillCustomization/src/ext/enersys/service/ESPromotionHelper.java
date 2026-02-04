package ext.enersys.service;

import org.apache.logging.log4j.Logger;

import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.lifecycle.State;
import wt.log4j.LogR;
import wt.maturity.MaturityException;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.maturity.PromotionTarget;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

/**
 * Helper class to handle promotion-related operations in Windchill. This class
 * provides a method to refresh the state of promotion targets based on the
 * lifecycle state of associated documents.
 */
public class ESPromotionHelper {

	// Logger instance for logging
	private static final Logger logger = LogR.getLogger(ESPromotionHelper.class.getName());

	/**
	 * Refreshes the state of promotion targets linked to the given promotion
	 * notice. It ensures that the create state of the promotion target matches the
	 * lifecycle state of the associated document.
	 * 
	 * @param promotionNotice The promotion notice containing the targets.
	 * @throws MaturityException       If an error occurs during maturity
	 *                                 operations.
	 * @throws WTException             If a Windchill-related exception occurs.
	 * @throws WTPropertyVetoException If an error occurs while setting the state.
	 */
	public static void refreshPromotionState(PromotionNotice promotionNotice)
			throws MaturityException, WTException, WTPropertyVetoException {

		logger.info("Refreshing promotion state for PromotionRequest: {}", promotionNotice.getDisplayIdentifier());

		QueryResult promotionTargets = MaturityHelper.service.getPromotionTargets(promotionNotice, false);

		while (promotionTargets.hasMoreElements()) {
			Object obj = promotionTargets.nextElement();

			// Check if the object is a PromotionTarget
			if (obj instanceof PromotionTarget) {
				PromotionTarget promotionTarget = (PromotionTarget) obj;

				// Get the create state of the promotion target
				State createState = promotionTarget.getCreateState();
				logger.debug("Current Create State: {}", createState);

				// Check if the target is associated with a WTDocument
				if (promotionTarget.getRoleBObject() instanceof WTDocument) {
					WTDocument document = (WTDocument) promotionTarget.getRoleBObject();

					// Retrieve the lifecycle state of the document
					State lifeCycleState = document.getLifeCycleState();
					logger.debug("Document Lifecycle State: {}", lifeCycleState);

					// If the create state and document lifecycle state differ, update it
					if (!createState.equals(lifeCycleState)) {
						promotionTarget.setCreateState(lifeCycleState);
						PersistenceHelper.manager.save(promotionTarget);

						logger.info("Updated PromotionTarget state to match Document lifecycle state: {}",
								lifeCycleState);
					}
				}
			}
		}
		logger.info("Promotion state refresh completed successfully.");
	}
}
