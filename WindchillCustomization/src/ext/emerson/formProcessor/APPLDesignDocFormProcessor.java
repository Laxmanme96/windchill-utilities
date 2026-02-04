package ext.emerson.formProcessor;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.forms.FormResultAction;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.enterprise.doc.forms.CreateDocFormProcessor;

import ext.emerson.properties.CustomProperties;
import ext.emerson.util.BusinessObjectValidator;
import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.log4j.LogR;
import wt.session.SessionHelper;
import wt.util.WTException;

public class APPLDesignDocFormProcessor extends CreateDocFormProcessor {

	private final CustomProperties props = new CustomProperties(CustomProperties.VALIDATE);
	protected static final Logger logger = LogR.getLogger(APPLDesignDocFormProcessor.class.getName());

	@Override
	public FormResult doOperation(NmCommandBean arg0, List<ObjectBean> arg1) throws WTException {
		// TODO Auto-generated method stub
		return super.doOperation(arg0, arg1);
	}

	@Override
	public FeedbackMessage getSuccessFeedbackMessage(List<ObjectBean> arg0) throws WTException {
		// TODO Auto-generated method stub
		return super.getSuccessFeedbackMessage(arg0);
	}

	@Override
	public FormResult postProcess(NmCommandBean clientData, List<ObjectBean> objectBeans) throws WTException {
		logger.debug("ApplDesignDocFormProcessor.postProcess() : START");
		Object object = null;
		boolean fail = false;
		WTDocument doc = null;
		String docNumber = null;
		FeedbackMessage message = null;
		FormResult FormResult = super.postProcess(clientData, objectBeans);
		String formCont = clientData.getContainer().getName();
		logger.debug("Form Container: "+formCont);
		logger.debug("Valid Container: "+props.getProperty(CustomProperties.APPLETONVALIDCONTEXTS));
		if (!BusinessObjectValidator.isObjectValid(props.getProperty(CustomProperties.APPLETONVALIDCONTEXTS), formCont)) {
			logger.debug("Invalid Context");
			return FormResult;
		}
		Map checkedMap = clientData.getChecked();
		Object listofCheckBox = checkedMap.get("generatePartNumberCheckBox");
		boolean customCheckBox = listofCheckBox != null ? true : false;
		for (ObjectBean objBean : objectBeans) {
			object = objBean.getObject();
			if (object instanceof WTDocument) {
				doc = (WTDocument) object;
				docNumber = doc.getNumber();
				logger.debug("Doc Number: "+docNumber);
				if (!BusinessObjectValidator.isObjectOfType(BusinessObjectValidator.DESIGNDOC, doc))
					continue;
				try {
					String itemBomName = getIBAValue(doc, "BOM_NAME");
					if (itemBomName != null) {
						logger.debug("BOM Name: "+itemBomName);
						return FormResult;
					}

					if (!customCheckBox)
						return FormResult;
					else {
						if (docNumber.toUpperCase().startsWith("CAD")) {
							String newItemBomName = "NCC" + docNumber.substring(3);
							setIBAValue(doc, "BOM_NAME", newItemBomName);
							logger.debug("BOM Name: "+newItemBomName);
							message = new FeedbackMessage(FeedbackType.SUCCESS, SessionHelper.getLocale(),
									"Item BOM Name filled Successfully", null,
									"Item BOM Name value " + newItemBomName + " is filled. Check In the Document ");
							FormResult.addFeedbackMessage(message);
						} else if(!(docNumber.toUpperCase().startsWith("DOC")||docNumber.toUpperCase().startsWith("NCC"))) {
							setIBAValue(doc, "BOM_NAME", docNumber);
							logger.debug("BOM Name: "+docNumber);
							message = new FeedbackMessage(FeedbackType.SUCCESS, SessionHelper.getLocale(),
									"Item BOM Name filled Successfully", null,
									"Item BOM Name value " + docNumber + " is filled. Check In the Document ");
							FormResult.addFeedbackMessage(message);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
					FormResult = new FormResult();
					FormResult.setNextAction(FormResultAction.NONE);
					FormResult.setStatus(FormProcessingStatus.FAILURE);
					FormResult.addException(e);
					return FormResult;
				}
			}
		}

		logger.debug("ApplDesignDocFormProcessor.postProcess() : END");
		return FormResult;

	}

	/**
	 * @param doc
	 * @return
	 * @throws Exception
	 */
	public static String getIBAValue(WTDocument doc, String iba) throws Exception {
		PersistableAdapter obj = new PersistableAdapter(doc, null, SessionHelper.getLocale(), null);
		obj.load(iba);
		String ibaValue = (String) obj.get(iba);
		if (ibaValue != null)
			logger.debug("BomItem Attribute Value  :" + ibaValue);
		return ibaValue;
	}

	/**
	 * 
	 * @param doc
	 * @param iba
	 * @param value
	 * @throws Exception
	 */
	public static void setIBAValue(WTDocument doc, String iba, String value) throws Exception {
		PersistableAdapter obj = new PersistableAdapter(doc, null, SessionHelper.getLocale(), null);
		obj.load(iba);
		// String ibaValue = (String) obj.get(iba);
		obj.set(iba, value);
		obj.apply();
		PersistenceHelper.manager.modify(doc);
		logger.debug("Setting BOM Name: "+value);

	}

	@Override
	public FormResult preProcess(NmCommandBean arg0, List<ObjectBean> arg1) throws WTException {
		// TODO Auto-generated method stub
		return super.preProcess(arg0, arg1);
	}

	@Override
	public FormResult setResultNextAction(FormResult arg0, NmCommandBean arg1, List<ObjectBean> arg2)
			throws WTException {
		// TODO Auto-generated method stub
		return super.setResultNextAction(arg0, arg1, arg2);
	}

}
