package ext.custom.datautility;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.AttributeDisplayCompositeComponent;
import com.ptc.core.components.rendering.guicomponents.AttributeGuiComponent;
import com.ptc.core.components.rendering.guicomponents.AttributeInputComponent;
import com.ptc.core.components.rendering.guicomponents.AttributeInputCompositeComponent;
import com.ptc.core.meta.server.TypeIdentifierUtility;
import com.ptc.core.ui.resources.ComponentMode;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.enterprise.team.server.TeamCCHelper;

import wt.inf.container.WTContainer;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.util.WTException;

/**
 * DataUtility to Hide the Attribute Attribute ReleasedValue is visible on
 * Create and Edit layout of only one type (BreakDown Element part in any
 * container. Also it is visible to other parts only in Standard Library
 * Container. This attribute should be visible in UI on Released state . On Edit
 * Page , This attribute is Non editable.
 * 
 */
public class AttributeHideDataUtility extends DefaultDataUtility {

	/** The Constant CLASSNAME. */
	private static final String CLASSNAME = AttributeHideDataUtility.class.getName();

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogR.getLogger(CLASSNAME);
	boolean flag = false;

	@Override
	public Object getDataValue(String paramString, Object paramObject, ModelContext modelContext) throws WTException {
		return hideAttributeIfNeeded(paramString, paramObject, modelContext,
				"wt.part.WTPart|microwave.net.Breakdown_Element");
	}

	public Object hideAttributeIfNeeded(String paramString, Object paramObject, ModelContext modelContext,
			String strType) throws WTException {

		System.out.println("Enter >> AttributeHidingDataUtility");
		Object guiComp = super.getDataValue(paramString, paramObject, modelContext);
		ComponentMode mode = modelContext.getDescriptorMode();
		// To know objects details and container details during creation we will use
		// nmcommandbean
		NmCommandBean commandBean = modelContext.getNmCommandBean();
		WTContainer Container = TeamCCHelper.getContainerFromObject(commandBean);
		String Contname = Container.getName();
		System.out.println("commandBean.getComboBox()--------------" + commandBean.getComboBox());
		// we are fetching selected Part Type During Creation by using commandbean
		// comboBox
		Map<String, ArrayList<String>> nmCommandBeanBox = commandBean.getComboBox();
		String selectedPartType = nmCommandBeanBox
				.getOrDefault("!~objectHandle~partHandle~!createType", new ArrayList<>()).stream().findFirst()
				.orElse("");

		System.out.println("selectedPart===========" + selectedPartType);

		// Extract PartType for Edit . during creation obj will come as default Cabinet
		// instance
		NmOid oid = commandBean.getPrimaryOid();
		Object obj = oid.getRefObject();

		if (obj != null && obj instanceof WTPart) {// Object is available
			String thePartSoftType = TypeIdentifierUtility.getTypeIdentifier(obj).getTypename();
			System.out.println("part soft type " + thePartSoftType);
			selectedPartType = thePartSoftType;
		}
		// UI Mode for both Edit & Create
		if ((mode.equals(ComponentMode.EDIT) || mode.equals(ComponentMode.CREATE))) {
			System.out.println("Entering Inside Logic---Create/ Edit mode ");
			if (guiComp instanceof AttributeInputCompositeComponent) {
				AttributeInputCompositeComponent component = (AttributeInputCompositeComponent) guiComp;
				AttributeInputComponent inputComponent = component.getValueInputComponent();
				if ((selectedPartType != null) && (!selectedPartType.toString().isEmpty())) {
					// If selected Part Type is not empty and Selected part type is breakdown
					// element or conatiner is Standard Library
					if ((selectedPartType.toString().equalsIgnoreCase(strType))
							|| Contname.equalsIgnoreCase("Standard Library")) {
						if (mode.equals(ComponentMode.CREATE)) {
							// Hide this attribute for Other Parts
							return inputComponent;
						} else {
							inputComponent.setEditable(false);
							return inputComponent;
						}
					} else {
						inputComponent.setComponentHidden(true);
						return inputComponent;
					}

				} // Check Selected Part
			} // Check Attribute Composite Component
		} // Check Create & Edit Mode
			// UI Mode when VIEW
		if (mode.equals(ComponentMode.VIEW)) {
			System.out.println("Entering Inside Logic---View mode");
			if (guiComp instanceof AttributeDisplayCompositeComponent) {
				AttributeDisplayCompositeComponent component = (AttributeDisplayCompositeComponent) guiComp;
				AttributeGuiComponent displaycomponent = component.getValueDisplayComponent();
				// If selected Part Type is not empty and Selected part type is breakdown
				// element or conatiner is Standard Library
				if ((selectedPartType != null) && (!selectedPartType.toString().isEmpty())) {
					if ((selectedPartType.toString().equalsIgnoreCase(strType))
							|| Contname.equalsIgnoreCase("Standard Library")) {
						if (obj instanceof WTPart) {
							WTPart part = (WTPart) obj;
							String LCState = part.getState().toString();
							if (!LCState.equalsIgnoreCase("RELEASED")) {
								displaycomponent.setComponentHidden(true);
								return displaycomponent;
							} else {
								return displaycomponent;
							}
						}
					} else {
						displaycomponent.setComponentHidden(true);
						return displaycomponent;
					}
				} // Check Selected Part
			} // Check Attribute Composite Component
		}
		System.out.println("End >> AttributeHidingDataUtility");
		return guiComp;
	}
}

/*
 * <Service name="com.ptc.core.components.descriptor.DataUtility" targetFile=
 * "codebase/com/ptc/core/components/components.dataUtilities.properties">
 * <Option serviceClass="ext.custom.datautility.AttributeHideDataUtility"
 * requestor="java.lang.Object" selector="	"
 * cardinality="duplicate"/> </Service>
 */