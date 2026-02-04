package ext.custom.filters;

import com.ptc.core.ui.validation.DefaultSimpleValidationFilter;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.part.WTPart;

public class FilterBasedONLifecycleState extends DefaultSimpleValidationFilter {

	@Override
	public UIValidationStatus preValidateAction(UIValidationKey valKey, UIValidationCriteria valCriteria) {
		UIValidationStatus valStatus = UIValidationStatus.ENABLED;
		
		//System.out.println("---- FilterBasedONLifecycleState filter Started-------------");

		WTContained contextObj = (WTContained) valCriteria.getContextObject().getObject();
		WTContainer container = contextObj.getContainer();

		if(contextObj instanceof WTPart) {
			WTPart part=(WTPart) contextObj;
			String state = part.getState().getState().toString();
			//if ( state.equalsIgnoreCase("RELEASED")&& valKey.getComponentID().toString().equals("FilterTest"))
			if ( state.equalsIgnoreCase("Released")){
				//System.out.println("---- FilterBasedONLifecycleState filter State is RELEASED-------------");

				valStatus = UIValidationStatus.HIDDEN;
			}
			else  {
				valStatus = UIValidationStatus.ENABLED;
				//System.out.println("---- FilterBasedONLifecycleState filter State is NOT RELEASED-------------");
			}
		}


		//}

		return valStatus;
	}
}
/*
 * <Service name="com.ptc.core.ui.validation.SimpleValidationFilter" targetFile="codebase/service.properties">
		<Option serviceClass="ext.custom.Filters.FilterBasedONLifecycleState"
			selector="actionVisibilityFilter"
			requestor="null"
			cardinality="duplicate"/>
	</Service>

   =======================================================================
   Custom Actions 
   ==========================================
 <objecttype name="jspAction" class="wt.part.WTPart" >
	<action name="customActionForJSP" >
		<label>Custom Action written for JSP</label>
         <command method="execute" windowType="popup" url="/netmarkets/jsp/ext/Dummyjsp.jsp"/>
		 <includeFilter name="actionVisibilityFilter"/>
      </action>
	</objecttype>
	==========================================================================
   
   
 * */
