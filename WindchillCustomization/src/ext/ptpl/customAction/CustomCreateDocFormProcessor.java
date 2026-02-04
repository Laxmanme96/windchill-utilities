package ext.ptpl.customAction;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.forms.FormResultAction;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.impl.WCTypeIdentifier;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.dpimpl.util.TypeHelper;
import com.ptc.windchill.enterprise.doc.forms.CreateDocFormProcessor;
import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.part.WTPartDescribeLink;
import wt.pom.PersistenceException;
import wt.session.SessionHelper;
import org.apache.logging.log4j.*;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.wip.NonLatestCheckoutException;
import wt.vc.wip.WorkInProgressException;
import wt.vc.wip.WorkInProgressHelper;
import wt.type.TypeDefinitionReference;
import wt.type.TypedUtilityServiceHelper;

public class CustomCreateDocFormProcessor extends CreateDocFormProcessor {

	protected static final Logger logger = LogR.getLogger(CustomCreateDocFormProcessor.class.getName());

	@Override
	public FormResult doOperation(NmCommandBean clientData, List<ObjectBean> objectBeans) throws WTException {
		logger.debug("CustomCreateDocFormProcessor.postProcess() : START");
		Object object = null;
		WTDocument doc = null;
		String docNumber = null;
		FormResult FormResult = new FormResult();
		super.doOperation(clientData, objectBeans);

		Map checkedMap = clientData.getChecked();
		Object listofCheckBox = checkedMap.get("generatePartNumberCheckBox");
		boolean customCheckBox = listofCheckBox != null; // ? true : false;

		if (!customCheckBox)
			return FormResult;
		else {
			for (ObjectBean objBean : objectBeans) {
				object = objBean.getObject();
				if (object instanceof WTDocument) {
					doc = (WTDocument) object;
					docNumber = doc.getNumber();
					System.out.println("Doc Number: " + docNumber);
					try {
						TypeIdentifier myCustomType = com.ptc.core.foundation.type.server.impl.TypeHelper
								.getTypeIdentifier("WCTYPE|wt.part.WTPart|com.ptc.ElectricalPart");
						WCTypeIdentifier myCustomWCType = (WCTypeIdentifier) myCustomType;
						TypeDefinitionReference myCustomTDR = TypedUtilityServiceHelper.service
								.getTypeDefinitionReference(myCustomWCType.getTypename());

						WTPart part = WTPart.newWTPart();
						part.setTypeDefinitionReference(myCustomTDR);
						part.setNumber(docNumber);
						part.setName(doc.getName());
						part.setContainer(doc.getContainer());

						Folder folder1 = FolderHelper.service.getFolder((FolderEntry) doc);
						logger.debug("Folder folder service: " + folder1.getFolderPath());
						Folder folder = FolderHelper.service.getFolder(folder1.getFolderPath(),
								doc.getContainerReference());
						FolderHelper.assignLocation((FolderEntry) part, folder);

						PersistenceHelper.manager.save(part);
												
						logger.debug("Part created: " + part.getNumber() + ", " + part.getName() + ", "
								+ part.getContainer().getName());
						
						createPartDocLink(part, doc);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			logger.debug("CustomCreateDocFormProcessor.postProcess() : END");
			return FormResult;
		}
	}

	public static void createPartDocLink(WTPart part, WTDocument doc)
			throws NonLatestCheckoutException, WorkInProgressException, WTPropertyVetoException, PersistenceException,
			WTException {

		if (!WorkInProgressHelper.isCheckedOut(part)) {
			part = (WTPart) WorkInProgressHelper.service
					.checkout(part, WorkInProgressHelper.service.getCheckoutFolder(), null).getWorkingCopy();
				WTPartDescribeLink describeLink = WTPartDescribeLink.newWTPartDescribeLink(part, doc);
				PersistenceHelper.manager.store(describeLink);
				part = (WTPart) WorkInProgressHelper.service.checkin(part, null);
				logger.debug("Link created: " + part.getNumber() + ", " + doc.getNumber());

			}

		}

	}

