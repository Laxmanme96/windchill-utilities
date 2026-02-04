package ext.enersys.utilities;

import org.apache.logging.log4j.Logger;

import com.ptc.core.businessfield.server.businessObject.BusinessAlgorithm;
import com.ptc.core.businessfield.server.businessObject.BusinessAlgorithmContext;
import com.ptc.core.businessfield.server.businessObject.BusinessObject;
import com.ptc.core.lwc.server.PersistableAdapter;

import wt.change2.WTChangeIssue;
import wt.change2.WTChangeRequest2;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.log4j.LogR;
import wt.util.WTException;

public class EnersysCREstimatedCostBusinessAlgorithm implements BusinessAlgorithm {
	private final static Logger LOGGER = LogR
			.getLoggerInternal(EnersysCREstimatedCostBusinessAlgorithm.class.getName());

	@Override
	public Object execute(BusinessAlgorithmContext arg0, Object[] arg1) {
		LOGGER.debug("Inside EnersysCREstimatedCostBusinessAlgorithm");
		Object attributeValue = "";
		BusinessObject businessObject = arg0.getCurrentBusinessObject();
		try {
			if (businessObject != null && businessObject.getWTReference() != null) {
				Persistable persitableObject = businessObject.getWTReference().getObject();
				LOGGER.debug("per" + persitableObject);
				if (persitableObject instanceof WTChangeRequest2) {
					WTChangeRequest2 changeRequest = (WTChangeRequest2) persitableObject;
					LOGGER.debug("changeRequest=" + changeRequest.getNumber());
					QueryResult qr = wt.change2.ChangeHelper2.service.getChangeIssues(changeRequest);
					LOGGER.debug("QueryResult Size=" + qr.size());
					while (qr.hasMoreElements()) {
						WTChangeIssue probleReport = (WTChangeIssue) qr.nextElement();
						LOGGER.debug("PR Number : " + probleReport.getNumber());
						PersistableAdapter adapter = new PersistableAdapter(probleReport, null, null, null);
						adapter.load("ext.enersys.EstimatedCost"); // Load the attribute into the adapter
						attributeValue = adapter.get("ext.enersys.EstimatedCost");
						LOGGER.debug("attributeValue : " + attributeValue);
					}
				}
			}
		} catch (WTException e) {
			LOGGER.debug(e.getMessage());
			e.printStackTrace();
		}
		return attributeValue;
	}

	@Override
	public Object getSampleValue() {
		return null;
	}

}
