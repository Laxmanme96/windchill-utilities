package ext.ptpl.tablebuilder;

import com.ptc.jca.mvc.components.JcaTableConfig;

import java.util.Iterator;

import com.ptc.jca.mvc.components.JcaComponentParams;
import com.ptc.mvc.components.AbstractComponentBuilder;
import com.ptc.mvc.components.ColumnConfig;
import com.ptc.mvc.components.ComponentBuilder;
import com.ptc.mvc.components.ComponentConfig;
import com.ptc.mvc.components.ComponentConfigFactory;
import com.ptc.mvc.components.ComponentParams;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.change2.ChangeException2;
import wt.change2.ChangeHelper2;
import wt.change2.WTChangeIssue;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeRequest2;
import wt.doc.WTDocument;
import wt.fc.QueryResult;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartUsageLink;
import wt.util.WTException;
import wt.vc.Mastered;
import wt.vc.VersionControlHelper;

@ComponentBuilder("ext.ptpl.CustomRelatedObjectsTable") //Declare the Component Builder 
public class CustomRelatedObjectsTable extends AbstractComponentBuilder { //Creating class which extends the OOTB table builder  	
	static WTHashSet set = new WTHashSet(); //Creating a set to store the meta data of buildComponentData 
	@Override //Override the buildComponentData ,which sets the meta data of table.
	public WTHashSet buildComponentData(ComponentConfig arg0, ComponentParams arg1) throws Exception { 
		System.out.println("--------buildComponentData Started");	
		//NmCommandBean to get the primaryoid of object for which Action is triggered
		NmCommandBean commandBean = ((JcaComponentParams) arg1).getHelperBean().getNmCommandBean();
		Object object = commandBean.getPrimaryOid().getRefObject();
		

		if (object instanceof WTPart) {  
		    WTPart Part = (WTPart) object;
		    System.out.println("-------> Part : " + Part.getNumber());
		    WTPartMaster pm = Part.getMaster();
		    WTPart lastestPart = (WTPart) VersionControlHelper.service.allIterationsOf(pm).nextElement();

		    try {
		        // Add each result to the set only if it's not null
		        Object childs = getChilds(lastestPart);
		        if (childs != null) {
		            set.add(childs);
		        }

		        Object describedByDoc = getDescribedByDoc(lastestPart);
		        if (describedByDoc != null) {
		            set.add(describedByDoc);
		        }

		        Object referencedByDoc = getReferencedByDoc(lastestPart);
		        if (referencedByDoc != null) {
		            set.add(referencedByDoc);
		        }

		        Object cn = getCN(lastestPart);
		        if (cn != null) {
		            set.add(cn);
		        }

		        Object cr = getCR(lastestPart);
		        if (cr != null) {
		            set.add(cr);
		        }

		        Object pr = getPR(lastestPart);
		        if (pr != null) {
		            set.add(pr);
		        }
		        
		        Object pn = getPromotionRequest(lastestPart);
		        if (pn != null) {
		            set.add(pn);
		        }

		    } catch (Exception e) { 
		        System.err.println("Error processing WTPart: " + e.getMessage());
		    }
 
		}
		return set;
	}
	
	public WTPart getChilds(WTPart part) throws WTException {
		System.out.println("-------> getChilds Started :");
		WTPart childPart = null;
	QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(part);
	while (qr.hasMoreElements()) {
		WTPartUsageLink ul = (WTPartUsageLink) qr.nextElement();
		WTPartMaster childPartMaster = (WTPartMaster) ul.getUses();
		QueryResult childPartIterations = VersionControlHelper.service.allIterationsOf(childPartMaster);
		//QueryResult childPartIterations2 = VersionControlHelper.service.allVersionsOf(childPartMaster);
		childPart = (WTPart) childPartIterations.nextElement();
	
		System.out.println("-------> Child Part : "+ childPart.getNumber());
		System.out.println("-------> getChilds Finished :");
		}
	return childPart;
	}
	
	public WTDocument getDescribedByDoc (WTPart part) throws WTException {
		System.out.println("-------> getdescribedByDoc Started :");
		Object describedByDocObj =null;
		WTDocument describedByDoc = null;
		QueryResult qr = WTPartHelper.service.getDescribedByWTDocuments(part);
		//QueryResult qr = PersistenceHelper.manager.navigate(lastestPart,WTPartDescribeLink.DESCRIBED_BY_ROLE, wt.part.WTPartDescribeLink.class,false);

		while (qr.hasMoreElements()){
			//WTPartDescribeLink link =  (WTPartDescribeLink) qr.nextElement();					
			describedByDocObj =  qr.nextElement();
			System.out.println("-------> Obj Class : "+ describedByDocObj.getClass());
			describedByDoc = (WTDocument) describedByDocObj;
			System.out.println("-------> Described Document : "+ describedByDoc.getNumber());
			if (describedByDoc != null) {
				System.out.println("-------> getdescribedByDoc Finished :");
				
			}
			
		}
		return describedByDoc;	
	}
	
	@SuppressWarnings("deprecation")
	public WTDocument getReferencedByDoc (WTPart Part) throws ChangeException2, WTException {
		System.out.println("-------> getReferencedByDoc Started :");
		Object referenceDocObj =null;	
		WTDocument referenceDoc =null;	
		
		QueryResult qr = WTPartHelper.service.getReferencesWTDocumentMasters(Part);		
		while (qr.hasMoreElements()){
			//WTPartDescribeLink link =  (WTPartDescribeLink) qr.nextElement();					
			referenceDocObj = qr.nextElement();
			QueryResult qr1 = VersionControlHelper.service.allIterationsOf((Mastered) referenceDocObj);
			referenceDoc = (WTDocument) qr1.nextElement(); 
			System.out.println("-------> Reference Document : "+ referenceDoc.getNumber());
						
			if (referenceDoc != null) {
				
				System.out.println("-------> getReferencedByDoc Finished :");
				
			}
			
		}
		
		return referenceDoc;	
	}
	
