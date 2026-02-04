/**
 *
 */
package ext.emerson.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import wt.log4j.LogR;
import wt.util.WTProperties;

/**
 * Base properties class for all Custom configurations...
 */
public class CustomProperties {

	public static final String PROMOTE = "promote";
	public static final String VALIDATE = "validate";
	public static final String PR_VALIDATE = "PRValidate";
	public static final String USER = "user";
	public static final String AUTOCREATE = "autocreate";
	public static final String APPLETONVALIDTYPES = "ValidTypes";
	public static final String APPLETONVALIDCONTEXTS = "ValidContext";

	public static boolean VERBOSE;

	public static Logger getlogger(String name) {
		return LogR.getLogger(name);
	}
// public static Logger getCustomLogger(String name) {
// return Logger.getLogger(name);
//
// }

	private final String componentName;
	private Properties customProperties;
	private String WT_HOME;
	WTProperties wtproperties;
	private String propsFolder;

	/**
	 * @param componentString
	 */
	public CustomProperties(String componentString) {

		componentName = componentString;
		customProperties = new Properties();
		try {
			wtproperties = WTProperties.getLocalProperties();
			WT_HOME = wtproperties.getProperty("wt.home", "");
			String file_sep = wtproperties.getProperty("dir.sep", "\\");
			String codebase = wtproperties.getProperty("wt.home", "") + file_sep + "codebase";
			propsFolder = wtproperties.getProperty("wt.home", "") + file_sep + "custom" + file_sep + "properties";

			if (componentName.equals(PROMOTE))
				customProperties.load(new FileInputStream(propsFolder + file_sep + "promote.properties"));
			else if (componentName.equals(VALIDATE))
				customProperties.load(new FileInputStream(propsFolder + file_sep + "validation.properties"));
			else if (componentName.equals(USER))
				customProperties.load(new FileInputStream(propsFolder + file_sep + "user.properties"));
			else if (componentName.equals(AUTOCREATE))
				customProperties.load(new FileInputStream(propsFolder + file_sep + "autocreate.properties"));
			else if (componentName.equals(PR_VALIDATE))
				customProperties.load(new FileInputStream(propsFolder + file_sep + "PRValidation.properties"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Set<Object> getAllKeys() {
		Set<Object> keys = customProperties.keySet();
		return keys;
	}

// public static Logger getCustomLogger(String appName, ) {
//
// Logger logger = Logger.getLogger();
//// // DOMConfigurator is used to configure logger from xml file
//// DOMConfigurator domc = new DOMConfigurator();
//// domc.configure("custom/log4j.xml");
//
// // Log in file
// logger.debug("Log4j appender configuration is successful !!");
// return logger;
//
// }

	public URL getCustomPropertiesURL() {

		URL url = CustomProperties.class.getResource("customProperties");
		return url;
	}

	public List<String> getMatchingWTProperties(String strPattern, String delim) {

		Enumeration<Object> labelKeys = wtproperties.keys();

		// Build up a buffer of label keys
		StringBuffer sb = new StringBuffer();
		while (labelKeys.hasMoreElements()) {
			String key = (String) labelKeys.nextElement();
			sb.append(key + delim);
		}

		// Choose the pattern for matching
		Pattern pattern1 = Pattern.compile(strPattern + delim);
		Matcher matcher = pattern1.matcher(sb);

		// Attempt to find all matching keys
		List<String> matchingLabelKeys = new ArrayList<>();
		while (matcher.find()) {
			String key = matcher.group();
			matchingLabelKeys.add(key.substring(0, key.length() - 1));
		}

		return matchingLabelKeys;

	}

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

	public String getProperty(String propertyKey) {
		return customProperties.getProperty(propertyKey);
	}

	public String getProperty(String propertyKey, String propertyDefaultValue) {

		return customProperties.getProperty(propertyKey, propertyDefaultValue);
	}

	public String getWT_HOME() {
		return WT_HOME;
	}

	public WTProperties getWtproperties() {
		return wtproperties;
	}

	/**
	 * @return the propsFolder
	 */
	public String getPropsFolder() {
		return propsFolder;
	}

	public  int getIntProperty(String key) {
		int pVal = 0;
		String val = getProperty(key);
		if (val == null) return pVal;
		pVal = java.lang.Integer.parseInt(val);

		return pVal;
	}

	public  boolean getBooleanProperty(String key ) {
		boolean pVal = false;
		String val = getProperty(key);
		if (val == null) return false;

		pVal = "true".equals(val) ? true : false ;

		return pVal;
	}
}
