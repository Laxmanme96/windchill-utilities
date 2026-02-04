package ext.emerson.windchill.iba;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.businessfield.common.BusinessField;
import com.ptc.core.businessfield.common.BusinessFieldIdFactoryHelper;
import com.ptc.core.businessfield.common.BusinessFieldServiceHelper;
import com.ptc.core.businessfield.server.BusinessFieldIdentifier;
import com.ptc.core.businessfield.server.businessObject.BusinessObject;
import com.ptc.core.businessfield.server.businessObject.BusinessObjectHelper;
import com.ptc.core.businessfield.server.businessObject.BusinessObjectHelperFactory;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.OperationIdentifier;
import com.ptc.core.meta.common.OperationIdentifierConstants;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.windchill.enterprise.traceability.service.SoftAttributes.TraceabilityLinkAttributes;

import ext.emerson.properties.CustomProperties;
import ext.emerson.windchill.iba.IBAHandler.IBACreateMode;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.iba.value.IBAHolder;
import wt.session.SessionHelper;
import wt.util.WTException;

public class CustomBusinessObjectHelper {
private static Logger logger = CustomProperties.getlogger(CustomBusinessObjectHelper.class.getName());

/**
 * Supplied by PTC. see C14774147 This will retrieve all soft attributes for multiple objects of same type.
 *
 * @param persistables
 * @return
 * @throws WTException
 */
public static HashMap<WTReference, HashMap<String, Object>> getAllSoftAttributesForSpecificType(
		Persistable[] persistables) throws WTException {
	HashMap<WTReference, HashMap<String, Object>> busObjMap = new HashMap<>();
	BusinessObjectHelper busObjHelper = BusinessObjectHelperFactory.getBusinessObjectHelper();
	OperationIdentifier opId = OperationIdentifier.newOperationIdentifier(OperationIdentifierConstants.VIEW);

	TypeIdentifier localTypeIdentifier = TypeIdentifierHelper.getType(persistables[0]);
	Collection<BusinessField> attrsToLoad = buildAttrIdsToLoad(localTypeIdentifier);

	List<BusinessObject> businessObjects = busObjHelper.newBusinessObjects(SessionHelper.getLocale(), opId, false,
			persistables);
	busObjHelper.load(businessObjects, attrsToLoad);
	for (BusinessObject bus_obj : businessObjects) {
		HashMap<String, Object> ibaMap = new HashMap<>();
		for (BusinessField bus_field : attrsToLoad) {
			logger.debug(bus_obj.getWTReference().getObject().getIdentity());
			ibaMap.put(bus_field.getName(), bus_obj.get(bus_field));
		}
		busObjMap.put(bus_obj.getWTReference(), ibaMap);
	}
	return busObjMap;

}

private static List<BusinessField> buildAttrIdsToLoad(TypeIdentifier typeId) throws WTException {
	List<BusinessField> attrsToLoad = new ArrayList<>();

	Map<String, Class> supportedAttrs = TraceabilityLinkAttributes.supportedAttributes();
	logger.debug("@@@@@@@@@@@@@@@@@@@@@@@@ " + supportedAttrs);
	for (String key : supportedAttrs.keySet()) {
		BusinessField busField = getBusinessField(typeId, key);
		if (!attrsToLoad.contains(busField)) {
			attrsToLoad.add(busField);
		}
	}
	return attrsToLoad;

}

/*
 * Retrieves attributes value from multiple objects of same type.
 *
 */
public static HashMap<WTReference, HashMap<String, Object>> fetchMultiObjIBA(Persistable[] persistables,
		List<String> allAtts) {
	logger.debug("=> fetchMultiObjIBA");
	HashMap<WTReference, HashMap<String, Object>> busObjMap = new HashMap<>();
	try {
		BusinessObjectHelper BUS_OBJ_HELPER = BusinessObjectHelperFactory.getBusinessObjectHelper();
		List<BusinessObject> bus_objs = BUS_OBJ_HELPER.newBusinessObjects(SessionHelper.getLocale(),
				new DisplayOperationIdentifier(), false, persistables);
		Collection<BusinessField> bus_fields = new ArrayList<>();
		if (persistables != null && persistables.length > 0) {
			TypeIdentifier localTypeIdentifier = TypeIdentifierHelper.getType(persistables[0]);

			for (String attribute : allAtts) {
				bus_fields.add(getBusinessField(localTypeIdentifier, attribute));
			}
			BUS_OBJ_HELPER.load(bus_objs, bus_fields);
			for (BusinessObject bus_obj : bus_objs) {
				HashMap<String, Object> ibaMap = new HashMap<>();
				for (BusinessField bus_field : bus_fields) {
					// logger.debug(bus_obj.getWTReference().getObject().getIdentity());
					// logger.debug(bus_field.getName());
					// logger.debug(bus_obj.get(bus_field));
					if (bus_field != null && bus_field.getName() != null) {
						ibaMap.put(bus_field.getName(), bus_obj.get(bus_field));
					}
				}
				busObjMap.put(bus_obj.getWTReference(), ibaMap);
			}
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
	logger.debug("<= fetchMultiObjIBA");
	return busObjMap;
}

public static void updatePersistable(Persistable[] persistables, HashMap<String, Object> lowerPartIbaMap) {
	logger.debug("=> updatePersistable");
	// /*
	// * Updates a Global attribute value ("smaString")
	// * on a soft type of WTPart ("com.ptc.ptcnet.smaPart")
	// */
	// try {
	// BusinessObjectHelper BUSINESS_OBJ_HELPER = BusinessObjectHelperFactory.getBusinessObjectHelper();
	// if (persistables != null && persistables.length > 0) {
	// List<BusinessObject> bus_objs = BUSINESS_OBJ_HELPER.newBusinessObjects(SessionHelper.getLocale(),
	// new UpdateOperationIdentifier(), false, persistables);
	//
	// Collection<BusinessField> bus_fields = new ArrayList<>();
	// TypeIdentifier localTypeIdentifier = TypeIdentifierHelper.getType(persistables[0]);
	// for (String attribute : lowerPartIbaMap.keySet()) {
	// BusinessField busField = getBusinessField(localTypeIdentifier, attribute);
	// bus_fields.add(busField);
	// }
	//
	// BUSINESS_OBJ_HELPER.load(bus_objs, bus_fields);
	// for (BusinessObject bus_obj : bus_objs) {
	// for (BusinessField bus_field : bus_fields) {
	// logger.debug("Setting bus field : " + bus_field.getName() + " VALUE : " + lowerPartIbaMap.get(bus_field.getName()));
	// bus_obj.set(bus_field, lowerPartIbaMap.get(bus_field.getName()));
	// }
	// }
	// List<Persistable> modif_objs = BUSINESS_OBJ_HELPER.apply(bus_objs);
	// PersistenceServerHelper.manager.update(new WTArrayList(modif_objs), false);
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }

	logger.debug("<= updatePersistable");
}

public static IBAHolder setAttributeValuesAndPersist(IBAHolder persistable, HashMap<String, Object> map)
		throws Exception {

	logger.debug("=> setAttributeValuesAndPersist");
	IBAHandler promotableIbaHandler = IBAHandler.newIBAHandler(persistable);
	if (map != null) {

		Set<String> attrIds = map.keySet();
		for (String attr : attrIds) {
			promotableIbaHandler.prepareIbaHolderForCreation(attr, IBACreateMode.CREATE_OR_UPDATE);
			promotableIbaHandler.updateIbaHolder();
			if (map.get(attr) != null) {
				if (map.get(attr) instanceof String && !((String) map.get(attr)).trim().equals("")) {
					promotableIbaHandler.createValue(attr, (String) map.get(attr), IBACreateMode.CREATE_OR_UPDATE, null,
							null, null, null);
				}

				else if (map.get(attr) instanceof ArrayList<?>) {

					ArrayList<String> values = (ArrayList<String>) map.get(attr);
					if (values != null) {

						promotableIbaHandler.createValues(attr, values, IBACreateMode.CREATE_OR_UPDATE, null, null,
								null, null);
					}
				}

				else if (map.get(attr) instanceof Object[]) {
					Object[] values = (Object[]) map.get(attr);
					if (values != null) {
						promotableIbaHandler.createValues(attr,
								Arrays.asList(Arrays.copyOf(values, values.length, String[].class)),
								IBACreateMode.CREATE_OR_UPDATE, null, null, null, null);
					}
				}
				promotableIbaHandler.updateIbaHolder();
			}
		}
	}
	persistable = (IBAHolder) PersistenceHelper.manager.refresh((Persistable) persistable);
	logger.debug("<= setAttributeValuesAndPersist");

	return persistable;
}

private static BusinessField getBusinessField(TypeIdentifier type_name, String attr_name) throws WTException {
	BusinessFieldIdentifier bfid = BusinessFieldIdFactoryHelper.FACTORY.getTypeBusinessFieldIdentifier(attr_name,
			type_name);
	BusinessField field = BusinessFieldServiceHelper.SERVICE.getBusinessField(bfid);

	return field;
}

public static HashMap<WTReference, HashMap<String, Object>> fetchMultiObjIBA(Persistable wto, List<String> attrList) {
	logger.debug("=> fetchMultiObjIBA");
	HashMap<WTReference, HashMap<String, Object>> busObjMap = new HashMap<>();
	try {
		BusinessObjectHelper BUS_OBJ_HELPER = BusinessObjectHelperFactory.getBusinessObjectHelper();
		List<BusinessObject> bus_objs = BUS_OBJ_HELPER.newBusinessObjects(SessionHelper.getLocale(),
				new DisplayOperationIdentifier(), false, wto);
		Collection<BusinessField> bus_fields = new ArrayList<>();
		if (wto != null ) {
			TypeIdentifier localTypeIdentifier = TypeIdentifierHelper.getType(wto);

			for (String attribute : attrList) {
				bus_fields.add(getBusinessField(localTypeIdentifier, attribute));
			}
			BUS_OBJ_HELPER.load(bus_objs, bus_fields);
			for (BusinessObject bus_obj : bus_objs) {
				HashMap<String, Object> ibaMap = new HashMap<>();
				for (BusinessField bus_field : bus_fields) {
					// logger.debug(bus_obj.getWTReference().getObject().getIdentity());
					// logger.debug(bus_field.getName());
					// logger.debug(bus_obj.get(bus_field));
					if (bus_field != null && bus_field.getName() != null) {
						ibaMap.put(bus_field.getName(), bus_obj.get(bus_field));
					}
				}
				logger.debug("ibamap : " + ibaMap);
				busObjMap.put(bus_obj.getWTReference(), ibaMap);
			}
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
	logger.debug("<= fetchMultiObjIBA");
	return busObjMap;

}

}
