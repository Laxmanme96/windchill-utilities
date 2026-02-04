package ext.enersys.utilities;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.apache.logging.log4j.Logger;
import com.ptc.core.businessfield.common.BusinessField;
import com.ptc.core.businessfield.common.BusinessFieldIdFactoryHelper;
import com.ptc.core.businessfield.common.BusinessFieldServiceHelper;
import com.ptc.core.businessfield.server.BusinessFieldIdentifier;
import com.ptc.core.businessfield.server.businessObject.BusinessObject;
import com.ptc.core.businessfield.server.businessObject.BusinessObjectHelper;
import com.ptc.core.businessfield.server.businessObject.BusinessObjectHelperFactory;
import com.ptc.core.lwc.client.util.EnumerationConstraintHelper;
import com.ptc.core.lwc.common.view.AttributeDefinitionReadView;
import com.ptc.core.lwc.common.view.ConstraintDefinitionReadView;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;
import com.ptc.core.meta.common.AttributeTypeIdentifier;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import wt.fc.Persistable;
import wt.log4j.LogR;
import wt.session.SessionHelper;
import wt.type.TypedUtility;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTException;
import com.ptc.core.lwc.common.view.PropertyValueReadView;
import wt.type.TypedUtility;


/**
 * SoftType helper class for EnerSys Customization activity
 * 
 * @CGI Team
 *
 */
public class EnerSysSoftTypeHelper {
	
	private static final String CLASSNAME = EnerSysSoftTypeHelper.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(CLASSNAME);
	
	// Generic SOFTTYPE_DOMAIN
	public static final String PTC_SOFTTYPE_DOMAIN = EnersysPropertyHelper.getSoftTypeDomain();
	// Used in CR,CN for ext.enersys.
	public static final String CUSTOM_SOFTTYPE_DOMAIN = EnersysPropertyHelper.getCustomSoftTypeDomain();
	// EnerSys Part
	public static final String ENERSYS_PART = EnersysPropertyHelper.getEnersysPartInternalName();
	public static final TypeIdentifier ENERSYS_PART_TI = TypeIdentifierHelper.getTypeIdentifier(PTC_SOFTTYPE_DOMAIN + ENERSYS_PART);

	// COTS Part
	public static final String COTS_PART = EnersysPropertyHelper.getCotsPartInternalName();
	public static final TypeIdentifier COTS_PART_TI = TypeIdentifierHelper.getTypeIdentifier(PTC_SOFTTYPE_DOMAIN + COTS_PART);

	// Firmware Part
	public static final String FIRMWARE_PART = EnersysPropertyHelper.getFirmwarePartInternalName();
	public static final TypeIdentifier FIRMWARE_PART_TI = TypeIdentifierHelper.getTypeIdentifier(PTC_SOFTTYPE_DOMAIN + FIRMWARE_PART);

	// LEGACY Firmware Part
	public static final String LEGACY_FIRMWARE_PART = EnersysPropertyHelper.getLegacyESGPartInternalName();
	public static final TypeIdentifier LEGACY_FIRMWARE_PART_TI = TypeIdentifierHelper.getTypeIdentifier(PTC_SOFTTYPE_DOMAIN + LEGACY_FIRMWARE_PART);

	// Firmware Document
	public static final String ENERSYS_FW_DOC = EnersysPropertyHelper.getFirmwareDocInternalName();
	public static final TypeIdentifier ENERSYS_FW_DOC_TI = TypeIdentifierHelper.getTypeIdentifier(PTC_SOFTTYPE_DOMAIN + ENERSYS_FW_DOC);

	// Reference Document
	public static final String REFERENCE_DOCUMENT = EnersysPropertyHelper.getEnerSysRefDocInternalName();
	public static final TypeIdentifier REFERENCE_DOC_TI = TypeIdentifierHelper.getTypeIdentifier(PTC_SOFTTYPE_DOMAIN + REFERENCE_DOCUMENT);

	// Supplier Part
	public static final String ENERSYS_SUPPLIER_PART = EnersysPropertyHelper.getSupplierPartInternalName();
	public static final TypeIdentifier ENERSYS_SUPPLIER_PART_TI = TypeIdentifierHelper.getTypeIdentifier(ENERSYS_SUPPLIER_PART);

