package ext.emerson.access;

import java.io.File;

import org.apache.logging.log4j.Logger;

import wt.log4j.LogR;
import wt.session.SessionContext;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.util.WTException;

public class CustomAccess extends SecurityManager {
	protected static final Logger logger = LogR.getLogger(CustomAccess.class.getName());

	public static final CustomAccess newInstance() {
		return new CustomAccess();
	}

protected boolean			enforced		= false;

protected SessionContext	previousContext	= null;

public final void switchToAdministrator() throws WTException {
	switchToPreviousUser();
	enforced = SessionServerHelper.manager.setAccessEnforced(false);
	previousContext = SessionContext.newContext();
	SessionHelper.manager.setAdministrator();
}

public final void switchToPreviousUser() {
	if (previousContext == null) {
		return;
	}
	SessionContext.setContext(previousContext);
	SessionServerHelper.manager.setAccessEnforced(enforced);
	previousContext = null;
	enforced = false;
}

public final void switchToNewUser(String name) throws WTException {
	switchToPreviousUser();
	enforced = SessionServerHelper.manager.setAccessEnforced(false);
	previousContext = SessionContext.newContext();
	SessionHelper.manager.setPrincipal(name);
}

public boolean checkFileRead(File file) {

	// set the policy file as the system security policy
	System.setProperty("java.security.policy", "file:/C:/java.policy");

	// set the system security manager
	System.setSecurityManager(this);
	try {
		// perform the check
		checkRead(file.getAbsolutePath());
		return true;
	} catch (SecurityException s) {

			logger.debug("SecurityException! File : " + file.getAbsolutePath() + " cannot be read.");
			s.getLocalizedMessage();
		}
		return false;
	}

public boolean checkFileWrite(File file) {

	// set the policy file as the system security policy
	System.setProperty("java.security.policy", "file:/C:/java.policy");

	// // create a security manager
	// CustomAccess sm = new CustomAccess();

	// set the system security manager
	System.setSecurityManager(this);
	try {
		// perform the check
		checkWrite(file.getAbsolutePath());
		return true;
	} catch (SecurityException s) {

			logger.debug("SecurityException!!!! File : " + file.getAbsolutePath() + " cannot be written.");
			s.printStackTrace();
		}
		return false;
	}

}
