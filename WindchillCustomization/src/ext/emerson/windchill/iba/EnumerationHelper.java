/**
 * 
 */
package ext.emerson.windchill.iba;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import wt.log4j.LogR;
import wt.meta.LocalizedValues;
import wt.util.WTException;
import wt.util.WTProperties;

import com.ptc.core.lwc.common.AttributeTemplateFlavor;
import com.ptc.core.lwc.common.LWCEnumerationIdentifier;
import com.ptc.core.lwc.common.TypeDefinitionService;
import com.ptc.core.lwc.common.view.AttributeDefinitionReadView;
import com.ptc.core.lwc.common.view.ConstraintDefinitionReadView;
import com.ptc.core.lwc.common.view.EnumerationDefinitionReadView;
import com.ptc.core.lwc.common.view.TypeDefinitionReadView;
import com.ptc.core.lwc.server.LWCIBAAttDefinition;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;
import com.ptc.core.lwc.server.cache.EnumerationDefinitionManager;
import com.ptc.core.meta.common.AttributeTypeIdentifier;
import com.ptc.core.meta.common.DataSet;
import com.ptc.core.meta.common.EnumeratedSet;
import com.ptc.core.meta.common.EnumerationEntryIdentifier;
import com.ptc.core.meta.type.common.TypeInstance;
import com.ptc.core.meta.type.common.impl.DefaultTypeInstance;

import ext.emerson.windchill.lwc.server.LWCEnumerationEntryValuesFactory;

/**
 * @author Pooja Sah
 * @Creationdate Jan 16, 2016
 * @SourceProject HJNaming
 * 
 */
public class EnumerationHelper {

	private static final TypeDefinitionService TYPE_DEF_SERVICE = TypeDefinitionServiceHelper.service;
	private static Logger logger = LogR.getLogger(EnumerationHelper.class.getCanonicalName());

	/**
	 * @param datum
	 * @param attributeContainer
	 * @param component_id
	 * @return
	 * @throws WTException
	 */
	public static AttributeDefinitionReadView getAttributeDefnFromAttributeContainer(DefaultTypeInstance datum, String component_id)
			throws WTException {
		AttributeTypeIdentifier[] allATI = ((TypeInstance) datum).getAttributeTypeIdentifiers();

		logger.debug("allATI " + allATI);
		for (int m = 0; m < allATI.length; m++) {
			logger.debug("@@@@@@@@@@@@@ ATI = " + allATI[m].toExternalForm());
			if (allATI[m].toExternalForm().contains(component_id)) {
				return TYPE_DEF_SERVICE.getAttributeDefView(allATI[m]);

			}
		}
		return null;
	}

	public static AttributeDefinitionReadView getAttributeFromClassificationNode(String clfNodeInternalName, String childAttribute, String childEnumId)
			throws WTException {

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

		/* Retrieve name and description on ClassificationNode */
		TypeDefinitionReadView nodeView = TYPE_DEF_SERVICE.getTypeDefView(AttributeTemplateFlavor.LWCSTRUCT, NAMESPACE, clfNodeInternalName);

		Collection<AttributeDefinitionReadView> collAttrDefnReadView = nodeView.getAllAttributes();
		Iterator localIterator = collAttrDefnReadView.iterator();
		while (localIterator.hasNext()) {
			AttributeDefinitionReadView localAttributeDefinitionReadView = (AttributeDefinitionReadView) localIterator.next();
			if (LWCIBAAttDefinition.class.getName().equals(localAttributeDefinitionReadView.getAttDefClass())
					&& localAttributeDefinitionReadView.getName().equals(childAttribute)) {
				return localAttributeDefinitionReadView;
			}
		}
		return null;
	}

	public static ArrayList<String> getDisplayValuesFromEnumeration(String enumId, EnumerationDefinitionReadView masterEnumDef, Locale paramLocale)
			throws WTException {
		ArrayList<String> list = new ArrayList<String>();
		list.add("");
		EnumerationDefinitionReadView enumView = getEnumerationDefinitionFromEnumId(enumId, masterEnumDef, paramLocale);
		if (enumView != null) {
			Object[] enumerationEntryIdentifierArray = enumView.getEnumeratedSet(paramLocale).getElements();

			for (Object object : enumerationEntryIdentifierArray) {
				if (object instanceof EnumerationEntryIdentifier) {
					LWCEnumerationEntryValuesFactory extFactory = new LWCEnumerationEntryValuesFactory();
					LocalizedValues localLocalizedValues = extFactory.get((EnumerationEntryIdentifier) object, paramLocale);

					list.add(localLocalizedValues.getDisplay());

				}
			}
		}
		logger.debug("Found display vals : " + list);
		return list;
	}

	public static EnumerationDefinitionReadView getEnumerationConstraintFromAttributeDefinition(AttributeDefinitionReadView attrDefn)
			throws WTException {

		Collection<ConstraintDefinitionReadView> test = attrDefn.getAllConstraints();

		for (ConstraintDefinitionReadView constr : test) {

			logger.debug("constr : " + constr.getAttName());
			if (constr.getEnumDef() != null) {
				return constr.getEnumDef();
			}

		}

		// Use as refereence

		// AttributeTypeIdentifier paramAttributeTypeIdentifier =
		// attrDefn.getAttributeTypeIdentifier();
		// TypeIdentifier localTypeIdentifier =
		// paramAttributeTypeIdentifier.getRootContext();
		//
		// AttributeTypeIdentifier[] arrayOfAttributeTypeIdentifier = {
		// paramAttributeTypeIdentifier };
		//
		// TypeInstance localTypeInstance =
		// TypedUtility.prepare(localTypeIdentifier,
		// arrayOfAttributeTypeIdentifier, new CreateOperationIdentifier(),
		// Locale.getDefault(), true, true);
		//
		// AttributeTypeSummary localAttributeTypeSummary =
		// localTypeInstance.getAttributeTypeSummary(paramAttributeTypeIdentifier);
		//
		// logger.debug("localAttributeTypeSummary found = " +
		// localAttributeTypeSummary.getAbbreviatedLabel());
		//
		return null;
	}

