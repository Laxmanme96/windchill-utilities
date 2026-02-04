package ext.ptpl.datautility;

import java.util.Locale;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.TextDisplayComponent;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.ui.resources.ComponentMode;

import ext.emerson.validators.APPLEditAttributeValidator;
import wt.fc.QueryResult;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

public class DUMultiValue extends DefaultDataUtility {
	protected static final Logger logger = LogR.getLogger(DUMultiValue.class.getName());

	@Override
	public Object getDataValue(String str, Object paramObject, ModelContext modelContext) throws WTException {
		logger.debug("Multi DataUtility STARTED");
		ComponentMode mode = modelContext.getDescriptorMode();
		Object object = super.getDataValue(str, paramObject, modelContext);
		if (mode.equals(ComponentMode.VIEW)) {
			if (paramObject instanceof WTPart) {
				WTPart part = (WTPart) paramObject;
				QueryResult qr = VersionControlHelper.service.allVersionsOf(part);
				// Use StringBuilder for better performance
				StringBuilder sb = new StringBuilder();

				while (qr.hasMoreElements()) {
					WTPart partVersion = (WTPart) qr.nextElement();
					PersistableAdapter pers = new PersistableAdapter(partVersion, null, Locale.US,
							new DisplayOperationIdentifier());
					pers.load("MultiValue");
					Object ibaValueObject = pers.get("MultiValue"); // IBA name
					if (ibaValueObject instanceof Object[]) {
						Object[] ibaValues = (Object[]) ibaValueObject;
						for (Object ibaValue : ibaValues) {
							logger.debug("IBA Value: " + ibaValue.toString());
							sb.append(ibaValue.toString()).append("\n");
						}
					}
				}
				String formattedValue = sb.toString().trim(); // Ensure no empty or null value
				if (!formattedValue.isEmpty()) {
					TextDisplayComponent gui = new TextDisplayComponent(formattedValue);
					gui.setValue(formattedValue);
					gui.setLabel(formattedValue); // Ensure the label is set correctly
					logger.debug("Returning formatted object: " + gui);
					return gui;

				} else {
					logger.debug("IBA Value was empty. Returning default object.");

				}
			}
		}
		logger.debug("Returning default object: " + object);
		return object;
	}
}