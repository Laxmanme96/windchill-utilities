package ext.ptpl.tablebuilder;

import java.util.Stack;

import com.ptc.core.htmlcomp.tableview.ConfigurableTable;
import com.ptc.core.ui.resources.ComponentMode;
import com.ptc.jca.mvc.components.JcaColumnConfig;
import com.ptc.jca.mvc.components.JcaComponentParams;
import com.ptc.jca.mvc.components.JcaTableConfig;
import com.ptc.mvc.components.ColumnConfig;
import com.ptc.mvc.components.ComponentBuilder;
import com.ptc.mvc.components.ComponentConfig;
import com.ptc.mvc.components.ComponentConfigFactory;
import com.ptc.mvc.components.ComponentParams;
import com.ptc.mvc.util.ClientMessageSource;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.netmarkets.util.beans.NmHelperBean;
import com.ptc.netmarkets.util.misc.NmContextItem;
import com.ptc.windchill.enterprise.part.mvc.builders.RelatedPartsDocumentsTableBuilder;
import com.ptc.windchill.enterprise.part.views.PartsDocumentsRefTableViews;

import wt.util.WTException;

@ComponentBuilder("ext.ptpl.tablebuilder.CustomDescribedByDocuments")
public class CustomDescribedByDocuments extends RelatedPartsDocumentsTableBuilder {
	private static String INST_DESCRIBE_DOCS = "part.relatedPartInstancesDescribedByDocuments.list";
	private static String DESCRIBE_DOCS = "part.relatedPartsDescribedByDocuments.list";
	private ClientMessageSource msgSource = this.getMessageSource("com.ptc.windchill.enterprise.part.partResource");
	private ClientMessageSource msgSourceComponentRB = this.getMessageSource("com.ptc.core.ui.componentRB");

	public ConfigurableTable buildConfigurableTable(String var1) throws WTException {
		return !var1.equalsIgnoreCase(INST_DESCRIBE_DOCS) && !var1.equalsIgnoreCase(DESCRIBE_DOCS)
				? null
				: new PartsDocumentsRefTableViews();
	}
		public ComponentConfig buildComponentConfig(ComponentParams var1) throws WTException  {
			JcaTableConfig cc= (JcaTableConfig) super.buildComponentConfig(var1);
			System.out.println("--------Custom Described By Documents Started---------");
			
			String var2 = "";
			ComponentConfigFactory var3 = this.getComponentConfigFactory();
			NmHelperBean var4 = ((JcaComponentParams) var1).getHelperBean();
			NmCommandBean var5 = var4.getNmCommandBean();
			JcaTableConfig var6 = (JcaTableConfig) var3.newTableConfig();
			boolean var7 = false;
			Stack var8 = var5.getContextBean().getContext().getContextItems();
			int var9 = var8.size();

			for (int var10 = 0; var10 < var9; ++var10) {
				NmContextItem var11 = (NmContextItem) var8.get(var10);
				if (var11.getAction().equalsIgnoreCase("relatedPartInstanceDocuments")) {
					var7 = true;
				}
			}

			if (var7) {
				var2 = INST_DESCRIBE_DOCS;
			} else {
				var2 = DESCRIBE_DOCS;
			}

			var6.setId(var2);
			var6.setComponentMode(ComponentMode.VIEW);
			var6.setLabel(this.msgSource.getMessage("DESCRIBED_BY_DOC_TABLE_HEADER"));
			var6.setConfigurable(true);
			var6.setSelectable(true);
			var6.setActionModel("relatedDocumentDescribesToolBar");
			ColumnConfig var20 = var3.newColumnConfig("type_icon", true);
			var6.addComponent(var20);
			ColumnConfig var21 = var3.newColumnConfig("number", true);
			var6.addComponent(var21);
			JcaColumnConfig var12 = (JcaColumnConfig) var3.newColumnConfig("version", true);
			var6.addComponent(var12);
			ColumnConfig var13 = var3.newColumnConfig("infoPageAction", false);
			var13.setDataUtilityId("infoPageAction");
			var6.addComponent(var13);
			ColumnConfig var14 = var3.newColumnConfig("name", true);
			var14.setLabel(this.msgSourceComponentRB.getMessage("NAME"));
			var6.addComponent(var14);
			ColumnConfig var15 = var3.newColumnConfig("containerName", true);
			var15.setLabel(this.msgSourceComponentRB.getMessage("CONTAINER_NAME"));
			var6.addComponent(var15);
			ColumnConfig var16 = var3.newColumnConfig("orgid", true);
			var6.addComponent(var16);
			ColumnConfig var17 = var3.newColumnConfig("state", true);
			var17.setLabel(this.msgSourceComponentRB.getMessage("STATE"));
			var6.addComponent(var17);
			ColumnConfig var18 = var3.newColumnConfig("thePersistInfo.modifyStamp", true);
			var6.addComponent(var18);
			
			ColumnConfig var22 = var3.newColumnConfig("dapTrack", true);
			var22.setLabel("DAP Track");
			var22.setDataUtilityId("dapTrack");
			var6.addComponent(var22);
			
			ColumnConfig var23 = var3.newColumnConfig("area", true); //Column to show custom Attribute
			var23.setDefaultSort(true);
			var23.setLabel("Area");
			var6.addComponent(var23);
			
			ColumnConfig var19 = var3.newColumnConfig("nmActions", false);
			((JcaColumnConfig) var19).setDescriptorProperty("actionModel", "relatedParts DescRef actions");
			var6.addComponent(var19);
			var6.setShowCount(true);
			var6.setHelpContext("doc_described_by_ref");
			System.out.println("--------Custom Described By Documents Ended---------");
			
			
			cc.addComponent(var19);
					
			return var6;
		}

}
