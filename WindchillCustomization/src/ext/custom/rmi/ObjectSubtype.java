package ext.custom.rmi;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Locale;

import wt.doc.WTDocument;
import wt.fc.ObjectIdentifier;
import wt.fc.ObjectReference;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

public class ObjectSubtype implements RemoteAccess{

	public static void main(String[] args) throws RemoteException, InvocationTargetException, WTException {
		RemoteMethodServer myServer = RemoteMethodServer.getDefault();

		myServer.setUserName("wcadmin");
		myServer.setPassword("wcadmin");
		//myServer.invoke("getShortTypeName", ObjectSubtype.class.getName(), null, null, null);	


		QuerySpec qs = new QuerySpec(WTDocument.class);
		WTDocument  doc = null;
		boolean islatest=true;
		System.out.println("Into getShortTypeName method");
		try{
			qs.appendWhere(new SearchCondition(WTDocument.class,WTDocument.NUMBER, SearchCondition.EQUAL,"0000000001"), null);
			QueryResult qr = PersistenceHelper.manager.find(qs);

			System.out.println("Query size : "+qr.size());
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
						ObjectReference objRef=ObjectReference.newObjectReference(doc);
						ReferenceFactory rf = new ReferenceFactory ();
						String refString = rf.getReferenceString (objRef);

						ObjectIdentifier obid = ObjectIdentifier.newObjectIdentifier(refString.substring(3));
						WTDocument part = (WTDocument)PersistenceHelper.manager.refresh(obid);
						String subtype = part.getDisplayType().getLocalizedMessage(Locale.ENGLISH);
						System.out.println("subtype ---> "+subtype);
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

/*
Consider a new WTPart is created, below is the VR and OR for difference revisions and iterations created:

Activity	Version	Version Reference (VR)	Object Reference (OR)
After Part Creation	Version A.1	oid=VR:wt.part.WTPart:246003	oid=OR:wt.part.WTPart:246004
After Iteration Change	Version A.2	oid=VR:wt.part.WTPart:246003	oid=OR:wt.part.WTPart:246018
After Revision Change	Version B.1	oid=VR:wt.part.WTPart:246041	oid=OR:wt.part.WTPart:246042
After Iteration Change	Version B.2	oid=VR:wt.part.WTPart:246041	oid=OR:wt.part.WTPart:246053
 

From the table, it is clear that Version Reference(VR) refers to a particular revision of a part i.e. VR will be the same for all the iterations of a particular revision.

The VR number is repeated and shared on a per iteration basis, meaning that all iterations to that mastered/revision controlled object will be the same. VR also redirects you to the latest iteration of the latest revision.

On the other hand, Object Reference(OR) refers to the actual object i.e. every iteration will have a different OR.

Finally, if you want to convert VR to OR, below is the code:

public static String getORFromVR(String vr) throws WTException
vr = vr.substring(0, vr.lastIndexOf(":"))
ReferenceFactory rf = new ReferenceFactory();
WTReference ref;
ref = rf.getReference(vr);
WTObject lct = (WTObject) ref.getObject();
ObjectReference objRef = ObjectReference.newObjectReference(lct);
return objRef.getKey().toString();
}
*/