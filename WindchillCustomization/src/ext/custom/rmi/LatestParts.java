package ext.custom.rmi;

import com.ptc.core.meta.common.TypeIdentifier;

import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.pds.StatementSpec;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.type.ClientTypedUtility;
import wt.util.WTException;
import wt.vc.VersionControlHelper;
import wt.vc.config.ConfigHelper;
import wt.vc.config.LatestConfigSpec;

/**
 * This class is used to get Latest Object
 */
public class LatestParts implements RemoteAccess
{

	public static void main(String ar[]) throws Exception
	{
		RemoteMethodServer remotemethodserver = RemoteMethodServer.getDefault();

		remotemethodserver.setUserName("wcadmin");
		remotemethodserver.setPassword("wcadmin");

		getLatestPartMethod1();
		//getLatestPartMethod2();
		//getPartByNumber();
		
	}


	/**
	 * This method will find out latest part and return it.
	 * @throws Exception
	 */
	public static void getLatestPartMethod1() throws Exception {

		QuerySpec querySpecPart = new QuerySpec(WTPart.class); //select * from wtpart where part number="0000000025";
		querySpecPart.appendWhere(
				new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, "NEW_NCC000043A000"),null);
		QueryResult queryResult = PersistenceHelper.manager.find(querySpecPart);
		System.out.println("Query size----------"+queryResult.size());
		if(queryResult.hasMoreElements()) {
			WTPart parts = (WTPart)queryResult.nextElement();
			
			String subtype = parts.getDisplayType().getLocalizedMessage(null);
			System.out.println("subtype Type : "+ subtype);
			WTPartMaster p1= parts.getMaster();
			QueryResult result = ConfigHelper.service.filteredIterationsOf(p1, new LatestConfigSpec());
			System.out.println("Master Object query size ---- : "+result.size());
			WTPart latestObject = (WTPart)result.nextElement();
			TypeIdentifier type=ClientTypedUtility.getTypeIdentifier(latestObject);		
			System.out.println("Latest Part ---  "+latestObject.getName() + "  " +VersionControlHelper.getIterationDisplayIdentifier(latestObject)  + "  "+ type);
		}
	}
	
	public static void getLatestPartMethod2() throws Exception {

		QuerySpec querySpecPart = new QuerySpec(WTPart.class);
		querySpecPart.appendWhere(
				new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, "0000000025"),null);
		QueryResult queryResult = PersistenceHelper.manager.find(querySpecPart);
		if(queryResult.hasMoreElements()) {
			WTPart parts = (WTPart)queryResult.nextElement();
			WTPartMaster p1= parts.getMaster();

			QueryResult qr = VersionControlHelper.service.allVersionsOf((p1));
			System.out.println("qr-------"+qr.size());
			if (qr.hasMoreElements()) {
				WTPart LatestPart = (WTPart)qr.nextElement(); 
				System.out.println("Latest Part Info------"+LatestPart.getName() + "  " +VersionControlHelper.getIterationDisplayIdentifier(LatestPart)  );

			}

		}
	}

	public static WTPart getPartByNumber() {
		WTPart resultPart = null;
		try {
			QuerySpec qSpec = new QuerySpec();
			qSpec.setAdvancedQueryEnabled(true);
			int wtPartId = qSpec.addClassList(WTPart.class, true);
			SearchCondition numberCond = new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL,"0000000025");
			qSpec.appendWhere(numberCond, new int[] { wtPartId });
			SearchCondition latestCond = new SearchCondition(WTPart.class, WTPart.LATEST_ITERATION,
					SearchCondition.IS_TRUE);
			qSpec.appendAnd();
			qSpec.appendWhere(latestCond, new int[] { wtPartId });

			QueryResult qResult = PersistenceHelper.manager.find((StatementSpec) qSpec);
			while (qResult.hasMoreElements()) {
				resultPart = (WTPart) ((Persistable[]) qResult.nextElement())[0];
				System.out.println(resultPart.getIterationDisplayIdentifier());
			}
		} catch (WTException exc) {
			System.out.println(exc.getMessage()+ exc);
		}
		return resultPart;
	}
}