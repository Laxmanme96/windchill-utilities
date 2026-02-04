package ext.enersys.service;

import wt.services.ServiceFactory;

public class EnerSysHelper {

	public static final EnerSysService service = (EnerSysService) ServiceFactory.getService(EnerSysService.class);

}
