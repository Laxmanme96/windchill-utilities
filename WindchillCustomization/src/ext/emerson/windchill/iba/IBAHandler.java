package ext.emerson.windchill.iba;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.ptc.core.lwc.common.AttributeTemplateFlavor;
import com.ptc.core.lwc.common.TypeDefinitionService;
import com.ptc.core.lwc.common.view.AttributeDefinitionReadView;
import com.ptc.core.lwc.common.view.TypeDefinitionReadView;
import com.ptc.core.lwc.server.LWCIBAAttDefinition;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.impl.TypeIdentifierUtilityHelper;

import ext.emerson.properties.CustomProperties;
import wt.fc.Persistable;
import wt.iba.constraint.IBAConstraintException;
import wt.iba.definition.litedefinition.AttributeDefDefaultView;
import wt.iba.definition.litedefinition.BooleanDefView;
import wt.iba.definition.litedefinition.FloatDefView;
import wt.iba.definition.litedefinition.IntegerDefView;
import wt.iba.definition.litedefinition.RatioDefView;
import wt.iba.definition.litedefinition.StringDefView;
import wt.iba.definition.litedefinition.TimestampDefView;
import wt.iba.definition.litedefinition.URLDefView;
import wt.iba.definition.litedefinition.UnitDefView;
import wt.iba.value.AttributeContainer;
import wt.iba.value.DefaultAttributeContainer;
import wt.iba.value.IBAHolder;
import wt.iba.value.litevalue.AbstractContextualValueDefaultView;
import wt.iba.value.litevalue.AbstractValueView;
import wt.iba.value.litevalue.BooleanValueDefaultView;
import wt.iba.value.litevalue.FloatValueDefaultView;
import wt.iba.value.litevalue.IntegerValueDefaultView;
import wt.iba.value.litevalue.RatioValueDefaultView;
import wt.iba.value.litevalue.StringValueDefaultView;
import wt.iba.value.litevalue.TimestampValueDefaultView;
import wt.iba.value.litevalue.URLValueDefaultView;
import wt.iba.value.litevalue.UnitValueDefaultView;
import wt.iba.value.service.IBAValueDBService;
import wt.iba.value.service.IBAValueDBServiceInterface;
import wt.iba.value.service.IBAValueHelper;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.util.WTProperties;

public class IBAHandler {
public static enum IBACreateMode {
	APPEND_ONLY, CREATE_ONLY, CREATE_OR_APPEND, CREATE_OR_UPDATE, UPDATE_ONLY;

