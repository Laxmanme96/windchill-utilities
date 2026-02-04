package ext.enersys.classification.es;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.AttributeDisplayCompositeComponent;
import com.ptc.core.components.rendering.guicomponents.AttributeInputCompositeComponent;
import com.ptc.core.components.rendering.guicomponents.PickerInputComponent;
import com.ptc.core.ui.resources.ComponentMode;

import ext.enersys.service.ESBusinessHelper;
import wt.folder.SubFolder;
import wt.folder.Cabinet;
import wt.inf.container.WTContainerRef;
import wt.part.WTPart;
import wt.util.WTException;

public class GetClassificationDataUtility extends DefaultDataUtility {
	private static final String preferenceValue = "/ext/enersys/CLASSIFICATION_NAMING_AUTOMATION";

	@Override
	public Object getDataValue(String string, Object obj, ModelContext context) throws WTException {
		Object superObject = super.getDataValue(string, obj, context);
		AttributeInputCompositeComponent inputComp = null;
		PickerInputComponent textbox = null;

		if (context.getDescriptorMode().equals(ComponentMode.CREATE)) {
			WTContainerRef containerReference = null;
			Object object = context.getNmCommandBean().getActionOid().getRefObject();
			if (object instanceof Cabinet) {
				Cabinet cabinet = (Cabinet) object;
				containerReference = cabinet.getContainerReference();
			} else if (object instanceof SubFolder) {
				SubFolder subFolder = (SubFolder) object;
				containerReference = subFolder.getContainerReference();
			} else if (object instanceof WTPart) {
				WTPart part = (WTPart) object;
				containerReference = part.getContainerReference();
			}

			boolean featureFlag = ESBusinessHelper.getFrameWorkPreferenceValue(preferenceValue, containerReference);
			if (featureFlag) {
				inputComp = (AttributeInputCompositeComponent) superObject;
				textbox = (PickerInputComponent) inputComp.getValueInputComponent();
				textbox.addJsAction("onchange", "setESClassificationNameFucntion()");
				return textbox;
			}
			return superObject;
		} else if (context.getDescriptorMode().equals(ComponentMode.EDIT)) {
			WTPart part = (WTPart) context.getNmCommandBean().getActionOid().getRefObject();
			WTContainerRef containerReference = part.getContainerReference();
			boolean featureFlag = ESBusinessHelper.getFrameWorkPreferenceValue(preferenceValue, containerReference);
			if (featureFlag) {
				inputComp = (AttributeInputCompositeComponent) superObject;
				textbox = (PickerInputComponent) inputComp.getValueInputComponent();
				textbox.addJsAction("onchange", "setESClassificationNameFucntion()");
				return textbox;
			}
			return superObject;
		} else {
			return superObject;
		}
	}
}
