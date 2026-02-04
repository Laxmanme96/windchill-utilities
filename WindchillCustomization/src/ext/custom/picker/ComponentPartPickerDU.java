package ext.custom.picker;


import java.util.Map;

import com.ptc.core.components.descriptor.ComponentDescriptor;
import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.AttributeDataUtilityHelper;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.PickerRenderConfigs;
import com.ptc.core.components.rendering.guicomponents.AttributeDisplayCompositeComponent;
import com.ptc.core.components.rendering.guicomponents.PickerInputComponent;
import com.ptc.core.components.rendering.guicomponents.TextDisplayComponent;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.ui.resources.ComponentMode;

import wt.fc.Persistable;
import wt.fc.ReferenceFactory;
import wt.fc.WTReference;
import wt.part.WTPart;
import wt.util.WTException;

public class ComponentPartPickerDU extends DefaultDataUtility { 
	

	public Object getDataValue(String paramString, Object paramObject,ModelContext paramModelContext) throws WTException 
	{
		Object obj=null;
		 String TYPE_COMPONENT_PART_FULL_NAME = "WCTYPE|wt.part.WTPart|com.pluraltech.windchill.MECHANICAL_PART";
		ComponentMode mod=paramModelContext.getDescriptorMode();
		System.out.println("-----Inside ComponentPartPicker class-----------");
		String value = getAttributeValue(paramString, paramModelContext);
		if(ComponentMode.CREATE.equals(mod) || ComponentMode.EDIT.equals(mod))
		{
			final int columnSize = 25;
			//System.out.println("Inside if statement");
			ComponentDescriptor componentdescriptor = paramModelContext.getDescriptor();
			Map<Object, Object> map = componentdescriptor.getProperties();
			
			String attLabelName = getLabel(paramString, paramModelContext);
			PickerRenderConfigs.setDefaultPickerProperty(map,PickerRenderConfigs.PICKER_ID, "wtpartPicker");
			PickerRenderConfigs.setDefaultPickerProperty(map,PickerRenderConfigs.OBJECT_TYPE, TYPE_COMPONENT_PART_FULL_NAME);
			/* search target object */
			PickerRenderConfigs.setDefaultPickerProperty(map,PickerRenderConfigs.PICKER_TITLE, "Search Existing Part");
			PickerRenderConfigs.setDefaultPickerProperty(map,PickerRenderConfigs.READ_ONLY_TEXTBOX, "true");
			PickerRenderConfigs.setDefaultPickerProperty(map, "showTypePicker","false");
			PickerRenderConfigs.setDefaultPickerProperty(map,PickerRenderConfigs.COMPONENT_ID, "partPicker");
			PickerRenderConfigs.setDefaultPickerProperty(map, PickerRenderConfigs.VERSION, "LATEST");
			/* picker search criteria configuration */
			PickerRenderConfigs.setDefaultPickerProperty(map, "showTypePicker", "false");
			PickerRenderConfigs.setDefaultPickerProperty(map, "defaultHiddenValue", (String) value);
			PickerRenderConfigs.setDefaultPickerProperty(map, "pickedAttributes", "number");
			PickerRenderConfigs.setDefaultPickerProperty(map, "displayAttribute", "number");
			PickerRenderConfigs.setDefaultPickerProperty(map, PickerRenderConfigs.SHOW_CLEAR_ACTION, "true");
			/* Picker return value to input field */
			final PickerInputComponent pickerinputcomponent = new PickerInputComponent(attLabelName, (String) value,
					PickerRenderConfigs.getPickerConfigs(map), columnSize);
			String columnName = AttributeDataUtilityHelper.getColumnName(paramString, paramObject, paramModelContext);
			pickerinputcomponent.setColumnName(columnName);
			pickerinputcomponent.setId(paramString);
			pickerinputcomponent.setName(paramString);
			pickerinputcomponent.setRequired(AttributeDataUtilityHelper.isInputRequired(paramModelContext));
			if (paramModelContext.getDescriptorMode() == ComponentMode.EDIT && value != null) {
				pickerinputcomponent.setValue(value);
			}
			return pickerinputcomponent;
		}
		if(paramModelContext.getDescriptorMode().equals(ComponentMode.VIEW)){
			System.out.println("Inside view");
			AttributeDisplayCompositeComponent displaycomp=(AttributeDisplayCompositeComponent) super.getDataValue(paramString, paramObject, paramModelContext);
			TextDisplayComponent textdisplaycom=(TextDisplayComponent) displaycomp.getValueDisplayComponent();
			Object rawValue=getIBAValue((Persistable) paramObject, paramString);
			ReferenceFactory rf = new ReferenceFactory ();		
			WTReference reference= (WTReference)rf.getReference(rawValue.toString());
			Persistable pers=(Persistable)reference.getObject();
			WTPart persistable = (WTPart) (pers);

			obj = persistable.getNumber();
			System.out.println("ibavalue :: "+obj);
			textdisplaycom.setValue((String) obj);
			return obj;
			
		}
		System.out.println("returning-----"+value);
		return value;
	}
	
	public static Object getIBAValue(Persistable targetObj, String ibaName) throws WTException {
		Object ibaValue = null;
		try {
			System.out.println("inside getIBAValue");
			PersistableAdapter obj = new PersistableAdapter(targetObj, null, null, null);
			System.out.println("inside getIBAValue1");
			obj.load(ibaName);
			System.out.println("inside getIBAValue2");
			ibaValue = obj.get(ibaName);
			System.out.println("inside getIBAValue3");
		} catch (WTException e) {
			e.printStackTrace();	
			throw new WTException(e);
		}
		return ibaValue;
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

