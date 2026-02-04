package ext.ptpl.bom;

import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.inf.container.WTContainerRef;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.part.LineNumber;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartUsageLink;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressHelper;

public class CreateFirstLevelBOM implements RemoteAccess {
	
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
		
		WTPartUsageLink.newWTPartUsageLink(part, null);
		
		
		
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