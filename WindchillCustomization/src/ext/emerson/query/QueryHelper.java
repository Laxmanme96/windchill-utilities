package ext.emerson.query;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;

import com.google.common.io.Files;
import com.infoengine.object.factory.Group;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.CreateOperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeInstanceIdentifier;
import com.ptc.core.meta.server.TypeIdentifierUtility;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.misc.NmAction;
import com.ptc.netmarkets.util.misc.NmActionServiceHelper;
import com.ptc.windchill.cadx.common.util.WorkspaceUtilities;

import ext.emerson.properties.CustomProperties;
import ext.emerson.windchill.type.util.BusinessObjectType;
import wt.change2.ChangeException2;
import wt.change2.ChangeHelper2;
import wt.change2.ChangeOrder2;
import wt.change2.WTChangeOrder2;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentHolder;
import wt.content.ContentItem;
import wt.content.ContentRoleType;
import wt.content.ContentServerHelper;
import wt.content.HolderToContent;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.enterprise.RevisionControlled;
import wt.epm.EPMDocument;
import wt.epm.build.EPMBuildRule;
import wt.epm.workspaces.EPMWorkspace;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.iba.definition.litedefinition.AttributeDefDefaultView;
import wt.iba.value.DefaultAttributeContainer;
import wt.iba.value.IBAHolder;
import wt.iba.value.IBAValueUtility;
import wt.iba.value.litevalue.AbstractValueView;
import wt.iba.value.service.IBAValueHelper;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.inf.library.WTLibrary;
import wt.inf.team.ContainerTeam;
import wt.inf.team.ContainerTeamHelper;
import wt.maturity.MaturityHelper;
import wt.maturity.Promotable;
import wt.maturity.PromotionNotice;
import wt.org.OrganizationServicesHelper;
import wt.org.WTGroup;
import wt.org.WTPrincipal;
import wt.org.WTPrincipalReference;
import wt.org.WTUser;
import wt.part.WTPart;
import wt.part.WTPartConfigSpec;
import wt.part.WTPartDescribeLink;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartReferenceLink;
import wt.part.WTPartStandardConfigSpec;
import wt.pdmlink.PDMLinkProduct;
import wt.pds.StatementSpec;
import wt.pom.PersistenceException;
import wt.project.Role;
import wt.query.ClassAttribute;
import wt.query.OrderBy;
import wt.query.QueryException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.sandbox.SandboxHelper;
import wt.type.ClientTypedUtility;
import wt.type.TypeDefinitionReference;
import wt.type.TypedUtility;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTContext;
import wt.util.WTException;
import wt.util.WTProperties;
import wt.util.WTPropertyVetoException;
import wt.util.WTRuntimeException;
import wt.vc.Iterated;
import wt.vc.Mastered;
import wt.vc.VersionControlHelper;
import wt.vc.Versioned;
import wt.vc.config.ConfigHelper;
import wt.vc.config.ConfigSpec;
import wt.vc.struct.StructHelper;
import wt.vc.views.View;
import wt.vc.views.ViewHelper;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;

/**
 * @author Pooja Sah Helper class for most CRUD operations in Windchill.
 *
 */
