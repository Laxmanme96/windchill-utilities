package ext.enersys.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;

import wt.log4j.LogR;
import wt.util.WTProperties;

/**
 * 
 * @author CGI Team
 * 
 */
public class EnersysPropertyHelper {
	private static final String CLASSNAME = EnersysPropertyHelper.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(EnersysPropertyHelper.class.getName());

	private static final String DELIMITER = ";";
	private static final String EXTRACTOR_PROPERTY_FILENAME = "enersys_customization.properties";
	private static final String fileSeperator = File.separator;
	private static final String PROPERTY_FILE_PATH = fileSeperator + "ext" + fileSeperator + "enersys" + fileSeperator;

	private static String WC_CODEBASE_LOCATION;
	
	public enum ENERSYS_PROP {
		APPROVAL_MAT_LOCATION("APPROVAL_MATRIX_DOCUMENT_LOCATION_WITH_FILENAME"), APPROVAL_MAT_TYPE("APPROVAL_MATRIX_DOCUMENT_TYPE"), APPROVAL_MAT_NAME(
				"APPROVAL_MATRIX_DOCUMENT_NAME"), APPROVAL_MAT_NUMBER("APPROVAL_MATRIX_DOCUMENT_NUMBER"), SOFTTYPE_DOMAIN("GENERIC_SOFTYPE_DOMAIN"), ENERSYS_PART_INTERNAL_NAME(
						"ENERSYS_PART_INTERNAL_NAME"), FIRMWARE_DOC_INTERNAL_NAME("FIRMWARE_DOC_INTERNAL_NAME"), FIRMWARE_PART_INTERNAL_NAME(
								"FIRMWARE_PART_INTERNAL_NAME"), PRODUCT_DOCUMENT_INTERNAL_NAME("PRODUCT_DOCUMENT_INTERNAL_NAME"),PRODUCT_REV_INDE_DOCUMENT_INTERNAL_NAME("PRODUCT_REV_INDE_DOCUMENT_INTERNAL_NAME"), EPM_DOCUMENT_INTERNAL_NAME(
										"EPM_DOCUMENT_INTERNAL_NAME"), QUALITY_DOCUMENT_INTERNAL_NAME("QUALITY_DOCUMENT_INTERNAL_NAME"), DOC_DOCUMENT_INTERNAL_NAME(
												"DOC_DOCUMENT_INTERNAL_NAME"), RESTRICTED_DOCUMENT_INTERNAL_NAME("RESTRICTED_DOCUMENT_INTERNAL_NAME"), SUPPLIER_PART_INTERNAL_NAME(
														"SUPPLIER_PART_INTERNAL_NAME"), LEGACY_ESG_FIRMWARE_PART_INTERNAL_NAME(
																"LEGACY_ESG_FIRMWARE_PART_INTERNAL_NAME"), COTS_PART_INTERNAL_NAME(
																		"COTS_PART_INTERNAL_NAME"), CUSTOM_SOFTTYPE_DOMAIN(
																				"CUSTOM_SOFTTYPE_DOMAIN"), ENERSYS_REFERENCE_DOCUMENT_INTERNAL_NAME(
																						"ENERSYS_REFERENCE_DOCUMENT_INTERNAL_NAME"), MFG_PART_INTERNAL_NAME(
																								"MFG_PART_INTERNAL_NAME"), VENDOR_PART_INTERNAL_NAME(
																										"VENDOR_PART_INTERNAL_NAME"), ADMIN_DOCUMENT_INTERNAL_NAME(
																												"ADMIN_DOCUMENT_INTERNAL_NAME");

		private ENERSYS_PROP() {
			this.key = null;
		}

		ENERSYS_PROP(String key) {
			this.key = key;
		}

		public final String key;
	}

	public static volatile Properties enersysProperties;

	static {
		loadProperties();
	}

