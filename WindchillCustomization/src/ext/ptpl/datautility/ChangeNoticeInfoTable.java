package ext.ptpl.datautility;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.ui.resources.ComponentMode;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.log4j.LogR;
import wt.part.WTPart;
import wt.util.WTException;

public class ChangeNoticeInfoTable extends DefaultDataUtility {
	protected static final Logger logger = LogR.getLogger(GeneratePartCheckBox.class.getName());

	@Override
	public Object getDataValue(String component_id, Object object, ModelContext modelContext) throws WTException {
		System.out.println("----------------ChangeNoticeInfoTable Started----------------- ");
		if (modelContext.getDescriptorMode().equals(ComponentMode.VIEW)) {

			NmCommandBean nm = modelContext.getNmCommandBean();
			NmOid oid = nm.getPrimaryOid();
			Object refObj = oid.getRefObject();
			System.out.println("Class of Ref Object: " + refObj);

			if (refObj instanceof WTPart) {

			}
		}

		return null;

	}

}
