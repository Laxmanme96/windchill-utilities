package ext.ptpl.tablebuilder;

import wt.util.WTException;
import com.ptc.core.htmlcomp.components.AbstractConfigurableTableBuilder;
import com.ptc.core.htmlcomp.tableview.ConfigurableTable;
import com.ptc.jca.mvc.components.JcaComponentParams;
import com.ptc.jca.mvc.components.JcaTreeConfig;
import com.ptc.mvc.components.ComponentBuilder;
import com.ptc.mvc.components.ComponentConfig;
import com.ptc.mvc.components.ComponentConfigFactory;
import com.ptc.mvc.components.ComponentParams;
import com.ptc.mvc.components.TreeConfig;
import com.ptc.netmarkets.model.NmOid;


@ComponentBuilder("ext.ptpl.CustomRelatedObjectsTree")
public class CustomRelatedObjectsTree extends AbstractConfigurableTableBuilder {

    @Override
    public ComponentConfig buildComponentConfig(ComponentParams params) throws WTException {
        ComponentConfigFactory factory = getComponentConfigFactory();
        TreeConfig tree = factory.newTreeConfig();
        tree.setLabel("Tree Builder");
        tree.setSelectable(true);
        tree.setActionModel("customRelatedObjectsTable_Model");
        tree.setNodeColumn("number");
        tree.setExpansionLevel("full");
        tree.setDisableAction("false");
        tree.addComponent(factory.newColumnConfig("name", true));
        tree.addComponent(factory.newColumnConfig("number", true));


        // is there an actionOid available
        NmOid primaryOid = ((JcaComponentParams) params).getHelperBean().getNmCommandBean().getPrimaryOid();
        if (primaryOid != null) {
            ((JcaTreeConfig) tree).setUniqueKey(primaryOid.toString());
        }

        return tree;
    }

    @Override
    public Object buildComponentData(ComponentConfig config, ComponentParams params) throws WTException {
        return new TreeHandler();
    }
   
    @Override
    public ConfigurableTable buildConfigurableTable(String arg0) throws WTException {
        // TODO Auto-generated method stub
        return null;
    }
}