	public WTChangeOrder2 getCN (WTPart Part) throws ChangeException2, WTException {
		System.out.println("-------> getCN Started :");
		Object obj =null;		
		QueryResult qr = ChangeHelper2.service.getUniqueImplementedChangeOrders(Part);		
		while (qr.hasMoreElements()){		
			obj = qr.nextElement();	
			System.out.println("-------> Object Class : "+ obj);	
			if (obj != null) {
				System.out.println("-------> getCN Finished :");
				
			}
			
		} 		
		
		return (WTChangeOrder2) obj;		
	}
	
	public WTChangeRequest2 getCR (WTPart Part) throws ChangeException2, WTException {
		System.out.println("-------> getCR Started :");
		Object obj =null;		
		QueryResult qr = wt.change2.ChangeHelper2.service.getRelevantChangeRequests(Part);
				
		while (qr.hasMoreElements()){		
			obj = qr.nextElement();	
			System.out.println("-------> Object Class : "+ obj.getClass());	
			if (obj != null) {
				System.out.println("-------> getCR Finished :");
				
			}
			
		} 		
		
		return (WTChangeRequest2) obj;	
	}
	
	public WTChangeIssue getPR (WTPart Part) throws ChangeException2, WTException {
		//QueryResult qr = ChangeHelper2.service.getChangeRequest(problemReport);
		
		System.out.println("-------> getPR Started :");
		WTChangeIssue obj =null;		
		QueryResult qr = ChangeHelper2.service.getLatestChangeIssue(Part);		
		while (qr.hasMoreElements()){		
			obj = (WTChangeIssue) qr.nextElement();	
			System.out.println("-------> Object Class : "+ obj.getClass());	
			if (obj != null) {
				System.out.println("-------> getPR Finished :");
				
			}
			
		} 		
		
		return obj;		
	}
	
	public PromotionNotice getPromotionRequest (WTPart Part) throws ChangeException2, WTException {
			
		System.out.println("-------> getPromotionRequest Started :");
		wt.fc.collections.WTCollection promotables = new wt.fc.collections.WTArrayList();
		promotables.add(Part);	
		
		WTCollection promotionNotices = MaturityHelper.service.getPromotionNotices(promotables);	
		
		for (Iterator iterator = promotionNotices.iterator(); iterator.hasNext();) {
			wt.fc.ObjectReference or = (wt.fc.ObjectReference) iterator.next();
			PromotionNotice promotionNotice = (wt.maturity.PromotionNotice) or.getObject();
			System.out.println("Promotion Notice:" + promotionNotice.getNumber() + " State:" + promotionNotice.getState().getState().getDisplay());
			System.out.println("-------> getPromotionRequest Finished :");
			return promotionNotice;
		}	
		
		return null;		
	}
	
	
	

	@Override //Override the buildComponentConfig ,which sets the display of table.
	public ComponentConfig buildComponentConfig(ComponentParams arg0) throws WTException { 
		ComponentConfigFactory configFactory = getComponentConfigFactory(); 
		JcaTableConfig table = (JcaTableConfig) configFactory.newTableConfig(); //Creation t'able'
		
		table.setLabel("Custom Related Objects");//To set title of Table
		
		table.setSelectable(true); //to enable row are selectable

		table.setActionModel("customRelatedObjectsTable_Model"); //Action Model to set shortcut actions
		
//		ColumnConfig columnConfig1 = configFactory.newColumnConfig("nmActions", false); //nmAction: the actions for "Right Click".
//		columnConfig1.setActionModel("BOMTab_table_row_actions");
//		table.addComponent(columnConfig1);

		ColumnConfig columnConfig = configFactory.newColumnConfig("type_icon", true); // Column to show object type icon
		columnConfig.setDefaultSort(true);
		table.addComponent(columnConfig);

		columnConfig = configFactory.newColumnConfig("name", true); // Column to show Name
		columnConfig.setDefaultSort(true);
		table.addComponent(columnConfig);

		columnConfig = configFactory.newColumnConfig("number", true);  // Column to show Number
		columnConfig.setDefaultSort(true);
		table.addComponent(columnConfig);

		columnConfig = configFactory.newColumnConfig("version", true);  // Column to show version
		columnConfig.setDefaultSort(true);
		table.addComponent(columnConfig);

		columnConfig = configFactory.newColumnConfig("thePersistInfo.modifyStamp", true);  // Column to show last modified date 
		columnConfig.setDefaultSort(true);
		table.addComponent(columnConfig);

		columnConfig = configFactory.newColumnConfig("containerName", true);  // Column to show contex/container name
		columnConfig.setDefaultSort(true);
		table.addComponent(columnConfig);

		columnConfig = configFactory.newColumnConfig("state", true); //Column to show life cycle state
		columnConfig.setDefaultSort(true);
		table.addComponent(columnConfig);

		columnConfig = configFactory.newColumnConfig("infoPageAction", true); //Column to show info page action button
		columnConfig.setLabel("");
		table.addComponent(columnConfig);
		
		//table.setMenubarName("customMenubar"); // to set Menu bar
		
		return table;
	}

}
