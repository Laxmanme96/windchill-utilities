package ext.emerson.datautility.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

		UserRecordHelper helper = new UserRecordHelper(params.getSearchTerm());

		try {
			List<UserRecord> userRecords = helper.searchDataContainedInAllColumns();
			if (userRecords.isEmpty()) {
				result.add(SuggestResult.valueOf(params.getSearchTerm()));
			} else {
				for (UserRecord record : userRecords) {
					logger.debug("Adding to SuggestResult default value:" + params.getSearchTerm());
					result.add(SuggestResult.valueOf(params.getSearchTerm()));
					if (record.getAbreviation() != null) {
						logger.debug("Adding to SuggestResult list : " + SuggestResult.valueOf(record.getAbreviation(),
								record.getAccountName() + "," + record.getDisplayName(), record.getAccountName()));
						result.add(SuggestResult.valueOf(record.getAbreviation(),
								record.getAccountName() + "," + record.getDisplayName(), record.getAccountName()));
					}
				}
			}

		} finally {
			// destroy the data source which should close underlying connections
			if (helper.connectionSource != null) {
				helper.connectionSource.close();
			}
		}
		logger.debug("Results added to User picker " + result.size() + " " + result);
		return result;

	}

}
