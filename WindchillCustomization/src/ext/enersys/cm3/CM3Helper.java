package ext.enersys.cm3;

import ext.enersys.cm3.service.CM3Service;
import wt.services.ServiceFactory;

/**
 * Helps facilitate usage of CM3.0 features in Create wizard, delegate processing & TeamTemplate processing.
 * 
 * @since Build v2.1
 * @author abhijith.sudheesh
 *
 */
public class CM3Helper {
	private CM3Helper() {

	}

	public static final CM3Service service = ServiceFactory.getService(CM3Service.class);
}
