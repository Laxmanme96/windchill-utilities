package ext.emerson.migration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PartCSV2XML {
	public static void main(String[] args) {
		String csvFile = "C:\\Users\\rajan.agarwal\\OneDrive - Emerson\\Desktop\\RH02\\APPL\\RH02_APPL_Part_Load_Split5.csv";
		String xmlFile = "C:\\Users\\rajan.agarwal\\OneDrive - Emerson\\Desktop\\RH02\\APPL\\RH02_APPL_Part_Load_Split5.xml";
		genXML(csvFile, xmlFile);

	}

	public static void genXML(String csvFile, String xmlFile) {

		try (BufferedReader br = new BufferedReader(new FileReader(csvFile));
				FileWriter writer = new FileWriter(xmlFile)) {

			writer.write("<?xml version=\"1.0\" ?>\n");
			writer.write("<!DOCTYPE NmLoader SYSTEM \"standard12_0.dtd\">\n");
			writer.write("<NmLoader>\n");

			String line;
			boolean firstLine = true;
			while ((line = br.readLine()) != null) {
				if (firstLine&&!line.startsWith("BeginWTPart")) {
					firstLine = false;
					continue; // Skip header line
				}
				System.out.println(line);
				String[] values = line.split(",");
				writer.write(
						"    <csvBeginAPPLPartLoader handler=\"ext.emerson.migration.APPLPartLoader.beginCreateWTPart\">\n");
				writer.write("        <csvuser>" + values[1] + "</csvuser>\n");
				writer.write("        <csvpartName>-</csvpartName>\n");
				writer.write("        <csvpartNumber>" + values[3] + "</csvpartNumber>\n");
				writer.write("        <csvtype>separable</csvtype>\n");
				writer.write("        <csvgenericType>" + values[5] + "</csvgenericType>\n");
				writer.write("        <csvcollapsible>" + values[6] + "</csvcollapsible>\n");
				writer.write("        <csvlogicbasePath>" + values[7] + "</csvlogicbasePath>\n");
				writer.write("        <csvsource>make</csvsource>\n");
				writer.write("        <csvfolder>/Default/Parts</csvfolder>\n");
				writer.write("        <csvlifecycle>Mechanical Part Life Cycle</csvlifecycle>\n");
				writer.write("        <csvview>" + values[11] + "</csvview>\n");
				writer.write("        <csvvariation1>" + values[12] + "</csvvariation1>\n");
				writer.write("        <csvvariation2>" + values[13] + "</csvvariation2>\n");
				writer.write("        <csvteamTemplate>" + values[14] + "</csvteamTemplate>\n");
				writer.write("        <csvlifecyclestate>" + values[15] + "</csvlifecyclestate>\n");
				writer.write("        <csvtypedef>priv.ia.asco.MECHANICAL_PART</csvtypedef>\n");
				writer.write("        <csvversion>" + values[17] + "</csvversion>\n");
				writer.write("        <csviteration>" + values[18] + "</csviteration>\n");
				writer.write("        <csvenditem></csvenditem>\n");
				writer.write("        <csvtraceCode>" + values[20] + "</csvtraceCode>\n");
				writer.write("        <csvorganizationName>" + values[21] + "</csvorganizationName>\n");
				writer.write("        <csvorganizationID>" + values[22] + "</csvorganizationID>\n");
				writer.write("        <csvsecurityLabels>" + values[23] + "</csvsecurityLabels>\n");
				writer.write("        <csvcreateTimestamp>" + values[24] + "</csvcreateTimestamp>\n");
				writer.write("        <csvmodifyTimestamp>" + values[25] + "</csvmodifyTimestamp>\n");
				writer.write("        <csvcustomcreatedby>" + values[26] + "</csvcustomcreatedby>\n");
				writer.write("        <csvcustommodifiedby>" + values[27] + "</csvcustommodifiedby>\n");
				writer.write("        <csvminRequired></csvminRequired>\n");
				writer.write("        <csvmaxAllowed></csvmaxAllowed>\n");
				writer.write("        <csvdefaultUnit></csvdefaultUnit>\n");
				writer.write("        <csvserviceable></csvserviceable>\n");
				writer.write("        <csvservicekit></csvservicekit>\n");
				writer.write("        <csvauthoringLanguage></csvauthoringLanguage>\n");
				writer.write("    </csvBeginAPPLPartLoader>\n");

				if (!values[28].isEmpty()) {
					writer.write("	<csvIBAValue handler=\"wt.iba.value.service.LoadValue.createIBAValue\" >\n");
					writer.write("		<csvdefinition>LEGACY_ID</csvdefinition>\n");
					writer.write("		<csvvalue1>" + values[28] + "</csvvalue1>\n");
					writer.write("		<csvvalue2></csvvalue2>\n");
					writer.write("		<csvdependency_id></csvdependency_id>\n");
					writer.write("	</csvIBAValue>\n");
				}

				writer.write("	<csvEndWTPart handler=\"ext.emerson.migration.APPLPartLoader.endCreateWTPart\" >\n");
				writer.write("		<csvparentContainerPath></csvparentContainerPath>\n");
				writer.write("	</csvEndWTPart>\n");
			}

			writer.write("</NmLoader>\n");

			System.out.println("CSV to XML conversion completed successfully.");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
