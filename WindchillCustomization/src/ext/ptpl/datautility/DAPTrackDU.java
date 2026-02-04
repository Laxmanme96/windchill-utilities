package ext.ptpl.datautility;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.ui.resources.ComponentMode;

import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.fc.QueryResult;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

public class DAPTrackDU extends DefaultDataUtility {
	protected static final Logger logger = LogR.getLogger(DAPTrackDU.class.getName());

	@Override
	public Object getDataValue(String str, Object paramObject, ModelContext modelContext) throws WTException {
		System.out.println("DAPTrackDU DataUtility STARTED");
		ComponentMode mode = modelContext.getDescriptorMode();
		Object object = super.getDataValue(str, paramObject, modelContext);
		if (mode.equals(ComponentMode.VIEW)) {

			if (paramObject instanceof WTDocument) {
				//WTDocument doc = (WTDocument) paramObject;
				//WTDocumentMaster pm = (WTDocumentMaster) doc.getMaster();
				//WTDocument lastestPart = (WTDocument) VersionControlHelper.service.allIterationsOf(pm).nextElement();
				//System.out.println("DAPTrackDU DataUtility is on Document");

			}
		}
		return object;
	}
}