public class QueryHelper {
private static Logger				logger;
protected static ReferenceFactory	referenceFactory	= new ReferenceFactory();
private static Pattern				shortObid, esc, exc;

private static WTProperties			wtprops;
static {
	try {
		wtprops = WTProperties.getLocalProperties();
		logger = CustomProperties.getlogger(QueryHelper.class.getName());

		shortObid = Pattern.compile("([OV]R:[^:]+:\\d+)");
		esc = Pattern.compile("(.*?)%([0-9A-Fa-f]{2})(.*)");
		exc = Pattern.compile(".*<exception-object CONTENT-TYPE.*>(.*)<.*");

	} catch (Throwable e) {
		e.printStackTrace();
		throw new ExceptionInInitializerError(e);
	}
}

protected static <T> void addObject(Object paramObject, Class paramClass, List<T> paramList) {
	if (paramObject instanceof Object[]) {
		for (Object localObject : (Object[]) paramObject)
			addObject(localObject, paramClass, paramList);
	} else {
		if (paramObject == null || paramClass != null && !paramClass.isAssignableFrom(paramObject.getClass()))
			return;
		paramList.add((T) paramObject);
	}
}

public static boolean attachPrimToDoc(String filePath, ContentHolder contentHolder)
		throws WTException, PropertyVetoException, FileNotFoundException, IOException {

	logger.debug("=> attachPrimToDoc");

	contentHolder = ContentHelper.service.getContents(contentHolder);
	// START -- Delete contents if they have same name as the file name to
	// be uploaded
	QueryResult qrContents = ContentHelper.service.getContentsByRole(contentHolder, ContentRoleType.PRIMARY);
	// Get file name of current file to be attached as primary content
	File objFile = new File(filePath);
	String nameOfFileToBeAttached = objFile.getName();
	ApplicationData fileContent = null;
	String nameOfExistingPrimContent = null;
	while (qrContents.hasMoreElements()) {
		fileContent = (ApplicationData) qrContents.nextElement();
		nameOfExistingPrimContent = fileContent.getFileName();
		if (nameOfFileToBeAttached.equalsIgnoreCase(nameOfExistingPrimContent)) {
			deleteExistingPrimContent(contentHolder, fileContent);
		}
	}
	// END -- Delete contents if they have same name as the file name to be
	// uploaded
	ApplicationData objAppData = ApplicationData.newApplicationData(contentHolder);
	objAppData.setRole(ContentRoleType.toContentRoleType("PRIMARY"));
	objAppData = ContentServerHelper.service.updateContent(contentHolder, objAppData, filePath);
	logger.debug("<= attachPrimToDoc");
	return true;
}

public static boolean checkType(String checkType, Object obj) throws WTException {
	TypeIdentifier tiCheck = ClientTypedUtility.getTypeIdentifier(checkType);
	TypeIdentifier tiObj = ClientTypedUtility.getTypeIdentifier(obj);
	// TypeIdentifier tiObj= ClientTypedUtility.getTypeIdentifier(obj);
	if (tiObj.isDescendedFrom(tiCheck) || tiObj.equals(tiCheck)) {
		return true;
	}
	return false;
}

public static boolean checkType(List<String> checkTypes, Object obj) throws WTException {
	for (String type : checkTypes) {

		if (QueryHelper.checkType(type, obj)) {
			return true;
		}
	}
	return false;
}

public static WTDocument createDoc(String number, String name, WTContainerRef wtContainerRef,
		String doctype) throws WTException, WTPropertyVetoException {
	WTDocument doc = WTDocument.newWTDocument();
	doc.setName(name);
	doc.setNumber(number);
	if (wtContainerRef != null) {
		doc.setContainerReference(wtContainerRef);
	} else {
		doc.setContainerReference(WTContainerHelper.service.getExchangeRef());
	}
	// doc.setf
	// String doctype =
	// prop.getProperty("ext.emerson.fpa.change2.bomspreadsheet.type",
	// "wt.doc.WTDocument");
	TypeDefinitionReference typeRef = TypedUtility.getTypeDefinitionReference(doctype);
	if (typeRef != null) {
		doc.setTypeDefinitionReference(typeRef);
	}
	doc = (WTDocument) PersistenceHelper.manager.store(doc);
	// Folder folder = getDefaultFolder(docnumber, container)
	//
	// if (folder != null) {
	// targetLocRef = rf.getReference(rf.getReferenceString(folder));
	// } else {
	// folder = getDefaultFolder(docNumber, (WTContainerRef)
	// rf.getReference(contextObid));
	// targetLocRef = rf.getReference(rf.getReferenceString(folder));
	// }
	return doc;
}

public static WTDocument createNewDoc(String number, String name, WTContainerRef context_ref, CustomProperties prop,
		String doctype) throws WTException, WTPropertyVetoException {
	logger.debug("=> createDoc");
	/* com.ptc.ptcnet.SoftPart is a WTPart soft type */
	PersistableAdapter obj = new PersistableAdapter(doctype, null, new CreateOperationIdentifier());

	/* myString is an multi-valued attribute */
	obj.load("name", "number", "containerReference");
	obj.set("name", name);

	obj.set("containerReference", context_ref);
	if (number != null) {
		obj.set("number", number);
	}
	TypeInstanceIdentifier docInstanceId = obj.persist();
	WTDocument doc = (WTDocument) TypeIdentifierUtility.getObjectReference(docInstanceId).getObject();
	logger.debug("<= createDoc");
	return doc;
}

public static WTDocument createNewDocumentWithContent(String number, String name, WTContainerRef ref, String typeId,
		String templateFilePath, CustomProperties prop) throws WTException, IOException, PropertyVetoException {
	logger.debug("=> createNewDocumentWithContent");
	logger.debug("Number : " + number);
	logger.debug("Name : " + name);
	logger.debug("Container : " + ref);
	logger.debug("typeId : " + typeId);
	// WTDocument doc = createDoc(number, name, ref, prop, typeId.substring(typeId.lastIndexOf("\\|") + 1));
	WTDocument doc = createNewDoc(number, name, ref, prop, typeId);
	if (templateFilePath != null) {
		File templateFile = new File(templateFilePath);
		File newFile = new File(
				templateFile.getParent() + "/" + doc.getNumber() + "." + FilenameUtils.getExtension(templateFilePath));
		Files.copy(templateFile, newFile);
		logger.debug("FILE : " + newFile.getPath());
		if (newFile.exists()) {
			attachPrimToDoc(newFile.getPath(), doc);
			newFile.delete();
		}
	}
	logger.debug("<= createNewDocumentWithContent");
	return doc;
}

/**
 * Function to delete a primary content, if one with the given name exists.
 *
 * @param contentholder
 * @param contentitem
 * @throws WTException
 * @throws WTPropertyVetoException
 */
public static void deleteExistingPrimContent(ContentHolder contentholder, ContentItem contentitem)
		throws WTException, WTPropertyVetoException {
	if (PersistenceHelper.isPersistent(contentitem)) {
		if (contentitem.getHolderLink() == null) {
			QueryResult queryresult = PersistenceHelper.manager.navigate(contentitem, HolderToContent.CONTENT_ITEM_ROLE,
					wt.content.HolderToContent.class, false);
			if (queryresult.hasMoreElements()) {
				contentitem.setHolderLink((HolderToContent) queryresult.nextElement());
			}
		}
		if (contentitem.getHolderLink() != null) {
			PersistenceHelper.manager.delete(contentitem.getHolderLink());
		}
	}
}

public static Map fetchIBA(IBAHolder ibaHolder) throws WTException, RemoteException {
	HashMap ibavalues = new HashMap(); // HashMap to build
	DefaultAttributeContainer dac = null; // IBA attribute container
	String name; // Name of the IBA
	String str; // Temporary string
	// Get attribute container
	ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, null, WTContext.getContext().getLocale(),
			null);
	dac = (DefaultAttributeContainer) ibaHolder.getAttributeContainer();
	AbstractValueView avv[] = dac.getAttributeValues();
	AttributeDefDefaultView addv = null;
	// Looping through the iba values
	for (int i = 0; i < avv.length; i++) {
		addv = avv[i].getDefinition();
		name = addv.getName();
		str = IBAValueUtility.getLocalizedIBAValueDisplayString(avv[i], java.util.Locale.getDefault());
		ibavalues.put(name, str);
	}
	return ibavalues;
}

