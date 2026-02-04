package ext.ptpl.datautility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.AttributeDataUtilityHelper;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.ComboBox;
import com.ptc.core.components.rendering.renderers.ComboBoxRenderer;
import com.ptc.core.lwc.common.TypeDefinitionService;
import com.ptc.core.lwc.common.view.EnumerationDefinitionReadView;
import com.ptc.core.lwc.common.view.EnumerationMembershipReadView;
import com.ptc.core.lwc.common.view.PropertyHolderHelper;

import wt.log4j.LogR;
import wt.services.ServiceFactory;
import wt.session.SessionHelper;
import wt.util.WTException;

public class Attribute1Attribute2DU extends DefaultDataUtility {

	protected static final Logger logger = LogR.getLogger(Attribute1Attribute2DU.class.getName());

	@Override
	public Object getDataValue(String componentId, Object datum, ModelContext modelContext) throws WTException {
		System.out.println("Attribute1Attribute2DU STARTING...");

		String funcOrg = "ConfigurationType";
		ArrayList<String> internalValues = new ArrayList<String>();
		ArrayList<String> displayValues = new ArrayList<String>();

		Map<String, String> internalDisplayValues = getEnumInternalAndDisplayValues(funcOrg);
		if (!internalDisplayValues.isEmpty()) {

			List<String> internalList = new ArrayList<String>(internalDisplayValues.keySet());
			Collections.sort(internalList);

			List<String> displayList = new ArrayList<String>(internalDisplayValues.values());
			Collections.sort(displayList);

			internalValues.addAll(internalList);
			displayValues.addAll(displayList);
		}
		ComboBox box = new ComboBox();
		box.setRenderer(new ComboBoxRenderer());
		box.setColumnName(AttributeDataUtilityHelper.getColumnName(componentId, datum, modelContext));
		box.setId(componentId);
		box.setInternalValues(internalValues);
		box.setValues(displayValues);
		box.addJsAction("onChange", "onChangeAttribute()");

		return box;
	}

	// Below method is used to read Global enumeration value
	public static Map<String, String> getEnumInternalAndDisplayValues(String enumInternalName) throws WTException {

		Map<String, String> internalDisplayValues = new HashMap<String, String>();
		TypeDefinitionService typeDefService = ServiceFactory.getService(TypeDefinitionService.class);
		EnumerationDefinitionReadView enumDefinition = typeDefService.getEnumDefView(enumInternalName);
		String displayValue = null;
		if (enumDefinition != null) {
			Collection<EnumerationMembershipReadView> entries = enumDefinition.getAllMemberships();
			Iterator<EnumerationMembershipReadView> it = entries.iterator();
			while (it.hasNext()) {
				EnumerationMembershipReadView readview = it.next();
				displayValue = PropertyHolderHelper.getDisplayName(readview.getMember(), SessionHelper.getLocale());
				internalDisplayValues.put(readview.getMember().getName(), displayValue);
			}

		}
		return internalDisplayValues;
	}

}