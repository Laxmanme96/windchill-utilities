package ext.enersys.workflow.es;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ext.enersys.service.ESBusinessHelper;
import ext.enersys.service.ESPropertyHelper;
import wt.change2.ChangeException2;
import wt.change2.ChangeHelper2;
import wt.change2.ChangeableIfc;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeReview;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentType;
import wt.fc.QueryResult;
import wt.inf.container.WTContainerRef;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.org.OrganizationServicesHelper;
import wt.org.WTUser;
import wt.project.Role;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTException;
import wt.workflow.work.WorkItem;
import wt.workflow.work.WorkflowHelper;

/**
 * This class retrieves user and timestamp information from workflow tasks
 * associated with Change Orders, Change Reviews, or Promotion Notices.
 */
public class GetUserAndTimeActivity {

	// Property helpers to read configuration properties
	private static final ESPropertyHelper props = new ESPropertyHelper(ESPropertyHelper.ES_APPROVERS);
	private static final ESPropertyHelper props1 = new ESPropertyHelper(ESPropertyHelper.ES_EPM_ATTRIBUTES);
	private static final String DAInternalName = "ext.enersys.es.DAInternalName";
	private static final String DAApprovers = "ext.es.esg.drawingApprovers.documentApproval";
	private static final String PRApprovers = "ext.es.esg.drawingApprovers.promotion";
	private static final String CNApprovers = "ext.es.esg.drawingApprovers.changeNotice";
	private static final String ReqLength = "ext.enersys.es.epm.approverLength";
	private static final String CADApprovalDateFormatPreferenceValue = "/ext/enersys/CAD_APPROVAL_DATE_FORMAT";

	// Logger for logging debug and info messages
	private static final Logger logger = LogManager.getLogger(GetUserAndTimeActivity.class);

	/**
	 * Retrieves the user and timestamp for a given process-based object (PBO).
	 *
	 * @param pbo The process-based object (Change Order, Change Review, or
	 *            Promotion Notice)
	 * @return A string containing approvers and timestamps in the format:
	 *         "Approvers#Timestamps"
	 * @throws ChangeException2 If an error occurs in the Change process
	 * @throws WTException      If a Windchill-related exception occurs
	 * @throws IOException      If an I/O exception occurs
	 * @throws ParseException
	 */
	public static String getApproversNameAndTimestamp(Object pbo)
			throws ChangeException2, WTException, IOException, ParseException {
		QueryResult workItems = null;
		String nameAndTime = null;
		boolean docTypeCheck = false;
		boolean documentApprovalCheck = false;
		WTContainerRef containerReference = null;

		// Identify the type of PBO and retrieve its work items
		if (pbo instanceof WTChangeReview) {
			WTChangeReview cr = (WTChangeReview) pbo;
			String designReviewInternalName = props1.getProperty(DAInternalName);
			containerReference = cr.getContainerReference();
			String typeIdentifier = TypedUtilityServiceHelper.service.getLocalizedTypeName(cr, Locale.US);

			if (typeIdentifier.equalsIgnoreCase(designReviewInternalName)) {
				QueryResult changeables = ChangeHelper2.service.getChangeables(cr);

				while (changeables.hasMoreElements()) {
					Object obj = changeables.nextElement();
					docTypeCheck = checkDocType(obj);

					if (docTypeCheck) {
						System.out.println("Getting name and time");
						workItems = WorkflowHelper.service.getWorkItems(cr);
						nameAndTime = getNameAndTime(workItems, pbo, containerReference);
					}
				}
			}

		} else if (pbo instanceof WTChangeOrder2) {
			WTChangeOrder2 cn = (WTChangeOrder2) pbo;
			QueryResult changeObjects = ChangeHelper2.service.getChangeablesAfter(cn);
			containerReference = cn.getContainerReference();

			while (changeObjects.hasMoreElements()) {
				Object obj = changeObjects.nextElement();
				docTypeCheck = checkDocType(obj);
				documentApprovalCheck = checkDocumentApprovalForObject(obj);

				if (docTypeCheck && !documentApprovalCheck) {
					workItems = WorkflowHelper.service.getWorkItems(cn);
					nameAndTime = getNameAndTime(workItems, pbo, containerReference);
				}

			}

		} else if (pbo instanceof PromotionNotice) {
			PromotionNotice pr = (PromotionNotice) pbo;
			QueryResult promotionObjects = MaturityHelper.service.getPromotionTargets(pr);
			containerReference = pr.getContainerReference();

			while (promotionObjects.hasMoreElements()) {
				Object obj = promotionObjects.nextElement();
				docTypeCheck = checkDocType(obj);
				documentApprovalCheck = checkDocumentApprovalForObject(obj);
				System.out.println("All checks----- " + docTypeCheck + " " + documentApprovalCheck);

				if (docTypeCheck && !documentApprovalCheck) {
					workItems = WorkflowHelper.service.getWorkItems(pr);
					nameAndTime = getNameAndTime(workItems, pbo, containerReference);
				}
			}
		}

		logger.info("Retrieved approvers and timestamps: {}", nameAndTime);
		System.out.println("Retrieved approvers and timestamps: {}" + nameAndTime);
		return nameAndTime;
	}

