package ext.ptpl.datautility;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.CheckBox;
import com.ptc.core.ui.resources.ComponentMode;

import wt.inf.container.WTContainer;
import wt.log4j.LogR;
import wt.util.WTException;

public class GeneratePartCheckBox extends DefaultDataUtility {
	
	protected static final Logger logger = LogR.getLogger(GeneratePartCheckBox.class.getName());

	@Override
	public Object getDataValue(String component_id, Object object, ModelContext modelContext) throws WTException {
		CheckBox checkBox = new CheckBox();

		checkBox.setName("generatePartNumberCheckBox");
		checkBox.setId("generatePartNumberCheckBox");
		if (modelContext.getDescriptorMode().equals(ComponentMode.CREATE)) {
			WTContainer container = modelContext.getNmCommandBean().getContainer();
				checkBox.setEnabled(true);
				checkBox.getInternalValue();
				logger.debug("###### Check Box Created For Product :" + container.getName());
		} else {
			checkBox.setEditable(false);
		}
		return checkBox;
	}
}