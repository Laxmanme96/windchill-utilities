package ext.custom.datautility;

import java.util.ArrayList;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.IconComponent;
import com.ptc.core.ui.resources.ComponentMode;


import wt.log4j.LogR;
import wt.util.WTException;

public class AttributeHelpIcon extends DefaultDataUtility {
	protected static final Logger logger = LogR.getLogger(AttributeHelpIcon.class.getName());

	@Override
	public Object getDataValue(String paramString, Object paramObject, ModelContext paramModelContext)
			throws WTException {
//		System.out.println(" AttributeHelpIcon STARTED   ");
		ComponentMode mode = paramModelContext.getDescriptorMode();
		ArrayList<Object> components = new ArrayList<Object>();
		components.add(super.getDataValue(paramString, paramObject, paramModelContext));
		if (mode.equals(ComponentMode.VIEW)) {
//			System.out.println(" AttributeHelpIcon IN VIEW MODE   ");
			IconComponent icon = new IconComponent("netmarkets/images/help_tablebutton.gif");
			icon.setId(paramString);
			icon.setTooltip("Example of icon tooltip(information)");
			components.add(icon);
		}
//		System.out.println(" components Values " + components.toString());
//		System.out.println(" AttributeHelpIcon ENDED   ");
		return components;

	}
}
/*
 * <Service context="default"
 * name="com.ptc.core.components.descriptor.DataUtility" targetFile=
 * "codebase/com/ptc/core/components/components.dataUtilities.properties">
 * <Option cardinality="singleton" order="0" overridable="true"
 * requestor="java.lang.Object" selector="HelpAttribute"
 * serviceClass="ext.custom.datattility.AttributeHelpIcon"/> </Service>
 */