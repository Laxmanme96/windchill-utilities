package ext.custom.rmi;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.OperationIdentifier;
import com.ptc.core.meta.common.OperationIdentifierConstants;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.iba.value.AttributeContainer;
import wt.iba.value.IBAHolder;
import wt.iba.value.service.IBAValueDBService;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.part.WTPart;
import wt.query.QueryException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

/**
 * This class is used to update Attribute Value from an Excel File and dynamically update success and failed csv files.
 * 
 * The class contains methods to load data from an Excel file, update attribute values on parts in Windchill, 
 * and generate success and failure logs.
 * 
 * @author xxxxxxxx xxxxx
 */
public class UpdateAttributeUtility implements RemoteAccess {

	public static PrintStream successfulData = null;
	public static PrintStream failedData = null;
	public static StringBuilder successSB = new StringBuilder();
	public static StringBuilder failedSB = new StringBuilder();
	public static boolean isClassification = false;

	/**
	 * The main method to initiate the attribute update process by invoking the load method.
	 *
	 * @param args Array containing the Excel file path, username, and password
	 * @throws RemoteException
	 * @throws InvocationTargetException
	 */
	public static void main(String[] args) throws RemoteException, InvocationTargetException {
		RemoteMethodServer rms = RemoteMethodServer.getDefault();
		String csvFile = args[0];
		String userName = args[1];
		String password = args[2];
		Object[] argValues = { csvFile };
		Class[] argTypes = { String.class };
		try {
			rms.setUserName(userName);
			rms.setPassword(password);
			rms.invoke("load", UpdateAttributeUtility.class.getName(), null, argTypes, argValues);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the data from the Excel file and processes it to update attribute values.
	 *
	 * @param excelFile The path to the Excel file containing attribute data
	 * @throws Exception
	 */
	public static void load(String excelFile) throws Exception {
		
		System.out.println("Input excel--------"+excelFile);
		String successfulOutput = excelFile.replace(".xlsx", "-success.csv");
		String failedOutput = excelFile.replace(".xlsx", "-failed.csv");

		int rowCount = 0;
		int successCount = 0;
		int failedCount = 0;

		try (PrintStream successfulData = new PrintStream(new FileOutputStream(successfulOutput), true, "UTF-8");
				PrintStream failedData = new PrintStream(new FileOutputStream(failedOutput), true, "UTF-8")) {

			successfulData.println("Input file : " + excelFile);
			// Read the Excel file using Apache POI
			List<Map<String, String>> excelRecordList = readExcel(excelFile);

			for (Map<String, String> row : excelRecordList) {
				try {
					successSB.append("\nRow " + (rowCount + 1) + ": ");
					// Handle each row
					if (row.size() < 3) {
						successSB.append("\nEmpty row: " + row);
						successCount++;
						continue;
					}
					updateAtrributeValues(row, successSB, failedSB);
					successSB.append("\nUpdate Attribute End");
					successCount++;
				} catch (Exception e) {
					failedSB.append("\nException: " + e.getMessage() + " in row: " + row);
					failedCount++;
				}
				rowCount++;
				// Periodically write to the success and failure files
				if (rowCount % 10000 == 0) {
					successfulData.println(successSB);
					failedData.println(failedSB);
					successSB = new StringBuilder();
					failedSB = new StringBuilder();
				}
			}
			// Write the final results to the success and failure files
			successfulData.println(successSB);
			failedData.println(failedSB);
			successfulData.println("Success rows: " + successCount);
			failedData.println("Failed rows: " + failedCount);
		} catch (Exception e) {
			failedSB.append("\nException in method load: " + e.getMessage());
		}
	}

	/**
	 * Method to update attribute values on a WTPart.
	 *
	 * @param map Contains the attribute name and value to be updated
	 * @param successSB StringBuilder to record successful updates
	 * @param failedSB StringBuilder to record failed updates
	 */
	private static void updateAtrributeValues(Map<String, String> map, StringBuilder successSB,
			StringBuilder failedSB) {
		successSB.append("\nUpdate Attribute Start");
		WTPart part = null;
		try {
			String partNumber = map.get("Part_Number");
			part = partNumber != null ? findPart(partNumber) : null;
			if (part != null) {
				for (Map.Entry<String, String> entry : map.entrySet()) {
					boolean isSuccess = false;
					if (entry.getKey().equals("Part_Number"))
						continue;
					else {
						isSuccess = setAttributeValueWithoutCheckout(part, part.getNumber(), entry.getKey(),
								entry.getValue());
						if (isSuccess) {
							successSB.append(
									"\nSUCCESS : " + part.getNumber() + " " + entry.getKey() + " " + entry.getValue());
						}
					}
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method to update the attribute value on a WTPart without checkout.
	 *
	 * @param persistable The object representing the WTPart to update
	 * @param partNumber The part number of the WTPart
	 * @param attributeName The name of the attribute to update
	 * @param attributeValue The new value of the attribute
	 * @return True if the update is successful, false otherwise
	 * @throws WTException
	 */
	public static boolean setAttributeValueWithoutCheckout(Persistable persistable, String partNumber,
			String attributeName, Object attributeValue) throws WTException {
		boolean isSuccess = false;
		try {
			PersistableAdapter persistableAdapter = new PersistableAdapter(persistable, null, SessionHelper.getLocale(),
					OperationIdentifier.newOperationIdentifier(OperationIdentifierConstants.UPDATE));
			persistableAdapter.load(attributeName);
			persistableAdapter.set(attributeName, attributeValue);
			persistable = persistableAdapter.apply();
			PersistenceServerHelper.manager.update(persistable, false);
			AttributeContainer attributecontainer = new IBAValueDBService()
					.updateAttributeContainer((IBAHolder) persistable, persistableAdapter, null, null);
			((IBAHolder) persistable).setAttributeContainer(attributecontainer);
			isSuccess = true;
		} catch (WTException e) {
			isSuccess = false;
			failedSB.append("\nFAILED : " + partNumber + " " + attributeName + " " + attributeValue);
			failedSB.append("\nDue To : \n" + e.getLocalizedMessage());
			e.printStackTrace();
		}
		return isSuccess;
	}

	/**
	 * Reads data from the Excel file and stores each row as a Map of attribute values.
	 *
	 * @param filePath The path to the Excel file
	 * @return A list of maps, where each map represents a row of attribute data from the Excel file
	 * @throws IOException
	 */
	private static List<Map<String, String>> readExcel(String filePath) throws IOException {
		List<Map<String, String>> list = new ArrayList<>();
		try (InputStream file = new FileInputStream(filePath);
			Workbook workbook = new XSSFWorkbook(file)) {
			Sheet sheet = workbook.getSheetAt(0); // Read the first sheet

			// Get header map, assume the first row is the header
			Row headerRow = sheet.getRow(0);
			LinkedHashMap<String, Integer> headerMap = new LinkedHashMap<>();
			for (Cell cell : headerRow) {
				headerMap.put(cell.getStringCellValue(), cell.getColumnIndex());
			}
			// Iterate over rows, starting from the second row
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null)
					continue; // Skip empty rows

				Map<String, String> rowData = new LinkedHashMap<>();
				for (String header : headerMap.keySet()) {
					Cell cell = row.getCell(headerMap.get(header));
					String value = getCellValueAsString(cell);
					rowData.put(header, value);
				}
				list.add(rowData);
			}
		}
		return list;
	}

	/**
	 * Helper method to convert the value of an Excel cell to a String.
	 *
	 * @param cell The Excel cell to extract the value from
	 * @return The string representation of the cell's value
	 */
	private static String getCellValueAsString(Cell cell) {
		if (cell == null) {
			return "";
		}
		switch (cell.getCellType()) {
		case STRING:
			return cell.getStringCellValue();
		case NUMERIC:
			if (DateUtil.isCellDateFormatted(cell)) {
				return cell.getDateCellValue().toString();
			} else {
				return Double.toString(cell.getNumericCellValue());
			}
		case BOOLEAN:
			return Boolean.toString(cell.getBooleanCellValue());
		case FORMULA:
			return cell.getCellFormula();
		case BLANK:
			return "";
		default:
			return "";
		}
	}

	/**
	 * Finds a WTPart by its part number.
	 *
	 * @param number The part number of the WTPart to find
	 * @return The WTPart object, or null if no part is found
	 * @throws QueryException
	 */
	public static WTPart findPart(String number) throws QueryException {
		boolean isLatest = true;
		QuerySpec qs = new QuerySpec(WTPart.class);
		try {
			qs.appendWhere(
					new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, number.toUpperCase()),
					null);
			QueryResult qr = PersistenceHelper.manager.find(qs);
			while (qr.hasMoreElements()) {
				WTPart part = (WTPart) qr.nextElement();
				QueryResult allVersion = VersionControlHelper.service.allVersionsOf(part);
				while (allVersion.hasMoreElements()) {
					if (isLatest) {
						return (WTPart) allVersion.nextElement();
					}
				}
				isLatest = false;
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}

//windchill ext.custom.utility.UpdateAttributeUtility C:\ptc\Windchill_12.1\UpdateAttributes.xlsx wcadmin wcadmin