	public static synchronized void loadProperties() {
		BufferedReader reader = null;
		try {
			WTProperties wtproperties = null;

			wtproperties = WTProperties.getLocalProperties();
			WC_CODEBASE_LOCATION = wtproperties.getProperty("wt.codebase.location", "");

			enersysProperties = new Properties();
			String propertyFilePath = WC_CODEBASE_LOCATION + PROPERTY_FILE_PATH + EXTRACTOR_PROPERTY_FILENAME;

			reader = new BufferedReader(new InputStreamReader(new FileInputStream(propertyFilePath), StandardCharsets.UTF_8));
			enersysProperties.load(reader);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reload Properties from file
	 */
	public static void refreshProperties() {
		loadProperties();
	}

	private static String getProp(ENERSYS_PROP o) {
		if (enersysProperties != null) {
			return (String) enersysProperties.get(o.key);
		}
		return null;
	}

	public static String getSoftTypeDomain() {
		return getProp(ENERSYS_PROP.SOFTTYPE_DOMAIN);
	}
	//

	public static String getProductDocumentInternalName() {
		return getProp(ENERSYS_PROP.PRODUCT_DOCUMENT_INTERNAL_NAME);
	}
	
	/**
	 * Returns Product Indepedentant Document fetched from enersys_customization.properties
	 * 
	 * @return
	 * @since build 3.2
	 */
	public static String getProductRevIndeDocumentInternalName() {
		return getProp(ENERSYS_PROP.PRODUCT_REV_INDE_DOCUMENT_INTERNAL_NAME);
	}

	public static String getQualityDocumentInternalName() {
		return getProp(ENERSYS_PROP.QUALITY_DOCUMENT_INTERNAL_NAME);
	}

	public static String getDocDocumentInternalName() {
		return getProp(ENERSYS_PROP.DOC_DOCUMENT_INTERNAL_NAME);
	}

	public static String getRestrictedDocumentInternalName() {
		return getProp(ENERSYS_PROP.RESTRICTED_DOCUMENT_INTERNAL_NAME);
	}

	public static String getEnersysPartInternalName() {
		return getProp(ENERSYS_PROP.ENERSYS_PART_INTERNAL_NAME);
	}

	public static String getFirmwarePartInternalName() {
		return getProp(ENERSYS_PROP.FIRMWARE_PART_INTERNAL_NAME);
	}

	/**
	 * @since Build v2.4
	 * @return
	 */
	public static String getLegacyESGPartInternalName() {
		return getProp(ENERSYS_PROP.LEGACY_ESG_FIRMWARE_PART_INTERNAL_NAME);
	}

	public static String getFirmwareDocInternalName() {
		return getProp(ENERSYS_PROP.FIRMWARE_DOC_INTERNAL_NAME);
	}

	public static String getApprovalMatrixDocNumber() {
		return getProp(ENERSYS_PROP.APPROVAL_MAT_NUMBER);
	}

	public static TypeIdentifier getApprovalMatrixDocType() {
		if (getProp(ENERSYS_PROP.APPROVAL_MAT_TYPE) != null) {
			return TypeIdentifierHelper.getTypeIdentifier(getProp(ENERSYS_PROP.APPROVAL_MAT_TYPE));
		}
		return null;
	}

	public static String getBaseWTPartName() {
		return "wt.part.WTPart";
	}

	public static String getApprovalMatrixDocName() {
		return getProp(ENERSYS_PROP.APPROVAL_MAT_NAME);
	}

	public static String getApprovalMatrixAbsLocationWithFileName() {
		if (getProp(ENERSYS_PROP.APPROVAL_MAT_LOCATION) != null) {
			return WC_CODEBASE_LOCATION + File.separator + getProp(ENERSYS_PROP.APPROVAL_MAT_LOCATION);
		}
		return null;
	}

	/**
	 * Build v1.13 for CAD State validator.
	 * 
	 * @return Returns EPMDocument main type name
	 * @since v1.13
	 */
	public static String getEPMDocumentInternalName() {
		return getProp(ENERSYS_PROP.EPM_DOCUMENT_INTERNAL_NAME);
	}

	/**
	 * 
	 * @return Supplier Part parent type name.
	 * @since Build v2.2
	 */
	public static String getSupplierPartInternalName() {
		return getProp(ENERSYS_PROP.SUPPLIER_PART_INTERNAL_NAME);
	}

	/**
	 * Returns COTS_PART_INTERNAL_NAME fetched from enersys_customization.properties
	 * 
	 * @return
	 * @since build 2.7
	 */
	public static String getCotsPartInternalName() {
		return getProp(ENERSYS_PROP.COTS_PART_INTERNAL_NAME);
	}

	/**
	 * Returns Custom domain set for few types.CUSTOM_SOFTTYPE_DOMAIN fetched from enersys_customization.properties
	 * 
	 * @return
	 * @since build 2.7
	 */
	public static String getCustomSoftTypeDomain() {
		return getProp(ENERSYS_PROP.CUSTOM_SOFTTYPE_DOMAIN);
	}

	/**
	 * Returns ENERSYS_REFERENCE_DOCUMENT_INTERNAL_NAME fetched from enersys_customization.properties
	 * 
	 * @return
	 * @since build 2.7
	 */
	public static String getEnerSysRefDocInternalName() {
		return getProp(ENERSYS_PROP.ENERSYS_REFERENCE_DOCUMENT_INTERNAL_NAME);
	}

	/**
	 * Returns MFG_PART_INTERNAL_NAME fetched from enersys_customization.properties
	 * 
	 * @return
	 * @since build 2.7
	 */
	public static String getManuFacturingPartInternalName() {
		return getProp(ENERSYS_PROP.MFG_PART_INTERNAL_NAME);
	}

	/**
	 * Returns VENDOR_PART_INTERNAL_NAME fetched from enersys_customization.properties
	 * 
	 * @return
	 * @since build 2.7
	 */
	public static String getVendorPartInternalName() {
		return getProp(ENERSYS_PROP.VENDOR_PART_INTERNAL_NAME);
	}

	/**
	 * Returns ADMIN_DOCUMENT_INTERNAL_NAME fetched from enersys_customization.properties
	 * 
	 * @return
	 * @since build 2.10
	 */
	public static String getAdminDocumentInternalName() {
		return getProp(ENERSYS_PROP.ADMIN_DOCUMENT_INTERNAL_NAME);
	}
}