	/**
	 * Extracts the approver names and timestamps from the work items.
	 *
	 * @param workItems The workflow items to process
	 * @param pbo       The process-based object (Change Notice, Promotion, Review)
	 * @return A string containing approvers and timestamps separated by #
	 * @throws ParseException
	 */
	public static String getNameAndTime(QueryResult workItems, Object pbo, WTContainerRef containerReference)
			throws ParseException, WTException {
		boolean isCustomisationEnabled = false;
		String property = null;
		HashMap<String, List<String>> map = new HashMap<>();

		StringBuilder approvers = new StringBuilder();
		StringBuilder timestamps = new StringBuilder();

		String approvalDateFormat = ESBusinessHelper.getContextPreferenceValue(CADApprovalDateFormatPreferenceValue,
				containerReference);

		// Read properties file to retrieve role-activity mappings
		if (pbo instanceof WTChangeOrder2) {
			isCustomisationEnabled = Boolean
					.parseBoolean(props.getProperty("ext.ptpl.esg.drawingApprovers.changeNotice.enabled", "true"));
			property = props.getProperty(CNApprovers);

		} else if (pbo instanceof PromotionNotice) {
			isCustomisationEnabled = Boolean
					.parseBoolean(props.getProperty("ext.ptpl.esg.drawingApprovers.promotion.enabled", "true"));
			property = props.getProperty(PRApprovers);

		} else if (pbo instanceof WTChangeReview) {
			isCustomisationEnabled = Boolean
					.parseBoolean(props.getProperty("ext.ptpl.esg.drawingApprovers.documentApproval.enabled", "true"));
			property = props.getProperty(DAApprovers);
		}

		if (isCustomisationEnabled && property != null) {
			for (String mapping : property.split(",")) {
				String[] parts = mapping.split(":");
				if (parts.length == 2) {
					map.put(parts[0], Arrays.asList(parts[1].split("\\|")));
				}
			}
		}

		String charLength = props1.getProperty(ReqLength);
		int length = Integer.parseInt(charLength);

		// Iterate through the work items and check for matching activities
		Map<String, Set<String>> roleToUsers = new HashMap<>(); // Role -> Set of users
		Map<String, String> roleToLatestDate = new HashMap<>(); // Role -> Latest modifyDate
		SimpleDateFormat sdf = new SimpleDateFormat(approvalDateFormat);

		while (workItems.hasMoreElements()) {
			WorkItem wi = (WorkItem) workItems.nextElement();
			String displayIdentifier = wi.getDisplayIdentifier().toString();

			for (String activity : map.keySet()) {
				if (displayIdentifier.contains(activity)) {
					Role role = wi.getRole();
					String roleStr = role.toString();

					if (map.get(activity).contains(roleStr)) {
						String completedBy = wi.getCompletedBy();
						WTUser user = OrganizationServicesHelper.manager.getAuthenticatedUser(completedBy);
						String fullName = user.getFullName();
						String[] name = fullName.split(" ");
						String nameInitials = name[0].substring(0, 1) + name[1].substring(0, 1);

						Timestamp modifyTimestamp = wi.getModifyTimestamp();
						String modifyDate = sdf.format(modifyTimestamp);

						// Update user list for this role
						Set<String> users = roleToUsers.getOrDefault(roleStr, new HashSet<>());

						if (!users.contains(nameInitials)) {
							users.add(nameInitials.toUpperCase()); // Add only if this user hasn't submitted under this
																	// role yet
							roleToUsers.put(roleStr, users);
						}

						// Always update latest date for the role if newer
						if (roleToLatestDate.containsKey(roleStr)) {
							Date existingDate = sdf.parse(roleToLatestDate.get(roleStr));
							Date currentDate = sdf.parse(modifyDate);

							if (currentDate.after(existingDate)) {
								roleToLatestDate.put(roleStr, modifyDate);
							}
						} else {
							roleToLatestDate.put(roleStr, modifyDate);
						}
					}
				}
			}
		}

		Set<String> addedUsers = new HashSet<>();
		Set<String> addedDates = new HashSet<>();

		for (String role : roleToUsers.keySet()) {
			for (String user : roleToUsers.get(role)) {
				if (!addedUsers.contains(user)) {
					approvers.append(user).append(",");
					addedUsers.add(user);
				}
			}
			String date = roleToLatestDate.get(role);
			if (addedDates.add(date)) {
				timestamps.append(date).append(",");
			}
		}

		if (approvers.length() > length) {
			if (pbo instanceof WTChangeOrder2) {
				WTChangeOrder2 cn = (WTChangeOrder2) pbo;
				approvers.delete(0, approvers.length());
				timestamps.delete(0, timestamps.length());
				approvers.append("See Change Notice - " + cn.getNumber());
			} else if (pbo instanceof PromotionNotice) {
				PromotionNotice pr = (PromotionNotice) pbo;
				approvers.delete(0, approvers.length());
				timestamps.delete(0, timestamps.length());
				approvers.append("See promotion Request - " + pr.getNumber());
			} else if (pbo instanceof WTChangeReview) {
				WTChangeReview cr = (WTChangeReview) pbo;
				approvers.delete(0, approvers.length());
				timestamps.delete(0, timestamps.length());
				approvers.append("See Change Review - " + cr.getNumber());
			}
		} else if (timestamps.length() > length) {
			if (pbo instanceof WTChangeOrder2) {
				WTChangeOrder2 cn = (WTChangeOrder2) pbo;
				timestamps.delete(0, timestamps.length());
				approvers.delete(0, approvers.length());
				timestamps.append("See Change Notice - " + cn.getNumber());
			} else if (pbo instanceof PromotionNotice) {
				PromotionNotice pr = (PromotionNotice) pbo;
				timestamps.delete(0, timestamps.length());
				approvers.delete(0, approvers.length());
				timestamps.append("See promotion Request - " + pr.getNumber());
			} else if (pbo instanceof WTChangeReview) {
				WTChangeReview cr = (WTChangeReview) pbo;
				timestamps.delete(0, timestamps.length());
				approvers.delete(0, approvers.length());
				timestamps.append("See Change Review - " + cr.getNumber());
			}
		}

		return approvers + "#" + timestamps;
	}

