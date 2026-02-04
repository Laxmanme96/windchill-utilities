package ext.emerson.migration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.Logger;

import ext.emerson.properties.CustomProperties;

public class CutFile {
	private static final Logger logger = CustomProperties.getlogger("ext.emerson.migration");

	public static void main(String[] args) {

//		String inDirectoryPath = "D:\\RH01_Migration\\Appleton_Document\\Doc";
//		String outDirectoryPath = "D:\\RH01_Migration\\Appleton_Document\\XML";
		String inputDirectoryPath = "D:\\Prod";
		String outputDirectoryPath = "D:\\Prod\\csv";
		int linesPerFile = 1000;

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(inputDirectoryPath), "*.csv")) {
			for (Path inputFilePath : directoryStream) {
				processFile(inputFilePath, outputDirectoryPath, linesPerFile);
			}
		} catch (IOException ex) {
			logger.debug("An error occurred: " + ex.getMessage());
		}
	}

	private static void processFile(Path inputFilePath, String outputDirectoryPath, int linesPerFile) {
		String line;
		int fcount = 1;
		int linecount = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath.toFile()))) {
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					getOutputFilename(outputDirectoryPath, inputFilePath.getFileName().toString(), fcount)));
			StringBuilder sb = new StringBuilder();

			while ((line = br.readLine()) != null) {
				line = line + "\n";
				sb.append(line);
				linecount++;

				if (linecount >= linesPerFile) {
					bw.write(sb.toString());
					bw.close();
					sb = new StringBuilder();
					fcount++;
					bw = new BufferedWriter(new FileWriter(
							getOutputFilename(outputDirectoryPath, inputFilePath.getFileName().toString(), fcount)));
					linecount = 0;
				}
			}

			// Write any remaining lines if they exist
			if (sb.length() > 0) {
				bw.write(sb.toString());
			}
			bw.close();
		} catch (IOException ex) {
			logger.debug("An error occurred while processing file " + inputFilePath + ": " + ex.getMessage());
		}
	}

	private static String getOutputFilename(String outputDirectoryPath, String originalFilename, int partNumber) {
		String baseFilename = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
		return outputDirectoryPath + "\\" + baseFilename + "_Split" + partNumber + ".csv";
	}
}
