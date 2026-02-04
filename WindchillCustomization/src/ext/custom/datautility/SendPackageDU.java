package ext.custom.datautility;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.AttributeDisplayCompositeComponent;
import com.ptc.core.components.rendering.guicomponents.AttributeGuiComponent;
import com.ptc.core.ui.resources.ComponentMode;

import wt.part.WTPart;
import wt.util.WTException;

public class SendPackageDU extends DefaultDataUtility {

	@Override
	public Object getDataValue(String paramString, Object paramObject, ModelContext modelContext) throws WTException {
		System.out.println("SendPackageDU DataUtility STARTED");
		Object object = super.getDataValue(paramString, paramObject, modelContext);

		ComponentMode modetype = modelContext.getDescriptorMode();
		// In Create Mode , we are creating StringInputComponent and showing Yes and No
		// values
		if (modetype.equals(ComponentMode.EDIT) || modetype.equals(ComponentMode.VIEW)) {

			Object obj = paramObject;
			System.out.println("--------obj" + obj.getClass());
			if (obj instanceof WTPart) {
				WTPart part = (WTPart) obj;
				System.out.println("Part processing : " + part.getNumber());
				if (object instanceof AttributeDisplayCompositeComponent) {
					AttributeDisplayCompositeComponent component = (AttributeDisplayCompositeComponent) object;
					AttributeGuiComponent displayComponent = component.getValueDisplayComponent();
					String state = part.getState().toString();
					System.out.println("---------Part " + part + " State is " + state);
					if (state.equalsIgnoreCase("RELEASED")) {
						displayComponent.setComponentHidden(true);
						System.out.println(
								"inputComponent Value will return, Attribute will be hidden in Released State : SendPackageDU DataUtility ENDED");
						return displayComponent;
					}
				}

			}
		}
		System.out.println("Default Value will return : SendPackageDU DataUtility ENDED");
		return object;

	}

}