public static QueryResult getAllEPMDocsLatestIterationInContainer(String containerName) {
	QueryResult qr = null;
	try {

		QuerySpec qs = new QuerySpec();
		int idx = qs.addClassList(EPMDocument.class, true);
		// cs = new ContainerSpec();
		// cs.addSearchContainer((WTContainerRef)
		// referenceFactory.getReference(cr));
		qs.setAdvancedQueryEnabled(true);
		// qs.appendWhere(
		// WTContainerHelper.getWhereContainerIn(cs, new Class[]
		// {EPMDocument.class}),
		// new int[]{idx});
		qs.appendWhere(new SearchCondition(EPMDocument.class, Iterated.LATEST_ITERATION, SearchCondition.IS_TRUE),
				new int[] { idx });
		// qs.appendWhere((WhereExpression) new
		// SearchCondition(EPMDocument.class,EPMDocument.TYPE,
		// SearchCondition.IS_TRUE), new int[]{idx});
		qr = PersistenceHelper.manager.find(qs);
		// }
	} catch (WTException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return qr;
}

public static List<WTPart> getAllPartVersions(String partNumber) throws WTException {

	WTPart part = null;
	QuerySpec qs = new QuerySpec();
	int queryIndex = qs.appendClassList(WTPart.class, true);
	qs.appendWhere(new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, partNumber),
			new int[] { 0 });
	// qs.appendAnd();
	// qs.appendWhere(new SearchCondition(WTPart.class,Iterated.LATEST_ITERATION, SearchCondition.IS_TRUE), new int[]{0});
	qs.appendOrderBy(new OrderBy(new ClassAttribute(WTPart.class, "master>name"), false), new int[] { queryIndex });
	qs.appendOrderBy(new OrderBy(new ClassAttribute(WTPart.class, "versionInfo.identifier.versionSortId"), true),
			new int[] { queryIndex });
	qs.appendOrderBy(new OrderBy(new ClassAttribute(WTPart.class, "iterationInfo.identifier.iterationId"), true),
			new int[] { queryIndex });

	QueryResult qr = PersistenceHelper.manager.find(qs);
	List<WTPart> parts = new ArrayList<>();
	while (qr.hasMoreElements()) {
		Persistable[] qresult = (Persistable[]) qr.nextElement();
		Persistable p = qresult[queryIndex];
		ReferenceFactory rf = new ReferenceFactory();
		part = (WTPart) rf.getReference(p).getObject();
		parts.add(part);
	}
	return parts;

}

/**
 * @param part
 * @return
 * @throws WTException
 */
public static List<EPMDocument> getAssociatedImageDocs(WTPart part) throws WTException {
	QueryResult qr2;
	List<EPMDocument> list = new ArrayList<>();
	qr2 = PersistenceHelper.manager.navigate(part, EPMBuildRule.ROLE_AOBJECT_ROLE, EPMBuildRule.class, false);
	logger.info("Related EPMBuildRule Image Doc size - " + qr2.size());
	for (; qr2.hasMoreElements();) {
		EPMBuildRule epmDocLink = (EPMBuildRule) qr2.nextElement();
		if (epmDocLink.getBuildType() == 4 || epmDocLink.getBuildType() == 6) {
			list.add((EPMDocument) epmDocLink.getRoleAObject());
		}
	}
	return list;
}

/**
 * @param part
 * @return
 * @throws WTException
 */
public static EPMDocument getAssociatedOwnerDoc(WTPart part) throws WTException {
	QueryResult qr2;
	qr2 = PersistenceHelper.manager.navigate(part, EPMBuildRule.BUILD_SOURCE_ROLE, EPMBuildRule.class, false);
	logger.info("Related EPMBuildRule Doc size - " + qr2.size());
	for (; qr2.hasMoreElements();) {
		EPMBuildRule epmDocLink = (EPMBuildRule) qr2.nextElement();
		if (epmDocLink.getBuildType() == 7) {
			return (EPMDocument) epmDocLink.getRoleAObject();
		}
	}
	return null;
}

