package ext.custom.picker;

/*
 * bcwti
 *
 *  Copyright (c) 2016 Parametric Technology Corporation (PTC). All Rights
 *  Reserved.
 *
 *  This software is the confidential and proprietary information of PTC.
 *  You shall not disclose such confidential information and shall use it
 *  only in accordance with the terms of the license agreement.
 *
 *  
 */

import java.util.HashMap;
import java.util.List;

import com.ptc.core.components.beans.CreateAndEditWizBean;
import com.ptc.core.components.descriptor.LogicSeparatedDataUtility;
import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.AttributeDataUtilityHelper;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.factory.dataUtilities.DefaultPickerDataUtility;
import com.ptc.core.components.rendering.guicomponents.PickerInputComponent;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.ui.resources.ComponentMode;

import ext.custom.formProcessor.CustomUserFormProcessorDelegate;
import wt.fc.Persistable;
import wt.util.WTException;

/**
 * This class implements a custom User picker for an attribute.
 *
 * @author https://support.ptc.com/appserver/cs/view/solution.jsp?n=CS230207
 * @since 1.2
 */
public class CustomUserPickerDataUtility extends DefaultPickerDataUtility implements LogicSeparatedDataUtility {

	ComponentMode mode = ComponentMode.CREATE;

	@Override
	public void setModelData(String component_id, List<?> objects, ModelContext mc) throws WTException {
		mode = mc.getDescriptorMode();
		super.setModelData(component_id, objects, mc);
		if (ComponentMode.CREATE.equals(mode) || ComponentMode.EDIT.equals(mode)) {
			buildUserPickerConfig(component_id, mc);
		}
	}

	public void buildUserPickerConfig(final String attributeID, final ModelContext mc) throws WTException {
		HashMap<String, Object> defaultProps = new HashMap<>();
		defaultProps.put("objectType", "wt.org.WTUser");
		defaultProps.put("pickedAttributes", "fullName");
		
		defaultProps.put("multiSelect", true);
		defaultProps.put("showUserType", "ActiveOnly");
		defaultProps.put("readOnlyPickerTextBox", "false");
		defaultProps.put("showSuggestion", "false");
		// defaultProps.put("suggestServiceKey", "DocUserPickerId");
		String label = "CUSTOM USER PICKER PAGE TITLE";
		defaultProps.put("label", label);
		defaultProps.put("pickerTitle", label);
		defaultProps.put("multiSelect", "true");

		HashMap<String, Object> customProps = new HashMap<>();
		if ("PackageCreator".equalsIgnoreCase(attributeID)) {
			customProps.put("pickerId", attributeID);
			customProps.put("pickerCallback", "CustomPickerInputComponentCallback");
		}
		defaultProps.putAll(customProps);
		mc.getDescriptor().setProperties(defaultProps);
	}

	@Override
	public Object getDataValue(String attributeID, Object object, ModelContext mc) throws WTException {
		Object obj = null;
		if (ComponentMode.CREATE.equals(mode) || ComponentMode.EDIT.equals(mode)) {
			obj = super.getDataValue(attributeID, object, mc);
			if (obj instanceof PickerInputComponent) {
				PickerInputComponent pickerComponent = (PickerInputComponent) obj;
				if(ComponentMode.EDIT.equals(mode))
				{
					pickerComponent.setColumnName(AttributeDataUtilityHelper.getColumnName(attributeID, obj, mc));
					pickerComponent.setValue(pickerComponent.getValue());
				}
				pickerComponent.addHiddenField(CreateAndEditWizBean.FORM_PROCESSOR_DELEGATE,
						CustomUserFormProcessorDelegate.class.getCanonicalName()); 
				return pickerComponent;			
			}
		} else {
			obj = new DefaultDataUtility().getDataValue(attributeID, object, mc);
		}
		return obj;
	}

	@Override
	public Object getPlainDataValue(String s, Object o, ModelContext modelContext) throws WTException {
		if (o instanceof Persistable) {
			return getAttributeValue((Persistable) o, s);
		} else {
			return "";
		}
	}
	public static Object getAttributeValue(Persistable persistable, String attributeName) {
		Object attValue = null;
		try {
			PersistableAdapter ibaHolder = new PersistableAdapter(persistable, null, null,
					new DisplayOperationIdentifier());
			ibaHolder.load(attributeName);
			attValue = ibaHolder.get(attributeName);
		} catch (WTException e) {
			System.out.println("ERROR getting " + attributeName + " attribute value " + e.toString());
		}

		return attValue;
	}

}

