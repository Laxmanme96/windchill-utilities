package ext.custom.picker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.ptc.core.components.suggest.SuggestParms;
import com.ptc.core.components.suggest.SuggestResult;
import com.ptc.core.components.suggest.Suggestable;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.DataSet;
import com.ptc.core.meta.common.DiscreteSet;
import com.ptc.core.meta.common.OperationIdentifier;
import com.ptc.core.meta.common.OperationIdentifierConstants;
import com.ptc.core.meta.container.common.AttributeTypeSummary;



		/*<Service context="default" name="com.ptc.core.components.suggest.Suggestable" targetFile="codebase/service.properties">
		<option cardinality="duplicate" order="1" overridable="true" requestor="null"
		selector="kaProductMainSuggestionHelper"
		serviceClass="ext.custom.picker.KAProductMainListSearch"/>
		</Service> */

public class KAProductMainSuggestionHelperService implements Suggestable{
	
	 public Collection<SuggestResult> getSuggestions(SuggestParms suggestParms) {
	        System.out.println("KAProductMainListSearch: Fetching suggestions...");

	        List<String> legalValueListValues;
	        try {
	            legalValueListValues = getLegalValueListByAttribute();
	            String searchKey = suggestParms.getSearchTerm().toLowerCase();
	            ArrayList<SuggestResult> suggestList = new ArrayList<>();

	            for (String s : legalValueListValues) {
	                int index = s.toLowerCase().indexOf(searchKey);
	                if (index >= 0) { // Fix: Ensure valid substring match
	                    suggestList.add(SuggestResult.valueOf(s));
	                }
	            }

	            return suggestList;
	        } catch (Exception e) {
	            e.printStackTrace();
	        }

	        System.out.println("KAProductMainListSearch: Suggestions fetch failed.");
	        return null;
	    }

	    public List<String> getLegalValueListByAttribute() throws Exception {
	        System.out.println("KAProductMainListSearch: Fetching legal values...");

	        List<String> list = new ArrayList<>();
	        PersistableAdapter obj = new PersistableAdapter("com.pluraltech.windchill.MECHANICAL_PART", 
	                Locale.getDefault(),
	                OperationIdentifier.newOperationIdentifier(OperationIdentifierConstants.VIEW));

	        obj.load("COUNTRY");

	        System.out.println("PersistableAdapter: " + obj);
	        AttributeTypeSummary ats = obj.getAttributeDescriptor("COUNTRY");
	        System.out.println("AttributeTypeSummary: " + ats);

	        DataSet ds = ats.getLegalValueSet();
	        System.out.println("Dataset: " + ds);

	        if (ds instanceof DiscreteSet) {
	            System.out.println("DiscreteSet Values: " + ((DiscreteSet) ds).getElements());
	            for (Object s : ((DiscreteSet) ds).getElements()) {
	                list.add(String.valueOf(s));
	            }
	        }

	        System.out.println("KAProductMainListSearch: Legal value fetching complete.");
	        return list;
	    }

}
