package ext.custom.rmi;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.UpdateOperationIdentifier;
import wt.doc.WTDocument;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;

public class UpdateAttribute {

	public static void run(WTDocument document) throws WTException,PropertyVetoException, IOException {
		//RemoteMethodServer myServer = RemoteMethodServer.getDefault();

		//myServer.setUserName("wcadmin");
		//myServer.setPassword("wcadmin"); 
		//myServer.invoke("getShortTypeName", ObjectSubtype.class.getName(), null, null, null);
		final String color = "color";
		final String address = "address";

		Map<String,String> attributeMap =new HashMap<String,String>();
		attributeMap.put(color,"PINK");
		attributeMap.put(address,"KHUNTI");

		QuerySpec qs = new QuerySpec(WTDocument.class);
		WTDocument  wtDoc = null;
		WTDocument  doc = null;
		boolean islatest=true;
		System.out.println("Into Update IBA  method");
		try{
			qs.appendWhere(new SearchCondition(WTDocument.class,WTDocument.NUMBER, SearchCondition.EQUAL,document.getNumber()), null);
			QueryResult qr = PersistenceHelper.manager.find(qs);

			System.out.println("Query size : "+qr.size());
			while(qr.hasMoreElements())
			{
				WTDocument doc2 = (WTDocument) qr.nextElement();
				QueryResult allVersions=  VersionControlHelper.service.allVersionsOf(doc2);
				System.out.println("version size "+allVersions.size());
				while(allVersions.hasMoreElements())
				{     
					if(islatest) //validate it is latest version or not ?
					{
						wtDoc = (WTDocument) allVersions.nextElement();
						if(!WorkInProgressHelper.isCheckedOut(doc)){
							System.out.println("CheckOut Document ");
							doc=(WTDocument)WorkInProgressHelper.service.checkout(wtDoc, WorkInProgressHelper.service.getCheckoutFolder(),"Checked Out").getWorkingCopy();

						}

						try{
							PersistableAdapter obj = new PersistableAdapter(doc, null,null, new UpdateOperationIdentifier());			
							obj.persist();
							for (Map.Entry<String,String> entry : attributeMap.entrySet()) {
								System.out.println("IBA Name "+entry.getKey() + "= "+ entry.getValue());
								obj.load(entry.getKey());
								obj.set(entry.getKey(),entry.getValue());
								System.out.println(" last line in for loop "); 

							} 
							obj.persist();
							System.out.println("Line 72 ");
							obj.apply();
							System.out.println("--------before modifystatement ");
							PersistenceServerHelper.manager.update((Persistable) doc);
							//wt.fc.PersistenceHelper.manager.modify(doc);
							if(WorkInProgressHelper.isCheckedOut(doc)){
								System.out.println("CheckIn doc");
								WorkInProgressHelper.service.checkin((Workable)doc,"Checked In");	
							}
						}catch(WTException e) {
							e.printStackTrace();
							System.out.println("Line 90 Inside catch block");
							try{
								if(WorkInProgressHelper.isCheckedOut(doc)){
									System.out.println("CheckIn doc");
									WorkInProgressHelper.service.checkin((Workable)doc,"Checked In");	
								}
							}catch(Exception e1){
								System.out.println("ERROR during checkout: "+ e1.getMessage());	
							}
						}
					}
				}
				islatest=false;
				break;
			}
		} catch(NumberFormatException nfe)
		{
			System.out.println("Into Catch Block");
		}
	}

}
