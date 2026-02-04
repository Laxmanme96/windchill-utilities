package ext.enersys.cm2.xml;

import ext.enersys.cm2.xml.service.EnerSysApprovalMatrixDefinition;
import ext.enersys.cm2.xml.service.StandardEnerSysApprovalMatrixDefinition;

//EnerSysApprovalMatrixHelper --> EnerSysApprovalMatrixUtility
public class EnerSysApprovalMatrixUtility {

	public static synchronized EnerSysApprovalMatrixDefinition getInstance() {
		if (instance == null) {
			instance = new StandardEnerSysApprovalMatrixDefinition();
		}
		return instance;
	}

	// public static final EnerSysApprovalMatrixDefinition service = (EnerSysApprovalMatrixDefinition) ServiceFactory.getService(EnerSysApprovalMatrixDefinition.class);
	private static EnerSysApprovalMatrixDefinition instance;
}
