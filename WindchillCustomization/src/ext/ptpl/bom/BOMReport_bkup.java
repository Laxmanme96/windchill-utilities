package ext.ptpl.bom;
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
import wt.util.WTException;
import wt.vc.VersionControlHelper;
public class BOMReport_bkup implements RemoteAccess {
	public static void getChild(String parentPartNumber) throws Exception {
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
			//System.out.println("Role A :"+ul.getRoleAObject().getIdentity()+" Role B: "+ul.getRoleBObject().getIdentity());
			QueryResult childPartIterations = VersionControlHelper.service.allIterationsOf(childPartMaster);
			WTPart childPart = (WTPart) childPartIterations.nextElement();
			System.out.println("PARENT: " + part + " , " + "CHILD: " + childPart.getNumber());
			getChildrens(childPart);
			
		}
	}
	private static void getChildrens(WTPart childPart) throws WTException {
		QueryResult qr2 = WTPartHelper.service.getUsesWTPartMasters(childPart);
		while (qr2.hasMoreElements()) {
			WTPartUsageLink ul = (WTPartUsageLink) qr2.nextElement();
			WTPartMaster subChildPartMaster = (WTPartMaster) ul.getUses();
			System.out.println("PARENT: " + childPart.getNumber() + " , " + "CHILD: " + subChildPartMaster.getNumber());
			QueryResult childPartIterations = VersionControlHelper.service.allIterationsOf(subChildPartMaster);
			WTPart subChildPart = (WTPart) childPartIterations.nextElement();
			getChildrens(subChildPart);
		}
	}

	public static void main(String[] args) {
		RemoteMethodServer rms = RemoteMethodServer.getDefault();
		rms.setUserName("wcadmin");
		rms.setPassword("wcadmin");
		String parentPartNumber = "0000000002";
		try {
			// rms.invoke("abc1","com.plural.javafiles.test1", null,null, args1);
			getChild(parentPartNumber);
			System.out.println("Welcome to WTshell");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
