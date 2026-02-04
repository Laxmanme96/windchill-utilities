package ext.emerson.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;

import ext.emerson.properties.CustomProperties;

public class JDBCConnection {
	private static Connection conn = null;

	private static final Logger logger = CustomProperties.getlogger("ext.emerson.migration");

	public static void main(String[] args) throws SQLException {
		Connection connection = null;
		try {
			connection = JDBCConnection.connect(args);
			System.out.println("Connection established!!!!!!!!!!!!!!!!");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if (connection != null) {
				connection.close();
				System.out.println("Connection closed.");
			}
		}

	}
	public static Connection connect(String[] args) throws SQLException {
		// JDBC URL, username and password of MySQL server

		DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());

		//"jdbc:sqlserver://serverName\\instanceName:portNumber;databaseName=yourDatabase";
//		String dbURL = "jdbc:sqlserver://USGDCASDENGDB07\\PDMLINK:1433;encrypt=true;"
//				+ "trustServerCertificate=true;databaseName=PDMLINK";
//		String user = "PDMLINK";
//		String pass = "PDMLINK";


		String dbURL = "jdbc:sqlserver://USSTLIAPENGDB03\\PDMLINK11:1442;encrypt=true;"
				+ "trustServerCertificate=true;databaseName=PDMLINK";
		String user = "PDMLINK";
		String pass = "PDMLINK";


		// JDBC variables for opening and managing connection

		try {
			// Step 1: Loading JDBC driver
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

			// Step 2: Establishing connection
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(dbURL, user, pass);
			System.out.println("Connected successfully!");

			// Do something with the connection here...

		} catch (SQLException e) {
			System.out.println("SQL Exception: " + e.getMessage());
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Class Not Found Exception: " + e.getMessage());
			e.printStackTrace();
		}
		return conn;
	}
}