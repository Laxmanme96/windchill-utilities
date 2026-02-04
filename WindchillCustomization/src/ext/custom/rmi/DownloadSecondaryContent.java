package ext.custom.rmi;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentHolder;
import wt.content.ContentRoleType;
import wt.content.ContentServerHelper;
import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

public class DownloadSecondaryContent{

	//public static void main(String[] args) throws InvocationTargetException, WTException, IOException, PropertyVetoException {
	public static void run(WTDocument document) throws WTException,PropertyVetoException, IOException {
		//RemoteMethodServer myServer = RemoteMethodServer.getDefault();

		//myServer.setUserName("wcadmin");
		//myServer.setPassword("wcadmin"); 
		//myServer.invoke("getShortTypeName", ObjectSubtype.class.getName(), null, null, null);	

		String filepath="C:\\Users\\riteshp1\\Desktop\\Projects\\Windchill Practice Codes\\Content";
		
		QuerySpec qs = new QuerySpec(WTDocument.class);
		WTDocument  doc = null;
		boolean islatest=true;
		System.out.println("Into Download Secondary Content  method");
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
					if(islatest) //validate its not latest version
					{
						doc = (WTDocument) allVersions.nextElement();
						String currfileName = null;
						File saveAsFile = null;
						ApplicationData appData = null;
						String newFileName=null; 
						ContentHolder contentSecondary = ContentHelper.service.getContents((ContentHolder)doc);
						QueryResult vcontent = ContentHelper.service.getContentsByRole(contentSecondary,ContentRoleType.SECONDARY); // this will return Secondary Attachments
						if(vcontent.size() > 0){

							System.out.println("Document has number of Secondary attachments -> " + vcontent.size());

							for(int i=0; i<vcontent.size(); i++) {

								appData = (ApplicationData) vcontent.nextElement();

								System.out.println("***** VContent -> " + appData);					 

								currfileName = appData.getFileName();
								System.out.println("Current File Name -> " + currfileName);
								
								newFileName = doc.getNumber()+"_"+currfileName;//+doc.getNumber()+getTimeStamp();
								
									System.out.println("New File name after Downloading will be : "+ newFileName);
									String tempPath = filepath+FileSystems.getDefault().getSeparator()+"Secondary";
									System.out.println("tempPath: "+ tempPath);
									saveAsFile = new java.io.File(tempPath,newFileName);
									ContentServerHelper.service.writeContentStream(appData, saveAsFile.getCanonicalPath());
								
							}
						}
					}
					islatest=false;
					break;
				}
				
			}
			

		} catch(NumberFormatException nfe)
		{
			System.out.println("Into Catch Block");

		}

	}
}

/*
 * Run this code from workflow else you will get below error :

Exception in thread "main" java.lang.ExceptionInInitializerError
at ext.custom.project.DownloadSecondaryContent.main(DownloadSecondaryContent.java:82)
Caused by: java.lang.NullPointerException
at wt.content.ContentServerHelper.<clinit>(ContentServerHelper.java:66)
... 1 more
------------------------------------------------------------------------------

In workflow execution expression robot write below code o call this class:

System.out.println("****************************Start of WorkFlow***********************************");
ext.custom.project.DownloadSecondaryContent.run((wt.doc.WTDocument)primaryBusinessObject);
System.out.println("****************************END of WorkFlow*************************************");

*/