	/**
	 * Checks whether the provided object is of the required document type.
	 *
	 * @param obj The object to check
	 * @return true if the object is of the required type, false otherwise
	 * @throws WTException If a Windchill-related exception occurs
	 */
	public static boolean checkDocType(Object obj) throws WTException {
		boolean flag = false;

		boolean isCustomisationEnabled = Boolean
				.parseBoolean(props.getProperty("ext.enersys.es.doc.docTypeAttributeInternalName.enabled", "true"));

		if (isCustomisationEnabled && obj instanceof EPMDocument) {
			EPMDocument empDoc = (EPMDocument) obj;
			EPMDocumentType epmdocumenttype = empDoc.getDocType();
			if (epmdocumenttype.equals(EPMDocumentType.toEPMDocumentType("CADDRAWING"))) {
				flag = true;
			}
		}
		return flag;
	}

	/**
	 * Checks whether the object has an approved design review.
	 *
	 * @param obj The object to check
	 * @return true if the design review is approved, false otherwise
	 * @throws ChangeException2 If a change-related exception occurs
	 * @throws WTException      If a Windchill-related exception occurs
	 * @throws RemoteException  If a remote exception occurs
	 */
	public static boolean checkDocumentApprovalForObject(Object obj)
			throws ChangeException2, WTException, RemoteException {
		boolean flag = false;
		QueryResult latestChangeReviews = null;
		if (obj instanceof EPMDocument) {
			EPMDocument epmDoc = (EPMDocument) obj;
			latestChangeReviews = ChangeHelper2.service.getLatestChangeReview((ChangeableIfc) epmDoc);
			while (latestChangeReviews.hasMoreElements()) {
				WTChangeReview changeReview = (WTChangeReview) latestChangeReviews.nextElement();

				String documentApprovalInternalName = props1.getProperty(DAInternalName);
				String typeIdentifier = TypedUtilityServiceHelper.service.getLocalizedTypeName(changeReview, Locale.US);

				if (typeIdentifier.equalsIgnoreCase(documentApprovalInternalName)
						&& "Approved".equalsIgnoreCase(changeReview.getLifeCycleState().toString())) {
					flag = true;
					return flag;
				}
			}
		}

		return flag;
	}
}