public static String getCheckedOutBy(Workable workable) throws WTException {
	String str = "";
	if (isCheckedOutorLocked(workable)) {
		ObjectReference obref = (ObjectReference) WorkInProgressHelper.service.allWorkingCopiesOf(workable)
				.toArray()[0];
		logger.debug("allWorkingCopiesOf.getIterationInfo().getModifier().getFullName() : "
				+ ((Workable) obref.getObject()).getIterationInfo().getModifier().getFullName() + " iter : "
				+ ((Workable) obref.getObject()).getIdentity());

		logger.debug("workable.getIterationInfo().getModifier().getFullName() : "
				+ workable.getIterationInfo().getModifier().getFullName() + "workable iter : "
				+ workable.getIdentity());
		return ((Workable) obref.getObject()).getIterationInfo().getModifier().getFullName();

	}
	return "";
}

/**
 * Returns all the described by WTDocuments on the provided part.
 *
 * @param part
 *            The part to search for related documents.
 *
 * @return A set with WTDocuments. Will be empty if no documents are found.
 * @throws WTException
 */
public static Set<WTDocument> getDescribedByWTDocuments(WTPart part) throws WTException {

	logger.debug("=> getDescribedByDocuments(WTPart part)");
	logger.debug("part: " + part.getIdentity());

	Set<WTDocument> set = new HashSet<>();
	QueryResult qr = WTPartHelper.service.getDescribedByDocuments(part);

	while (qr.hasMoreElements()) {
		Object o = qr.nextElement();
		if (o instanceof WTDocument) {
			logger.debug("Found the WTDocument: " + ((WTDocument) o).getIdentity());
			set.add((WTDocument) o);
		} else if (o instanceof ObjectReference) {
			WTDocument doc = (WTDocument) ((ObjectReference) o).getObject();
			logger.debug("Found the WTDocument from reference: " + doc.getIdentity());
			set.add(doc);
		}
	}
	logger.debug("<= getDescribedByDocuments(WTPart part)");

	return set;
}

//
// private void isEPM(Object primaryBusinessObject) {
// String result;
//
// result = "hasNoEPMDocuments";
//
// String checkType = "WCTYPE|wt.epm.EPMDocument";
// com.ptc.core.meta.common.TypeIdentifier tiCheck;
// try {
// tiCheck = wt.type.ClientTypedUtility.getTypeIdentifier(checkType);
//
// wt.fc.QueryResult qr = wt.maturity.MaturityHelper.service
// .getPromotionTargets((wt.maturity.PromotionNotice) primaryBusinessObject);
//
// while (qr.hasMoreElements()) {
//
// wt.fc.Persistable obj = (wt.fc.Persistable) qr.nextElement();
// com.ptc.core.meta.common.TypeIdentifier tiObj = wt.type.ClientTypedUtility.getTypeIdentifier(obj);
//
// if (tiObj.isDescendedFrom(tiCheck) || tiObj.equals(tiCheck)) {
// result = "hasEPMDocuments";
// break;
// }
//
// }
//
// } catch (WTException e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// }
// }

public static WTUser getDisconnectedUser(String userName) throws WTException {
	Vector vect = OrganizationServicesHelper.manager.listofInvalidPrincipals(WTUser.class, userName);

	for (Object obj : vect) {
		if (obj instanceof WTUser) {
			WTUser user = (WTUser) obj;
			return user;
		}
	}
	return null;

}

public static WTDocument getDoc(String docNumber, String version, String iteration, String doctype) throws WTException {
	WTDocument doc = null;
	QuerySpec criteria = new QuerySpec(WTDocument.class);
	criteria.appendSearchCondition(
			new SearchCondition(WTDocument.class, WTDocument.NUMBER, SearchCondition.EQUAL, docNumber, false));
	if (version != null && version.trim().length() > 0) {
		criteria.appendAnd();
		criteria.appendWhere(
				new SearchCondition(WTDocument.class, "versionInfo.identifier.versionId", "=", version, false));
	}
	if (iteration != null && iteration.trim().length() > 0) {
		criteria.appendAnd();
		criteria.appendWhere(
				new SearchCondition(WTDocument.class, "iterationInfo.identifier.iterationId", "=", iteration, false));
	}

	if (doctype != null && iteration.trim().length() > 0) {

		TypeDefinitionReference typeDefRef = null;
		try {
			typeDefRef = TypedUtilityServiceHelper.service.getTypeDefinitionReference(doctype);

		} catch (RemoteException e) {

			e.printStackTrace();
		}

		criteria.appendAnd();
		criteria.appendWhere(new SearchCondition(WTDocument.class, "typeDefinitionReference.key.id",
				SearchCondition.EQUAL, typeDefRef.getKey().getId()));

	}
	QueryResult results = PersistenceHelper.manager.find(criteria);
	if (results.hasMoreElements()) {
		doc = (WTDocument) results.nextElement();
		if (version == null || version.equals("")) {
			QueryResult qr = VersionControlHelper.service.allVersionsOf(doc);
			if (qr.hasMoreElements()) {
				return (WTDocument) qr.nextElement();
			}
		}
	}
	logger.debug("Document found :" + doc);
	return doc;
}

public static EPMDocument getEPMDocument(String number) throws PersistenceException, WTException {
	EPMDocument result = null;

	QuerySpec qs = new QuerySpec(EPMDocument.class);
	qs.appendWhere(new SearchCondition(EPMDocument.class, EPMDocument.NUMBER, SearchCondition.EQUAL, number));
	QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
	// QueryResult qr = ConfigHelper.service.queryIterations(qs, getDefaultConfigSpec(EPMDocument.class));
	if (qr.hasMoreElements()) {
		result = (EPMDocument) qr.nextElement();
	}

	return result;
}

