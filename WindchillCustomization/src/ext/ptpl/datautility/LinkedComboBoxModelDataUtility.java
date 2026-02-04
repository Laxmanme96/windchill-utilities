package ext.ptpl.datautility;

import java.util.ArrayList;
import java.util.Arrays;

import wt.util.WTException;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.AttributeDataUtilityHelper;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.ComboBox;
import com.ptc.core.ui.resources.ComponentMode;

public class LinkedComboBoxModelDataUtility extends DefaultDataUtility{
	@Override
	public Object getDataValue(String componentId, Object datum, ModelContext modelContext) throws WTException {
		
		Object object = super.getDataValue(componentId, datum, modelContext);
        ComboBox comboBox = new ComboBox(); 
        
        String o=TgsPropertyHelper.getPropertyValue("Country");
        String[] ab=o.split(",");
        
        comboBox = new ComboBox();
        ArrayList<String> displayList = new ArrayList<String>();
        ArrayList<String> internalList = new ArrayList<String>();
        internalList.addAll(Arrays.asList(ab));
        displayList.addAll(Arrays.asList(ab));
        if (modelContext.getDescriptorMode().equals(ComponentMode.CREATE) || modelContext.getDescriptorMode().equals(ComponentMode.EDIT)) {
			 
            comboBox.setId(componentId);
            comboBox.setInternalValues(internalList);
            comboBox.setColumnName(AttributeDataUtilityHelper.getColumnName(componentId, datum, modelContext));
            comboBox.setValues(displayList);
          // comboBox.setSelected("");
            comboBox.setEnabled(true);
            //comboBox.setRequired(true);
            comboBox.addJsAction("onchange","loadHarnessVariant()");
            return comboBox;            
           
		 }
       
        return object;
        
	}
}