	// Manufacturing Part
	public static final String ENERSYS_MANUFACTURING_PART = EnersysPropertyHelper.getManuFacturingPartInternalName();
	public static final TypeIdentifier ENERSYS_MANUFACTURING_PART_TI = TypeIdentifierHelper.getTypeIdentifier(ENERSYS_MANUFACTURING_PART);

	// Vendor Part
	public static final String ENERSYS_VENDOR_PART = EnersysPropertyHelper.getVendorPartInternalName();
	public static final TypeIdentifier ENERSYS_VENDOR_PART_TI = TypeIdentifierHelper.getTypeIdentifier(ENERSYS_VENDOR_PART);

	// Admin Execution Document Part build 2.10
	public static final String ENERSYS_ADMIN_DOCUMENT = EnersysPropertyHelper.getAdminDocumentInternalName();
	public static final TypeIdentifier ENERSYS_ADMIN_DOCUMENT_TI = TypeIdentifierHelper.getTypeIdentifier(PTC_SOFTTYPE_DOMAIN + ENERSYS_ADMIN_DOCUMENT);

	/**
	 * Method return Display value of a given type
	 * 
	 * @param validTypeIdentifier
	 * @param locale
	 * @return
	 * @throws RemoteException
	 * @throws WTException
	 */
	public static String getTypeDisplayName(@NotNull TypeIdentifier validTypeIdentifier, Locale locale) throws RemoteException, WTException {
		return TypedUtilityServiceHelper.service.getLocalizedTypeName(validTypeIdentifier, locale);
	}

	/**
	 * Method return internal value of a given object
	 * 
	 * @param persistable
	 * @return
	 * @throws RemoteException
	 * @throws WTException
	 */
	public static String getTypeInternalName(@NotNull Object persistable) throws RemoteException, WTException {
		return TypedUtilityServiceHelper.service.getExternalTypeIdentifier(persistable);
	}

	/**
	 * Method looks for exact match for given object's type and TypeIdentifier
	 * 
	 * @param persistable
	 * @param validTypeIdentifier
	 * @return
	 */
	public static boolean isExactlyType(@NotNull Object persistable, @NotNull TypeIdentifier validTypeIdentifier) {

		TypeIdentifier objectTypeIdentifier = TypeIdentifierHelper.getType(persistable);
//		return objectTypeIdentifier.isEquivalentTypeIdentifier(validTypeIdentifier);
		return objectTypeIdentifier.equals(validTypeIdentifier);
	}

	/**
	 * Method looks for exact match for given object's type and Internal name of type
	 * 
	 * @param persistable
	 * @param type
	 * @return
	 */
	public static boolean isExactlyType(@NotNull Object persistable, @NotNull String type) {

		TypeIdentifier objectTypeIdentifier = TypeIdentifierHelper.getType(persistable);
		TypeIdentifier validTypeIdentifier = TypeIdentifierHelper.getTypeIdentifier(type);
		return objectTypeIdentifier.equals(validTypeIdentifier);

	}

	/**
	 * Method looks for match & it's softtypes for given object's type and TypeIdentifier
	 * 
	 * @param persistable
	 * @param validTypeIdentifier
	 * @return
	 */
	public static boolean isDecendentFrom(@NotNull Object persistable, @NotNull TypeIdentifier validTypeIdentifier) {

		TypeIdentifier objectTypeIdentifier = TypeIdentifierHelper.getType(persistable);

		return objectTypeIdentifier.isDescendedFrom(validTypeIdentifier);

	}

	/**
	 * Method looks for match & it's softtypes for given object's type and Internal name of type
	 * 
	 * @param persistable
	 * @param type
	 * @return
	 */
	public static boolean isDecendentFrom(@NotNull Object persistable, @NotNull String type) {

		TypeIdentifier objectTypeIdentifier = TypeIdentifierHelper.getType(persistable);
		TypeIdentifier validTypeIdentifier = TypeIdentifierHelper.getTypeIdentifier(type);
		return objectTypeIdentifier.isDescendedFrom(validTypeIdentifier);

	}
	/**
	 * Get values for Part for given internal name
	 * 
	 * @param persistable
	 * @param internalName
	 * @return
	 * @throws WTException
	 */
	public static Object getAttributesValues(Persistable persistable, String internalName) throws WTException {
		LOGGER.debug("Enter :: getAttributesValues");
		String type = TypedUtility.getTypeIdentifier(persistable).getTypeInternalName();
		DisplayOperationIdentifier displayOperationId = new DisplayOperationIdentifier();
		Persistable[] persistables = new Persistable[] { persistable };
		BusinessObjectHelper busObjHelper = BusinessObjectHelperFactory.getBusinessObjectHelper();
		List<BusinessObject> busObjs = busObjHelper.newBusinessObjects(SessionHelper.getLocale(), displayOperationId,
				true, persistables);
		BusinessFieldIdentifier id = BusinessFieldIdFactoryHelper.FACTORY.getTypeBusinessFieldIdentifier(internalName,
				type);
		BusinessField bussField = BusinessFieldServiceHelper.SERVICE.getBusinessField(id);
		busObjHelper.load(busObjs, Arrays.asList(bussField));
		Object object = busObjs.get(0).get(bussField);
		LOGGER.debug("Attribute Value :: " + object);
		LOGGER.debug("Exit :: getAttributesValues");
		return object;
	}
	
