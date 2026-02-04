package ext.custom.rmi;

import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.maturity.PromotionNotice;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;

public class GetPromotionTargetObjectsRMI implements RemoteAccess {

	public static void main(String ar[]) throws Exception {
		RemoteMethodServer remotemethodserver = RemoteMethodServer.getDefault();

		remotemethodserver.setUserName("wcadmin");
		remotemethodserver.setPassword("wcadmin");
		String pnTargetObjects = "";
		QuerySpec qs = new QuerySpec(PromotionNotice.class);
		int count = 0;
		try {
			qs.appendWhere(
					new SearchCondition(PromotionNotice.class, PromotionNotice.NUMBER, SearchCondition.EQUAL, "00081"),
					null);
			QueryResult qr = PersistenceHelper.manager.find(qs);
			System.out.println("Query size : " + qr.size());

			while (qr.hasMoreElements()) {
				if (count >= 1) {
					break; // Stop after processing the first document
				}
				Object primaryBusinessObject = qr.nextElement();
				wt.maturity.PromotionNotice pn = (wt.maturity.PromotionNotice) primaryBusinessObject;
				wt.fc.QueryResult query = wt.maturity.MaturityHelper.service.getPromotionTargets(pn);
				while (query.hasMoreElements()) {
					Object obj = query.nextElement();
					if (obj instanceof wt.part.WTPart) {
						wt.part.WTPart part = (wt.part.WTPart) query.nextElement();
						pnTargetObjects = part.getNumber().toString();
						System.out.println("pnTargetObjects Part Type : " + pnTargetObjects);
					}
					if (obj instanceof wt.doc.WTDocument) {
						wt.doc.WTDocument doc = (wt.doc.WTDocument) query.nextElement();
						pnTargetObjects = doc.getNumber().toString();
						System.out.println("pnTargetObjects Document Type : " + pnTargetObjects);
					}
				}

				count++;
			}

		} catch (WTException e) {
			e.printStackTrace();
		}

	}
}
