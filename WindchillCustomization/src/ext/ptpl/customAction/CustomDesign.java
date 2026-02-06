package ext.ptpl.customAction;

import java.util.ArrayList;
import java.util.Locale;

import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.ui.resources.FeedbackType;

import wt.doc.Document;
import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.State;
import wt.part.WTPart;
import wt.part.WTPartDescribeLink;
import wt.part.WTPartMaster;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;

public class CustomDesign {
//TO Iterate part >>
	public static void checkInCheckOut(WTPart part) throws WTException, WTPropertyVetoException {
		System.out.println("Processgin part:" + part);
		if (!WorkInProgressHelper.isCheckedOut((Workable) part)) {
			WorkInProgressHelper.service.checkout(part, WorkInProgressHelper.service.getCheckoutFolder(), "");
			System.out.println("Part is Checked Out. Now Checking In");
			part = (WTPart) WorkInProgressHelper.service.checkin(part, "Checkin by Action");
		} else {
			part = (WTPart) WorkInProgressHelper.service.checkin(part, "Checkin by Action");
		}
	}

//TO Release part >>
	public static void releasePart(WTPart part) throws WTException, WTPropertyVetoException {
		State currentstate = part.getLifeCycleState();
		if (currentstate.equals(State.toState("INWORK"))) {
			try {
				LifeCycleHelper.service.setLifeCycleState(part, State.toState("RELEASED"));
			} catch (Exception e) {
				throw new WTException("Failed to Released" + e.getMessage());
			}
		} else if (currentstate.equals(State.toState("RELEASED"))) {
			WTPartMaster pm = part.getMaster();
			WTPart lastestPart = (WTPart) VersionControlHelper.service.allIterationsOf(pm).nextElement();
			WTPart revisedPart = (WTPart) VersionControlHelper.service.newVersion(lastestPart);
			PersistenceHelper.manager.save(revisedPart);
			LifeCycleHelper.service.setLifeCycleState(part, State.toState("RELEASED"));
		}
	}

	public static ArrayList<Document> describedByDocuments(WTPart part) throws WTException, WTPropertyVetoException {
		ArrayList<Document> list = new ArrayList<>();
		WTPartMaster pm = part.getMaster();
		WTPart lastestPart = (WTPart) VersionControlHelper.service.allIterationsOf(pm).nextElement();
		QueryResult qr = PersistenceHelper.manager.navigate(lastestPart, WTPartDescribeLink.DESCRIBED_BY_ROLE,
				wt.part.WTPartDescribeLink.class, false);
		while (qr.hasMoreElements()) {
			WTDocument describedByDoc = (WTDocument) qr.nextElement();
			if (describedByDoc != null) {
				list.add(describedByDoc);
			}
		}
		return list;

	}
//This is for Feedback Message
	public static FeedbackMessage regenerateName(WTPart part) throws WTException, WTPropertyVetoException {
		System.out.println("regenerateName Processgin part:" + part);
		WTPart partObject = part;
		FeedbackMessage feedbackMessage = null;
		StringBuilder message = new StringBuilder();
		feedbackMessage = new FeedbackMessage(FeedbackType.SUCCESS, Locale.getDefault(),
				"The action is performed successfully", null, message.toString());

		PersistableAdapter obj = new PersistableAdapter(partObject, null, java.util.Locale.US,
				new com.ptc.core.meta.common.DisplayOperationIdentifier());

		/* Get value of IBAName soft attribute */
		obj.load("SUPPLEMENTARYTITLE1");
		java.lang.String sTitle1 = (java.lang.String) obj.get("SUPPLEMENTARYTITLE1");
		System.out.println("Soft attibute value1 - SUPPLEMENTARYTITLE1 : " + sTitle1);
		obj.load("TITLE");
		java.lang.String title = (java.lang.String) obj.get("TITLE");
		System.out.println("Soft attibute value2 - TITLE : " + title);
		// partObject.setName(sTitle1 + "|" + title);
		if (title == null || title.isEmpty()) {
			System.out.println("Inside 2nd if : ");
			feedbackMessage = new FeedbackMessage(FeedbackType.FAILURE, Locale.getDefault(),
					"Title attribute value cannot be null or empty", null, message.toString());
			System.out.println("Inside 2nd if ends : ");
		}
		System.out.println("Form Result : " + feedbackMessage);
		return feedbackMessage;
	}

}
