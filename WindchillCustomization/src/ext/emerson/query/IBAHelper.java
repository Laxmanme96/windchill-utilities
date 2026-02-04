package ext.emerson.query;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.lwc.common.view.AttributeDefaultValueReadView;
import com.ptc.core.lwc.common.view.AttributeDefinitionReadView;
import com.ptc.core.lwc.common.view.TypeDefinitionReadView;
import com.ptc.core.lwc.server.LWCEnumerationEntryValuesFactory;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;
import com.ptc.core.meta.common.DataSet;
import com.ptc.core.meta.common.EnumeratedSet;
import com.ptc.core.meta.common.EnumerationEntryIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.impl.TypeIdentifierUtilityHelper;
import com.ptc.core.meta.container.common.AttributeTypeSummary;

import ext.emerson.properties.CustomProperties;
import ext.emerson.windchill.iba.IBAHandler;
import ext.emerson.windchill.iba.IBAHandler.IBACreateMode;
import wt.enterprise.RevisionControlled;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTReference;
import wt.iba.definition.litedefinition.AttributeDefDefaultView;
import wt.iba.value.DefaultAttributeContainer;
import wt.iba.value.IBAHolder;
import wt.iba.value.IBAValueUtility;
import wt.iba.value.litevalue.AbstractValueView;
import wt.iba.value.service.IBAValueHelper;
import wt.meta.LocalizedValues;
import wt.part.WTPart;
import wt.pds.StatementSpec;
import wt.pom.PersistenceException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.type.ClientTypedUtility;
import wt.util.WTContext;
import wt.util.WTException;
import wt.vc.Iterated;
import wt.vc.VersionControlHelper;
import wt.vc.Versioned;
import wt.vc.config.ConfigHelper;
import wt.vc.config.ConfigSpec;

/**
 * @author Pooja Sah
 *
 *
 */
public class IBAHelper {
private static Logger				logger				= CustomProperties.getlogger(IBAHelper.class.getName());
protected static ReferenceFactory	referenceFactory	= new ReferenceFactory();

protected static <T> void addObject(Object paramObject, Class paramClass, List<T> paramList) {
	if (paramObject instanceof Object[]) {
		for (Object localObject : (Object[]) paramObject) {
			addObject(localObject, paramClass, paramList);
		}
	} else {
		if (paramObject == null || paramClass != null && !paramClass.isAssignableFrom(paramObject.getClass())) {
			return;
		}
		paramList.add((T) paramObject);
	}
}

public static boolean checkType(String checkType, Object obj) throws WTException {
	TypeIdentifier tiCheck = ClientTypedUtility.getTypeIdentifier(checkType);
	logger.debug("tiCheck = " + tiCheck);
	// obj: wt.doc.WTDocument
	TypeIdentifier tiObj = ClientTypedUtility.getTypeIdentifier(obj);
	if (tiObj.isDescendedFrom(tiCheck) || tiObj.equals(tiCheck)) {
		return true;
	}
	return false;
}

public static HashMap<String, ArrayList<String>> fetchIBA(IBAHolder ibaHolder) throws WTException, RemoteException {
	HashMap<String, ArrayList<String>> ibavalues = new HashMap<>(); // HashMap
	// to
	// build
	DefaultAttributeContainer dac = null; // IBA attribute container
	String name; // Name of the IBA
	ArrayList<String> list; //
	if (ibaHolder != null) {
		// Get attribute container
		ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, null,
				WTContext.getContext().getLocale(), null);
		dac = (DefaultAttributeContainer) ibaHolder.getAttributeContainer();
		AbstractValueView avv[] = dac.getAttributeValues();
		AttributeDefDefaultView addv = null;

		// Looping through the iba values
		for (int i = 0; i < avv.length; i++) {
			list = new ArrayList<>();
			addv = avv[i].getDefinition();
			name = addv.getName();
			String str;

			str = IBAValueUtility.getLocalizedIBAValueDisplayString(avv[i], java.util.Locale.getDefault());

			if (ibavalues.get(name) != null) {
				ArrayList<String> temp1 = ibavalues.get(name);
				temp1.add(str);
				ibavalues.put(name, temp1);
			} else {
				list.add(str);
				ibavalues.put(name, list);
			}
		}
	}
	return ibavalues;
}

