
package ext.emerson.util;


import java.util.Arrays;

import wt.fc.Persistable;
import wt.fc.WTObject;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.type.TypedUtilityService;
import wt.util.WTException;

public class BusinessObjectValidator {

	public static final String WTDOCUMENT = "WCTYPE|wt.doc.WTDocument";
	public static final String WTPART = "WCTYPE|wt.part.WTPart";
	public static final String EPMDOCUMENT = "WCTYPE|wt.epm.EPMDocument";
	public static final String DESIGNDOC = "com.pluraltech.windchill.DESIGN_DOCUMENT";
	public static final String APPLDOC = "com.pluraltech.windchill.APPL_DOCUMENT";
	public static final String MECHANICALPART = "wt.part.WTPart";

	public static final String APPLCONTEXT = "APPL";
	public static final String SOLACONTEXT = "SOLA";

	public static boolean isObjectOfType(String typename, Persistable object) throws WTException {

		String typeArr[] = typename.split(",");
		TypedUtilityService service = wt.services.ServiceFactory.getService(TypedUtilityService.class);
		String typeName = service.getTypeIdentifier(object).getTypename();
		if (Arrays.stream(typeArr).anyMatch(typeName::contains))
			return true;
		return false;

	}

	public static boolean isObjectinContext(String contextName, WTObject object) throws WTException {

		String contArr[] = contextName.split(",");
		WTContainer container = WTContainerHelper.getContainer((WTContained) object);
		if (Arrays.stream(contArr).anyMatch(container.getName()::contains))
			return true;
		return false;

	}

	public static boolean isObjectValid(String prop, String toCheck) throws WTException {

		String contArr[] = prop.split(",");
		// WTContainer container=WTContainerHelper.getContainer((WTContained)object);
		if (Arrays.stream(contArr).anyMatch(toCheck::contains))
			return true;
		return false;

	}
}
