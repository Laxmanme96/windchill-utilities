package ext.enersys.cm4;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import ext.enersys.cm4.service.CM4Service;
import ext.enersys.cm4.service.StandardCM4Service;
import ext.enersys.utilities.Debuggable;
import ext.enersys.utilities.EnerSysLogUtils;
import wt.fc.Persistable;
import wt.fc.WTObject;
import wt.inf.container.WTContainer;
import wt.log4j.LogR;
import wt.util.WTException;

public class CM4ServiceUtility {

	/*public static synchronized CM4Service getInstance(WTObject obj) {
		if (instance == null) {
			instance = new StandardCM4Service(obj);
		}
		return instance;
	}*/
	
	public static synchronized CM4Service getInstance() {
		if (instance == null) {
			instance = new StandardCM4Service();
		}
		return instance;
	}
	private static CM4Service instance;
	private static final String CLASSNAME = CM4ServiceUtility.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(CLASSNAME);
}

