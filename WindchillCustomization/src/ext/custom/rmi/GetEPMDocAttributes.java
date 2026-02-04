package ext.custom.rmi;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.Locale;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.core.meta.server.TypeIdentifierUtility;

import wt.doc.WTDocument;
import wt.epm.EPMDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.method.RemoteMethodServer;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTException;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.WorkInProgressState;

public class GetEPMDocAttributes {

	public static void main(String ar[]) throws Exception {
		RemoteMethodServer remotemethodserver = RemoteMethodServer.getDefault();

		remotemethodserver.setUserName("wcadmin");
		remotemethodserver.setPassword("wcadmin");
		String epmDocNumber = "0000000001";
		String attributeInternalname = "state.state";
		run(epmDocNumber, attributeInternalname);
	}

	public static void run(String epmDocNumber, String attributeInternalname)
			throws WTException, PropertyVetoException, IOException {

		QuerySpec qs = new QuerySpec(EPMDocument.class);
		EPMDocument doc = null;
		//boolean islatest = true;
		System.out.println("Into Retriving IBA  value ");
		try {
			qs.appendWhere(new SearchCondition(EPMDocument.class, EPMDocument.NUMBER, SearchCondition.EQUAL, epmDocNumber),
					null);
			QueryResult qr = PersistenceHelper.manager.find(qs);
			System.out.println("Query size : " + qr.size());
			while (qr.hasMoreElements()) {
				System.out.println("version size 2 " + qr.size());
				EPMDocument doc2 = (EPMDocument) qr.nextElement();
				String subtype = doc2.getDisplayType().getLocalizedMessage(null);
				System.out.println("subtype Type : "+ subtype);
				
				QueryResult allVersions = VersionControlHelper.service.allVersionsOf(doc2);
				System.out.println("version size " + allVersions.size());
				while (allVersions.hasMoreElements()) {
//					if (islatest) // validate it is latest version or not ?
//					{
						doc = (EPMDocument) allVersions.nextElement();
//						Boolean isPartCheckedOut = WorkInProgressHelper.isCheckedOut(doc);
//						WorkInProgressState wipState = WorkInProgressHelper.getState(doc);
//						System.out.println("Working copy of doc : "+ isPartCheckedOut);
//						System.out.println("Object Stae : "+ wipState);
						
		
						System.out.println("getLifeCyclename : "+ doc.getLifeCycleName());
						System.out.println("getLifeCycleState : "+ doc.getLifeCycleState().getDisplay());
						System.out.println("getState : "+ doc.getState());
						
						try {
							PersistableAdapter obj = new PersistableAdapter(doc, null, null,
									new DisplayOperationIdentifier());
							obj.persist();
							obj.load(attributeInternalname);
							String attrVal = (String) obj.get(attributeInternalname);
							System.out.println(
									"\n Attribute  " + attributeInternalname + "  has value  -> " + attrVal + "\n");
							System.out.println(" last line in for loop ");
							System.out.println(" Out of For loop ");

						} catch (WTException e) {
							e.printStackTrace();
							System.out.println("Line 90 Inside catch block");
						}
					}
				//}
//				islatest = false;
				break;
			}
		} catch (NumberFormatException nfe) {
			System.out.println("Into Catch Block");
		}
	}

}
