package ext.enersys.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.ptc.core.businessfield.common.BusinessField;
import com.ptc.core.businessfield.common.BusinessFieldIdFactoryHelper;
import com.ptc.core.businessfield.common.BusinessFieldServiceHelper;
import com.ptc.core.businessfield.server.BusinessFieldIdentifier;
import com.ptc.core.businessfield.server.businessObject.BusinessObject;
import com.ptc.core.businessfield.server.businessObject.BusinessObjectHelper;
import com.ptc.core.businessfield.server.businessObject.BusinessObjectHelperFactory;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.FloatingPoint;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.core.meta.common.UpdateOperationIdentifier;

import wt.access.NotAuthorizedException;
import wt.doc.WTDocument;
import wt.fc.Identified;
import wt.fc.IdentityHelper;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.iba.definition.IBADefinitionException;
import wt.iba.definition.litedefinition.AttributeDefDefaultView;
import wt.iba.definition.litedefinition.FloatDefView;
import wt.iba.definition.litedefinition.IntegerDefView;
import wt.iba.definition.litedefinition.StringDefView;
import wt.iba.definition.litedefinition.TimestampDefView;
import wt.iba.definition.service.IBADefinitionHelper;
import wt.iba.definition.service.StandardIBADefinitionService;
import wt.iba.value.DefaultAttributeContainer;
import wt.iba.value.IBAHolder;
import wt.iba.value.litevalue.AbstractContextualValueDefaultView;
import wt.iba.value.litevalue.AbstractValueView;
import wt.iba.value.litevalue.FloatValueDefaultView;
import wt.iba.value.litevalue.IntegerValueDefaultView;
import wt.iba.value.litevalue.StringValueDefaultView;
import wt.iba.value.litevalue.TimestampValueDefaultView;
import wt.iba.value.service.IBAValueHelper;
import wt.iba.value.service.StandardIBAValueService;
import wt.inf.container.OrgContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.log4j.LogR;
import wt.org.WTOrganization;
import wt.org.WTUser;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.part.WTPartMasterIdentity;
import wt.pdmlink.PDMLinkProduct;
import wt.pom.PersistenceException;
import wt.preference.PreferenceClient;
import wt.preference.PreferenceHelper;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.type.ClientTypedUtility;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.WorkInProgressServerHelper;
import wt.vc.wip.Workable;

public class ESBusinessHelper {

	private static final String ES_CONTEXT_VISIBILITY_PREF_INTR_NAME = "/ext/enersys/ES_CONTEXT_PREFERENCE";
	private static final String BELLINGHAM_CONTEXT_VISIBILITY_PREF_INTR_NAME = "/ext/enersys/BELLINGHAM_CONTEXT_PREFERENCE";
	private static final String PREF_VAL_SEPERATOR = ";";
	private static final String CLASSNAME = ESBusinessHelper.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(CLASSNAME);

	public static WTObject getWTObjectByNumber(Class<?> objectClassType, String objectNumber) throws WTException {

		WTObject object = null;
		QuerySpec qs = new QuerySpec(objectClassType);
		qs.appendWhere(new SearchCondition(objectClassType, "master>number", SearchCondition.EQUAL, objectNumber),
				new int[] { 0 });
		QueryResult result = PersistenceServerHelper.manager.query(qs);
		if (result.hasMoreElements())
			object = (WTObject) result.nextElement();
		else
			LOGGER.debug("Either Object Class or Name is incorrect");
		return object;
	}

