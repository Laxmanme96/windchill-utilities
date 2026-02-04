package ext.enersys.utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import wt.log4j.LogR;
import wt.util.WTProperties;

public class DrivenAttributePropertyHelper {

	private static final String CLASSNAME = DrivenAttributePropertyHelper.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(DrivenAttributePropertyHelper.class.getName());

	public static String getQualityRecordPropertyValue(String key) {
		String value = "";
		try {
			if (key != null && !key.isEmpty()) {
				LOGGER.debug("-->key:" + key);
				WTProperties props = WTProperties.getLocalProperties();
				Properties customProperties = new Properties();
				String codebase = props.getProperty("wt.codebase.location");
				customProperties.load(new FileInputStream(codebase + "/ext/enersys/properties/QRTypeCategoryProperties.properties"));
				value = (String) customProperties.getProperty(key, "").trim();
				LOGGER.debug("-->values:" + value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return value;
	}

}
