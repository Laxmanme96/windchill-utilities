package ext.ptpl.formProcessor;

import java.util.List;
import java.util.Locale;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.DefaultObjectFormProcessor;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.fc.Persistable;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.WTReference;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.config.ConfigHelper;
import wt.vc.config.LatestConfigSpec;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;

public class RegenerateNameForm extends DefaultObjectFormProcessor {
	@Override
	public FormResult doOperation(NmCommandBean nmCommandBean, List<ObjectBean> objectBean) throws WTException {

		super.doOperation(nmCommandBean, objectBean);
		FeedbackMessage feedbackMessage = null;
		StringBuilder message = new StringBuilder();
		FormResult formResult = new FormResult();

		NmOid nmOid = nmCommandBean.getPageOid();
		WTReference wtReference = nmOid.getWtRef();
		Persistable persistableObject = wtReference.getObject();
		if (persistableObject instanceof WTPart) {

			WTPart part = (WTPart) persistableObject;
			//System.out.println("regenerateName Processgin part:" + part);

			try {
				feedbackMessage = new FeedbackMessage(FeedbackType.SUCCESS, Locale.getDefault(),
						"The action is performed successfully", null, message.toString());

				PersistableAdapter obj = new PersistableAdapter(part, null, java.util.Locale.US,
						new com.ptc.core.meta.common.DisplayOperationIdentifier());

				/* Get value of IBAName soft attribute */
				obj.load("SUPPLEMENTARYTITLE1");
				java.lang.String sTitle1 = (java.lang.String) obj.get("SUPPLEMENTARYTITLE1");
				//System.out.println("Soft attibute value1 - SUPPLEMENTARYTITLE1 : " + sTitle1);
				obj.load("TITLE");
				java.lang.String title = (java.lang.String) obj.get("TITLE");
				//System.out.println("Soft attibute value2 - TITLE : " + title);
				String newName = sTitle1 + "|" + title;
				//System.out.println("newName is  " + newName);

				if (title == null || title.isEmpty()) {
					//System.out.println("Inside 2nd if : ");
					feedbackMessage = new FeedbackMessage(FeedbackType.FAILURE, Locale.getDefault(),
							"Title attribute value cannot be null or empty", null, message.toString());
					formResult.setStatus(FormProcessingStatus.FAILURE);
					formResult.addFeedbackMessage(feedbackMessage);

					return formResult;
				}
				try {

					WTPartMaster partMaster = part.getMaster();
					QueryResult result = ConfigHelper.service.filteredIterationsOf(partMaster, new LatestConfigSpec());
					//System.out.println("Master Object query size ---- : " + result.size());
					WTPart latestObject = (WTPart) result.nextElement();
					if (!WorkInProgressHelper.isCheckedOut(latestObject)) {
						//System.out.println("Checking Out LastestPart");
						Workable wrk = WorkInProgressHelper.service.checkout(latestObject,
								WorkInProgressHelper.service.getCheckoutFolder(), "Checked Out throughc code")
								.getWorkingCopy();
						//System.out.println("Object is CheckedOut ");
						PersistableAdapter pa = new PersistableAdapter(wrk, null, null, null);
						pa.persist();
						pa.load("CUSTOMNAME");
						pa.set("CUSTOMNAME", "newName");
						pa.apply();
						PersistenceServerHelper.manager.update(wrk);
						//System.out.println("CheckIn Part");
						WorkInProgressHelper.service.checkin(wrk, "Checked In");

					} else {
						//System.out.println("Object is already CheckedOut ");
					}

				} catch (WTPropertyVetoException e) {
					e.printStackTrace();
				}
				//System.out.println("Form Result : " + feedbackMessage);
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		return formResult;
	}

}