public static QueryResult getAllEPMDocsLatestIterationInContainer(String containerName) {

	QueryResult qr = null;
	try {
		// wt.org.WTOrganization localWTOrganization =
		// OrganizationServicesHelper.manager.getOrganization(SessionHelper.manager.getPrincipal());
		// WTContainer cr =
		// WTContainerHelper.service.getOrgContainer(localWTOrganization);
		// WTContainerHelper.
		// if (cr != null) {
		// ContainerSpec cs;

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

public static String getDisplayFromEnum(Persistable persisable, String attrName) throws WTException {

	StringBuffer buf = new StringBuffer();
	/* load object */
	PersistableAdapter obj = new PersistableAdapter(persisable, null, java.util.Locale.getDefault(),
			new com.ptc.core.meta.common.DisplayOperationIdentifier());

	/* load attribute */
	obj.load(attrName);

	Object[] enumValue;
	/* get attribute value, note if the attribute is multivalued it will return object array */
	if (obj.get(attrName) instanceof Object[]) {
		enumValue = (Object[]) obj.get(attrName);
	} else {
		enumValue = new Object[] { obj.get(attrName) };
	}
	AttributeTypeSummary ats = obj.getAttributeDescriptor(attrName);
	DataSet ds = ats.getLegalValueSet();
	for (int i = 0; i < enumValue.length; i++) {
		logger.debug("enumValue = " + enumValue[i].toString());

		/* get the EnumerationEntryIdentifier for enumeration key */
		EnumerationEntryIdentifier eei = ((EnumeratedSet) ds).getElementByKey(enumValue[i].toString());
		LWCEnumerationEntryValuesFactory eevf = new LWCEnumerationEntryValuesFactory();
		LocalizedValues value = eevf.get(eei, Locale.ENGLISH);
		// logger.debug("The localized display value is: " +
		// value.getDisplay());
		if (!buf.toString().equals("")) {
			buf.append(",");
		}
		buf.append(value.getDisplay());
	}
	return buf.toString();
}

public static EPMDocument getEPMDocument(String number) throws PersistenceException, WTException {
	EPMDocument result = null;

	QuerySpec qs = new QuerySpec(EPMDocument.class);
	qs.appendWhere(new SearchCondition(EPMDocument.class, EPMDocument.NUMBER, SearchCondition.EQUAL, number));
	QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
	// QueryResult qr = ConfigHelper.service.queryIterations(qs,
	// getDefaultConfigSpec(EPMDocument.class));
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

public static RevisionControlled getLatestObject(Versioned object) throws PersistenceException, WTException {

	return (RevisionControlled) VersionControlHelper.service.allVersionsOf(object).nextElement();

}

public static WTPart getLatestPart(String partNumber) throws WTException {

	try {
		QuerySpec partSpec = new QuerySpec(wt.part.WTPart.class);
		int[] fromIndicies = { 0, 1 };
		partSpec.appendWhere(
				new SearchCondition(wt.part.WTPart.class, "master>number", SearchCondition.EQUAL, partNumber, false),
				fromIndicies);
		QueryResult queryresult = PersistenceHelper.manager.find((StatementSpec) partSpec);
		if (queryresult.hasMoreElements()) {
			return (WTPart) getLatestObject((WTPart) queryresult.nextElement());

		}
	} catch (WTException wte) {
		logger.debug("QueryHelper.getPart:ERROR: while trying to Query for Part with Number:" + partNumber);
		throw wte;
	}

	return null;
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

public static String getOid(Persistable paramPersistable) throws WTException {
	if (paramPersistable == null) {
		throw new RuntimeException("persistable is null");
	}
	return referenceFactory.getReferenceString(paramPersistable);
}

public static WTReference getReference(String paramString) throws WTException {
	if (paramString == null || paramString.isEmpty()) {
		throw new RuntimeException("oid is null");
	}
	int i = paramString.indexOf(126);
	paramString = paramString.substring(i + 1);
	return referenceFactory.getReference(paramString);
}

private ConfigSpec getDefaultConfigSpec(Class class1) throws WTException {
	return ConfigHelper.service.getDefaultConfigSpecFor(WTPart.class);
}

public static IBAHolder setAttributeValuesAndPersist(IBAHolder ibaHolder, HashMap<String, String> map)
		throws Exception {
	ibaHolder = (IBAHolder) PersistenceHelper.manager.refresh((Persistable) ibaHolder);
	IBAHandler promotableIbaHandler = IBAHandler.newIBAHandler(ibaHolder);
	if (map != null) {
		Set<String> attrIds = map.keySet();
		for (String attr : attrIds) {

			if (map.get(attr) != null) {
				promotableIbaHandler.prepareIbaHolderForCreation(attr, IBACreateMode.CREATE_OR_UPDATE);
				promotableIbaHandler.updateIbaHolder();
				promotableIbaHandler.createValue(attr, map.get(attr), IBACreateMode.CREATE_OR_UPDATE, null, null, null,
						null);
				promotableIbaHandler.updateIbaHolder();
			}
		}
	}
	ibaHolder = (IBAHolder) PersistenceHelper.manager.refresh((Persistable) ibaHolder);
	return ibaHolder;

}

public static List<String> getDefaultValueForTarget(IBAHolder holder, String attribute)
		throws WTException, RemoteException {
	logger.debug("=>getDefaultValueForTarget()");

	TypeIdentifier localTypeIdentifier = TypeIdentifierUtilityHelper.service.getTypeIdentifier(holder);
	TypeDefinitionReadView tdrv = TypeDefinitionServiceHelper.service.getTypeDefView(localTypeIdentifier);
	ArrayList<String> defaults = new ArrayList<>();
	AttributeDefinitionReadView attrView = tdrv.getAttributeByName(attribute);
	if (attrView != null) { // get attribute default values
		Collection<AttributeDefaultValueReadView> attributeDefaultValues = attrView.getAllDefaultValues();
		for (AttributeDefaultValueReadView attributeDefaultValue : attributeDefaultValues) {
			defaults.add(String.valueOf(attributeDefaultValue.getValue()));
		}
	}
	return defaults;
}
}

/*
 * Location: D:\ptc\Windchill_10.2\Windchill\codebase\ext\emerson\query\QueryHelper Qualified Name: ext.emerson.query.QueryHelper Java Class Version: JD-Core Version:
 */