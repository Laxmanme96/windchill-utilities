package ext.emerson.migration;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RenameFilePaths {

    public static void main(String[] args) {
        String excelFilePath = "C:\\Users\\E1494388\\Emerson\\FY24-DAUT Appleton Migration - General\\20-Workspace\\40-Migration\\Input Sheet\\RH02\\SOLA Design Document FilePath Correction.xlsx";
        try (FileInputStream fis = new FileInputStream(new File(excelFilePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(1);
            for (Row row : sheet) {
            	String oldFilePath="";
            	String newFilePath="";
            	try { Cell newFilePathCell = row.getCell(0);
                Cell oldFilePathCell = row.getCell(1);

                if (newFilePathCell != null && oldFilePathCell != null) {
                     newFilePath = newFilePathCell.getStringCellValue();
                     oldFilePath = oldFilePathCell.getStringCellValue();
                     System.out.println("Reading row : " + row.getRowNum() + " oldPath -> "+oldFilePath + " newPath -> " +newFilePath);
                 	   renameFile(oldFilePath, newFilePath);
                }
               }
               catch (Exception e) {
            	   System.err.println("Failed to rename: " + oldFilePath + " -> " + newFilePath);
                   e.printStackTrace();
			}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void renameFile(String oldFilePath, String newFilePath) {
        try {
            Files.move(Paths.get(oldFilePath), Paths.get(newFilePath));
            System.out.println("Renamed: " + oldFilePath + " -> " + newFilePath);
        } catch (IOException e) {
            System.err.println("Failed to rename: " + oldFilePath + " -> " + newFilePath);
            e.printStackTrace();
        }
    }
}