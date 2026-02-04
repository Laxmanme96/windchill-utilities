package ext.custom.datautility;

import java.util.Locale;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.TextDisplayComponent;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.ui.resources.ComponentMode;

import wt.fc.QueryResult;
import wt.part.WTPart;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

public class DUMultiValue extends DefaultDataUtility {

	@Override
	public Object getDataValue(String str, Object paramObject, ModelContext modelContext) throws WTException {
		System.out.println("Multi DataUtility STARTED");
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
//							System.out.println("IBA Value: " + ibaValue.toString());
							sb.append(ibaValue.toString()).append("\n");
						}
					}
				}
				String formattedValue = sb.toString().trim(); // Ensure no empty or null value
				if (!formattedValue.isEmpty()) {
					TextDisplayComponent gui = new TextDisplayComponent(formattedValue);
					gui.setValue(formattedValue);
					gui.setLabel(formattedValue); // Ensure the label is set correctly
//					System.out.println("Returning formatted object: " + gui);
					return gui;

				} else {
					System.out.println("IBA Value was empty. Returning default object.");

				}
			}
		}
//		System.out.println("Returning default object: " + object);
		return object;
	}
}