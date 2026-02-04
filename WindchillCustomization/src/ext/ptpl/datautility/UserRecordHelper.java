package ext.ptpl.datautility;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.db.jdbc.ConnectionSource;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;

import ext.emerson.properties.CustomProperties;

/**
 *
 */
public class UserRecordHelper {
	// we are using the SQLite database
	private final CustomProperties props = new CustomProperties(CustomProperties.USER);
	private static final Logger logger = CustomProperties.getlogger("ext.emerson.windchill.datautility");

	private String DATABASE_URL = props.getProperty("ext.emerson.datautility.user.db");
	private Dao<UserRecord, Integer> userRecDao;
	private String searchTerm = "";
	ConnectionSource connectionSource = null;

	public static void main(String[] args) throws Exception {
		System.out.println(logger.getName() + logger.isDebugEnabled());
		UserRecordHelper helper = new UserRecordHelper();

		try {
			helper.setSearchTerm("lam");
			helper.searchDataContainedInAllColumns();
		} finally {
			// destroy the data source which should close underlying connections
			if (helper.connectionSource != null) {
				helper.connectionSource.close();
			}
		}
	}

	/**
	 * @throws Exception
	 */
	public UserRecordHelper() throws Exception {
		init();
	}

	private void init() throws Exception {

		// create our data-source for the database
		Class.forName("org.sqlite.JDBC");
		connectionSource = new JdbcConnectionSource(DATABASE_URL);
		// setup our database and DAOs
		setupDatabase(connectionSource);
		logger.debug("Connection established to db " + connectionSource);
	}

	/**
	 * Setup our database and DAOs
	 */
	/**
	 * @param connectionSource
	 * @throws Exception
	 */
	private void setupDatabase(ConnectionSource connectionSource) throws Exception {

		userRecDao = DaoManager.createDao(connectionSource, UserRecord.class);

		// if you need to create the table
		// TableUtils.createTable(connectionSource, UserRecord.class);
	}

	/**
	 * Read and write some example data.
	 */
	public List<UserRecord> searchDataContainedInAllColumns() throws Exception {
		// query for all items in the database
		List<UserRecord> userRecords = userRecDao.queryForAll();

		// construct a query using the QueryBuilder
		QueryBuilder<UserRecord, Integer> statementBuilder = userRecDao.queryBuilder();

		statementBuilder.where().like(UserRecord.DISPLAY_NAME_FIELD, "%" + getSearchTerm() + "%").or().like(UserRecord.ACCOUNT_NAME_FIELD, "%" + getSearchTerm() + "%").or().like(UserRecord.ABBREVIATION_FIELD, "%" + getSearchTerm() + "%");

		logger.debug("statementBuilder : " + statementBuilder.prepareStatementString());

// statementBuilder.joinOr(statementBuilder)
//		for (UserRecord rec : userRecords) {
//			logger.debug("Found all user : " + rec);
//		}
		userRecords = userRecDao.query(statementBuilder.prepare());
		for (UserRecord rec : userRecords) {
			logger.debug("Found user in db : " + rec);
			String temp = rec.getAccountName() + "  " + rec.getAbreviation() + " " + rec.getDisplayName();
			String abbr = temp.split("\\s+")[1];
			logger.debug("abbr : " + abbr);

		}
		return userRecords;
	}

	/**
	 * Read anlogger.debugle data.
	 */
	public List<UserRecord> searchDataContainedInAbbrevColumn() throws Exception {
		// query for all items in the database
		List<UserRecord> userRecords = userRecDao.queryForAll();

		// construct a query using the QueryBuilder
		QueryBuilder<UserRecord, Integer> statementBuilder = userRecDao.queryBuilder();
		statementBuilder.where().eq(UserRecord.ABBREVIATION_FIELD, getSearchTerm());

		logger.debug("statementBuilder : " + statementBuilder.prepareStatementString());
		logger.debug("statementBuilder info: " + statementBuilder.prepareStatementInfo());

// statementBuilder.joinOr(statementBuilder)
//		for (UserRecord rec : userRecords) {
//			logger.debug("Found all user : " + rec);
//		}
		userRecords = userRecDao.query(statementBuilder.prepare());
		for (UserRecord rec : userRecords) {
			logger.debug("Found user : " + rec);
			String temp = rec.getAccountName() + "  " + rec.getAbreviation() + " " + rec.getDisplayName();
			String abbr = temp.split("\\s+")[1];
			logger.debug("abbr : " + abbr);

		}
		return userRecords;
	}

	/**
	 * Verify that the record stored in the database was the same as the expected
	 * object.
	 */
	private void verifyDb(int id, UserRecord expected) throws SQLException, Exception {
		// make sure we can read it back
		UserRecord record2 = userRecDao.queryForId(id);
		if (record2 == null) {
			throw new Exception("Should have found id '" + id + "' in the database");
		}
		verifyAccount(expected, record2);
	}

	/**
	 * Verify that the account is the same as expected.
	 */
	private static void verifyAccount(UserRecord expected, UserRecord record2) {
		if (expected != record2) {
			logger.debug("expected record name does not equal other account name" + expected + " : " + record2);

		}
	}

	/**
	 * @return the searchTerm
	 */
	public String getSearchTerm() {
		return searchTerm;
	}

	/**
	 * @param searchTerm the searchTerm to set
	 */
	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}

}
