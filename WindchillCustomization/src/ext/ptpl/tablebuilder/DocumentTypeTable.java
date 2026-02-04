package ext.ptpl.tablebuilder;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.htmlcomp.tableview.ConfigurableTable;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.ui.resources.ComponentMode;
import com.ptc.jca.mvc.components.JcaComponentParams;
import com.ptc.jca.mvc.components.JcaTableConfig;
import com.ptc.mvc.components.AbstractComponentBuilder;
import com.ptc.mvc.components.ColumnConfig;
import com.ptc.mvc.components.ComponentBuilder;
import com.ptc.mvc.components.ComponentConfig;
import com.ptc.mvc.components.ComponentConfigFactory;
import com.ptc.mvc.components.ComponentParams;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.enterprise.part.views.PartsDocumentsRefTableViews;

import wt.doc.WTDocument;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTHashSet;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.vc.VersionControlHelper;
import wt.log4j.LogR;

@ComponentBuilder("ext.ptpl.DocumentTypeTable")
public class DocumentTypeTable extends AbstractComponentBuilder {
	protected static final Logger logger = LogR.getLogger(DocumentTypeTable.class.getName());
	static WTHashSet set = new WTHashSet(); // Creating a set to store the meta data of buildComponentData
	private static String INST_DESCRIBE_DOCS = "part.relatedPartInstancesDescribedByDocuments.list";
	private static String DESCRIBE_DOCS = "part.relatedPartsDescribedByDocuments.list";
	
	public ConfigurableTable buildConfigurableTable(String var1) throws WTException {
		return !var1.equalsIgnoreCase(INST_DESCRIBE_DOCS) && !var1.equalsIgnoreCase(DESCRIBE_DOCS)
				? null
				: new PartsDocumentsRefTableViews();
	}

	@Override
	public Object buildComponentData(ComponentConfig arg0, ComponentParams arg1) throws Exception {
		//System.out.println("--------buildComponentData Started");
		// NmCommandBean to get the primaryoid of object for which Action is triggered
		NmCommandBean commandBean = ((JcaComponentParams) arg1).getHelperBean().getNmCommandBean();
		Object object = commandBean.getPrimaryOid().getRefObject();

		if (object instanceof WTPart) {
			WTPart Part = (WTPart) object;
			System.out.println("-------> Part : " + Part.getNumber());
			WTPartMaster pm = Part.getMaster();
			WTPart lastestPart = (WTPart) VersionControlHelper.service.allIterationsOf(pm).nextElement();

			PersistableAdapter pers = new PersistableAdapter(lastestPart, null, Locale.US,
					new DisplayOperationIdentifier());
			pers.load("DOCUMENT_TYPE");
			Object ibaValueObject = pers.get("DOCUMENT_TYPE"); // IBA name
			System.out.println("-------> Part.IBAValue : " + ibaValueObject.toString());
			if (ibaValueObject instanceof Object[]) {
				Object[] ibaValues = (Object[]) ibaValueObject;
				for (Object ibaValue : ibaValues) {
					System.out.println("IBA Value: " + ibaValue.toString());
					String documentType = ibaValue.toString();
					WTDocument document = getDocument(documentType);
					if (!(document == null)) {
						set.add(document);
						
					}
				}
			}
		}
		//System.out.println("--------buildComponentData Set Values :"+set.toString());
		
		return set;
	}

	@Override //Override the buildComponentConfig ,which sets the display of table.
	public ComponentConfig buildComponentConfig(ComponentParams arg0) throws WTException { 
		ComponentConfigFactory configFactory = getComponentConfigFactory(); 
		JcaTableConfig table = (JcaTableConfig) configFactory.newTableConfig(); //Creation t'able'
		table.setComponentMode(ComponentMode.VIEW);
		table.setConfigurable(true);
		table.setLabel("Documet Type Table");//To set title of Table
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
		
		columnConfig = configFactory.newColumnConfig("area", true); //Column to show custom Attribute
		columnConfig.setDefaultSort(true);
		columnConfig.setLabel("Area");
		table.addComponent(columnConfig);
		
		columnConfig = configFactory.newColumnConfig("dapTrack", true); //Column to show custom Attribute
		columnConfig.setDefaultSort(true);
		columnConfig.setLabel("DAP Track");
		table.addComponent(columnConfig);
		
		//table.setMenubarName("customMenubar"); // to set Menu bar
		
		return table;
	}



	public static WTDocument getDocument(String number) {
		try {
			QuerySpec querySpec = new QuerySpec(WTDocument.class);
			querySpec.appendWhere(new SearchCondition(WTDocument.class, WTDocument.NUMBER, SearchCondition.EQUAL, number),
					null);
			QueryResult result = wt.fc.PersistenceHelper.manager.find(querySpec);
			System.out.println("--------QueryResult :"+result.size());
			while (result.hasMoreElements()) {
				WTDocument doc = (WTDocument) result.nextElement();
				QueryResult qr = VersionControlHelper.service.allVersionsOf(doc);
				while (qr.hasMoreElements()) {
					WTDocument latestDoc = (WTDocument) qr.nextElement();
					//System.out.println("-------Latest Doc with latest iteration " +	latestDoc.getIdentity() + latestDoc.getIterationDisplayIdentifier());
					return latestDoc;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
