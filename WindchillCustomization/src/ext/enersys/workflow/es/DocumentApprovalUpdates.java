package ext.enersys.workflow.es;

import org.apache.logging.log4j.Logger;

import ext.enersys.cm2.CM2Helper;
import ext.enersys.service.ESBusinessHelper;
import wt.change2.WTChangeReview;
import wt.doc.WTDocument;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.fc.collections.WTHashSet;
import wt.log4j.LogR;
import wt.vc.VersionControlHelper;

public class DocumentApprovalUpdates {
	private static final String CLASSNAME = DocumentApprovalUpdates.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(CLASSNAME);
	public static final String DAP_TRACK = "com.Windchill.Demo.dapTrack";
	public static final String A_B_APPROVED = "A_B_APPROVED";
	public static final String C_PRODUCTION_APPROVED = "C_PRODUCTION_APPROVED";

	public static void updateDocApprovalAttribute(WTObject primaryBusinessObject) {
		try {
			if (primaryBusinessObject instanceof WTChangeReview) {
				WTChangeReview pbo = (WTChangeReview) primaryBusinessObject;
				String dapTrack = (String) ESBusinessHelper.getIBAValue(pbo, DAP_TRACK);
				LOGGER.debug("DAP Track: " + dapTrack);
				WTHashSet reviewObjects = CM2Helper.service.getAllAffectedObjects(pbo);
				java.util.Iterator reviewObjectsIter = reviewObjects.iterator();
				while (reviewObjectsIter.hasNext()) {
					Persistable per = ((WTReference) reviewObjectsIter.next()).getObject();
					String valueToSet = "";
					if (per instanceof WTDocument) {
						WTDocument doc = (WTDocument) per;
						LOGGER.debug("Doc in process: " + doc.getNumber() + " " + doc.getName() + " "
								+ VersionControlHelper.getVersionDisplayIdentifier(doc));
						if (dapTrack.equals(A_B_APPROVED))
							valueToSet = "A/B Approved";
						else if (dapTrack.equals(C_PRODUCTION_APPROVED))
							valueToSet = "C/Production Approved";
					} else if (per instanceof EPMDocument) {
						EPMDocument epmDoc = (EPMDocument) per;
						LOGGER.debug("Doc in process: " + epmDoc.getNumber() + " " + epmDoc.getName() + " "
								+ VersionControlHelper.getVersionDisplayIdentifier(epmDoc));
						if (dapTrack.equals(A_B_APPROVED))
							valueToSet = "A/B Approved";
						else if (dapTrack.equals(C_PRODUCTION_APPROVED))
							valueToSet = "C/Production Approved";
					}
					LOGGER.debug("Setting Document Approval Status as: " + valueToSet);
					ESBusinessHelper.updateIBAWithoutIteration(per, "ext.enersys.IS_DOC_APPROVED", valueToSet);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
