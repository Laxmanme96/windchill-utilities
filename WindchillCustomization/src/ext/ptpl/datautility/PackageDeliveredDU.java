package ext.ptpl.datautility;

import java.util.ArrayList;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.AttributeDataUtilityHelper;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.StringInputComponent;
import com.ptc.core.ui.resources.ComponentMode;
import com.ptc.netmarkets.util.beans.NmCommandBean;


import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.inf.container.WTContainer;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;

public class PackageDeliveredDU extends DefaultDataUtility {
	protected static final Logger logger = LogR.getLogger(PackageDeliveredDU.class.getName());

	@Override
	public Object getDataValue(String componentId, Object datum, ModelContext modelContext) throws WTException {
		Object object = super.getDataValue(componentId, datum, modelContext);
		ComponentMode modetype = modelContext.getDescriptorMode();
		// In Create Mode , we are creating StringInputComponent and showing Yes and No
		// values
		if (modetype.equals(ComponentMode.CREATE)) {

			ArrayList<String> displayList = getContainerPart(modelContext);

			StringInputComponent sic = new StringInputComponent(componentId, displayList, displayList);
			sic.setColumnName(AttributeDataUtilityHelper.getColumnName(componentId, datum, modelContext));
			return sic;

		}
		return object;
	}

	public ArrayList<String> getContainerPart(ModelContext modelContext) throws WTException {

		ArrayList<String> displayList = new ArrayList<String>();
		NmCommandBean nmBean = modelContext.getNmCommandBean();
		WTContainer product = nmBean.getContainer();

		QuerySpec qs = new QuerySpec(WTPart.class);
		qs.appendWhere(new SearchCondition(WTPart.class, "containerReference.key.classname", SearchCondition.EQUAL,
				product.getPersistInfo().getObjectIdentifier().getClassname().toString()), new int[] { 0 });
		qs.appendAnd();
		qs.appendWhere(new SearchCondition(WTPart.class, "containerReference.key.id", SearchCondition.EQUAL,
				product.getPersistInfo().getObjectIdentifier().getId()), new int[] { 0 });
		logger.debug("####### qs : " + qs.toString());
		QueryResult qr = PersistenceHelper.manager.find(qs);
		// logger.debug("####### getagreementAuthorizedObject - qr.size() : " +
		// qr.size());
		while (qr.hasMoreElements()) {
			WTPart part = (WTPart) qr.nextElement();
			logger.debug("####### " + part.getName() + "," + part.getNumber() + ","
					+ part.getVersionIdentifier().getValue() + "." + part.getIterationIdentifier().getValue());

			displayList.add(part.getNumber().toString());
		}

		return displayList;

	}

}