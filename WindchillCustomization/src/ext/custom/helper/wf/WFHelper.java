package ext.custom.helper.wf;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.OperationIdentifier;
import com.ptc.core.meta.common.OperationIdentifierConstants;

import ext.enersys.cm2.CM2Helper;
import ext.enersys.service.ESBusinessHelper;
import wt.change2.WTChangeReview;
import wt.doc.WTDocument;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.PersistenceServerHelper;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.fc.collections.WTHashSet;
import wt.iba.value.AttributeContainer;
import wt.iba.value.IBAHolder;
import wt.iba.value.service.IBAValueDBService;
import wt.part.WTPart;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

public class WFHelper {

	//private static final String CLASSNAME = DocumentApprovalUpdates.class.getName();
	//private final static Logger LOGGER = LogR.getLoggerInternal(CLASSNAME);
	public static final String DAP_TRACK = "com.Windchill.Demo.dapTrack";
	public static final String A_B_APPROVED = "A_B_APPROVED";
	public static final String C_PRODUCTION_APPROVED = "C_PRODUCTION_APPROVED";
	public static StringBuilder failedSB = new StringBuilder();

	public static void updateDocApprovalAttribute(WTObject primaryBusinessObject) {
		try {
			if (primaryBusinessObject instanceof WTChangeReview) {
				WTChangeReview pbo = (WTChangeReview) primaryBusinessObject;
				String dapTrack = (String) ESBusinessHelper.getIBAValue(pbo, DAP_TRACK);
				System.out.println("DAP Track: " + dapTrack);
				WTHashSet reviewObjects = CM2Helper.service.getAllAffectedObjects(pbo);
				java.util.Iterator reviewObjectsIter = reviewObjects.iterator();
				while (reviewObjectsIter.hasNext()) {
					Persistable per = ((WTReference) reviewObjectsIter.next()).getObject();
					String valueToSet = "";
					if (per instanceof WTDocument) {
						WTDocument doc = (WTDocument) per;
						System.out.println("Doc in process: " + doc.getNumber() + " " + doc.getName() + " "
								+ VersionControlHelper.getVersionDisplayIdentifier(doc));
						if (dapTrack.equals(A_B_APPROVED))
							valueToSet = "A/B Approved";
						else if (dapTrack.equals(C_PRODUCTION_APPROVED))
							valueToSet = "C/Production Approved";
					} else if (per instanceof WTPart) {
						WTPart part = (WTPart) per;
						System.out.println("Part in process: " + part.getNumber() + " " + part.getName() + " "
								+ VersionControlHelper.getVersionDisplayIdentifier(part));
						if (dapTrack.equals(A_B_APPROVED))
							valueToSet = "A/B Approved";
						else if (dapTrack.equals(C_PRODUCTION_APPROVED))
							valueToSet = "C/Production Approved";
				} 
					
					else if (per instanceof EPMDocument) {
						EPMDocument epmDoc = (EPMDocument) per;
						System.out.println("Doc in process: " + epmDoc.getNumber() + " " + epmDoc.getName() + " "
								+ VersionControlHelper.getVersionDisplayIdentifier(epmDoc));
						if (dapTrack.equals(A_B_APPROVED))
							valueToSet = "A/B Approved";
						else if (dapTrack.equals(C_PRODUCTION_APPROVED))
							valueToSet = "C/Production Approved";
					}
					System.out.println("Setting Document Approval Status as: " + valueToSet);
					setAttributeValueWithoutCheckout(per, "dapTrack", valueToSet);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void  setAttributeValueWithoutCheckout(Persistable persistable,String attributeName, Object attributeValue) throws WTException {
		//boolean isSuccess = false;
		try {
			PersistableAdapter persistableAdapter = new PersistableAdapter(persistable, null, SessionHelper.getLocale(),
					OperationIdentifier.newOperationIdentifier(OperationIdentifierConstants.UPDATE));
			persistableAdapter.load(attributeName);
			persistableAdapter.set(attributeName, attributeValue);
			persistable = persistableAdapter.apply();
			PersistenceServerHelper.manager.update(persistable, false);
			AttributeContainer attributecontainer = new IBAValueDBService()
					.updateAttributeContainer((IBAHolder) persistable, persistableAdapter, null, null);
			((IBAHolder) persistable).setAttributeContainer(attributecontainer);
			//isSuccess = true;
		} catch (WTException e) {
			//isSuccess = false;
			failedSB.append("\nFAILED : " +  " " + attributeName + " " + attributeValue);
			failedSB.append("\nDue To : \n" + e.getLocalizedMessage());
			e.printStackTrace();
		}
		//return isSuccess;
	}

	
	
	
	
	
	

}
