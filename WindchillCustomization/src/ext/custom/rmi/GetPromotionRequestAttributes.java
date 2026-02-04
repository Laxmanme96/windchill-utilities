package ext.custom.rmi;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.maturity.MaturityException;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.maturity.PromotionTarget;
import wt.method.RemoteMethodServer;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;

public class GetPromotionRequestAttributes {

	public static void main(String ar[]) throws Exception {
		RemoteMethodServer remotemethodserver = RemoteMethodServer.getDefault();

		remotemethodserver.setUserName("wcadmin");
		remotemethodserver.setPassword("wcadmin");
		String pnNumber = "00021";
		String attributeInternalname = "state.state";
		run(pnNumber, attributeInternalname);
	}

	public static void run(String pnNumber, String attributeInternalname)
			throws WTException, PropertyVetoException, IOException {

		QuerySpec qs = new QuerySpec(PromotionNotice.class);
		boolean islatest = true;
		String pnTargetObjects = "";
		System.out.println("Into Retriving IBA  value ");
		int count = 0;
		try {
			qs.appendWhere(
					new SearchCondition(PromotionNotice.class, PromotionNotice.NUMBER, SearchCondition.EQUAL, pnNumber),
					null);
			QueryResult qr = PersistenceHelper.manager.find(qs);
			System.out.println("Query size : " + qr.size());

			while (qr.hasMoreElements()) {
				if (count >= 1) {
					break; // Stop after processing the first document
				}
				Object primaryBusinessObject = qr.nextElement();
				PromotionNotice pn = (PromotionNotice) primaryBusinessObject;
				QueryResult query = MaturityHelper.service.getPromotionTargets(pn);
				 Map<WTObject, String> promotedFromStates = getPromotedFromStates(pn);
				while (query.hasMoreElements()) {
					Object obj = query.nextElement();
					 String state = promotedFromStates.get(obj);
					if (obj instanceof wt.part.WTPart) {
						wt.part.WTPart part = (wt.part.WTPart) obj;
						pnTargetObjects = part.getNumber().toString();
						System.out.println("pnTargetObjects Part Type : " + pnTargetObjects + " State : "+ state);
					}
					if (obj instanceof wt.doc.WTDocument) {
						WTDocument doc = (wt.doc.WTDocument) obj;
						pnTargetObjects = doc.getNumber().toString();
						System.out.println("pnTargetObjects Document Type : " + pnTargetObjects + " State : "+ state);
					}
				}
				
				//System.out.println("Promoted from state: " + promotedFromStates.toString());
			}

		} catch (NumberFormatException nfe) {
			System.out.println("Into Catch Block");
		}
	}

	public static Map<WTObject, String> getPromotedFromStates(PromotionNotice pn) throws MaturityException, WTException {
	    Map<WTObject, String> promotedFromStates = new HashMap<>();

	    QueryResult promotionObjectLinks = MaturityHelper.service.getPromotionTargets(pn, false);
	    while (promotionObjectLinks.hasMoreElements()) {
	        PromotionTarget pt = (PromotionTarget) promotionObjectLinks.nextElement();
	        WTObject targetObject = (WTObject) pt.getRoleBObject();
	        WTObject pbo = (WTObject) pt.getRoleAObject();
	        String state = pt.getCreateState().getDisplay().toString();
	        promotedFromStates.put(targetObject, state);
	        //System.out.println("Role A Object: " + pbo.getClass() + ", Promoted from state: " + state);
	        //System.out.println("Role B Object: " + targetObject.getClass() + ", Promoted from state: " + state);
	    }
	    return promotedFromStates;
	}
}
