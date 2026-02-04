package ext.emerson.autopart;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.Logger;

import com.ptc.core.foundation.type.server.impl.TypeHelper;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.UpdateOperationIdentifier;
import com.ptc.core.meta.common.impl.WCTypeIdentifier;

import ext.emerson.properties.CustomProperties;
import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.log4j.LogR;
import wt.method.RemoteAccess;
import wt.part.WTPart;
import wt.part.WTPartDescribeLink;
import wt.pom.PersistenceException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.services.ManagerException;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.session.SessionContext;
import wt.session.SessionHelper;
import wt.type.TypeDefinitionReference;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.VersionControlHelper;
import wt.vc.wip.NonLatestCheckoutException;
import wt.vc.wip.WorkInProgressException;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.WorkInProgressServiceEvent;
import wt.vc.wip.Workable;

public class EmersonPreCheckInAutoCreatePart extends StandardManager
		implements EmersonStandardListener_PreCheckIn, RemoteAccess, Serializable {

	private static final long serialVersionUID = 1L;
	private static String CLASSNAME = EmersonPreCheckInAutoCreatePart.class.getName();
	protected static final Logger logger = LogR.getLogger(EmersonPreCheckInAutoCreatePart.class.getName());
	private final static CustomProperties validateProps = new CustomProperties(CustomProperties.VALIDATE);
	private final static CustomProperties autocreateProps = new CustomProperties(CustomProperties.AUTOCREATE);

	public String getCLASSNAME() {
		return CLASSNAME;
	}

	public static EmersonPreCheckInAutoCreatePart newEmersonPreCheckInAutoCreatePart() throws WTException {
		EmersonPreCheckInAutoCreatePart instance = new EmersonPreCheckInAutoCreatePart();

		instance.initialize();
		return instance;
	}

	@Override
	protected synchronized void performStartupProcess() throws ManagerException {
		// TODO Auto-generated method stub
		super.performStartupProcess();
		getManagerService().addEventListener(new ServiceEventListenerAdapter(this.getCLASSNAME()) {
			public void notifyVetoableEvent(Object event) throws Exception {
				WorkInProgressServiceEvent e = (WorkInProgressServiceEvent) event;

				Workable target = e.getOriginalCopy();

				if (target instanceof WTDocument) {
					WTDocument working = (WTDocument) e.getWorkingCopy();
					String itemBomName = getIBAValue(working, "BOM_NAME");
					if (itemBomName != null)
						processPreCheckInAutoPartCreate(working);
					working = (WTDocument) PersistenceHelper.manager.refresh(working, true, true);
				}

			}
		}, WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.PRE_CHECKIN));
	}

	public static String getIBAValue(WTDocument doc, String iba) throws Exception {
		PersistableAdapter obj = new PersistableAdapter(doc, null, SessionHelper.getLocale(), null);
		obj.load(iba);
		String ibaValue = (String) obj.get(iba);
		if (ibaValue != null)
			logger.debug("BomItem Attribute Value  :" + ibaValue);
		return ibaValue;
	}

	@Override
	public void processPreCheckInAutoPartCreate(Object eventtarget) throws WTException {

		SessionContext prev = SessionContext.newContext();
		Boolean transferAtt = false;
		try {
			SessionHelper.manager.setAdministrator();
			WTDocument doc = (WTDocument) eventtarget;
			logger.debug("Doc Number: " + doc.getNumber());
			String itemBomName = getIBAValue(doc, "BOM_NAME");
			WTPart existPart = getWTPartByNumber(itemBomName);
			if (existPart != null) {
				logger.debug("Part already exists with the number: " + itemBomName);
				if (!isLinkExist(existPart, doc))
					createPartDocLink(existPart, doc, transferAtt);
				SessionContext.setContext(prev);
				return;
			}

			TypeIdentifier myCustomType = TypeHelper
					.getTypeIdentifier("WCTYPE|wt.part.WTPart|priv.ia.asco.MECHANICAL_PART");
			WCTypeIdentifier myCustomWCType = (WCTypeIdentifier) myCustomType;
			// find the TypeDefinitionReference
			TypeDefinitionReference myCustomTDR = TypedUtilityServiceHelper.service
					.getTypeDefinitionReference(myCustomWCType.getTypename());

			WTPart part = WTPart.newWTPart();
			part.setTypeDefinitionReference(myCustomTDR);
			part.setNumber(itemBomName);
			part.setName(doc.getName());
			part.setContainer(doc.getContainer());
			WTDocument docTemp = (WTDocument) WorkInProgressHelper.service.originalCopyOf(doc);
			Folder folder1 = FolderHelper.service.getFolder((FolderEntry) docTemp);
			logger.debug("Folder folder service: " + folder1.getFolderPath());
			Folder folder = FolderHelper.service.getFolder(folder1.getFolderPath(), doc.getContainerReference());
			FolderHelper.assignLocation((FolderEntry) part, folder);
			PersistenceHelper.manager.save(part);

			logger.debug(
					"Part created: " + part.getNumber() + ", " + part.getName() + ", " + part.getContainer().getName());
			transferAtt = true;
			createPartDocLink(part, doc, transferAtt);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (SessionContext.getContext() != prev)
				SessionContext.setContext(prev);
		}

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

	public static void createPartDocLink(WTPart part, WTDocument doc, Boolean transferAtt)
			throws NonLatestCheckoutException, WorkInProgressException, WTPropertyVetoException, PersistenceException,
			WTException {

		if (!WorkInProgressHelper.isCheckedOut(part)) {
			part = (WTPart) WorkInProgressHelper.service
					.checkout(part, WorkInProgressHelper.service.getCheckoutFolder(), null).getWorkingCopy();
			if (transferAtt)
				part = trasnferAttDocToPart(part, doc);
			WTPartDescribeLink describeLink = WTPartDescribeLink.newWTPartDescribeLink(part, doc);
			PersistenceHelper.manager.store(describeLink);
			part = (WTPart) WorkInProgressHelper.service.checkin(part, null);
			logger.debug("Link created: " + part.getNumber() + ", " + doc.getNumber());
		}

	}

	public static WTPart trasnferAttDocToPart(WTPart part, WTDocument doc) throws WTException {

		PersistableAdapter docObj = new PersistableAdapter(doc, null, Locale.US, new DisplayOperationIdentifier());
		PersistableAdapter partObj = new PersistableAdapter(part, null, Locale.US, new UpdateOperationIdentifier());
		List<String> attList = autocreateProps.getProperties("TransferAttributes");
		if (!attList.isEmpty()) {
			docObj.load(attList);
			partObj.load(attList);
			System.out.println("List of att to be copied: " + attList);
			for (String att : attList) {
				if (docObj.get(att) != null) {
					if (docObj.get(att) instanceof String) {
						String docAtt = (String) docObj.get(att);
						System.out.println("Value of Att (from Doc): " + att + " is: " + docAtt);
					}
					partObj.set(att, docObj.get(att));
					part = (WTPart) partObj.apply();

				}
			}
			part = (WTPart) PersistenceHelper.manager.modify(part);
		} else {
			System.out.println("Property is null");
		}
		return part;
	}

	public static boolean isLinkExist(WTPart part, WTDocument doc) throws WTException {

		boolean flag = false;
		QueryResult qr = PersistenceHelper.manager.navigate(part, WTPartDescribeLink.DESCRIBED_BY_ROLE,
				WTPartDescribeLink.class, false);
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

		return flag;

	}

}
