package ext.enersys.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import wt.util.WTProperties;

public class ESPropertyHelper {
	public static final String ES_BUSINESSRULE = "es_businessrule";
	public static final String ES_FILTER = "es_filter";
	public static final String ES_APPROVERS = "es_approvers";
	public static final String ES_CONFIGURABLE_LINK = "es_configurable_link";
	public static final String ES_CONFIGURABLE_TYPE = "es_configurable_type";
	public static final String ES_ENERSYS_PART_TYPE = "es_enersys_part_type";
	public static final String ES_EPM_ATTRIBUTES = "es_epm_attributes";
	public static final String ES_NAMING_HELPER = "es_naming_helper";
	public static final String ES_ESI_HELPER = "es_esi_helper";
	public static final String ES_DOCUMENTAPPROVAL_HELPER = "es_documentapproval_helper";
	public static final String ES_AUTOSELECTPARTICIPANT = "es_autoselectparticipant";
	public static final String ES_STANDALONE_TYPE = "es_standalone_helper";

	private final String componentName;
	private Properties ESProperties;
	private String WT_HOME;
	WTProperties wtproperties;
	private String ESPropsFolder;

	public ESPropertyHelper(String componentString) {
		componentName = componentString;
		ESProperties = new Properties();
		try {
			wtproperties = WTProperties.getLocalProperties();
			WT_HOME = wtproperties.getProperty("wt.home", "");
			String file_sep = wtproperties.getProperty("dir.sep", "\\");
			String codebase = wtproperties.getProperty("wt.home", "") + file_sep + "codebase";
			ESPropsFolder = wtproperties.getProperty("wt.home", "") + file_sep + "codebase" + file_sep + "es" + file_sep
					+ "properties";

			if (componentName.equals(ES_BUSINESSRULE)) {
				ESProperties.load(new FileInputStream(
						codebase + file_sep + "es" + file_sep + "properties" + file_sep + "ESBusinessRule.properties"));
			} else if (componentName.equals(ES_FILTER)) {
				ESProperties.load(new FileInputStream(codebase + file_sep + "es" + file_sep + "properties" + file_sep
						+ "validRoleForActionVisibility.properties"));
			} else if (componentName.equals(ES_APPROVERS)) {
				ESProperties.load(new FileInputStream(codebase + file_sep + "es" + file_sep + "properties" + file_sep
						+ "participantSelection.properties"));
			} else if (componentName.equals(ES_CONFIGURABLE_LINK)) {
				ESProperties.load(new FileInputStream(codebase + file_sep + "es" + file_sep + "properties" + file_sep
						+ "configurableLink.properties"));
			} else if (componentName.equals(ES_CONFIGURABLE_TYPE)) {
				ESProperties.load(new FileInputStream(codebase + file_sep + "es" + file_sep + "properties" + file_sep
						+ "configurableTypeAttribute.properties"));
			} else if (componentName.equals(ES_ENERSYS_PART_TYPE)) {
				ESProperties.load(new FileInputStream(codebase + file_sep + "es" + file_sep + "properties" + file_sep
						+ "enersysPartType.properties"));
			} else if (componentName.equals(ES_EPM_ATTRIBUTES)) {
				ESProperties.load(new FileInputStream(codebase + file_sep + "es" + file_sep + "properties" + file_sep
						+ "EPMAttributeInternalNames.properties"));
			} else if (componentName.equals(ES_NAMING_HELPER)) {
				ESProperties.load(new FileInputStream(codebase + file_sep + "es" + file_sep + "properties" + file_sep
						+ "typeAndCategoryNamingHelper.properties"));
			} else if (componentName.equals(ES_ESI_HELPER)) {
				ESProperties.load(new FileInputStream(
						codebase + file_sep + "es" + file_sep + "properties" + file_sep + "ESI_Helper.properties"));
			} else if (componentName.equals(ES_DOCUMENTAPPROVAL_HELPER)) {
				ESProperties.load(new FileInputStream(codebase + file_sep + "es" + file_sep + "properties" + file_sep
						+ "documentType_Approval.properties"));
			} else if (componentName.equals(ES_AUTOSELECTPARTICIPANT)) {
				ESProperties.load(new FileInputStream(codebase + file_sep + "es" + file_sep + "properties" + file_sep
						+ "autoSelectParticipant.properties"));
			} else if (componentName.equals(ES_STANDALONE_TYPE)) {
				ESProperties.load(new FileInputStream(codebase + file_sep + "es" + file_sep + "properties" + file_sep
						+ "standaloneObjectTypes.properties"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> getProperties(String paramString) {
		String str1 = getProperty(paramString);
		ArrayList localArrayList = new ArrayList();
		if (str1 != null && !str1.equals("")) {
			if (str1.contains(",")) {
				String[] arrayOfString1 = str1.split(",");
				for (String str2 : arrayOfString1) {
					localArrayList.add(str2);
				}
			} else {
				localArrayList.add(str1);
			}
		}
		return localArrayList;
	}

	public HashMap<String, String> streamConvert() {
		return ESProperties.entrySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getKey()),
				e -> String.valueOf(e.getValue()), (prev, next) -> next, HashMap::new));
	}

	public String getProperty(String propertyKey) {
		return ESProperties.getProperty(propertyKey);
	}

	public String getProperty(String propertyKey, String propertyDefaultValue) {
		return ESProperties.getProperty(propertyKey, propertyDefaultValue);
	}

	public String getWT_HOME() {
		return WT_HOME;
	}

	public WTProperties getWtproperties() {
		return wtproperties;
	}

	public String getESPropsFolder() {
		return ESPropsFolder;
	}
}