	private IBACreateMode() {
	}
}

private static Logger logger = CustomProperties.getlogger(IBAHandler.class.getName());

public static List<String> getValuesAsString(List<AbstractContextualValueDefaultView> paramList) {
	ArrayList localArrayList = new ArrayList(paramList.size());
	Iterator localIterator = paramList.iterator();
	while (localIterator.hasNext()) {
		AbstractContextualValueDefaultView localAbstractContextualValueDefaultView = (AbstractContextualValueDefaultView) localIterator
				.next();
		localArrayList.add(localAbstractContextualValueDefaultView.getValueAsString());
	}
	return localArrayList;
}

public static IBAHandler newIBAHandler(IBAHolder paramIBAHolder) throws Exception {
	IBAHandler localIBAHandler = new IBAHandler(paramIBAHolder);
	localIBAHandler.initalize();
	return localIBAHandler;
}

protected Map<String, AttributeDefinitionReadView>	attributeDefinitionMap	= null;
protected DefaultAttributeContainer					container				= null;
protected int										defaultFloatPrecision	= 2;
protected double									defaultRatioDenominator	= 100.0D;

protected int										defaultUnitPrecision	= 2;

protected String									defaultURLDescription	= "URL";

protected Map<String, String>						globalNameToContextName	= null;
protected IBAValueDBServiceInterface				ibaDBService			= null;

protected IBAHolder									ibaHolder				= null;

protected IBAHandler(IBAHolder paramIBAHolder) {
	ibaHolder = paramIBAHolder;
}

public AbstractValueView createIBA(AttributeDefDefaultView paramAttributeDefDefaultView, Object paramObject,
		Integer paramInteger1, Integer paramInteger2, Double paramDouble, String paramString) throws Exception {
	paramInteger1 = Integer.valueOf(paramInteger1 == null ? defaultFloatPrecision : paramInteger1.intValue());
	paramInteger2 = Integer.valueOf(paramInteger2 == null ? defaultUnitPrecision : paramInteger2.intValue());
	paramDouble = Double.valueOf(paramDouble == null ? defaultRatioDenominator : paramDouble.doubleValue());
	paramString = paramString == null ? defaultURLDescription : paramString;
	Object localObject1;
	AbstractValueView localObject2;
	if (paramAttributeDefDefaultView instanceof StringDefView) {
		localObject1 = paramAttributeDefDefaultView;
		localObject2 = new StringValueDefaultView((StringDefView) localObject1, (String) paramObject);
		return localObject2;
	}
	if (paramAttributeDefDefaultView instanceof BooleanDefView) {
		localObject1 = paramAttributeDefDefaultView;
		localObject2 = new BooleanValueDefaultView((BooleanDefView) localObject1,
				((Boolean) paramObject).booleanValue());
		return localObject2;
	}
	if (paramAttributeDefDefaultView instanceof FloatDefView) {
		localObject1 = paramAttributeDefDefaultView;
		localObject2 = new FloatValueDefaultView((FloatDefView) localObject1);
		((FloatValueDefaultView) localObject2).setValue((double) paramObject);
		return localObject2;
	}
	if (paramAttributeDefDefaultView instanceof IntegerDefView) {
		localObject1 = paramAttributeDefDefaultView;
		localObject2 = new IntegerValueDefaultView((IntegerDefView) localObject1, ((Integer) paramObject).intValue());
		return localObject2;
	}
	if (paramAttributeDefDefaultView instanceof TimestampDefView) {
		localObject1 = paramAttributeDefDefaultView;
		localObject2 = new TimestampValueDefaultView((TimestampDefView) localObject1, (Timestamp) paramObject);
		return localObject2;
	}
	if (paramAttributeDefDefaultView instanceof URLDefView) {
		localObject1 = paramAttributeDefDefaultView;
		localObject2 = new URLValueDefaultView((URLDefView) localObject1, (String) paramObject, paramString);
		return localObject2;
	}
	if (paramAttributeDefDefaultView instanceof UnitDefView) {
		localObject1 = paramAttributeDefDefaultView;
		localObject2 = new UnitValueDefaultView((UnitDefView) localObject1, ((Double) paramObject).doubleValue(),
				paramInteger2.intValue());
		return localObject2;
	}
	if (paramAttributeDefDefaultView instanceof RatioDefView) {
		localObject1 = paramAttributeDefDefaultView;
		localObject2 = new RatioValueDefaultView((RatioDefView) localObject1, ((Double) paramObject).doubleValue(),
				paramDouble.doubleValue());
		return localObject2;
	}
	throw new WTException("Unsupported IBA type " + paramAttributeDefDefaultView);
}

public AbstractValueView createIBAFromString(AttributeDefDefaultView paramAttributeDefDefaultView, String paramString1,
		Integer paramInteger1, Integer paramInteger2, Double paramDouble, String paramString2) throws Exception {
	if (paramAttributeDefDefaultView instanceof StringDefView) {
		return createIBA(paramAttributeDefDefaultView, paramString1, paramInteger1, paramInteger2, paramDouble,
				paramString2);
	}
	if (paramAttributeDefDefaultView instanceof BooleanDefView) {
		return createIBA(paramAttributeDefDefaultView, Boolean.valueOf(paramString1), paramInteger1, paramInteger2,
				paramDouble, paramString2);
	}
	if (paramAttributeDefDefaultView instanceof FloatDefView) {

		return createIBA(paramAttributeDefDefaultView, Double.parseDouble(paramString1), paramInteger1, paramInteger2,
				paramDouble, paramString2);
	}
	if (paramAttributeDefDefaultView instanceof IntegerDefView) {
		return createIBA(paramAttributeDefDefaultView, Integer.valueOf(paramString1), paramInteger1, paramInteger2,
				paramDouble, paramString2);
	}
	if (paramAttributeDefDefaultView instanceof TimestampDefView) {
		return createIBA(paramAttributeDefDefaultView, Timestamp.valueOf(paramString1), paramInteger1, paramInteger2,
				paramDouble, paramString2);
	}
	if (paramAttributeDefDefaultView instanceof URLDefView) {
		return createIBA(paramAttributeDefDefaultView, paramString1, paramInteger1, paramInteger2, paramDouble,
				paramString2);
	}
	if (paramAttributeDefDefaultView instanceof UnitDefView) {
		return createIBA(paramAttributeDefDefaultView, Double.valueOf(paramString1), paramInteger1, paramInteger2,
				paramDouble, paramString2);
	}
	if (paramAttributeDefDefaultView instanceof RatioDefView) {
		return createIBA(paramAttributeDefDefaultView, Double.valueOf(paramString1), paramInteger1, paramInteger2,
				paramDouble, paramString2);
	}
	throw new WTException("Unsupported IBA type " + paramAttributeDefDefaultView);
}

public void createValue(String ibaName, String ibaValue, IBACreateMode paramIBACreateMode, Integer paramInteger1,
		Integer paramInteger2, Double paramDouble, String paramString3) throws Exception {
	ArrayList localArrayList = new ArrayList(1);
	localArrayList.add(ibaValue);
	createValues(ibaName, localArrayList, paramIBACreateMode, paramInteger1, paramInteger2, paramDouble, paramString3);
}

public void createValues(Map<String, List<String>> paramMap, IBACreateMode paramIBACreateMode, Integer paramInteger1,
		Integer paramInteger2, Double paramDouble, String paramString) throws Exception {
	Iterator localIterator = paramMap.keySet().iterator();
	while (localIterator.hasNext()) {
		String str = (String) localIterator.next();
		createValues(str, paramMap.get(str), paramIBACreateMode, paramInteger1, paramInteger2, paramDouble,
				paramString);
	}
}

public void createValues(String attrName, List<String> paramList, IBACreateMode paramIBACreateMode,
		Integer paramInteger1, Integer paramInteger2, Double paramDouble, String paramString2) throws Exception {
	try {
		if (paramList == null || paramList.isEmpty()) {
			throw new WTException("Empty or null values array");
		}
		AttributeDefinitionReadView localAttributeDefinitionReadView = attributeDefinitionMap.get(attrName);
		if (localAttributeDefinitionReadView != null) {

			prepareIbaHolderForCreation(attrName, paramIBACreateMode);
			Iterator localIterator = paramList.iterator();
			while (localIterator.hasNext()) {
				String str = (String) localIterator.next();
				AbstractValueView localAbstractValueView = createIBAFromString(
						localAttributeDefinitionReadView.getIBARefView(), str, paramInteger1, paramInteger2,
						paramDouble, paramString2);
				container.addAttributeValue(localAbstractValueView);
			}
		}
	} catch (Exception localException) {
		localException.printStackTrace();
		throw new Exception(
				"IBA holder : " + ibaHolder + "IBA MODE : " + paramIBACreateMode + " IBA name : " + attrName);
	}
}

public void deleteIba(String paramString) throws Exception {
	List localList = getValues(paramString);
	try {
		Iterator localIterator = localList.iterator();
		while (localIterator.hasNext()) {
			AbstractContextualValueDefaultView localAbstractContextualValueDefaultView = (AbstractContextualValueDefaultView) localIterator
					.next();
			container.deleteAttributeValues(localAbstractContextualValueDefaultView.getDefinition());
		}
	} catch (IBAConstraintException localIBAConstraintException) {
		throw new Exception(localIBAConstraintException);
	}
}

public int getDefaultFloatPrecision() {

	return defaultFloatPrecision;
}

public double getDefaultRatioDenominator() {
	return defaultRatioDenominator;
}

public int getDefaultUnitPrecision() {
	return defaultUnitPrecision;
}

public String getDefaultURLDescription() {
	return defaultURLDescription;
}

public IBAHolder getIbaHolder() {
	ibaHolder.setAttributeContainer(container);
	return ibaHolder;
}

public List<AbstractContextualValueDefaultView> getValues(String paramString) throws Exception {
	Map localMap = getValuesMap();
	List localList = (List) localMap.get(paramString);
	return localList;
}

public List<String> getValuesAsString(String paramString) throws Exception {
	List localList = getValues(paramString);
	if (localList == null) {
		return null;
	}
	return getValuesAsString(localList);
}

public Map<String, List<String>> getValuesAsStringMap() throws Exception {
	Map localMap = getValuesMap();
	HashMap localHashMap = new HashMap(localMap.size());
	Iterator localIterator = localMap.keySet().iterator();
	while (localIterator.hasNext()) {
		String str = (String) localIterator.next();
		List localList = (List) localMap.get(str);
		localHashMap.put(str, getValuesAsString(localList));
	}
	return localHashMap;
}

public Map<String, List<AbstractContextualValueDefaultView>> getValuesMap() throws Exception {
	AbstractValueView[] arrayOfAbstractValueView1 = container.getAttributeValues();
	HashMap localHashMap = new HashMap(arrayOfAbstractValueView1.length);
	for (AbstractValueView localAbstractValueView : arrayOfAbstractValueView1) {
		String str1 = localAbstractValueView.getDefinition().getName();
		String str2 = globalNameToContextName.get(str1);
		if (str2 != null) {
			Object localObject = localHashMap.get(str2);
			if (localObject == null) {
				localObject = new ArrayList();
				localHashMap.put(str2, localObject);
			}
			if (!(localAbstractValueView instanceof AbstractContextualValueDefaultView)) {
				throw new Exception("Could not cast value class Value view " + localAbstractValueView);
			}
			((List) localObject).add(localAbstractValueView);
		}
	}
	return localHashMap;
}

public void initalize() throws Exception {
	try {
		if (ibaHolder.getAttributeContainer() == null) {
			ibaHolder = IBAValueHelper.service.refreshIBAHolder(ibaHolder, null, null, null);
		}
		initializeContainer();
		initializeDefinitionMaps();
		initializeClassificationDefinitionMaps();
	} catch (Exception localException) {

	}
}

protected void initializeClassificationDefinitionMaps() throws WTException {

	final String NAMESPACE;
	final String DEFAULT_NAMESPACE = "com.ptc.csm.default_clf_namespace";
	final TypeDefinitionService TYPE_DEF_SERVICE = TypeDefinitionServiceHelper.service;

	Properties PROPERTIES = null;
	try {
		PROPERTIES = WTProperties.getLocalProperties();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	NAMESPACE = PROPERTIES.getProperty("com.ptc.csm.namespace", DEFAULT_NAMESPACE);

	/* Retrieve the ClassificationNode reference from WTPart */
	String attr_name = "PartsClassification";
	PersistableAdapter obj = new PersistableAdapter((Persistable) ibaHolder, null, SessionHelper.getLocale(),
			new DisplayOperationIdentifier());

	obj.load(attr_name);
	String clfNodeInternalName = (String) obj.get(attr_name);

	/* Retrieve name and description on ClassificationNode */
	TypeDefinitionReadView nodeView = TYPE_DEF_SERVICE.getTypeDefView(AttributeTemplateFlavor.LWCSTRUCT, NAMESPACE,
			clfNodeInternalName);
	logger.debug("Class node " + clfNodeInternalName);
	Collection<AttributeDefinitionReadView> collAttrDefnReadView = nodeView.getAllAttributes();
	// .getAttributeByName(property);
	// logger.debug("attrdefn : " + attrDefnReadView);

	Iterator localIterator = collAttrDefnReadView.iterator();
	while (localIterator.hasNext()) {
		AttributeDefinitionReadView localAttributeDefinitionReadView = (AttributeDefinitionReadView) localIterator
				.next();
		if (LWCIBAAttDefinition.class.getName().equals(localAttributeDefinitionReadView.getAttDefClass())) {
			AttributeDefDefaultView localAttributeDefDefaultView = localAttributeDefinitionReadView.getIBARefView();
			if (localAttributeDefDefaultView != null) {
				String str = localAttributeDefinitionReadView.getName();
				attributeDefinitionMap.put(str, localAttributeDefinitionReadView);
				globalNameToContextName.put(localAttributeDefDefaultView.getName(), str);
			}
		}
	}
}

protected void initializeContainer() throws WTException {
	AttributeContainer localAttributeContainer = ibaHolder.getAttributeContainer();
	if (localAttributeContainer == null) {
		throw new WTException("null IBA container");
	}
	if (!(localAttributeContainer instanceof DefaultAttributeContainer)) {
		throw new WTException("Container is invalid");
	}
	container = (DefaultAttributeContainer) localAttributeContainer;
}

protected void initializeDefinitionMaps() throws RemoteException, WTException {
	attributeDefinitionMap = new HashMap();
	globalNameToContextName = new HashMap();
	TypeIdentifier localTypeIdentifier = TypeIdentifierUtilityHelper.service.getTypeIdentifier(ibaHolder);
	TypeDefinitionReadView localTypeDefinitionReadView = TypeDefinitionServiceHelper.service
			.getTypeDefView(localTypeIdentifier);
	Collection localCollection = localTypeDefinitionReadView.getAllAttributes();
	Iterator localIterator = localCollection.iterator();
	while (localIterator.hasNext()) {
		AttributeDefinitionReadView localAttributeDefinitionReadView = (AttributeDefinitionReadView) localIterator
				.next();
		if (LWCIBAAttDefinition.class.getName().equals(localAttributeDefinitionReadView.getAttDefClass())) {
			AttributeDefDefaultView localAttributeDefDefaultView = localAttributeDefinitionReadView.getIBARefView();
			if (localAttributeDefDefaultView != null) {
				String str = localAttributeDefinitionReadView.getName();
				attributeDefinitionMap.put(str, localAttributeDefinitionReadView);
				globalNameToContextName.put(localAttributeDefDefaultView.getName(), str);
			}
		}
	}
}

public void prepareIbaHolderForCreation(String paramString, IBACreateMode paramIBACreateMode)
		throws Exception, WTException {

	List localList = getValues(paramString);
	AttributeDefDefaultView localAttributeDefDefaultView;

	if (paramIBACreateMode.equals(IBACreateMode.CREATE_ONLY))

	{
		logger.debug("paramIBACreateMode is CREATE_ONLY");
		if (localList != null && !localList.isEmpty()) {
			throw new WTException("Could not create IBA as it already exists");
		}
	} else if (paramIBACreateMode.equals(IBACreateMode.UPDATE_ONLY)) {
		// logger.debug("paramIBACreateMode is UPDATE_ONLY");

		if (localList == null && localList.isEmpty()) {
			throw new WTException("Could not update IBA as it doest not exists");
		}
		localAttributeDefDefaultView = ((AbstractContextualValueDefaultView) localList.get(0)).getDefinition();
		container.deleteAttributeValues(localAttributeDefDefaultView);
	} else if (paramIBACreateMode.equals(IBACreateMode.APPEND_ONLY)) {
		// logger.debug("paramIBACreateMode is APPEND_ONLY");
		if (localList == null && localList.isEmpty()) {
			throw new WTException("Could not append IBA as it doest not exists");
		}
	} else if (paramIBACreateMode.equals(IBACreateMode.CREATE_OR_UPDATE)) {

		if (localList != null && !localList.isEmpty()) {
			logger.debug(
					"paramIBACreateMode CREATE_OR_UPDATE is selected!!! : " + paramString + " " + localList.get(0));
			logger.debug("Value to update : "
					+ ((AbstractContextualValueDefaultView) localList.get(0)).getDefinition().getDisplayName());
			localAttributeDefDefaultView = ((AbstractContextualValueDefaultView) localList.get(0)).getDefinition();
			container.deleteAttributeValues(localAttributeDefDefaultView);
		}

	}
}

public void setDefaultFloatPrecision(int paramInt) {
	defaultFloatPrecision = paramInt;
}

public void setDefaultRatioDenominator(double paramDouble) {
	defaultRatioDenominator = paramDouble;
}

public void setDefaultUnitPrecision(int paramInt) {
	defaultUnitPrecision = paramInt;
}

public void setDefaultURLDescription(String paramString) {
	defaultURLDescription = paramString;
}

public void updateIbaHolder() throws WTException {
	if (ibaDBService == null) {
		ibaDBService = IBAValueDBService.newIBAValueDBService();
	}

	container = (DefaultAttributeContainer) ibaDBService.updateAttributeContainer(getIbaHolder(), null, null, null);

}
}