public static EPMDocument getLatestEPMDocument(String epmNumber) throws WTException {
	try {
		QuerySpec partSpec = new QuerySpec(wt.epm.EPMDocument.class);
		int[] fromIndicies = { 0, 1 };
		partSpec.appendWhere(
				new SearchCondition(wt.epm.EPMDocument.class, "master>number", SearchCondition.EQUAL, epmNumber, false),
				fromIndicies);
		QueryResult queryresult = PersistenceHelper.manager.find((StatementSpec) partSpec);
		if (queryresult.hasMoreElements()) {
			return (EPMDocument) getLatestObject((EPMDocument) queryresult.nextElement());

		}
	} catch (WTException wte) {
		logger.debug("QueryHelper.getPart:ERROR: while trying to Query for EPMDocument with Number:" + epmNumber);
		throw wte;
	}

	return null;

}

public static RevisionControlled getLatestObject(Mastered object) throws PersistenceException, WTException {

	return (RevisionControlled) VersionControlHelper.service.allVersionsOf(object).nextElement();

}

public static RevisionControlled getLatestObject(Versioned object) throws PersistenceException, WTException {
	return (RevisionControlled) VersionControlHelper.service.allVersionsOf(object).nextElement();
}

public static WTPart getLatestPart(String partNumber) throws WTException {

	try {
		QuerySpec partSpec = new QuerySpec(WTPart.class);
		int[] fromIndicies = { 0, 1 };
		partSpec.appendWhere(
				new SearchCondition(WTPart.class, "master>number", SearchCondition.EQUAL, partNumber, false),
				fromIndicies);
		QueryResult queryresult = PersistenceHelper.manager.find((StatementSpec) partSpec);
		if (queryresult.hasMoreElements()) {
			return (WTPart) getLatestObject((WTPart) queryresult.nextElement());

		}
	} catch (WTException wte) {
		logger.debug("QueryHelper.getPart:ERROR: while trying to Query for Part with Number:" + partNumber);
		wte.printStackTrace();

	}

	return null;
}

/**
 * Returns the latest WTPart object with number=<name> and view=<view>
 *
 * @param number
 * @param view_name
 * @return
 * @throws WTException
 */
public static WTPart getLatestPartByView(String number, String view_name, List<String> validTypes) throws WTException {

	try {
		QuerySpec querySpec = new QuerySpec(WTPartMaster.class);
		SearchCondition sc = new SearchCondition(WTPartMaster.class, WTPartMaster.NUMBER, SearchCondition.EQUAL,
				number);
		querySpec.appendWhere(sc, new int[] { 0, -1 });
		QueryResult qr = PersistenceHelper.manager.find(querySpec);

		WTPartStandardConfigSpec stdSpec = WTPartStandardConfigSpec.newWTPartStandardConfigSpec();
		View view = ViewHelper.service.getView(view_name);
		stdSpec.setView(view);
		WTPartConfigSpec configSpec = WTPartConfigSpec.newWTPartConfigSpec(stdSpec);

		QueryResult qr2 = ConfigHelper.service.filteredIterationsOf(qr, configSpec);
		while (qr2.hasMoreElements()) {
			WTPart tempPart = (WTPart) qr2.nextElement();
			if (tempPart.getViewName().equalsIgnoreCase(view_name)) {
				for (String validType : validTypes) {
					if (tempPart != null && QueryHelper.checkType(validType, tempPart)) {
						logger.debug("View : " + view.getName() + " Part found : " + tempPart.getIdentity() + ", "
								+ tempPart.getIterationDisplayIdentifier());
						return tempPart;
					}
				}
			}
		}

		// if (qr.hasMoreElements()) {
		// WTPartMaster object = (WTPartMaster) qr.nextElement();
		// QueryResult qr2 = ConfigHelper.service.filteredIterationsOf(object, configSpec);
		// while (qr2.hasMoreElements()) {
		// WTPart tempPart = (WTPart) qr2.nextElement();
		// logger.debug("view : " + view.getName() + " tempPart : " + tempPart.getIdentity());
		// for (String validType : validTypes) {
		// logger.debug("validType" + validType);
		// if (tempPart != null && QueryHelper.checkType(validType, tempPart)) {
		// return tempPart;
		// }
		// }
		// }
		// }
	} catch (Exception e) {
		logger.debug("Exception in getLatestPartByView() " + e);
		e.printStackTrace();
	}
	return null;
}

public static WTPart getLatestPartQuery(String partNumber) throws WTException {

	WTPart part = null;
	QuerySpec qs = new QuerySpec();
	int queryIndex = qs.appendClassList(WTPart.class, true);
	qs.appendWhere(new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, partNumber),
			new int[] { 0 });
	// qs.appendAnd();
	// qs.appendWhere(new SearchCondition(WTPart.class,Iterated.LATEST_ITERATION, SearchCondition.IS_TRUE), new int[]{0});
	qs.appendOrderBy(new OrderBy(new ClassAttribute(WTPart.class, "master>name"), false), new int[] { queryIndex });
	qs.appendOrderBy(new OrderBy(new ClassAttribute(WTPart.class, "versionInfo.identifier.versionSortId"), true),
			new int[] { queryIndex });
	qs.appendOrderBy(new OrderBy(new ClassAttribute(WTPart.class, "iterationInfo.identifier.iterationId"), true),
			new int[] { queryIndex });

	QueryResult qr = PersistenceHelper.manager.find(qs);

	if (qr.hasMoreElements()) {
		Persistable[] qresult = (Persistable[]) qr.nextElement();
		Persistable p = qresult[queryIndex];
		ReferenceFactory rf = new ReferenceFactory();
		part = (WTPart) rf.getReference(p).getObject();
		logger.debug("\t" + part.getIdentity() + ", v: " + wt.vc.VersionControlHelper
				.getIterationDisplayIdentifier(part).getLocalizedMessage(java.util.Locale.getDefault()));
	}
	return part;

}

