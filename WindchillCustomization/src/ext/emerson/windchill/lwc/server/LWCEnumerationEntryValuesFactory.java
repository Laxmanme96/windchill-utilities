package ext.emerson.windchill.lwc.server;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;

import com.ptc.core.lwc.common.LWCEnumerationIdentifier;
import com.ptc.core.lwc.common.TypeDefinitionService;
import com.ptc.core.lwc.common.view.EnumerationDefinitionReadView;
import com.ptc.core.lwc.common.view.EnumerationEntryReadView;
import com.ptc.core.lwc.common.view.EnumerationMembershipReadView;
import com.ptc.core.lwc.common.view.PropertyValueReadView;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;
import com.ptc.core.meta.common.DefinitionIdentifier;
import com.ptc.core.meta.common.EnumerationEntryIdentifier;
import com.ptc.core.meta.descriptor.server.impl.AbstractLocalizedValuesFactory;

import wt.log4j.LogR;
import wt.meta.LocalizedValues;

public class LWCEnumerationEntryValuesFactory extends AbstractLocalizedValuesFactory implements Serializable {
private static final String					IS_SELECTABLE		= "IS_SELECTABLE";
private static final Logger					logger				= LogR
		.getLogger(LWCEnumerationEntryValuesFactory.class.getName());
private static final long					serialVersionUID	= 6109098275857252443L;
private static final TypeDefinitionService	TYPE_DEF_SERVICE	= TypeDefinitionServiceHelper.service;

@Override
public LocalizedValues get(DefinitionIdentifier paramDefinitionIdentifier, Locale paramLocale) {

	LocalizedValues localLocalizedValues = null;
	Object localObject1;
	String str1;
	if ((paramDefinitionIdentifier instanceof EnumerationEntryIdentifier)) {
		localObject1 = paramDefinitionIdentifier;
		str1 = getBundleKey((DefinitionIdentifier) localObject1);
		EnumerationMembershipReadView localEnumerationMembershipReadView = getEntry(
				(EnumerationEntryIdentifier) localObject1, str1);
		EnumerationEntryReadView enumEntryReadView = localEnumerationMembershipReadView != null
				? localEnumerationMembershipReadView.getMember() : null;
		if (enumEntryReadView != null) {
			ConcurrentHashMap localConcurrentHashMap = new ConcurrentHashMap(6);
			if ((enumEntryReadView).getPropertyValueByName("selectable") == null) {
				localConcurrentHashMap.put("IS_SELECTABLE", "true");
			}
			for (Iterator localIterator = enumEntryReadView.getAllProperties().iterator(); localIterator.hasNext();) {
				PropertyValueReadView localPropertyValueReadView = (PropertyValueReadView) localIterator.next();
				String str2 = localPropertyValueReadView.getName();

				String str3 = localPropertyValueReadView.getValueAsString(paramLocale, true);

				// logger.debug("Name : " + str2 + " Value : " +
				// str3);

				if (str3 != null) {
					localConcurrentHashMap.put(str2, str3);
					if (str2.equals("displayName")) {
						localConcurrentHashMap.put("display", str3);
						localConcurrentHashMap.put("abbreviatedDisplay", str3);
						localConcurrentHashMap.put("fullDisplay", str3);
					} else if (str2.equals("description")) {
						localConcurrentHashMap.put("longDescription", str3);
					} else if (str2.equals("tooltip")) {
						localConcurrentHashMap.put("shortDescription", str3);
					} else if (str2.equals("selectable")) {
						localConcurrentHashMap.put("IS_SELECTABLE", str3);
					}
				}
			}

			localLocalizedValues = new LocalizedValues(localConcurrentHashMap);
			// PropertyValueReadView localPropertyValueReadView;
			// String str2;
			// String str3;
			// for (Iterator<PropertyValueReadView> localIterator =
			// localEnumerationMembershipReadView.getAllProperties().iterator();
			// localIterator.hasNext();)
			// {
			// localPropertyValueReadView =
			// (PropertyValueReadView)localIterator.next();
			//
			// str2 = localPropertyValueReadView.getName();
			// str3 =
			// localPropertyValueReadView.getValueAsString(paramLocale,
			// true);
			// if (str3 != null) {
			// localConcurrentHashMap.put(str2, str3);
			// }
			// }
			// localLocalizedValues = new
			// LocalizedValues(localConcurrentHashMap);
			// }
			// }
			// if (localLocalizedValues == null)
			// {
			// localObject1 = new ConcurrentHashMap(5);
			// str1 = paramDefinitionIdentifier.toExternalForm();
			// ((Map)localObject1).put("abbreviatedDisplay", str1);
			// ((Map)localObject1).put("display", str1);
			// ((Map)localObject1).put("fullDisplay", str1);
			// ((Map)localObject1).put("shortDescription", str1);
			// ((Map)localObject1).put("longDescription", str1);
			// localLocalizedValues = new
			// LocalizedValues((Map)localObject1);
			// }
			if (logger.isTraceEnabled()) {
				logger.trace("leaving get(DefinitionIdentifier,Locale) " + localLocalizedValues);
			}
			return localLocalizedValues;
		}
	}
	return null;
}

@Override
public String getBundleKey(DefinitionIdentifier paramDefinitionIdentifier) {
	if ((paramDefinitionIdentifier instanceof EnumerationEntryIdentifier)) {
		return ((EnumerationEntryIdentifier) paramDefinitionIdentifier).getKey().toString();
	}
	return null;
}

@Override
public String getBundleName(DefinitionIdentifier paramDefinitionIdentifier) {
	return null;
}

private EnumerationMembershipReadView getEntry(EnumerationEntryIdentifier paramEnumerationEntryIdentifier,
		String paramString) {
	EnumerationDefinitionReadView localEnumerationDefinitionReadView = getEnumerationReadView(
			paramEnumerationEntryIdentifier);
	EnumerationMembershipReadView localEnumerationMembershipReadView = (localEnumerationDefinitionReadView == null)
			|| (paramString == null) ? null : localEnumerationDefinitionReadView.getMembershipByName(paramString);

	return localEnumerationMembershipReadView;
}

private EnumerationDefinitionReadView getEnumerationReadView(
		EnumerationEntryIdentifier paramEnumerationEntryIdentifier) {
	LWCEnumerationIdentifier localLWCEnumerationIdentifier = (LWCEnumerationIdentifier) paramEnumerationEntryIdentifier
			.getContext();
	EnumerationDefinitionReadView localEnumerationDefinitionReadView = null;
	try {
		localEnumerationDefinitionReadView = TYPE_DEF_SERVICE.getEnumDefView(localLWCEnumerationIdentifier);
	} catch (Exception localException) {
		logger.error("", localException);
	}
	return localEnumerationDefinitionReadView;
}

public ConcurrentHashMap getMap(DefinitionIdentifier paramDefinitionIdentifier, Locale paramLocale) {

	Object localObject1;
	String str1;
	if ((paramDefinitionIdentifier instanceof EnumerationEntryIdentifier)) {
		localObject1 = paramDefinitionIdentifier;
		str1 = getBundleKey((DefinitionIdentifier) localObject1);
		EnumerationMembershipReadView localEnumerationMembershipReadView = getEntry(
				(EnumerationEntryIdentifier) localObject1, str1);
		EnumerationEntryReadView enumEntryReadView = localEnumerationMembershipReadView != null
				? localEnumerationMembershipReadView.getMember() : null;
		if (enumEntryReadView != null) {
			ConcurrentHashMap localConcurrentHashMap = new ConcurrentHashMap(6);
			if ((enumEntryReadView).getPropertyValueByName("selectable") == null) {
				localConcurrentHashMap.put("IS_SELECTABLE", "true");
			}
			for (Iterator localIterator = enumEntryReadView.getAllProperties().iterator(); localIterator.hasNext();) {
				PropertyValueReadView localPropertyValueReadView = (PropertyValueReadView) localIterator.next();
				String str2 = localPropertyValueReadView.getName();

				String str3 = localPropertyValueReadView.getValueAsString(paramLocale, true);

				if (str3 != null) {
					localConcurrentHashMap.put(str2, str3);
					if (str2.equals("displayName")) {
						localConcurrentHashMap.put("display", str3);
						localConcurrentHashMap.put("abbreviatedDisplay", str3);
						localConcurrentHashMap.put("fullDisplay", str3);
					} else if (str2.equals("description")) {
						localConcurrentHashMap.put("longDescription", str3);
					} else if (str2.equals("tooltip")) {
						localConcurrentHashMap.put("shortDescription", str3);
					} else if (str2.equals("selectable")) {
						localConcurrentHashMap.put("IS_SELECTABLE", str3);
					}
				}
			}

			// PropertyValueReadView localPropertyValueReadView;
			// String str2;
			// String str3;
			// for (Iterator<PropertyValueReadView> localIterator =
			// localEnumerationMembershipReadView.getAllProperties().iterator();
			// localIterator.hasNext();)
			// {
			// localPropertyValueReadView =
			// (PropertyValueReadView)localIterator.next();
			//
			// str2 = localPropertyValueReadView.getName();
			// str3 =
			// localPropertyValueReadView.getValueAsString(paramLocale,
			// true);
			// if (str3 != null) {
			// localConcurrentHashMap.put(str2, str3);
			// }
			// }
			// localLocalizedValues = new
			// LocalizedValues(localConcurrentHashMap);
			// }
			// }
			// if (localLocalizedValues == null)
			// {
			// localObject1 = new ConcurrentHashMap(5);
			// str1 = paramDefinitionIdentifier.toExternalForm();
			// ((Map)localObject1).put("abbreviatedDisplay", str1);
			// ((Map)localObject1).put("display", str1);
			// ((Map)localObject1).put("fullDisplay", str1);
			// ((Map)localObject1).put("shortDescription", str1);
			// ((Map)localObject1).put("longDescription", str1);
			// localLocalizedValues = new
			// LocalizedValues((Map)localObject1);
			// }
			if (logger.isTraceEnabled()) {
				logger.trace("leaving get(DefinitionIdentifier,Locale) " + localConcurrentHashMap);
			}

			// logger.debug("MAP : " + localConcurrentHashMap);
			return localConcurrentHashMap;
		}
	}
	return null;
}

@Override
public String getSecondaryBundleKey(DefinitionIdentifier paramDefinitionIdentifier) {
	return null;
}
}
