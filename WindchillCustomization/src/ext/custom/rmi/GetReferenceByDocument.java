package ext.custom.rmi;

import java.util.Iterator;
import java.util.Set;

import wt.doc.WTDocument;
import wt.fc.ObjectReference;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTReference;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTKeyedMap;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.part.PartDocHelper;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.vc.config.ConfigHelper;
import wt.vc.config.LatestConfigSpec;


public class GetReferenceByDocument implements RemoteAccess{

	public static void Download(String name) throws Exception
	{  
		WTPart part1=getLatestPartMethod(name);

		WTCollection collectionPart=new WTArrayList();
		collectionPart.add(part1);
		//get the describedByDocuments to the part
		 WTKeyedMap refDoc  = PartDocHelper.service.getAssociatedReferenceDocuments(collectionPart);
		if(refDoc.size()==0) {
			System.out.println("This part has no describe by document");
		}
		 Set<?> set = refDoc.keySet();
	        Iterator<?> iter = set.iterator();
	        Iterator<?> iter2 ;
	        wt.fc.ObjectReference objref = null;
	        WTDocument wtdoc;
	        while(iter.hasNext()){
	            wt.fc.collections.WTHashSet myWTHashSet = (WTHashSet) refDoc.get(iter.next());
	            iter2 = myWTHashSet.iterator();
	            while(iter2.hasNext()){
	                objref = (ObjectReference) iter2.next();
	    			System.out.println("Obj Ref for ref doc by Document name : "+objref);
	    			ReferenceFactory rf = new ReferenceFactory ();		
	    			WTReference reference= (WTReference)rf.getReference(objref.toString());
	    			wtdoc=(WTDocument)reference.getObject();
	    			System.out.println("Ref By Document name : "+wtdoc.getName());
	                
	            }
	        }
	}

	public static void main(String args[]) throws Exception
	{
		RemoteMethodServer myServer = RemoteMethodServer.getDefault();

		myServer.setUserName("wcadmin");
		myServer.setPassword("wcadmin"); 
		GetReferenceByDocument.Download("Part1");	
	}

	public static WTPart getLatestPartMethod(String name) throws Exception {
		WTPart latestObject=null;
		QuerySpec querySpecPart = new QuerySpec(WTPart.class); //select * from wtpart where part number="0000000025";
		querySpecPart.appendWhere(
				new SearchCondition(WTPart.class, WTPart.NAME, SearchCondition.EQUAL, name),null);
		QueryResult queryResult = PersistenceHelper.manager.find(querySpecPart);
		//System.out.println("Query size----------"+queryResult.size());
		if(queryResult.hasMoreElements()) {
			WTPart parts = (WTPart)queryResult.nextElement();
			WTPartMaster p1= parts.getMaster();
			QueryResult result = ConfigHelper.service.filteredIterationsOf(p1, new LatestConfigSpec());
			//System.out.println("Master Object query size ---- : "+result.size());
			latestObject = (WTPart)result.nextElement();
			//TypeIdentifier type=ClientTypedUtility.getTypeIdentifier(latestObject);		
			//System.out.println("Latest Part ---  "+latestObject.getName() + "  " +VersionControlHelper.getIterationDisplayIdentifier(latestObject)  + "  "+ type);
		}
		return latestObject;
	}
}

