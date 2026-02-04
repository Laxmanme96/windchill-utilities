package ext.enersys.cm2;

import ext.enersys.cm2.service.CM2Service;
import wt.services.ServiceFactory;

public class CM2Helper {
	public static final CM2Service service = (CM2Service) ServiceFactory.getService(CM2Service.class);
}