	public static WTPart getPart(String partNumber) throws WTException {
		try {
			QuerySpec qs = new QuerySpec(WTPart.class);
			qs.appendWhere(new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, partNumber),
					new int[] { 0 });
			QueryResult result = PersistenceServerHelper.manager.query(qs);
			if (result.size() > 0) {
				WTPart part = (WTPart) result.nextElement();
				return part;
			}
		} catch (Exception e) {
			LOGGER.debug("Exception");
		}
		return null;
	}

	// Method to check out a WTPart if it is not already checked out
	public static void checkOutPart(WTPart part) throws WTException, WTPropertyVetoException {
		if (!WorkInProgressHelper.isCheckedOut((Workable) part)) {
			WorkInProgressHelper.service.checkout(part, WorkInProgressHelper.service.getCheckoutFolder(), "");
		} else {
			LOGGER.debug("Part is already checked out.");
		}
	}

	// Method to check in a WTPart with a comment if it is checked out
	public static void checkInPart(WTPart part, String comment) throws WTException, WTPropertyVetoException {
		if (WorkInProgressHelper.isCheckedOut((Workable) part)) {
			part = (WTPart) WorkInProgressHelper.service.checkin(part, comment);
			LOGGER.debug("Part checked in with comment: " + comment);
		} else {
			LOGGER.debug("Part is not checked out, cannot check in.");
		}
	}

	// Method to undo checkout for a WTPart if it is checked out
	public static WTPart undoCheckout(WTPart part) throws WTException, WTPropertyVetoException {
		if (WorkInProgressHelper.isCheckedOut((Workable) part)) {
			part = (WTPart) WorkInProgressHelper.service.undoCheckout(part);
			LOGGER.debug("Checkout undone for part: " + part.getNumber());
			return part;
		} else {
			LOGGER.debug("Part is not checked out, cannot undo.");
			return part;
		}
	}

	// Method to find and return the latest iteration of a WTPart
	public static WTPart findLatestIteration(WTPart part) throws WTException {
		part = (WTPart) VersionControlHelper.service.allIterationsOf(part.getMaster()).nextElement();
		LOGGER.debug("Latest iteration found: " + part.getIterationIdentifier().getValue());
		return part;
	}

	// Method to get and print all iterations of a WTPart
	public static void getAllPartIterations(WTPart part) throws PersistenceException, WTException {
		WTPartMaster partMaster = (WTPartMaster) part.getMaster();
		QueryResult allIterations = VersionControlHelper.service.allIterationsOf(partMaster);

		LOGGER.debug("Iterations............:" + allIterations.size());
		while (allIterations.hasMoreElements()) {
			WTPart prt = (WTPart) allIterations.nextElement();
			LOGGER.debug(prt.getVersionIdentifier().getValue() + "." + prt.getIterationIdentifier().getValue());
		}
	}

	// Retrieves and prints all iterations of a given WTPart
	public static void getAllPartRevisions(WTPart part) throws PersistenceException, WTException {
		QueryResult allrev = VersionControlHelper.service.allVersionsOf(part);

		LOGGER.debug("Revisions............:" + allrev.size());
		while (allrev.hasMoreElements()) {
			WTPart prt = (WTPart) allrev.nextElement();
			LOGGER.debug(prt.getVersionIdentifier().getValue() + "." + prt.getIterationIdentifier().getValue());
		}
	}

	// Method to get the IBA value for a WTPart
	public static String getIBAValue(WTPart part, String ibaAttributeName) throws WTException {
		try {
			PersistableAdapter pa = new PersistableAdapter(part, null, Locale.US, null);
			pa.load(ibaAttributeName);
			Object ibaValue = pa.get(ibaAttributeName);
			return ibaValue != null ? ibaValue.toString() : null;
		} catch (Exception e) {
			throw new WTException(e);
		}
	}

	// Method to update the IBA value for a WTDocument
	public static void updateIBAValueDoc(WTDocument checkoutDoc, String attributeName, String attributeValue)
			throws WTException {
		PersistableAdapter pa = new PersistableAdapter(checkoutDoc, null, Locale.US, null);
		pa.load(attributeName);
		pa.set(attributeName, attributeValue);
		pa.apply();
		PersistenceHelper.manager.modify(checkoutDoc);
		LOGGER.debug("IBA Value updated...");
	}

	// Method to update the IBA value for a WTPart
	public static void updateIBAValuePart(WTPart part, String ibaAttributeName, String newValue) throws WTException {
		try {
			PersistableAdapter pa = new PersistableAdapter(part, null, Locale.US, null);
			pa.load(ibaAttributeName);
			pa.set(ibaAttributeName, newValue);
			pa.apply();
			PersistenceHelper.manager.modify(part);
			LOGGER.debug("IBA Value updated: " + ibaAttributeName + " = " + newValue);
		} catch (Exception e) {
			throw new WTException(e);
		}
	}

	public static Object getIBAValue(Persistable persistable, String attributeName) throws WTException {

		Object strIBAValue = null;
		Object ibaObj = null;
		try {
			PersistableAdapter obj = new PersistableAdapter(persistable, null, Locale.US, null);
			obj.load(attributeName);
			ibaObj = obj.get(attributeName);
			if (ibaObj != null) {
				if (ibaObj instanceof String) {
					strIBAValue = ibaObj.toString();
				} else if (ibaObj instanceof Boolean) {
					strIBAValue = (Boolean) ibaObj;
				} else {
					LOGGER.debug("Sorry this data type not allowed");
				}
			} else {
				LOGGER.debug("Please enter the attribute");
			}
		} catch (Exception e) {
			e.toString();
		}
		return strIBAValue;
	}

	// Generic method to update IBA value
	public static void updateIBAValue(Persistable persistable, String ibaAttributeName, Object newValue)
			throws WTException {
		try {
			if (persistable instanceof WTPart) {
				WTPart part = (WTPart) persistable;
				WTPart latestElement = (WTPart) VersionControlHelper.service.allIterationsOf(part.getMaster())
						.nextElement();
				if (!WorkInProgressHelper.isCheckedOut((Workable) latestElement)) {
					part = (WTPart) WorkInProgressHelper.service
							.checkout(latestElement, WorkInProgressHelper.service.getCheckoutFolder(), "")
							.getWorkingCopy();
				}
				PersistableAdapter pa = new PersistableAdapter(part, null, Locale.US, null);
				pa.load(ibaAttributeName);
				Object typeobj = pa.get(ibaAttributeName);

				if (typeobj instanceof String && newValue instanceof String) {
					pa.set(ibaAttributeName, newValue);
				} else if (typeobj instanceof Boolean && newValue instanceof Boolean) {
					pa.set(ibaAttributeName, newValue);
				} else if (typeobj instanceof Integer && newValue instanceof Integer) {
					pa.set(ibaAttributeName, newValue);
				} else {
					LOGGER.debug("Unsupported class type..");
				}
				pa.apply();
				PersistenceHelper.manager.modify(part);
				LOGGER.debug("IBA Value updated: " + ibaAttributeName + " = " + newValue);
				WorkInProgressHelper.service.checkin(part, "checked in by API");
			} else if (persistable instanceof WTDocument) {
				WTDocument doc = (WTDocument) persistable;
				WTDocument latestElement = (WTDocument) VersionControlHelper.service.allIterationsOf(doc.getMaster())
						.nextElement();
				if (!WorkInProgressHelper.isCheckedOut((Workable) latestElement)) {
					doc = (WTDocument) WorkInProgressHelper.service
							.checkout((Workable) latestElement, WorkInProgressHelper.service.getCheckoutFolder(), "")
							.getWorkingCopy();
				}
				PersistableAdapter pa = new PersistableAdapter(doc, null, Locale.US, null);
				pa.load(ibaAttributeName);
				Object typeobj = pa.get(ibaAttributeName);

				if (typeobj instanceof String && newValue instanceof String) {
					pa.set(ibaAttributeName, newValue);
				} else if (typeobj instanceof Boolean && newValue instanceof Boolean) {
					pa.set(ibaAttributeName, newValue);
				} else if (typeobj instanceof Integer && newValue instanceof Integer) {
					pa.set(ibaAttributeName, newValue);
				} else {
					LOGGER.debug("Unsupported class type..");
				}
				pa.apply();
				PersistenceHelper.manager.modify(doc);
				LOGGER.debug("IBA Value updated: " + ibaAttributeName + " = " + newValue);
				WorkInProgressHelper.service.checkin((Workable) doc, "checked in by API");
			}

		} catch (Exception e) {
			throw new WTException(e);
		}
	}

	public static WTPart changeNameAndNumberOfPart(String oldNumber, String newNumber, String newName)
			throws WTException, WTPropertyVetoException {
		WTPart part = null;

		if ((WTPart) getWTObjectByNumber(WTPart.class, newNumber) != null)
			LOGGER.debug("Part with the same number already exists");
		else {
			part = (WTPart) getWTObjectByNumber(WTPart.class, oldNumber);

			Boolean enforced = SessionServerHelper.manager.isAccessEnforced();
			SessionServerHelper.manager.setAccessEnforced(false);

			part = (WTPart) VersionControlHelper.service.allIterationsOf(part.getMaster()).nextElement();

			if (!WorkInProgressHelper.isCheckedOut(part)) {

				checkOutPart(part);
				checkInPart(part, "");

				Identified id = (Identified) part.getMaster();// WTPartMaster partMaster
				WTPartMasterIdentity identity = (WTPartMasterIdentity) id.getIdentificationObject(); // master.getIden
				if (newName != null && !newName.isEmpty())
					identity.setName(newName);
				if (newNumber != null && !newNumber.isEmpty())
					identity.setNumber(newNumber);

				// identityMap.put(partMaster, identity);

				id = IdentityHelper.service.changeIdentity(id, identity);
				PersistenceServerHelper.manager.update(part.getMaster());
				PersistenceHelper.manager.refresh(id);

				LOGGER.debug("#Part Name:" + part.getName());
				LOGGER.debug("#Part Number:" + part.getNumber());

				if (enforced)
					SessionServerHelper.manager.setAccessEnforced(true);
			}
		}

		return part;
	}

	public static Properties readPropertiesFile(String fileName) throws IOException {
		FileInputStream fis = null;
		Properties prop = null;
		try {
			fis = new FileInputStream(fileName);
			prop = new Properties();
			prop.load(fis);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			fis.close();
		}
		return prop;
	}

	public static WTContainerRef getContainerRef(WTPart part) throws WTException {
		PDMLinkProduct product = (PDMLinkProduct) WTContainerHelper.getContainer(part);
		WTContainerRef c_ref = WTContainerRef.newWTContainerRef(product);
		return c_ref;
	}

	public static String getContainerPreferences(WTPart part, String preferenceInternalName) throws WTException {
		WTContainerRef containerRef = getContainerRef(part);
		WTUser user = (WTUser) SessionHelper.manager.getPrincipal();
		String prefValue = (String) PreferenceHelper.service.getValue(containerRef, preferenceInternalName,
				PreferenceClient.WINDCHILL_CLIENT_NAME, user);
		return prefValue;
	}

	public static String getUserPreferences(String preferenceInternalName) throws WTException {
		WTUser user = (WTUser) SessionHelper.manager.getPrincipal();
		String prefValue = (String) PreferenceHelper.service.getValue(user, preferenceInternalName,
				PreferenceClient.WINDCHILL_CLIENT_NAME);
		return prefValue;
	}

	public static String getPreferenceValue(String preferenceInternalName, WTContainerRef containerRef)
			throws WTException {
		String prefValue = (String) PreferenceHelper.service.getValue(containerRef, preferenceInternalName,
				PreferenceClient.WINDCHILL_CLIENT_NAME, null);
		return prefValue;
	}

	public static String getContextPreferenceValue(String preferenceInternalName, WTContainerRef containerRef)
			throws WTException {
		WTUser user = (WTUser) SessionHelper.manager.getPrincipal();
		String prefValue = (String) PreferenceHelper.service.getValue(containerRef, preferenceInternalName,
				PreferenceClient.WINDCHILL_CLIENT_NAME, user);
		return prefValue;
	}

	public static Boolean getFrameWorkPreferenceValue(String preferenceInternalName, WTContainerRef containerRef)
			throws WTException {
		WTUser user = (WTUser) SessionHelper.manager.getPrincipal();
		boolean prefValue = (boolean) PreferenceHelper.service.getValue(containerRef, preferenceInternalName,
				PreferenceClient.WINDCHILL_CLIENT_NAME, user);
		return prefValue;
	}

	public static TypeIdentifier getTypeIdentifier(String typeInternalName) {
		TypeIdentifier typeIdentifier = TypeIdentifierHelper.getTypeIdentifier(typeInternalName);
		return typeIdentifier;
	}

	public static void removeAttributeValue(WTObject obj, String attributeInternalName) throws Exception {
		final BusinessObjectHelper BUSINESS_OBJ_HELPER = BusinessObjectHelperFactory.getBusinessObjectHelper();

		final List<BusinessObject> businessObjects = BUSINESS_OBJ_HELPER.newBusinessObjects(SessionHelper.getLocale(),
				new UpdateOperationIdentifier(), false, obj);

		TypeIdentifier typeIdentifier = ClientTypedUtility.getTypeIdentifier(obj);
		BusinessFieldIdentifier businessFieldIdentifier = BusinessFieldIdFactoryHelper.FACTORY
				.getTypeBusinessFieldIdentifier(attributeInternalName, typeIdentifier);
		BusinessField businessField = BusinessFieldServiceHelper.SERVICE.getBusinessField(businessFieldIdentifier);
		Collection<BusinessField> businessFields = Arrays.asList(businessField);
		BUSINESS_OBJ_HELPER.load(businessObjects, businessFields);

		for (BusinessObject businessObject : businessObjects) {
			Object attributeCurrentValue = businessObject.get(businessField);
			LOGGER.debug("#Inside For: " + attributeCurrentValue);
			if (attributeCurrentValue != null && !attributeCurrentValue.equals("")) {
				LOGGER.debug("#INSIDE IF");
				LOGGER.debug("#Attribute Value: " + businessObject.get(businessField).toString());
				businessObject.set(businessField, "");
				List<Persistable> modif_objs = BUSINESS_OBJ_HELPER.apply(businessObjects);
				PersistenceHelper.manager.modify(new WTArrayList(modif_objs));
				LOGGER.debug("#IBA Value updated...");
			}
		}
	}

	public static void updateIBAWithoutIteration(Persistable obj, String attributeName, Object ibaValue)
			throws Exception, IBADefinitionException, NotAuthorizedException, RemoteException, WTException,
			WTPropertyVetoException {
		try {
			IBAHolder ibaholder = (IBAHolder) obj;
			ibaholder = IBAValueHelper.service.refreshAttributeContainer(ibaholder, null, null, null);
			DefaultAttributeContainer defaultattributecontainer = (DefaultAttributeContainer) ibaholder
					.getAttributeContainer();
			AttributeDefDefaultView addv = IBADefinitionHelper.service.getAttributeDefDefaultViewByPath(attributeName);
			AbstractValueView aabstractvalueview[] = defaultattributecontainer.getAttributeValues(addv);
			if (aabstractvalueview.length < 1) {
				StringValueDefaultView svdobj = new StringValueDefaultView((StringDefView) addv, (String) ibaValue);
				defaultattributecontainer.addAttributeValue(((AbstractValueView) (svdobj)));
			} else {
				for (int k = 0; k < aabstractvalueview.length; k++) {
					((StringValueDefaultView) aabstractvalueview[k]).setValue((String) ibaValue);
					defaultattributecontainer.updateAttributeValue(aabstractvalueview[k]);
				}
			}
			StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaholder, null, null, null);
		} catch (Exception e) {
			e.getMessage();
			e.printStackTrace();
		}
	}

	public static Persistable updateIBAValueNoIter(IBAHolder ibaHolder, String ibaName, Object ibaValue)
			throws RemoteException, WTException, WTPropertyVetoException {
		LOGGER.debug("...Inside updateIBAValue...");
		ibaHolder = IBAValueHelper.service.refreshAttributeContainer(ibaHolder, null, null, null);
		StandardIBADefinitionService defService = new StandardIBADefinitionService();
		DefaultAttributeContainer attributeContainer = (DefaultAttributeContainer) ibaHolder.getAttributeContainer();
		AttributeDefDefaultView attributeDefinition = defService.getAttributeDefDefaultViewByPath(ibaName);

		AbstractContextualValueDefaultView attrValue = null;
		AbstractValueView abstractValueView[] = attributeContainer.getAttributeValues(attributeDefinition);
		LOGGER.debug("abstractValueView.length..." + abstractValueView.length);
		if ((abstractValueView.length == 0)) { // No existing value, needs to be created if approvedDate argument has a
												// value

			if (null != ibaValue) {

				if (attributeDefinition instanceof TimestampDefView && ibaValue instanceof Timestamp) {
					LOGGER.debug("attributeDefinition is TimestampDefView...");
					attrValue = new TimestampValueDefaultView((TimestampDefView) attributeDefinition,
							(Timestamp) ibaValue);

				} else if (attributeDefinition instanceof StringDefView) {
					LOGGER.debug("attributeDefinition is StringDefView...");
					attrValue = new StringValueDefaultView((StringDefView) attributeDefinition, ibaValue.toString());

				} else if (attributeDefinition instanceof FloatDefView) {
					LOGGER.debug("attributeDefinition is FloatDefView...");
					if (ibaValue instanceof FloatingPoint) {
						LOGGER.debug("ibaValue is FloatingPoint..." + ibaValue);
						ibaValue = ((FloatingPoint) ibaValue).getValue();
						LOGGER.debug("ibaValue..." + ibaValue);
					}

					attrValue = new FloatValueDefaultView((FloatDefView) attributeDefinition, (Double) ibaValue, 5);

				} else if (attributeDefinition instanceof IntegerDefView) {
					LOGGER.debug("attributeDefinition is IntegerDefView...");
					attrValue = new IntegerValueDefaultView((IntegerDefView) attributeDefinition, (Long) ibaValue);

				}
				attributeContainer.addAttributeValue(attrValue);
			} else {
				LOGGER.debug("ibaValue is null...");
				return (Persistable) ibaHolder;
			}
		} else { // Has existing value, needs to be updated/resetted
			AbstractValueView avv = abstractValueView[0];
			if (null == ibaValue) {// Reset case

				attributeContainer.deleteAttributeValue(avv);

			} else { // Update case

				LOGGER.debug("Update Case...");
				if (avv instanceof TimestampValueDefaultView) {
					LOGGER.debug("avv is TimestampValueDefaultView...");
					((TimestampValueDefaultView) avv).setValue((Timestamp) ibaValue);

				} else if (avv instanceof StringValueDefaultView) {
					LOGGER.debug("avv is StringValueDefaultView...");
					((StringValueDefaultView) avv).setValue(ibaValue.toString());

				} else if (avv instanceof FloatValueDefaultView) {
					LOGGER.debug("avv is FloatValueDefaultView...");
					if (ibaValue instanceof FloatingPoint) {
						LOGGER.debug("ibaValue is FloatingPoint for Update case..." + ibaValue);
						ibaValue = ((FloatingPoint) ibaValue).getValue();
						LOGGER.debug("ibaValue..." + ibaValue);
					}
					((FloatValueDefaultView) avv).setValue((Double) ibaValue);

				} else if (avv instanceof IntegerValueDefaultView) {
					LOGGER.debug("avv is IntegerValueDefaultView...");
					((IntegerValueDefaultView) avv).setValue((Long) ibaValue);
				}
				attributeContainer.updateAttributeValue(avv);

			}

		}
		ibaHolder.setAttributeContainer(attributeContainer);
		StandardIBAValueService.theIBAValueDBService.updateAttributeContainer(ibaHolder, null, null, null);
		WTCollection byPassIterationModifierSet = new WTHashSet();
		byPassIterationModifierSet.add(ibaHolder);
		WorkInProgressServerHelper.putInTxMapForValidateModifiable(byPassIterationModifierSet);
		Persistable persObject = PersistenceHelper.manager.save((Persistable) ibaHolder);
		// PersistenceServerHelper.manager.insert((Persistable)ibaHolder);
		return persObject;
	}
}
