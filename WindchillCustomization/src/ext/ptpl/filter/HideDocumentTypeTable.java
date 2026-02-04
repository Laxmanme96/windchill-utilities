package ext.ptpl.filter;

import com.ptc.core.meta.server.TypeIdentifierUtility;
import com.ptc.core.ui.validation.DefaultSimpleValidationFilter;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.fc.WTReference;
import wt.part.WTPart;

public class HideDocumentTypeTable  extends DefaultSimpleValidationFilter {

	@Override
	public UIValidationStatus preValidateAction(UIValidationKey vKey, UIValidationCriteria vCriteria) {
		// TODO Auto-generated method stub
		//System.out.println("---------- HideDocumentTypeTable Filter Started-----------");
		UIValidationStatus result = UIValidationStatus.HIDDEN;
		
		WTReference reference = vCriteria.getContextObject();
		Object obj = reference.getObject();

		if (obj instanceof WTPart) {
			String partSoftType = TypeIdentifierUtility.getTypeIdentifier(obj).getTypename();
			//System.out.println("---------- HideDocumentTypeTable Filter Type : "+ partSoftType);
			if (partSoftType.contains("com.Windchill.Demo.CUSTOM_PART")) {
				result = UIValidationStatus.ENABLED;
				//System.out.println("---------- HideDocumentTypeTable Filter Started : UIValidationStatus.ENABLED");
			}
			else {
				//System.out.println("---------- HideDocumentTypeTable Filter Started : UIValidationStatus.DISABLED");
				
			}
		}
		
		return result;

	}

}
