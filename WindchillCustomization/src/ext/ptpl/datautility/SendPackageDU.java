package ext.ptpl.datautility;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.AttributeDisplayCompositeComponent;
import com.ptc.core.components.rendering.guicomponents.AttributeGuiComponent;
import com.ptc.core.ui.resources.ComponentMode;
import wt.log4j.LogR;

import wt.part.WTPart;
import wt.util.WTException;

public class SendPackageDU extends DefaultDataUtility {
	protected static final Logger logger = LogR.getLogger(SendPackageDU.class.getName());

	@Override
	public Object getDataValue(String paramString, Object paramObject, ModelContext modelContext) throws WTException {
		logger.debug("SendPackageDU DataUtility STARTED");
		Object object = super.getDataValue(paramString, paramObject, modelContext);

		ComponentMode modetype = modelContext.getDescriptorMode();
		// In Create Mode , we are creating StringInputComponent and showing Yes and No
		// values
		if (modetype.equals(ComponentMode.EDIT) || modetype.equals(ComponentMode.VIEW)) {

			Object obj = paramObject;
			logger.debug("--------obj" + obj.getClass());
			if (obj instanceof WTPart) {
				WTPart part = (WTPart) obj;
				logger.debug("Part processing : " + part.getNumber());
				if (object instanceof AttributeDisplayCompositeComponent) {
					AttributeDisplayCompositeComponent component = (AttributeDisplayCompositeComponent) object;
					AttributeGuiComponent displayComponent = component.getValueDisplayComponent();
					String state = part.getState().toString();
					logger.debug("---------Part " + part + " State is " + state);
					if (state.equalsIgnoreCase("RELEASED")) {
						displayComponent.setComponentHidden(true);
						logger.debug(
								"inputComponent Value will return, Attribute will be hidden in Released State : SendPackageDU DataUtility ENDED");
						return displayComponent;
					}
				}

			}
		}
		logger.debug("Default Value will return : SendPackageDU DataUtility ENDED");
		return object;

	}

}
