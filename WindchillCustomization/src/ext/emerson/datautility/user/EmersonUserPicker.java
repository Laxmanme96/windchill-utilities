package ext.emerson.datautility.user;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.AttributeDataUtilityHelper;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.SuggestTextBox;
import com.ptc.core.components.rendering.guicomponents.TextDisplayComponent;
import com.ptc.core.ui.resources.ComponentMode;

import ext.emerson.properties.CustomProperties;
import wt.doc.LoadDoc;
import wt.iba.value.service.LoadValue;
import wt.util.WTException;

/**
 * Class file for custom user suggestion picker
 *
 */
public class EmersonUserPicker extends DefaultDataUtility {

	private Logger logger = CustomProperties.getlogger("ext.emerson.windchill.datautility");

	/**
	 * OOTB method to override for custom behavior. In view mode,
	 * TextDisplayComponent is generated. In create/edit mode SuggestTextBox is
	 * created.
	 */
	@Override
	public Object getDataValue(String componentId, Object datum, ModelContext mc) throws WTException {
		// TODO Auto-generated method stub
		logger.debug(">>>>>Enter custom getDataValue<<<<<<");
		// NmCommandBean commandBean = mc.getNmCommandBean();
		Object originalValue = mc.getJCAObject().get(componentId);
		if (mc.getDescriptorMode().equals(ComponentMode.VIEW)) {
			logger.debug("View mode of componentId : " + componentId);
//			AttributeDisplayCompositeComponent obj =
//					(AttributeDisplayCompositeComponent) super.getDataValue(componentId,datum, mc);
//			obj.set
//			return super.getDataValue(component_id, datum, modelcontext);

			TextDisplayComponent box = new TextDisplayComponent((String) originalValue);
			try {
				box.setTooltip(getFullNameFromDB((String) originalValue));
			} catch (Exception e) {
				e.printStackTrace();
			}
			box.setValue((String) originalValue);
			// box.setEditable(false);
			return box;
		}
		SuggestTextBox suggestionBox = new SuggestTextBox(componentId, "emerson_autosuggest_helper");

		suggestionBox.setColumnName(AttributeDataUtilityHelper.getColumnName(componentId, datum, mc));
		suggestionBox.setId(componentId);
		logger.debug("Create/Edit mode of componentId : " + componentId + " value: " + originalValue);
		suggestionBox.setValue(originalValue == null ? "" : (String) originalValue);
		suggestionBox.setEnabled(true);
		suggestionBox.setEditable(true);
		suggestionBox.setRenderLabelOnRight(true);
		suggestionBox.setMinChars(1);
		suggestionBox.setMaxResults(50);
		suggestionBox.setMaxLength(8);

		return suggestionBox;
	}

	private String getFullNameFromDB(String searchTerm) throws Exception {

		UserRecordHelper helper = new UserRecordHelper(searchTerm);
		StringBuffer buf = new StringBuffer();
		try {
			List<UserRecord> userRecords = helper.searchDataContainedInAbbrevColumn();
			if (userRecords.isEmpty()) {
				return "";

			} else {
				for (UserRecord record : userRecords) {
					buf.append(record.getDisplayName() + "\n");
				}
			}
		} finally {
			// destroy the data source which should close underlying connections
			if (helper.connectionSource != null) {
				helper.connectionSource.close();
			}
		}
		logger.debug("Results found for User picker display name");
		return buf.toString();

	}

}
