package ext.custom.picker;


import java.util.Map;

import com.ptc.core.components.descriptor.ComponentDescriptor;
import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.AbstractDataUtility;
import com.ptc.core.components.factory.dataUtilities.AttributeDataUtilityHelper;
import com.ptc.core.components.rendering.PickerRenderConfigs;
import com.ptc.core.components.rendering.guicomponents.PickerInputComponent;
import com.ptc.core.ui.resources.ComponentMode;
import wt.util.WTException;

public class CatalogPartPickerDataUtility extends AbstractDataUtility {

	public Object getDataValue(String componentId, Object datum, ModelContext mc) throws WTException {
		ComponentMode mode = mc.getParentDescriptor().getDescriptorMode();
		String value = getAttributeValue(componentId, mc);
		if (ComponentMode.CREATE.equals(mode) || ComponentMode.EDIT.equals(mode)) {
			final int columnSize = 25;
			final ComponentDescriptor componentdescriptor = mc.getDescriptor();
			final Map<Object, Object> map = componentdescriptor.getProperties();
			final String attLabelName = getLabel(componentId, mc);
			PickerRenderConfigs.setDefaultPickerProperty(map, PickerRenderConfigs.PICKER_ID, componentId);
			PickerRenderConfigs.setDefaultPickerProperty(map, PickerRenderConfigs.OBJECT_TYPE,"WCTYPE|wt.part.WTPart|com.pluraltech.windchill.MECHANICAL_PART");
			PickerRenderConfigs.setDefaultPickerProperty(map, PickerRenderConfigs.PICKER_TITLE, "Find Catalog Part");
			PickerRenderConfigs.setDefaultPickerProperty(map, PickerRenderConfigs.COMPONENT_ID,componentId);
			PickerRenderConfigs.setDefaultPickerProperty(map, PickerRenderConfigs.VERSION, "LATEST");
			PickerRenderConfigs.setDefaultPickerProperty(map, "showTypePicker", "false");
			PickerRenderConfigs.setDefaultPickerProperty(map, "defaultHiddenValue", (String) value);
			PickerRenderConfigs.setDefaultPickerProperty(map, "pickedAttributes", "number");
			PickerRenderConfigs.setDefaultPickerProperty(map, "displayAttribute", "number");
			PickerRenderConfigs.setDefaultPickerProperty(map, PickerRenderConfigs.SHOW_CLEAR_ACTION, "true");
			PickerRenderConfigs.setDefaultPickerProperty(map, PickerRenderConfigs.PICKER_CALLBACK,
					"csmPartPickerCallback");
			final PickerInputComponent pickerinputcomponent = new PickerInputComponent(attLabelName, (String) value,
					PickerRenderConfigs.getPickerConfigs(map), columnSize);
			String columnName = AttributeDataUtilityHelper.getColumnName(componentId, datum, mc);
			pickerinputcomponent.setColumnName(columnName);
			pickerinputcomponent.setId(componentId);
			pickerinputcomponent.setName(componentId);
			pickerinputcomponent.setRequired(false);
			if (mode == ComponentMode.EDIT && value != null) {
				pickerinputcomponent.setValue(value);
			}
			return pickerinputcomponent;
		}
		return value;
	}

	private String getAttributeValue(String component_id, ModelContext mc) throws WTException {
		String result = "";

		Object rawVal = mc.getRawValue();
		if (rawVal != null) {
			result = (String) rawVal;
		}

		return result;
	}

}

