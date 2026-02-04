package ext.ptpl.datautility;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.suggest.SuggestParms;
import com.ptc.core.components.suggest.SuggestResult;
import com.ptc.core.components.suggest.Suggestable;

import ext.emerson.properties.CustomProperties;

/**
 * Helper class to generate suggestions from SQLite db
 */
public class EmersonAutoSuggestSearchHelper implements Suggestable {
	private CustomProperties props = new CustomProperties(CustomProperties.USER);
	private Logger logger = CustomProperties.getlogger("ext.emerson.windchill.datautility");
	private ArrayList<SuggestResult> result = new ArrayList<SuggestResult>();

	/**
	 * OOTB method to override for suggestions
	 */
	@Override
	public Collection<SuggestResult> getSuggestions(SuggestParms params) {
		// String search = params.getSearchTerm().toLowerCase();

		try {
			getUserSuggestionsFromDB(params);
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

	private Collection<SuggestResult> getUserSuggestionsFromDB(SuggestParms params) throws Exception {
		
		SuggestParms params1 = params;
		
		params1.getCommandBean().gt
		
		
		

		return result;

	}

}
