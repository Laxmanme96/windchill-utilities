package ext.custom.rmi;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.UpdateOperationIdentifier;
import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

public class GetAttributeValue {

	public static void run(WTDocument document) throws WTException,PropertyVetoException, IOException {
		final String color = "color";
		final String address = "address";

		List<String> attributeList =new ArrayList<String>();
		attributeList.add(color);
		attributeList.add(address);

		QuerySpec qs = new QuerySpec(WTDocument.class);
		WTDocument  doc = null;
		boolean islatest=true;
		System.out.println("Into Retriving IBA  value ");
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
						doc = (WTDocument) allVersions.nextElement();
						try{
							PersistableAdapter obj = new PersistableAdapter(doc, null,null, new UpdateOperationIdentifier());			
							obj.persist();
							for (int i=0;i<attributeList.size();i++) {
								System.out.println("IBA Name "+attributeList.get(i));
								obj.persist();
								obj.load(attributeList.get(i));
								String attrVal = (String)obj.get(attributeList.get(i));
								System.out.println("\n Attribute  "+  attributeList.get(i)+"  has value  -> "+ attrVal +"\n");
								System.out.println(" last line in for loop ");
							} 
							System.out.println(" Out of For loop ");
							
						}catch(WTException e) {
							e.printStackTrace();
							System.out.println("Line 90 Inside catch block");
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
