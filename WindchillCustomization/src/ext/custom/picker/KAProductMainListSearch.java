package ext.custom.picker;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.AttributeDataUtilityHelper;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.SuggestTextBox;

import wt.util.WTException;

public class KAProductMainListSearch extends DefaultDataUtility  {

    @SuppressWarnings("unlikely-arg-type")
	@Override
    public Object getDataValue(String componentId, Object datum, ModelContext modelContext) throws WTException {
        System.out.println("KAProductMainListSearch STARTING...");

        super.getDataValue(componentId, datum, modelContext);
        
        SuggestTextBox suggest = new SuggestTextBox(componentId,"KAProductMainSuggestionHelperService");
        suggest.setLabel(componentId);
        
        // Persist selected value in view mode
        suggest.setColumnName(AttributeDataUtilityHelper.getColumnName(componentId, datum, modelContext));
        suggest.setMinChars(2);
        suggest.setRequired(true);
        suggest.setMaxResults(50);
        suggest.setMaxLength(40);
        suggest.setEnabled(true);
        suggest.setEditable(true);
        suggest.setWidth(60);

        if (modelContext.getDescriptorMode() == null || modelContext.getDescriptorMode().equals("CREATE")) {
            suggest.setValue("");
        } else {
            suggest.setValue(suggest.getParm(componentId));
        }

        System.out.println("KAProductMainListSearch ENDING...");
        return suggest;
    }

   
}
