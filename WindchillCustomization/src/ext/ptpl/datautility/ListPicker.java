package ext.ptpl.datautility;


import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.AttributeDataUtilityHelper;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.SuggestTextBox;
import com.ptc.core.ui.resources.ComponentMode;

import ext.emerson.properties.CustomProperties;
import wt.util.WTException;

/**
 * Class file for custom user suggestion picker Updated to fetch values from an
 * Excel file instead of a database.
 */
public class ListPicker extends DefaultDataUtility {

	private Logger logger = CustomProperties.getlogger("ext.emerson.windchill.datautility");

	/**
	 * OOTB method to override for custom behavior. In view mode,
	 * TextDisplayComponent is generated. In create/edit mode, SuggestTextBox is
	 * created.
	 */
	@Override
	public Object getDataValue(String componentId, Object datum, ModelContext mc) throws WTException {
		System.out.println("----------ListPicker getDataValue Started---------------------");

		Object originalValue = mc.getJCAObject().get(componentId);
		SuggestTextBox suggestionBox = new SuggestTextBox(componentId, "emerson_autosuggest_helper");
		if ((mc.getDescriptorMode().equals(ComponentMode.CREATE))
				|| (mc.getDescriptorMode().equals(ComponentMode.EDIT))) {

		// SuggestTextBox for Create/Edit mode
		suggestionBox.setColumnName(AttributeDataUtilityHelper.getColumnName(componentId, datum, mc));
		suggestionBox.setId(componentId);
		logger.debug("Create/Edit mode of componentId : " + componentId + " value: " + originalValue);
		suggestionBox.setValue(originalValue == null ? "" : (String) originalValue);
		suggestionBox.setEnabled(true);
		suggestionBox.setEditable(true);
		suggestionBox.setRenderLabelOnRight(true);
		suggestionBox.setMinChars(3);
		suggestionBox.setMaxResults(50);
		suggestionBox.setMaxLength(8);

	}
		return suggestionBox;
	}

}
