package ext.ptpl.formProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.FormResult;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.enterprise.change2.forms.processors.CreateChangeReviewFormProcessor;
import wt.util.WTException;

public class PTPLCreateChangeReviewFormProcessor extends CreateChangeReviewFormProcessor {
    
	@Override
    public FormResult preProcess(NmCommandBean var1, List<ObjectBean> var2) throws WTException {
    	FormResult formResult = new FormResult();
    	
        HashMap<String, List<NmOid>> items = var1.getAddedItems();
        var1.getAddedItemsByName("changeReview_affectedData_table");
        
        for (Map.Entry<String, List<NmOid>> entry : items.entrySet()) {
            String key = entry.getKey();
            List<NmOid> valueList = entry.getValue();
            
            System.out.println("Key: " + key);
            for (NmOid oid : valueList) {
                System.out.println("  NmOid: " + oid);
            }
        }

        return formResult;
    }
}
