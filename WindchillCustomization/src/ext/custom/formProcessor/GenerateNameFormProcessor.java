package ext.custom.formProcessor;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wt.fc.IdentityHelper;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.folder.Cabinet;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.part.WTPartMasterIdentity;
import wt.util.WTException;

public class GenerateNameFormProcessor extends DefaultObjectFormProcessor {

	private final static Logger LOGGER = LogManager.getLogger(GenerateNameFormProcessor.class.getClass());

	@Override
	public FormResult doOperation(NmCommandBean nmCommandBean, List<ObjectBean> objectBeanList) throws WTException {
		FormResult result = new FormResult();

		WTPart parent = getContextObject(nmCommandBean);   //Get wtpart using nmcommandbean
		LOGGER.debug("parent name is :- " + parent.getName());
		String nametoset = setName(parent);  //Read attribute value and append title and supplement title attribute as this will be new part name
		if (null != nametoset && !nametoset.equalsIgnoreCase("")) {
			WTPartMaster master = (WTPartMaster) parent.getMaster(); // Change Name in master object using IdentityHelper Api
			WTPartMasterIdentity masterIdentity = (WTPartMasterIdentity) master.getIdentificationObject();
			try {
				masterIdentity.setName(nametoset);  // set name

				IdentityHelper.service.changeIdentity(master, masterIdentity);
				PersistenceHelper.manager.refresh(parent);
				result.setStatus(FormProcessingStatus.SUCCESS);
				result.createAndAddDynamicRefreshInfo((Persistable) parent, (Persistable) parent,  // Refreshing page automatically
						NmCommandBean.DYNAMIC_UPDATE);
				String message = "Name Generated Successfully";
				FeedbackMessage localFeedbackMessage = new FeedbackMessage(FeedbackType.SUCCESS, Locale.US, null, null,
						new String[] { message });
				result.addFeedbackMessage(localFeedbackMessage);
			} catch (Exception ex) {
				ex.printStackTrace();
				result.setStatus(FormProcessingStatus.FAILURE);
				String message = "Name Generation failure. Please contact your Administrator.";
				FeedbackMessage localFeedbackMessage = new FeedbackMessage(FeedbackType.FAILURE, Locale.US, null, null,
						new String[] { message });
				result.addFeedbackMessage(localFeedbackMessage);
				return result;
			}
		} else {                                              //If Title attribute is null , donot allow this rename activity
			result.setStatus(FormProcessingStatus.FAILURE);
			String message = "Title is blank for this part therefore part cannot be renamed.";
			FeedbackMessage localFeedbackMessage = new FeedbackMessage(FeedbackType.FAILURE, Locale.US, null, null,
					new String[] { message });
			result.addFeedbackMessage(localFeedbackMessage);
		}


		return result;
	}

	public static WTPart getContextObject(NmCommandBean nmCommandBean) throws WTException {
		WTPart parent = null;
		NmOid oid = nmCommandBean.getPrimaryOid();
		
		Persistable p = oid.getWtRef().getObject();
		if (p instanceof WTPart) {
			parent = (WTPart) p;
		}
		if (p instanceof Cabinet) {
			parent =(WTPart) nmCommandBean.getActionOidsWithWizard().get(0).getRefObject();			
			LOGGER.debug("Number is :- " + parent.getNumber());		
		}
		return parent;
	}

	public static String setName(WTPart part) throws WTException {

		String title = getIBAValueByPersistableAdapter(part, "Title");
		LOGGER.debug("title is :- " + title);
		String supplementaryTitle = getIBAValueByPersistableAdapter(part, "SupplementaryTitle");
		LOGGER.debug("supplementaryTitle is :- " + supplementaryTitle);


		if (title != null && !title.equalsIgnoreCase("") && supplementaryTitle != null
				&& !supplementaryTitle.equalsIgnoreCase("")) {
			return title + " | " + supplementaryTitle;
		}
		if (title != null && !title.equalsIgnoreCase("")
				&& (null == supplementaryTitle || supplementaryTitle.equalsIgnoreCase(""))) {
			return title;
		}
		if (null == title || title.equalsIgnoreCase("")) {
			return "";
		}

		return "";

	}

	public static String getIBAValueByPersistableAdapter(WTPart part, String attrName) throws WTException {
		String classification = "";
		try {
			PersistableAdapter obj = new PersistableAdapter(part, null, Locale.ENGLISH, null);
			obj.load(attrName);
			Object object = obj.get(attrName) == null ? "" : obj.get(attrName);
			if (object instanceof String) {
				classification = (String) object;
			}
		} catch (WTException wtexp) {
			throw wtexp;
		}
		return classification;
	}

}

