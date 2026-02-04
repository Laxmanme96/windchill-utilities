package ext.custom.rmi;

import com.ptc.core.meta.common.TypeIdentifier;

import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.part.WTPart;
import wt.query.QuerySpec;
import wt.type.ClientTypedUtility;

public class ListAllParts implements RemoteAccess
{
	
	public static void main(String ar[]) throws Exception
	{
		RemoteMethodServer remotemethodserver = RemoteMethodServer.getDefault();

		remotemethodserver.setUserName("wcadmin");
		remotemethodserver.setPassword("wcadmin");
		
		getPart();
	}
	
	public static void getPart() throws Exception {
		
		QuerySpec querySpecPart = new QuerySpec(WTPart.class); //select * from wtpart
		System.out.println("querySpecPart--------"+querySpecPart);
		QueryResult queryResult = PersistenceHelper.manager.find(querySpecPart); //This will return part data in Query Result
		System.out.println("queryResult--------"+queryResult);
		System.out.println(queryResult.size());
		while(queryResult.hasMoreElements()) { //iterating over query result and fetch part one by one
			WTPart part = (WTPart)queryResult.nextElement();
			TypeIdentifier type1= ClientTypedUtility.getTypeIdentifier(part);
			
			System.out.println(part.getName() + "        " + type1);
			}
		
		}
	}
