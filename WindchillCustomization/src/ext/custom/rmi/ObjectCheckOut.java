package ext.custom.rmi;

import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.method.RemoteMethodServer;
import wt.pom.PersistenceException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressHelper;

public class ObjectCheckOut {

	public static void main(String[] args) throws PersistenceException, WTException, WTPropertyVetoException {

		RemoteMethodServer myServer = RemoteMethodServer.getDefault();

		myServer.setUserName("wcadmin");
		myServer.setPassword("wcadmin");
		//myServer.invoke("getShortTypeName", ObjectSubtype.class.getName(), null, null, null);	


		QuerySpec qs = new QuerySpec(WTDocument.class); //select * from wtdocument where number="0000000007";
		WTDocument  doc = null;
		boolean islatest=true;
		System.out.println("Into CheckOut method");
		try{
			qs.appendWhere(new SearchCondition(WTDocument.class,WTDocument.NUMBER, SearchCondition.EQUAL,"0000000007"), null);
			QueryResult qr = PersistenceHelper.manager.find(qs);

			System.out.println("Query for fetching doc with particular Number =====================\n: "+qs);
			int versionCounter= 1;
			while(qr.hasMoreElements())
			{
				WTDocument doc2 = (WTDocument) qr.nextElement();
				QueryResult allVersions=  VersionControlHelper.service.allVersionsOf(doc2);
				System.out.println("version size "+allVersions.size());
				while(allVersions.hasMoreElements())
				{     
					if(islatest) //validate its not latest version
					{
						System.out.println("counter: "+ versionCounter);
						versionCounter++;
						doc = (WTDocument) allVersions.nextElement();
						if(!WorkInProgressHelper.isCheckedOut(doc)){
							System.out.println("Checking Out document");
							WorkInProgressHelper.service.checkout(doc, WorkInProgressHelper.service.getCheckoutFolder(),"Checked Out throughc code").getWorkingCopy();
							System.out.println("Object is CheckedOut ");
						}
						else {
							System.out.println("Object is already CheckedOut ");
						}
					}
					islatest=false;
					break;
				}
				break;
			}
			
			

		} catch(NumberFormatException nfe)
		{
			System.out.println("Into Catch Block");

		}

	
	}

}
