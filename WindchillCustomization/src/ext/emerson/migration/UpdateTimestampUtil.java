package ext.emerson.migration;

import java.io.File;
/*     */
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
/*     */ import java.sql.ResultSet;
/*     */ import java.sql.Statement;
/*     */ import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import wt.log4j.LogR;

public class UpdateTimestampUtil {
	private static final Logger logger = LogR.getLoggerInternal("ext.emerson.migration");
	private static Connection connection;

	public static void main(String[] args) throws Exception {
		try {
			connection = JDBCConnection.connect(null);
			// Specify the path to your Excel file
			String excelFilePath = "E:\\ptc\\Windchill_12.0\\Windchill\\RH02_test_timestamp.xslx";
			if (args!=null && args.length > 0 && args[0] != null) {
				excelFilePath = args[0];
			}

			List<Record> records = readWorkBook(excelFilePath);

			// Print all records. Get doc and update revs
			for (Record record : records) {

				store(record, record.createTimestamp, record.modifyTimestamp);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Step 3: Closing connection
			if (connection != null) {
				connection.close();
				System.out.println("Connection closed.");
			}
		}
	}

	public static List<Record> readWorkBook(String excelFilePath) throws IOException{
		// Create a FileInputStream to read from the Excel file
					FileInputStream inputStream = new FileInputStream(new File(excelFilePath));

					// Create a workbook object
					Workbook workbook = WorkbookFactory.create(inputStream);

					// Get the first sheet
					Sheet sheet = workbook.getSheetAt(0);

					// Initialize a list to store records
					List<Record> records = new ArrayList<>();
					// Iterate through each row
					for (Row row : sheet) {
						if (row.getRowNum() == 0) {
							// Skip header row
							continue;
						}

						// Read data from each column of the row
						String number = row.getCell(0).getStringCellValue();
						String version = row.getCell(1).getStringCellValue();
						String iteration = row.getCell(2).getStringCellValue();

						String createTimestamp = row.getCell(3).getStringCellValue();
						String modifyTimestamp = row.getCell(4).getStringCellValue();

						// Create a Record object and add it to the list
						Record record = new Record(number, version, iteration, createTimestamp, modifyTimestamp);
					//	System.out.println("Reading : " + record);
						records.add(record);
					}

					// Close workbook and input stream
					workbook.close();
					inputStream.close();
					return records;

	}
	public static void store(Record record, String create, String modify) throws Exception {

		if(connection==null || connection.isClosed()) {
			connection = JDBCConnection.connect(null);
		}
		Statement statement1 = connection.createStatement();
		Statement statement2 = connection.createStatement();
		Statement statement3 = connection.createStatement();

		connection.setAutoCommit(true);

		try {
			ResultSet resultset = statement1.executeQuery(
					"select d.idA2A2 id,dm.WTDocumentNumber num,d.versionIdA2versionInfo ver,d.iterationIdA2iterationInfo iter, d.modifyStampA2 modifyT, d.createStampA2 createT from WTDocument d, WTDocumentMaster dm where d.idA3masterReference=dm.idA2A2 and dm.WTDocumentNumber='"
							+ record.number + "' and d.versionIdA2versionInfo='" + record.version
							+ "' and d.iterationIdA2iterationInfo='" + record.iteration + "'");
			while (resultset.next()) {
				// C.2

				System.out.println(resultset.getString("id") + " Before db number :" + resultset.getString("num")
						+ " db ver.iter : " + resultset.getString("ver") + "." + resultset.getString("iter")
						+ " db modify  " + resultset.getString("modifyT") + " db create "
						+ resultset.getString("createT"));
//				int temp = statement2.executeUpdate("update WTDocument set modifyStampA2='" + modify
//						+ "' where idA2A2 = '" + resultset.getString("id") + "'");
//
				int temp = statement2.executeUpdate("update WTDocument set modifyStampA2='" + modify
						+ "', createStampA2='" + create + "' where idA2A2 = '" + resultset.getString("id") + "'");

				System.out.println("rows updated " + temp);

			}

			connection.commit();
			connection.setAutoCommit(true);

			ResultSet resultset2 = statement3.executeQuery(
					"select d.idA2A2 id,dm.WTDocumentNumber num,d.versionIdA2versionInfo ver,d.iterationIdA2iterationInfo iter, d.modifyStampA2 modifyT, d.createStampA2 createT from WTDocument d, WTDocumentMaster dm where d.idA3masterReference=dm.idA2A2 and dm.WTDocumentNumber='"
							+ record.number + "' and d.versionIdA2versionInfo='" + record.version
							+ "' and d.iterationIdA2iterationInfo='" + record.iteration + "'");

			while (resultset2.next()) {
				// C.2

				System.out.println(resultset2.getString("id") + " After update db number :"
						+ resultset2.getString("num") + " db ver.iter : " + resultset2.getString("ver") + "."
						+ resultset2.getString("iter") + " db modify  " + resultset2.getString("modifyT")
						+ " db create " + resultset2.getString("createT"));

			}
		}

		catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			statement1.close();
			statement2.close();
			statement3.close();
		}

	}

	// Define a Record class to store the data
	static class Record implements Serializable {
		String number;
		String version;
		String iteration;
		String createTimestamp;
		String modifyTimestamp;

		public Record(String number, String version, String iteration2, String createTimestamp2,
				String modifyTimestamp2) {
			this.number = number;
			this.version = version;
			this.iteration = iteration2;
			this.createTimestamp = createTimestamp2;
			this.modifyTimestamp = modifyTimestamp2;
		}

		@Override
		public String toString() {
			return "Record{" + "number='" + number + '\'' + ", version='" + version + '\'' + ", iteration=" + iteration
					+ ", createTimestamp=" + createTimestamp + ", modifyTimestamp=" + modifyTimestamp + '}';
		}
	}

}