	public static EnumerationDefinitionReadView getEnumerationDefinitionFromEnumId(String enumId, EnumerationDefinitionReadView masterEnumDefn,
			Locale paramLocale) throws WTException {

		EnumerationDefinitionManager manager = EnumerationDefinitionManager.getEnumerationDefinitionManagerInstance();
		Set<EnumerationDefinitionReadView> setViews = manager.getAllChildrenEnumerationDefViews(masterEnumDefn.getId());
		for (EnumerationDefinitionReadView view : setViews) {
			// logger.debug("view.getEnumerationIdentifier().getName() : "
			// + view.getEnumerationIdentifier().getName());
			if (view.getEnumerationIdentifier().getName().equalsIgnoreCase(enumId)) {
				// logger.debug("View : " +
				// view.getPropertyValueByName("displayName").getValueAsString());
				return view;
			}

		}

		return null;

	}

	private static EnumerationDefinitionReadView getEnumerationReadView(EnumerationEntryIdentifier paramEnumerationEntryIdentifier) {
		LWCEnumerationIdentifier localLWCEnumerationIdentifier = (LWCEnumerationIdentifier) paramEnumerationEntryIdentifier.getContext();
		EnumerationDefinitionReadView localEnumerationDefinitionReadView = null;
		try {
			localEnumerationDefinitionReadView = TYPE_DEF_SERVICE.getEnumDefView(localLWCEnumerationIdentifier);
		} catch (Exception localException) {
			logger.error("", localException);
		}
		return localEnumerationDefinitionReadView;
	}

	/**
	 * Start Approach B
	 * 
	 * 
	 */
	public static ArrayList<String> getInternalValuesForEnumeration(String enumId, EnumerationDefinitionReadView masterEnumView, Locale paramLocale)
			throws WTException {
		ArrayList<String> list = new ArrayList<String>();
		list.add("");
		logger.debug("Enum id = " + enumId);
		EnumerationDefinitionReadView enumView = getEnumerationDefinitionFromEnumId(enumId, masterEnumView, paramLocale);
		if (enumView != null) {
			DataSet localDataSet = enumView.getEnumeratedSet(paramLocale);
			if (localDataSet != null) {
				for (Object object : ((EnumeratedSet) localDataSet).getElements()) {
					if (object instanceof EnumerationEntryIdentifier) {
						EnumerationEntryIdentifier enumerationEntryIdentifier = (EnumerationEntryIdentifier) object;
						String str = enumerationEntryIdentifier.getKey().toString();
						if (str != null) {

							list.add(str);

						}
					}
				}

			}
		}
		logger.debug("Found internal vals : " + list);
		return list;
	}
	// public static ArrayList<String> getEnumeratedValues(Object paramObject,
	// AttributeTypeSummary paramAttributeTypeSummary, Locale paramLocale,
	// ComponentMode paramComponentMode)
	// {
	// if (paramAttributeTypeSummary == null) {
	// return null;
	// }
	// DataSet localDataSet = paramAttributeTypeSummary.getLegalValueSet();
	//
	// ArrayList localArrayList = null;
	// if ((localDataSet != null) && ((localDataSet instanceof DiscreteSet)))
	// {
	// Object[] arrayOfObject1 = ((DiscreteSet)localDataSet).getElements();
	//
	// int i = 0;
	// if ((paramObject != null) && (!paramObject.toString().equals("")) &&
	// (!((DiscreteSet)localDataSet).contains(paramObject)))
	// {
	// localArrayList = new ArrayList(arrayOfObject1.length + 1);
	//
	// i = 1;
	// }
	// else
	// {
	// localArrayList = new ArrayList(arrayOfObject1.length);
	// }
	// EnumerationEntryValuesFactory localEnumerationEntryValuesFactory = new
	// EnumerationEntryValuesFactory();
	// for (Object localObject : arrayOfObject1) {
	// if ((paramComponentMode != null) && ((localObject instanceof
	// EnumerationEntryIdentifier)))
	// {
	// EnumerationEntryIdentifier localEnumerationEntryIdentifier =
	// (EnumerationEntryIdentifier)localObject;
	//
	// LocalizedValues localLocalizedValues =
	// localEnumerationEntryValuesFactory.get(localEnumerationEntryIdentifier,
	// paramLocale);
	//
	//
	//
	// DefinitionDescriptor localDefinitionDescriptor =
	// localEnumerationEntryValuesFactory.get(localEnumerationEntryIdentifier,
	// localLocalizedValues);
	//
	//
	//
	// String str = localDefinitionDescriptor.getProperty("IS_SELECTABLE");
	// if ((str.equalsIgnoreCase("false")) && (paramComponentMode ==
	// ComponentMode.CREATE)) {}
	// }
	// else
	// {
	// localArrayList.add(SoftAttributesHelper.convertAttributeValueToString(localObject,
	// paramLocale));
	// }
	// }
	// if (i != 0) {
	// localArrayList.add(SoftAttributesHelper.convertAttributeValueToString(paramObject,
	// paramLocale));
	// }
	// }
	// if (localArrayList != null) {
	// localArrayList.trimToSize();
	// }
	// return localArrayList;
	// }
	//
}
