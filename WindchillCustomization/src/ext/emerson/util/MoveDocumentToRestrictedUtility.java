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
import wt.doc.WTDocument;
import wt.fc.QueryResult;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTValuedHashMap;
import wt.fc.collections.WTValuedMap;
import wt.folder.Folder;
import wt.folder.FolderHelper;
import wt.inf.container.WTContainerRef;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressHelper;

public class MoveDocumentToRestrictedUtility implements RemoteAccess {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Please provide the full path to the input Excel file.");
            return;
        }
        String inputFilePath = args[0];

        RemoteMethodServer rms = RemoteMethodServer.getDefault();
        moveDocumentsToRestrictedFolder(inputFilePath);
    }

    @SuppressWarnings("resource")
    public static void moveDocumentsToRestrictedFolder(String inputFilePath) {
        BufferedWriter logWriter = null;
        int resultRowNum = 1;

        try {
            File inputFile = new File(inputFilePath);
            if (!inputFile.exists()) {
                System.out.println("Input file does not exist: " + inputFilePath);
                return;
            }

            String parentFolder = inputFile.getParent();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            String resultFilePath = parentFolder + "/Result_Move_Documents_" + timestamp + ".xlsx";
            String logFilePath = parentFolder + "/Log_Move_Documents_" + timestamp + ".txt";
            logWriter = new BufferedWriter(new FileWriter(logFilePath));

            System.out.println("Input file: " + inputFile.getName());

            FileInputStream fis = new FileInputStream(inputFile);
            Workbook workbook = new XSSFWorkbook(fis);
            
            // Pick the first available sheet
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

			if (numberCol == -1) {
				logWriter.write("ERROR: 'Number' column not found in the Excel file.\n");
				logWriter.close();
				return;
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

				logWriter.write("\n--- Processing document number: " + number + " ---\n");

				try {
					WTDocument doc = getDocument(number);
					if (doc == null) {
						logWriter.write("ERROR: Document not found.\n");
						writeResultRow(resultSheet, number, "ERROR", "Document not found", resultRowNum++);
						continue;
					}

					Boolean isDocCheckedOut = WorkInProgressHelper.isCheckedOut(doc);
					logWriter.write("Info : Is Document Checked Out : " + isDocCheckedOut + "\n");
					if (isDocCheckedOut) {
						logWriter.write("ERROR: Document is Checked Out.\n");
						writeResultRow(resultSheet, number, "ERROR", "Document is Checked Out", resultRowNum++);
						continue;
					}

					String containerName = doc.getContainerName();
					if ((!containerName.equals("APPL")) && (!containerName.equals("SOLA"))) {
						logWriter.write("ERROR: Item is from " + containerName + " product.\n");
						writeResultRow(resultSheet, number, "ERROR", "Item from different product", resultRowNum++);
						continue;
					}

					try {
						
						String folderPath = "/Default/Converted to EPM CAD";
						WTContainerRef containerRef = doc.getContainerReference();
						Folder targetFolder = FolderHelper.service.getFolder(folderPath, containerRef);
						WTValuedMap objFolderMap = new WTValuedHashMap(1);
						objFolderMap.put(doc, targetFolder);
						WTCollection col = ContainerMoveHelper.service.moveAllVersions(objFolderMap);
						
					} catch (Exception folderEx) {
						logWriter.write("ERROR: Folder move failed: " + folderEx.getMessage() + "\n");
						writeResultRow(resultSheet, number, "ERROR", "Folder move failed: " + folderEx.getMessage(),
								resultRowNum++);
								continue;
					}
					logWriter.write("SUCCESS: Document moved to restricted folder.\n");
					writeResultRow(resultSheet, number, "SUCCESS", "Moved to Converted to EPM CAD folder", resultRowNum++);

				} catch (Exception ex) {
					logWriter.write("ERROR: Exception occurred: " + ex.getMessage() + "\n");
					ex.printStackTrace();
					writeResultRow(resultSheet, number, "ERROR", ex.getMessage(), resultRowNum++);
					continue;
				}
			}

			try (FileOutputStream fos = new FileOutputStream(resultFilePath)) {
				resultWorkbook.write(fos);
			}

			workbook.close();
			fis.close();
			resultWorkbook.close();

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

    public static WTDocument getDocument(String number) {
        try {
            QuerySpec querySpec = new QuerySpec(WTDocument.class);
            querySpec.appendWhere(new SearchCondition(WTDocument.class, WTDocument.NUMBER, SearchCondition.EQUAL, number), null);
            QueryResult result = wt.fc.PersistenceHelper.manager.find(querySpec);
            while (result.hasMoreElements()) {
                WTDocument doc = (WTDocument) result.nextElement();
                QueryResult qr = VersionControlHelper.service.allVersionsOf(doc);
                while (qr.hasMoreElements()) {
                    WTDocument latestDoc = (WTDocument) qr.nextElement();  
                    //System.out.println("-------Latest Doc with latest iteration " + latestDoc.getIdentity() + latestDoc.getIterationDisplayIdentifier());
                    return latestDoc;
                }
            }
        } catch (Exception e) {
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
