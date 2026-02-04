package ext.enersys.classification.es;

import java.util.Collection;

import com.ptc.core.lwc.common.AttributeTemplateFlavor;
import com.ptc.core.lwc.common.dynamicEnum.EnumerationEntryInfo;
import com.ptc.core.lwc.common.view.AttributeDefinitionReadView;
import com.ptc.core.lwc.common.view.TypeDefinitionReadView;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;

import wt.facade.classification.ClassificationFacade;
import wt.session.SessionHelper;
import wt.util.WTException;

public class GetClassificationDescriptionName {
	private static final String CLASSIFICATION_NAMESPACE = "com.ptc.csm.default_clf_namespace";

	public static String getClassificationInternalNameAndDescription(String classificationInternalName)
			throws WTException {
		String description = "";

		TypeDefinitionReadView readView = TypeDefinitionServiceHelper.service.getTypeDefView(
				AttributeTemplateFlavor.LWCSTRUCT, CLASSIFICATION_NAMESPACE, classificationInternalName);
		Collection<AttributeDefinitionReadView> allAttributes = readView.getAllAttributes();
		boolean isAttributePresent = allAttributes.isEmpty();

		ClassificationFacade facadeInstance = ClassificationFacade.getInstance();
		Object classificationNodeInfo = facadeInstance.getClassificationNodeInfo(classificationInternalName,
				CLASSIFICATION_NAMESPACE);
		EnumerationEntryInfo entryInfo = (EnumerationEntryInfo) classificationNodeInfo;
		description = entryInfo.getLocalizablePropertyValue(EnumerationEntryInfo.DESCRIPTION,
				SessionHelper.getLocale());
		System.out.println("Description : " + description);

		String finalString = description + "#" + isAttributePresent;
		if (description != null) {
			return finalString;
		} else {
			return description;
		}
	}

}