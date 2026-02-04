package ext.emerson.migration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.Logger;

import ext.emerson.properties.CustomProperties;
import wt.doc.WTDocument;
import wt.fc.ReferenceFactory;
import wt.util.WTException;
import wt.util.WTRuntimeException;

public class AnalyseMigrationRepFailure {
	static String inputDirectoryPath = "D:/RH01_Migration/AllDocsNoRep_APPLcontext.csv";
	static String outputDirectoryPath = "D:/RH01_Migration/AllDocsNoRep_APPLcontextwithNumbers.csv";
	private static final Logger logger = CustomProperties.getlogger("ext.emerson.migration");

	public static void main(String[] args) throws WTRuntimeException, WTException {
		AnalyseMigrationRepFailure.getDocAndwriteRecords();
	}
	private static void getDocAndwriteRecords() throws WTRuntimeException, WTException {
		String line;

		try (BufferedReader br = new BufferedReader(new FileReader(inputDirectoryPath))) {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputDirectoryPath));

			while ((line = br.readLine()) != null) {
				StringBuilder sb = new StringBuilder();
				String[] str = line.split(",");

				for(String oid: str) {
					logger.debug("reading : " + oid);
					if(oid ==null || oid.isEmpty()) {break;

					}
						sb.append(oid + ",");
					WTDocument doc = getDoc(oid);
					if(doc!=null) {
						sb.append(doc.getNumber() + "," +doc.getVersionIdentifier().getValue() + ","+doc.getIterationIdentifier().getValue());
					}
					bw.write(sb.toString() +"\n");
				}
			}

			bw.close();
		} catch (IOException ex) {
			logger.debug("An error occurred while processing file " + inputDirectoryPath + ": " + ex.getMessage());
		}
	}


	private static WTDocument getDoc(String oid) throws WTRuntimeException, WTException {
		ReferenceFactory ref = new ReferenceFactory();
		// TODO Auto-generated method stub
		return ((WTDocument)ref.getReference(oid).getObject());

	}
}