public static <T> List<T> getListFromQueryResult(QueryResult paramQueryResult) throws WTException {
	return getListFromQueryResult(paramQueryResult, null);
}

public static <T> List<T> getListFromQueryResult(QueryResult paramQueryResult, Class paramClass) throws WTException {
	if (paramQueryResult == null) {
		return null;
	}
	paramQueryResult.reset();
	ArrayList localArrayList = new ArrayList(paramQueryResult.size());
	while (paramQueryResult.hasMoreElements()) {
		addObject(paramQueryResult.nextElement(), paramClass, localArrayList);
	}
	return localArrayList;
}

public static WTDocument getNewOrExistingDocument(String number, String name, Object ref, String typeId,
		String templateFilePath) throws Exception {
	logger.debug("Number : " + number);
	logger.debug("Name : " + name);
	logger.debug("Container : " + ((String[]) ref)[0]);
	logger.debug("typeId : " + typeId);
	Persistable t2 = ((ObjectReference) referenceFactory.getReference(((String[]) ref)[0])).getObject();
	WTDocument existingDoc = getDoc(number, "", "", typeId);
	if (existingDoc != null) {
		return existingDoc;
	}
	WTDocument doc = createDoc(number, name, (WTContainerRef) referenceFactory.getReference(t2),
			typeId.split("\\|")[3]);
	if (templateFilePath != null) {
		File templateFile = new File(templateFilePath);
		File newFile = new File(templateFile.getParent() + "/" + doc.getNumber() + ".xlsx");
		Files.copy(templateFile, newFile);
		logger.debug("FILE : " + newFile.getPath());
		if (newFile.exists()) {
			attachPrimToDoc(newFile.getPath(), doc);
			newFile.delete();
		}
	}
	return doc;
}

public static String getNumber(Persistable persistable) throws WTException {

	String number;
	if (checkType(BusinessObjectType.WTDOCUMENT, persistable)) {
		return ((WTDocument) persistable).getNumber();
	}
	if (checkType(BusinessObjectType.EPMDOCUMENT, persistable)) {
		return ((EPMDocument) persistable).getNumber();
	}
	if (checkType(BusinessObjectType.WTPART, persistable)) {
		return ((WTPart) persistable).getNumber();
	}
	return persistable.getIdentity();

}

public static Persistable getObject(String paramString) throws WTException {
	if (paramString == null || paramString.isEmpty()) {
		throw new RuntimeException("oid is null");
	}
	WTReference localWTReference = getReference(paramString);
	if (localWTReference == null) {
		throw new WTException("Reference not found " + paramString);
	}
	return localWTReference.getObject();
}

