package ext.ptpl.bom;

import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartUsageLink;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.vc.VersionControlHelper;

public class FirstLevelBOM implements RemoteAccess {
	
	public static void firstLevelBOM(String parentPartNumber)throws Exception  {
		QuerySpec qs = new QuerySpec(WTPart.class);
		qs.appendWhere(new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, parentPartNumber),
				new int[] { 0 });
		QueryResult qr = PersistenceHelper.manager.find(qs);
		WTPart part = null;
		if (qr.hasMoreElements()) {
			part = (WTPart) qr.nextElement();
		}
		part = (WTPart)VersionControlHelper.service.getLatestIteration(part,false);
		QueryResult qr2 = WTPartHelper.service.getUsesWTPartMasters(part);
		while (qr2.hasMoreElements()) {
			WTPartUsageLink ul = (WTPartUsageLink) qr2.nextElement();
			WTPartMaster childPartMaster = (WTPartMaster) ul.getUses();
			QueryResult childPartIterations = VersionControlHelper.service.allIterationsOf(childPartMaster);
			WTPart childPart = (WTPart) childPartIterations.nextElement();
			System.out.println("PARENT: " + part.getNumber() + " , " + "CHILD: " + childPart.getNumber()+ ", Attributes: State: " + childPart.getLifeCycleState() +
				 " , Created By: " + childPart.getCreatorName()   );
			System.out.println("PARENT: " + part.getNumber() + " , " + "CHILD PartMaster: " + childPartMaster.getNumber()+ ", Attributes: Line Number: " + ul.getLineNumber() +
					 " , Find Number: " + ul.getFindNumber() + " , Quntity: " + ul.getQuantity());
			}
		}
	
	public static void main(String[] args) {
		RemoteMethodServer rms = RemoteMethodServer.getDefault();
		rms.setUserName("wcadmin");
		rms.setPassword("wcadmin");
		String[] args1 = { "0000000001" };
		String parentPartNumber = "0000000001";
		try {
			//rms.invoke("firstLevelBOM","ext.ptpl.FirstLevelBOM", null,null, args1);
			System.out.println("Welcome to WTshell");
			firstLevelBOM(parentPartNumber);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}