    /**
     * Gets Global Enumeration map of an attribute that has Enumerated Value List constraint.
     *
     * @param attribute attribute name
     * @param typeIdentifier type identifier
     * @return Map<String, String> with internal value as key and display value as value
     * @throws WTException if failed to query enumeration
     */
    public static Map<String, String> getGlobalEnumerationMap(String attribute, TypeIdentifier typeIdentifier) throws WTException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ENTER ClinicalSizesOnTradeItemHelper.getGlobalEnumerationMap() For attribute : " + attribute + " typeIdentifier : " + typeIdentifier);
        }
        AttributeTypeIdentifier attTypeId = TypedUtility.getAttributeTypeIdentifier(attribute, typeIdentifier);
        Map<String, String> enumMap = getGlobalEnumeration(attTypeId);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("EXIT ClinicalSizesOnTradeItemHelper.getGlobalEnumerationMap() Obtained Enum Map : " + enumMap + " for attribute : " + attribute
                    + " typeIdentifier : " + typeIdentifier);
        }
        return enumMap;
    }
    /**
     * Gets the global enumeration defined as constraint on an attribute.
     *
     * @param attTypeId the attribute type identifier
     * @return Map<String, String> with internal value as key and display value as value
     * @throws WTException if failed get attribute definition view
     */
    private static Map<String, String> getGlobalEnumeration(AttributeTypeIdentifier attTypeId) throws WTException {
        Map<String, String> result = new HashMap<>();
        AttributeDefinitionReadView attDefReadView = TypeDefinitionServiceHelper.service.getAttributeDefView(attTypeId);

        /*
         * We could use EnumerationConstraintHelper.getGlobalEnumeratedValueListConstraint (attDefReadView) but it check cdrvIt.getEnumDef().getName() != null,
         * that's always FALSE ...
         */
        if (null != attDefReadView) {
            Collection<ConstraintDefinitionReadView> colConstrDefRV = attDefReadView.getAllConstraints();
            ConstraintDefinitionReadView cdrv = null;
            for (ConstraintDefinitionReadView cdrvIt : colConstrDefRV) {
                if (!isConstraintDisabled(cdrvIt) && (cdrvIt.getAllConditions().isEmpty()) && (null != cdrvIt.getEnumDef()) && (!(cdrvIt.isDisabled()))) {
                    cdrv = cdrvIt;
                    break;
                }
            }
            if (cdrv != null) {
                Collection<ConstraintDefinitionReadView> col = new HashSet<>();
                col.add(cdrv);
                Map<String, String> apiRet = EnumerationConstraintHelper.getEnumInternalAndDisplayValuesFromConstraints(attDefReadView, col);
                if (apiRet != null) {
                    result = apiRet;
                }
            }
        }
        return result;
    }
    /**
     * Checks whether the constraint is disabled or not.
     *
     * @param paramConstraintDefinitionReadView the parameter constraint definition read view
     * @return {@code true} if the constraint is disabled, {@code false} otherwise.
     */
    private static boolean isConstraintDisabled(ConstraintDefinitionReadView paramConstraintDefinitionReadView) {
        String disabled = "disabled";
        PropertyValueReadView localPropertyValueReadView = paramConstraintDefinitionReadView.getPropertyValueByName(disabled);
        if (localPropertyValueReadView == null) {
            return false;
        }
        return Boolean.parseBoolean(localPropertyValueReadView.getValueAsString());
    }

}
