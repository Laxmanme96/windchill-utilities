package ext.custom.datautility;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.IconComponent;

import wt.part.WTPart;
import wt.util.WTException;

public class LifecycleIconDataUtility extends DefaultDataUtility {

	@Override
	public Object getDataValue(String attribute, Object datum, ModelContext ModCon) throws WTException {
		System.out.println(" LifecycleIconDataUtility STARTED   ");
		String imgURL = null;
		if (datum instanceof WTPart) { // handle object type carefully if you
										// register using internal name.
			WTPart part = (WTPart) datum;

//			String subtype = part.getDisplayType().getLocalizedMessage(null);
//			System.out.println("Subtype in LifecycleIconDataUtility " + subtype);
//
//			if (subtype.equals("com.pluraltech.windchill.Custom_Part")) {

			String state = part.getState().getState().toString();

			if (state.equals("RELEASED")) {
				imgURL = "netmarkets/images/green.gif";
			} else if (state.equals("INWORK")) {
				imgURL = "netmarkets/images/yellow.gif";
			} else {
				imgURL = "netmarkets/images/red.gif";
			}
		}
		// }
		// else if(datum instanceof WTDocument) {

		// }
		IconComponent iconComponent = new IconComponent(imgURL);
		return iconComponent;
	}

}

/*
 * XCONF Entry
 * 
 * <Service name="com.ptc.core.components.descriptor.DataUtility" targetFile=
 * "codebase/com/ptc/core/components/components.dataUtilities.properties">
 * 
 * <Option serviceClass="ext.custom.datautility.LifecycleIconDataUtility"
 * requestor="java.lang.Object" selector="lifecycleIconDataUtility"
 * cardinality="singleton"/> </Service>
 * 
 */
