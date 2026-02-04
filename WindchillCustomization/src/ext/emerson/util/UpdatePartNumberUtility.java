package ext.emerson.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import wt.dataops.containermove.ContainerMoveHelper;
import wt.fc.IdentityHelper;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTValuedHashMap;
import wt.fc.collections.WTValuedMap;
import wt.folder.Folder;
import wt.folder.FolderHelper;
import wt.inf.container.WTContainerRef;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.part.WTPartMasterIdentity;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressHelper;

public class UpdatePartNumberUtility implements RemoteAccess {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
            System.out.println("Please provide the full path to the input Excel file.");
            return;
        }
        String inputFilePath = args[0];
		RemoteMethodServer rms = RemoteMethodServer.getDefault();
		UpdatePartNumber(inputFilePath);
	}

	public static void UpdatePartNumber(String inputFilePath) {
		int resultRowNum = 1;
		BufferedWriter logWriter = null;

		try {
			File inputFile = new File(inputFilePath);
            if (!inputFile.exists()) {
                System.out.println("Input file does not exist: " + inputFilePath);
                return;
            }
			String parentFolder = inputFile.getParent();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            String resultFilePath = parentFolder + "/Result_Rename_&_Move_Parts_" + timestamp + ".xlsx";
            String logFilePath = parentFolder + "/Log_Rename_&_Move_Parts_" + timestamp + ".txt";
            logWriter = new BufferedWriter(new FileWriter(logFilePath));
	    
	    		System.out.println("Input file: " + inputFile.getName());
			FileInputStream fis = new FileInputStream(inputFile);
			Workbook workbook = new XSSFWorkbook(fis);
          		
			Sheet sheet = workbook.getSheetAt(0);

			Workbook resultWorkbook = new XSSFWorkbook();
			Sheet resultSheet = resultWorkbook.createSheet("Result");
			writeResultHeader(resultSheet);
			int numberCol = -1;
			Row header = sheet.getRow(0);
			for (Cell cell : header) {
				if (cell.getStringCellValue().trim().equalsIgnoreCase("Number")) {
					numberCol = cell.getColumnIndex();
					break;
				}
			}

			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null)
					continue;

				Cell cell = row.getCell(numberCol);
				if (cell == null || cell.getCellType() == CellType.BLANK)
					continue;

				String number = cell.getStringCellValue().trim();
				if (number.isEmpty())
					continue;

				logWriter.write("\n--- Processing part number: " + number + " ---\n");

				try {
					WTPart part = getPart(number);
					if (part == null) {
						logWriter.write("ERROR: Part not found.\n");
						writeResultRow(resultSheet, number, "ERROR", "Part not found", resultRowNum++);
						continue;
					}

					Boolean isPartCheckedOut = WorkInProgressHelper.isCheckedOut(part);
					logWriter.write("Info : Is Part Checked Out : " + isPartCheckedOut + "\n");
					if (isPartCheckedOut) {
						logWriter.write("ERROR: Part is Checked Out.\n");
						writeResultRow(resultSheet, number, "ERROR", "Part is Checked Out", resultRowNum++);
						continue;
					}

					String containerName = part.getContainerName();
					logWriter.write("Info : Part is from " + containerName + " product.\n");
					if ((!containerName.equals("APPL")) && (!containerName.equals("SOLA"))) {
						logWriter.write("ERROR: "+ containerName + " product is invalid.\n");
						writeResultRow(resultSheet, number, "ERROR", "Item from " + containerName +" product", resultRowNum++);
						continue;
					}

					String currentNumber = part.getNumber();
					System.out.println("Prinitng Current Number "+currentNumber);
					String newNumberFormat = "DONOTUSE_" + currentNumber;
					System.out.println("Prinitng New Number  format "+newNumberFormat);
					WTPart existing = getPart(newNumberFormat);
					if (existing != null) {
						writeResultRow(resultSheet, number, "ERROR", "Part exists with new number", resultRowNum++);
						logWriter.write("ERROR: Part already exists with new number.\n");
						continue;
					}

					logWriter.write("INFO: Renaming to: " + newNumberFormat + "\n");
					WTPart newPart = setNewNumber(part, newNumberFormat);
					if (newPart == null) {
						logWriter.write("ERROR: Failed to rename.\n");
						writeResultRow(resultSheet, number, "ERROR", "Failed to rename", resultRowNum++);
						continue;
					}

					try {
						String folderPath = "/Default/Converted to EPM CAD";
						WTContainerRef containerRef = newPart.getContainerReference();
						Folder targetFolder = FolderHelper.service.getFolder(folderPath, containerRef);
						WTValuedMap objFolderMap = new WTValuedHashMap(1);
						objFolderMap.put(newPart, targetFolder);
						WTCollection col = ContainerMoveHelper.service.moveAllVersions(objFolderMap);

					} catch (Exception folderEx) {
						logWriter.write("ERROR: Folder move failed: " + folderEx.getMessage() + "\n");
						writeResultRow(resultSheet, number, "ERROR",
								"Renamed but folder move failed: " + folderEx.getMessage(), resultRowNum++);
						continue;
					}

					writeResultRow(resultSheet, number, "SUCCESS", "Rename and move successful",
							resultRowNum++);
					logWriter.write("SUCCESS: Rename and move successful.\n");

				} catch (Exception ex) {
					logWriter.write("ERROR: Exception occurred: " + ex.getMessage() + "\n");
					ex.printStackTrace();
					writeResultRow(resultSheet, number, "ERROR", ex.getMessage(), resultRowNum++);
				}
			}

			// Save output files
			try (FileOutputStream fos = new FileOutputStream(resultFilePath)) {
				resultWorkbook.write(fos);
			}
			resultWorkbook.close();
			workbook.close();
			fis.close();

			System.out.println("Result file saved at: " + resultFilePath);
			System.out.println("Log file saved at: " + logFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (logWriter != null) {
				try {
					logWriter.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

		public static WTPart getPart(String number) {
		try {
			QuerySpec querySpec = new QuerySpec(WTPart.class);
			querySpec.appendWhere(new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, number),
					null);

			QueryResult result = wt.fc.PersistenceHelper.manager.find(querySpec);
			while (result.hasMoreElements()) {
				WTPart part = (WTPart) result.nextElement();
				 QueryResult qr = VersionControlHelper.service.allVersionsOf(part);
	                while (qr.hasMoreElements()) {
	                	WTPart latestPart = (WTPart) qr.nextElement();
	                    //System.out.println("-------Latest Part with latest iteration " + latestPart.getIdentity() + latestPart.getIterationDisplayIdentifier());
	                    return latestPart;
	                }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static WTPart setNewNumber(WTPart part, String newNumber) {
		try {
			WTPartMaster master = (WTPartMaster) part.getMaster();
			WTPartMasterIdentity identity = (WTPartMasterIdentity) master.getIdentificationObject();
			identity.setNumber(newNumber);
			IdentityHelper.service.changeIdentity(master, identity);
			PersistenceHelper.manager.refresh(master);
			return part;
		} catch (WTPropertyVetoException | WTException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void writeResultHeader(Sheet sheet) {
		Row row = sheet.createRow(0);
		row.createCell(0).setCellValue("Number");
		row.createCell(1).setCellValue("Status");
		row.createCell(2).setCellValue("Message");
	}

	private static void writeResultRow(Sheet sheet, String number, String status, String message, int rowNum) {
		Row row = sheet.createRow(rowNum);
		row.createCell(0).setCellValue(number);
		row.createCell(1).setCellValue(status);
		row.createCell(2).setCellValue(message);
	}
}