public static String getObjectURL(Group objectg, String obid) {
	String href = "";
	try {
		// Persistable object = (Persistable) PersistenceServerHelper.manager.restore(new ObjectIdentifier(obid));
		Persistable object = new ReferenceFactory().getReference(obid).getObject();
		com.ptc.netmarkets.model.NmOid tgtOid = null;
		tgtOid = new com.ptc.netmarkets.model.NmOid(wt.fc.PersistenceHelper.getObjectIdentifier(object));
		com.ptc.netmarkets.util.misc.NmAction infoPageAction;
		infoPageAction = NmActionServiceHelper.service.getAction(NmAction.Type.OBJECT, "view");
		infoPageAction.setContextObject(tgtOid);
		infoPageAction.setIcon(null);
		href = infoPageAction.getActionUrlExternal();
		logger.debug("link: " + href);
	} catch (Exception e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	return href;
}

public static String getObjectURL(String obid) {
	String href = "";
	try {
		// Persistable object = (Persistable) PersistenceServerHelper.manager.restore(new ObjectIdentifier(obid));
		Persistable object = new ReferenceFactory().getReference(obid).getObject();
		NmOid tgtOid = null;
		tgtOid = new com.ptc.netmarkets.model.NmOid(PersistenceHelper.getObjectIdentifier(object));
		NmAction infoPageAction;
		infoPageAction = NmActionServiceHelper.service.getAction(com.ptc.netmarkets.util.misc.NmAction.Type.OBJECT,
				"view");
		infoPageAction.setContextObject(tgtOid);
		infoPageAction.setIcon(null);
		href = infoPageAction.getActionUrlExternal();
		logger.debug("link: " + href);
	} catch (Exception e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	return href;
}

public static String getOid(Persistable paramPersistable) throws WTException {
	if (paramPersistable == null)
		throw new RuntimeException("persistable is null");
	return referenceFactory.getReferenceString(paramPersistable);
}

public static WTPart getPart(String partNumber, String version, String iteration) throws WTException {
	WTPart part = null;
	QuerySpec criteria = new QuerySpec(WTPart.class);
	criteria.appendSearchCondition(
			new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, partNumber, false));
	criteria.appendAnd();
	criteria.appendWhere(new SearchCondition(WTPart.class, "versionInfo.identifier.versionId", "=", version, false));
	criteria.appendAnd();
	criteria.appendWhere(
			new SearchCondition(WTPart.class, "iterationInfo.identifier.iterationId", "=", iteration, false));
	QueryResult results = PersistenceHelper.manager.find(criteria);
	while (results.hasMoreElements()) {
		part = (WTPart) results.nextElement();
	}
	return part;
}

public static WTPartMaster getPartMaster(String orgNumber) throws WTException {

	WTPart part = getLatestPartQuery(orgNumber);
	if (part != null) {
		return part.getMaster();
	}

	return null;
}

public static String getPBOName(Object primaryBusinessObject) {
	if (primaryBusinessObject instanceof WTChangeOrder2) {
		return ((WTChangeOrder2) primaryBusinessObject).getName();
	}
	return "";
}

public static String getPBONumber(Object primaryBusinessObject) {
	if (primaryBusinessObject instanceof WTChangeOrder2) {
		return ((WTChangeOrder2) primaryBusinessObject).getNumber();
	}
	return "";
}

public static WTPrincipal getPrincipal(String name) throws WTException {
	WTPrincipal principal = null;
	if (name != null && !name.equals("")) {

		String[] services = OrganizationServicesHelper.manager.getDirectoryServiceNames();

		// for site groups:
		wt.org.DirectoryContextProvider dc_provider = OrganizationServicesHelper.manager
				.newDirectoryContextProvider(services, null);

		Enumeration e = OrganizationServicesHelper.manager.getPrincipal(name.trim(), dc_provider);
		while (e.hasMoreElements()) {
			principal = (WTPrincipal) e.nextElement();
			logger.debug("Active Principal found : " + principal.getIdentity());
		}
		if (principal == null) {

			principal = getDisconnectedUser(name);
			if (principal != null) {
				logger.debug("Disconnected Principal found " + principal.getName());
				if (principal.getName() != null && principal.getName().equals(name)) {
					return principal;
				}
			}
		}
	}
	return principal;
}

/**
 * Given a product name will returns PDMLinkProduct.
 *
 * @param productName
 * @throws WTException
 */
public static PDMLinkProduct getProduct(String productName) throws WTException {
	PDMLinkProduct product = null; // To return
	QuerySpec qspec; // To query
	QueryResult qr; // To query

	qspec = new QuerySpec(wt.pdmlink.PDMLinkProduct.class);
	qspec.appendWhere(new SearchCondition(wt.pdmlink.PDMLinkProduct.class, PDMLinkProduct.NAME, SearchCondition.EQUAL,
			productName));
	qr = PersistenceHelper.manager.find(qspec);
	while (qr.hasMoreElements()) {
		product = (PDMLinkProduct) qr.nextElement();
	}
	return product;
}

public static List<Promotable> getPromotables(PromotionNotice pn) throws WTException {
	QueryResult qr = MaturityHelper.service.getPromotionTargets(pn);
	List<Promotable> promotables = QueryHelper.getListFromQueryResult(qr, WTObject.class);
	return promotables;
}// getPromotableTargets

public static List<Promotable> getPromotableTargets(PromotionNotice paramPromotionNotice) throws WTException {
	QueryResult localQueryResult = MaturityHelper.service.getPromotionTargets(paramPromotionNotice);
	List localList = getListFromQueryResult(localQueryResult, WTObject.class);
	return localList;
}

public static WTReference getReference(String paramString) throws WTException {
	if (paramString == null || paramString.isEmpty()) {
		throw new RuntimeException("oid is null");
	}
	int i = paramString.indexOf(126);
	paramString = paramString.substring(i + 1);
	return referenceFactory.getReference(paramString);
}

/**
 * Returns all the referenced by latest WTDocuments on the provided part.
 *
 * @param part
 *            The part to search for related documents.
 *
 * @return A set with WTDocuments. Will be empty if no documents are found.
 * @throws WTException
 */
public static Set<WTDocument> getReferencedByWTDocuments(WTPart part) throws WTException {

	logger.debug("=> getReferencedByWTDocuments(WTPart part) : " + part.getIdentity());

	Set<WTDocument> set = new HashSet<>();
	QueryResult queryReferences = StructHelper.service.navigateReferences(part, WTPartReferenceLink.class, true);
	while (queryReferences.hasMoreElements()) {
		Object o = queryReferences.nextElement();

		if (o instanceof WTDocumentMaster) {
			set.add((WTDocument) getLatestObject((Mastered) o));
		} else if (o instanceof ObjectReference) {
			Persistable temp = ((ObjectReference) o).getObject();
			if (temp instanceof WTDocumentMaster) {
				logger.debug("Found the WTDocument: " + ((WTDocumentMaster) temp).getIdentity());
				set.add((WTDocument) getLatestObject((Mastered) temp));
			}

		}
	}
	logger.debug("<= getReferencedByWTDocuments(WTPart part)");

	return set;
}

public static ArrayList<WTDocument> getRelatedWTDocuments(WTPart part) throws WTException {

	QueryResult qr = PersistenceHelper.manager.navigate(part, WTPartDescribeLink.DESCRIBED_BY_ROLE,
			wt.part.WTPartDescribeLink.class, true);
	ArrayList<WTDocument> listDocs = new ArrayList<>();
	while (qr.hasMoreElements()) {
		WTDocument doc = (WTDocument) qr.nextElement();
		listDocs.add(doc);
	}
	qr = PersistenceHelper.manager.navigate(part, WTPartReferenceLink.REFERENCES_ROLE,
			wt.part.WTPartReferenceLink.class, true);

	while (qr.hasMoreElements()) {
		WTDocument doc = (WTDocument) getLatestObject((WTDocumentMaster) qr.nextElement());
		listDocs.add(doc);
	}

	return listDocs;

}

public static List getResultingData(ChangeOrder2 ecn, boolean b) throws ChangeException2, WTException {

	QueryResult qrlink = ChangeHelper2.service.getChangeablesAfter(ecn, b);
	return QueryHelper.getListFromQueryResult(qrlink);
}

/**
 * Retrieve user given the role played by user in PDMLink.
 *
 * @param contextRef
 * @param rolename
 * @throws wt.query.QueryException
 * @throws wt.util.WTException
 * @return user
 */
public static Set<WTUser> getUsersFromContext(String contextRef, String rolename) throws QueryException, WTException {
	ReferenceFactory ref = new ReferenceFactory();
	WTUser user = null;
	ContainerTeam team = null;
	Object obj; // To hold Product or Library.
	Set<WTUser> users = new HashSet<>();
	obj = ref.getReference(contextRef).getObject();

	if (obj instanceof PDMLinkProduct) {
		team = ContainerTeamHelper.service.getContainerTeam((PDMLinkProduct) obj);
	} else if (obj instanceof WTLibrary) {
		team = ContainerTeamHelper.service.getContainerTeam((WTLibrary) obj);
	}

	ArrayList<WTPrincipalReference> allMembers = team.getAllPrincipalsForTarget(Role.toRole(rolename));

	for (WTPrincipalReference principalRef : allMembers) {
		Object principal = ref.getReference(ref.getReferenceString(principalRef)).getObject();
		if (principal instanceof WTUser) {
			user = (WTUser) principal;
			users.add(user);
		} else if (principal instanceof WTGroup) {
			Enumeration<WTUser> groupUsers = OrganizationServicesHelper.manager.members((WTGroup) principal, true);
			while (groupUsers.hasMoreElements()) {
				WTUser selectedUser = groupUsers.nextElement();
				logger.debug("Found user in group : " + selectedUser.getName());
				users.add(selectedUser);
			}

		}
	}

	return users;
}

public static boolean hasChangeAttachments(String contentHolderRefString) throws WTException, PropertyVetoException {
	logger.debug("contentHolderRefString = " + contentHolderRefString);
	// contentHolder = (WTChangeOrder2) ContentHelper.service.getContents(contentHolder);
	// QueryResult qrContents = ContentHelper.service.getContentsByRole(contentHolder, ContentRoleType.SECONDARY);
	// if (qrContents != null && qrContents.hasMoreElements()) {
	// // This ECN (PCN) has attached documents"
	// return true;
	// }
	return false;
}

public static Persistable inflatePersistable(String obid) throws WTRuntimeException, WTException {
	return new ReferenceFactory().getReference(obid).getObject();
}

public static boolean isCheckedOutorLocked(Workable workable) throws WTException {

	if (workable != null) {

		if (WorkInProgressHelper.isCheckedOut(workable) || WorkInProgressHelper.isWorkingCopy(workable)
				|| WorkInProgressHelper.service.isLocked(workable)) {
			return true;
		}
		EPMWorkspace localEPMWorkspace = WorkspaceUtilities.getCheckoutWorkspace(workable);

		if (localEPMWorkspace != null) {
			return WorkspaceUtilities.isCheckedOut(workable, localEPMWorkspace);
		}
		if (SandboxHelper.isCheckedOutToSandbox(workable)) {
			return true;
		}

	}
	return false;

}

/**
 * extract the short ([OV]R:class.name:seqnr) obid from a ufid
 *
 * @param longObid
 * @param throwException
 *
 * @return String
 *
 * @exception Exception
 */
public static String shortObid(Object longObid, boolean throwException) throws WTException {
	Matcher r;

	if ((r = shortObid.matcher(longObid.toString())).find()) {
		return r.group(1);
	}
	// retry with OR:
	if ((r = shortObid.matcher("OR:" + longObid)).find()) {
		return r.group(1);
	}
	if (throwException) {
		throw new WTException("cannot find a short obid in " + longObid);
	}
	return longObid.toString();
}

protected Versioned compareVersion(Versioned v1, Versioned v2) throws WTException {

	if (v1.getVersionIdentifier().getSeries().lessThan(v2.getVersionIdentifier().getSeries())) {

		return v2;
	} else {
		return v1;
	}
}

private ConfigSpec getDefaultConfigSpec(Class class1) throws WTException {
	return ConfigHelper.service.getDefaultConfigSpecFor(class1);
}
}
/*
 * Location: D:\ptc\Windchill_10.2\Windchill\codebase\ext\emerson\query\QueryHelper Qualified Name: ext.emerson.query.QueryHelper Java Class Version: JD-Core Version:
 */
